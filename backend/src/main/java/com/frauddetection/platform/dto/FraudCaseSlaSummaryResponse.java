package com.frauddetection.platform.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record FraudCaseSlaSummaryResponse(
    Instant snapshotAt,
    BigDecimal targetHours,
    long breachedBacklogCount,
    long breachedOpenCaseCount,
    long breachedEscalatedCaseCount,
    BigDecimal breachRate,
    List<FraudBacklogAgingBucketResponse> backlogAgingBuckets
) {
}
