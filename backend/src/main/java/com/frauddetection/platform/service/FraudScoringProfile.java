package com.frauddetection.platform.service;

import com.frauddetection.platform.config.FraudScoringProperties;

public record FraudScoringProfile(
    int challengeThreshold,
    int holdThreshold,
    int declineThreshold
) {

    public FraudScoringProfile {
        if (challengeThreshold <= 0) {
            throw new IllegalArgumentException("challengeThreshold must be greater than zero.");
        }
        if (holdThreshold <= challengeThreshold) {
            throw new IllegalArgumentException("holdThreshold must be greater than challengeThreshold.");
        }
        if (declineThreshold <= holdThreshold || declineThreshold > 100) {
            throw new IllegalArgumentException("declineThreshold must be greater than holdThreshold and at most 100.");
        }
    }

    public static FraudScoringProfile from(FraudScoringProperties properties) {
        return new FraudScoringProfile(
            properties.challengeThreshold(),
            properties.holdThreshold(),
            properties.declineThreshold()
        );
    }
}
