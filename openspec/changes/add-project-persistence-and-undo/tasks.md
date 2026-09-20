## 1. Historial de deshacer/rehacer

- [x] 1.1 Implementar `UndoHistory` (`editor/UndoHistory.kt`) con pilas de `Project`, límite configurable, `record`/`undo`/`redo`/`clear` y agrupación por clave e intervalo de 700 ms, sin dependencias de Android
- [x] 1.2 Escribir tests unitarios en `app/src/test/.../editor/UndoHistoryTest.kt`: deshacer/rehacer, rehacer descartado por una edición nueva, agrupación de un gesto continuo, claves distintas, ventana superada, sin clave y límite del historial
- [x] 1.3 Registrar el estado previo en `EditorViewModel.update` solo cuando el proyecto cambia, con clave por tipo de edición (recorte, música, capa, gesto) y exponer `canUndo`/`canRedo`
- [x] 1.4 Vaciar el historial al crear o abrir un proyecto
- [x] 1.5 Añadir los botones de deshacer/rehacer en la fila de controles, con iconos vectoriales propios y deshabilitados cuando no hay historial

## 2. Persistencia de proyectos en JSON

- [x] 2.1 Añadir el plugin `kotlin-serialization` y la dependencia `kotlinx-serialization-json`, y anotar el modelo (`Project`, `VideoClip`, `OverlayLayer`, `LayerKeyframe`, `MusicTrack`, `LayerKind`)
- [x] 2.2 Añadir la propiedad `Project.isEmpty` y usarla para habilitar o deshabilitar el guardado
- [x] 2.3 Implementar `SavedProject` y `ProjectStore(rootDir: File)` (`data/ProjectStore.kt`): guardar con escritura atómica, listar ordenado por fecha, cargar, borrar y tolerar JSON corrupto
- [x] 2.4 Escribir tests unitarios en `app/src/test/.../data/ProjectStoreTest.kt`: ida y vuelta completa sin perder campos, sobrescritura sin duplicar, orden de la lista, borrado, JSON corrupto y creación de la carpeta
- [x] 2.5 Copiar clips importados y PNG de la galería a `filesDir/media`, y las grabaciones a `filesDir/captures`, para que un proyecto guardado no dependa de la caché ni del permiso del selector
- [x] 2.6 Conectar el `ProjectStore` al `EditorViewModel`: guardar, guardar como, abrir, borrar, refrescar la lista, nombre por defecto con fecha y conservación de `createdAtMs`

## 3. Interfaz de proyectos

- [x] 3.1 Añadir el menú de proyecto (⋮) en la barra superior con "Guardar", "Guardar como…" y "Abrir proyecto…"
- [x] 3.2 Mostrar el nombre del proyecto abierto en el título de la barra superior
- [x] 3.3 Añadir el diálogo de nombre y el diálogo de apertura con lista de proyectos, fecha y borrado
- [x] 3.4 Pedir confirmación solo cuando hay cambios sin guardar (nuevo proyecto y apertura), calculando `dirty` por comparación con la última versión guardada

## 4. Fuentes de música gratis

- [x] 4.1 Añadir al selector de música la sección "Música gratis para descargar" con enlaces a YouTube Audio Library, Pixabay, Free Music Archive, ccMixter y Musopen, abiertos en el navegador
- [x] 4.2 Añadir el botón "Actualizar" que vuelve a leer `MediaStore.Audio` y explicar en el estado vacío que hay que descargar y actualizar
- [x] 4.3 No empaquetar audio en la app y documentar por qué (licencia estándar de la Biblioteca de audio de YouTube limitada a YouTube y sin redistribución)

## 5. Verificación y documentación

- [x] 5.1 Ejecutar la verificación en CI (`lint test assembleDebug assembleDebugAndroidTest assembleRelease`) y corregir lo que falle
- [x] 5.2 Actualizar `README.md`: capacidades nuevas, sección de comportamiento (deshacer/rehacer, proyectos, música) y estructura con el paquete `data`
