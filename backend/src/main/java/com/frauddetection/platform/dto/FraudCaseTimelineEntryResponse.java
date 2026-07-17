package com.frauddetection.platform.dto;

import com.frauddetection.platform.model.FraudCaseActionType;
import java.time.Instant;
import java.util.UUID;

public record FraudCaseTimelineEntryResponse(
    UUID entryId,
    FraudCaseActionType actionType,
    String actor,
    String detail,
    Instant createdAt
) {
}
