## Why

Hoy el único camino para meter material en VidCam es abrir la app y usar sus
selectores internos ("Importar vídeo", "Imagen", "Fondo", "Música"). Quien ya
tiene el vídeo, la imagen o la pista en otra app —Galería, WhatsApp, Archivos,
Drive, un navegador— tiene que salir, abrir VidCam y volver a buscarlo. Ese
rodeo hace que VidCam no aparezca en la hoja de compartir del sistema, aunque
es exactamente el tipo de app que se usa para eso.

## What Changes

- **Hojas de compartir del sistema**: `MainActivity` declara filtros para
  `ACTION_SEND` (un archivo) y `ACTION_SEND_MULTIPLE` (varios) con `video/*`,
  `image/*` y `audio/*`, y para `ACTION_VIEW` al abrir un vídeo con VidCam.
- **Clasificación por tipo**: cada archivo recibido se clasifica por MIME y, si
  el remitente no lo declara, por la extensión del nombre:
  - `video/*` → clip en la línea de tiempo (mismo camino que "Importar vídeo",
    con el límite de 30 s y el recorte automático ya existentes).
  - `image/*` → capa PNG sobre el lienzo (mismo camino que "Imagen").
  - `audio/*` → pista de música del proyecto (el proyecto tiene una sola pista:
    se usa el primer audio y los demás se informan como omitidos).
  - Cualquier otro tipo se informa y se ignora.
- **Copia al almacenamiento propio**: los archivos compartidos se copian a
  `filesDir/media`, igual que los importados desde el selector, para que el
  permiso temporal del remitente no deje roto un proyecto guardado.
- **Recepción en caliente**: `MainActivity` pasa a `singleTop` y procesa
  `onNewIntent`, así compartir con VidCam ya abierto agrega el material al
  proyecto actual en vez de abrir una segunda copia de la app.
- **Sin perder lo compartido**: si la app todavía no tiene los permisos de
  medios, los archivos quedan pendientes y se incorporan cuando el usuario
  concede el acceso; la pantalla de permisos lo indica.
- **Aviso de resultado**: un solo mensaje resume qué se agregó y cuántos
  archivos se omitieron, y las incorporaciones entran al historial de deshacer.

## Capabilities

### New Capabilities
- `shared-media-intake`: recepción de archivos compartidos o abiertos desde
  otras apps (vídeo, imagen, audio) con clasificación por tipo, copia al
  almacenamiento propio, incorporación al proyecto, aviso de resultado y
  comportamiento cuando faltan permisos.

### Modified Capabilities
- Sin cambios en las specs existentes: la importación de clips, las capas PNG
  y la música conservan sus requisitos; esta capacidad solo agrega otra puerta
  de entrada hacia esos mismos caminos.

## Impact

- **Manifiesto**: `AndroidManifest.xml` (intent-filters de `SEND`,
  `SEND_MULTIPLE` y `VIEW`, `launchMode="singleTop"`).
- **Arranque**: `MainActivity` (lectura del intent, `onNewIntent`, estado de
  archivos pendientes), `VidCamApp` (entrega al editor y nota en la pantalla de
  permisos), `PermissionScreen` (aviso de archivos pendientes).
- **Editor**: `EditorViewModel` (`importShared`, refactor de `importVideo` y
  `addPngLayer` a funciones suspendidas reutilizables, música desde un archivo
  compartido).
- **Nuevo paquete** `com.vidcam.app.share`: `SharedKinds` (clasificación pura,
  testeable en la JVM) y `SharedMedia` (lectura de `Intent`, MIME y nombre).
- **Tests**: `SharedMediaTest` cubre la clasificación por MIME y por extensión.
- **Sin dependencias nuevas**: `Intent`, `ContentResolver` y `OpenableColumns`
  son API del sistema.
- **Fuera de alcance**: recibir archivos de proyecto JSON (`SavedProject`) y
  arrastrar y soltar (drag & drop) dentro de la app.
