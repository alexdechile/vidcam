# Alex — resumen personal

> Referencia de contexto para que las sesiones de este proyecto recuerden al
> propietario, sus herramientas y su forma de trabajo.

## Identidad y contacto

- Nombre: **Alex Zamorano** (usuario de WhatsApp y chat más reciente de wacli).
- País: Chile (código móvil +56).
- Email de git local: `alex.zamorano@gmail.comm` (así está configurado a nivel
  de repo; no corregir sin pedirlo).
- GitHub: `alexdechile` (repos como VidCam).
- WhatsApp para envío de builds/artefactos: **+56 9 9221 5761**
  (`56992215761@s.whatsapp.net`). Ojo: `569992215761` (con 9 extra) **no** es
  un móvil chileno válido y no existe en los chats de wacli.
- Otros contactos frecuentes en wacli: `i` = +56993206000 (Iván Zamorano),
  `r` = +56994480523 (Ramón Zamorano).

## Proyecto actual: VidCam

- Aplicación Android (Kotlin + Jetpack Compose + Material 3) para grabar y
  editar vídeos verticales ≤ 30 s: cámara con audio, importación, música del
  dispositivo, capas PNG/textos interactivas y exportación MP4 (H.264/AAC).
- Especificado en OpenSpec en `openspec/changes/add-vidcam-mvp/`.
- Versión actual: 1.0.0 release firmada y entregada (3,01 MB, R8 + shrink).

## Forma de trabajo

- Sigue el flujo de OpenSpec (proposal → design → specs → tasks).
- Los commits en `main` disparan `ci.yml` (lint + unit tests + debug + release +
  androidTest). Los tags `v*` o el dispatch manual disparan `release.yml`.
- No hay `JAVA_HOME` configurado localmente: los builds se hacen en **GitHub
  Actions**. A nivel local solo se edita código; la verificación de compilación
  ocurre en CI.
- Con la versión de Compose del proyecto **no** existe
  `Modifier.offset(Density.(IntSize) -> IntOffset)`; usar `graphicsLayer {}`
  con `size` para centrar y `translationX/Y`. Tampoco hay referencia
  `kotlin.math.toDegrees` (usar `java.lang.Math.toDegrees`).
- Import correcto para el primer gesto: `androidx.compose.foundation.gestures.awaitFirstDown` (no el de `ui.input.pointer`).

## Herramientas

- **wacli.exe**: `C:\Users\alexz\AppData\Local\Programs\wacli\wacli.exe` — CLI
  de WhatsApp; con `send file --file <ruta> --to <JID>` envía APKs.
- **telomando** (`…\wacli\telomando` y `.sh`): script Bash que envuelve wacli:
  `telomando <archivo> [atajo|número] [mensaje]`. Atajos `i` / `r`.
- Descargas de CI: `C:\Users\alexz\AppData\Local\Temp\opencode\vidcam-download\`.

## Preferencias

- Comunicación en español.
- Respuestas concisas; explicar qué hace cada comando antes de ejecutarlo.
- No publicar secretos; los secrets de firma (keystore) viven solo en GitHub
  Actions.