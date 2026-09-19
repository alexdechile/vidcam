package com.vidcam.app.export

import android.content.Context
import android.net.Uri
import com.vidcam.app.media.MediaProbe
import com.vidcam.app.model.Project

/**
 * Geometría del fotograma de salida.
 *
 * La exportación dibuja las capas en coordenadas normalizadas (0..1) sobre el
 * fotograma completo del vídeo. Para que la vista previa coincida, esta debe
 * mostrar exactamente el mismo fotograma: la misma relación de aspecto y sin
 * recortes. Ambas partes obtienen aquí las dimensiones.
 */
object FrameGeometry {

    /** Tamaño de salida cuando no se puede leer el primer clip. */
    const val DEFAULT_WIDTH = 1080
    const val DEFAULT_HEIGHT = 1920

    /**
     * Tamaño en píxeles del fotograma de salida: el del primer clip (respetando
     * la rotación del metadato) o [DEFAULT_WIDTH]x[DEFAULT_HEIGHT] si no se
     * puede leer.
     */
    fun frameSize(context: Context, project: Project): Pair<Int, Int> =
        project.clips.firstOrNull()
            ?.let { MediaProbe.videoSize(context, Uri.parse(it.uri)) }
            ?.takeIf { it.first > 0 && it.second > 0 }
            ?: (DEFAULT_WIDTH to DEFAULT_HEIGHT)

    /**
     * Escala [frameWidth]x[frameHeight] para que quepa completo dentro de
     * [maxWidth]x[maxHeight] conservando la relación de aspecto (equivalente a
     * un ajuste *fit* centrado). Devuelve el tamaño en las mismas unidades que
     * las entradas.
     */
    fun fit(
        frameWidth: Float,
        frameHeight: Float,
        maxWidth: Float,
        maxHeight: Float,
    ): Pair<Float, Float> {
        if (frameWidth <= 0f || frameHeight <= 0f || maxWidth <= 0f || maxHeight <= 0f) {
            return maxWidth.coerceAtLeast(0f) to maxHeight.coerceAtLeast(0f)
        }
        val scale = minOf(maxWidth / frameWidth, maxHeight / frameHeight)
        return frameWidth * scale to frameHeight * scale
    }
}
