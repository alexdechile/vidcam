## Context

La exportación compone en Media3: una secuencia por clips, otra por fondos y otra
por música. Media3 1.5.1 mezcla el audio de las secuencias con
`DefaultAudioMixer`, y por eso la música ya suena sobre el audio del clip. Añadir
voz es una secuencia más, pero hay tres problemas propios del doblaje: cómo se
captura sin ruido, cómo se coloca en el tiempo y cómo se mezcla con volumen.

`EditedMediaItem` no expone volumen y `AudioMixer.setSourceVolume` no es API
pública, así que la ganancia por pista tiene que entrar como `AudioProcessor`.

## Goals / Non-Goals

**Goals:**
- Grabar la voz después del montaje, con el vídeo reproduciéndose como guía.
- Reducir el ruido de entorno de forma consistente entre equipos.
- Colocar la toma en el segundo correcto y poder ajustarla.
- Mezclar voz, audio original y música con volumen independiente.

**Non-Goals:**
- Narración en vivo con la cámara (el clip seguiría trayendo su propio audio sin
  limpieza; es otra función).
- Cancelación de eco con hardware dedicado (`AcousticEchoCanceler` varía por
  equipo y obliga a una fuente de audio de llamada, de banda más angosta).
- Varias tomas en capas, automatización de volumen y edición de onda.
- Regrabar el audio de un clip ya grabado dentro del mismo MP4.

## Decisions

### RNNoise vendorizado en vez de los efectos del sistema
`NoiseSuppressor`/`AcousticEchoCanceler` dependen del fabricante y, cuando
existen, están afinados para llamadas (16 kHz, agresivos y con bombeo). Medido en
un tono con ruido, RNNoise 0.1.1 deja el ruido y conserva la voz de forma
parecida en todos los teléfonos. Se vendoriza el código (BSD-3) en
`app/src/main/cpp/rnnoise` para que el build no dependa de descargas: ~250 KB de
`.so` sin comprimir por ABI, 5 s de compilación.

Se descarta RNNoise 0.2: su modelo pesa 29 MB de fuente y deja el APK varias
veces más grande, sin diferencia perceptible en una toma de 30 s de voz.

### La limpieza se aplica después de grabar, no en vivo
La toma se guarda cruda (WAV PCM 16 bits mono, 48 kHz, ~2,9 MB por 30 s) y
después se escribe `voz-…-limpia.wav`. Así el interruptor "Aislar entorno" se
puede apagar y encender sin volver a hablar, y el proceso no puede glitchear en
tiempo real. Sobre la toma cruda se aplica: paso alto 85 Hz, RNNoise mezclado con
la señal seca (0,9 por defecto) y nivelación con pico a −1 dBFS.

Sin la librería nativa disponible (ABI no incluida, tests de la JVM) el
procesamiento degrada a paso alto + nivelación y la app sigue funcionando.

### La toma se graba con `AudioRecord`, no con `MediaRecorder`
`MediaRecorder` obliga a decidir el codificador al abrir y da un contenedor que
hay que re-mezclar igual. `AudioRecord` con fuente `UNPROCESSED` (sin AGC ni
supresores del sistema) entrega PCM parejo, que es lo que RNNoise espera, y se
envuelve en WAV sin recomprimir. Si el equipo no expone `UNPROCESSED` se cae a
`VOICE_RECOGNITION` y luego a `MIC`.

### La colocación de la toma se resuelve escribiendo el WAV
`VoiceoverTrack.startMs` positivo agrega silencio delante y negativo recorta el
comienzo. En vez de depender de `addGap` o de números de muestra del mezclador,
la exportación escribe un WAV del tramo exacto (relleno + recorte, acotado a la
duración del vídeo) y lo entrega como secuencia de audio. La vista previa usa
`VoiceoverMath.filePositionMs` para sincronizar y `VoiceoverMath.plan` para
exportar: la misma regla en los dos caminos, con tests unitarios.

### El monitoreo baja al 25 % mientras se graba
Al doblar, el vídeo suena por el parlante y el micrófono lo captura. Bajar el
monitoreo a una cuarta parte deja escuchar la guía sin que el altavoz domine la
toma, y la hoja pide usar audífonos. No se activa AEC por hardware porque la
fuente que lo soporta baja la calidad del resto de la toma.

### Volumen por pista mediante `AudioProcessor`
`GainAudioProcessor` escala PCM 16 bits o float en la secuencia de cada pista.
Con volumen 0 la pista se omite (el audio original además se marca
`setRemoveAudio`), y la voz con volumen 0 no entra a la mezcla. El "ducking" es
estático: el usuario baja la música a mano, igual que en una consola.

## Risks / Trade-offs

- **Mezcla de tres secuencias**: es el mismo mecanismo que ya mezcla música y
  clip. Si un equipo no lo soporta, la exportación falla con un mensaje de
  Media3; no hay plan B con mezcla propia en esta entrega.
- **NDK en CI**: agrega descarga e instalación (~1 min) a cada build. Se fija
  NDK 27.0.12077973 y CMake 3.22.1 para que el resultado sea reproducible.
- **48 kHz obligatorio**: si un equipo no soporta 48 kHz mono para `AudioRecord`,
  la toma no arranca y se avisa. Se elige así porque RNNoise solo trabaja a
  48 kHz y remuestrear agregaría latencia y dependencias.
- **Sin desfase automático**: la latencia de salida puede hacer que la voz quede
  unos milisegundos tarde. Se resuelve con el slider "Entrada" (±2 s con pasos
  de 1 ms en el modelo y 50 ms en la UI), no con una estimación que podría
  empeorarlo.
