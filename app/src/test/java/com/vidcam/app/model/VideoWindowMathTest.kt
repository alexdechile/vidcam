package com.vidcam.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoWindowMathTest {

    private fun background(id: String, startMs: Long, endMs: Long) =
        BackgroundLayer(id = id, uri = "file:///fondo-$id.png", startMs = startMs, endMs = endMs)

    @Test
    fun `backgroundAt devuelve el fondo del tramo y null en los huecos`() {
        val backgrounds = listOf(background("a", 0L, 1_000L), background("b", 2_000L, 3_000L))

        assertEquals("a", VideoWindowMath.backgroundAt(backgrounds, 0L)?.id)
        assertEquals("a", VideoWindowMath.backgroundAt(backgrounds, 999L)?.id)
        assertNull(VideoWindowMath.backgroundAt(backgrounds, 1_000L))
        assertEquals("b", VideoWindowMath.backgroundAt(backgrounds, 2_500L)?.id)
        assertNull(VideoWindowMath.backgroundAt(backgrounds, 3_000L))
    }

    @Test
    fun `trimOverlaps recorta el final del fondo existente`() {
        val result = VideoWindowMath.trimOverlaps(
            backgrounds = listOf(background("viejo", 0L, 5_000L)),
            incoming = background("nuevo", 2_000L, 8_000L),
            totalMs = 10_000L,
        )

        assertEquals(2, result.size)
        assertEquals(0L, result[0].startMs)
        assertEquals(2_000L, result[0].endMs)
        assertEquals(2_000L, result[1].startMs)
        assertEquals(8_000L, result[1].endMs)
    }

    @Test
    fun `trimOverlaps recorta el inicio del fondo existente`() {
        val result = VideoWindowMath.trimOverlaps(
            backgrounds = listOf(background("viejo", 3_000L, 9_000L)),
            incoming = background("nuevo", 0L, 5_000L),
            totalMs = 10_000L,
        )

        assertEquals(2, result.size)
        assertEquals(0L, result[0].startMs)
        assertEquals(5_000L, result[0].endMs)
        assertEquals(5_000L, result[1].startMs)
        assertEquals(9_000L, result[1].endMs)
    }

    @Test
    fun `trimOverlaps descarta un fondo contenido y limita al total`() {
        val result = VideoWindowMath.trimOverlaps(
            backgrounds = listOf(background("contenido", 2_000L, 4_000L)),
            incoming = background("nuevo", -500L, 20_000L),
            totalMs = 10_000L,
        )

        assertEquals(1, result.size)
        assertEquals("nuevo", result[0].id)
        assertEquals(0L, result[0].startMs)
        assertEquals(10_000L, result[0].endMs)
    }

    @Test
    fun `backgroundSegments rellena los huecos con null`() {
        val segments = VideoWindowMath.backgroundSegments(
            backgrounds = listOf(background("a", 2_000L, 4_000L), background("b", 6_000L, 8_000L)),
            totalMs = 10_000L,
        )

        assertEquals(5, segments.size)
        assertNull(segments[0].background)
        assertEquals(0L, segments[0].startMs)
        assertEquals(2_000L, segments[0].endMs)
        assertEquals("a", segments[1].background?.id)
        assertNull(segments[2].background)
        assertEquals("b", segments[3].background?.id)
        assertNull(segments[4].background)
        assertEquals(10_000L, segments[4].endMs)
    }

    @Test
    fun `windowSize ocupa el lienzo completo con original y cabe en cuadrado`() {
        val original = VideoWindowMath.windowSize(1080f, 1920f, 9f / 16f, 1f)
        assertEquals(1080f, original.first, 0.01f)
        assertEquals(1920f, original.second, 0.01f)

        val cuadrado = VideoWindowMath.windowSize(1080f, 1920f, 1f, 1f)
        assertEquals(1080f, cuadrado.first, 0.01f)
        assertEquals(1080f, cuadrado.second, 0.01f)

        val mitad = VideoWindowMath.windowSize(1080f, 1920f, 1f, 0.5f)
        assertEquals(540f, mitad.first, 0.01f)
        assertEquals(540f, mitad.second, 0.01f)
    }

    @Test
    fun `windowSize limita la escala entre 0_2 y 1`() {
        val minimo = VideoWindowMath.windowSize(1080f, 1920f, 1f, 0.01f)
        assertEquals(216f, minimo.first, 0.01f)

        val maximo = VideoWindowMath.windowSize(1080f, 1920f, 1f, 5f)
        assertEquals(1080f, maximo.first, 0.01f)
    }

    @Test
    fun `hitTest respeta la rotacion de la ventana`() {
        assertTrue(
            VideoWindowMath.hitTest(
                centerX = 0f,
                centerY = 0f,
                halfWidth = 100f,
                halfHeight = 50f,
                rotationDeg = 90f,
                pointX = 0f,
                pointY = 90f,
            ),
        )
        assertFalse(
            VideoWindowMath.hitTest(
                centerX = 0f,
                centerY = 0f,
                halfWidth = 100f,
                halfHeight = 50f,
                rotationDeg = 90f,
                pointX = 90f,
                pointY = 0f,
            ),
        )
    }

    @Test
    fun `croppedNativeSize recorta al centro segun la forma`() {
        val cuadradoDesdeAncho = VideoWindowMath.croppedNativeSize(1920, 1080, 1f)
        assertEquals(1080f, cuadradoDesdeAncho.first, 0.01f)
        assertEquals(1080f, cuadradoDesdeAncho.second, 0.01f)

        val cuadradoDesdeAlto = VideoWindowMath.croppedNativeSize(1080, 1920, 1f)
        assertEquals(1080f, cuadradoDesdeAlto.first, 0.01f)
        assertEquals(1080f, cuadradoDesdeAlto.second, 0.01f)

        val vertical = VideoWindowMath.croppedNativeSize(1080, 1920, 9f / 16f)
        assertEquals(1080f, vertical.first, 0.01f)
        assertEquals(1920f, vertical.second, 0.01f)
    }

    @Test
    fun `compositorScale convierte la escala relativa al tamano nativo`() {
        val escala = VideoWindowMath.compositorScale(
            frameWidth = 1080f,
            frameHeight = 1920f,
            nativeWidth = 1080f,
            nativeHeight = 1080f,
            aspect = 1f,
            scale = 0.5f,
        )
        assertEquals(0.5f, escala, 0.01f)
    }

    @Test
    fun `un proyecto sin fondos ni ventana no requiere composicion`() {
        val project = Project()
        assertTrue(project.backgrounds.isEmpty())
        assertTrue(project.videoWindow.isDefault)
        assertFalse(project.requiresVideoComposition)
        assertTrue(project.isEmpty)
    }
}
