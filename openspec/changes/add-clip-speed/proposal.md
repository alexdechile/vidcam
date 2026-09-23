## Why

VidCam solo reproduce los clips a velocidad normal. Los vídeos virales de
cámara rápida y cámara lenta necesitan poder acelerar o ralentizar cada clip,
tanto en la vista previa como en el MP4 exportado, con selección de velocidad
en el momento de grabar y para cualquier clip ya en la línea de tiempo.

## What Changes

- **Velocidad por clip**: `VideoClip` gana `playbackSpeed` (0.25x, 0.5x, 1x,
  2x, 4x; valor por defecto 1x). La duración en la línea de tiempo pasa a ser
  la duración recortada dividida por la velocidad (cámara rápida acorta,
  cámara lenta alarga) y el límite de 30 s se mide sobre esa duración efectiva.
- **Selector durante la grabación**: fila de chips de velocidad junto a los
  controles de cámara; la grabación se captura a fps normal y el clip entra a
  la línea de tiempo con la velocidad elegida.
- **Selector por clip**: cada clip de la línea de tiempo permite cambiar su
  velocidad sin volver a recortarlo; al cambiarla se mantiene el límite de 30 s
  recortando el final del clip si fuera necesario.
- **Vista previa**: cada clip se reproduce a su velocidad (ExoPlayer
  `PlaybackParameters`), con la marca temporal del lienzo y la música
  sincronizadas sobre la duración efectiva.
- **Exportación**: Media3 hereda la velocidad de cada clip
  (`EditedMediaItem.Builder.setSpeed`), por lo que el MP4 conserva el
  cámara rápida/lenta con su audio.
- **Persistencia**: la velocidad se serializa en el JSON del proyecto; los
  proyectos antiguos sin el campo cargan con velocidad 1x (default de
  kotlinx.serialization).

## Capabilities

### New Capabilities
- `clip-speed`: elección de velocidad de reproducción por clip —en grabación
  y en la línea de tiempo— con su efecto en duraciones, límite de 30 s, vista
  previa, exportación y persistencia.

### Modified Capabilities
<!-- Ninguna spec existente cambia de requisitos: la velocidad es nueva y el
     resto de las capacidades (recorte, música, capas) operan sobre la duración
     efectiva sin cambiar su comportamiento. Deliberately sin deltas. -->
- Sin cambios.

## Impact

- **Modelo**: `Project.kt` (`VideoClip.playbackSpeed`, `effectiveDurationMs`),
  `TimelineMath.kt` (`totalDuration`, `trimToMax` y `clipStartMs` conscientes
  de velocidad).
- **Editor**: `EditorViewModel` añade `setClipSpeed` y pasa la velocidad al
  agregar grabaciones; `EditorScreen` añade el selector de velocidad en cámara
  y en cada fila de clip; `TimelinePreview` aplica velocidad de reproducción y
  mapa de tiempo.
- **Export**: `VideoExporter.buildSequences` aplica `setSpeed`.
- **Tests**: `TimelineMathTest` y `LayerAnimationTest` cubren duración
  efectiva y límites con velocidad.
- **Sin dependencias nuevas**: la velocidad la resuelven Media3 (ExoPlayer y
  Transformer) ya incluidos.