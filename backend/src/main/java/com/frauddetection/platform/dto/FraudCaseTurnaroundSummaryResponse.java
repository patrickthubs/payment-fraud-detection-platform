package com.frauddetection.platform.dto;

import java.math.BigDecimal;

public record FraudCaseTurnaroundSummaryResponse(
    long openCaseCount,
    long escalatedCaseCount,
    long resolvedCaseCount,
    BigDecimal averageResolutionHours,
    BigDecimal averageBacklogAgeHours,
    BigDecimal oldestBacklogAgeHours,
    BigDecimal resolvedWithin24HoursRate,
    FraudCaseSlaSummaryResponse sla
) {
}
