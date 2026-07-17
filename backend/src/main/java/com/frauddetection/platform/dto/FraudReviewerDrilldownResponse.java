package com.frauddetection.platform.dto;

import java.math.BigDecimal;

public record FraudReviewerDrilldownResponse(
    String reviewer,
    long assignedBacklogCount,
    long assignedOpenCases,
    long assignedEscalatedCases,
    long assignedResolvedCases,
    long breachedBacklogCount,
    BigDecimal averageAssignedBacklogAgeHours,
    BigDecimal oldestAssignedBacklogAgeHours,
    BigDecimal backlogShareRate
) {
}
