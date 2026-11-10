# Design: Extensión del límite de duración a 5 minutos y exportación de fotograma PNG en alta calidad

## Context

Actualmente VidCam utiliza una constante global `MAX_DURATION_MS = 30_000L` (30 segundos), la cual acota grabaciones, vídeos de imágenes fijas, clips importados y compartidos, y los controles de recorte y velocidad.

Asimismo, la aplicación cuenta con un diálogo de vista previa previa a la exportación (`ExportPreviewDialog`), que simula visualmente la composición del vídeo antes de mandarlo a Media3 Transformer. Sin embargo, no existía un mecanismo para extraer y exportar una imagen fija de dicha composición en formato PNG sin pérdida (lossless) con la máxima resolución del proyecto.

## Goals / Non-Goals

**Goals:**
- Elevar `MAX_DURATION_MS` a `300_000L` (5 minutos = 300 s) como cota base para grabaciones de cámara y proyectos de imágenes.
- Permitir que los clips importados desde la galería o recibidos mediante `ACTION_SEND` ingresen a la línea de tiempo con su duración íntegra (`sourceDurationMs`), expandiendo el proyecto sin truncamiento.
- Ajustar `updateTrim` y `setClipSpeed` para calcular el presupuesto disponible respetando la duración completa del clip cuando supere el límite base.
- Ampliar las opciones de duración fija en el editor (`TARGET_DURATIONS_MS`) incorporando `60_000L` (1 min) y `300_000L` (5 min), con etiquetas legibles ("1 min", "5 min").
- Proporcionar un motor de exportación de fotograma compuesto (`FrameExporter`) que renderice la composición completa (vídeo + fondo + ventana + capas + subtítulos) a resolución completa (`1080x1920` en `ARGB_8888`) y lo guarde en PNG sin pérdida en la galería (`Pictures/VidCam`).
- Integrar la acción "Exportar fotograma (PNG)" en `ExportPreviewDialog` y habilitar su opción de compartir vía `ShareUtils`.

**Non-Goals:**
- Reemplazar el renderizador de vídeo MP4 en streaming de Media3 Transformer.
- Permitir grabaciones continuas de cámara indefinidas (se preserva la parada automática a los 5 minutos para seguridad de hardware).

## Decisions

### 1. `MAX_DURATION_MS = 300_000L` como base del sistema
- **Razón**: 5 minutos es la duración estándar para contenidos de vídeo vertical y medio, garantizando estabilidad en memoria y almacenamiento.

### 2. Distinción de clips importados en `addClip`
- **Razón**: Un parámetro `isImported: Boolean = false` permite distinguir entre grabaciones de cámara (sujetas a recorte contra el presupuesto de 5 min) y archivos importados/compartidos, los cuales ingresan completos sin llamar a `TimelineMath.trimToMax`.

### 3. Presupuesto elástico en `updateTrim` y `setClipSpeed`
- **Razón**: `timelineCeiling = maxOf(MAX_DURATION_MS, others + (clip.sourceDurationMs / speed).toLong())` garantiza que clips largos mantengan su rango navegable completo.

### 4. Motor de fotogramas PNG de alta calidad (`FrameExporter`)
- **Razón**: Para lograr la más alta calidad posible:
  1. La extracción del fotograma de vídeo se realiza con `MediaMetadataRetriever.OPTION_CLOSEST` en espacio `ARGB_8888`.
  2. El lienzo de composición se genera a resolución nativa de exportación (`1080x1920` o la del vídeo nativo).
  3. Los fondos PNG y las capas (PNG transparente con filtro bilineal `FILTER_BITMAP_FLAG`, textos con tipografía de sistema/Google Fonts, subtítulos y stickers) se componen sobre un `android.graphics.Canvas` idéntico al render final.
  4. La compresión se realiza con `Bitmap.CompressFormat.PNG, 100`, que utiliza codificación DEFLATE sin pérdida matemática de píxeles ni artefactos de compresión JPEG.

### 5. Guardado en MediaStore y Compartir
- **Razón**: Se agrega `GallerySaver.saveImageToGallery(context, file)` apuntando a `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` en `Pictures/VidCam`, y `ShareUtils.shareImage(context, file)` con MIME `image/png`.

### 6. Integración en `ExportPreviewDialog`
- **Razón**: El diálogo ya cuenta con el fotograma extraído y el instante de previsualización más representativo (`previewTimeMs`). Agregar un botón "Exportar fotograma (PNG)" permite guardar la portada en alta resolución de inmediato o proceder a la exportación de vídeo.

## Risks / Trade-offs

- **Tiempo de extracción con `OPTION_CLOSEST`** → *Mitigación*: `OPTION_CLOSEST` busca el fotograma exacto. En un único fotograma toma típicamente menos de 100 ms en IO thread y se ejecuta en background mediante corrutinas de ViewModel.
- **Tamaño del PNG resultante** → *Mitigación*: Un PNG 1080x1920 sin pérdida ocupa entre 1 y 3 MB, perfectamente manejable en almacenamiento y galería.
