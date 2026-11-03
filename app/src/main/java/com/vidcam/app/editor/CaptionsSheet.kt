package com.vidcam.app.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vidcam.app.data.TranscriptionSettings
import com.vidcam.app.model.Caption
import com.vidcam.app.model.CaptionStyle
import com.vidcam.app.model.Project

/**
 * Hoja de subtítulos: transcribir la voz en off, agregar o editar subtítulos a
 * mano y ajustar el estilo común con el que se queman en el MP4.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CaptionsSheet(
    project: Project,
    transcribing: Boolean,
    onDismiss: () -> Unit,
    onTranscribe: () -> Unit,
    onAdd: (String, Long, Long) -> Unit,
    onUpdate: (Caption) -> Unit,
    onRemove: (String) -> Unit,
    onStyle: (CaptionStyle) -> Unit,
    loadSettings: () -> TranscriptionSettings,
    onSaveSettings: (TranscriptionSettings) -> Unit,
) {
    var editing by remember { mutableStateOf<Caption?>(null) }
    var adding by remember { mutableStateOf(false) }
    var showSettings by remember { mutableStateOf(false) }
    val style = project.captionStyle
    val totalMs = project.outputDurationMs

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text("Subtítulos", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Se queman en el vídeo. \"Transcribir\" toma la voz en off y " +
                    "crea los subtítulos con sus tiempos.",
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = onTranscribe,
                    enabled = project.voiceover != null && !transcribing,
                ) {
                    Text(if (transcribing) "Transcribiendo…" else "Transcribir voz")
                }
                Spacer(Modifier.width(8.dp))
                TextButton(onClick = { adding = true }, enabled = totalMs > 0L) {
                    Text("Agregar")
                }
                TextButton(onClick = { showSettings = true }) { Text("Servicio…") }
            }
            if (transcribing) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(top = 8.dp))
            }
            if (project.voiceover == null) {
                Text(
                    text = "Graba primero la voz en off para poder transcribir.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text("Estilo", style = MaterialTheme.typography.titleSmall)
            SliderRow(
                label = "Tamaño",
                value = style.sizeFraction,
                range = CaptionStyle.MIN_SIZE_FRACTION..CaptionStyle.MAX_SIZE_FRACTION,
                onChange = { onStyle(style.copy(sizeFraction = it)) },
            )
            SliderRow(
                label = "Altura en pantalla",
                value = style.yFraction,
                range = CaptionStyle.MIN_Y_FRACTION..CaptionStyle.MAX_Y_FRACTION,
                onChange = { onStyle(style.copy(yFraction = it)) },
            )
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("MAYÚSCULAS", modifier = Modifier.weight(1f))
                Switch(
                    checked = style.uppercase,
                    onCheckedChange = { onStyle(style.copy(uppercase = it)) },
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Fondo detrás del texto", modifier = Modifier.weight(1f))
                Switch(
                    checked = style.backgroundArgb != 0,
                    onCheckedChange = { onStyle(style.copy(backgroundArgb = if (it) 0x99000000.toInt() else 0)) },
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "Todos (${project.captions.size})",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.weight(1f),
                )
            }
            if (project.captions.isEmpty()) {
                Text(
                    text = "Todavía no hay subtítulos.",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.height(240.dp)) {
                    items(project.captions, key = { it.id }) { caption ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { editing = caption }
                                .padding(vertical = 6.dp),
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = caption.text,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    style = MaterialTheme.typography.bodySmall,
                                )
                                Text(
                                    text = "${formatTime(caption.startMs)} → ${formatTime(caption.endMs)}",
                                    style = MaterialTheme.typography.labelSmall,
                                )
                            }
                            TextButton(onClick = { onRemove(caption.id) }) { Text("Quitar") }
                        }
                        HorizontalDivider()
                    }
                }
            }
        }
    }

    editing?.let { caption ->
        CaptionEditDialog(
            caption = caption,
            totalMs = totalMs,
            onDismiss = { editing = null },
            onConfirm = { updated ->
                onUpdate(updated)
                editing = null
            },
        )
    }
    if (adding) {
        CaptionEditDialog(
            caption = Caption(
                id = "cap-${System.currentTimeMillis()}",
                text = "",
                startMs = 0L,
                endMs = minOf(2_500L, totalMs.coerceAtLeast(1_000L)),
            ),
            totalMs = totalMs,
            onDismiss = { adding = false },
            onConfirm = { created ->
                onAdd(created.text, created.startMs, created.endMs)
                adding = false
            },
        )
    }
    if (showSettings) {
        TranscriptionSettingsDialog(
            initial = loadSettings(),
            onDismiss = { showSettings = false },
            onSave = {
                onSaveSettings(it)
                showSettings = false
            },
        )
    }
}

@Composable
private fun CaptionEditDialog(
    caption: Caption,
    totalMs: Long,
    onDismiss: () -> Unit,
    onConfirm: (Caption) -> Unit,
) {
    var text by remember { mutableStateOf(caption.text) }
    var startMs by remember { mutableStateOf(caption.startMs.toFloat()) }
    var endMs by remember { mutableStateOf(caption.endMs.toFloat()) }
    val max = totalMs.coerceAtLeast(1_000L).toFloat()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Subtítulo") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                OutlinedTextField(
                    value = text,
                    onValueChange = { text = it },
                    label = { Text("Texto") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                    maxLines = 4,
                )
                Spacer(Modifier.height(8.dp))
                Text("Entrada ${formatTime(startMs.toLong())}", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = startMs.coerceIn(0f, max),
                    onValueChange = { value ->
                        startMs = value
                        if (endMs <= value) endMs = (value + 500f).coerceAtMost(max)
                    },
                    valueRange = 0f..max,
                )
                Text("Salida ${formatTime(endMs.toLong())}", style = MaterialTheme.typography.bodySmall)
                Slider(
                    value = endMs.coerceIn(0f, max),
                    onValueChange = { endMs = it.coerceAtLeast(startMs + 100f).coerceAtMost(max) },
                    valueRange = 0f..max,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onConfirm(caption.copy(text = text, startMs = startMs.toLong(), endMs = endMs.toLong()))
                },
                enabled = text.isNotBlank(),
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun TranscriptionSettingsDialog(
    initial: TranscriptionSettings,
    onDismiss: () -> Unit,
    onSave: (TranscriptionSettings) -> Unit,
) {
    var url by remember { mutableStateOf(initial.url) }
    var token by remember { mutableStateOf(initial.token) }
    var language by remember { mutableStateOf(initial.language) }
    var model by remember { mutableStateOf(initial.model) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Servicio de transcripción") },
        text = {
            Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                Text(
                    text = "Cualquier endpoint tipo Whisper que reciba el campo " +
                        "\"file\". Por ejemplo el del homelab: " +
                        "http://192.168.1.10:3007/transcribe",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = url,
                    onValueChange = { url = it },
                    label = { Text("URL") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = token,
                    onValueChange = { token = it },
                    label = { Text("Clave (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = language,
                    onValueChange = { language = it },
                    label = { Text("Idioma (es, en, …)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = model,
                    onValueChange = { model = it },
                    label = { Text("Modelo (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSave(
                        TranscriptionSettings(
                            url = url,
                            token = token,
                            language = language,
                            model = model,
                        ),
                    )
                },
            ) { Text("Guardar") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } },
    )
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
        )
    }
}

private fun formatTime(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) + 500L) / 1000L
    return "${totalSeconds / 60L}:${(totalSeconds % 60L).toString().padStart(2, '0')}"
}
