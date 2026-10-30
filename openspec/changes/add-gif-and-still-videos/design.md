## Context

VidCam compone en Media3: la exportación prepara los fondos como PNG del tamaño
del fotograma y los entrega como secuencia de imágenes con duración
(`EditedMediaItem.setDurationUs` + `setFrameRate`), mientras las capas se dibujan
con `BitmapOverlay` sobre el fotograma. La vista previa (`TimelinePreview`)
reproduce los clips con ExoPlayer y dibuja el mismo lienzo en Compose.

Ese camino de imágenes ya existía solo para fondos; esta propuesta lo aprovecha
para exportar proyectos sin clips. Ver `proposal.md` para la motivación y las
tres specs de `specs/`.

## Goals / Non-Goals

**Goals:**
- GIF animado como capa, idéntico en vista previa y MP4.
- Duración de grabación fijada de antemano, con parada automática.
- Exportar un proyecto solo con PNG (fondos, capas, textos, música).

**Non-Goals:**
- GIF animado como fondo (un fondo se recorta a un PNG fijo).
- WebP/HEIF animados.
- Recortar la duración de un clip ya grabado (eso es el recorte existente).

## Decisions

### El GIF se rasteriza una vez en una lista corta de fotogramas
`android.graphics.Movie` decodifica GIF pero solo dibuja sobre un `Canvas`; la
exportación pide un `Bitmap` por fotograma. En vez de redibujar en cada
fotograma del MP4, `GifAnimation` muestrea el bucle del GIF a 10 Hz con un tope
de 24 fotogramas y 384 px de lado, y cachea dos GIF como mucho. El costo es
acotado (≈14 MB en el peor caso) y el comportamiento determinista; la pérdida de
suavidad es invisible para un sticker sobre un vídeo de 30 fps.

### El GIF se detecta por cabecera, no por extensión
Los archivos importados se copian con extensión `.png` (el camino antiguo), así
que la extensión miente. `GifAnimation.isGif(File)` lee los 6 primeros bytes
(`GIF87a`/`GIF89a`), que es lo que decide el formato real. Un JPEG sigue siendo
PNG estático, y un GIF conserva la animación sin importar cómo se llamó al
archivo.

### Un solo campo para dos usos: `targetDurationMs`
La duración fijada sirve de tope de grabación y de duración del vídeo cuando no
hay clips. `Project.outputDurationMs` resuelve cuál manda: los clips si
existen, la duración fijada si no. `totalDurationMs` no cambia su significado
(suma de clips) para no romper el presupuesto de 30 s, el recorte ni la
timeline; solo la música y la exportación miran `outputDurationMs`.

### Reutilizar la secuencia de imágenes de Media3 para el vídeo de PNG
El MP4 de imágenes es la misma construcción que ya usan los fondos: una
secuencia de `EditedMediaItem` de imágenes con duración y sin audio, más la
secuencia de música opcional y el `OverlayEffect` de capas. Sin clips, esa
secuencia es la entrada 0 del compositor, así que no se aplica
`WindowCompositorSettings` (no hay ventana de vídeo que mover).

### La vista previa sin clips usa un reloj propio
`TimelinePreview` reproduce con ExoPlayer cuando hay clips; sin clips no hay
pista que reproducir, así que un contador avanza con `withFrameNanos` sobre
`outputDurationMs` y se reinicia al terminar. Así los GIF se animan, las capas
animadas se reproducen y los gestos de arrastre siguen funcionando sin
duplicar la pantalla.

### La duración fijada arrastra capas y fondos
Al cambiar de 30 s a 5 s, una capa que llegaba al final quedaría fuera del
vídeo. `setTargetDuration` estira o recorta (con `LayerTiming`) las capas y
fondos que terminaban en el final anterior, y deja intactos los que terminaban
antes. Se puede deshacer como cualquier otra edición.

## Risks / Trade-offs

- **Un GIF de muchos fotogramas se muestrea a 10 fps**: suficiente para
  stickers, no para animaciones de 30 fps; el tope de memoria lo justifica.
- **La música de un vídeo de imágenes puede desincronizarse de la animación de
  los GIF al repetir el bucle de vista previa**: el MP4 exportado sí queda
  alineado, que es lo que importa.
- **Cambiar la duración fijada modifica capas existentes**: es intencional y
  reversible con "Deshacer".
