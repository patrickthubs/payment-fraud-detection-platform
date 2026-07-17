package com.frauddetection.platform.service;

import com.frauddetection.platform.dto.PaymentRiskAssessmentRequest;
import com.frauddetection.platform.entity.FraudAssessmentRecordEntity;
import com.frauddetection.platform.entity.FraudReviewCaseEntity;
import com.frauddetection.platform.entity.PaymentRecordEntity;
import com.frauddetection.platform.model.ReviewCaseStatus;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.repository.FraudAssessmentRecordRepository;
import com.frauddetection.platform.repository.FraudReviewCaseRepository;
import io.micrometer.core.instrument.Timer;
import java.time.Instant;
import java.util.UUID;
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
        this.velocityFeatureService = velocityFeatureService;
        this.fraudRiskScoringService = fraudRiskScoringService;
        this.fraudAssessmentRecordRepository = fraudAssessmentRecordRepository;
        this.fraudReviewCaseRepository = fraudReviewCaseRepository;
        this.fraudAssessmentEventPublisher = fraudAssessmentEventPublisher;
        this.paymentLifecycleService = paymentLifecycleService;
        this.platformMetricsService = platformMetricsService;
        this.fraudNotificationHookService = fraudNotificationHookService;
    }

    @Transactional
    public FraudAssessmentResult assess(PaymentRiskAssessmentRequest request) {
        Timer.Sample sample = platformMetricsService.startAssessmentSample();
        Instant assessedAt = Instant.now();
        VelocitySnapshot velocitySnapshot = velocityFeatureService.resolveFeatures(request);
        FraudRiskAssessment assessment = fraudRiskScoringService.assess(request, velocitySnapshot);

        FraudAssessmentRecordEntity assessmentRecord = fraudAssessmentRecordRepository.save(new FraudAssessmentRecordEntity(
            UUID.randomUUID(),
            request.paymentId(),
            request.customerId(),
            assessment.riskScore(),
            assessment.decision(),
            velocitySnapshot.source(),
            assessment.summary(),
            assessment.triggeredFactors().stream().map(factor -> factor.code().name()).toList().toString(),
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
