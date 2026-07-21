package com.frauddetection.platform.service;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

@Service
public class CurrentTenantService {

    static final UUID LOCAL_DEMO_ORGANIZATION_ID = UUID.fromString("f2000000-0000-0000-0000-000000000001");

    private final boolean demoTenantFallbackEnabled;
    private final String machineOrganizationIdClaim;

    public CurrentTenantService(
        @Value("${fraud.security.demo-users-enabled:false}") boolean demoTenantFallbackEnabled,
        @Value("${fraud.security.machine-auth.organization-id-claim:organization_id}") String machineOrganizationIdClaim
    ) {
        this.demoTenantFallbackEnabled = demoTenantFallbackEnabled;
        this.machineOrganizationIdClaim = machineOrganizationIdClaim;
    }

    public UUID organizationId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return fallbackOrReject();
        }
        Object principal = authentication.getPrincipal();
        if (principal instanceof FraudOperatorPrincipal operatorPrincipal) {
            return operatorPrincipal.organizationId();
        }
        if (principal instanceof Jwt jwt) {
            return resolveMachineOrganization(jwt);
        }
        return fallbackOrReject();
    }

    private UUID resolveMachineOrganization(Jwt jwt) {
        Object claim = jwt.getClaims().get(machineOrganizationIdClaim);
        if (claim == null || claim.toString().isBlank()) {
            throw new AccessDeniedException(
                "Machine authentication token must include organization claim '%s'.".formatted(machineOrganizationIdClaim)
            );
        }
        try {
            return UUID.fromString(claim.toString());
        }
        catch (IllegalArgumentException exception) {
            throw new AccessDeniedException(
                "Machine authentication token organization claim '%s' must be a UUID.".formatted(machineOrganizationIdClaim)
            );
        }
    }

    private UUID fallbackOrReject() {
        if (demoTenantFallbackEnabled) {
            return LOCAL_DEMO_ORGANIZATION_ID;
        }
        throw new AccessDeniedException("Authenticated tenant context is required.");
    }
}
