package com.frauddetection.platform.dto;

public record FraudOutboundRetryBatchResponse(
    int requested,
    int queuedForRetry
) {
}
