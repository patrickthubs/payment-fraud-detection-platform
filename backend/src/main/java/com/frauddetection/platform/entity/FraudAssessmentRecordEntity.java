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

    @Column(name = "scoring_profile_id")
    private UUID scoringProfileId;

    @Column(name = "scoring_profile_version", nullable = false)
    private int scoringProfileVersion;

    @Column(name = "ruleset_version", nullable = false, length = 80)
    private String rulesetVersion;

    @Column(name = "input_snapshot", nullable = false, length = 12000)
    private String inputSnapshot;

    @Column(name = "factor_details", nullable = false, length = 12000)
    private String factorDetails;

    @Column(name = "idempotency_key", unique = true, length = 120)
    private String idempotencyKey;

    @Column(name = "request_hash", length = 64)
    private String requestHash;

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
        this(
            id, paymentId, customerId, riskScore, decision, velocitySource, summary, triggeredFactorCodes,
            null, 0, "rules-v1", "{}", "[]", null, null, createdAt
        );
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
        UUID scoringProfileId,
        int scoringProfileVersion,
        String rulesetVersion,
        String inputSnapshot,
        String factorDetails,
        String idempotencyKey,
        String requestHash,
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
        this.scoringProfileId = scoringProfileId;
        this.scoringProfileVersion = scoringProfileVersion;
        this.rulesetVersion = rulesetVersion;
        this.inputSnapshot = inputSnapshot;
        this.factorDetails = factorDetails;
        this.idempotencyKey = idempotencyKey;
        this.requestHash = requestHash;
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

    public UUID getScoringProfileId() {
        return scoringProfileId;
    }

    public int getScoringProfileVersion() {
        return scoringProfileVersion;
    }

    public String getRulesetVersion() {
        return rulesetVersion;
    }

    public String getInputSnapshot() {
        return inputSnapshot;
    }

    public String getFactorDetails() {
        return factorDetails;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public String getRequestHash() {
        return requestHash;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
