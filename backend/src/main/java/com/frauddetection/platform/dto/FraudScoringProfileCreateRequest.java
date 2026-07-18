package com.frauddetection.platform.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import com.frauddetection.platform.model.FraudRuleSet;

public record FraudScoringProfileCreateRequest(
    @NotBlank @Size(max = 120) String profileName,
    @Min(1) @Max(100) int challengeThreshold,
    @Min(1) @Max(100) int holdThreshold,
    @Min(1) @Max(100) int declineThreshold,
    @NotBlank @Size(max = 500) String changeSummary,
    FraudRuleSet rules
) {
    public FraudScoringProfileCreateRequest(
        String profileName,
        int challengeThreshold,
        int holdThreshold,
        int declineThreshold,
        String changeSummary
    ) {
        this(profileName, challengeThreshold, holdThreshold, declineThreshold, changeSummary, null);
    }
}
