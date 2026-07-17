package com.frauddetection.platform.dto;

public record FraudOutboundDeliverySummaryResponse(
    long pendingCount,
    long deliveredCount,
    long failedCount
) {
}
