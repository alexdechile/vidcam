package com.vidcam.app.share

import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.OpenableColumns

/** Archivo recibido desde otra aplicación, ya clasificado por [kind]. */
data class SharedFile(
    val uri: Uri,
    val mimeType: String?,
    val displayName: String?,
) {
    val kind: SharedKind
        get() = SharedKinds.kindOf(mimeType, displayName)
}

/**
 * Lectura de los archivos que llegan por la hoja de compartir del sistema
 * (`ACTION_SEND`, `ACTION_SEND_MULTIPLE`) o al abrir un archivo con VidCam
 * (`ACTION_VIEW`).
 *
 * El emisor entrega URIs temporales con permiso de lectura mientras esta
 * actividad esté viva, así que todo lo recibido se copia al almacenamiento
 * propio antes de usarse (ver `EditorViewModel.importShared`).
 *
 * El tipo MIME se consulta al `ContentResolver` porque `ACTION_SEND_MULTIPLE`
 * declara un único tipo para todos los adjuntos y algunos emisores no declaran
 * ninguno.
 */
object SharedMedia {

    /** Archivos utilizables del [intent]; lista vacía si no trae ninguno. */
    fun fromIntent(context: Context, intent: Intent?): List<SharedFile> {
        if (intent == null) return emptyList()
        val candidates: List<Uri> = when (intent.action) {
            Intent.ACTION_SEND -> listOfNotNull(singleStream(intent))
            Intent.ACTION_SEND_MULTIPLE -> multipleStreams(intent)
            Intent.ACTION_VIEW -> listOfNotNull(intent.data)
            else -> return emptyList()
        }
        val declaredType = intent.type
        return candidates
            .filter { it.scheme == ContentResolver.SCHEME_CONTENT || it.scheme == ContentResolver.SCHEME_FILE }
            .distinctBy { it.toString() }
            .map { uri ->
                SharedFile(
                    uri = uri,
                    mimeType = mimeTypeOf(context, uri, declaredType),
                    displayName = displayNameOf(context, uri),
                )
            }
    }

    /** Nombre visible del archivo, útil para el aviso y para la extensión. */
    fun displayNameOf(context: Context, uri: Uri): String? = try {
        if (uri.scheme == ContentResolver.SCHEME_FILE) {
            uri.lastPathSegment
        } else {
            context.contentResolver
                .query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
                ?: uri.lastPathSegment
        }
    } catch (e: Exception) {
        null
    }

    private fun mimeTypeOf(context: Context, uri: Uri, declaredType: String?): String? {
        val resolved = try {
            context.contentResolver.getType(uri)
        } catch (e: Exception) {
            null
        }
        return SharedKinds.normalize(resolved) ?: SharedKinds.normalize(declaredType)
    }

    private fun singleStream(intent: Intent): Uri? =
        parcelableStream(intent) ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri

    private fun multipleStreams(intent: Intent): List<Uri> {
        val fromExtra: List<Uri> = streamList(intent)
        val fromClipData = buildList<Uri> {
            val clipData = intent.clipData ?: return@buildList
            for (index in 0 until clipData.itemCount) {
                clipData.getItemAt(index)?.uri?.let { add(it) }
            }
        }
        return fromExtra + fromClipData
    }

    @Suppress("DEPRECATION")
    private fun parcelableStream(intent: Intent): Uri? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }

    @Suppress("DEPRECATION")
    private fun streamList(intent: Intent): List<Uri> =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java).orEmpty()
        } else {
            intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM).orEmpty()
        }
}
