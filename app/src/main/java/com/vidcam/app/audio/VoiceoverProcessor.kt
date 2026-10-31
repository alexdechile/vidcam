package com.vidcam.app.audio

import java.io.File

/**
 * Limpieza de una toma de voz en off: paso alto, RNNoise (aislación del
 * entorno), normalización y recorte seguro.
 *
 * El procesamiento se hace **después** de grabar, no en vivo: la toma siempre
 * queda guardada en crudo y se puede volver a procesar con otra fuerza sin
 * volver a hablar frente al micrófono.
 */
object VoiceoverProcessor {

    /** Frecuencia a la que se graba la voz: la que espera RNNoise. */
    const val SAMPLE_RATE = 48_000

    /** Fuerza por defecto de RNNoise (0 = solo filtro, 1 = denoised completo). */
    const val DEFAULT_STRENGTH = 0.9f

    /** Pico objetivo después de normalizar: -1 dBFS. */
    private const val TARGET_PEAK = 0.89f * 32767f

    /**
     * Procesa [input] y escribe el resultado en [output]. Si la librería nativa
     * no está disponible, el resultado sigue siendo válido (paso alto y
     * nivelación), solo sin la red de RNNoise.
     *
     * Devuelve `false` solo cuando el WAV de entrada no se puede leer o no se
     * puede escribir la salida.
     */
    fun denoise(input: File, output: File, strength: Float = DEFAULT_STRENGTH): Boolean {
        val audio = WavFile.read(input) ?: return false
        val samples = audio.samples
        if (samples.isEmpty()) return false
        PcmDsp.highPass(samples, audio.sampleRate, cutoffHz = 85f)
        if (Rnnoise.isAvailable) {
            suppressNoise(samples, strength.coerceIn(0f, 1f))
        }
        normalize(samples)
        return WavFile.write(output, samples, audio.sampleRate)
    }

    /**
     * Mezcla la salida de RNNoise con la señal original: [strength] en 1 suena
     * limpio y en 0 queda el crudo solo con el filtro paso alto.
     */
    private fun suppressNoise(samples: ShortArray, strength: Float) {
        if (strength <= 0f) return
        val frame = Rnnoise.frameSize
        if (frame <= 0) return
        val state = Rnnoise.create()
        if (state == 0L) return
        try {
            val input = ShortArray(frame)
            val wet = FloatArray(frame)
            var offset = 0
            while (offset < samples.size) {
                val count = minOf(frame, samples.size - offset)
                System.arraycopy(samples, offset, input, 0, count)
                for (index in count until frame) input[index] = 0
                Rnnoise.process(state, input, wet, frame)
                for (index in 0 until count) {
                    val dry = input[index].toFloat()
                    val mixed = wet[index] * strength + dry * (1f - strength)
                    samples[offset + index] = mixed.toInt().coerceIn(-32768, 32767).toShort()
                }
                offset += count
            }
        } finally {
            Rnnoise.destroy(state)
        }
    }

    /**
     * Lleva el pico a -1 dBFS: la voz se escucha pareja sin subir el ruido de
     * fondo que RNNoise ya dejó casi en silencio.
     */
    private fun normalize(samples: ShortArray) {
        val peak = PcmDsp.peak(samples)
        if (peak <= 0) return
        val gain = (TARGET_PEAK / peak).coerceIn(0.25f, 3f)
        PcmDsp.scale(samples, gain)
    }
}
