# Proposal

## Why

VidCam actualmente edita clips, capas, subtítulos y voice-over de forma 100% local, pero carece de asistencia creativa offline para redactar titulares, guiones o descripciones directamente en el teléfono. Incorporar el motor on-device de Google AI Edge (MediaPipe GenAI / LiteRT) permite ejecutar modelos Gemma locales sin internet, sin cuotas de API y manteniendo la APK liviana al cargar modelos externos.

## What Changes

- Agregar soporte para inferencia LLM on-device mediante `com.google.mediapipe:tasks-genai` (el mismo motor subyacente de Google AI Edge Gallery) sobre la arquitectura `arm64-v8a` ya soportada por VidCam (`minSdk = 26`).
- Incorporar un gestor de modelo local que permita seleccionar o importar un archivo `.task` / `.litertlm` desde el almacenamiento del dispositivo (por ejemplo, el descargado previamente en el teléfono) sin empaquetar pesos dentro de la APK.
- Añadir un asistente de texto offline en el editor para generar o mejorar titulares/capas de texto, guiones de voice-over y sugerencias de subtítulos a partir de un prompt breve del usuario.
- Permitir desactivar o descargar el modelo de memoria en cualquier momento para no competir por RAM durante la exportación MP4 o grabación de voice-over.

## Capabilities

### New Capabilities
- `on-device-ai`: Gestión del ciclo de vida del modelo local (`.task`/`.litertlm`), configuración de ruta en almacenamiento del dispositivo, inferencia offline con MediaPipe GenAI / LiteRT y acciones de asistencia creativa en el editor.

### Modified Capabilities
<!-- Ninguna capacidad existente cambia sus requisitos a nivel de especificación. -->

## Impact

- **Dependencias (`gradle/libs.versions.toml`, `app/build.gradle.kts`)**: incorporación de `com.google.mediapipe:tasks-genai` (solo librería ligera; los pesos del modelo no se incluyen en el bundle).
- **Código Android/Kotlin**: nueva capa de servicio de IA local (`OnDeviceAiEngine` / `ModelStore`) y hoja/diálogo Compose en el editor para seleccionar modelo y generar texto hacia titulares o voice-over.
- **Almacenamiento y memoria**: uso opcional bajo demanda; el modelo se libera al cerrar el asistente o antes de exportar vídeo.
- **CI / Compilación**: validación y build de APK en GitHub Actions (`arm64-v8a`).
