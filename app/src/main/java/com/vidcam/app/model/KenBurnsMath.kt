package com.vidcam.app.model

import kotlin.math.PI
import kotlin.math.sin

/**
 * Cálculo puro del efecto Ken Burns y animaciones procedimentales sobre imágenes
 * de fondo fijas.
 *
 * Incluye selector de velocidad ([AnimationSpeed.NORMAL], [AnimationSpeed.RAPIDO],
 * [AnimationSpeed.TURBO]) y tanto barridos Ken Burns (`ACERCAR`, `ALEJAR`,
 * `AUTOMATICO`, `PANEO`, `DIAGONAL`) como bucles continuos (`PULSO`, `FLOTAR`,
 * `VAIVEN`, `TEMBLOR`).
 *
 * El desplazamiento y la rotación se acotan siempre a lo que el zoom permite
 * (`|offset| <= (scale - 1) / 2`), de modo que el borde de la imagen nunca entra
 * en el encuadre y no aparecen franjas negras.
 *
 * Vive fuera de Compose y de Media3 para que la vista previa y la exportación
 * usen exactamente las mismas cuentas y para poder probarla en la JVM.
 */
object KenBurnsMath {

    /**
     * Zoom base de la animación en velocidad normal (0.36 = 36 %, más de 2,5x
     * el valor inicial de 14 % para que el movimiento sea claro y dinámico).
     */
    const val ZOOM_AMOUNT = 0.36f

    /** Zoom máximo según la velocidad elegida. */
    fun zoomAmount(speed: AnimationSpeed = AnimationSpeed.NORMAL): Float = when (speed) {
        AnimationSpeed.NORMAL -> ZOOM_AMOUNT
        AnimationSpeed.RAPIDO -> 0.50f
        AnimationSpeed.TURBO -> 0.68f
    }

    /**
     * Transformación de una imagen en un instante: [scale] es el factor de zoom
     * (1 = imagen quieta a encuadre completo), [offsetX]/[offsetY] el
     * desplazamiento en unidades normalizadas del lienzo (1 = ancho o alto) y
     * [rotationDeg] el giro en grados horarios.
     */
    data class KenBurnsTransform(
        val scale: Float = 1f,
        val offsetX: Float = 0f,
        val offsetY: Float = 0f,
        val rotationDeg: Float = 0f,
    )

    /** Imagen quieta, sin zoom, desplazamiento ni rotación. */
    val SIN_EFECTO = KenBurnsTransform()

    /** `true` si [mode] mueve la imagen en algún momento. */
    fun isActive(mode: KenBurnsMode): Boolean = mode != KenBurnsMode.NINGUNO

    /**
     * Modo efectivo de una imagen: [KenBurnsMode.AUTOMATICO] alterna acercar y
     * alejar según el índice de la imagen en la línea de tiempo, para que dos
     * imágenes consecutivas no repitan el mismo movimiento.
     */
    fun resolveMode(mode: KenBurnsMode, index: Int): KenBurnsMode = when (mode) {
        KenBurnsMode.AUTOMATICO ->
            if (index.coerceAtLeast(0) % 2 == 0) KenBurnsMode.ACERCAR else KenBurnsMode.ALEJAR
        else -> mode
    }

    /**
     * Posición de la imagen dentro del proyecto (orden por inicio), que decide
     * qué dirección usa [KenBurnsMode.AUTOMATICO]. Devuelve 0 si [id] no existe.
     */
    fun indexOf(backgrounds: List<BackgroundLayer>, id: String?): Int {
        if (id == null) return 0
        val index = backgrounds.sortedBy { it.startMs }.indexOfFirst { it.id == id }
        return index.coerceAtLeast(0)
    }

    /**
     * Progreso `[0, 1]` de la animación en [timeMs], con suavizado de entrada y
     * salida para que el zoom no arranque ni termine de golpe.
     *
     * En velocidades [AnimationSpeed.RAPIDO] y [AnimationSpeed.TURBO], si el
     * tramo es más largo que el ciclo rápido, el barrido hace vaivén continuo
     * para que incluso un fondo de 15 o 30 s se mueva con agilidad desde el
     * primer segundo.
     */
    fun progressAt(
        startMs: Long,
        endMs: Long,
        timeMs: Long,
        speed: AnimationSpeed = AnimationSpeed.NORMAL,
    ): Float {
        val span = endMs - startMs
        if (span <= 0L) return 0f
        if (speed == AnimationSpeed.NORMAL) {
            val linear = ((timeMs - startMs).toFloat() / span.toFloat()).coerceIn(0f, 1f)
            return smoothstep(linear)
        }
        val clampedTime = timeMs.coerceIn(startMs, endMs)
        val localMs = (clampedTime - startMs).coerceAtLeast(0L)
        val targetSweepMs = (BASE_KEN_BURNS_SWEEP_MS / speed.multiplier).toLong().coerceAtLeast(400L)
        if (span <= targetSweepMs) {
            val linear = (localMs.toFloat() / span.toFloat()).coerceIn(0f, 1f)
            return smoothstep(linear)
        }
        val cyclePos = localMs.toFloat() / targetSweepMs.toFloat()
        val period = (cyclePos / 2f).toInt()
        val rem = cyclePos - period * 2f
        val wave = if (rem <= 1f) rem else 2f - rem
        return smoothstep(wave)
    }

    /**
     * Transformación de la imagen en [timeMs] para un tramo `[startMs, endMs)`.
     * Fuera del tramo se devuelve el primer o el último fotograma del efecto, así
     * que la imagen nunca salta al entrar o salir.
     */
    fun transformAt(
        mode: KenBurnsMode,
        index: Int,
        startMs: Long,
        endMs: Long,
        timeMs: Long,
        speed: AnimationSpeed = AnimationSpeed.NORMAL,
    ): KenBurnsTransform {
        val resolved = resolveMode(mode, index)
        if (resolved == KenBurnsMode.NINGUNO) return SIN_EFECTO
        val span = endMs - startMs
        if (span <= 0L) return SIN_EFECTO

        val zoom = zoomAmount(speed)
        val progress = progressAt(startMs, endMs, timeMs, speed)
        val localMs = (timeMs.coerceIn(startMs, endMs) - startMs).coerceAtLeast(0L)

        return when (resolved) {
            KenBurnsMode.NINGUNO,
            KenBurnsMode.AUTOMATICO -> SIN_EFECTO

            KenBurnsMode.ACERCAR -> {
                val scale = 1f + zoom * progress
                val drift = -1f + 2f * progress
                val maxOffset = (scale - 1f) / 2f
                KenBurnsTransform(
                    scale = scale,
                    offsetX = drift * maxOffset,
                    offsetY = 0f,
                )
            }

            KenBurnsMode.ALEJAR -> {
                val scale = 1f + zoom * (1f - progress)
                val drift = 1f - 2f * progress
                val maxOffset = (scale - 1f) / 2f
                KenBurnsTransform(
                    scale = scale,
                    offsetX = drift * maxOffset,
                    offsetY = 0f,
                )
            }

            KenBurnsMode.PANEO -> {
                val swell = sin(progress * PI).toFloat()
                val scale = 1f + zoom * (0.85f + 0.15f * swell)
                val maxOffset = (scale - 1f) / 2f
                val drift = -1f + 2f * progress
                KenBurnsTransform(
                    scale = scale,
                    offsetX = drift * maxOffset * 0.9f,
                    offsetY = 0f,
                )
            }

            KenBurnsMode.DIAGONAL -> {
                val scale = 1f + zoom * (0.45f + 0.55f * progress)
                val maxOffset = (scale - 1f) / 2f
                val drift = -1f + 2f * progress
                KenBurnsTransform(
                    scale = scale,
                    offsetX = drift * maxOffset * 0.7f,
                    offsetY = -drift * maxOffset * 0.7f,
                )
            }

            KenBurnsMode.PULSO -> {
                val preset = computePngPresetTransform(
                    preset = PngAnimationPreset.PULSO,
                    speed = speed,
                    localTimeMs = localMs,
                    durationMs = span,
                    isBackground = true,
                )
                KenBurnsTransform(scale = preset.scale.coerceAtLeast(1f))
            }

            KenBurnsMode.FLOTAR -> presetToKenBurns(
                preset = PngAnimationPreset.FLOTAR,
                speed = speed,
                localMs = localMs,
                span = span,
            )

            KenBurnsMode.VAIVEN -> presetToKenBurns(
                preset = PngAnimationPreset.VAIVEN,
                speed = speed,
                localMs = localMs,
                span = span,
            )

            KenBurnsMode.TEMBLOR -> presetToKenBurns(
                preset = PngAnimationPreset.TEMBLOR,
                speed = speed,
                localMs = localMs,
                span = span,
            )
        }
    }

    private fun presetToKenBurns(
        preset: PngAnimationPreset,
        speed: AnimationSpeed,
        localMs: Long,
        span: Long,
    ): KenBurnsTransform {
        val raw = computePngPresetTransform(
            preset = preset,
            speed = speed,
            localTimeMs = localMs,
            durationMs = span,
            isBackground = true,
        )
        val scale = ensureBackgroundCoverScale(
            scale = raw.scale,
            dx = raw.dx,
            dy = raw.dy,
            rotationDeg = raw.rotationDeg,
        )
        val maxOffset = (scale - 1f) / 2f
        return KenBurnsTransform(
            scale = scale,
            offsetX = raw.dx.coerceIn(-maxOffset, maxOffset),
            offsetY = raw.dy.coerceIn(-maxOffset, maxOffset),
            rotationDeg = raw.rotationDeg,
        )
    }

    /**
     * Cambio de zoom por segundo que implica un tramo de `[startMs, endMs)`.
     * Sirve para documentar y probar que un vídeo más largo o una velocidad
     * mayor cambia la tasa de zoom.
     */
    fun zoomPerSecond(
        startMs: Long,
        endMs: Long,
        speed: AnimationSpeed = AnimationSpeed.NORMAL,
    ): Float {
        val spanMs = (endMs - startMs).coerceAtLeast(1L)
        val effectiveSpanMs = if (speed == AnimationSpeed.NORMAL) {
            spanMs
        } else {
            minOf(spanMs, (BASE_KEN_BURNS_SWEEP_MS / speed.multiplier).toLong().coerceAtLeast(400L))
        }
        return zoomAmount(speed) / (effectiveSpanMs.toFloat() / 1000f)
    }

    /**
     * Desplazamiento horizontal en NDC (-1..1, origen en el centro), que es el
     * espacio en el que trabaja Media3. El lienzo normalizado usa 1 = ancho
     * completo, así que el factor es 2.
     */
    fun ndcOffsetX(transform: KenBurnsTransform): Float = transform.offsetX * 2f

    /** Desplazamiento vertical en NDC; el eje Y de Media3 apunta hacia arriba. */
    fun ndcOffsetY(transform: KenBurnsTransform): Float = transform.offsetY * -2f

    private fun smoothstep(x: Float): Float {
        val t = x.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }
}
