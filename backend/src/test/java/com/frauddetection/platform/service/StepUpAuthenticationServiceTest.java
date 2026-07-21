package com.frauddetection.platform.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.frauddetection.platform.config.FraudStepUpProperties;
import com.frauddetection.platform.entity.FraudOperatorEntity;
import com.frauddetection.platform.entity.FraudOperatorRoleEntity;
import com.frauddetection.platform.entity.StepUpOperatorSecurityStateEntity;
import com.frauddetection.platform.entity.StepUpTokenDeliveryEntity;
import com.frauddetection.platform.exception.StepUpDeliveryFailedException;
import com.frauddetection.platform.exception.StepUpRateLimitedException;
import com.frauddetection.platform.model.StepUpDeliveryChannel;
import com.frauddetection.platform.repository.FraudOperatorRepository;
import com.frauddetection.platform.repository.StepUpOperatorSecurityStateRepository;
import com.frauddetection.platform.repository.StepUpTokenDeliveryRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.ott.OneTimeToken;
import org.springframework.security.authentication.ott.OneTimeTokenService;

class StepUpAuthenticationServiceTest {

    private static final UUID ORGANIZATION_ID = UUID.fromString("f2000000-0000-0000-0000-000000000001");

    private OneTimeTokenService oneTimeTokenService;
    private FraudOperatorRepository fraudOperatorRepository;
    private StepUpOperatorSecurityStateRepository stepUpOperatorSecurityStateRepository;
    private StepUpTokenDeliveryRepository stepUpTokenDeliveryRepository;
    private StepUpDeliveryGateway stepUpDeliveryGateway;
    private JdbcOperations jdbcOperations;
    private CurrentTenantService currentTenantService;
    private StepUpAuthenticationService stepUpAuthenticationService;

    @BeforeEach
    void setUp() {
        oneTimeTokenService = mock(OneTimeTokenService.class);
        fraudOperatorRepository = mock(FraudOperatorRepository.class);
        stepUpOperatorSecurityStateRepository = mock(StepUpOperatorSecurityStateRepository.class);
        stepUpTokenDeliveryRepository = mock(StepUpTokenDeliveryRepository.class);
        stepUpDeliveryGateway = mock(StepUpDeliveryGateway.class);
        jdbcOperations = mock(JdbcOperations.class);
        currentTenantService = mock(CurrentTenantService.class);
        when(currentTenantService.organizationId()).thenReturn(ORGANIZATION_ID);
        stepUpAuthenticationService = new StepUpAuthenticationService(
            oneTimeTokenService,
            new FraudStepUpProperties(
                Duration.ofMinutes(5),
                Duration.ofMinutes(10),
                true,
                StepUpDeliveryChannel.EMAIL,
                "no-reply@fraud-platform.internal",
                "[Fraud Platform]",
                Duration.ofMinutes(10),
                3,
                Duration.ofMinutes(10),
                5,
                Duration.ofMinutes(15)
            ),
            Clock.fixed(Instant.parse("2026-07-17T18:00:00Z"), ZoneOffset.UTC),
            fraudOperatorRepository,
            stepUpOperatorSecurityStateRepository,
            stepUpTokenDeliveryRepository,
            stepUpDeliveryGateway,
            jdbcOperations,
            currentTenantService
        );
    }

    @Test
    void marksDeliveryFailedAndDeletesTokenWhenGatewayFails() {
        FraudOperatorEntity operator = operator();
        when(fraudOperatorRepository.findByUsernameIgnoreCase("senior.analyst"))
            .thenReturn(Optional.of(operator));
        when(stepUpTokenDeliveryRepository.findTopByOrganizationIdAndOperatorUsernameIgnoreCaseOrderByCreatedAtDesc(
            ORGANIZATION_ID,
            "senior.analyst"
        ))
            .thenReturn(Optional.empty());
        when(stepUpTokenDeliveryRepository.findByOrganizationIdAndOperatorUsernameIgnoreCaseAndStatusIn(
            eq(ORGANIZATION_ID),
            eq("senior.analyst"),
            any()
        ))
            .thenReturn(List.of());
        when(stepUpTokenDeliveryRepository.save(any(StepUpTokenDeliveryEntity.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));
        when(oneTimeTokenService.generate(any()))
            .thenReturn(new TestOneTimeToken("token-123", "senior.analyst", Instant.parse("2026-07-17T18:05:00Z")));

        RuntimeException sendFailure = new StepUpDeliveryFailedException("SMTP delivery failed.");
        org.mockito.Mockito.doThrow(sendFailure).when(stepUpDeliveryGateway).deliver(any());

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/security/step-up/token");
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);

        assertThatThrownBy(() -> stepUpAuthenticationService.generateToken("senior.analyst", request))
            .isInstanceOf(StepUpDeliveryFailedException.class)
            .hasMessage("SMTP delivery failed.");

        ArgumentCaptor<StepUpTokenDeliveryEntity> savedDeliveries =
            ArgumentCaptor.forClass(StepUpTokenDeliveryEntity.class);
        verify(stepUpTokenDeliveryRepository, org.mockito.Mockito.atLeast(2)).save(savedDeliveries.capture());
        assertThat(savedDeliveries.getAllValues().getLast().getStatus().name()).isEqualTo("FAILED");
        verify(jdbcOperations).update("delete from one_time_tokens where token_value = ?", "token-123");
    }

    @Test
    void rejectsTokenGenerationWhenOperatorIsTemporarilyLocked() {
        FraudOperatorEntity operator = operator();
        when(fraudOperatorRepository.findByUsernameIgnoreCase("senior.analyst"))
            .thenReturn(Optional.of(operator));
        when(stepUpOperatorSecurityStateRepository.findByOrganizationIdAndOperatorUsernameIgnoreCase(
            ORGANIZATION_ID,
            operator.getUsername()
        ))
            .thenReturn(Optional.of(new StepUpOperatorSecurityStateEntity(
                operator.getId(),
                ORGANIZATION_ID,
                operator.getUsername(),
                Instant.parse("2026-07-17T17:55:00Z"),
                3,
                Instant.parse("2026-07-17T17:56:00Z"),
                5,
                Instant.parse("2026-07-17T18:10:00Z"),
                "Too many failed verification attempts.",
                Instant.parse("2026-07-17T17:55:00Z"),
                Instant.parse("2026-07-17T18:00:00Z")
            )));

        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/v1/security/step-up/token");

        assertThatThrownBy(() -> stepUpAuthenticationService.generateToken("senior.analyst", request))
            .isInstanceOf(StepUpRateLimitedException.class)
            .hasMessageContaining("Retry after");
    }

    private FraudOperatorEntity operator() {
        return new FraudOperatorEntity(
            UUID.fromString("f1000000-0000-0000-0000-000000000003"),
            "senior.analyst",
            "Senior Analyst",
            "{bcrypt}hash",
            true,
            true,
            "senior.analyst@internal.local",
            StepUpDeliveryChannel.EMAIL,
            Instant.parse("2026-07-17T08:00:00Z"),
            Instant.parse("2026-07-17T08:00:00Z"),
            Set.of(
                new FraudOperatorRoleEntity("FRAUD_ANALYST", "Analyst"),
                new FraudOperatorRoleEntity("FRAUD_SUPERVISOR", "Supervisor")
            )
        );
    }

    private record TestOneTimeToken(String tokenValue, String username, Instant expiresAt) implements OneTimeToken {
        @Override
        public String getTokenValue() {
            return tokenValue;
        }

        @Override
        public String getUsername() {
            return username;
        }

        @Override
        public Instant getExpiresAt() {
            return expiresAt;
        }
    }
}
