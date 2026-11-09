package com.vidcam.app.model.scheme

import com.vidcam.app.model.AnimationSpeed
import com.vidcam.app.model.CaptionStyle
import com.vidcam.app.model.KenBurnsMode
import com.vidcam.app.model.LayerTransitionPreset
import com.vidcam.app.model.PngAnimationPreset
import com.vidcam.app.model.VideoWindow
import com.vidcam.app.model.VideoWindowAspect

/**
 * Catálogo de plantillas y esquemas de uso predefinidos de fábrica en VidCam.
 */
object FactorySchemes {

    /**
     * Esquema emblemático: 5 etapas en cascada de 3 segundos (15 s totales).
     *
     * - Fondo continuo con paneo activo (etapas 1 a 5).
     * - Imagen 1 (Gancho): etapas 1 a 2 (0s a 6s).
     * - Imagen 2 (Desarrollo): etapas 2 a 4 (3s a 12s).
     * - Imagen 3 (Cierre): etapas 3 a 5 (6s a 15s).
     * - Audio completo (etapas 1 a 5).
     */
    val CASCADE_5_STAGES_15S = UsageScheme(
        id = "factory_cascade_15s",
        name = "5 Etapas en Cascada (15s)",
        description = "Coreografía de 3 imágenes solapadas por etapas sobre fondo dinámico.",
        version = 1,
        totalDurationSec = 15,
        stagesCount = 5,
        background = SchemeBackground(
            kenBurns = KenBurnsMode.PANEO,
            kenBurnsSpeed = AnimationSpeed.RAPIDO,
            stageStart = 1,
            stageEnd = 5,
            label = "Fondo dinámico continuo",
        ),
        videoWindow = VideoWindow(
            aspect = VideoWindowAspect.ORIGINAL,
        ),
        slots = listOf(
            SchemeSlot(
                id = "slot_img1",
                kind = SlotKind.IMAGE,
                label = "Imagen 1 (Intro/Gancho)",
                stageStart = 1,
                stageEnd = 2,
                x = 0.5f,
                y = 0.48f,
                scale = 0.9f,
                transitionIn = LayerTransitionPreset.POP,
                transitionOut = LayerTransitionPreset.FUNDIDO,
                animation = PngAnimationPreset.FLOTAR,
                animationSpeed = AnimationSpeed.RAPIDO,
            ),
            SchemeSlot(
                id = "slot_img2",
                kind = SlotKind.IMAGE,
                label = "Imagen 2 (Desarrollo)",
                stageStart = 2,
                stageEnd = 4,
                x = 0.5f,
                y = 0.48f,
                scale = 0.95f,
                transitionIn = LayerTransitionPreset.DESLIZAR,
                transitionOut = LayerTransitionPreset.FUNDIDO,
                animation = PngAnimationPreset.PULSO,
                animationSpeed = AnimationSpeed.RAPIDO,
            ),
            SchemeSlot(
                id = "slot_img3",
                kind = SlotKind.IMAGE,
                label = "Imagen 3 (Cierre/Llamada)",
                stageStart = 3,
                stageEnd = 5,
                x = 0.5f,
                y = 0.48f,
                scale = 0.9f,
                transitionIn = LayerTransitionPreset.POP,
                transitionOut = LayerTransitionPreset.FUNDIDO,
                animation = PngAnimationPreset.VAIVEN,
                animationSpeed = AnimationSpeed.RAPIDO,
            ),
        ),
        audio = SchemeAudio(
            stageStart = 1,
            stageEnd = 5,
            fadeInSec = 0.5f,
            fadeOutSec = 1.0f,
            musicVolume = 1f,
            duckingAmount = 0.5f,
        ),
        captionStyle = CaptionStyle(
            sizeFraction = 0.045f,
            yFraction = 0.85f,
            uppercase = true,
        ),
    )

    /**
     * Plantilla de Reacción PiP (15s): Ventana cuadrada flotante, titular superior y logo.
     */
    val PIP_REACTION_15S = UsageScheme(
        id = "factory_pip_reaction_15s",
        name = "Reacción PiP con Titular (15s)",
        description = "Vídeo centrado en formato cuadrado con fondo suave y rótulo superior.",
        version = 1,
        totalDurationSec = 15,
        stagesCount = 5,
        background = SchemeBackground(
            kenBurns = KenBurnsMode.FLOTAR,
            kenBurnsSpeed = AnimationSpeed.NORMAL,
            stageStart = 1,
            stageEnd = 5,
            label = "Fondo ambiente",
        ),
        videoWindow = VideoWindow(
            aspect = VideoWindowAspect.CUADRADO,
            x = 0.5f,
            y = 0.42f,
            scale = 0.85f,
        ),
        slots = listOf(
            SchemeSlot(
                id = "slot_title",
                kind = SlotKind.TEXT,
                label = "Titular Superior",
                stageStart = 1,
                stageEnd = 5,
                x = 0.5f,
                y = 0.12f,
                scale = 1f,
                text = "¡MIRA ESTO!",
                fontName = "Bangers",
                fontSizeSp = 44f,
                transitionIn = LayerTransitionPreset.POP,
                animation = PngAnimationPreset.PULSO,
            ),
            SchemeSlot(
                id = "slot_badge",
                kind = SlotKind.IMAGE,
                label = "Insignia / Sticker",
                stageStart = 2,
                stageEnd = 5,
                x = 0.82f,
                y = 0.76f,
                scale = 0.6f,
                transitionIn = LayerTransitionPreset.DESLIZAR,
                animation = PngAnimationPreset.VAIVEN,
            ),
        ),
    )

    /**
     * Plantilla Noticia Flash (10s): 4 etapas breves con zoom diagonal y dos bloques informativos.
     */
    val NEWS_FLASH_10S = UsageScheme(
        id = "factory_news_flash_10s",
        name = "Noticia Flash (10s)",
        description = "4 etapas rápidas para alertas o noticias de impacto.",
        version = 1,
        totalDurationSec = 10,
        stagesCount = 4,
        background = SchemeBackground(
            kenBurns = KenBurnsMode.DIAGONAL,
            kenBurnsSpeed = AnimationSpeed.TURBO,
            stageStart = 1,
            stageEnd = 4,
            label = "Fondo dinámico",
        ),
        slots = listOf(
            SchemeSlot(
                id = "slot_headline",
                kind = SlotKind.TEXT,
                label = "Titular urgente",
                stageStart = 1,
                stageEnd = 2,
                x = 0.5f,
                y = 0.25f,
                text = "ÚLTIMO MINUTO",
                fontSizeSp = 48f,
                transitionIn = LayerTransitionPreset.POP,
                animation = PngAnimationPreset.TEMBLOR,
            ),
            SchemeSlot(
                id = "slot_detail",
                kind = SlotKind.IMAGE,
                label = "Gráfico / Foto",
                stageStart = 2,
                stageEnd = 4,
                x = 0.5f,
                y = 0.55f,
                scale = 0.95f,
                transitionIn = LayerTransitionPreset.DESLIZAR,
                animation = PngAnimationPreset.PULSO,
            ),
        ),
    )

    /** Lista completa de presets de fábrica ordenados para el usuario. */
    val ALL: List<UsageScheme> = listOf(
        CASCADE_5_STAGES_15S,
        PIP_REACTION_15S,
        NEWS_FLASH_10S,
    )
}
