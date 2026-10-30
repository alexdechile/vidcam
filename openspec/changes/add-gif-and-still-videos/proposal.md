## Why

Tres huecos concretos que aparecieron grabando y compartiendo:

1. **Los GIF quedan quietos.** Un GIF compartido desde WhatsApp o la galería
   entra como capa de imagen y se dibuja con su primer fotograma: pierde
   justamente lo que lo hace GIF.
2. **No se puede fijar la duración de la grabación.** La cámara graba hasta el
   tope de 30 s, así que un clip de 5 s se consigue grabando 6 y recortando.
3. **Un vídeo solo con PNG no se puede exportar.** El exportador exige al menos
   un clip, aunque el lienzo ya esté compuesto con fondos, capas PNG, textos y
   música.

## What Changes

- **GIF animados**: nueva clase de capa (`LayerKind.GIF`). El archivo se
  detecta por su cabecera (`GIF87a`/`GIF89a`), se rasteriza una vez en una lista
  corta de fotogramas y se dibuja el fotograma que corresponde al tiempo del
  vídeo, tanto en la vista previa como en el MP4 exportado.
- **Duración fijada de antemano**: el proyecto guarda `targetDurationMs`
  (5, 10, 15 o 30 s) y la grabación se detiene sola en ese tiempo, con un
  cronómetro visible. Nunca se pasa de lo que queda libre del proyecto de 30 s.
- **Vídeos solo con PNG**: cuando no hay clips, el MP4 se arma con la secuencia
  de imágenes preparadas (fondos o lienzo negro) durante `outputDurationMs`, con
  las capas, los GIF y la música por encima. La vista previa y el diálogo de
  exportación muestran ese lienzo, y la duración del vídeo es la fijada.
- Las capas y fondos nuevos cubren la duración de salida (no el timeline vacío),
  y al cambiar la duración fijada se estiran o recortan los que llegaban al
  final.

## Capabilities

### New Capabilities
- `gif-layer-animation`: capas GIF animadas en vista previa y exportación.
- `still-image-video`: exportación de proyectos sin clips, compuestos solo con
  fondos, capas PNG/texto/GIF y música.
- `recording-target-duration`: duración de grabación fijada de antemano y su
  uso como duración del vídeo de imágenes.

### Modified Capabilities
- Sin cambios en `shared-media-intake`: un GIF compartido sigue clasificándose
  como imagen (`image/*`); lo nuevo es qué se hace con él después.

## Impact

- **Modelo**: `Project.targetDurationMs`, `Project.outputDurationMs`,
  `Project.hasStillContent`, `Project.canExport`, `LayerKind.GIF`; `TimelineMath`
  mide la música sobre la duración de salida y añade `recordingLimitMs`.
- **GIF**: `media/GifAnimation` (detección, rasterizado y emparejamiento
  tiempo→fotograma), `export/Overlays` (overlay que cambia de fotograma) y
  `EditorScreen.LayerBox` (fotograma según el instante).
- **Exportación**: `export/VideoExporter` permite proyectos sin clips y usa la
  secuencia de imágenes como pista de vídeo; el compositor de ventana no se
  aplica cuando no hay vídeo.
- **Editor**: `EditorViewModel.setTargetDuration`, detección de GIF al agregar
  una imagen, y en `EditorScreen` los chips de duración, el cronómetro, el reloj
  de vista previa sin clips y los botones de exportar.
- **Tests**: `GifAnimationTest` (emparejamiento de fotogramas) y
  `TargetDurationTest` (duración de salida, tope de grabación, ventana de música
  y exportabilidad).
- **Sin dependencias nuevas**: `android.graphics.Movie` y Media3 ya están en el
  proyecto.
- **Fuera de alcance**: GIF animados como *fondo* (se sigue usando el primer
  fotograma), WebP animado, y elegir la duración de un clip ya grabado (para eso
  está el recorte).
