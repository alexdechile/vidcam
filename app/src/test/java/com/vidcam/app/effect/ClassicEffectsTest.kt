package com.vidcam.app.effect

import com.vidcam.app.model.ColorFilterPreset
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClassicEffectsTest {

    private fun rowsToCompose(rows: Array<FloatArray>): FloatArray {
        val out = FloatArray(20)
        for (i in 0..3) {
            for (j in 0..3) out[i * 5 + j] = rows[i][j]
        }
        return out
    }

    private fun rowsToGl(rows: Array<FloatArray>): FloatArray {
        val out = FloatArray(16)
        for (i in 0..3) {
            for (j in 0..3) out[j * 4 + i] = rows[i][j]
        }
        return out
    }

    @Test
    fun ningunoNoAplicaEfecto() {
        assertNull(classicEffect(ColorFilterPreset.NINGUNO))
    }

    @Test
    fun sepiaTieneMatrizEsperada() {
        val effect = classicEffect(ColorFilterPreset.SEPIA)!!
        val rows = arrayOf(
            floatArrayOf(0.393f, 0.769f, 0.189f, 0f),
            floatArrayOf(0.349f, 0.686f, 0.168f, 0f),
            floatArrayOf(0.272f, 0.534f, 0.131f, 0f),
            floatArrayOf(0f, 0f, 0f, 1f),
        )
        assertArrayEquals(rowsToCompose(rows), effect.toCompose(), 0.0001f)
        assertArrayEquals(rowsToGl(rows), effect.toGl(), 0.0001f)
    }

    @Test
    fun blancoYNegroUsaLuminanciaBt709() {
        val effect = classicEffect(ColorFilterPreset.BLANCO_Y_NEGRO)!!
        val rows = arrayOf(
            floatArrayOf(0.2126f, 0.7152f, 0.0722f, 0f),
            floatArrayOf(0.2126f, 0.7152f, 0.0722f, 0f),
            floatArrayOf(0.2126f, 0.7152f, 0.0722f, 0f),
            floatArrayOf(0f, 0f, 0f, 1f),
        )
        assertArrayEquals(rowsToCompose(rows), effect.toCompose(), 0.0001f)
        assertArrayEquals(rowsToGl(rows), effect.toGl(), 0.0001f)
    }

    @Test
    fun vintageYAltoContrasteMantienenAlfaYOffsetsCero() {
        for (preset in listOf(ColorFilterPreset.VINTAGE, ColorFilterPreset.ALTO_CONTRASTE)) {
            val effect = classicEffect(preset)!!
            val gl = effect.toGl()
            // Fila alfa en column-major: índices 3, 7, 11, 15.
            assertArrayEquals(floatArrayOf(0f, 0f, 0f, 1f), gl.copyOfRange(12, 16), 0.0001f)
            // Offsets de color (última columna de Compose 4x5): índices 4, 9, 14, 19.
            // Sin offsets, igual que la matriz GL de Media3 (que no tiene columna de traslación).
            val compose = effect.toCompose()
            assertArrayEquals(
                floatArrayOf(0f, 0f, 0f, 0f),
                floatArrayOf(compose[4], compose[9], compose[14], compose[19]),
                0.0001f,
            )
        }
    }

    @Test
    fun lenteGrupoUsaFactorDigital65() {
        org.junit.Assert.assertEquals(0.65f, com.vidcam.app.effect.WIDE_LENS_SCALE, 0.0001f)
    }
}