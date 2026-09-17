## 1. Andamiaje del proyecto y CI

- [x] 1.1 Crear el proyecto Gradle Kotlin DSL con el módulo `:app`, AGP 8.x y toolchain JDK 17; fijar `minSdk 26` y `targetSdk` vigente (D1, D10)
- [x] 1.2 Configurar `applicationId`, versión, nombre e ícono de la app, y habilitar Compose con Material 3
- [x] 1.3 Agregar dependencias: CameraX, Media3 (transformer, extractor, muxer, common, exoplayer), Compose Material 3, `ui-text-google-fonts` y corrutinas (D2, D4, D6)
- [x] 1.4 Declarar permisos `CAMERA`, `RECORD_AUDIO`, `READ_MEDIA_VIDEO`, `READ_MEDIA_AUDIO`, `READ_MEDIA_IMAGES` y `READ_EXTERNAL_STORAGE` con `maxSdkVersion` para API < 33 (D8)
- [x] 1.5 Crear `.github/workflows/ci.yml` con checkout, `setup-java` temurin 17, `gradle/actions/setup-gradle`, `./gradlew lint test assembleDebug` y subida del APK de depuración como artefacto (D10)
- [x] 1.6 Crear `.github/workflows/release.yml` disparado por tag `v*` y por ejecución manual: decodificar el keystore desde `KEYSTORE_BASE64`, ejecutar `assembleRelease`/`bundleRelease` y publicar APK/AAB como artefacto y GitHub Release (D10)
- [x] 1.7 Configurar la firma de release a partir de variables de entorno (`KEYSTORE_FILE`, `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD`) con degradación a firma de depuración cuando no existan (D10)
- [x] 1.8 Verificar que el build de depuración compila localmente sin secretos y que el workflow de CI pasa en el primer push

## 2. Permisos y shell de arranque

- [x] 2.1 Implementar la pantalla de arranque minimalista en Compose con estado de permisos y una acción principal (spec `app-shell-permissions`)
- [x] 2.2 Implementar el flujo único de `RequestMultiplePermissions` para cámara, micrófono y medios al primer arranque, persistiendo que ya se solicitó (D8)
- [x] 2.3 Manejar denegación con reintento y denegación permanente con enlace a los ajustes del sistema
- [x] 2.4 Crear la pantalla única del editor como contenedor Compose (vista previa, timeline, controles y lista de capas) (D1, D9)

## 3. Captura e importación

- [x] 3.1 Integrar CameraX con `PreviewView` vertical 9:16 y alternancia de cámara frontal/trasera (D2)
- [x] 3.2 Implementar la grabación con límite de 30 segundos y detención automática, guardando en un archivo temporal de `cacheDir` (D2)
- [x] 3.3 Agregar el clip grabado al timeline al finalizar la grabación (D3)
- [x] 3.4 Implementar la importación de vídeos con el selector de medios del sistema y agregarlos como clips (D8)
- [x] 3.5 Recortar a 30 segundos los clips importados mayores al límite y avisar al usuario
- [ ] 3.6 Manejar el estado sin permisos habilitando solo la importación cuando falte la cámara

## 4. Biblioteca de música

- [x] 4.1 Consultar `MediaStore.Audio.Media` y construir la lista de pistas con título, artista y duración
- [x] 4.2 Implementar la interfaz de selección de música en Compose, con estado vacío cuando no haya pistas
- [x] 4.3 Asignar la pista elegida al modelo del proyecto como audio de música (D5)

## 5. Modelo de timeline y recorte

- [x] 5.1 Definir el modelo serializable del proyecto: clips con `trimStart`/`trimEnd`, pista de música y lista de capas normalizadas (D3)
- [x] 5.2 Implementar los cálculos de duración total, posición de clips y validación del límite de 30 segundos
- [x] 5.3 Implementar la línea de tiempo en Compose con controles de inicio y fin por clip con precisión de 0,1 s
- [x] 5.4 Conectar la vista previa con ExoPlayer al segmento recortado del clip activo (D9)
- [x] 5.5 Bloquear la exportación y avisar cuando la duración total supere los 30 segundos

## 6. Edición de audio

- [x] 6.1 Implementar el conmutador de silencio del audio original en el modelo y la vista previa (D5)
- [x] 6.2 Implementar la reproducción de la música alineada al inicio del proyecto en la vista previa
- [x] 6.3 Permitir elegir el punto de inicio de la música y recortarla cuando exceda el timeline
- [x] 6.4 Configurar la mezcla de audio en Transformer con secuencias de `Composition` (`setRemoveAudio` para silenciar y `setIsLooping` para la música en bucle); Media3 1.5.1 no expone `AudioMixerSettings` (D4, D5)

## 7. Capas PNG transparentes

- [x] 7.1 Agregar el conjunto integrado de PNG con alfa en `assets/` y la selección desde la galería (D7)
- [x] 7.2 Implementar el modelo y los gestos de mover, escalar y rotar capas PNG en la vista previa (D3, D7)
- [x] 7.3 Configurar la duración de cada capa PNG y reflejarla en la vista previa
- [x] 7.4 Aplicar las capas PNG en la exportación con `BitmapOverlay` respetando el canal alfa (D4, D7)

## 8. Textos con Google Fonts

- [x] 8.1 Implementar el modelo de texto con contenido, tamaño, color y posición, y su edición en Compose
- [x] 8.2 Integrar el proveedor de fuentes descargables de Google Fonts con caché y selector de fuentes (D6)
- [x] 8.3 Implementar el fallback a fuente local y el aviso cuando una fuente no esté disponible sin conexión
- [x] 8.4 Aplicar los textos con su fuente en la exportación rasterizando el texto a un `BitmapOverlay` (Media3 1.5.1 no aplica Google Fonts descargables en `TextOverlay`) (D4, D6)

## 9. Exportación, guardado y compartir

- [x] 9.1 Construir el pipeline de exportación con Media3 Transformer: recortes, overlays y audio a MP4 H.264/AAC 9:16 en resolución objetivo (D4)
- [x] 9.2 Implementar la barra de progreso, la cancelación y el manejo de errores en español (spec `export-and-share`)
- [x] 9.3 Guardar el MP4 resultante en la galería mediante `MediaStore.Video` y exponerlo con `FileProvider`
- [x] 9.4 Implementar la hoja de compartir del sistema con `ACTION_SEND` y tipo `video/mp4`
- [x] 9.5 Evitar dejar archivos parciales en la galería cuando la exportación se cancela o falla

## 10. Calidad, pruebas y release

- [x] 10.1 Escribir pruebas unitarias de los cálculos de recorte, duración total y validación de límites (D3)
- [x] 10.2 Escribir pruebas unitarias de la selección y el recorte de la pista de música (D5)
- [ ] 10.3 Agregar pruebas de UI de Compose para la pantalla de permisos y la pantalla del editor
- [ ] 10.4 Probar en dispositivos o emuladores API 26, 30 y 34+ el flujo completo: grabar, importar, música, capas, textos, exportar y compartir
- [x] 10.5 Documentar en el README los secrets de firma y el proceso de release, y etiquetar la primera versión `v1.0.0` para validar el workflow de release
