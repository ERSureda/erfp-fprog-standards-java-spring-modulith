# Manual de Excepciones y Errores — Java Spring Modulith

> **Naturaleza del documento.** Especificación técnica normativa y Fuente Única de Verdad (SSOT) para el diseño, jerarquía, tipado, captura y serialización de excepciones en el Preset `java-spring-modulith`.
>
> **Documento normativo asociado.** `ARCHITECTURE.md`. Implementa y desarrolla en detalle las reglas de arquitectura `INP-03` y `SHR-04` vinculadas a la decisión técnica `ADR-003`.
>
> **Fuerza normativa.** Las reglas etiquetadas como `MUST` son obligaciones técnicas estrictas cuyo incumplimiento bloquea el pipeline de integración continua o la aprobación de Pull Requests. Las reglas `NEVER` son prohibiciones taxativas que solo admiten excepciones mediante un registro de decisión formal (`ADR`) con condición de salida escrita.

---

## 1. Metadatos de Gobernanza y Fuerza Normativa

* **Preset / Ecosistema:** `java-spring-modulith` (Java 21+ LTS / Spring Boot 3 / PostgreSQL 17 / Spring Modulith).
* **Versión del Estándar:** `v1.0.0`.
* **Estado:** `Normativo`.
* **Repositorio Semilla Asociado:** `erft-fprog-seed-java-spring`.
* **Alcance:** Aplicable a todos los servicios, capas de aplicación, controladores web, brokers y workers asíncronos desarrollados bajo este stack técnico dentro del marco FProg.
* **Ubicación en Catálogo Central:** `presets/java-spring-modulith/EXCEPTIONS.md`.
* **Ubicación en Proyecto Consumidor:** `.standards/EXCEPTIONS.md` (vía `syncStandards`).

---

## 2. Filosofía y Principios de Gestión de Errores

El subsistema de errores y excepciones se rige por tres pilares de ingeniería inquebrantables:

### 2.1 Excepciones de Coste Cero o Ultrabajo (*Zero / Low-Overhead*)

En la máquina virtual de Java (JVM), entre el 90% y el 95% del coste computacional al instanciar y lanzar una excepción proviene de capturar, inspeccionar y rellenar la traza de pila de ejecución (*stack trace*) mediante la llamada nativa `fillInStackTrace()`.

* Las **excepciones de negocio, conflicto y validación sintáctica** son flujos de control alternativos normales y esperados, **no anomalías técnicas del sistema**.
* Toda excepción de negocio hereda de `BaseException`, la cual traslada el flag `category.capturesDiagnostics()` al parámetro nativo `writableStackTrace` del constructor de `Throwable`.
* Al ser este valor `false` para todos los errores funcionales, la JVM **desactiva la generación de trazas de pila y la inspección del hilo de ejecución**, eliminando el consumo innecesario de memoria y CPU en rutas críticas de alto tráfico (*hot-paths*).
* **Reutilización del catálogo cerrado de `shared`:** Para evitar la proliferación de clases vacías (*boilerplate* sin lógica añadida), el catálogo ubicado en `<namespace.base>.shared.domain.exception` es cerrado y autosuficiente. Los módulos de negocio no deben crear subclases por herencia para cada casuística; la especificidad se logra instanciando directamente la excepción de `shared` parametrizada con el `ErrorCode` del módulo.

### 2.2 Tratamiento Diferenciado: Negocio vs. Técnico

* **Excepciones de Negocio / Dominio:** Se registran con nivel `WARN` en los logs del servidor sin traza forense de pila, informando únicamente del código semántico y el motivo funcional.
* **Excepciones Técnicas / Infraestructura (`INTERNAL`):** Representan fallos imprevistos del sistema o fallos de red (pérdida de conexión a base de datos, caídas de almacenamiento, timeouts de socket). Estas **sí capturan la traza forense completa** (`writableStackTrace = true`) y se registran con nivel `ERROR` para posibilitar el diagnóstico posterior.

### 2.3 Seguridad por Defecto (*Safe-by-Default*)

* Ningún detalle interno de implementación (nombres de tablas relacionales, fragmentos de sentencias SQL, IPs, puertos o volcados de memoria) se expone jamás al exterior en errores técnicos.
* Todo fallo técnico no controlado es interceptado en el perímetro por `GlobalExceptionHandler` y enmascarado hacia el cliente con el mensaje genérico:

```text
"An unexpected error occurred"
```

---

## 3. Catálogo Normativo de Reglas de Error (`ERR`)

### 3.1 Identificadores de Regla

La notación sigue la estructura unívoca: **`ERR-nn · FUERZA [TIPO]`**.

* **`MUST`**: Obligación técnica estricta.
* **`NEVER`**: Prohibición técnica absoluta.
* **`[A]`**: Verificación automatizada obligatoria en el pipeline de CI (linters o pruebas estáticas con ArchUnit).
* **`[R]`**: Verificación manual obligatoria en Pull Request mediante checklist técnico.

### 3.2 Reglas del Sistema de Errores

* **`ERR-01 · MUST` [A]** Toda excepción lanzada en la capa de dominio o aplicación debe heredar directa o indirectamente de la clase base abstracta `<namespace.base>.shared.domain.exception.BaseException`.
* **`ERR-02 · MUST` [A]** Las excepciones funcionales y de validación deben desactivar la inspección de trazas de pila en el runtime de la JVM (`capturesDiagnostics() == false` mapeado a `writableStackTrace = false`).
* **`ERR-03 · MUST` [A]** Todo error de negocio debe asociarse obligatoriamente a un código alfanumérico inmutable y tipado mediante la implementación de la interfaz `ErrorCode`.
* **`ERR-04 · MUST` [A]** Toda respuesta de error en la API pública debe serializarse bajo el modelo estándar `ErrorResponse`, omitiendo el campo `errors` cuando no contenga violaciones de campo mediante `@JsonInclude(JsonInclude.Include.NON_EMPTY)`.
* **`ERR-05 · NEVER` [A]** Se capturarán excepciones de forma genérica para silenciarlas (`catch (Exception e) {}` vacío) ni se lanzarán tipos genéricos no tipados del lenguaje (como `RuntimeException` o `Exception` planas).
* **`ERR-06 · MUST` [R]** Todo fallo técnico capturado en adaptadores secundarios (`InfrastructureException`, `ExternalServiceException`) debe encadenar obligatoriamente la causa técnica original (`cause`) para preservar la cadena forense en logs internos.
* **`ERR-07 · NEVER` [R]** Se crearán subclases de excepción dentro de los submódulos funcionales (`<subdominio>.domain.exception.*`); los módulos deben consumir directamente las excepciones canónicas provistas en `shared` parametrizadas con su propio `ErrorCode`, salvo que se justifique en PR un flujo de trabajo operativo o compensatorio único.

---

## 4. Jerarquía Canónica de Excepciones

Ubicación transversal: `<namespace.base>.shared.domain.exception`.

### 4.1 Árbol de Herencia

```text
RuntimeException (JDK)
 └── BaseException (abstracta: porta ErrorCode y ErrorCategory)
      ├── ResourceNotFoundException       [404 NOT_FOUND]
      ├── ConflictException               [409 CONFLICT]
      ├── ValidationException             [400 VALIDATION]
      ├── ForbiddenException              [403 FORBIDDEN]
      ├── UnauthenticatedException        [401 UNAUTHENTICATED]
      ├── InfrastructureException         [500 INTERNAL]
      └── ExternalServiceException        [500 INTERNAL]
```

### 4.2 Catálogo de Excepciones Base

| Excepción | Categoría | Transporte HTTP | Comportamiento en Workers / Colas | Traza | Cuándo Utilizarla |
| --- | --- | --- | --- | --- | --- |
| **`ResourceNotFoundException`** | `NOT_FOUND` | **404** | Descarte / DLQ (No reintentable) | ❌ No | Una entidad o recurso solicitado no existe por su identificador o criterio de búsqueda. |
| **`ConflictException`** | `CONFLICT` | **409** | Descarte / DLQ (No reintentable) | ❌ No | Violación de reglas de unicidad (email duplicado) o colisión con el estado del agregado. |
| **`ValidationException`** | `VALIDATION` | **400** | Descarte / DLQ (No reintentable) | ❌ No | Infracción de invariantes de negocio; encapsula lista inmutable de `FieldViolation`. |
| **`ForbiddenException`** | `FORBIDDEN` | **403** | Descarte / DLQ (No reintentable) | ❌ No | Actor autenticado sin permisos o falta de contexto de inquilino (`tenantId`). |
| **`UnauthenticatedException`** | `UNAUTHENTICATED` | **401** | Descarte / DLQ (No reintentable) | ❌ No | Se requiere una identidad válida (`userId`) y la petición llegó de forma anónima. |
| **`InfrastructureException`** | `INTERNAL` | **500** | Reintento con backoff ➔ DLQ | ✅ Sí | Fallo técnico imprevisto en persistencia, disco, drivers SQL o timeouts locales. |
| **`ExternalServiceException`** | `INTERNAL` | **500** | Reintento con backoff ➔ DLQ | ✅ Sí | Fallo de red, timeout o respuesta corrupta de servicios downstream. |

---

## 5. Clasificación Semántica (`ErrorCategory`)

El enum `<namespace.base>.shared.domain.error.ErrorCategory` define la semántica del fallo y gobierna tanto el código de transporte HTTP como el comportamiento en flujos asíncronos y la captura de diagnóstico:

```java
package com.empresa.proyecto.shared.domain.error;

public enum ErrorCategory {
    VALIDATION(false),       // HTTP 400 | Async: No reintentable (DLQ) | Traza: false
    UNAUTHENTICATED(false),  // HTTP 401 | Async: No reintentable (DLQ) | Traza: false
    FORBIDDEN(false),        // HTTP 403 | Async: No reintentable (DLQ) | Traza: false
    NOT_FOUND(false),        // HTTP 404 | Async: No reintentable (DLQ) | Traza: false
    CONFLICT(false),         // HTTP 409 | Async: No reintentable (DLQ) | Traza: false
    INTERNAL(true);          // HTTP 500 | Async: REINTENTABLE          | Traza: TRUE

    private final boolean capturesDiagnostics;

    ErrorCategory(boolean capturesDiagnostics) {
        this.capturesDiagnostics = capturesDiagnostics;
    }

    public boolean capturesDiagnostics() {
        return capturesDiagnostics;
    }
}
```

### Clase Base Abstracta (`BaseException`)

Implementa el mecanismo de supresión de trazas para garantizar coste cero en ejecución:

```java
package com.empresa.proyecto.shared.domain.exception;

import com.empresa.proyecto.shared.domain.error.ErrorCategory;
import com.empresa.proyecto.shared.domain.error.ErrorCode;

public abstract class BaseException extends RuntimeException {

    private final ErrorCode errorCode;
    private final ErrorCategory category;

    protected BaseException(ErrorCode errorCode, ErrorCategory category, String message) {
        this(errorCode, category, message, null);
    }

    protected BaseException(ErrorCode errorCode, ErrorCategory category, String message, Throwable cause) {
        super(
            message,
            cause,
            true,                              // enableSuppression
            category.capturesDiagnostics()      // writableStackTrace (false = zero-overhead)
        );
        this.errorCode = errorCode;
        this.category = category;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public ErrorCategory category() {
        return category;
    }
}
```

---

## 6. Contratos de Identificación y Salida Web

### 6.1 Contrato `ErrorCode`

Interfaz funcional que obliga a exponer un identificador alfanumérico estable (`code() -> String`):

```java
package com.empresa.proyecto.shared.domain.error;

public interface ErrorCode {
    String code();
}
```

* **Errores Comunes (`CommonError`):** Provistos por el kernel `shared` (`RESOURCE_NOT_FOUND`, `CONFLICT`, `VALIDATION_ERROR`, `FORBIDDEN`, `UNAUTHENTICATED`, `INTERNAL_ERROR`).
* **Errores de Módulo (`<Subdominio>Error`):** Enums tipados declarados en cada Bounded Context (ej. `<namespace.base>.ordering.domain.OrderingError`) que implementan `ErrorCode`:

```java
package com.empresa.proyecto.ordering.domain;

import com.empresa.proyecto.shared.domain.error.ErrorCode;

public enum OrderingError implements ErrorCode {
    ORDER_NOT_FOUND,
    ORDER_ALREADY_SHIPPED,
    INSUFFICIENT_STOCK,
    PAYMENT_GATEWAY_TIMEOUT;

    @Override
    public String code() {
        return name();
    }
}
```

* **Patrón Canónico de Uso (Sin Crear Subclases):** Los servicios y agregados no declaran clases de excepción locales; instancian directamente las excepciones canónicas de `shared` asociándolas al `ErrorCode` del subdominio:

```java
// ✅ USO CORRECTO: Reutiliza ConflictException de shared parametrizada con OrderingError
if (order.isShipped()) {
    throw new ConflictException(OrderingError.ORDER_ALREADY_SHIPPED, "Cannot cancel an order that has already shipped");
}

// ✅ USO CORRECTO: Reutiliza ResourceNotFoundException de shared con constructor formateado
Order order = orderRepository.findById(orderId)
    .orElseThrow(() -> new ResourceNotFoundException(Order.class, orderId));
```

### 6.2 Contrato Universal de Salida JSON (`ErrorResponse`)

Toda respuesta de error en la API pública emite estrictamente este record inmutable:

```java
package com.empresa.proyecto.shared.infrastructure.adapter.in.web.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.empresa.proyecto.shared.domain.error.FieldViolation;

import java.util.List;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
    int status,
    String code,
    String detail,
    List<FieldViolation> errors
) {
    public ErrorResponse(int status, String code, String detail) {
        this(status, code, detail, List.of());
    }
}
```

* **Payload con violaciones de validación sintáctica (HTTP 400):**

```json
{
  "status": 400,
  "code": "VALIDATION_ERROR",
  "detail": "Validation failed for one or more fields",
  "errors": [
    {
      "field": "email",
      "message": "must be a well-formed email address"
    }
  ]
}
```

* **Payload de error simple (HTTP 404 / 409 / 500):** Gracias a `@JsonInclude(NON_EMPTY)`, el campo `errors` **no se emite** en el payload serializado al estar vacío, optimizando el ancho de banda:

```json
{
  "status": 409,
  "code": "ORDER_ALREADY_SHIPPED",
  "detail": "Cannot cancel an order that has already shipped"
}
```

---

## 7. Responsabilidades del Manejador Centralizado

La captura y traducción de errores reside en adaptadores perimetrales dedicados:

### 7.1 Perímetro Síncrono (Web / API)

`GlobalExceptionHandler` (`@RestControllerAdvice`) centraliza la captura de excepciones y garantiza el cumplimiento del contrato:

1. Intercepta cualquier excepción derivada de `BaseException`.
2. Mapea la categoría semántica (`ErrorCategory`) a su respectivo código de transporte HTTP.
3. Si la categoría es `INTERNAL`: registra un log de nivel `ERROR` con la traza completa forense y enmascara el campo `detail` a `"An unexpected error occurred"`.
4. Si la categoría es de negocio (`INTERNAL == false`): registra un log de nivel `WARN` sin traza y devuelve el mensaje descriptivo funcional.
5. Captura fallos de validación sintáctica de entrada (Bean Validation con `MethodArgumentNotValidException`) transformándolos en instancias de `ErrorResponse` con su lista de `FieldViolation`.
6. Captura cualquier excepción técnica no controlada (`Throwable`) devolviendo un HTTP 500 seguro y enmascarado.

```java
package com.empresa.proyecto.shared.infrastructure.adapter.in.web.exception;

import com.empresa.proyecto.shared.domain.error.CommonError;
import com.empresa.proyecto.shared.domain.error.ErrorCategory;
import com.empresa.proyecto.shared.domain.error.FieldViolation;
import com.empresa.proyecto.shared.domain.exception.BaseException;
import com.empresa.proyecto.shared.domain.exception.ValidationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorResponse> handleBaseException(BaseException ex) {
        HttpStatus status = mapCategoryToStatus(ex.category());

        if (ex.category().capturesDiagnostics()) {
            log.error("Internal technical failure [code={}]: {}", ex.errorCode().code(), ex.getMessage(), ex);
            return ResponseEntity.status(status).body(new ErrorResponse(
                status.value(),
                ex.errorCode().code(),
                "An unexpected error occurred"
            ));
        }

        log.warn("Operational business exception [code={}]: {}", ex.errorCode().code(), ex.getMessage());

        List<FieldViolation> errors = (ex instanceof ValidationException ve) ? ve.violations() : List.of();

        return ResponseEntity.status(status).body(new ErrorResponse(
            status.value(),
            ex.errorCode().code(),
            ex.getMessage(),
            errors
        ));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<FieldViolation> violations = ex.getBindingResult().getFieldErrors().stream()
            .map(err -> new FieldViolation(err.getField(), err.getDefaultMessage()))
            .toList();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            CommonError.VALIDATION_ERROR.code(),
            "Validation failed for one or more fields",
            violations
        ));
    }

    @ExceptionHandler(Throwable.class)
    public ResponseEntity<ErrorResponse> handleUnhandled(Throwable ex) {
        log.error("Unhandled unexpected exception: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            CommonError.INTERNAL_ERROR.code(),
            "An unexpected error occurred"
        ));
    }

    private HttpStatus mapCategoryToStatus(ErrorCategory category) {
        return switch (category) {
            case VALIDATION -> HttpStatus.BAD_REQUEST;
            case UNAUTHENTICATED -> HttpStatus.UNAUTHORIZED;
            case FORBIDDEN -> HttpStatus.FORBIDDEN;
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
            case INTERNAL -> HttpStatus.INTERNAL_SERVER_ERROR;
        };
    }
}
```

### 7.2 Perímetro Asíncrono (Workers / Consumidores de Eventos)

1. Si la excepción interceptada es de negocio (`category.capturesDiagnostics() == false`): el worker descarta el mensaje o lo envía directamente a la Dead-Letter Queue (DLQ), confirmando el mensaje (ACK) ante el broker para evitar bucles infinitos de reintento sobre datos que jamás serán válidos.
2. Si la excepción es técnica (`category.capturesDiagnostics() == true`): el worker programa el reintento con backoff exponencial incrementando el contador `retry_count`; superado el límite máximo configurado, deriva el mensaje a la DLQ con registro de la causa técnica original en `last_error`.

---

## 8. Matriz Pedagógica de Anti-Patrones y Checklist de Pull Request

### 8.1 Matriz de Anti-Patrones Comunes

| ❌ Anti-Patrón | 💥 Por qué falla | ✅ Solución Normativa | Regla Asociada |
| --- | --- | --- | --- |
| **Proliferación de Subclases por Módulo** | Crear clases hijas vacías (`UserConflictException`, `OrderNotFoundException`) genera decenas de archivos duplicados sin valor. | Usar directamente las excepciones canónicas de `shared` parametrizándolas con el `ErrorCode` del módulo. | `ERR-01`, `ERR-07` |
| **Silenciar Excepciones (`catch {}` vacío)** | Provoca inconsistencia transaccional al impedir el rollback y oculta errores críticos en producción. | Dejar propagar las excepciones hacia el manejador central o relanzar excepciones tipadas. | `ERR-05`, `TRX-01` |
| **Manejadores Locales por Controlador** | Fragmenta el contrato de salida y rompe la estandarización del esquema `ErrorResponse`. | Centralizar toda captura y serialización de errores en el componente perimetral global. | `ERR-04`, `INP-03` |
| **Lanzar Excepciones Genéricas (`RuntimeException`)** | Oculta la naturaleza del fallo retornando HTTP 500 para errores funcionales y degrada métricas operativas. | Lanzar exclusivamente subclases tipadas derivadas de `BaseException` (`ResourceNotFoundException`, `ConflictException`, etc.). | `ERR-01`, `ERR-03` |
| **Omitir Causa Original en Fallos Técnicos** | Destruye la cadena de error original, imposibilitando el diagnóstico forense en los logs del servidor. | Encadenar siempre el error original (`cause`) en constructores de excepciones de infraestructura. | `ERR-06` |
| **Reintentar Errores de Negocio en Colas** | Reintentar un payload con datos inválidos satura la cola, bloquea el procesamiento y desperdicia CPU. | Desviar directamente a DLQ o emitir ACK si el fallo es de categoría no técnica (`VALIDATION`, `CONFLICT`). | `ERR-02`, `TRX-05` |
| **Filtrar Datos Sensibles en Mensajes** | Los errores de negocio exponen su `detail` al cliente; emitir tokens, credenciales o secretos viola normativas de seguridad. | Restringir `detail` a explicaciones funcionales libres de datos personales o confidenciales. | `ERR-04`, `INP-03` |
| **Hardcodear Textos sin Código Semántico** | Obliga a clientes frontend y APIs consumidoras a parsear cadenas de texto frágiles en lugar de validar códigos estables. | Declarar enums o constantes que implementen `ErrorCode` por subdominio, separando el código del texto. | `ERR-03`, `SHR-04` |

### 8.2 Checklist de Verificación para Pull Requests

* [ ] ¿Toda nueva excepción introducida hereda directa o indirectamente de `BaseException`? (`ERR-01`)
* [ ] ¿Las excepciones funcionales o de validación desactivan la captura de traza en el runtime de la JVM mediante `category.capturesDiagnostics() == false`? (`ERR-02`)
* [ ] ¿Cada error funcional se asocia a un código alfanumérico tipado mediante la implementación de `ErrorCode`? (`ERR-03`)
* [ ] ¿La respuesta JSON final en la API pública cumple estrictamente la estructura de `ErrorResponse`, omitiendo `errors` cuando está vacío? (`ERR-04`)
* [ ] ¿El código está completamente libre de bloques `catch` vacíos y de lanzamientos de excepciones genéricas no tipadas (`RuntimeException`, `Exception`)? (`ERR-05`)
* [ ] ¿Los fallos técnicos en adaptadores de persistencia o red enlazan obligatoriamente el error original como causa (`cause`)? (`ERR-06`)
* [ ] ¿Se consumen directamente las excepciones canónicas de `shared` sin crear clases hijas innecesarias dentro del módulo, salvo flujo único justificado? (`ERR-07`)
* [ ] ¿Los mensajes de detalle funcional (`detail`) están completamente libres de secretos, contraseñas, tokens o volcados de base de datos? (`ERR-04`, `INP-03`)