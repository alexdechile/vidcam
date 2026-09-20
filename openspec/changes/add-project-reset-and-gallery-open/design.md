## Context

- `EditorViewModel` mantiene el único `Project` de la sesión en un
  `MutableStateFlow`; no hay persistencia ni serialización.
- La exportación (`EditorViewModel.export`) escribe el MP4 con `VideoExporter`,
  lo copia a la galería con `GallerySaver.saveToGallery` (que ya devuelve el
  `Uri` de `MediaStore`) y publica un mensaje en español.
- `ShareUtils.shareVideo` ya usa `startActivity` con
  `FLAG_ACTIVITY_NEW_TASK` desde el contexto de aplicación.

## Goals / Non-Goals

**Goals:**
- Poder empezar un proyecto vacío sin reiniciar la aplicación y sin guardar nada.
- Ver el vídeo exportado inmediatamente en el visor de la galería.
- No romper el flujo actual de guardado ni de compartir.

**Non-Goals:**
- Persistencia o guardado de proyectos (siguen siendo efímeros).
- Elegir explícitamente una aplicación de galería (se usa el visor
  predeterminado con `ACTION_VIEW`).
- Borrar archivos temporales de la caché al empezar un proyecto nuevo.

## Decisions

### 1. `newProject()` en el `EditorViewModel`

```kotlin
fun newProject() {
    if (_exporting.value) return
    _motionRecording.value = false
    _lastExport.value = null
    _project.value = Project()
}
```

Reinicia el estado en memoria: proyecto vacío, grabación de movimiento apagada y
sin referencia a la exportación anterior (la barra superior deja de ofrecer
"Compartir" para un vídeo que ya no pertenece a este proyecto). Se ignora la
llamada mientras hay una exportación en curso, que además queda bloqueada por el
diálogo modal de progreso.

**Alternativa descartada**: recrear el `ViewModel` desde la UI. El proyecto vive
en el `ViewModel` de la actividad; forzar su recreación desde Compose es más
frágil que una función explícita y complica el test.

### 2. Confirmación solo cuando hay trabajo que perder

El botón "Nuevo" de la barra superior muestra un `AlertDialog` ("Nuevo
proyecto" / "Descartar" / "Cancelar") únicamente si el proyecto tiene clips,
capas o música. Si está vacío, limpia directamente para no añadir fricción.

### 3. Abrir el vídeo con `ACTION_VIEW` sobre el `Uri` de `MediaStore`

`GallerySaver.openInGallery(context, uri)` lanza:

```kotlin
Intent(Intent.ACTION_VIEW).apply {
    setDataAndType(uri, "video/mp4")
    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
```

El `Uri` es el de `MediaStore.Video` que la propia app acaba de insertar, así
que el visor de la galería lo abre como cualquier vídeo del dispositivo.
`FLAG_ACTIVITY_NEW_TASK` es obligatorio porque el contexto es el de la
aplicación; `FLAG_GRANT_READ_URI_PERMISSION` cubre a los visores que respetan el
permiso explícito. Si ningún visor responde (`ActivityNotFoundException`), se
devuelve `false` y el *snackbar* informa solo del guardado: el archivo ya está en
la galería y no se pierde nada.

**Alternativa descartada**: abrir la carpeta/álbum de la galería en lugar del
vídeo. No existe un intent estándar y fiable para "abrir la galería en el álbum
X"; abrir el propio vídeo es lo que el usuario percibe como "ver el trabajo
exportado".

## Risks / Trade-offs

- **[Abrir otra aplicación justo al terminar la exportación]** → Es la
  funcionalidad pedida; si molesta, se puede convertir en una acción explícita
  sin cambiar el resto. El guardado se completa antes de abrir.
- **[El visor no soporta `ACTION_VIEW` con URI de contenido]** → Se captura la
  excepción y el mensaje degrada a "Vídeo guardado en la galería".
- **[Pérdida accidental de trabajo con "Nuevo"]** → Confirmación obligatoria si
  hay clips, capas o música; no hay nada guardado que recuperar.

## Migration Plan

Sin cambios de modelo ni de datos persistidos: no hay migración. El proyecto
sigue siendo efímero por sesión.

## Open Questions

- Ninguna que afecte a las specs. Si más adelante se añade persistencia, el
  diálogo de confirmación pasará a ofrecer "Guardar y salir".
