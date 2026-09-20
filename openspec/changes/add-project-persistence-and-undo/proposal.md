## Why

El editor es hoy una sesión de un solo uso: no hay forma de deshacer una edición
y, al cerrar la aplicación, el proyecto desaparece. El botón "Nuevo" ya avisa
que no guarda nada, y el propio modelo (`Project`) es un `data class` inmutable
que solo contiene tipos primitivos y cadenas, así que ambas carencias se pueden
resolver sin tocar la exportación ni el pipeline de Media3.

Además, el usuario no tiene dónde conseguir música libre de forma segura: la
Biblioteca de audio de YouTube no se puede empaquetar en una app (su licencia
estándar está limitada a vídeos de YouTube y prohíbe la redistribución), así que
la vía correcta es guiar al usuario a fuentes legales sin incluir ninguna pista.

## What Changes

- **Deshacer / rehacer**: historial de instantáneas del proyecto en el
  `EditorViewModel`, con botones en la fila de controles. Las ediciones continuas
  (recortes, gestos sobre capas, controles deslizantes) se agrupan por clave e
  intervalo para que una entrada de historial corresponda a un gesto completo.
- **Proyectos guardados en JSON**: `Project` pasa a ser serializable con
  kotlinx.serialization; cada proyecto se guarda como un JSON en
  `filesDir/projects/` con nombre y fechas, y el menú de la barra superior
  permite guardar, guardar como y abrir. La lista permite borrar proyectos.
- **Medios persistentes**: los clips importados, las grabaciones y los PNG
  elegidos de la galería se copian a `filesDir/media` en lugar de a la caché,
  porque un proyecto guardado los referencia y una limpieza de caché lo dejaría
  roto.
- **Estado de cambios sin guardar**: el editor sabe si el proyecto difiere de lo
  guardado y solo pide confirmación cuando hay algo que perder.
- **Fuentes de música gratis**: el selector de música añade enlaces a YouTube
  Audio Library, Pixabay, Free Music Archive, ccMixter y Musopen, más un botón
  "Actualizar" que vuelve a leer la biblioteca del dispositivo. La aplicación no
  empaqueta ninguna pista.

## Capabilities

### New Capabilities
- `editor-history`: deshacer y rehacer ediciones del proyecto con agrupación de
  gestos continuos.
- `project-persistence`: guardar, listar, abrir y borrar proyectos como JSON
  local, con medios propios y persistencia de la ruta.
- `music-sources`: guía hacia fuentes de música descargables legalmente, sin
  redistribuir audio dentro de la app.

### Modified Capabilities
- `timeline-editing`: los clips importados y los PNG de la galería se almacenan
  en el almacenamiento propio de la app para sobrevivir al cierre y a la limpieza
  de caché.

## Impact

- **Modelo**: `Project.kt` añade `@Serializable` y la propiedad `isEmpty`; sin
  cambios de campos.
- **Nuevos archivos**: `data/ProjectStore.kt` (persistencia pura sobre `File`),
  `editor/UndoHistory.kt` (historial puro) y sus tests unitarios.
- **Editor**: `EditorViewModel` gana historial, estado de guardado y apertura;
  `EditorScreen` añade menú de proyecto, diálogos y botones de deshacer/rehacer;
  `MusicSheet` añade las fuentes.
- **Build**: plugin `kotlinx-serialization` y dependencia
  `kotlinx-serialization-json`.
- **Medios**: `MediaProbe.copyToCache` pasa a `copyToStorage` sobre
  `filesDir/media`; las grabaciones pasan a `filesDir/captures`.
- **Sin permisos nuevos** y sin cambios en la exportación ni en el formato del
  MP4.
