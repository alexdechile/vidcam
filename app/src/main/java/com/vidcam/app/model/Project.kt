package com.vidcam.app.model

import kotlinx.serialization.Serializable

const val MAX_DURATION_MS = 30_000L

/** Velocidades de reproducción disponibles (cámara lenta y rápida). */
val CLIP_SPEEDS: List<Float> = listOf(0.25f, 0.5f, 1f, 2f, 4f)

/** Presets de filtro de color clásicos disponibles por clip. */
@Serializable
enum class ColorFilterPreset {
    NINGUNO,
    SEPIA,
    BLANCO_Y_NEGRO,
    VINTAGE,
    ALTO_CONTRASTE,
}

/**
 * Factor de encuadre ancho (lente ancho).
 *
 * `false` = encuadre normal; `true` = encuadre grupal más abierto (0.65x digital).
 */
@Serializable
enum class WideLensMode {
    NORMAL,
    GRUPO,
}

@Serializable
data class VideoClip(
    val id: String,
    val uri: String,
    val sourceDurationMs: Long,
    val trimStartMs: Long = 0L,
    val trimEndMs: Long = sourceDurationMs,
    /** Velocidad de reproducción del clip: >1 cámara rápida, <1 cámara lenta. */
    val playbackSpeed: Float = 1f,
    /** Filtro de color clásico aplicado al clip. NINGUNO = sin filtro (retrocompatible). */
    val colorFilter: ColorFilterPreset = ColorFilterPreset.NINGUNO,
    /** Encuadre ancho del clip. NORMAL = encuadre normal (retrocompatible). */
    val wideLens: WideLensMode = WideLensMode.NORMAL,
) {
    val trimmedDurationMs: Long
        get() = (trimEndMs - trimStartMs).coerceIn(0L, sourceDurationMs.coerceAtLeast(0L))

    /** Duración que el clip ocupa en la línea de tiempo, según su velocidad. */
    val effectiveDurationMs: Long
        get() = (trimmedDurationMs / playbackSpeed.coerceAtLeast(0.01f).toDouble()).toLong()
}

@Serializable
enum class LayerKind { PNG, TEXT }

/**
 * Fotograma clave del transform de una capa. El tiempo es absoluto dentro de la
 * línea de tiempo del proyecto (no relativo a la ventana de la capa), para que
 * vista previa y exportación coincidan.
 */
@Serializable
data class LayerKeyframe(
    val timeMs: Long,
    val x: Float,
    val y: Float,
    val scale: Float,
    val rotationDeg: Float,
)

@Serializable
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

@Serializable
data class MusicTrack(
    val id: Long,
    val uri: String,
    val title: String,
    val artist: String,
    val durationMs: Long,
)

@Serializable
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

    /** Un proyecto sin clips, capas ni música no tiene nada que guardar. */
    val isEmpty: Boolean
        get() = clips.isEmpty() && layers.isEmpty() && music == null

    fun clipStartMs(index: Int): Long =
        clips.take(index).sumOf { it.effectiveDurationMs }
}
