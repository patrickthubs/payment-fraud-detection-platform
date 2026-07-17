package com.frauddetection.platform.service;

import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.RiskDecision;
import java.time.Instant;
import java.util.UUID;

public record PaymentStatusChangedEvent(
    UUID assessmentId,
    String paymentId,
    String customerId,
    PaymentStatus previousStatus,
    PaymentStatus currentStatus,
    RiskDecision latestDecision,
    String source,
    String reason,
    Instant changedAt
) {
}
