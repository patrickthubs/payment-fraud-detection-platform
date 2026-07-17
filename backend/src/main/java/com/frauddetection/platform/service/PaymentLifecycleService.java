package com.frauddetection.platform.service;

import com.frauddetection.platform.dto.CompletePaymentChallengeRequest;
import com.frauddetection.platform.dto.PaymentRiskAssessmentRequest;
import com.frauddetection.platform.entity.FraudAssessmentRecordEntity;
import com.frauddetection.platform.entity.PaymentRecordEntity;
import com.frauddetection.platform.entity.PaymentStateTransitionEntity;
import com.frauddetection.platform.exception.PaymentChallengeActionNotAllowedException;
import com.frauddetection.platform.exception.PaymentNotFoundException;
import com.frauddetection.platform.model.ChallengeOutcome;
import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.repository.PaymentRecordRepository;
import com.frauddetection.platform.repository.PaymentStateTransitionRepository;
import java.time.Instant;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentLifecycleService {

    private final PaymentRecordRepository paymentRecordRepository;
    private final PaymentStateTransitionRepository paymentStateTransitionRepository;
    private final PlatformMetricsService platformMetricsService;
    private final PaymentOrchestrationEventPublisher paymentOrchestrationEventPublisher;

    public PaymentLifecycleService(
        PaymentRecordRepository paymentRecordRepository,
        PaymentStateTransitionRepository paymentStateTransitionRepository,
        PlatformMetricsService platformMetricsService,
        PaymentOrchestrationEventPublisher paymentOrchestrationEventPublisher
    ) {
        this.paymentRecordRepository = paymentRecordRepository;
        this.paymentStateTransitionRepository = paymentStateTransitionRepository;
        this.platformMetricsService = platformMetricsService;
        this.paymentOrchestrationEventPublisher = paymentOrchestrationEventPublisher;
    }

    @Transactional
    public PaymentRecordEntity recordAssessmentOutcome(
        PaymentRiskAssessmentRequest request,
        FraudAssessmentRecordEntity assessmentRecord,
        Instant updatedAt
    ) {
        PaymentStatus targetStatus = mapDecisionToStatus(assessmentRecord.getDecision());
        PaymentRecordEntity existingRecord = paymentRecordRepository.findByPaymentId(request.paymentId()).orElse(null);

        PaymentStatus previousStatus;
        PaymentRecordEntity paymentRecord;
        if (existingRecord == null) {
            paymentRecord = paymentRecordRepository.save(new PaymentRecordEntity(
                UUID.randomUUID(),
                request.paymentId(),
                request.customerId(),
                request.amount(),
                request.currency(),
                request.paymentChannel(),
                request.merchantCategory(),
                assessmentRecord.getId(),
                assessmentRecord.getRiskScore(),
                assessmentRecord.getDecision(),
                targetStatus,
                null,
                targetStatus == PaymentStatus.CHALLENGED ? updatedAt : null,
                null,
                null,
                null,
                updatedAt,
                updatedAt
            ));
            previousStatus = null;
        } else {
            previousStatus = existingRecord.applyAssessment(
                assessmentRecord.getId(),
                request.amount(),
                request.currency(),
                request.paymentChannel(),
                request.merchantCategory(),
                assessmentRecord.getRiskScore(),
                assessmentRecord.getDecision(),
                targetStatus,
                updatedAt
            );
            paymentRecord = paymentRecordRepository.save(existingRecord);
        }

        paymentStateTransitionRepository.save(new PaymentStateTransitionEntity(
            UUID.randomUUID(),
            paymentRecord.getPaymentId(),
            previousStatus,
            targetStatus,
            assessmentRecord.getSummary(),
            assessmentRecord.getId(),
            updatedAt
        ));
        platformMetricsService.recordPaymentTransition(previousStatus, targetStatus, "ASSESSMENT");
        paymentOrchestrationEventPublisher.publish(new PaymentStatusChangedEvent(
            assessmentRecord.getId(),
            paymentRecord.getPaymentId(),
            paymentRecord.getCustomerId(),
            previousStatus,
            targetStatus,
            paymentRecord.getLatestDecision(),
            "ASSESSMENT",
            assessmentRecord.getSummary(),
            updatedAt
        ));

        return paymentRecord;
    }

    private PaymentStatus mapDecisionToStatus(RiskDecision decision) {
        return switch (decision) {
            case ALLOW -> PaymentStatus.APPROVED;
            case CHALLENGE -> PaymentStatus.CHALLENGED;
            case HOLD -> PaymentStatus.HELD;
            case DECLINE -> PaymentStatus.DECLINED;
        };
    }

    @Transactional
    public PaymentRecordEntity transitionFromCaseResolution(
        String paymentId,
        PaymentStatus targetStatus,
        String reason,
        UUID assessmentId,
        Instant updatedAt
    ) {
        PaymentRecordEntity paymentRecord = paymentRecordRepository.findByPaymentId(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        PaymentStatus previousStatus = paymentRecord.transitionTo(targetStatus, updatedAt);
        PaymentRecordEntity savedRecord = paymentRecordRepository.save(paymentRecord);

        paymentStateTransitionRepository.save(new PaymentStateTransitionEntity(
            UUID.randomUUID(),
            paymentId,
            previousStatus,
            targetStatus,
            reason,
            assessmentId,
            updatedAt
        ));
        platformMetricsService.recordPaymentTransition(previousStatus, targetStatus, "CASE_RESOLUTION");
        paymentOrchestrationEventPublisher.publish(new PaymentStatusChangedEvent(
            assessmentId,
            savedRecord.getPaymentId(),
            savedRecord.getCustomerId(),
            previousStatus,
            targetStatus,
            savedRecord.getLatestDecision(),
            "CASE_RESOLUTION",
            reason,
            updatedAt
        ));

        return savedRecord;
    }

    @Transactional
    public PaymentRecordEntity completeChallenge(
        String paymentId,
        String operator,
        CompletePaymentChallengeRequest request,
        UUID assessmentId,
        Instant updatedAt
    ) {
        PaymentRecordEntity paymentRecord = paymentRecordRepository.findByPaymentId(paymentId)
            .orElseThrow(() -> new PaymentNotFoundException(paymentId));

        if (paymentRecord.getPaymentStatus() != PaymentStatus.CHALLENGED) {
            throw new PaymentChallengeActionNotAllowedException(
                "Payment %s is not awaiting a challenge outcome.".formatted(paymentId)
            );
        }

        UUID resolvedAssessmentId = assessmentId != null ? assessmentId : paymentRecord.getLatestAssessmentId();
        String reason = "Challenge outcome %s: %s".formatted(request.outcome().name(), request.note());
        PaymentStatus targetStatus = mapChallengeOutcomeToStatus(request.outcome());
        PaymentStatus previousStatus = paymentRecord.completeChallenge(
            request.outcome(),
            operator,
            request.note(),
            targetStatus,
            updatedAt
        );
        PaymentRecordEntity savedRecord = paymentRecordRepository.save(paymentRecord);

        paymentStateTransitionRepository.save(new PaymentStateTransitionEntity(
            UUID.randomUUID(),
            paymentId,
            previousStatus,
            targetStatus,
            reason,
            resolvedAssessmentId,
            updatedAt
        ));
        platformMetricsService.recordPaymentTransition(previousStatus, targetStatus, "CHALLENGE_OUTCOME");
        paymentOrchestrationEventPublisher.publish(new PaymentStatusChangedEvent(
            resolvedAssessmentId,
            savedRecord.getPaymentId(),
            savedRecord.getCustomerId(),
            previousStatus,
            targetStatus,
            savedRecord.getLatestDecision(),
            "CHALLENGE_OUTCOME",
            reason,
            updatedAt
        ));

        return savedRecord;
    }

    private PaymentStatus mapChallengeOutcomeToStatus(ChallengeOutcome challengeOutcome) {
        return switch (challengeOutcome) {
            case PASSED -> PaymentStatus.APPROVED;
            case FAILED, ABANDONED -> PaymentStatus.DECLINED;
        };
    }
}
