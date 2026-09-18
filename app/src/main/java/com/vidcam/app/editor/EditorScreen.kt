@file:OptIn(ExperimentalMaterial3Api::class)

package com.vidcam.app.editor

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
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.vidcam.app.capture.CameraRecorder
import com.vidcam.app.export.LayerBitmaps
import com.vidcam.app.model.LayerKind
import com.vidcam.app.model.MAX_DURATION_MS
import com.vidcam.app.model.MusicTrack
import com.vidcam.app.model.OverlayLayer
import com.vidcam.app.model.Project
import com.vidcam.app.model.TimelineMath
import com.vidcam.app.model.VideoClip
import com.vidcam.app.ui.BUILT_IN_STICKERS
import com.vidcam.app.ui.CameraPreview
import com.vidcam.app.ui.GOOGLE_FONT_NAMES
import com.vidcam.app.ui.rememberGoogleFontFamily
import com.vidcam.app.ui.rememberGoogleFontsAvailable
import com.vidcam.app.ui.stickerAssetUri
import com.vidcam.app.util.formatDuration
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.defaultMinSize
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

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val recorder = remember { CameraRecorder(context) }
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var recording by remember { mutableStateOf(false) }
    var showMusicSheet by remember { mutableStateOf(false) }
    var showTextDialog by remember { mutableStateOf(false) }
    var showStickerDialog by remember { mutableStateOf(false) }
    var editingLayer by remember { mutableStateOf<OverlayLayer?>(null) }

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
        val dir = File(context.cacheDir, "captures").apply { mkdirs() }
        val file = File(dir, "rec-${System.currentTimeMillis()}.mp4")
        recording = true
        recorder.start(file) { recorded ->
            recording = false
            viewModel.addRecordedClip(recorded)
        }
        scope.launch {
            delay(MAX_DURATION_MS)
            if (recorder.isRecording) recorder.stop()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("VidCam") },
                actions = {
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
                        onLayerChange = viewModel::updateLayer,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }

            ControlsSection(
                project = project,
                recording = recording,
                canRecord = canRecord,
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
    onRequestPermissions: () -> Unit,
    onToggleLens: () -> Unit,
    onToggleRecord: () -> Unit,
    onImportVideo: () -> Unit,
    onAddPng: () -> Unit,
    onAddText: () -> Unit,
    onAddSticker: () -> Unit,
    onMusic: () -> Unit,
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = if (layer.kind == LayerKind.TEXT) layer.text else layer.label,
                            modifier = Modifier.weight(1f),
                            maxLines = 1,
                        )
                        Text(
                            text = "${formatDuration(layer.startMs)}–${formatDuration(layer.endMs)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        IconButton(onClick = { viewModel.removeLayer(layer.id) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar capa")
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
                text = "Clip · ${formatDuration(clip.trimmedDurationMs)}",
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
    }
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
private fun TimelinePreview(
    project: Project,
    onLayerChange: (OverlayLayer) -> Unit,
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

    LaunchedEffect(videoPlayer) {
        while (true) {
            val clips = clipsState.value
            val index = videoPlayer.currentMediaItemIndex
            val base = if (index in clips.indices) {
                clips.take(index).sumOf { it.trimmedDurationMs }
            } else {
                0L
            }
            positionMs = base + videoPlayer.currentPosition
            delay(100L)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            videoPlayer.release()
            musicPlayer.release()
        }
    }

    BoxWithConstraints(modifier) {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = videoPlayer
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        val density = LocalDensity.current
        val containerWidth = with(density) { maxWidth.toPx() }
        val containerHeight = with(density) { maxHeight.toPx() }
        Box(modifier = Modifier.fillMaxSize()) {
            project.layers
                .filter { positionMs in it.startMs..it.endMs }
                .forEach { layer ->
                    LayerBox(
                        layer = layer,
                        containerWidth = containerWidth,
                        containerHeight = containerHeight,
                        onLayerChange = onLayerChange,
                        isSelected = layer.id == selectedLayerId,
                        onSelect = { selectedLayerId = layer.id },
                    )
                }
        }
    }
}

@Composable
private fun LayerBox(
    layer: OverlayLayer,
    containerWidth: Float,
    containerHeight: Float,
    onLayerChange: (OverlayLayer) -> Unit,
    isSelected: Boolean = false,
    onSelect: () -> Unit = {},
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val currentLayer by rememberUpdatedState(layer)
    val bitmap = remember(layer.kind, layer.uri) {
        if (layer.kind == LayerKind.PNG) {
            layer.uri?.let { LayerBitmaps.decodePng(context, it) }
        } else {
            null
        }
    }

    // El tamaño base se expresa como fracción del ancho del lienzo y la
    // exportación usa la misma fracción sobre el ancho del vídeo. La escala
    // del gesto se aplica luego con graphicsLayer.
    val basePngWidthPx = containerWidth * LayerBitmaps.BASE_PNG_WIDTH_FRACTION
    val baseTextSizePx = minOf(containerWidth, containerHeight) *
        LayerBitmaps.BASE_TEXT_SIZE_FRACTION *
        (layer.fontSizeSp / LayerBitmaps.BASE_TEXT_SIZE_SP)

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .defaultMinSize(48.dp, 48.dp)
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
            )
            .pointerInput(layer.id) {
                awaitPointerEventScope {
                    while (true) {
                        val down = awaitFirstDown()
                        down.consume()

                        var dragging = false
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

                            when (changes.size) {
                                1 -> {
                                    val change = changes.first()
                                    val pan = change.position - down.position

                                    if (pan.getDistance() > viewConfiguration.touchSlop) {
                                        dragging = true
                                        val delta = change.position - lastPosition
                                        lastPosition = change.position
                                        val current = currentLayer
                                        onLayerChange(
                                            current.copy(
                                                x = (current.x + delta.x / containerWidth)
                                                    .coerceIn(0f, 1f),
                                                y = (current.y + delta.y / containerHeight)
                                                    .coerceIn(0f, 1f),
                                            ),
                                        )
                                    }
                                }
                                2 -> {
                                    dragging = true
                                    val p1 = changes[0].position
                                    val p2 = changes[1].position
                                    val distance = (p1 - p2).getDistance()
                                    val angle = atan2(p2.y - p1.y, p2.x - p1.x)

                                    if (lastDistance > 0f) {
                                        val zoomFactor = distance / lastDistance
                                        val rotationDelta = angle - lastAngle
                                        val current = currentLayer
                                        onLayerChange(
                                            current.copy(
                                                scale = (current.scale * zoomFactor)
                                                    .coerceIn(0.2f, 4f),
                                                rotationDeg = current.rotationDeg +
                                                    Math.toDegrees(rotationDelta.toDouble()).toFloat(),
                                            ),
                                        )
                                    }

                                    lastDistance = distance
                                    lastAngle = angle
                                }
                            }

                            changes.forEach { it.consume() }
                        } while (event.changes.any { it.pressed })

                        if (!dragging) {
                            onSelect()
                        }
                    }
                }
            },
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
    var tracks by remember { mutableStateOf<List<MusicTrack>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        viewModel.loadMusic {
            tracks = it
            loading = false
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        when {
            loading -> Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            tracks.isEmpty() -> Text(
                text = "No se encontró música en el dispositivo",
                modifier = Modifier.padding(24.dp),
            )

            else -> LazyColumn(modifier = Modifier.fillMaxWidth()) {
                items(tracks, key = { it.id }) { track ->
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
        }
    }
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
