create table one_time_tokens (
    token_value varchar(128) primary key,
    username varchar(100) not null,
    expires_at timestamp not null
);
