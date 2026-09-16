package com.vidcam.app.util

import org.junit.Assert.assertEquals
import org.junit.Test

class TimeUtilsTest {

    @Test
    fun formatsSeconds() {
        assertEquals("0:05", formatDuration(5_000))
    }

    @Test
    fun formatsMinutes() {
        assertEquals("1:05", formatDuration(65_000))
    }

    @Test
    fun clampsNegativeValues() {
        assertEquals("0:00", formatDuration(-1_000))
    }
}
