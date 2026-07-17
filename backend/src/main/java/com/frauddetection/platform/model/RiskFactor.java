package com.frauddetection.platform.model;

public record RiskFactor(
    RiskFactorCode code,
    int weight,
    String detail
) {
}
