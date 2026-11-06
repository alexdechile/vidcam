package com.vidcam.app.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class KenBurnsMathTest {

    private fun background(id: String, startMs: Long, endMs: Long, mode: KenBurnsMode) =
        BackgroundLayer(
            id = id,
            uri = "file:///fondo-$id.png",
            startMs = startMs,
            endMs = endMs,
            kenBurns = mode,
        )

    @Test
    fun `sin efecto la imagen queda quieta`() {
        val transform = KenBurnsMath.transformAt(
            mode = KenBurnsMode.NINGUNO,
            index = 0,
            startMs = 0L,
            endMs = 10_000L,
            timeMs = 5_000L,
        )

        assertEquals(1f, transform.scale, 0.0001f)
        assertEquals(0f, transform.offsetX, 0.0001f)
        assertEquals(0f, transform.offsetY, 0.0001f)
        assertFalse(KenBurnsMath.isActive(KenBurnsMode.NINGUNO))
    }

    @Test
    fun `acercar empieza en el encuadre completo y termina con el zoom maximo`() {
        val inicio = KenBurnsMath.transformAt(KenBurnsMode.ACERCAR, 0, 0L, 10_000L, 0L)
        val fin = KenBurnsMath.transformAt(KenBurnsMode.ACERCAR, 0, 0L, 10_000L, 10_000L)

        assertEquals(1f, inicio.scale, 0.0001f)
        assertEquals(0f, inicio.offsetX, 0.0001f)
        assertEquals(1f + KenBurnsMath.ZOOM_AMOUNT, fin.scale, 0.0001f)
    }

    @Test
    fun `alejar empieza con el zoom maximo y termina en el encuadre completo`() {
        val inicio = KenBurnsMath.transformAt(KenBurnsMode.ALEJAR, 0, 0L, 10_000L, 0L)
        val fin = KenBurnsMath.transformAt(KenBurnsMode.ALEJAR, 0, 0L, 10_000L, 10_000L)

        assertEquals(1f + KenBurnsMath.ZOOM_AMOUNT, inicio.scale, 0.0001f)
        assertEquals(1f, fin.scale, 0.0001f)
        assertEquals(0f, fin.offsetX, 0.0001f)
    }

    @Test
    fun `el desplazamiento nunca deja ver el borde de la imagen`() {
        listOf(KenBurnsMode.ACERCAR, KenBurnsMode.ALEJAR).forEach { mode ->
            for (step in 0..100) {
                val timeMs = step * 100L
                val transform = KenBurnsMath.transformAt(mode, 0, 0L, 10_000L, timeMs)
                val limite = (transform.scale - 1f) / 2f
                assertTrue(
                    "offsetX fuera de rango en $mode (t=$timeMs)",
                    abs(transform.offsetX) <= limite + 0.0001f,
                )
                assertEquals(0f, transform.offsetY, 0.0001f)
            }
        }
    }

    @Test
    fun `un tramo mas largo mueve la imagen mas despacio`() {
        val corto = KenBurnsMath.transformAt(KenBurnsMode.ACERCAR, 0, 0L, 4_000L, 2_000L)
        val largo = KenBurnsMath.transformAt(KenBurnsMode.ACERCAR, 0, 0L, 20_000L, 2_000L)

        // A los 2 s el tramo de 4 s ya recorrió la mitad del efecto y el de 20 s solo una décima parte.
        assertTrue(corto.scale > largo.scale)
        assertTrue(
            KenBurnsMath.zoomPerSecond(0L, 4_000L) >
                KenBurnsMath.zoomPerSecond(0L, 20_000L),
        )
    }

    @Test
    fun `el modo automatico alterna acercar y alejar por indice`() {
        assertEquals(KenBurnsMode.ACERCAR, KenBurnsMath.resolveMode(KenBurnsMode.AUTOMATICO, 0))
        assertEquals(KenBurnsMode.ALEJAR, KenBurnsMath.resolveMode(KenBurnsMode.AUTOMATICO, 1))
        assertEquals(KenBurnsMode.ACERCAR, KenBurnsMath.resolveMode(KenBurnsMode.AUTOMATICO, 2))
        assertEquals(KenBurnsMode.ACERCAR, KenBurnsMath.resolveMode(KenBurnsMode.ACERCAR, 1))

        val primero = KenBurnsMath.transformAt(KenBurnsMode.AUTOMATICO, 0, 0L, 10_000L, 10_000L)
        val segundo = KenBurnsMath.transformAt(KenBurnsMode.AUTOMATICO, 1, 0L, 10_000L, 10_000L)

        assertEquals(1f + KenBurnsMath.ZOOM_AMOUNT, primero.scale, 0.0001f)
        assertEquals(1f, segundo.scale, 0.0001f)
    }

    @Test
    fun `el progreso se suaviza y se acota al tramo`() {
        assertEquals(0f, KenBurnsMath.progressAt(1_000L, 3_000L, 0L), 0.0001f)
        assertEquals(0f, KenBurnsMath.progressAt(1_000L, 3_000L, 1_000L), 0.0001f)
        assertEquals(0.5f, KenBurnsMath.progressAt(1_000L, 3_000L, 2_000L), 0.0001f)
        assertEquals(1f, KenBurnsMath.progressAt(1_000L, 3_000L, 3_000L), 0.0001f)
        assertEquals(1f, KenBurnsMath.progressAt(1_000L, 3_000L, 9_000L), 0.0001f)
    }

    @Test
    fun `un tramo sin duracion no anima`() {
        val transform = KenBurnsMath.transformAt(KenBurnsMode.ACERCAR, 0, 5_000L, 5_000L, 5_000L)

        assertEquals(1f, transform.scale, 0.0001f)
        assertEquals(0f, transform.offsetX, 0.0001f)
    }

    @Test
    fun `indexOf ordena las imagenes por inicio y tolera ids desconocidos`() {
        val backgrounds = listOf(
            background("segundo", 4_000L, 9_000L, KenBurnsMode.AUTOMATICO),
            background("primero", 0L, 4_000L, KenBurnsMode.AUTOMATICO),
        )

        assertEquals(0, KenBurnsMath.indexOf(backgrounds, "primero"))
        assertEquals(1, KenBurnsMath.indexOf(backgrounds, "segundo"))
        assertEquals(0, KenBurnsMath.indexOf(backgrounds, "no-existe"))
        assertEquals(0, KenBurnsMath.indexOf(backgrounds, null))
    }

    @Test
    fun `un fondo sin ken burns explicito queda quieto`() {
        val fondo = BackgroundLayer(id = "bg", uri = "file:///fondo.png")

        assertEquals(KenBurnsMode.NINGUNO, fondo.kenBurns)
    }
}
