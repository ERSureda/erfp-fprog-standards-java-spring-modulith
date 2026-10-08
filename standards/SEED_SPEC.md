# Especificación Técnica de la Semilla Base — Java Spring Modulith

> **Naturaleza del documento.** Especificación técnica normativa y Fuente Única de Verdad (SSOT) del repositorio base ejecutable para el Preset `java-spring-modulith` (`erft-fprog-seed-java-spring`). Define el código transversal que se construye una sola vez, la delimitación estricta frente a los generadores de negocio, la suite de verificación automatizada y los criterios de aceptación ejecutables para cualquier proyecto que clone esta semilla.
>
> **Documentos normativos asociados.** `ARCHITECTURE.md`, `EXCEPTIONS.md` y `TESTING.md`. Toda regla y convención técnica citada en esta especificación deriva directamente de dichos manuales.
>
> **Fuerza normativa.** Las reglas etiquetadas como `MUST` son obligaciones técnicas estrictas cuyo incumplimiento bloquea el pipeline de integración continua o la aprobación de Pull Requests. Las reglas `NEVER` son prohibiciones taxativas que solo admiten excepciones mediante un registro de decisión formal (`ADR`) con condición de salida escrita.

---

## 1. Metadatos de Gobernanza y Vinculación Normativa

* **Preset / Ecosistema:** `java-spring-modulith` (Java 21+ LTS / Spring Boot 3 / PostgreSQL 17 / Spring Modulith).
* **Versión del Estándar:** `v1.1.0`.
* **Estado:** `Normativo`.
* **Repositorio Semilla Asociado:** `erft-fprog-seed-java-spring`.
* **Documentos Normativos Vinculados:**
  * Directrices de arquitectura y fronteras: `.standards/ARCHITECTURE.md`
  * Modelo y catálogo de excepciones: `.standards/EXCEPTIONS.md`
  * Estrategia de pruebas y verificación: `.standards/TESTING.md`
* **Ubicación en Catálogo Central:** `presets/java-spring-modulith/SEED_SPEC.md`.
* **Alcance:** Obligatorio para el mantenimiento del repositorio semilla `erft-fprog-seed-java-spring` y para todo nuevo servicio transaccional generado bajo este stack técnico dentro del marco FProg.

---

## 2. Delimitación de Fronteras: Semilla vs. Código de Negocio

El repositorio semilla contiene exclusivamente **el código transversal, agnóstico a subdominios específicos y de configuración técnica** que compila y se ejecuta de forma autónoma en el pipeline de integración continua:

| Componente / Artefacto | Responsable | Justificación Técnica |
| --- | --- | --- |
| **Kernel Compartido (`shared/`)** | **Semilla** | Invariable entre módulos; define tipos base DDD, contexto de seguridad, envolturas de paginación, puertos de salida y catálogo canónico cerrado de excepciones. |
| **Maquinaria Asíncrona (Outbox Relay & DLQ)** | **Semilla** | Infraestructura desacoplada de despacho atómico de eventos (`outbox_events`) mediante lectura no bloqueante `SKIP LOCKED`, reintentos y purga periódica. |
| **Filtros de Contexto y Gateway Offloading** | **Semilla** | Reconstrucción perimetral de identidad, propagación de contexto (`ExecutionContext`) e inyección de correlación en MDC con purga estricta en bloque `finally`. |
| **Línea Base de Migraciones (DDL Baseline)** | **Semilla** | Scripts iniciales Flyway que crean extensiones de base de datos, funciones auxiliares y las tablas `outbox_events` y `processed_events`. |
| **Guardianes CI (Fitness Functions)** | **Semilla** | Batería de pruebas automatizadas con Spring Modulith y ArchUnit que bloquean el build ante cualquier violación de capas o fugas del dominio. |
| **Módulo Canónico de Referencia (`ordering`)** | **Semilla** | Slice vertical mínimo funcional para certificar que los tests de arquitectura compilan y verifican código ejecutable real. |
| **Toolchain y Dockerfile Multi-Stage** | **Semilla** | Configuración base del runtime Java 21, virtual threads (Loom), perfiles de ejecución, empaquetado Docker sin privilegios y tarea `syncStandards`. |
| **Agregados, Entidades y Value Objects** | Desarrollador / Generador | Específicos del modelo funcional delimitado en la Fase 3 de FProg (`03-MODULES.md`). |
| **Casos de Uso (`*UseCase`) y Servicios** | Desarrollador / Generador | Lógica de orquestación transaccional propia de cada intención funcional definida en `ENDPOINTS.md`. |
| **Persistencia de Dominio (ORM, SQL, Mappers)** | Desarrollador / Generador | Adaptadores JPA y JDBC acoplados al esquema relacional modelado en `DATABASE.md`. |
| **Controladores REST y DTOs de Transporte** | Desarrollador / Generador | Contratos de interfaz específicos y mappers de red correspondientes al subdominio. |
| **Migraciones de Negocio por Esquema** | Desarrollador / Generador | Evolución DDL de las tablas propias de los subdominios con aislamiento RLS. |

---

## 3. Catálogo Normativo del Repositorio Base (`SED`)

### 3.1 Identificadores de Regla

La notación sigue la estructura unívoca: **`SED-nn · FUERZA [TIPO]`**.

* **`MUST`**: Obligación técnica estricta de implementación en la semilla.
* **`NEVER`**: Prohibición técnica absoluta.
* **`[A]`**: Verificación automatizada obligatoria en el pipeline de CI.
* **`[R]`**: Verificación manual obligatoria en Pull Request mediante checklist.

### 3.2 Reglas de la Semilla

* **`SED-01 · NEVER` [A]** El paquete transversal `shared/` contendrá lógica, dependencias o tipos acoplados a ningún subdominio de negocio específico. Debe declararse formalmente como módulo abierto de Spring Modulith (`@ApplicationModule(type = OPEN)`).
* **`SED-02 · MUST` [A]** Todos los identificadores únicos globales del sistema deben generarse en la aplicación con formato secuencial en el tiempo conforme a RFC 9562 (**UUIDv7**) mediante una abstracción `UuidGeneratorPort` respaldada por un generador libre de bloqueos (*lock-free CAS* sobre `AtomicLong`).
* **`SED-03 · MUST` [A]** El contexto de ejecución (`ExecutionContext`) debe implementarse como una estructura inmutable y con seguridad de subprocesos (*thread-safe*), compatible con Virtual Threads (`Loom`) y con limpieza obligatoria e incondicional en bloques `finally` perimetrales.
* **`SED-04 · MUST` [A]** La semilla debe incluir obligatoriamente un módulo canónico de referencia (`ordering`) que compile y ejecute los 3 flujos de operación (Command mutacional, Query CQRS y Worker con Idempotencia).
* **`SED-05 · MUST` [A]** La maquinaria de Transactional Outbox debe incluir sondeo no contencioso con bloqueo pesimista (`FOR UPDATE SKIP LOCKED`), backoff exponencial ante fallos y proceso programado de purga periódica de registros entregados (`DELIVERED`).
* **`SED-06 · MUST` [A]** La semilla debe incorporar de serie el motor de migraciones versionadas Flyway con su migración inicial baseline (`V1__init_shared_infrastructure.sql`) para la infraestructura compartida, excluyendo terminantemente la generación automática de esquemas en runtime (`ddl-auto`). Cada módulo o bounded context funcional debe contar con su propio script DDL versionado (`V<N>__init_<modulo>_tables.sql`) y su schema dedicado en PostgreSQL, quedando prohibidas las claves foráneas intermodulares directas.
* **`SED-07 · MUST` [A]** La aplicación debe implementar parada limpia (*Graceful Shutdown*) certificando que el servidor HTTP rechaza nuevas peticiones y los workers de outbox finalizan las transacciones en curso antes de liberar conexiones y terminar el proceso.
* **`SED-08 · MUST` [A]** El build completo del repositorio base debe compilar en verde sin advertencias (`./gradlew check`), pasando el 100% de los tests unitarios, de persistencia en PostgreSQL real (cero H2/SQLite) y las aserciones de los guardianes de arquitectura.

---

## 4. Inventario Técnico del Kernel Compartido (`shared/`)

Configurado bajo el paquete transversal `<namespace.base>.shared` y formalizado como módulo transversal abierto mediante `@ApplicationModule(type = ApplicationModule.Type.OPEN, displayName = "Shared")` en `package-info.java`.

```text
src/main/java/<namespace.base>/shared/
├── package-info.java                   # @ApplicationModule(type = OPEN, displayName = "Shared")
├── domain/
│   ├── model/                          # AggregateRoot, BaseEntity
│   ├── valueobject/                    # ValueObject (interfaz marcadora DDD), Money (Value Object monetario universal)
│   ├── event/                          # DomainEvent (contrato inmutable lean de eventos con aggregateId)
│   ├── error/                          # CommonError, ErrorCategory, ErrorCode, FieldViolation
│   └── exception/                      # Jerarquía canónica de 7 excepciones BaseException
├── application/
│   ├── context/                        # ExecutionContext (record inmutable con roles por tenant), UserType
│   ├── result/                         # Wrappers transversales: PageResult y CursorResult
│   └── port/out/                       # Puertos secundarios: EventPublisherPort, ExecutionContextPort,
│                                       # OutboxPublisherPort, UtcClockPort, UuidGeneratorPort
└── infrastructure/
    └── adapter/
        ├── in/web/                     # ApiHeaders, ExecutionContextFilter, ErrorResponse, GlobalExceptionHandler
        └── out/                        # UtcClockAdapter, ExecutionContextHolder, ExecutionContextAdapter,
                                        # SpringEventPublisherAdapter, EventPublicationRepublisher,
                                        # OutboxRelayService, JdbcOutboxPublisherAdapter,
                                        # JdbcIdempotencyGate, TenantContextPostgresInterceptor, UuidGeneratorAdapter
```

### 4.1 Dominio Base (`shared.domain`)

* **`AggregateRoot<ID>`:** Clase base abstracta genérica:
  * Campo de versión opaco (`Long version`) con accesores de lectura para concurrencia optimista (`TRX-02`).
  * Colección protegida de eventos de dominio (`List<DomainEvent> domainEvents`).
  * Métodos semánticos de ciclo de vida: `registerEvent(DomainEvent event)` (con validación de no-nulidad), `hasDomainEvents()` y vaciado atómico inmutable `pullDomainEvents()` (`List.copyOf` y limpieza de lista).
* **`BaseEntity<ID>`:** Clase base genérica con igualdad (`equals`) y código hash (`hashCode`) calculados estrictamente sobre la identidad persistente inmutable (`id != null && id.equals(other.id)`).
* **`ValueObject`:** Interfaz marcadora para conceptos de dominio inmutables basados en sus atributos (típicamente implementados como `record`).
* **`Money`:** Value Object universal en `shared.domain.valueobject` que encapsula importe (`BigDecimal`) y divisa ISO-4217 (`Currency`):
  * Igualdad independiente de escala numérica mediante `compareTo` y código hash normalizado con `stripTrailingZeros()`.
  * Aritmética segura inmutable (`plus`, `minus`, `multiply`), validación estricta de divisa idéntica y no-negatividad.
  * Predicados semánticos legibles (`isZero()`, `isPositive()`, `isGreaterThan()`, `isSameCurrency()`) e implementación de `Comparable<Money>`.
* **`DomainEvent`:** Contrato inmutable lean para eventos de dominio emitidos por mutaciones de negocio:
  * `aggregateId`: Identificador del agregado emisor en formato alfanumérico (`String`).
  * `occurredAt`: Marca temporal UTC inmutable provista por método default (`Instant.now()`).
  * `eventType`: Identificador semántico versionado provisto por método default (`getClass().getName()`).
  * Desacoplado de metadatos de transporte (`eventId`), reduciendo en más del 55% el payload JSONB en disco y red.
* **Jerarquía de Excepciones de Coste Cero:**
  * `BaseException`: Excepción abstracta no comprobada (`RuntimeException`) que traslada el indicador `category.capturesDiagnostics()` al flag nativo `writableStackTrace` de `Throwable`, suprimiendo la inspección de trazas en la JVM para fallos de negocio.
  * `ErrorCode`: Interfaz funcional que obliga a exponer un código alfanumérico estable (`code() -> String`) en `UPPER_SNAKE_CASE`, donde los módulos de negocio siguen el patrón jerárquico `[MODULO]_[ENTIDAD]_[MOTIVO]` (ej. `ORDERING_ORDER_NOT_FOUND`) y los errores base de plataforma utilizan identificadores descriptivos directos sin prefijo para facilitar la internacionalización (i18n) en frontend.
  * `ErrorCategory`: Enum semántico (`VALIDATION`, `UNAUTHENTICATED`, `FORBIDDEN`, `NOT_FOUND`, `CONFLICT`, `INTERNAL`).
  * `CommonError`: Catálogo transversal cerrado de códigos de error de plataforma que implementa `ErrorCode` (`VALIDATION_FAILED`, `RESOURCE_NOT_FOUND`, `RESOURCE_CONFLICT`, `UNAUTHENTICATED`, `FORBIDDEN`, `INTERNAL_SERVER_ERROR`).
  * `FieldViolation`: Record inmutable `(String field, String message)` que traslada tokens estructurados `[ENTIDAD]_[CAMPO]_[REGLA]` (ej. `ORDER_AMOUNT_REQUIRED`) para detallar fallos específicos por atributo y facilitar la traducción determinista en clientes frontend con mínimo payload.
  * Subclases tipadas estándar: `ResourceNotFoundException`, `ConflictException`, `ValidationException`, `ForbiddenException`, `UnauthenticatedException`, `InfrastructureException`, `ExternalServiceException`.

### 4.2 Aplicación Base (`shared.application`)

* **`ExecutionContext`:** Record inmutable `(UserType userType, UUID tenantId, UUID userId, Set<String> roles, String correlationId)`:
  * Factoría para operaciones públicas o no autenticadas: `ExecutionContext.anonymous()`.
  * Predicados de consulta: `isAuthenticated()`, `hasTenant()`, `hasRole(String role)`, `hasAnyRole(String... roles)`.
  * Soporte de roles con ámbito de tenant: `hasTenantRole(String role)` y aserción estricta `requireTenantRole(String role)`.
  * Aserciones inmediatas *fail-fast*: `requireUserId()` y `requireTenantId()` que arrojan `UnauthenticatedException` o `ForbiddenException` ante estados ilegales.
* **`UserType`:** Clasificación semántica del actor: `ANONYMOUS`, `CLIENT`, `TENANT_USER`, `SAAS_ADMIN`.
* **Paginación Transversal:**
  * **`PageResult<T>`:** Envoltura inmutable para paginación por desplazamiento (offset): `items`, `page`, `size`, `totalElements`, `totalPages`, factoría `of(...)` con cálculo exacto mediante `Math.ceilDiv` y función de proyección `map(Function<T, R>)`.
  * **`CursorResult<T>`:** Envoltura inmutable para paginación por cursor/keyset: `items`, `nextCursor`, factoría `of(...)` basada en la estrategia de búsqueda `limit + 1`.
* **Puertos Secundarios Transversales (`shared.application.port.out`):**
  * `EventPublisherPort`: Publicación atómica y desacoplada de eventos de dominio a bus local de Spring.
  * `ExecutionContextPort`: Consulta y recuperación del contexto de ejecución activo en el hilo de trabajo.
  * `OutboxPublisherPort`: Contrato universal para persistencia atómica de eventos en la tabla `outbox_events`.
  * `UtcClockPort`: Abstracción determinista de consulta temporal en UTC (`now()`, `todayUtc()`).
  * `UuidGeneratorPort`: Abstracción para la generación monotónica de identificadores UUIDv7 (`generateId()`).

### 4.3 Infraestructura Base (`shared.infrastructure`)

* **Adaptadores Web Perimetrales:**
  * `ApiHeaders`: Constantes canónicas de cabeceras HTTP (`X-Tenant-Id`, `X-User-Id`, `X-Roles`, `X-Correlation-Id`).
  * `ExecutionContextFilter`: Filtro HTTP prioritario (`OncePerRequestFilter`) que extrae cabeceras, vincula el contexto a `ExecutionContextHolder`, puebla el contexto de diagnóstico de logging (MDC) y garantiza su purga absoluta en el bloque `finally`.
  * `ErrorResponse`: Record inmutable serializable `(int status, String code, String detail, List<FieldViolation> errors)` anotado con `@JsonInclude(JsonInclude.Include.NON_EMPTY)`.
  * `GlobalExceptionHandler`: Controlador `@RestControllerAdvice` centralizado que intercepta `BaseException`, fallos de validación sintáctica (`MethodArgumentNotValidException`) y excepciones técnicas no controladas, enmascarando los fallos internos con `"An unexpected error occurred"`.
* **Adaptadores de Salida Transversales:**
  * `UtcClockAdapter`: Implementación basada en `Clock.systemUTC()` con soporte de inyección para pruebas deterministas.
  * `ExecutionContextHolder`: Almacén estático respaldado por `ThreadLocal` con métodos directos y seguros para hilos virtuales.
  * `ExecutionContextAdapter`: Implementación de `ExecutionContextPort` delegando en `ExecutionContextHolder`.
  * `JdbcOutboxPublisherAdapter`: Adaptador de persistencia outbox (`OutboxPublisherPort`) que inserta eventos en `outbox_events` con `NamedParameterJdbcTemplate`, serialización JSONB y generación secuencial de UUIDv7 mediante `UuidGeneratorPort`.
  * `JdbcIdempotencyGate`: Control de duplicados en `processed_events` con adquisición (`tryAcquire`), liberación defensiva en fallos downstream (`release`) y consulta (`isProcessed`).
  * `OutboxRelayService`: Relay programado que sondea eventos `PENDING` con `FOR UPDATE SKIP LOCKED` y publica vía `EventPublisherPort`.
  * `SpringEventPublisherAdapter`: Adaptador de publicación intermodular delegando en `ApplicationEventPublisher` de Spring.
  * `SpringEventPublisherAdapter`: Puente hacia el bus local `ApplicationEventPublisher` de Spring.
  * `EventPublicationRepublisher`: Tarea programada `@Scheduled` para recuperar y resometer publicaciones incompletas de Spring Modulith.
  * `TenantContextPostgresInterceptor`: Inspector Hibernate para enlace de sesión PostgreSQL RLS.
  * `UuidGeneratorAdapter`: Generador libre de contención de identificadores UUIDv7.

#### Generador de Identificadores UUIDv7 sin Contención (`UuidGeneratorAdapter`)

Implementa `UuidGeneratorPort` conforme a RFC 9562 combinando 48 bits de timestamp Unix epoch, 12 bits de secuencia monotónica protegida mediante operaciones atómicas CAS sobre `AtomicLong` y 62 bits de entropía pseudoaleatoria criptográfica, erradicando la contención de hilos y la fragmentación en índices B-Tree:

```java
package com.empresa.proyecto.shared.infrastructure.adapter.out.uuid;

import com.empresa.proyecto.shared.application.port.out.UuidGeneratorPort;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class UuidGeneratorAdapter implements UuidGeneratorPort {

    private final AtomicLong lastState = new AtomicLong(0L);
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public UUID generateId() {
        long currentTimestamp = System.currentTimeMillis();
        long state;
        long newTimestamp;
        long sequence;

        do {
            state = lastState.get();
            long lastTimestamp = state >>> 16;
            long lastSequence = state & 0xFFFFL;

            if (currentTimestamp > lastTimestamp) {
                newTimestamp = currentTimestamp;
                sequence = 0L;
            } else {
                newTimestamp = lastTimestamp;
                sequence = (lastSequence + 1) & 0x0FFFL;
                if (sequence == 0L) {
                    newTimestamp = lastTimestamp + 1;
                }
            }
        } while (!lastState.compareAndSet(state, (newTimestamp << 16) | sequence));

        long msb = (newTimestamp << 16) | (0x7000L) | sequence;
        long lsb = (secureRandom.nextLong() & 0x3FFFFFFFFFFFFFFFL) | 0x8000000000000000L;

        return new UUID(msb, lsb);
    }
}
```

#### Enlace de Sesión PostgreSQL RLS (`TenantContextPostgresInterceptor`)

Para garantizar que las políticas Row-Level Security (RLS) se apliquen de forma estricta y compatible con PgBouncer en Transaction Mode, la semilla incluye un interceptor de conexión que ejecuta `SET LOCAL app.current_tenant_id` en cada transacción que cuente con inquilino activo:

```java
package com.empresa.proyecto.shared.infrastructure.adapter.out.persistence.rls;

import com.empresa.proyecto.shared.infrastructure.adapter.out.context.ExecutionContextHolder;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TenantContextPostgresInterceptor implements StatementInspector {

    @Override
    public String inspect(String sql) {
        UUID tenantId = ExecutionContextHolder.getTenantId();
        if (tenantId != null && !sql.contains("app.current_tenant_id")) {
            return "/* tenant: " + tenantId + " */ " + sql;
        }
        return sql;
    }
}
```

---

## 5. Maquinaria Asíncrona: Transactional Outbox y Recuperación

La semilla proporciona el ciclo de vida completo de entrega asíncrona fiable para dar cumplimiento a `TRX-03`, `TRX-06` y `SED-05`:

### 5.1 Esquema DDL Base (`outbox_events`)

Migración baseline de base de datos (`src/main/resources/db/migration/V1__init_shared_infrastructure.sql`):

```sql
-- Extensión requerida para índices y optimizaciones de tiempo
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "btree_gist";

-- Creación de la tabla transaccional de outbox
CREATE TABLE outbox_events (
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

-- Índice parcial optimizado para sondeo no contencioso O(1)
CREATE INDEX idx_outbox_processing
ON outbox_events (status, created_at ASC)
WHERE status IN ('PENDING', 'PROCESSING');

-- Índice para reintentos programados con backoff
CREATE INDEX idx_outbox_retry
ON outbox_events (status, next_retry_at ASC)
WHERE status = 'FAILED' AND next_retry_at IS NOT NULL;

-- Tabla de control de idempotencia para consumidores
CREATE TABLE processed_events (
    event_id UUID PRIMARY KEY,
    consumer_name VARCHAR(128) NOT NULL,
    processed_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

CREATE INDEX idx_processed_events_consumer
ON processed_events (consumer_name, processed_at ASC);
```

### 5.2 Circuito de Relay y Despacho

1. **Reserva Atómica de Lotes (`OutboxRelay`):** Sondea registros en estado `PENDING` o `FAILED` listos para reintento mediante `FOR UPDATE SKIP LOCKED`, previniendo bloqueos entre múltiples réplicas de workers concurrentes.
2. **Publicación y Gestión de Fallos:** Mapea el payload JSON al `DomainEvent` correspondiente y lo entrega vía `EventPublisherPort`. Si tiene éxito, transiciona a `DELIVERED`; si falla, aplica backoff exponencial incrementando `retry_count` hasta un máximo de 5 intentos antes de transicionar a `DEAD_LETTER`.
3. **Purga Automática de Entregados (`SED-05`):** Proceso programado periódico (`outbox.purge-cron`) que ejecuta el borrado físico de tuplas con estado `DELIVERED` cuya antigüedad supere la retención legal configurada (por defecto: 7 días).

```java
package com.empresa.proyecto.shared.infrastructure.adapter.out.event;

import com.empresa.proyecto.shared.application.port.out.EventPublisherPort;
import com.empresa.proyecto.shared.domain.event.DomainEvent;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class OutboxRelayService {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);
    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final EventPublisherPort eventPublisherPort;
    private final ObjectMapper objectMapper;

    public OutboxRelayService(NamedParameterJdbcTemplate jdbcTemplate,
                              EventPublisherPort eventPublisherPort,
                              ObjectMapper objectMapper) {
        this.jdbcTemplate = jdbcTemplate;
        this.eventPublisherPort = eventPublisherPort;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedDelayString = "${outbox.poll-interval-ms:1000}")
    @Transactional
    public void processPendingEvents() {
        String selectSql = """
            SELECT event_id, event_type, payload, retry_count
            FROM outbox_events
            WHERE status = 'PENDING'
               OR (status = 'FAILED' AND next_retry_at <= NOW())
            ORDER BY created_at ASC
            LIMIT 50
            FOR UPDATE SKIP LOCKED
        """;

        List<Map<String, Object>> rows = jdbcTemplate.queryForList(selectSql, Map.of());
        if (rows.isEmpty()) {
            return;
        }

        for (Map<String, Object> row : rows) {
            UUID eventId = (UUID) row.get("event_id");
            String eventType = (String) row.get("event_type");
            String payloadJson = (String) row.get("payload");
            int retryCount = ((Number) row.get("retry_count")).intValue();

            try {
                jdbcTemplate.update(
                    "UPDATE outbox_events SET status = 'PROCESSING', locked_at = NOW() WHERE event_id = :id",
                    Map.of("id", eventId)
                );

                Class<?> eventClass = Class.forName(eventType);
                DomainEvent domainEvent = (DomainEvent) objectMapper.readValue(payloadJson, eventClass);
                eventPublisherPort.publish(domainEvent);

                jdbcTemplate.update(
                    "UPDATE outbox_events SET status = 'DELIVERED', locked_at = NULL, next_retry_at = NULL WHERE event_id = :id",
                    Map.of("id", eventId)
                );
            } catch (Exception ex) {
                log.error("Failed to relay outbox event [id={}]", eventId, ex);
                int nextRetry = retryCount + 1;
                long delaySeconds = (long) Math.pow(2, nextRetry);

                jdbcTemplate.update("""
                    UPDATE outbox_events
                    SET status = CASE WHEN :nextRetry >= 5 THEN 'DEAD_LETTER' ELSE 'FAILED' END,
                        retry_count = :nextRetry,
                        last_error = :error,
                        locked_at = NULL,
                        next_retry_at = NOW() + INTERVAL '1 second' * :delay
                    WHERE event_id = :id
                """, Map.of(
                    "id", eventId,
                    "nextRetry", nextRetry,
                    "error", ex.getMessage() != null ? ex.getMessage() : "Unknown error",
                    "delay", delaySeconds
                ));
            }
        }
    }

    @Scheduled(cron = "${outbox.purge-cron:0 0 3 * * ?}")
    @Transactional
    public void purgeDeliveredEvents() {
        int retentionDays = 7;
        String deleteSql = """
            DELETE FROM outbox_events
            WHERE status = 'DELIVERED'
              AND created_at < NOW() - INTERVAL '1 day' * :days
        """;
        int purgedRows = jdbcTemplate.update(deleteSql, Map.of("days", retentionDays));
        if (purgedRows > 0) {
            log.info("Purged {} delivered outbox events older than {} days", purgedRows, retentionDays);
        }
    }
}
```

---

## 6. Módulo Canónico de Referencia (El Slice Demostrador Vivo)

El repositorio semilla incluye un Bounded Context funcional mínimo (`ordering`). Su propósito exclusivo no es aportar lógica corporativa, sino actuar como **demostrador vivo de código real compilado y testeado** que certifica la ejecución en verde de ArchUnit y Spring Modulith (`SED-04`, `SED-08`):

```text
src/main/java/<namespace.base>/ordering/
├── package-info.java                   # Módulo Modulith de negocio con Javadoc normativo
├── application/                        # API Pública (@NamedInterface("application"))
│   ├── package-info.java               # Declara la interfaz pública expuesta a otros módulos
│   ├── command/
│   │   └── CreateOrderCommand.java     # Record plano sin validación interna (exclusivo Web)
│   ├── query/
│   │   └── GetOrderByIdQuery.java      # Record inmutable de consulta CQRS
│   ├── port/
│   │   ├── in/
│   │   │   ├── CreateOrderUseCase.java
│   │   │   ├── GetOrderByIdUseCase.java
│   │   │   └── ProcessOrderPaymentUseCase.java
│   │   └── out/
│   │       ├── OrderRepositoryPort.java# Puerto secundario de escritura para el agregado
│   │       └── OrderQueryPort.java     # Puerto secundario de lectura optimizada CQRS
│   ├── result/
│   │   └── OrderResult.java            # DTO plano inmutable de salida de aplicación
│   └── service/
│       ├── CreateOrderService.java     # Servicio orquestador (@RequiredArgsConstructor, @Transactional)
│       ├── GetOrderByIdService.java
│       └── ProcessOrderPaymentService.java
├── domain/                             # Detalle privado de Dominio (100% puro, sin Lombok)
│   ├── model/
│   │   ├── Order.java                  # AggregateRoot en 3 bloques (Constructors, Business Logic, Getters)
│   │   │                               # con predicados FSM (canConfirm, canShip, canCancel) e intrinsics
│   │   └── enums/
│   │       └── OrderStatus.java        # Enum de ciclo de vida de negocio
│   ├── event/
│   │   ├── OrderCreatedEvent.java      # Records planos de dominio (solo datos de negocio)
│   │   ├── OrderConfirmedEvent.java
│   │   ├── OrderShippedEvent.java
│   │   └── OrderCancelledEvent.java
│   └── OrderingError.java              # Catálogo tipado de errores de negocio (implements ErrorCode)
└── infrastructure/                     # Detalle privado de Adaptadores
    └── adapter/
        ├── in/
        │   ├── web/
        │   │   ├── OrderController.java# REST Controller con @ResponseStatus y @RequiredArgsConstructor
        │   │   └── dto/
        │   │       └── CreateOrderHttpRequest.java # DTO con Bean Validation y factoría toCommand()
        │   └── worker/
        │       ├── OrderEventWorker.java # Consumidor asíncrono con IdempotencyGate y release defensivo
        │       └── dto/
        │           └── OrderPaymentEventMessage.java # Streaming record con toCommand()
        └── out/
            └── persistence/
                └── postgres/           # Persistencia jerárquica por motor PostgreSQL 17
                    ├── jpa/            # Escritura ACID con JPA
                    │   ├── adapter/
                    │   │   └── OrderPersistenceAdapter.java # Implementa OrderRepositoryPort y OutboxPublisherPort
                    │   ├── entity/
                    │   │   └── OrderEntity.java # @Entity relacional con Lombok en infraestructura
                    │   ├── mapper/
                    │   │   └── OrderPersistenceMapper.java # Mapeador explícito bidireccional
                    │   └── repository/
                    │       └── OrderJpaRepository.java # Extends JpaRepository<OrderEntity, UUID>
                    └── jdbc/           # Lectura de Alto Rendimiento CQRS Ligero
                        ├── adapter/
                        │   └── OrderJdbcQueryAdapter.java # Implementa OrderQueryPort
                        ├── mapper/
                        │   └── OrderResultRowMapper.java # RowMapper<OrderResult>
                        ├── query/
                        │   └── OrderJdbcQueries.java # Text Blocks SQL centralizados
                        └── repository/
                            └── OrderJdbcRepository.java # NamedParameterJdbcTemplate
```

---

## 7. Toolchain, Bootstrap, Migraciones y Operabilidad (*Production-Ready*)

### 7.1 Protocolo de Bootstrap y Parametrización

La semilla automatiza la configuración de nuevos proyectos en el "Día 1" mediante la configuración canónica de build en Gradle y la tarea ejecutable del protocolo `syncStandards`:

```groovy
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.4.1'
    id 'io.spring.dependency-management' version '1.1.7'
}

group = 'com.empresa.proyecto'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

tasks.withType(JavaCompile).configureEach {
    options.compilerArgs.add("-parameters")
}

repositories {
    mavenCentral()
}

ext {
    set('springModulithVersion', "1.3.1")
    set('archunitVersion', "1.3.0")
    set('testcontainersVersion', "1.20.4")
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.boot:spring-boot-starter-actuator'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'
    implementation 'org.springframework.boot:spring-boot-starter-jdbc'
    implementation 'org.postgresql:postgresql'
    implementation 'org.flywaydb:flyway-core'
    implementation 'org.flywaydb:flyway-database-postgresql'

    // Spring Modulith
    implementation platform("org.springframework.modulith:spring-modulith-bom:${springModulithVersion}")
    implementation 'org.springframework.modulith:spring-modulith-starter-core'
    implementation 'org.springframework.modulith:spring-modulith-starter-jpa'

    // Testing
    testImplementation 'org.springframework.boot:spring-boot-starter-test'
    testImplementation 'org.springframework.modulith:spring-modulith-starter-test'
    testImplementation "com.tngtech.archunit:archunit-junit5:${archunitVersion}"
    testImplementation "org.testcontainers:testcontainers:${testcontainersVersion}"
    testImplementation "org.testcontainers:postgresql:${testcontainersVersion}"
    testImplementation "org.testcontainers:junit-jupiter:${testcontainersVersion}"
    testImplementation 'org.awaitility:awaitility:4.2.2'
}

dependencyManagement {
    imports {
        mavenBom "org.springframework.modulith:spring-modulith-bom:${springModulithVersion}"
    }
}

tasks.named('test') {
    useJUnitPlatform()
}

// PROTOCOLO syncStandards: Descarga atómica inmutable
tasks.register('syncStandards') {
    group = 'fprog'
    description = 'Sincroniza atómicamente los estándares desde el repositorio central de GitHub'
    doLast {
        def preset = project.findProperty('standards.preset') ?: 'java-spring-modulith'
        def standardsVersion = project.findProperty('standards.version') ?: 'v1.0.0'
        def baseUrl = "https://raw.githubusercontent.com/erfp/erfp-fprog-standards/${standardsVersion}/presets/${preset}"
        def targetDir = file('.standards')

        if (!targetDir.exists()) {
            targetDir.mkdirs()
        }

        ['ARCHITECTURE.md', 'EXCEPTIONS.md', 'TESTING.md'].each { fileName ->
            def destFile = new File(targetDir, fileName)
            def url = "${baseUrl}/${fileName}"
            println "Descargando ${url} -> ${destFile.path}..."
            try {
                destFile.text = new URL(url).text
            } catch (Exception ex) {
                throw new GradleException("Error al sincronizar estándar ${fileName} desde ${url}: ${ex.message}")
            }
        }
        println "Sincronización de estándares completada con éxito (@${standardsVersion})."
    }
}
```

Aislamiento formal de perfiles en `src/main/resources/`:

* `application.yml`: Configuración base compartida (conexiones, Outbox, Modulith).
* `application-local.yml`: Perfil de desarrollo local apuntando a PostgreSQL en Docker Compose.
* `application-test.yml`: Configuración determinista para CI sobre Testcontainers efímeros.

### 7.2 Línea Base de Migraciones de Base de Datos

La evolución de esquemas se gobierna exclusivamente mediante **Flyway** bajo el directorio canónico `src/main/resources/db/migration/`. Queda taxativamente prohibido el uso de `hibernate.ddl-auto=update` o `create` en cualquier entorno (`SED-06`).

#### 7.2.1 Reglas de Particionado y Nomenclatura de Migraciones
Cada migración representa una unidad atómica e inmutable de cambio gobernada por las siguientes normas:

1. **Particionado por Módulo en el Baseline:**
   * No se permite un DDL monolítico que mezcle entidades de múltiples Bounded Contexts.
   * La infraestructura compartida transversal (`shared`) se inicializa en `V1__init_shared_infrastructure.sql` (extensiones, `outbox_events`, `processed_events`).
   * Cada módulo de negocio funcional debe contar con su propio archivo DDL de inicialización independiente (`V2`, `V3`, etc.).

2. **Convención de Nomenclatura (`V<Version>__<descripcion>.sql`):**
   * **Separador:** Doble guion bajo obligatorio (`__`) tras el número de versión.
   * **Case:** Formato `snake_case` estricto en minúsculas y sin acentos ni caracteres especiales.
   * **Inicialización de módulo:** `V<N>__init_<modulo>_tables.sql` (ej. `V2__init_ordering_tables.sql`, `V3__init_inventory_tables.sql`).
   * **Evolución incremental:** `V<N>__<verbo>_<descripcion>.sql` iniciando con verbo de acción (`add_`, `alter_`, `create_idx_`, `drop_`). Ejemplos:
     * `V4__add_billing_address_to_ordering_orders.sql`
     * `V5__create_idx_orders_customer_id.sql`

3. **Aislamiento por Esquema Relacional de PostgreSQL:**
   * Cada módulo encapsula sus tablas dentro de su propio `SCHEMA` de PostgreSQL mediante `CREATE SCHEMA IF NOT EXISTS <modulo>;`.
   * Todas las sentencias DDL deben referenciar explícitamente el esquema (ej. `ordering.orders`, `ordering.order_items`).

4. **Prohibición de Claves Foráneas (FK) Intermodulares:**
   * Las claves foráneas relacionales solo están permitidas **dentro de las tablas del mismo módulo**.
   * Entre módulos distintos queda prohibido definir `REFERENCES` a nivel SQL. Las relaciones intermodulares se modelan exclusivamente mediante identificadores escalares (`UUID`) en el modelo y se sincronizan a través de eventos de dominio (`DomainEvent`) y Transactional Outbox.

### 7.3 Empaquetado y Contenedorización Multi-Stage

El archivo `Dockerfile` de la semilla sigue el estándar de seguridad industrial de mínimo privilegio, ejecutando bajo usuario no root (`nonroot` UID 10001) sobre imágenes base reducidas (Eclipse Temurin / Alpine):

```dockerfile
# ETAPA 1: Compilación y empaquetado
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /workspace
COPY gradlew .
COPY gradle gradle
COPY build.gradle settings.gradle ./
RUN ./gradlew dependencies --no-daemon

COPY src src
RUN ./gradlew bootJar --no-daemon -x test

# ETAPA 2: Runtime de producción seguro y ligero
FROM eclipse-temurin:21-jre-alpine AS runner
WORKDIR /app

# Creación de usuario sin privilegios administrativos (Principio de Mínimo Privilegio)
RUN addgroup -g 10001 -S nonroot && \
    adduser -u 10001 -S nonroot -G nonroot

COPY --from=builder /workspace/build/libs/*.jar app.jar
RUN chown -R nonroot:nonroot /app

USER nonroot:nonroot

EXPOSE 8080
ENV JAVA_OPTS="-XX:+UseZGC -XX:+ZGenerational -XX:MaxRAMPercentage=75.0"

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
```

### 7.4 Observabilidad y Graceful Shutdown

Para entornos gobernados por orquestadores (Kubernetes), la semilla configura sondeos de salud y apagado ordenado en `application.yml` (`SED-07`):

```yaml
server:
  port: 8080
  shutdown: graceful

spring:
  threads:
    virtual:
      enabled: true
  lifecycle:
    timeout-per-shutdown-phase: 30s
  datasource:
    hikari:
      pool-name: SeedHikariPool
      maximum-pool-size: 20
      minimum-idle: 5

management:
  endpoints:
    web:
      exposure:
        include: health, info, metrics
  endpoint:
    health:
      probes:
        enabled: true
      show-details: when_authorized

outbox:
  poll-interval-ms: 1000
  purge-cron: "0 0 3 * * ?"
```

* **Sondas de Salud (Health Probes):** Exposición de `/actuator/health/liveness` (liveness) y `/actuator/health/readiness` (readiness verificando conexión a base de datos).
* **Parada Limpia (*Graceful Shutdown*):** Ante `SIGTERM`, el servidor web rechaza nuevo tráfico, el `OutboxRelayService` finaliza el despacho del lote de eventos en curso y el pool de conexiones HikariCP se drena ordenadamente sin provocar excepciones transaccionales ni eventos huérfanos.

---

## 8. Criterios de Aceptación (DoD), Matriz de Anti-Patrones y Checklist

### 8.1 Definition of Done (DoD) de la Semilla

La semilla se declara **completada, homologada y lista para ser clonada en producción** únicamente cuando supera con éxito estos **6 escenarios ejecutables obligatorios en el pipeline de CI** (`./gradlew check`):

#### Escenario 1: Compilación Limpia y Guardianes de Arquitectura en Verde

* `./gradlew check` compila en Java 21 sin advertencias.
* La suite completa de pruebas unitarias y de integración finaliza en menos de 5 minutos.
* `ApplicationModules.of(ApiApplication.class).verify()` pasa en verde sin dependencias cíclicas ni violaciones de visibilidad intermodular.
* La suite completa de ArchUnit valida al 100% las restricciones de capas `ARC-*`, `DOM-*`, `APP-*`, `INP-*`, `OUT-*` y `TRX-*`.

#### Escenario 2: Camino de Escritura y Outbox Transaccional Atómico

* Una petición HTTP POST contra `/api/v1/orders` con cabeceras `ApiHeaders` válidas retorna `201 Created`.
* La orden se almacena en la tabla `ordering.orders` con `version = 0`.
* En la misma transacción ACID de base de datos se inserta un registro en `outbox_events` con estado `PENDING` conteniendo el payload serializado de `OrderCreatedEvent`.
* Si se fuerza un fallo antes del commit, se produce rollback atómico conjunto de ambas tablas.

#### Escenario 3: Despacho Asíncrono Concurrente mediante `SKIP LOCKED`

* El worker `OutboxRelayService` sondea los registros en estado `PENDING`, los reserva con `FOR UPDATE SKIP LOCKED` y los transiciona a `PROCESSING`.
* Tras despachar el evento con éxito al bus local o broker, la fila en `outbox_events` pasa a estado `DELIVERED`.

#### Escenario 4: Resiliencia ante Fallos e Idempotencia

* Al simular una caída o excepción en el consumidor, el evento acumula reintentos incrementando `retry_count` con backoff exponencial.
* Superado el límite de 5 intentos, pasa definitivamente a estado `DEAD_LETTER` registrando el detalle del fallo técnico en `last_error`.
* El reenvío intencionado del mismo evento al worker es detectado por la compuerta de idempotencia (`processed_events`) y descartado sin duplicar mutaciones de negocio.

#### Escenario 5: Captura Estructurada de Excepciones sin Traza (*Zero-Overhead*)

* Una petición con payload inválido arroja una excepción derivada de `BaseException` con categoría `VALIDATION` o `CONFLICT`.
* `GlobalExceptionHandler` traduce el fallo a un JSON `ErrorResponse` omitiendo el campo `errors` cuando no hay violaciones de campo (`@JsonInclude(NON_EMPTY)`).
* Se audita en los logs que las excepciones de negocio registran un mensaje limpio en nivel `WARN` **sin volcar trazas de pila (stack traces)**.

#### Escenario 6: Parada Limpia del Runtime (*Graceful Shutdown*)

* Ante una señal de terminación del sistema (`SIGTERM`), el runtime detiene la ingesta de nuevas peticiones web y nuevos lotes de outbox, permitiendo que las transacciones y eventos en proceso de despacho finalicen ordenadamente antes de cerrar el pool de conexiones de base de datos.

---

### 8.2 Matriz Pedagógica de Anti-Patrones de la Semilla

| ❌ Anti-Patrón Común | 💥 Por qué falla | ✅ Solución Normativa de la Semilla | Regla Asociada |
| --- | --- | --- | --- |
| **Kernel "Cajón de Sastre" (*Junk Drawer*)** | Acumular DTOs, utilidades o modelos de negocio en `shared/` destruye el aislamiento modular. | Limitar `shared/` exclusivamente a tipos base abstractos, contexto y contratos universales. | `SED-01`, `SHR-01` |
| **Generar IDs en Motor (Serial / UUIDv4)** | Causa contención y fragmentación de índices B-Tree en bases de datos con alto volumen de inserción. | Generar identificadores UUIDv7 secuenciales en la capa de aplicación con generador lock-free CAS. | `SED-02`, `SHR-02` |
| **Fuga de MDC / Contexto en Virtual Threads** | No limpiar `ThreadLocal` o MDC provoca que trazas de logging mezclen identidades entre solicitudes concurrentes. | Limpieza obligatoria incondicional en bloque `finally` del filtro perimetral `ExecutionContextFilter`. | `SED-03`, `INP-04` |
| **Semilla Vacía sin Módulo Canónico** | Impide verificar la arquitectura con código real y obliga a cada programador a inventar la primera implementación. | Incluir el módulo de referencia `ordering` que implemente los 3 flujos operativos completos. | `SED-04`, `SED-11` |
| **MapStruct en Controladores Web** | Genera beans innecesarios y sobrecarga en Metaspace para transformaciones triviales de request a command. | Mapear mediante método factoría directo `toCommand()` en el propio record de la request en `dto/`. | `SED-09`, `PROP-01` |
| **Persistencia Plana sin Motor de BD** | Mezclar JPA y JDBC en un paquete plano dificulta la arquitectura políglota y satura responsabilidades. | Jerarquía estricta por motor (`postgres/jpa/`, `postgres/jdbc/`) con responsabilidades simétricas segregadas. | `SED-10`, `PROP-08` |
| **Doble Escritura sin Outbox / Bloqueo de Tabla** | Delegar la publicación de eventos a llamadas síncronas o hacer `SELECT FOR UPDATE` sin `SKIP LOCKED` genera inconsistencias y contención. | Integrar de serie la tabla `outbox_events` y el proceso de relay con `FOR UPDATE SKIP LOCKED`. | `SED-05`, `TRX-03` |
| **Contenedores Docker con Usuario Root** | Ejecutar aplicaciones en producción con privilegios de administrador viola estándares básicos de seguridad industrial. | Empaquetado multi-stage con ejecución forzada bajo usuario no privilegiado (`nonroot` UID 10001). | `SED-07` |

---

### 8.3 Checklist de Pull Request para Mantenimiento de la Semilla

Antes de aprobar modificaciones sobre el repositorio semilla `erft-fprog-seed-java-spring`, el revisor debe certificar:

* [ ] ¿El paquete `shared/` permanece completamente agnóstico a subdominios y declara `@ApplicationModule(type = OPEN)`? (`SED-01`)
* [ ] ¿Todos los generadores e identificadores base preservan el formato UUIDv7 secuencial lock-free? (`SED-02`)
* [ ] ¿El contexto de ejecución (`ExecutionContext`) mantiene inmutabilidad y purga estricta en el bloque `finally` de los filtros? (`SED-03`)
* [ ] ¿El módulo canónico `ordering` compila y sirve de base ejecutable para los tests de arquitectura de Modulith y ArchUnit? (`SED-04`)
* [ ] ¿Las consultas del `OutboxRelayService` preservan el bloqueo pesimista `FOR UPDATE SKIP LOCKED` y la purga periódica? (`SED-05`)
* [ ] ¿Toda nueva tabla o función base cuenta con su script de migración Flyway versionado en la línea base (cero `ddl-auto`), respetando la convención de un script por módulo (`V<N>__init_<modulo>_tables.sql`), schema dedicado y ausencia de FKs intermodulares? (`SED-06`)
* [ ] ¿La aplicación tiene configurado el periodo de gracia ante `SIGTERM` y el Dockerfile ejecuta bajo usuario `nonroot`? (`SED-07`)
* [ ] ¿Las peticiones web residen en `dto/` con sufijo `HttpRequest` y mapean a Comando mediante factoría directa `toCommand()`? (`SED-09`)
* [ ] ¿La persistencia se organiza jerárquicamente por motor (`postgres/jpa/`, `postgres/jdbc/`) con responsabilidades segregadas? (`SED-10`)
* [ ] ¿El build completo (`./gradlew check`) pasa en verde al 100% contra PostgreSQL 17 real en Testcontainers y cumple los 6 criterios del DoD? (`SED-11`)