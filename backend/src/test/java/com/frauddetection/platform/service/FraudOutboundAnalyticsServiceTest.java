package com.frauddetection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.frauddetection.platform.dto.FraudOutboundAnalyticsResponse;
import com.frauddetection.platform.entity.FraudOutboundEventEntity;
import com.frauddetection.platform.model.FraudOutboundEventStatus;
import com.frauddetection.platform.repository.FraudOutboundEventRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class FraudOutboundAnalyticsServiceTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString("f2000000-0000-0000-0000-000000000001");

    private FraudOutboundEventRepository fraudOutboundEventRepository;
    private CurrentTenantService currentTenantService;
    private FraudOutboundAnalyticsService fraudOutboundAnalyticsService;

    @BeforeEach
    void setUp() {
        fraudOutboundEventRepository = mock(FraudOutboundEventRepository.class);
        currentTenantService = mock(CurrentTenantService.class);
        when(currentTenantService.organizationId()).thenReturn(ORGANIZATION_ID);
        fraudOutboundAnalyticsService = new FraudOutboundAnalyticsService(
            fraudOutboundEventRepository,
            currentTenantService,
            Clock.fixed(Instant.parse("2026-07-17T18:00:00Z"), ZoneOffset.UTC)
        );
    }

    @Test
    void buildsRetryTrendsAndIncidentAgingAnalytics() {
        when(fraudOutboundEventRepository.findByOrganizationIdAndCreatedAtGreaterThanEqualOrderByCreatedAtAsc(
            ORGANIZATION_ID,
            Instant.parse("2026-07-14T18:00:00Z")
        )).thenReturn(List.of(
            event(FraudOutboundEventStatus.DELIVERED, 2, Instant.parse("2026-07-15T10:00:00Z"), null),
            event(
                FraudOutboundEventStatus.FAILED,
                3,
                Instant.parse("2026-07-16T11:00:00Z"),
                "Investigating downstream timeout."
            ),
            event(FraudOutboundEventStatus.PENDING, 1, Instant.parse("2026-07-17T07:30:00Z"), null)
        ));
        when(fraudOutboundEventRepository.findByOrganizationIdAndStatusOrderByCreatedAtAsc(ORGANIZATION_ID, FraudOutboundEventStatus.FAILED))
            .thenReturn(List.of(
                event(FraudOutboundEventStatus.FAILED, 3, Instant.parse("2026-07-17T17:50:00Z"), null),
                event(
                    FraudOutboundEventStatus.FAILED,
                    2,
                    Instant.parse("2026-07-17T17:01:00Z"),
                    "Awaiting broker recovery."
                ),
                event(FraudOutboundEventStatus.FAILED, 2, Instant.parse("2026-07-17T15:30:00Z"), null),
                event(
                    FraudOutboundEventStatus.FAILED,
                    4,
                    Instant.parse("2026-07-17T11:00:00Z"),
                    "Escalated for manual follow-up."
                )
            ));

        FraudOutboundAnalyticsResponse response = fraudOutboundAnalyticsService.getAnalytics(3);

        assertThat(response.windowDays()).isEqualTo(3);
        assertThat(response.retryMetrics().totalEventsInWindow()).isEqualTo(3);
        assertThat(response.retryMetrics().retriedEvents()).isEqualTo(2);
        assertThat(response.retryMetrics().retryRate()).isEqualByComparingTo("66.67");
        assertThat(response.retryMetrics().deliveredAfterRetryCount()).isEqualTo(1);
        assertThat(response.retryMetrics().failedAfterRetryCount()).isEqualTo(1);
        assertThat(response.retryMetrics().averageAttemptCount()).isEqualByComparingTo("2.00");
        assertThat(response.dailyTrends()).hasSize(3);
        assertThat(response.dailyTrends()).extracting(FraudOutboundAnalyticsResponse.FraudOutboundDailyTrendResponse::day)
            .containsExactly("2026-07-15", "2026-07-16", "2026-07-17");
        assertThat(response.dailyTrends().get(0).deliveredCount()).isEqualTo(1);
        assertThat(response.dailyTrends().get(1).failedCount()).isEqualTo(1);
        assertThat(response.dailyTrends().get(2).pendingCount()).isEqualTo(1);
        assertThat(response.incidentAging()).extracting(FraudOutboundAnalyticsResponse.FraudOutboundIncidentAgingBucketResponse::bucket)
            .containsExactly(
                "UNDER_15_MINUTES",
                "BETWEEN_15_MINUTES_AND_1_HOUR",
                "BETWEEN_1_HOUR_AND_4_HOURS",
                "OVER_4_HOURS"
            );
        assertThat(response.incidentAging().get(0).totalCount()).isEqualTo(1);
        assertThat(response.incidentAging().get(0).unnotedCount()).isEqualTo(1);
        assertThat(response.incidentAging().get(1).totalCount()).isEqualTo(1);
        assertThat(response.incidentAging().get(1).notedCount()).isEqualTo(1);
        assertThat(response.incidentAging().get(2).totalCount()).isEqualTo(1);
        assertThat(response.incidentAging().get(2).unnotedCount()).isEqualTo(1);
        assertThat(response.incidentAging().get(3).totalCount()).isEqualTo(1);
        assertThat(response.incidentAging().get(3).notedCount()).isEqualTo(1);
    }

    private FraudOutboundEventEntity event(
        FraudOutboundEventStatus status,
        int attemptCount,
        Instant createdAt,
        String operatorNote
    ) {
        return new FraudOutboundEventEntity(
            UUID.randomUUID(),
            ORGANIZATION_ID,
            "CASE_ESCALATED",
            "fraud-notifications",
            "PAY-401",
            "{\"paymentId\":\"PAY-401\"}",
            status,
            attemptCount,
            createdAt,
            createdAt,
            status == FraudOutboundEventStatus.DELIVERED ? createdAt.plusSeconds(30) : null,
            status == FraudOutboundEventStatus.FAILED ? "broker unavailable" : null,
            operatorNote,
            operatorNote == null ? null : "analyst.one",
            operatorNote == null ? null : createdAt.plusSeconds(60),
            createdAt,
            createdAt
        );
    }
}
