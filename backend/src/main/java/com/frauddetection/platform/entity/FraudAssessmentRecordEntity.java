package com.frauddetection.platform.entity;

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
@Table(name = "fraud_assessment_records")
public class FraudAssessmentRecordEntity {

    @Id
    private UUID id;

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
    @Column(name = "velocity_source", nullable = false, length = 30)
    private VelocitySource velocitySource;

    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    @Column(name = "triggered_factor_codes", nullable = false, length = 500)
    private String triggeredFactorCodes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FraudAssessmentRecordEntity() {
    }

    public FraudAssessmentRecordEntity(
        UUID id,
        String paymentId,
        String customerId,
        int riskScore,
        RiskDecision decision,
        VelocitySource velocitySource,
        String summary,
        String triggeredFactorCodes,
        Instant createdAt
    ) {
        this.id = id;
        this.paymentId = paymentId;
        this.customerId = customerId;
        this.riskScore = riskScore;
        this.decision = decision;
        this.velocitySource = velocitySource;
        this.summary = summary;
        this.triggeredFactorCodes = triggeredFactorCodes;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
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

    public VelocitySource getVelocitySource() {
        return velocitySource;
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
