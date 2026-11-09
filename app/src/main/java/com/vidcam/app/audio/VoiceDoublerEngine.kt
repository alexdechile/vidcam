package com.vidcam.app.audio

import com.vidcam.app.model.VoiceDoublerConfig
import com.vidcam.app.model.VoiceDoublerPreset
import kotlin.math.sqrt

/**
 * Generador de doblaje automático (ADT) y murmullo multi-voz para locuciones
 * en off.
 *
 * Clona la toma base de 1 a 5 veces con desfases de tiempo (10..80 ms),
 * transposición de semitonos y micro-desafinaciones en cents para emular una
 * sala con varias personas hablando o engrosar la presencia vocal.
 *
 * La mezcla final aplica una normalización suave y un factor de atenuación
 * proporcional a la raíz cuadrada de las capas para prevenir saturación
 * (anti-clipping digital).
 */
object VoiceDoublerEngine {

    /** Pico objetivo después de mezclar: -1 dBFS */
    private const val TARGET_PEAK = 0.89f * 32767f

    /**
     * Aplica el efecto de doblaje sobre [samples] según [config].
     * Si el doblador está desactivado, devuelve una copia directa de [samples].
     */
    fun process(
        samples: ShortArray,
        sampleRate: Int = 48_000,
        config: VoiceDoublerConfig,
    ): ShortArray {
        if (samples.isEmpty() || !config.enabled || config.preset == VoiceDoublerPreset.OFF) {
            return samples.copyOf()
        }

        val count = config.voiceCount.coerceIn(1, 5)
        val wetMix = config.wetMix.coerceIn(0f, 1f)
        val baseDelaySamples = (config.delayMs * sampleRate / 1000L).toInt().coerceAtLeast(0)

        // Generamos cada capa doblada
        val layers = ArrayList<ShortArray>(count)
        val layerDelays = IntArray(count)

        for (k in 0 until count) {
            val (semi, cents) = voicePitchParams(config, index = k, total = count)
            val shifted = VoicePitchShifter.shift(
                samples = samples,
                sampleRate = sampleRate,
                semitones = semi,
                cents = cents,
            )
            layers.add(shifted)

            // Desfase escalonado: la primera voz usa el delay base, las siguientes se separan
            val delayJitter = when (k) {
                0 -> 0
                1 -> (15L * sampleRate / 1000L).toInt()
                2 -> (30L * sampleRate / 1000L).toInt()
                3 -> (45L * sampleRate / 1000L).toInt()
                else -> (60L * sampleRate / 1000L).toInt()
            }
            layerDelays[k] = baseDelaySamples + delayJitter
        }

        // Mezclamos la señal seca (original) y las capas húmedas con atenuación ponderada
        val output = ShortArray(samples.size)
        val dryGain = 1.0f - 0.35f * wetMix
        val wetFactor = (wetMix / sqrt(count.toDouble())).toFloat()

        for (i in samples.indices) {
            var sum = samples[i].toFloat() * dryGain

            for (k in 0 until count) {
                val delay = layerDelays[k]
                val sampleIdx = i - delay
                if (sampleIdx in layers[k].indices) {
                    sum += layers[k][sampleIdx].toFloat() * wetFactor
                }
            }
            output[i] = sum.toInt().coerceIn(-32768, 32767).toShort()
        }

        // Normalización suave para garantizar presencia sin clipping
        val peak = PcmDsp.peak(output)
        if (peak > 0) {
            val gain = (TARGET_PEAK / peak).coerceIn(0.2f, 1.25f)
            if (gain < 1.0f || peak < TARGET_PEAK * 0.7f) {
                PcmDsp.scale(output, gain)
            }
        }

        return output
    }

    /**
     * Calcula semitonos y cents para la capa [index] de [total] voces.
     */
    private fun voicePitchParams(config: VoiceDoublerConfig, index: Int, total: Int): Pair<Int, Int> {
        val baseSemi = config.pitchSemitones
        val baseCents = config.detuneCents

        if (total == 1) {
            return Pair(baseSemi, baseCents)
        }

        // Distribución armónica y de micro-desafinación para murmullo / multitud
        val semiOffset = when (index) {
            0 -> 0
            1 -> if (baseSemi <= 0) -2 else 2
            2 -> if (baseSemi <= 0) -4 else 3
            3 -> if (baseSemi <= 0) 3 else -3
            else -> if (baseSemi <= 0) -5 else 5
        }
        val centsOffset = when (index % 3) {
            0 -> baseCents
            1 -> -baseCents
            else -> baseCents / 2
        }

        return Pair((baseSemi + semiOffset).coerceIn(-12, 12), centsOffset.coerceIn(-50, 50))
    }
}
