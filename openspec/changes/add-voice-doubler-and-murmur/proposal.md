# Proposal

## Why

Los creadores de contenido buscan dinamismo, impacto y dramatismo en sus locuciones de vídeos verticales cortos (Reels, TikTok, Shorts). En VidCam, la voz en off actual solo permite grabar una toma monofónica en tono natural y aislarla con RNNoise.

Incorporar doblaje automático (ADT - Artificial Double Tracking), modulación tonal (pitch shifting) y generación de murmullo multi-voz permite transformar una sola toma de voz en una locución cinematográfica profunda, un coro armónico o un murmullo de multitud desfasado, con control total e intuitivo para el usuario sin tener que regrabar.

## What Changes

- **Modelo de datos de doblaje y murmullo (`VoiceDoublerConfig`)**:
  - Incorpora modos/presets: `OFF`, `GRAVE` (-12 semitonos / tráiler de cine), `AGUDO` (+12 semitonos / coro alto), `ESTUDIO` (duplicado engrosado ADT al unísono con micro-delay y micro-detune), `MURMULLO` (3 a 5 voces desfasadas en tiempo y tono simulan una multitud/cuchicheo) y `PERSONALIZADO`.
  - Parámetros granulares controlables por el usuario:
    - `voiceCount`: número de voces dobladas añadidas (1 a 5).
    - `pitchSemitones`: desplazamiento de tono (-12 a +12 semitonos).
    - `delayMs`: desfase temporal entre las voces (10 a 80 ms).
    - `detuneCents`: micro-desafinación (±5 a ±30 cents) para romper la sincronía perfecta y aportar naturalidad humana sin cancelación de fase metálica.
    - `wetMix`: proporción de volumen de las voces dobladas frente a la voz original principal (0.0 a 1.0).
- **Extensión de `VoiceoverTrack`**:
  - Guarda la configuración de doblaje en el proyecto para que persista en el JSON y soporte Deshacer/Rehacer (`UndoHistory`).
- **Motor DSP de doblaje (`VoiceDoublerProcessor`)**:
  - Procesa la toma de voz PCM 16-bit a 48 kHz (aprovechando la señal limpia de RNNoise o cruda).
  - Algoritmo de pitch shifting por modulación de muestreo / SOLA (Waveform Similarity Overlap-Add) y buffer de retardo para clonar las pistas.
  - Mezcla ponderada y escalado de ganancia suave para evitar saturación (*clipping* digital).
- **Interfaz en `VoiceoverSheet`**:
  - Chips de presets inmediatos de un toque ("Apagado", "Grave", "Agudo", "Estudio", "Murmullo", "Ajustes").
  - Controles deslizantes desplegables para afinar voces, tono, desfase y volumen de mezcla.
- **Sincronización en vista previa y exportación MP4**:
  - La pista activa procesada con doblaje se reproduce de forma fluida en la vista previa del editor y se inyecta en `VideoExporter` al renderizar el MP4 final.

## Capabilities

### New Capabilities
- `voice-doubler`: Generación de doblaje automático, transposición de tono (pitch shifting), micro-desfase temporal y multiplicación multi-voz en capas para simular coros o murmullos controlados sobre la locución.

### Modified Capabilities
<!-- Sin modificaciones a requerimientos de specs previas -->

## Impact

- **Modelos**: `VoiceoverTrack` en `app/src/main/java/com/vidcam/app/model/Project.kt` suma `doubler: VoiceDoublerConfig`.
- **Audio DSP**: nuevo componente `VoiceDoublerProcessor.kt` y utilidades matemáticas en `PcmDsp.kt` / `VoiceoverProcessor.kt`.
- **UI Compose**: sección de doblaje en `app/src/main/java/com/vidcam/app/editor/VoiceoverSheet.kt`.
- **ViewModel**: métodos para alternar presets y ajustar sliders de doblaje con actualización atómica de proyecto.
- **Exportación**: `VideoExporter.kt` exporta la pista resultante con la mezcla de doblaje sin romper el ducking ni los fundidos.
- **Tests**: tests unitarios de modelos, serialización JSON, cálculos de retardo y DSP en JVM.
