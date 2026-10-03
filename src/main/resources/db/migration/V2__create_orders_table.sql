-- Schema creation for ordering bounded context
CREATE SCHEMA IF NOT EXISTS ordering;

-- Orders aggregate root persistence table
CREATE TABLE ordering.orders (
    id UUID PRIMARY KEY,
    customer_id UUID NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

-- Performance indices
CREATE INDEX idx_orders_customer ON ordering.orders (customer_id);
CREATE INDEX idx_orders_status ON ordering.orders (status);
