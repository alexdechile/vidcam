package com.vidcam.app.model

/**
 * Colocación de la voz en off dentro de la línea de tiempo.
 *
 * La toma se guarda desde el instante en que se presionó "Grabar", así que su
 * posición dentro del vídeo se resuelve al reproducir y al exportar: un
 * [VoiceoverTrack.startMs] positivo agrega silencio delante y uno negativo
 * recorta el comienzo de la toma.
 *
 * Sin dependencias de Android para poder probarlo con tests unitarios.
 */
object VoiceoverMath {

    /** Adelanto máximo de la toma respecto del inicio del vídeo. */
    const val MIN_START_MS = -2_000L

    /** Retardo máximo que se puede dar a la toma. */
    const val MAX_START_MS = 5_000L

    /**
     * Tramo de audio que hay que escribir para colocar la toma en el vídeo:
     * [padMs] de silencio delante, [skipMs] descartados del archivo y [takeMs]
     * de audio.
     */
    data class RenderPlan(
        val padMs: Long,
        val skipMs: Long,
        val takeMs: Long,
    ) {
        val isEmpty: Boolean
            get() = takeMs <= 0L
    }

    /** Ajuste de entrada válido para un vídeo de [projectMs]. */
    fun clampStartMs(startMs: Long, projectMs: Long): Long {
        val top = MAX_START_MS.coerceAtMost(projectMs.coerceAtLeast(0L))
        return startMs.coerceIn(MIN_START_MS, top)
    }

    /** Plan de escritura de la toma, recortado a la duración del vídeo. */
    fun plan(track: VoiceoverTrack, projectMs: Long): RenderPlan {
        val total = projectMs.coerceAtLeast(0L)
        val pad = track.startMs.coerceAtLeast(0L).coerceAtMost(total)
        val skip = (-track.startMs).coerceAtLeast(0L).coerceAtMost(track.durationMs)
        val available = (track.durationMs - skip).coerceAtLeast(0L)
        val take = minOf(available, (total - pad).coerceAtLeast(0L))
        return RenderPlan(padMs = pad, skipMs = skip, takeMs = take)
    }

    /**
     * Posición dentro del archivo de la toma para un instante del vídeo, o
     * `null` si la voz no suena en ese instante. La vista previa lo usa para
     * sincronizar el reproductor de la voz con el del vídeo.
     */
    fun filePositionMs(timelineMs: Long, track: VoiceoverTrack): Long? {
        if (timelineMs < 0L || !track.isUsable) return null
        val pad = track.startMs.coerceAtLeast(0L)
        val skip = (-track.startMs).coerceAtLeast(0L).coerceAtMost(track.durationMs)
        if (timelineMs < pad) return null
        val offset = timelineMs - pad + skip
        return if (offset in 0 until track.durationMs) offset else null
    }
}
