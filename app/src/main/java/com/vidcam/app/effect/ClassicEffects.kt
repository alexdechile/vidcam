package com.vidcam.app.effect

import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.media3.effect.RgbMatrix
import androidx.media3.effect.ScaleAndRotateTransformation
import com.vidcam.app.model.ColorFilterPreset
import com.vidcam.app.model.WideLensMode

/**
 * Presets de color clásicos y encuadre ancho, compartidos entre la vista previa
 * (Compose) y la exportación (Media3 Transformer) para que ambos motores
 * produzcan exactamente el mismo resultado.
 *
 * Cada preset se define por filas RGB (orden que entiende [ColorMatrix]) y se
 * entrega en dos formatos:
 *  - [colorFilter]: [ColorFilter] de Compose para la vista previa.
 *  - [media3Effect]: [RgbMatrix] para la exportación.
 *
 * Ningún preset usa offsets de color (todas las matrices son 4x4 sin columna de
 * traslación). Así el render de Compose y el de OpenGL coinciden byte a byte.
 */

/** Encuadre ancho digital (0.65x): reduce el fotograma el mismo factor en ambos motores. */
const val WIDE_LENS_SCALE = 0.65f

data class ClassicEffect(
    val rows: Array<FloatArray>,
) {
    val isIdentity: Boolean
        get() = rows.indices.all { i ->
            rows[i].indices.all { j -> rows[i][j] == if (i == j) 1f else 0f }
        }

    /** Matriz 4x5 fila-major para [ColorMatrix] de Compose (offsets en cero). */
    fun toCompose(): FloatArray {
        val out = FloatArray(20)
        for (i in 0..3) {
            for (j in 0..3) out[i * 5 + j] = rows[i][j]
        }
        return out
    }

    /**
     * Matriz 4x4 column-major de OpenGL para el exportador, transpuesta respecto
     * de [toCompose] (offsets no aplicables en Media3, por eso son cero).
     */
    fun toGl(): FloatArray {
        val out = FloatArray(16)
        for (i in 0..3) {
            for (j in 0..3) out[j * 4 + i] = rows[i][j]
        }
        return out
    }
}

fun classicEffect(preset: ColorFilterPreset): ClassicEffect? = when (preset) {
    ColorFilterPreset.NINGUNO -> null
    ColorFilterPreset.SEPIA -> ClassicEffect(
        rows = arrayOf(
            floatArrayOf(0.393f, 0.769f, 0.189f, 0f),
            floatArrayOf(0.349f, 0.686f, 0.168f, 0f),
            floatArrayOf(0.272f, 0.534f, 0.131f, 0f),
            floatArrayOf(0f, 0f, 0f, 1f),
        ),
    )
    ColorFilterPreset.BLANCO_Y_NEGRO -> ClassicEffect(
        rows = arrayOf(
            floatArrayOf(0.2126f, 0.7152f, 0.0722f, 0f),
            floatArrayOf(0.2126f, 0.7152f, 0.0722f, 0f),
            floatArrayOf(0.2126f, 0.7152f, 0.0722f, 0f),
            floatArrayOf(0f, 0f, 0f, 1f),
        ),
    )
    ColorFilterPreset.VINTAGE -> ClassicEffect(
        rows = arrayOf(
            floatArrayOf(0.95f, 0.35f, 0.10f, 0f),
            floatArrayOf(0.05f, 0.80f, 0.10f, 0f),
            floatArrayOf(0.00f, 0.10f, 0.55f, 0f),
            floatArrayOf(0f, 0f, 0f, 1f),
        ),
    )
    ColorFilterPreset.ALTO_CONTRASTE -> ClassicEffect(
        rows = arrayOf(
            floatArrayOf(1.2f, -0.1f, -0.1f, 0f),
            floatArrayOf(-0.1f, 1.2f, -0.1f, 0f),
            floatArrayOf(-0.1f, -0.1f, 1.2f, 0f),
            floatArrayOf(0f, 0f, 0f, 1f),
        ),
    )
}

/** [ColorFilter] de Compose para la vista previa del preset, o `null` si no aplica. */
fun ColorFilterPreset.composeColorFilter(): ColorFilter? =
    classicEffect(this)?.let { ColorFilter.colorMatrix(ColorMatrix(it.toCompose())) }

/** [RgbMatrix] de Media3 para la exportación del preset, o `null` si no aplica. */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun ColorFilterPreset.media3Effect(): RgbMatrix? {
    val effect = classicEffect(this) ?: return null
    return object : RgbMatrix {
        override fun getMatrix(presentationTimeUs: Long, useHdr: Boolean): FloatArray =
            effect.toGl()
    }
}

/**
 * Transformación de encuadre ancho para la exportación, o `null` si el clip usa
 * el encuadre normal.
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun WideLensMode.media3Transformation(): ScaleAndRotateTransformation? =
    if (this == WideLensMode.GRUPO) {
        ScaleAndRotateTransformation.Builder()
            .setScale(WIDE_LENS_SCALE, WIDE_LENS_SCALE)
            .build()
    } else {
        null
    }