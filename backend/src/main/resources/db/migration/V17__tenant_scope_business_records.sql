alter table fraud_assessment_records
    add column organization_id uuid;

update fraud_assessment_records
set organization_id = 'f2000000-0000-0000-0000-000000000001'
where organization_id is null;

alter table fraud_assessment_records
    alter column organization_id set not null;

alter table fraud_assessment_records
    add constraint fk_fraud_assessments_organization
        foreign key (organization_id) references fraud_organizations (id);

drop index if exists uq_fraud_assessment_idempotency_key;

create unique index uq_fraud_assessment_org_idempotency_key
    on fraud_assessment_records (organization_id, idempotency_key);

create index idx_fraud_assessment_org_created
    on fraud_assessment_records (organization_id, created_at desc);

alter table fraud_review_cases
    add column organization_id uuid;

update fraud_review_cases
set organization_id = assessment.organization_id
from fraud_assessment_records assessment
where fraud_review_cases.assessment_id = assessment.id
  and fraud_review_cases.organization_id is null;

alter table fraud_review_cases
    alter column organization_id set not null;

alter table fraud_review_cases
    add constraint fk_fraud_review_cases_organization
        foreign key (organization_id) references fraud_organizations (id);

create index idx_fraud_review_cases_org_status_created
    on fraud_review_cases (organization_id, case_status, created_at desc);

alter table payment_records
    add column organization_id uuid;

update payment_records
set organization_id = assessment.organization_id
from fraud_assessment_records assessment
where payment_records.latest_assessment_id = assessment.id
  and payment_records.organization_id is null;

alter table payment_records
    alter column organization_id set not null;

alter table payment_records
    add constraint fk_payment_records_organization
        foreign key (organization_id) references fraud_organizations (id);

alter table payment_records
    drop constraint if exists payment_records_payment_id_key;

create unique index uq_payment_records_org_payment_id
    on payment_records (organization_id, payment_id);

create index idx_payment_records_org_status_updated
    on payment_records (organization_id, payment_status, updated_at desc);

alter table payment_state_transitions
    add column organization_id uuid;

update payment_state_transitions
set organization_id = assessment.organization_id
from fraud_assessment_records assessment
where payment_state_transitions.assessment_id = assessment.id
  and payment_state_transitions.organization_id is null;

alter table payment_state_transitions
    alter column organization_id set not null;

alter table payment_state_transitions
    add constraint fk_payment_state_transitions_organization
        foreign key (organization_id) references fraud_organizations (id);

create index idx_payment_state_transitions_org_payment_created
    on payment_state_transitions (organization_id, payment_id, created_at asc);

alter table fraud_case_timeline_entries
    add column organization_id uuid;

update fraud_case_timeline_entries
set organization_id = review_case.organization_id
from fraud_review_cases review_case
where fraud_case_timeline_entries.case_id = review_case.id
  and fraud_case_timeline_entries.organization_id is null;

alter table fraud_case_timeline_entries
    alter column organization_id set not null;

alter table fraud_case_timeline_entries
    add constraint fk_fraud_case_timeline_organization
        foreign key (organization_id) references fraud_organizations (id);

create index idx_fraud_case_timeline_org_case_created
    on fraud_case_timeline_entries (organization_id, case_id, created_at asc);

alter table fraud_scoring_profiles
    add column organization_id uuid;

update fraud_scoring_profiles
set organization_id = 'f2000000-0000-0000-0000-000000000001'
where organization_id is null;

alter table fraud_scoring_profiles
    alter column organization_id set not null;

alter table fraud_scoring_profiles
    add constraint fk_fraud_scoring_profiles_organization
        foreign key (organization_id) references fraud_organizations (id);

alter table fraud_scoring_profiles
    drop constraint if exists fraud_scoring_profiles_version_number_key;

alter table fraud_scoring_profiles
    drop constraint if exists fraud_scoring_profiles_profile_name_key;

create unique index uq_fraud_scoring_profiles_org_version
    on fraud_scoring_profiles (organization_id, version_number);

create unique index uq_fraud_scoring_profiles_org_name
    on fraud_scoring_profiles (organization_id, profile_name);

create index idx_fraud_scoring_profiles_org_active
    on fraud_scoring_profiles (organization_id, active);

alter table fraud_outcomes
    add column organization_id uuid;

update fraud_outcomes
set organization_id = assessment.organization_id
from fraud_assessment_records assessment
where fraud_outcomes.assessment_id = assessment.id
  and fraud_outcomes.organization_id is null;

alter table fraud_outcomes
    alter column organization_id set not null;

alter table fraud_outcomes
    add constraint fk_fraud_outcomes_organization
        foreign key (organization_id) references fraud_organizations (id);

alter table fraud_outcomes
    drop constraint if exists fraud_outcomes_assessment_id_key;

create unique index uq_fraud_outcomes_org_assessment
    on fraud_outcomes (organization_id, assessment_id);

alter table fraud_replay_batches
    add column organization_id uuid;

update fraud_replay_batches
set organization_id = 'f2000000-0000-0000-0000-000000000001'
where organization_id is null;

alter table fraud_replay_batches
    alter column organization_id set not null;

alter table fraud_replay_batches
    add constraint fk_fraud_replay_batches_organization
        foreign key (organization_id) references fraud_organizations (id);

create index idx_fraud_replay_batches_org_created
    on fraud_replay_batches (organization_id, created_at desc);

alter table fraud_replay_batch_items
    add column organization_id uuid;

update fraud_replay_batch_items
set organization_id = batch.organization_id
from fraud_replay_batches batch
where fraud_replay_batch_items.batch_id = batch.id
  and fraud_replay_batch_items.organization_id is null;

alter table fraud_replay_batch_items
    alter column organization_id set not null;

alter table fraud_replay_batch_items
    add constraint fk_fraud_replay_items_organization
        foreign key (organization_id) references fraud_organizations (id);

create index idx_fraud_replay_items_org_batch
    on fraud_replay_batch_items (organization_id, batch_id, scenario_index);

alter table fraud_outbound_events
    add column organization_id uuid;

update fraud_outbound_events
set organization_id = 'f2000000-0000-0000-0000-000000000001'
where organization_id is null;

alter table fraud_outbound_events
    alter column organization_id set not null;

alter table fraud_outbound_events
    add constraint fk_fraud_outbound_events_organization
        foreign key (organization_id) references fraud_organizations (id);

create index idx_fraud_outbound_events_org_status_next
    on fraud_outbound_events (organization_id, status, next_attempt_at);

create index idx_fraud_outbound_events_org_created
    on fraud_outbound_events (organization_id, created_at desc);
