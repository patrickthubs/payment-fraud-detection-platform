package com.frauddetection.platform.entity;

import com.frauddetection.platform.model.StepUpDeliveryChannel;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(name = "fraud_operators")
public class FraudOperatorEntity {

    @Id
    private UUID id;

    @Column(name = "username", nullable = false, unique = true, length = 120)
    private String username;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "account_non_locked", nullable = false)
    private boolean accountNonLocked;

    @Column(name = "email", nullable = false, length = 255)
    private String email;

    @Column(name = "step_up_delivery_channel", nullable = false, length = 40)
    @Enumerated(EnumType.STRING)
    private StepUpDeliveryChannel stepUpDeliveryChannel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "organization_id", nullable = false)
    private FraudOrganizationEntity organization;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
        name = "fraud_operator_role_assignments",
        joinColumns = @JoinColumn(name = "operator_id"),
        inverseJoinColumns = @JoinColumn(name = "role_code", referencedColumnName = "code")
    )
    private Set<FraudOperatorRoleEntity> roles = new LinkedHashSet<>();

    protected FraudOperatorEntity() {
    }

    public FraudOperatorEntity(
        UUID id,
        String username,
        String displayName,
        String passwordHash,
        boolean active,
        boolean accountNonLocked,
        String email,
        StepUpDeliveryChannel stepUpDeliveryChannel,
        Instant createdAt,
        Instant updatedAt,
        Set<FraudOperatorRoleEntity> roles
    ) {
        this(
            id,
            username,
            displayName,
            passwordHash,
            active,
            accountNonLocked,
            email,
            stepUpDeliveryChannel,
            new FraudOrganizationEntity(
                UUID.fromString("f2000000-0000-0000-0000-000000000001"),
                "signal-desk-demo",
                "Signal Desk Demo Bank",
                "LOCAL_DEMO",
                "ACTIVE",
                createdAt,
                updatedAt
            ),
            createdAt,
            updatedAt,
            roles
        );
    }

    public FraudOperatorEntity(
        UUID id,
        String username,
        String displayName,
        String passwordHash,
        boolean active,
        boolean accountNonLocked,
        String email,
        StepUpDeliveryChannel stepUpDeliveryChannel,
        FraudOrganizationEntity organization,
        Instant createdAt,
        Instant updatedAt,
        Set<FraudOperatorRoleEntity> roles
    ) {
        this.id = id;
        this.username = username;
        this.displayName = displayName;
        this.passwordHash = passwordHash;
        this.active = active;
        this.accountNonLocked = accountNonLocked;
        this.email = email;
        this.stepUpDeliveryChannel = stepUpDeliveryChannel;
        this.organization = organization;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.roles = new LinkedHashSet<>(roles);
    }

    public FraudOperatorEntity(
        UUID id,
        String username,
        String displayName,
        String passwordHash,
        boolean active,
        boolean accountNonLocked,
        Instant createdAt,
        Instant updatedAt,
        Set<FraudOperatorRoleEntity> roles
    ) {
        this(
            id,
            username,
            displayName,
            passwordHash,
            active,
            accountNonLocked,
            username + "@internal.local",
            StepUpDeliveryChannel.EMAIL,
            new FraudOrganizationEntity(
                UUID.fromString("f2000000-0000-0000-0000-000000000001"),
                "signal-desk-demo",
                "Signal Desk Demo Bank",
                "LOCAL_DEMO",
                "ACTIVE",
                createdAt,
                updatedAt
            ),
            createdAt,
            updatedAt,
            roles
        );
    }

    public UUID getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public boolean isActive() {
        return active;
    }

    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public String getEmail() {
        return email;
    }

    public StepUpDeliveryChannel getStepUpDeliveryChannel() {
        return stepUpDeliveryChannel;
    }

    public FraudOrganizationEntity getOrganization() {
        return organization;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Set<FraudOperatorRoleEntity> getRoles() {
        return roles;
    }
}
