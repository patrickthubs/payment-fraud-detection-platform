package com.frauddetection.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties("fraud.outbox")
public record FraudOutboxProperties(
    @DefaultValue("true") boolean enabled,
    @DefaultValue("50") int batchSize,
    @DefaultValue("5") int maxAttempts,
    @DefaultValue("15000") long dispatchDelayMs,
    @DefaultValue("30000") long retryDelayMs
) {
}
