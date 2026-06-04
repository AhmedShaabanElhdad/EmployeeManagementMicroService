CREATE TABLE employee (
    id UUID PRIMARY KEY,
    first_name VARCHAR(100) NOT NULL,
    last_name VARCHAR(100) NOT NULL,
    email VARCHAR(255) UNIQUE,
    hire_at DATE NOT NULL,
    phone_number VARCHAR(25) NOT NULL,
    is_verified BOOLEAN NOT NULL DEFAULT FALSE,
    account_creation_token VARCHAR(255),
    position VARCHAR(255) NOT NULL,
    department_id UUID NOT NULL,
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX idx_employee_email ON employee (email);
CREATE INDEX idx_employee_department ON employee (department_id);

CREATE TABLE outbox (
    id UUID PRIMARY KEY,
    event_id UUID NOT NULL,
    aggregate_id VARCHAR(255) NOT NULL,
    aggregate_type VARCHAR(255) NOT NULL,
    event_type VARCHAR(255) NOT NULL,
    payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    processed_at TIMESTAMP WITH TIME ZONE,
    processed BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE TABLE idempotency_keys (
    key VARCHAR(255) PRIMARY KEY,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    request_path VARCHAR(255),
    http_method VARCHAR(10),
    response_hash VARCHAR(255)
);

CREATE TABLE employee_list_view (
    id UUID PRIMARY KEY,
    full_name VARCHAR(255),
    position VARCHAR(255),
    department_name VARCHAR(255),
    email VARCHAR(255),
    status VARCHAR(50)
);
