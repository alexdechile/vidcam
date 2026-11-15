# Design

## Context

VidCam es una app Android nativa en Kotlin + Jetpack Compose (`minSdk = 26`, `targetSdk = 35`, ABIs `arm64-v8a`, `armeabi-v7a`, `x86_64`). En el dispositivo de referencia (Moto G55 5G, 7.4 GB RAM) conviven el editor, reproductores ExoPlayer, RNNoise nativo y exportación MP4/PNG, por lo que la memoria RAM es un recurso sensible. Ver `proposal.md` para la motivación del cambio.

## Goals / Non-Goals

**Goals:**
- Integrar `com.google.mediapipe:tasks-genai` (`LlmInference`) para ejecutar modelos Gemma `.task` / `.litertlm` 100% offline.
- Mantener la APK liviana cargando el archivo del modelo desde almacenamiento externo/interno elegido por el usuario.
- Encapsular la inferencia en un motor asíncrono (`OnDeviceAiEngine`) con soporte de streaming o respuesta completa y cancelación/liberación explícita de recursos (`close()`).
- Conectar el asistente en el editor de VidCam para crear/actualizar capas de texto y guiones de voice-over.

**Non-Goals:**
- Empaquetar pesos `.task` dentro de `assets/` de la APK.
- Depender de la app externa Google AI Edge Gallery o de Termux/Ollama en tiempo de ejecución.
- Inferencia automática en segundo plano sobre cada fotograma de vídeo durante la reproducción.

## Decisions

1. **Motor `com.google.mediapipe:tasks-genai` vs HTTP local a Ollama**
   - *Decisión*: Usar `com.google.mediapipe:tasks-genai` directamente en Kotlin.
   - *Motivo*: Funciona sin Termux abierto, aprovecha aceleración LiteRT/MediaPipe en Android y convierte a VidCam en autocontenida.
   - *Alternativa descartada*: Llamar a `127.0.0.1:11434` (Ollama en Termux), ya que exige servicio externo manual y no sirve para otros usuarios de la APK.

2. **Modelo externo seleccionado vía Storage Access Framework (`OpenDocument`)**
   - *Decisión*: El usuario elige un `.task` o `.litertlm` descargado en el teléfono; VidCam lo importa/copia a `filesDir/models/` (o conserva la ruta privada) para acceso directo por `File` path requerido por `LlmInference.LlmInferenceOptions`.
   - *Motivo*: `tasks-genai` requiere ruta de archivo local en el sistema de archivos, no `content://` URI directa, y evita inflar la APK con >1 GB.

3. **Ciclo de vida bajo demanda y liberación antes de exportar**
   - *Decisión*: Instanciar `LlmInference` solo cuando el usuario abre o ejecuta una consulta en el asistente y cerrarlo al salir del sheet o al disparar exportación MP4/PNG.
   - *Motivo*: Un modelo 1B–3B ocupa ~1–2 GB de RAM; liberarlo evita OOM durante `MediaCodec` / exportación.

4. **Compatibilidad de ABI (`arm64-v8a`)**
   - *Decisión*: Proteger la inicialización para dispositivos `arm64-v8a` y capturar errores de inicialización en otros emuladores/ABIs (`x86_64` / `armeabi-v7a`) mostrando mensaje amigable sin crashear la app.

## Risks / Trade-offs

- **Consumo de RAM (~1–2 GB con modelo cargado)** → Cargar bajo demanda y liberar `LlmInference.close()` antes de exportar o al entrar en `onStop`.
- **Latencia de primera carga del modelo (varios segundos)** → Mostrar indicador de progreso claro en el sheet y ejecutar inicialización e inferencia en `Dispatchers.IO`.
- **Compatibilidad de versión `.task` vs `.litertlm`** → Validar extensión/cabecera al importar y mostrar error accionable si `LlmInference.createFromOptions` falla.
