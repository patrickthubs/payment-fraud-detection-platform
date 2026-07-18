package com.frauddetection.platform.dto;

import com.frauddetection.platform.model.FraudRuleSet;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

public record FraudSimulationComparisonRequest(
    @NotNull @Valid PaymentRiskAssessmentRequest scenario,
    @NotNull @Valid FraudScoringOverrideRequest overrides,
    FraudRuleSet rules
) {
}
