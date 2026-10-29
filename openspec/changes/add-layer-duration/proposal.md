## Why

Hoy una capa (PNG o texto) aparece en el vídeo desde el segundo 0 hasta el
final: su ventana `startMs..endMs` existe en el modelo y la respetan la vista
previa y la exportación, pero **el usuario no puede cambiarla**. El único
diálogo que la ajustaba (`LayerEditDialog`) nunca se abre porque
`editingLayer` jamás se asigna, así que en la práctica todo sticker dura todo
el vídeo. Eso obliga a recortar mentalmente el vídeo para saber cuándo se verá
un sticker, y hace imposible el uso más natural de una capa PNG: un rótulo que
aparece durante unos segundos y se va.

## What Changes

- **Slider de duración por capa**: cada capa de la lista "Capas" muestra un
  control deslizante **"En pantalla"** en segundos, que define cuánto tiempo
  permanece visible. El valor se acota al tiempo real del proyecto, no a los
  30 s teóricos.
- **Slider de inicio**: junto al anterior, **"Aparece en"** define en qué
  segundo entra la capa, conservando su duración. Así se puede colocar un
  sticker a mitad del vídeo sin tocar el resto.
- **Edición de capa accesible**: se añade el botón "Editar capa" en cada fila,
  que abre el diálogo existente (hasta ahora inalcanzable) con tamaño, fuente,
  posición y los mismos controles de tiempo, todos acotados a la duración real
  del proyecto en lugar de a los 30 s teóricos.
- **Vista previa de exportación coherente**: el resumen de exportación elige un
  fotograma donde haya capas visibles y cuenta cuántas se verán, en vez de
  asumir siempre t=0.
- **Reglas de tiempo compartidas**: una capa de modelo única (`LayerTiming`)
  define mínimo, máximo y ajuste de la ventana, usada por la UI, la vista previa
  y el editor para que no haya tres versiones de la misma regla.

## Capabilities

### New Capabilities
- `layer-timing`: duración en pantalla y segundo de aparición configurables por
  capa, con límites derivados de la duración real del proyecto.

### Modified Capabilities
- `timeline-editing`: la lista de capas deja de ser solo informativa y ofrece
  control de tiempo y acceso a la edición de la capa.

## Impact

- **Modelo**: nuevo `model/LayerTiming.kt` con las reglas puras (sin Android,
  testeable en JVM). `OverlayLayer` no cambia: `startMs`/`endMs` ya existen.
- **Editor**: `EditorScreen.kt` gana `LayerRow` con los dos sliders, el botón de
  edición y reescribe `LayerEditDialog` con límites reales; `EditorViewModel`
  gana `setLayerDuration` y `setLayerStart`, ambas deshacibles con agrupación
  por gesto.
- **Exportación**: sin cambios en `Overlays.kt`; ya aplica la ventana temporal
  fotograma a fotograma. `ExportPreviewDialog` pasa a elegir mejor el instante
  del resumen.
- **Sin dependencias nuevas**, sin permisos nuevos, sin cambios de formato de
  proyecto: los JSON guardados siguen cargando igual y las capas antiguas
  mantienen la ventana completa.
- **Tests**: unitarios nuevos de `LayerTiming` (acotado, mínimo, ajuste al
  cambiar de duración e inicio, y duración al acortar el vídeo).
