package com.frauddetection.platform.entity;

import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.RiskDecision;
import com.frauddetection.platform.model.VelocitySource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_replay_batch_items")
public class FraudReplayBatchItemEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "batch_id", nullable = false)
    private UUID batchId;

    @Column(name = "scenario_index", nullable = false)
    private int scenarioIndex;

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
    @Column(name = "projected_payment_status", nullable = false, length = 20)
    private PaymentStatus projectedPaymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "velocity_source", nullable = false, length = 30)
    private VelocitySource velocitySource;

    @Column(name = "review_case_would_be_created", nullable = false)
    private boolean reviewCaseWouldBeCreated;

    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    @Column(name = "triggered_factor_codes", nullable = false, length = 500)
    private String triggeredFactorCodes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FraudReplayBatchItemEntity() {
    }

    public FraudReplayBatchItemEntity(
        UUID id,
        UUID batchId,
        int scenarioIndex,
        String paymentId,
        String customerId,
        int riskScore,
        RiskDecision decision,
        PaymentStatus projectedPaymentStatus,
        VelocitySource velocitySource,
        boolean reviewCaseWouldBeCreated,
        String summary,
        String triggeredFactorCodes,
        Instant createdAt
    ) {
        this(
            id,
            UUID.fromString("f2000000-0000-0000-0000-000000000001"),
            batchId,
            scenarioIndex,
            paymentId,
            customerId,
            riskScore,
            decision,
            projectedPaymentStatus,
            velocitySource,
            reviewCaseWouldBeCreated,
            summary,
            triggeredFactorCodes,
            createdAt
        );
    }

    public FraudReplayBatchItemEntity(
        UUID id,
        UUID organizationId,
        UUID batchId,
        int scenarioIndex,
        String paymentId,
        String customerId,
        int riskScore,
        RiskDecision decision,
        PaymentStatus projectedPaymentStatus,
        VelocitySource velocitySource,
        boolean reviewCaseWouldBeCreated,
        String summary,
        String triggeredFactorCodes,
        Instant createdAt
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.batchId = batchId;
        this.scenarioIndex = scenarioIndex;
        this.paymentId = paymentId;
        this.customerId = customerId;
        this.riskScore = riskScore;
        this.decision = decision;
        this.projectedPaymentStatus = projectedPaymentStatus;
        this.velocitySource = velocitySource;
        this.reviewCaseWouldBeCreated = reviewCaseWouldBeCreated;
        this.summary = summary;
        this.triggeredFactorCodes = triggeredFactorCodes;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getBatchId() {
        return batchId;
    }

    public int getScenarioIndex() {
        return scenarioIndex;
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

    public PaymentStatus getProjectedPaymentStatus() {
        return projectedPaymentStatus;
    }

    public VelocitySource getVelocitySource() {
        return velocitySource;
    }

    public boolean isReviewCaseWouldBeCreated() {
        return reviewCaseWouldBeCreated;
    }

    public String getSummary() {
        return summary;
    }

    public String getTriggeredFactorCodes() {
        return triggeredFactorCodes;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
