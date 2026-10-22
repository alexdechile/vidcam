# Alex — resumen personal

## Último Realizado

- **2026-10-22**: Vista previa de exportación con imagen fija (Opción A):
  1. Diálogo modal `ExportPreviewDialog` al presionar "Exportar", que extrae un fotograma del clip con `MediaMetadataRetriever` y lo renderiza con el fondo PNG, ventana configurada (aspecto, escala, traslación, rotación), filtro de color Compose (`composeColorFilter`), lente ancho y capas superpuestas.
  2. Resumen visual previo con duración, filtro, tipo de ventana y fondo antes de iniciar el render final.
  3. Botones "Exportar ahora" y "Volver a editar" para ajustes rápidos sin esperar la exportación completa.
  4. Generación de release en GitHub Actions y envío del APK a teléfono por `telomando`.

## ¿Qué hace esta App?

**VidCam** es una aplicación Android orientada a la creación y edición rápida de vídeos verticales (≤ 30 s) con exportación MP4 (H.264/AAC):
- Grabación de cámara con audio, cambio de cámara y estabilizador.
- Importación de clips de vídeo y ajuste de velocidad por clip.
- Biblioteca de música del dispositivo y efectos (p. ej. Shake/temblor).
- Capas interactivas: PNG transparente y textos con Google Fonts, manipulables mediante gestos (arrastrar, escalar, rotar) y grabación de movimiento en el lienzo (animación de posición en tiempo real).
- Fondos PNG a pantalla completa por tramos de tiempo.
- Ventana de vídeo configurable (aspecto 1:1 o 9:16), arrastrable, escalable y girable sobre el fondo.
- Persistencia de proyectos en formato JSON y soporte para Deshacer/Rehacer (Undo/Redo).
- Exportación acelerada a MP4 (H.264/AAC), guardado directo en galería y diálogo nativo para compartir.

## Stack Técnico

- **Lenguaje y plataforma**: Kotlin 2.0+, Android SDK (minSdk 26, targetSdk 35, compileSdk 35).
- **UI / Frontend**: Jetpack Compose + Material 3 (con Google Fonts descargables).
- **Cámara y captura**: AndroidX CameraX (Camera2Interop, VideoCapture, Recorder).
- **Procesamiento de vídeo y exportación**: AndroidX Media3 Transformer + VideoCompositor / Effects.
- **Persistencia**: Kotlinx Serialization JSON.
- **CI / CD**: GitHub Actions (`ci.yml` para validación y `release.yml` para tags `v*`). Minificación y ofuscación con R8/ProGuard.


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