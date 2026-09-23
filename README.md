# VidCam

Aplicación Android para grabar y editar vídeos verticales de hasta 30 segundos:
cámara con audio, importación de clips, biblioteca de música del dispositivo,
capas PNG transparentes y textos con Google Fonts arrastrables, escalables,
rotables y animables con grabación de movimiento en el lienzo, deshacer/rehacer
y proyectos guardados en JSON, con exportación a MP4 (H.264/AAC) con guardado en
galería y hoja de compartir.

El comportamiento y las decisiones técnicas están especificados en OpenSpec, en
`openspec/changes/add-vidcam-mvp/`,
`openspec/changes/add-layer-motion-recording/`,
`openspec/changes/add-project-persistence-and-undo/` y
`openspec/changes/add-clip-speed/`.

## Requisitos

- JDK 17
- Android SDK con `compileSdk 35`
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
- **Música**: el selector lista la música del dispositivo y, debajo, enlaces a
  fuentes de música gratis (YouTube Audio Library, Pixabay, Free Music Archive,
  ccMixter y Musopen) con un botón "Actualizar" que vuelve a leer `MediaStore`.
  La app **no empaqueta ninguna pista**: el usuario descarga el archivo y lo usa
  desde la biblioteca del teléfono, que es lo que permite cada licencia.

## Estructura

- `app/src/main/java/com/vidcam/app/model`: modelo del proyecto y cálculos de timeline.
- `app/src/main/java/com/vidcam/app/data`: persistencia de proyectos (JSON por proyecto).
- `app/src/main/java/com/vidcam/app/capture`: grabación con CameraX.
- `app/src/main/java/com/vidcam/app/media`: importación y biblioteca de música.
- `app/src/main/java/com/vidcam/app/editor`: ViewModel y pantalla del editor.
- `app/src/main/java/com/vidcam/app/export`: exportación con Media3, overlays, guardado y compartir.
- `app/src/main/java/com/vidcam/app/ui`: arranque, permisos, fuentes y tema.
