package com.frauddetection.platform.service;

import com.frauddetection.platform.config.FraudEventProperties;
import org.springframework.stereotype.Service;

@Service
public class PaymentOrchestrationEventPublisher {

    private final FraudOutboundEventService fraudOutboundEventService;
    private final FraudEventProperties fraudEventProperties;

    public PaymentOrchestrationEventPublisher(
        FraudOutboundEventService fraudOutboundEventService,
        FraudEventProperties fraudEventProperties
    ) {
        this.fraudOutboundEventService = fraudOutboundEventService;
        this.fraudEventProperties = fraudEventProperties;
    }

    public void publish(PaymentStatusChangedEvent event) {
        fraudOutboundEventService.enqueue(
            PaymentStatusChangedEvent.class.getSimpleName(),
            fraudEventProperties.paymentStatusTopic(),
            event.paymentId(),
            event,
            event.changedAt()
        );
    }
}
