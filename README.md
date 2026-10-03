# erfp-fprog-standards-java-spring-modulith

Repositorio semilla canónico (*Production-Ready Reference Seed*) para el desarrollo de servicios empresariales en **Java 21/25** y **Spring Boot 4** gobernado por **Spring Modulith**, **Arquitectura Hexagonal (Puertos y Adaptadores)** y **Domain-Driven Design (DDD)** táctico, certificado con una suite de pruebas automatizadas y fitness functions al 100% de cumplimiento con las normativas de arquitectura corporativas.

---

## 🏛️ Arquitectura del Sistema

El proyecto implementa una arquitectura modular con estricto desacoplamiento de capas y verificación en tiempo de compilación y pruebas:

```text
src/main/java/com/template/api/
├── shared/                             # Kernel Transversal Abierto (@ApplicationModule(Type.OPEN))
│   ├── application/                    # Contexto de ejecución, puertos transversales
│   ├── domain/                         # Tipos base DDD (AggregateRoot, BaseEntity, DomainEvent)
│   │   ├── error/                      # Contratos ErrorCode, FieldViolation
│   │   └── exception/                  # Excepciones lean sin traza (Zero-Overhead)
│   └── infrastructure/                 # Adaptadores de infraestructura transversal
│       ├── adapter/in/web/             # GlobalExceptionHandler, ExecutionContextFilter, ApiHeaders
│       └── adapter/out/                # OutboxRelayService, JdbcIdempotencyGate, Clock, UUIDv7
└── ordering/                           # Bounded Context Canónico de Referencia (@ApplicationModule)
    ├── package-info.java               # Metadatos del módulo de negocio
    ├── application/                    # API Pública (@NamedInterface("application"))
    │   ├── package-info.java           # Contrato de visibilidad intermodular
    │   ├── command/                    # CreateOrderCommand, ProcessOrderPaymentCommand
    │   ├── query/                      # GetOrderByIdQuery
    │   ├── port/in/                    # UseCases: CreateOrderUseCase, GetOrderByIdUseCase, ProcessOrderPaymentUseCase
    │   ├── port/out/                   # OrderRepositoryPort, OrderQueryPort
    │   ├── result/                     # OrderResult DTO
    │   └── service/                    # CreateOrderService, GetOrderByIdService, ProcessOrderPaymentService
    ├── domain/                         # Núcleo Puro de Dominio (Cero dependencias externas)
    │   ├── model/                      # Order (AggregateRoot), Money (Value Object)
    │   │   └── enums/                  # OrderStatus
    │   ├── event/                      # OrderCreatedEvent (DomainEvent)
    │   └── OrderingError.java          # Catálogo de errores tipados (implements ErrorCode)
    └── infrastructure/                 # Adaptadores Técnicos Privados
        └── adapter/
            ├── in/
            │   ├── web/                # OrderController, dto/CreateOrderHttpRequest (@Valid)
            │   └── worker/             # OrderEventWorker (Idempotency Gate)
                └── persistence/
                    └── postgres/
                        ├── jdbc/
                        │   ├── adapter/    # OrderJdbcQueryAdapter (Implementa OrderQueryPort)
                        │   ├── mapper/     # OrderResultRowMapper (RowMapper<OrderResult>)
                        │   ├── query/      # OrderJdbcQueries (Sentencias SQL con Java 21 Text Blocks)
                        │   └── repository/ # OrderJdbcRepository (Ejecución con NamedParameterJdbcTemplate)
                        └── jpa/
                            ├── adapter/    # OrderPersistenceAdapter (Implementa OrderRepositoryPort + Outbox)
                            ├── entity/     # OrderEntity (@Entity JPA optimizada con Lombok)
                            ├── mapper/     # OrderPersistenceMapper (Domain <-> JPA Entity)
                            └── repository/ # OrderJpaRepository (Spring Data JpaRepository)
```

---

## ⚡ Los 3 Flujos Operativos Canónicos

1. **Flujo de Mutación (Command Flow):**
   - Entrada vía `OrderController` (`POST /api/v1/orders`) validando payload mediante `@Valid`.
   - Transacción ACID en `CreateOrderService` (`@Transactional`).
   - El agregado `Order` aplica invariantes de negocio y registra `OrderCreatedEvent`.
   - `OrderPersistenceAdapter` persiste la entidad en `ordering.orders` e inserta el evento atómicamente en `outbox_events` (Transactional Outbox Pattern).

2. **Flujo de Consulta (CQRS Query Flow):**
   - Entrada vía `OrderController` (`GET /api/v1/orders/{id}`).
   - `GetOrderByIdService` bajo `@Transactional(readOnly = true)`.
   - `OrderJdbcQueryAdapter` proyecta directamente desde `ordering.orders` a `OrderResult` con `NamedParameterJdbcTemplate` sin instanciar entidades ORM ni hidratar Agregados.

3. **Flujo Asíncrono con Idempotencia (Worker Flow):**
   - `OrderEventWorker` recibe eventos de pago o mensajes asíncronos.
   - Idempotency Gate (`JdbcIdempotencyGate`) verifica y bloquea tuplas en `processed_events`.
   - Procesa `ProcessOrderPaymentUseCase` garantizando procesamiento exactamente una vez (*effectively once*).

---

## 🛡️ Fitness Functions y Gobernanza de Arquitectura

El pipeline de CI ejecuta permanentemente los guardianes arquitectónicos con **ArchUnit** y **Spring Modulith**:

- `ModulithStructureTest`: Valida fronteras entre módulos (`modules.verify()`) y genera documentación viva (`PlantUML`).
- `ApplicationRulesArchTest`: Verifica que cada servicio en `application.service` implemente exactamente una interfaz `*UseCase` (`APP-01`) y que `@Transactional` resida exclusivamente en aplicación (`TRX-01`).
- `DomainRulesArchTest`: Certifica pureza de dominio sin frameworks (`DOM-01`), constructores protegidos/no públicos (`DOM-02`) y ausencia de setters mutadores (`DOM-04`).
- `PersistenceRulesArchTest`: Confina `@Entity` exclusivamente al paquete `persistence.jpa` (`OUT-01`) y el uso de `NamedParameterJdbcTemplate` a persistencia JDBC (`OUT-05`).
- `WebRulesArchTest`: Garantiza que los adaptadores web no dependan de entidades de dominio excepto enums (`INP-01`, `ADR-005`) y prohíbe `@Transactional` en controladores (`INP-02`).

---

## 🗄️ Persistencia y Migraciones Baseline (Flyway)

Queda terminantemente prohibido `ddl-auto=update` o `create`. Todas las mutaciones de base de datos se gestionan mediante Flyway bajo `src/main/resources/db/migration/`:

- `V1__init_shared_infrastructure.sql`: Extensiones `uuid-ossp`, `btree_gist`, tabla transaccional `outbox_events` con índice parcial `SKIP LOCKED` y tabla de idempotencia `processed_events`.
- `V2__create_orders_table.sql`: Esquema `ordering` y tabla de persistencia `ordering.orders` con columna de versión optimista (`version`).

---

## 🚀 Tareas Gradle y Protocolo de Estándares

### Compilación y Ejecución de Pruebas
```bash
# Compilar y ejecutar la suite completa de pruebas unitarias, slices y fitness functions
./gradlew check

# Ejecutar pruebas únicamente
./gradlew test
```

### Sincronización de Estándares (`syncStandards`)
Descarga e integra atómicamente los documentos de estándares normativos desde el repositorio central:
```bash
./gradlew syncStandards
```

---

## 🐳 Contenerización Segura (Dockerfile)

Multi-stage build con principio de mínimo privilegio (`USER nonroot:nonroot` UID 10001) y optimizaciones de runtime JVM:
```bash
docker build -t erfp-fprog-standards-java-spring-modulith .
```

---

## 📚 Registro de Decisiones de Arquitectura (ADRs)

Las decisiones estructurales están formalizadas en el directorio [`/adr`](adr/):
- [ADR-001: Persistencia Híbrida y Desacoplamiento de Modelos](adr/ADR-001.md)
- [ADR-002: Publicación Asíncrona mediante Transactional Outbox](adr/ADR-002.md)
- [ADR-003: Estandarización de Errores y Excepciones de Coste Cero](adr/ADR-003.md)
- [ADR-004: Gobernanza Modular y Verificación en Compilación](adr/ADR-004.md)
- [ADR-005: Consumo Directo de Enums de Estado en Adaptadores Primarios](adr/ADR-005.md)
- [ADR-006: Contenerización Mínima y Seguridad en Runtime](adr/ADR-006.md)
