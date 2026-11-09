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
 * Duración base (en velocidad [AnimationSpeed.NORMAL]) de un barrido completo
 * de Ken Burns. En [AnimationSpeed.RAPIDO] se reduce a la mitad (1,5 s) y en
 * [AnimationSpeed.TURBO] a ~0,85 s, evitando que un vídeo de 15 o 30 s tenga un
 * zoom lento o imperceptible.
 */
const val BASE_KEN_BURNS_SWEEP_MS = 3_000L

/** Duración base de una transición de entrada o salida en velocidad normal. */
const val BASE_TRANSITION_MS = 450L

/**
 * Transformación incremental producida por un preset de animación o transición
 * en un instante dado, expresada en unidades normalizadas del lienzo.
 */
data class PngAnimationTransform(
    val scale: Float = 1f,
    val dx: Float = 0f,
    val dy: Float = 0f,
    val rotationDeg: Float = 0f,
    val alpha: Float = 1f,
)

/**
 * Transform de la capa resuelto en [timeMs] combinando:
 * 1. Sus fotogramas clave grabados (o el transform base si no los hay).
 * 2. El preset continuo o Ken Burns ([OverlayLayer.animation]) a la velocidad
 *    [OverlayLayer.animationSpeed].
 * 3. La transición de entrada/salida ([OverlayLayer.transition]).
 *
 * Se apoya en el invariante de que [OverlayLayer.keyframes] está ordenada por
 * tiempo (lo garantizan [upsertKeyframe] y [seedKeyframesAt]) para no ordenar en
 * cada fotograma de la vista previa o de la exportación.
 */
fun OverlayLayer.resolvedAt(timeMs: Long): OverlayLayer {
    val base = resolvedKeyframesAt(timeMs)
    if (animation == PngAnimationPreset.NINGUNO && transition == LayerTransitionPreset.NINGUNA) {
        return base
    }
    val localMs = (timeMs - startMs).coerceAtLeast(0L)
    val durationMs = (endMs - startMs).coerceAtLeast(1L)
    val preset = computePngPresetTransform(
        preset = animation,
        speed = animationSpeed,
        localTimeMs = localMs,
        durationMs = durationMs,
        isBackground = false,
    )
    val trans = computeLayerTransitionTransform(
        transition = transition,
        speed = animationSpeed,
        localTimeMs = localMs,
        durationMs = durationMs,
    )
    return base.copy(
        x = base.x + preset.dx + trans.dx,
        y = base.y + preset.dy + trans.dy,
        scale = (base.scale * preset.scale * trans.scale).coerceAtLeast(0.05f),
        rotationDeg = base.rotationDeg + preset.rotationDeg + trans.rotationDeg,
    )
}

/**
 * Opacidad resuelta de la capa en [timeMs] (0..1) según su transición de
 * entrada y salida.
 */
fun OverlayLayer.resolvedAlphaAt(timeMs: Long): Float {
    if (transition == LayerTransitionPreset.NINGUNA) return 1f
    val localMs = (timeMs - startMs).coerceAtLeast(0L)
    val durationMs = (endMs - startMs).coerceAtLeast(1L)
    return computeLayerTransitionTransform(
        transition = transition,
        speed = animationSpeed,
        localTimeMs = localMs,
        durationMs = durationMs,
    ).alpha.coerceIn(0f, 1f)
}

/**
 * Transformación resuelta de un fondo PNG en [timeMs], garantizando escala de
 * cobertura suficiente para no descubrir bordes negros.
 */
fun BackgroundLayer.resolvedTransformAt(
    timeMs: Long,
    index: Int = 0,
): PngAnimationTransform {
    if (kenBurns == KenBurnsMode.NINGUNO) return PngAnimationTransform()
    val kb = KenBurnsMath.transformAt(
        mode = kenBurns,
        index = index,
        startMs = startMs,
        endMs = endMs,
        timeMs = timeMs,
        speed = kenBurnsSpeed,
    )
    return PngAnimationTransform(
        scale = kb.scale,
        dx = kb.offsetX,
        dy = kb.offsetY,
        rotationDeg = kb.rotationDeg,
    )
}

/**
 * Transform de la capa resuelto únicamente por sus fotogramas clave en [timeMs],
 * sin sumar el preset continuo ni la transición de entrada/salida.
 */
fun OverlayLayer.resolvedKeyframesAt(timeMs: Long): OverlayLayer {
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

/**
 * Calcula la transformación del preset continuo o Ken Burns en [localTimeMs]
 * dentro de un tramo de duración [durationMs].
 *
 * Cuando [isBackground] es `true`, la escala resultante se eleva con
 * [ensureBackgroundCoverScale] para cubrir siempre el lienzo completo.
 */
fun computePngPresetTransform(
    preset: PngAnimationPreset,
    speed: AnimationSpeed,
    localTimeMs: Long,
    durationMs: Long,
    isBackground: Boolean = false,
): PngAnimationTransform {
    if (preset == PngAnimationPreset.NINGUNO) return PngAnimationTransform()

    val clampedLocalMs = localTimeMs.coerceAtLeast(0L)
    val effectiveDurationMs = durationMs.coerceAtLeast(300L)
    val baseSweepMs = minOf(effectiveDurationMs, BASE_KEN_BURNS_SWEEP_MS)
    val sweepMs = (baseSweepMs / speed.multiplier).toLong().coerceAtLeast(180L)
    val cyclePos = clampedLocalMs.toFloat() / sweepMs.toFloat()
    val p = smoothstep(triangleWave(cyclePos))
    val tSec = (clampedLocalMs / 1000.0) * speed.multiplier

    val raw = when (preset) {
        PngAnimationPreset.NINGUNO -> PngAnimationTransform()

        PngAnimationPreset.KEN_BURNS_ACERCAR -> PngAnimationTransform(
            scale = 1f + 0.45f * p,
            dx = 0.06f * p,
            dy = -0.04f * p,
        )

        PngAnimationPreset.KEN_BURNS_ALEJAR -> {
            val inv = 1f - p
            PngAnimationTransform(
                scale = 1f + 0.45f * inv,
                dx = 0.06f * inv,
                dy = 0.04f * inv,
            )
        }

        PngAnimationPreset.KEN_BURNS_PANEO -> {
            val swell = sin(p * PI).toFloat()
            PngAnimationTransform(
                scale = 1.32f + 0.06f * swell,
                dx = lerp(-0.12f, 0.12f, p),
                dy = -0.02f * swell,
            )
        }

        PngAnimationPreset.KEN_BURNS_DIAGONAL -> PngAnimationTransform(
            scale = 1.24f + 0.24f * p,
            dx = lerp(-0.09f, 0.09f, p),
            dy = lerp(0.09f, -0.09f, p),
        )

        PngAnimationPreset.PULSO -> {
            val wave = ((sin(tSec * 2.0 * PI * 1.3 - PI / 2.0) + 1.0) * 0.5).toFloat()
            val pulse = wave * wave
            PngAnimationTransform(
                scale = 1f + 0.25f * pulse,
            )
        }

        PngAnimationPreset.FLOTAR -> {
            val dy = (sin(tSec * 2.0 * PI * 0.85) * 0.045).toFloat()
            val dx = (sin(tSec * 2.0 * PI * 0.425) * 0.020).toFloat()
            val rot = (sin(tSec * 2.0 * PI * 0.60) * 3.5).toFloat()
            val scale = if (isBackground) {
                1.18f
            } else {
                1f + (sin(tSec * 2.0 * PI * 0.85) * 0.03).toFloat()
            }
            PngAnimationTransform(
                scale = scale,
                dx = dx,
                dy = dy,
                rotationDeg = rot,
            )
        }

        PngAnimationPreset.VAIVEN -> {
            val s = sin(tSec * 2.0 * PI * 1.8).toFloat()
            val rot = s * 9.0f
            val baseScale = if (isBackground) 1.24f else 1f
            PngAnimationTransform(
                scale = baseScale + abs(s) * 0.06f,
                rotationDeg = rot,
            )
        }

        PngAnimationPreset.TEMBLOR -> {
            val dx = (sin(tSec * 5.3) * 0.032 + sin(tSec * 11.1 + 0.7) * 0.020).toFloat()
            val dy = (cos(tSec * 6.1) * 0.032 + sin(tSec * 12.3 + 1.8) * 0.020).toFloat()
            val rot = (sin(tSec * 7.0 + 0.9) * 2.8).toFloat()
            val scale = if (isBackground) 1.18f else 1.05f
            PngAnimationTransform(
                scale = scale,
                dx = dx,
                dy = dy,
                rotationDeg = rot,
            )
        }
    }

    return if (isBackground) {
        raw.copy(
            scale = ensureBackgroundCoverScale(
                scale = raw.scale,
                dx = raw.dx,
                dy = raw.dy,
                rotationDeg = raw.rotationDeg,
            ),
        )
    } else {
        raw
    }
}

/**
 * Garantiza que un fondo animado tenga escala suficiente para cubrir el lienzo
 * completo sin descubrir franjas negras al desplazarse o rotar.
 */
fun ensureBackgroundCoverScale(
    scale: Float,
    dx: Float,
    dy: Float,
    rotationDeg: Float,
): Float {
    val rad = abs(rotationDeg) * PI.toFloat() / 180f
    val rotMargin = sin(rad.toDouble()).toFloat() * 1.45f
    val transMargin = 2.05f * maxOf(abs(dx), abs(dy))
    val minCover = 1f + transMargin + rotMargin
    return maxOf(scale, minCover)
}

/**
 * Calcula la transformación de entrada y salida ([transition]) en [localTimeMs]
 * dentro de un tramo de duración [durationMs].
 */
fun computeLayerTransitionTransform(
    transition: LayerTransitionPreset,
    speed: AnimationSpeed,
    localTimeMs: Long,
    durationMs: Long,
): PngAnimationTransform {
    if (transition == LayerTransitionPreset.NINGUNA) return PngAnimationTransform()

    val effectiveDuration = durationMs.coerceAtLeast(160L)
    val scaledWindow = (BASE_TRANSITION_MS / ((speed.multiplier + 1f) * 0.5f)).toLong()
    val windowMs = scaledWindow.coerceIn(80L, (effectiveDuration / 3L).coerceAtLeast(80L))

    val clampedLocal = localTimeMs.coerceIn(0L, effectiveDuration)
    val inProgress = (clampedLocal.toFloat() / windowMs.toFloat()).coerceIn(0f, 1f)
    val remainingMs = (effectiveDuration - clampedLocal).coerceAtLeast(0L)
    val outProgress = (remainingMs.toFloat() / windowMs.toFloat()).coerceIn(0f, 1f)
    val edgeProgress = minOf(inProgress, outProgress)

    return when (transition) {
        LayerTransitionPreset.NINGUNA -> PngAnimationTransform()

        LayerTransitionPreset.POP -> {
            val scale = when {
                inProgress < 1f -> popElasticCurve(inProgress)
                outProgress < 1f -> 0.15f + 0.85f * smoothstep(outProgress)
                else -> 1f
            }
            val alpha = smoothstep((edgeProgress * 2.5f).coerceIn(0f, 1f))
            PngAnimationTransform(scale = scale, alpha = alpha)
        }

        LayerTransitionPreset.DESLIZAR -> {
            val dy = when {
                inProgress < 1f -> 0.22f * (1f - easeOutCubic(inProgress))
                outProgress < 1f -> 0.22f * (1f - easeOutCubic(outProgress))
                else -> 0f
            }
            val alpha = smoothstep((edgeProgress * 2f).coerceIn(0f, 1f))
            PngAnimationTransform(dy = dy, alpha = alpha)
        }

        LayerTransitionPreset.FUNDIDO -> PngAnimationTransform(
            alpha = smoothstep(edgeProgress),
        )
    }
}

private fun triangleWave(u: Float): Float {
    val pos = u.coerceAtLeast(0f)
    val period = (pos / 2f).toInt()
    val rem = pos - period * 2f
    return if (rem <= 1f) rem else 2f - rem
}

private fun smoothstep(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

private fun easeOutCubic(x: Float): Float {
    val inv = 1f - x.coerceIn(0f, 1f)
    return 1f - inv * inv * inv
}

private fun popElasticCurve(t: Float): Float {
    val clamped = t.coerceIn(0f, 1f)
    return if (clamped < 0.7f) {
        val u = clamped / 0.7f
        0.15f + (1.16f - 0.15f) * smoothstep(u)
    } else {
        val u = (clamped - 0.7f) / 0.3f
        1.16f - 0.16f * smoothstep(u)
    }
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
    val held = resolvedKeyframesAt(endMs)
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
