package com.frauddetection.platform.dto;

import com.frauddetection.platform.model.FraudOutcomeLabel;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record FraudOutcomeResponse(
    UUID outcomeId,
    UUID assessmentId,
    FraudOutcomeLabel outcomeLabel,
    String source,
    BigDecimal actualLoss,
    BigDecimal recoveredAmount,
    String notes,
    String labelledBy,
    Instant occurredAt,
    Instant createdAt,
    Instant updatedAt
) {
}
