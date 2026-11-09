package com.vidcam.app.model.scheme

import com.vidcam.app.model.BackgroundLayer
import com.vidcam.app.model.LayerKind
import com.vidcam.app.model.OverlayLayer
import com.vidcam.app.model.Project
import java.util.UUID

/**
 * Lógica matemática de conversión de etapas temporales y adaptación bidireccional
 * entre [UsageScheme] y [Project].
 *
 * Es pura y desacoplada del framework de Android, lo que permite probarla directamente
 * con tests unitarios en la JVM.
 */
object SchemeTiming {

    /**
     * Calcula el intervalo [startMs, endMs] de un elemento basado en sus etapas
     * de inicio y fin (1-indexado).
     */
    fun calculateIntervalMs(
        stageStart: Int,
        stageEnd: Int,
        stagesCount: Int,
        totalDurationMs: Long,
    ): Pair<Long, Long> {
        val count = stagesCount.coerceAtLeast(1)
        val stageDuration = totalDurationMs.coerceAtLeast(1L) / count
        val startStage = stageStart.coerceIn(1, count)
        val endStage = stageEnd.coerceIn(startStage, count)

        val startMs = (startStage - 1) * stageDuration
        val endMs = if (endStage == count) {
            totalDurationMs
        } else {
            endStage * stageDuration
        }

        return Pair(startMs, endMs.coerceAtLeast(startMs + 1L))
    }

    /**
     * Deduce la etapa más cercana (1..stagesCount) a partir de un tiempo en milisegundos.
     */
    fun msToStage(timeMs: Long, stagesCount: Int, totalDurationMs: Long, roundUp: Boolean = false): Int {
        val count = stagesCount.coerceAtLeast(1)
        val total = totalDurationMs.coerceAtLeast(1L)
        val stageDuration = total.toDouble() / count
        val raw = timeMs.coerceIn(0L, total).toDouble() / stageDuration
        val stage = if (roundUp) kotlin.math.ceil(raw).toInt() else kotlin.math.floor(raw).toInt() + 1
        return stage.coerceIn(1, count)
    }

    /**
     * Aplica la coreografía y estructura de un [UsageScheme] sobre un [Project].
     *
     * @param scheme Plantilla a aplicar.
     * @param current Proyecto actual sobre el que se aplica.
     * @param keepClips Si es `true`, conserva los clips de vídeo grabados o importados.
     */
    fun applyToProject(
        scheme: UsageScheme,
        current: Project = Project(),
        keepClips: Boolean = true,
    ): Project {
        val totalMs = scheme.totalDurationMs
        val count = scheme.stagesCount

        // 1. Convertir slots a OverlayLayers
        val newLayers = scheme.slots.mapIndexed { index, slot ->
            val (slotStart, slotEnd) = calculateIntervalMs(
                stageStart = slot.stageStart,
                stageEnd = slot.stageEnd,
                stagesCount = count,
                totalDurationMs = totalMs,
            )
            val layerKind = when (slot.kind) {
                SlotKind.IMAGE -> LayerKind.PNG
                SlotKind.TEXT -> LayerKind.TEXT
                SlotKind.GIF -> LayerKind.GIF
            }
            OverlayLayer(
                id = "slot-${slot.id}-${UUID.randomUUID().toString().take(6)}",
                kind = layerKind,
                startMs = slotStart,
                endMs = slotEnd,
                x = slot.x,
                y = slot.y,
                scale = slot.scale,
                rotationDeg = slot.rotationDeg,
                label = slot.label.ifBlank { "Slot ${index + 1}" },
                text = slot.text,
                fontName = slot.fontName,
                colorArgb = slot.colorArgb,
                fontSizeSp = slot.fontSizeSp,
                zIndex = slot.zIndex.takeIf { it != 0 } ?: index,
                animation = slot.animation,
                animationSpeed = slot.animationSpeed,
                transition = slot.effectiveTransitionIn,
            )
        }

        // 2. Fondo del esquema
        val newBackgrounds = scheme.background?.let { bg ->
            val (bgStart, bgEnd) = calculateIntervalMs(
                stageStart = bg.stageStart,
                stageEnd = bg.stageEnd,
                stagesCount = count,
                totalDurationMs = totalMs,
            )
            listOf(
                BackgroundLayer(
                    id = "bg-${UUID.randomUUID().toString().take(6)}",
                    uri = "", // Placeholder sin archivo local ligado
                    startMs = bgStart,
                    endMs = bgEnd,
                    label = bg.label,
                    kenBurns = bg.kenBurns,
                    kenBurnsSpeed = bg.kenBurnsSpeed,
                )
            )
        } ?: current.backgrounds

        // 3. Ajustes de audio
        val audio = scheme.audio

        return current.copy(
            clips = if (keepClips) current.clips else emptyList(),
            layers = newLayers,
            backgrounds = newBackgrounds,
            videoWindow = scheme.videoWindow,
            captionStyle = scheme.captionStyle,
            targetDurationMs = totalMs,
            musicFadeInMs = audio?.let { (it.fadeInSec * 1000f).toLong() } ?: current.musicFadeInMs,
            musicFadeOutMs = audio?.let { (it.fadeOutSec * 1000f).toLong() } ?: current.musicFadeOutMs,
            musicVolume = audio?.musicVolume ?: current.musicVolume,
            duckingAmount = audio?.duckingAmount ?: current.duckingAmount,
        )
    }

    /**
     * Extrae un [UsageScheme] a partir de la composición de un [Project] existente,
     * sanitizando las rutas privadas locales.
     */
    fun createFromProject(
        project: Project,
        id: String,
        name: String,
        description: String = "",
        stagesCount: Int = 5,
    ): UsageScheme {
        val totalMs = project.outputDurationMs.coerceIn(1_000L, 30_000L)
        val totalSec = kotlin.math.max(1, kotlin.math.round(totalMs / 1000.0).toInt())
        val count = stagesCount.coerceAtLeast(1)

        val slots = project.layers.mapIndexed { index, layer ->
            val startStage = msToStage(layer.startMs, count, totalMs, roundUp = false)
            val endStage = msToStage(layer.endMs, count, totalMs, roundUp = true).coerceAtLeast(startStage)
            val kind = when (layer.kind) {
                LayerKind.PNG -> SlotKind.IMAGE
                LayerKind.TEXT -> SlotKind.TEXT
                LayerKind.GIF -> SlotKind.GIF
            }
            SchemeSlot(
                id = "slot_${index + 1}",
                kind = kind,
                label = layer.label.ifBlank { "Capa ${index + 1}" },
                stageStart = startStage,
                stageEnd = endStage,
                x = layer.x,
                y = layer.y,
                scale = layer.scale,
                rotationDeg = layer.rotationDeg,
                zIndex = layer.zIndex,
                transition = layer.transition,
                animation = layer.animation,
                animationSpeed = layer.animationSpeed,
                text = layer.text,
                fontName = layer.fontName,
                colorArgb = layer.colorArgb,
                fontSizeSp = layer.fontSizeSp,
            )
        }

        val bg = project.backgrounds.firstOrNull()?.let { b ->
            val startStage = msToStage(b.startMs, count, totalMs, roundUp = false)
            val endStage = msToStage(b.endMs, count, totalMs, roundUp = true).coerceAtLeast(startStage)
            SchemeBackground(
                kenBurns = b.kenBurns,
                kenBurnsSpeed = b.kenBurnsSpeed,
                stageStart = startStage,
                stageEnd = endStage,
                label = b.label,
            )
        }

        val audio = SchemeAudio(
            stageStart = 1,
            stageEnd = count,
            fadeInSec = project.musicFadeInMs / 1000f,
            fadeOutSec = project.musicFadeOutMs / 1000f,
            musicVolume = project.musicVolume,
            duckingAmount = project.duckingAmount,
        )

        return UsageScheme(
            id = id,
            name = name,
            description = description,
            totalDurationSec = totalSec,
            stagesCount = count,
            background = bg,
            videoWindow = project.videoWindow,
            slots = slots,
            audio = audio,
            captionStyle = project.captionStyle,
        )
    }
}
