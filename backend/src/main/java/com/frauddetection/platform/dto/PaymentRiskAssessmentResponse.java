package com.frauddetection.platform.dto;

import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.model.VelocitySource;
import java.util.List;
import java.util.UUID;

public record PaymentRiskAssessmentResponse(
    UUID assessmentId,
    UUID reviewCaseId,
    String paymentId,
    String customerId,
    int riskScore,
    RiskDecision decision,
    PaymentStatus paymentStatus,
    VelocitySource velocitySource,
    String summary,
    List<RiskFactorView> triggeredFactors
) {
}
