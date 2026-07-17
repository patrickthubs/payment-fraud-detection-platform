create table fraud_replay_batches (
    id uuid primary key,
    batch_name varchar(160) not null,
    scenario_count integer not null,
    challenge_threshold integer not null,
    hold_threshold integer not null,
    decline_threshold integer not null,
    created_by varchar(120) not null,
    created_at timestamp with time zone not null
);

create table fraud_replay_batch_items (
    id uuid primary key,
    batch_id uuid not null references fraud_replay_batches(id),
    scenario_index integer not null,
    payment_id varchar(100) not null,
    customer_id varchar(100) not null,
    risk_score integer not null,
    decision varchar(20) not null,
    projected_payment_status varchar(20) not null,
    velocity_source varchar(30) not null,
    review_case_would_be_created boolean not null,
    summary varchar(500) not null,
    triggered_factor_codes varchar(500) not null,
    created_at timestamp with time zone not null
);

create index idx_fraud_replay_batches_created_at
    on fraud_replay_batches (created_at desc);

create index idx_fraud_replay_batch_items_batch_id
    on fraud_replay_batch_items (batch_id, scenario_index);
