package com.frauddetection.platform.exception;

import java.util.UUID;

public class FraudOutboundEventNotFoundException extends RuntimeException {

    public FraudOutboundEventNotFoundException(UUID eventId) {
        super("Fraud outbound event %s was not found.".formatted(eventId));
    }
}
