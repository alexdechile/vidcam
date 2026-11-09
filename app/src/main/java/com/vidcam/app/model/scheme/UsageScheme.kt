package com.vidcam.app.model.scheme

import com.vidcam.app.model.AnimationSpeed
import com.vidcam.app.model.CaptionStyle
import com.vidcam.app.model.KenBurnsMode
import com.vidcam.app.model.LayerKind
import com.vidcam.app.model.LayerTransitionPreset
import com.vidcam.app.model.PngAnimationPreset
import com.vidcam.app.model.VideoWindow
import kotlinx.serialization.Serializable

/**
 * Tipo o rol de contenido que espera un slot en la plantilla.
 */
@Serializable
enum class SlotKind {
    /** Imagen, foto o sticker transparente (PNG o similar). */
    IMAGE,

    /** Rótulo o titular de texto con tipografía de Google Fonts. */
    TEXT,

    /** GIF o sticker animado. */
    GIF,
}

/**
 * Configuración de fondo para un esquema de uso.
 */
@Serializable
data class SchemeBackground(
    val kenBurns: KenBurnsMode = KenBurnsMode.NINGUNO,
    val kenBurnsSpeed: AnimationSpeed = AnimationSpeed.RAPIDO,
    /** Etapa en la que inicia el fondo (1-indexado). Por defecto 1 (inicio). */
    val stageStart: Int = 1,
    /** Etapa final que cubre el fondo (1-indexado). */
    val stageEnd: Int = 1,
    val label: String = "Fondo",
    /** Identificador de recurso o textura sugerida (ej. "default_gradient"). */
    val placeholder: String? = null,
)

/**
 * Slot o elemento temporal dentro de la coreografía por etapas de un esquema.
 */
@Serializable
data class SchemeSlot(
    val id: String,
    val kind: SlotKind = SlotKind.IMAGE,
    val label: String = "",
    /** Etapa de entrada (1-indexado). */
    val stageStart: Int = 1,
    /** Etapa de salida (1-indexado). La capa permanece activa hasta el final de esta etapa. */
    val stageEnd: Int = 1,
    val x: Float = 0.5f,
    val y: Float = 0.5f,
    val scale: Float = 1f,
    val rotationDeg: Float = 0f,
    val zIndex: Int = 0,
    /** Transición de entrada y salida de la capa. */
    val transition: LayerTransitionPreset = LayerTransitionPreset.NINGUNA,
    val transitionIn: LayerTransitionPreset = transition,
    val transitionOut: LayerTransitionPreset = transition,
    /** Preset de animación procedimental continua o Ken Burns. */
    val animation: PngAnimationPreset = PngAnimationPreset.NINGUNO,
    val animationSpeed: AnimationSpeed = AnimationSpeed.RAPIDO,
    /** Contenido inicial de texto (aplica si [kind] es [SlotKind.TEXT]). */
    val text: String = "",
    val fontName: String? = null,
    val colorArgb: Int = 0xFFFFFFFF.toInt(),
    val fontSizeSp: Float = 42f,
    /** Identificador de sticker o recurso por defecto en la app. */
    val defaultAssetId: String? = null,
) {
    /** Determina la transición efectiva de entrada. */
    val effectiveTransitionIn: LayerTransitionPreset
        get() = if (transitionIn != LayerTransitionPreset.NINGUNA) transitionIn else transition

    /** Determina la transición efectiva de salida. */
    val effectiveTransitionOut: LayerTransitionPreset
        get() = if (transitionOut != LayerTransitionPreset.NINGUNA) transitionOut else transition
}

/**
 * Parámetros de audio sugeridos por el esquema.
 */
@Serializable
data class SchemeAudio(
    val stageStart: Int = 1,
    val stageEnd: Int = 1,
    val fadeInSec: Float = 0f,
    val fadeOutSec: Float = 0f,
    val musicVolume: Float = 1f,
    val duckingAmount: Float = 0f,
)

/**
 * Esquema de uso o plantilla (Template) con coreografía temporal dividida en etapas modulares.
 *
 * Es portable y agnóstico al dispositivo local: no contiene URIs absolutos de archivos privados.
 */
@Serializable
data class UsageScheme(
    val id: String,
    val name: String,
    val description: String = "",
    val version: Int = 1,
    /** Duración total objetivo del esquema en segundos (ej. 15 s). */
    val totalDurationSec: Int = 15,
    /** Cantidad total de etapas en las que se divide la duración (ej. 5 etapas). */
    val stagesCount: Int = 5,
    /** Configuración del fondo con movimiento. */
    val background: SchemeBackground? = null,
    /** Transform y proporción de la ventana de vídeo de cámara/clips. */
    val videoWindow: VideoWindow = VideoWindow(),
    /** Slots de capas de imágenes, textos o stickers. */
    val slots: List<SchemeSlot> = emptyList(),
    /** Ajustes sugeridos de audio. */
    val audio: SchemeAudio? = null,
    /** Estilo predeterminado de subtítulos. */
    val captionStyle: CaptionStyle = CaptionStyle(),
) {
    /** Duración total en milisegundos. */
    val totalDurationMs: Long
        get() = totalDurationSec.coerceAtLeast(1) * 1_000L

    /** Duración de cada etapa individual en milisegundos. */
    val stageDurationMs: Long
        get() = totalDurationMs / stagesCount.coerceAtLeast(1)
}
