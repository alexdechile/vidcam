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
./gradlew lint test assembleDebug
```

El APK de depuración se genera en `app/build/outputs/apk/debug/`.

## Integración continua

`.github/workflows/ci.yml` ejecuta `lint test assembleDebug` en cada push a `main`
y sube el APK de depuración y el reporte de lint como artefactos.

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

Para generar el valor de `KEYSTORE_BASE64`:

```bash
base64 -w 0 release.jks
```

Si los secrets no están definidos, el build de release degrada a la firma de
depuración y sigue siendo válido para pruebas internas.

Para publicar una versión:

```bash
git tag v1.0.0
git push origin v1.0.0
```

## Estructura

- `app/src/main/java/com/vidcam/app/model`: modelo del proyecto y cálculos de timeline.
- `app/src/main/java/com/vidcam/app/capture`: grabación con CameraX.
- `app/src/main/java/com/vidcam/app/media`: importación y biblioteca de música.
- `app/src/main/java/com/vidcam/app/editor`: ViewModel y pantalla del editor.
- `app/src/main/java/com/vidcam/app/export`: exportación con Media3, overlays, guardado y compartir.
- `app/src/main/java/com/vidcam/app/ui`: arranque, permisos, fuentes y tema.
