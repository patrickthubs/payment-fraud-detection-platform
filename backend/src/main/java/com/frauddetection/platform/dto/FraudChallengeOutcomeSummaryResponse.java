package com.frauddetection.platform.dto;

import java.math.BigDecimal;
import java.util.List;

public record FraudChallengeOutcomeSummaryResponse(
    long challengedPayments,
    long pendingChallenges,
    long completedChallenges,
    long passedChallenges,
    long failedChallenges,
    long abandonedChallenges,
    BigDecimal completionRate,
    BigDecimal passRate,
    BigDecimal abandonmentRate,
    BigDecimal averageRiskScoreForPassedChallenges,
    BigDecimal averageRiskScoreForAbandonedChallenges,
    List<FraudMetricCountResponse> challengesByOutcome
) {
}
