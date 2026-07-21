package com.frauddetection.platform;

import static org.assertj.core.api.Assertions.assertThat;

import com.frauddetection.platform.entity.FraudScoringProfileEntity;
import com.frauddetection.platform.repository.FraudScoringProfileRepository;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@SpringBootTest(properties = {
    "fraud.security.demo-users-enabled=false",
    "fraud.outbox.enabled=false",
    "spring.jpa.hibernate.ddl-auto=validate"
})
@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlMigrationIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
        DockerImageName.parse("postgres:17-alpine")
    )
        .withDatabaseName("fraud_platform_test")
        .withUsername("fraud_test")
        .withPassword("fraud_test");

    @DynamicPropertySource
    static void configurePostgres(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private FraudScoringProfileRepository fraudScoringProfileRepository;

    @Test
    void appliesEveryMigrationAndSupportsJpaRepositories() {
        Integer successfulMigrations = jdbcTemplate.queryForObject(
            "select count(*) from flyway_schema_history where success",
            Integer.class
        );

        assertThat(successfulMigrations).isEqualTo(18);
        Integer organizations = jdbcTemplate.queryForObject(
            "select count(*) from fraud_organizations",
            Integer.class
        );
        assertThat(organizations).isEqualTo(1);

        Instant now = Instant.parse("2026-07-18T10:00:00Z");
        FraudScoringProfileEntity profile = fraudScoringProfileRepository.saveAndFlush(
            new FraudScoringProfileEntity(
                UUID.randomUUID(), UUID.fromString("f2000000-0000-0000-0000-000000000001"), 1, "PostgreSQL validation", 45, 65, 85,
                "Testcontainers repository validation", "integration-test", null, true, now, now, null
            )
        );

        Integer tenantScopedPayments = jdbcTemplate.queryForObject(
            "select count(*) from information_schema.columns where table_name = 'payment_records' and column_name = 'organization_id'",
            Integer.class
        );
        assertThat(tenantScopedPayments).isOne();
        Integer tenantScopedStepUpDeliveries = jdbcTemplate.queryForObject(
            "select count(*) from information_schema.columns where table_name = 'step_up_token_deliveries' and column_name = 'organization_id'",
            Integer.class
        );
        assertThat(tenantScopedStepUpDeliveries).isOne();
        Integer tenantScopedStepUpSecurity = jdbcTemplate.queryForObject(
            "select count(*) from information_schema.columns where table_name = 'step_up_operator_security_state' and column_name = 'organization_id'",
            Integer.class
        );
        assertThat(tenantScopedStepUpSecurity).isOne();

        assertThat(fraudScoringProfileRepository.findByOrganizationIdAndActiveTrue(UUID.fromString("f2000000-0000-0000-0000-000000000001")))
            .hasValueSatisfying(persistedProfile -> {
                assertThat(persistedProfile.getId()).isEqualTo(profile.getId());
                assertThat(persistedProfile.getProfileName()).isEqualTo("PostgreSQL validation");
            });
    }
}
