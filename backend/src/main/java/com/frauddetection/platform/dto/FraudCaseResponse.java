package com.frauddetection.platform.dto;

import com.frauddetection.platform.model.CaseResolutionOutcome;
import com.frauddetection.platform.model.ReviewCaseStatus;
import com.frauddetection.platform.model.RiskDecision;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record FraudCaseResponse(
    UUID caseId,
    UUID assessmentId,
    String paymentId,
    String customerId,
    int riskScore,
    RiskDecision decision,
    ReviewCaseStatus status,
    String summary,
    String currentAssignee,
    String resolutionSummary,
    CaseResolutionOutcome resolutionOutcome,
    Instant createdAt,
    Instant updatedAt,
    List<FraudCaseTimelineEntryResponse> timelineEntries
) {
}
