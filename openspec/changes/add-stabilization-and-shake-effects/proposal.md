## Why

VidCam permite grabar y editar vídeos verticales con filtros clásicos y encuadre ancho, pero faltaban dos herramientas clave de dinámica de cámara:

1. **Estabilizador de imagen**: Al grabar cámara en mano en movimiento (caminando, tomas activas), el sensor registra vibraciones bruscas involuntarias. Se requiere activar la estabilización óptica y electrónica (OIS / EIS) nativa del hardware mediante CameraX/Camera2 para obtener tomas fluidas y estables.
2. **Efecto de Temblor (Shake / Cámara temblorosa)**: Para darle impacto cinematográfico, dinamismo o simular un temblor/terremoto/cámara en mano agitada a clips grabados o importados, se necesita un efecto de temblor procedimental configurable por niveles de intensidad (suave, medio, fuerte), sincronizado de manera idéntica entre la vista previa de ExoPlayer y la exportación de Media3 Transformer.

## What Changes

- **Estabilización de imagen en grabación**:
  - `CameraRecorder` implementa control de estabilización de vídeo (EIS) y estabilización óptica (OIS) vía `Camera2CameraControl`.
  - La interfaz de grabación incorpora un control conmutable ("Estabilizador: Sí/No") para activar o desactivar la estabilización de cámara.

- **Efecto de Temblor (Shake) en clips**:
  - Nuevo enum `ShakeIntensity` (`NINGUNO`, `SUAVE`, `MEDIO`, `FUERTE`) añadido al modelo en `Project.kt`.
  - Campo `shake: ShakeIntensity = ShakeIntensity.NINGUNO` agregado a `VideoClip`, manteniendo retrocompatibilidad total con proyectos JSON existentes.
  - Implementación de `ShakeEffect.kt` que genera una `MatrixTransformation` de Media3 calculando traslaciones periódicas multicapa y micro-rotaciones en función del `presentationTimeUs`, con escala de compensación para evitar bordes negros.
  - Selector de temblor en la barra de grabación (para aplicar al clip grabado) y en la edición de cada clip en `ClipRow`.
  - Integración en `videoPlayer.setVideoEffects` (vista previa en vivo) y `VideoExporter.buildSequences` (exportación MP4).
  - Integración en el sistema de deshacer/rehacer (`UndoHistory`).

## Capabilities

### New Capabilities
- `specs/camera-stabilization/spec.md`: Grabación de vídeo con estabilización óptica y electrónica (EIS/OIS) mediante CameraX y Camera2 interop.
- `specs/clip-shake-effect/spec.md`: Efecto de vibración y temblor de cámara procedimental por clip con intensidades seleccionables.

### Modified Capabilities
- `specs/timeline-editing/spec.md`: Soporte para configurar y persistir la intensidad de temblor por clip y deshacer cambios.

## Impact

- **Modelo**: `Project.kt` incorpora `ShakeIntensity` y `VideoClip.shake`.
- **Captura**: `CameraRecorder.kt` añade soporte para activar/desactivar estabilización de cámara.
- **Efectos**: Nuevo archivo `ShakeEffect.kt` con matemática de temblor y transformaciones Media3.
- **Editor**: `EditorViewModel` agrega `setClipShake`, y `EditorScreen.kt` expone selector de chips de temblor y botón de estabilización.
- **Exportación**: `VideoExporter.kt` incluye la transformación de temblor en la cadena de efectos del clip.
- **Persistencia**: Compatible hacia atrás sin migración; clips existentes cargan con `ShakeIntensity.NINGUNO`.
