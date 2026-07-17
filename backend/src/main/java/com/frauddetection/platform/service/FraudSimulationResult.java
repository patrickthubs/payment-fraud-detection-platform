package com.frauddetection.platform.service;

import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.VelocitySource;

public record FraudSimulationResult(
    VelocitySource velocitySource,
    PaymentStatus projectedPaymentStatus,
    boolean reviewCaseWouldBeCreated,
    FraudRiskAssessment assessment
) {
}
