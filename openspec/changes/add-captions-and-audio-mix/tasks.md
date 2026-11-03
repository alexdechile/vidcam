## 1. Subtítulos

- [ ] 1.1 `Caption`, `CaptionStyle` y `CaptionMath` (visibilidad, mayúsculas,
      normalización sin solapes)
- [ ] 1.2 `Project.captions` y `Project.captionStyle` con valores por defecto
      compatibles
- [ ] 1.3 `export/Captions` (rasterizado con corte de líneas, contorno y fondo) y
      `CaptionOverlay` en la exportación
- [ ] 1.4 `editor/CaptionView` en la vista previa con las mismas fracciones
- [ ] 1.5 `CaptionsSheet`: transcribir, agregar, editar, quitar y estilo

## 2. Transcripción

- [ ] 2.1 `data/TranscriptParser` (segments, chunks, texto plano y reparto)
- [ ] 2.2 `data/Transcription` (cliente multipart, resultado tipado y
      preferencias de URL/clave/idioma/modelo)
- [ ] 2.3 `EditorViewModel.transcribeVoiceover` con desplazamiento por entrada y
      recorte de la toma

## 3. Mezcla dinámica

- [ ] 3.1 `AudioMixMath` (fundido suave, envolvente de ducking, niveles por
      bloque y ganancia combinada)
- [ ] 3.2 `audio/EnvelopeAudioProcessor` con ganancia por bloque
- [ ] 3.3 Exportación: música con fundidos × ducking × volumen y voz con
      fundidos × volumen
- [ ] 3.4 Vista previa: volúmenes recalculados a 20 Hz con la misma curva
- [ ] 3.5 Controles de fundidos y ducking en la hoja de voz

## 4. Huecos técnicos

- [ ] 4.1 Alineación de 16 KB en la librería nativa de RNNoise

## 5. Pruebas y documentación

- [ ] 5.1 `CaptionMathTest` (visibilidad, mayúsculas, normalización, límites)
- [ ] 5.2 `AudioMixMathTest` (fundidos, ducking, umbral, ganancia combinada,
      alineación temporal)
- [ ] 5.3 `TranscriptParserTest` (segmentos, chunks, texto plano, vacíos)
- [ ] 5.4 Change OpenSpec `add-captions-and-audio-mix`, README y `alex.md`
- [ ] 5.5 CI en verde, APK con alineación 16 KB verificada y release publicado
