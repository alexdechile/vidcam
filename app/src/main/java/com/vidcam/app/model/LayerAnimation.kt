package com.vidcam.app.model

import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * Operaciones puras de animación y de *hit-test* de capas, sin dependencias de
 * Android, para poder verificarlas con pruebas unitarias de JVM.
 *
 * La interpolación es lineal para posición y escala, y por el camino angular
 * más corto para la rotación.
 */

/** Intervalo mínimo entre fotogramas clave grabados. */
const val MOTION_MIN_INTERVAL_MS = 40L

/** Movimiento normalizado mínimo para forzar un fotograma clave dentro del intervalo. */
const val MOTION_MIN_MOVE = 0.005f

/** Cambio de escala mínimo para forzar un fotograma clave dentro del intervalo. */
const val MOTION_MIN_SCALE_DELTA = 0.02f

/** Cambio de rotación mínimo (grados) para forzar un fotograma clave dentro del intervalo. */
const val MOTION_MIN_ROTATION_DEG = 1f

/**
 * Transform de la capa resuelto en [timeMs] según sus fotogramas clave.
 *
 * Se apoya en el invariante de que [OverlayLayer.keyframes] está ordenada por
 * tiempo (lo garantizan [upsertKeyframe] y [seedKeyframesAt]) para no ordenar en
 * cada fotograma de la vista previa o de la exportación.
 */
fun OverlayLayer.resolvedAt(timeMs: Long): OverlayLayer {
    if (keyframes.isEmpty()) return this
    val first = keyframes.first()
    if (timeMs <= first.timeMs) return withTransform(first)
    val last = keyframes.last()
    if (timeMs >= last.timeMs) return withTransform(last)

    var index = 0
    while (index < keyframes.size - 2 && keyframes[index + 1].timeMs <= timeMs) {
        index++
    }
    val from = keyframes[index]
    val to = keyframes[index + 1]
    val span = (to.timeMs - from.timeMs).coerceAtLeast(1L)
    val fraction = (timeMs - from.timeMs).toFloat() / span.toFloat()
    return copy(
        x = lerp(from.x, to.x, fraction),
        y = lerp(from.y, to.y, fraction),
        scale = lerp(from.scale, to.scale, fraction),
        rotationDeg = lerpAngle(from.rotationDeg, to.rotationDeg, fraction),
    )
}

private fun OverlayLayer.withTransform(keyframe: LayerKeyframe): OverlayLayer = copy(
    x = keyframe.x,
    y = keyframe.y,
    scale = keyframe.scale,
    rotationDeg = keyframe.rotationDeg,
)

private fun lerp(from: Float, to: Float, fraction: Float): Float =
    from + (to - from) * fraction

/** Interpola ángulos por el camino más corto (evita el salto 359°→1°). */
fun lerpAngle(from: Float, to: Float, fraction: Float): Float {
    var delta = (to - from) % 360f
    if (delta > 180f) delta -= 360f
    if (delta < -180f) delta += 360f
    return from + delta * fraction
}

/** Inserta o reemplaza el fotograma clave en su tiempo, manteniendo el orden. */
fun OverlayLayer.upsertKeyframe(keyframe: LayerKeyframe): OverlayLayer {
    val existing = keyframes.toMutableList()
    val index = existing.indexOfFirst { it.timeMs == keyframe.timeMs }
    if (index >= 0) {
        existing[index] = keyframe
    } else {
        existing.add(keyframe)
    }
    return copy(keyframes = existing.sortedBy { it.timeMs })
}

/** Elimina el fotograma clave exactamente en [timeMs]. */
fun OverlayLayer.removeKeyframeAt(timeMs: Long): OverlayLayer =
    copy(keyframes = keyframes.filterNot { it.timeMs == timeMs })

/**
 * Siembra un fotograma clave con el transform fijo actual para que una capa sin
 * animación no salte al empezar a grabar.
 */
fun OverlayLayer.seedKeyframesAt(timeMs: Long): OverlayLayer =
    if (keyframes.isEmpty()) {
        copy(keyframes = listOf(keyframeAt(timeMs)))
    } else {
        this
    }

/** Fotograma clave con el transform fijo actual de la capa. */
fun OverlayLayer.keyframeAt(timeMs: Long): LayerKeyframe =
    LayerKeyframe(timeMs = timeMs, x = x, y = y, scale = scale, rotationDeg = rotationDeg)

/**
 * Elimina la animación y deja la capa fija en el transform que tenía al final de
 * su ventana, para que el cambio sea visualmente continuo.
 */
fun OverlayLayer.clearAnimation(): OverlayLayer {
    if (keyframes.isEmpty()) return this
    val held = resolvedAt(endMs)
    return copy(
        x = held.x,
        y = held.y,
        scale = held.scale,
        rotationDeg = held.rotationDeg,
        keyframes = emptyList(),
    )
}

/**
 * Decide si una muestra de gesto debe convertirse en fotograma clave. Se guarda
 * siempre la primera muestra, se reemplaza la clave del mismo tiempo y se limita
 * la frecuencia a [MOTION_MIN_INTERVAL_MS], salvo que el transform se haya
 * movido lo suficiente como para no perder un gesto rápido.
 */
fun shouldSampleKeyframe(previous: LayerKeyframe?, candidate: LayerKeyframe): Boolean {
    if (previous == null) return true
    if (candidate.timeMs <= previous.timeMs) return true
    if (candidate.timeMs - previous.timeMs >= MOTION_MIN_INTERVAL_MS) return true
    return abs(candidate.x - previous.x) >= MOTION_MIN_MOVE ||
        abs(candidate.y - previous.y) >= MOTION_MIN_MOVE ||
        abs(candidate.scale - previous.scale) >= MOTION_MIN_SCALE_DELTA ||
        abs(candidate.rotationDeg - previous.rotationDeg) >= MOTION_MIN_ROTATION_DEG
}

/**
 * Caja táctil de una capa: el [layer] ya resuelto y su tamaño base medido en
 * píxeles del lienzo (sin aplicar escala). La rotación y la escala se aplican en
 * el *hit-test*.
 */
data class LayerHitTarget(
    val layer: OverlayLayer,
    val widthPx: Float,
    val heightPx: Float,
)

/**
 * Devuelve la capa de arriba que contiene el punto, o null. Los objetivos deben
 * venir en orden de dibujo (el último se dibuja encima).
 */
fun hitTestLayers(
    targets: List<LayerHitTarget>,
    pointX: Float,
    pointY: Float,
    containerWidth: Float,
    containerHeight: Float,
): OverlayLayer? {
    for (target in targets.asReversed()) {
        val layer = target.layer
        val halfWidth = target.widthPx * layer.scale / 2f
        val halfHeight = target.heightPx * layer.scale / 2f
        if (halfWidth <= 0f || halfHeight <= 0f) continue

        val centerX = layer.x * containerWidth
        val centerY = layer.y * containerHeight
        val dx = pointX - centerX
        val dy = pointY - centerY
        val radians = layer.rotationDeg * PI / 180.0
        val cosine = cos(radians)
        val sine = sin(radians)
        // Coordenadas del punto en el sistema local (sin rotación) de la capa.
        val localX = dx * cosine + dy * sine
        val localY = -dx * sine + dy * cosine
        if (abs(localX) <= halfWidth && abs(localY) <= halfHeight) return layer
    }
    return null
}
