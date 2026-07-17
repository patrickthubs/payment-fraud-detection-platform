package com.frauddetection.platform.dto;

import com.frauddetection.platform.model.RiskFactorCode;

public record RiskFactorView(
    RiskFactorCode code,
    int weight,
    String detail
) {
}
