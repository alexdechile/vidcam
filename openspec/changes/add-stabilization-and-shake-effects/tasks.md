# Tasks

- [ ] Añadir enum `ShakeIntensity` (NINGUNO, SUAVE, MEDIO, FUERTE) al modelo y campo `shake` en `VideoClip` con default `NINGUNO`
- [ ] Implementar soporte de estabilización de vídeo (EIS/OIS) en `CameraRecorder.kt` usando `Camera2CameraControl`
- [ ] Crear `ShakeEffect.kt` con generador de `MatrixTransformation` de Media3 y cálculos de vibración/offset
- [ ] Conectar `setClipShake(id, intensity)` en `EditorViewModel` con soporte para historial de deshacer/rehacer
- [ ] Integrar el efecto de temblor en `videoPlayer.setVideoEffects` dentro de `EditorScreen.kt`
- [ ] Integrar el efecto de temblor en `VideoExporter.kt` para la exportación final
- [ ] Añadir selector de temblor (chips) y toggle de estabilizador en la interfaz de usuario de grabación y edición de clips
- [ ] Escribir tests unitarios para `ShakeEffect` y persistencia de `VideoClip.shake`
- [ ] Validar tests y flujo de compilación
