## 1. Modelo de animación

- [x] 1.1 Añadir `LayerKeyframe(timeMs, x, y, scale, rotationDeg)` y el campo `keyframes: List<LayerKeyframe> = emptyList()` en `OverlayLayer` (`model/Project.kt`), manteniendo los campos estáticos actuales
- [x] 1.2 Implementar en un archivo nuevo (`model/LayerAnimation.kt`) la resolución `OverlayLayer.resolvedAt(timeMs)` con interpolación lineal de `x`, `y`, `scale` y camino angular más corto para `rotationDeg`, manteniendo extremos antes/después de la primera y última clave
- [x] 1.3 Implementar en la misma capa de modelo las operaciones puras de edición de claves: sembrar clave de estado actual, insertar/reemplazar clave en un tiempo (con orden por tiempo) y eliminar clave
- [x] 1.4 Escribir tests unitarios en `app/src/test/.../model/LayerAnimationTest.kt`: interpolación entre dos claves, capa sin claves (estática), extremos, rotación con salto 359°→1°, inserción ordenada y reemplazo en el mismo tiempo

## 2. Gestos fiables en el lienzo (arreglo del pellizco)

- [x] 2.1 Extraer a una función pura (sin dependencias de Android) la resolución de la capa bajo un punto: *hit-test* en orden inverso de dibujo sobre la caja envolvente escalada y rotada, devolviendo la capa de arriba o `null`
- [x] 2.2 Escribir tests unitarios del *hit-test*: toque dentro/fuera, solapamiento (gana la de arriba), capa escalada y capa rotada
- [x] 2.3 Sustituir el `pointerInput` de cada `LayerBox` (`editor/EditorScreen.kt`) por una única superficie de gestos en el `Box` del lienzo que resuelva la capa objetivo al primer `down`
- [x] 2.4 Implementar en esa superficie el enrutado de 1 puntero (arrastre), 2 punteros (escala por ratio de distancias y rotación por delta de ángulo) y toque sin desplazamiento (selección), consumiendo los eventos y respetando el `touchSlop`
- [x] 2.5 Verificar que los gestos siguen respetando el encuadre (mismas coordenadas normalizadas que la exportación) y que la capa seleccionada se resalta

## 3. Vista previa animada

- [x] 3.1 Cambiar el bucle de posición de `TimelinePreview` de `delay(100)` a un muestreo por fotograma con `withFrameNanos` mientras el reproductor está en marcha, conservando el muestreo de respaldo en pausa
- [x] 3.2 Aplicar `layer.resolvedAt(positionMs)` a cada capa visible al dibujarla, de modo que el transform animado se refleje en la vista previa
- [x] 3.3 Ajustar `LayerBox` para recibir la capa ya resuelta sin cambiar el *hit-test* ni el tamaño base (misma fracción del ancho del lienzo que la exportación)

## 4. Modo de grabación de movimiento

- [x] 4.1 Añadir el estado de modo (`motionMode`/`motionRecording`) y el botón "Grabar movimiento" en los controles del editor, con indicador visible de grabación activa
- [x] 4.2 Ignorar gestos y auto-detener la grabación al salir del modo o al terminar la pasada del vídeo para evitar claves desordenadas por el `REPEAT_MODE_ALL`
- [x] 4.3 Muestrear los gestos en tiempo de grabación como claves en el tiempo global del cabezal, sembrando una clave inicial con el transform actual y decimando por umbral de movimiento y de tiempo (~40 ms)
- [x] 4.4 Añadir las acciones de borrar toda la animación de una capa y borrar una clave concreta en `EditorViewModel`
- [x] 4.5 Dibujar marcas de fotogramas clave en la línea de tiempo y permitir borrar una marca; al ajustar posición/escala/rotación de una capa ya animada, actualizar la clave del tiempo actual sin destruir el resto
- [x] 4.6 Escribir tests unitarios del muestreo: siembra de clave inicial, decimación, orden temporal y descarte de muestras anteriores a la última clave

## 5. Exportación fiel del movimiento

- [x] 5.1 Refactorizar `LayerBitmapOverlay`/`LayerBitmaps` (`export/Overlays.kt`) para que el bitmap sea el base y la escala se derive del transform resuelto en `getOverlaySettings(presentationTimeUs)`
- [x] 5.2 Resolver `layer.resolvedAt(presentationTimeUs / 1000)` en `getOverlaySettings` para anclaje, rotación y escala, manteniendo la visibilidad por ventana temporal
- [x] 5.3 Comprobar que una capa sin claves exporta exactamente igual que antes (misma escala, anclaje y rotación) y que el texto animado rasteriza a tamaño base y escala por fotograma

## 6. Verificación y documentación

- [x] 6.1 Extender los tests instrumentados (`EditorScreenTest`) para cubrir el modo de grabación y la reproducción animada sin cámara
- [x] 6.2 Ejecutar la verificación en CI (`lint test assembleDebug assembleDebugAndroidTest`) y corregir lo que falle
- [x] 6.3 Actualizar `README.md` (sección de comportamiento) con el pellizco corregido y la grabación de movimiento, y sincronizar las specs OpenSpec al archivar
