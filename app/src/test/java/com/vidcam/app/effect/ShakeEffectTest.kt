package com.vidcam.app.effect

import com.vidcam.app.model.ShakeIntensity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class ShakeEffectTest {

    @Test
    fun ningunoNoProduceTransformacion() {
        val transform = computeShakeTransform(ShakeIntensity.NINGUNO, 500_000L)
        assertEquals(1f, transform.scale, 0.0001f)
        assertEquals(0f, transform.dx, 0.0001f)
        assertEquals(0f, transform.dy, 0.0001f)
        assertEquals(0f, transform.rotationDeg, 0.0001f)
        assertNull(ShakeIntensity.NINGUNO.media3Transformation())
    }

    @Test
    fun intensidadesTienenEscalaProporcionalYCompensada() {
        val suave = computeShakeTransform(ShakeIntensity.SUAVE, 1_000_000L)
        val medio = computeShakeTransform(ShakeIntensity.MEDIO, 1_000_000L)
        val fuerte = computeShakeTransform(ShakeIntensity.FUERTE, 1_000_000L)

        assertTrue(suave.scale > 1.0f)
        assertTrue(medio.scale > suave.scale)
        assertTrue(fuerte.scale > medio.scale)

        // Comprueba que el margen de escala compensa el desplazamiento máximo
        val tiempos = listOf(0L, 250_000L, 500_000L, 1_000_000L, 2_345_678L)
        for (intensity in listOf(ShakeIntensity.SUAVE, ShakeIntensity.MEDIO, ShakeIntensity.FUERTE)) {
            for (us in tiempos) {
                val t = computeShakeTransform(intensity, us)
                val dist = sqrt(t.dx * t.dx + t.dy * t.dy)
                val margin = t.scale - 1.0f
                assertTrue("Margen ($margin) debe cubrir desplazamiento ($dist)", margin >= dist * 0.95f)
            }
        }
    }

    @Test
    fun temblorVariaDinamicamenteEnElTiempo() {
        val t1 = computeShakeTransform(ShakeIntensity.MEDIO, 0L)
        val t2 = computeShakeTransform(ShakeIntensity.MEDIO, 350_000L)
        val t3 = computeShakeTransform(ShakeIntensity.MEDIO, 850_000L)

        assertNotEquals(t1.dx, t2.dx, 0.0001f)
        assertNotEquals(t2.dy, t3.dy, 0.0001f)
    }

    @Test
    fun rotacionAcotadaSegunIntensidad() {
        val us = 1_500_000L
        val suave = computeShakeTransform(ShakeIntensity.SUAVE, us)
        val medio = computeShakeTransform(ShakeIntensity.MEDIO, us)
        val fuerte = computeShakeTransform(ShakeIntensity.FUERTE, us)

        assertTrue(kotlin.math.abs(suave.rotationDeg) <= 1.0f)
        assertTrue(kotlin.math.abs(medio.rotationDeg) <= 2.5f)
        assertTrue(kotlin.math.abs(fuerte.rotationDeg) <= 5.0f)
    }
}
