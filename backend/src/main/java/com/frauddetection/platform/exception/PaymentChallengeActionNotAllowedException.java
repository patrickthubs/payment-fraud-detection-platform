package com.frauddetection.platform.exception;

public class PaymentChallengeActionNotAllowedException extends RuntimeException {

    public PaymentChallengeActionNotAllowedException(String message) {
        super(message);
    }
}
