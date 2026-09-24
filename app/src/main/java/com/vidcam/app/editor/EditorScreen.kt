@file:OptIn(ExperimentalMaterial3Api::class)

package com.vidcam.app.editor

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RangeSlider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.Effect
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.vidcam.app.R
import com.vidcam.app.capture.CameraRecorder
import com.vidcam.app.data.SavedProject
import com.vidcam.app.effect.media3Effect
import com.vidcam.app.effect.media3Transformation
import com.vidcam.app.export.FrameGeometry
import com.vidcam.app.export.LayerBitmaps
import com.vidcam.app.media.MediaProbe
import com.vidcam.app.model.CLIP_SPEEDS
import com.vidcam.app.model.ColorFilterPreset
import com.vidcam.app.model.LayerHitTarget
import com.vidcam.app.model.LayerKind
import com.vidcam.app.model.MAX_DURATION_MS
import com.vidcam.app.model.MusicTrack
import com.vidcam.app.model.OverlayLayer
import com.vidcam.app.model.Project
import com.vidcam.app.model.TimelineMath
import com.vidcam.app.model.VideoClip
import com.vidcam.app.model.WideLensMode
import com.vidcam.app.model.hitTestLayers
import com.vidcam.app.model.resolvedAt
import com.vidcam.app.ui.BUILT_IN_STICKERS
import com.vidcam.app.ui.CameraPreview
import com.vidcam.app.ui.GOOGLE_FONT_NAMES
import com.vidcam.app.ui.rememberGoogleFontFamily
import com.vidcam.app.ui.rememberGoogleFontsAvailable
import com.vidcam.app.ui.stickerAssetUri
import com.vidcam.app.util.formatDateTime
import com.vidcam.app.util.formatDuration
import com.vidcam.app.util.formatShortTime
import com.vidcam.app.util.formatSpeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import java.io.File
import kotlin.math.atan2

@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    canRecord: Boolean = true,
    onRequestPermissions: () -> Unit = {},
) {
    val project by viewModel.project.collectAsStateWithLifecycle()
    val exporting by viewModel.exporting.collectAsStateWithLifecycle()
    val progress by viewModel.progress.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    val lastExport by viewModel.lastExport.collectAsStateWithLifecycle()
    val motionRecording by viewModel.motionRecording.collectAsStateWithLifecycle()
    val canUndo by viewModel.canUndo.collectAsStateWithLifecycle()
    val canRedo by viewModel.canRedo.collectAsStateWithLifecycle()
    val savedProjects by viewModel.savedProjects.collectAsStateWithLifecycle()
    val currentProjectName by viewModel.currentProjectName.collectAsStateWithLifecycle()
    val currentProjectId by viewModel.currentProjectId.collectAsStateWithLifecycle()
    val dirty by viewModel.dirty.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val recorder = remember { CameraRecorder(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var recording by remember { mutableStateOf(false) }
    var showMusicSheet by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showStickerDialog by remember { mutableStateOf(false) }
    var showNewProjectDialog by remember { mutableStateOf(false) }
    var showOpenDialog by remember { mutableStateOf(false) }
    var showSaveAsDialog by remember { mutableStateOf(false) }
    var showProjectMenu by remember { mutableStateOf(false) }
    var pendingOpen by remember { mutableStateOf<SavedProject?>(null) }
    var editingLayer by remember { mutableStateOf<OverlayLayer?>(null) }
    var recordSpeed by remember { mutableStateOf(1f) }
    var recordFilter by remember { mutableStateOf(ColorFilterPreset.NINGUNO) }
    var recordWide by remember { mutableStateOf(WideLensMode.NORMAL) }

    DisposableEffect(lifecycleOwner, canRecord) {
        if (canRecord) recorder.controller.bindToLifecycle(lifecycleOwner)
        onDispose { recorder.stop() }
    }

    LaunchedEffect(message) {
        val current = message
        if (current != null) {
            snackbarHostState.showSnackbar(current)
            viewModel.consumeMessage()
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::importVideo) }

    val imagePicker = rememberLauncherForActivityResult(
        ActivityResultContracts.PickVisualMedia(),
    ) { uri -> uri?.let(viewModel::addPngLayer) }

    fun startRecording() {
        val file = File(MediaProbe.capturesDir(context), "rec-${System.currentTimeMillis()}.mp4")
        recording = true
        recorder.start(file) { recorded ->
            recording = false
            viewModel.addRecordedClip(recorded, recordSpeed, recordFilter, recordWide)
        }
        scope.launch {
            delay(MAX_DURATION_MS)
            if (recorder.isRecording) recorder.stop()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = currentProjectName ?: "VidCam",
                        maxLines = 1,
                    )
                },
                actions = {
                    TextButton(
                        onClick = {
                            if (dirty) {
                                showNewProjectDialog = true
                            } else {
                                viewModel.newProject()
                            }
                        },
                        enabled = !exporting,
                    ) {
                        Text("Nuevo")
                    }
                    Box {
                        IconButton(
                            onClick = { showProjectMenu = true },
                            enabled = !exporting,
                        ) {
                            Icon(
                                Icons.Default.MoreVert,
                                contentDescription = "Proyecto",
                            )
                        }
                        DropdownMenu(
                            expanded = showProjectMenu,
                            onDismissRequest = { showProjectMenu = false },
                        ) {
                            DropdownMenuItem(
                                text = { Text("Guardar") },
                                enabled = !project.isEmpty,
                                onClick = {
                                    showProjectMenu = false
                                    if (currentProjectId == null) {
                                        showSaveAsDialog = true
                                    } else {
                                        viewModel.saveProject()
                                    }
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Guardar como…") },
                                enabled = !project.isEmpty,
                                onClick = {
                                    showProjectMenu = false
                                    showSaveAsDialog = true
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("Abrir proyecto…") },
                                onClick = {
                                    showProjectMenu = false
                                    viewModel.refreshSavedProjects()
                                    showOpenDialog = true
                                },
                            )
                        }
                    }
                    if (lastExport != null) {
                        IconButton(onClick = viewModel::shareLastExport) {
                            Icon(Icons.Default.Share, contentDescription = "Compartir")
                        }
                    }
                    TextButton(
                        onClick = viewModel::export,
                        enabled = !exporting && project.clips.isNotEmpty(),
                    ) {
                        Text("Exportar")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color.Black),
                contentAlignment = Alignment.Center,
            ) {
                if (project.clips.isEmpty()) {
                    if (canRecord) {
                        CameraPreview(recorder = recorder, modifier = Modifier.fillMaxSize())
                    } else {
                        ImportOnlyPlaceholder(onRequestPermissions = onRequestPermissions)
                    }
                } else {
                    TimelinePreview(
                        project = project,
                        motionRecording = motionRecording,
                        onLayerGesture = viewModel::applyLayerGesture,
                        onMotionAutoStop = { viewModel.setMotionRecording(false) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            ControlsSection(
                project = project,
                recording = recording,
                canRecord = canRecord,
                canUndo = canUndo,
                canRedo = canRedo,
                onUndo = viewModel::undo,
                onRedo = viewModel::redo,
                onRequestPermissions = onRequestPermissions,
                onToggleLens = recorder::toggleLens,
                onToggleRecord = { if (recording) recorder.stop() else startRecording() },
                onImportVideo = {
                    videoPicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.VideoOnly),
                    )
                },
                onAddPng = {
                    imagePicker.launch(
                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                    )
                },
                onAddText = { showTextDialog = true },
                onAddSticker = { showStickerDialog = true },
                onMusic = { showMusicSheet = true },
                motionRecording = motionRecording,
                onToggleMotion = { viewModel.setMotionRecording(!motionRecording) },
                recordSpeed = recordSpeed,
                onSelectSpeed = { recordSpeed = it },
                recordFilter = recordFilter,
                onSelectRecordFilter = { recordFilter = it },
                recordWide = recordWide,
                onToggleRecordWide = { recordWide = if (recordWide == WideLensMode.GRUPO) WideLensMode.NORMAL else WideLensMode.GRUPO },
                viewModel = viewModel,
            )

            TimelineSection(project = project, viewModel = viewModel)
        }
    }

    if (showMusicSheet) {
        MusicSheet(viewModel = viewModel, onDismiss = { showMusicSheet = false })
    }

    if (showTextDialog) {
        TextLayerDialog(
            onDismiss = { showTextDialog = false },
            onConfirm = { text, font ->
                viewModel.addTextLayer(text, font)
                showTextDialog = false
            },
        )
    }

    if (showStickerDialog) {
        StickerPickerDialog(
            onDismiss = { showStickerDialog = false },
            onPick = { path ->
                viewModel.addPngLayer(Uri.parse(stickerAssetUri(path)))
                showStickerDialog = false
            },
        )
    }

    editingLayer?.let { layer ->
        LayerEditDialog(
            layer = layer,
            onDismiss = { editingLayer = null },
            onConfirm = { updated ->
                viewModel.updateLayer(updated)
                editingLayer = null
            },
        )
    }

    if (showNewProjectDialog) {
        AlertDialog(
            onDismissRequest = { showNewProjectDialog = false },
            title = { Text("Nuevo proyecto") },
            text = {
                Text("Tienes cambios sin guardar. Se descartarán al empezar de cero.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.newProject()
                        showNewProjectDialog = false
                    },
                ) {
                    Text("Descartar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewProjectDialog = false }) {
                    Text("Cancelar")
                }
            },
        )
    }

    pendingOpen?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingOpen = null },
            title = { Text("Abrir proyecto") },
            text = {
                Text(
                    "Tienes cambios sin guardar. Se descartarán al abrir " +
                        "\"${target.name}\".",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.openProject(target)
                        pendingOpen = null
                    },
                ) {
                    Text("Descartar y abrir")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingOpen = null }) { Text("Cancelar") }
            },
        )
    }

    if (showSaveAsDialog) {
        SaveProjectDialog(
            initialName = currentProjectName.orEmpty(),
            onDismiss = { showSaveAsDialog = false },
            onConfirm = { name ->
                viewModel.saveProject(
                    name = name,
                    asNew = currentProjectId != null,
                )
                showSaveAsDialog = false
            },
        )
    }

    if (showOpenDialog) {
        OpenProjectDialog(
            projects = savedProjects,
            onDismiss = { showOpenDialog = false },
            onOpen = { saved ->
                showOpenDialog = false
                if (dirty) {
                    pendingOpen = saved
                } else {
                    viewModel.openProject(saved)
                }
            },
            onDelete = viewModel::deleteProject,
        )
    }

    if (exporting) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text("Exportando") },
            text = {
                Column {
                    LinearProgressIndicator(
                        progress = { progress / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Spacer(Modifier.height(8.dp))
                    Text("$progress %")
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = viewModel::cancelExport) { Text("Cancelar") }
            },
        )
    }
}

@Composable
private fun ImportOnlyPlaceholder(onRequestPermissions: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(24.dp),
    ) {
        Text(
            text = "Sin acceso a la cámara",
            color = Color.White,
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Puedes importar un vídeo o conceder el permiso para grabar.",
            color = Color.White,
            style = MaterialTheme.typography.bodyMedium,
        )
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRequestPermissions) { Text("Permiso de cámara") }
    }
}

@Composable
private fun ControlsSection(
    project: Project,
    recording: Boolean,
    canRecord: Boolean,
    canUndo: Boolean,
    canRedo: Boolean,
    onUndo: () -> Unit,
    onRedo: () -> Unit,
    onRequestPermissions: () -> Unit,
    onToggleLens: () -> Unit,
    onToggleRecord: () -> Unit,
    onImportVideo: () -> Unit,
    onAddPng: () -> Unit,
    onAddText: () -> Unit,
    onAddSticker: () -> Unit,
    onMusic: () -> Unit,
    motionRecording: Boolean,
    onToggleMotion: () -> Unit,
    recordSpeed: Float,
    onSelectSpeed: (Float) -> Unit,
    recordFilter: ColorFilterPreset,
    onSelectRecordFilter: (ColorFilterPreset) -> Unit,
    recordWide: WideLensMode,
    onToggleRecordWide: () -> Unit,
    viewModel: EditorViewModel,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onUndo, enabled = canUndo) {
                Icon(
                    painter = painterResource(R.drawable.ic_undo),
                    contentDescription = "Deshacer",
                )
            }
            IconButton(onClick = onRedo, enabled = canRedo) {
                Icon(
                    painter = painterResource(R.drawable.ic_redo),
                    contentDescription = "Rehacer",
                )
            }
            Spacer(Modifier.width(8.dp))
            if (project.clips.isEmpty()) {
                if (canRecord) {
                    OutlinedButton(onClick = onToggleLens) { Text("Girar") }
                    Spacer(Modifier.width(8.dp))
                    Button(onClick = onToggleRecord) {
                        Icon(Icons.Default.PlayArrow, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (recording) "Detener" else "Grabar")
                    }
                } else {
                    OutlinedButton(onClick = onRequestPermissions) { Text("Permiso de cámara") }
                }
                Spacer(Modifier.width(8.dp))
            }
            OutlinedButton(onClick = onImportVideo) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(Modifier.width(4.dp))
                Text("Importar")
            }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onAddPng) { Text("PNG") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onAddText) { Text("Texto") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onAddSticker) { Text("Sticker") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(onClick = onMusic) { Text("Música") }
            Spacer(Modifier.width(8.dp))
            OutlinedButton(
                onClick = onToggleMotion,
                enabled = project.layers.isNotEmpty(),
            ) {
                Text(if (motionRecording) "Detener grabación" else "Grabar movimiento")
            }
        }

        if (project.clips.isEmpty() && canRecord) {
            SpeedChips(
                selected = recordSpeed,
                onSelect = onSelectSpeed,
                enabled = !recording,
                modifier = Modifier.padding(top = 4.dp),
            )
            FilterChips(
                selected = recordFilter,
                onSelect = onSelectRecordFilter,
                enabled = !recording,
                modifier = Modifier.padding(top = 4.dp),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Text("Lente ancho", modifier = Modifier.weight(1f))
                FilterChip(
                    selected = recordWide == WideLensMode.GRUPO,
                    onClick = onToggleRecordWide,
                    enabled = !recording,
                    label = { Text("Grupo") },
                )
            }
        }

        if (motionRecording) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .background(Color.Red, CircleShape),
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = "Grabando movimiento: mueve o pellizca una capa",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 4.dp),
        ) {
            Text("Silenciar audio original", modifier = Modifier.weight(1f))
            Switch(
                checked = project.originalAudioMuted,
                onCheckedChange = viewModel::setOriginalAudioMuted,
            )
        }

        val music = project.music
        if (music != null) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Switch(
                    checked = project.musicEnabled,
                    onCheckedChange = viewModel::setMusicEnabled,
                )
                Spacer(Modifier.width(8.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = music.title,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                    )
                    Slider(
                        value = project.musicStartMs.toFloat(),
                        onValueChange = { viewModel.setMusicStart(it.toLong()) },
                        valueRange = 0f..music.durationMs.toFloat().coerceAtLeast(1f),
                    )
                }
                TextButton(onClick = viewModel::clearMusic) { Text("Quitar") }
            }
        }
    }
}

@Composable
private fun TimelineSection(project: Project, viewModel: EditorViewModel) {
    Surface(tonalElevation = 3.dp, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
        ) {
            Text(
                text = "Línea de tiempo · ${formatDuration(project.totalDurationMs)} / 0:30",
                style = MaterialTheme.typography.labelLarge,
            )
            if (project.clips.isEmpty()) {
                Text(
                    text = "Graba o importa un vídeo para empezar",
                    style = MaterialTheme.typography.bodySmall,
                )
            } else {
                project.clips.forEach { clip ->
                    ClipRow(clip = clip, viewModel = viewModel)
                }
            }

            if (project.layers.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text(text = "Capas", style = MaterialTheme.typography.labelLarge)
                project.layers.forEach { layer ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (layer.kind == LayerKind.TEXT) layer.text else layer.label,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                            )
                            if (layer.keyframes.isNotEmpty()) {
                                Text(
                                    text = "${layer.keyframes.size} claves",
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            Text(
                                text = "${formatDuration(layer.startMs)}–${formatDuration(layer.endMs)}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            IconButton(onClick = { viewModel.removeLayer(layer.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Eliminar capa")
                            }
                        }
                        if (layer.keyframes.isNotEmpty()) {
                            Row(
                                modifier = Modifier.horizontalScroll(rememberScrollState()),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                layer.keyframes.forEach { keyframe ->
                                    TextButton(
                                        onClick = {
                                            viewModel.removeLayerKeyframe(layer.id, keyframe.timeMs)
                                        },
                                    ) {
                                        Text(
                                            text = formatShortTime(keyframe.timeMs),
                                            style = MaterialTheme.typography.labelSmall,
                                        )
                                    }
                                }
                                TextButton(onClick = { viewModel.clearLayerAnimation(layer.id) }) {
                                    Text(
                                        text = "Borrar animación",
                                        style = MaterialTheme.typography.labelSmall,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ClipRow(clip: VideoClip, viewModel: EditorViewModel) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "Clip · ${formatDuration(clip.effectiveDurationMs)}",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
            )
            IconButton(onClick = { viewModel.removeClip(clip.id) }) {
                Icon(Icons.Default.Delete, contentDescription = "Eliminar clip")
            }
        }
        RangeSlider(
            value = clip.trimStartMs.toFloat()..clip.trimEndMs.toFloat(),
            onValueChange = { range ->
                viewModel.updateTrim(clip.id, range.start.toLong(), range.endInclusive.toLong())
            },
            valueRange = 0f..clip.sourceDurationMs.toFloat().coerceAtLeast(1f),
        )
        SpeedChips(
            selected = clip.playbackSpeed,
            onSelect = { viewModel.setClipSpeed(clip.id, it) },
            modifier = Modifier.padding(top = 2.dp),
        )
        FilterChips(
            selected = clip.colorFilter,
            onSelect = { viewModel.setClipColorFilter(clip.id, it) },
            modifier = Modifier.padding(top = 2.dp),
        )
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 2.dp),
        ) {
            Text(
                text = "Lente ancho",
                style = MaterialTheme.typography.labelSmall,
                modifier = Modifier.weight(1f),
            )
            FilterChip(
                selected = clip.wideLens == WideLensMode.GRUPO,
                onClick = {
                    viewModel.setClipWideLens(
                        clip.id,
                        if (clip.wideLens == WideLensMode.GRUPO) {
                            WideLensMode.NORMAL
                        } else {
                            WideLensMode.GRUPO
                        },
                    )
                },
                label = {
                    Text(
                        if (clip.wideLens == WideLensMode.GRUPO) "Grupo" else "Normal",
                        style = MaterialTheme.typography.labelMedium,
                    )
                },
            )
        }
    }
}

@Composable
private fun SpeedChips(
    selected: Float,
    onSelect: (Float) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        CLIP_SPEEDS.forEach { speed ->
            FilterChip(
                selected = speed == selected,
                onClick = { onSelect(speed) },
                enabled = enabled,
                label = { Text(formatSpeed(speed), style = MaterialTheme.typography.labelMedium) },
            )
            Spacer(Modifier.width(4.dp))
        }
    }
}

private fun ColorFilterPreset.label(): String = when (this) {
    ColorFilterPreset.NINGUNO -> "Ninguno"
    ColorFilterPreset.SEPIA -> "Sepia"
    ColorFilterPreset.BLANCO_Y_NEGRO -> "B/N"
    ColorFilterPreset.VINTAGE -> "Vintage"
    ColorFilterPreset.ALTO_CONTRASTE -> "Contraste"
}

@Composable
private fun FilterChips(
    selected: ColorFilterPreset,
    onSelect: (ColorFilterPreset) -> Unit,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier.horizontalScroll(rememberScrollState()),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ColorFilterPreset.entries.forEach { preset ->
            FilterChip(
                selected = preset == selected,
                onClick = { onSelect(preset) },
                enabled = enabled,
                label = { Text(preset.label(), style = MaterialTheme.typography.labelMedium) },
            )
            Spacer(Modifier.width(4.dp))
        }
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun TimelinePreview(
    project: Project,
    motionRecording: Boolean,
    onLayerGesture: (OverlayLayer, Long, Boolean) -> Unit,
    onMotionAutoStop: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val videoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_ALL
            playWhenReady = true
        }
    }
    val musicPlayer = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = true }
    }
    val clipsState = rememberUpdatedState(project.clips)
    var positionMs by remember { mutableStateOf(0L) }
    var selectedLayerId by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(project.clips) {
        val items = project.clips.map { clip ->
            MediaItem.Builder()
                .setUri(clip.uri)
                .setClippingConfiguration(
                    MediaItem.ClippingConfiguration.Builder()
                        .setStartPositionMs(clip.trimStartMs)
                        .setEndPositionMs(clip.trimEndMs)
                        .build(),
                )
                .build()
        }
        videoPlayer.setMediaItems(items)
        videoPlayer.prepare()
        videoPlayer.play()
    }

    LaunchedEffect(
        project.music,
        project.musicEnabled,
        project.musicStartMs,
        project.originalAudioMuted,
        project.totalDurationMs,
    ) {
        videoPlayer.volume = if (project.originalAudioMuted) 0f else 1f
        val music = project.music
        if (music == null || !project.musicEnabled || project.totalDurationMs <= 0L) {
            musicPlayer.stop()
            musicPlayer.clearMediaItems()
        } else {
            val start = project.musicStartMs.coerceIn(
                0L,
                (music.durationMs - 1L).coerceAtLeast(0L),
            )
            val end = (start + project.totalDurationMs).coerceAtMost(music.durationMs)
            musicPlayer.repeatMode = if (TimelineMath.musicShouldLoop(project)) {
                Player.REPEAT_MODE_ONE
            } else {
                Player.REPEAT_MODE_OFF
            }
            musicPlayer.setMediaItem(
                MediaItem.Builder()
                    .setUri(music.uri)
                    .setClippingConfiguration(
                        MediaItem.ClippingConfiguration.Builder()
                            .setStartPositionMs(start)
                            .setEndPositionMs(end)
                            .build(),
                    )
                    .build(),
            )
            musicPlayer.prepare()
            musicPlayer.play()
        }
    }

    val recordingNow by rememberUpdatedState(motionRecording)
    val onMotionAutoStopNow by rememberUpdatedState(onMotionAutoStop)

    LaunchedEffect(videoPlayer) {
        var previousPositionMs = 0L
        var reachedEnd = false
        var lastSpeed = 1f
        var lastEffects: List<Effect> = emptyList()
        while (true) {
            // Sincronizado con el fotograma mientras hay reproducción; en pausa
            // basta un muestreo lento.
            if (videoPlayer.isPlaying) {
                withFrameNanos { }
            } else {
                delay(100L)
            }
            val clips = clipsState.value
            val index = videoPlayer.currentMediaItemIndex
            val clip = clips.getOrNull(index)
            val speed = clip?.playbackSpeed?.coerceAtLeast(0.01f) ?: 1f
            // Cámara lenta/rápida en la vista previa: el clip local avanza a
            // `velocidad` mientras el timeline avanza en tiempo real.
            if (speed != lastSpeed) {
                lastSpeed = speed
                videoPlayer.playbackParameters = PlaybackParameters(speed, speed)
            }
            val currentEffects: List<Effect> = clip?.let {
                listOfNotNull(
                    it.colorFilter.media3Effect(),
                    it.wideLens.media3Transformation(),
                )
            }.orEmpty()
            if (currentEffects != lastEffects) {
                lastEffects = currentEffects
                videoPlayer.setVideoEffects(currentEffects)
            }
            val base = if (index in clips.indices) {
                clips.take(index).sumOf { it.effectiveDurationMs }
            } else {
                0L
            }
            val total = clips.sumOf { it.effectiveDurationMs }
            val next = base + (videoPlayer.currentPosition / speed).toLong()
            if (recordingNow) {
                // Solo se considera "pasada terminada" después de haber visto el
                // final, para no confundir el seek inicial a 0 con el reinicio
                // del bucle.
                if (total > 0L && next >= total - 100L) reachedEnd = true
                if (reachedEnd && next < previousPositionMs) {
                    reachedEnd = false
                    onMotionAutoStopNow()
                }
            } else {
                reachedEnd = false
            }
            previousPositionMs = next
            positionMs = next
        }
    }

    LaunchedEffect(motionRecording) {
        if (motionRecording) videoPlayer.seekTo(0L)
    }

    DisposableEffect(Unit) {
        onDispose {
            videoPlayer.release()
            musicPlayer.release()
        }
    }

    // El lienzo de la vista previa debe tener la misma relación de aspecto que
    // el fotograma que exporta Media3 (el del primer clip). Si no, las capas se
    // ven desplazadas al exportar porque la exportación no recorta el vídeo.
    val firstClipUri = project.clips.firstOrNull()?.uri
    var frameSize by remember { mutableStateOf<Pair<Int, Int>?>(null) }

    LaunchedEffect(firstClipUri) {
        frameSize = withContext(Dispatchers.IO) {
            FrameGeometry.frameSize(context, project)
        }
    }

    BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
        val density = LocalDensity.current
        val availableWidth = with(density) { maxWidth.toPx() }
        val availableHeight = with(density) { maxHeight.toPx() }
        val (containerWidth, containerHeight) = frameSize?.let { (width, height) ->
            FrameGeometry.fit(
                frameWidth = width.toFloat(),
                frameHeight = height.toFloat(),
                maxWidth = availableWidth,
                maxHeight = availableHeight,
            )
        } ?: (availableWidth to availableHeight)

        val resolvedLayers = project.layers
            .filter { positionMs in it.startMs..it.endMs }
            .map { it.resolvedAt(positionMs) }
        val currentLayers by rememberUpdatedState(resolvedLayers)
        val currentPositionMs by rememberUpdatedState(positionMs)
        val currentContainerWidth by rememberUpdatedState(containerWidth)
        val currentContainerHeight by rememberUpdatedState(containerHeight)
        val currentOnGesture by rememberUpdatedState(onLayerGesture)
        // Tamaño medido de cada capa, en píxeles y sin escala, para el hit-test.
        val measuredSizes = remember { mutableMapOf<String, IntSize>() }

        Box(
            modifier = Modifier.size(
                width = with(density) { containerWidth.toDp() },
                height = with(density) { containerHeight.toDp() },
            ),
        ) {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = videoPlayer
                        useController = false
                        // FIT y no ZOOM: el vídeo se muestra completo dentro del
                        // fotograma, igual que lo escala Media3 al exportar.
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // Una sola superficie de gestos sobre todo el lienzo: así el
                    // pellizco funciona aunque un dedo caiga fuera de la capa.
                    .pointerInput(Unit) {
                        awaitPointerEventScope {
                            while (true) {
                                val down = awaitFirstDown(requireUnconsumed = false)
                                down.consume()

                                val layers = currentLayers
                                val targets = layers.mapNotNull { resolved ->
                                    measuredSizes[resolved.id]?.let { size ->
                                        LayerHitTarget(
                                            layer = resolved,
                                            widthPx = size.width.toFloat(),
                                            heightPx = size.height.toFloat(),
                                        )
                                    }
                                }
                                val hit = hitTestLayers(
                                    targets = targets,
                                    pointX = down.position.x,
                                    pointY = down.position.y,
                                    containerWidth = currentContainerWidth,
                                    containerHeight = currentContainerHeight,
                                )
                                // Si el toque no cae en ninguna capa se mantiene la
                                // selección, para poder pellizcar fuera de ella.
                                val targetLayer = hit
                                    ?: selectedLayerId?.let { id -> layers.firstOrNull { it.id == id } }
                                if (targetLayer == null) continue
                                selectedLayerId = targetLayer.id

                                var working: OverlayLayer? = null
                                var moved = false
                                var emitted = false
                                var lastPointerCount = 0
                                var lastPosition = down.position
                                var lastDistance = 0f
                                var lastAngle = 0f

                                do {
                                    val event = awaitPointerEvent()
                                    val changes = event.changes.filter { it.pressed }

                                    if (changes.size != lastPointerCount) {
                                        lastPointerCount = changes.size
                                        lastPosition = changes.firstOrNull()?.position ?: down.position
                                        lastDistance = 0f
                                        lastAngle = 0f
                                    }

                                    when {
                                        changes.size >= 2 -> {
                                            val p1 = changes[0].position
                                            val p2 = changes[1].position
                                            val distance = (p1 - p2).getDistance()
                                            val angle = atan2(p2.y - p1.y, p2.x - p1.x)
                                            if (lastDistance > 0f) {
                                                val base = working
                                                    ?: currentLayers.firstOrNull {
                                                        it.id == targetLayer.id
                                                    }
                                                    ?: targetLayer
                                                val next = base.copy(
                                                    scale = (base.scale * (distance / lastDistance))
                                                        .coerceIn(0.2f, 4f),
                                                    rotationDeg = base.rotationDeg +
                                                        Math.toDegrees(
                                                            (angle - lastAngle).toDouble(),
                                                        ).toFloat(),
                                                )
                                                working = next
                                                moved = true
                                                currentOnGesture(next, currentPositionMs, !emitted)
                                                emitted = true
                                            }
                                            lastDistance = distance
                                            lastAngle = angle
                                        }

                                        changes.size == 1 -> {
                                            val change = changes.first()
                                            if (!moved &&
                                                (change.position - down.position).getDistance() >
                                                viewConfiguration.touchSlop
                                            ) {
                                                moved = true
                                            }
                                            if (moved) {
                                                val delta = change.position - lastPosition
                                                lastPosition = change.position
                                                val base = working
                                                    ?: currentLayers.firstOrNull {
                                                        it.id == targetLayer.id
                                                    }
                                                    ?: targetLayer
                                                val next = base.copy(
                                                    x = (base.x + delta.x / currentContainerWidth)
                                                        .coerceIn(0f, 1f),
                                                    y = (base.y + delta.y / currentContainerHeight)
                                                        .coerceIn(0f, 1f),
                                                )
                                                working = next
                                                currentOnGesture(next, currentPositionMs, !emitted)
                                                emitted = true
                                            }
                                        }
                                    }

                                    changes.forEach { it.consume() }
                                } while (event.changes.any { it.pressed })

                                // Última muestra sin decimación, para no perder el
                                // punto donde el dedo terminó el gesto.
                                working?.let { currentOnGesture(it, currentPositionMs, true) }
                            }
                        }
                    },
            ) {
                resolvedLayers.forEach { layer ->
                    LayerBox(
                        layer = layer,
                        containerWidth = containerWidth,
                        containerHeight = containerHeight,
                        isSelected = layer.id == selectedLayerId,
                        onMeasured = { measuredSizes[layer.id] = it },
                    )
                }
            }

            if (motionRecording) {
                Surface(
                    color = Color.Red.copy(alpha = 0.85f),
                    shape = RoundedCornerShape(4.dp),
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp),
                ) {
                    Text(
                        text = "REC movimiento",
                        color = Color.White,
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun LayerBox(
    layer: OverlayLayer,
    containerWidth: Float,
    containerHeight: Float,
    isSelected: Boolean,
    onMeasured: (IntSize) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val bitmap = remember(layer.kind, layer.uri) {
        if (layer.kind == LayerKind.PNG) {
            layer.uri?.let { LayerBitmaps.decodePng(context, it) }
        } else {
            null
        }
    }

    // El tamaño base se expresa como fracción del ancho del lienzo y la
    // exportación usa la misma fracción sobre el ancho del vídeo. La escala
    // resuelta de la animación se aplica luego con graphicsLayer.
    val basePngWidthPx = containerWidth * LayerBitmaps.BASE_PNG_WIDTH_FRACTION
    val baseTextSizePx = minOf(containerWidth, containerHeight) *
        LayerBitmaps.BASE_TEXT_SIZE_FRACTION *
        (layer.fontSizeSp / LayerBitmaps.BASE_TEXT_SIZE_SP)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .defaultMinSize(48.dp, 48.dp)
            .onGloballyPositioned { onMeasured(it.size) }
            .graphicsLayer {
                translationX = containerWidth * layer.x - size.width / 2f
                translationY = containerHeight * layer.y - size.height / 2f
                scaleX = layer.scale
                scaleY = layer.scale
                rotationZ = layer.rotationDeg
            }
            .then(
                if (isSelected) Modifier.border(2.dp, Color.Cyan, RoundedCornerShape(4.dp))
                else Modifier
            ),
    ) {
        when {
            layer.kind == LayerKind.TEXT -> Text(
                text = layer.text,
                color = Color(layer.colorArgb),
                fontFamily = rememberGoogleFontFamily(layer.fontName),
                fontSize = with(density) { baseTextSizePx.toSp() },
            )

            bitmap != null -> {
                val width = with(density) { basePngWidthPx.toDp() }
                val height = with(density) {
                    (basePngWidthPx * bitmap.height / bitmap.width.coerceAtLeast(1)).toDp()
                }
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = layer.label,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(width, height),
                )
            }

            else -> {
                val size = with(density) { basePngWidthPx.toDp() }
                Surface(
                    color = Color.White.copy(alpha = 0.2f),
                    border = BorderStroke(1.dp, Color.White),
                    modifier = Modifier.size(size),
                ) {
                    Text(text = "PNG", color = Color.White, modifier = Modifier.padding(4.dp))
                }
            }
        }
    }
}

@Composable
private fun StickerPickerDialog(onDismiss: () -> Unit, onPick: (String) -> Unit) {
    val context = LocalContext.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Stickers") },
        text = {
            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(BUILT_IN_STICKERS) { path ->
                    val bitmap = remember(path) {
                        LayerBitmaps.decodePng(context, stickerAssetUri(path))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(path) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        if (bitmap != null) {
                            Image(
                                bitmap = bitmap.asImageBitmap(),
                                contentDescription = path,
                                modifier = Modifier.size(40.dp),
                            )
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(text = path.substringAfterLast('/').removeSuffix(".png"))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}

@Composable
private fun MusicSheet(viewModel: EditorViewModel, onDismiss: () -> Unit) {
    val context = LocalContext.current
    var tracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    fun reload() {
        loading = true
        viewModel.loadMusic {
            tracks = it
            loading = false
        }
    }

    LaunchedEffect(Unit) { reload() }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        LazyColumn(modifier = Modifier.fillMaxWidth()) {
            item {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    Text(
                        text = "Música del dispositivo",
                        style = MaterialTheme.typography.labelLarge,
                        modifier = Modifier.weight(1f),
                    )
                    TextButton(onClick = { reload() }) { Text("Actualizar") }
                }
            }

            when {
                loading -> item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                tracks.isEmpty() -> item {
                    Text(
                        text = "No se encontró música en el dispositivo. Descarga una " +
                            "pista de las fuentes de abajo y toca Actualizar.",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.bodySmall,
                    )
                }

                else -> items(tracks, key = { it.id }) { track ->
                    ListItem(
                        headlineContent = { Text(track.title, maxLines = 1) },
                        supportingContent = { Text(track.artist, maxLines = 1) },
                        trailingContent = { Text(formatDuration(track.durationMs)) },
                        modifier = Modifier.clickable {
                            viewModel.setMusic(track)
                            onDismiss()
                        },
                    )
                    HorizontalDivider()
                }
            }

            item {
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                Text(
                    text = "Música gratis para descargar",
                    style = MaterialTheme.typography.labelLarge,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
                Text(
                    text = "Se abre en el navegador. Descarga el archivo y toca " +
                        "Actualizar para que aparezca en la lista de arriba.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                )
            }

            items(FREE_MUSIC_SOURCES) { source ->
                ListItem(
                    headlineContent = { Text(source.name) },
                    supportingContent = { Text(source.note) },
                    modifier = Modifier.clickable { openUrl(context, source.url) },
                )
            }
            item { Spacer(Modifier.height(24.dp)) }
        }
    }
}

/** Fuente de música descargable: la app no empaqueta ninguna pista. */
private data class MusicSource(val name: String, val note: String, val url: String)

private val FREE_MUSIC_SOURCES = listOf(
    MusicSource(
        name = "YouTube Audio Library",
        note = "Gratis para vídeos de YouTube; revisa la licencia de cada pista",
        url = "https://studio.youtube.com/channel/UC/music",
    ),
    MusicSource(
        name = "Pixabay Music",
        note = "Uso libre, sin crédito",
        url = "https://pixabay.com/music/",
    ),
    MusicSource(
        name = "Free Music Archive",
        note = "Filtra por licencia Creative Commons",
        url = "https://freemusicarchive.org/",
    ),
    MusicSource(
        name = "ccMixter",
        note = "Creative Commons; algunas pistas piden crédito",
        url = "https://ccmixter.org/",
    ),
    MusicSource(
        name = "Musopen",
        note = "Música clásica y de dominio público",
        url = "https://musopen.org/music/",
    ),
)

private fun openUrl(context: Context, url: String) {
    try {
        context.startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        )
    } catch (e: Exception) {
        // Sin navegador disponible: no hay nada que abrir.
    }
}

@Composable
private fun SaveProjectDialog(
    initialName: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    var name by remember { mutableStateOf(initialName) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Guardar proyecto") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Nombre") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name) },
                enabled = name.isNotBlank(),
            ) {
                Text("Guardar")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun OpenProjectDialog(
    projects: List<SavedProject>,
    onDismiss: () -> Unit,
    onOpen: (SavedProject) -> Unit,
    onDelete: (String) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Abrir proyecto") },
        text = {
            if (projects.isEmpty()) {
                Text("Todavía no hay proyectos guardados.")
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(projects, key = { it.id }) { saved ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onOpen(saved) }
                                .padding(vertical = 4.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(saved.name, maxLines = 1)
                                Text(
                                    text = formatDateTime(saved.updatedAtMs),
                                    style = MaterialTheme.typography.bodySmall,
                                )
                            }
                            IconButton(onClick = { onDelete(saved.id) }) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Eliminar proyecto",
                                )
                            }
                        }
                        HorizontalDivider()
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cerrar") } },
    )
}

@Composable
private fun TextLayerDialog(onDismiss: () -> Unit, onConfirm: (String, String?) -> Unit) {
    var text by remember { mutableStateOf("") }
    var font by remember { mutableStateOf(GOOGLE_FONT_NAMES.first()) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Añadir texto") },
        text = {
            Column {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Texto") },
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(12.dp))
                Box {
                    OutlinedButton(onClick = { expanded = true }) {
                        Text(text = font, fontFamily = rememberGoogleFontFamily(font))
                    }
                    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                        GOOGLE_FONT_NAMES.forEach { name ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = name,
                                        fontFamily = rememberGoogleFontFamily(name),
                                    )
                                },
                                onClick = {
                                    font = name
                                    expanded = false
                                },
                            )
                        }
                    }
                }
                if (!rememberGoogleFontsAvailable()) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Sin conexión o sin Google Play Services: se usará la " +
                            "fuente del sistema.",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(text, font) },
                enabled = text.isNotBlank(),
            ) {
                Text("Añadir")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun LayerEditDialog(
    layer: OverlayLayer,
    onDismiss: () -> Unit,
    onConfirm: (OverlayLayer) -> Unit,
) {
    var current by remember { mutableStateOf(layer) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Editar capa") },
        text = {
            Column {
                if (current.kind == LayerKind.TEXT) {
                    OutlinedTextField(
                        value = current.text,
                        onValueChange = { current = current.copy(text = it) },
                        label = { Text("Texto") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    text = "Inicio: ${formatDuration(current.startMs)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Slider(
                    value = current.startMs.toFloat(),
                    onValueChange = { current = current.copy(startMs = it.toLong()) },
                    valueRange = 0f..current.endMs.toFloat().coerceAtLeast(1f),
                )
                Text(
                    text = "Fin: ${formatDuration(current.endMs)}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Slider(
                    value = current.endMs.toFloat(),
                    onValueChange = { current = current.copy(endMs = it.toLong()) },
                    valueRange = current.startMs.toFloat().coerceAtLeast(0f)..MAX_DURATION_MS.toFloat(),
                )
                Text(text = "Tamaño", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = current.scale,
                    onValueChange = { current = current.copy(scale = it) },
                    valueRange = 0.2f..3f,
                )
                if (current.kind == LayerKind.TEXT) {
                    Text(text = "Tamaño de letra", style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = current.fontSizeSp,
                        onValueChange = { current = current.copy(fontSizeSp = it) },
                        valueRange = 12f..120f,
                    )
                }
                Text(text = "Posición X", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = current.x,
                    onValueChange = { current = current.copy(x = it) },
                    valueRange = 0f..1f,
                )
                Text(text = "Posición Y", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = current.y,
                    onValueChange = { current = current.copy(y = it) },
                    valueRange = 0f..1f,
                )
            }
        },
        confirmButton = { TextButton(onClick = { onConfirm(current) }) { Text("Guardar") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}
