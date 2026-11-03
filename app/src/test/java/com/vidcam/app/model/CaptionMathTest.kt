package com.vidcam.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Subtítulos: tiempos, texto mostrado y orden sin solapes. */
class CaptionMathTest {

    private fun caption(
        id: String = "c1",
        text: String = "hola",
        startMs: Long = 0L,
        endMs: Long = 1_000L,
    ) = Caption(id = id, text = text, startMs = startMs, endMs = endMs)

    @Test
    fun `el subtitulo activo depende del instante`() {
        val captions = listOf(
            caption(id = "a", startMs = 0L, endMs = 1_000L),
            caption(id = "b", startMs = 1_000L, endMs = 2_000L),
        )

        assertEquals("a", CaptionMath.activeAt(captions, 0L)?.id)
        assertEquals("a", CaptionMath.activeAt(captions, 999L)?.id)
        assertEquals("b", CaptionMath.activeAt(captions, 1_000L)?.id)
        assertNull(CaptionMath.activeAt(captions, 2_000L))
    }

    @Test
    fun `un subtitulo sin duracion nunca se ve`() {
        assertNull(CaptionMath.activeAt(listOf(caption(startMs = 500L, endMs = 500L)), 500L))
    }

    @Test
    fun `el texto se muestra en mayusculas si el estilo lo pide`() {
        val style = CaptionStyle(uppercase = true)
        val plain = CaptionStyle(uppercase = false)

        assertEquals("ESTO ES UNA PRUEBA", CaptionMath.displayText(caption(text = "  esto es una prueba "), style))
        assertEquals("esto es", CaptionMath.displayText(caption(text = " esto es "), plain))
    }

    @Test
    fun `normalizar descarta vacios y ordena`() {
        val captions = listOf(
            caption(id = "b", text = "segundo", startMs = 2_000L, endMs = 3_000L),
            caption(id = "a", text = "primero", startMs = 0L, endMs = 1_000L),
            caption(id = "vacio", text = "   ", startMs = 4_000L, endMs = 5_000L),
            caption(id = "cero", text = "sin duración", startMs = 6_000L, endMs = 6_000L),
        )

        val normalized = CaptionMath.normalize(captions, totalMs = 10_000L)

        assertEquals(listOf("a", "b"), normalized.map { it.id })
    }

    @Test
    fun `normalizar corta el solape del anterior`() {
        val captions = listOf(
            caption(id = "a", startMs = 0L, endMs = 2_000L),
            caption(id = "b", startMs = 1_500L, endMs = 3_000L),
        )

        val normalized = CaptionMath.normalize(captions, totalMs = 5_000L)

        assertEquals(1_500L, normalized[0].endMs)
        assertEquals(1_500L, normalized[1].startMs)
    }

    @Test
    fun `normalizar recorta al final del video`() {
        val normalized = CaptionMath.normalize(
            listOf(caption(startMs = 1_000L, endMs = 9_000L)),
            totalMs = 2_000L,
        )

        assertEquals(1_000L, normalized.single().startMs)
        assertEquals(2_000L, normalized.single().endMs)
    }

    @Test
    fun `el estilo se mantiene dentro de los limites`() {
        val style = CaptionStyle(sizeFraction = 0.5f, yFraction = 2f).bounded()

        assertEquals(CaptionStyle.MAX_SIZE_FRACTION, style.sizeFraction, 0.0001f)
        assertEquals(CaptionStyle.MAX_Y_FRACTION, style.yFraction, 0.0001f)
        assertTrue(style.uppercase)
    }
}
