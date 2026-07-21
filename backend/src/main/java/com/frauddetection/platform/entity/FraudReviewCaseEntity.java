package com.frauddetection.platform.entity;

import com.frauddetection.platform.model.CaseResolutionOutcome;
import com.frauddetection.platform.model.ReviewCaseStatus;
import com.frauddetection.platform.model.RiskDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_review_cases")
public class FraudReviewCaseEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "assessment_id", nullable = false, unique = true)
    private UUID assessmentId;

    @Column(name = "payment_id", nullable = false, length = 100)
    private String paymentId;

    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;

    @Column(name = "risk_score", nullable = false)
    private int riskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 20)
    private RiskDecision decision;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_status", nullable = false, length = 20)
    private ReviewCaseStatus status;

    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    @Column(name = "current_assignee", length = 120)
    private String currentAssignee;

    @Column(name = "resolution_summary", length = 500)
    private String resolutionSummary;

    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_outcome", length = 30)
    private CaseResolutionOutcome resolutionOutcome;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "entity_version", nullable = false)
    private long entityVersion;

    protected FraudReviewCaseEntity() {
    }

    public FraudReviewCaseEntity(
        UUID id,
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
        Instant updatedAt
    ) {
        this(
            id,
            UUID.fromString("f2000000-0000-0000-0000-000000000001"),
            assessmentId,
            paymentId,
            customerId,
            riskScore,
            decision,
            status,
            summary,
            currentAssignee,
            resolutionSummary,
            resolutionOutcome,
            createdAt,
            updatedAt
        );
    }

    public FraudReviewCaseEntity(
        UUID id,
        UUID organizationId,
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
        Instant updatedAt
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.assessmentId = assessmentId;
        this.paymentId = paymentId;
        this.customerId = customerId;
        this.riskScore = riskScore;
        this.decision = decision;
        this.status = status;
        this.summary = summary;
        this.currentAssignee = currentAssignee;
        this.resolutionSummary = resolutionSummary;
        this.resolutionOutcome = resolutionOutcome;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public int getRiskScore() {
        return riskScore;
    }

    public RiskDecision getDecision() {
        return decision;
    }

    public ReviewCaseStatus getStatus() {
        return status;
    }

    public String getSummary() {
        return summary;
    }

    public String getCurrentAssignee() {
        return currentAssignee;
    }

    public String getResolutionSummary() {
        return resolutionSummary;
    }

    public CaseResolutionOutcome getResolutionOutcome() {
        return resolutionOutcome;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void assign(String assignee, Instant updatedAt) {
        this.currentAssignee = assignee;
        this.updatedAt = updatedAt;
    }

    public void escalate(Instant updatedAt) {
        this.status = ReviewCaseStatus.ESCALATED;
        this.updatedAt = updatedAt;
    }

    public void resolve(String resolutionSummary, CaseResolutionOutcome resolutionOutcome, Instant updatedAt) {
        this.status = ReviewCaseStatus.RESOLVED;
        this.resolutionSummary = resolutionSummary;
        this.resolutionOutcome = resolutionOutcome;
        this.updatedAt = updatedAt;
    }
}
