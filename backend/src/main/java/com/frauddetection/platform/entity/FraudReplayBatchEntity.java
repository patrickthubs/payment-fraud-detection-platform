package com.frauddetection.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_replay_batches")
public class FraudReplayBatchEntity {

    @Id
    private UUID id;

    @Column(name = "batch_name", nullable = false, length = 160)
    private String batchName;

    @Column(name = "scenario_count", nullable = false)
    private int scenarioCount;

    @Column(name = "challenge_threshold", nullable = false)
    private int challengeThreshold;

    @Column(name = "hold_threshold", nullable = false)
    private int holdThreshold;

    @Column(name = "decline_threshold", nullable = false)
    private int declineThreshold;

    @Column(name = "created_by", nullable = false, length = 120)
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected FraudReplayBatchEntity() {
    }

    public FraudReplayBatchEntity(
        UUID id,
        String batchName,
        int scenarioCount,
        int challengeThreshold,
        int holdThreshold,
        int declineThreshold,
        String createdBy,
        Instant createdAt
    ) {
        this.id = id;
        this.batchName = batchName;
        this.scenarioCount = scenarioCount;
        this.challengeThreshold = challengeThreshold;
        this.holdThreshold = holdThreshold;
        this.declineThreshold = declineThreshold;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getBatchName() {
        return batchName;
    }

    public int getScenarioCount() {
        return scenarioCount;
    }

    public int getChallengeThreshold() {
        return challengeThreshold;
    }

    public int getHoldThreshold() {
        return holdThreshold;
    }

    public int getDeclineThreshold() {
        return declineThreshold;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
