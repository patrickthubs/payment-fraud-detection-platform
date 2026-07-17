alter table payment_records
    add column latest_risk_score integer not null default 0;

alter table payment_records
    add column challenge_outcome varchar(20);

alter table payment_records
    add column challenged_at timestamp with time zone;

alter table payment_records
    add column challenge_completed_at timestamp with time zone;

alter table payment_records
    add column challenge_completed_by varchar(100);

alter table payment_records
    add column challenge_outcome_note varchar(500);

update payment_records pr
set latest_risk_score = (
    select far.risk_score
    from fraud_assessment_records far
    where far.id = pr.latest_assessment_id
);

create index idx_payment_records_challenge_outcome
    on payment_records (challenge_outcome);

create index idx_payment_records_payment_status_challenge_outcome
    on payment_records (payment_status, challenge_outcome);
