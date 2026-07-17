package com.frauddetection.platform.dto;

import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.model.VelocitySource;
import java.util.List;

public record FraudSimulationResponse(
    String paymentId,
    String customerId,
    int riskScore,
    RiskDecision decision,
    PaymentStatus projectedPaymentStatus,
    VelocitySource velocitySource,
    boolean reviewCaseWouldBeCreated,
    String summary,
    List<RiskFactorView> triggeredFactors
) {
}
