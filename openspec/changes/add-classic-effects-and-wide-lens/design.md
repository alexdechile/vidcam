# Design — Efectos clásicos y lente ancho en VidCam

## Context

El proyecto ya exporta vídeo vertical H.264/AAC (30 s máx) componiendo
`Composition` con `EditedMediaItemSequence` (clips recortados con velocidad
`clip.playbackSpeed`), una secuencia opcional de música y efectos por capa
(`OverlayEffect`) — ver `VideoExporter.buildSequences` y el proposal.md.
La vista previa del editor usa **ExoPlayer plano** (`TimelinePreview`),
mientras que la exportación usa **media3 Transformer**, dos motores distintos.
Hoy el selector de grabación solo ofrece velocidad.

Los requisitos (specs/clip-classic-effects/spec.md) piden que los filtros de
color sean idénticos entre vista previa y MP4 exportado, que seleccionables
al grabar y por clip, y persistentes en el JSON.

Esta capability fue modificada a partir de `clip-speed` (selector de
grabación) y `export-overlay` (exportación), así que el primer problema de
diseño es **cómo reflejar el mismo filtro en dos motores distintos** sin
duplicar la matemática y sin añadir dependencias.

## Goals / Non-Goals

**Goals**
- Un solo origen de verdad para la apariencia del clip (matriz de color 4x5 /
  RGBA) usado por preview y por export, sin divergencia perceptual.
- Filtros y lente ancho aplicados por clip al grabar y por clip al editar,
  persistidos en el JSON.
- Vista previa del editor muestra el filtro de color aplicado al vídeo de la
  línea de tiempo; el selector de grabación muestra el chip del filtro actual.
- Lente ancho: usar la cámara física de mayor FOV (ultra gran angular) cuando
  exista; si no existe, encuadre digital 0.65x (alejar manteniendo resolución)
  lo más fiel posible a lo que se verá en el MP4.

**Non-Goals**
- Implementar filtros GL custom fuera de lo que trae media3-effect 1.5.1
  (ojo de pez real requiere shader a medida — no viable en CI sin preview).
- Cambiar la resolución de salida ni el límite de 30 s del proyecto.
- Aplicar filtros a la cámara en vivo (sensor crudo) mientras se graba.
- Migración de proyectos antiguos: el JSON sin el campo carga con `NINGUNO`
  (default de la serialización).

## Decisions

### D1: Un modelo de color por clip, sin dependencia de binarios de shaders

Cada `VideoClip` lleva `colorFilter: ColorFilterPreset` donde el valor es un
enum puro (`NINGUNO, SEPIA, BLANCO_Y_NEGRO, VINTAGE, ALTO_CONTRASTE`) y
`wideLens: WideLensMode` (enum `NORMAL`/`GRUPO`, en lugar del `Boolean`
descartado para dejar el modelo extensible si mañana hay otro encuadre). Los
enums viven en `Project.kt` (capa de modelo), no en `media3.effect`, para
que la matemática y los tests JVM no dependan de Android. Una función
`ColorFilterPreset.media3Effect(): RgbMatrix` mapea el enum a la matriz
equivalente, y `WideLensMode.media3Transformation()` a la transformación de
encuadre, en `classicEffects.kt`. El mismo preset produce un `ColorMatrix`
(de Compose) para usarlo también desde una capa Compose si hiciera falta.

**Alternativa descartada**: guardar la `RgbMatrix` completa serializada en el
JSON. Garantiza fidelidad idéntica, pero rompe la compatibilidad del JSON
(arrays de 16 índices por clip) y complica la persistencia; además el proyecto
antiguo (sin campo) tendría que migrarse. Se prefiere el enum con un mapeo
cerrado y testeable (tests de JVM para cada matriz).

### D2: Vista previa = ExoPlayer plano + `setVideoEffects` (mismo motor Media3)

Media3 Transformer (export) soporta `Effects` con `RgbAdjustment`/`RgbMatrix`.
La vista previa del editor usa ExoPlayer. ExoPlayer (desde media3 1.5) también
expone `Player.setVideoEffects(List<Effect>)`, que aplica la MISMA familia de
efectos de `media3-effect` sobre el fotograma decodificado: exactamente
`RgbMatrix` (filtro) y `ScaleAndRotateTransformation` (encuadre ancho) que usa
el exportador. Por eso `TimelinePreview` aplica por clip, al cambiar el índice
del `MediaItem` en reproducción, la misma lista de efectos que aplicará
`buildSequences` en la exportación (mismo motor, fidelidad exacta).

**Alternativa descartada**: aplicar el filtro con `graphicsLayer` +
`ColorFilter` de Compose sobre el `PlayerView`. No afecta al contenido de un
`SurfaceView` (se dibuja en una superficie aparte) y reintroduce dos motores;
se guarda el `ColorMatrix` por preset como utilidad pura, pero el camino de
preview es `setVideoEffects`.

### D3: Encuadre ancho — digital 0.65x idéntico en preview y export

Para el selector de lente ancho al grabar y por clip:
- El clip lleva `wideLens = GRUPO` y el encuadre se resuelve **digitalmente**:
  `WideLensMode.media3Transformation()` devuelve una
  `ScaleAndRotateTransformation` con escalado `0.65` (alejar), que se aplica
  con la misma matriz en la vista previa (`setVideoEffects`) y en la
  exportación (`Effects` del `EditedMediaItem`).
- La cámara física de mayor FOV (ultra gran angular vía CameraX
  `PhysicalCameraSelector`) queda como **mejora futura** fuera de alcance: hoy
  solo hay `CameraRecorder.toggleLens` (frontal/trasera) y no hay dispositivo
  físico disponible para validar selección por FOV en CI.

**Decisión clave**: la simetría preview/export se logra usando la misma
transformación `ScaleAndRotateTransformation` de media3-effect en ambos
caminos. La resolución de salida no cambia: solo la matriz de transformación
del contenido dentro del mismo canvas, así `FrameGeometry`/`TextOverlay` no se
tocan.

## Risks / Trade-offs

- **[Simetría preview/export del filtro]**: ambos caminos usan la MISMA
  `RgbMatrix` de media3-effect: preview vía `Player.setVideoEffects`, export
  vía `Effects` del `EditedMediaItem`. Fidelidad exacta por mismo motor. Hay
  tests de JVM que comparan cada preset contra su matriz esperada.
- **SurfaceView no recibe ColorFilter de Compose**: por eso la preview NO usa
  `graphicsLayer`/`ColorFilter`; usa `setVideoEffects`, que sí afecta al
  contenido decodificado antes de pintarlo en cualquier superficie.
- **Ojo de pez digital (barril) no disponible**: media3-effect 1.5.1 no trae
  distorsión de barril; un shader GL a medida rompería preview/export y
  arriesgaría el CI. → Se acepta: el "lente ancho" es encuadre digital 0.65x
  de alejamiento, idéntico en ambos caminos.
- **UGA físico fuera de alcance**: seleccionar la cámara física de mayor FOV
  requiere dispositivo con ultra gran angular y pruebas en hardware; hoy no hay
  forma de validarlo en CI. Queda documentado como mejora futura.
- **Persistencia sin migración**: proyecto antiguo sin `colorFilter`/`wideLens`
  carga con `NINGUNO`/`NORMAL` gracias a los defaults de kotlinx.serialization
  (`ignoreUnknownKeys`). Se prueba con un JSON legacy (escenario "proyecto
  antiguo").

## Migration Plan

1. Agregar `colorFilter` y `wideLens` a `VideoClip` (defaults seguros).
2. `EditorViewModel`: `setClipColorFilter(id, filter)`, `setClipWideLens(id, mode)`
   con presupuesto de 30 s (sin cambio de duración), historial.
3. `EditorScreen`: chips de filtro + botón "Grupo" en selector de grabación y
   por clip.
4. `VideoExporter`: aplicar `RgbMatrix` + `ScaleAndRotateTransformation` por
   clip en `buildSequences`.
5. CI (repos cloud) hasta verde, tag v1.2.0, release, enviar APK por WhatsApp.

Rollback: revertir el commit del cambio; el JSON antiguo sigue cargándose sin
migración (los nuevos campos solo aplican a clips re-grabados/reeditados).
