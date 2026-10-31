## 1. Motor nativo (RNNoise)

- [ ] 1.1 Vendorizar RNNoise 0.1.1 en `app/src/main/cpp/rnnoise` con licencia y
      notas de origen
- [ ] 1.2 Puente JNI (`rnnoise_jni.c`) con `frameSize`, `create`, `process` y
      `destroy`
- [ ] 1.3 `CMakeLists.txt` y `externalNativeBuild` en Gradle con NDK fijado y
      ABIs `arm64-v8a`, `armeabi-v7a`, `x86_64`
- [ ] 1.4 Reglas de R8 para conservar `com.vidcam.app.audio.Rnnoise`
- [ ] 1.5 Instalación de NDK y CMake en `ci.yml` y `release.yml`

## 2. Audio y modelo

- [ ] 2.1 `audio/WavFile` (lectura, escritura, tramo con relleno y recorte,
      duración por cabecera)
- [ ] 2.2 `audio/PcmDsp` (nivel, RMS, pico, ganancia con recorte, paso alto)
- [ ] 2.3 `audio/VoiceoverRecorder` (AudioRecord 48 kHz mono, `UNPROCESSED` con
      reserva, medidor de nivel, WAV final)
- [ ] 2.4 `audio/VoiceoverProcessor` (paso alto + RNNoise mezclado + nivelación)
- [ ] 2.5 `audio/GainAudioProcessor` para volumen por pista en Media3
- [ ] 2.6 `VoiceoverTrack`, `Project.voiceover`, `originalVolume` y `musicVolume`
- [ ] 2.7 `VoiceoverMath` (plan de relleno/recorte y posición en el archivo)

## 3. Editor

- [ ] 3.1 `EditorViewModel`: grabar, detener, aplicar toma, procesar, ajustar
      entrada y volumen, quitar
- [ ] 3.2 `VoiceoverSheet` con grabación, medidor, aislación, entrada y mezcla
- [ ] 3.3 Botón "Voz" en los controles, con marca cuando hay toma
- [ ] 3.4 `TimelinePreview`: reproductor de voz sincronizado, monitoreo al 25 %
      durante la toma y reinicio desde el principio al grabar

## 4. Exportación

- [ ] 4.1 Secuencia de voz con relleno/recorte acotada al vídeo
- [ ] 4.2 Ganancia por pista en clips, música y voz
- [ ] 4.3 Limpieza de la carpeta temporal de voz al terminar o cancelar

## 5. Pruebas y documentación

- [ ] 5.1 `WavFileTest` (ida y vuelta, duración, relleno, recorte, bytes)
- [ ] 5.2 `PcmDspTest` (nivel, pico, ganancia, paso alto)
- [ ] 5.3 `VoiceoverMathTest` (entrada, adelanto, recorte, ajuste acotado,
      posición en archivo, toma activa)
- [ ] 5.4 Documentar en OpenSpec (`add-voiceover`), README y `alex.md`
- [ ] 5.5 CI en verde y APK de release publicado con la voz en off
