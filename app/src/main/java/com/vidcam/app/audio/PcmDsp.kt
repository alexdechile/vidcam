package com.vidcam.app.audio

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Operaciones de audio PCM 16 bits sin dependencias de Android, para poder
 * probarlas con tests unitarios.
 */
object PcmDsp {

    private const val FULL_SCALE = 32767f

    /** Valor eficaz (RMS) en la escala de int16. */
    fun rms(samples: ShortArray, from: Int = 0, to: Int = samples.size): Float {
        val start = from.coerceIn(0, samples.size)
        val end = to.coerceIn(start, samples.size)
        if (end <= start) return 0f
        var sum = 0.0
        for (index in start until end) {
            val value = samples[index].toDouble()
            sum += value * value
        }
        return sqrt(sum / (end - start)).toFloat()
    }

    /** Pico absoluto en la escala de int16. */
    fun peak(samples: ShortArray, from: Int = 0, to: Int = samples.size): Int {
        val start = from.coerceIn(0, samples.size)
        val end = to.coerceIn(start, samples.size)
        var maximum = 0
        for (index in start until end) {
            val value = kotlin.math.abs(samples[index].toInt())
            if (value > maximum) maximum = value
        }
        return maximum
    }

    /**
     * Escala la señal en el sitio con recorte duro para no desbordar int16.
     * Devuelve la ganancia aplicada.
     */
    fun scale(samples: ShortArray, gain: Float): Float {
        if (gain == 1f || samples.isEmpty()) return gain
        for (index in samples.indices) {
            val value = samples[index] * gain
            samples[index] = when {
                value > FULL_SCALE -> FULL_SCALE.toInt().toShort()
                value < -FULL_SCALE -> (-FULL_SCALE).toInt().toShort()
                else -> value.toInt().toShort()
            }
        }
        return gain
    }

    /**
     * Filtro paso alto de segundo orden (Butterworth, Q = 0.707) para quitar el
     * retumbe de la habitación y el ruido de baja frecuencia antes de RNNoise.
     */
    fun highPass(samples: ShortArray, sampleRate: Int, cutoffHz: Float) {
        if (samples.isEmpty() || sampleRate <= 0 || cutoffHz <= 0f) return
        val w0 = 2.0 * PI * cutoffHz / sampleRate
        val cosW = cos(w0)
        val sinW = sin(w0)
        val alpha = sinW / (2.0 * 0.7071067811865476)
        val a0 = 1.0 + alpha
        val b0 = (1.0 + cosW) / 2.0 / a0
        val b1 = -(1.0 + cosW) / a0
        val b2 = (1.0 + cosW) / 2.0 / a0
        val a1 = -2.0 * cosW / a0
        val a2 = (1.0 - alpha) / a0
        var x1 = 0.0
        var x2 = 0.0
        var y1 = 0.0
        var y2 = 0.0
        for (index in samples.indices) {
            val x0 = samples[index].toDouble()
            val y0 = b0 * x0 + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2
            x2 = x1
            x1 = x0
            y2 = y1
            y1 = y0
            samples[index] = y0.toInt().coerceIn(-32768, 32767).toShort()
        }
    }

    /** Nivel 0..1 a partir del RMS, con la escala de -60 dBFS a 0 dBFS. */
    fun level(rms: Float): Float {
        if (rms <= 0f) return 0f
        val db = 20f * kotlin.math.log10(rms / 32768f)
        return ((db + 60f) / 60f).coerceIn(0f, 1f)
    }
}
