package com.frauddetection.platform.dto;

import java.time.Instant;
import java.util.UUID;

public record FraudScoringProfileResponse(
    UUID profileId,
    int versionNumber,
    String profileName,
    boolean active,
    boolean systemDefault,
    FraudScoringThresholdsResponse thresholds,
    String changeSummary,
    String createdBy,
    Instant createdAt,
    String activatedBy,
    Instant activatedAt
) {
}
