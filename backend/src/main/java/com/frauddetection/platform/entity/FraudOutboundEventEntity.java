package com.frauddetection.platform.entity;

import com.frauddetection.platform.model.FraudOutboundEventStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_outbound_events")
public class FraudOutboundEventEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "topic_name", nullable = false, length = 200)
    private String topicName;

    @Column(name = "message_key", nullable = false, length = 120)
    private String messageKey;

    @Column(name = "payload", nullable = false, length = 12000)
    private String payload;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private FraudOutboundEventStatus status;

    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_attempted_at")
    private Instant lastAttemptedAt;

    @Column(name = "published_at")
    private Instant publishedAt;

    @Column(name = "last_error", length = 1000)
    private String lastError;

    @Column(name = "operator_note", length = 2000)
    private String operatorNote;

    @Column(name = "noted_by", length = 120)
    private String notedBy;

    @Column(name = "noted_at")
    private Instant notedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FraudOutboundEventEntity() {
    }

    public FraudOutboundEventEntity(
        UUID id,
        String eventType,
        String topicName,
        String messageKey,
        String payload,
        FraudOutboundEventStatus status,
        int attemptCount,
        Instant nextAttemptAt,
        Instant lastAttemptedAt,
        Instant publishedAt,
        String lastError,
        String operatorNote,
        String notedBy,
        Instant notedAt,
        Instant createdAt,
        Instant updatedAt
    ) {
        this(
            id,
            UUID.fromString("f2000000-0000-0000-0000-000000000001"),
            eventType,
            topicName,
            messageKey,
            payload,
            status,
            attemptCount,
            nextAttemptAt,
            lastAttemptedAt,
            publishedAt,
            lastError,
            operatorNote,
            notedBy,
            notedAt,
            createdAt,
            updatedAt
        );
    }

    public FraudOutboundEventEntity(
        UUID id,
        UUID organizationId,
        String eventType,
        String topicName,
        String messageKey,
        String payload,
        FraudOutboundEventStatus status,
        int attemptCount,
        Instant nextAttemptAt,
        Instant lastAttemptedAt,
        Instant publishedAt,
        String lastError,
        String operatorNote,
        String notedBy,
        Instant notedAt,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.organizationId = organizationId;
        this.eventType = eventType;
        this.topicName = topicName;
        this.messageKey = messageKey;
        this.payload = payload;
        this.status = status;
        this.attemptCount = attemptCount;
        this.nextAttemptAt = nextAttemptAt;
        this.lastAttemptedAt = lastAttemptedAt;
        this.publishedAt = publishedAt;
        this.lastError = lastError;
        this.operatorNote = operatorNote;
        this.notedBy = notedBy;
        this.notedAt = notedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getMessageKey() {
        return messageKey;
    }

    public String getPayload() {
        return payload;
    }

    public FraudOutboundEventStatus getStatus() {
        return status;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public Instant getNextAttemptAt() {
        return nextAttemptAt;
    }

    public Instant getLastAttemptedAt() {
        return lastAttemptedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public String getLastError() {
        return lastError;
    }

    public String getOperatorNote() {
        return operatorNote;
    }

    public String getNotedBy() {
        return notedBy;
    }

    public Instant getNotedAt() {
        return notedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void markDelivered(Instant deliveredAt) {
        this.status = FraudOutboundEventStatus.DELIVERED;
        this.attemptCount += 1;
        this.lastAttemptedAt = deliveredAt;
        this.publishedAt = deliveredAt;
        this.lastError = null;
        this.updatedAt = deliveredAt;
    }

    public void markRetryScheduled(Instant attemptedAt, Instant nextAttemptAt, String lastError) {
        this.status = FraudOutboundEventStatus.PENDING;
        this.attemptCount += 1;
        this.lastAttemptedAt = attemptedAt;
        this.nextAttemptAt = nextAttemptAt;
        this.lastError = truncate(lastError);
        this.updatedAt = attemptedAt;
    }

    public void markFailed(Instant attemptedAt, String lastError) {
        this.status = FraudOutboundEventStatus.FAILED;
        this.attemptCount += 1;
        this.lastAttemptedAt = attemptedAt;
        this.lastError = truncate(lastError);
        this.updatedAt = attemptedAt;
    }

    public void retryNow(Instant requestedAt) {
        this.status = FraudOutboundEventStatus.PENDING;
        this.nextAttemptAt = requestedAt;
        this.updatedAt = requestedAt;
    }

    public void updateOperatorNote(String operatorNote, String notedBy, Instant notedAt) {
        this.operatorNote = truncate(operatorNote, 2000);
        this.notedBy = truncate(notedBy, 120);
        this.notedAt = notedAt;
        this.updatedAt = notedAt;
    }

    private String truncate(String value) {
        return truncate(value, 1000);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
