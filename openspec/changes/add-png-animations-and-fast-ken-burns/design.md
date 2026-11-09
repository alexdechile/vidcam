# Design — Ken Burns rápido y animaciones de PNG en capas y fondos

## Context

VidCam renderiza capas (`OverlayLayer`) y fondos (`BackgroundLayer`) en dos motores que deben coincidir cuadro a cuadro:
1. **Vista previa en Compose** (`TimelinePreview` y `ExportPreviewDialog`).
2. **Exportación MP4 en Media3 Transformer** (`LayerBitmapOverlay` para capas y `EditedMediaItem` con efectos OpenGL para fondos).

## Decisions

### D1: Ciclo base corto y `AnimationSpeed.RAPIDO` por defecto para Ken Burns
- **Problema**: Repartir un zoom de `1.0 → 1.15` a lo largo de 15 o 30 s produce una velocidad imperceptible (< 1 % por segundo).
- **Decisión**:
  - Los presets de Ken Burns usan un barrido base de `3.0 s` en velocidad `NORMAL`, `1.5 s` en `RAPIDO` (valor por defecto) y `0.85 s` en `TURBO` (o la duración del tramo si es más corta), con onda triangular suavizada (*smoothstep ping-pong*) cuando el tramo dura más que un barrido, y amplitud generosa (zoom hasta `1.45x`, paneo de `±12 %`).
  - Así el movimiento arranca con velocidad evidente desde el primer segundo y nunca se queda congelado en vídeos de 10, 15 o 30 s.

### D2: Composición ortogonal en `OverlayLayer`: Keyframes + Preset + Transición
- `OverlayLayer.resolvedAt(timeMs)` resuelve en orden:
  1. Transform base o interpolación de `keyframes` (acelerada por `animationSpeed` si el usuario elige `RAPIDO` o `TURBO` sobre keyframes grabados).
  2. Desplazamiento `(dx, dy)`, factor de escala y delta de rotación del `PngAnimationPreset`.
  3. Escala y desplazamiento vertical de la transición de entrada/salida (`LayerTransitionPreset.POP` o `DESLIZAR`).
- `OverlayLayer.resolvedAlphaAt(timeMs)` calcula la opacidad (`0f..1f`) para `FUNDIDO` y suavizado de bordes en `POP`/`DESLIZAR`, aplicándose en `graphicsLayer.alpha` (Compose) y `OverlaySettings.Builder.setAlphaScale` (Media3).

### D3: Cobertura garantizada en `BackgroundLayer` y `MatrixTransformation` en Media3
- Un fondo ocupa todo el lienzo; cualquier traslación `(dx, dy)` o rotación descubriría bordes negros si la escala fuera `1.0`.
- `ensureBackgroundCoverScale(scale, dx, dy, rotationDeg)` eleva automáticamente la escala mínima a `1 + 2·max(|dx|, |dy|) + sin(|rot|)·1.42` para que ningún preset (`KEN_BURNS_PANEO`, `FLOTAR`, `VAIVEN`, `TEMBLOR`, etc.) muestre franjas negras.
- En Media3, cada segmento de fondo (`PreparedBackdrop`) recibe un `MatrixTransformation` que convierte `(scale, dx, dy, rotationDeg)` al espacio NDC `[-1, 1]` (`dx * 2`, `-dy * 2`, `-rotationDeg`), idéntico a `graphicsLayer` en Compose.

### D4: Grabación de movimiento habilitada en vídeos solo con PNG
- Se elimina la condición `&& project.clips.isNotEmpty()` del botón `"Grabar movimiento"`.
- Cuando `clips.isEmpty()`, activar `motionRecording` reinicia `stillPositionMs = 0L` y detiene automáticamente la grabación al completar una vuelta a `outputDurationMs`.
