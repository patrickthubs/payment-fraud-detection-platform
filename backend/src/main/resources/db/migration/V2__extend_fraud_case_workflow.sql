alter table fraud_review_cases
    add column current_assignee varchar(120);

alter table fraud_review_cases
    add column resolution_summary varchar(500);

create table fraud_case_timeline_entries (
    id uuid primary key,
    case_id uuid not null,
    action_type varchar(30) not null,
    actor varchar(120) not null,
    detail varchar(500) not null,
    created_at timestamp with time zone not null,
    constraint fk_fraud_case_timeline_entries_case
        foreign key (case_id) references fraud_review_cases (id)
);

create index idx_fraud_case_timeline_entries_case_created_at
    on fraud_case_timeline_entries (case_id, created_at asc);
