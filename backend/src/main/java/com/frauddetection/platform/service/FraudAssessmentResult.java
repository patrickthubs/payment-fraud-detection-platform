package com.frauddetection.platform.service;

import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.VelocitySource;
import java.util.UUID;

public record FraudAssessmentResult(
    UUID assessmentId,
    UUID reviewCaseId,
    VelocitySource velocitySource,
    PaymentStatus paymentStatus,
    FraudRiskAssessment assessment
) {
}
