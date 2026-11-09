# Design

## Context

El sistema de esquemas de uso (`UsageScheme`, `SchemeTiming`, `SchemesSheet`) gestiona coreografías por etapas modulares. Sin embargo, los esquemas son intencionalmente agnósticos a archivos locales privados para ser portables. Al aplicarse a un proyecto, se requiere un mecanismo que enlace las ranuras abstractas (`SchemeSlot`, `SchemeBackground`) con los recursos multimedia locales concretos (`Uri` de fondo, `Uri` de capas PNG, textos y pista de voz opcional).

Ver `proposal.md` y `specs/usage-schemes/spec.md`.

## Goals / Non-Goals

**Goals:**
- Definir el modelo `SchemeBinding` para representar el mapeo de recursos locales a slots de un esquema.
- Implementar auto-mapeo inteligente que preseleccione fondos y capas PNG existentes del proyecto activo en orden cronológico.
- Crear la interfaz `SchemeBindingSheet` en Jetpack Compose que permita visualizar y reasignar slots, ejecutar selección múltiple en lote (`PickMultipleVisualMedia`) y configurar textos.
- Proveer el paso opcional de doblaje de voz en off, abriendo `VoiceoverSheet` tras la aplicación si el usuario lo activa.
- Garantizar atomicidad y soporte de Deshacer (`UndoHistory`).

**Non-Goals:**
- No modificar el formato de guardado de los esquemas (`UsageScheme.kt`), preservando su portabilidad sin rutas absolutas privadas.
- No implementar herramientas complejas de recorte o edición de imagen dentro del asistente de vinculación (se utilizan los controles existentes del editor).

## Decisions

### 1. Desacoplamiento entre `UsageScheme` y `SchemeBinding`
- **Decisión**: Crear una estructura independiente `SchemeBinding` en `com.vidcam.app.model.scheme` que almacena los `Uri` de fondos y de cada slot por ID (`Map<String, SlotContentBinding>`), en lugar de contaminar `UsageScheme` con rutas locales.
- **Alternativa descartada**: Guardar rutas locales en `UsageScheme.slots`. Rompería la portabilidad y la sincronización/exportación de plantillas JSON entre dispositivos.

### 2. Auto-mapeo inteligente en `SchemeTiming.prepareBinding`
- **Decisión**: Una función pura JVM `prepareBinding(scheme: UsageScheme, current: Project): SchemeBinding` que:
  - Asigna `current.backgrounds.firstOrNull()?.uri` al fondo del esquema si existe.
  - Asigna en orden las capas PNG existentes a los slots de imagen del esquema.
  - Permite al usuario tener un punto de partida listo sin tener que volver a elegir fotos que ya colocó en el lienzo.

### 3. Selección múltiple en lote vía `PickMultipleVisualMedia`
- **Decisión**: Ofrecer un botón destacado *"⚡ Asignar imágenes en lote"* que lance `PickMultipleVisualMedia(maxItems = imageSlotsCount)`. Las imágenes seleccionadas se asignan secuencialmente a los slots de imagen pendientes.
- **Alternativa descartada**: Obligar al usuario a pulsar N veces el botón de galería para N imágenes. La selección en lote ahorra múltiples pasos y fricción en móviles.

### 4. Modal flexible frente a wizard rígido
- **Decisión**: Implementar `SchemeBindingSheet` con vista general de todos los slots en una sola hoja deslizable con scroll, permitiendo editar o ignorar cualquier slot con libertad.
- **Razón**: Aprobado por el usuario como la opción más ágil y cómoda en comparación a un asistente paso a paso que bloquea la pantalla.

### 5. Doblaje opcional desacoplado
- **Decisión**: Un flag `openVoiceoverOnApply: Boolean` que, al confirmarse la aplicación, indica a `EditorScreen` que despliegue de inmediato la hoja de doblaje `VoiceoverSheet`.

## Risks / Trade-offs

- **[Riesgo]** El orden de fotos devuelto por el PhotoPicker de Android en selección múltiple puede no coincidir con la preferencia del usuario.
  $\rightarrow$ **Mitigación**: Cada slot individual muestra su miniatura o nombre con un botón para cambiar o reordenar fácilmente.
- **[Riesgo]** Aplicación parcial si el usuario cancela la selección de imágenes.
  $\rightarrow$ **Mitigación**: Los slots no asignados retienen placeholders seguros (`uri = null` o vacío) y se puede rellenar o borrar en el editor sin errores.
