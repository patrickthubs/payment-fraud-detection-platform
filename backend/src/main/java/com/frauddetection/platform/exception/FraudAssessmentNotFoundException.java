package com.frauddetection.platform.exception;

import java.util.UUID;

public class FraudAssessmentNotFoundException extends RuntimeException {
    public FraudAssessmentNotFoundException(UUID assessmentId) {
        super("Fraud assessment %s was not found.".formatted(assessmentId));
    }
}
