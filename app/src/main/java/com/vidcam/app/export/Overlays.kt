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

/**
 * Overlay de Media3 que dibuja un [Bitmap] y respeta el canal alfa.
 * La visibilidad se controla con la ventana temporal de la capa.
 */
@androidx.annotation.OptIn(UnstableApi::class)
class LayerBitmapOverlay(
    private val bitmap: Bitmap,
    private val layer: OverlayLayer,
) : BitmapOverlay() {

    override fun getBitmap(presentationTimeUs: Long): Bitmap = bitmap

    override fun getOverlaySettings(presentationTimeUs: Long): OverlaySettings {
        val startUs = layer.startMs * 1000L
        val endUs = layer.endMs * 1000L
        val visible = presentationTimeUs in startUs..endUs
        return OverlaySettings.Builder()
            .setAlphaScale(if (visible) 1f else 0f)
            .setScale(layer.scale, layer.scale)
            .setRotationDegrees(layer.rotationDeg)
            .setOverlayFrameAnchor(layer.x * 2f - 1f, layer.y * 2f - 1f)
            .build()
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
object LayerBitmaps {

    const val ASSET_SCHEME = "asset://"

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

    fun renderText(layer: OverlayLayer): Bitmap {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = layer.colorArgb
            textSize = layer.fontSizeSp * 2f
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

    fun buildOverlay(context: Context, layer: OverlayLayer): LayerBitmapOverlay? {
        val bitmap = when (layer.kind) {
            LayerKind.PNG -> layer.uri?.let { decodePng(context, it) }
            LayerKind.TEXT -> renderText(layer)
        } ?: return null
        return LayerBitmapOverlay(bitmap, layer)
    }
}
