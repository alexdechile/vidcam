package com.vidcam.app.editor

import com.vidcam.app.model.Project
import com.vidcam.app.model.VideoClip
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class UndoHistoryTest {

    private fun project(id: String) = Project(
        clips = listOf(
            VideoClip(id = id, uri = "file:///$id.mp4", sourceDurationMs = 1000L),
        ),
    )

    private val a = project("a")
    private val b = project("b")
    private val c = project("c")

    @Test
    fun `undo devuelve el estado anterior y redo lo rehace`() {
        val history = UndoHistory()
        history.record(before = a, nowMs = 0L)
        history.record(before = b, nowMs = 0L)

        assertEquals(b, history.undo(c))
        assertEquals(a, history.undo(b))
        assertNull(history.undo(a))

        assertEquals(b, history.redo(a))
        assertEquals(c, history.redo(b))
        assertNull(history.redo(c))
    }

    @Test
    fun `una edicion nueva descarta el rehacer`() {
        val history = UndoHistory()
        history.record(before = a, nowMs = 0L)
        assertEquals(a, history.undo(b))
        assertTrue(history.canRedo)

        history.record(before = a, nowMs = 0L)
        assertFalse(history.canRedo)
        assertNull(history.redo(b))
    }

    @Test
    fun `no hay historial al empezar`() {
        val history = UndoHistory()
        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
        assertNull(history.undo(a))
        assertNull(history.redo(a))
    }

    @Test
    fun `las ediciones continuas con la misma clave se agrupan en una entrada`() {
        val history = UndoHistory(coalesceWindowMs = 700L)
        // Un gesto que emite cada 16 ms: solo debe quedar el estado inicial.
        history.record(before = a, key = "gesture:L1", nowMs = 0L)
        history.record(before = b, key = "gesture:L1", nowMs = 16L)
        history.record(before = b, key = "gesture:L1", nowMs = 32L)
        history.record(before = b, key = "gesture:L1", nowMs = 48L)

        assertEquals(a, history.undo(b))
        assertNull(history.undo(a))
    }

    @Test
    fun `claves distintas no se agrupan`() {
        val history = UndoHistory()
        history.record(before = a, key = "trim:1", nowMs = 0L)
        history.record(before = b, key = "trim:2", nowMs = 16L)

        assertEquals(b, history.undo(c))
        assertEquals(a, history.undo(b))
    }

    @Test
    fun `pasada la ventana de agrupacion se guarda una entrada nueva`() {
        val history = UndoHistory(coalesceWindowMs = 700L)
        history.record(before = a, key = "trim:1", nowMs = 0L)
        history.record(before = b, key = "trim:1", nowMs = 701L)

        assertEquals(b, history.undo(c))
        assertEquals(a, history.undo(b))
    }

    @Test
    fun `sin clave no se agrupa aunque llegue en el mismo instante`() {
        val history = UndoHistory()
        history.record(before = a, nowMs = 0L)
        history.record(before = b, nowMs = 0L)

        assertEquals(b, history.undo(c))
        assertEquals(a, history.undo(b))
    }

    @Test
    fun `el historial respeta el limite`() {
        val history = UndoHistory(limit = 3)
        history.record(before = project("0"), nowMs = 0)
        history.record(before = project("1"), nowMs = 0)
        history.record(before = project("2"), nowMs = 0)
        history.record(before = project("3"), nowMs = 0)

        assertEquals(project("3"), history.undo(project("4")))
        assertEquals(project("2"), history.undo(project("3")))
        assertEquals(project("1"), history.undo(project("2")))
        assertNull(history.undo(project("1")))
    }

    @Test
    fun `clear deja el historial vacio en ambos sentidos`() {
        val history = UndoHistory()
        history.record(before = a, nowMs = 0L)
        history.undo(b)
        history.clear()

        assertFalse(history.canUndo)
        assertFalse(history.canRedo)
    }
}
