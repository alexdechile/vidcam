# Tasks

- [x] Añadir enum `ShakeIntensity` (NINGUNO, SUAVE, MEDIO, FUERTE) al modelo y campo `shake` en `VideoClip` con default `NINGUNO`
- [x] Implementar soporte de estabilización de vídeo (EIS/OIS) en `CameraRecorder.kt` usando `Camera2CameraControl`
- [x] Crear `ShakeEffect.kt` con generador de `MatrixTransformation` de Media3 y cálculos de vibración/offset
- [x] Conectar `setClipShake(id, intensity)` en `EditorViewModel` con soporte para historial de deshacer/rehacer
- [x] Integrar el efecto de temblor en `videoPlayer.setVideoEffects` dentro de `EditorScreen.kt`
- [x] Integrar el efecto de temblor en `VideoExporter.kt` para la exportación final
- [x] Añadir selector de temblor (chips) y toggle de estabilizador en la interfaz de usuario de grabación y edición de clips
- [x] Escribir tests unitarios para `ShakeEffect` y persistencia de `VideoClip.shake`
- [x] Validar tests y flujo de compilación
