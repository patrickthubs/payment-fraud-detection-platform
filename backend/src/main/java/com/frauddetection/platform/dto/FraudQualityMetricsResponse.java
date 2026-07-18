package com.frauddetection.platform.dto;

import java.math.BigDecimal;

public record FraudQualityMetricsResponse(
    long totalLabelled,
    long conclusiveLabels,
    long truePositives,
    long falsePositives,
    long falseNegatives,
    long trueNegatives,
    BigDecimal precision,
    BigDecimal recall,
    BigDecimal falsePositiveRate,
    BigDecimal actualLoss,
    BigDecimal recoveredAmount,
    BigDecimal netLoss
) {
}
