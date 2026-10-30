package com.vidcam.app.share

/**
 * Tipo de archivo que VidCam sabe recibir desde otra aplicación.
 *
 * [UNKNOWN] cubre todo lo que la app no puede usar (documentos, comprimidos,
 * APK…): se informa y se descarta, sin tocar el proyecto.
 */
enum class SharedKind { VIDEO, IMAGE, AUDIO, UNKNOWN }

/**
 * Clasificación de un archivo recibido a partir de su tipo MIME o, cuando el
 * emisor no lo declara, de la extensión de su nombre visible.
 *
 * No depende de Android a propósito: la lógica de decisión se prueba con tests
 * unitarios en la JVM (ver `SharedMediaTest`), mientras que la lectura del
 * `Intent` y del `ContentResolver` vive en [SharedMedia].
 */
object SharedKinds {

    private val VIDEO_EXTENSIONS = setOf(
        "mp4", "m4v", "mov", "mkv", "webm", "3gp", "3g2", "avi", "ts", "mts", "m2ts",
    )

    private val IMAGE_EXTENSIONS = setOf(
        "png", "jpg", "jpeg", "webp", "heic", "heif", "gif", "bmp", "avif",
    )

    private val AUDIO_EXTENSIONS = setOf(
        "mp3", "m4a", "aac", "ogg", "oga", "opus", "wav", "flac", "amr", "wma",
    )

    /** Tipos MIME habituales con la extensión con la que se guarda la copia. */
    private val MIME_EXTENSIONS = mapOf(
        "video/mp4" to "mp4",
        "video/quicktime" to "mov",
        "video/webm" to "webm",
        "video/x-matroska" to "mkv",
        "video/3gpp" to "3gp",
        "image/png" to "png",
        "image/jpeg" to "jpg",
        "image/webp" to "webp",
        "image/heic" to "heic",
        "image/heif" to "heif",
        "audio/mpeg" to "mp3",
        "audio/mp4" to "m4a",
        "audio/m4a" to "m4a",
        "audio/x-m4a" to "m4a",
        "audio/ogg" to "ogg",
        "audio/opus" to "opus",
        "audio/wav" to "wav",
        "audio/x-wav" to "wav",
        "audio/flac" to "flac",
        "audio/aac" to "aac",
    )

    /**
     * Clasifica el archivo. Primero manda el tipo MIME —con los parámetros
     * (`; charset=…`) recortados— y, si no sirve, la extensión del nombre.
     */
    fun kindOf(mimeType: String?, displayName: String?): SharedKind {
        val mime = normalize(mimeType)
        if (mime != null) {
            when {
                mime.startsWith("video/") -> return SharedKind.VIDEO
                mime.startsWith("image/") -> return SharedKind.IMAGE
                mime.startsWith("audio/") -> return SharedKind.AUDIO
            }
        }
        return when (extensionOf(displayName)) {
            in VIDEO_EXTENSIONS -> SharedKind.VIDEO
            in IMAGE_EXTENSIONS -> SharedKind.IMAGE
            in AUDIO_EXTENSIONS -> SharedKind.AUDIO
            else -> SharedKind.UNKNOWN
        }
    }

    /**
     * Extensión con la que se guarda la copia propia del archivo: la del nombre
     * visible si es razonable, si no la del tipo MIME y, en último término, una
     * por defecto según [kind].
     */
    fun extensionFor(mimeType: String?, displayName: String?, kind: SharedKind): String {
        val fromName = extensionOf(displayName)
        if (fromName.matches(Regex("[a-z0-9]{1,5}"))) return fromName
        MIME_EXTENSIONS[normalize(mimeType)]?.let { return it }
        return when (kind) {
            SharedKind.VIDEO -> "mp4"
            SharedKind.IMAGE -> "png"
            SharedKind.AUDIO -> "m4a"
            SharedKind.UNKNOWN -> "bin"
        }
    }

    /** Extensión en minúsculas del nombre visible, sin el punto; `""` si no hay. */
    fun extensionOf(displayName: String?): String {
        // Sin `missingDelimiterValue` explícito: si no hay carpeta, el nombre
        // visible completo es el que se analiza.
        val name = displayName?.substringAfterLast('/')?.trim().orEmpty()
        val dot = name.lastIndexOf('.')
        if (dot <= 0 || dot == name.lastIndex) return ""
        return name.substring(dot + 1).lowercase()
    }

    /** Tipo MIME en minúsculas y sin parámetros; `null` si es genérico o vacío. */
    fun normalize(mimeType: String?): String? =
        mimeType
            ?.substringBefore(';')
            ?.trim()
            ?.lowercase()
            ?.takeIf { it.isNotEmpty() && it != "*/*" && it != "application/octet-stream" }
}
