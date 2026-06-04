CREATE TABLE payroll (
    id UUID PRIMARY KEY,
    employee_id UUID NOT NULL UNIQUE,
    salary DECIMAL(19, 2) NOT NULL,
    status VARCHAR(50) NOT NULL
);
