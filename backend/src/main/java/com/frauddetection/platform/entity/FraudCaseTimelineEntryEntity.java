package com.frauddetection.platform.entity;

import com.frauddetection.platform.model.FraudCaseActionType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_case_timeline_entries")
public class FraudCaseTimelineEntryEntity {

    @Id
    private UUID id;

    @Column(name = "case_id", nullable = false)
    private UUID caseId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 30)
    private FraudCaseActionType actionType;

    @Column(name = "actor", nullable = false, length = 120)
    private String actor;

    @Column(name = "detail", nullable = false, length = 500)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FraudCaseTimelineEntryEntity() {
    }

    public FraudCaseTimelineEntryEntity(
        UUID id,
        UUID caseId,
        FraudCaseActionType actionType,
        String actor,
        String detail,
        Instant createdAt
    ) {
        this.id = id;
        this.caseId = caseId;
        this.actionType = actionType;
        this.actor = actor;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCaseId() {
        return caseId;
    }

    public FraudCaseActionType getActionType() {
        return actionType;
    }

    public String getActor() {
        return actor;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
