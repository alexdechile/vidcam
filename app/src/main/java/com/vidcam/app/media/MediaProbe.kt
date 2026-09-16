package com.vidcam.app.media

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import java.io.File

object MediaProbe {

    fun durationMs(context: Context, uri: Uri): Long {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                ?.toLongOrNull() ?: 0L
        } catch (e: Exception) {
            0L
        } finally {
            try {
                retriever.release()
            } catch (ignored: Exception) {
                // sin acción
            }
        }
    }

    fun copyToCache(context: Context, uri: Uri, prefix: String): File? = try {
        val dir = File(context.cacheDir, "imports").apply { mkdirs() }
        val target = File(dir, "$prefix-${System.currentTimeMillis()}.mp4")
        context.contentResolver.openInputStream(uri)?.use { input ->
            target.outputStream().use { output -> input.copyTo(output) }
        }
        if (target.exists() && target.length() > 0L) target else null
    } catch (e: Exception) {
        null
    }
}
