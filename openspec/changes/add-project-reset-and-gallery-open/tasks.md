## 1. Modelo de estado del editor

- [x] 1.1 Añadir `newProject()` a `EditorViewModel`: vaciar el proyecto, apagar la grabación de movimiento, descartar la última exportación y no hacer nada si hay una exportación en curso

## 2. Interfaz de nuevo proyecto

- [x] 2.1 Añadir la acción "Nuevo" en la barra superior de `EditorScreen`, deshabilitada durante la exportación
- [x] 2.2 Mostrar un `AlertDialog` de confirmación ("Nuevo proyecto", "Descartar", "Cancelar") solo cuando haya clips, capas o música; si el proyecto está vacío, limpiar sin preguntar

## 3. Apertura del vídeo exportado en la galería

- [x] 3.1 Añadir `GallerySaver.openInGallery(context, uri)` con `ACTION_VIEW`, tipo `video/mp4`, `FLAG_GRANT_READ_URI_PERMISSION` y `FLAG_ACTIVITY_NEW_TASK`, devolviendo `false` si no hay visor
- [x] 3.2 Invocar la apertura en `EditorViewModel.export()` cuando el guardado en la galería devuelve un `Uri`, ajustando el mensaje si no hay visor

## 4. Verificación y documentación

- [x] 4.1 Añadir un test instrumentado de Compose que verifique que "Nuevo" descarta las capas y deja el editor vacío
- [x] 4.2 Ejecutar la verificación en CI (`lint test assembleDebug assembleDebugAndroidTest`) y corregir lo que falle
- [x] 4.3 Actualizar `README.md` (sección de comportamiento) con el botón "Nuevo" y la apertura en la galería
