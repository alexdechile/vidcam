package com.vidcam.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimelineMathTest {

    private fun clip(
        id: String,
        duration: Long,
        start: Long = 0L,
        end: Long = duration,
    ) = VideoClip(
        id = id,
        uri = "file://$id",
        sourceDurationMs = duration,
        trimStartMs = start,
        trimEndMs = end,
    )

    @Test
    fun totalDurationSumsTrimmedLengths() {
        val clips = listOf(clip("a", 10_000, 2_000, 8_000), clip("b", 5_000))
        assertEquals(11_000L, TimelineMath.totalDuration(clips))
    }

    @Test
    fun clampTrimKeepsValuesInsideSource() {
        val result = TimelineMath.clampTrim(clip("a", 10_000), 12_000, 2_000)
        assertEquals(10_000L, result.trimStartMs)
        assertEquals(10_000L, result.trimEndMs)
    }

    @Test
    fun trimToMaxCapsLength() {
        val result = TimelineMath.trimToMax(clip("a", 60_000))
        assertEquals(0L, result.trimStartMs)
        assertEquals(MAX_DURATION_MS, result.trimEndMs)
        assertEquals(MAX_DURATION_MS, result.trimmedDurationMs)
    }

    @Test
    fun musicWindowRespectsStartAndTimeline() {
        val project = Project(
            clips = listOf(clip("a", 10_000)),
            music = MusicTrack(1L, "x", "t", "a", 30_000),
            musicEnabled = true,
            musicStartMs = 5_000,
        )
        assertEquals(5_000L to 15_000L, TimelineMath.musicWindowMs(project))
        assertFalse(TimelineMath.musicShouldLoop(project))
    }

    @Test
    fun musicLoopsWhenShorterThanTimeline() {
        val project = Project(
            clips = listOf(clip("a", 20_000)),
            music = MusicTrack(1L, "x", "t", "a", 8_000),
            musicEnabled = true,
        )
        assertTrue(TimelineMath.musicShouldLoop(project))
    }

    @Test
    fun musicWindowNullWhenDisabled() {
        val project = Project(
            clips = listOf(clip("a", 5_000)),
            music = MusicTrack(1L, "x", "t", "a", 30_000),
            musicEnabled = false,
        )
        assertNull(TimelineMath.musicWindowMs(project))
    }
}
