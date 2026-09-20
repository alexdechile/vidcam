package com.vidcam.app.export

import android.content.ActivityNotFoundException
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import java.io.File

object GallerySaver {

    /** Copia el archivo exportado a la galería de vídeos del dispositivo. */
    fun saveToGallery(context: Context, file: File): Uri? {
        val values = ContentValues().apply {
            put(MediaStore.Video.Media.DISPLAY_NAME, file.name)
            put(MediaStore.Video.Media.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(
                    MediaStore.Video.Media.RELATIVE_PATH,
                    "${Environment.DIRECTORY_MOVIES}/VidCam",
                )
                put(MediaStore.Video.Media.IS_PENDING, 1)
            }
        }
        val collection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        val uri = try {
            context.contentResolver.insert(collection, values)
        } catch (e: Exception) {
            null
        } ?: return null

        try {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                file.inputStream().use { input -> input.copyTo(output) }
            }
        } catch (e: Exception) {
            context.contentResolver.delete(uri, null, null)
            return null
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val clear = ContentValues().apply {
                put(MediaStore.Video.Media.IS_PENDING, 0)
            }
            context.contentResolver.update(uri, clear, null, null)
        }
        return uri
    }

    /**
     * Abre el vídeo ya guardado en el visor de la galería del dispositivo.
     * Devuelve `true` si alguna aplicación pudo manejarlo.
     */
    fun openInGallery(context: Context, uri: Uri): Boolean {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "video/mp4")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            true
        } catch (e: ActivityNotFoundException) {
            false
        }
    }
}
