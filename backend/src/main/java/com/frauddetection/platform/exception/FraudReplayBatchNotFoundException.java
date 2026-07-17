package com.frauddetection.platform.exception;

import java.util.UUID;

public class FraudReplayBatchNotFoundException extends RuntimeException {

    public FraudReplayBatchNotFoundException(UUID batchId) {
        super("Fraud replay batch %s was not found.".formatted(batchId));
    }
}
