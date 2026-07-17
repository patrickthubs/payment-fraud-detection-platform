create table fraud_operator_roles (
    code varchar(50) primary key,
    description varchar(200) not null
);

create table fraud_operators (
    id uuid primary key,
    username varchar(120) not null unique,
    display_name varchar(160) not null,
    password_hash varchar(255) not null,
    active boolean not null,
    account_non_locked boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create table fraud_operator_role_assignments (
    operator_id uuid not null,
    role_code varchar(50) not null,
    created_at timestamp with time zone not null,
    primary key (operator_id, role_code),
    constraint fk_fraud_operator_role_assignments_operator
        foreign key (operator_id) references fraud_operators (id),
    constraint fk_fraud_operator_role_assignments_role
        foreign key (role_code) references fraud_operator_roles (code)
);

create index idx_fraud_operators_username
    on fraud_operators (username);

create index idx_fraud_operator_role_assignments_role_code
    on fraud_operator_role_assignments (role_code);

insert into fraud_operator_roles (code, description)
values
    ('SCORING_CLIENT', 'Machine-to-machine client allowed to submit scoring requests.'),
    ('FRAUD_ANALYST', 'First-line analyst access to fraud operations and review queues.'),
    ('FRAUD_SUPERVISOR', 'Supervisor authority for escalations, exports, and final case decisions.'),
    ('PLATFORM_ADMIN', 'Administrative access to protected platform diagnostics and API docs.');

insert into fraud_operators (id, username, display_name, password_hash, active, account_non_locked, created_at, updated_at)
values
    ('f1000000-0000-0000-0000-000000000001', 'ingest.client', 'Ingest Client', '{bcrypt}$2a$10$qmW.spe56KymO3cf6KJUmu1cLjvsTw/fT3S3xDubE8XmiwXqwuod6', true, true, timestamp with time zone '2026-07-17 08:00:00+00', timestamp with time zone '2026-07-17 08:00:00+00'),
    ('f1000000-0000-0000-0000-000000000002', 'analyst.one', 'Analyst One', '{bcrypt}$2a$10$B.n5u4oz8d54WI3K03s8ReUNUX/Sy20X4dKDqKm3retDFHxSO8oTK', true, true, timestamp with time zone '2026-07-17 08:00:00+00', timestamp with time zone '2026-07-17 08:00:00+00'),
    ('f1000000-0000-0000-0000-000000000003', 'senior.analyst', 'Senior Analyst', '{bcrypt}$2a$10$8JJbhFQ1Rcrs0aHWv9k6t.kPJHw16lfTT7Xy9DQTRiS.bt6d4FS9C', true, true, timestamp with time zone '2026-07-17 08:00:00+00', timestamp with time zone '2026-07-17 08:00:00+00'),
    ('f1000000-0000-0000-0000-000000000004', 'platform.admin', 'Platform Administrator', '{bcrypt}$2a$10$.yrE1HXDzjJ8Ozyb7VsRuOQCENuO50YXgtkdNIHPnpMXzinbQVb.S', true, true, timestamp with time zone '2026-07-17 08:00:00+00', timestamp with time zone '2026-07-17 08:00:00+00');

insert into fraud_operator_role_assignments (operator_id, role_code, created_at)
values
    ('f1000000-0000-0000-0000-000000000001', 'SCORING_CLIENT', timestamp with time zone '2026-07-17 08:00:00+00'),
    ('f1000000-0000-0000-0000-000000000002', 'FRAUD_ANALYST', timestamp with time zone '2026-07-17 08:00:00+00'),
    ('f1000000-0000-0000-0000-000000000003', 'FRAUD_ANALYST', timestamp with time zone '2026-07-17 08:00:00+00'),
    ('f1000000-0000-0000-0000-000000000003', 'FRAUD_SUPERVISOR', timestamp with time zone '2026-07-17 08:00:00+00'),
    ('f1000000-0000-0000-0000-000000000004', 'FRAUD_ANALYST', timestamp with time zone '2026-07-17 08:00:00+00'),
    ('f1000000-0000-0000-0000-000000000004', 'FRAUD_SUPERVISOR', timestamp with time zone '2026-07-17 08:00:00+00'),
    ('f1000000-0000-0000-0000-000000000004', 'PLATFORM_ADMIN', timestamp with time zone '2026-07-17 08:00:00+00');
