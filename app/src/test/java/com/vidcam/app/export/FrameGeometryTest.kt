package com.vidcam.app.export

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameGeometryTest {

    @Test
    fun fitWideFrameLimitedByWidth() {
        // Marco 16:9 dentro de un hueco que es más alto relativamente.
        val (w, h) = FrameGeometry.fit(1920f, 1080f, 1000f, 1000f)
        assertEquals(1000f, w, 0.001f)
        assertEquals(562.5f, h, 0.001f)
    }

    @Test
    fun fitVerticalFrameLimitedByHeight() {
        // Marco 9:16 (vertical) dentro de un hueco ancho.
        val (w, h) = FrameGeometry.fit(1080f, 1920f, 2000f, 1000f)
        assertEquals(562.5f, w, 0.001f)
        assertEquals(1000f, h, 0.001f)
    }

    @Test
    fun fitKeepsAspectRatioWhenWidthLimits() {
        val (w, h) = FrameGeometry.fit(1080f, 1920f, 540f, 4000f)
        assertEquals(540f, w, 0.001f)
        assertEquals(960f, h, 0.001f)
    }

    @Test
    fun fitExactlyMatchingFrameDoesNotChange() {
        val (w, h) = FrameGeometry.fit(1080f, 1920f, 1080f, 1920f)
        assertEquals(1080f, w, 0.001f)
        assertEquals(1920f, h, 0.001f)
    }

    @Test
    fun fitWithInvalidSizeFallsBackToAvailable() {
        val (w, h) = FrameGeometry.fit(0f, 0f, 800f, 600f)
        assertEquals(800f, w, 0.001f)
        assertEquals(600f, h, 0.001f)
    }
}
