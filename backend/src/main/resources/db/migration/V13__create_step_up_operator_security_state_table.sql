create table step_up_operator_security_state (
    operator_id uuid primary key,
    operator_username varchar(120) not null unique,
    issue_window_started_at timestamp with time zone,
    issue_attempt_count integer not null default 0,
    verify_window_started_at timestamp with time zone,
    verify_failure_count integer not null default 0,
    locked_until timestamp with time zone,
    last_failure_reason varchar(255),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_step_up_security_state_locked_until
    on step_up_operator_security_state (locked_until);
