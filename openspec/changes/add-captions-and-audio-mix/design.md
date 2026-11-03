## Context

El proyecto ya tiene una toma de voz en off con aislación (RNNoise) y volumen por
pista, y la exportación compone pistas separadas que Media3 mezcla. Faltan dos
cosas para que el audio suene acabado —dinámica (fundidos, ducking)— y una para
que el vídeo se entienda sin sonido: subtítulos.

La vista previa dibuja capas PNG/texto y la exportación las replica como
overlays de Media3 con la misma geometría normalizada. Los subtítulos siguen ese
mismo camino para no inventar un segundo sistema de coordenadas.

## Goals / Non-Goals

**Goals:**
- Subtítulos quemados con estilo global y tiempos por subtítulo.
- Crear subtítulos a partir de la voz grabada, con tiempos reales si el servicio
  los entrega.
- Fundidos de entrada/salida y ducking audibles igual en vista previa y MP4.
- Cerrar el requisito de páginas de 16 KB de la librería nativa.

**Non-Goals:**
- Transiciones visuales entre clips (corte seco sigue siendo el único: Media3 no
  trae crossfade y hay que rearmar la composición con una secuencia por clip).
- Traducción, karaoke palabra por palabra, importar/exportar SRT.
- Transcripción sin voz grabada o sin red.

## Decisions

### Un endpoint configurable en vez de un SDK de transcripción
La app no trae claves ni servidor propio. `TranscriptionClient` hace un
`multipart/form-data` con el campo `file` a la URL configurada (homelab o
OpenAI), con `HttpURLConnection` para no sumar OkHttp por una llamada. Se
guardan URL, clave opcional, idioma y modelo en `SharedPreferences`.

`TranscriptParser` acepta `segments[]` (Whisper `verbose_json`), `chunks[]` y
texto plano. Sin tiempos, reparte el texto en tramos de ≤8 palabras y ≤3,5 s
proporcionales a la duración: suficiente para un primer pase que luego se edita.
Los tramos se desplazan con `VoiceoverMath.plan` (relleno y recorte de la toma),
así el subtítulo cae donde suena la voz.

### Estilo global con fracciones del fotograma
`CaptionStyle.sizeFraction` y `yFraction` son fracciones del alto del vídeo. La
vista previa las multiplica por el alto del lienzo en pantalla y la exportación
por el alto del fotograma: mismos píxeles relativos, sin depender de la
densidad. El texto se rasteriza una sola vez por subtítulo (bitmap con fondo y
contorno) y se entrega como `CaptionOverlay` con alfa por ventana de tiempo.

### El corte de líneas es el mismo en ambos caminos
La exportación corta el texto midiendo con `Paint` (ancho máximo 86 % del
fotograma); la vista previa usa `widthIn` con la misma fracción y el mismo
tamaño. La diferencia de salto de línea posible es cosmética; la alternativa
—rasterizar también en la vista previa— costaba nitidez y memoria.

### Envolventes en vez de mezcla manual
`EnvelopeAudioProcessor` evalúa la ganancia cada 50 ms y la aplica en bloque.
Con eso se implementan fundidos y ducking sin reescribir el mezclador de Media3:
la música entra con `fade × duck × volumen` y la voz con `fade × volumen`.

El ducking se calcula una vez por exportación con los niveles de la voz ya
colocados en la línea de tiempo (`AudioMixMath.timelinePeaks`), con umbral de
−42 dBFS, ataque de 60 ms y liberación de 320 ms. En la vista previa se
recalcula a 20 Hz sobre el reproductor de música, para que se escuche igual.

### La alineación de 16 KB va en el enlazador
`-Wl,-z,max-page-size=16384` en `CMakeLists.txt`. Sin eso la `.so` de RNNoise
queda a 4 KB y no carga en Android 15 con páginas de 16 KB; el resto de librerías
del APK ya estaba alineada a 16 KB.

## Risks / Trade-offs

- **Transcripción sin tiempos**: el reparto proporcional puede desincronizarse si
  el habla tiene pausas largas. Se acepta como primer pase editable.
- **Ducking por bloques**: la atenuación tiene resolución de 50 ms; con música
  muy rítmica puede notarse, pero evita el coste de una envolvente por muestra.
- **Subtítulos sobre vídeo con ventana**: se dibujan en coordenadas del
  fotograma completo, igual que las capas; si la ventana está muy arriba, el
  subtítulo puede quedar sobre el fondo y no sobre el vídeo.
- **Endpoint externo**: el audio sale del teléfono hacia el servicio elegido; es
  responsabilidad del usuario apuntarlo a uno de confianza.
