package com.frauddetection.platform.dto;

import java.math.BigDecimal;
import java.util.List;

public record FraudOperationsSummaryResponse(
    long totalAssessments,
    long totalTrackedPayments,
    long totalReviewCases,
    long distinctCustomersAssessed,
    BigDecimal averageRiskScore,
    long reviewBacklogCount,
    FraudChallengeOutcomeSummaryResponse challengeOutcomeSummary,
    FraudReviewerAnalyticsSummaryResponse reviewerAnalytics,
    FraudOutboundDeliverySummaryResponse outboundDeliverySummary,
    List<FraudMetricCountResponse> assessmentsByDecision,
    List<FraudMetricCountResponse> assessmentsByVelocitySource,
    List<FraudMetricCountResponse> paymentsByStatus,
    List<FraudMetricCountResponse> casesByStatus,
    List<FraudMetricCountResponse> resolutionsByOutcome
) {
}
