package com.frauddetection.platform.dto;

import com.frauddetection.platform.model.CaseResolutionOutcome;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;

public record ResolveFraudCaseRequest(
    @NotBlank String resolutionSummary,
    @NotNull CaseResolutionOutcome outcome
) {
}
