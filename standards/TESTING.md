# Manual de Estrategia de Pruebas y Verificación Automatizada — Java Spring Modulith

> **Naturaleza del documento.** Especificación técnica normativa y Fuente Única de Verdad (SSOT) para el diseño, estructura, ejecución y automatización de pruebas en el Preset `java-spring-modulith`.
>
> **Documento normativo asociado.** `ARCHITECTURE.md`. Implementa y desarrolla en detalle los mecanismos de verificación técnica de las reglas `[A]` y la fiabilidad de los flujos de operación CQRS y Outbox.
>
> **Fuerza normativa.** Las reglas etiquetadas como `MUST` son obligaciones técnicas estrictas cuyo incumplimiento bloquea el pipeline de integración continua o la aprobación de Pull Requests. Las reglas `NEVER` son prohibiciones taxativas que solo admiten excepciones mediante un registro de decisión formal (`ADR`) con condición de salida escrita.

---

## 1. Metadatos de Gobernanza y Fuerza Normativa

* **Preset / Ecosistema:** `java-spring-modulith` (Java 21+ LTS / Spring Boot 3 / PostgreSQL 17 / Spring Modulith).
* **Versión del Estándar:** `v1.1.0`.
* **Estado:** `Normativo`.
* **Repositorio Semilla Asociado:** `erft-fprog-seed-java-spring`.
* **Alcance:** Aplicable a la totalidad de suites de pruebas unitarias, de integración, slices de transporte, pruebas de carga de eventos y guardianes de arquitectura en proyectos regidos por este Preset.
* **Ubicación en Catálogo Central:** `presets/java-spring-modulith/TESTING.md`.
* **Ubicación en Proyecto Consumidor:** `.standards/TESTING.md` (vía `syncStandards`).

---

## 2. Filosofía, Pirámide y Presupuesto de Ejecución (*CI Budget*)

El sistema de pruebas garantiza confianza de despliegue a producción sin comprometer la velocidad de entrega (*Time-to-Market*):

### 2.1 Principios Rectores

1. **Determinismo Absoluto:** Todo test debe producir el mismo resultado independientemente del entorno de ejecución, orden de llamada o concurrencia de hilos. Queda prohibida la dependencia de redes externas o servicios no controlados.
2. **Verosimilitud en Persistencia (Zero In-Memory DBs):** Las pruebas de persistencia se ejecutan obligatoriamente contra el motor de base de datos real de producción (PostgreSQL 17) mediante contenedores efímeros gestionados con Testcontainers. Queda terminantemente prohibido el uso de emuladores o bases de datos en memoria (H2, SQLite), dado que falsean dialectos SQL, extensiones nativas (JSONB, UUIDv7), operadores avanzados y políticas de aislamiento RLS.
3. **Cero Aserciones Triviales:** Queda prohibido escribir pruebas que verifiquen getters, setters o código autogenerado; cada test debe validar una invariante de dominio, una orquestación transaccional o un contrato de interfaz.

### 2.2 Proporción de la Pirámide de Pruebas

La distribución del esfuerzo de pruebas sigue una estructura piramidal estricta:

```text
       ▲
      / \     5%  Flujo Crítico E2E / Humo (Happy Path de 01-OPPORTUNITY.md)
     /───\
    /     \   25% Integración Confinada (Slices JPA/JDBC con Testcontainers + MockMvc)
   /───────\
  /         \ 70% Unitarias Puras (Dominio puro y UseCases en memoria < 10 ms)
 ─────────────
```

* **Base (70% — Unitarias Puras):** Pruebas de Agregados, Entidades, Value Objects y Casos de Uso aislados. Ejecución instantánea en memoria sin arranque del contexto de Spring ni contenedores (milisegundos).
* **Cuerpo (25% — Integración Confinada / Slices):** Pruebas de adaptadores de persistencia JPA/JDBC contra PostgreSQL real y slices web con MockMvc.
* **Cúspide (5% — Flujo Crítico E2E / Humo):** Verificación restringida exclusivamente al Happy Path del flujo de conversión principal definido en la especificación de producto (`01-OPPORTUNITY.md`).

### 2.3 Presupuesto Máximo de Ejecución (*CI Time Budget*)

* **Tiempo total del pipeline de pruebas:** La suite completa de tests de un proyecto (`./gradlew check`) no debe superar los **3 a 5 minutos** en los ejecutores de CI.
* **Umbral unitario:** Cada prueba unitaria de dominio debe ejecutarse en menos de **10 milisegundos**.
* **Umbral de integración:** Los tests que interactúan con base de datos deben reutilizar una única instancia singleton de contenedor efímero para evitar penalizaciones de arranque.

---

## 3. Catálogo Normativo de Reglas de Testing (`TST`)

### 3.1 Identificadores de Regla

La notación sigue la estructura unívoca: **`TST-nn · FUERZA [TIPO]`**.

* **`MUST`**: Obligación técnica estricta.
* **`NEVER`**: Prohibición técnica absoluta.
* **`[A]`**: Verificación automatizada obligatoria en el pipeline de CI.
* **`[R]`**: Verificación manual obligatoria en Pull Request mediante checklist técnico.

### 3.2 Reglas del Sistema de Pruebas

* **`TST-01 · NEVER` [A]** Se utilizarán dobles de prueba (*mocks*, *stubs*, *spies*) en pruebas de la capa de Dominio; los agregados, entidades y Value Objects se prueban exclusivamente mediante instanciación real en memoria.
* **`TST-02 · MUST` [A]** En pruebas de Casos de Uso (`application/service`), los dobles de prueba están confinados estrictamente a las interfaces secundarias de salida (`port/out`), prohibiéndose el mocking de clases de dominio o estructuras internas de aplicación.
* **`TST-03 · MUST` [A]** Toda prueba de adaptadores de persistencia debe ejecutarse contra una instancia de PostgreSQL 17 real mediante contenedores efímeros aislados (Testcontainers), prohibiéndose el uso de bases en memoria (cero H2/SQLite).
* **`TST-04 · MUST` [A]** Las pruebas de adaptadores web deben ejecutarse como slices de transporte (`@WebMvcTest`), validando exclusivamente mapeo sintáctico (`@Valid`), autorizaciones de cabecera (`ApiHeaders` ➔ `ExecutionContext`), status HTTP y serialización de `ErrorResponse`, mockeando la interfaz del caso de uso (`port/in`).
* **`TST-05 · MUST` [A]** Todo consumidor asíncrono o worker debe contar con una prueba de idempotencia obligatoria que certifique la deduplicación ante la recepción consecutiva del mismo evento (`eventId`).
* **`TST-06 · NEVER` [A]** Se utilizarán pausas ciegas de tiempo (`Thread.sleep()`) en pruebas asíncronas; toda sincronización debe efectuarse mediante herramientas reactivas o sondeo activo condicional con timeout (*Awaitility*).
* **`TST-07 · MUST` [A]** La suite de pruebas debe incorporar la ejecución obligatoria de los guardianes de arquitectura (Spring Modulith y ArchUnit) que verifiquen las reglas `ARC-*`, `DOM-*`, `APP-*`, `INP-*`, `OUT-*` y `TRX-*`, rompiendo el build ante infracciones.
* **`TST-08 · MUST` [R]** Toda prueba debe estructurarse obligatoriamente bajo el patrón **Arrange-Act-Assert (AAA)** (o *Given-When-Then*) y utilizar una convención de nomenclatura semántica que describa la intención y el escenario evaluado (`should_<ExpectedBehavior>_when_<Condition>`).

---

## 4. Topología y Delimitación de Pruebas por Capa

```text
┌────────────────────────────────────────────────────────────────────────┐
│ 4.1 DOMINIO: Unitarias Puras (Invariantes, Value Objects, Factorías)   │ ──► Cero mocks, 100% en memoria
├────────────────────────────────────────────────────────────────────────┤
│ 4.2 APLICACIÓN: Orquestación (UseCases con mocks en port/out)          │ ──► Mocks solo en dependencias I/O
├────────────────────────────────────────────────────────────────────────┤
│ 4.3 SALIDA / PERSISTENCIA: Integración Real (Contenedor Efímero)       │ ──► Motor SQL real, mapeo, RLS, Outbox
├────────────────────────────────────────────────────────────────────────┤
│ 4.4 ENTRADA / WEB: Slices de Transporte (MockMvc / HTTP Test Client)   │ ──► Validaciones, Headers, HTTP Status
├────────────────────────────────────────────────────────────────────────┤
│ 4.5 WORKERS / ASÍNCRONO: Idempotencia y Políticas de Fallo / DLQ       │ ──► Replay de eventos, deduplicación
└────────────────────────────────────────────────────────────────────────┘
```

### Matriz Operativa: Asignación de Tests por Artefacto

| Capa / Componente | Qué se Testea (Responsabilidad) | Dónde se Ubica el Test | Tipo de Test | Anotaciones Técnicas Requeridas | Dobles Permitidos |
| --- | --- | --- | --- | --- | --- |
| **`domain.model.AggregateRoot`** | Invariantes de negocio, transiciones válidas, factory methods (`create`, `reconstruct`), registro de `DomainEvent` en `registerEvent()`. | `src/test/java/.../<subdominio>/domain/` | Unitario Puro | Ninguna (JUnit 5 puro) | ❌ Cero mocks. |
| **`domain.model.ValueObject`** | Auto-validación en constructor/compact record, inmutabilidad y cálculo de valor. | `src/test/java/.../<subdominio>/domain/` | Unitario Puro | Ninguna (JUnit 5 puro) | ❌ Cero mocks. |
| **`application.service.*Service`** | Orquestación del caso de uso, flujo transaccional, propagación de excepciones de negocio (`BaseException`), mapeo a DTO `*Result`. | `src/test/java/.../<subdominio>/application/` | Unitario con Mocks | `@ExtendWith(MockitoExtension.class)` | Mocks exclusivos en `port/out`. |
| **`adapter.out.persistence.jpa`** | Mapeo bidireccional Entity-Agregado, constraints (`UNIQUE`, `CHECK`), control de concurrencia optimista (`version`). | `src/test/java/.../<subdominio>/infrastructure/adapter/out/persistence/postgres/jpa/` | Integración (Slice JPA) | `@DataJpaTest`, `@AutoConfigureTestDatabase(replace = NONE)` | ❌ Cero mocks (Postgres Real). |
| **`adapter.out.persistence.jdbc`** | Queries SQL avanzadas, funciones nativas, proyecciones directas a DTOs de lectura sin hidratar Agregados. | `src/test/java/.../<subdominio>/infrastructure/adapter/out/persistence/postgres/jdbc/` | Integración (Slice JDBC) | `@JdbcTest`, `@AutoConfigureTestDatabase(replace = NONE)` | ❌ Cero mocks (Postgres Real). |
| **`adapter.in.web.*Controller`** | Validación sintáctica (`@Valid`), extracción de cabeceras (`ApiHeaders`), mapeo a Command/Query, HTTP Status, serialización de `ErrorResponse`. | `src/test/java/.../<subdominio>/infrastructure/adapter/in/web/` | Slice de Transporte | `@WebMvcTest(controllers = *Controller.class)` | `@MockBean` en `*UseCase`. |
| **`adapter.in.worker.*Worker`** | Deserialización de eventos del outbox/broker, compuerta de idempotencia (`eventId`), reintentos y desvío a DLQ. | `src/test/java/.../<subdominio>/infrastructure/adapter/in/worker/` | Integración de Flujo | `@SpringBootTest` con Awaitility | Mocks en brokers externos. |
| **`architecture` (Gobernanza)** | Fronteras de Spring Modulith, ausencia de acoplamiento ilegal, confinamiento de JPA/JDBC, pureza del dominio. | `src/test/java/.../architecture/` | Fitness Functions | `@AnalyzeClasses(packages = "...")` | ❌ Cero mocks. |

---

### 4.1 Pruebas de Dominio (Unitarias Puras)

* **Qué se prueba:** Invariantes de `AggregateRoot`, métodos factoría estáticos (`create`, `reconstruct`), Value Objects inmutables, transiciones de estado legales y registro de eventos de dominio (`DomainEvent`) mediante `registerEvent`.
* **Qué está prohibido:** Levantar contextos de Spring (`@SpringBootTest`), abrir transacciones, simular métodos de dominio con Mockito o acceder al sistema de archivos.
* **Herramientas:** JUnit 5, AssertJ.

```java
package com.empresa.proyecto.ordering.domain;

import com.empresa.proyecto.ordering.domain.event.OrderCreatedEvent;
import com.empresa.proyecto.shared.domain.valueobject.Money;
import com.empresa.proyecto.ordering.domain.model.Order;
import com.empresa.proyecto.ordering.domain.model.enums.OrderStatus;
import com.empresa.proyecto.shared.domain.exception.ValidationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OrderTest {

    @Test
    @DisplayName("should_RegisterOrderCreatedEvent_when_OrderIsCreated")
    void should_RegisterOrderCreatedEvent_when_OrderIsCreated() {
        // Arrange
        UUID orderId = UUID.randomUUID();
        UUID customerId = UUID.randomUUID();
        Money amount = Money.of(new BigDecimal("150.00"), Currency.getInstance("EUR"));

        // Act
        Order order = Order.create(orderId, customerId, amount);

        // Assert
        assertThat(order.getId()).isEqualTo(orderId);
        assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(order.pullDomainEvents())
            .hasSize(1)
            .first()
            .isInstanceOf(OrderCreatedEvent.class);
    }

    @Test
    @DisplayName("should_ThrowValidationException_when_AmountIsNegative")
    void should_ThrowValidationException_when_AmountIsNegative() {
        // Arrange & Act & Assert
        assertThatThrownBy(() -> Money.of(new BigDecimal("-10.00"), Currency.getInstance("EUR")))
            .isInstanceOf(ValidationException.class);
    }
}
```

---

### 4.2 Pruebas de Casos de Uso (Aplicación)

* **Qué se prueba:** Orquestación técnica del flujo de negocio: recuperación de la entidad vía repositorio, invocación del método de negocio del agregado, llamada a persistencia y mapeo al DTO de salida (`*Result`). Verificación del lanzamiento de excepciones canónicas de `shared` (`ResourceNotFoundException`, `ConflictException`).
* **Aislamiento:** Los puertos secundarios (`application/port/out`: repositorios, publicadores de eventos, clientes externos) se mockean con Mockito. El dominio y los comandos/queries son instancias reales.
* **Herramientas:** `@ExtendWith(MockitoExtension.class)`, Mockito, AssertJ.

```java
package com.empresa.proyecto.ordering.application.service;

import com.empresa.proyecto.ordering.application.command.CreateOrderCommand;
import com.empresa.proyecto.ordering.application.port.out.OrderRepositoryPort;
import com.empresa.proyecto.ordering.application.result.OrderResult;
import com.empresa.proyecto.ordering.domain.model.Order;
import com.empresa.proyecto.shared.application.context.ExecutionContext;
import com.empresa.proyecto.shared.application.context.UserType;
import com.empresa.proyecto.shared.application.port.out.ExecutionContextPort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateOrderServiceTest {

    @Mock
    private OrderRepositoryPort orderRepository;

    @Mock
    private ExecutionContextPort executionContextPort;

    @InjectMocks
    private CreateOrderService createOrderService;

    @Test
    @DisplayName("should_CreateOrderAndPersist_when_CommandIsValid")
    void should_CreateOrderAndPersist_when_CommandIsValid() {
        // Arrange
        UUID tenantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        when(executionContextPort.current()).thenReturn(
            new ExecutionContext(UserType.TENANT_USER, tenantId, userId, Set.of("USER"))
        );

        CreateOrderCommand command = new CreateOrderCommand(new BigDecimal("150.00"), "EUR");
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        // Act
        OrderResult result = createOrderService.execute(command);

        // Assert
        assertThat(result).isNotNull();
        verify(orderRepository).save(any(Order.class));
    }
}
```

---

### 4.3 Pruebas de Adaptadores de Salida (Persistencia Real)

* **Qué se prueba:** Mapeo explícito bidireccional (Agregado ➔ Entidad JPA ➔ Agregado), restricciones de clave foránea (`FOREIGN KEY`), constraints de unicidad (`UNIQUE`), comprobaciones `CHECK`, control de concurrencia optimista (`version = version + 1`) y consultas nativas con `NamedParameterJdbcTemplate`.
* **Infraestructura:** Contenedor efímero de PostgreSQL 17 gestionado mediante Testcontainers.
* **Herramientas:** `@DataJpaTest` o `@JdbcTest`, `@AutoConfigureTestDatabase(replace = NONE)`.

```java
package com.empresa.proyecto.ordering.infrastructure.adapter.out.persistence.jpa;

import com.empresa.proyecto.shared.domain.valueobject.Money;
import com.empresa.proyecto.ordering.domain.model.Order;
import com.empresa.proyecto.shared.infrastructure.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(OrderPersistenceAdapter.class)
class OrderPersistenceAdapterTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private OrderPersistenceAdapter adapter;

    @Test
    @DisplayName("should_PersistAndHydrateOrderCorrectly")
    void should_PersistAndHydrateOrderCorrectly() {
        // Arrange
        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), Money.of(new BigDecimal("100.00"), Currency.getInstance("EUR")));

        // Act
        adapter.save(order);
        Optional<Order> loaded = adapter.findById(order.getId());

        // Assert
        assertThat(loaded).isPresent();
        assertThat(loaded.get().getId()).isEqualTo(order.getId());
        assertThat(loaded.get().getVersion()).isEqualTo(0);
    }
}
```

---

### 4.4 Pruebas de Adaptadores de Entrada (Web Slices)

* **Qué se prueba:** Validación sintáctica de payloads (`@Valid`), extracción y propagación de cabeceras perimetrales (`X-Tenant-Id`, `X-User-Id`, `X-Roles`), códigos de estado HTTP (200, 201, 204) y serialización estructurada de `ErrorResponse` ante excepciones capturadas por `GlobalExceptionHandler`.
* **Aislamiento:** Se levanta únicamente la capa web con `@WebMvcTest`. El caso de uso (`application/port/in`) se sustituye mediante `@MockBean`.
* **Herramientas:** Spring MockMvc, `@WebMvcTest`.

```java
package com.empresa.proyecto.ordering.infrastructure.adapter.in.web;

import com.empresa.proyecto.ordering.application.port.in.CreateOrderUseCase;
import com.empresa.proyecto.ordering.application.result.OrderResult;
import com.empresa.proyecto.shared.infrastructure.adapter.in.web.ApiHeaders;
import com.empresa.proyecto.shared.infrastructure.adapter.in.web.context.ExecutionContextFilter;
import com.empresa.proyecto.shared.infrastructure.adapter.in.web.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = OrderController.class)
@Import({GlobalExceptionHandler.class, ExecutionContextFilter.class})
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CreateOrderUseCase createOrderUseCase;

    @Test
    @DisplayName("should_Return201Created_when_PayloadIsValid")
    void should_Return201Created_when_PayloadIsValid() throws Exception {
        // Arrange
        UUID orderId = UUID.randomUUID();
        when(createOrderUseCase.execute(any())).thenReturn(
            new OrderResult(orderId, "PENDING", new BigDecimal("100.00"))
        );

        // Act & Assert
        mockMvc.perform(post("/api/v1/orders")
                .header(ApiHeaders.TENANT_ID, UUID.randomUUID().toString())
                .header(ApiHeaders.USER_ID, UUID.randomUUID().toString())
                .header(ApiHeaders.ROLES, "OPERATOR")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "amount": 100.00,
                        "currency": "EUR"
                    }
                """))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(orderId.toString()))
            .andExpect(jsonPath("$.status").value("PENDING"));
    }

    @Test
    @DisplayName("should_Return400BadRequest_when_ValidationFails")
    void should_Return400BadRequest_when_ValidationFails() throws Exception {
        mockMvc.perform(post("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                        "amount": -5.00,
                        "currency": ""
                    }
                """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
            .andExpect(jsonPath("$.errors").isNotEmpty());
    }
}
```

---

### 4.5 Pruebas de Consumidores Asíncronos (Workers)

* **Qué se prueba:** Deserialización de payloads JSON desde la tabla de Outbox o broker, propagación del contexto de correlación, ejecución del caso de uso de destino, deduplicación mediante la compuerta de idempotencia y reintentos ante fallos transitorios frente a permanentes.
* **Aislamiento:** Los brokers de red externos se simulan o ejecutan en bus de eventos en memoria; la persistencia de idempotencia (`processed_events`) se valida sobre el motor PostgreSQL real.
* **Herramientas:** JUnit 5, Awaitility para sincronización no bloqueante conforme a `TST-06`.

```java
package com.empresa.proyecto.ordering.infrastructure.adapter.in.worker;

import com.empresa.proyecto.ordering.application.port.in.ProcessOrderPaymentUseCase;
import com.empresa.proyecto.shared.infrastructure.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@SpringBootTest
class OrderEventWorkerTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private OrderEventWorker orderEventWorker;

    @Autowired
    private NamedParameterJdbcTemplate jdbcTemplate;

    @MockBean
    private ProcessOrderPaymentUseCase processOrderPaymentUseCase;

    @BeforeEach
    void setUp() {
        jdbcTemplate.getJdbcTemplate().execute("TRUNCATE TABLE processed_events");
    }

    @Test
    @DisplayName("should_DeduplicateEvent_when_ReceivedTwiceWithSameEventId")
    void should_DeduplicateEvent_when_ReceivedTwiceWithSameEventId() {
        // Arrange
        UUID eventId = UUID.randomUUID();
        String payloadJson = """
            {
                "orderId": "%s",
                "paymentStatus": "CONFIRMED"
            }
        """.formatted(UUID.randomUUID());

        // Act: Primera entrega
        orderEventWorker.consumeOrderPaymentEvent(eventId, payloadJson);

        // Assert: Esperar procesamiento asíncrono e inserción en tabla de idempotencia
        await().atMost(Duration.ofSeconds(3)).untilAsserted(() -> {
            Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM processed_events WHERE event_id = :id",
                Map.of("id", eventId),
                Integer.class
            );
            assertThat(count).isEqualTo(1);
        });

        // Act: Replay del mismo evento (misma clave eventId)
        orderEventWorker.consumeOrderPaymentEvent(eventId, payloadJson);

        // Assert: Verificar deduplicación estricta (el caso de uso solo se invocó una vez)
        verify(processOrderPaymentUseCase, times(1)).execute(any());
    }
}
```

---

## 5. Pruebas Transversales Críticas: Multi-Tenancy e Idempotencia

### 5.1 Test Mandatorio de Fuga entre Inquilinos (*Tenant Leakage Test*)

En arquitecturas multi-tenant basadas en Row-Level Security (RLS) en PostgreSQL, es obligatorio implementar la prueba negativa de aislamiento:

1. **Configuración:** Persistir un registro perteneciente al `Tenant A` en la base de datos.
2. **Ejecución:** Inicializar el contexto de ejecución bajo el `Tenant B` e intentar leer, actualizar o borrar dicho registro a través de los adaptadores correspondientes.
3. **Aserción:** La consulta debe retornar resultado vacío (0 registros) o lanzar `ResourceNotFoundException` / `ForbiddenException`. Queda estrictamente prohibido que los datos del `Tenant A` sean visibles para el `Tenant B`.

```java
@Test
@DisplayName("should_PreventAccessToDataFromAnotherTenant_when_QueryingUnderDifferentTenant")
void should_PreventAccessToDataFromAnotherTenant_when_QueryingUnderDifferentTenant() {
    // 1. Arrange: Crear registro bajo el contexto del Tenant A
    UUID tenantA = UUID.randomUUID();
    UUID tenantB = UUID.randomUUID();
    UUID orderId = UUID.randomUUID();

    executeInTenantContext(tenantA, () -> {
        jdbcTemplate.update(
            "INSERT INTO ordering.orders (id, tenant_id, amount, status, version) VALUES (?, ?, 100.00, 'PENDING', 0)",
            orderId, tenantA
        );
    });

    // 2. Act & 3. Assert: Intentar leer el registro bajo el contexto del Tenant B
    executeInTenantContext(tenantB, () -> {
        List<UUID> results = jdbcTemplate.query(
            "SELECT id FROM ordering.orders WHERE id = ?",
            (rs, rowNum) -> rs.getObject("id", UUID.class),
            orderId
        );
        assertThat(results)
            .as("El Tenant B no debe tener visibilidad sobre datos del Tenant A")
            .isEmpty();
    });
}

private void executeInTenantContext(UUID tenantId, Runnable action) {
    transactionTemplate.executeWithoutResult(status -> {
        jdbcTemplate.execute("SET LOCAL app.current_tenant_id = '" + tenantId + "'");
        action.run();
    });
}
```

---

### 5.2 Test Mandatorio de Atomicidad del Transactional Outbox

Garantiza que la mutación del agregado y la inserción del evento en `outbox_events` formen una única transacción atómica:

1. **Configuración:** Ejecutar un caso de uso o comando de negocio que mute estado y registre un `DomainEvent`.
2. **Escenario de Fallo:** Provocar una excepción de base de datos o un fallo forzado antes del cierre transaccional.
3. **Aserción:** Se certifica que la transacción ejecutó un rollback atómico: ni el registro del agregado ni la fila en `outbox_events` deben persistirse en la base de datos.

```java
@Test
@DisplayName("should_RollbackAggregateAndOutboxEvent_when_TransactionFailsBeforeCommit")
void should_RollbackAggregateAndOutboxEvent_when_TransactionFailsBeforeCommit() {
    // 1. Arrange
    CreateOrderCommand command = new CreateOrderCommand(new BigDecimal("200.00"), "EUR");

    // 2. Act & Assert: Forzar un fallo antes del commit
    assertThatThrownBy(() -> testTransactionService.executeWithForcedError(command))
        .isInstanceOf(RuntimeException.class);

    // 3. Assert: Verificar que no hay registros huérfanos en orders ni en outbox_events
    Integer ordersCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM ordering.orders WHERE amount = 200.00",
        Integer.class
    );
    Integer outboxCount = jdbcTemplate.queryForObject(
        "SELECT COUNT(*) FROM outbox_events WHERE payload::text LIKE '%200.00%'",
        Integer.class
    );

    assertThat(ordersCount).isZero();
    assertThat(outboxCount).isZero();
}
```

---

### 5.3 Test Mandatorio de Idempotencia Asíncrona

Certifica la deduplicación ante la llegada de mensajes repetidos con el mismo identificador:

1. **Configuración:** Enviar un mensaje con un `eventId` unívoco al worker de procesamiento.
2. **Primera Ejecución:** El worker procesa la lógica, persiste el resultado y registra el `eventId` en la tabla de control de idempotencia.
3. **Segunda Ejecución (Replay):** Reenviar el mismo mensaje con idéntico `eventId`.
4. **Aserción:** El worker detecta el registro previo en la compuerta de idempotencia, cortocircuita la ejecución sin volver a mutar el estado y emite confirmación de éxito (*ACK*).

---

## 6. Gestión de Infraestructura de Test y Control Temporal

### 6.1 Contenedor Compartido Singleton (*Shared Singleton Container*)

Para cumplir el presupuesto de tiempo de CI (< 5 minutos) y evitar inicializaciones redundantes:

* Se prohíbe iniciar y detener contenedores de PostgreSQL por cada clase de prueba.
* Todas las pruebas de integración heredan de una clase base común que administra un contenedor singleton reutilizable a lo largo de todo el ciclo de compilación.

```java
package com.empresa.proyecto.shared.infrastructure;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

public abstract class AbstractPostgresIntegrationTest {

    private static final PostgreSQLContainer<?> POSTGRES_CONTAINER;

    static {
        POSTGRES_CONTAINER = new PostgreSQLContainer<>("postgres:17-alpine")
            .withDatabaseName("test_db")
            .withUsername("test_user")
            .withPassword("test_pass")
            .withReuse(true);
        POSTGRES_CONTAINER.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES_CONTAINER::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES_CONTAINER::getUsername);
        registry.add("spring.datasource.password", POSTGRES_CONTAINER::getPassword);
    }
}
```

* **Limpieza de Estado:** Cada prueba de persistencia corre bajo `@Transactional` de prueba o ejecuta un script rápido de truncado de esquemas entre ejecuciones, preservando la misma instancia activa del contenedor.

---

### 6.2 Factorías de Datos (Patrón Object Mother / Builders)

* Se prohíbe instanciar manualmente entidades complejas o construir JSONs repetitivos dentro del cuerpo de los tests.
* Los datos de prueba se generan mediante factorías especializadas (*Object Mother*) ubicadas en `src/test/java/.../fixture/`, las cuales proveen instancias válidas preconfiguradas con sobreescritura selectiva de campos.

```java
package com.empresa.proyecto.ordering.fixture;

import com.empresa.proyecto.shared.domain.valueobject.Money;
import com.empresa.proyecto.ordering.domain.model.Order;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.UUID;

public final class OrderFixture {

    private OrderFixture() {}

    public static Order createPendingOrder(UUID customerId) {
        return Order.create(UUID.randomUUID(), customerId, Money.of(new BigDecimal("100.00"), Currency.getInstance("EUR")));
    }
}
```

---

### 6.3 Control Temporal Determinista

* Toda lógica dependiente de fechas, expiraciones o marcas de auditoría debe consumir la interfaz `UtcClockPort`.
* En las pruebas unitarias y de integración se inyecta un reloj fijo determinista (`Clock.fixed(instant, ZoneOffset.UTC)`), garantizando que las pruebas nunca dependan del reloj del hardware o del sistema operativo.

---

## 7. Guardianes de Arquitectura (*Fitness Functions*)

La suite de pruebas contiene tests automatizados que ejecutan Spring Modulith y ArchUnit en compilación. Si se comete una violación arquitectónica, `./gradlew check` aborta de inmediato:

| Guardián de Arquitectura | Regla Verificada | Condición de Fallo Automatizada en Test |
| --- | --- | --- |
| **`Fronteras Modulares`** | `ARC-01`, `ARC-02` | `ApplicationModules.of(ApiApplication.class).verify()` detecta dependencias cruzadas entre módulos fuera de `@NamedInterface("application")`. |
| **`Aislamiento de Dominio`** | `DOM-01` | ArchUnit detecta importaciones de Spring, JPA, Jackson o Lombok en paquetes `..domain..`. |
| **`Inmutabilidad de Agregados`** | `DOM-04` | ArchUnit detecta métodos públicos que comiencen por `set*` en `..domain.model..`. |
| **`Estructura de Casos de Uso`** | `APP-01` | Clases en `..application.service..` no implementan exactamente una interfaz `*UseCase` o no terminan en `Service`. |
| **`Confinamiento de Persistencia`** | `OUT-01`, `OUT-05` | Clases anotadas con `@Entity` o que utilicen `NamedParameterJdbcTemplate` residen fuera de sus paquetes de infraestructura designados. |
| **`Demarcación Transaccional`** | `TRX-01` | Anotaciones `@Transactional` presentes fuera de la capa `..application..`. |

```java
package com.empresa.proyecto.architecture;

import com.empresa.proyecto.ApiApplication;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import jakarta.persistence.Entity;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.transaction.annotation.Transactional;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.*;

@AnalyzeClasses(packages = "com.empresa.proyecto", importOptions = ImportOption.DoNotIncludeTests.class)
class ArchitectureFitnessArchTest {

    @Test
    @DisplayName("ARC-01: Las fronteras de Spring Modulith deben ser respetadas")
    void modulith_modules_must_be_free_of_cycles() {
        ApplicationModules.of(ApiApplication.class).verify();
    }

    @ArchTest
    static final ArchRule DOM_01_domain_must_be_pure =
        noClasses().that().resideInAPackage("..domain..")
            .should().dependOnClassesThat().resideInAnyPackage(
                "org.springframework..",
                "jakarta.persistence..",
                "com.fasterxml.jackson..",
                "lombok.."
            ).as("DOM-01: El dominio no debe depender de frameworks ni ORMs");

    @ArchTest
    static final ArchRule DOM_04_aggregates_must_not_have_setters =
        methods().that().areDeclaredInClassesThat().resideInAPackage("..domain.model..")
            .and().arePublic()
            .should().notHaveNameStartingWith("set")
            .as("DOM-04: Los agregados y entidades de dominio no deben exponer setters");

    @ArchTest
    static final ArchRule APP_01_services_must_implement_usecase =
        classes().that().resideInAPackage("..application.service..")
            .should().haveSimpleNameEndingWith("Service")
            .andShould().implement(descriptiveInterface("UseCase"))
            .as("APP-01: Los servicios de aplicación deben implementar interfaces UseCase");

    @ArchTest
    static final ArchRule OUT_01_entities_confined_to_jpa =
        classes().that().areAnnotatedWith(Entity.class)
            .should().resideInAPackage("..infrastructure.adapter.out.persistence..jpa..")
            .as("OUT-01: Las entidades JPA deben residir exclusivamente en adaptadores de persistencia JPA");

    @ArchTest
    static final ArchRule TRX_01_transactional_only_in_application =
        methods().that().areAnnotatedWith(Transactional.class)
            .should().beDeclaredInClassesThat().resideInAPackage("..application..")
            .as("TRX-01: @Transactional reside exclusivamente en la capa de aplicación");

    private static com.tngtech.archunit.base.DescribedPredicate<com.tngtech.archunit.core.domain.JavaClass> descriptiveInterface(String suffix) {
        return new com.tngtech.archunit.base.DescribedPredicate<>("una interfaz terminada en " + suffix) {
            @Override
            public boolean test(com.tngtech.archunit.core.domain.JavaClass input) {
                return input.getInterfaces().stream().anyMatch(i -> i.getSimpleName().endsWith(suffix));
            }
        };
    }
}
```

---

## 8. Matriz Pedagógica de Anti-Patrones y Checklist de Pull Request

### 8.1 Matriz de Anti-Patrones Comunes en Testing

| ❌ Anti-Patrón | 💥 Por qué falla | ✅ Solución Normativa | Regla Asociada |
| --- | --- | --- | --- |
| **Mockear el ORM / Persistencia** | La prueba pasa en verde pero oculta fallos de sintaxis SQL, violaciones de tipos o bloqueos en motor. | Probar la persistencia contra el motor PostgreSQL 17 real en contenedor efímero. | `TST-03` |
| **Mockear el Dominio en UseCases** | Desconecta la orquestación de las reglas de negocio; los tests pasan en verde incluso si las invariantes del agregado están rotas. | Usar instancias reales de agregados; mockear únicamente puertos de salida (`port/out`). | `TST-01`, `TST-02` |
| **Pausas Ciegas (`Thread.sleep`)** | Provoca ralentización severa y falsos positivos (*flaky tests*) por contención de CPU en los entornos de CI. | Sincronización condicional no bloqueante con Awaitility (`await().until(...)`). | `TST-06` |
| **Tests Dependientes de Orden** | Un test pasa solo si corre después de otro, enmascarando polución de estado en la base de datos. | Limpieza transaccional o truncado atómico de tablas; cada test es 100% aislado. | `TST-08` |
| **Revalidar Reglas de Dominio en Web Slices** | Duplica esfuerzo: un cambio de regla de negocio rompe tests en múltiples capas a la vez. | El test web solo valida códigos HTTP, cabeceras perimetrales y serialización JSON. | `TST-04` |
| **Ignorar Pruebas de Multi-Tenancy** | Deja expuesta la mayor vulnerabilidad de seguridad: fuga accidental de datos privados entre clientes. | Implementar obligatoriamente el Test de Fuga de Inquilino (*Tenant Leakage Test*). | `TST-08` |
| **Reiniciar Contenedores por Clase** | Multiplica por 5 a 10 el tiempo de compilación, superando el presupuesto de CI de 5 minutos. | Utilizar el patrón Singleton para reutilizar la instancia de Testcontainers. | `TST-03` |

---

### 8.2 Checklist de Verificación para Pull Requests

* [ ] ¿Las pruebas de Dominio están completamente libres de mocks, frameworks o anotaciones de Spring? (`TST-01`)
* [ ] ¿En los tests de Casos de Uso solo se mockean interfaces secundarias de salida (`port/out`)? (`TST-02`)
* [ ] ¿Toda nueva consulta de persistencia se prueba contra PostgreSQL 17 real en Testcontainers (cero H2/SQLite)? (`TST-03`)
* [ ] ¿Los tests de controladores web verifican status HTTP, headers y contratos de error mediante `@WebMvcTest`? (`TST-04`)
* [ ] ¿Se ha incluido la prueba de idempotencia con reenvío de `eventId` duplicado en nuevos consumidores asíncronos? (`TST-05`)
* [ ] ¿La suite de tests asíncronos utiliza Awaitility en lugar de `Thread.sleep()`? (`TST-06`)
* [ ] ¿Se ejecutan y pasan en verde los guardianes automatizados de ArchUnit y Spring Modulith? (`TST-07`)
* [ ] ¿Las nuevas tablas con aislamiento multi-tenant cuentan con su respectivo Test de Fuga de Inquilinos? (`TST-08`)
* [ ] ¿El tiempo total de compilación y ejecución (`./gradlew check`) se mantiene por debajo de los 5 minutos? (`TST-08`)