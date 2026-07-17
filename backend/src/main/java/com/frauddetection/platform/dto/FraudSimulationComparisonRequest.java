package com.frauddetection.platform.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record FraudSimulationComparisonRequest(
    @NotNull @Valid PaymentRiskAssessmentRequest scenario,
    @NotNull @Valid FraudScoringOverrideRequest overrides
) {
}
