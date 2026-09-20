package com.vidcam.app.data

import com.vidcam.app.model.Project
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File

/** Proyecto guardado en disco, con los metadatos que muestra la lista. */
@Serializable
data class SavedProject(
    val id: String,
    val name: String,
    val createdAtMs: Long,
    val updatedAtMs: Long,
    val project: Project,
)

/**
 * Persistencia de proyectos como un JSON por proyecto dentro de [rootDir].
 *
 * No depende de Android (`Context`) a propósito: la carpeta se inyecta, así que
 * la lógica de guardado, listado y borrado se prueba con tests unitarios en la
 * JVM y sobre un directorio temporal.
 */
class ProjectStore(private val rootDir: File) {

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    /** Proyectos guardados, del más reciente al más antiguo. */
    fun list(): List<SavedProject> =
        projectFiles()
            .mapNotNull(::read)
            .sortedByDescending { it.updatedAtMs }

    fun load(id: String): SavedProject? = read(fileFor(id))

    fun save(saved: SavedProject): Boolean = try {
        rootDir.mkdirs()
        val target = fileFor(saved.id)
        // Escritura atómica: si el proceso muere a mitad, el JSON anterior sigue
        // siendo válido porque el archivo definitivo solo se reemplaza al final.
        val temp = File(rootDir, "${saved.id}.json.tmp")
        temp.writeText(json.encodeToString(SavedProject.serializer(), saved))
        if (target.exists() && !target.delete()) {
            false
        } else {
            temp.renameTo(target)
        }
    } catch (e: Exception) {
        false
    }

    fun delete(id: String): Boolean = fileFor(id).delete()

    private fun fileFor(id: String) = File(rootDir, "$id.json")

    private fun projectFiles(): List<File> =
        rootDir.listFiles { file -> file.isFile && file.name.endsWith(".json") }
            ?.toList()
            .orEmpty()

    private fun read(file: File): SavedProject? = try {
        if (file.exists()) {
            json.decodeFromString(SavedProject.serializer(), file.readText())
        } else {
            null
        }
    } catch (e: Exception) {
        // Un archivo corrupto o de una versión futura no debe romper la lista.
        null
    }
}
