package com.vidcam.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin

class VoicePitchShifterTest {

    @Test
    fun `calculo de ratio tonal responde a la escala musical logaritmica`() {
        assertEquals(1.0f, VoicePitchShifter.pitchRatio(0, 0), 1e-4f)
        assertEquals(2.0f, VoicePitchShifter.pitchRatio(12, 0), 1e-4f)
        assertEquals(0.5f, VoicePitchShifter.pitchRatio(-12, 0), 1e-4f)
        assertEquals(1.4983f, VoicePitchShifter.pitchRatio(7, 0), 1e-3f) // Quinta justa
    }

    @Test
    fun `desplazamiento en cero conserva muestras exactamente`() {
        val original = shortArrayOf(0, 1000, 2000, -1000, -2000)
        val result = VoicePitchShifter.shift(original, semitones = 0, cents = 0)

        assertEquals(original.size, result.size)
        for (i in original.indices) {
            assertEquals(original[i], result[i])
        }
    }

    @Test
    fun `desplazamiento a tono grave y agudo preserva tamano y no desborda`() {
        val sampleRate = 48_000
        val size = sampleRate // 1 segundo
        val original = ShortArray(size) { i ->
            (sin(2.0 * PI * 220.0 * i / sampleRate) * 16000.0).toInt().toShort()
        }

        val grave = VoicePitchShifter.shift(original, sampleRate, semitones = -12)
        assertEquals(size, grave.size)
        assertTrue(PcmDsp.peak(grave) > 0)

        val agudo = VoicePitchShifter.shift(original, sampleRate, semitones = 12)
        assertEquals(size, agudo.size)
        assertTrue(PcmDsp.peak(agudo) > 0)
    }
}
