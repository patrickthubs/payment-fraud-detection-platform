package com.frauddetection.platform.exception;

import java.util.UUID;

public class FraudCaseActionNotAllowedException extends RuntimeException {

    public FraudCaseActionNotAllowedException(UUID caseId, String action) {
        super("Fraud case %s cannot be %s in its current state.".formatted(caseId, action));
    }
}
