package com.vidcam.app.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.effect.BitmapOverlay
import androidx.media3.effect.OverlaySettings
import com.vidcam.app.model.LayerKind
import com.vidcam.app.model.OverlayLayer
import com.vidcam.app.model.resolvedAt

/**
 * Overlay de Media3 que dibuja un [Bitmap] y respeta el canal alfa.
 * La visibilidad se controla con la ventana temporal de la capa.
 *
 * La posición se expresa en coordenadas normalizadas (0..1) con origen en la
 * esquina superior izquierda, igual que en la vista previa. Media3 usa NDC
 * (-1..1) con el eje Y hacia arriba y ancla el centro de la capa en el punto
 * indicado.
 *
 * [baseScale] es el factor de escala del bitmap para una capa con escala 1; en
 * cada fotograma se multiplica por la escala resuelta de la animación, de modo
 * que el movimiento grabado se reproduce en el MP4.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class LayerBitmapOverlay(
    private val bitmap: Bitmap,
    private val layer: OverlayLayer,
    private val baseScale: Float,
) : BitmapOverlay() {

    override fun getBitmap(presentationTimeUs: Long): Bitmap = bitmap

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        val startUs = layer.startMs * 1000L
        val endUs = layer.endMs * 1000L
        val visible = presentationTimeUs in startUs..endUs
        val animated = layer.resolvedAt(presentationTimeUs / 1000L)
        val scale = baseScale * animated.scale
        return OverlaySettings.Builder()
            .setAlphaScale(if (visible) 1f else 0f)
            .setScale(scale, scale)
            // Compose rota en sentido horario; Media3 recibe grados antihorarios.
            .setRotationDegrees(-animated.rotationDeg)
            .setBackgroundFrameAnchor(
                animated.x * 2f - 1f,
                1f - animated.y * 2f,
            )
            .setOverlayFrameAnchor(0f, 0f)
            .build()
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
object LayerBitmaps {

    const val ASSET_SCHEME = "asset://"

    /** Fracción del ancho del vídeo que ocupa un PNG con escala 1. */
    const val BASE_PNG_WIDTH_FRACTION = 0.35f

    /** Tamaño de texto en píxeles como fracción del ancho del vídeo a escala 1. */
    const val BASE_TEXT_SIZE_FRACTION = 0.06f

    /** Valor de [OverlayLayer.fontSizeSp] que corresponde a [BASE_TEXT_SIZE_FRACTION]. */
    const val BASE_TEXT_SIZE_SP = 42f

    /**
     * Factor de resolución con el que se rasteriza el texto: se dibuja más
     * grande y el overlay lo reduce con su escala, para que al agrandar una capa
     * animada el texto no se vea borroso.
     */
    const val TEXT_RASTER_FACTOR = 2f

    fun decodePng(context: Context, uriString: String): Bitmap? = try {
        val stream = if (uriString.startsWith(ASSET_SCHEME)) {
            context.assets.open(uriString.removePrefix(ASSET_SCHEME))
        } else {
            context.contentResolver.openInputStream(Uri.parse(uriString))
        }
        stream?.use { BitmapFactory.decodeStream(it) }
    } catch (e: Exception) {
        null
    }

    /**
     * Rasteriza el texto en un Bitmap. [textSizePx] es el tamaño de fuente en
     * píxeles del vídeo de salida, para que la exportación coincida con la
     * vista previa.
     */
    fun renderText(layer: OverlayLayer, textSizePx: Float): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = layer.colorArgb
            textSize = textSizePx.coerceAtLeast(1f)
            typeface = layer.fontName
                ?.let { runCatching { Typeface.create(it, Typeface.NORMAL) }.getOrNull() }
                ?: Typeface.DEFAULT_BOLD
            textAlign = Paint.Align.LEFT
        }
        val text = layer.text.ifBlank { " " }
        val padding = paint.textSize * 0.25f
        val metrics = paint.fontMetrics
        val width = (paint.measureText(text) + padding * 2f).toInt().coerceAtLeast(2)
        val height = (metrics.bottom - metrics.top + padding * 2f).toInt().coerceAtLeast(2)
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.TRANSPARENT)
        canvas.drawText(text, padding, padding - metrics.top, paint)
        return bitmap
    }

    /**
     * Construye el overlay de una capa. [videoWidth] y [videoHeight] son las
     * dimensiones en píxeles del vídeo de salida; se usan para que el tamaño
     * relativo coincida con la vista previa. El bitmap se entrega a escala 1 y
     * la escala animada se aplica por fotograma.
     */
    fun buildOverlay(
        context: Context,
        layer: OverlayLayer,
        videoWidth: Int,
        videoHeight: Int,
    ): LayerBitmapOverlay? {
        val width = videoWidth.coerceAtLeast(1)
        val height = videoHeight.coerceAtLeast(1)
        val bitmap: Bitmap
        val baseScale: Float
        when (layer.kind) {
            LayerKind.PNG -> {
                bitmap = layer.uri?.let { decodePng(context, it) } ?: return null
                val targetWidth = width * BASE_PNG_WIDTH_FRACTION
                baseScale = (targetWidth / bitmap.width.coerceAtLeast(1))
                    .coerceIn(0.01f, 20f)
            }
            LayerKind.TEXT -> {
                val textSizePx = minOf(width, height) * BASE_TEXT_SIZE_FRACTION *
                    (layer.fontSizeSp / BASE_TEXT_SIZE_SP)
                bitmap = renderText(layer, textSizePx * TEXT_RASTER_FACTOR)
                baseScale = 1f / TEXT_RASTER_FACTOR
            }
        }
        return LayerBitmapOverlay(bitmap, layer, baseScale)
    }
}
