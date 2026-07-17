package com.frauddetection.platform.dto;

import java.time.Instant;

public record StepUpRevokeResponse(
    int revokedCount,
    Instant revokedAt
) {
}
