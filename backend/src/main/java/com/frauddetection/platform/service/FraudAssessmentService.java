package com.frauddetection.platform.service;

import com.frauddetection.platform.dto.PaymentRiskAssessmentRequest;
import com.frauddetection.platform.dto.FraudScoringProfileResponse;
import com.frauddetection.platform.entity.FraudAssessmentRecordEntity;
import com.frauddetection.platform.entity.FraudReviewCaseEntity;
import com.frauddetection.platform.entity.PaymentRecordEntity;
import com.frauddetection.platform.model.ReviewCaseStatus;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.repository.FraudAssessmentRecordRepository;
import com.frauddetection.platform.repository.FraudReviewCaseRepository;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.time.Clock;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.platform.exception.IdempotencyConflictException;
import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.RiskFactor;
import org.springframework.beans.factory.annotation.Autowired;
import java.util.UUID;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudAssessmentService {

    private final VelocityFeatureService velocityFeatureService;
    private final FraudRiskScoringService fraudRiskScoringService;
    private final FraudAssessmentRecordRepository fraudAssessmentRecordRepository;
    private final FraudReviewCaseRepository fraudReviewCaseRepository;
    private final FraudAssessmentEventPublisher fraudAssessmentEventPublisher;
    private final PaymentLifecycleService paymentLifecycleService;
    private final PlatformMetricsService platformMetricsService;
    private final FraudNotificationHookService fraudNotificationHookService;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public FraudAssessmentService(
        VelocityFeatureService velocityFeatureService,
        FraudRiskScoringService fraudRiskScoringService,
        FraudAssessmentRecordRepository fraudAssessmentRecordRepository,
        FraudReviewCaseRepository fraudReviewCaseRepository,
        FraudAssessmentEventPublisher fraudAssessmentEventPublisher,
        PaymentLifecycleService paymentLifecycleService,
        PlatformMetricsService platformMetricsService,
        FraudNotificationHookService fraudNotificationHookService
    ) {
        this(
            velocityFeatureService, fraudRiskScoringService, fraudAssessmentRecordRepository,
            fraudReviewCaseRepository, fraudAssessmentEventPublisher, paymentLifecycleService,
            platformMetricsService, fraudNotificationHookService, Clock.systemUTC(), new ObjectMapper()
        );
    }

    @Autowired
    public FraudAssessmentService(
        VelocityFeatureService velocityFeatureService,
        FraudRiskScoringService fraudRiskScoringService,
        FraudAssessmentRecordRepository fraudAssessmentRecordRepository,
        FraudReviewCaseRepository fraudReviewCaseRepository,
        FraudAssessmentEventPublisher fraudAssessmentEventPublisher,
        PaymentLifecycleService paymentLifecycleService,
        PlatformMetricsService platformMetricsService,
        FraudNotificationHookService fraudNotificationHookService,
        Clock clock,
        ObjectMapper objectMapper
    ) {
        this.velocityFeatureService = velocityFeatureService;
        this.fraudRiskScoringService = fraudRiskScoringService;
        this.fraudAssessmentRecordRepository = fraudAssessmentRecordRepository;
        this.fraudReviewCaseRepository = fraudReviewCaseRepository;
        this.fraudAssessmentEventPublisher = fraudAssessmentEventPublisher;
        this.paymentLifecycleService = paymentLifecycleService;
        this.platformMetricsService = platformMetricsService;
        this.fraudNotificationHookService = fraudNotificationHookService;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public FraudAssessmentResult assess(PaymentRiskAssessmentRequest request) {
        return assess(request, null);
    }

    @Transactional
    public FraudAssessmentResult assess(PaymentRiskAssessmentRequest request, String idempotencyKey) {
        String normalizedKey = normalizeKey(idempotencyKey);
        String requestHash = hash(request);
        if (normalizedKey != null) {
            FraudAssessmentResult existingResult = fraudAssessmentRecordRepository.findByIdempotencyKey(normalizedKey)
                .map(existing -> restoreIdempotentResult(existing, requestHash, normalizedKey))
                .orElse(null);
            if (existingResult != null) {
                return existingResult;
            }
        }

        Timer.Sample sample = platformMetricsService.startAssessmentSample();
        Instant assessedAt = clock.instant();
        VelocitySnapshot velocitySnapshot = velocityFeatureService.resolveFeatures(request);
        FraudScoringProfileResponse profileDetails = fraudRiskScoringService.activeProfileDetails();
        FraudScoringProfile profile = profileDetails == null
            ? fraudRiskScoringService.activeProfile()
            : new FraudScoringProfile(
                profileDetails.thresholds().challengeThreshold(),
                profileDetails.thresholds().holdThreshold(),
                profileDetails.thresholds().declineThreshold(),
                profileDetails.rulesetVersion(),
                profileDetails.rules()
            );
        FraudRiskAssessment assessment = fraudRiskScoringService.assess(request, velocitySnapshot, profile);

        FraudAssessmentRecordEntity assessmentRecord = fraudAssessmentRecordRepository.save(new FraudAssessmentRecordEntity(
            UUID.randomUUID(),
            request.paymentId(),
            request.customerId(),
            assessment.riskScore(),
            assessment.decision(),
            velocitySnapshot.source(),
            assessment.summary(),
            assessment.triggeredFactors().stream().map(factor -> factor.code().name()).toList().toString(),
            profileDetails == null ? null : profileDetails.profileId(),
            profileDetails == null ? 0 : profileDetails.versionNumber(),
            profile.rulesetVersion(),
            writeJson(request),
            writeJson(assessment.triggeredFactors()),
            normalizedKey,
            requestHash,
            assessedAt
        ));

        PaymentRecordEntity paymentRecord = paymentLifecycleService.recordAssessmentOutcome(request, assessmentRecord, assessedAt);
        FraudReviewCaseEntity reviewCase = maybeCreateReviewCase(assessmentRecord, assessedAt);
        fraudAssessmentEventPublisher.publish(assessmentRecord, assessment, reviewCase, assessedAt);
        fraudNotificationHookService.publishAssessmentNotifications(paymentRecord, reviewCase, assessment, assessedAt);
        velocityFeatureService.recordAssessment(request, velocitySnapshot);
        platformMetricsService.recordAssessment(
            sample,
            assessment.decision(),
            paymentRecord.getPaymentStatus(),
            velocitySnapshot.source(),
            reviewCase != null
        );

        return new FraudAssessmentResult(
            assessmentRecord.getId(),
            reviewCase == null ? null : reviewCase.getId(),
            velocitySnapshot.source(),
            paymentRecord.getPaymentStatus(),
            assessment
        );
    }

    private FraudAssessmentResult restoreIdempotentResult(
        FraudAssessmentRecordEntity existing,
        String requestHash,
        String idempotencyKey
    ) {
        if (!requestHash.equals(existing.getRequestHash())) {
            throw new IdempotencyConflictException(idempotencyKey);
        }
        List<RiskFactor> factors = readFactors(existing.getFactorDetails());
        FraudRiskAssessment assessment = new FraudRiskAssessment(
            existing.getRiskScore(), existing.getDecision(), existing.getSummary(), factors
        );
        UUID reviewCaseId = fraudReviewCaseRepository.findByAssessmentId(existing.getId())
            .map(FraudReviewCaseEntity::getId)
            .orElse(null);
        return new FraudAssessmentResult(
            existing.getId(), reviewCaseId, existing.getVelocitySource(), mapDecisionToStatus(existing.getDecision()), assessment
        );
    }

    private PaymentStatus mapDecisionToStatus(RiskDecision decision) {
        return switch (decision) {
            case ALLOW -> PaymentStatus.APPROVED;
            case CHALLENGE -> PaymentStatus.CHALLENGED;
            case HOLD -> PaymentStatus.HELD;
            case DECLINE -> PaymentStatus.DECLINED;
        };
    }

    private String normalizeKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            return null;
        }
        String normalized = idempotencyKey.trim();
        if (normalized.length() > 120) {
            throw new IllegalArgumentException("Idempotency-Key must not exceed 120 characters.");
        }
        return normalized;
    }

    private String hash(PaymentRiskAssessmentRequest request) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(writeJson(request).getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is unavailable.", exception);
        }
    }

    private String writeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not persist the fraud decision snapshot.", exception);
        }
    }

    private List<RiskFactor> readFactors(String value) {
        try {
            return List.copyOf(objectMapper.readValue(value, new TypeReference<List<RiskFactor>>() { }));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored fraud factor details are invalid.", exception);
        }
    }

    private FraudReviewCaseEntity maybeCreateReviewCase(FraudAssessmentRecordEntity assessmentRecord, Instant createdAt) {
        if (assessmentRecord.getDecision() != RiskDecision.HOLD && assessmentRecord.getDecision() != RiskDecision.DECLINE) {
            return null;
        }

        return fraudReviewCaseRepository.save(new FraudReviewCaseEntity(
            UUID.randomUUID(),
            assessmentRecord.getId(),
            assessmentRecord.getPaymentId(),
            assessmentRecord.getCustomerId(),
            assessmentRecord.getRiskScore(),
            assessmentRecord.getDecision(),
            ReviewCaseStatus.OPEN,
            assessmentRecord.getSummary(),
            null,
            null,
            null,
            createdAt,
            createdAt
        ));
    }
}
