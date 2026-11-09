package com.vidcam.app.model.scheme

/**
 * Contenido y recursos asignados a un slot individual de un esquema.
 */
data class SlotBinding(
    val slotId: String,
    /** URI del archivo local (PNG, foto, GIF). Null si no tiene imagen asignada. */
    val uri: String? = null,
    /** Texto personalizado (si el slot es de tipo TEXT o lleva rótulo). */
    val text: String? = null,
    /** Etiqueta descriptiva para el slot. */
    val label: String = "",
    /** Si es true, el slot no se creará en el proyecto resultante. */
    val omitted: Boolean = false,
)

/**
 * Vinculación del fondo del esquema a un recurso local.
 */
data class BackgroundBinding(
    /** URI del fondo (imagen local). Si es nulo o vacío, se usará un fondo neutro o vacío. */
    val uri: String? = null,
    val label: String = "Fondo",
    /** Si es true, no se crea el fondo del esquema. */
    val omitted: Boolean = false,
)

/**
 * Especificación completa de vinculación entre un [UsageScheme] y recursos reales,
 * lista para ser aplicada a un [com.vidcam.app.model.Project].
 */
data class SchemeBinding(
    val scheme: UsageScheme,
    val background: BackgroundBinding = BackgroundBinding(),
    val slots: Map<String, SlotBinding> = emptyMap(),
    /** Si es true, tras aplicar el esquema se abre la hoja de doblaje de voz en off. */
    val openVoiceoverOnApply: Boolean = false,
) {
    /** Retorna el binding asignado para un slot específico o uno por defecto si no existe. */
    fun getSlot(slotId: String): SlotBinding {
        return slots[slotId] ?: SlotBinding(slotId = slotId)
    }

    /** Retorna una copia con un slot actualizado. */
    fun withSlot(slotId: String, update: (SlotBinding) -> SlotBinding): SchemeBinding {
        val current = getSlot(slotId)
        val updated = update(current)
        return copy(slots = slots + (slotId to updated))
    }

    /** Retorna una copia con el fondo actualizado. */
    fun withBackground(update: (BackgroundBinding) -> BackgroundBinding): SchemeBinding {
        return copy(background = update(background))
    }
}
