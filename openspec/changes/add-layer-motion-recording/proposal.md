## Why

Hoy las capas (PNG y textos) tienen una única posición, escala y rotación fijas
para todo el vídeo, así que no se pueden animar. Además, el pellizco para
escalar y rotar **no responde** en el dispositivo: el gesto vive en cada
`LayerBox`, y en Compose cada puntero se *hit-testea* al bajar, por lo que el
segundo dedo —que casi siempre cae fuera del sticker— nunca llega al nodo y el
zoom de dos dedos jamás se activa. Se necesita una base de gestos fiable en todo
el lienzo y, sobre ella, poder **grabar el movimiento** de los objetos en el
tiempo y verlo igual en la exportación.

## What Changes

- **Corregir los gestos de transformación**: los gestos pasan a detectarse en el
  lienzo completo (no en cada capa), con *hit-test* de la capa bajo el dedo. Un
  dedo arrastra, dos dedos escalan y rotan **en cualquier punto del lienzo**, y
  un toque selecciona. Esto arregla el pellizco que hoy no responde.
- **Keyframes por capa**: nueva lista de fotogramas clave `(tiempo, x, y, escala,
  rotación)` con interpolación. Sin keyframes, la capa mantiene su
  comportamiento estático actual.
- **Modo de grabación de movimiento**: un modo de edición nuevo ("Grabar
  movimiento") reproduce el vídeo y muestrea los gestos del usuario en tiempo
  real, creando keyframes en la posición actual del cabezal. Incluye
  indicador de grabación, marcas de keyframes en la línea de tiempo y acciones
  para borrar la animación de una capa.
- **Reproducción animada en la vista previa**: la capa se transforma según los
  keyframes en el tiempo de reproducción, no solo con su estado estático.
- **Exportación fiel**: la exportación interpola los keyframes en cada
  fotograma, de modo que el movimiento grabado se ve en el MP4.
- **Ajustes manuales** coherentes: editar posición/escala/rotación con la capa
  animada actualiza (o crea) el keyframe del tiempo actual en lugar de
  sobrescribir toda la animación.

## Capabilities

### New Capabilities
- `layer-motion`: keyframes por capa, grabación de movimiento en tiempo real
  durante la reproducción, reproducción animada en la vista previa y
  exportación fiel del movimiento.

### Modified Capabilities
- `timeline-editing`: los gestos de mover, escalar y rotar capas se detectan a
  nivel de lienzo y funcionan con dos dedos en cualquier punto, corrigiendo el
  pellizco que hoy no responde.

## Impact

- **Modelo**: `OverlayLayer` gana `keyframes: List<LayerKeyframe>`; nuevo tipo
  `LayerKeyframe` y lógica de interpolación (nuevo `model/LayerAnimation.kt`).
- **Editor**: `EditorScreen.kt` rehace el manejo de gestos (una superficie de
  puntero en el lienzo, *hit-test* manual y resolución de la capa objetivo),
  añade el modo de grabación y dibuja marcas de keyframes; `EditorViewModel`
  gana acciones de grabación/edición de keyframes.
- **Exportación**: `Overlays.kt` (`LayerBitmapOverlay.getOverlaySettings`)
  interpola el transform en `presentationTimeUs`; `LayerBitmaps.buildOverlay`
  ya no calcula la escala una sola vez.
- **Sin dependencias nuevas** ni cambios de permisos; sigue siendo un pipeline
  local con Media3. Sin cambios de ruptura: las capas sin keyframes conservan
  el comportamiento actual.
- **Tests**: unitarios de interpolación y de *hit-test*/resolución de gestos;
  instrumentados de grabación y reproducción animada.
