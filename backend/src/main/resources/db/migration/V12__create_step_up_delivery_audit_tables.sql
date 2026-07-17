alter table fraud_operators
    add column email varchar(255) not null default 'operator@internal.local';

alter table fraud_operators
    add column step_up_delivery_channel varchar(40) not null default 'EMAIL';

update fraud_operators
set email = case username
    when 'ingest.client' then 'ingest.client@internal.local'
    when 'analyst.one' then 'analyst.one@internal.local'
    when 'senior.analyst' then 'senior.analyst@internal.local'
    when 'platform.admin' then 'platform.admin@internal.local'
    else lower(replace(username, ' ', '.')) || '@internal.local'
end,
step_up_delivery_channel = 'EMAIL';

create table step_up_token_deliveries (
    id uuid primary key,
    operator_id uuid not null,
    operator_username varchar(120) not null,
    delivery_channel varchar(40) not null,
    delivery_destination varchar(255) not null,
    destination_masked varchar(255) not null,
    status varchar(40) not null,
    token_hash varchar(128) not null unique,
    resend_sequence integer not null,
    attempt_count integer not null,
    failure_reason varchar(500),
    created_at timestamp with time zone not null,
    expires_at timestamp with time zone not null,
    last_attempted_at timestamp with time zone,
    delivered_at timestamp with time zone,
    consumed_at timestamp with time zone,
    revoked_at timestamp with time zone,
    constraint fk_step_up_token_deliveries_operator
        foreign key (operator_id) references fraud_operators (id)
);

create index idx_step_up_token_deliveries_operator_created
    on step_up_token_deliveries (operator_username, created_at desc);

create index idx_step_up_token_deliveries_status_created
    on step_up_token_deliveries (status, created_at desc);
