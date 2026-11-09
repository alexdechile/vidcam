# Design

## Context

En VidCam, la voz en off se graba como PCM 16-bit mono a 48 kHz (`WavFile`), se limpia opcionalmente con RNNoise (`VoiceoverProcessor.denoise`) y se almacena en el sistema de archivos de la app. La reproducción en el editor usa un `ExoPlayer` secundario sincronizado a la línea de tiempo del vídeo, y la exportación MP4 inyecta la pista a través de `VideoExporter.buildVoiceItem` con soporte de fundidos y ducking.

## Goals / Non-Goals

**Goals:**
- Extender el modelo con `VoiceDoublerConfig` y `VoiceDoublerPreset` (`OFF`, `GRAVE`, `AGUDO`, `ESTUDIO`, `MURMULLO`, `PERSONALIZADO`).
- Implementar un motor DSP ligero (`VoicePitchShifter` y `VoiceDoublerEngine`) capaz de modular tono (-12 a +12 semitonos), aplicar micro-desfases de tiempo (10 a 80 ms) y sumar hasta 5 voces clones con humanización sin clipping.
- Generar el audio duplicado de manera no destructiva: la toma cruda y la toma aislada se conservan; el doblaje se sintetiza rápidamente sobre la señal base.
- Integrar controles táctiles en `VoiceoverSheet` con chips de presets inmediatos y sliders para ajuste granular.
- Garantizar que la pista procesada fluya de forma idéntica en la vista previa del editor y en la exportación MP4 de Media3.

**Non-Goals:**
- Corrección de tono en vivo (*auto-tune* o corrección tonal cromática por notas musicales) durante la grabación.
- Doblaje de voz por inteligencia artificial generativa o síntesis de texto a voz (TTS).

## Decisions

### 1. Algoritmo DSP de Pitch Shifting y Multi-Capa en Kotlin puro
- **Decisión**: Implementar `VoicePitchShifter` mediante un algoritmo granular OLA (Overlap-Add) optimizado para voz humana en Kotlin puro, complementado con búferes circulares de retardo (`delayMs`) y micro-desafinación (`detuneCents`).
- **Razón**: Permite ejecutar pruebas unitarias instantáneas en la JVM (`AudioMixMathTest`, `VoiceDoublerTest`) sin depender del emulador ni de bibliotecas C++ adicionales, y procesa tomas de 30 segundos en menos de 150 milisegundos en CPU móvil.
- **Alternativas consideradas**:
  - *SonicAudioProcessor de Media3*: Solo opera en streaming dentro del pipeline de Media3; no permite exportar/manipular archivos WAV directos fuera del reproductor de forma sencilla ni inspeccionar los picos para ducking.
  - *Librería C++ nativa (SoundTouch)*: Aumentaría el tamaño del APK y requeriría bindings JNI adicionales.

### 2. Estructura de generación de audio en cadena
- **Decisión**: La cadena de procesamiento opera en etapas desacopladas:
  `Toma Cruda (.wav)` → `Aislación RNNoise (.wav)` → `Doblador / Murmullo (.wav)` → `activeUri`.
- **Razón**: Cuando el usuario mueve los controles de doblaje, no es necesario volver a calcular RNNoise; el motor parte directamente de la señal limpia existente, ofreciendo una respuesta prácticamente inmediata.

### 3. Mezcla ponderada y prevención de saturación (*Anti-Clipping*)
- **Decisión**: Al superponer de 1 a 5 clones sobre la voz original, la suma de amplitudes puede sobrepasar el límite de 16-bit (`±32767`). El motor aplica un factor de escala ponderado en función del número de voces activas y la proporción `wetMix`, finalizando con un compresor/limitador suave antes de escribir el archivo PCM final.

### 4. Experiencia de usuario en Compose
- **Decisión**: Presentar una fila de chips horizontales para cambios rápidos de 1 toque (`Normal`, `Grave`, `Agudo`, `Estudio`, `Murmullo`) y un acordeón desplegable "Ajustes avanzados" con sliders (`Voces`, `Tono`, `Desfase`, `Mezcla`).
- **Razón**: El 80% de los usuarios querrá un efecto directo e impactante con un solo toque, mientras que los usuarios avanzados tendrán control milimétrico sobre cada parámetro.

## Risks / Trade-offs

- **[Carga de CPU al mover sliders continuamente]** → *Mitigación*: Se aplica un retardo (*debounce* de 150 ms) en el ViewModel para evitar disparar síntesis repetitivas mientras el usuario desliza el control.
- **[Efecto robótico o desfase metálico tipo peine]** → *Mitigación*: Cada voz clonada en el preset de murmullo tiene un micro-desfase variable no correlacionado y una micro-desafinación de unos pocos *cents* que destruyen la interferencia periódica y generan la acústica de una sala con varias personas.
