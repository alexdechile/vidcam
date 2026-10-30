package com.vidcam.app.media

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Emparejamiento tiempo→fotograma de un GIF animado. Es la parte pura de
 * [GifAnimation]: no necesita Android ni un archivo real, así que se prueba en
 * la JVM y sirve de contrato para la vista previa y la exportación.
 */
class GifAnimationTest {

    @Test
    fun `el primer fotograma cubre el inicio`() {
        assertEquals(0, GifAnimation.frameIndexFor(0L, 1_000L, 10))
        assertEquals(0, GifAnimation.frameIndexFor(50L, 1_000L, 10))
    }

    @Test
    fun `reparte el bucle entre los fotogramas`() {
        // Bucle de 1 s con 10 fotogramas: cada uno dura 100 ms.
        assertEquals(4, GifAnimation.frameIndexFor(499L, 1_000L, 10))
        assertEquals(5, GifAnimation.frameIndexFor(500L, 1_000L, 10))
        assertEquals(9, GifAnimation.frameIndexFor(999L, 1_000L, 10))
    }

    @Test
    fun `el gif se repite despues de un bucle`() {
        assertEquals(0, GifAnimation.frameIndexFor(1_000L, 1_000L, 10))
        assertEquals(5, GifAnimation.frameIndexFor(1_500L, 1_000L, 10))
        assertEquals(5, GifAnimation.frameIndexFor(3_250L, 2_000L, 8))
    }

    @Test
    fun `sin bucle o con un solo fotograma siempre es el primero`() {
        assertEquals(0, GifAnimation.frameIndexFor(12_345L, 0L, 8))
        assertEquals(0, GifAnimation.frameIndexFor(12_345L, 1_000L, 1))
    }

    @Test
    fun `no se sale del rango de fotogramas`() {
        assertEquals(true, GifAnimation.frameIndexFor(999L, 1_000L, 3) in 0..2)
        assertEquals(true, GifAnimation.frameIndexFor(987_654_321L, 1_000L, 7) in 0..6)
    }
}
