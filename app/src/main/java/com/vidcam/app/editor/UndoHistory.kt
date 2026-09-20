package com.vidcam.app.editor

import com.vidcam.app.model.Project

/**
 * Historial de deshacer/rehacer sobre instantáneas inmutables del proyecto.
 *
 * Las ediciones continuas (arrastrar un clip, un pellizco sobre una capa, mover
 * un control deslizante) llegan como decenas de cambios seguidos. Para que
 * "deshacer" retroceda a un gesto completo y no a un fotograma intermedio, las
 * ediciones que comparten [key] dentro de [coalesceWindowMs] se agrupan en una
 * sola entrada: se conserva el estado previo a la primera del grupo.
 *
 * Sin dependencias de Android para poder probarlo con tests unitarios.
 */
class UndoHistory(
    private val limit: Int = 60,
    private val coalesceWindowMs: Long = 700L,
) {

    private val undoStack = ArrayDeque<Project>()
    private val redoStack = ArrayDeque<Project>()
    private var lastKey: String? = null
    private var lastRecordedAtMs = Long.MIN_VALUE

    val canUndo: Boolean
        get() = undoStack.isNotEmpty()

    val canRedo: Boolean
        get() = redoStack.isNotEmpty()

    /**
     * Registra [before] como el estado al que volver. Debe llamarse solo cuando
     * el proyecto realmente cambia.
     */
    fun record(before: Project, key: String? = null, nowMs: Long) {
        redoStack.clear()
        val coalesces = key != null &&
            key == lastKey &&
            undoStack.isNotEmpty() &&
            nowMs - lastRecordedAtMs <= coalesceWindowMs
        if (coalesces) {
            // Sigue el mismo gesto: basta con el estado previo ya guardado.
            lastRecordedAtMs = nowMs
            return
        }
        undoStack.addLast(before)
        while (undoStack.size > limit) {
            undoStack.removeFirst()
        }
        lastKey = key
        lastRecordedAtMs = nowMs
    }

    /** Devuelve el estado anterior a [current], o `null` si no hay historial. */
    fun undo(current: Project): Project? {
        val previous = undoStack.removeLastOrNull() ?: return null
        redoStack.addLast(current)
        resetCoalescing()
        return previous
    }

    /** Devuelve el estado siguiente a [current], o `null` si no hay rehacer. */
    fun redo(current: Project): Project? {
        val next = redoStack.removeLastOrNull() ?: return null
        undoStack.addLast(current)
        resetCoalescing()
        return next
    }

    fun clear() {
        undoStack.clear()
        redoStack.clear()
        resetCoalescing()
    }

    private fun resetCoalescing() {
        lastKey = null
        lastRecordedAtMs = Long.MIN_VALUE
    }
}
