package com.frauddetection.platform.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record FraudOutboundAnalyticsResponse(
    Instant snapshotAt,
    int windowDays,
    FraudOutboundRetryMetricsResponse retryMetrics,
    List<FraudOutboundDailyTrendResponse> dailyTrends,
    List<FraudOutboundIncidentAgingBucketResponse> incidentAging
) {
    public record FraudOutboundRetryMetricsResponse(
        long totalEventsInWindow,
        long retriedEvents,
        BigDecimal retryRate,
        long deliveredAfterRetryCount,
        long failedAfterRetryCount,
        BigDecimal averageAttemptCount
    ) {
    }

    public record FraudOutboundDailyTrendResponse(
        String day,
        long pendingCount,
        long deliveredCount,
        long failedCount
    ) {
    }

    public record FraudOutboundIncidentAgingBucketResponse(
        String bucket,
        long totalCount,
        long notedCount,
        long unnotedCount
    ) {
    }
}
