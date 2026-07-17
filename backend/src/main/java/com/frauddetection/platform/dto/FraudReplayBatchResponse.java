package com.frauddetection.platform.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FraudReplayBatchResponse(
    UUID batchId,
    String batchName,
    int scenarioCount,
    FraudScoringThresholdsResponse thresholds,
    String createdBy,
    Instant createdAt,
    List<FraudMetricCountResponse> decisions,
    List<FraudMetricCountResponse> projectedPaymentStatuses,
    long reviewCaseWouldBeCreatedCount,
    List<FraudReplayBatchItemResponse> items
) {
}
