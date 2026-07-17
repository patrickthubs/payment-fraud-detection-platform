package com.frauddetection.platform.service;

import com.frauddetection.platform.model.FraudNotificationType;
import com.frauddetection.platform.model.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

public record FraudNotificationEvent(
    FraudNotificationType notificationType,
    UUID reviewCaseId,
    String paymentId,
    String customerId,
    PaymentStatus paymentStatus,
    String subject,
    String message,
    Instant createdAt
) {
}
