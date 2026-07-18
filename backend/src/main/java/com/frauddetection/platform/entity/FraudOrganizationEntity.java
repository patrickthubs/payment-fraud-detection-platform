package com.frauddetection.platform.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "fraud_organizations")
public class FraudOrganizationEntity {

    @Id
    private UUID id;

    @Column(name = "slug", nullable = false, unique = true, length = 80)
    private String slug;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "plan_code", nullable = false, length = 40)
    private String planCode;

    @Column(name = "status", nullable = false, length = 40)
    private String status;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected FraudOrganizationEntity() {
    }

    public FraudOrganizationEntity(
        UUID id,
        String slug,
        String displayName,
        String planCode,
        String status,
        Instant createdAt,
        Instant updatedAt
    ) {
        this.id = id;
        this.slug = slug;
        this.displayName = displayName;
        this.planCode = planCode;
        this.status = status;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public String getSlug() {
        return slug;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPlanCode() {
        return planCode;
    }

    public String getStatus() {
        return status;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
