package com.frauddetection.platform.exception;

public class PaymentNotFoundException extends RuntimeException {

    public PaymentNotFoundException(String paymentId) {
        super("Payment '%s' was not found.".formatted(paymentId));
    }
}
