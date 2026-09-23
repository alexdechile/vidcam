## 1. Modelo y matemática de timeline

- [ ] 1.1 Añadir `playbackSpeed: Float = 1f` a `VideoClip` y constantes de velocidades válidas (`CLIP_SPEEDS`) en `Project.kt`
- [ ] 1.2 Añadir `effectiveDurationMs` a `VideoClip` y hacer `TimelineMath.totalDuration`/`clipStartMs`/`trimToMax` conscientes de velocidad (presupuesto en timeline × velocidad para origen)

## 2. Editor y exportación

- [ ] 2.1 `EditorViewModel`: `addRecordedClip(file, speed)`, `setClipSpeed(id, speed)` con presupuesto de 30 s (recorte defensivo) y aviso si no cabe
- [ ] 2.2 `VideoExporter.buildSequences`: aplicar `setSpeed(clip.playbackSpeed)` a cada `EditedMediaItem`
- [ ] 2.3 `TimelinePreview`: base de timeline en duración efectiva, `positionMs = base + currentPosition / speed` y `PlaybackParameters(speed, speed)` por clip en el loop de fotogramas

## 3. UI

- [ ] 3.1 Selector de velocidad (chips 0.25x/0.5x/1x/2x/4x) en `ControlsSection` junto a los controles de cámara; pasar el valor elegido a la grabación
- [ ] 3.2 Selector de velocidad por clip en `ClipRow` (independiente del recorte) con `setClipSpeed`
- [ ] 3.3 Versión 1.1.0 (versionCode 7) en `app/build.gradle.kts` y nota en README "Comportamiento"

## 4. Verificación

- [ ] 4.1 Tests unitarios de `TimelineMath`/modelo: duración efectiva, recorte con velocidad y límite de 30 s
- [ ] 4.2 Empujar a `main`, CI verde (lint + test + assemble); luego tag `v1.1.0` para el release firmado
- [ ] 4.3 Descargar el APK release y enviarlo por WhatsApp al +56 9 9221 5761 con la skill de telomando