package com.frauddetection.platform.service;

import com.frauddetection.platform.model.ReviewCaseStatus;
import java.time.Instant;

public record FraudCaseFilterCriteria(
    ReviewCaseStatus status,
    String assignee,
    Integer minRiskScore,
    Integer maxRiskScore,
    Boolean breachedOnly,
    Boolean unassignedOnly,
    String paymentId,
    String customerId,
    Instant createdFrom,
    Instant createdTo
) {
}
