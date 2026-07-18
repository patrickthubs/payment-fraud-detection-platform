package com.frauddetection.platform.service;

import com.frauddetection.platform.entity.FraudOrganizationEntity;
import java.util.Collection;
import java.util.UUID;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

public class FraudOperatorPrincipal extends User {

    private final UUID organizationId;
    private final String organizationSlug;
    private final String organizationName;
    private final String organizationPlanCode;
    private final String organizationStatus;

    public FraudOperatorPrincipal(
        String username,
        String password,
        boolean enabled,
        boolean accountNonExpired,
        boolean credentialsNonExpired,
        boolean accountNonLocked,
        Collection<? extends GrantedAuthority> authorities,
        FraudOrganizationEntity organization
    ) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.organizationId = organization.getId();
        this.organizationSlug = organization.getSlug();
        this.organizationName = organization.getDisplayName();
        this.organizationPlanCode = organization.getPlanCode();
        this.organizationStatus = organization.getStatus();
    }

    public UUID organizationId() {
        return organizationId;
    }

    public String organizationSlug() {
        return organizationSlug;
    }

    public String organizationName() {
        return organizationName;
    }

    public String organizationPlanCode() {
        return organizationPlanCode;
    }

    public String organizationStatus() {
        return organizationStatus;
    }
}
