package com.frauddetection.platform.dto;

public record FraudWorkloadRecommendationResponse(
    String priority,
    String recommendationType,
    String summary,
    String sourceReviewer,
    String targetReviewer,
    long affectedCaseCount
) {
}
