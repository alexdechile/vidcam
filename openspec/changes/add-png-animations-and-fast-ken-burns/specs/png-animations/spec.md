# Spec — Animaciones de PNG y Ken Burns rápido

## Requirements

### R1: Presets de Ken Burns y loops continuos en capas y fondos
- `OverlayLayer` y `BackgroundLayer` deben soportar el enum `@Serializable PngAnimationPreset`:
  - `NINGUNO`
  - `KEN_BURNS_ACERCAR`
  - `KEN_BURNS_ALEJAR`
  - `KEN_BURNS_PANEO`
  - `KEN_BURNS_DIAGONAL`
  - `PULSO`
  - `FLOTAR`
  - `VAIVEN`
  - `TEMBLOR`
- Los fondos animados deben garantizar una escala de cobertura suficiente para no descubrir bordes negros durante paneos, rotaciones o vibraciones.

### R2: Selector de velocidad (`AnimationSpeed`)
- `OverlayLayer` y `BackgroundLayer` deben soportar `AnimationSpeed` (`NORMAL`, `RAPIDO`, `TURBO`), con `RAPIDO` como valor por defecto cuando se activa una animación.
- Incrementar la velocidad (`NORMAL` → `RAPIDO` → `TURBO`) debe reducir proporcionalmente el período del barrido Ken Burns y aumentar la frecuencia de los loops procedimentales.

### R3: Transiciones de entrada y salida en capas (`LayerTransitionPreset`)
- `OverlayLayer` debe soportar `LayerTransitionPreset` (`NINGUNA`, `POP`, `DESLIZAR`, `FUNDIDO`).
- `POP` aplica un rebote elástico de escala al entrar y encoge al salir; `DESLIZAR` desplaza verticalmente al entrar y salir; `FUNDIDO` interpola el canal alfa de `0` a `1` al entrar y de `1` a `0` al salir.

### R4: Grabación de movimiento en proyectos sin clips
- El botón `"Grabar movimiento"` debe estar habilitado siempre que `project.layers.isNotEmpty()`, incluso si `project.clips.isEmpty()`.
- Al iniciar la grabación de movimiento en un proyecto sin clips, el reloj de vista previa reinicia en `0 ms` y desactiva `motionRecording` automáticamente al completar una pasada de `outputDurationMs`.
