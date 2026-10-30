package com.vidcam.app.editor

import android.app.Application
import android.net.Uri
import android.os.SystemClock
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vidcam.app.data.ProjectStore
import com.vidcam.app.data.SavedProject
import com.vidcam.app.export.GallerySaver
import com.vidcam.app.export.ShareUtils
import com.vidcam.app.export.VideoExporter
import com.vidcam.app.media.GifAnimation
import com.vidcam.app.media.MediaProbe
import com.vidcam.app.media.MusicRepository
import com.vidcam.app.model.ColorFilterPreset
import com.vidcam.app.model.ShakeIntensity
import com.vidcam.app.model.BackgroundLayer
import com.vidcam.app.model.LayerKeyframe
import com.vidcam.app.model.LayerKind
import com.vidcam.app.model.LayerTiming
import com.vidcam.app.model.MAX_DURATION_MS
import com.vidcam.app.model.MIN_TARGET_DURATION_MS
import com.vidcam.app.model.MusicTrack
import com.vidcam.app.model.OverlayLayer
import com.vidcam.app.model.Project
import com.vidcam.app.model.TimelineMath
import com.vidcam.app.model.VideoClip
import com.vidcam.app.model.VideoWindow
import com.vidcam.app.model.VideoWindowMath
import com.vidcam.app.model.WideLensMode
import com.vidcam.app.model.clearAnimation
import com.vidcam.app.model.removeKeyframeAt
import com.vidcam.app.model.seedKeyframesAt
import com.vidcam.app.model.shouldSampleKeyframe
import com.vidcam.app.model.upsertKeyframe
import com.vidcam.app.share.SharedFile
import com.vidcam.app.share.SharedKind
import com.vidcam.app.share.SharedKinds
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class EditorViewModel(application: Application) : AndroidViewModel(application) {

    private val _project = MutableStateFlow(Project())
    val project: StateFlow<Project> = _project.asStateFlow()

    private val _exporting = MutableStateFlow(false)
    val exporting: StateFlow<Boolean> = _exporting.asStateFlow()

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _lastExport = MutableStateFlow<File?>(null)
    val lastExport: StateFlow<File?> = _lastExport.asStateFlow()

    private val _motionRecording = MutableStateFlow(false)
    val motionRecording: StateFlow<Boolean> = _motionRecording.asStateFlow()

    private val _canUndo = MutableStateFlow(false)
    val canUndo: StateFlow<Boolean> = _canUndo.asStateFlow()

    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    private val _savedProjects = MutableStateFlow<List<SavedProject>>(emptyList())
    val savedProjects: StateFlow<List<SavedProject>> = _savedProjects.asStateFlow()

    private val _currentProjectId = MutableStateFlow<String?>(null)
    val currentProjectId: StateFlow<String?> = _currentProjectId.asStateFlow()

    private val _currentProjectName = MutableStateFlow<String?>(null)
    val currentProjectName: StateFlow<String?> = _currentProjectName.asStateFlow()

    private val _dirty = MutableStateFlow(false)
    val dirty: StateFlow<Boolean> = _dirty.asStateFlow()

    private val musicRepository = MusicRepository(application)
    private val exporter = VideoExporter(application)
    private val projectStore = ProjectStore(File(application.filesDir, "projects"))
    private val history = UndoHistory()

    /** Última versión que coincide con lo guardado en disco, para calcular `dirty`. */
    private var savedSnapshot: Project = _project.value

    private val context: Application
        get() = getApplication()

    init {
        refreshSavedProjects()
    }

    fun consumeMessage() {
        _message.value = null
    }

    private fun update(key: String? = null, block: (Project) -> Project) {
        val before = _project.value
        val next = block(before)
        if (next == before) return
        history.record(before, key, SystemClock.elapsedRealtime())
        _project.value = next
        syncHistory()
    }

    private fun syncHistory() {
        _canUndo.value = history.canUndo
        _canRedo.value = history.canRedo
        _dirty.value = _project.value != savedSnapshot
    }

    // --- Deshacer / rehacer ------------------------------------------------

    fun undo() {
        val previous = history.undo(_project.value) ?: return
        _project.value = previous
        syncHistory()
    }

    fun redo() {
        val next = history.redo(_project.value) ?: return
        _project.value = next
        syncHistory()
    }

    // --- Proyecto ----------------------------------------------------------

    /** Descarta el trabajo actual y empieza un proyecto vacío sin guardarlo. */
    fun newProject() {
        if (_exporting.value) return
        _motionRecording.value = false
        _lastExport.value = null
        _project.value = Project()
        _currentProjectId.value = null
        _currentProjectName.value = null
        savedSnapshot = _project.value
        history.clear()
        syncHistory()
    }

    fun refreshSavedProjects() {
        viewModelScope.launch {
            _savedProjects.value = withContext(Dispatchers.IO) { projectStore.list() }
        }
    }

    /**
     * Guarda el proyecto actual como JSON en el almacenamiento propio de la app.
     * Con [asNew] siempre crea una entrada nueva; sin él sobrescribe la que esté
     * abierta y, si no hay ninguna, crea la primera.
     */
    fun saveProject(name: String? = null, asNew: Boolean = false) {
        val project = _project.value
        if (project.isEmpty) {
            _message.value = "Todavía no hay nada que guardar"
            return
        }
        val nowMs = System.currentTimeMillis()
        val currentId = _currentProjectId.value
        val currentName = _currentProjectName.value
        viewModelScope.launch {
            val newId = "proj-$nowMs"
            val id = if (asNew || currentId == null) newId else currentId
            val previous = if (asNew || currentId == null) {
                null
            } else {
                withContext(Dispatchers.IO) { projectStore.load(id) }
            }
            val saved = SavedProject(
                id = id,
                name = name?.trim()?.takeIf { it.isNotBlank() }
                    ?: currentName
                    ?: defaultProjectName(nowMs),
                createdAtMs = previous?.createdAtMs ?: nowMs,
                updatedAtMs = nowMs,
                project = project,
            )
            val ok = withContext(Dispatchers.IO) { projectStore.save(saved) }
            if (ok) {
                _currentProjectId.value = saved.id
                _currentProjectName.value = saved.name
                savedSnapshot = project
                syncHistory()
                _message.value = "Proyecto guardado: ${saved.name}"
                refreshSavedProjects()
            } else {
                _message.value = "No se pudo guardar el proyecto"
            }
        }
    }

    fun openProject(saved: SavedProject) {
        if (_exporting.value) return
        _motionRecording.value = false
        _lastExport.value = null
        _project.value = saved.project
        _currentProjectId.value = saved.id
        _currentProjectName.value = saved.name
        savedSnapshot = saved.project
        history.clear()
        syncHistory()
        _message.value = "Proyecto abierto: ${saved.name}"
    }

    fun deleteProject(id: String) {
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { projectStore.delete(id) }
            if (_currentProjectId.value == id) {
                // El proyecto sigue abierto, pero ya no está en disco: vuelve a
                // contar como cambios sin guardar.
                _currentProjectId.value = null
                _currentProjectName.value = null
                savedSnapshot = Project()
                syncHistory()
            }
            _message.value = if (ok) "Proyecto eliminado" else "No se pudo eliminar"
            refreshSavedProjects()
        }
    }

    private fun defaultProjectName(nowMs: Long): String {
        val format = SimpleDateFormat("d MMM HH:mm", Locale.forLanguageTag("es"))
        return "Proyecto ${format.format(Date(nowMs))}"
    }

    // --- Clips -------------------------------------------------------------

    fun addRecordedClip(
        file: File,
        speed: Float = 1f,
        colorFilter: ColorFilterPreset = ColorFilterPreset.NINGUNO,
        wideLens: WideLensMode = WideLensMode.NORMAL,
        shake: ShakeIntensity = ShakeIntensity.NINGUNO,
    ) {
        val uri = Uri.fromFile(file)
        val duration = MediaProbe.durationMs(context, uri)
        if (duration <= 0L) {
            _message.value = "No se pudo leer la grabación"
            return
        }
        addClip(uri.toString(), duration, speed, colorFilter, wideLens, shake)
    }

    fun importVideo(uri: Uri) {
        viewModelScope.launch { importVideoInternal(uri) }
    }

    /**
     * Copia [uri] al almacenamiento propio y la agrega como clip. Devuelve
     * `true` cuando el clip entró a la línea de tiempo.
     */
    private suspend fun importVideoInternal(uri: Uri): Boolean {
        val stored = withContext(Dispatchers.IO) {
            MediaProbe.copyToStorage(context, uri, "clip")
        }
        if (stored == null) {
            _message.value = "No se pudo importar el vídeo"
            return false
        }
        val duration = withContext(Dispatchers.IO) {
            MediaProbe.durationMs(context, Uri.fromFile(stored))
        }
        if (duration <= 0L) {
            stored.delete()
            _message.value = "No se pudo leer el vídeo importado"
            return false
        }
        return addClip(Uri.fromFile(stored).toString(), duration)
    }

    private fun addClip(
        uri: String,
        sourceDurationMs: Long,
        speed: Float = 1f,
        colorFilter: ColorFilterPreset = ColorFilterPreset.NINGUNO,
        wideLens: WideLensMode = WideLensMode.NORMAL,
        shake: ShakeIntensity = ShakeIntensity.NINGUNO,
    ): Boolean {
        val remaining = MAX_DURATION_MS - _project.value.totalDurationMs
        if (remaining <= 0L) {
            _message.value = "Límite de 30 s alcanzado"
            return false
        }
        val clip = VideoClip(
            id = "clip-${System.currentTimeMillis()}",
            uri = uri,
            sourceDurationMs = sourceDurationMs,
            playbackSpeed = speed,
            colorFilter = colorFilter,
            wideLens = wideLens,
            shake = shake,
        )
        val trimmed = TimelineMath.trimToMax(clip, minOf(remaining, MAX_DURATION_MS))
        update { it.copy(clips = it.clips + trimmed) }
        return true
    }

    fun removeClip(id: String) =
        update { project -> project.copy(clips = project.clips.filterNot { it.id == id }) }

    /**
     * Cambia la velocidad de reproducción (cámara lenta/rápida). Si la duración
     * efectiva resultante supera el presupuesto restante de 30 s, el clip se
     * recorta por el final; si no cabe ni un fotograma, no se aplica.
     */
    fun setClipSpeed(id: String, speed: Float) {
        val project = _project.value
        val clip = project.clips.firstOrNull { it.id == id } ?: return
        if (speed <= 0f || speed == clip.playbackSpeed) return
        val others = project.clips.filterNot { it.id == id }.sumOf { it.effectiveDurationMs }
        val maxTimeline = (MAX_DURATION_MS - others).coerceAtLeast(0L)
        val candidate = clip.copy(playbackSpeed = speed)
        val fitted = if (candidate.effectiveDurationMs <= maxTimeline) {
            candidate
        } else if (maxTimeline <= 0L) {
            _message.value = "Límite de 30 s alcanzado"
            return
        } else {
            val maxSource = (maxTimeline * speed).toLong()
            TimelineMath.clampTrim(candidate, clip.trimStartMs, clip.trimStartMs + maxSource)
        }
        update { p -> p.copy(clips = p.clips.map { if (it.id == id) fitted else it }) }
    }

    fun setClipColorFilter(id: String, preset: ColorFilterPreset) =
        update("filter:$id") { project ->
            project.copy(
                clips = project.clips.map { clip ->
                    if (clip.id == id) clip.copy(colorFilter = preset) else clip
                },
            )
        }

    fun setClipShake(id: String, intensity: ShakeIntensity) =
        update("shake:$id") { project ->
            project.copy(
                clips = project.clips.map { clip ->
                    if (clip.id == id) clip.copy(shake = intensity) else clip
                },
            )
        }

    fun setClipWideLens(id: String, wideLens: WideLensMode) =
        update("wide:$id") { project ->
            project.copy(
                clips = project.clips.map { clip ->
                    if (clip.id == id) clip.copy(wideLens = wideLens) else clip
                },
            )
        }

    fun updateTrim(id: String, startMs: Long, endMs: Long) = update("trim:$id") { project ->
        val clip = project.clips.firstOrNull { it.id == id } ?: return@update project
        val others = project.clips.filterNot { it.id == id }.sumOf { it.effectiveDurationMs }
        val maxTimeline = (MAX_DURATION_MS - others).coerceAtLeast(0L)
        val maxForClip = (maxTimeline * clip.playbackSpeed.coerceAtLeast(0.01f)).toLong()
        project.copy(
            clips = project.clips.map { thisClip ->
                if (thisClip.id != id) {
                    thisClip
                } else {
                    val boundedEnd = endMs.coerceAtMost(startMs + maxForClip)
                    TimelineMath.clampTrim(thisClip, startMs, boundedEnd)
                }
            },
        )
    }

    // --- Archivos compartidos desde otras apps ------------------------------

    /**
     * Incorpora lo que otra aplicación compartió con VidCam: los vídeos entran
     * como clips, las imágenes como capas PNG y el primer audio como música del
     * proyecto (el modelo tiene una sola pista, así que los audios siguientes
     * se informan como omitidos). Todo se copia antes al almacenamiento propio,
     * porque el permiso de lectura del remitente caduca con la actividad.
     *
     * Cada incorporación pasa por `update`, así que entra al historial y se
     * revierte con "Deshacer". Se publica un solo mensaje con el resultado,
     * conservando el motivo concreto cuando no se pudo agregar nada.
     */
    fun importShared(files: List<SharedFile>) {
        if (files.isEmpty()) return
        viewModelScope.launch {
            val previousMessage = _message.value
            var videos = 0
            var images = 0
            var audios = 0
            var omitted = 0
            // Los vídeos entran primero: la duración del proyecto que ven las
            // capas y la música es la definitiva, no la de un timeline vacío.
            for (file in files.sortedBy { kindOrder(it.kind) }) {
                when (file.kind) {
                    SharedKind.VIDEO ->
                        if (importVideoInternal(file.uri)) videos++ else omitted++

                    SharedKind.IMAGE -> {
                        val extension = SharedKinds.extensionFor(
                            file.mimeType,
                            file.displayName,
                            file.kind,
                        )
                        if (addPngLayerInternal(file.uri, extension)) images++ else omitted++
                    }

                    SharedKind.AUDIO ->
                        if (audios == 0 && setSharedMusic(file)) audios++ else omitted++

                    SharedKind.UNKNOWN -> omitted++
                }
            }
            val added = videos + images + audios
            _message.value = when {
                added > 0 -> sharedSummary(videos, images, audios, omitted)
                _message.value == previousMessage -> "No se pudo agregar lo que compartiste"
                else -> _message.value
            }
        }
    }

    /** Orden de incorporación: primero los clips, después capas y música. */
    private fun kindOrder(kind: SharedKind): Int = when (kind) {
        SharedKind.VIDEO -> 0
        SharedKind.IMAGE -> 1
        SharedKind.AUDIO -> 2
        SharedKind.UNKNOWN -> 3
    }

    /** Aviso único del resultado: qué entró y cuántos archivos quedaron fuera. */
    private fun sharedSummary(videos: Int, images: Int, audios: Int, omitted: Int): String {
        val parts = buildList {
            if (videos > 0) add(if (videos == 1) "1 vídeo" else "$videos vídeos")
            if (images > 0) add(if (images == 1) "1 imagen" else "$images imágenes")
            if (audios > 0) {
                add(if (audios == 1) "1 pista de música" else "$audios pistas de música")
            }
        }
        val listed = parts.joinToString(", ")
        return if (omitted > 0) {
            "Desde otra app: $listed · $omitted sin agregar"
        } else {
            "Desde otra app: $listed"
        }
    }

    // --- Música ------------------------------------------------------------

    fun loadMusic(onLoaded: (List<MusicTrack>) -> Unit) {
        viewModelScope.launch {
            val tracks = withContext(Dispatchers.IO) { musicRepository.loadTracks() }
            onLoaded(tracks)
        }
    }

    fun setMusic(track: MusicTrack) =
        update { it.copy(music = track, musicEnabled = true, musicStartMs = 0L) }

    /**
     * Copia un audio compartido desde otra app y lo deja como música del
     * proyecto. Devuelve `true` si la pista quedó seleccionada.
     */
    private suspend fun setSharedMusic(file: SharedFile): Boolean {
        val extension = SharedKinds.extensionFor(file.mimeType, file.displayName, file.kind)
        val stored = withContext(Dispatchers.IO) {
            MediaProbe.copyToStorage(context, file.uri, "musica", extension)
        }
        if (stored == null) {
            _message.value = "No se pudo agregar la música compartida"
            return false
        }
        val duration = withContext(Dispatchers.IO) {
            MediaProbe.durationMs(context, Uri.fromFile(stored))
        }
        if (duration <= 0L) {
            stored.delete()
            _message.value = "No se pudo leer la música compartida"
            return false
        }
        val title = file.displayName
            ?.substringBeforeLast('.')
            ?.takeIf { it.isNotBlank() }
            ?: "Música compartida"
        update {
            it.copy(
                music = MusicTrack(
                    id = System.currentTimeMillis(),
                    uri = Uri.fromFile(stored).toString(),
                    title = title,
                    artist = "Compartida",
                    durationMs = duration,
                ),
                musicEnabled = true,
                musicStartMs = 0L,
            )
        }
        return true
    }

    fun clearMusic() =
        update { it.copy(music = null, musicEnabled = false, musicStartMs = 0L) }

    fun setMusicEnabled(enabled: Boolean) =
        update { it.copy(musicEnabled = enabled && it.music != null) }

    fun setMusicStart(ms: Long) = update("music-start") { project ->
        val max = (project.music?.durationMs ?: 0L).coerceAtLeast(0L)
        project.copy(musicStartMs = ms.coerceIn(0L, max))
    }

    fun setOriginalAudioMuted(muted: Boolean) =
        update { it.copy(originalAudioMuted = muted) }

    // --- Capas -------------------------------------------------------------

    /**
     * Agrega una capa PNG. Los stickers integrados (`asset://`) se usan tal cual;
     * una imagen elegida de la galería se copia al almacenamiento propio para que
     * el proyecto guardado siga funcionando cuando caduque el permiso del
     * selector.
     */
    fun addPngLayer(uri: Uri) {
        viewModelScope.launch { addPngLayerInternal(uri) }
    }

    /**
     * Copia [uri] al almacenamiento propio y la agrega como capa de imagen.
     * Devuelve `true` si la capa entró al proyecto.
     *
     * La copia se inspecciona por su cabecera: si es un GIF (compartido desde
     * otra app o elegido de la galería) la capa queda animada, aunque el archivo
     * se haya guardado con extensión `.png`.
     */
    private suspend fun addPngLayerInternal(uri: Uri, extension: String = "png"): Boolean {
        if (uri.scheme == "asset") {
            addPngLayerWithUri(uri.toString(), LayerKind.PNG)
            return true
        }
        val stored = withContext(Dispatchers.IO) {
            MediaProbe.copyToStorage(context, uri, "png", extension)
        }
        if (stored == null) {
            _message.value = "No se pudo agregar la imagen"
            return false
        }
        val isGif = withContext(Dispatchers.IO) { GifAnimation.isGif(stored) }
        addPngLayerWithUri(
            uri = Uri.fromFile(stored).toString(),
            kind = if (isGif) LayerKind.GIF else LayerKind.PNG,
        )
        return true
    }

    private fun addPngLayerWithUri(uri: String, kind: LayerKind = LayerKind.PNG) =
        update { project ->
            val nextZ = project.layers.maxByOrNull { it.zIndex }?.zIndex ?: -1
            project.copy(
                layers = project.layers + newLayerWindow(
                    project = project,
                    layer = OverlayLayer(
                        id = "layer-${System.currentTimeMillis()}",
                        kind = kind,
                        uri = uri,
                        label = if (kind == LayerKind.GIF) "GIF" else "Imagen",
                        zIndex = nextZ + 1,
                    ),
                ),
            )
        }

    fun addTextLayer(text: String, fontName: String?) = update { project ->
        val nextZ = project.layers.maxByOrNull { it.zIndex }?.zIndex ?: -1
        project.copy(
            layers = project.layers + newLayerWindow(
                project = project,
                layer = OverlayLayer(
                    id = "layer-${System.currentTimeMillis()}",
                    kind = LayerKind.TEXT,
                    text = text,
                    fontName = fontName,
                    label = text.ifBlank { "Texto" },
                    zIndex = nextZ + 1,
                ),
            ),
        )
    }

    /**
     * Ventana inicial de una capa recién añadida: todo el vídeo actual, con los
     * mismos límites que usan los controles de duración.
     */
    private fun newLayerWindow(project: Project, layer: OverlayLayer): OverlayLayer {
        val totalMs = project.outputDurationMs
        val durationMs = totalMs.coerceAtLeast(1000L)
        return LayerTiming.withDuration(layer, durationMs, totalMs)
    }

    fun updateLayer(layer: OverlayLayer) = update("layer-edit:${layer.id}") { project ->
        project.copy(layers = project.layers.map { if (it.id == layer.id) layer else it })
    }

    /**
     * Cambia cuántos segundos permanece en pantalla la capa. La duración se
     * acota al tiempo real del proyecto y el inicio no se mueve.
     */
    fun setLayerDuration(id: String, durationMs: Long) =
        update("layer-duration:$id") { project ->
            project.copy(
                layers = project.layers.map {
                    if (it.id == id) {
                        LayerTiming.withDuration(it, durationMs, project.outputDurationMs)
                    } else {
                        it
                    }
                },
            )
        }

    /**
     * Cambia en qué segundo aparece la capa, conservando su duración en
     * pantalla.
     */
    fun setLayerStart(id: String, startMs: Long) =
        update("layer-start:$id") { project ->
            project.copy(
                layers = project.layers.map {
                    if (it.id == id) {
                        LayerTiming.withStart(it, startMs, project.outputDurationMs)
                    } else {
                        it
                    }
                },
            )
        }

    fun removeLayer(id: String) =
        update { project -> project.copy(layers = project.layers.filterNot { it.id == id }) }

    /**
     * Mueve una capa una posición hacia arriba (incrementa zIndex).
     * Reordena todas las capas para mantener zIndex contiguos.
     */
    fun moveLayerUp(id: String) = update("layer-move-up:$id") { project ->
        val layers = project.layers.toMutableList()
        val index = layers.indexOfFirst { it.id == id }
        if (index <= 0) return@update project // Ya está arriba
        val layer = layers.removeAt(index)
        layers.add(index - 1, layer)
        // Reasignar zIndex contiguos (0..N-1)
        project.copy(layers = layers.mapIndexed { i, l -> l.copy(zIndex = i) })
    }

    /**
     * Mueve una capa una posición hacia abajo (decrementa zIndex).
     * Reordena todas las capas para mantener zIndex contiguos.
     */
    fun moveLayerDown(id: String) = update("layer-move-down:$id") { project ->
        val layers = project.layers.toMutableList()
        val index = layers.indexOfFirst { it.id == id }
        if (index < 0 || index >= layers.size - 1) return@update project // Ya está abajo
        val layer = layers.removeAt(index)
        layers.add(index + 1, layer)
        // Reasignar zIndex contiguos (0..N-1)
        project.copy(layers = layers.mapIndexed { i, l -> l.copy(zIndex = i) })
    }

    /**
     * Mueve un fondo una posición hacia arriba (incrementa zIndex).
     */
    fun moveBackgroundUp(id: String) = update("bg-move-up:$id") { project ->
        val backgrounds = project.backgrounds.toMutableList()
        val index = backgrounds.indexOfFirst { it.id == id }
        if (index <= 0) return@update project
        val bg = backgrounds.removeAt(index)
        backgrounds.add(index - 1, bg)
        project.copy(backgrounds = backgrounds.mapIndexed { i, b -> b.copy(zIndex = i) })
    }

    /**
     * Mueve un fondo una posición hacia abajo (decrementa zIndex).
     */
    fun moveBackgroundDown(id: String) = update("bg-move-down:$id") { project ->
        val backgrounds = project.backgrounds.toMutableList()
        val index = backgrounds.indexOfFirst { it.id == id }
        if (index < 0 || index >= backgrounds.size - 1) return@update project
        val bg = backgrounds.removeAt(index)
        backgrounds.add(index + 1, bg)
        project.copy(backgrounds = backgrounds.mapIndexed { i, b -> b.copy(zIndex = i) })
    }

    // --- Fondos y ventana de vídeo -----------------------------------------

    /**
     * Agrega un PNG de fondo copiándolo al almacenamiento propio, igual que las
     * capas, para que el proyecto guardado siga funcionando después de que
     * caduque el permiso del selector.
     */
    fun addBackground(uri: Uri, startMs: Long, endMs: Long) {
        if (endMs <= startMs) {
            _message.value = "El tramo del fondo debe durar más de cero"
            return
        }
        viewModelScope.launch {
            val stored = withContext(Dispatchers.IO) {
                MediaProbe.copyToStorage(context, uri, "fondo", "png")
            }
            if (stored == null) {
                _message.value = "No se pudo agregar la imagen de fondo"
                return@launch
            }
            addBackgroundWithUri(Uri.fromFile(stored).toString(), startMs, endMs)
        }
    }

    private fun addBackgroundWithUri(uri: String, startMs: Long, endMs: Long) {
        val id = "bg-${System.currentTimeMillis()}"
        update("background-add:$id") { project ->
            val total = project.outputDurationMs
            val nextZ = project.backgrounds.maxByOrNull { it.zIndex }?.zIndex ?: -1
            val background = BackgroundLayer(
                id = id,
                uri = uri,
                startMs = startMs,
                endMs = endMs,
                zIndex = nextZ + 1,
            )
            project.copy(
                backgrounds = VideoWindowMath.trimOverlaps(
                    backgrounds = project.backgrounds,
                    incoming = background,
                    totalMs = total,
                ),
            )
        }
    }

    fun updateBackground(background: BackgroundLayer) {
        if (background.endMs <= background.startMs) {
            _message.value = "El tramo del fondo debe durar más de cero"
            return
        }
        update("background-edit:${background.id}") { project ->
            val total = project.outputDurationMs
            val others = project.backgrounds.filterNot { it.id == background.id }
            project.copy(
                backgrounds = VideoWindowMath.trimOverlaps(
                    backgrounds = others,
                    incoming = background,
                    totalMs = total,
                ),
            )
        }
    }

    fun removeBackground(id: String) =
        update("background-remove:$id") { project ->
            project.copy(backgrounds = project.backgrounds.filterNot { it.id == id })
        }

    /** Actualiza el transform global de la ventana dentro de los límites válidos. */
    fun updateVideoWindow(window: VideoWindow) = update("video-window") { project ->
        project.copy(
            videoWindow = window.copy(
                x = window.x.coerceIn(0f, 1f),
                y = window.y.coerceIn(0f, 1f),
                scale = window.scale.coerceIn(
                    VideoWindowMath.MIN_SCALE,
                    VideoWindowMath.MAX_SCALE,
                ),
            ),
        )
    }

    fun resetVideoWindow() = update("video-window") { project ->
        project.copy(
            videoWindow = project.videoWindow.copy(
                x = 0.5f,
                y = 0.5f,
                scale = 1f,
                rotationDeg = 0f,
            ),
        )
    }

    // --- Duración fijada de antemano ---------------------------------------

    /**
     * Fija cuánto dura el vídeo cuando la línea de tiempo no tiene clips (un
     * proyecto solo con PNG) y cuánto graba la cámara de una vez.
     *
     * En un proyecto sin clips, las capas y los fondos que llegaban hasta el
     * final se estiran o recortan con la nueva duración; así el ajuste no deja
     * huecos ni material huérfano.
     */
    fun setTargetDuration(durationMs: Long) {
        val target = durationMs.coerceIn(MIN_TARGET_DURATION_MS, MAX_DURATION_MS)
        update("target-duration") { project ->
            if (project.targetDurationMs == target) return@update project
            val previousEnd = project.outputDurationMs
            val stillOnly = project.clips.isEmpty()
            project.copy(
                targetDurationMs = target,
                layers = if (stillOnly) {
                    project.layers.map { layer ->
                        if (layer.endMs >= previousEnd) {
                            LayerTiming.withDuration(layer, target - layer.startMs, target)
                        } else {
                            layer
                        }
                    }
                } else {
                    project.layers
                },
                backgrounds = if (stillOnly) {
                    project.backgrounds.map { background ->
                        if (background.endMs >= previousEnd) {
                            background.copy(endMs = target)
                        } else {
                            background
                        }
                    }
                } else {
                    project.backgrounds
                },
            )
        }
    }

    // --- Animación de capas -------------------------------------------------

    fun setMotionRecording(recording: Boolean) {
        _motionRecording.value = recording
    }

    /**
     * Aplica el resultado de un gesto sobre una capa. Si la grabación está activa
     * o la capa ya tiene animación, el cambio se guarda como fotograma clave en
     * [atMs]; si no, actualiza el transform fijo. Con [force] se salta la
     * decimación para no perder la última muestra de un gesto.
     *
     * La primera muestra de una capa sin animación solo fija el punto de partida
     * en el tiempo actual: así la capa se mantiene quieta hasta que empieza el
     * gesto, en vez de derivar desde el inicio de la línea de tiempo. El
     * movimiento se registra desde la muestra siguiente (unos milisegundos
     * después).
     *
     * Todas las muestras de un mismo gesto comparten clave de historial, así que
     * "deshacer" retrocede el gesto completo y no un fotograma intermedio.
     */
    fun applyLayerGesture(layer: OverlayLayer, atMs: Long, force: Boolean = false) =
        update("gesture:${layer.id}") { project ->
            val target = project.layers.firstOrNull { it.id == layer.id }
                ?: return@update project
            if (!_motionRecording.value && target.keyframes.isEmpty()) {
                return@update project.copy(
                    layers = project.layers.map {
                        if (it.id != layer.id) {
                            it
                        } else {
                            it.copy(
                                x = layer.x,
                                y = layer.y,
                                scale = layer.scale,
                                rotationDeg = layer.rotationDeg,
                            )
                        }
                    },
                )
            }

            if (target.keyframes.isEmpty()) {
                val seeded = target.seedKeyframesAt(atMs)
                return@update project.copy(
                    layers = project.layers.map { if (it.id == layer.id) seeded else it },
                )
            }

            val candidate = LayerKeyframe(
                timeMs = atMs,
                x = layer.x,
                y = layer.y,
                scale = layer.scale,
                rotationDeg = layer.rotationDeg,
            )
            val previous = target.keyframes.lastOrNull { it.timeMs <= atMs }
            val updated = if (force || shouldSampleKeyframe(previous, candidate)) {
                target.upsertKeyframe(candidate).copy(
                    x = layer.x,
                    y = layer.y,
                    scale = layer.scale,
                    rotationDeg = layer.rotationDeg,
                )
            } else {
                target
            }
            project.copy(
                layers = project.layers.map { if (it.id == layer.id) updated else it },
            )
        }

    fun removeLayerKeyframe(id: String, timeMs: Long) = update { project ->
        project.copy(
            layers = project.layers.map { layer ->
                if (layer.id == id) layer.removeKeyframeAt(timeMs) else layer
            },
        )
    }

    fun clearLayerAnimation(id: String) = update { project ->
        project.copy(
            layers = project.layers.map { layer ->
                if (layer.id == id) layer.clearAnimation() else layer
            },
        )
    }

    // --- Exportación -------------------------------------------------------

    fun export() {
        if (_exporting.value) return
        _exporting.value = true
        _progress.value = 0
        exporter.export(
            _project.value,
            object : VideoExporter.Callback {
                override fun onProgress(percent: Int) {
                    _progress.value = percent
                }

                override fun onSuccess(file: File) {
                    viewModelScope.launch {
                        val saved = withContext(Dispatchers.IO) {
                            GallerySaver.saveToGallery(context, file)
                        }
                        _lastExport.value = file
                        _exporting.value = false
                        _message.value = if (saved != null) {
                            if (GallerySaver.openInGallery(context, saved)) {
                                "Vídeo guardado y abierto en la galería"
                            } else {
                                "Vídeo guardado en la galería"
                            }
                        } else {
                            "Vídeo exportado"
                        }
                    }
                }

                override fun onError(message: String) {
                    _exporting.value = false
                    _message.value = message
                }
            },
        )
    }

    fun cancelExport() {
        exporter.cancel()
        _exporting.value = false
        _message.value = "Exportación cancelada"
    }

    fun shareLastExport() {
        _lastExport.value?.let { ShareUtils.shareVideo(context, it) }
    }

    override fun onCleared() {
        super.onCleared()
        exporter.cancel()
    }
}
