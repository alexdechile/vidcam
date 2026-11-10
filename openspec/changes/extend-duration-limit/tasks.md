# Tasks: Extensión del límite de duración a 5 minutos y exportación de fotograma PNG en alta calidad

## 1. Modelo y Matemáticas de Línea de Tiempo

- [x] 1.1 Actualizar la constante `MAX_DURATION_MS = 300_000L` y la lista `TARGET_DURATIONS_MS` con opciones de 60 s y 300 s en `Project.kt`.
- [x] 1.2 Ajustar cotas en `SchemeTiming.kt` para permitir esquemas con la duración del proyecto sin acotar forzosamente a 30 s.
- [x] 1.3 Actualizar pruebas unitarias en `TargetDurationTest.kt` y `TimelineMathTest.kt` para validar el tope de 5 minutos y el recorte defensivo de grabaciones.

## 2. Incorporación de Clips y Presupuesto en ViewModel

- [x] 2.1 Modificar `EditorViewModel.addClip` con parámetro `isImported: Boolean = false` para no recortar clips importados/compartidos y actualizar el aviso a "Límite de 5 min alcanzado".
- [x] 2.2 Configurar `importVideoInternal` para pasar `isImported = true` al invocar `addClip`.
- [x] 2.3 Actualizar `updateTrim` y `setClipSpeed` en `EditorViewModel` con cálculo de techo elástico que preserve clips con duración nativa superior a 5 minutos.
- [x] 2.4 Agregar pruebas unitarias en `TimelineMathTest.kt` verificando clips largos (más de 5 min) con velocidad y recorte.

## 3. Motor de Renderizado y Guardado de Fotograma PNG

- [x] 3.1 Crear `FrameExporter.kt` para componer y renderizar un fotograma en resolución completa (1080x1920, `ARGB_8888`) con vídeo, fondo, ventana, capas y subtítulos en formato PNG sin pérdida (`Bitmap.CompressFormat.PNG, 100`).
- [x] 3.2 Implementar `GallerySaver.saveImageToGallery` para registrar imágenes PNG en `MediaStore.Images.Media.EXTERNAL_CONTENT_URI` bajo `Pictures/VidCam`.
- [x] 3.3 Implementar `ShareUtils.shareImage` para compartir imágenes PNG vía Intent con `image/png`.
- [x] 3.4 Agregar método `exportFramePng(timeMs: Long)` en `EditorViewModel` con gestión de estados, guardado en galería y notificación.

## 4. UI y Pantalla de Edición

- [x] 4.1 Formatear los chips de duración fija en `EditorScreen.kt` para desplegar "1 min" y "5 min" legibles.
- [x] 4.2 Actualizar el snackbar de grabación en `EditorScreen.kt` al alcanzar el límite de 5 minutos.
- [x] 4.3 Agregar el botón "Exportar fotograma (PNG)" en `ExportPreviewDialog` permitiendo exportar el PNG o el vídeo MP4.
- [x] 4.4 Actualizar la descripción de bienvenida en `PermissionScreen.kt`.

## 5. Documentación y Verificación

- [x] 5.1 Actualizar `alex.md` y `README.md` reflejando el límite ampliado a 5 minutos y la exportación de fotogramas PNG en alta calidad.
- [x] 5.2 Validar la propuesta con `openspec validate extend-duration-limit --strict`.
