-- =============================================================================
-- V2__init_ordering_tables.sql
-- Bounded Context: ORDERING
-- Cumplimiento normativo: OUT-06, SED-06, TRX-02, SHR-02, DB-01..DB-04
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS ordering;
SET search_path TO ordering, shared, public;

-- -----------------------------------------------------------------------------
-- 1. TIPOS ENUMERADOS
-- -----------------------------------------------------------------------------
CREATE TYPE ordering.order_status AS ENUM (
    'PENDING',
    'CONFIRMED',
    'SHIPPED',
    'CANCELLED'
);

-- -----------------------------------------------------------------------------
-- 2. TABLA: orders (Aggregate Root)
-- -----------------------------------------------------------------------------
CREATE TABLE ordering.orders (
    version      BIGINT        NOT NULL DEFAULT 0,
    created_at   TIMESTAMPTZ   NOT NULL DEFAULT clock_timestamp(),
    updated_at   TIMESTAMPTZ   NOT NULL DEFAULT clock_timestamp(),
    amount       NUMERIC(12,2) NOT NULL,
    id           UUID          NOT NULL,
    customer_id  UUID          NOT NULL,
    currency     VARCHAR(3)    NOT NULL,
    status       VARCHAR(32)   NOT NULL DEFAULT 'PENDING',

    CONSTRAINT pk_orders                    PRIMARY KEY (id),
    CONSTRAINT chk_orders_version_non_neg   CHECK (version >= 0),
    CONSTRAINT chk_orders_updated_coherence CHECK (updated_at >= created_at),
    CONSTRAINT chk_orders_amount_positive   CHECK (amount >= 0),
    CONSTRAINT chk_orders_currency_format   CHECK (currency ~ '^[A-Z]{3}$'),
    CONSTRAINT chk_orders_status_valid      CHECK (status IN ('PENDING', 'CONFIRMED', 'SHIPPED', 'CANCELLED'))
) WITH (
    fillfactor = 90
);

-- Índices operativos y de consulta
CREATE INDEX idx_orders_customer ON ordering.orders (customer_id);
CREATE INDEX idx_orders_status   ON ordering.orders (status, created_at DESC);

-- -----------------------------------------------------------------------------
-- 3. AUDIT LOG (Particionado Mensual)
-- -----------------------------------------------------------------------------
CREATE TABLE ordering.audit_log (
    occurred_at    TIMESTAMPTZ       NOT NULL DEFAULT clock_timestamp(),
    actor_type     shared.actor_type NOT NULL DEFAULT 'SYSTEM',
    id             UUID              NOT NULL,
    actor_id       UUID              NOT NULL,
    entity_id      UUID              NOT NULL,
    tenant_id      UUID,
    changes        JSONB             NOT NULL,
    action         VARCHAR(64)       NOT NULL,
    entity_name    VARCHAR(64)       NOT NULL,
    ip_address     INET,
    correlation_id VARCHAR(120),

    CONSTRAINT pk_ordering_audit                 PRIMARY KEY (id, occurred_at),
    CONSTRAINT chk_ordering_audit_changes_object CHECK (jsonb_typeof(changes) = 'object')
) PARTITION BY RANGE (occurred_at);

-- Partición por defecto
CREATE TABLE ordering.audit_log_default
    PARTITION OF ordering.audit_log DEFAULT;

-- Particiones mensuales pre-aprovisionadas
CREATE TABLE ordering.audit_log_y2026m10 PARTITION OF ordering.audit_log
    FOR VALUES FROM ('2026-10-01 00:00:00+00') TO ('2026-11-01 00:00:00+00');

CREATE TABLE ordering.audit_log_y2026m11 PARTITION OF ordering.audit_log
    FOR VALUES FROM ('2026-11-01 00:00:00+00') TO ('2026-12-01 00:00:00+00');

CREATE TABLE ordering.audit_log_y2026m12 PARTITION OF ordering.audit_log
    FOR VALUES FROM ('2026-12-01 00:00:00+00') TO ('2027-01-01 00:00:00+00');

CREATE TABLE ordering.audit_log_y2027m01 PARTITION OF ordering.audit_log
    FOR VALUES FROM ('2027-01-01 00:00:00+00') TO ('2027-02-01 00:00:00+00');

-- Índices de consulta de auditoría
CREATE INDEX idx_ordering_audit_tenant_lookup ON ordering.audit_log (tenant_id, occurred_at DESC)
    WHERE tenant_id IS NOT NULL;
CREATE INDEX idx_ordering_audit_entity_lookup ON ordering.audit_log (entity_name, entity_id, occurred_at DESC);
CREATE INDEX idx_ordering_audit_actor_lookup  ON ordering.audit_log (actor_id, occurred_at DESC);

-- -----------------------------------------------------------------------------
-- 4. POLÍTICAS DE SEGURIDAD (RLS)
-- -----------------------------------------------------------------------------
ALTER TABLE ordering.audit_log ENABLE ROW LEVEL SECURITY;
ALTER TABLE ordering.audit_log FORCE ROW LEVEL SECURITY;
CREATE POLICY rls_ordering_audit_log ON ordering.audit_log
    USING (tenant_id IS NOT DISTINCT FROM (SELECT nullif(current_setting('app.current_tenant_id', true), '')::uuid))
    WITH CHECK (tenant_id IS NOT DISTINCT FROM (SELECT nullif(current_setting('app.current_tenant_id', true), '')::uuid));

-- -----------------------------------------------------------------------------
-- 5. PERMISOS E INMUTABILIDAD
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'template_app') THEN
        GRANT USAGE ON SCHEMA ordering TO template_app;
        GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA ordering TO template_app;
        ALTER DEFAULT PRIVILEGES IN SCHEMA ordering GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO template_app;
        REVOKE UPDATE, DELETE, TRUNCATE ON ordering.audit_log FROM template_app;
    END IF;
END
$$;
