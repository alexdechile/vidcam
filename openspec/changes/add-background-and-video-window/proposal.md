## Why

Hoy el vídeo siempre ocupa el lienzo completo y las capas PNG solo se dibujan encima: no
existe forma de usar una imagen como **fondo** ni de reencuadrar el vídeo. Eso limita los
usos del editor a un formato único (vídeo a pantalla completa) y obliga a resolver los
fondos, marcos y plantillas fuera de la app.

Con un PNG de fondo a pantalla completa y una **ventana de vídeo** móvil, escalable,
girable y con forma 1:1 o 9:16, VidCam sirve también para plantillas, marcos decorativos,
comparativas, collages de un vídeo y vídeos con márgenes de color, sin perder la
exportación actual cuando no se usa la función.

## What Changes

- **Fondos PNG por tramos**:
  - Nueva lista `Project.backgrounds` con `BackgroundLayer(id, uri, startMs, endMs, label)`.
  - El PNG de fondo cubre todo el lienzo con recorte (*cover*), no se arrastra ni se
    escala: es el nivel inferior de la composición.
  - Se pueden agregar varios fondos con ventanas de tiempo propias; en los huecos sin
    fondo se ve negro.
  - Botón **Fondo** en los controles, con selector de imagen de la galería, diálogo de
    inicio/fin y lista de fondos con editar/eliminar.
- **Ventana de vídeo global**:
  - Nuevo `Project.videoWindow: VideoWindow(aspect, x, y, scale, rotationDeg)` con
    `aspect` en `ORIGINAL`, `CUADRADO` (1:1) o `VERTICAL` (9:16).
  - Posición, tamaño y giro se editan con gestos sobre el lienzo (arrastrar, pellizcar,
    girar) y con botones de forma 1:1 / 9:16 / original.
  - El vídeo se recorta (*cover*) para respetar la forma elegida, sin deformarse.
- **Vista previa y exportación coherentes**:
  - La vista previa dibuja fondo → ventana de vídeo → capas PNG/texto, con la misma
    geometría normalizada que la exportación.
  - La exportación usa el compositor de Media3 (`setVideoCompositorSettings`) para
    componer el fondo por debajo del vídeo cuando hay fondos; sin fondos y con la ventana
    por defecto se mantiene el camino de exportación actual.
- **Persistencia y deshacer**: los fondos y la ventana se guardan en el JSON del proyecto
  (retrocompatible) y participan de deshacer/rehacer.

## Capabilities

### New Capabilities
- `background-layers`: fondos PNG a pantalla completa con ventanas de tiempo, agregar,
  editar y eliminar en el editor, visibles en vista previa y MP4.
- `video-window`: ventana de vídeo global con forma 1:1 / 9:16 / original, posición,
  escala y rotación editables por gestos, aplicada igual en vista previa y exportación.

### Modified Capabilities
<!-- No hay capacidades sincronizadas en openspec/specs/ cuyos requisitos cambien. -->

## Impact

- **Modelo**: `Project.kt` agrega `BackgroundLayer`, `VideoWindow`, `VideoWindowAspect`,
  `Project.backgrounds` y `Project.videoWindow` con valores por defecto.
- **Editor**: `EditorViewModel` agrega `addBackground`, `updateBackground`,
  `removeBackground`, `updateVideoWindow`; `EditorScreen.kt` incorpora el fondo en
  `TimelinePreview`, la ventana de vídeo con gestos, el botón **Fondo** y los diálogos.
- **Exportación**: `VideoExporter.kt` compone secuencias de imágenes de fondo + clips de
  vídeo con `Composition.setVideoCompositorSettings`; `Overlays.kt`/nuevo helper calcula
  el recorte *cover* de fondos y ventana.
- **Persistencia**: `ProjectStore` guarda los campos nuevos; proyectos anteriores cargan
  con `backgrounds = []` y `videoWindow` por defecto, sin migración.
- **Dependencias**: se mantiene Media3 1.5.1 (`media3-effect` ya incluye
  `VideoCompositorSettings` y `Crop`). Sin librerías nuevas.
- **Riesgo**: el compositor multi-secuencia es API inestable de Media3; se valida en CI y,
  si un dispositivo no lo soporta, la exportación falla con mensaje explícito.
