package com.frauddetection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

class CurrentTenantServiceTest {

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void resolvesOrganizationFromMachineJwtClaim() {
        UUID organizationId = UUID.fromString("f2000000-0000-0000-0000-000000000099");
        Jwt jwt = jwt(Map.of("organization_id", organizationId.toString()));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(jwt, null, "ROLE_SCORING_CLIENT"));

        CurrentTenantService currentTenantService = new CurrentTenantService(false, "organization_id");

        assertThat(currentTenantService.organizationId()).isEqualTo(organizationId);
    }

    @Test
    void rejectsMachineJwtWithoutTenantClaimWhenDemoFallbackDisabled() {
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
            jwt(Map.of("sub", "machine-client")),
            null,
            "ROLE_SCORING_CLIENT"
        ));

        CurrentTenantService currentTenantService = new CurrentTenantService(false, "organization_id");

        assertThatThrownBy(currentTenantService::organizationId)
            .isInstanceOf(AccessDeniedException.class)
            .hasMessageContaining("organization claim 'organization_id'");
    }

    @Test
    void rejectsAnonymousTenantLookupWhenDemoFallbackDisabled() {
        CurrentTenantService currentTenantService = new CurrentTenantService(false, "organization_id");

        assertThatThrownBy(currentTenantService::organizationId)
            .isInstanceOf(AccessDeniedException.class)
            .hasMessage("Authenticated tenant context is required.");
    }

    @Test
    void keepsLocalDemoFallbackAvailableForLocalAndUnitTestFlows() {
        CurrentTenantService currentTenantService = new CurrentTenantService(true, "organization_id");

        assertThat(currentTenantService.organizationId()).isEqualTo(CurrentTenantService.LOCAL_DEMO_ORGANIZATION_ID);
    }

    private Jwt jwt(Map<String, Object> claims) {
        return new Jwt(
            "token-value",
            Instant.parse("2026-07-21T18:00:00Z"),
            Instant.parse("2026-07-21T18:05:00Z"),
            Map.of("alg", "none"),
            claims
        );
    }
}
