package com.vidcam.app.model

/**
 * Operaciones puras sobre la línea de tiempo, sin dependencias de Android,
 * para poder verificarlas con pruebas unitarias de JVM.
 */
object TimelineMath {

    /** Suma de los tiempos que cada clip ocupa en la línea de tiempo (duración efectiva). */
    fun totalDuration(clips: List<VideoClip>): Long =
        clips.sumOf { it.effectiveDurationMs }

    /** Ajusta el recorte de un clip manteniendo límites válidos dentro del origen. */
    fun clampTrim(clip: VideoClip, startMs: Long, endMs: Long): VideoClip {
        val max = clip.sourceDurationMs.coerceAtLeast(0L)
        val start = startMs.coerceIn(0L, max)
        val end = endMs.coerceIn(start, max)
        return clip.copy(trimStartMs = start, trimEndMs = end)
    }

    /**
     * Recorta un clip que exceda [maxMs] de línea de tiempo. Como la velocidad
     * amplía o reduce la duración efectiva, el tope de origen se convierte
     * multiplicando por la velocidad del clip.
     */
    fun trimToMax(clip: VideoClip, maxMs: Long = MAX_DURATION_MS): VideoClip {
        if (clip.effectiveDurationMs <= maxMs) return clip
        val maxSource = (maxMs * clip.playbackSpeed.coerceAtLeast(0.01f)).toLong()
            .coerceAtMost(clip.sourceDurationMs.coerceAtLeast(0L))
        return clip.copy(
            trimStartMs = 0L,
            trimEndMs = maxSource.coerceAtLeast(0L),
        )
    }

    /**
     * Ventana [inicio, fin] de la pista de música dentro del archivo original,
     * o null si no hay música activa.
     */
    fun musicWindowMs(project: Project): Pair<Long, Long>? {
        val music = project.music ?: return null
        if (!project.musicEnabled) return null
        val total = totalDuration(project.clips)
        if (total <= 0L) return null
        val maxStart = (music.durationMs - 1).coerceAtLeast(0L)
        val start = project.musicStartMs.coerceIn(0L, maxStart)
        val available = (music.durationMs - start).coerceAtLeast(0L)
        val end = start + minOf(total, available)
        return start to end
    }

    /** Indica si la música debe repetirse en bucle para cubrir el timeline. */
    fun musicShouldLoop(project: Project): Boolean {
        val music = project.music ?: return false
        if (!project.musicEnabled) return false
        val total = totalDuration(project.clips)
        val available = (music.durationMs - project.musicStartMs).coerceAtLeast(0L)
        return total > available
    }
}
