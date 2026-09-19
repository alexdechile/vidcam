## Context

Ver `proposal.md` para la motivación. Punto de partida relevante:

- Las capas (`OverlayLayer`) tienen un transform estático (`x`, `y`, `scale`,
  `rotationDeg`) y una ventana `startMs..endMs`; la vista previa y la exportación
  lo replican.
- El gesto vive hoy en cada `LayerBox` (`EditorScreen.kt`, `pointerInput` por
  capa). Como cada puntero se *hit-testea* al bajar, el segundo dedo del pellizco
  suele caer fuera del sticker y el nodo nunca recibe dos punteros: por eso el
  pellizco no responde.
- La vista previa obtiene el tiempo con un bucle que consulta
  `videoPlayer.currentPosition` cada 100 ms.
- La exportación usa `LayerBitmapOverlay.getOverlaySettings(presentationTimeUs)`,
  que Media3 invoca por fotograma; hoy calcula un transform constante.

## Goals / Non-Goals

**Goals:**
- Gestos de transformación fiables en todo el lienzo (arregla el pellizco).
- Modelo de keyframes interpolado, compartido por vista previa y exportación.
- Modo de grabación en tiempo real que muestrea gestos sobre el vídeo que corre.
- Movimiento idéntico en el MP4 exportado.

**Non-Goals:**
- Curvas de easing o editor de curvas de velocidad (la interpolación es lineal).
- Persistencia del proyecto en disco (hoy el proyecto es efímero).
- Animación de opacidad, color o tamaño de fuente del texto.
- Animación del clip de vídeo o del audio.

## Decisions

### 1. Superficie de gestos a nivel de lienzo con *hit-test* propio

Se elimina el `pointerInput` de cada `LayerBox` y se coloca **una sola** superficie
de gestos en el `Box` que envuelve el vídeo y las capas. Esa superficie:

1. Al primer `down`, resuelve la capa objetivo con un *hit-test* en orden inverso
   de dibujo (la de arriba primero) sobre su caja envolvente escalada/rotada.
2. Enruta el gesto: 1 puntero → arrastre; 2 punteros → escala (ratio de
   distancias) y rotación (delta de ángulo).
3. Un toque sin desplazamiento selecciona la capa bajo el dedo.

**Por qué**: los dos dedos se *hit-testean* contra el lienzo completo, así que el
pellizco funciona aunque un dedo caiga fuera de la capa. Es la única forma de que
el gesto de dos dedos sea fiable con capas pequeñas. Alternativa descartada:
mantener el gesto por capa y "agrandar" el área táctil; no resuelve el caso de
separar mucho los dedos y complica la selección.

**Ubicación de la lógica**: la resolución de capa y el cálculo del transform se
extraen a una función pura (`LayerGestureController` / helpers en un archivo sin
dependencias de Android) para poder probarla con tests unitarios.

### 2. Modelo: keyframes opcionales por capa

```kotlin
data class LayerKeyframe(
    val timeMs: Long,
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotationDeg: Float,
)

data class OverlayLayer(
    // ... campos actuales ...
    val keyframes: List<LayerKeyframe> = emptyList(),
)
```

`keyframes` vacío ⇒ comportamiento estático actual (sin cambios de ruptura).
Una función `OverlayLayer.resolvedAt(timeMs): OverlayLayer` interpola el
transform entre las claves que rodean `timeMs` y devuelve la capa con el
transform resuelto. Vista previa y exportación llaman a esta misma función, lo
que garantiza fidelidad por construcción.

**Interpolación**: lineal para `x`, `y`, `scale`; para la rotación se interpola
por el camino angular más corto (evita el salto 359°→1°). Antes de la primera
clave y después de la última se mantiene el valor del extremo.

### 3. Grabación: muestreo de gestos con decimación

En modo grabación, cada evento de puntero con transform efectivo genera una clave
en el tiempo actual (posición del cabezal, con el mismo mapeo clip+offset que ya
usa la vista previa). Para evitar miles de claves:

- Se descarta un muestreo si no supera un umbral mínimo de movimiento o si
  transcurrieron menos de ~40 ms desde el anterior.
- Al empezar a grabar sobre una capa sin animación se siembra una clave en t=0
  (o en el inicio de la ventana de la capa) con su transform actual, para que la
  animación arranque desde donde estaba.
- Los tiempos se registran en la línea de tiempo global (no relativos a la capa)
  para que coincidan con la exportación.

### 4. Vista previa animada con tiempo por fotograma

El bucle de `delay(100)` es demasiado tosco para ver movimiento. Se cambia a un
bucle con `withFrameNanos` mientras el reproductor está en `isPlaying`, leyendo
`currentPosition` + offset del clip actual, y se resuelve `resolvedAt(positionMs)`
para cada capa visible. Se conserva el muestreo a 100 ms solo como respaldo
cuando está pausado.

**Alternativa considerada**: `VideoFrameMetadataListener` da el tiempo exacto de
presentación, pero añade acoplamiento al `PlayerView`/ExoPlayer; `withFrameNanos`
es suficiente para la precisión visual de la vista previa.

### 5. Exportación: transform por fotograma

`LayerBitmapOverlay.getOverlaySettings(presentationTimeUs)` resuelve
`resolvedAt(presentationTimeUs / 1000)` y de ahí deriva anclaje, rotación y
escala. `LayerBitmaps.buildOverlay` deja de calcular la escala una sola vez:
entrega el bitmap base (PNG decodificado o texto rasterizado a tamaño base) y el
overlay recalcula el factor de escala en cada fotograma a partir del transform
animado.

**Coste**: `OverlaySettings` es inmutable, así que se construye por fotograma.
Es una asignación pequeña frente al coste de dibujar el bitmap; se acepta. Si
aparece *jank* en gama baja, se puede cachear la última `OverlaySettings` y
reutilizarla cuando el transform no cambia materialmente (optimización
posterior, no bloquea).

### 6. Modo de edición distinto

Se añade un estado de UI `motionRecording` (y un `motionMode` para mostrar la
línea de tiempo de keyframes) en `EditorScreen`, con un botón "Grabar
movimiento" junto a los controles existentes. Las acciones de borrar animación y
borrar clave viven en el `EditorViewModel` (`updateLayerKeyframes`). El modo no
bloquea la edición normal: al salir de grabación el usuario sigue pudiendo mover
capas, y esos ajustes actualizan la clave del tiempo actual si la capa ya está
animada.

## Risks / Trade-offs

- **[Hit-test impreciso en capas rotadas]** → Se usa la caja envolvente
  transformada por rotación; es una aproximación aceptable para gestos y se
  documenta. El caso estático actual (sin rotación) es exacto.
- **[Explosión de keyframes durante una grabación larga]** → Decimación por
  umbral de movimiento y de tiempo; además se ordenan e insertan por tiempo para
  mantener la interpolación barata.
- **[Preview a 60 fps con varias capas puede consumir batería]** → Solo se itera
  por fotograma mientras el reproductor está en marcha; en pausa no hay bucle.
- **[Cambio de posición del cabezal por el bucle REPEAT_MODE_ALL]** → Al grabar,
  si el vídeo se repite la posición vuelve a 0 y las claves nuevas quedarían
  fuera de orden; se mitiga limitando la grabación a una pasada (indicador y
  auto-stop al llegar al final) o descartando muestras cuyo tiempo sea anterior
  a la última clave.
- **[Regresión en los gestos de una capa]** → Tests unitarios de resolución de
  capa y de transform; los tests instrumentados existentes deben seguir pasando.

## Migration Plan

No hay datos persistidos: el modelo se construye en memoria por sesión, así que
no se necesita migración. Las capas sin `keyframes` mantienen el comportamiento
actual, por lo que el cambio es retrocompatible dentro de la app.

Nota de proceso: el delta de `timeline-editing` modifica una capability cuyas
specs canónicas todavía no están archivadas (`add-vidcam-mvp` está en 48/49);
conviene archivar ese cambio antes de archivar este.

## Open Questions

- Ninguna que afecte a las specs o al plan. El valor exacto del umbral de
  decimación y del auto-stop de grabación se puede ajustar en implementación sin
  cambiar el comportamiento especificado.
