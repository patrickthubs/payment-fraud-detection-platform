package com.frauddetection.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_scoring_profiles")
public class FraudScoringProfileEntity {

    @Id
    private UUID id;

    @Column(name = "version_number", nullable = false, unique = true)
    private int versionNumber;

    @Column(name = "profile_name", nullable = false, unique = true, length = 120)
    private String profileName;

    @Column(name = "challenge_threshold", nullable = false)
    private int challengeThreshold;

    @Column(name = "hold_threshold", nullable = false)
    private int holdThreshold;

    @Column(name = "decline_threshold", nullable = false)
    private int declineThreshold;

    @Column(name = "change_summary", nullable = false, length = 500)
    private String changeSummary;

    @Column(name = "created_by", nullable = false, length = 120)
    private String createdBy;

    @Column(name = "activated_by", length = 120)
    private String activatedBy;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "activated_at")
    private Instant activatedAt;

    protected FraudScoringProfileEntity() {
    }

    public FraudScoringProfileEntity(
        UUID id,
        int versionNumber,
        String profileName,
        int challengeThreshold,
        int holdThreshold,
        int declineThreshold,
        String changeSummary,
        String createdBy,
        String activatedBy,
        boolean active,
        Instant createdAt,
        Instant updatedAt,
        Instant activatedAt
    ) {
        this.id = id;
        this.versionNumber = versionNumber;
        this.profileName = profileName;
        this.challengeThreshold = challengeThreshold;
        this.holdThreshold = holdThreshold;
        this.declineThreshold = declineThreshold;
        this.changeSummary = changeSummary;
        this.createdBy = createdBy;
        this.activatedBy = activatedBy;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.activatedAt = activatedAt;
    }

    public UUID getId() {
        return id;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public String getProfileName() {
        return profileName;
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

    public String getChangeSummary() {
        return changeSummary;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public String getActivatedBy() {
        return activatedBy;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getActivatedAt() {
        return activatedAt;
    }

    public void activate(Instant activatedAt, String activatedBy) {
        this.active = true;
        this.activatedAt = activatedAt;
        this.activatedBy = truncate(activatedBy, 120);
        this.updatedAt = activatedAt;
    }

    public void deactivate(Instant updatedAt) {
        this.active = false;
        this.updatedAt = updatedAt;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
