delete from step_up_token_deliveries
where operator_id in (
    select id from fraud_operators
    where username in ('ingest.client', 'analyst.one', 'senior.analyst', 'platform.admin')
);

delete from step_up_operator_security_state
where operator_username in ('ingest.client', 'analyst.one', 'senior.analyst', 'platform.admin');

delete from one_time_tokens
where username in ('ingest.client', 'analyst.one', 'senior.analyst', 'platform.admin');

delete from fraud_operator_role_assignments
where operator_id in (
    select id from fraud_operators
    where username in ('ingest.client', 'analyst.one', 'senior.analyst', 'platform.admin')
);

delete from fraud_operators
where username in ('ingest.client', 'analyst.one', 'senior.analyst', 'platform.admin');
