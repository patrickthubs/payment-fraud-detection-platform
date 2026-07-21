package com.frauddetection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.frauddetection.platform.entity.FraudOutboundEventEntity;
import com.frauddetection.platform.model.FraudOutboundEventStatus;
import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.repository.FraudOutboundEventRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FraudOutboundEventServiceTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString("f2000000-0000-0000-0000-000000000001");

    @Mock
    private FraudOutboundEventRepository fraudOutboundEventRepository;

    @Mock
    private CurrentTenantService currentTenantService;

    private FraudOutboundEventService fraudOutboundEventService;

    @BeforeEach
    void setUp() {
        when(currentTenantService.organizationId()).thenReturn(ORGANIZATION_ID);
        fraudOutboundEventService = new FraudOutboundEventService(
            fraudOutboundEventRepository,
            new ObjectMapper().findAndRegisterModules(),
            currentTenantService
        );
        when(fraudOutboundEventRepository.save(any(FraudOutboundEventEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void storesPendingOutboxEvent() {
        Instant createdAt = Instant.parse("2026-07-17T11:00:00Z");
        PaymentStatusChangedEvent event = new PaymentStatusChangedEvent(
            UUID.randomUUID(),
            "PAY-11",
            "CUST-11",
            PaymentStatus.APPROVED,
            PaymentStatus.HELD,
            RiskDecision.HOLD,
            "ASSESSMENT",
            "Held after scoring.",
            createdAt
        );

        fraudOutboundEventService.enqueue(
            "PaymentStatusChangedEvent",
            "payment-status-changed",
            "PAY-11",
            event,
            createdAt
        );

        ArgumentCaptor<FraudOutboundEventEntity> captor = ArgumentCaptor.forClass(FraudOutboundEventEntity.class);
        verify(fraudOutboundEventRepository).save(captor.capture());
        FraudOutboundEventEntity saved = captor.getValue();
        assertThat(saved.getEventType()).isEqualTo("PaymentStatusChangedEvent");
        assertThat(saved.getOrganizationId()).isEqualTo(ORGANIZATION_ID);
        assertThat(saved.getTopicName()).isEqualTo("payment-status-changed");
        assertThat(saved.getMessageKey()).isEqualTo("PAY-11");
        assertThat(saved.getStatus()).isEqualTo(FraudOutboundEventStatus.PENDING);
        assertThat(saved.getAttemptCount()).isZero();
        assertThat(saved.getNextAttemptAt()).isEqualTo(createdAt);
        assertThat(saved.getPayload()).contains("\"paymentId\":\"PAY-11\"");
    }
}
