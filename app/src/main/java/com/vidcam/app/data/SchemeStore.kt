package com.vidcam.app.data

import com.vidcam.app.model.scheme.FactorySchemes
import com.vidcam.app.model.scheme.UsageScheme
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Esquema guardado en disco o provisto de fábrica con metadatos para UI.
 */
@Serializable
data class SavedScheme(
    val id: String,
    val name: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val isCustom: Boolean = true,
    val scheme: UsageScheme,
)

/**
 * Persistencia de esquemas de uso como JSON individuales dentro de [rootDir],
 * combinados con el catálogo de plantillas de fábrica ([FactorySchemes]).
 *
 * Desacoplado de Android (`Context`), permitiendo pruebas unitarias en la JVM.
 */
class SchemeStore(private val rootDir: File) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /**
     * Lista completa de esquemas: primero los predefinidos de fábrica,
     * seguidos de los personalizados creados por el usuario ordenados por fecha.
     */
    fun list(): List<SavedScheme> {
        val factory = FactorySchemes.ALL.map { scheme ->
            SavedScheme(
                id = scheme.id,
                name = scheme.name,
                createdAtMs = 0L,
                updatedAtMs = 0L,
                isCustom = false,
                scheme = scheme,
            )
        }
        val custom = listCustom()
        return factory + custom
    }

    /**
     * Lista únicamente los esquemas personalizados guardados por el usuario.
     */
    fun listCustom(): List<SavedScheme> =
        schemeFiles()
            .mapNotNull(::read)
            .sortedByDescending { it.updatedAtMs }

    fun load(id: String): SavedScheme? {
        // 1. Revisar si es de fábrica
        FactorySchemes.ALL.firstOrNull { it.id == id }?.let { scheme ->
            return SavedScheme(
                id = scheme.id,
                name = scheme.name,
                createdAtMs = 0L,
                updatedAtMs = 0L,
                isCustom = false,
                scheme = scheme,
            )
        }
        // 2. Revisar almacenamiento local
        return read(fileFor(id))
    }

    /**
     * Guarda un esquema personalizado con escritura atómica.
     */
    fun save(saved: SavedScheme): Boolean = try {
        rootDir.mkdirs()
        val target = fileFor(saved.id)
        val temp = File(rootDir, "${saved.id}.json.tmp")
        temp.writeText(json.encodeToString(SavedScheme.serializer(), saved.copy(isCustom = true)))
        if (target.exists() && !target.delete()) {
            false
        } else {
            temp.renameTo(target)
        }
    } catch (e: Exception) {
        false
    }

    /**
     * Elimina un esquema personalizado. No permite borrar esquemas de fábrica.
     */
    fun delete(id: String): Boolean {
        if (FactorySchemes.ALL.any { it.id == id }) return false
        val file = fileFor(id)
        return if (file.exists()) file.delete() else false
    }

    /**
     * Exporta un [UsageScheme] a texto JSON portable.
     */
    fun exportToJson(scheme: UsageScheme): String =
        json.encodeToString(UsageScheme.serializer(), scheme)

    /**
     * Importa un [UsageScheme] desde un texto JSON, tolerando campos desconocidos.
     */
    fun importFromJson(jsonString: String): UsageScheme? = try {
        json.decodeFromString(UsageScheme.serializer(), jsonString)
    } catch (e: Exception) {
        null
    }

    private fun fileFor(id: String) = File(rootDir, "$id.json")

    private fun schemeFiles(): List<File> =
        rootDir.listFiles { file -> file.isFile && file.name.endsWith(".json") }
            ?.toList()
            .orEmpty()

    private fun read(file: File): SavedScheme? = try {
        if (file.exists()) {
            json.decodeFromString(SavedScheme.serializer(), file.readText())
        } else {
            null
        }
    } catch (e: Exception) {
        null
    }
}
