package com.vidcam.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Procesamiento de nivel de la voz: nivel del medidor, pico, nivelación y
 * filtro paso alto (lo que queda cuando no hay RNNoise disponible).
 */
class PcmDspTest {

    @Test
    fun `el nivel crece con el volumen de la senal`() {
        val silence = PcmDsp.level(0f)
        val quiet = PcmDsp.level(100f)
        val loud = PcmDsp.level(16_000f)

        assertEquals(0f, silence, 0.001f)
        assertTrue(quiet < loud)
        assertTrue(loud <= 1f)
    }

    @Test
    fun `el pico es el valor absoluto maximo`() {
        val samples = shortArrayOf(0, -12_000, 8_000, 300)

        assertEquals(12_000, PcmDsp.peak(samples))
    }

    @Test
    fun `escalar recorta sin desbordar int16`() {
        val samples = shortArrayOf(20_000, -20_000)

        PcmDsp.scale(samples, 4f)

        assertEquals(32767.toShort(), samples[0])
        assertEquals((-32767).toShort(), samples[1])
    }

    @Test
    fun `escalar con ganancia uno no toca la senal`() {
        val samples = shortArrayOf(1, -2, 3)

        PcmDsp.scale(samples, 1f)

        assertEquals(listOf<Short>(1, -2, 3), samples.toList())
    }

    @Test
    fun `el paso alto quita la componente continua`() {
        // Señal constante (DC) de 10 s: un paso alto tiene que dejarla en cero.
        val samples = ShortArray(48_000 * 10) { 5_000 }

        PcmDsp.highPass(samples, 48_000, 85f)

        val last = samples.last().toInt()
        assertTrue("queda $last", abs(last) < 200)
    }

    @Test
    fun `el paso alto deja pasar una voz de 1 kHz`() {
        val sampleRate = 48_000
        val samples = ShortArray(sampleRate) { index ->
            val value = 8_000.0 * kotlin.math.sin(2.0 * Math.PI * 1_000.0 * index / sampleRate)
            value.toInt().toShort()
        }

        PcmDsp.highPass(samples, sampleRate, 85f)

        assertTrue(PcmDsp.rms(samples, 1_000, 47_000) > 4_000f)
    }

    @Test
    fun `el rms de una senal vacia es cero`() {
        assertEquals(0f, PcmDsp.rms(ShortArray(0)), 0.001f)
    }
}
