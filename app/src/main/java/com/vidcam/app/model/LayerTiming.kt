package com.vidcam.app.model

/**
 * Reglas puras de la ventana temporal de una capa: desde qué segundo aparece
 * ([OverlayLayer.startMs]) y cuánto permanece en pantalla
 * ([OverlayLayer.endMs]).
 *
 * Vive aparte de la UI y del `EditorViewModel` porque la lista de capas, el
 * diálogo de edición y la vista previa deben aplicar exactamente los mismos
 * límites; al no depender de Android se puede probar con tests de JVM, igual
 * que [TimelineMath] o [VideoWindowMath].
 */
object LayerTiming {

    /**
     * Ventana mínima que se puede asignar a una capa. Evita ventanas de duración
     * cero, en las que la capa nunca se llegaría a dibujar.
     */
    const val MIN_DURATION_MS: Long = 300L

    /**
     * Duración de referencia del proyecto.
     *
     * Sin clips todavía se usa el tope del proyecto ([MAX_DURATION_MS]) para que
     * el slider de una capa recién añadida siga teniendo un rango útil.
     */
    fun timelineMs(projectTotalMs: Long): Long =
        (if (projectTotalMs > 0L) projectTotalMs else MAX_DURATION_MS)
            .coerceAtLeast(MIN_DURATION_MS)

    /**
     * Segundos que la capa está en pantalla, acotados a lo que queda de proyecto
     * desde su inicio.
     */
    fun durationMs(layer: OverlayLayer, projectTotalMs: Long): Long {
        val total = timelineMs(projectTotalMs)
        // El inicio se acota igual que en [withDuration] para que el valor que
        // se muestra sea el que se escribiría al soltar el slider.
        val start = layer.startMs.coerceIn(0L, (total - MIN_DURATION_MS).coerceAtLeast(0L))
        val maxDuration = (total - start).coerceAtLeast(MIN_DURATION_MS)
        return (layer.endMs - layer.startMs).coerceIn(MIN_DURATION_MS, maxDuration)
    }

    /**
     * Devuelve la capa con una duración en pantalla de [durationMs], conservando
     * su inicio. El inicio se acota para que quepa al menos la duración mínima.
     */
    fun withDuration(
        layer: OverlayLayer,
        durationMs: Long,
        projectTotalMs: Long,
    ): OverlayLayer {
        val total = timelineMs(projectTotalMs)
        val start = layer.startMs.coerceIn(0L, (total - MIN_DURATION_MS).coerceAtLeast(0L))
        val maxDuration = (total - start).coerceAtLeast(MIN_DURATION_MS)
        val duration = durationMs.coerceIn(MIN_DURATION_MS, maxDuration)
        return layer.copy(startMs = start, endMs = start + duration)
    }

    /**
     * Devuelve la capa empezando en [startMs], conservando su duración en
     * pantalla (mover el inicio no debe estirar ni encoger la capa).
     */
    fun withStart(
        layer: OverlayLayer,
        startMs: Long,
        projectTotalMs: Long,
    ): OverlayLayer {
        val total = timelineMs(projectTotalMs)
        val duration = durationMs(layer, total)
        val start = startMs.coerceIn(0L, (total - duration).coerceAtLeast(0L))
        return layer.copy(startMs = start, endMs = start + duration)
    }

    /** `true` si la capa se dibuja en [positionMs]; los bordes cuentan. */
    fun isVisibleAt(layer: OverlayLayer, positionMs: Long): Boolean =
        positionMs in layer.startMs..layer.endMs

    /**
     * Sanea una capa que quedó fuera del proyecto (por ejemplo tras acortar o
     * quitar clips), conservando su duración en lo posible.
     */
    fun fitToTimeline(layer: OverlayLayer, projectTotalMs: Long): OverlayLayer =
        withDuration(layer, layer.endMs - layer.startMs, projectTotalMs)
}
