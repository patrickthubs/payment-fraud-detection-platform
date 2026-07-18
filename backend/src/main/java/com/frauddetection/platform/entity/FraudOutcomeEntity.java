package com.frauddetection.platform.entity;

import com.frauddetection.platform.model.FraudOutcomeLabel;
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
@Table(name = "fraud_outcomes")
public class FraudOutcomeEntity {

    @Id
    private UUID id;

    @Column(name = "assessment_id", nullable = false, unique = true)
    private UUID assessmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "outcome_label", nullable = false, length = 40)
    private FraudOutcomeLabel outcomeLabel;

    @Column(name = "source", nullable = false, length = 60)
    private String source;

    @Column(name = "actual_loss", nullable = false, precision = 19, scale = 2)
    private BigDecimal actualLoss;

    @Column(name = "recovered_amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal recoveredAmount;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "labelled_by", nullable = false, length = 120)
    private String labelledBy;

    @Column(name = "occurred_at")
    private Instant occurredAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "entity_version", nullable = false)
    private long entityVersion;

    protected FraudOutcomeEntity() {
    }

    public FraudOutcomeEntity(
        UUID id,
        UUID assessmentId,
        FraudOutcomeLabel outcomeLabel,
        String source,
        BigDecimal actualLoss,
        BigDecimal recoveredAmount,
        String notes,
        String labelledBy,
        Instant occurredAt,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.assessmentId = assessmentId;
        update(outcomeLabel, source, actualLoss, recoveredAmount, notes, labelledBy, occurredAt, updatedAt);
        this.createdAt = createdAt;
    }

    public void update(
        FraudOutcomeLabel outcomeLabel,
        String source,
        BigDecimal actualLoss,
        BigDecimal recoveredAmount,
        String notes,
        String labelledBy,
        Instant occurredAt,
        Instant updatedAt
    ) {
        this.outcomeLabel = outcomeLabel;
        this.source = source;
        this.actualLoss = actualLoss;
        this.recoveredAmount = recoveredAmount;
        this.notes = notes;
        this.labelledBy = labelledBy;
        this.occurredAt = occurredAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() { return id; }
    public UUID getAssessmentId() { return assessmentId; }
    public FraudOutcomeLabel getOutcomeLabel() { return outcomeLabel; }
    public String getSource() { return source; }
    public BigDecimal getActualLoss() { return actualLoss; }
    public BigDecimal getRecoveredAmount() { return recoveredAmount; }
    public String getNotes() { return notes; }
    public String getLabelledBy() { return labelledBy; }
    public Instant getOccurredAt() { return occurredAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
