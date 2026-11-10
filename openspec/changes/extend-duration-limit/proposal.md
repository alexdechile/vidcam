# Proposal: Extensión del límite de duración a 5 minutos y exportación de fotograma PNG en alta calidad

## Why

1. **Límite de tiempo restrictivo**: El límite histórico de 30 segundos fue adoptado como una cota conservadora para teléfonos de gama baja, pero restringe innecesariamente la grabación y corta los vídeos que los usuarios importan o comparten desde otras aplicaciones.
2. **Necesidad de miniaturas y capturas sin pérdida**: Los usuarios necesitan extraer fotogramas individuales de su composición (con fondos, capas de texto/PNG, filtros y ventana de vídeo) en formato PNG con la máxima resolución y fidelidad gráfica (lossless), para usarlos como miniaturas, portadas o publicaciones estáticas.

## What Changes

- **Límite base a 5 minutos (`300_000L` ms)**: se eleva el tope temporal estándar `MAX_DURATION_MS` de 30 s a 5 minutos (300 s) para grabaciones de cámara, proyectos de imágenes fijas y presupuesto del timeline.
- **Respeto a la duración completa de vídeos importados o compartidos**: al importar un clip de la galería o recibirlo por `ACTION_SEND`, no se recorta a 5 minutos si dura más; se admite la duración íntegra del archivo.
- **Selector de duración ampliado**: `TARGET_DURATIONS_MS` incorpora 1 min (`60_000L`) y 5 min (`300_000L`) con etiquetas legibles en la interfaz.
- **Recorte y velocidad elásticos**: `updateTrim` y `setClipSpeed` permiten ajustar todo el rango del archivo original.
- **Exportación de fotograma PNG de máxima calidad**:
  - Renderizado del fotograma compuesto (vídeo + fondos + ventana + filtros + capas PNG/texto + subtítulos) en resolución completa (1080x1920 en vertical o tamaño nativo del vídeo) en espacio de color `ARGB_8888`.
  - Guardado sin pérdida (`Bitmap.CompressFormat.PNG, 100`) en la galería (`Pictures/VidCam`).
  - Acción directa en `ExportPreviewDialog` ("Exportar fotograma PNG") junto a la exportación de vídeo MP4, con guardado en galería y opción de compartir.

## Capabilities

### New Capabilities
- `video-duration-limit`: Gestión de límites temporales ampliados a 5 minutos para grabaciones, adaptación automática a la duración total para vídeos importados/compartidos, y exportación de fotogramas compuestos en PNG de máxima calidad.

### Modified Capabilities
*Ninguna (no existen especificaciones previas en `openspec/specs/` para esta capacidad).*

## Impact

- **Modelos (`com.vidcam.app.model`)**:
  - `Project.kt`: `MAX_DURATION_MS = 300_000L`, `TARGET_DURATIONS_MS` con opciones de 1 min y 5 min.
  - `TimelineMath.kt`: cálculos de límite sobre 5 minutos.
- **Exportación y medios (`com.vidcam.app.export`, `com.vidcam.app.media`)**:
  - `FrameExporter.kt`: renderizador de fotograma compuesto en alta definición a partir de un instante del proyecto.
  - `GallerySaver.kt`: método `saveImageToGallery` para registrar PNGs en `MediaStore.Images`.
  - `ShareUtils.kt`: soporte para compartir imágenes PNG con `image/png`.
- **Editor (`com.vidcam.app.editor`)**:
  - `EditorViewModel.kt`: lógica de incorporación íntegra de clips importados, presupuesto elástico y método `exportCurrentFrame()`.
  - `EditorScreen.kt`: chips legibles de duración, avisos actualizados y botón de exportar PNG en `ExportPreviewDialog`.
