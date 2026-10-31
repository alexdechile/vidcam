package com.vidcam.app.audio

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

/**
 * Formato de las tomas de voz en off: WAV PCM 16 bits mono a 48 kHz, y la
 * copia con relleno o recorte que usa la exportación para colocarlas en su
 * segundo de entrada.
 */
class WavFileTest {

    @get:Rule
    val folder = TemporaryFolder()

    @Test
    fun `escribe y lee el mismo audio`() {
        val samples = ShortArray(1_000) { (it % 200 - 100).toShort() }
        val file = folder.newFile("toma.wav")

        assertTrue(WavFile.write(file, samples, 48_000))

        val read = WavFile.read(file)
        assertNotNull(read)
        assertEquals(48_000, read!!.sampleRate)
        assertEquals(samples.size, read.samples.size)
        assertEquals(samples.toList(), read.samples.toList())
        assertEquals(20L, read.durationMs)
    }

    @Test
    fun `la duracion sale de la cabecera sin cargar el audio`() {
        val samples = ShortArray(48_000) { 1 }
        val file = folder.newFile("un-segundo.wav")

        WavFile.write(file, samples, 48_000)

        assertEquals(1_000L, WavFile.durationMs(file))
    }

    @Test
    fun `el relleno se escribe delante del tramo elegido`() {
        val samples = ShortArray(48_000) { (it % 100).toShort() }
        val file = folder.newFile("con-relleno.wav")
        // 250 ms de silencio y luego 500 ms desde la muestra 4800.
        val written = WavFile.writeSegment(
            file = file,
            samples = samples,
            sampleRate = 48_000,
            padSamples = 12_000,
            skipSamples = 4_800,
            takeSamples = 24_000,
        )

        assertTrue(written)
        val read = WavFile.read(file)
        assertNotNull(read)
        assertEquals(12_000 + 24_000, read!!.samples.size)
        assertEquals(0.toShort(), read.samples[0])
        assertEquals(0.toShort(), read.samples[11_999])
        assertEquals(samples[4_800], read.samples[12_000])
        assertEquals(samples[4_800 + 23_999], read.samples[36_000 - 1])
    }

    @Test
    fun `el recorte negativo descarta el comienzo de la toma`() {
        val samples = ShortArray(10_000) { (it + 1).toShort() }
        val file = folder.newFile("recortada.wav")

        WavFile.writeSegment(
            file = file,
            samples = samples,
            sampleRate = 48_000,
            padSamples = 0,
            skipSamples = 2_000,
            takeSamples = 5_000,
        )

        val read = WavFile.read(file)
        assertNotNull(read)
        assertEquals(5_000, read!!.samples.size)
        assertEquals(samples[2_000], read.samples[0])
    }

    @Test
    fun `un archivo que no es wav devuelve null`() {
        val file = folder.newFile("basura.wav")
        file.writeText("no soy un wav")

        assertNull(WavFile.read(file))
        assertEquals(0L, WavFile.durationMs(file))
    }

    @Test
    fun `las muestras se convierten a bytes little endian`() {
        val bytes = WavFile.toBytes(shortArrayOf(0x0102, -2), 2)

        assertEquals(4, bytes.size)
        assertEquals(0x02.toByte(), bytes[0])
        assertEquals(0x01.toByte(), bytes[1])
        assertEquals(0xfe.toByte(), bytes[2])
        assertEquals(0xff.toByte(), bytes[3])
    }
}
