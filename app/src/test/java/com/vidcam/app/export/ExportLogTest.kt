package com.vidcam.app.export

import com.vidcam.app.model.BackgroundLayer
import com.vidcam.app.model.KenBurnsMode
import com.vidcam.app.model.LayerKind
import com.vidcam.app.model.MusicTrack
import com.vidcam.app.model.OverlayLayer
import com.vidcam.app.model.Project
import com.vidcam.app.model.VideoClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ExportLogTest {

    @Test
    fun `formatLine usa segundos con dos decimales y acota negativos`() {
        assertEquals("[+00.00s] Inicio", ExportLog.formatLine(-50L, "Inicio"))
        assertEquals("[+01.25s] Preparando", ExportLog.formatLine(1250L, "Preparando"))
        assertEquals("[+12.04s] Listo", ExportLog.formatLine(12040L, "Listo"))
    }

    @Test
    fun `summarizeProject resume clips fondos capas y musica activa`() {
        val project = Project(
            clips = listOf(VideoClip(id = "c1", uri = "file:///a.mp4", sourceDurationMs = 4000L)),
            backgrounds = listOf(
                BackgroundLayer(
                    id = "b1",
                    uri = "file:///bg.png",
                    endMs = 4000L,
                    kenBurns = KenBurnsMode.AUTOMATICO,
                ),
            ),
            layers = listOf(OverlayLayer(id = "l1", kind = LayerKind.TEXT, text = "Hola")),
            music = MusicTrack(
                id = 1L,
                uri = "content://audio/1",
                title = "Cueca",
                artist = "Demo",
                durationMs = 15000L,
            ),
            musicEnabled = true,
            musicVolume = 0.8f,
        )

        val summary = ExportLog.summarizeProject(project, 1080, 1920)
        assertTrue(summary.any { "1 clip(s), 1 fondo(s), 1 capa(s)" in it })
        assertTrue(summary.any { "1080x1920 · 4000 ms · composición=true" in it })
        assertTrue(summary.any { "Música activa: \"Cueca\"" in it })
    }

    @Test
    fun `writeToFile guarda y reemplaza el contenido anterior`() {
        val temp = File.createTempFile("vidcam-export-log", ".log")
        temp.deleteOnExit()
        ExportLog.writeToFile(temp, listOf("uno", "dos"))
        assertEquals("uno\ndos", temp.readText())
        ExportLog.writeToFile(temp, listOf("nuevo"))
        assertEquals("nuevo", temp.readText())
    }
}
