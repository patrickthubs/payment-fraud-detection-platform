package com.frauddetection.platform.dto;

import com.frauddetection.platform.model.ChallengeOutcome;
import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.RiskDecision;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PaymentStatusResponse(
    UUID id,
    String paymentId,
    String customerId,
    BigDecimal amount,
    String currency,
    String paymentChannel,
    String merchantCategory,
    UUID latestAssessmentId,
    int latestRiskScore,
    RiskDecision latestDecision,
    PaymentStatus paymentStatus,
    ChallengeOutcome challengeOutcome,
    Instant challengedAt,
    Instant challengeCompletedAt,
    String challengeCompletedBy,
    String challengeOutcomeNote,
    Instant createdAt,
    Instant updatedAt,
    List<PaymentTransitionResponse> transitions
) {
}
