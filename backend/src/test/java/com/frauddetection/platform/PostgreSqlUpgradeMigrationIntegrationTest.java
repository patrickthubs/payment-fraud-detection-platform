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
            try (var delivery = connection.prepareStatement("""
                insert into step_up_token_deliveries (
                    id, operator_id, operator_username, delivery_channel, delivery_destination,
                    destination_masked, status, token_hash, resend_sequence, attempt_count,
                    created_at, expires_at
                ) values (?, ?, ?, 'EMAIL', ?, ?, 'DELIVERED', ?, 0, 1, ?, ?)
                """)) {
                delivery.setObject(1, UUID.randomUUID());
                delivery.setObject(2, UUID.fromString("f1000000-0000-0000-0000-000000000004"));
                delivery.setString(3, "platform.admin");
                delivery.setString(4, "platform.admin@internal.local");
                delivery.setString(5, "p***@internal.local");
                delivery.setString(6, UUID.randomUUID().toString());
                delivery.setTimestamp(7, Timestamp.from(Instant.parse("2026-07-18T10:00:00Z")));
                delivery.setTimestamp(8, Timestamp.from(Instant.parse("2026-07-18T10:05:00Z")));
                delivery.executeUpdate();
            }
        }

        var result = Flyway.configure()
            .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
            .load()
            .migrate();

        assertThat(result.targetSchemaVersion).hasToString("15");
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
    }
}
