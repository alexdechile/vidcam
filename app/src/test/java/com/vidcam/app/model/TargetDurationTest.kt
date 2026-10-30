package com.vidcam.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Duración fijada de antemano y su efecto en el vídeo resultante: sirve tanto
 * para el tope de la grabación como para la duración de un proyecto hecho solo
 * con PNG (sin clips).
 */
class TargetDurationTest {

    private fun clip(id: String, durationMs: Long) = VideoClip(
        id = id,
        uri = "file:///$id.mp4",
        sourceDurationMs = durationMs,
    )

    @Test
    fun `sin clips la duracion de salida es la fijada`() {
        val project = Project(targetDurationMs = 5_000L)
        assertEquals(5_000L, project.outputDurationMs)
        assertEquals(0L, project.totalDurationMs)
    }

    @Test
    fun `con clips manda la linea de tiempo`() {
        val project = Project(
            clips = listOf(clip("a", 4_000L), clip("b", 3_000L)),
            targetDurationMs = 15_000L,
        )
        assertEquals(7_000L, project.outputDurationMs)
    }

    @Test
    fun `la duracion fijada se acota al rango valido`() {
        assertEquals(
            MIN_TARGET_DURATION_MS,
            Project(targetDurationMs = 10L).outputDurationMs,
        )
        assertEquals(
            MAX_DURATION_MS,
            Project(targetDurationMs = 120_000L).outputDurationMs,
        )
    }

    @Test
    fun `la grabacion se corta en la duracion fijada`() {
        assertEquals(10_000L, TimelineMath.recordingLimitMs(10_000L, 0L))
        // Ya hay 25 s grabados: solo caben 5 s del proyecto de 30 s.
        assertEquals(5_000L, TimelineMath.recordingLimitMs(10_000L, 25_000L))
        // Sin espacio, no se graba.
        assertEquals(0L, TimelineMath.recordingLimitMs(10_000L, 30_000L))
        assertEquals(0L, TimelineMath.recordingLimitMs(10_000L, 31_000L))
    }

    @Test
    fun `solo se puede exportar si hay clips o imagenes`() {
        assertFalse(Project().canExport)
        assertTrue(Project(clips = listOf(clip("a", 1_000L))).canExport)
        assertTrue(
            Project(
                layers = listOf(
                    OverlayLayer(id = "l1", kind = LayerKind.PNG, uri = "file:///l1.png"),
                ),
            ).canExport,
        )
        assertTrue(
            Project(
                backgrounds = listOf(BackgroundLayer(id = "b1", uri = "file:///b1.png")),
            ).canExport,
        )
    }

    @Test
    fun `la musica cubre el video de solo imagenes`() {
        val track = MusicTrack(
            id = 1L,
            uri = "file:///tema.mp3",
            title = "Tema",
            artist = "Artista",
            durationMs = 120_000L,
        )
        val project = Project(
            music = track,
            musicEnabled = true,
            targetDurationMs = 5_000L,
        )
        assertEquals(0L to 5_000L, TimelineMath.musicWindowMs(project))
        assertFalse(TimelineMath.musicShouldLoop(project))
    }

    @Test
    fun `la musica se repite si no alcanza para el video de imagenes`() {
        val track = MusicTrack(
            id = 1L,
            uri = "file:///corto.mp3",
            title = "Corto",
            artist = "Artista",
            durationMs = 3_000L,
        )
        val project = Project(
            music = track,
            musicEnabled = true,
            targetDurationMs = 10_000L,
        )
        assertTrue(TimelineMath.musicShouldLoop(project))
    }

    @Test
    fun `un proyecto antiguo mantiene el tope de 30 segundos`() {
        // Sin el campo en el JSON, kotlinx.serialization usa el valor por defecto.
        assertEquals(MAX_DURATION_MS, Project().targetDurationMs)
    }
}
