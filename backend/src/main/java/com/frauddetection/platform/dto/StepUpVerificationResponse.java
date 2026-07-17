package com.frauddetection.platform.dto;

import java.time.Instant;

public record StepUpVerificationResponse(
    String operator,
    Instant verifiedAt,
    Instant validUntil
) {
}
