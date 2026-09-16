## Why

No existe todavía una app Android propia en este repositorio. Se necesita una herramienta **minimalista y local-first** para producir vídeos verticales de hasta 30 segundos (formato historia/TikTok), usando la cámara del teléfono, su biblioteca de medios (música) y sus vídeos ya grabados, con un build reproducible y auditable vía GitHub Actions. El objetivo es publicar contenido de redes sociales sin depender de apps de terceros, sin cuentas y sin subir archivos a la nube.

## What Changes

- Se crea la aplicación Android **VidCam** (Kotlin + Jetpack Compose, un solo módulo `:app`) con una pantalla de edición minimalista.
- Permisos de **cámara, micrófono y acceso a medios** solicitados en un único flujo al primer arranque, con fallback a ajustes si se deniegan.
- **Grabación de vídeo** vertical de hasta 30 s con CameraX (cámara frontal/trasera) e **importación** de vídeos existentes del teléfono.
- **Biblioteca de música** que lee de la carpeta de medios de Android (`MediaStore.Audio`) y permite usar una pista como audio del proyecto.
- **Edición esencial no destructiva**: recorte (trim) de vídeo y audio, silenciado del audio original y mezcla con la música elegida.
- **Capas visuales**: PNG con transparencia (stickers) y **textos con Google Fonts**, con posición, tamaño y duración.
- **Exportación a MP4** (H.264 + AAC, 9:16) con guardado en la galería y hoja de compartir del sistema para publicar en redes sociales.
- **CI/CD en GitHub Actions**: workflow de verificación (lint + tests + assembleDebug) y workflow de release firmado (APK/AAB) con keystore desde secrets.
- Sin cambios de ruptura: es un proyecto nuevo.

## Capabilities

### New Capabilities
- `app-shell-permissions`: arranque, estado de primer uso y solicitud única de permisos de cámara, micrófono y medios.
- `capture-and-import`: grabación de vídeo ≤30 s, importación de vídeos del teléfono y selección de música desde la biblioteca de medios.
- `timeline-editing`: línea de tiempo no destructiva con recorte de vídeo/audio, mute del audio original, capas PNG transparentes y textos con Google Fonts.
- `export-and-share`: render a MP4 (H.264/AAC 9:16), guardado en la galería y compartir a apps sociales.
- `build-and-release`: compilación, verificación y release firmado mediante GitHub Actions.

### Modified Capabilities
<!-- Ninguna: el repositorio no tiene specs previas. -->

## Impact

- **Nuevo código**: proyecto Gradle Android completo (`app/`), módulos de captura, edición y exportación en Kotlin/Compose.
- **Dependencias nuevas**: AndroidX CameraX, Media3 (transformer, extractor, muxer, common), Compose Material 3, `androidx.compose.ui:ui-text-google-fonts` / Play Services Fonts.
- **Permisos de Android**: `CAMERA`, `RECORD_AUDIO`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, `READ_MEDIA_IMAGES` (y `READ_EXTERNAL_STORAGE` en API < 33), `INTERNET` (descarga de fuentes; el resto del pipeline es offline).
- **Infraestructura**: `.github/workflows/ci.yml` y `.github/workflows/release.yml`; secrets de repositorio para el keystore de firma (`KEYSTORE_BASE64`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`).
- **Almacenamiento**: uso de `MediaStore` + `FileProvider` para leer/crear archivos; procesamiento temporal en caché de la app.
- **Compatibilidad**: `minSdk 26`, `targetSdk` vigente; sin dependencias nativas (sin FFmpeg).
