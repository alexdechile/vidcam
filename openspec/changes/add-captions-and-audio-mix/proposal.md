## Why

Tres huecos que se notan en cuanto el vídeo es para redes:

1. **Sin subtítulos, el vídeo se ve en silencio.** La mayoría lo mira sin audio,
   y hoy no hay forma de poner el texto encima, ni a mano ni a partir de la voz.
2. **El audio entra y sale de golpe.** La música arranca a volumen pleno en el
   segundo cero y corta igual al final; la voz también. Suena a borrador.
3. **La música compite con la voz.** Para que se entienda la narración hay que
   bajar la música a mano para todo el vídeo, perdiendo el fondo musical en los
   silencios.

## What Changes

- **Subtítulos quemados** con estilo propio (tamaño, altura, mayúsculas, fondo):
  se guardan en el proyecto con tiempos absolutos y se dibujan igual en la vista
  previa y en el MP4.
- **Transcripción de la voz en off** contra un endpoint configurable compatible
  con Whisper (el del homelab `:3007/transcribe` o la API de OpenAI): envía la
  toma, lee los tramos con tiempos y crea un subtítulo por tramo; si el servicio
  solo devuelve texto, se reparte sobre la duración de la toma. Sin servicio
  configurado, se avisa y no se toca el proyecto.
- **Fundidos de audio** de entrada y salida para música y voz, con curva suave
  (smoothstep), aplicados como envolvente en la exportación y en la vista previa.
- **Ducking**: la música baja sola mientras hay voz, con ataque y liberación, y
  vuelve en los silencios. El nivel de atenuación es configurable.
- **Alineación de 16 KB** de la librería nativa de RNNoise (quedaba a 4 KB, la
  única del APK), requisito de Google Play para `targetSdk 35`.

## Capabilities

### New Capabilities
- `video-captions`: subtítulos quemados, editor y transcripción de la voz.
- `audio-mix`: fundidos de entrada/salida y ducking de la música bajo la voz.

### Modified Capabilities
- El proyecto persiste subtítulos, estilo y parámetros de mezcla; sin ellos se
  comporta como antes (`captions` vacío, fundidos en 0, ducking en 0).

## Impact

- **Modelo**: `Caption`, `CaptionStyle`, `CaptionMath`, `AudioMixMath`,
  `Project.captions`, `project.captionStyle`, `musicFadeInMs/OutMs`,
  `duckingAmount`, `VoiceoverTrack.fadeInMs/OutMs`.
- **Exportación**: overlays de subtítulo (`CaptionOverlay`/`CaptionBitmaps`) en
  la misma lista que las capas, y `EnvelopeAudioProcessor` para las envolventes
  de música y voz.
- **Transcripción**: `data/Transcription` (cliente multipart + preferencias) y
  `data/TranscriptParser` (segmentos, chunks y reparto sin tiempos).
- **Editor**: hoja de subtítulos, diálogo de estilo/edición, hoja de voz con
  fundidos y ducking, y vista previa con los volúmenes recalculados a 20 Hz.
- **Tests**: `CaptionMathTest`, `AudioMixMathTest`, `TranscriptParserTest`.
- **Fuera de alcance**: traducción, estilos por palabra (karaoke), SRT/VTT
  externos, subtítulos automáticos sin voz grabada y transiciones visuales
  (requieren rearmar la composición por clip; van en su propio cambio).
