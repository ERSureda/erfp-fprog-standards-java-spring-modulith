# Manual de Arquitectura — DDD Hexagonal Modular con Spring Modulith

> **Naturaleza del documento.** Especificación técnica normativa y Fuente Única de Verdad (SSOT) para desarrolladores, revisiones de código en Pull Requests, herramientas de análisis estático/linters de arquitectura y generadores automáticos o agentes de IA.
>
> **Fuerza normativa.** Las reglas etiquetadas como `MUST` son obligaciones técnicas estrictas cuyo incumplimiento bloquea el pipeline de integración continua o la aprobación de Pull Requests. Las reglas `NEVER` son prohibiciones taxativas que solo admiten excepciones mediante un registro de decisión formal (`ADR`) con condición de salida escrita.

---

## 1. Metadatos de Gobernanza y Fuerza Normativa

* **Preset / Ecosistema:** `java-spring-modulith` (Java 21+ LTS / Spring Boot 3 / PostgreSQL 17 / Spring Modulith).
* **Versión del Estándar:** `v1.1.0`.
* **Estado:** `Normativo`.
* **Repositorio Semilla Asociado:** `erft-fprog-seed-java-spring`.
* **Alcance:** Aplicable a todos los servicios y aplicaciones transaccionales desarrolladas bajo este stack tecnológico dentro del marco de trabajo FProg.
* **Ubicación en Catálogo Central:** `presets/java-spring-modulith/ARCHITECTURE.md`.
* **Ubicación en Proyecto Consumidor:** `.standards/ARCHITECTURE.md` (vía `syncStandards`).

---

## 2. Notación y Taxonomía de Reglas

### 2.1 Identificadores de Regla

La notación sigue la estructura unívoca: **`ÁMB-nn — FUERZA [TIPO]`**.

* **`MUST`**: Obligación técnica estricta.
* **`NEVER`**: Prohibición técnica absoluta.
* **`SHOULD`**: Recomendación técnica prioritaria sujeta a verificación de diseño.
* **`[A]`**: Verificación automatizada obligatoria en el pipeline de CI (Spring Modulith / ArchUnit / linters de análisis estático estructural).
* **`[R]`**: Verificación manual obligatoria en Pull Request mediante checklist técnico.

### 2.2 Catálogo Cerrado de Ámbitos (Acrónimos de 3 Letras)

* **`ARC`**: Principios macro, fronteras modulares, visibilidad pública y verificación estructural con Spring Modulith.
* **`DOM`**: Modelo de Dominio puro, entidades, agregados, invariantes, enums de estado y Value Objects.
* **`APP`**: Casos de uso (`UseCases`), orquestación de aplicación, separación CQRS y contratos.
* **`INP`**: Adaptadores de entrada / *driving* (Controladores REST, Workers, Schedulers).
* **`OUT`**: Adaptadores de salida / *driven* (Persistencia Híbrida JPA/JDBC, Clientes HTTP, Brokers).
* **`TRX`**: Delimitación transaccional, concurrencia optimista y Transactional Outbox.
* **`SHR`**: Kernel compartido técnico, contexto inmutable (`ExecutionContext`), identificadores UUIDv7 y catálogo de errores.

---

## 3. Topología de Directorios y Fronteras Modulares

### 3.1 Estructura Canónica del Proyecto

El sistema se organiza en módulos de negocio independientes gobernados por Spring Modulith bajo el paquete base `<namespace.base>`. La infraestructura transversal técnica reside en el módulo abierto `shared/` (`@ApplicationModule(type = OPEN)`).

```text
src/main/java/<namespace.base>/
├── ApiApplication.java                     # Bootstrap Spring Boot / Spring Modulith
│
├── shared/                                 # Kernel compartido transversal (@ApplicationModule OPEN)
│   ├── package-info.java                   # Declaración @ApplicationModule(type = OPEN, displayName = "Shared")
│   ├── domain/                             # Tipos base genéricos (AggregateRoot, BaseEntity, ValueObject, DomainEvent)
│   │   ├── model/                          # AggregateRoot, BaseEntity
│   │   ├── valueobject/                    # ValueObject (interfaz marcadora), Money (VO monetario universal)
│   │   ├── event/                          # DomainEvent (contrato inmutable lean de eventos con aggregateId)
│   │   ├── error/                          # CommonError, ErrorCategory, ErrorCode, FieldViolation
│   │   └── exception/                      # Jerarquía base de excepciones sin traza (BaseException, ConflictException, etc.)
│   ├── application/                        # Contexto transversal, paginación (PageResult/CursorResult) y puertos universales
│   │   ├── context/                        # ExecutionContext (record con UserType, multitenancy y roles con ámbito de tenant)
│   │   ├── result/                         # Wrappers transversales: PageResult (offset) y CursorResult (keyset)
│   │   └── port/out/                       # Puertos secundarios universales (EventPublisherPort, ExecutionContextPort,
│   │                                       # OutboxPublisherPort, UtcClockPort, UuidGeneratorPort)
│   └── infrastructure/                     # Adaptadores transversales técnicos
│       └── adapter/
│           ├── in/web/                     # ApiHeaders, ExecutionContextFilter, ErrorResponse, GlobalExceptionHandler
│           └── out/                        # UtcClockAdapter, ExecutionContextHolder, ExecutionContextAdapter,
│                                           # SpringEventPublisherAdapter, EventPublicationRepublisher,
│                                           # OutboxRelayService, JdbcOutboxPublisherAdapter,
│                                           # JdbcIdempotencyGate, TenantContextPostgresInterceptor, UuidGeneratorAdapter
│
└── <subdominio>/                           # Módulo / Bounded Context de Negocio
    ├── package-info.java                   # Declaración del módulo raíz Modulith con Javadoc normativo
    ├── application/                        # API PÚBLICA DEL MÓDULO (@NamedInterface("application"))
    │   ├── package-info.java               # Declara la interfaz nombrada pública expuesta a otros módulos
    │   ├── command/                        # Comandos inmutables de mutación (records planos o fail-fast)
    │   ├── query/                          # Modelos de consulta inmutables (parámetros y filtros)
    │   ├── port/
    │   │   ├── in/                         # Casos de uso / interfaces primarias (*UseCase)
    │   │   └── out/                        # Puertos secundarios (repositorios de agregados y consultas CQRS)
    │   ├── service/                        # Implementaciones orquestadoras transaccionales (*Service con @RequiredArgsConstructor)
    │   ├── result/                         # DTOs inmutables de salida de la aplicación (*Result)
    │   └── mapper/                         # Mappers de aplicación (si aplican transformaciones complejas)
    ├── domain/                             # DETALLE PRIVADO INTERNO (100% puro, agnóstico a frameworks y sin Lombok)
    │   ├── model/                          # Agregados (AggregateRoot), Entidades y Value Objects locales
    │   │   └── enums/                      # Enums de estado de negocio
    │   ├── event/                          # Eventos de dominio generados ante mutaciones (*Event records planos)
    │   └── <Subdominio>Error.java          # Catálogo tipado de errores de negocio (implements ErrorCode)
    └── infrastructure/                     # DETALLE PRIVADO INTERNO (Tecnología, transporte y persistencia)
        └── adapter/
            ├── in/                         # Adaptadores Primarios (Driving)
            │   ├── web/                    # Controladores REST (*Controller con @RequiredArgsConstructor y @ResponseStatus)
            │   │   └── dto/                # DTOs de entrada HTTP (*HttpRequest con método factoría toCommand())
            │   └── worker/                 # Consumidores de mensajes (*Worker), Schedulers con IdempotencyGate y release
            └── out/                        # Adaptadores Secundarios (Driven)
                ├── persistence/            # Persistencia desacoplada jerárquica por motor de base de datos
                │   └── postgres/           # Implementación PostgreSQL 17
                │       ├── jpa/            # Escritura / Transacciones ACID mediante JPA y Hibernate
                │       │   ├── adapter/    # *PersistenceAdapter (implements AggregateRepositoryPort)
                │       │   ├── entity/     # *Entity (@Entity relacional con Lombok en infraestructura)
                │       │   ├── mapper/     # *PersistenceMapper (mapeador explícito bidireccional)
                │       │   └── repository/ # *JpaRepository (extends JpaRepository)
                │       └── jdbc/           # Lectura de Alto Rendimiento (CQRS Ligero)
                │           ├── adapter/    # *QueryAdapter (implements AggregateQueryPort)
                │           ├── mapper/     # *RowMapper (implements RowMapper<*Result>)
                │           ├── query/      # *JdbcQueries (Text Blocks SQL centralizados)
                │           └── repository/ # *JdbcRepository (NamedParameterJdbcTemplate)
                ├── messaging/              # Publicadores a colas, brokers o relé outbox
                └── client/                 # Clientes HTTP downstream (RestClient)
```

### 3.2 Contrato de Visibilidad Inter-Módulo

1. **API Pública del Módulo (`application`):** Es el único punto de entrada a través del cual otros módulos pueden interactuar con este Bounded Context. Debe declararse explícitamente mediante `@NamedInterface("application")` en su respectivo `package-info.java` y expone exclusivamente interfaces de casos de uso (`port/in`), Comandos, Queries y DTOs de salida (`*Result`).
2. **Confinamiento Privado (`domain` e `infrastructure`):** Queda estrictamente prohibido que un módulo acceda directamente al modelo de dominio o a las clases de persistencia/adaptadores de otro módulo. La colaboración intermodular se efectúa mediante contratos en `application` o suscripciones asíncronas a eventos de dominio.
3. **Estandarización de `package-info.java` (`ARC-01`):**
   - Todo paquete raíz de subdominio debe contar con un `package-info.java` con anotación `@ApplicationModule` y Javadoc que defina el rol del bounded context.
   - El paquete `application` de cada subdominio debe declarar explícitamente `@NamedInterface("application")` en su respectivo `package-info.java`.
   - El módulo transversal técnico `shared` debe declarar `@ApplicationModule(type = ApplicationModule.Type.OPEN, displayName = "Shared")`.

---

## 4. Matriz de Dependencias y Regla de Purificación de Contexto

### 4.1 Regla Direccional y Visibilidad entre Capas

Las dependencias fluyen estrictamente desde el exterior hacia el centro: **Adaptadores → Aplicación → Dominio**.

| Capa / Componente | Puede depender de | Prohibido depender de |
| --- | --- | --- |
| **Dominio** (`domain`) | Tipos base de `shared/domain`, tipos nativos de Java (`java.*`). | Frameworks de transporte (HTTP/Web), ORMs/JPA, serializadores externos (Jackson), Aplicación, Adaptadores, Lombok (`DOM-01`). |
| **Aplicación** (`application`) | `domain` local, `shared/domain`, `shared/application`, contratos de `application` expuestos por otros módulos. Uso autorizado de Lombok (`@RequiredArgsConstructor`). | Adaptadores locales (`infrastructure/adapter/*`), bases de datos, tecnologías de transporte (HTTP/REST), paquetes internos de otros módulos. |
| **Adaptadores Entrada** (`adapter.in`) | `application` local, `shared`, enums de estado inmutables de `domain`. Uso autorizado de Lombok (`@RequiredArgsConstructor`, `@Slf4j`). | Entidades persistentes completas de base de datos (`@Entity`), adaptadores de salida, cualquier paquete interno de otro módulo. Prohibido MapStruct en web (`INP-01`). |
| **Adaptadores Salida** (`adapter.out`) | Puertos de `application/port/out` locales, tipos de dominio para mapeo explícito, `shared`. Uso autorizado de Lombok (`@RequiredArgsConstructor`, `@Slf4j`, `@Getter`, `@Setter` en entidades). | Adaptadores de entrada, controladores REST, cualquier paquete de otro módulo. |
| **Kernel Compartido** (`shared`) | Tipos nativos del JDK, utilidades transversales técnicas. | Cualquier módulo o subdominio de negocio específico (`<namespace.base>.<subdominio>.*`). |

### 4.2 Regla de Purificación de Contexto y Seguridad

El contexto de seguridad y transporte (`ExecutionContext`: token, cabeceras, identidad y roles) **jamás debe penetrar en el modelo de dominio**:

* **Adaptadores de entrada (`adapter.in`):** `ExecutionContextFilter` intercepta y extrae las cabeceras perimetrales (`X-Tenant-Id`, `X-User-Id`, `X-Roles`), reconstruye el `ExecutionContext` inmutable, lo deposita en `ExecutionContextHolder` (`ThreadLocal`) y puebla el MDC de logging asegurando su limpieza en el bloque `finally`.
* **Capa de Aplicación (`application`):** Valida la autorización o permisos invocando métodos defensivos sobre el puerto de contexto (`context.requireUserId()`, `context.requireTenantId()`, `context.requireTenantRole(role)`) y extrae los identificadores planos requeridos (`UUID`).
* **Capa de Dominio (`domain`):** Recibe exclusivamente tipos primitivos, identificadores planos (`UUID`) o Value Objects. El dominio desconoce por completo la existencia de tokens de transporte, cabeceras HTTP, cookies, sesiones o frameworks de seguridad.

---

## 5. Los 3 Flujos Canónicos de Operación

### 5.1 Camino de Escritura Síncrono (Command Flow — Mutación de Estado)

Obliga a hidratar el Agregado de Dominio para validar invariantes y ejecutar lógica de negocio protegida:

```text
[Cliente / Consumidor]
      │
      ▼
1. [Adapter IN (Controller)]  ──► Valida sintaxis de la petición (@Valid en DTO *HttpRequest).
      │                           Mapea payload a Command inmutable vía factoría directa en record: request.toCommand(...).
      │
2. [Application (Service)]    ──► Abre delimitación transaccional (@Transactional).
      │                           Hidrata el Agregado invocando puerto de salida (port/out).
      │
3. [Domain (AggregateRoot)]   ──► Ejecuta método de negocio semántico guiado por predicados FSM (canConfirm(), etc.).
      │                           Valida invariantes legales (fail-fast JIT intrinsics) y encola DomainEvent inmutable.
      │
4. [Adapter OUT (Persistence)]──► Persiste el nuevo estado del Agregado en BD con versión optimista vía JPA.
      │                           Publica el DomainEvent vía OutboxPublisherPort persistiendo en 'outbox_events' (UUIDv7).
      │
5. [Application (Service)]    ──► Cierra transacción. Retorna DTO de salida (*Result)
      │                           o lanza excepción tipada de coste cero (BaseException).
      │
6. [Adapter IN (Controller)]  ──► Devuelve DTO directamente con @ResponseStatus(HttpStatus.CREATED) para códigos fijos.
                                  Si hay fallo, GlobalExceptionHandler mapea a ErrorResponse estándar.
```

### 5.2 Camino de Lectura Síncrono (Query Flow — CQRS Ligero)

Optimiza la memoria y el rendimiento proyectando directamente desde la base de datos a DTOs de salida. **Queda estrictamente prohibido hidratar Agregados de dominio para operaciones de consulta**:

```text
[Cliente / Consumidor]
      │
      ▼
1. [Adapter IN (Controller)]  ──► Mapea parámetros de búsqueda y paginación a Query plano.
      │
      ▼
2. [Application (Service)]    ──► Ejecuta caso de uso bajo transacción de solo lectura (@Transactional(readOnly = true)).
      │                           Invoca puerto de consulta optimizada en application/port/out (*QueryPort).
      │
3. [Adapter OUT (Proyección)] ──► *QueryAdapter delega en *JdbcRepository.
      │                           Ejecuta consulta SQL (*JdbcQueries) mapeada directamente vía *RowMapper a DTOs
      │                           (*Result, PageResult, CursorResult) sin instanciar entidades pesadas de ORM ni Agregados.
      │
4. [Adapter IN (Controller)]  ──► Devuelve HTTP 200 con el DTO o PageResult paginado.
```

### 5.3 Camino de Consumo Asíncrono / Background (Worker & Event Flow)

Gobierna la recepción desacoplada de eventos, mensajes de broker o tareas programadas (*Schedulers*), garantizando entrega fiable y procesamiento seguro:

```text
[Message Broker / Schedulers / Outbox Relay]
      │
      ▼
1. [Adapter IN (Worker)]      ──► Recibe payload del evento/mensaje con deserialización streaming a record inmutable.
      │                           Inicializa ExecutionContext propagando tenantId y correlationId (RLS compatibility).
      │
2. [Idempotency Gate]         ──► VERIFICACIÓN DE IDEMPOTENCIA: tryAcquire(eventId, consumerName).
      │                           - Si YA fue procesado ──► Cortocircuita, emite ACK y termina.
      │                           - Si NO fue procesado ──► Continúa ejecución.
      │
3. [Application (Service)]    ──► Mapea a Command inmutable (message.toCommand()) e invoca el UseCase correspondiente.
      │                           Abre delimitación transaccional local (@Transactional).
      │
4. [Domain / Persistencia]    ──► Procesa lógica de negocio y registra mutaciones en base de datos.
      │
      ▼
5. [Adapter IN (Worker)]      ──► Confirma ejecución y retorna CompletableFuture<Void> para coordinar ACK del broker.
                                  LIBERACIÓN DEFENSIVA: Si ocurre un fallo transitorio downstream, el bloque catch
                                  invoca idempotencyGate.release(eventId) para permitir reintentos del broker sin bloqueos.
```

---

## 6. Catálogo Normativo de Reglas por Ámbito

### 6.1 Arquitectura y Fronteras Modulares (`ARC`)

* **`ARC-01 — MUST` [A]** Cada módulo de negocio debe estar encapsulado como un `@ApplicationModule` independiente de Spring Modulith con interfaces nombradas formalmente:
  * El paquete raíz del módulo declara su frontera en `package-info.java` con `@ApplicationModule` y Javadoc normativo.
  * La capa `application` declara `@NamedInterface("application")` en su respectivo `package-info.java`.
  * El kernel compartido `shared` declara `@ApplicationModule(type = ApplicationModule.Type.OPEN, displayName = "Shared")`.
* **`ARC-02 — NEVER` [A]** Ningún módulo accederá a paquetes privados (`domain` o `infrastructure`) de otro módulo; la comunicación es exclusivamente a través de `application` (`@NamedInterface`) o suscripción a eventos de dominio.
* **`ARC-03 — MUST` [A]** No existirán dependencias cíclicas directas ni indirectas entre módulos, auditado automáticamente por `ApplicationModules.verify()`.

### 6.2 Dominio (`DOM`)

* **`DOM-01 — NEVER` [A]** El paquete `domain` contendrá referencias, importaciones o anotaciones de Spring, Jakarta, Jackson, Hibernate/JPA, Lombok o cualquier librería externa (`ArchUnit`). El dominio es 100% Java puro.
* **`DOM-02 — MUST` [A]** Las entidades y agregados de dominio no expondrán constructores públicos; la instanciación se realiza mediante métodos factoría semánticos (`create`) o factorías de reconstrucción de persistencia (`reconstruct`), con constructores privados que validan invariantes mediante `Objects.requireNonNull` (fail-fast JIT intrinsics).
* **`DOM-03 — MUST` [A]** Toda mutación de estado en un Agregado se realiza mediante métodos de negocio semánticos acompañados de predicados de consulta positiva de máquina de estados finita (`canConfirm()`, `canShip()`, `canCancel()`).
* **`DOM-04 — NEVER` [A]** Ningún objeto de dominio expondrá métodos mutadores genéricos (`setters`); la mutación es siempre intencional y contextualizada.
* **`DOM-05 — MUST` [A]** Todo Agregado (`AggregateRoot`) registrará eventos de dominio inmutables (`DomainEvent`) ante cualquier mutación de estado válida. El contrato `DomainEvent` exige únicamente `String aggregateId()`, con métodos `default` para `occurredAt()` y `eventType()`, manteniendo los eventos limpios de metadatos de transporte perimetrales (`eventId`).

### 6.3 Aplicación (`APP`)

* **`APP-01 — MUST` [A]** Cada servicio de aplicación orquestador residirá en `application/service`, implementará exactamente un caso de uso (`*UseCase`) de `application/port/in`, y su nombre terminará estrictamente con el sufijo `Service`.
* **`APP-02 — NEVER` [A]** La capa de aplicación contendrá lógica de cálculo de negocio o validación de invariantes; su función se limita estrictamente a la orquestación técnica del flujo.
* **`APP-03 — MUST` [R]** La entrada a un caso de uso debe ser un Comando o Query inmutable:
  * **Comandos exclusivamente Web:** Si se consumen únicamente por HTTP, se definen como `record` planos sin validación interna redundante, delegando en la validación sintáctica (`@Valid`) del adaptador web (`ADR-006`).
  * **Comandos Multicanal:** Si pueden ser invocados desde colas, brokers o schedulers, deben implementar comprobaciones defensivas inmediatas (*fail-fast zero-allocation*) de no-nulidad mediante `Objects.requireNonNull` en su constructor compacto (`ADR-006`).
  * **Queries:** Se definen siempre como `record` planos de parámetros sin lógica interna.
* **`APP-04 — NEVER` [A]** Un caso de uso devolverá agregados de dominio o entidades de base de datos hacia los adaptadores primarios o hacia otros módulos. La salida es siempre un DTO plano (`*Result`, `PageResult`, `CursorResult`) o el lanzamiento de una excepción tipada (`BaseException`).

### 6.4 Adaptadores de Entrada (`INP`)

* **`INP-01 — MUST` [A]** Los controladores delegan inmediatamente a la capa de aplicación tras mapear las peticiones a Comandos o Queries.
  * Los DTOs de entrada HTTP residen en `infrastructure/adapter/in/web/dto/` con sufijo `HttpRequest` (ej. `CreateOrderHttpRequest`).
  * El mapeo a Comando se realiza mediante un método factoría directo en el propio record de la petición (`request.toCommand(...)`), prohibiéndose el uso de MapStruct en controladores web (`PROP-01`).
  * Se autoriza el uso de enums puros de estado de dominio (`domain.model.enums.*`) en controladores y DTOs web locales (`ADR-005`).
* **`INP-02 — NEVER` [A]** Ningún controlador ni adaptador primario contendrá lógica de negocio ni gestionará transacciones de base de datos directamente.
* **`INP-03 — MUST` [A]** Todas las excepciones de la API deben traducirse de forma centralizada mediante `GlobalExceptionHandler` al contrato estandarizado `ErrorResponse` (`status`, `code`, `detail`, `errors`), omitiendo detalles técnicos internos en producción.
* **`INP-04 — MUST` [R]** Todo proceso en background (Workers, Schedulers) debe inicializar o propagar su `ExecutionContext` (incluyendo `tenantId` para compatibilidad RLS) antes de invocar cualquier caso de uso.
* **`INP-05 — SHOULD` [R]** Para endpoints con código de respuesta HTTP fijo (ej. `200 OK`, `201 CREATED`) y sin manipulación dinámica de cabeceras de red, los métodos del controlador deben devolver directamente el DTO de salida (`*Result`) anotando el método con `@ResponseStatus(HttpStatus.CREATED)`. `ResponseEntity<T>` se reserva para endpoints con cabeceras dinámicas (`Location`, `ETag`, `Set-Cookie`).

### 6.5 Adaptadores de Salida y Persistencia (`OUT`)

* **`OUT-01 — NEVER` [A]** Las entidades anotadas o vinculadas al ORM/persistencia (`@Entity`) saldrán del adaptador de persistencia JPA hacia la aplicación o el dominio.
* **`OUT-02 — MUST` [A]** La persistencia se organizará jerárquicamente por motor de base de datos (`postgres/jpa/`, `postgres/jdbc/`) con responsabilidades simétricas y segregadas:
  * **JPA (`postgres/jpa/`):** `adapter/` (`*PersistenceAdapter`), `entity/` (`*Entity`), `mapper/` (`*PersistenceMapper`), `repository/` (`*JpaRepository`).
  * **JDBC (`postgres/jdbc/`):** `adapter/` (`*QueryAdapter`), `mapper/` (`*RowMapper`), `query/` (`*JdbcQueries`), `repository/` (`*JdbcRepository`).
* **`OUT-03 — NEVER` [R]** Se permitirá la carga perezosa (*lazy loading*) fuera del adaptador de persistencia; los agregados se cargan completos y consistentes.
* **`OUT-04 — MUST` [R]** Las consultas de lectura y listados **no deben hidratar Agregados de dominio**; deben mapear desde base de datos directamente a DTOs de proyección optimizados (`*Result`) mediante adaptadores JDBC directos (`NamedParameterJdbcTemplate`).
* **`OUT-05 — MUST` [A]** El Transactional Outbox se desacoplará de las entidades mediante el puerto transversal `OutboxPublisherPort` en `shared/application/port/out/` y su adaptador `JdbcOutboxPublisherAdapter` en `shared/infrastructure/adapter/out/event/`, persistiendo eventos en `outbox_events` con UUIDv7 generado en infraestructura.

### 6.6 Transacciones, Consistencia y Outbox (`TRX`)

* **`TRX-01 — MUST` [A]** La demarcación transaccional residirá exclusivamente en la capa de aplicación: `@Transactional` en casos de uso de escritura y `@Transactional(readOnly = true)` en consultas de solo lectura.
* **`TRX-02 — MUST` [A]** Los agregados de dominio deben implementar control de concurrencia optimista mediante un atributo o campo de versión opaco (`version`).
* **`TRX-03 — MUST` [A]** Todo evento de dominio generado por un agregado que deba ser emitido al exterior se persistirá en la misma transacción local de base de datos en la tabla `outbox_events` (**Transactional Outbox Pattern**), empleando identificadores UUIDv7 ordenados por tiempo para evitar fragmentación B-Tree.
* **`TRX-04 — NEVER` [A]** Se realizarán publicaciones síncronas por red hacia brokers de mensajería dentro de la transacción de negocio; la publicación la ejecuta un worker desacoplado que lee del outbox.
* **`TRX-05 — MUST` [R]** Todo consumidor de eventos debe implementar control de **idempotencia** (`IdempotencyGate.tryAcquire`). Si ocurre un fallo downstream transitorio, el bloque catch debe invocar `idempotencyGate.release(eventId)` para permitir reintentos del broker sin bloqueos espurios.
* **`TRX-06 — MUST` [A]** Las tablas de outbox deben disponer de un proceso programado de purga para eliminar registros entregados con éxito (`status = 'DELIVERED'`) que superen el periodo de retención legal configurado.

### 6.7 Kernel Compartido y Preocupaciones Transversales (`SHR`)

* **`SHR-01 — NEVER` [A]** El módulo compartido `shared` contendrá reglas de negocio específicas de ningún subdominio.
* **`SHR-02 — MUST` [A]** Todos los identificadores únicos globales del sistema se generarán en la capa de aplicación o infraestructura con formato **UUIDv7** secuencial en el tiempo conforme a RFC 9562 mediante `UuidGeneratorPort` (implementado con operaciones lock-free CAS sobre `AtomicLong`).
* **`SHR-03 — MUST` [A]** Toda operación debe propagar el contexto inmutable de ejecución (`ExecutionContext`: tenant, actor, roles y correlationId), soportando verificación de roles con ámbito de tenant (`hasTenantRole`, `requireTenantRole`).
* **`SHR-04 — MUST` [R]** Los errores de negocio deben asociarse a un código alfanumérico inmutable y tipado mediante `ErrorCode` (`code()`), separando el código técnico del mensaje descriptivo.
* **`SHR-05 — MUST` [A]** Los Value Objects universales compartidos entre múltiples bounded contexts (como `Money`) residirán en `shared.domain.valueobject`, implementando igualdad monetaria independiente de escala (`BigDecimal.compareTo` y `stripTrailingZeros()`) y aritmética inmutable segura.

---

## 7. Matriz de Automatización (Guardianes de CI)

La integridad arquitectónica de este manual se verifica obligatoriamente en el pipeline de CI mediante suites automatizadas:

| Regla | Mecanismo de Validación | Condición Técnica de Fallo Automatizada |
| --- | --- | --- |
| **`ARC-01`** | `Spring Modulith` | Dependencias circulares entre módulos, falta de interfaces públicas o módulos mal ubicados detectados por `ApplicationModules.verify()`. |
| **`ARC-02`** | `Spring Modulith / ArchUnit` | Clases de un módulo importan paquetes privados (`domain..` o `infrastructure..`) de otro módulo. |
| **`DOM-01`** | `ArchUnit` | Clases en `..domain..` importan librerías de frameworks web, persistencia/ORM, Jackson o Lombok. |
| **`DOM-04`** | `ArchUnit` | Métodos públicos en clases de `..domain.model..` tienen nombres que comienzan por `set`. |
| **`APP-01`** | `ArchUnit` | Clases en `..application.service..` no implementan exactamente una interfaz `UseCase` o no terminan en `Service`. |
| **`INP-01`** | `ArchUnit` | Clases en adaptadores de entrada dependen de `..domain.model..` (excluyendo enums puros de estado en `..domain.model.enums..`). |
| **`OUT-01`** | `ArchUnit` | Clases anotadas con `@Entity` residen fuera de `..persistence..jpa..`. |
| **`OUT-05`** | `ArchUnit` | Clases que utilizan `NamedParameterJdbcTemplate` residen fuera de `..persistence..jdbc..` o de adaptadores transversales outbox. |
| **`TRX-01`** | `ArchUnit` | Demarcación transaccional `@Transactional` presente en clases o métodos situados fuera de la capa `..application..`. |

---

## 8. Matriz Pedagógica de Anti-Patrones Comunes

| ❌ Anti-Patrón Común | ⚠️ Por qué falla | ✅ Solución Normativa | Regla Asociada |
| --- | --- | --- | --- |
| **Acoplamiento Directo Inter-Módulo** | Un módulo inyecta repositorios o accede a entidades de otro módulo, destruyendo la modularidad y bloqueando la escalabilidad. | Comunicar módulos exclusivamente mediante contratos en `application` expuestos formalmente o vía eventos asíncronos. | `ARC-01`, `ARC-02` |
| **Hidratar Agregados para Lecturas** | Degrada drásticamente la latencia y satura la memoria al instanciar grafos enteros de objetos cuando solo se querían mostrar datos en pantalla. | Proyectar consultas de lectura directamente desde la base de datos a DTOs de salida optimizados (CQRS Ligero). | `OUT-04` |
| **Doble Escritura (Dual-Write) sin Outbox** | Si la base de datos confirma el guardado pero la red falla al notificar al broker, el sistema entra en inconsistencia irrecuperable. | Transactional Outbox Pattern: guardar en base de datos local y publicar de forma desacoplada en segundo plano con worker. | `TRX-03`, `TRX-04` |
| **Consumo Asíncrono sin Idempotencia** | La red y los brokers garantizan entrega al menos una vez (*at-least-once*); procesar duplicados corrompe saldos y estados. | Idempotency Gate en adaptadores worker: verificar `eventId` antes de ejecutar mutaciones de estado y `release` en catch. | `TRX-05` |
| **Agregado Anémico con Setters** | El agregado pierde el control de sus invariantes y cualquier servicio externo corrompe el estado interno. | Mutaciones mediante métodos de negocio semánticos, predicados FSM (`canConfirm`) y validación fail-fast en constructores. | `DOM-03`, `DOM-04` |
| **Entidad de Persistencia usada como Negocio** | El ORM o motor invade el dominio, forzando constructores vacíos y acoplando la lógica de negocio a tablas SQL. | Separar Agregado de Dominio de la entidad de persistencia mediante un mapper explícito bidireccional en `postgres/jpa/mapper`. | `DOM-01`, `OUT-01` |
| **Fuga del Contexto de Seguridad al Dominio** | Pasar objetos de sesión/token al dominio contamina las invariantes puras con detalles perimetrales de transporte. | La capa de aplicación valida el contexto y pasa al dominio únicamente identificadores planos primitivos. | `SHR-03`, `DOM-01` |
| **Lógica de Negocio en el Caso de Uso** | El caso de uso se vuelve un procedimiento monolítico y el modelo de dominio queda huérfano de lógica. | Mover las reglas de cálculo, transiciones e invariantes al interior de los métodos del Agregado. | `APP-02` |
| **Duplicación redundante de Enums en Web** | Crear enums idénticos en la web solo para aislar el dominio multiplica el código redundante sin aportar valor funcional. | Permitir el consumo directo de enums de estado inmutables del dominio en controladores REST locales. | `INP-01`, `ADR-005` |
| **MapStruct en Controladores Web** | Genera beans de Spring innecesarios en Metaspace y llamadas polimórficas virtuales para mapeos triviales de request a command. | Mapear DTO a Comando mediante método factoría directo `toCommand()` en el propio record de la petición. | `INP-01`, `PROP-01` |
| **Instanciación de ResponseEntity innecesario** | Retornar `ResponseEntity.status(...).body(...)` genera objetos efímeros en el Heap en cada request fija aumentando la frecuencia de GC. | Retornar directamente el DTO de salida anotado con `@ResponseStatus(HttpStatus.CREATED)` para códigos HTTP fijos. | `INP-05`, `PROP-02` |
| **Doble validación en Comandos exclusivos Web** | Revalidar en el Command lo que ya verificó Bean Validation (`@Valid`) en el controlador web duplica código innecesariamente. | Definir comandos exclusivos de HTTP como records planos sin validación interna. | `APP-03`, `ADR-006` |
| **Comando Multicanal sin validación fail-fast** | Permite que peticiones defectuosas desde colas o schedulers alcancen el dominio o abran transacciones. | Validación defensiva inmediata con `Objects.requireNonNull` en el constructor compacto de comandos multicanal. | `APP-03`, `ADR-006` |
| **Metadatos de Transporte en Eventos de Dominio** | Incluir `eventId` y `occurredAt` manuales en el record del evento contamina el dominio con transporte y duplica datos en JSONB. | `DomainEvent` lean con `aggregateId()`; infraestructura genera UUIDv7 secuencial al persistir en el outbox. | `DOM-05`, `PROP-09` |
| **Value Objects Transversales confinados en Módulos** | Confinar tipos universales como `Money` en un submódulo (`ordering`) fuerza duplicación DRY o violaciones de fronteras Modulith. | Promover Value Objects compartidos a `shared.domain.valueobject` con igualdad independiente de escala. | `SHR-05`, `PROP-11` |

---

## 9. Checklist de Pull Request y Gobernanza de Decisiones (ADRs)

### 9.1 Checklist de Verificación en Pull Request

Antes de aprobar la integración de código a ramas principales, el revisor debe certificar:

* [ ] ¿Las fronteras del módulo se respetan sin invadir paquetes internos (`domain` o `infrastructure`) de otros módulos? (`ARC-02`)
* [ ] ¿El paquete raíz del subdominio y el paquete `application` cuentan con su respectivo `package-info.java` normativo? (`ARC-01`, `PROP-04`)
* [ ] ¿El paquete `application` expone su contrato formalmente como API pública hacia el exterior mediante `@NamedInterface("application")`? (`ARC-01`)
* [ ] ¿El contexto de ejecución (`ExecutionContext`) se purifica en la aplicación sin filtrarse hacia el dominio, soportando roles por tenant si aplica? (`SHR-03`, `DOM-01`, `PROP-03`)
* [ ] ¿El Agregado protege sus invariantes con predicados FSM positivos (`can...()`) y constructores fail-fast sin exponer `setters`? (`DOM-02`, `DOM-04`, `PROP-10`)
* [ ] ¿El caso de uso orquesta el flujo sin absorber reglas de cálculo que corresponden al Agregado, utilizando `@RequiredArgsConstructor`? (`APP-01`, `APP-02`, `PROP-07`)
* [ ] ¿Las peticiones web residen en `dto/` con sufijo `HttpRequest` y mapean a Comando mediante factoría directa `toCommand()`? (`INP-01`, `PROP-01`)
* [ ] ¿Los controladores con código de estado HTTP fijo retornan directamente el DTO anotado con `@ResponseStatus`? (`INP-05`, `PROP-02`)
* [ ] ¿Las entidades de base de datos/ORM están estrictamente confinadas al adaptador de persistencia JPA (`postgres/jpa/`)? (`OUT-01`, `OUT-02`, `PROP-08`)
* [ ] ¿Las consultas de lectura y listados van directas a proyecciones DTO vía JDBC (`postgres/jdbc/`) sin hidratar Agregados? (`OUT-04`, `OUT-05`, `PROP-08`)
* [ ] ¿Toda mutación de negocio encola su correspondiente `DomainEvent` lean para el outbox transaccional desacoplado (`OutboxPublisherPort`)? (`DOM-05`, `TRX-03`, `PROP-09`)
* [ ] ¿Todo consumidor de eventos (Worker) cuenta con compuerta de idempotencia y libera defensivamente el lock (`release`) ante fallos transitorios? (`TRX-05`, `PROP-05`)
* [ ] ¿Los errores se traducen a través del manejador global al contrato estándar `ErrorResponse`? (`INP-03`)

### 9.2 Índice de Registro de Decisiones de Arquitectura (ADR Base)

Toda decisión estructural adoptada en este Preset debe estar formalizada en el directorio `/adr` del repositorio:

| Identificador | Título de la Decisión | Estado | Decisión y Justificación Técnica | Enlace al Documento |
| --- | --- | --- | --- | --- |
| **`ADR-001`** | Persistencia Híbrida y Desacoplamiento de Modelos | Aceptado | Separación total de Agregados y entidades persistentes; JPA para transacciones y JDBC directo (`NamedParameterJdbcTemplate`) para consultas optimizadas. | [`adr/ADR-001.md`](adr/ADR-001.md) |
| **`ADR-002`** | Publicación Asíncrona mediante Transactional Outbox | Aceptado | Persistencia local ACID de eventos en `outbox_events` con UUIDv7 y despacho con `FOR UPDATE SKIP LOCKED` para garantizar consistencia eventual. | [`adr/ADR-002.md`](adr/ADR-002.md) |
| **`ADR-003`** | Estandarización de Errores y Excepciones de Coste Cero | Aceptado | Contrato lean `ErrorResponse` (`@JsonInclude(NON_EMPTY)`) y desactivación de trazas de pila (`writableStackTrace = false`) en excepciones funcionales. | [`adr/ADR-003.md`](adr/ADR-003.md) |
| **`ADR-004`** | Gobernanza Modular y Verificación en Compilación | Aceptado | Bounded Contexts gobernados por Spring Modulith con API pública en `application` y guardianes CI ejecutables en cada build. | [`adr/ADR-004.md`](adr/ADR-004.md) |
| **`ADR-005`** | Consumo Directo de Enums de Estado en Adaptadores Primarios | Aceptado | Se autoriza el uso directo de enums de estado inmutables del dominio en controladores REST locales para evitar boilerplate duplicado. | [`adr/ADR-005.md`](adr/ADR-005.md) |
| **`ADR-006`** | Validación Híbrida en Comandos (Web vs. Multicanal) | Aceptado | Comandos web como records planos sin validación interna; comandos multicanal con validación fail-fast `Objects.requireNonNull`. | [`adr/ADR-006.md`](adr/ADR-006.md) |
| **`ADR-007`** | Contenerización Mínima y Seguridad en Runtime | Aceptado | Multi-stage build (Temurin Alpine), usuario nonroot (10001), ZGC generacional y graceful shutdown de 30s. | [`adr/ADR-007.md`](adr/ADR-007.md) |\n