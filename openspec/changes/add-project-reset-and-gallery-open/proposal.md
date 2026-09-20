## Why

Hoy el editor solo puede crear un proyecto por sesión: una vez que hay clips o
capas, la única forma de empezar otro es cerrar y volver a abrir la aplicación,
y la exportación deja el MP4 en la galería sin ofrecer ninguna forma de verlo de
inmediato. Hacen falta dos salidas naturales del flujo: **Nuevo proyecto** para
limpiar el editor sin guardar nada (todavía no existe persistencia) y **abrir el
vídeo exportado en la galería** para comprobarlo apenas termina la exportación.

## What Changes

- **Botón "Nuevo"**: en la barra superior del editor, descarta el proyecto en
  memoria y deja el editor vacío. Como no hay persistencia, no guarda nada antes
  de limpiar; pide confirmación cuando hay trabajo (clips, capas o música) y
  actúa sin preguntar si el proyecto ya está vacío. Detiene la grabación de
  movimiento y descarta la referencia de la última exportación.
- **Abrir en la galería**: al terminar la exportación y guardar el MP4, la
  aplicación lo abre en el visor de vídeo del sistema con `ACTION_VIEW`. Si
  ninguna aplicación puede manejarlo, conserva el guardado y avisa por el
  *snackbar*.

## Capabilities

### New Capabilities
- `project-reset`: descartar el proyecto actual y empezar uno vacío desde el
  editor, sin persistencia y con confirmación cuando hay trabajo que perder.

### Modified Capabilities
- `export-and-share`: tras guardar el MP4 en la galería, la aplicación lo abre
  en el visor del sistema; si no hay visor disponible, el guardado se mantiene.

## Impact

- **Editor**: `EditorScreen.kt` añade la acción "Nuevo" y su diálogo de
  confirmación; `EditorViewModel` gana `newProject()`.
- **Exportación**: `GallerySaver` gana `openInGallery(context, uri)` y
  `EditorViewModel.export()` lo invoca después de guardar.
- **Sin dependencias nuevas, sin permisos nuevos y sin cambios de modelo**: el
  proyecto sigue siendo efímero (en memoria).
- **Tests**: test instrumentado de Compose para el descarte del proyecto; los
  tests existentes deben seguir pasando.
