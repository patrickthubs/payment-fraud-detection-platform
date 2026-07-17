create table fraud_assessment_records (
    id uuid primary key,
    payment_id varchar(100) not null,
    customer_id varchar(100) not null,
    risk_score integer not null,
    decision varchar(20) not null,
    velocity_source varchar(30) not null,
    summary varchar(500) not null,
    triggered_factor_codes varchar(500) not null,
    created_at timestamp with time zone not null
);

create index idx_fraud_assessment_records_payment_id
    on fraud_assessment_records (payment_id);

create index idx_fraud_assessment_records_customer_id
    on fraud_assessment_records (customer_id);

create table fraud_review_cases (
    id uuid primary key,
    assessment_id uuid not null unique,
    payment_id varchar(100) not null,
    customer_id varchar(100) not null,
    risk_score integer not null,
    decision varchar(20) not null,
    case_status varchar(20) not null,
    summary varchar(500) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_fraud_review_cases_assessment
        foreign key (assessment_id) references fraud_assessment_records (id)
);

create index idx_fraud_review_cases_status_created_at
    on fraud_review_cases (case_status, created_at desc);
