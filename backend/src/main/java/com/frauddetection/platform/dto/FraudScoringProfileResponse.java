package com.frauddetection.platform.dto;

import java.time.Instant;
import java.util.UUID;
import com.frauddetection.platform.model.FraudRuleSet;

public record FraudScoringProfileResponse(
    UUID profileId,
    int versionNumber,
    String profileName,
    boolean active,
    boolean systemDefault,
    FraudScoringThresholdsResponse thresholds,
    String rulesetVersion,
    FraudRuleSet rules,
    String changeSummary,
    String createdBy,
    Instant createdAt,
    String activatedBy,
    Instant activatedAt
) {
}
