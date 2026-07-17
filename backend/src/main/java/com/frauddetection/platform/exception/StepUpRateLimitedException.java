package com.frauddetection.platform.exception;

public class StepUpRateLimitedException extends RuntimeException {

    public StepUpRateLimitedException(String message) {
        super(message);
    }
}
