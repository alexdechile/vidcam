package com.vidcam.app.data

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.InputStream

/**
 * Metadata of a locally registered on-device LLM model file (`.task` or `.litertlm`).
 */
data class RegisteredModel(
    val name: String,
    val path: String,
    val sizeBytes: Long,
)

/**
 * Manages importing, validating, persisting, and deleting external `.task` / `.litertlm`
 * model files inside the app's private `filesDir/models/` directory without bundling
 * model weights in the APK.
 */
class LocalAiModelStore(
    private val modelsDir: File,
    private val prefsFile: File,
) {
    constructor(context: Context) : this(
        modelsDir = File(context.filesDir, "models"),
        prefsFile = File(context.filesDir, "ai_active_model.path"),
    )

    /**
     * Returns the currently registered model if its file still exists and is valid,
     * or `null` if none is configured or the file was removed.
     */
    fun activeModel(): RegisteredModel? {
        if (!prefsFile.isFile) return null
        val savedPath = runCatching { prefsFile.readText().trim() }.getOrNull().orEmpty()
        if (savedPath.isBlank()) return null
        val file = File(savedPath)
        val valid = validateModelFile(file).getOrNull() ?: return null
        return RegisteredModel(
            name = valid.name,
            path = valid.absolutePath,
            sizeBytes = valid.length(),
        )
    }

    /**
     * Imports a model stream into private storage after validating its extension and content.
     * Replaces any previously imported model to avoid accumulating multi-GB files.
     */
    fun importFromInputStream(
        displayName: String,
        inputStream: InputStream,
    ): Result<RegisteredModel> {
        val cleanName = sanitizeFileName(displayName)
        if (!isSupportedModelFileName(cleanName)) {
            return Result.failure(
                IllegalArgumentException("Formato no soportado. Selecciona un archivo .task o .litertlm"),
            )
        }

        return runCatching {
            modelsDir.mkdirs()
            val tempFile = File(modelsDir, "$cleanName.tmp")
            val targetFile = File(modelsDir, cleanName)
            try {
                tempFile.outputStream().use { output ->
                    inputStream.copyTo(output, bufferSize = DEFAULT_COPY_BUFFER_SIZE)
                }
                if (tempFile.length() <= 0L) {
                    tempFile.delete()
                    throw IllegalArgumentException("El archivo del modelo está vacío")
                }

                // Remove older model files so private storage only holds the active model.
                modelsDir.listFiles()?.forEach { existing ->
                    if (existing != tempFile && existing != targetFile) {
                        existing.delete()
                    }
                }
                if (targetFile.exists()) {
                    targetFile.delete()
                }
                if (!tempFile.renameTo(targetFile)) {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                }

                prefsFile.parentFile?.mkdirs()
                prefsFile.writeText(targetFile.absolutePath)

                RegisteredModel(
                    name = targetFile.name,
                    path = targetFile.absolutePath,
                    sizeBytes = targetFile.length(),
                )
            } catch (t: Throwable) {
                tempFile.delete()
                throw t
            }
        }
    }

    /**
     * Resolves a SAF `content://` URI's display name and copies the model into private storage.
     */
    fun importFromUri(context: Context, uri: Uri): Result<RegisteredModel> {
        val displayName = resolveDisplayName(context, uri)
        if (!isSupportedModelFileName(displayName)) {
            return Result.failure(
                IllegalArgumentException("Formato no soportado ($displayName). Usa un archivo .task o .litertlm"),
            )
        }
        val stream = context.contentResolver.openInputStream(uri)
            ?: return Result.failure(IllegalStateException("No se pudo abrir el archivo seleccionado"))
        return stream.use { importFromInputStream(displayName, it) }
    }

    /**
     * Deletes all imported model files and clears the persisted active model path.
     */
    fun clearActiveModel() {
        runCatching {
            modelsDir.listFiles()?.forEach { it.delete() }
            if (prefsFile.exists()) prefsFile.delete()
        }
    }

    private fun resolveDisplayName(context: Context, uri: Uri): String {
        val queried = runCatching {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) cursor.getString(idx) else null
                } else {
                    null
                }
            }
        }.getOrNull()
        val raw = queried?.takeIf { it.isNotBlank() } ?: uri.lastPathSegment.orEmpty()
        return sanitizeFileName(raw)
    }

    companion object {
        private const val DEFAULT_COPY_BUFFER_SIZE = 64 * 1024
        val SUPPORTED_EXTENSIONS = setOf("task", "litertlm")

        fun isSupportedModelFileName(fileName: String): Boolean {
            val trimmed = fileName.trim()
            if (trimmed.isEmpty()) return false
            val ext = trimmed.substringAfterLast('.', missingDelimiterValue = "").lowercase()
            return ext in SUPPORTED_EXTENSIONS
        }

        fun sanitizeFileName(rawName: String): String {
            val base = rawName.substringAfterLast('/').substringAfterLast('\\').trim()
            val safe = base.replace(Regex("[^A-Za-z0-9._-]"), "_")
            return safe.ifBlank { "model" }
        }

        fun validateModelFile(file: File): Result<File> {
            if (!file.exists() || !file.isFile) {
                return Result.failure(IllegalStateException("El archivo del modelo no existe"))
            }
            if (file.length() <= 0L) {
                return Result.failure(IllegalStateException("El archivo del modelo está vacío"))
            }
            if (!isSupportedModelFileName(file.name)) {
                return Result.failure(IllegalArgumentException("Extensión no soportada: ${file.name}"))
            }
            return Result.success(file)
        }
    }
}
