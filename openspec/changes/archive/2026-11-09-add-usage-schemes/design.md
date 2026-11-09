# Design: Esquemas de Uso y Plantillas por Etapas (Usage Schemes)

## Context

VidCam cuenta con un modelo de proyecto (`Project`) serializable con `kotlinx.serialization` y un almacén de proyectos (`ProjectStore`). Las capas y fondos soportan transiciones (`LayerTransitionPreset`), animaciones procedimentales (`PngAnimationPreset`, `KenBurnsMode`) y velocidad (`AnimationSpeed`). Sin embargo, no existe una abstracción desacoplada de archivos concretos para reutilizar coreografías temporales y de composición.

## Goals / Non-Goals

**Goals:**
- Definir un modelo de datos puro y desacoplado de archivos locales (`UsageScheme`) basado en compases temporales (`stagesCount`, `stageStart`, `stageEnd`).
- Implementar la función de cálculo de tiempos (`SchemeTiming`) para convertir etapas a intervalos exactos de milisegundos (`startMs`, `endMs`).
- Crear `SchemeStore` para almacenar, listar y recuperar esquemas JSON en disco, junto a presets integrados de fábrica (`FactorySchemes`).
- Integrar la conversión bidireccional: aplicar un esquema sobre un `Project` y extraer un esquema desde un `Project`.
- Proporcionar UI en Compose para explorar esquemas, aplicarlos y guardar composiciones actuales como esquemas.

**Non-Goals:**
- Sincronización en la nube o marketplace de plantillas online (los esquemas se comparten directamente por JSON / WhatsApp / Telegram).
- Modificar el pipeline de exportación de vídeo de Media3 (un esquema se traduce en un `Project` estándar de VidCam, por lo que el exportador actual procesa todo de forma nativa).

## Decisions

### 1. Representación por Etapas (Modular Stages) vs. Segundos Absolutos
- **Decisión**: El esquema define `totalDurationSec` (ej. 15) y `stagesCount` (ej. 5). Los slots definen su ventana temporal como `stageStart` y `stageEnd` (1-indexado).
- **Razón**: Permite pensar en términos de ritmo ("intro en etapas 1 a 2, desarrollo en 2 a 4, cierre en 3 a 5"). Además, permite escalar la duración total (de 15 s a 30 s) sin romper la sincronía ni reescribir cada capa.
- **Alternativa descartada**: Tiempos absolutos en milisegundos en el JSON. Descartada porque hace el esquema rígido y complejo de editar manualmente.

### 2. Desacoplamiento de Archivos y Placeholders
- **Decisión**: Los slots de imagen/sticker en el esquema (`SchemeSlot`) no guardan rutas absolutas locales (`/data/user/0/...` o `content://...`). Utilizan identificadores semánticos (`id`, `label`, `placeholderType`, stickers preinstalados en la app o recursos por defecto). Al aplicar el esquema en el editor, se inicializan con placeholders visuales editables.
- **Razón**: Máxima portabilidad. Un esquema JSON enviado por WhatsApp a otro dispositivo funcionará de inmediato sin errores de archivo no encontrado.

### 3. Almacenamiento desacoplado con `SchemeStore`
- **Decisión**: Crear `SchemeStore(rootDir: File)` con una API análoga a `ProjectStore`, testeable directamente en JVM sin depender del SDK de Android.
- **Razón**: Sigue la arquitectura limpia del proyecto y permite validación rápida en tests unitarios.

### 4. Preset de fábrica destacado: "5 Etapas en Cascada (15s)"
- **Decisión**: Incluir en código (`FactorySchemes.kt`) el esquema de 15 segundos y 5 etapas solicitado por Alex (fondo paneo 1..5, imagen 1 en 1..2, imagen 2 en 2..4, imagen 3 en 3..5, audio en 1..5).
- **Razón**: Ofrece valor inmediato desde el primer arranque sin obligar al usuario a diseñar un esquema desde cero.

## Risks / Trade-offs

- **[Riesgo]** Conflicto entre capas existentes y slots al aplicar un esquema sobre un proyecto con edición avanzada previa.
  - *Mitigación*: Ofrecer opción de "Reemplazar capas" o "Añadir a proyecto actual", con soporte total de Deshacer (`UndoHistory`).
- **[Riesgo]** Clíps de vídeo grabados con duración distinta a la del esquema.
  - *Mitigación*: La duración del esquema fija `targetDurationMs` en proyectos sin clips; si hay clips, la ventana del vídeo se adapta y los slots de capas se distribuyen proporcionalmente sobre la duración real del montaje.
