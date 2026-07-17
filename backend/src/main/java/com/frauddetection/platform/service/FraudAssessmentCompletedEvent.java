package com.frauddetection.platform.service;

import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.model.VelocitySource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FraudAssessmentCompletedEvent(
    UUID assessmentId,
    UUID reviewCaseId,
    String paymentId,
    String customerId,
    int riskScore,
    RiskDecision decision,
    VelocitySource velocitySource,
    List<String> triggeredFactorCodes,
    String summary,
    Instant assessedAt
) {
}
