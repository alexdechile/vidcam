## Context

VidCam es una sola `Activity` (`MainActivity`) que monta Compose con
`VidCamApp`, el cual exige permisos de medios antes de mostrar `EditorScreen`
con su `EditorViewModel`. Los caminos de entrada actuales ya existen y son
reutilizables: `EditorViewModel.importVideo(uri)`, `addPngLayer(uri)` y
`setMusic(track)`, todos copiando el contenido a `filesDir/media` mediante
`MediaProbe.copyToStorage`.

El sistema entrega los archivos compartidos como URIs temporales
(`content://`) con permiso de lectura mientras la actividad está viva. Ver
`proposal.md` para la motivación y `specs/shared-media-intake/spec.md` para los
requisitos.

## Goals / Non-Goals

**Goals:**
- Aparecer en la hoja de compartir de Android para vídeo, imagen y audio, y en
  el diálogo "Abrir con" para vídeos.
- Clasificar archivos aunque el remitente declare `*/*` o no declare MIME.
- Incorporar al proyecto actual sin duplicar la actividad y sin perder lo
  compartido cuando faltan permisos.
- Un aviso claro de qué entró y qué se omitió.

**Non-Goals:**
- Elegir el destino de cada archivo con un diálogo previo: las imágenes entran
  como capa PNG (el caso de uso mayoritario) y, si el usuario la quiere de
  fondo, la vuelve a elegir desde "Fondo", que ya existe.
- Recibir proyectos `.json` compartidos entre teléfonos.
- Drag & drop entre apps en pantalla dividida.
- Múltiples pistas de música: el modelo `Project` tiene una sola.

## Decisions

### Recibir en la misma `MainActivity` (`singleTop`) en vez de una actividad aparte
Una segunda `Activity` tendría que crear su propio `EditorViewModel` y
duplicaría la sesión de edición: compartir con VidCam abierto abriría un editor
vacío por encima del proyecto en curso. Con `launchMode="singleTop"` y
`onNewIntent`, el material se agrega al proyecto que el usuario ya tiene
abierto, y `update()` deja cada incorporación en el historial, así que
"Deshacer" la quita.

### Un `StateFlow` de archivos pendientes, consumido cuando hay editor
`MainActivity` acumula lo recibido en `sharedFiles` (no lo procesa en
`onCreate`, porque el editor aún no existe). `VidCamApp` lo consume con
`LaunchedEffect(sharedFiles)` dentro de la rama con permisos concedidos y avisa
`onSharedFilesConsumed` para vaciar la lista. Así el mismo mecanismo sirve para
el arranque en frío, para `onNewIntent` y para cuando el usuario concede los
permisos después de haber compartido.

### Clasificación pura separada de la lectura de `Intent`
`SharedKinds` (sin imports de Android) decide entre vídeo, imagen, audio y
desconocido, y propone la extensión con la que se guarda la copia;
`SharedMedia` lee `EXTRA_STREAM`, `ClipData` y `ACTION_VIEW`, y resuelve MIME y
nombre visible con el `ContentResolver`. La parte pura se prueba en la JVM con
`SharedMediaTest`, sin emulador.

### El MIME se resuelve contra el `ContentResolver`
`ACTION_SEND_MULTIPLE` declara un único `type` para todos los adjuntos
(`video/*` aunque vengan imágenes mezcladas), así que se consulta
`contentResolver.getType(uri)` y solo si no hay respuesta se usa el tipo del
intent y, en último término, la extensión del nombre visible.

### Copia inmediata al almacenamiento propio
El permiso de lectura del remitente caduca con la actividad; los proyectos
guardados referencian rutas. Se reutiliza `MediaProbe.copyToStorage` con
prefijos `clip-`, `png-` y `musica-`, igual que las rutas existentes, para que
un proyecto guardado siga abriendo después.

### Mensaje único de resultado
En lugar de un aviso por archivo (que se pisarían en el snackbar), el
`EditorViewModel` cuenta vídeos, imágenes y audios agregados y omisiones, y
publica una línea: `Desde otra app: 1 vídeo, 1 imagen`. Si nada entró, se
conserva el motivo concreto que dejó la importación (por ejemplo "Límite de
30 s alcanzado") y, si no hubo motivo, se muestra uno genérico.

## Risks / Trade-offs

- **Compartir con un proyecto en curso agrega en vez de reemplazar.** Es la
  opción que no destruye trabajo ajeno y se deshace con un toque; se documenta
  en el README.
- **Un audio reemplaza la pista de música vigente.** Es el único hueco del
  modelo; el mensaje de resultado lo hace visible y "Deshacer" lo revierte.
- **El límite de 30 s recorta un vídeo compartido largo.** Es el mismo
  comportamiento que "Importar vídeo", ya documentado.
- **Aplicaciones que comparten un `file://` sin permiso** producen un fallo de
  lectura controlado: se cuenta como error y se avisa, sin excepción visible.
