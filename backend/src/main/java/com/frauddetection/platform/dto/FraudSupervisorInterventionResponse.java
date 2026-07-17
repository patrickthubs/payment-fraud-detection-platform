package com.frauddetection.platform.dto;

public record FraudSupervisorInterventionResponse(
    String severity,
    String interventionType,
    String summary,
    String reviewer,
    long affectedCaseCount
) {
}
