package com.frauddetection.platform.dto;

import java.math.BigDecimal;

public record FraudCasePriorityRoutingHintResponse(
    String priorityBand,
    String recommendedQueue,
    String rationale,
    long affectedCaseCount,
    long breachedCaseCount,
    long unassignedCaseCount,
    long escalatedCaseCount,
    BigDecimal averageRiskScore
) {
}
