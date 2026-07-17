package com.frauddetection.platform.dto;

import jakarta.validation.constraints.NotBlank;

public record FraudCaseNoteRequest(
    @NotBlank String note
) {
}
