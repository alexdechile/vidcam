package com.vidcam.app.model.scheme

import com.vidcam.app.model.BackgroundLayer
import com.vidcam.app.model.LayerKind
import com.vidcam.app.model.MAX_DURATION_MS
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
     * Prepara una vinculación inicial ([SchemeBinding]) auto-mapeando de forma inteligente
     * los fondos y capas existentes en el [current] proyecto hacia los slots de la plantilla.
     */
    fun prepareInitialBinding(
        scheme: UsageScheme,
        current: Project = Project(),
    ): SchemeBinding {
        // 1. Auto-mapeo de fondo
        val bgBinding = if (scheme.background != null) {
            val existingBg = current.backgrounds.firstOrNull()
            BackgroundBinding(
                uri = existingBg?.uri,
                label = scheme.background.label.ifBlank { existingBg?.label.orEmpty().ifBlank { "Fondo" } },
                omitted = false,
            )
        } else {
            BackgroundBinding(omitted = true)
        }

        // 2. Auto-mapeo de capas de imagen y GIF
        val existingImages = current.layers.filter { it.kind == LayerKind.PNG || it.kind == LayerKind.GIF }
        val imageSlots = scheme.slots.filter { it.kind == SlotKind.IMAGE || it.kind == SlotKind.GIF }
        val imageBindings = mutableMapOf<String, SlotBinding>()

        imageSlots.forEachIndexed { index, slot ->
            val match = existingImages.getOrNull(index)
            imageBindings[slot.id] = SlotBinding(
                slotId = slot.id,
                uri = match?.uri,
                label = slot.label.ifBlank { match?.label.orEmpty().ifBlank { "Imagen ${index + 1}" } },
                omitted = false,
            )
        }

        // 3. Auto-mapeo de textos
        val existingTexts = current.layers.filter { it.kind == LayerKind.TEXT }
        val textSlots = scheme.slots.filter { it.kind == SlotKind.TEXT }
        val textBindings = mutableMapOf<String, SlotBinding>()

        textSlots.forEachIndexed { index, slot ->
            val match = existingTexts.getOrNull(index)
            textBindings[slot.id] = SlotBinding(
                slotId = slot.id,
                text = match?.text?.takeIf { it.isNotBlank() } ?: slot.text,
                label = slot.label.ifBlank { match?.label.orEmpty().ifBlank { "Texto ${index + 1}" } },
                omitted = false,
            )
        }

        return SchemeBinding(
            scheme = scheme,
            background = bgBinding,
            slots = imageBindings + textBindings,
            openVoiceoverOnApply = false,
        )
    }

    /**
     * Asigna secuencialmente una lista de URIs de imágenes a los slots de imagen del esquema.
     */
    fun withBatchImages(
        binding: SchemeBinding,
        uris: List<String>,
    ): SchemeBinding {
        if (uris.isEmpty()) return binding
        val imageSlots = binding.scheme.slots.filter { it.kind == SlotKind.IMAGE || it.kind == SlotKind.GIF }
        val updatedSlots = binding.slots.toMutableMap()

        uris.forEachIndexed { index, uri ->
            if (index < imageSlots.size) {
                val slot = imageSlots[index]
                val prev = updatedSlots[slot.id] ?: SlotBinding(slotId = slot.id)
                updatedSlots[slot.id] = prev.copy(uri = uri)
            }
        }
        return binding.copy(slots = updatedSlots)
    }

    /**
     * Aplica la coreografía y estructura de un [UsageScheme] sobre un [Project]
     * utilizando una vinculación explícita de recursos [SchemeBinding].
     */
    fun applyBindingToProject(
        binding: SchemeBinding,
        current: Project = Project(),
        keepClips: Boolean = true,
    ): Project {
        val scheme = binding.scheme
        val totalMs = scheme.totalDurationMs
        val count = scheme.stagesCount

        // 1. Convertir slots activos a OverlayLayers inyectando URIs y textos vinculados
        val newLayers = scheme.slots.mapNotNull { slot ->
            val slotBinding = binding.getSlot(slot.id)
            if (slotBinding.omitted) return@mapNotNull null

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
                label = slotBinding.label.ifBlank { slot.label.ifBlank { "Slot ${slot.id}" } },
                uri = slotBinding.uri,
                text = slotBinding.text ?: slot.text,
                fontName = slot.fontName,
                colorArgb = slot.colorArgb,
                fontSizeSp = slot.fontSizeSp,
                zIndex = slot.zIndex,
                animation = slot.animation,
                animationSpeed = slot.animationSpeed,
                transition = slot.effectiveTransitionIn,
            )
        }

        // 2. Fondo del esquema con URI vinculada
        val newBackgrounds = if (scheme.background != null && !binding.background.omitted) {
            val bg = scheme.background
            val (bgStart, bgEnd) = calculateIntervalMs(
                stageStart = bg.stageStart,
                stageEnd = bg.stageEnd,
                stagesCount = count,
                totalDurationMs = totalMs,
            )
            listOf(
                BackgroundLayer(
                    id = "bg-${UUID.randomUUID().toString().take(6)}",
                    uri = binding.background.uri.orEmpty(),
                    startMs = bgStart,
                    endMs = bgEnd,
                    label = binding.background.label.ifBlank { bg.label },
                    kenBurns = bg.kenBurns,
                    kenBurnsSpeed = bg.kenBurnsSpeed,
                )
            )
        } else {
            emptyList()
        }

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
        val initialBinding = prepareInitialBinding(scheme, current)
        return applyBindingToProject(initialBinding, current, keepClips = keepClips)
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
        val totalMs = project.outputDurationMs.coerceIn(1_000L, maxOf(MAX_DURATION_MS, project.outputDurationMs))
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
