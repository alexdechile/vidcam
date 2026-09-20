package com.vidcam.app.data

import com.vidcam.app.model.LayerKeyframe
import com.vidcam.app.model.LayerKind
import com.vidcam.app.model.MusicTrack
import com.vidcam.app.model.OverlayLayer
import com.vidcam.app.model.Project
import com.vidcam.app.model.VideoClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class ProjectStoreTest {

    @get:Rule
    val temp = TemporaryFolder()

    private val store: ProjectStore by lazy { ProjectStore(temp.root.resolve("projects")) }

    private fun fullProject() = Project(
        clips = listOf(
            VideoClip(
                id = "clip-1",
                uri = "file:///data/media/clip-1.mp4",
                sourceDurationMs = 8_000L,
                trimStartMs = 500L,
                trimEndMs = 6_500L,
            ),
            VideoClip(id = "clip-2", uri = "file:///data/media/clip-2.mp4", sourceDurationMs = 3_000L),
        ),
        music = MusicTrack(
            id = 42L,
            uri = "content://media/external/audio/media/42",
            title = "Pista",
            artist = "Artista",
            durationMs = 120_000L,
        ),
        musicEnabled = true,
        musicStartMs = 1_200L,
        originalAudioMuted = true,
        layers = listOf(
            OverlayLayer(
                id = "layer-png",
                kind = LayerKind.PNG,
                uri = "asset://stickers/star.png",
                label = "Estrella",
                startMs = 0L,
                endMs = 9_000L,
                x = 0.25f,
                y = 0.75f,
                scale = 1.5f,
                rotationDeg = 30f,
                keyframes = listOf(
                    LayerKeyframe(timeMs = 0L, x = 0.25f, y = 0.75f, scale = 1f, rotationDeg = 0f),
                    LayerKeyframe(timeMs = 2_000L, x = 0.5f, y = 0.5f, scale = 2f, rotationDeg = 45f),
                ),
            ),
            OverlayLayer(
                id = "layer-text",
                kind = LayerKind.TEXT,
                text = "Hola",
                fontName = "Roboto",
                colorArgb = 0xFF00FF00.toInt(),
                fontSizeSp = 56f,
            ),
        ),
    )

    @Test
    fun `guarda y recupera un proyecto completo sin perder campos`() {
        val saved = SavedProject(
            id = "p1",
            name = "Mi proyecto",
            createdAtMs = 1_000L,
            updatedAtMs = 2_000L,
            project = fullProject(),
        )

        assertTrue(store.save(saved))
        assertEquals(saved, store.load("p1"))
        assertEquals(fullProject(), store.load("p1")?.project)
    }

    @Test
    fun `sobrescribe el mismo id sin duplicar`() {
        val original = SavedProject("p1", "Uno", 0L, 10L, Project())
        val updated = original.copy(name = "Dos", updatedAtMs = 20L, project = fullProject())

        assertTrue(store.save(original))
        assertTrue(store.save(updated))

        assertEquals(1, store.list().size)
        assertEquals("Dos", store.load("p1")?.name)
    }

    @Test
    fun `lista del mas reciente al mas antiguo`() {
        store.save(SavedProject("viejo", "Viejo", 0L, 10L, Project()))
        store.save(SavedProject("nuevo", "Nuevo", 0L, 30L, Project()))
        store.save(SavedProject("medio", "Medio", 0L, 20L, Project()))

        assertEquals(listOf("nuevo", "medio", "viejo"), store.list().map { it.id })
    }

    @Test
    fun `borra un proyecto guardado`() {
        store.save(SavedProject("p1", "Uno", 0L, 0L, Project()))
        assertTrue(store.delete("p1"))
        assertNull(store.load("p1"))
        assertTrue(store.list().isEmpty())
    }

    @Test
    fun `un json corrupto no rompe el listado`() {
        store.save(SavedProject("bueno", "Bueno", 0L, 0L, Project()))
        temp.root.resolve("projects").resolve("roto.json").writeText("{ esto no es json")

        assertEquals(listOf("bueno"), store.list().map { it.id })
        assertFalse(store.list().isEmpty())
    }

    @Test
    fun `la carpeta se crea sola en el primer guardado`() {
        assertFalse(temp.root.resolve("projects").exists())
        assertTrue(store.save(SavedProject("p1", "Uno", 0L, 0L, Project())))
        assertTrue(temp.root.resolve("projects").isDirectory)
    }
}
