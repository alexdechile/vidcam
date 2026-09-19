package com.vidcam.app.editor

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.vidcam.app.export.GallerySaver
import com.vidcam.app.export.ShareUtils
import com.vidcam.app.export.VideoExporter
import com.vidcam.app.media.MediaProbe
import com.vidcam.app.media.MusicRepository
import com.vidcam.app.model.LayerKeyframe
import com.vidcam.app.model.LayerKind
import com.vidcam.app.model.MAX_DURATION_MS
import com.vidcam.app.model.MusicTrack
import com.vidcam.app.model.OverlayLayer
import com.vidcam.app.model.Project
import com.vidcam.app.model.TimelineMath
import com.vidcam.app.model.VideoClip
import com.vidcam.app.model.clearAnimation
import com.vidcam.app.model.removeKeyframeAt
import com.vidcam.app.model.seedKeyframesAt
import com.vidcam.app.model.shouldSampleKeyframe
import com.vidcam.app.model.upsertKeyframe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

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

    private val musicRepository = MusicRepository(application)
    private val exporter = VideoExporter(application)

    private val context: Application
        get() = getApplication()

    fun consumeMessage() {
        _message.value = null
    }

    private fun update(block: (Project) -> Project) {
        _project.value = block(_project.value)
    }

    // --- Clips -------------------------------------------------------------

    fun addRecordedClip(file: File) {
        val uri = Uri.fromFile(file)
        val duration = MediaProbe.durationMs(context, uri)
        if (duration <= 0L) {
            _message.value = "No se pudo leer la grabación"
            return
        }
        addClip(uri.toString(), duration)
    }

    fun importVideo(uri: Uri) {
        viewModelScope.launch {
            val cached = withContext(Dispatchers.IO) {
                MediaProbe.copyToCache(context, uri, "clip")
            }
            if (cached == null) {
                _message.value = "No se pudo importar el vídeo"
                return@launch
            }
            val duration = withContext(Dispatchers.IO) {
                MediaProbe.durationMs(context, Uri.fromFile(cached))
            }
            if (duration <= 0L) {
                _message.value = "No se pudo leer el vídeo importado"
                return@launch
            }
            addClip(Uri.fromFile(cached).toString(), duration)
        }
    }

    private fun addClip(uri: String, sourceDurationMs: Long) {
        val remaining = MAX_DURATION_MS - _project.value.totalDurationMs
        if (remaining <= 0L) {
            _message.value = "Límite de 30 s alcanzado"
            return
        }
        val clip = VideoClip(
            id = "clip-${System.currentTimeMillis()}",
            uri = uri,
            sourceDurationMs = sourceDurationMs,
        )
        val trimmed = TimelineMath.trimToMax(clip, minOf(remaining, MAX_DURATION_MS))
        update { it.copy(clips = it.clips + trimmed) }
    }

    fun removeClip(id: String) =
        update { project -> project.copy(clips = project.clips.filterNot { it.id == id }) }

    fun updateTrim(id: String, startMs: Long, endMs: Long) = update { project ->
        val others = project.clips.filterNot { it.id == id }.sumOf { it.trimmedDurationMs }
        val maxForClip = (MAX_DURATION_MS - others).coerceAtLeast(0L)
        project.copy(
            clips = project.clips.map { clip ->
                if (clip.id != id) {
                    clip
                } else {
                    val boundedEnd = endMs.coerceAtMost(startMs + maxForClip)
                    TimelineMath.clampTrim(clip, startMs, boundedEnd)
                }
            },
        )
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

    fun clearMusic() =
        update { it.copy(music = null, musicEnabled = false, musicStartMs = 0L) }

    fun setMusicEnabled(enabled: Boolean) =
        update { it.copy(musicEnabled = enabled && it.music != null) }

    fun setMusicStart(ms: Long) = update { project ->
        val max = (project.music?.durationMs ?: 0L).coerceAtLeast(0L)
        project.copy(musicStartMs = ms.coerceIn(0L, max))
    }

    fun setOriginalAudioMuted(muted: Boolean) =
        update { it.copy(originalAudioMuted = muted) }

    // --- Capas -------------------------------------------------------------

    fun addPngLayer(uri: Uri) = update { project ->
        project.copy(
            layers = project.layers + OverlayLayer(
                id = "layer-${System.currentTimeMillis()}",
                kind = LayerKind.PNG,
                uri = uri.toString(),
                label = "Imagen",
                endMs = project.totalDurationMs.coerceAtLeast(1000L),
            ),
        )
    }

    fun addTextLayer(text: String, fontName: String?) = update { project ->
        project.copy(
            layers = project.layers + OverlayLayer(
                id = "layer-${System.currentTimeMillis()}",
                kind = LayerKind.TEXT,
                text = text,
                fontName = fontName,
                label = text.ifBlank { "Texto" },
                endMs = project.totalDurationMs.coerceAtLeast(1000L),
            ),
        )
    }

    fun updateLayer(layer: OverlayLayer) = update { project ->
        project.copy(layers = project.layers.map { if (it.id == layer.id) layer else it })
    }

    fun removeLayer(id: String) =
        update { project -> project.copy(layers = project.layers.filterNot { it.id == id }) }

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
     */
    fun applyLayerGesture(layer: OverlayLayer, atMs: Long, force: Boolean = false) =
        update { project ->
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
                            "Vídeo guardado en la galería"
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
