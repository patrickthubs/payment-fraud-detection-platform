package com.frauddetection.platform.service;

import com.frauddetection.platform.config.FraudScoringProperties;
import com.frauddetection.platform.model.FraudRuleSet;

public record FraudScoringProfile(
    int challengeThreshold,
    int holdThreshold,
    int declineThreshold,
    String rulesetVersion,
    FraudRuleSet rules
) {

    public FraudScoringProfile(int challengeThreshold, int holdThreshold, int declineThreshold) {
        this(challengeThreshold, holdThreshold, declineThreshold, "rules-v1", FraudRuleSet.defaults());
    }

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
        if (rulesetVersion == null || rulesetVersion.isBlank() || rules == null) {
            throw new IllegalArgumentException("A scoring profile must identify its complete rule set.");
        }
    }

    public static FraudScoringProfile from(FraudScoringProperties properties) {
        return new FraudScoringProfile(
            properties.challengeThreshold(),
            properties.holdThreshold(),
            properties.declineThreshold(),
            "system-rules-v1",
            FraudRuleSet.defaults()
        );
    }
}
