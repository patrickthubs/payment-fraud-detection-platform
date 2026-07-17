alter table fraud_outbound_events
    add column operator_note varchar(2000);

alter table fraud_outbound_events
    add column noted_by varchar(120);

alter table fraud_outbound_events
    add column noted_at timestamp with time zone;

create index idx_fraud_outbound_events_topic_name
    on fraud_outbound_events (topic_name);

create index idx_fraud_outbound_events_event_type
    on fraud_outbound_events (event_type);
