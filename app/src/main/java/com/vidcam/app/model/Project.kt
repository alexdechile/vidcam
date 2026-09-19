package com.vidcam.app.model

const val MAX_DURATION_MS = 30_000L

data class VideoClip(
    val id: String,
    val uri: String,
    val sourceDurationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = sourceDurationMs,
) {
    val trimmedDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceIn(0L, sourceDurationMs.coerceAtLeast(0L))
}

enum class LayerKind { PNG, TEXT }

/**
 * Fotograma clave del transform de una capa. El tiempo es absoluto dentro de la
 * línea de tiempo del proyecto (no relativo a la ventana de la capa), para que
 * vista previa y exportación coincidan.
 */
data class LayerKeyframe(
    val timeMs: Long,
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotationDeg: Float,
)

data class OverlayLayer(
    val id: String,
    val kind: LayerKind,
    val startMs: Long = 0L,
    val endMs: Long = MAX_DURATION_MS,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val uri: String? = null,
    val label: String = "",
    val text: String = "",
    val fontName: String? = null,
    val colorArgb: Int = 0xFFFFFFFF.toInt(),
    val fontSizeSp: Float = 42f,
    /**
     * Animación de la capa. Vacío significa transform fijo, como antes de que
     * existiera la grabación de movimiento.
     */
    val keyframes: List<LayerKeyframe> = emptyList(),
)

data class MusicTrack(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
)

data class Project(
    val clips: List<VideoClip> = emptyList(),
    val music: MusicTrack? = null,
    val musicEnabled: Boolean = false,
    val musicStartMs: Long = 0L,
    val originalAudioMuted: Boolean = false,
    val layers: List<OverlayLayer> = emptyList(),
) {
    val totalDurationMs: Long
        get() = TimelineMath.totalDuration(clips)

    val isOverLimit: Boolean
        get() = totalDurationMs > MAX_DURATION_MS

    fun clipStartMs(index: Int): Long =
        clips.take(index).sumOf { it.trimmedDurationMs }
}
