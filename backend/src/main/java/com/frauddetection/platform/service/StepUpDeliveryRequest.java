package com.frauddetection.platform.service;

import com.frauddetection.platform.entity.FraudOperatorEntity;
import com.frauddetection.platform.model.StepUpDeliveryChannel;
import java.time.Instant;
import java.util.UUID;

public record StepUpDeliveryRequest(
    UUID deliveryId,
    FraudOperatorEntity operator,
    StepUpDeliveryChannel deliveryChannel,
    String destination,
    String destinationMasked,
    String verificationUrl,
    Instant expiresAt
) {
}
