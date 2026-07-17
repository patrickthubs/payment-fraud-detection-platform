package com.frauddetection.platform.dto;

import com.frauddetection.platform.model.StepUpDeliveryChannel;
import java.time.Instant;
import java.util.UUID;

public record StepUpTokenResponse(
    UUID deliveryId,
    Instant expiresAt,
    StepUpDeliveryChannel deliveryChannel,
    String destinationMasked,
    String verificationUrl
) {
}
