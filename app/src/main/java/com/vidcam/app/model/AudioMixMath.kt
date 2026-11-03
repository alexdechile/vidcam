package com.vidcam.app.model

import kotlin.math.abs

/**
 * Curvas de ganancia para fundidos y ducking, sin dependencias de Android.
 *
 * Todo se calcula a partir de bloques de tiempo (por defecto 50 ms), que es lo
 * que después usa la exportación para armar la envolvente de cada pista.
 */
object AudioMixMath {

    /** Tamaño de bloque de la envolvente de ducking. */
    const val BLOCK_MS = 50L

    /** Umbral de voz: por debajo de esto la música no se agacha (dBFS). */
    private const val VOICE_THRESHOLD_DB = -42f

    /** Tiempo de ataque del ducking: cuánto tarda la música en bajar. */
    private const val ATTACK_MS = 60L

    /** Tiempo de liberación: cuánto tarda en volver al volumen normal. */
    private const val RELEASE_MS = 320L

    /**
     * Ganancia del fundido de entrada/salida en [positionMs] sobre una pista de
     * [lengthMs]. Usa una curva suave (smoothstep), no una rampa lineal, para
     * que el oído no note el borde.
     *
     * Devuelve 0..1.
     */
    fun fadeGain(
        positionMs: Long,
        lengthMs: Long,
        fadeInMs: Long,
        fadeOutMs: Long,
    ): Float {
        if (lengthMs <= 0L) return 1f
        val position = positionMs.coerceIn(0L, lengthMs)
        val fadeIn = fadeInMs.coerceIn(0L, lengthMs)
        val fadeOut = fadeOutMs.coerceIn(0L, lengthMs)
        val inGain = if (fadeIn <= 0L) 1f else smooth(position.toFloat() / fadeIn)
        val remaining = lengthMs - position
        val outGain = if (fadeOut <= 0L) 1f else smooth(remaining.toFloat() / fadeOut)
        return minOf(inGain, outGain).coerceIn(0f, 1f)
    }

    private fun smooth(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }

    /**
     * Envolvente de ganancia de la música a partir del nivel de la voz, con un
     * bloque por cada [BLOCK_MS] de vídeo.
     *
     * [voicePeak] es el nivel (0..1, escala de int16 / 32768) por bloque de la
     * toma de voz; se considera "hay voz" cuando supera el umbral. [amount] es
     * cuánto baja la música: 0,7 deja la pista al 30 %.
     *
     * Antes del primer bloque con voz no hay atenuación (no tiene sentido
     * agachar la música en un silencio inicial).
     */
    fun duckingEnvelope(
        voicePeak: FloatArray,
        amount: Float,
    ): FloatArray {
        val duck = amount.coerceIn(0f, 0.95f)
        if (duck <= 0f || voicePeak.isEmpty()) {
            return FloatArray(voicePeak.size) { 1f }
        }
        val threshold = Math.pow(10.0, (VOICE_THRESHOLD_DB / 20f).toDouble()).toFloat()
        val attack = (ATTACK_MS.toFloat() / BLOCK_MS).coerceAtLeast(1f)
        val release = (RELEASE_MS.toFloat() / BLOCK_MS).coerceAtLeast(1f)
        val envelope = FloatArray(voicePeak.size)
        var gain = 1f
        var seenVoice = false
        for (index in voicePeak.indices) {
            val voiced = voicePeak[index] > threshold
            if (voiced) seenVoice = true
            val target = if (seenVoice && voiced) 1f - duck else 1f
            val coefficient = 1f / (if (target < gain) attack else release)
            gain += (target - gain) * coefficient
            envelope[index] = gain.coerceIn(0f, 1f)
        }
        return envelope
    }

    /** Nivel por bloque de una señal ya normalizada (0..1), para [duckingEnvelope]. */
    fun peaksByBlock(
        samples: FloatArray,
        sampleRate: Int,
        blockMs: Long = BLOCK_MS,
    ): FloatArray {
        if (samples.isEmpty() || sampleRate <= 0 || blockMs <= 0L) return FloatArray(0)
        val blockSamples = (sampleRate * blockMs / 1000L).toInt().coerceAtLeast(1)
        val blocks = (samples.size + blockSamples - 1) / blockSamples
        val peaks = FloatArray(blocks)
        for (block in 0 until blocks) {
            val from = block * blockSamples
            val to = minOf(samples.size, from + blockSamples)
            var peak = 0f
            for (index in from until to) {
                val value = abs(samples[index])
                if (value > peak) peak = value
            }
            peaks[block] = peak
        }
        return peaks
    }

    /**
     * Niveles de la voz ya colocados en la línea de tiempo: un valor por bloque
     * de [BLOCK_MS], con ceros donde la toma todavía no suena.
     *
     * [samples] es la toma cruda, [padMs] el silencio que la precede y [skipMs]
     * el recorte de su comienzo (ver `VoiceoverMath.plan`).
     */
    fun timelinePeaks(
        samples: ShortArray,
        sampleRate: Int,
        padMs: Long,
        skipMs: Long,
        projectMs: Long,
        blockMs: Long = BLOCK_MS,
    ): FloatArray {
        if (samples.isEmpty() || sampleRate <= 0 || projectMs <= 0L || blockMs <= 0L) {
            return FloatArray(0)
        }
        val blockSamples = (sampleRate * blockMs / 1000L).toInt().coerceAtLeast(1)
        val blocks = (projectMs / blockMs).toInt().coerceAtLeast(1)
        val peaks = FloatArray(blocks)
        val skip = skipMs.coerceIn(0L, samples.size.toLong()).toInt()
        val startBlock = (padMs / blockMs).toInt().coerceAtLeast(0)
        var block = startBlock
        var index = skip
        while (index < samples.size && block < blocks) {
            var peak = 0f
            val end = minOf(samples.size, index + blockSamples)
            while (index < end) {
                val value = abs(samples[index] / 32768f)
                if (value > peak) peak = value
                index++
            }
            peaks[block] = peak
            block++
        }
        return peaks
    }

    /** Ganancia en un instante de la pista, combinando fundidos y ducking. */
    fun trackGain(
        positionMs: Long,
        lengthMs: Long,
        fadeInMs: Long,
        fadeOutMs: Long,
        ducking: FloatArray?,
    ): Float {
        val fade = fadeGain(positionMs, lengthMs, fadeInMs, fadeOutMs)
        if (ducking == null || ducking.isEmpty()) return fade
        val index = (positionMs.coerceAtLeast(0L) / BLOCK_MS).toInt()
        val duck = ducking.getOrElse(index) { ducking.last() }
        return (fade * duck).coerceIn(0f, 1f)
    }
}
