package com.frauddetection.platform.exception;

public class FraudOutboundEventActionNotAllowedException extends RuntimeException {

    public FraudOutboundEventActionNotAllowedException(String message) {
        super(message);
    }
}
