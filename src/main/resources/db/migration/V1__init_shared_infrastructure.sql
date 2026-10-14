-- =============================================================================
-- V1__init_shared_infrastructure.sql
-- Infraestructura Transversal y Kernel Compartido
-- Cumplimiento normativo: SED-05, SED-06, TRX-03, TRX-05, TRX-06, OUT-05, OUT-06
-- =============================================================================

-- -----------------------------------------------------------------------------
-- 1. EXTENSIONES DEL MOTOR
-- -----------------------------------------------------------------------------
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "btree_gist";
CREATE EXTENSION IF NOT EXISTS "pg_trgm";

-- -----------------------------------------------------------------------------
-- 2. ESQUEMA TRANSVERSAL
-- -----------------------------------------------------------------------------
CREATE SCHEMA IF NOT EXISTS shared;
SET search_path TO shared, public;

-- -----------------------------------------------------------------------------
-- 3. DOMINIOS Y VALIDACIONES DE TIPO
-- -----------------------------------------------------------------------------
-- Código ISO-4217 de divisa (tres letras mayúsculas)
CREATE DOMAIN shared.currency AS VARCHAR(3)
    CONSTRAINT chk_currency_iso_format CHECK (VALUE ~ '^[A-Z]{3}$');

-- Teléfono en formato internacional E.164 (+ seguido de 1 a 14 dígitos)
CREATE DOMAIN shared.phone_e164 AS VARCHAR(20)
    CONSTRAINT chk_phone_e164_format CHECK (VALUE ~ '^\+[1-9]\d{1,14}$');

-- Formato canónico de correo electrónico RFC 5322 simplificado
CREATE DOMAIN shared.email AS VARCHAR(255)
    CONSTRAINT chk_email_format CHECK (VALUE ~ '^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\.[a-zA-Z]{2,}$');

-- Identificador semántico en kebab-case
CREATE DOMAIN shared.kebab_code AS VARCHAR(64)
    CONSTRAINT chk_kebab_code_format CHECK (VALUE ~ '^[a-z0-9]+(-[a-z0-9]+)*$');

-- Código de país ISO 3166-1 alfa-2 (dos letras mayúsculas)
CREATE DOMAIN shared.country_code AS VARCHAR(2)
    CONSTRAINT chk_country_code_format CHECK (VALUE ~ '^[A-Z]{2}$');

-- Código de idioma ISO 639-1 / BCP-47
CREATE DOMAIN shared.language AS VARCHAR(5)
    CONSTRAINT chk_language_format CHECK (VALUE ~ '^[a-z]{2}(-[A-Z]{2})?$');

-- Hash criptográfico SHA-256 en hexadecimal (64 caracteres minúsculos)
CREATE DOMAIN shared.sha256_hex AS VARCHAR(64)
    CONSTRAINT chk_sha256_hex_format CHECK (VALUE ~ '^[0-9a-f]{64}$');

-- -----------------------------------------------------------------------------
-- 4. TIPOS ENUMERADOS GLOBALES
-- -----------------------------------------------------------------------------
CREATE TYPE shared.actor_type AS ENUM (
    'ANONYMOUS',
    'CLIENT',
    'TENANT_USER',
    'SAAS_ADMIN',
    'SYSTEM',
    'API_CLIENT'
);

CREATE TYPE shared.vehicle_type AS ENUM (
    'SEDAN',
    'ESTATE',
    'VAN',
    'EXECUTIVE',
    'ACCESSIBLE',
    'ECO'
);

CREATE TYPE shared.payment_method AS ENUM (
    'CASH',
    'CREDIT_CARD',
    'IN_APP_WALLET',
    'CORPORATE_ACCOUNT',
    'VOUCHER'
);

CREATE TYPE shared.outbox_status AS ENUM (
    'PENDING',
    'PROCESSING',
    'DELIVERED',
    'FAILED',
    'DEAD_LETTER'
);

-- -----------------------------------------------------------------------------
-- 5. TABLA: outbox_events (Transactional Outbox)
-- -----------------------------------------------------------------------------
CREATE TABLE public.outbox_events (
    created_at     TIMESTAMPTZ          NOT NULL DEFAULT clock_timestamp(),
    next_retry_at  TIMESTAMPTZ,
    locked_at      TIMESTAMPTZ,
    delivered_at   TIMESTAMPTZ,
    status         shared.outbox_status NOT NULL DEFAULT 'PENDING',
    retry_count    INTEGER              NOT NULL DEFAULT 0,
    event_id       UUID                 NOT NULL,
    tenant_id      UUID,
    payload        JSONB                NOT NULL,
    aggregate_type VARCHAR(64)          NOT NULL,
    aggregate_id   VARCHAR(64)          NOT NULL,
    event_type     VARCHAR(128)         NOT NULL,
    correlation_id VARCHAR(120),
    last_error     TEXT,

    CONSTRAINT pk_outbox_events                 PRIMARY KEY (event_id),
    CONSTRAINT chk_outbox_events_retry_count    CHECK (retry_count >= 0),
    CONSTRAINT chk_outbox_events_payload_object CHECK (jsonb_typeof(payload) = 'object'),
    CONSTRAINT chk_outbox_events_delivered      CHECK ((status = 'DELIVERED') = (delivered_at IS NOT NULL)),
    CONSTRAINT chk_outbox_events_processing     CHECK (status <> 'PROCESSING' OR locked_at IS NOT NULL),
    CONSTRAINT chk_outbox_events_failed         CHECK (status <> 'FAILED' OR (next_retry_at IS NOT NULL AND last_error IS NOT NULL)),
    CONSTRAINT chk_outbox_events_dead_letter    CHECK (status <> 'DEAD_LETTER' OR last_error IS NOT NULL)
) WITH (
    fillfactor = 80,
    autovacuum_vacuum_scale_factor = 0.01,
    autovacuum_vacuum_cost_limit = 2000,
    autovacuum_analyze_scale_factor = 0.02
);

COMMENT ON TABLE public.outbox_events IS 'Tabla centralizada de Transactional Outbox para publicación de eventos de dominio.';
COMMENT ON COLUMN public.outbox_events.event_id IS 'Identificador UUIDv7 único del evento outbox.';
COMMENT ON COLUMN public.outbox_events.tenant_id IS 'Identificador multitenant del contexto de ejecución si aplica.';
COMMENT ON COLUMN public.outbox_events.aggregate_type IS 'Nombre discriminador del agregado emisor del evento.';
COMMENT ON COLUMN public.outbox_events.aggregate_id IS 'Identificador de la instancia del agregado de dominio.';
COMMENT ON COLUMN public.outbox_events.event_type IS 'Nombre canónico o FQCN del evento de dominio para deserialización reflexiva.';
COMMENT ON COLUMN public.outbox_events.payload IS 'Payload del evento serializado en formato binario JSONB.';
COMMENT ON COLUMN public.outbox_events.correlation_id IS 'Identificador de correlación distribuida para trazabilidad transversal.';
COMMENT ON COLUMN public.outbox_events.status IS 'Estado del ciclo de vida del evento outbox (PENDING, PROCESSING, DELIVERED, FAILED, DEAD_LETTER).';
COMMENT ON COLUMN public.outbox_events.retry_count IS 'Contador acumulado de intentos fallidos de despacho.';
COMMENT ON COLUMN public.outbox_events.last_error IS 'Descripción o mensaje del último fallo registrado durante el despacho.';
COMMENT ON COLUMN public.outbox_events.created_at IS 'Marca temporal de persistencia del evento con zona horaria UTC.';
COMMENT ON COLUMN public.outbox_events.locked_at IS 'Marca temporal de adquisición de bloqueo de procesamiento exclusivo.';
COMMENT ON COLUMN public.outbox_events.next_retry_at IS 'Marca temporal programada para el siguiente reintento tras fallo con backoff.';
COMMENT ON COLUMN public.outbox_events.delivered_at IS 'Marca temporal de confirmación de entrega exitosa.';

-- Índices operativos (SKIP LOCKED y reintentos)
CREATE INDEX idx_outbox_events_pending
    ON public.outbox_events (created_at ASC, event_id ASC)
    WHERE status = 'PENDING';

CREATE INDEX idx_outbox_events_processing
    ON public.outbox_events (status, created_at ASC)
    WHERE status IN ('PENDING', 'PROCESSING');

CREATE INDEX idx_outbox_events_retry
    ON public.outbox_events (next_retry_at ASC, event_id ASC)
    WHERE status = 'FAILED' AND next_retry_at IS NOT NULL;

CREATE INDEX idx_outbox_events_stale_locks
    ON public.outbox_events (locked_at ASC)
    WHERE status = 'PROCESSING';

CREATE INDEX idx_outbox_events_purge
    ON public.outbox_events (delivered_at ASC)
    WHERE status = 'DELIVERED';

CREATE INDEX idx_outbox_events_dlq
    ON public.outbox_events (created_at DESC)
    WHERE status = 'DEAD_LETTER';

CREATE INDEX idx_outbox_events_aggregate
    ON public.outbox_events (aggregate_id, created_at DESC);

CREATE INDEX idx_outbox_events_tenant
    ON public.outbox_events (tenant_id, created_at DESC)
    WHERE tenant_id IS NOT NULL;

-- -----------------------------------------------------------------------------
-- 6. TABLA: processed_events (Compuerta de Idempotencia)
-- -----------------------------------------------------------------------------
CREATE TABLE public.processed_events (
    processed_at  TIMESTAMPTZ  NOT NULL DEFAULT clock_timestamp(),
    event_id      UUID         NOT NULL,
    consumer_name VARCHAR(128) NOT NULL,

    CONSTRAINT pk_processed_events        PRIMARY KEY (event_id),
    CONSTRAINT uq_processed_events_target UNIQUE (event_id, consumer_name)
) WITH (
    autovacuum_vacuum_scale_factor = 0.02,
    autovacuum_analyze_scale_factor = 0.02
);

COMMENT ON TABLE public.processed_events IS 'Registro de deduplicación de eventos para garantizar idempotencia en consumidores.';
COMMENT ON COLUMN public.processed_events.event_id IS 'UUID del evento recibido garantizado único por consumidor.';
COMMENT ON COLUMN public.processed_events.consumer_name IS 'Nombre del consumidor o worker que ejecuta la compuerta de idempotencia.';
COMMENT ON COLUMN public.processed_events.processed_at IS 'Marca temporal de procesamiento exitoso con zona horaria UTC.';

-- Índices de consulta y purga TTL
CREATE INDEX idx_processed_events_purge
    ON public.processed_events (processed_at ASC);

CREATE INDEX idx_processed_events_consumer
    ON public.processed_events (consumer_name, processed_at DESC);

-- -----------------------------------------------------------------------------
-- 7. PERMISOS
-- -----------------------------------------------------------------------------
DO $$
BEGIN
    IF EXISTS (SELECT FROM pg_roles WHERE rolname = 'template_app') THEN
        GRANT USAGE ON SCHEMA shared TO template_app;
        GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA shared TO template_app;
        ALTER DEFAULT PRIVILEGES IN SCHEMA shared GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO template_app;
    END IF;
END
$$;
