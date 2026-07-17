package com.frauddetection.platform.service;

import com.frauddetection.platform.dto.FraudOutboundAnalyticsResponse;
import com.frauddetection.platform.entity.FraudOutboundEventEntity;
import com.frauddetection.platform.model.FraudOutboundEventStatus;
import com.frauddetection.platform.repository.FraudOutboundEventRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FraudOutboundAnalyticsService {

    private static final int DEFAULT_WINDOW_DAYS = 7;
    private static final int MAX_WINDOW_DAYS = 30;

    private final FraudOutboundEventRepository fraudOutboundEventRepository;
    private final Clock clock;

    public FraudOutboundAnalyticsService(
        FraudOutboundEventRepository fraudOutboundEventRepository,
        Clock clock
    ) {
        this.fraudOutboundEventRepository = fraudOutboundEventRepository;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public FraudOutboundAnalyticsResponse getAnalytics(Integer days) {
        int windowDays = normalizeWindowDays(days);
        Instant snapshotAt = clock.instant();
        Instant createdAfter = snapshotAt.minus(Duration.ofDays(windowDays));
        List<FraudOutboundEventEntity> eventsInWindow = fraudOutboundEventRepository
            .findByCreatedAtGreaterThanEqualOrderByCreatedAtAsc(createdAfter);
        List<FraudOutboundEventEntity> failedEvents = fraudOutboundEventRepository
            .findByStatusOrderByCreatedAtAsc(FraudOutboundEventStatus.FAILED);

        return new FraudOutboundAnalyticsResponse(
            snapshotAt,
            windowDays,
            buildRetryMetrics(eventsInWindow),
            buildDailyTrends(eventsInWindow, windowDays, snapshotAt),
            buildIncidentAging(failedEvents, snapshotAt)
        );
    }

    private FraudOutboundAnalyticsResponse.FraudOutboundRetryMetricsResponse buildRetryMetrics(
        List<FraudOutboundEventEntity> eventsInWindow
    ) {
        long totalEvents = eventsInWindow.size();
        long retriedEvents = eventsInWindow.stream()
            .filter(event -> event.getAttemptCount() > 1)
            .count();
        long deliveredAfterRetryCount = eventsInWindow.stream()
            .filter(event -> event.getStatus() == FraudOutboundEventStatus.DELIVERED)
            .filter(event -> event.getAttemptCount() > 1)
            .count();
        long failedAfterRetryCount = eventsInWindow.stream()
            .filter(event -> event.getStatus() == FraudOutboundEventStatus.FAILED)
            .filter(event -> event.getAttemptCount() > 1)
            .count();
        BigDecimal retryRate = percentage(retriedEvents, totalEvents);
        BigDecimal averageAttemptCount = totalEvents == 0
            ? BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP)
            : BigDecimal.valueOf(eventsInWindow.stream()
                .mapToInt(FraudOutboundEventEntity::getAttemptCount)
                .average()
                .orElse(0.0d))
                .setScale(2, RoundingMode.HALF_UP);

        return new FraudOutboundAnalyticsResponse.FraudOutboundRetryMetricsResponse(
            totalEvents,
            retriedEvents,
            retryRate,
            deliveredAfterRetryCount,
            failedAfterRetryCount,
            averageAttemptCount
        );
    }

    private List<FraudOutboundAnalyticsResponse.FraudOutboundDailyTrendResponse> buildDailyTrends(
        List<FraudOutboundEventEntity> eventsInWindow,
        int windowDays,
        Instant snapshotAt
    ) {
        LocalDate endDay = snapshotAt.atZone(ZoneOffset.UTC).toLocalDate();
        LocalDate startDay = endDay.minusDays(windowDays - 1L);

        Map<LocalDate, Map<FraudOutboundEventStatus, Long>> grouped = eventsInWindow.stream()
            .collect(java.util.stream.Collectors.groupingBy(
                event -> event.getCreatedAt().atZone(ZoneOffset.UTC).toLocalDate(),
                java.util.stream.Collectors.groupingBy(
                    FraudOutboundEventEntity::getStatus,
                    () -> new EnumMap<>(FraudOutboundEventStatus.class),
                    java.util.stream.Collectors.counting()
                )
            ));

        List<FraudOutboundAnalyticsResponse.FraudOutboundDailyTrendResponse> trends = new ArrayList<>();
        for (LocalDate day = startDay; !day.isAfter(endDay); day = day.plusDays(1)) {
            Map<FraudOutboundEventStatus, Long> counts = grouped.getOrDefault(
                day,
                Map.of()
            );
            trends.add(new FraudOutboundAnalyticsResponse.FraudOutboundDailyTrendResponse(
                day.toString(),
                counts.getOrDefault(FraudOutboundEventStatus.PENDING, 0L),
                counts.getOrDefault(FraudOutboundEventStatus.DELIVERED, 0L),
                counts.getOrDefault(FraudOutboundEventStatus.FAILED, 0L)
            ));
        }
        return trends;
    }

    private List<FraudOutboundAnalyticsResponse.FraudOutboundIncidentAgingBucketResponse> buildIncidentAging(
        List<FraudOutboundEventEntity> failedEvents,
        Instant snapshotAt
    ) {
        List<IncidentAgingBucket> buckets = List.of(
            new IncidentAgingBucket("UNDER_15_MINUTES", Duration.ZERO, Duration.ofMinutes(15)),
            new IncidentAgingBucket("BETWEEN_15_MINUTES_AND_1_HOUR", Duration.ofMinutes(15), Duration.ofHours(1)),
            new IncidentAgingBucket("BETWEEN_1_HOUR_AND_4_HOURS", Duration.ofHours(1), Duration.ofHours(4)),
            new IncidentAgingBucket("OVER_4_HOURS", Duration.ofHours(4), null)
        );

        return buckets.stream()
            .map(bucket -> toBucketResponse(bucket, failedEvents, snapshotAt))
            .toList();
    }

    private FraudOutboundAnalyticsResponse.FraudOutboundIncidentAgingBucketResponse toBucketResponse(
        IncidentAgingBucket bucket,
        List<FraudOutboundEventEntity> failedEvents,
        Instant snapshotAt
    ) {
        List<FraudOutboundEventEntity> matches = failedEvents.stream()
            .filter(event -> bucket.matches(Duration.between(event.getCreatedAt(), snapshotAt)))
            .toList();

        long notedCount = matches.stream()
            .filter(event -> event.getOperatorNote() != null && !event.getOperatorNote().isBlank())
            .count();

        return new FraudOutboundAnalyticsResponse.FraudOutboundIncidentAgingBucketResponse(
            bucket.label(),
            matches.size(),
            notedCount,
            matches.size() - notedCount
        );
    }

    private int normalizeWindowDays(Integer days) {
        if (days == null) {
            return DEFAULT_WINDOW_DAYS;
        }
        return Math.max(1, Math.min(days, MAX_WINDOW_DAYS));
    }

    private BigDecimal percentage(long numerator, long denominator) {
        if (denominator == 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(numerator)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(denominator), 2, RoundingMode.HALF_UP);
    }

    private record IncidentAgingBucket(
        String label,
        Duration lowerInclusive,
        Duration upperExclusive
    ) {
        private boolean matches(Duration age) {
            boolean meetsLowerBound = !age.minus(lowerInclusive).isNegative();
            boolean belowUpperBound = upperExclusive == null || age.compareTo(upperExclusive) < 0;
            return meetsLowerBound && belowUpperBound;
        }
    }
}
