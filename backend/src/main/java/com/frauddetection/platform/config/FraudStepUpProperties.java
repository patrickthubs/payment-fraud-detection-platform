package com.frauddetection.platform.config;

import com.frauddetection.platform.model.StepUpDeliveryChannel;
import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("fraud.security.step-up")
public record FraudStepUpProperties(
    Duration tokenValidity,
    Duration sessionValidity,
    boolean exposeVerificationUrl,
    StepUpDeliveryChannel deliveryChannel,
    String fromAddress,
    String subjectPrefix,
    Duration issueWindow,
    int maxIssueAttempts,
    Duration verifyWindow,
    int maxVerifyFailures,
    Duration lockoutDuration
) {
}
