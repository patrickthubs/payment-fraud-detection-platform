package com.frauddetection.platform.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignFraudCaseRequest(
    @NotBlank String assignee,
    @NotBlank String note
) {
}
