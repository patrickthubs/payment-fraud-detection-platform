package com.frauddetection.platform.dto;

import com.frauddetection.platform.model.StepUpDeliveryChannel;
import com.frauddetection.platform.model.StepUpDeliveryStatus;
import java.time.Instant;
import java.util.UUID;

public record StepUpDeliveryResponse(
    UUID deliveryId,
    String operator,
    StepUpDeliveryChannel deliveryChannel,
    String destinationMasked,
    StepUpDeliveryStatus status,
    int resendSequence,
    int attemptCount,
    String failureReason,
    Instant createdAt,
    Instant expiresAt,
    Instant deliveredAt,
    Instant consumedAt,
    Instant revokedAt
) {
}
