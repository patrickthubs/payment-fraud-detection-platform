package com.frauddetection.platform.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "fraud.events")
public record FraudEventProperties(
    String assessmentTopic,
    String paymentStatusTopic,
    String notificationTopic
) {
}
