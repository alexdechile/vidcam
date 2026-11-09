# Tasks

## 1. Modelos de datos y serialización

- [x] 1.1 Crear `VoiceDoublerConfig` y `VoiceDoublerPreset` en `app/src/main/java/com/vidcam/app/model/Project.kt` con soporte de serialización Kotlinx JSON y valores por defecto retrocompatibles.
- [x] 1.2 Extender `VoiceoverTrack` con el campo `doubler: VoiceDoublerConfig?` y verificar con tests unitarios en `ProjectJsonTest` que proyectos antiguos sin este campo carguen con `doubler = null`.

## 2. Motor de Audio DSP (Pitch Shifting, Delay y Murmullo)

- [x] 2.1 Implementar `VoicePitchShifter` en `app/src/main/java/com/vidcam/app/audio/` para modular el tono (-12 a +12 semitonos) en PCM 16-bit 48 kHz y verificar su funcionamiento con tests unitarios.
- [x] 2.2 Implementar `VoiceDoublerEngine` en `app/src/main/java/com/vidcam/app/audio/` para gestionar de 1 a 5 capas duplicadas, búferes de desfase temporal (10 a 80 ms), micro-desafinación y normalización suave anti-clipping.
- [x] 2.3 Conectar el procesamiento en `VoiceoverProcessor` para generar el archivo WAV con doblaje a partir de la toma aislada sin modificar la grabación cruda.

## 3. Integración en ViewModel y UI Compose

- [x] 3.1 Agregar métodos en `EditorViewModel.kt` (`setVoiceDoublerPreset`, `updateVoiceDoublerConfig`) con debounce para disparar la re-síntesis de audio de forma atómica y soportar `UndoHistory`.
- [x] 3.2 Incorporar en `VoiceoverSheet.kt` los chips de presets inmediatos ("Normal", "Grave", "Agudo", "Estudio", "Murmullo") y el acordeón con sliders ("Voces", "Tono", "Desfase", "Mezcla").
- [x] 3.3 Conectar la reactividad en `EditorScreen.kt` para que el reproductor de vista previa apunte al archivo resultante de doblaje en cuanto finalice el procesamiento.

## 4. Exportación, Validación y Documentación

- [x] 4.1 Verificar que `VideoExporter.kt` exporte la pista con doblaje en el MP4 final manteniendo intactos el cálculo de ducking y los fundidos de entrada/salida.
- [x] 4.2 Ejecutar la suite completa de tests unitarios de audio y proyecto (`./gradlew testDebugUnitTest`) y verificar que todos pasen.
- [x] 4.3 Actualizar la bitácora `alex.md` y `README.md` documentando el efecto de doblaje y murmullo.
