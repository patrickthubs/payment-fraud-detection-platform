package com.frauddetection.platform.dto;

import com.frauddetection.platform.model.ChallengeOutcome;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CompletePaymentChallengeRequest(
    @NotNull ChallengeOutcome outcome,
    @NotBlank @Size(max = 500) String note
) {
}
