## Why

Los proyectos de VidCam usan imágenes PNG tanto en **capas superpuestas** (`OverlayLayer`) como en **fondos** (`BackgroundLayer`), además de soportar vídeos hechos **solo con PNG** (`project.clips.isEmpty()`). Sin embargo, animar los PNG tenía tres limitaciones importantes:

1. **Ken Burns lento o manual**: No existían presets directos de Ken Burns (Acercar, Alejar, Paneo, Diagonal) para capas ni fondos, y al animar por keyframes en líneas de tiempo de 15 a 30 s la interpolación lineal producía un movimiento demasiado lento.
2. **Falta de variedad de animaciones para PNG**: Faltaban loops procedimentales vivos (**Pulso/Latido**, **Flotar**, **Vaivén/Wiggle**, **Temblor/Shake**) y transiciones de entrada/salida (**Pop/Rebote**, **Deslizar/Slide**, **Fundido/Fade**) aplicables con un toque.
3. **"Grabar movimiento" bloqueado en vídeos solo con PNG**: El botón `"Grabar movimiento"` exigía `project.clips.isNotEmpty()`, impidiendo grabar trayectorias con el dedo cuando el proyecto estaba compuesto únicamente por imágenes PNG y fondos.

## What Changes

- **Presets de Ken Burns rápido y loops continuos (`PngAnimationPreset`)** para `OverlayLayer` y `BackgroundLayer`:
  - `NINGUNO`, `KEN_BURNS_ACERCAR`, `KEN_BURNS_ALEJAR`, `KEN_BURNS_PANEO`, `KEN_BURNS_DIAGONAL`, `PULSO`, `FLOTAR`, `VAIVEN`, `TEMBLOR`.
- **Selector de velocidad de animación (`AnimationSpeed`)**:
  - `NORMAL` (`1x`), `RAPIDO` (`2x`, por defecto) y `TURBO` (`3.5x`), aplicable tanto a presets de capas y fondos como a la reproducción de keyframes grabados.
- **Transiciones de entrada y salida (`LayerTransitionPreset`)** para `OverlayLayer`:
  - `NINGUNA`, `POP` (rebote elástico al entrar y encogimiento al salir), `DESLIZAR` (slide vertical suave) y `FUNDIDO` (fade in/out de opacidad).
- **Grabación de movimiento con el dedo en vídeos solo con PNG**:
  - Se habilita `"Grabar movimiento"` siempre que haya al menos una capa (`project.layers.isNotEmpty()`), y el reloj sin clips (`stillPositionMs`) reinicia al comenzar la grabación y la detiene automáticamente al completar el ciclo.
- **Paridad total Vista Previa (Compose) ↔ Exportación MP4 (Media3)**:
  - Las capas aplican escala, posición, rotación y opacidad (`resolvedAt` + `resolvedAlphaAt`) en `LayerBox` y en `LayerBitmapOverlay`.
  - Los fondos aplican su transformación animada con escala de cobertura garantizada (sin bordes negros) en `TimelinePreview`, `ExportPreviewDialog` y `VideoExporter` mediante `MatrixTransformation`.

## Capabilities

### New Capabilities
- `specs/png-animations/spec.md`: Presets de Ken Burns acelerado, loops procedimentales, transiciones de entrada/salida y grabación de movimiento en proyectos solo con PNG.

## Impact

- **Modelo**: `Project.kt` incorpora `PngAnimationPreset`, `AnimationSpeed` y `LayerTransitionPreset`, y campos retrocompatibles en `OverlayLayer` y `BackgroundLayer`.
- **Matemática de animación**: `LayerAnimation.kt` añade el motor puro de evaluación de presets, velocidades, transiciones, opacidad y cobertura de fondos.
- **Efectos y Exportación**: `Overlays.kt` aplica la opacidad de transición y `VideoExporter.kt` aplica `MatrixTransformation` a los segmentos de fondo animados.
- **Editor UI**: `EditorViewModel.kt` y `EditorScreen.kt` exponen chips de animación, velocidad y transición en filas y diálogos de capas/fondos, y desbloquean `"Grabar movimiento"` sin clips.
- **Tests**: Nuevas pruebas unitarias en `LayerAnimationTest.kt` y `ProjectStoreTest.kt`.
