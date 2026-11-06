package com.vidcam.app.export

import android.graphics.Matrix
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.MatrixTransformation
import com.vidcam.app.model.KenBurnsMath
import com.vidcam.app.model.KenBurnsMode

/**
 * Ken Burns de una imagen de fondo como transformación del propio elemento.
 *
 * Se aplica a cada PNG de fondo preparado, antes de la composición, en lugar de
 * mover la textura desde [androidx.media3.effect.VideoCompositorSettings]: así
 * funciona igual en un proyecto con clips (donde el compositor compone vídeo y
 * fondo) y en un vídeo hecho solo con imágenes (donde no hay compositor).
 *
 * El tiempo que entrega Media3 a una transformación puede venir del inicio del
 * elemento o del inicio de la composición; para no depender de eso, el primer
 * fotograma fija el origen y el resto se mide en milisegundos desde ahí. Cada
 * elemento lleva su **propia** instancia, porque guarda ese origen.
 *
 * El resultado es la misma matriz que la vista previa aplica con
 * `graphicsLayer`: escala alrededor del centro del fotograma y desplazamiento en
 * coordenadas NDC (-1..1, eje Y hacia arriba), donde el centro es el origen.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class KenBurnsTransformation(
    private val mode: KenBurnsMode,
    private val index: Int,
    /** Instante del proyecto en el que empieza este elemento. */
    private val itemStartMs: Long,
    /** Ventana del fondo original, la misma que usa la vista previa. */
    private val startMs: Long,
    private val endMs: Long,
) : MatrixTransformation {

    private var firstFrameUs = C.TIME_UNSET

    override fun getMatrix(presentationTimeUs: Long): Matrix {
        if (firstFrameUs == C.TIME_UNSET && presentationTimeUs != C.TIME_UNSET) {
            firstFrameUs = presentationTimeUs
        }
        val elapsedMs =
            if (firstFrameUs == C.TIME_UNSET) 0L else (presentationTimeUs - firstFrameUs) / 1000L
        val kenBurns = KenBurnsMath.transformAt(
            mode = mode,
            index = index,
            startMs = startMs,
            endMs = endMs,
            timeMs = itemStartMs + elapsedMs,
        )
        return Matrix().apply {
            setScale(kenBurns.scale, kenBurns.scale)
            postTranslate(
                KenBurnsMath.ndcOffsetX(kenBurns),
                KenBurnsMath.ndcOffsetY(kenBurns),
            )
        }
    }
}
