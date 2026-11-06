## Why

Las imágenes fijas del proyecto (los fondos PNG) se ven completamente quietas durante todo su
tramo, así que un fondo que ocupa varios segundos parece una diapositiva pegada sobre el vídeo.
Eso se nota todavía más desde que la app exporta vídeos hechos **solo con imágenes**: ahí el fondo
es lo único que se ve.

El efecto Ken Burns —un zoom lento con un desplazamiento mínimo— es el recurso estándar para dar
vida a una foto sin pedirle nada al usuario: no hay fotogramas clave, ni gestos, ni curvas que
configurar. Hoy la app solo puede animar capas con grabación de movimiento, que exige gestos en
tiempo real y no sirve para un fondo que debe llenar el lienzo.

## What Changes

- **Efecto por imagen**: `BackgroundLayer` agrega `kenBurns: KenBurnsMode` con cuatro valores:
  `NINGUNO`, `ACERCAR` (zoom in), `ALEJAR` (zoom out) y `AUTOMATICO`.
- **Automático de verdad**: `AUTOMATICO` alterna acercar y alejar según el orden de la imagen en
  la línea de tiempo, así dos fondos consecutivos no repiten el mismo movimiento. Es el valor con
  el que entra un fondo nuevo.
- **La velocidad la fija la duración**: el zoom total (14 % del encuadre) se reparte sobre el
  tramo visible de la imagen, con suavizado de entrada y salida. Un tramo de 4 s se mueve más
  rápido que uno de 20 s, sin configuración adicional: un vídeo más largo produce un movimiento
  más lento.
- **Nunca aparecen bordes**: el desplazamiento se acota a `(escala - 1) / 2`, el margen que deja
  el propio zoom, así que la imagen siempre cubre el lienzo.
- **Vista previa y exportación idénticas**: el mismo cálculo puro (`KenBurnsMath`) alimenta la
  vista previa en Compose y la exportación en Media3, evaluado por fotograma. También funciona en
  los vídeos sin clips, donde el fondo preparado hace de pista de vídeo.
- **Interfaz**: chips `Sin zoom / Zoom in / Zoom out / Ken Burns auto` en la fila de cada fondo,
  con deshacer/rehacer, y el resumen de la vista previa de exportación indica el efecto activo.

## Capabilities

### New Capabilities
- `ken-burns`: efecto de zoom lento (Ken Burns) sobre las imágenes fijas del proyecto, con
  dirección automática, velocidad derivada de la duración y el mismo resultado en vista previa y
  en el MP4 exportado.

### Modified Capabilities
<!-- No hay capacidades sincronizadas en openspec/specs/ cuyos requisitos cambien: la función
     agrega un campo opcional a los fondos y no altera requisitos de background-layers,
     still-image-video ni export-and-share. -->

## Impact

- **Modelo**: `Project.kt` agrega el enum serializable `KenBurnsMode` y el campo
  `BackgroundLayer.kenBurns` con valor por defecto `NINGUNO`.
- **Cálculo puro**: nuevo `model/KenBurnsMath.kt` (`transformAt`, `resolveMode`, `progressAt`,
  `indexOf`, `zoomPerSecond`), sin dependencias de Android ni de Media3.
- **Exportación**: `VideoExporter.kt` resuelve el Ken Burns del fondo activo en cada fotograma y
  lo entrega como `OverlaySettings` (escala y ancla) al compositor de Media3. El compositor pasa a
  configurarse siempre que hay fondos preparados —también sin clips—, y en ese caso la entrada 0
  es el fondo, no una ventana de vídeo. El fondo preparado guarda además la ventana del Ken Burns
  para que un tramo recortado por un solape no desfase el zoom.
- **Editor**: `EditorViewModel` agrega `setBackgroundKenBurns` (clave de deshacer
  `background-ken-burns:<id>`) y los fondos nuevos entran con `AUTOMATICO`; `EditorScreen.kt`
  dibuja el fondo con `KenBurnsBackground` en la vista previa y en el resumen de exportación, y
  muestra los chips del efecto.
- **Tests**: `KenBurnsMathTest` (JVM) cubre dirección, acotado del desplazamiento, relación con la
  duración, alternancia y tramos inválidos; `ProjectStoreTest` cubre la persistencia del campo y la
  compatibilidad con JSON anteriores.
- **Riesgo**: el efecto solo se aplica a fondos PNG; las capas PNG, los textos y los GIF conservan
  su animación propia. El zoom amplía un PNG ya recortado al tamaño del fotograma, por lo que a
  zoom máximo hay un 14 % de ampliación digital (pérdida de nitidez imperceptible a 1080p).
