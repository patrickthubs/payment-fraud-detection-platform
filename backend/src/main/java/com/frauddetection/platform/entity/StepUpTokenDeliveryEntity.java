package com.frauddetection.platform.entity;

import com.frauddetection.platform.model.StepUpDeliveryChannel;
import com.frauddetection.platform.model.StepUpDeliveryStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "step_up_token_deliveries")
public class StepUpTokenDeliveryEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "operator_id", nullable = false)
    private UUID operatorId;

    @Column(name = "operator_username", nullable = false, length = 120)
    private String operatorUsername;

    @Column(name = "delivery_channel", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private StepUpDeliveryChannel deliveryChannel;

    @Column(name = "delivery_destination", nullable = false, length = 255)
    private String deliveryDestination;

    @Column(name = "destination_masked", nullable = false, length = 255)
    private String destinationMasked;

    @Column(name = "status", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private StepUpDeliveryStatus status;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "resend_sequence", nullable = false)
    private int resendSequence;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "last_attempted_at")
    private Instant lastAttemptedAt;

    @Column(name = "delivered_at")
    private Instant deliveredAt;

    @Column(name = "consumed_at")
    private Instant consumedAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    protected StepUpTokenDeliveryEntity() {
    }

    public StepUpTokenDeliveryEntity(
        UUID id,
        UUID organizationId,
        UUID operatorId,
        String operatorUsername,
        StepUpDeliveryChannel deliveryChannel,
        String deliveryDestination,
        String destinationMasked,
        StepUpDeliveryStatus status,
        String tokenHash,
        int resendSequence,
        int attemptCount,
        String failureReason,
        Instant createdAt,
        Instant expiresAt,
        Instant lastAttemptedAt,
        Instant deliveredAt,
        Instant consumedAt,
        Instant revokedAt
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.operatorId = operatorId;
        this.operatorUsername = operatorUsername;
        this.deliveryChannel = deliveryChannel;
        this.deliveryDestination = deliveryDestination;
        this.destinationMasked = destinationMasked;
        this.status = status;
        this.tokenHash = tokenHash;
        this.resendSequence = resendSequence;
        this.attemptCount = attemptCount;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
        this.lastAttemptedAt = lastAttemptedAt;
        this.deliveredAt = deliveredAt;
        this.consumedAt = consumedAt;
        this.revokedAt = revokedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getOperatorId() {
        return operatorId;
    }

    public String getOperatorUsername() {
        return operatorUsername;
    }

    public StepUpDeliveryChannel getDeliveryChannel() {
        return deliveryChannel;
    }

    public String getDeliveryDestination() {
        return deliveryDestination;
    }

    public String getDestinationMasked() {
        return destinationMasked;
    }

    public StepUpDeliveryStatus getStatus() {
        return status;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public int getResendSequence() {
        return resendSequence;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getLastAttemptedAt() {
        return lastAttemptedAt;
    }

    public Instant getDeliveredAt() {
        return deliveredAt;
    }

    public Instant getConsumedAt() {
        return consumedAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public boolean isOpen() {
        return status == StepUpDeliveryStatus.PENDING || status == StepUpDeliveryStatus.SENT;
    }

    public void markSent(Instant attemptedAt) {
        this.status = StepUpDeliveryStatus.SENT;
        this.lastAttemptedAt = attemptedAt;
        this.deliveredAt = attemptedAt;
        this.failureReason = null;
    }

    public void markFailed(String reason, Instant attemptedAt) {
        this.status = StepUpDeliveryStatus.FAILED;
        this.lastAttemptedAt = attemptedAt;
        this.failureReason = reason;
    }

    public void markConsumed(Instant consumedAt) {
        this.status = StepUpDeliveryStatus.CONSUMED;
        this.consumedAt = consumedAt;
    }

    public void markRevoked(Instant revokedAt, String reason) {
        this.status = StepUpDeliveryStatus.REVOKED;
        this.revokedAt = revokedAt;
        this.failureReason = reason;
    }

    public void markExpired(String reason) {
        this.status = StepUpDeliveryStatus.EXPIRED;
        this.failureReason = reason;
    }
}
