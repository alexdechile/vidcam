## Context

Ver `proposal.md` para la motivación. Punto de partida verificado en el código:

- `OverlayLayer` ya tiene `startMs = 0L` y `endMs = MAX_DURATION_MS`, y una capa
  nueva al añadirse fija `endMs = project.totalDurationMs.coerceAtLeast(1000L)`
  (`EditorViewModel.addPngLayerWithUri` / `addTextLayer`).
- La vista previa filtra con `positionMs in it.startMs..it.endMs`
  (`TimelinePreview`) y el exportador aplica la misma ventana con
  `OverlaySettings.setAlphaScale(0f)` fuera de rango (`Overlays.kt`
  `LayerBitmapOverlay.getOverlaySettings`). Es decir, **el tiempo ya funciona**
  en ambos extremos.
- Los únicos controles de tiempo existentes son dos `Slider` de milisegundos
  absolutos dentro de `LayerEditDialog`, con `valueRange` hasta
  `MAX_DURATION_MS` aunque el vídeo dure 4 s. Además, ese diálogo **nunca se
  abre**: `editingLayer` se declara en `EditorScreen` y solo se lee para
  mostrarlo, sin ninguna asignación.
- La fila de capa en "Capas" solo muestra el rango como texto
  (`0:00–0:30`) y botones de orden y borrado.

## Goals / Non-Goals

**Goals:**
- Que el usuario fije con un slider cuántos segundos está en pantalla una capa.
- Que ese tiempo se mida contra la duración real del vídeo.
- Coherencia total vista previa / exportación respetando lo ya implementado.
- Un único lugar donde vivan las reglas de tiempo (sin duplicar `coerceIn`).

**Non-Goals:**
- Efectos de entrada/salida animados (fundidos, deslizamientos): la capa
  aparece y desaparece en un corte, igual que en la exportación actual.
- Múltiples ventanas por capa (una capa = un intervalo).
- Sincronizar la ventana temporal con la animación por keyframes (una capa
  animada sigue animándose dentro de su ventana, sin recortar claves).
- Cambiar el formato de proyecto guardado.

## Decisions

### 1. Slider de duración en la fila de capa, no en un diálogo

Se añade bajo cada fila de "Capas" un control **"En pantalla"** con el valor en
segundos (`2,5 s`) y, debajo, **"Aparece en"** con el segundo de entrada.

**Por qué**: el ajuste es de iteración rápida —se prueba, se mira la vista
previa y se corrige— y meterlo en un diálogo obliga a cerrar y abrir para ver
el resultado. La lista ya es el lugar donde se administran las capas (orden,
borrado), así que el control de tiempo vive donde está el contexto.

**Alternativa descartada**: `RangeSlider` con dos manijas (inicio y fin). Se ve
bien en una línea de tiempo, pero no expresa la pregunta del usuario ("¿cuántos
segundos se ve?") y con un vídeo corto las dos manijas quedan pegadas y son
difíciles de agarrar. La duración como valor propio se lee de un vistazo.

El inicio es un segundo slider **independiente** porque sin él la capa solo
puede cubrir el comienzo del vídeo (`0..duración`). Mantener la duración
constante al mover el inicio evita el clásico "muevo el inicio y se me estira la
capa".

### 2. Reglas en una capa de modelo pura: `LayerTiming`

```kotlin
object LayerTiming {
    const val MIN_DURATION_MS = 300L
    fun timelineMs(projectTotalMs: Long): Long
    fun durationMs(layer: OverlayLayer, projectTotalMs: Long): Long
    fun withDuration(layer: OverlayLayer, durationMs: Long, projectTotalMs: Long): OverlayLayer
    fun withStart(layer: OverlayLayer, startMs: Long, projectTotalMs: Long): OverlayLayer
    fun isVisibleAt(layer: OverlayLayer, positionMs: Long): Boolean
    fun fitToTimeline(layer: OverlayLayer, projectTotalMs: Long): OverlayLayer
}
```

`timelineMs` usa la duración del proyecto y, si aún no hay clips, el tope de
30 s del proyecto: así el slider funciona incluso cuando el usuario añade un
sticker antes de grabar (caso real: los tests instrumentados añaden capas sin
clips).

**Por qué** no en `OverlayLayer` ni en el ViewModel: son reglas puras con las
que la UI, el diálogo y la vista previa deben coincidir exactamente, y así se
prueban en JVM sin emulador, como ya se hace con `TimelineMath`,
`VideoWindowMath` y `LayerAnimation`.

### 3. El ajuste se hace con los métodos existentes

`setLayerDuration` y `setLayerStart` usan `update("layer-duration:$id")` /
`update("layer-start:$id")`, de modo que arrastrar el slider produce **un solo**
paso de deshacer por gesto (agrupación de `UndoHistory` por clave y ventana de
700 ms), igual que el resto de controles continuos.

### 4. La exportación no cambia

`LayerBitmapOverlay` ya consulta `presentationTimeUs` y aplica
`setAlphaScale(0f)` fuera de la ventana. Cualquier cambio aquí sería redundante
y arriesgaría desalinear vista previa y MP4. La única intervención en
exportación es de la **vista previa de resumen**: `ExportPreviewDialog`
renderizaba las capas visibles en t=0 y el fondo `backgrounds.firstOrNull()`,
así que con una capa que empieza en t=2 s el resumen mostraba el lienzo "vacío"
y parecía que la capa se había perdido. Ahora elige un instante representativo
(el 0 si hay capas visibles ahí; si no, el centro de la ventana de la primera
capa) y usa `VideoWindowMath.backgroundAt` para el fondo, que es lo que ya hace
la vista previa en reproducción.

### 5. Se revive `LayerEditDialog`

En lugar de dejarlo como código muerto, la fila de capa gana un botón "Editar
capa" que lo abre con `totalMs` real, y sus dos sliders absolutos de
milisegundos se sustituyen por los mismos `withDuration` / `withStart` de
`LayerTiming`. Así el usuario también puede ajustar tamaño, posición y fuente,
que hoy solo se pueden tocar con gestos sobre el lienzo.

## Risks / Trade-offs

- **[Cambiar la ventana deja keyframes "huérfanos"]** → Los keyframes son
  tiempo absoluto del proyecto; si la capa se acorta, las claves fuera de la
  ventana simplemente no se ven (la capa está oculta). No se borran datos: si el
  usuario vuelve a alargar la duración, la animación reaparece. Se documenta en
  la spec.
- **[Acortar el vídeo deja capas fuera del timeline]** → `durationMs` acota el
  valor mostrado y `fitToTimeline` está disponible para sanear; la exportación
  ignora el tramo posterior al final del vídeo, así que no rompe nada.
- **[Densidad visual de la lista]** → Dos sliders por capa añaden altura; la
  sección ya vive dentro de un panel con `verticalScroll` y el layout es
  compacto (etiqueta corta + slider + valor). Con muchas capas hay que
  desplazarse, igual que ya ocurre con las filas de clips.
- **[Regresión de los tests instrumentados]** → `EditorScreenTest` añade capas
  sin clips y comprueba "Capas"/"Grabar movimiento"; los cambios mantienen
  esos textos y añaden nodos nuevos, sin quitarlos.

## Migration Plan

Sin migración: `startMs` y `endMs` ya forman parte del JSON guardado y las
capas existentes conservan su ventana. No hay cambio de esquema, así que los
proyectos abiertos se comportan igual hasta que el usuario mueva un slider.

## Open Questions

- Ninguna que afecte a las specs. El paso del slider (0,1 s) y el mínimo de
  300 ms se pueden ajustar en implementación sin cambiar el comportamiento
  especificado.
