package com.vidcam.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fundidos y ducking: la curva que se aplica a la música y a la voz al exportar
 * tiene que ser la misma que se usa en la vista previa.
 */
class AudioMixMathTest {

    @Test
    fun `sin fundidos la ganancia es uno`() {
        val gain = AudioMixMath.fadeGain(positionMs = 500L, lengthMs = 4_000L, fadeInMs = 0L, fadeOutMs = 0L)

        assertEquals(1f, gain, 0.0001f)
    }

    @Test
    fun `el fundido de entrada arranca en cero y llega a uno`() {
        val start = AudioMixMath.fadeGain(0L, 4_000L, 1_000L, 0L)
        val half = AudioMixMath.fadeGain(500L, 4_000L, 1_000L, 0L)
        val done = AudioMixMath.fadeGain(1_000L, 4_000L, 1_000L, 0L)

        assertEquals(0f, start, 0.001f)
        assertEquals(0.5f, half, 0.001f)
        assertEquals(1f, done, 0.001f)
    }

    @Test
    fun `el fundido de salida baja hasta cero al final`() {
        val beforeEnd = AudioMixMath.fadeGain(3_500L, 4_000L, 0L, 1_000L)
        val end = AudioMixMath.fadeGain(4_000L, 4_000L, 0L, 1_000L)

        assertTrue(beforeEnd > 0f)
        assertEquals(0f, end, 0.001f)
    }

    @Test
    fun `los fundidos de entrada y salida se combinan`() {
        // Un fundido de 500 ms a cada lado de un clip de 400 ms deja el centro bajo.
        val middle = AudioMixMath.fadeGain(200L, 400L, 500L, 500L)

        assertTrue(middle < 1f)
    }

    @Test
    fun `sin voz el ducking no toca la musica`() {
        val envelope = AudioMixMath.duckingEnvelope(FloatArray(20) { 0f }, amount = 0.7f)

        assertTrue(envelope.all { it == 1f })
    }

    @Test
    fun `el ducking baja la musica mientras hay voz`() {
        val voice = FloatArray(40) { if (it in 10..30) 0.4f else 0f }

        val envelope = AudioMixMath.duckingEnvelope(voice, amount = 0.7f)

        assertEquals(1f, envelope[0], 0.001f)
        // Unos bloques después de empezar la voz ya está atenuado.
        assertTrue("bloque 20 = ${envelope[20]}", envelope[20] < 0.45f)
        // Al terminar la voz vuelve cerca del máximo.
        assertTrue("final = ${envelope[39]}", envelope[39] > 0.8f)
    }

    @Test
    fun `el ducking se mide en decibelios y respeta el umbral`() {
        // Nivel muy bajo (silencio de fondo) no debe agachar la música.
        val quiet = FloatArray(20) { 0.001f }

        val envelope = AudioMixMath.duckingEnvelope(quiet, amount = 0.8f)

        assertTrue(envelope.all { it == 1f })
    }

    @Test
    fun `la ganancia de pista combina fundido y ducking`() {
        val ducking = floatArrayOf(1f, 0.5f, 0.5f, 1f)
        val gain = AudioMixMath.trackGain(
            positionMs = 100L,
            lengthMs = 1_000L,
            fadeInMs = 0L,
            fadeOutMs = 0L,
            ducking = ducking,
        )

        assertEquals(0.5f, gain, 0.001f)
    }

    @Test
    fun `los niveles de voz se alinean con la linea de tiempo`() {
        val sampleRate = 48_000
        val samples = ShortArray(sampleRate) { 16_384 }
        // La toma entra en 1 s: los primeros 20 bloques (1 s) deben estar a cero.
        val peaks = AudioMixMath.timelinePeaks(
            samples = samples,
            sampleRate = sampleRate,
            padMs = 1_000L,
            skipMs = 0L,
            projectMs = 3_000L,
        )

        assertEquals(60, peaks.size)
        assertEquals(0f, peaks[0], 0.0001f)
        assertEquals(0f, peaks[19], 0.0001f)
        assertTrue(peaks[20] > 0.4f)
    }

    @Test
    fun `una duracion invalida no revienta`() {
        assertEquals(1f, AudioMixMath.fadeGain(0L, 0L, 100L, 100L), 0.0001f)
        assertEquals(0, AudioMixMath.timelinePeaks(ShortArray(0), 48_000, 0L, 0L, 0L).size)
    }
}
