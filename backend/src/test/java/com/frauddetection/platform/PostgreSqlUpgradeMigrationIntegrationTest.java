package com.frauddetection.platform;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.DriverManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class PostgreSqlUpgradeMigrationIntegrationTest {

    @Container
    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
        DockerImageName.parse("postgres:17-alpine")
    )
        .withDatabaseName("fraud_platform_upgrade_test")
        .withUsername("fraud_test")
        .withPassword("fraud_test");

    @Test
    void upgradesExistingStepUpHistoryThroughDemoOperatorIsolation() throws Exception {
        Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .target(MigrationVersion.fromVersion("13"))
            .load()
            .migrate();

        try (var connection = DriverManager.getConnection(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        )) {
            UUID operatorId = UUID.fromString("f1000000-0000-0000-0000-000000000099");
            try (var operator = connection.prepareStatement("""
                insert into fraud_operators (
                    id, username, display_name, password_hash, active, account_non_locked,
                    email, step_up_delivery_channel, created_at, updated_at
                ) values (?, 'legacy.supervisor', 'Legacy Supervisor', '{bcrypt}hash', true, true,
                    'legacy.supervisor@internal.local', 'EMAIL', ?, ?)
                """)) {
                operator.setObject(1, operatorId);
                operator.setTimestamp(2, Timestamp.from(Instant.parse("2026-07-18T09:00:00Z")));
                operator.setTimestamp(3, Timestamp.from(Instant.parse("2026-07-18T09:00:00Z")));
                operator.executeUpdate();
            }
            try (var delivery = connection.prepareStatement("""
                insert into step_up_token_deliveries (
                    id, operator_id, operator_username, delivery_channel, delivery_destination,
                    destination_masked, status, token_hash, resend_sequence, attempt_count,
                    created_at, expires_at
                ) values (?, ?, ?, 'EMAIL', ?, ?, 'DELIVERED', ?, 0, 1, ?, ?)
                """)) {
                delivery.setObject(1, UUID.randomUUID());
                delivery.setObject(2, operatorId);
                delivery.setString(3, "legacy.supervisor");
                delivery.setString(4, "legacy.supervisor@internal.local");
                delivery.setString(5, "l***@internal.local");
                delivery.setString(6, UUID.randomUUID().toString());
                delivery.setTimestamp(7, Timestamp.from(Instant.parse("2026-07-18T10:00:00Z")));
                delivery.setTimestamp(8, Timestamp.from(Instant.parse("2026-07-18T10:05:00Z")));
                delivery.executeUpdate();
            }
            try (var state = connection.prepareStatement("""
                insert into step_up_operator_security_state (
                    operator_id, operator_username, issue_attempt_count, verify_failure_count, created_at, updated_at
                ) values (?, 'legacy.supervisor', 1, 0, ?, ?)
                """)) {
                state.setObject(1, operatorId);
                state.setTimestamp(2, Timestamp.from(Instant.parse("2026-07-18T10:00:00Z")));
                state.setTimestamp(3, Timestamp.from(Instant.parse("2026-07-18T10:00:00Z")));
                state.executeUpdate();
            }
        }

        var result = Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .load()
            .migrate();

        assertThat(result.targetSchemaVersion).hasToString("18");
        try (var connection = DriverManager.getConnection(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        ); var statement = connection.createStatement(); var rows = statement.executeQuery("""
            select count(*)
            from fraud_operators
            where username in ('ingest.client', 'analyst.one', 'senior.analyst', 'platform.admin')
            """)) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getInt(1)).isZero();
        }
        try (var connection = DriverManager.getConnection(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        ); var statement = connection.createStatement(); var rows = statement.executeQuery("""
            select count(*)
            from fraud_organizations
            where slug = 'signal-desk-demo'
            """)) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getInt(1)).isOne();
        }
        try (var connection = DriverManager.getConnection(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        ); var statement = connection.createStatement(); var rows = statement.executeQuery("""
            select count(*)
            from step_up_token_deliveries delivery
            join fraud_organizations organization on organization.id = delivery.organization_id
            where delivery.operator_username = 'legacy.supervisor'
              and organization.slug = 'signal-desk-demo'
            """)) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getInt(1)).isOne();
        }
        try (var connection = DriverManager.getConnection(
            POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword()
        ); var statement = connection.createStatement(); var rows = statement.executeQuery("""
            select count(*)
            from step_up_operator_security_state state
            join fraud_organizations organization on organization.id = state.organization_id
            where state.operator_username = 'legacy.supervisor'
              and organization.slug = 'signal-desk-demo'
            """)) {
            assertThat(rows.next()).isTrue();
            assertThat(rows.getInt(1)).isOne();
        }
    }
}
