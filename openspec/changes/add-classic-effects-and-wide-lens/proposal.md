## Why

VidCam graba y edita vídeo vertical de 30 s con música, capas animadas y
velocidad por clip, pero el material sale "crudo": sin personalidad estética.
Para tomas grupales (familia, amigos, equipo) el encuadre normal se queda
corto y no hay manera de ampliar el campo de visión. El usuario quiere:

1. **Efectos clásicos**: filtros de color reconocibles (sepia, blanco y negro,
   vintage, alto contraste) que transformen la toma y hagan que el resultado se
   vea "producido", igual en la vista previa que en el MP4 exportado.
2. **Lente ancho**: encuadre más abierto para tomas grupales, aprovechando el
   ultra gran angular físico del dispositivo cuando exista y cayendo a un
   encuadre digital equivalente cuando no.

Ambos comparten un único requisito transversal que ya resuelve el flujo de
`speed`: **el efecto debe verse idéntico en la vista previa y en la
exportación** (mismo motor Media3 en ambos caminos) y **persistir por clip**
en el JSON del proyecto.

## What Changes

- **Modelo**: `VideoClip` gana `colorFilter: ColorFilterPreset`
  (NINGUNO, SEPIA, BLANCO_Y_NEGRO, VINTAGE, ALTO_CONTRASTE) y
  `wideLens: WideLensMode` (NORMAL/GRUPO, enum en vez de `Boolean` para
  dejar el modelo extensible a futuro encuadres; GRUPO activa el encuadre
  ancho del clip). El
  proyecto antiguo, sin estos campos, carga con filtro ninguno y sin lente
  ancho (default de kotlinx.serialization), sin migración.

- **Selector al grabar**: junto a los valores de velocidad aparecen **filtros
  clásicos** (chips) y el botón de **lente ancho** (Grupo) con su propio
  selector; la elección se aplica al clip grabado y, si procede, fuerza el
  recorte de 30 s sobre la duración efectiva.

- **Selector por clip**: cada `ClipRow` permite cambiar el filtro y el lente
  ancho a posteriori, sin volver a grabar, con la misma línea de deshacer.

- **Vista previa**: muestra el clip con el filtro de color y el encuadre
  ancho aplicados (idéntico a la exportación), reproduciendo además la
  velocidad existente sin romperla.

- **Exportación**: `VideoExporter` aplica a cada `EditedMediaItem` el filtro
  de color (`RgbAdjustment`/`RgbMatrix`/`Contrast` de media3-effect) y el
  encuadre ancho (cámara física de mayor FOV, o `RgbMatrix`+zoom al 0.65x
  como respaldo digital), manteniendo la sincronización de capas/música sobre
  la duración efectiva.

- **Persistencia**: filtro y lente ancho se serializan en el JSON del
  proyecto y se restauran al abrirlo.

## Assumptions

- **Sin ojo de pez digital**: media3-effect 1.5.1 no trae distorsión de
  barril/fisheye y la vista previa usa ExoPlayer plano (no el motor del
  Transformer), así que un ojo de pez por shader GL a medida no es viable hoy.
  En su lugar el "lente ancho" es **encuadre ultra gran angular físico** (FOV
  real del dispositivo vía CameraX) y, si no existe, un **encuadre digital
  0.65x** (aleja la cámara manteniendo la resolución) idéntico en preview y
  export. Tomar una decisión distinta (ojo de pez) rompería preview/export y
  arriesgaría el CI.
- **Filtros solo en exportación y preview de timeline**: el filtro se aplica
  a nivel de composición en ambos caminos. No se aplica a la cámara en vivo
  (grabando), que sigue mostrando el sensor crudo; el chip se limita a marcar
  qué filtro tendrá el clip, como ya pasa con la velocidad.
- **Límite de 30 s**: el presupuesto se mide sobre la duración efectiva
  (recorte × velocidad) igual que hoy; el lente ancho/filtro no cambia
  duraciones.
- **Rendimiento**: filtros y encuadre se aplican por fotograma en el GPU de
  Media3; se priorizan matrices/capas ligeras y se evita la vista previa de
  alta frecuencia cuando no aplica.

## Capabilities

### New Capabilities

- `clip-classic-effects`: filtros de color clásicos por clip (sepia, B/N,
  vintage, alto contraste, ninguno) con selección al grabar y por clip,
  persistencia, vista previa fiel y exportación con el mismo motor.

### Modified Capabilities

- `clip-speed` (existente): se extiende el selector de grabación (que hoy solo
  muestra velocidad) para incluir filtros y lente ancho; la cabecera del clip
  pasa de "Velocidad" a "Efectos" sin cambiar el comportamiento de velocidad.
- `export-overlay` (existente): la exportación aplica también filtro de color
  y encuadre ancho por clip antes de la composición de capas/música, sin
  cambiar el orden ni la resolución de salida.

## Impact

- **Modelo**: `Project.kt` (`VideoClip.colorFilter`, `VideoClip.wideLens`).
- **Editor**: `EditorViewModel` (`setClipFilter`, `setClipWide`, `wideLens`
  en `addRecordedClip`/`addClip`), `EditorScreen` (chips de filtro + botón
  Grupo en grabación y por clip, `TimelinePreview` con filtro/encuadre).
- **Export**: `VideoExporter.buildSequences` aplica filtro y encuadre por
  clip; `Effects` de composición por clip.
- **Preview**: `TimelinePreview` aplica el filtro de color con el mismo
  mecanismo que la exportación para la capa de vídeo; el encuadre ancho vía
  `graphicsLayer` (escala 1/0.65) cuando es digital.
- **Tests**: `TimelineMathTest` (duración efectiva sin cambios con filtro),
  pruebas de mapeo de filtros→matrices.
- **Sin dependencias nuevas**: media3-effect 1.5.1 y CameraX ya están en el
  proyecto.
