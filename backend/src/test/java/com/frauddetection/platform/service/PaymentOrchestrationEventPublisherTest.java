package com.frauddetection.platform.service;

import static org.mockito.Mockito.verify;

import com.frauddetection.platform.config.FraudEventProperties;
import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.RiskDecision;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentOrchestrationEventPublisherTest {

    @Mock
    private FraudOutboundEventService fraudOutboundEventService;

    private PaymentOrchestrationEventPublisher paymentOrchestrationEventPublisher;

    @BeforeEach
    void setUp() {
        paymentOrchestrationEventPublisher = new PaymentOrchestrationEventPublisher(
            fraudOutboundEventService,
            new FraudEventProperties("fraud-assessment-completed", "payment-status-changed", "fraud-notifications")
        );
    }

    @Test
    void publishesPaymentStatusChangedEvent() {
        PaymentStatusChangedEvent event = new PaymentStatusChangedEvent(
            UUID.randomUUID(),
            "PAY-1",
            "CUST-1",
            PaymentStatus.HELD,
            PaymentStatus.APPROVED,
            RiskDecision.HOLD,
            "CASE_RESOLUTION",
            "Released by analyst.",
            Instant.parse("2026-07-17T10:00:00Z")
        );

        paymentOrchestrationEventPublisher.publish(event);

        verify(fraudOutboundEventService).enqueue(
            "PaymentStatusChangedEvent",
            "payment-status-changed",
            "PAY-1",
            event,
            Instant.parse("2026-07-17T10:00:00Z")
        );
    }
}
