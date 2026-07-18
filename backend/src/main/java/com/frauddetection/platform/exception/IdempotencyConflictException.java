package com.frauddetection.platform.exception;

public class IdempotencyConflictException extends RuntimeException {

    public IdempotencyConflictException(String key) {
        super("Idempotency key '%s' was already used for a different assessment request.".formatted(key));
    }
}
