# Design — Fondos PNG y ventana de vídeo

## Context

Ver `proposal.md` para la motivación y `specs/` para el comportamiento exigido.

Estado actual relevante:

- La vista previa (`TimelinePreview` en `EditorScreen.kt`) muestra el `PlayerView` a
  pantalla completa dentro de un `Box` con la proporción del primer clip, y dibuja las
  capas PNG/texto encima con `graphicsLayer`.
- La exportación (`VideoExporter.kt`) usa `Composition` con tres tipos de secuencia:
  clips de vídeo, música (solo audio) y un `OverlayEffect` a nivel de composición para
  las capas.
- `FrameGeometry.frameSize()` define el tamaño del fotograma de salida a partir del
  primer clip; la vista previa se ajusta a esa misma proporción.
- Media3 1.5.1 (ya en `libs.versions.toml`) incluye `Composition.Builder
  .setVideoCompositorSettings`, `VideoCompositorSettings` y `Crop` en `media3-effect`,
  todas marcadas `@UnstableApi`.

## Goals / Non-Goals

**Goals:**

- Componer fondo → vídeo (ventana) → capas, con una única geometría normalizada usada
  por la vista previa y por el exportador.
- Mantener intacto el camino de exportación actual cuando el proyecto no usa fondos ni
  cambia la ventana, para no arriesgar regresiones.
- Que los tramos de fondo sean datos puros y testeables en la JVM.

**Non-Goals:**

- Animar la ventana de vídeo con fotogramas clave (solo transform global).
- Soportar vídeo como fondo (solo PNG), varios vídeos en paralelo o giro del fondo.
- Cambiar el tamaño/proporción del fotograma exportado ni el máximo de 30 s.
- Edición de fondo durante la grabación de movimiento de capas.

## Decisions

### D1: Composición en la exportación con `VideoCompositorSettings`

La exportación usará el compositor multi-entrada de Media3: una secuencia de clips de
vídeo y una secuencia de imágenes de fondo, con `Composition.setVideoCompositorSettings`.

Hechos verificados en el código de Media3 1.5.1 que condicionan la implementación:

- **Orden de dibujo**: `DefaultVideoCompositor` dibuja de atrás hacia adelante recorriendo
  los índices al revés, y la **entrada primaria (índice 0) queda encima**. Por lo tanto la
  secuencia de clips de vídeo debe ir **primero** (encima) y la de fondos **después**
  (debajo). La música sigue siendo una secuencia de solo audio y no entra al compositor.
- **Escala por píxeles nativos**: `OverlayMatrixProvider` escala cada textura por
  `tamaño_entrada / tamaño_salida`; es decir, `setScale(1, 1)` dibuja la textura a su
  tamaño nativo en píxeles, centrada, no estirada al fotograma. Las escalas se calculan
  sobre esa base.
- **Sin huecos**: el compositor espera una imagen de cada entrada en cada instante. Una
  secuencia de fondos con `addGap()` dejaría tramos sin textura y bloquearía el
  ensamblado, así que los huecos se rellenan con un PNG negro temporal.

Alternativas descartadas:

- *Shader GL propio que pinte el fondo detrás del vídeo*: habría que reducir el vídeo
  dentro del mismo shader y reconstruir toda la matemática de transformaciones; más
  frágil que usar el compositor oficial.
- *Renderizar fotograma a fotograma en CPU*: inviable para 30 s a 1080p en móvil.

### D2: Fondos pre-renderizados al tamaño exacto del fotograma (*cover*)

Antes de exportar, cada PNG de fondo se decodifica (respetando la orientación EXIF),
se recorta por el centro y se escala a exactamente `frameWidth x frameHeight`, y se
guarda como PNG temporal en `cacheDir`. Así el compositor lo dibuja con escala 1, sin
cálculos por fotograma, y el recorte *cover* queda garantizado e idéntico a la vista
previa (`ContentScale.Crop`).

Se decodifica con muestreo (`inSampleSize`) y se procesa un fondo a la vez para acotar
memoria; los temporales se borran al terminar o cancelar la exportación.

### D3: Recorte del vídeo con `Crop` para la forma 1:1 o 9:16

La forma de la ventana se consigue recortando el fotograma de cada clip con
`androidx.media3.effect.Crop` en coordenadas NDC, antes de entrar al compositor:

- 1:1: se recorta izquierda/derecha si el clip es más ancho que alto, o arriba/abajo si
  es más alto que ancho, hasta dejar un cuadrado central.
- 9:16: el mismo cálculo con proporción objetivo 9/16.
- `ORIGINAL`: no se agrega `Crop`.

El recorte se añade a los efectos de vídeo del clip (junto a filtro de color, lente ancha,
temblor y velocidad) y es no-op cuando la proporción ya coincide. Así el vídeo nunca se
deforma. En la vista previa el equivalente es un `PlayerView` con
`RESIZE_MODE_ZOOM` dentro de un `Box` con la proporción objetivo, que recorta igual.

### D4: Escala de la ventana relativa al “mayor tamaño que cabe”

`VideoWindow.scale` (0.2..1) se interpreta sobre el mayor tamaño que cabe en el lienzo
para la forma elegida. Un helper compartido calcula el tamaño en píxeles del lienzo y la
escala a pasarle al compositor:

```
tamañoDeseado = fit(frameW, frameH, aspectoVentana) * scale
escalaCompositor = tamañoDeseado / tamañoNativoRecortado
```

El tamaño nativo recortado se toma del primer clip (misma fuente que
`FrameGeometry.frameSize`). Si los clips tienen resoluciones distintas, la diferencia es
de pocos píxeles y no cambia la proporción; se documenta como limitación.

`ORIGINAL` + `scale = 1` + centrada + sin giro equivale exactamente al comportamiento
actual (100 % del lienzo).

### D5: Camino de exportación condicional

`VideoExporter` decide:

- **Sin fondos y ventana por defecto** → camino actual (una sola secuencia de vídeo +
  overlays). Cero riesgo de regresión.
- **Con fondos o ventana modificada** → compositor: secuencia de vídeo (primaria, arriba)
  + secuencia de fondos (debajo, con negro en los huecos) + música, y
  `VideoCompositorSettings` que devuelve el tamaño del fotograma y las transformaciones de
  cada entrada.

Las capas PNG/texto siguen aplicándose como `OverlayEffect` a nivel de composición, por lo
que quedan por encima de todo.

### D6: Modelo y geometría puros y testeables

Nuevo archivo `model/VideoWindowMath.kt` con funciones puras (sin Android):

- `backgroundAt(backgrounds, timeMs)` → fondo activo o `null` (tramos ordenados, `[inicio,
  fin)`).
- `normalizeBackgrounds(...)` → recorta solapamientos al agregar o mover un tramo.
- `windowSize(frameW, frameH, aspect, scale)` y `windowHitTest(...)` con rotación
  (transformar el punto al espacio sin girar de la ventana).

Esto permite tests unitarios JVM de la lógica que hoy sería imposible probar dentro de
Compose o Media3.

### D7: Gestos y prioridad

`TimelinePreview` ya tiene una superficie de gestos única que hace hit-test sobre las
capas. Se extiende así:

1. Si el toque cae en una capa visible → gesto de capa (comportamiento actual).
2. Si no, y cae dentro del rectángulo (rotado) de la ventana → gesto de ventana: arrastrar
   mueve, pellizcar escala, girar con dos dedos.
3. Si no cae en nada → se deselecciona.

La ventana seleccionada muestra un borde guía. Los cambios se emiten al `ViewModel` con la
misma estrategia de muestras que las capas (una por evento y una final).

### D8: Persistencia, deshacer y compatibilidad

- `Project.backgrounds: List<BackgroundLayer> = emptyList()` y
  `Project.videoWindow: VideoWindow = VideoWindow()` con valores por defecto serializables.
- Los PNG elegidos se copian al almacenamiento propio con `MediaProbe.copyToStorage`,
  igual que las capas, para que el proyecto guardado siga abriendo.
- Claves de deshacer: `background-add:<id>`, `background-edit:<id>`,
  `background-remove:<id>` y `video-window`.
- No hay migración: proyectos antiguos deserializan con los valores por defecto y
  `ProjectStore` ya usa `ignoreUnknownKeys`, así que una versión anterior de la app
  también puede abrir proyectos nuevos ignorando los campos extra.

### D9: Tramos de fondo en la interfaz

- Botón **Fondo** junto a PNG/Texto/Sticker: elige imagen y abre un diálogo con dos
  controles de inicio y fin (por defecto `0..duración total`, con duración mínima de 1 s).
- Sección **Fondos** bajo **Capas**: miniatura, tramo, editar y eliminar.
- Chips **Original / 1:1 / 9:16** y botón **Centrar** para la ventana, visibles junto a los
  controles de capas.

## Risks / Trade-offs

- **API inestable del compositor** (`@UnstableApi`) puede cambiar o fallar en algunos
  dispositivos → se aísla en `VideoExporter` detrás del camino condicional, se compila en
  CI y cualquier fallo se reporta al usuario con el mensaje del `ExportException`.
- **Mezcla de `ColorInfo` no soportada** (vídeo HDR + PNG SDR) → el compositor lanza
  excepción; se muestra “No se pudo exportar con fondos” en vez de un MP4 corrupto.
- **Desfase de fotogramas entre secuencias** si la imagen estática y el vídeo tienen
  cadencias distintas → se fija `setFrameRate(30)` en la secuencia de fondos y se rellenan
  los huecos con negro para que siempre haya textura.
- **Memoria de bitmaps** con PNG grandes → muestreo al decodificar, un fondo a la vez,
  temporales en caché y borrado al terminar.
- **Resoluciones mixtas entre clips** → la escala de la ventana se calcula con el primer
  clip; clips de otra resolución pueden variar algunos píxeles, nunca la proporción.
- **Rotación + arrastre** pueden dejar la ventana parcialmente fuera del lienzo → el
  centro se limita a `[0,1]`, pero se permite salir parcialmente para composiciones
  creativas; el fondo negro cubre el resto.

## Migration Plan

1. Publicar el cambio en una versión menor; los proyectos existentes abren sin fondos y
   con la ventana por defecto (comportamiento idéntico).
2. Si hay que revertir, basta con volver al código anterior: los JSON nuevos siguen
   abriendo porque los campos extra se ignoran, aunque se pierden fondos y ventana al
   volver a guardar.

## Open Questions

Ninguna pendiente: las decisiones de alcance (ventana global, recorte *cover*, fondos por
tramos) ya están resueltas en `proposal.md` y `specs/`.
