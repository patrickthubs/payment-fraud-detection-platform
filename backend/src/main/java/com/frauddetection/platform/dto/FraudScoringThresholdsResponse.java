package com.frauddetection.platform.dto;

public record FraudScoringThresholdsResponse(
    int challengeThreshold,
    int holdThreshold,
    int declineThreshold
) {
}
