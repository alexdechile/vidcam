## Why

VidCam graba el audio dentro del clip de cámara y solo deja mezclar una pista de
música con un interruptor de silencio para el sonido original. Falta lo que
hace que un vídeo corto se vea profesional: **la voz**.

Hoy no se puede:

1. **Doblar un vídeo ya montado.** No hay forma de grabar la narración después
   del montaje, y grabar mientras se habla con la cámara obliga a acertar todo
   en una sola toma.
2. **Grabar en un lugar con ruido.** Un ventilador, la calle o el eco de una
   habitación arruinan la toma, y el micrófono del teléfono no filtra nada.
3. **Mezclar con criterio.** El audio original se silencia o se deja a volumen
   1,0, y la música también: no hay volumen por pista.

## What Changes

- **Voz en off (doblaje)**: botón "Voz" en el editor. Reproduce el vídeo y graba
  la narración encima, a 48 kHz mono, con medidor de nivel, parada automática al
  terminar el vídeo y regrabado libre. La toma queda guardada en el proyecto
  (`VoiceoverTrack`) y se puede reubicar con un ajuste de entrada.
- **Aislación de entorno con RNNoise**: la toma cruda se procesa **después** de
  grabar (paso alto 85 Hz + RNNoise 0.1.1 vía JNI + nivelación a −1 dBFS). El
  interruptor "Aislar entorno" permite comparar crudo y limpio sin volver a
  hablar. El procesamiento vive en `audio/` y no depende de la red.
- **Volumen por pista**: `Project.originalVolume`, `Project.musicVolume` y
  `VoiceoverTrack.volume`, con sliders en la hoja de voz. La exportación aplica
  cada volumen como `AudioProcessor` sobre su secuencia, porque Media3 1.5 no
  expone volumen por pista en `Composition`.
- **Vista previa sincronizada**: un segundo `ExoPlayer` reproduce la voz alineada
  con el vídeo, respetando entrada, recorte y volumen; durante la toma el
  monitoreo del vídeo baja al 25 % para que el altavoz no domine el micrófono.

## Capabilities

### New Capabilities
- `voiceover-dubbing`: grabar, reubicar, limpiar y exportar una toma de voz en
  off sobre el montaje.
- `audio-track-mix`: volumen independiente para voz, audio original y música en
  la vista previa y en el MP4.

### Modified Capabilities
- La exportación (`export/VideoExporter`) suma una secuencia de audio más a la
  composición; el comportamiento sin voz en off no cambia.

## Impact

- **Nativo**: `app/src/main/cpp` con RNNoise 0.1.1 vendorizado (BSD-3), puente
  JNI (`rnnoise_jni.c`) y CMake; ABIs `arm64-v8a`, `armeabi-v7a` y `x86_64`
  (~130 KB por ABI). R8 conserva `com.vidcam.app.audio.Rnnoise` por los nombres
  JNI.
- **Audio**: `audio/WavFile`, `audio/PcmDsp`, `audio/VoiceoverRecorder`,
  `audio/VoiceoverProcessor`, `audio/GainAudioProcessor`.
- **Modelo**: `VoiceoverTrack`, `Project.voiceover`, `Project.originalVolume`,
  `Project.musicVolume` y `VoiceoverMath` (colocación de la toma).
- **Editor**: `EditorViewModel` (grabación, procesamiento, ajustes),
  `VoiceoverSheet`, botón "Voz" y sincronía en `TimelinePreview`.
- **Exportación**: secuencia de voz con relleno/recorte, y ganancia por pista en
  clips y música.
- **CI**: se instala NDK 27.0.12077973 y CMake 3.22.1 en `ci.yml` y `release.yml`.
- **Fuera de alcance**: narración en vivo con la cámara encendida, cancelación de
  eco por hardware, varias tomas simultáneas y edición de la forma de onda.
