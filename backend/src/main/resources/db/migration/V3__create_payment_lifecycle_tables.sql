create table payment_records (
    id uuid primary key,
    payment_id varchar(100) not null unique,
    customer_id varchar(100) not null,
    amount numeric(19, 2) not null,
    currency varchar(3) not null,
    payment_channel varchar(60) not null,
    merchant_category varchar(80) not null,
    latest_assessment_id uuid not null,
    latest_decision varchar(20) not null,
    payment_status varchar(20) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint fk_payment_records_latest_assessment
        foreign key (latest_assessment_id) references fraud_assessment_records (id)
);

create index idx_payment_records_customer_id
    on payment_records (customer_id);

create index idx_payment_records_status_updated_at
    on payment_records (payment_status, updated_at desc);

create table payment_state_transitions (
    id uuid primary key,
    payment_id varchar(100) not null,
    from_status varchar(20),
    to_status varchar(20) not null,
    reason varchar(500) not null,
    assessment_id uuid not null,
    created_at timestamp with time zone not null,
    constraint fk_payment_state_transitions_assessment
        foreign key (assessment_id) references fraud_assessment_records (id)
);

create index idx_payment_state_transitions_payment_created_at
    on payment_state_transitions (payment_id, created_at asc);
