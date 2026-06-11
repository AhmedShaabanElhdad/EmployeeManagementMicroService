ALTER TABLE user_account ADD COLUMN failed_attempts INTEGER NOT NULL DEFAULT 0;
ALTER TABLE user_account ADD COLUMN lock_time TIMESTAMP WITHOUT TIME ZONE;

CREATE TABLE tokens (
    id UUID PRIMARY KEY,
    token VARCHAR(255) NOT NULL UNIQUE,
    type VARCHAR(50) NOT NULL,
    revoked BOOLEAN NOT NULL,
    expired BOOLEAN NOT NULL,
    user_id UUID REFERENCES user_account(id)
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    action VARCHAR(255) NOT NULL,
    username VARCHAR(255),
    details TEXT,
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL,
    ip_address VARCHAR(255)
);
