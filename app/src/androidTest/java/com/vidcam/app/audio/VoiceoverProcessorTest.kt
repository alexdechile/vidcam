package com.vidcam.app.audio

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import kotlin.math.abs
import kotlin.math.sin

/**
 * Comprueba en un dispositivo la librería nativa de RNNoise y la cadena de
 * limpieza de la voz, que es lo único que no se puede verificar en la JVM.
 */
@RunWith(AndroidJUnit4::class)
class VoiceoverProcessorTest {

    @Test
    fun rnnoiseProcesaUnFotograma() {
        assertTrue("No se pudo cargar librnnoise_jni", Rnnoise.isAvailable)
        val frame = Rnnoise.frameSize
        assertEquals(480, frame)

        val state = Rnnoise.create()
        assertTrue("No se pudo crear el estado de RNNoise", state != 0L)
        val input = ShortArray(frame) { index ->
            val value = 8_000.0 * sin(2.0 * Math.PI * 200.0 * index / frame)
            value.toInt().toShort()
        }
        val output = FloatArray(frame)
        val vad = try {
            Rnnoise.process(state, input, output, frame)
        } finally {
            Rnnoise.destroy(state)
        }

        assertTrue("VAD fuera de rango: $vad", vad in 0f..1f)
        // Con una señal limpia la red no puede devolver exactamente la entrada.
        assertTrue((0 until frame).any { abs(output[it] - input[it]) > 0.5f })
    }

    @Test
    fun laLimpiezaEscribeUnWavDelMismoTamano() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val dir = File(context.cacheDir, "voz-test").apply { mkdirs() }
        val sampleRate = 48_000
        val samples = ShortArray(sampleRate * 2) { index ->
            val tone = 6_000.0 * sin(2.0 * Math.PI * 220.0 * index / sampleRate)
            val noise = ((index * 7919) % 2_400) - 1_200
            (tone + noise).toInt().toShort()
        }
        val input = File(dir, "entrada.wav")
        val output = File(dir, "salida.wav")
        assertTrue(WavFile.write(input, samples, sampleRate))

        assertTrue(VoiceoverProcessor.denoise(input, output, strength = 0.9f))

        val result = WavFile.read(output)
        assertTrue(result != null)
        assertEquals(samples.size, result!!.samples.size)
        assertEquals(sampleRate, result.sampleRate)
        assertTrue(PcmDsp.peak(result.samples) > 1_000)
        input.delete()
        output.delete()
    }
}
