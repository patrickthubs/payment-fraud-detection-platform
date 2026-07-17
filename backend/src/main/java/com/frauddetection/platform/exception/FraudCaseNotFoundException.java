package com.frauddetection.platform.exception;

import java.util.UUID;

public class FraudCaseNotFoundException extends RuntimeException {

    public FraudCaseNotFoundException(UUID caseId) {
        super("Fraud case %s was not found.".formatted(caseId));
    }
}
