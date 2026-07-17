package com.frauddetection.platform.dto;

import com.frauddetection.platform.model.PaymentStatus;
import java.time.Instant;
import java.util.UUID;

public record PaymentTransitionResponse(
    UUID id,
    PaymentStatus fromStatus,
    PaymentStatus toStatus,
    String reason,
    UUID assessmentId,
    Instant createdAt
) {
}
