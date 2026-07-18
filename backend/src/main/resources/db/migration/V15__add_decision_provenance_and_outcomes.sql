alter table fraud_scoring_profiles
    add column ruleset_version varchar(80) not null default 'rules-v1';

alter table fraud_scoring_profiles
    add column rule_definition varchar(12000) not null default '{}';

alter table fraud_scoring_profiles
    add column entity_version bigint not null default 0;

alter table fraud_assessment_records
    add column scoring_profile_id uuid;

alter table fraud_assessment_records
    add column scoring_profile_version integer not null default 0;

alter table fraud_assessment_records
    add column ruleset_version varchar(80) not null default 'rules-v1';

alter table fraud_assessment_records
    add column input_snapshot varchar(12000) not null default '{}';

alter table fraud_assessment_records
    add column factor_details varchar(12000) not null default '[]';

alter table fraud_assessment_records
    add column idempotency_key varchar(120);

alter table fraud_assessment_records
    add column request_hash varchar(64);

alter table fraud_assessment_records
    add constraint fk_fraud_assessment_scoring_profile
        foreign key (scoring_profile_id) references fraud_scoring_profiles (id);

create unique index uq_fraud_assessment_idempotency_key
    on fraud_assessment_records (idempotency_key);

create index idx_fraud_assessment_profile_version
    on fraud_assessment_records (scoring_profile_version, created_at desc);

alter table fraud_review_cases
    add column entity_version bigint not null default 0;

alter table payment_records
    add column entity_version bigint not null default 0;

create table fraud_outcomes (
    id uuid primary key,
    assessment_id uuid not null unique,
    outcome_label varchar(40) not null,
    source varchar(60) not null,
    actual_loss numeric(19, 2) not null default 0,
    recovered_amount numeric(19, 2) not null default 0,
    notes varchar(1000),
    labelled_by varchar(120) not null,
    occurred_at timestamp with time zone,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    entity_version bigint not null default 0,
    constraint fk_fraud_outcomes_assessment
        foreign key (assessment_id) references fraud_assessment_records (id),
    constraint chk_fraud_outcomes_actual_loss_non_negative check (actual_loss >= 0),
    constraint chk_fraud_outcomes_recovered_non_negative check (recovered_amount >= 0),
    constraint chk_fraud_outcomes_recovery_not_above_loss check (recovered_amount <= actual_loss)
);

create index idx_fraud_outcomes_label_created
    on fraud_outcomes (outcome_label, created_at desc);
