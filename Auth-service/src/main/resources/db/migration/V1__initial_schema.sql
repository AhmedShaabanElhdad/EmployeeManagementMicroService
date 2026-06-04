CREATE TABLE user_account (
    id UUID PRIMARY KEY,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    role VARCHAR(255) CHECK (role IN ('USER', 'ADMIN')),
    enabled BOOLEAN NOT NULL,
    account_locked BOOLEAN NOT NULL,
    employee_id UUID NOT NULL
);

CREATE INDEX idx_username ON user_account (username);

CREATE TABLE auth_outbox (
    id UUID PRIMARY KEY,
    aggregate_id VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    processed BOOLEAN NOT NULL DEFAULT FALSE
);
