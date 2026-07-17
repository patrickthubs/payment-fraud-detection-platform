create table fraud_scoring_profiles (
    id uuid primary key,
    version_number integer not null unique,
    profile_name varchar(120) not null unique,
    challenge_threshold integer not null,
    hold_threshold integer not null,
    decline_threshold integer not null,
    change_summary varchar(500) not null,
    created_by varchar(120) not null,
    activated_by varchar(120),
    active boolean not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    activated_at timestamp with time zone
);

create index idx_fraud_scoring_profiles_active
    on fraud_scoring_profiles (active);

create index idx_fraud_scoring_profiles_created_at
    on fraud_scoring_profiles (created_at desc);
