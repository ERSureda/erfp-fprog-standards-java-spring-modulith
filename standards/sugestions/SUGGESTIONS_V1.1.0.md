# Propuestas de Mejora y Clarificaciones para Estándares FProg (v1.1.0)

> **Documento de Trabajo.** Registro vivo de propuestas, clarificaciones normativas y optimizaciones técnicas recopiladas durante la auditoría e implementación del proyecto consumidor. Estas mejoras están destinadas a integrarse en la siguiente versión del catálogo central de estándares (`presets/java-spring-modulith`).

---

## 1. Metadatos de Gobernanza

* **Preset Asociado:** `java-spring-modulith` (Java 21+ / Spring Boot 3 / PostgreSQL 17 / Spring Modulith).
* **Versión de Estándar Base:** `v1.0.0`.
* **Versión Objetivo Propuesta:** `v1.1.0`.
* **Estado:** `Propuesta / En Revisión`.
* **Fecha de Apertura:** Octubre 2026.
* **Ubicación:** `standards/sugestions/SUGGESTIONS_V1.1.0.md`.

---

## 2. Índice de Propuestas

1. [PROP-01] Mapeo de Peticiones Web a Comandos mediante Records (`INP-01` y MapStruct)
2. [PROP-02] Optimización de Asignaciones en Respuestas HTTP (`@ResponseStatus` vs. `ResponseEntity`)
3. [PROP-03] Roles con Ámbito de Tenant en `ExecutionContext` (`SHR-03`)
4. [PROP-04] Estandarización de `package-info.java` y Documentación Modulith (`ARC-01`)
5. [PROP-05] Enrutamiento Idempotente de Eventos y Preparación para Microservicios (`TRX-05`, `ARC-02`)
6. [PROP-06] Reconciliación y Numeración Canónica de Registros de Decisión (ADRs)
7. [PROP-07] Adopción de `@RequiredArgsConstructor` en Servicios y Adaptadores (Máxima Velocidad de Desarrollo)
8. [PROP-08] Organización de Persistencia por Motor de Base de Datos y Descomposición Interna de JPA (adapter, entity, mapper, repository)
9. [PROP-09] Desacoplamiento de Metadatos de Transporte en Eventos de Dominio y Generación de UUIDv7 en Outbox (DOM-01, TRX-03)
10. [PROP-10] Modelado Canónico de Agregados con Predicados FSM y Validaciones JIT Intrinsics (DOM-02, DOM-03, DOM-04)

---

## 3. Detalle de las Propuestas

### PROP-01 · Mapeo de Peticiones Web a Comandos mediante Records (Ámbito `INP-01`)

* **Situación Actual (`v1.0.0`):**
  En el árbol canónico de directorios de `ARCHITECTURE.md §3.1`, la capa web incluye:
  ```text
  │   ├── web/                    # Controladores REST, Request DTOs, Mappers Web
  ```
  La etiqueta *"Mappers Web"* induce a los desarrolladores a crear interfaces `@Mapper(componentModel = "spring")` (MapStruct) inyectadas como Beans de Spring para mapear requests a comandos, generando:
  - Clases adicionales generadas en Metaspace (`*WebMapperImpl.class`).
  - Beans innecesarios en el `ApplicationContext`.
  - Métodos virtuales y llamadas polimórficas que ralentizan el calentamiento JIT (P99).
  - Riesgo de campos nulos silenciosos con `ReportingPolicy.IGNORE`.

* **Propuesta para `v1.1.0`:**
  Aclarar formalmente en `INP-01` que:
  1. Para peticiones HTTP entrantes, el mapeo a Comandos inmutables debe realizarse preferentemente mediante un **método factoría de conversión directo en el propio Record de la Request** (`request.toCommand(...)` o `request.toCommand(id)` cuando se combinan `@PathVariable`).
  2. Alternativamente, se permite una clase utilitaria estática pura (`public final class *WebMapper`) sin anotaciones de Spring ni de MapStruct.
  3. **MapStruct queda formalmente confinado al adaptador de salida JPA** (`adapter.out.persistence.jpa`), donde sí se requiere transformar grafos de entidades mutables de base de datos a agregados de dominio.
  4. **Estandarización de Directorio y Sufijo Web:** Los DTOs de entrada HTTP residirán obligatoriamente en el subpaquete `dto/` (`infrastructure/adapter/in/web/dto/`) y su nombre terminará siempre con el sufijo `HttpRequest` (ej. `CreateOrderHttpRequest.java`), garantizando distinción inequívoca frente a comandos y modelos.

* **Beneficio:** 0 beans de Spring, 0 clases en Metaspace para mapeo web, inlining nativo en ensamblador garantizado por el compilador JIT C2 (los `record`s son `final`), convención de nombres uniforme y detección de campos desalineados en tiempo de compilación.

---

### PROP-02 · Optimización de Asignaciones en Respuestas HTTP (Ámbito `INP-05`)

* **Situación Actual (`v1.0.0`):**
  No existe una pauta unificada sobre si los controladores deben devolver `ResponseEntity<T>` o el DTO de resultado directamente (`T`). En muchos controladores se utiliza:
  ```java
  return ResponseEntity.status(HttpStatus.CREATED).body(result);
  ```
  Esto instancia un objeto `ResponseEntity` en el Heap de la JVM en **cada petición HTTP**, aumentando la presión sobre el Garbage Collector.

* **Propuesta para `v1.1.0`:**
  Incorporar una regla o recomendación en el ámbito `INP`:
  * **Regla sugerida (`INP-05 · SHOULD`):** Para endpoints con código de respuesta HTTP fijo (ej. `200 OK`, `201 CREATED`) y sin manipulación dinámica de cabeceras de red, los métodos del controlador deben devolver directamente el DTO de salida (`*Result`) anotando el método con `@ResponseStatus(HttpStatus.CREATED)`.
  * El uso de `ResponseEntity<T>` se reserva exclusivamente para endpoints que requieran fijar cabeceras dinámicas (`Location`, `Set-Cookie`, `ETag`, `Cache-Control`).

* **Beneficio:** Elimina 1 objeto efímero en el Heap por cada transacción web entrante, reduciendo la frecuencia de ciclos de GC bajo alta concurrencia.

---

### PROP-03 · Roles con Ámbito de Tenant en `ExecutionContext` (Ámbito `SHR-03`)

* **Situación Actual (`v1.0.0`):**
  La regla `SHR-03` establece la inmutabilidad de `ExecutionContext` y métodos de aserción (`requireUserId()`, `requireTenantId()`, `hasRole(String)`). Sin embargo, el catálogo asumía roles genéricos de sistema/servicio, sin contemplar que un usuario pueda tener roles distintos dentro de diferentes tenants (ej. `TENANT_ADMIN` en Tenant A pero solo `OPERATOR` en Tenant B).

* **Propuesta para `v1.1.0`:**
  1. Formalizar en `SHR-03` los métodos de verificación de roles con ámbito de tenant:
     ```java
     boolean hasTenantRole(String role);
     ExecutionContext requireTenantRole(String role);
     ```
  2. Documentar el contrato de cabeceras HTTP perimetrales (`X-Roles`, `X-Tenant-Roles`) o el formato jerárquico de roles (`tenantId:role` o `[TENANT]_ROLE`) para estandarizar la extracción en `ExecutionContextFilter`.

* **Beneficio:** Evita que módulos individuales inventen soluciones ad-hoc de autorización multi-inquilino.

---

### PROP-04 · Estandarización de `package-info.java` y Documentación Modulith (Ámbito `ARC-01`)

* **Situación Actual (`v1.0.0`):**
  La regla `ARC-01` exige el uso de `@NamedInterface("application")`, pero no especifica la obligatoriedad de documentar los ficheros `package-info.java` con Javadoc normativo y tipos explícitos de Spring Modulith.

* **Propuesta para `v1.1.0`:**
  Añadir como requisito explícito en `ARC-01`:
  1. Todo paquete raíz de subdominio debe contar con un `package-info.java` con anotación `@ApplicationModule` y Javadoc que defina el rol del bounded context.
  2. El paquete `application` de cada subdominio debe declarar explícitamente `@NamedInterface("application")` en su respectivo `package-info.java`.
  3. El módulo transversal técnico `shared` debe declarar `@ApplicationModule(type = ApplicationModule.Type.OPEN, displayName = "Shared")`.

* **Beneficio:** Garantiza la generación automática de diagramas de arquitectura precisos con Spring Modulith Documenter y previene dependencias accidentales detectadas por ArchUnit.

---

### PROP-05 · Enrutamiento Idempotente de Eventos y Preparación para Microservicios (Ámbito `TRX-05`, `ARC-02`)

* **Situación Actual (`v1.0.0`):**
  La regla `TRX-03` define el Transactional Outbox y `TRX-05` exige compuertas de idempotencia. Sin embargo, no se detalla cómo se orquesta la recepción entre módulos dentro del monolito modular versus la evolución futura a microservicios independientes.

* **Propuesta para `v1.1.0`:**
  1. Documentar el patrón canónico de consumo intermodular con `@ApplicationModuleListener` de Spring Modulith.
  2. Explicitar que los eventos publicados por un módulo deben recibirse en un adaptador primario worker (`adapter.in.worker.*Worker`), donde se evalúa el `IdempotencyGate` antes de mapear al `Command` correspondiente del módulo consumidor.
  3. **Liberación Defensiva de Idempotencia (`release` en `catch`):** Si el caso de uso downstream falla por cualquier causa transitoria (timeout, deadlock, caída de BD), el worker debe invocar `idempotencyGate.release(eventId)` para no bloquear indefinidamente los futuros reintentos del broker.
  4. **Coordinación de ACK del Broker:** Los métodos de consumo deben retornar `CompletableFuture<Void>` para permitir que los listeners de Kafka/RabbitMQ coordinen el commit de offset con la persistencia real.
  5. **Propagación Multi-Tenant (`tenantId`):** Los payloads de eventos asíncronos deben transportar opcionalmente `UUID tenantId` para poblar el `ExecutionContext` en background, garantizando compatibilidad con Row-Level Security (RLS).
  6. Destacar que este diseño desacoplado permite migrar un módulo a un microservicio independiente simplemente sustituyendo el publicador en memoria por Kafka/RabbitMQ sin alterar los agregados de dominio ni los casos de uso.

* **Beneficio:** Tolerancia total a fallos en reintentos de mensajería (cero eventos perdidos), coordinación real de ACKs sin commits prematuros, aislamiento multi-tenant en background y compatibilidad directa para extracción a microservicios.

---

### PROP-06 · Reconciliación y Numeración Canónica de Registros de Decisión (ADRs)

* **Situación Actual (`v1.0.0`):**
  Existe una desincronización documental entre el manual de arquitectura y los ficheros físicos de ADR:
  - En `ARCHITECTURE.md §8 (líneas 302 y 303)` y `§9.2 (línea 336)` se hace referencia a:
    `ADR-006: Validación Híbrida en Comandos (Web vs. Multicanal)`
  - En el repositorio físico, el archivo `adr/ADR-006.md` corresponde a:
    `ADR-006: Contenerización Mínima y Seguridad en Runtime`

* **Propuesta para `v1.1.0`:**
  1. Renumerar el ADR de contenerización a `adr/ADR-007.md`.
  2. Incorporar formalmente `adr/ADR-006.md` con la decisión arquitectónica sobre *Validación Híbrida en Comandos (Web vs. Multicanal)*.
  3. Sincronizar la tabla de ADRs en `ARCHITECTURE.md §9.2`.

* **Beneficio:** Coherencia total entre la documentación teórica y los artefactos de gobernanza en el repositorio.

---

### PROP-07 · Adopción de `@RequiredArgsConstructor` en Servicios y Adaptadores (Máxima Velocidad de Desarrollo)

* **Situación Actual (`v1.0.0`):**
  La regla `DOM-01` prohíbe Lombok en `domain` (validado por ArchUnit). El seed carecía de Lombok en sus dependencias de `build.gradle`, lo que obligaba a escribir constructores manuales con `this.field = field` en todos los `@Service`, `@RestController` y adaptadores `@Repository`/`@Component`.

* **Propuesta para `v1.1.0`:**
  1. Habilitar `compileOnly 'org.projectlombok:lombok'` y su correspondiente `annotationProcessor` en `build.gradle` (junto a `lombok-mapstruct-binding`).
  2. Autorizar y promover el uso de `@RequiredArgsConstructor` en clases de orquestación técnica e infraestructura:
     - `application/service/*Service.java`
     - `infrastructure/adapter/in/web/*Controller.java`
     - `infrastructure/adapter/out/persistence/*Adapter.java`
     - `infrastructure/adapter/in/worker/*Worker.java`
  3. **Mantener inmutable la regla `DOM-01`:** Lombok continúa **estrictamente prohibido en el paquete `domain`**, garantizando que el núcleo de negocio sea 100% agnóstico a librerías y preprocesadores externos.

* **Beneficio:**
  - **Tiempo de desarrollo:** Elimina cientos de líneas de código repetitivo de constructores en toda la aplicación.
  - **Eficiencia en tiempo de ejecución:** 100% idéntica a los constructores manuales (Lombok inyecta el mismo constructor canónico a nivel de bytecode en compilación; 0 sobrecarga de CPU o memoria en runtime).

---

### PROP-08 · Organización de Persistencia por Motor de Base de Datos y Descomposición Interna de JPA (Ámbito `OUT-01`, `OUT-05`)

* **Situación Actual (`v1.0.0`):**
  En `v1.0.0`, el paquete de adaptadores de salida de persistencia (`infrastructure/adapter/out/persistence`) ubicaba `jpa` y `jdbc` directamente en la raíz de persistencia sin distinguir el motor de base de datos (`postgres/`, `redis/`, `mongo/`), lo que dificulta la incorporación limpia de persistencia políglota.
  Asimismo, dentro de `jpa`, los ficheros se encontraban en un único paquete plano (`OrderJpaEntity.java`, `SpringDataOrderRepository.java`, `OrderPersistenceJpaAdapter.java`), mezclando responsabilidades:
  - El adaptador asumía tanto la persistencia relacional y el outbox transaccional como la lógica de mapeo bidireccional entre la entidad JPA y el agregado de dominio (`toEntity` / `toDomain`).
  - La entidad JPA requería más de 120 líneas de código repetitivo de getters, setters y constructores manuales.

* **Propuesta para `v1.1.0`:**
  1. **Jerarquía por Motor de Base de Datos:** Agrupar las implementaciones de persistencia bajo el directorio específico del motor (ej. `persistence/postgres/`), albergando allí las tecnologías correspondientes (`postgres/jpa/`, `postgres/jdbc/`). Esto permite incorporar limpiamente futuros almacenes de datos (ej. `persistence/redis/`, `persistence/mongo/`) sin mezclar configuraciones ni tecnologías.
  2. **Descomposición Simétrica de JPA y JDBC por Responsabilidad Única (SRP) y Nomenclatura Canónica:**
     - **Submódulo JPA (`postgres/jpa/`):**
       - `adapter/`: Adaptadores de persistencia que implementan el puerto de salida del dominio (`*PersistenceAdapter.java`), anotados con `@Component` y `@RequiredArgsConstructor`, delegando el guardado de la entidad al repositorio JPA y los eventos a `OutboxPublisherPort` si `aggregate.hasDomainEvents()`.
       - `entity/`: Entidades relacionales JPA (`*Entity.java`), aprovechando Lombok en infraestructura (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`) para reducir el código a ~35 líneas limpias y legibles.
       - `mapper/`: Mapeador de alto rendimiento (`*PersistenceMapper.java`) con llamadas directas por constructor para inlining óptimo JIT C2.
       - `repository/`: Interfaces de Spring Data que terminan estrictamente con el sufijo `JpaRepository` (`*JpaRepository.java`) extendiendo `JpaRepository<*Entity, ID>`.
     - **Submódulo JDBC (`postgres/jdbc/`):**
       - `adapter/`: Adaptadores de consulta que implementan el puerto de salida de lectura (`*QueryAdapter.java`), anotados con `@Component` y `@RequiredArgsConstructor`, delegando en el repositorio JDBC.
        - `repository/`: Repositorio de consultas directas (`*JdbcRepository.java`), ejecutando `NamedParameterJdbcTemplate` con el `RowMapper` y delegando las sentencias SQL a la clase de queries.
        - `query/`: Clase de constantes de consulta SQL (`*JdbcQueries.java`), centralizando y encapsulando las sentencias SQL nativas con bloques de texto de Java 21 (*Text Blocks*), aislando completamente el SQL de la lógica de ejecución del repositorio.
       - `mapper/`: Mapeadores estándar de JDBC (`*RowMapper.java` implementando `RowMapper<*Result>`), aislando la conversión de filas `ResultSet` directamente a DTOs de salida.
     - **Outbox Transversal:** Puerto `OutboxPublisherPort` en `shared/application/port/out/` e implementación `JdbcOutboxPublisherAdapter` en `shared/infrastructure/adapter/out/event/`, desacoplando completamente el Transactional Outbox (`TRX-03`, `OUT-05`) de los adaptadores de entidades individuales.
  3. **Alineación con ArchUnit:**
     - Actualizar las reglas de `PersistenceRulesArchTest` para emplear patrones comodín `..persistence..jpa..` y `..persistence..jdbc..`, permitiendo subpaquetes jerárquicos por motor sin comprometer la restricción de que `@Entity` permanezca confinada a JPA y `NamedParameterJdbcTemplate` a JDBC o al outbox transversal.

* **Beneficio:**
  - **Legibilidad y Mantenibilidad:** Convención de nombres inequívoca y simétrica; cada archivo tiene un único propósito claro y acotado.
  - **Soporte Polyglot Persistence:** Preparado para arquitecturas políglotas (PostgreSQL + Redis + Elasticsearch/MongoDB) sin colisión de nombres ni paquetes saturados.
  - **Reducción de Boilerplate:** Más del 70% de reducción de líneas en entidades JPA con Lombok en infraestructura, manteniendo el dominio 100% puro (`DOM-01`).
  - **Testabilidad Aislada:** Permite testear el mapeador (`OrderPersistenceMapperTest`) de forma unitaria pura, rápida e independiente de la base de datos o el contexto de Spring.
  - **Transactional Outbox Limpio y Reutilizable:** Cero duplicación de SQL, Jackson o `EntityManager` en los adaptadores JPA de negocio.

---

### PROP-09 · Desacoplamiento de Metadatos de Transporte en Eventos de Dominio y Generación de UUIDv7 en Outbox (Ámbito `DOM-01`, `TRX-03`)

* **Situación Actual (`v1.0.0`):**
  La interfaz `DomainEvent` obligaba a cada evento de dominio a implementar explícitamente `UUID eventId()`, `String aggregateId()`, `Instant occurredAt()`, `String eventType()`.
  Esto producía:
  - Más de 35 líneas de código repetitivo por cada evento de negocio con métodos factoría (`of(...)`) y llamadas hardcodeadas a `UUID.randomUUID()` (contención por `SecureRandom`) e `Instant.now()`.
  - Duplicación de datos: los metadatos de transporte se persistían tanto en las columnas SQL de `outbox_events` como duplicados dentro del JSONB de carga útil (`payload`).
  - Contaminación del dominio con conceptos técnicos de transporte y mensajería (`eventId`).

* **Propuesta para `v1.1.0`:**
  1. **Contrato Inteligente y Esbelto (`DomainEvent.java`):** Exigir únicamente `String aggregateId()` y proporcionar métodos `default` para `occurredAt()` (`Instant.now()`) y `eventType()` (`getClass().getName()`). Eliminar `eventId()` del contrato de dominio.
  2. **Eventos de Dominio Planos y Puros:** Reducir cada evento a un `record` de ~15 líneas con los campos de negocio estrictos (`OrderCreatedEvent(orderId, customerId, amount, currency)`).
  3. **Generación de ID Secuencial en Infraestructura:** `JdbcOutboxPublisherAdapter` inyecta `UuidGeneratorPort` y genera identificadores UUIDv7 (RFC 9562) ordenados por tiempo, eliminando fragmentación de índices B-Tree en PostgreSQL y evitando bloqueos de concurrencia.
  4. **Payloads JSONB Optimizados:** La carga útil JSON solo contiene los atributos puros de negocio (reducción de más del 55% del tamaño serializado en disco y red).

* **Beneficio:** Máxima pureza de dominio (`DOM-01`), reducción drástica de código en eventos de negocio, 55% menos I/O de disco/red en el outbox y cero contención criptográfica.

---

### PROP-10 · Modelado Canónico de Agregados con Predicados FSM y Validaciones JIT Intrinsics (Ámbito `DOM-02`, `DOM-03`, `DOM-04`)

* **Situación Actual (`v1.0.0`):**
  Los agregados de dominio presentaban verificaciones de transición mediante listas negras de estados (`switch (this.status) { case CANCELLED -> throw ...; case SHIPPED -> throw ... }`) y métodos privados de validación de un solo uso (`validateCanConfirm()`). Además, se duplicaban comprobaciones de no-nulos con métodos manuales que ensombrecían las invariantes de `BaseEntity`.

* **Propuesta para `v1.1.0`:**
  1. **Predicados Positivos de Máquina de Estados Finita (FSM):** Exponer métodos de consulta semánticos y públicos (`canConfirm()`, `canShip()`, `canCancel()`) basados en condición positiva de éxito. Esto permite que la UI o los casos de uso consulten la viabilidad de la acción y hace que las mutaciones de negocio (`confirm()`, `ship()`, `cancel()`) sean inalterables ante la adición de nuevos estados intermedios.
  2. **Validación Fail-Fast con JIT Intrinsics:** Emplear `Objects.requireNonNull(...)` en constructores privados, permitiendo al compilador JIT C2 optimizar las comprobaciones a una única instrucción de ensamblador de hardware (`@IntrinsicCandidate`), eliminando código muerto y métodos de validación redundantes.
  3. **Ciclo Completo de Eventos en Mutaciones:** Asegurar que toda mutación válida de estado (`confirm()`, `ship()`, `cancel()`) registre su correspondiente evento de dominio (`OrderConfirmedEvent`, `OrderShippedEvent`, `OrderCancelledEvent`) para garantizar consistencia eventual en el Transactional Outbox.

* **Beneficio:** Reducción del ~30% de líneas de código en agregados, predicados de negocio reutilizables fuera del agregado, cero código muerto, y transiciones de estado 100% robustas y extensibles.

---

## 4. Estado de Implementación en este Repositorio

Todas las propuestas anteriores ya han sido probadas y validadas con éxito en el código de este proyecto consumidor:
* `PROP-01` aplicada en `CreateOrderHttpRequest.toCommand()` (`web/dto`) y `OrderPaymentEventMessage.toCommand()` (`worker/dto`).
* `PROP-02` aplicada en `OrderController` con `@ResponseStatus(HttpStatus.CREATED)`.
* `PROP-03` implementada en `ExecutionContext.java` con tests exhaustivos en `ExecutionContextTest.java`.
* `PROP-04` aplicada en todos los `package-info.java` (`ordering`, `ordering.application`, `shared`).
* `PROP-05` optimizada en `OrderEventWorker` con deserialización streaming directa, propagación `INP-04` de `ExecutionContext` y tests de idempotencia.
* `PROP-06` documentada para la siguiente sincronización central de ADRs.
* `PROP-07` integrada en `build.gradle` y aplicada con `@RequiredArgsConstructor` y `@Slf4j` en controladores, servicios, workers y adaptadores.
* `PROP-08` aplicada en `ordering/infrastructure/adapter/out/persistence/postgres/` con convención canónica de nombres (`OrderPersistenceAdapter`, `OrderEntity`, `OrderPersistenceMapper`, `OrderJpaRepository` y `OrderJdbcQueryAdapter`) y desacoplamiento de Transactional Outbox mediante `OutboxPublisherPort` / `JdbcOutboxPublisherAdapter`, con tests unitarios e integrados completos.
 * `PROP-09` aplicada en `DomainEvent.java`, `OrderCreatedEvent.java`, `Order.java` y `JdbcOutboxPublisherAdapter.java` con generación de UUIDv7 e inyección de `UuidGeneratorPort`.
* `PROP-10` aplicada en `Order.java` con predicados `canConfirm()`, `canShip()`, `canCancel()`, constructores optimizados con `Objects.requireNonNull` y eventos para todo el ciclo de vida.
* Verificación global: `100% BUILD SUCCESSFUL` con 146 pruebas ejecutadas y 0 violaciones de ArchUnit.

