package com.vidcam.app.model

import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/**
 * Geometría pura de los fondos y de la ventana de vídeo.
 *
 * Vive fuera de Compose y de Media3 para que la vista previa y la exportación
 * compartan exactamente las mismas cuentas y para poder probarla en la JVM.
 */
object VideoWindowMath {

    const val MIN_SCALE = 0.2f
    const val MAX_SCALE = 1f

    /** Duración mínima de un tramo de fondo, en milisegundos. */
    const val MIN_SEGMENT_MS = 1L

    /** Fondo activo en [timeMs], o `null` si ese instante queda negro. */
    fun backgroundAt(backgrounds: List<BackgroundLayer>, timeMs: Long): BackgroundLayer? =
        backgrounds.firstOrNull { timeMs >= it.startMs && timeMs < it.endMs }

    /**
     * Agrega [incoming] recortando los fondos existentes para que no se
     * solapen. El tramo nuevo tiene prioridad; los tramos que quedan con
     * duración cero se descartan. El resultado queda ordenado por inicio.
     */
    fun trimOverlaps(
        backgrounds: List<BackgroundLayer>,
        incoming: BackgroundLayer,
        totalMs: Long,
    ): List<BackgroundLayer> {
        val limit = totalMs.coerceAtLeast(MIN_SEGMENT_MS)
        val start = incoming.startMs.coerceIn(0L, limit - MIN_SEGMENT_MS)
        val end = incoming.endMs.coerceIn(start + MIN_SEGMENT_MS, limit)
        val fixed = incoming.copy(startMs = start, endMs = end)
        val trimmed = backgrounds
            .filterNot { it.id == fixed.id }
            .mapNotNull { existing ->
                when {
                    existing.endMs <= start || existing.startMs >= end -> existing
                    existing.startMs < start ->
                        existing.copy(endMs = start).takeIf { it.endMs > it.startMs }
                    existing.endMs > end ->
                        existing.copy(startMs = end).takeIf { it.endMs > it.startMs }
                    else -> null
                }
            }
        return (trimmed + fixed).sortedBy { it.startMs }
    }

    /** Tramo de la línea de tiempo cubierto por un fondo, o `null` si es un hueco. */
    data class BackgroundSegment(
        val startMs: Long,
        val endMs: Long,
        val background: BackgroundLayer?,
    )

    /**
     * Divide `[0, totalMs)` en tramos consecutivos, marcando con `null` los
     * instantes sin fondo. Es lo que usa la exportación para rellenar los huecos
     * con negro.
     */
    fun backgroundSegments(
        backgrounds: List<BackgroundLayer>,
        totalMs: Long,
    ): List<BackgroundSegment> {
        if (totalMs <= 0L) return emptyList()
        val segments = mutableListOf<BackgroundSegment>()
        var cursor = 0L
        backgrounds
            .sortedBy { it.startMs }
            .forEach { background ->
                val start = background.startMs.coerceIn(0L, totalMs)
                val end = background.endMs.coerceIn(0L, totalMs)
                if (end <= start) return@forEach
                if (start > cursor) {
                    segments += BackgroundSegment(cursor, start, null)
                }
                val from = max(cursor, start)
                if (end > from) {
                    segments += BackgroundSegment(from, end, background)
                    cursor = end
                }
            }
        if (cursor < totalMs) {
            segments += BackgroundSegment(cursor, totalMs, null)
        }
        return segments
    }

    /** Proporción ancho/alto de la ventana para [aspect]. */
    fun aspectRatio(aspect: VideoWindowAspect, originalAspect: Float): Float =
        when (aspect) {
            VideoWindowAspect.ORIGINAL ->
                originalAspect.takeIf { it > 0f && it.isFinite() } ?: (9f / 16f)
            VideoWindowAspect.CUADRADO -> 1f
            VideoWindowAspect.VERTICAL -> 9f / 16f
        }

    /**
     * Tamaño en píxeles de la ventana: el mayor rectángulo con [aspect] que cabe
     * en el lienzo, multiplicado por [scale].
     */
    fun windowSize(
        frameWidth: Float,
        frameHeight: Float,
        aspect: Float,
        scale: Float,
    ): Pair<Float, Float> {
        if (frameWidth <= 0f || frameHeight <= 0f || aspect <= 0f) return 0f to 0f
        val maxWidth: Float
        val maxHeight: Float
        if (frameWidth / frameHeight > aspect) {
            maxHeight = frameHeight
            maxWidth = maxHeight * aspect
        } else {
            maxWidth = frameWidth
            maxHeight = maxWidth / aspect
        }
        val bounded = scale.coerceIn(MIN_SCALE, MAX_SCALE)
        return maxWidth * bounded to maxHeight * bounded
    }

    /**
     * `true` si [pointX], [pointY] cae dentro del rectángulo centrado en
     * [centerX], [centerY], con semiancho [halfWidth], semialto [halfHeight] y
     * girado [rotationDeg] grados en sentido horario.
     */
    fun hitTest(
        centerX: Float,
        centerY: Float,
        halfWidth: Float,
        halfHeight: Float,
        rotationDeg: Float,
        pointX: Float,
        pointY: Float,
    ): Boolean {
        if (halfWidth <= 0f || halfHeight <= 0f) return false
        // Se gira el punto en sentido contrario para comparar contra el
        // rectángulo sin girar.
        val radians = Math.toRadians(-rotationDeg.toDouble())
        val cos = cos(radians).toFloat()
        val sin = sin(radians).toFloat()
        val dx = pointX - centerX
        val dy = pointY - centerY
        val localX = dx * cos - dy * sin
        val localY = dx * sin + dy * cos
        return abs(localX) <= halfWidth && abs(localY) <= halfHeight
    }

    /**
     * Tamaño nativo del fotograma de un clip después de recortarlo a
     * [targetAspect] (recorte *cover* por el centro).
     */
    fun croppedNativeSize(
        width: Int,
        height: Int,
        targetAspect: Float,
    ): Pair<Float, Float> {
        val w = width.coerceAtLeast(1).toFloat()
        val h = height.coerceAtLeast(1).toFloat()
        val aspect = targetAspect.coerceAtLeast(0.01f)
        return if (w / h > aspect) (h * aspect) to h else w to (w / aspect)
    }

    /**
     * Escala que espera el compositor de Media3 para que la ventana ocupe
     * [scale] del mayor tamaño que cabe. [nativeWidth] es el ancho del clip ya
     * recortado a la proporción de la ventana.
     */
    fun compositorScale(
        frameWidth: Float,
        frameHeight: Float,
        nativeWidth: Float,
        nativeHeight: Float,
        aspect: Float,
        scale: Float,
    ): Float {
        if (nativeWidth <= 0f || nativeHeight <= 0f) return scale
        val (width, _) = windowSize(frameWidth, frameHeight, aspect, scale)
        return (width / nativeWidth).coerceIn(0.01f, 20f)
    }
}
