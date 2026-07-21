package com.frauddetection.platform.entity;

import com.frauddetection.platform.model.ChallengeOutcome;
import com.frauddetection.platform.model.PaymentStatus;
import com.frauddetection.platform.model.RiskDecision;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_records")
public class PaymentRecordEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "payment_id", nullable = false, unique = true, length = 100)
    private String paymentId;

    @Column(name = "customer_id", nullable = false, length = 100)
    private String customerId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @Column(name = "payment_channel", nullable = false, length = 60)
    private String paymentChannel;

    @Column(name = "merchant_category", nullable = false, length = 80)
    private String merchantCategory;

    @Column(name = "latest_assessment_id", nullable = false)
    private UUID latestAssessmentId;

    @Column(name = "latest_risk_score", nullable = false)
    private int latestRiskScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "latest_decision", nullable = false, length = 20)
    private RiskDecision latestDecision;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status", nullable = false, length = 20)
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "challenge_outcome", length = 20)
    private ChallengeOutcome challengeOutcome;

    @Column(name = "challenged_at")
    private Instant challengedAt;

    @Column(name = "challenge_completed_at")
    private Instant challengeCompletedAt;

    @Column(name = "challenge_completed_by", length = 100)
    private String challengeCompletedBy;

    @Column(name = "challenge_outcome_note", length = 500)
    private String challengeOutcomeNote;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "entity_version", nullable = false)
    private long entityVersion;

    protected PaymentRecordEntity() {
    }

    public PaymentRecordEntity(
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
        Instant updatedAt
    ) {
        this(
            id,
            UUID.fromString("f2000000-0000-0000-0000-000000000001"),
            paymentId,
            customerId,
            amount,
            currency,
            paymentChannel,
            merchantCategory,
            latestAssessmentId,
            latestRiskScore,
            latestDecision,
            paymentStatus,
            challengeOutcome,
            challengedAt,
            challengeCompletedAt,
            challengeCompletedBy,
            challengeOutcomeNote,
            createdAt,
            updatedAt
        );
    }

    public PaymentRecordEntity(
        UUID id,
        UUID organizationId,
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
        Instant updatedAt
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.paymentId = paymentId;
        this.customerId = customerId;
        this.amount = amount;
        this.currency = currency;
        this.paymentChannel = paymentChannel;
        this.merchantCategory = merchantCategory;
        this.latestAssessmentId = latestAssessmentId;
        this.latestRiskScore = latestRiskScore;
        this.latestDecision = latestDecision;
        this.paymentStatus = paymentStatus;
        this.challengeOutcome = challengeOutcome;
        this.challengedAt = challengedAt;
        this.challengeCompletedAt = challengeCompletedAt;
        this.challengeCompletedBy = challengeCompletedBy;
        this.challengeOutcomeNote = challengeOutcomeNote;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public String getCustomerId() {
        return customerId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getPaymentChannel() {
        return paymentChannel;
    }

    public String getMerchantCategory() {
        return merchantCategory;
    }

    public UUID getLatestAssessmentId() {
        return latestAssessmentId;
    }

    public int getLatestRiskScore() {
        return latestRiskScore;
    }

    public RiskDecision getLatestDecision() {
        return latestDecision;
    }

    public PaymentStatus getPaymentStatus() {
        return paymentStatus;
    }

    public ChallengeOutcome getChallengeOutcome() {
        return challengeOutcome;
    }

    public Instant getChallengedAt() {
        return challengedAt;
    }

    public Instant getChallengeCompletedAt() {
        return challengeCompletedAt;
    }

    public String getChallengeCompletedBy() {
        return challengeCompletedBy;
    }

    public String getChallengeOutcomeNote() {
        return challengeOutcomeNote;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public PaymentStatus applyAssessment(
        UUID assessmentId,
        BigDecimal amount,
        String currency,
        String paymentChannel,
        String merchantCategory,
        int latestRiskScore,
        RiskDecision latestDecision,
        PaymentStatus paymentStatus,
        Instant updatedAt
    ) {
        PaymentStatus previousStatus = this.paymentStatus;
        this.latestAssessmentId = assessmentId;
        this.amount = amount;
        this.currency = currency;
        this.paymentChannel = paymentChannel;
        this.merchantCategory = merchantCategory;
        this.latestRiskScore = latestRiskScore;
        this.latestDecision = latestDecision;
        this.paymentStatus = paymentStatus;
        if (paymentStatus == PaymentStatus.CHALLENGED) {
            this.challengeOutcome = null;
            this.challengedAt = updatedAt;
            this.challengeCompletedAt = null;
            this.challengeCompletedBy = null;
            this.challengeOutcomeNote = null;
        }
        this.updatedAt = updatedAt;
        return previousStatus;
    }

    public PaymentStatus transitionTo(PaymentStatus paymentStatus, Instant updatedAt) {
        PaymentStatus previousStatus = this.paymentStatus;
        this.paymentStatus = paymentStatus;
        this.updatedAt = updatedAt;
        return previousStatus;
    }

    public PaymentStatus completeChallenge(
        ChallengeOutcome challengeOutcome,
        String completedBy,
        String challengeOutcomeNote,
        PaymentStatus paymentStatus,
        Instant updatedAt
    ) {
        PaymentStatus previousStatus = this.paymentStatus;
        this.challengeOutcome = challengeOutcome;
        this.challengeCompletedBy = completedBy;
        this.challengeOutcomeNote = challengeOutcomeNote;
        this.challengeCompletedAt = updatedAt;
        this.paymentStatus = paymentStatus;
        this.updatedAt = updatedAt;
        return previousStatus;
    }
}
