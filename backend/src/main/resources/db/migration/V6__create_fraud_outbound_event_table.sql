create table fraud_outbound_events (
    id uuid primary key,
    event_type varchar(120) not null,
    topic_name varchar(200) not null,
    message_key varchar(120) not null,
    payload varchar(12000) not null,
    status varchar(20) not null,
    attempt_count integer not null,
    next_attempt_at timestamp with time zone not null,
    last_attempted_at timestamp with time zone,
    published_at timestamp with time zone,
    last_error varchar(1000),
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null
);

create index idx_fraud_outbound_events_status_next_attempt
    on fraud_outbound_events (status, next_attempt_at);

create index idx_fraud_outbound_events_created_at
    on fraud_outbound_events (created_at);
