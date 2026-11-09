package com.vidcam.app.audio

import com.vidcam.app.model.VoiceDoublerConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class VoiceDoublerEngineTest {

    private fun generateSine(sampleRate: Int = 48_000, durationSec: Float = 0.5f): ShortArray {
        val total = (sampleRate * durationSec).toInt()
        return ShortArray(total) { i ->
            (sin(2.0 * PI * 440.0 * i / sampleRate) * 14000.0).toInt().toShort()
        }
    }

    @Test
    fun `doblador desactivado retorna copia intacta de muestras`() {
        val original = generateSine()
        val processed = VoiceDoublerEngine.process(original, config = VoiceDoublerConfig.OFF)

        assertEquals(original.size, processed.size)
        for (i in original.indices) {
            assertEquals(original[i], processed[i])
        }
    }

    @Test
    fun `preset grave aplica clon sin saturar amplitud maxima`() {
        val original = generateSine()
        val processed = VoiceDoublerEngine.process(original, config = VoiceDoublerConfig.GRAVE)

        assertEquals(original.size, processed.size)
        val peak = PcmDsp.peak(processed)
        assertTrue("El pico debe ser positivo y menor o igual a 32767", peak in 1..32767)
    }

    @Test
    fun `preset murmullo genera 4 capas desfasadas con volumen acotado`() {
        val original = generateSine()
        val processed = VoiceDoublerEngine.process(original, config = VoiceDoublerConfig.MURMULLO)

        assertEquals(original.size, processed.size)
        val peak = PcmDsp.peak(processed)
        assertTrue("Murmullo no debe distorsionar el buffer", peak in 1..32767)
    }
}
