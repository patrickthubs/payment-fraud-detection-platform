package com.frauddetection.platform.service;

import com.frauddetection.platform.model.VelocitySource;
import java.math.BigDecimal;

public record VelocitySnapshot(
    int transactionCountLastFiveMinutes,
    BigDecimal spendLastHour,
    VelocitySource source
) {
}
