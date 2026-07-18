package com.frauddetection.platform.service;

import com.frauddetection.platform.dto.FraudScoringOverrideRequest;
import com.frauddetection.platform.dto.PaymentRiskAssessmentRequest;
import com.frauddetection.platform.model.FraudRuleSet;
import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.RiskDecision;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Service;

@Service
public class FraudSimulationService {

    private final VelocityFeatureService velocityFeatureService;
    private final FraudRiskScoringService fraudRiskScoringService;
    private final PlatformMetricsService platformMetricsService;

    public FraudSimulationService(
        VelocityFeatureService velocityFeatureService,
        FraudRiskScoringService fraudRiskScoringService,
        PlatformMetricsService platformMetricsService
    ) {
        this.velocityFeatureService = velocityFeatureService;
        this.fraudRiskScoringService = fraudRiskScoringService;
        this.platformMetricsService = platformMetricsService;
    }

    public FraudSimulationResult simulate(PaymentRiskAssessmentRequest request) {
        Timer.Sample sample = platformMetricsService.startAssessmentSample();
        VelocitySnapshot velocitySnapshot = velocityFeatureService.resolveFeatures(request);
        FraudRiskAssessment assessment = fraudRiskScoringService.assess(request, velocitySnapshot);
        return toSimulationResult(sample, velocitySnapshot, assessment);
    }

    public FraudSimulationResult simulate(
        PaymentRiskAssessmentRequest request,
        FraudScoringProfile scoringProfile
    ) {
        Timer.Sample sample = platformMetricsService.startAssessmentSample();
        VelocitySnapshot velocitySnapshot = velocityFeatureService.resolveFeatures(request);
        FraudRiskAssessment assessment = fraudRiskScoringService.assess(request, velocitySnapshot, scoringProfile);
        return toSimulationResult(sample, velocitySnapshot, assessment);
    }

    public FraudScoringProfile activeProfile() {
        return fraudRiskScoringService.activeProfile();
    }

    public FraudScoringProfile mergeOverrides(FraudScoringOverrideRequest overrides) {
        return mergeOverrides(overrides, null);
    }

    public FraudScoringProfile mergeOverrides(FraudScoringOverrideRequest overrides, FraudRuleSet candidateRules) {
        FraudScoringProfile baseProfile = activeProfile();
        return new FraudScoringProfile(
            overrides.challengeThreshold() != null ? overrides.challengeThreshold() : baseProfile.challengeThreshold(),
            overrides.holdThreshold() != null ? overrides.holdThreshold() : baseProfile.holdThreshold(),
            overrides.declineThreshold() != null ? overrides.declineThreshold() : baseProfile.declineThreshold(),
            candidateRules == null ? baseProfile.rulesetVersion() : "candidate-rules",
            candidateRules == null ? baseProfile.rules() : candidateRules
        );
    }

    private FraudSimulationResult toSimulationResult(
        Timer.Sample sample,
        VelocitySnapshot velocitySnapshot,
        FraudRiskAssessment assessment
    ) {
        PaymentStatus projectedPaymentStatus = mapDecisionToStatus(assessment.decision());
        boolean reviewCaseWouldBeCreated = assessment.decision() == RiskDecision.HOLD
            || assessment.decision() == RiskDecision.DECLINE;

        platformMetricsService.recordAssessment(
            sample,
            assessment.decision(),
            projectedPaymentStatus,
            velocitySnapshot.source(),
            reviewCaseWouldBeCreated
        );

        return new FraudSimulationResult(
            velocitySnapshot.source(),
            projectedPaymentStatus,
            reviewCaseWouldBeCreated,
            assessment
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
}
