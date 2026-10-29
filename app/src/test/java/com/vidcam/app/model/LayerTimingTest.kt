package com.vidcam.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LayerTimingTest {

    private fun png(
        startMs: Long = 0L,
        endMs: Long = MAX_DURATION_MS,
    ): OverlayLayer = OverlayLayer(
        id = "layer-1",
        kind = LayerKind.PNG,
        startMs = startMs,
        endMs = endMs,
    )

    @Test
    fun sinClipsElTimelineUsaElLimiteDelProyecto() {
        assertEquals(MAX_DURATION_MS, LayerTiming.timelineMs(0L))
        assertEquals(4_000L, LayerTiming.timelineMs(4_000L))
        // Un proyecto absurdamente corto no baja del mínimo de la capa.
        assertEquals(
            LayerTiming.MIN_DURATION_MS,
            LayerTiming.timelineMs(10L),
        )
    }

    @Test
    fun unaCapaNuevaNoDuraMasQueElVideo() {
        val layer = png(startMs = 0L, endMs = MAX_DURATION_MS)
        assertEquals(4_000L, LayerTiming.durationMs(layer, 4_000L))
    }

    @Test
    fun laDuracionNuncaBajaDelMinimo() {
        val layer = png(startMs = 1_000L, endMs = 1_050L)
        assertEquals(LayerTiming.MIN_DURATION_MS, LayerTiming.durationMs(layer, 4_000L))
        val fitted = LayerTiming.withDuration(layer, 0L, 4_000L)
        assertEquals(LayerTiming.MIN_DURATION_MS, fitted.endMs - fitted.startMs)
    }

    @Test
    fun maximaDuracionEsElTiempoQueQuedaDesdeElInicio() {
        val layer = png(startMs = 2_000L, endMs = 2_500L)
        val fitted = LayerTiming.withDuration(layer, 9_999L, 4_000L)
        assertEquals(2_000L, fitted.startMs)
        assertEquals(4_000L, fitted.endMs)
    }

    @Test
    fun cambiarLaDuracionConservaElInicio() {
        val layer = png(startMs = 1_500L, endMs = 3_000L)
        val fitted = LayerTiming.withDuration(layer, 3_000L, 10_000L)
        assertEquals(1_500L, fitted.startMs)
        assertEquals(4_500L, fitted.endMs)
    }

    @Test
    fun moverElInicioConservaLaDuracion() {
        val layer = png(startMs = 0L, endMs = 3_000L)
        val moved = LayerTiming.withStart(layer, 5_000L, 10_000L)
        assertEquals(5_000L, moved.startMs)
        assertEquals(8_000L, moved.endMs)
        assertEquals(3_000L, moved.endMs - moved.startMs)
    }

    @Test
    fun elInicioNoEmpujaLaCapaFueraDelVideo() {
        val layer = png(startMs = 0L, endMs = 4_000L)
        val moved = LayerTiming.withStart(layer, 9_000L, 10_000L)
        assertEquals(6_000L, moved.startMs)
        assertEquals(10_000L, moved.endMs)
    }

    @Test
    fun unaCapaSinVentanaExplicitaSigueCubriendoTodoElVideo() {
        val layer = OverlayLayer(id = "layer-2", kind = LayerKind.TEXT)
        assertEquals(0L, layer.startMs)
        assertTrue(LayerTiming.isVisibleAt(layer, 0L))
        assertTrue(LayerTiming.isVisibleAt(layer, 12_345L))
    }

    @Test
    fun isVisibleAtIncluyeLosBordes() {
        val layer = png(startMs = 1_000L, endMs = 2_000L)
        assertFalse(LayerTiming.isVisibleAt(layer, 999L))
        assertTrue(LayerTiming.isVisibleAt(layer, 1_000L))
        assertTrue(LayerTiming.isVisibleAt(layer, 2_000L))
        assertFalse(LayerTiming.isVisibleAt(layer, 2_001L))
    }

    @Test
    fun fitToTimelineRecuperaUnaCapaQueQuedoFuera() {
        val layer = png(startMs = 8_000L, endMs = 12_000L)
        val fitted = LayerTiming.fitToTimeline(layer, 5_000L)
        assertTrue(fitted.startMs >= 0L)
        assertTrue(fitted.endMs <= 5_000L)
        // Justo antes del final solo cabe la ventana mínima, para que la capa
        // siga existiendo en lugar de desaparecer del proyecto.
        assertEquals(5_000L - LayerTiming.MIN_DURATION_MS, fitted.startMs)
    }

    @Test
    fun laVentanaNoPierdeLaAnimacionAlAcortarYAlargar() {
        val layer = png(startMs = 0L, endMs = 8_000L).copy(
            keyframes = listOf(
                LayerKeyframe(timeMs = 0L, x = 0.1f, y = 0.1f, scale = 1f, rotationDeg = 0f),
                LayerKeyframe(timeMs = 7_000L, x = 0.9f, y = 0.9f, scale = 1f, rotationDeg = 0f),
            ),
        )
        val shortened = LayerTiming.withDuration(layer, 2_000L, 10_000L)
        assertEquals(2_000L, shortened.endMs - shortened.startMs)
        // Los keyframes siguen ahí; solo quedan ocultos mientras la capa no se ve.
        assertEquals(2, shortened.keyframes.size)

        val widened = LayerTiming.withDuration(shortened, 8_000L, 10_000L)
        assertEquals(8_000L, widened.endMs - widened.startMs)
        assertEquals(2, widened.keyframes.size)
    }
}
