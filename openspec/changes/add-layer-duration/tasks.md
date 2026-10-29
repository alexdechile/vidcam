## 1. Reglas de tiempo en el modelo

- [x] 1.1 Crear `model/LayerTiming.kt` con `MIN_DURATION_MS = 300L`, `timelineMs(projectTotalMs)` (duración real del proyecto, o 30 s si aún no hay clips), `durationMs(layer, total)` y `isVisibleAt(layer, positionMs)`
- [x] 1.2 Implementar `withDuration(layer, durationMs, total)` (conserva el inicio, acota la duración al tiempo disponible) y `withStart(layer, startMs, total)` (conserva la duración, acota el inicio para no pasar del final)
- [x] 1.3 Implementar `fitToTimeline(layer, total)` para sanear capas que queden fuera tras acortar el vídeo
- [x] 1.4 Tests unitarios de `LayerTiming` en `app/src/test/.../model/LayerTimingTest.kt`: sin clips usa 30 s, mínimo de 0,3 s, máximo = tiempo disponible desde el inicio, cambio de inicio conservando duración, inicio acotado al final, `isVisibleAt` en los bordes y `fitToTimeline`
- [x] 1.5 Añadir en `util/TimeUtils.kt` un formateador de segundos con décima y coma decimal (`2,5 s`) y su test

## 2. Acciones del ViewModel

- [x] 2.1 Añadir `setLayerDuration(id, durationMs)` y `setLayerStart(id, startMs)` en `EditorViewModel`, usando `update("layer-duration:$id")` y `update("layer-start:$id")` para agrupar el arrastre en un único paso de deshacer
- [x] 2.2 Usar las mismas reglas al añadir capas (`addPngLayerWithUri`, `addTextLayer`) para que una capa nueva abarque el vídeo actual con la ventana válida

## 3. UI de duración en la lista de capas

- [x] 3.1 Extraer la fila de capa de `TimelineSection` a un composable `LayerRow(layer, totalMs, viewModel, canMoveUp, canMoveDown, onEdit)` que muestre el nombre, el resumen de tiempo, los botones de orden y el de borrado
- [x] 3.2 Añadir el slider "En pantalla" en segundos (mínimo 0,3 s, máximo el tiempo disponible) con el valor visible junto al control
- [x] 3.3 Añadir el slider "Aparece en" en segundos (0 s .. final menos duración) que conserve la duración de la capa
- [x] 3.4 Añadir el botón "Editar capa" que abre `LayerEditDialog`, y pasarle la duración real del proyecto
- [x] 3.5 Reescribir los controles de tiempo de `LayerEditDialog` con `LayerTiming` y con los límites reales, dentro de una columna desplazable

## 4. Coherencia de vista previa y exportación

- [x] 4.1 Usar `LayerTiming.isVisibleAt` en la vista previa (`TimelinePreview`) y en la vista previa de exportación en lugar de comparar el rango a mano
- [x] 4.2 Hacer que `ExportPreviewDialog` elija un instante representativo (0 s si hay capas visibles ahí, si no el centro de la ventana de la primera capa) y use ese instante para el fondo con `VideoWindowMath.backgroundAt`
- [x] 4.3 Mostrar en el resumen de exportación cuántas capas se verán en ese instante

## 5. Verificación y documentación

- [x] 5.1 Revisar que `Overlays.kt` no necesita cambios (la ventana temporal ya se aplica por fotograma) y que una capa con duración corta se oculta en el MP4
- [x] 5.2 Actualizar `README.md` con la duración en pantalla por capa y `alex.md` con el trabajo realizado
- [ ] 5.3 Ejecutar la verificación en CI (`lint test assembleDebug assembleDebugAndroidTest`) y corregir lo que falle
