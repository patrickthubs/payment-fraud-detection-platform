alter table step_up_token_deliveries
    add column organization_id uuid;

update step_up_token_deliveries delivery
set organization_id = operator.organization_id
from fraud_operators operator
where delivery.operator_id = operator.id
  and delivery.organization_id is null;

alter table step_up_token_deliveries
    alter column organization_id set not null;

alter table step_up_token_deliveries
    add constraint fk_step_up_token_deliveries_organization
        foreign key (organization_id) references fraud_organizations (id);

create index idx_step_up_token_deliveries_org_operator_created
    on step_up_token_deliveries (organization_id, operator_username, created_at desc);

create index idx_step_up_token_deliveries_org_status_created
    on step_up_token_deliveries (organization_id, status, created_at desc);

alter table step_up_operator_security_state
    add column organization_id uuid;

update step_up_operator_security_state state
set organization_id = operator.organization_id
from fraud_operators operator
where state.operator_id = operator.id
  and state.organization_id is null;

alter table step_up_operator_security_state
    alter column organization_id set not null;

alter table step_up_operator_security_state
    add constraint fk_step_up_security_state_organization
        foreign key (organization_id) references fraud_organizations (id);

alter table step_up_operator_security_state
    add constraint fk_step_up_security_state_operator
        foreign key (operator_id) references fraud_operators (id);

alter table step_up_operator_security_state
    drop constraint if exists step_up_operator_security_state_operator_username_key;

create unique index uq_step_up_security_state_org_operator_username
    on step_up_operator_security_state (organization_id, operator_username);

create index idx_step_up_security_state_org_locked_until
    on step_up_operator_security_state (organization_id, locked_until);
