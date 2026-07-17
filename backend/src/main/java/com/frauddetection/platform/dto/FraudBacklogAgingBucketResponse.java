package com.frauddetection.platform.dto;

public record FraudBacklogAgingBucketResponse(
    String bucket,
    long totalCount,
    long openCount,
    long escalatedCount,
    boolean breached
) {
}
