package com.frauddetection.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "step_up_operator_security_state")
public class StepUpOperatorSecurityStateEntity {

    @Id
    @Column(name = "operator_id", nullable = false)
    private UUID operatorId;

    @Column(name = "operator_username", nullable = false, unique = true, length = 120)
    private String operatorUsername;

    @Column(name = "issue_window_started_at")
    private Instant issueWindowStartedAt;

    @Column(name = "issue_attempt_count", nullable = false)
    private int issueAttemptCount;

    @Column(name = "verify_window_started_at")
    private Instant verifyWindowStartedAt;

    @Column(name = "verify_failure_count", nullable = false)
    private int verifyFailureCount;

    @Column(name = "locked_until")
    private Instant lockedUntil;

    @Column(name = "last_failure_reason", length = 255)
    private String lastFailureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected StepUpOperatorSecurityStateEntity() {
    }

    public StepUpOperatorSecurityStateEntity(
        UUID operatorId,
        String operatorUsername,
        Instant issueWindowStartedAt,
        int issueAttemptCount,
        Instant verifyWindowStartedAt,
        int verifyFailureCount,
        Instant lockedUntil,
        String lastFailureReason,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.operatorId = operatorId;
        this.operatorUsername = operatorUsername;
        this.issueWindowStartedAt = issueWindowStartedAt;
        this.issueAttemptCount = issueAttemptCount;
        this.verifyWindowStartedAt = verifyWindowStartedAt;
        this.verifyFailureCount = verifyFailureCount;
        this.lockedUntil = lockedUntil;
        this.lastFailureReason = lastFailureReason;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public boolean isLockedAt(Instant now) {
        return lockedUntil != null && lockedUntil.isAfter(now);
    }

    public void registerIssueAttempt(Instant now, Duration issueWindow) {
        if (issueWindowStartedAt == null || issueWindowStartedAt.plus(issueWindow).isBefore(now)) {
            issueWindowStartedAt = now;
            issueAttemptCount = 0;
        }
        issueAttemptCount++;
        updatedAt = now;
    }

    public void registerVerificationFailure(Instant now, Duration verifyWindow, String reason) {
        if (verifyWindowStartedAt == null || verifyWindowStartedAt.plus(verifyWindow).isBefore(now)) {
            verifyWindowStartedAt = now;
            verifyFailureCount = 0;
        }
        verifyFailureCount++;
        lastFailureReason = reason;
        updatedAt = now;
    }

    public void lockUntil(Instant now, Duration lockoutDuration, String reason) {
        lockedUntil = now.plus(lockoutDuration);
        lastFailureReason = reason;
        updatedAt = now;
    }

    public void clearVerificationFailures(Instant now) {
        verifyWindowStartedAt = null;
        verifyFailureCount = 0;
        lastFailureReason = null;
        lockedUntil = null;
        updatedAt = now;
    }

    public UUID getOperatorId() {
        return operatorId;
    }

    public String getOperatorUsername() {
        return operatorUsername;
    }

    public Instant getIssueWindowStartedAt() {
        return issueWindowStartedAt;
    }

    public int getIssueAttemptCount() {
        return issueAttemptCount;
    }

    public Instant getVerifyWindowStartedAt() {
        return verifyWindowStartedAt;
    }

    public int getVerifyFailureCount() {
        return verifyFailureCount;
    }

    public Instant getLockedUntil() {
        return lockedUntil;
    }

    public String getLastFailureReason() {
        return lastFailureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
