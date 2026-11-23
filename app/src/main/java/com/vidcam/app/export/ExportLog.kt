package com.vidcam.app.export

import com.vidcam.app.model.Project
import java.io.File
import java.util.Locale

/**
 * Utilidades puras para armar y guardar el registro paso a paso de la
 * exportación, de modo que cualquier fallo se entienda sin conectar el teléfono
 * por cable.
 */
object ExportLog {

    const val MAX_LINES = 200

    /**
     * Formatea una línea con el tiempo transcurrido desde el inicio de la
     * exportación (`[+01.25s] mensaje`).
     */
    fun formatLine(elapsedMs: Long, message: String): String {
        val bounded = elapsedMs.coerceAtLeast(0L)
        val seconds = bounded / 1000.0
        val stamp = String.format(Locale.US, "+%05.2fs", seconds)
        return "[$stamp] $message"
    }

    /** Resume la composición de [project] antes de arrancar el motor de vídeo. */
    fun summarizeProject(
        project: Project,
        frameWidth: Int,
        frameHeight: Int,
    ): List<String> = buildList {
        add(
            "Proyecto: ${project.clips.size} clip(s), ${project.backgrounds.size} fondo(s), " +
                "${project.layers.size} capa(s), ${project.captions.size} subtítulo(s)",
        )
        add(
            "Salida: ${frameWidth}x$frameHeight · ${project.outputDurationMs} ms · " +
                "composición=${project.requiresVideoComposition}",
        )
        project.music?.takeIf { project.musicEnabled }?.let { music ->
            add("Música activa: \"${music.title}\" (${music.durationMs} ms, vol=${project.musicVolume})")
        }
        project.voiceover?.takeIf { it.isUsable }?.let { voice ->
            add(
                "Voz en off: ${voice.durationMs} ms (inicio=${voice.startMs} ms, " +
                    "vol=${voice.volume}, limpia=${voice.denoise})",
            )
        }
    }

    /** Escribe el registro completo en [file], reemplazando la corrida anterior. */
    fun writeToFile(file: File, lines: List<String>) {
        try {
            file.parentFile?.mkdirs()
            file.writeText(lines.joinToString("\n"))
        } catch (_: Exception) {
            // El diagnóstico nunca debe interrumpir el flujo principal.
        }
    }
}
