package com.vidcam.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Lectura de respuestas de transcripción: con tiempos del servicio y sin ellos.
 */
class TranscriptParserTest {

    @Test
    fun `lee los segmentos con tiempos de whisper`() {
        val body = """
            {"text":"hola mundo","segments":[
              {"start":0.0,"end":1.4,"text":" hola "},
              {"start":1.4,"end":2.9,"text":" mundo "}
            ]}
        """.trimIndent()

        val segments = TranscriptParser.parse(body, durationMs = 3_000L)

        assertEquals(2, segments.size)
        assertEquals(0L, segments[0].startMs)
        assertEquals(1_400L, segments[0].endMs)
        assertEquals("hola", segments[0].text)
        assertEquals(1_400L, segments[1].startMs)
        assertEquals(2_900L, segments[1].endMs)
    }

    @Test
    fun `acepta chunks como formato alternativo`() {
        val body = """{"chunks":[{"start":0.5,"end":1.5,"text":"una frase"}]}"""

        val segments = TranscriptParser.parse(body, durationMs = 2_000L)

        assertEquals(1, segments.size)
        assertEquals(500L, segments.single().startMs)
        assertEquals("una frase", segments.single().text)
    }

    @Test
    fun `sin tiempos reparte el texto sobre la duracion`() {
        val body = """{"success":true,"text":"uno dos tres cuatro cinco seis siete ocho nueve","language":"es"}"""

        val segments = TranscriptParser.parse(body, durationMs = 9_000L)

        assertTrue(segments.isNotEmpty())
        assertEquals(0L, segments.first().startMs)
        assertEquals(9_000L, segments.last().endMs)
        // Los tramos no se solapan y van en orden.
        for (index in 1 until segments.size) {
            assertTrue(segments[index].startMs >= segments[index - 1].startMs)
            assertTrue(segments[index].startMs <= segments[index - 1].endMs + 1L)
        }
    }

    @Test
    fun `un cuerpo que no es json se usa como texto`() {
        val segments = TranscriptParser.parse("hola desde texto plano", durationMs = 2_000L)

        assertEquals(1, segments.size)
        assertEquals("hola desde texto plano", segments.single().text)
    }

    @Test
    fun `sin texto no hay subtitulos`() {
        assertTrue(TranscriptParser.parse("""{"text":""}""", 5_000L).isEmpty())
        assertTrue(TranscriptParser.parse("", 5_000L).isEmpty())
        assertTrue(TranscriptParser.parse("""{"text":"algo"}""", 0L).isEmpty())
    }

    @Test
    fun `los tramos no se solapan al repartir`() {
        val text = (1..40).joinToString(" ") { "palabra$it" }

        val segments = TranscriptParser.fallback(text, durationMs = 12_000L)

        for (index in 1 until segments.size) {
            assertTrue(segments[index].startMs >= segments[index - 1].endMs - 1L)
        }
        assertEquals(12_000L, segments.last().endMs)
    }
}
