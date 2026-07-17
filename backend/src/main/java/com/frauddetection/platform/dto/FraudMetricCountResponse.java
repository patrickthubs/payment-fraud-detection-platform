package com.frauddetection.platform.dto;

public record FraudMetricCountResponse(
    String label,
    long total
) {
}
