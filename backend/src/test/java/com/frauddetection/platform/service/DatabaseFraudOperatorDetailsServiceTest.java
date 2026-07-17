package com.frauddetection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.frauddetection.platform.entity.FraudOperatorEntity;
import com.frauddetection.platform.entity.FraudOperatorRoleEntity;
import com.frauddetection.platform.repository.FraudOperatorRepository;
import java.time.Instant;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

class DatabaseFraudOperatorDetailsServiceTest {

    private FraudOperatorRepository fraudOperatorRepository;
    private DatabaseFraudOperatorDetailsService databaseFraudOperatorDetailsService;

    @BeforeEach
    void setUp() {
        fraudOperatorRepository = mock(FraudOperatorRepository.class);
        databaseFraudOperatorDetailsService = new DatabaseFraudOperatorDetailsService(fraudOperatorRepository);
    }

    @Test
    void loadsOperatorWithAuthoritiesAndStateFlags() {
        FraudOperatorEntity operator = new FraudOperatorEntity(
            UUID.fromString("f1000000-0000-0000-0000-000000000099"),
            "senior.analyst",
            "Senior Analyst",
            "{bcrypt}$2a$10$8JJbhFQ1Rcrs0aHWv9k6t.kPJHw16lfTT7Xy9DQTRiS.bt6d4FS9C",
            true,
            true,
            Instant.parse("2026-07-17T08:00:00Z"),
            Instant.parse("2026-07-17T08:00:00Z"),
            Set.of(
                new FraudOperatorRoleEntity("FRAUD_ANALYST", "Analyst"),
                new FraudOperatorRoleEntity("FRAUD_SUPERVISOR", "Supervisor")
            )
        );
        when(fraudOperatorRepository.findByUsernameIgnoreCase("senior.analyst")).thenReturn(Optional.of(operator));

        var userDetails = databaseFraudOperatorDetailsService.loadUserByUsername("senior.analyst");

        assertThat(userDetails.getUsername()).isEqualTo("senior.analyst");
        assertThat(userDetails.getPassword()).startsWith("{bcrypt}");
        assertThat(userDetails.isEnabled()).isTrue();
        assertThat(userDetails.isAccountNonLocked()).isTrue();
        assertThat(userDetails.getAuthorities()).extracting("authority")
            .containsExactlyInAnyOrder("ROLE_FRAUD_ANALYST", "ROLE_FRAUD_SUPERVISOR");
    }

    @Test
    void rejectsUnknownOperator() {
        when(fraudOperatorRepository.findByUsernameIgnoreCase("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> databaseFraudOperatorDetailsService.loadUserByUsername("ghost"))
            .isInstanceOf(UsernameNotFoundException.class)
            .hasMessage("Fraud operator ghost was not found.");
    }
}
