## 1. Recepción de archivos

- [x] 1.1 Crear `com.vidcam.app.share.SharedKinds` con `SharedKind`
      (VIDEO/IMAGE/AUDIO/UNKNOWN), clasificación por MIME y por extensión, y
      extensión de destino para la copia
- [x] 1.2 Crear `com.vidcam.app.share.SharedMedia` que lea `ACTION_SEND`,
      `ACTION_SEND_MULTIPLE` y `ACTION_VIEW` (`EXTRA_STREAM`, `ClipData` y
      `data`), resuelva MIME con el `ContentResolver` y el nombre visible con
      `OpenableColumns`
- [x] 1.3 Registrar en `AndroidManifest.xml` los filtros de `SEND`,
      `SEND_MULTIPLE` y `VIEW` para `video/*`, `image/*` y `audio/*`, y poner
      `MainActivity` en `singleTop`

## 2. Incorporación al editor

- [x] 2.1 `MainActivity`: estado `sharedFiles`, lectura del intent en `onCreate`
      y `onNewIntent`, entrega a `VidCamApp`
- [x] 2.2 `VidCamApp`: consumir los archivos pendientes con `LaunchedEffect`
      cuando el editor está disponible y avisar el consumo; nota en la pantalla
      de permisos cuando hay archivos esperando
- [x] 2.3 `EditorViewModel.importShared`: vídeos a clips, imágenes a capas PNG,
      primer audio a música, conteo de agregados y omitidos; refactor de
      `importVideo`/`addPngLayer` a funciones suspendidas reutilizables
- [x] 2.4 Mensaje único de resultado (`Desde otra app: …`) conservando el motivo
      cuando nada se pudo agregar

## 3. Documentación y versión

- [x] 3.1 README: sección de archivos compartidos en "Comportamiento"
- [x] 3.2 Versión 1.4.0 (versionCode 12) en `app/build.gradle.kts` y entrada en
      `alex.md`

## 4. Verificación

- [x] 4.1 `SharedMediaTest`: clasificación por MIME, por extensión, tipo
      desconocido y extensión de destino
- [x] 4.2 Empujar a `main` y dejar el CI verde (lint + test + assemble debug,
      release y androidTest)
- [x] 4.3 Descargar el APK del CI y enviarlo por WhatsApp al +56 9 9221 5761
