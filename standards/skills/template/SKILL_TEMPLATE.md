---
name: <nombre-del-skill-en-kebab-case>
description: >-
  <Descripción en tercera persona de QUÉ hace y CUÁNDO debe activarse esta skill.
  Es el campo más crítico: el agente lo evalúa para decidir si la carga bajo demanda.
  Ejemplo: "Genera exclusivamente un agregador de dominio Java puro con control de invariantes y eventos de negocio cuando se solicita un nuevo Aggregate Root.">
---

# Skill: <Nombre Legible de la Tarea Específica>

> **Guía para el autor de la skill**: 
> Cada skill debe encapsular **una única tarea o flujo de trabajo específico** (Principio de Responsabilidad Única).
> Completa o adapta las secciones siguientes sustituyendo los valores entre `<...>` según el caso.

---

## 1. PROPÓSITO Y ALCANCE (BLAST RADIUS)

* **Objetivo puntual**: <Definir con precisión la única responsabilidad que cumple esta skill>.
* **Artefacto(s) objetivo de escritura**:
  * `<RutaDestino>/<NombreComponente>.<ext>`
* **Prohibición de alcance colateral (Blast Radius = 0)**:
  * <Indicar explícitamente qué NO debe modificar o generar (ej. no mutar otros módulos, bases de datos no solicitadas ni contratos externos)>.
* **Gestión de dependencias ausentes**:
  * Si para ejecutar esta tarea falta un elemento previo indispensable (<ej. modelo base, puerto, interfaz, spec>): **abortar de inmediato** e informar al usuario de la dependencia faltante sin inventar código o asumir contratos no especificados.

---

## 2. CONTRATO DE ENTRADA Y SALIDA (I/O)

* **Entradas Requeridas (Lectura)**:
  * `<Archivo / Spec / Contrato>`: <Descripción de qué se inspecciona para obtener la información necesaria>.
* **Salida Producida (Escritura)**:
  * `<Ruta Canónica>`: <Archivo resultante y formato esperado>.
* **Convenciones de Entorno / Formato**:
  * <Tecnología, lenguaje o versión: ej. Java 21 LTS, TypeScript strict, Python 3.12>.
  * <Convenciones de estilo: imports explícitos, sin comodines `*`, terminación de archivo con salto simple `\n`>.

---

## 3. PROCEDIMIENTO PASO A PASO (WORKFLOW)

Describe la secuencia de ejecución exacta que debe seguir el agente:

1. **Paso 1: Inspección y Pre-condiciones**:
   * Verificar la existencia de las entradas requeridas.
   * Si falta información crítica, detenerse y solicitar aclaración.
2. **Paso 2: Generación / Transformación**:
   * Aplicar la plantilla, reglas de negocio o lógica requerida para la tarea.
3. **Paso 3: Validación Posterior (Verification Gate)**:
   * Comprobar que el archivo se ha generado en la ruta canónica y cumple las restricciones formales.
   * <Opcional: Comando de validación rápida, linter o compilación>.

---

## 4. REGLAS NORMATIVAS ESTRICTAS

Reglas concretas e innegociables para el artefacto generado:

* **`[TAG-01 · MUST]`**: <Regla mandatoria esencial para este componente/tarea>.
* **`[TAG-02 · NEVER]`**: <Anti-patrón o práctica terminantemente prohibida>.
* **`[TAG-03 · SHOULD]`**: <Recomendación o buena práctica deseable si aplica>.

---

## 5. INVARIANTE DE ANATOMÍA / ESTRUCTURA

Estructura canónica requerida para el archivo o artefacto generado:

```<lenguaje>
<Plantilla o esqueleto de código/archivo con los bloques estructurales obligatorios>
```

---

## 6. PRE-FLIGHT CHECKLIST (Auto-Verificación previa a la entrega)

Checklist que el agente debe repasar mentalmente antes de emitir su respuesta:

- [ ] ¿Se genera estrictamente el artefacto solicitado sin mutaciones colaterales (`Blast Radius = 0`)?
- [ ] ¿La ruta, nomenclatura y paquete coinciden exactamente con la convención del proyecto?
- [ ] ¿Se respetan todas las reglas `MUST` y se evitaron todos los `NEVER`?
- [ ] ¿Está libre de dependencias no permitidas o inventadas?

---

## 7. EJEMPLOS (FEW-SHOT / REFERENCE)

### ❌ Anti-Patrón (Lo que NO debe hacerse)
```<lenguaje>
// Código o contenido que rompe las reglas de esta skill
```

### ✅ Formato Canónico (Resultado esperado)
```<lenguaje>
// Código o contenido perfectamente alineado con las directrices
```