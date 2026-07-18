create table fraud_organizations (
    id uuid primary key,
    slug varchar(80) not null unique,
    display_name varchar(160) not null,
    plan_code varchar(40) not null,
    status varchar(40) not null,
    created_at timestamp with time zone not null,
    updated_at timestamp with time zone not null,
    constraint chk_fraud_organizations_status
        check (status in ('ACTIVE', 'SUSPENDED', 'CLOSED'))
);

insert into fraud_organizations (
    id, slug, display_name, plan_code, status, created_at, updated_at
) values (
    'f2000000-0000-0000-0000-000000000001',
    'signal-desk-demo',
    'Signal Desk Demo Bank',
    'LOCAL_DEMO',
    'ACTIVE',
    timestamp with time zone '2026-07-18 08:00:00+00',
    timestamp with time zone '2026-07-18 08:00:00+00'
);

alter table fraud_operators
    add column organization_id uuid;

update fraud_operators
set organization_id = 'f2000000-0000-0000-0000-000000000001'
where organization_id is null;

alter table fraud_operators
    alter column organization_id set not null;

alter table fraud_operators
    add constraint fk_fraud_operators_organization
        foreign key (organization_id) references fraud_organizations (id);

create index idx_fraud_operators_organization
    on fraud_operators (organization_id);
