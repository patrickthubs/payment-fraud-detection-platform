package com.frauddetection.platform.dto;

import jakarta.validation.constraints.NotBlank;

public record EscalateFraudCaseRequest(
    @NotBlank String reason
) {
}
