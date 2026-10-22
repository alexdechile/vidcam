## 1. Modelo y geometría

- [ ] 1.1 Agregar `BackgroundLayer`, `VideoWindowAspect` y `VideoWindow` serializables en `model/Project.kt`, más `Project.backgrounds = emptyList()` y `Project.videoWindow = VideoWindow()`
- [ ] 1.2 Incluir fondos y ventana en `Project.isEmpty` y en la comparación que usa el historial de deshacer
- [ ] 1.3 Crear `model/VideoWindowMath.kt` con `backgroundAt`, `normalizeBackgrounds` (recorte de solapamientos), `windowSize`, `windowHitTest` y `composerScale`, sin dependencias de Android
- [ ] 1.4 Escribir tests JVM en `app/src/test/.../model/` para tramos, solapamientos, tamaño de ventana, hit-test rotado y valores por defecto
- [ ] 1.5 Verificar que un JSON de proyecto anterior deserializa sin fondos y con la ventana por defecto

## 2. ViewModel y persistencia

- [ ] 2.1 Agregar `addBackground(uri, startMs, endMs)` en `EditorViewModel` copiando la imagen con `MediaProbe.copyToStorage`
- [ ] 2.2 Agregar `updateBackground` y `removeBackground` con claves de deshacer `background-add/edit/remove:<id>`
- [ ] 2.3 Agregar `updateVideoWindow` con clave de deshacer `video-window`
- [ ] 2.4 Asegurar que guardar/abrir proyecto conserva fondos y ventana (test de `ProjectStore`)

## 3. Vista previa

- [ ] 3.1 Dibujar el fondo activo en `TimelinePreview` como `Image` con `ContentScale.Crop` debajo del vídeo, siguiendo `backgroundAt`
- [ ] 3.2 Reemplazar el `PlayerView` a pantalla completa por una ventana con `Box` de la proporción elegida, `RESIZE_MODE_ZOOM` y `graphicsLayer` para posición, escala y giro
- [ ] 3.3 Extender la superficie de gestos: prioridad capa → ventana → deseleccionar, con arrastre, pellizco y rotación de la ventana
- [ ] 3.4 Mostrar borde guía en la ventana seleccionada y respetar el modo de grabación de movimiento de capas

## 4. Interfaz del editor

- [ ] 4.1 Agregar botón **Fondo** con selector de imagen y diálogo de inicio/fin (por defecto `0..duración total`)
- [ ] 4.2 Agregar sección **Fondos** con miniatura, tramo, editar y eliminar
- [ ] 4.3 Agregar chips **Original / 1:1 / 9:16** y botón **Centrar** para la ventana
- [ ] 4.4 Validar entradas: duración mayor que cero, sin solapamiento, imagen ilegible con mensaje claro

## 5. Exportación

- [ ] 5.1 Crear `export/BackgroundBitmaps.kt` que decodifique (con orientación EXIF y muestreo), recorte en *cover* y escriba PNG temporales del tamaño exacto del fotograma
- [ ] 5.2 Generar un PNG negro temporal para los huecos sin fondo
- [ ] 5.3 Agregar la secuencia de fondos al `Composition` y configurar `VideoCompositorSettings` con el tamaño del fotograma y las transformaciones por entrada
- [ ] 5.4 Ordenar las secuencias con los clips primero (capa superior) y los fondos después (capa inferior), según el orden de dibujo del compositor
- [ ] 5.5 Aplicar `Crop` a los clips cuando la ventana es 1:1 o 9:16, junto a los efectos existentes de filtro, lente, temblor y velocidad
- [ ] 5.6 Mantener el camino de exportación actual cuando no hay fondos y la ventana está por defecto
- [ ] 5.7 Limpiar los PNG temporales al terminar, fallar o cancelar la exportación, y reportar el error con mensaje explícito

## 6. Verificación

- [ ] 6.1 Compilar y correr tests en GitHub Actions (`lint`, `test`, `assembleDebug`, `assembleRelease`) y corregir lo que falle
- [ ] 6.2 Probar en dispositivo: fondo único, dos fondos consecutivos, hueco negro, ventana 1:1 movida y girada, y exportación equivalente sin fondos
- [ ] 6.3 Actualizar `README.md` con la nueva función y las capacidades de OpenSpec
