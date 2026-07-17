package com.frauddetection.platform.dto;

public record FraudSimulationComparisonResponse(
    String paymentId,
    String customerId,
    FraudSimulationOutcomeResponse baselineOutcome,
    FraudSimulationOutcomeResponse overrideOutcome,
    boolean decisionChanged,
    boolean projectedPaymentStatusChanged,
    boolean reviewCaseCreationChanged,
    String comparisonSummary
) {
}
