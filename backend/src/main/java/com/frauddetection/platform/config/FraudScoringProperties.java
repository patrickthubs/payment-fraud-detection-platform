package com.frauddetection.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fraud.scoring")
public record FraudScoringProperties(
    int challengeThreshold,
    int holdThreshold,
    int declineThreshold
) {
}
