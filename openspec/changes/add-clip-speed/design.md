## Context

VidCam graba con CameraX a fps fijo y compone en Media3: `VideoClip` es un
modelo serializable en `Project.kt`, la duración del timeline la calcula
`TimelineMath.totalDuration`, la vista previa repite los clips con ExoPlayer
(`TimelinePreview`) y la exportación los procesa con `VideoExporter`,
`EditedMediaItemSequence` en un solo sequence y `OverlayEffect` a nivel de
composición. Ver proposal.md para la motivación y
specs/clip-speed/spec.md para los requisitos.

## Goals / Non-Goals

**Goals:**
- Velocidad de reproducción por clip en cámara rápida (2x, 4x) y lenta
  (0.5x, 0.25x), con la duración efectiva gobernando timeline, límite de 30 s,
  música y capas.
- Aplicable tanto al grabar (selector junto a los controles de cámara) como a
  cualquier clip ya en la línea de tiempo.
- Coherente entre vista previa y exportación, y persistente en el JSON.

**Non-Goals:**
- Captura a alta tasa de fotogramas (120/240 fps) para cámara lenta *smooth*:
  es dependiente del hardware y CameraX no la expone de forma fiable. La
  grabación se mantiene a fps normal y la ralentización se aplica en la
  reproducción/exportación, como hace el resto del editor.
- Cambiar el *pitch* del audio de forma configurable, ni pausar/continuar la
  velocidad durante una grabación activa.

## Decisions

### Velocidad como propiedad del clip (`playbackSpeed: Float = 1f`)
Se añade al `data class VideoClip` serializable. Los proyectos antiguos sin el
campo cargan con el default 1x de kotlinx.serialization, sin migración.

### Duración efectiva = recorte / velocidad
`VideoClip.effectiveDurationMs` y `TimelineMath.totalDuration` pasan a sumar
duraciones efectivas. `clipStartMs` y el mapa de tiempo de la vista previa
usan la misma base, así música y capas se mantienen sincronizadas: el timeline
avanza en tiempo real y el clip local avanza a `posición * velocidad`.

### Exportación con `EditedMediaItem.Builder.setSpeed`
Media3 Transformer ya soporta velocidad no unitaria dentro de un sequence
(no requiere intermediate extra ni cambia el `OverlayEffect` de composición,
que muestra que el diseño no cambia el enfoque de capas sobre el timeline).
La exportación entrega el MP4 con el efecto aplicado y su audio acelerado o
ralentizado.

### Vista previa con `ExoPlayer.PlaybackParameters`
La vista previa reproduce un clip a la vez; el loop de fotogramas detecta el
índice actual y aplica `PlaybackParameters(speed, speed)` (pitch = velocidad,
para que suene como cámara lenta/rápida). La posición de timeline se calcula
como `base + currentPosition / speed`.

### Límite de 30 s en duración efectiva
`TimelineMath.trimToMax` y `EditorViewModel.updateTrim`/`setClipSpeed`
convierten el presupuesto de timeline a duración de origen multiplicando por la
velocidad del clip. Al cambiar la velocidad de un clip que excede el
presupuesto, se recorta el final del clip; si todo reduce a cero, se avisa y no
se aplica el cambio.

### Selector de velocidad
- Al grabar (timeline vacío): fila de chips 0.25x / 0.5x / 1x / 2x / 4x junto
  a los controles de cámara; la elección se pasa a `addRecordedClip`.
- Por clip: la misma fila de chips en cada `ClipRow`, que llama a
  `setClipSpeed`, registrado en el historial de deshacer.

## Risks / Trade-offs

- **Slow motion menos suave que la captura real a alta fps** → Aceptado: es la
  úncia forma fiable multiplataforma con CameraX + Media3 y encaja con el
  modelo de edición post-grabación de la app.
- **La vista previa con `setSpeed` re-pone media items al cambiar la lista** →
  La velocidad se cambia sobre el clip ya existente; el restablecimiento de la
  reproducción al inicio ya ocurre hoy con cualquier edición de la lista.
- **Cámara lenta duplica la duración y puede chocar con el límite de 30 s** →
  Mitigado recortando el clip al presupuesto disponible y avisando si no cabe
  ni un fotograma.