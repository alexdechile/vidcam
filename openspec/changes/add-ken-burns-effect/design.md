# Design — Ken Burns en imágenes fijas

## Context

Ver `proposal.md` para la motivación y `specs/` para el comportamiento exigido.

Estado actual relevante:

- Las imágenes fijas del proyecto son los **fondos PNG** (`Project.backgrounds`), dibujados por
  debajo del vídeo y de las capas durante su tramo `[startMs, endMs)`.
- La vista previa (`TimelinePreview` en `EditorScreen.kt`) dibuja el fondo activo con
  `VideoWindowMath.backgroundAt(positionMs)`, `Image` + `ContentScale.Crop` y el `positionMs` que
  se refresca con el reproductor (o con el reloj propio cuando no hay clips).
- La exportación (`VideoExporter.kt`) usa el compositor multi-entrada de Media3: los clips son la
  entrada 0 y la secuencia de fondos las entradas siguientes. Cada fondo se pre-renderiza como PNG
  recortado (*cover*) al tamaño exacto del fotograma (`BackgroundBitmaps.prepareFile`), por lo que
  el compositor lo dibuja con escala 1.
- Sin clips, la secuencia de fondos es la única pista de vídeo y el compositor no se configura
  hoy: el tamaño de salida sale de la propia imagen preparada.
- `VideoCompositorSettings.getOverlaySettings(inputId, presentationTimeUs)` se invoca **por
  fotograma**, así que admite transformaciones animadas sin efectos adicionales.
- `LayerBitmapOverlay` ya explota esa propiedad para la animación de capas con fotogramas clave,
  lo que confirma que el compositor respeta ajustes distintos en cada instante.

## Goals / Non-Goals

**Goals:**

- Un único cálculo puro que defina el zoom y el desplazamiento en cada instante y que usen por
  igual la vista previa y la exportación.
- Velocidad ligada a la duración: la misma animación repartida sobre el tramo visible, sin
  controles nuevos de tiempo.
- Garantizar que el encuadre siempre queda cubierto por la imagen (sin franjas negras).
- Mantener intacto el comportamiento de un proyecto sin Ken Burns, con y sin clips.

**Non-Goals:**

- Fotogramas clave o curvas por imagen: el efecto es un preset de una sola decisión.
- Ken Burns en capas PNG, textos o GIF, ni en el vídeo: las capas ya tienen su propia animación
  con grabación de movimiento y los GIF cambian de fotograma.
- Elegir el punto de interés de la foto o la dirección del desplazamiento por separado.
- Rotación, deformaciones o transiciones entre imágenes.

## Decisions

### D1: `KenBurnsMath` como única fuente de verdad

Nuevo `model/KenBurnsMath.kt`, sin Android ni Media3:

```
transformAt(mode, index, startMs, endMs, timeMs) -> KenBurnsTransform(scale, offsetX, offsetY)
progressAt(startMs, endMs, timeMs)                -> [0, 1] con suavizado
resolveMode(mode, index)                          -> ACERCAR | ALEJAR | NINGUNO
indexOf(backgrounds, id)                          -> posición de la imagen en la línea de tiempo
zoomPerSecond(startMs, endMs)                     -> zoom/segundo implícito del tramo
```

La vista previa y el exportador solo traducen `KenBurnsTransform` a su propio formato
(`graphicsLayer` en Compose, `OverlaySettings` en Media3). Como no hay dos implementaciones, no hay
deriva posible entre lo que se ve y lo que se exporta.

Alternativa descartada: efectos de Media3 (`Presentation`/`ScaleAndRotateTransformation` animados
por fotograma) sobre la secuencia de fondos. Exigiría una instancia por fondo, dependería de si el
tiempo que recibe cada elemento es local o de composición, y no tendría equivalente directo en
Compose.

### D2: Zoom total fijo y velocidad derivada de la duración

`ZOOM_AMOUNT = 0.14f` (14 % del encuadre) repartido sobre el tramo:

```
p = smoothstep(clamp((t - startMs) / (endMs - startMs), 0, 1))
ACERCAR: scale = 1 + ZOOM_AMOUNT * p
ALEJAR:  scale = 1 + ZOOM_AMOUNT * (1 - p)
```

El resultado es lo que pidió la función: la **misma** animación se ve más rápida en un tramo corto
y más lenta en uno largo, así que la velocidad depende de la duración del vídeo (o del tramo del
fondo) sin un control de velocidad aparte. `zoomPerSecond(...)` documenta y prueba esa relación
(4 s ⇒ 3,5 %/s; 20 s ⇒ 0,7 %/s).

`smoothstep` evita que el zoom arranque y termine de golpe; es determinista y se evalúa igual en
ambos motores.

Alternativa descartada: velocidad constante (zoom/segundo fijo). Haría que un fondo de 30 s
terminara con un zoom enorme, o que un tramo de 1 s apenas se moviera, y rompería la promesa de
"más rápido o más lento según la duración".

### D3: Desplazamiento acotado por el propio zoom

Se dibuja la imagen con escala `s` y se desplaza el contenido `offset`. Para que la imagen siga
cubriendo el lienzo hace falta `|offset| <= (s - 1) / 2` (en unidades normalizadas del lienzo).
El cálculo define un "drift" `d ∈ [-1, 1]` que recorre de izquierda a derecha (acercar) o al revés
(alejar) y lo multiplica por ese margen:

```
offsetX = d * (scale - 1) / 2
offsetY = 0
```

Con `scale = 1` (inicio de acercar, fin de alejar) el margen es cero, así que el desplazamiento
también: la imagen empieza y termina exactamente encuadrada. Esto hace imposible que aparezcan
franjas negras, sin necesidad de comprobaciones en tiempo de ejecución.

### D4: Dirección automática alternada por índice

`AUTOMATICO` se resuelve con el índice de la imagen ordenada por inicio: pares ⇒ `ACERCAR`,
impares ⇒ `ALEJAR`. Así una secuencia de fondos alterna el movimiento (efecto Ken Burns clásico en
montajes de fotos) y una sola imagen hace un acercamiento. `KenBurnsMath.indexOf` define ese orden
de la misma forma para la vista previa y para la exportación.

### D5: Exportación por fotograma en el compositor

`WindowCompositorSettings` recibe la lista de fondos preparados (`PreparedBackdrop`) y, para las
entradas que no son el vídeo, busca el fondo activo en `presentationTimeUs` y devuelve:

```
setScale(kb.scale, kb.scale)
setBackgroundFrameAnchor(kb.offsetX * 2, kb.offsetY * -2)
setOverlayFrameAnchor(0, 0)
```

Media3 trabaja en NDC (-1..1, eje Y hacia arriba), de ahí el factor 2 y la inversión del eje
vertical. Sin efecto (`NINGUNO`) el resultado es escala 1 y ancla 0, exactamente lo que hacía el
compositor antes, así que los proyectos sin Ken Burns no cambian.

Dos cambios necesarios en el exportador:

- **Entrada primaria sin clips**: en un proyecto de solo imágenes la secuencia de fondos es la
  entrada 0, así que el compositor se configura siempre que hay fondos preparados (antes solo con
  clips) y la ventana de vídeo solo se aplica cuando `project.clips.isNotEmpty()`. Con clips el
  comportamiento es idéntico al anterior; sin clips la salida sigue siendo del tamaño del
  fotograma y el único cambio es el transform del fondo.
- **Ventana del Ken Burns**: `PreparedBackdrop` guarda el tramo del compositor y, por separado, la
  ventana del fondo original, para que un tramo recortado por un solape de `zIndex` no desfase el
  zoom respecto de la vista previa (que usa la ventana del fondo).

### D6: Vista previa en Compose

`KenBurnsBackground` dibuja el PNG activo igual que antes (`Image` + `ContentScale.Crop`) y aplica
el transform con `graphicsLayer`:

```
scaleX = scaleY = kb.scale
translationX = kb.offsetX * ancho
translationY = kb.offsetY * alto
```

El `graphicsLayer` escala alrededor del centro, que es el mismo origen que usa el compositor para
la textura del fondo, y la caja contenedora ya recorta (`clipToBounds`). Se reutiliza en
`TimelinePreview` (con `positionMs`, que se refresca por fotograma durante la reproducción y
también en el reloj de los vídeos sin clips) y en `ExportPreviewDialog` (con el instante del
resumen).

### D7: Modelo, interfaz y compatibilidad

- `KenBurnsMode` es un enum serializable; `BackgroundLayer.kenBurns` tiene valor por defecto
  `NINGUNO`, así los JSON anteriores cargan sin efecto (igual que el resto de campos agregados).
- Un fondo nuevo entra con `AUTOMATICO` desde `EditorViewModel.addBackground`, porque es lo que el
  usuario espera al elegir esta función; desactivarlo es un toque.
- Chips en la fila del fondo (`Sin zoom`, `Zoom in`, `Zoom out`, `Ken Burns auto`) con clave de
  deshacer `background-ken-burns:<id>`, igual que el resto de ajustes del fondo.
- El resumen de la vista previa de exportación muestra `Fondo: PNG · Ken Burns auto` cuando el
  fondo activo tiene efecto.

## Risks / Trade-offs

- **Nitidez**: el fondo se amplía hasta un 14 % sobre un PNG ya recortado al tamaño del fotograma.
  A 1080p es imperceptible; si en el futuro molesta, se puede pre-renderizar el fondo con un 14 %
  más de resolución y compensar la escala.
- **Coste por fotograma**: cada fotograma evalúa una búsqueda lineal en la lista de fondos (unos
  pocos elementos) y una función de suavizado. Es despreciable frente a la decodificación y
  codificación del vídeo.
- **Dos motores, una cuenta**: si Compose y Media3 interpretaran distinto el origen de la escala,
  el encuadre se vería desplazado. Se mitiga usando las mismas convenciones ya validadas por las
  capas (escala alrededor del centro, NDC con Y invertida) y probando en dispositivo.
- **Interacción con la ventana de vídeo**: el vídeo va encima del fondo y no se mueve con él; el
  zoom solo afecta a la imagen de fondo, que es lo que define el efecto.
- **Tramos muy cortos** (1 s): el zoom recorre el 14 % en un segundo y puede percibirse nervioso;
  se acepta porque es proporcional, y el usuario puede elegir `Sin zoom` para ese fondo.

## Migration Plan

1. Publicar en una versión menor: los proyectos existentes abren con `kenBurns = NINGUNO` y se ven
   exactamente igual que antes.
2. Si hay que revertir, el código anterior ignora el campo extra al deserializar
   (`ignoreUnknownKeys`) y al volver a guardar se pierde el ajuste, sin romper los proyectos.

## Open Questions

Ninguna pendiente: la única decisión de alcance discutible (aplicarlo también a las capas PNG) se
resuelve en `proposal.md` como non-goal, porque esas capas ya tienen animación propia.
