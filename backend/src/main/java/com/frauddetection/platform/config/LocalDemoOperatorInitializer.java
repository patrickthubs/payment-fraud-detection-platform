package com.frauddetection.platform.config;

import java.sql.Timestamp;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(name = "fraud.security.demo-users-enabled", havingValue = "true")
public class LocalDemoOperatorInitializer implements ApplicationRunner {

    private static final UUID DEMO_ORGANIZATION_ID = UUID.fromString("f2000000-0000-0000-0000-000000000001");

    private static final List<DemoOperator> OPERATORS = List.of(
        new DemoOperator(
            UUID.fromString("f1000000-0000-0000-0000-000000000001"),
            "ingest.client",
            "Ingest Client",
            "ingest.client@internal.local",
            "{bcrypt}$2a$10$qmW.spe56KymO3cf6KJUmu1cLjvsTw/fT3S3xDubE8XmiwXqwuod6",
            List.of("SCORING_CLIENT")
        ),
        new DemoOperator(
            UUID.fromString("f1000000-0000-0000-0000-000000000002"),
            "analyst.one",
            "Analyst One",
            "analyst.one@internal.local",
            "{bcrypt}$2a$10$B.n5u4oz8d54WI3K03s8ReUNUX/Sy20X4dKDqKm3retDFHxSO8oTK",
            List.of("FRAUD_ANALYST")
        ),
        new DemoOperator(
            UUID.fromString("f1000000-0000-0000-0000-000000000003"),
            "senior.analyst",
            "Senior Analyst",
            "senior.analyst@internal.local",
            "{bcrypt}$2a$10$8JJbhFQ1Rcrs0aHWv9k6t.kPJHw16lfTT7Xy9DQTRiS.bt6d4FS9C",
            List.of("FRAUD_ANALYST", "FRAUD_SUPERVISOR")
        ),
        new DemoOperator(
            UUID.fromString("f1000000-0000-0000-0000-000000000004"),
            "platform.admin",
            "Platform Administrator",
            "platform.admin@internal.local",
            "{bcrypt}$2a$10$.yrE1HXDzjJ8Ozyb7VsRuOQCENuO50YXgtkdNIHPnpMXzinbQVb.S",
            List.of("FRAUD_ANALYST", "FRAUD_SUPERVISOR", "PLATFORM_ADMIN")
        )
    );

    private final JdbcTemplate jdbcTemplate;
    private final Clock clock;

    public LocalDemoOperatorInitializer(JdbcTemplate jdbcTemplate, Clock clock) {
        this.jdbcTemplate = jdbcTemplate;
        this.clock = clock;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Timestamp now = Timestamp.from(clock.instant());
        ensureDemoOrganization(now);
        for (DemoOperator operator : OPERATORS) {
            if (operatorExists(operator.username())) {
                continue;
            }
            jdbcTemplate.update(
                """
                    insert into fraud_operators (
                        id, organization_id, username, display_name, password_hash, active, account_non_locked,
                        created_at, updated_at, email, step_up_delivery_channel
                    ) values (?, ?, ?, ?, ?, true, true, ?, ?, ?, 'EMAIL')
                    """,
                operator.id(),
                DEMO_ORGANIZATION_ID,
                operator.username(),
                operator.displayName(),
                operator.passwordHash(),
                now,
                now,
                operator.email()
            );
            for (String role : operator.roles()) {
                jdbcTemplate.update(
                    "insert into fraud_operator_role_assignments (operator_id, role_code, created_at) values (?, ?, ?)",
                    operator.id(),
                    role,
                    now
                );
            }
        }
    }

    private void ensureDemoOrganization(Timestamp now) {
        Integer count = jdbcTemplate.queryForObject(
            "select count(*) from fraud_organizations where id = ?",
            Integer.class,
            DEMO_ORGANIZATION_ID
        );
        if (count != null && count > 0) {
            return;
        }
        jdbcTemplate.update(
            """
                insert into fraud_organizations (
                    id, slug, display_name, plan_code, status, created_at, updated_at
                ) values (?, 'signal-desk-demo', 'Signal Desk Demo Bank', 'LOCAL_DEMO', 'ACTIVE', ?, ?)
                """,
            DEMO_ORGANIZATION_ID,
            now,
            now
        );
    }

    private boolean operatorExists(String username) {
        Integer count = jdbcTemplate.queryForObject(
            "select count(*) from fraud_operators where username = ?",
            Integer.class,
            username
        );
        return count != null && count > 0;
    }

    private record DemoOperator(
        UUID id,
        String username,
        String displayName,
        String email,
        String passwordHash,
        List<String> roles
    ) {
    }
}
