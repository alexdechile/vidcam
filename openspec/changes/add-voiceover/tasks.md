## 1. Motor nativo (RNNoise)

- [x] 1.1 Vendorizar RNNoise 0.1.1 en `app/src/main/cpp/rnnoise` con licencia y
      notas de origen
- [x] 1.2 Puente JNI (`rnnoise_jni.c`) con `frameSize`, `create`, `process` y
      `destroy`
- [x] 1.3 `CMakeLists.txt` y `externalNativeBuild` en Gradle con NDK fijado y
      ABIs `arm64-v8a`, `armeabi-v7a`, `x86_64`
- [x] 1.4 Reglas de R8 para conservar `com.vidcam.app.audio.Rnnoise`
- [x] 1.5 Instalación de NDK y CMake en `ci.yml` y `release.yml`

## 2. Audio y modelo

- [x] 2.1 `audio/WavFile` (lectura, escritura, tramo con relleno y recorte,
      duración por cabecera)
- [x] 2.2 `audio/PcmDsp` (nivel, RMS, pico, ganancia con recorte, paso alto)
- [x] 2.3 `audio/VoiceoverRecorder` (AudioRecord 48 kHz mono, `UNPROCESSED` con
      reserva, medidor de nivel, WAV final)
- [x] 2.4 `audio/VoiceoverProcessor` (paso alto + RNNoise mezclado + nivelación)
- [x] 2.5 `audio/GainAudioProcessor` para volumen por pista en Media3
- [x] 2.6 `VoiceoverTrack`, `Project.voiceover`, `originalVolume` y `musicVolume`
- [x] 2.7 `VoiceoverMath` (plan de relleno/recorte y posición en el archivo)

## 3. Editor

- [x] 3.1 `EditorViewModel`: grabar, detener, aplicar toma, procesar, ajustar
      entrada y volumen, quitar
- [x] 3.2 `VoiceoverSheet` con grabación, medidor, aislación, entrada y mezcla
- [x] 3.3 Botón "Voz" en los controles, con marca cuando hay toma
- [x] 3.4 `TimelinePreview`: reproductor de voz sincronizado, monitoreo al 25 %
      durante la toma y reinicio desde el principio al grabar

## 4. Exportación

- [x] 4.1 Secuencia de voz con relleno/recorte acotada al vídeo
- [x] 4.2 Ganancia por pista en clips, música y voz
- [x] 4.3 Limpieza de la carpeta temporal de voz al terminar o cancelar

## 5. Pruebas y documentación

- [x] 5.1 `WavFileTest` (ida y vuelta, duración, relleno, recorte, bytes)
- [x] 5.2 `PcmDspTest` (nivel, pico, ganancia, paso alto)
- [x] 5.3 `VoiceoverMathTest` (entrada, adelanto, recorte, ajuste acotado,
      posición en archivo, toma activa)
- [x] 5.4 Documentar en OpenSpec (`add-voiceover`), README y `alex.md`
- [x] 5.5 CI en verde y APK de release publicado con la voz en off
