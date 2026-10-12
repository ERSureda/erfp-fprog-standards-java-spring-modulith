---
name: out-db-migration-gen
description: >-
  Genera exclusivamente scripts DDL de migración Flyway iniciales e inmutables para
  PostgreSQL particionados estrictamente por Bounded Context (V<N>__init_<modulo>_tables.sql).
  Se activa bajo demanda a partir de un script o especificación SQL en bruto (de uno o
  múltiples módulos/tablas) para establecer la línea base relacional del proyecto,
  garantizando aislamiento por esquemas, claves primarias UUIDv7, concurrencia optimista
  y desacoplamiento intermodular sin claves foráneas cruzadas.
---

# Skill: Generación de Migraciones Flyway Iniciales de Base de Datos (PostgreSQL)

> **Guía para el autor de la skill**:
> Esta skill encapsula la responsabilidad exclusiva de ingerir sentencias SQL DDL sin procesar
> (procedentes de diagramas relacionales, dumps o especificaciones funcionales) y emitir los
> scripts de migración iniciales Flyway (`baseline`) para Spring Modulith y PostgreSQL 17,
> garantizando particionado modular, pureza relacional y cero claves foráneas intermodulares.

---

## 1. PROPÓSITO Y ALCANCE (BLAST RADIUS)

* **Objetivo puntual**:
  * Transformar especificaciones o sentencias SQL en bruto (pertenecientes a uno o múltiples módulos de negocio) en scripts de migración iniciales Flyway (`V<N>__init_<modulo>_tables.sql`) atómicos, inmutables y normalizados según los estándares de arquitectura modular.
* **Artefacto(s) objetivo de escritura**:
  * `src/main/resources/db/migration/V<N>__init_<modulo>_tables.sql` (un archivo independiente por cada Bounded Context identificado, con numeración secuencial consecutiva).
* **Prohibición de alcance colateral (Blast Radius = 0)**:
  * **NUNCA** modificar ni sobrescribir scripts de migración previos ya versionados en `src/main/resources/db/migration/` (las migraciones Flyway son inmutables).
  * **NUNCA** alterar ni redefinir los objetos de infraestructura compartida de `V1__init_shared_infrastructure.sql` (extensiones `uuid-ossp`, `btree_gist`, tablas `outbox_events` y `processed_events`).
  * **NUNCA** modificar archivos de configuración de Spring Boot (`application*.yaml`) ni alterar `hibernate.ddl-auto: validate`.
  * **NUNCA** generar código Java (entidades `@Entity`, repositorios, mappers o controladores); esta skill genera exclusivamente DDL SQL para Flyway.
  * **NUNCA** declarar restricciones de clave foránea (`FOREIGN KEY` / `REFERENCES`) entre esquemas o tablas de módulos distintos (`OUT-06`, `SED-06`).
* **Gestión de dependencias ausentes**:
  * Si el script SQL de entrada contiene tablas sin asignación evidente a un Bounded Context o existen ambigüedades en las fronteras de dominio: **abortar de inmediato** y solicitar al usuario la asignación explícita de cada tabla a su módulo correspondiente antes de escribir cualquier archivo.
  * Si no existe en el proyecto el archivo de línea base transversal `src/main/resources/db/migration/V1__init_shared_infrastructure.sql`: **abortar de inmediato** e instruir al usuario para inicializar la infraestructura compartida antes de generar migraciones de negocio.

---

## 2. CONTRATO DE ENTRADA Y SALIDA (I/O)

* **Entradas Requeridas (Lectura)**:
  * `Script SQL de Entrada`: Sentencias DDL (`CREATE TABLE`, tipos, restricciones, índices) en bruto correspondientes a uno o varios submódulos.
  * `src/main/resources/db/migration/`: Inspección del directorio de migraciones para identificar la última versión `V<N>` existente y continuar la secuencia numérica sin colisiones.
  * `Mapa de Bounded Contexts`: Definición de los módulos funcionales del sistema (ej. `ordering`, `billing`, `inventory`, `catalog`, etc.).
* **Salida Producida (Escritura)**:
  * `src/main/resources/db/migration/V<N>__init_<modulo>_tables.sql`: Script DDL Flyway completo, idempotente y autocontenido por cada módulo.
* **Convenciones de Entorno / Formato**:
  * **Motor y Versión**: PostgreSQL 17 LTS.
  * **Gestor de Migraciones**: Flyway bajo convención estricta `V<N>__init_<modulo>_tables.sql` (`OUT-06`, `SED-06`).
  * **Separador de versión**: Doble guion bajo obligatorio (`__`).
  * **Formato de texto**: UTF-8 sin BOM, terminación de línea Unix `\n`.
  * **Estilo SQL**:
    * Palabras clave SQL reservadas en **MAYÚSCULAS** (`CREATE SCHEMA`, `CREATE TABLE`, `UUID`, `NOT NULL`, `DEFAULT`, `PRIMARY KEY`, `BIGINT`, `TIMESTAMPTZ`, `NUMERIC`, `VARCHAR`, `CONSTRAINT`, etc.).
    * Identificadores de esquemas, tablas, columnas, restricciones e índices en minúsculas `snake_case`.
    * Calificación obligatoria del esquema en toda sentencia: `<modulo>.<tabla>` (ej. `ordering.orders`, `ordering.order_items`).

---

## 3. PROCEDIMIENTO PASO A PASO (WORKFLOW)

### Paso 1: Inspección y Análisis de Fronteras Modulares (Pre-condiciones)
1. **Inspeccionar directorio de migraciones**: Leer `src/main/resources/db/migration/` y calcular el índice inicial libre `N` (si existen `V1` y `V2`, el siguiente disponible es `V3`).
2. **Particionado por Bounded Context**:
   * Si el SQL de entrada contiene un único módulo: asignar `V<N>__init_<modulo>_tables.sql`.
   * Si el SQL de entrada contiene múltiples módulos: desagrupar las tablas por módulo funcional. Asignar un número correlativo consecutivo a cada módulo (ej. `V3__init_inventory_tables.sql`, `V4__init_billing_tables.sql`).
3. **Auditoría de Claves Foráneas**:
   * **Intramodulares** (tablas del mismo módulo, ej. `ordering.order_items` -> `ordering.orders`): Conservar la restricción `FOREIGN KEY` con nombre explícito `fk_<tabla>_<referencia>`.
   * **Intermodulares** (tablas de módulos distintos, ej. `orders` referenciando `customers`): **Eliminar terminantemente el `REFERENCES / FOREIGN KEY`**. Mantener la columna como identificador escalar desacoplado `UUID NOT NULL` (o nullable según el modelo) y generar un índice secundario para optimización.

### Paso 2: Generación y Normalización DDL (Transformation Gate)
Para cada módulo a generar:
1. **Creación del Esquema**: Incluir siempre al inicio `CREATE SCHEMA IF NOT EXISTS <modulo>;`.
2. **Normalización de Tipos y Columnas**:
   * **Clave Primaria**: `id UUID PRIMARY KEY` (diseñado para UUIDv7 secuencial generado en aplicación mediante `UuidGeneratorPort`, `SHR-02`). Eliminar `SERIAL`, `BIGSERIAL` o secuencias autonuméricas.
   * **Concurrencia Optimista (`TRX-02`)**: Añadir `version BIGINT NOT NULL DEFAULT 0` en toda tabla que persista un Aggregate Root.
   * **Auditoría Temporal**: Añadir `created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()` y `updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()`. Prohibido usar `TIMESTAMP` sin zona horaria (`DB-01`).
   * **Importes Monetarios y Decimales (`DB-02`)**: Utilizar `NUMERIC(precision, scale)` (ej. `NUMERIC(12, 2)`) acompañado de la columna de moneda `currency VARCHAR(3)`. Prohibido `FLOAT` o `DOUBLE`.
   * **Textos y Estados**: Emplear `VARCHAR(n)` con límites razonables o `TEXT`. Estados de máquina finita en `VARCHAR(32)` con valores esperados en `UPPER_SNAKE_CASE`.
   * **Booleanos**: `BOOLEAN NOT NULL DEFAULT false` (o `true`).
3. **Estrategia de Índices (`DB-03`)**:
   * Crear índices B-Tree para toda clave foránea intramodular: `CREATE INDEX idx_<tabla>_<columna> ON <modulo>.<tabla> (<columna>);`.
   * Crear índices B-Tree para todo identificador escalar intermodular (ej. `customer_id`, `product_id`, `tenant_id`).
   * Crear índices B-Tree para columnas de consulta y filtrado frecuente (ej. `status`).
4. **Escritura del archivo**: Guardar en `src/main/resources/db/migration/V<N>__init_<modulo>_tables.sql`.

### Paso 3: Validación Posterior (Verification Gate)
1. **Verificar nomenclatura**: Confirmar patrón `V<N>__init_<modulo>_tables.sql` con doble guion bajo `__`.
2. **Verificar aislamiento**: Confirmar que no exista ninguna referencia `REFERENCES` hacia otro esquema o módulo (`OUT-06`).
3. **Verificar concurrencia**: Confirmar que cada Aggregate Root cuenta con `version BIGINT NOT NULL DEFAULT 0` (`TRX-02`).
4. **Verificación de build**: Ejecutar `./gradlew test` o la suite de arquitectura para constatar que la migración es válida sintácticamente y Flyway inicializa correctamente en el entorno de pruebas.

---

## 4. REGLAS NORMATIVAS ESTRICTAS

* **`[OUT-06 · MUST] [A]`**: Las migraciones de base de datos se gestionarán exclusivamente mediante scripts versionados de Flyway en `src/main/resources/db/migration/` con la convención `V<N>__init_<modulo>_tables.sql` para el baseline de cada módulo. Cada bounded context poseerá su propio `SCHEMA` relacional en PostgreSQL (`CREATE SCHEMA IF NOT EXISTS <modulo>`). Queda terminantemente prohibido definir claves foráneas (`FOREIGN KEY`) entre esquemas o tablas de módulos distintos; la integración intermodular se realiza exclusivamente mediante identificadores escalares (`UUID`) y consistencia eventual gobernada por eventos de dominio y Transactional Outbox.
* **`[SED-06 · MUST] [A]`**: Cero tolerancia al uso de `hibernate.ddl-auto=update` o `create` en cualquier entorno (`ddl-auto: validate`). Toda nueva tabla o función base cuenta con su script de migración Flyway inmutable, schema dedicado y ausencia de FKs intermodulares.
* **`[TRX-02 · MUST] [A]`**: Los agregados de dominio deben implementar control de concurrencia optimista mediante un atributo de versión opaco (`version BIGINT NOT NULL DEFAULT 0`).
* **`[SHR-02 · MUST] [A]`**: Todos los identificadores únicos globales del sistema se generarán en la capa de aplicación o infraestructura con formato **UUIDv7** secuencial en el tiempo conforme a RFC 9562; la base de datos almacena las claves primarias como `UUID PRIMARY KEY`, prohibiéndose identificadores autonuméricos de motor (`SERIAL`, `BIGSERIAL`).
* **`[OUT-01 · NEVER] [A]`**: Las entidades de persistencia y el DDL relacional están desacoplados del modelo puro de dominio; no se exportan ni contaminan capas externas.
* **`[DB-01 · NEVER] [R]`**: Queda terminantemente prohibido el uso del tipo `TIMESTAMP` sin zona horaria; todo campo de fecha/hora debe utilizar `TIMESTAMPTZ` con valor por defecto `clock_timestamp()`.
* **`[DB-02 · NEVER] [R]`**: Queda terminantemente prohibido el uso de tipos de coma flotante (`FLOAT`, `DOUBLE`, `REAL`) para cantidades monetarias o cálculos de negocio; debe emplearse `NUMERIC(precision, scale)`.
* **`[DB-03 · MUST] [R]`**: Toda clave foránea intramodular y toda columna de referencia escalar intermodular (ej. `customer_id`, `tenant_id`) o discriminador frecuente (`status`) debe contar con un índice B-Tree explícito (`idx_<tabla>_<columna>`).
* **`[DB-04 · NEVER] [R]`**: Queda prohibido agrupar tablas de diferentes Bounded Contexts en un único archivo DDL o en el esquema `public`. Cada módulo reside en su propio archivo DDL y esquema PostgreSQL.

---

## 5. INVARIANTE DE ANATOMÍA / ESTRUCTURA

Todo script de migración generado para un módulo debe estructurarse con la siguiente anatomía canónica:

```sql
-- =========================================================================
-- V<N>__init_<modulo>_tables.sql
-- Línea base de persistencia para el Bounded Context: <modulo>
-- Cumplimiento normativo: OUT-06, SED-06, TRX-02, SHR-02, DB-01..DB-04
-- =========================================================================

-- 1. Creación de esquema aislado para el bounded context
CREATE SCHEMA IF NOT EXISTS <modulo>;

-- 2. Tabla de persistencia del Aggregate Root principal
CREATE TABLE <modulo>.<tabla_agregado> (
    id UUID PRIMARY KEY,
    -- Referencias escalares intermodulares (sin FOREIGN KEY)
    <otro_modulo>_id UUID NOT NULL,
    
    -- Atributos de negocio del agregado
    <campo_texto> VARCHAR(120) NOT NULL,
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    
    -- Control de concurrencia optimista (TRX-02)
    version BIGINT NOT NULL DEFAULT 0,
    
    -- Metadatos de auditoría temporal (DB-01)
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

-- 3. Tablas de entidades subordinadas del mismo agregado (si existen)
CREATE TABLE <modulo>.<tabla_hija> (
    id UUID PRIMARY KEY,
    <tabla_agregado>_id UUID NOT NULL,
    item_name VARCHAR(120) NOT NULL,
    quantity INT NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    
    -- Clave foránea intramodular permitida
    CONSTRAINT fk_<tabla_hija>_<tabla_agregado>
        FOREIGN KEY (<tabla_agregado>_id)
        REFERENCES <modulo>.<tabla_agregado>(id)
        ON DELETE CASCADE
);

-- 4. Índices de rendimiento para claves foráneas intramodulares
CREATE INDEX idx_<tabla_hija>_<tabla_agregado>
ON <modulo>.<tabla_hija> (<tabla_agregado>_id);

-- 5. Índices de rendimiento para referencias escalares intermodulares
CREATE INDEX idx_<tabla_agregado>_<otro_modulo>
ON <modulo>.<tabla_agregado> (<otro_modulo>_id);

-- 6. Índices de consulta frecuente y discriminadores
CREATE INDEX idx_<tabla_agregado>_status
ON <modulo>.<tabla_agregado> (status);
```

---

## 6. PRE-FLIGHT CHECKLIST (Auto-Verificación previa a la entrega)

Antes de emitir o escribir los archivos generados, repasar mentalmente:

- [ ] ¿El nombre del archivo sigue estrictamente `V<N>__init_<modulo>_tables.sql` con doble guion bajo `__` y la versión secuencial correcta?
- [ ] ¿El archivo reside exactamente en `src/main/resources/db/migration/`?
- [ ] ¿Se genera un archivo independiente por cada Bounded Context sin mezclar módulos en un solo DDL?
- [ ] ¿Se incluye `CREATE SCHEMA IF NOT EXISTS <modulo>;` al inicio?
- [ ] ¿Todas las sentencias DDL califican explícitamente el esquema (`<modulo>.<tabla>`)?
- [ ] ¿Están absolutamente eliminadas todas las claves foráneas intermodulares (`REFERENCES` a otros esquemas/módulos), manteniéndolas solo como columnas escalares `UUID`?
- [ ] ¿Toda clave foránea intramodular (dentro del mismo módulo) tiene su restricción explícita y su índice correspondiente?
- [ ] ¿Las claves primarias son `UUID PRIMARY KEY` sin generadores de motor (`SERIAL`, `BIGSERIAL`)?
- [ ] ¿Cada tabla de Aggregate Root contiene `version BIGINT NOT NULL DEFAULT 0` (`TRX-02`)?
- [ ] ¿Las columnas temporales usan `TIMESTAMPTZ` con `DEFAULT clock_timestamp()` y no `TIMESTAMP` (`DB-01`)?
- [ ] ¿Los importes monetarios usan `NUMERIC(p, s)` y `VARCHAR(3)` en lugar de coma flotante (`DB-02`)?
- [ ] ¿Están creados los índices B-Tree para referencias escalares intermodulares y campos de estado (`DB-03`)?
- [ ] ¿Se respetó `Blast Radius = 0` sin mutar migraciones existentes ni código Java?

---

## 7. EJEMPLOS (FEW-SHOT / REFERENCE)

### ❌ Anti-Patrón (Lo que NO debe hacerse)

El siguiente ejemplo muestra un archivo monolítico que viola los estándares del proyecto:

```sql
-- ARCHIVO ERRÓNEO: V2__create_all_tables.sql
-- ❌ Rompe SED-06 / OUT-06: Archivo monolítico multi-módulo que no usa la convención V<N>__init_<modulo>_tables.sql

-- ❌ Rompe OUT-06: No crea schema por módulo y usa el schema público
CREATE TABLE customers (
    -- ❌ Rompe SHR-02: Genera IDs autonuméricos con SERIAL en lugar de UUIDv7
    id SERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    -- ❌ Rompe DB-01: Usa TIMESTAMP sin zona horaria
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    -- ❌ Rompe SHR-02: SERIAL en lugar de UUID
    id SERIAL PRIMARY KEY,
    
    -- ❌ Rompe OUT-06 / DB-04: CLAVE FORÁNEA INTERMODULAR DIRECTA.
    -- El módulo 'ordering' no puede acoplarse relacionalmente a 'customer'.
    customer_id INT REFERENCES customers(id),
    
    -- ❌ Rompe DB-02: Uso de coma flotante DOUBLE PRECISION para moneda
    total_amount DOUBLE PRECISION NOT NULL,
    status VARCHAR(20) NOT NULL,
    
    -- ❌ Rompe TRX-02: Falta la columna 'version BIGINT NOT NULL DEFAULT 0' para concurrencia optimista
    
    -- ❌ Rompe DB-01: TIMESTAMP sin time zone
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_items (
    id SERIAL PRIMARY KEY,
    order_id INT REFERENCES orders(id),
    product_id INT NOT NULL, -- ❌ Falta índice en clave foránea
    price REAL NOT NULL      -- ❌ Rompe DB-02: REAL en dinero
);
```

---

### ✅ Formato Canónico (Resultado esperado)

A partir del mismo contexto de entrada (tablas de clientes y pedidos), la skill particiona y normaliza la solución en dos migraciones independientes:

#### Archivo 1: `src/main/resources/db/migration/V2__init_customer_tables.sql`

```sql
-- =========================================================================
-- V2__init_customer_tables.sql
-- Línea base de persistencia para el Bounded Context: customer
-- Cumplimiento normativo: OUT-06, SED-06, TRX-02, SHR-02, DB-01..DB-04
-- =========================================================================

-- 1. Esquema dedicado del módulo
CREATE SCHEMA IF NOT EXISTS customer;

-- 2. Tabla del Aggregate Root Customer
CREATE TABLE customer.customers (
    id UUID PRIMARY KEY,
    full_name VARCHAR(120) NOT NULL,
    email VARCHAR(160) NOT NULL,
    status VARCHAR(32) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

-- 3. Restricciones e índices
CREATE UNIQUE INDEX uq_customers_email ON customer.customers (email);
CREATE INDEX idx_customers_status ON customer.customers (status);
```

#### Archivo 2: `src/main/resources/db/migration/V3__init_ordering_tables.sql`

```sql
-- =========================================================================
-- V3__init_ordering_tables.sql
-- Línea base de persistencia para el Bounded Context: ordering
-- Cumplimiento normativo: OUT-06, SED-06, TRX-02, SHR-02, DB-01..DB-04
-- =========================================================================

-- 1. Esquema dedicado del módulo
CREATE SCHEMA IF NOT EXISTS ordering;

-- 2. Tabla del Aggregate Root Order
CREATE TABLE ordering.orders (
    id UUID PRIMARY KEY,
    
    -- Referencia escalar intermodular sin FOREIGN KEY relacional (OUT-06)
    customer_id UUID NOT NULL,
    
    amount NUMERIC(12, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    status VARCHAR(32) NOT NULL,
    
    -- Concurrencia optimista obligatoria (TRX-02)
    version BIGINT NOT NULL DEFAULT 0,
    
    -- Marcas de tiempo de auditoría con zona horaria (DB-01)
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp()
);

-- 3. Tabla subordinada perteneciente al mismo Agregado (intramodular)
CREATE TABLE ordering.order_items (
    id UUID PRIMARY KEY,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity INT NOT NULL,
    unit_price NUMERIC(12, 2) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT clock_timestamp(),
    
    -- Clave foránea intramodular válida
    CONSTRAINT fk_order_items_order
        FOREIGN KEY (order_id)
        REFERENCES ordering.orders(id)
        ON DELETE CASCADE
);

-- 4. Índices para relaciones intramodulares e intermodulares
CREATE INDEX idx_order_items_order ON ordering.order_items (order_id);
CREATE INDEX idx_order_items_product ON ordering.order_items (product_id);

-- 5. Índices de consulta frecuente para pedidos
CREATE INDEX idx_orders_customer ON ordering.orders (customer_id);
CREATE INDEX idx_orders_status ON ordering.orders (status);
```
