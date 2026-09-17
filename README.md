# VidCam

Aplicación Android para grabar y editar vídeos verticales de hasta 30 segundos:
cámara con audio, importación de clips, biblioteca de música del dispositivo,
capas PNG transparentes, textos con Google Fonts y exportación a MP4 (H.264/AAC)
con guardado en galería y hoja de compartir.

El comportamiento y las decisiones técnicas están especificados en OpenSpec, en
`openspec/changes/add-vidcam-mvp/`.

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

## Estructura

- `app/src/main/java/com/vidcam/app/model`: modelo del proyecto y cálculos de timeline.
- `app/src/main/java/com/vidcam/app/capture`: grabación con CameraX.
- `app/src/main/java/com/vidcam/app/media`: importación y biblioteca de música.
- `app/src/main/java/com/vidcam/app/editor`: ViewModel y pantalla del editor.
- `app/src/main/java/com/vidcam/app/export`: exportación con Media3, overlays, guardado y compartir.
- `app/src/main/java/com/vidcam/app/ui`: arranque, permisos, fuentes y tema.
