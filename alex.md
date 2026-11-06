# Alex — resumen personal

## Último Realizado

- **2026-11-06**: Efecto Ken Burns en imágenes fijas (v1.8.0, change OpenSpec
  `add-ken-burns-effect`):
  1. Cada fondo PNG elige su zoom con chips **Sin zoom / Zoom in / Zoom out /
     Ken Burns auto**; un fondo nuevo entra con el automático, que alterna
     acercar y alejar entre imágenes consecutivas.
  2. El cálculo vive en `model/KenBurnsMath.kt` (zoom del 14 % repartido sobre el
     tramo con suavizado), así que el mismo efecto se ve más rápido en un fondo
     de 4 s y más lento en uno de 20 s: la velocidad la fija la duración.
  3. El desplazamiento se acota a `(escala - 1) / 2`, el margen que deja el
     propio zoom, para que la imagen nunca deje ver los bordes.
  4. La exportación no necesita efectos nuevos: el compositor de Media3 recibe
     escala y ancla por fotograma en `getOverlaySettings`, y también se activa
     cuando el proyecto no tiene clips (el fondo es la pista de vídeo).
  5. Retrocompatible: `BackgroundLayer.kenBurns = NINGUNO` por defecto, con
     tests JVM del cálculo y del JSON.

- **2026-11-02**: Subtítulos con transcripción y mezcla con fundidos y ducking (v1.7.0):
  1. **Subtítulos quemados**: `Caption`/`CaptionStyle` con tiempos absolutos y
     estilo global (tamaño, altura, mayúsculas, fondo). Se dibujan con la misma
     geometría en la vista previa (`CaptionView`) y en el MP4 (`CaptionBitmaps`).
  2. **Transcripción**: hoja `Subs` → "Transcribir voz" manda la toma a un endpoint
     Whisper configurable (`/transcribe` del homelab o API de OpenAI) y crea un
     subtítulo por tramo; sin tiempos, el texto se reparte proporcionalmente.
  3. **Fundidos** de entrada/salida en música y voz con curva smoothstep, y
     **ducking** de la música bajo la voz (umbral −42 dBFS, ataque 60 ms,
     liberación 320 ms). Exportación con `EnvelopeAudioProcessor`; vista previa a
     20 Hz con la misma curva.
  4. **Alineación 16 KB** de `librnnoise_jni.so` (`-Wl,-z,max-page-size=16384`):
     era la única librería del APK a 4 KB, bloqueante en Android 15 y Play.
  5. 27 tests unitarios nuevos (`CaptionMathTest`, `AudioMixMathTest`,
     `TranscriptParserTest`) y change OpenSpec `add-captions-and-audio-mix`.

- **2026-10-31**: Voz en off con aislación de entorno y volumen por pista (v1.6.0):
  1. Botón **"Voz"**: doblaje con la vista previa reproduciéndose, medidor de nivel,
     parada automática al terminar el vídeo, regrabado libre y ajuste de entrada
     (−2 s recorta el comienzo, +5 s la retrasa).
  2. **Aislación de entorno**: RNNoise 0.1.1 vendorizado y compilado por JNI/CMake
     (~128 KB por ABI). La toma se graba cruda a 48 kHz mono con `AudioRecord`
     (fuente `UNPROCESSED`) y se procesa después: paso alto 85 Hz, RNNoise y
     nivelación a −1 dBFS; el interruptor compara limpio/crudo sin regrabar.
  3. **Volumen por pista** (`originalVolume`, `musicVolume`, `VoiceoverTrack.volume`)
     aplicado al exportar con `GainAudioProcessor`: Media3 1.5 no expone volumen en
     `EditedMediaItem` y `AudioMixer.setSourceVolume` no es API pública.
  4. Vista previa con un segundo `ExoPlayer` sincronizado al vídeo; durante la toma
     el monitoreo baja al 25 % para que el altavoz no domine el micrófono.
  5. CI instala NDK 27.0.12077973 + CMake 3.22.1; APK release de 3,6 MB firmado,
     22 tests unitarios nuevos y 2 instrumentados verificados en emulador, y el
     change OpenSpec `add-voiceover`.

- **2026-10-30**: GIF animados, duración de grabación fijada y vídeos solo con PNG (v1.5.0):
  1. `LayerKind.GIF` + `media/GifAnimation`: el GIF se detecta por cabecera (no por el nombre), se rasteriza en hasta 24 fotogramas de 384 px y se elige el fotograma según el tiempo del vídeo, en vista previa y en el MP4.
  2. `Project.targetDurationMs`: chips "Duración" (5/10/15/30 s) que cortan la grabación sola con cronómetro y definen cuánto dura un vídeo sin clips; nunca se pasa de los 30 s del proyecto.
  3. Exportación sin clips: la secuencia de imágenes preparadas (fondos o lienzo negro) hace de pista de vídeo durante la duración fijada, con capas, GIF y música; la vista previa usa un reloj propio y el resumen de exportación ya no espera un fotograma inexistente.
  4. Documentado en OpenSpec (`add-gif-and-still-videos`), README y tests (`GifAnimationTest`, `TargetDurationTest`).

- **2026-10-30**: Recepción de archivos compartidos desde otras apps (v1.4.0):
  1. `MainActivity` con `singleTop` y filtros `SEND`, `SEND_MULTIPLE` y `VIEW`; los archivos que llegan se clasifican por MIME y, si falta, por extensión (`SharedKinds`/`SharedMedia`).
  2. Vídeos entran como clips, imágenes como capas PNG y el primer audio como música del proyecto; todo se copia a `filesDir/media` y se puede deshacer.
  3. Los archivos pendientes esperan en la pantalla de permisos y se incorporan al concederlos; un solo aviso resume el resultado.
  4. Documentado en OpenSpec (`add-shared-file-intake`), README y tests unitarios de clasificación.

- **2026-10-29**: Duración en pantalla de las capas con slider (change OpenSpec
  `add-layer-duration`):
  1. Cada capa (PNG o texto) tiene en "Capas" un slider **"En pantalla"** en
     segundos y otro **"Aparece en"** para el segundo de entrada, acotados a la
     duración real del proyecto (no a los 30 s teóricos).
  2. Reglas de tiempo centralizadas en `model/LayerTiming.kt` (mínimo 0,3 s,
     máximo = lo que queda de vídeo) y usadas por la lista, el diálogo y la vista
     previa; con tests unitarios.
  3. Se revivió `LayerEditDialog`, que era código muerto (`editingLayer` nunca se
     asignaba): ahora hay botón "Editar capa" con los mismos sliders de tiempo
     más tamaño, posición y fuente.
  4. La vista previa de exportación elige un instante donde haya capas visibles e
     informa cuántas se verán, en vez de asumir siempre t=0.
  5. La exportación no necesitó cambios: `LayerBitmapOverlay` ya aplica la ventana
     temporal por fotograma.

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
- Fondos PNG a pantalla completa por tramos de tiempo, con efecto Ken Burns de
  zoom (zoom in / zoom out / automático alternado) cuya velocidad depende de la
  duración del vídeo.
- Ventana de vídeo configurable (aspecto 1:1 o 9:16), arrastrable, escalable y girable sobre el fondo.
- Persistencia de proyectos en formato JSON y soporte para Deshacer/Rehacer (Undo/Redo).
- Exportación acelerada a MP4 (H.264/AAC), guardado directo en galería y diálogo nativo para compartir.

## Stack Técnico

- **Lenguaje y plataforma**: Kotlin 2.0+, Android SDK (minSdk 26, targetSdk 35, compileSdk 35).
- **UI / Frontend**: Jetpack Compose + Material 3 (con Google Fonts descargables).
- **Cámara y captura**: AndroidX CameraX (Camera2Interop, VideoCapture, Recorder).
- **Audio de voz**: `AudioRecord` (PCM 48 kHz mono) + RNNoise 0.1.1 compilado con
  NDK/CMake y puente JNI (`app/src/main/cpp`).
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
  - Usa **un solo store**: `C:\Users\alexz\.wacli` (cuenta vinculada
    `56226832189`). `config.yaml` está vacío (`{}`), así que `--store` es
    implícito. No hace falta `--store` en los envíos.
  - `~/.wacli/accounts/personal` era un store **vacío y sin autenticar** creado
    por un `default_account` en el config; daba `not authenticated` en cada
    envío. Se borró el 2026-10-29 y `config.yaml` quedó como `{}`.
  - Diagnosticar con `wacli doctor` (debe decir `AUTHENTICATED true`). Si alguna
    vez sale `false`, hay que revincular: `wacli auth` (QR) o
    `wacli auth --phone <número>` (código de 8 dígitos).
- **telomando** (`…\wacli\telomando` y `.sh`): script Bash que envuelve wacli:
  `telomando <archivo> [atajo|número] [mensaje]`. Atajos `i` / `r`.
- Descargas de CI: `C:\Users\alexz\AppData\Local\Temp\opencode\vidcam-download\`.

## Preferencias

- Comunicación en español.
- Respuestas concisas; explicar qué hace cada comando antes de ejecutarlo.
- No publicar secretos; los secrets de firma (keystore) viven solo en GitHub
  Actions.