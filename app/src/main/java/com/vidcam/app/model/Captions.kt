package com.vidcam.app.model

import kotlinx.serialization.Serializable

/**
 * Subtítulo quemado en el vídeo.
 *
 * Los tiempos son absolutos dentro de la línea de tiempo del proyecto, igual
 * que los de las capas, para que la vista previa y el MP4 coincidan sin
 * conversiones. El estilo (tamaño, posición, colores) es global al proyecto
 * ([CaptionStyle]): así un cambio de estilo se ve al instante en todos.
 */
@Serializable
data class Caption(
    val id: String,
    val text: String,
    val startMs: Long = 0L,
    val endMs: Long = 0L,
) {
    val durationMs: Long
        get() = (endMs - startMs).coerceAtLeast(0L)

    /** `true` cuando el subtítulo debe verse en [timeMs]. */
    fun isVisibleAt(timeMs: Long): Boolean =
        durationMs > 0L && timeMs >= startMs && timeMs < endMs
}

/**
 * Aspecto de todos los subtítulos del proyecto.
 *
 * [sizeFraction] y [yFraction] son fracciones del alto del fotograma, de modo
 * que la vista previa y la exportación calculan los mismos píxeles sin depender
 * de la densidad de pantalla.
 */
@Serializable
data class CaptionStyle(
    /** Alto de la letra como fracción del alto del vídeo (0,042 = 4,2 %). */
    val sizeFraction: Float = 0.042f,
    /** Centro vertical del bloque de texto (0 = arriba, 1 = abajo). */
    val yFraction: Float = 0.82f,
    val colorArgb: Int = 0xFFFFFFFF.toInt(),
    /** Fondo del bloque; 0 = sin fondo. */
    val backgroundArgb: Int = 0x99000000.toInt(),
    /** Mayúsculas forzadas, el estilo habitual en vídeo vertical. */
    val uppercase: Boolean = true,
) {
    companion object {
        const val MIN_SIZE_FRACTION = 0.02f
        const val MAX_SIZE_FRACTION = 0.09f
        const val MIN_Y_FRACTION = 0.1f
        const val MAX_Y_FRACTION = 0.95f
    }

    fun bounded(): CaptionStyle = copy(
        sizeFraction = sizeFraction.coerceIn(MIN_SIZE_FRACTION, MAX_SIZE_FRACTION),
        yFraction = yFraction.coerceIn(MIN_Y_FRACTION, MAX_Y_FRACTION),
    )
}

/**
 * Utilidades de subtítulos sin dependencias de Android, para probarlas con
 * tests unitarios.
 */
object CaptionMath {

    /** Subtítulo que se ve en [timeMs], o `null`. */
    fun activeAt(captions: List<Caption>, timeMs: Long): Caption? =
        captions.firstOrNull { it.isVisibleAt(timeMs) }

    /** Texto tal como se dibuja: mayúsculas y sin espacios sobrantes. */
    fun displayText(caption: Caption, style: CaptionStyle): String {
        val trimmed = caption.text.trim()
        return if (style.uppercase) trimmed.uppercase() else trimmed
    }

    /**
     * Ordena y recorta una lista de subtítulos: descarta los vacíos, fuerza
     * duración positiva y evita solapes moviendo el final del anterior.
     */
    fun normalize(captions: List<Caption>, totalMs: Long): List<Caption> {
        val ordered = captions
            .filter { it.text.isNotBlank() && it.endMs > it.startMs }
            .sortedBy { it.startMs }
        val result = mutableListOf<Caption>()
        for (caption in ordered) {
            val start = caption.startMs.coerceIn(0L, totalMs.coerceAtLeast(0L))
            val end = caption.endMs.coerceIn(start, totalMs.coerceAtLeast(start))
            if (end <= start) continue
            val previous = result.lastOrNull()
            if (previous != null && start < previous.endMs) {
                result[result.lastIndex] = previous.copy(endMs = start)
            }
            result += caption.copy(startMs = start, endMs = end)
        }
        return result
    }
}
