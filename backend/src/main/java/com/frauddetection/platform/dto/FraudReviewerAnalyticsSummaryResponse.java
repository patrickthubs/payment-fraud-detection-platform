package com.frauddetection.platform.dto;

import java.util.List;

public record FraudReviewerAnalyticsSummaryResponse(
    FraudCaseTurnaroundSummaryResponse turnaround,
    List<FraudReviewerProductivityResponse> reviewers,
    List<FraudReviewerDrilldownResponse> drilldown,
    List<FraudWorkloadRecommendationResponse> workloadRecommendations,
    List<FraudCasePriorityRoutingHintResponse> priorityRoutingHints,
    List<FraudSupervisorInterventionResponse> supervisorInterventions
) {
}
