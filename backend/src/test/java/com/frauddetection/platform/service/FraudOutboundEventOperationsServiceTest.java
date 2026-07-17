package com.frauddetection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.frauddetection.platform.dto.FraudOutboundDispatchResponse;
import com.frauddetection.platform.dto.FraudOutboundEventResponse;
import com.frauddetection.platform.dto.FraudOutboundIncidentNoteRequest;
import com.frauddetection.platform.dto.FraudOutboundRetryBatchResponse;
import com.frauddetection.platform.entity.FraudOutboundEventEntity;
import com.frauddetection.platform.exception.FraudOutboundEventActionNotAllowedException;
import com.frauddetection.platform.model.FraudOutboundEventStatus;
import com.frauddetection.platform.repository.FraudOutboundEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

class FraudOutboundEventOperationsServiceTest {

    private FraudOutboundEventRepository fraudOutboundEventRepository;
    private ObjectProvider<FraudOutboundEventDispatcher> fraudOutboundEventDispatcherProvider;
    private FraudOutboundEventDispatcher fraudOutboundEventDispatcher;
    private FraudOutboundEventOperationsService fraudOutboundEventOperationsService;

    @BeforeEach
    void setUp() {
        fraudOutboundEventRepository = mock(FraudOutboundEventRepository.class);
        fraudOutboundEventDispatcher = mock(FraudOutboundEventDispatcher.class);
        fraudOutboundEventDispatcherProvider = new TestObjectProvider(null);
        fraudOutboundEventOperationsService = new FraudOutboundEventOperationsService(
            fraudOutboundEventRepository,
            fraudOutboundEventDispatcherProvider
        );
    }

    @Test
    void returnsFailedEventsForOperatorReview() {
        when(fraudOutboundEventRepository.findAll(
            anySpecification(),
            any(Pageable.class)
        )).thenReturn(new PageImpl<>(List.of(event(FraudOutboundEventStatus.FAILED, 3))));

        List<FraudOutboundEventResponse> response = fraudOutboundEventOperationsService.findEvents(
            FraudOutboundEventStatus.FAILED,
            null,
            null,
            null,
            null,
            25
        );

        assertThat(response).singleElement().satisfies(event -> {
            assertThat(event.status()).isEqualTo(FraudOutboundEventStatus.FAILED);
            assertThat(event.attemptCount()).isEqualTo(3);
            assertThat(event.lastError()).contains("broker unavailable");
        });
    }

    @Test
    void retriesFailedEventByMovingItBackToPending() {
        UUID eventId = UUID.randomUUID();
        FraudOutboundEventEntity entity = event(FraudOutboundEventStatus.FAILED, 4);
        when(fraudOutboundEventRepository.findById(eventId)).thenReturn(Optional.of(entity));

        FraudOutboundEventResponse response = fraudOutboundEventOperationsService.retryEvent(eventId);

        assertThat(response.eventId()).isEqualTo(entity.getId());
        assertThat(response.status()).isEqualTo(FraudOutboundEventStatus.PENDING);
        assertThat(response.nextAttemptAt()).isNotNull();
    }

    @Test
    void rejectsRetryForDeliveredEvent() {
        UUID eventId = UUID.randomUUID();
        when(fraudOutboundEventRepository.findById(eventId))
            .thenReturn(Optional.of(event(FraudOutboundEventStatus.DELIVERED, 1)));

        assertThatThrownBy(() -> fraudOutboundEventOperationsService.retryEvent(eventId))
            .isInstanceOf(FraudOutboundEventActionNotAllowedException.class)
            .hasMessage("Delivered outbound events cannot be retried.");
    }

    @Test
    void storesIncidentNoteOnOutboundEvent() {
        UUID eventId = UUID.randomUUID();
        FraudOutboundEventEntity entity = event(FraudOutboundEventStatus.FAILED, 2);
        when(fraudOutboundEventRepository.findById(eventId)).thenReturn(Optional.of(entity));

        FraudOutboundEventResponse response = fraudOutboundEventOperationsService.addIncidentNote(
            eventId,
            "senior.analyst",
            new FraudOutboundIncidentNoteRequest("Reviewed after broker outage.")
        );

        assertThat(response.operatorNote()).isEqualTo("Reviewed after broker outage.");
        assertThat(response.notedBy()).isEqualTo("senior.analyst");
        assertThat(response.notedAt()).isNotNull();
    }

    @Test
    void retriesBatchOfFailedEvents() {
        when(fraudOutboundEventRepository.findByStatusOrderByCreatedAtDesc(
            FraudOutboundEventStatus.FAILED,
            Pageable.ofSize(2).withPage(0)
        )).thenReturn(List.of(
            event(FraudOutboundEventStatus.FAILED, 2),
            event(FraudOutboundEventStatus.FAILED, 1)
        ));

        FraudOutboundRetryBatchResponse response = fraudOutboundEventOperationsService.retryFailedEvents(2);

        assertThat(response.requested()).isEqualTo(2);
        assertThat(response.queuedForRetry()).isEqualTo(2);
    }

    @Test
    void exportsFailedEventsAsCsv() {
        when(fraudOutboundEventRepository.findByStatusOrderByCreatedAtDesc(
            FraudOutboundEventStatus.FAILED,
            Pageable.ofSize(25).withPage(0)
        )).thenReturn(List.of(event(FraudOutboundEventStatus.FAILED, 3)));

        String csv = fraudOutboundEventOperationsService.exportFailedEvents(25);

        assertThat(csv).contains("eventId,eventType,topicName,messageKey,status");
        assertThat(csv).contains("\"PAYMENT_RELEASED\"");
        assertThat(csv).contains("\"broker unavailable\"");
    }

    @Test
    void dispatchesPendingEventsOnDemandWhenDispatcherIsAvailable() {
        fraudOutboundEventDispatcherProvider = new TestObjectProvider(fraudOutboundEventDispatcher);
        fraudOutboundEventOperationsService = new FraudOutboundEventOperationsService(
            fraudOutboundEventRepository,
            fraudOutboundEventDispatcherProvider
        );
        when(fraudOutboundEventDispatcher.dispatchReadyEvents()).thenReturn(4);

        FraudOutboundDispatchResponse response = fraudOutboundEventOperationsService.dispatchNow();

        assertThat(response.processedCount()).isEqualTo(4);
        verify(fraudOutboundEventDispatcher).dispatchReadyEvents();
    }

    @SuppressWarnings("unchecked")
    private Specification<FraudOutboundEventEntity> anySpecification() {
        return any(Specification.class);
    }

    private static final class TestObjectProvider implements ObjectProvider<FraudOutboundEventDispatcher> {
        private final FraudOutboundEventDispatcher dispatcher;

        private TestObjectProvider(FraudOutboundEventDispatcher dispatcher) {
            this.dispatcher = dispatcher;
        }

        @Override
        public FraudOutboundEventDispatcher getObject(Object... args) {
            if (dispatcher == null) {
                throw new IllegalStateException("Dispatcher not configured.");
            }
            return dispatcher;
        }

        @Override
        public FraudOutboundEventDispatcher getIfAvailable() {
            return dispatcher;
        }

        @Override
        public FraudOutboundEventDispatcher getIfUnique() {
            return dispatcher;
        }

        @Override
        public FraudOutboundEventDispatcher getObject() {
            if (dispatcher == null) {
                throw new IllegalStateException("Dispatcher not configured.");
            }
            return dispatcher;
        }
    }

    private FraudOutboundEventEntity event(FraudOutboundEventStatus status, int attemptCount) {
        Instant createdAt = Instant.parse("2026-07-17T17:00:00Z");
        return new FraudOutboundEventEntity(
            UUID.randomUUID(),
            "PAYMENT_RELEASED",
            "fraud-notifications",
            "PAY-2201",
            "{\"paymentId\":\"PAY-2201\"}",
            status,
            attemptCount,
            createdAt,
            Instant.parse("2026-07-17T17:05:00Z"),
            status == FraudOutboundEventStatus.DELIVERED ? Instant.parse("2026-07-17T17:05:00Z") : null,
            "broker unavailable",
            null,
            null,
            null,
            createdAt,
            createdAt
        );
    }
}
