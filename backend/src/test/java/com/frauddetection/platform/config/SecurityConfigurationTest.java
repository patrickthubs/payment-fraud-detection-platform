package com.frauddetection.platform.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;

class SecurityConfigurationTest {

    @Test
    void mapsMachineTokenRolesAndScopesToSpringAuthorities() {
        Jwt jwt = Jwt.withTokenValue("machine-token")
            .header("alg", "none")
            .subject("fraud-scoring-client")
            .issuedAt(Instant.parse("2026-07-18T10:00:00Z"))
            .expiresAt(Instant.parse("2026-07-18T10:05:00Z"))
            .claim("scope", "fraud.score")
            .claim("roles", List.of("SCORING_CLIENT", "ROLE_FRAUD_ANALYST"))
            .build();

        var authentication = new SecurityConfiguration()
            .machineJwtAuthenticationConverter("roles")
            .convert(jwt);

        assertThat(authentication).isNotNull();
        assertThat(authentication.getAuthorities())
            .extracting("authority")
            .containsExactlyInAnyOrder(
                "SCOPE_fraud.score",
                "ROLE_SCORING_CLIENT",
                "ROLE_FRAUD_ANALYST",
                "FACTOR_BEARER"
            );
    }
}
