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

    @Test
    fun formatsShortTimeWithTenths() {
        assertEquals("1.5s", formatShortTime(1_500))
        assertEquals("0.0s", formatShortTime(-100))
    }

    @Test
    fun formatsSecondsWithCommaForLayerControls() {
        assertEquals("2,5 s", formatSeconds(2_500))
        assertEquals("30,0 s", formatSeconds(30_000))
        assertEquals("0,0 s", formatSeconds(-500))
    }
}
