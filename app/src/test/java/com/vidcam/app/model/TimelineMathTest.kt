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
        val result = TimelineMath.trimToMax(clip("a", 600_000))
        assertEquals(0L, result.trimStartMs)
        assertEquals(MAX_DURATION_MS, result.trimEndMs)
        assertEquals(MAX_DURATION_MS, result.trimmedDurationMs)
    }

    @Test
    fun trimToMaxLeavesClipsUnderFiveMinutesUntouched() {
        val result = TimelineMath.trimToMax(clip("a", 60_000))
        assertEquals(0L, result.trimStartMs)
        assertEquals(60_000L, result.trimEndMs)
        assertEquals(60_000L, result.trimmedDurationMs)
    }

    @Test
    fun effectiveDurationIsTrimmedDividedBySpeed() {
        val c = clip("a", 10_000, 1_000, 9_000).copy(playbackSpeed = 4f)
        assertEquals(2_000L, c.effectiveDurationMs)
    }

    @Test
    fun filtrosYLenteNoCambianDuracion() {
        val base = clip("a", 10_000, 1_000, 9_000)
        val filtrada = base.copy(
            colorFilter = ColorFilterPreset.SEPIA,
            wideLens = WideLensMode.GRUPO,
        )
        assertEquals(base.effectiveDurationMs, filtrada.effectiveDurationMs)
        assertEquals(
            TimelineMath.totalDuration(listOf(base)),
            TimelineMath.totalDuration(listOf(filtrada)),
        )
    }

    @Test
    fun totalDurationUsesEffectiveDurations() {
        val clips = listOf(
            clip("a", 10_000).copy(playbackSpeed = 2f),
            clip("b", 10_000).copy(playbackSpeed = 0.5f),
        )
        assertEquals(25_000L, TimelineMath.totalDuration(clips))
    }

    @Test
    fun trimToMaxConvertsBudgetToSourceWithSlowSpeed() {
        val slow = clip("a", 600_000).copy(playbackSpeed = 0.5f)
        val result = TimelineMath.trimToMax(slow, MAX_DURATION_MS)
        assertEquals(150_000L, result.trimmedDurationMs)
        assertEquals(MAX_DURATION_MS, result.effectiveDurationMs)
    }

    @Test
    fun trimToMaxAllowsMoreSourceWithFastSpeed() {
        val fast = clip("a", 2_000_000).copy(playbackSpeed = 4f)
        val result = TimelineMath.trimToMax(fast, MAX_DURATION_MS)
        assertEquals(1_200_000L, result.trimmedDurationMs)
        assertEquals(MAX_DURATION_MS, result.effectiveDurationMs)
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

    @Test
    fun clipsLargosSumanDuracionCompletaMasAllaDeCincoMinutos() {
        val clips = listOf(clip("a", 420_000L), clip("b", 180_000L))
        assertEquals(600_000L, TimelineMath.totalDuration(clips))
    }

    @Test
    fun clampTrimSoportaClipsExtensosDeDiezMinutos() {
        val clipDiezMin = clip("a", 600_000L)
        val recortado = TimelineMath.clampTrim(clipDiezMin, 60_000L, 540_000L)
        assertEquals(60_000L, recortado.trimStartMs)
        assertEquals(540_000L, recortado.trimEndMs)
        assertEquals(480_000L, recortado.trimmedDurationMs)
    }

    @Test
    fun musicaCubreTimelineExtensoDeOchoMinutos() {
        val project = Project(
            clips = listOf(clip("a", 480_000L)),
            music = MusicTrack(1L, "x", "t", "a", 600_000L),
            musicEnabled = true,
            musicStartMs = 0L,
        )
        assertEquals(0L to 480_000L, TimelineMath.musicWindowMs(project))
        assertFalse(TimelineMath.musicShouldLoop(project))
    }
}
