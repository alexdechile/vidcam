package com.vidcam.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Colocación de la voz en off: entrada (silencio delante), adelanto (recorte) y
 * recorte al final del vídeo, iguales en la vista previa y en la exportación.
 */
class VoiceoverMathTest {

    private fun track(
        startMs: Long = 0L,
        durationMs: Long,
        volume: Float = 1f,
        denoise: Boolean = true,
        processedUri: String? = null,
    ) = VoiceoverTrack(
        uri = "file:///voz.wav",
        processedUri = processedUri,
        denoise = denoise,
        startMs = startMs,
        durationMs = durationMs,
        volume = volume,
    )

    @Test
    fun `sin desplazamiento la toma entra completa`() {
        val plan = VoiceoverMath.plan(track(durationMs = 5_000L), projectMs = 10_000L)

        assertEquals(0L, plan.padMs)
        assertEquals(0L, plan.skipMs)
        assertEquals(5_000L, plan.takeMs)
    }

    @Test
    fun `un retardo agrega silencio delante`() {
        val plan = VoiceoverMath.plan(track(startMs = 1_500L, durationMs = 4_000L), 10_000L)

        assertEquals(1_500L, plan.padMs)
        assertEquals(0L, plan.skipMs)
        assertEquals(4_000L, plan.takeMs)
    }

    @Test
    fun `un adelanto descarta el comienzo de la toma`() {
        val plan = VoiceoverMath.plan(track(startMs = -800L, durationMs = 3_000L), 10_000L)

        assertEquals(0L, plan.padMs)
        assertEquals(800L, plan.skipMs)
        assertEquals(2_200L, plan.takeMs)
    }

    @Test
    fun `el tramo se recorta al final del video`() {
        val plan = VoiceoverMath.plan(track(startMs = 1_000L, durationMs = 8_000L), 5_000L)

        assertEquals(1_000L, plan.padMs)
        assertEquals(4_000L, plan.takeMs)
    }

    @Test
    fun `un retardo mayor que el video no deja audio`() {
        val plan = VoiceoverMath.plan(track(startMs = 6_000L, durationMs = 2_000L), 5_000L)

        assertTrue(plan.isEmpty)
    }

    @Test
    fun `el ajuste de entrada se acota a lo permitido`() {
        assertEquals(VoiceoverMath.MIN_START_MS, VoiceoverMath.clampStartMs(-9_000L, 30_000L))
        assertEquals(VoiceoverMath.MAX_START_MS, VoiceoverMath.clampStartMs(20_000L, 30_000L))
        // En un vídeo corto el retardo no puede pasar de la duración.
        assertEquals(4_000L, VoiceoverMath.clampStartMs(9_000L, 4_000L))
        assertEquals(0L, VoiceoverMath.clampStartMs(0L, 30_000L))
    }

    @Test
    fun `la posicion del archivo sigue a la linea de tiempo`() {
        val voice = track(startMs = 1_000L, durationMs = 3_000L)

        assertNull(VoiceoverMath.filePositionMs(0L, voice))
        assertEquals(0L, VoiceoverMath.filePositionMs(1_000L, voice))
        assertEquals(2_500L, VoiceoverMath.filePositionMs(3_500L, voice))
        assertNull(VoiceoverMath.filePositionMs(4_000L, voice))
    }

    @Test
    fun `con adelanto el archivo empieza en la muestra recortada`() {
        val voice = track(startMs = -500L, durationMs = 2_000L)

        assertEquals(500L, VoiceoverMath.filePositionMs(0L, voice))
        assertEquals(1_000L, VoiceoverMath.filePositionMs(500L, voice))
    }

    @Test
    fun `la toma limpia solo suena cuando la aislacion esta activa`() {
        val limpia = track(
            durationMs = 1_000L,
            denoise = true,
            processedUri = "file:///voz-limpia.wav",
        )
        val cruda = limpia.copy(denoise = false)

        assertEquals("file:///voz-limpia.wav", limpia.activeUri)
        assertEquals("file:///voz.wav", cruda.activeUri)
        assertEquals("file:///voz.wav", track(durationMs = 1_000L).activeUri)
    }
}
