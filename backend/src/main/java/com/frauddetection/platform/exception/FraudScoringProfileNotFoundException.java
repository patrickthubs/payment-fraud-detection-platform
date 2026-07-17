package com.frauddetection.platform.exception;

import java.util.UUID;

public class FraudScoringProfileNotFoundException extends RuntimeException {

    public FraudScoringProfileNotFoundException(UUID profileId) {
        super("Fraud scoring profile %s was not found.".formatted(profileId));
    }
}
