package com.vidcam.app.effect

import android.graphics.Matrix
import androidx.media3.effect.MatrixTransformation
import com.vidcam.app.model.ShakeIntensity
import kotlin.math.cos
import kotlin.math.sin

/**
 * Representación desacoplada de la transformación de temblor en un instante dado,
 * útil para pruebas unitarias sin dependencias de Android framework.
 */
data class ShakeTransform(
    val scale: Float,
    val dx: Float,
    val dy: Float,
    val rotationDeg: Float,
)

/**
 * Calcula los componentes del efecto temblor según la intensidad y el tiempo de presentación en microsegundos.
 * Combina múltiples frecuencias sinusoidales para producir una vibración orgánica.
 */
fun computeShakeTransform(intensity: ShakeIntensity, presentationTimeUs: Long): ShakeTransform {
    if (intensity == ShakeIntensity.NINGUNO) {
        return ShakeTransform(scale = 1f, dx = 0f, dy = 0f, rotationDeg = 0f)
    }

    val t = presentationTimeUs / 1_000_000.0

    return when (intensity) {
        ShakeIntensity.NINGUNO -> ShakeTransform(1f, 0f, 0f, 0f)
        ShakeIntensity.SUAVE -> {
            val scale = 1.05f
            val dx = (sin(t * 1.7) * 0.015 + sin(t * 3.9 + 0.8) * 0.010).toFloat()
            val dy = (cos(t * 2.1) * 0.015 + sin(t * 4.3 + 1.4) * 0.010).toFloat()
            val rot = (sin(t * 2.5 + 0.3) * 0.6).toFloat()
            ShakeTransform(scale = scale, dx = dx, dy = dy, rotationDeg = rot)
        }
        ShakeIntensity.MEDIO -> {
            val scale = 1.10f
            val dx = (sin(t * 3.1) * 0.035 + sin(t * 6.7 + 1.1) * 0.020).toFloat()
            val dy = (cos(t * 3.7) * 0.035 + sin(t * 7.9 + 2.0) * 0.020).toFloat()
            val rot = (sin(t * 4.2 + 0.5) * 1.5).toFloat()
            ShakeTransform(scale = scale, dx = dx, dy = dy, rotationDeg = rot)
        }
        ShakeIntensity.FUERTE -> {
            val scale = 1.16f
            val dx = (sin(t * 5.3) * 0.055 + sin(t * 11.1 + 0.7) * 0.035).toFloat()
            val dy = (cos(t * 6.1) * 0.055 + sin(t * 12.3 + 1.8) * 0.035).toFloat()
            val rot = (sin(t * 7.0 + 0.9) * 3.2).toFloat()
            ShakeTransform(scale = scale, dx = dx, dy = dy, rotationDeg = rot)
        }
    }
}

/**
 * Construye una matriz 3x3 de Android para el espacio normalizado de coordenadas (NDC: [-1, 1]).
 */
fun computeShakeMatrix(intensity: ShakeIntensity, presentationTimeUs: Long): Matrix {
    val transform = computeShakeTransform(intensity, presentationTimeUs)
    return Matrix().apply {
        setScale(transform.scale, transform.scale)
        postRotate(transform.rotationDeg)
        postTranslate(transform.dx, transform.dy)
    }
}

/**
 * Transforma el clip con temblor usando [MatrixTransformation] de Media3 para exportación
 * y vista previa de ExoPlayer. Retorna `null` si la intensidad es [ShakeIntensity.NINGUNO].
 */
@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
fun ShakeIntensity.media3Transformation(): MatrixTransformation? {
    if (this == ShakeIntensity.NINGUNO) return null
    return MatrixTransformation { presentationTimeUs ->
        computeShakeMatrix(this, presentationTimeUs)
    }
}
