## 1. GIF animado

- [x] 1.1 `LayerKind.GIF` y `media/GifAnimation` con detección por cabecera,
      rasterizado acotado (24 fotogramas, 384 px) y emparejamiento puro
      tiempo→fotograma
- [x] 1.2 `EditorViewModel`: al agregar una imagen, inspeccionar la copia y crear
      la capa como GIF si corresponde
- [x] 1.3 `EditorScreen.LayerBox`: fotograma según el instante y parámetro de
      tiempo en la vista previa y en el resumen de exportación
- [x] 1.4 `export/Overlays`: overlay que devuelve el fotograma del GIF para cada
      fotograma del vídeo

## 2. Duración fijada de antemano

- [x] 2.1 `Project.targetDurationMs`, `outputDurationMs`, `canExport`,
      `hasStillContent` y `TARGET_DURATIONS_MS`
- [x] 2.2 `TimelineMath.recordingLimitMs` y música medida sobre la duración de
      salida
- [x] 2.3 `EditorViewModel.setTargetDuration` con arrastre de capas y fondos
- [x] 2.4 `EditorScreen`: chips de duración, parada automática de la grabación y
      cronómetro

## 3. Vídeos solo con PNG

- [x] 3.1 `export/VideoExporter`: exportar sin clips usando la secuencia de
      imágenes como pista de vídeo; compositor de ventana solo con clips
- [x] 3.2 `EditorScreen`: vista previa del lienzo sin clips (reloj propio, sin
      PlayerView), botón "Exportar" habilitado y resumen sin fotograma de vídeo
- [x] 3.3 Capas, fondos y sliders medidos sobre la duración de salida

## 4. Verificación y entrega

- [x] 4.1 `GifAnimationTest` y `TargetDurationTest`
- [ ] 4.2 CI en verde (lint + test + assemble debug, release y androidTest)
- [ ] 4.3 APK release enviado por WhatsApp
