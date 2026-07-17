package com.frauddetection.platform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

public record FraudScoringOverrideRequest(
    @Min(1) @Max(100) Integer challengeThreshold,
    @Min(1) @Max(100) Integer holdThreshold,
    @Min(1) @Max(100) Integer declineThreshold
) {
}
