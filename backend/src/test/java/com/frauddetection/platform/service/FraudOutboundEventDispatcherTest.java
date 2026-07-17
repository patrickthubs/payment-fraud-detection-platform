package com.frauddetection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.platform.config.FraudOutboxProperties;
import com.frauddetection.platform.entity.FraudOutboundEventEntity;
import com.frauddetection.platform.model.FraudOutboundEventStatus;
import com.frauddetection.platform.repository.FraudOutboundEventRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

@ExtendWith(MockitoExtension.class)
class FraudOutboundEventDispatcherTest {

    @Mock
    private FraudOutboundEventRepository fraudOutboundEventRepository;

    @Mock
    private KafkaTemplate<String, Object> kafkaTemplate;

    private FraudOutboundEventDispatcher fraudOutboundEventDispatcher;

    @BeforeEach
    void setUp() {
        fraudOutboundEventDispatcher = new FraudOutboundEventDispatcher(
            fraudOutboundEventRepository,
            kafkaTemplate,
            new FraudOutboxProperties(true, 50, 3, 15000, 30000),
            new ObjectMapper().findAndRegisterModules()
        );
    }

    @Test
    void marksEventDeliveredWhenKafkaSendSucceeds() {
        FraudOutboundEventEntity event = buildEvent(0);
        when(fraudOutboundEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            any(),
            any(),
            any(Pageable.class)
        )).thenReturn(List.of(event));
        when(kafkaTemplate.send(eq("payment-status-changed"), eq("PAY-22"), any()))
            .thenReturn(CompletableFuture.completedFuture((SendResult<String, Object>) null));

        int processed = fraudOutboundEventDispatcher.dispatchReadyEvents();

        assertThat(processed).isEqualTo(1);
        assertThat(event.getStatus()).isEqualTo(FraudOutboundEventStatus.DELIVERED);
        assertThat(event.getAttemptCount()).isEqualTo(1);
    }

    @Test
    void marksEventFailedAfterFinalAttempt() {
        FraudOutboundEventEntity event = buildEvent(2);
        when(fraudOutboundEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByCreatedAtAsc(
            any(),
            any(),
            any(Pageable.class)
        )).thenReturn(List.of(event));
        CompletableFuture<SendResult<String, Object>> failedFuture = new CompletableFuture<>();
        failedFuture.completeExceptionally(new IllegalStateException("broker unavailable"));
        when(kafkaTemplate.send(eq("payment-status-changed"), eq("PAY-22"), any()))
            .thenReturn(failedFuture);

        fraudOutboundEventDispatcher.dispatchReadyEvents();

        assertThat(event.getStatus()).isEqualTo(FraudOutboundEventStatus.FAILED);
        assertThat(event.getAttemptCount()).isEqualTo(3);
        assertThat(event.getLastError()).contains("broker unavailable");
        verify(kafkaTemplate).send(eq("payment-status-changed"), eq("PAY-22"), any());
    }

    private FraudOutboundEventEntity buildEvent(int attemptCount) {
        Instant createdAt = Instant.parse("2026-07-17T11:00:00Z");
        return new FraudOutboundEventEntity(
            UUID.randomUUID(),
            "PaymentStatusChangedEvent",
            "payment-status-changed",
            "PAY-22",
            "{\"paymentId\":\"PAY-22\"}",
            FraudOutboundEventStatus.PENDING,
            attemptCount,
            createdAt,
            null,
            null,
            null,
            null,
            null,
            null,
            createdAt,
            createdAt
        );
    }
}
