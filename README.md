# VidCam

Aplicación Android para grabar y editar vídeos verticales de hasta 30 segundos:
cámara con audio, importación de clips, biblioteca de música del dispositivo,
capas PNG transparentes y textos con Google Fonts arrastrables, escalables,
rotables y animables con grabación de movimiento en el lienzo, fondos PNG a
pantalla completa por tramos, ventana de vídeo móvil y girable con forma 1:1 o
9:16, deshacer/rehacer y proyectos guardados en JSON, con exportación a MP4
(H.264/AAC) con guardado en galería y hoja de compartir, recepción de vídeos,
imágenes y música compartidos desde otras apps, capas GIF animadas, vídeos
hechos solo con imágenes y voz en off (doblaje) con aislación de entorno y
volumen por pista.

El comportamiento y las decisiones técnicas están especificados en OpenSpec, en
`openspec/changes/add-vidcam-mvp/`,
`openspec/changes/add-layer-motion-recording/`,
`openspec/changes/add-project-persistence-and-undo/`,
`openspec/changes/add-clip-speed/`,
`openspec/changes/add-background-and-video-window/`,
`openspec/changes/add-shared-file-intake/`,
`openspec/changes/add-gif-and-still-videos/` y
`openspec/changes/add-voiceover/`.

## Voz en off

- **Doblar**: botón `Voz` en el editor. La vista previa vuelve al principio, se
  reproduce con el monitoreo al 25 % y la toma se graba desde el segundo cero
  hasta que termina el vídeo (o hasta `Detener`). Se puede regrabar las veces
  que haga falta.
- **Aislación de entorno**: la toma se guarda cruda a 48 kHz mono y se procesa
  después con paso alto, RNNoise (0.1.1 vendorizado, JNI/CMake) y nivelación a
  −1 dBFS. El interruptor `Aislar entorno (RNNoise)` compara la toma limpia con
  la cruda sin volver a hablar. Sin la librería nativa disponible se aplica solo
  el filtro y la nivelación.
- **Entrada**: el slider mueve la toma dentro del vídeo (−2 s recorta el
  comienzo, valores positivos la retrasan); lo que pasa del final del vídeo no
  se exporta.
- **Mezcla**: la misma hoja ajusta el volumen de la voz, del audio original y de
  la música. En la exportación cada pista entra al mezclador de Media3 ya
  escalada (`GainAudioProcessor`), porque `EditedMediaItem` no expone volumen.
- La vista previa reproduce la voz con un segundo `ExoPlayer` sincronizado al
  vídeo; mientras se graba, la voz existente se silencia para no realimentar el
  micrófono.

## Fondos y ventana de vídeo

- **Fondo**: botón `Fondo` para elegir un PNG que cubre todo el lienzo por
  debajo del vídeo y de las capas. Se pueden agregar varios fondos con tramos
  propios; los huecos quedan negros y los tramos nunca se solapan.
- **Ventana**: el vídeo se puede recortar a `1:1` o `9:16`, arrastrar, pellizcar
  para escalar y girar con dos dedos. `Centrar` restablece posición, escala y
  giro. Con `Original` y escala 1 ocupa el lienzo completo, como antes.
- La vista previa y el MP4 usan la misma geometría: la exportación compone el
  fondo y la ventana con el compositor de Media3 y mantiene el camino anterior
  cuando el proyecto no usa estas funciones.

## Requisitos

- JDK 17
- Android SDK con `compileSdk 35`
- NDK 27.0.12077973 y CMake 3.22.1 (solo para compilar el módulo nativo de
  RNNoise; el CI los instala solos)
- Gradle wrapper incluido (`./gradlew`)

## Compilar y probar

```bash
./gradlew lint test assembleDebug assembleDebugAndroidTest assembleRelease
```

- APK de depuración: `app/build/outputs/apk/debug/`
- APK de release minificado: `app/build/outputs/apk/release/`
- Tests instrumentados de Compose en `app/src/androidTest/`: se compilan en CI y
  se ejecutan en dispositivo o emulador con `connectedDebugAndroidTest`.

## Integración continua

`.github/workflows/ci.yml` ejecuta
`lint test assembleDebug assembleDebugAndroidTest assembleRelease` en cada push a
`main` y sube el APK de depuración, el APK de release y el reporte de lint como
artefactos. El release se minifica con R8 (`~3 MB` frente a `~15 MB` sin
minificar); los `.so` incluidos suman menos de 300 KB, por lo que los splits por
ABI no aportan.

## Firmar y publicar (release)

`.github/workflows/release.yml` se dispara al empujar un tag `v*` (por ejemplo
`v1.0.0`) o manualmente desde la pestaña Actions. Genera `assembleRelease` y
`bundleRelease`, publica los artefactos APK/AAB y crea un GitHub Release.

La firma de release se toma de los siguientes *repository secrets*:

| Secret              | Descripción                                                        |
| ------------------- | ------------------------------------------------------------------ |
| `KEYSTORE_BASE64`   | Keystore JKS codificado en base64                                  |
| `KEYSTORE_PASSWORD` | Contraseña del almacén de claves                                   |
| `KEY_ALIAS`         | Alias de la clave de firma                                         |
| `KEY_PASSWORD`      | Contraseña de la clave de firma                                    |

Para crear un keystore y generar el valor de `KEYSTORE_BASE64`:

```bash
keytool -genkeypair -v -keystore release.jks -alias vidcam \
  -keyalg RSA -keysize 2048 -validity 10000 -dname "CN=VidCam"
base64 -w 0 release.jks
```

El APK de release se minifica con R8 y `shrinkResources` (`isMinifyEnabled` y
`isShrinkResources` en `app/build.gradle.kts`); las reglas adicionales viven en
`app/proguard-rules.pro` y las de las librerías las aportan sus reglas de
consumer.

Si los secrets no están definidos, el workflow de release falla con un mensaje
explícito y nunca publica un APK sin firmar; el build de Gradle degrada a la
firma de depuración, que es la que usa CI para validar el release.

Para publicar una versión:

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Comportamiento

- Si faltan los permisos de cámara o micrófono pero hay acceso a medios, la app
  abre el editor en modo solo importación: oculta los controles de grabación y
  ofrece solicitar el permiso o abrir los ajustes.
- Las capas (PNG con alfa y textos) se manipulan directamente en la vista previa:
  los gestos se reconocen sobre todo el lienzo, no solo sobre la capa. Un dedo
  arrastra, dos dedos escalan y rotan (pellizco) aunque un dedo caiga fuera de
  la capa, y un toque selecciona la capa bajo el dedo, que se resalta con un
  borde de selección. La posición, escala y rotación se guardan normalizadas en
  el modelo y se replican en la exportación.
- **Duración en pantalla por capa**: cada capa de la lista "Capas" tiene dos
  controles en segundos —"En pantalla", que fija cuántos segundos se ve, y
  "Aparece en", que fija en qué segundo entra conservando esa duración—. Los
  valores se acotan a la duración real del proyecto, así que una capa nunca puede
  durar ni empezar más allá del final del vídeo. La vista previa responde al
  instante y la exportación aplica la misma ventana fotograma a fotograma, de modo
  que un sticker puede durar 3 s de un vídeo de 20 s. El botón de edición de la
  fila abre además tamaño, posición y fuente.
- **Grabación de movimiento**: con el botón "Grabar movimiento" el vídeo se
  reproduce desde el inicio y los gestos que el usuario aplica sobre una capa se
  registran como fotogramas clave en el tiempo en que ocurren. La vista previa
  reproduce la animación sincronizada con el vídeo y la exportación la evalúa en
  cada fotograma, así que el MP4 conserva la misma trayectoria, escala y
  rotación. La línea de tiempo muestra las marcas de cada fotograma clave y
  permite borrar una marca o toda la animación de una capa (que entonces vuelve
  a quedar fija).
- La vista previa dibuja el lienzo con la misma relación de aspecto y el mismo
  encuadre que el fotograma de salida (el del primer clip, ajustado *fit* dentro
  del hueco disponible). Así las coordenadas normalizadas de las capas coinciden
  exactamente entre edición y exportación.
- **Cámara rápida y cámara lenta**: cada clip tiene una velocidad de
  reproducción (0.25x, 0.5x, 1x, 2x o 4x). Antes de grabar se elige con unos
  chips junto a los controles de cámara, y cualquier clip de la línea de tiempo
  permite cambiarla después. La duración que un clip ocupa en el timeline es su
  recorte dividido por la velocidad (rápido acorta, lento alarga), el límite de
  30 s se mide sobre esa duración efectiva —si una velocidad no cabe, el clip se
  recorta por el final—, la vista previa y el MP4 exportado reproducen el efecto
  con su audio, y la velocidad se guarda en el JSON del proyecto.
- **Nuevo proyecto**: el botón "Nuevo" de la barra superior descarta el trabajo
  actual y deja el editor vacío. Pide confirmación solo cuando hay cambios sin
  guardar.
- **Deshacer y rehacer**: los botones de la fila de controles retroceden o
  rehacen las ediciones del proyecto (clips, recortes, capas, animación, música).
  Las ediciones continuas —arrastrar un recorte, mover o pellizcar una capa,
  desplazar un control— se agrupan en una sola entrada de historial, así que
  deshacer retrocede el gesto completo y no un fotograma intermedio. El
  historial se pierde al abrir o crear otro proyecto.
- **Proyectos guardados (JSON)**: el menú de la barra superior ("⋮") permite
  guardar, guardar como y abrir proyectos. Cada proyecto se serializa con
  kotlinx.serialization a un JSON en `filesDir/projects/`, con nombre y fechas;
  la lista muestra los más recientes primero y permite borrarlos. Los medios que
  referencian los clips y las imágenes importadas se copian a `filesDir/media`
  (no a la caché) para que un proyecto guardado siga funcionando después.
- **Apertura en la galería**: al terminar la exportación, además de guardar el
  MP4 en la galería, la app lo abre en el visor de vídeo del sistema mediante
  `ACTION_VIEW`. Si ninguna aplicación puede manejarlo, el vídeo queda guardado
  igualmente y solo se muestra el aviso.
- **GIF animados**: un GIF compartido desde otra app o elegido con el botón
  "PNG" entra como capa animada (`LayerKind.GIF`). El formato se detecta por la
  cabecera del archivo, no por el nombre, así que también funciona cuando la
  copia interna se guarda como `.png`. El GIF se ve en movimiento en la vista
  previa y en el MP4: sus fotogramas se rasterizan una vez (máximo 24, 384 px de
  lado) y se elige el que corresponde al tiempo del vídeo. Un GIF usado como
  *fondo* se sigue viendo con su primer fotograma.
- **Duración fijada de antemano**: los chips "Duración" (5, 10, 15 o 30 s)
  determinan cuánto graba la cámara —se detiene sola y muestra un cronómetro— y
  cuánto dura un vídeo hecho solo con imágenes. Nunca se pasa de lo que queda
  libre del proyecto de 30 s. La elección se guarda en el JSON del proyecto.
- **Vídeos solo con PNG**: con un fondo, capas PNG/texto o GIF ya se puede
  exportar, aunque no haya ningún clip. El MP4 se arma con la secuencia de
  imágenes preparadas (o un lienzo negro si no hay fondo) durante la duración
  fijada, con las capas y la música por encima; la vista previa muestra ese
  lienzo con gestos y animación, y el diálogo de exportación lo resume sin
  esperar un fotograma de vídeo que no existe.
- **Archivos compartidos desde otras apps**: VidCam aparece en la hoja de
  compartir de Android (y en "Abrir con") para vídeo, imagen y audio. Un vídeo
  compartido entra como clip de la línea de tiempo (con el límite de 30 s y el
  recorte automático de siempre), una imagen entra como capa PNG y un audio pasa
  a ser la música del proyecto. Los archivos se copian a `filesDir/media` antes
  de usarse, así que un proyecto guardado sigue funcionando cuando caduca el
  permiso del remitente. Si VidCam ya está abierta, el material se agrega al
  proyecto en curso sin abrir otra copia de la app —y se puede deshacer—; si aún
  no hay permisos, la pantalla de permisos avisa que hay archivos esperando y los
  incorpora al concederlos.
- **Música**: el selector lista la música del dispositivo y, debajo, enlaces a
  fuentes de música gratis (YouTube Audio Library, Pixabay, Free Music Archive,
  ccMixter y Musopen) con un botón "Actualizar" que vuelve a leer `MediaStore`.
  La app **no empaqueta ninguna pista**: el usuario descarga el archivo y lo usa
  desde la biblioteca del teléfono, que es lo que permite cada licencia.

## Estructura

- `app/src/main/java/com/vidcam/app/model`: modelo del proyecto y cálculos de timeline.
- `app/src/main/java/com/vidcam/app/data`: persistencia de proyectos (JSON por proyecto).
- `app/src/main/java/com/vidcam/app/capture`: grabación con CameraX.
- `app/src/main/java/com/vidcam/app/media`: importación, biblioteca de música y
  animación de GIF.
- `app/src/main/java/com/vidcam/app/share`: recepción y clasificación de archivos
  compartidos desde otras apps.
- `app/src/main/java/com/vidcam/app/editor`: ViewModel y pantalla del editor.
- `app/src/main/java/com/vidcam/app/export`: exportación con Media3, overlays, guardado y compartir.
- `app/src/main/java/com/vidcam/app/ui`: arranque, permisos, fuentes y tema.
