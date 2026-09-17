## Context

Proyecto Android nuevo y vacío. Ver `proposal.md` para la motivación. Restricciones que condicionan el diseño:

- Todo el procesamiento debe ser local y funcionar sin red (salvo la descarga de fuentes).
- El formato de salida es vertical 9:16 y la duración total está acotada a 30 segundos.
- No se permiten dependencias nativas empaquetadas (sin FFmpeg) para mantener el APK pequeño y el build simple.
- El build debe ser reproducible en CI y no depender de archivos locales.

## Goals / Non-Goals

**Goals:**
- Una arquitectura mínima y mantenible: un solo módulo `:app` con UI Compose y un modelo de proyecto (timeline) serializable.
- Reutilizar las APIs oficiales de Android para captura, reproducción, composición y exportación.
- Permitir que la vista previa y la exportación consuman el mismo modelo de composición.
- Build y release automatizados vía GitHub Actions sin secretos locales.

**Non-Goals:**
- Edición multipista avanzada, transiciones, efectos, filtros o keyframes.
- Publicación automática directa a TikTok/Instagram más allá de la hoja de compartir.
- Colaboración, cuentas, backend o sincronización en la nube.
- Soporte tablet o pantallas horizontales en la v1.

## Decisions

### D1. Stack de UI y estructura
Kotlin + Jetpack Compose + Material 3, un módulo `:app`, patrón MVVM ligero con estado de UI por `ViewModel`. Sin inyección de dependencias al inicio (se instancian repositorios directamente) para reducir andamiaje.
- **Alternativa descartada:** XML Views (más código y menos expresivo para overlays), multi-módulo (sobrecarga innecesaria para el tamaño del proyecto).

### D2. Captura de vídeo
CameraX (`camera-video` + `camera-view`) con `PreviewView` y `VideoCapture`, límite de duración configurable y alternancia frontal/trasera. La grabación escribe a un archivo temporal en `cacheDir`.
- **Alternativa descartada:** Camera2 (demasiado bajo nivel), `ACTION_VIDEO_CAPTURE` (sin control real del límite de 30 s ni del formato).

### D3. Modelo de composición (timeline)
El proyecto se representa con un modelo serializable: clips de vídeo con `trimStart`/`trimEnd`, una pista de música opcional (`uri`, `offset`, `gain`) y una lista de capas (`PngOverlay` y `TextOverlay`) con posición normalizada (0..1), escala, rotación y ventana temporal. Este modelo es la única fuente de verdad para vista previa y exportación.
- **Rationale:** separar la edición de la materialización permite recorte no destructivo y exportación reproducible.

### D4. Exportación y composición
Media3 **Transformer** con la **Effects API** (OpenGL). Los recortes se aplican con `MediaItem.ClippingConfiguration`; las capas (PNG con alfa y textos, estos últimos rasterizados a un `Bitmap`) se dibujan con un `BitmapOverlay`. La mezcla de audio se resuelve con la composición de secuencias (`EditedMediaItemSequence` y `EditedMediaItem.setRemoveAudio`), ya que Media3 1.5.1 no expone un mezclador independiente. Salida MP4 H.264 + AAC 9:16, con resolución objetivo configurable (por defecto 1080×1920, con fallback a la resolución del origen).
- **Alternativas descartadas:** FFmpeg (binario nativo grande y licencias), MP4Parser (sin composición GL de overlays), OpenGL/MediaCodec propio (complejidad y mantenimiento altos).

### D5. Audio
Se usan las pistas de audio de los clips como audio base y una pista de música opcional de `MediaStore.Audio`. `setRemoveAudio` silencia el audio base y la música se añade en una segunda secuencia de la `Composition`, de modo que Transformer mezcla ambas pistas al exportar. Si la música es más larga que el timeline se recorta al segmento elegido; si es más corta, se repite en bucle (`EditedMediaItemSequence.setIsLooping`) hasta cubrir el timeline con un fundido de salida.
- **Alternativa descartada:** `AudioMixerSettings` (no existe en Media3 1.5.1; se resuelve con secuencias de la `Composition`).

### D6. Fuentes de Google
`androidx.compose.ui:ui-text-google-fonts` con el proveedor de fuentes descargables. Las fuentes se descargan bajo demanda y quedan en caché del proveedor. Se usa `GoogleFont(name, bestEffort = true)` para que una fuente no disponible (sin red y sin caché) caiga silenciosamente en la fuente local, y una comprobación de conectividad (`ConnectivityManager`) decide si mostrar el aviso de "se descargará con conexión". No se usa `isAvailableOnDevice` porque es API interna de la librería. La exportación usa la misma fuente ya resuelta.

### D7. Capas PNG
Conjunto integrado pequeño de PNG con alfa en `assets/` más selección desde la galería mediante el selector de medios del sistema. En vista previa se dibujan con Compose; en exportación con `BitmapOverlay`.
- **Rationale:** un set integrado garantiza la demo sin depender de archivos del usuario.

### D8. Permisos y primer arranque
Un único flujo de `RequestMultiplePermissions` al primer arranque para `CAMERA`, `RECORD_AUDIO` y permisos de medios (con variantes por versión), persistido en preferencias. Los permisos se agrupan en `recording` (cámara + micrófono) y `media` (lectura). La importación de vídeos/imágenes usa el selector de medios del sistema para no requerir permiso de lectura global cuando está disponible. Si los permisos de medios están concedidos pero faltan los de grabación, la app entra igualmente al editor en **modo solo importación**: se oculta el control de grabación, la vista previa se reemplaza por un aviso y se ofrece solicitar el permiso o abrir los ajustes; al volver a primer plano se reevalúan los permisos (`LifecycleEventEffect`).
- **Alternativa descartada:** pedir permisos de forma perezosa (peor experiencia, solicitado explícitamente "una vez al arrancar").

### D9. Vista previa aproximada, exportación autoritativa
La vista previa reproduce el vídeo base con ExoPlayer y dibuja las capas encima con Compose usando las mismas coordenadas normalizadas del modelo. La composición final (píxel a píxel, alfa, audio) la produce Transformer al exportar.
- **Trade-off aceptado:** puede haber diferencias menores de renderizado que se mitigan compartiendo el modelo y las mismas métricas de layout.

### D10. CI/CD con GitHub Actions
- `ci.yml`: en `push`/`pull_request` corre `./gradlew lint test assembleDebug assembleDebugAndroidTest assembleRelease` con `gradle/actions/setup-gradle` y JDK 17, y sube el APK de depuración, el APK de release (firmado con la clave de depuración al no haber secrets) y el reporte de lint como artefactos.
- `release.yml`: en tags `v*` o ejecución manual valida que exista `KEYSTORE_BASE64` (falla con mensaje explícito si falta), decodifica el keystore, firma `assembleRelease`/`bundleRelease` y publica APK/AAB como artefacto y GitHub Release. Requiere el permiso `contents: write` del `GITHUB_TOKEN`.
- La configuración de firma lee el keystore desde variables de entorno; si no existen, los builds de depuración y el release de CI siguen funcionando con la clave de depuración.
- **Alternativa descartada:** servicios de CI propietarios (el requisito pide GitHub Actions).

### D11. Tamaño del APK y minificación
El APK de release se compila con R8 (`isMinifyEnabled`) y reducción de recursos (`isShrinkResources`) para bajar el peso (~15 MB sin minificar → ~3 MB). Se confía en las reglas de consumer de Media3, CameraX y Compose en lugar de un `-keep` global, y CI compila el release en cada push para detectar problemas de ofuscación.
- **Rationale:** el grueso del peso son las clases dex; los splits por ABI no aportan porque las librerías nativas suman menos de 300 KB.
- **Trade-off aceptado:** CI no ejecuta el APK, así que una regla de keep faltante se detectaría en la prueba manual (tarea 10.4).

## Risks / Trade-offs

- **[Render de overlays costoso en exportación]** → limitar el número de capas visibles simultáneas, usar el encoder por hardware y resolución objetivo acotada.
- **[APIs experimentales de Media3 Transformer]** → fijar la versión de Media3 (1.5.1), aislar overlays y pipeline de audio en clases propias (`Overlays`, `VideoExporter`) y anotar los usos con `@OptIn(UnstableApi::class)`.
- **[R8 elimina clases usadas por reflexión]** → confiar en las reglas de consumer de las librerías y compilar el release en CI; la verificación final es la prueba en dispositivo (tarea 10.4).
- **[Diferencias entre vista previa y exportación]** → modelo único con coordenadas normalizadas y verificación de un caso de prueba por tipo de capa.
- **[Fuente de Google sin conexión]** → fallback a fuente local y aviso al usuario; precarga opcional de las fuentes favoritas.
- **[Duración/memoria en dispositivos gama baja]** → grabar y exportar por streaming a `cacheDir`, evitar mantener el vídeo completo en memoria y liberar recursos al salir.
- **[Permisos denegados]** → degradar con gracia: sin cámara permitir solo importación; sin micrófono grabar en silencio; mensajes claros en español.
- **[Secretos de firma mal configurados]** → el workflow de release falla con mensaje explícito y nunca publica un artefacto sin firmar.

## Migration Plan

Proyecto nuevo: no hay migración de datos. Orden de entrega incremental: andamiaje + CI → permisos y shell → captura/importación/música → edición (recorte/audio/capas) → exportación/compartir → release firmado. Rollback: al ser un APK nuevo, basta con no publicar el tag de release.

## Open Questions

- Resolución de exportación por defecto cuando el origen es menor que 1080×1920 (se puede decidir por dispositivo sin afectar el diseño).
- Tamaño y contenido finales del set integrado de PNG (ampliable sin cambios de arquitectura).
