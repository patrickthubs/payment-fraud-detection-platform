package com.frauddetection.platform.dto;

import com.frauddetection.platform.model.FraudOutboundEventStatus;
import java.time.Instant;
import java.util.UUID;

public record FraudOutboundEventResponse(
    UUID eventId,
    String eventType,
    String topicName,
    String messageKey,
    FraudOutboundEventStatus status,
    int attemptCount,
    Instant nextAttemptAt,
    Instant lastAttemptedAt,
    Instant publishedAt,
    String lastError,
    String operatorNote,
    String notedBy,
    Instant notedAt,
    Instant createdAt,
    Instant updatedAt
) {
}
