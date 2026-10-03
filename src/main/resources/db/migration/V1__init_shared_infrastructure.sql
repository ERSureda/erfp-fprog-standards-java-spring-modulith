-- =========================================================================
-- V1__init_shared_infrastructure.sql
-- Línea base de infraestructura compartida: Extensiones, Outbox e Idempotencia
-- Cumplimiento normativo: SED-05, SED-06, TRX-03, TRX-05, TRX-06
-- =========================================================================

-- 1. Extensiones requeridas para generación y optimización temporal
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "btree_gist";

-- 2. Tabla transaccional de eventos (Transactional Outbox Pattern - TRX-03, SED-05)
CREATE TABLE IF NOT EXISTS outbox_events (
    event_id UUID PRIMARY KEY,
    tenant_id UUID,
    aggregate_type VARCHAR(64) NOT NULL,
    aggregate_id VARCHAR(64) NOT NULL,
    event_type VARCHAR(128) NOT NULL,
    payload JSONB NOT NULL,
    correlation_id VARCHAR(120),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    retry_count INT NOT NULL DEFAULT 0,
    last_error TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    locked_at TIMESTAMPTZ,
    next_retry_at TIMESTAMPTZ
);

-- Índice parcial optimizado para sondeo no contencioso O(1) con FOR UPDATE SKIP LOCKED
CREATE INDEX IF NOT EXISTS idx_outbox_processing
ON outbox_events (status, created_at ASC)
WHERE status IN ('PENDING', 'PROCESSING');

-- Índice para reintentos programados con backoff exponencial
CREATE INDEX IF NOT EXISTS idx_outbox_retry
ON outbox_events (status, next_retry_at ASC)
WHERE status = 'FAILED' AND next_retry_at IS NOT NULL;

-- 3. Tabla de control de idempotencia para consumidores y workers (TRX-05, SED-05)
CREATE TABLE IF NOT EXISTS processed_events (
    event_id UUID PRIMARY KEY,
    consumer_name VARCHAR(128) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

-- Índice para deduplicación rápida por consumidor
CREATE INDEX IF NOT EXISTS idx_processed_events_consumer
ON processed_events (consumer_name, processed_at ASC);
