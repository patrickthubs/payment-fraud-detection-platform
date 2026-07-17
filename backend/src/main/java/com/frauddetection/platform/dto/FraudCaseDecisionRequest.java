package com.frauddetection.platform.dto;

import jakarta.validation.constraints.NotBlank;

public record FraudCaseDecisionRequest(
    @NotBlank String resolutionSummary
) {
}
