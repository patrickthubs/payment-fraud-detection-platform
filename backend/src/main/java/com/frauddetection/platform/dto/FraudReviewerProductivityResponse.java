package com.frauddetection.platform.dto;

import java.math.BigDecimal;

public record FraudReviewerProductivityResponse(
    String reviewer,
    long assignedOpenCases,
    long assignedEscalatedCases,
    long resolvedCases,
    long releasedPayments,
    long confirmedDeclines,
    BigDecimal averageResolutionHours,
    long assignmentActions,
    long escalationActions,
    long noteActions,
    long resolutionActions
) {
}
