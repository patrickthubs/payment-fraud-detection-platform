package com.frauddetection.platform.entity;

import com.frauddetection.platform.model.PaymentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payment_state_transitions")
public class PaymentStateTransitionEntity {

    @Id
    private UUID id;

    @Column(name = "payment_id", nullable = false, length = 100)
    private String paymentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", length = 20)
    private PaymentStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 20)
    private PaymentStatus toStatus;

    @Column(name = "reason", nullable = false, length = 500)
    private String reason;

    @Column(name = "assessment_id", nullable = false)
    private UUID assessmentId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PaymentStateTransitionEntity() {
    }

    public PaymentStateTransitionEntity(
        UUID id,
        String paymentId,
        PaymentStatus fromStatus,
        PaymentStatus toStatus,
        String reason,
        UUID assessmentId,
        Instant createdAt
    ) {
        this.id = id;
        this.paymentId = paymentId;
        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.reason = reason;
        this.assessmentId = assessmentId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getPaymentId() {
        return paymentId;
    }

    public PaymentStatus getFromStatus() {
        return fromStatus;
    }

    public PaymentStatus getToStatus() {
        return toStatus;
    }

    public String getReason() {
        return reason;
    }

    public UUID getAssessmentId() {
        return assessmentId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
