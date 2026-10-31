package com.vidcam.app.editor

import android.os.SystemClock
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.vidcam.app.model.Project
import com.vidcam.app.model.VoiceoverMath
import kotlinx.coroutines.delay

/**
 * Hoja de la voz en off: graba el doblaje sobre el vídeo ya montado, elige si
 * se aísla el entorno con RNNoise, ajusta la entrada y mezcla voz, audio
 * original y música.
 *
 * La vista previa queda detrás de la hoja, reproduciéndose: el usuario habla
 * encima de lo que ve y la toma se detiene sola al terminar el vídeo.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceoverSheet(
    project: Project,
    recording: Boolean,
    processing: Boolean,
    level: Float,
    totalMs: Long,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onDismiss: () -> Unit,
    onToggleDenoise: (Boolean) -> Unit,
    onVolume: (Float) -> Unit,
    onStartMs: (Long) -> Unit,
    onRemove: () -> Unit,
    onOriginalVolume: (Float) -> Unit,
    onOriginalMuted: (Boolean) -> Unit,
    onMusicVolume: (Float) -> Unit,
    onMusicEnabled: (Boolean) -> Unit,
) {
    val voice = project.voiceover
    var elapsedMs by remember { mutableStateOf(0L) }

    LaunchedEffect(recording) {
        if (!recording) {
            elapsedMs = 0L
            return@LaunchedEffect
        }
        val startedAt = SystemClock.elapsedRealtime()
        while (true) {
            elapsedMs = SystemClock.elapsedRealtime() - startedAt
            delay(100L)
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Text("Voz en off", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(4.dp))
            Text(
                text = "La vista previa se reproduce y tú hablas encima: la toma " +
                    "se corta sola al terminar el vídeo. Usa audífonos, porque el " +
                    "altavoz se cuela en el micrófono.",
                style = MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Button(
                    onClick = if (recording) onStop else onStart,
                    enabled = totalMs > 0L && !processing,
                ) {
                    Text(
                        when {
                            recording -> "Detener"
                            voice == null -> "Grabar"
                            else -> "Regrabar"
                        },
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    LinearProgressIndicator(
                        progress = { if (recording) level else 0f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = when {
                            recording -> "${formatMs(elapsedMs)} / ${formatMs(totalMs)}"
                            processing -> "Aislando el entorno…"
                            voice != null -> "Toma de ${formatMs(voice.durationMs)}"
                            else -> "Sin toma todavía"
                        },
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                if (voice != null && !recording) {
                    TextButton(onClick = onRemove, enabled = !processing) { Text("Quitar") }
                }
            }

            if (voice != null) {
                Spacer(Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Aislar entorno (RNNoise)")
                        Text(
                            text = "Quita ventilador, calle y ruido de fondo de la toma",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = voice.denoise,
                        onCheckedChange = onToggleDenoise,
                        enabled = !processing,
                    )
                }
                SliderRow(
                    label = "Volumen de la voz",
                    value = voice.volume,
                    range = 0f..1f,
                    onChange = onVolume,
                )
                val maxStart = VoiceoverMath.MAX_START_MS.coerceAtMost(totalMs.coerceAtLeast(0L))
                SliderRow(
                    label = "Entrada ${signedSeconds(voice.startMs)}",
                    value = voice.startMs.toFloat(),
                    range = VoiceoverMath.MIN_START_MS.toFloat()..maxStart.toFloat(),
                    onChange = { onStartMs(it.toLong()) },
                )
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))
            Text("Mezcla", style = MaterialTheme.typography.titleSmall)
            Spacer(Modifier.height(4.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Audio original")
                    Text(
                        text = "El sonido que grabó la cámara",
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
                Switch(
                    checked = !project.originalAudioMuted,
                    onCheckedChange = { onOriginalMuted(!it) },
                )
            }
            SliderRow(
                label = "Volumen del audio original",
                value = project.originalVolume,
                range = 0f..1f,
                onChange = onOriginalVolume,
                enabled = !project.originalAudioMuted,
            )
            if (project.music != null) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(project.music.title, maxLines = 1)
                        Text(
                            text = "Música del proyecto",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Switch(
                        checked = project.musicEnabled,
                        onCheckedChange = onMusicEnabled,
                    )
                }
                SliderRow(
                    label = "Volumen de la música",
                    value = project.musicVolume,
                    range = 0f..1f,
                    onChange = onMusicVolume,
                    enabled = project.musicEnabled,
                )
            }
        }
    }
}

@Composable
private fun SliderRow(
    label: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    onChange: (Float) -> Unit,
    enabled: Boolean = true,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
        )
        Slider(
            value = value.coerceIn(range.start, range.endInclusive),
            onValueChange = onChange,
            valueRange = range,
            enabled = enabled,
        )
    }
}

private fun formatMs(ms: Long): String {
    val totalSeconds = (ms.coerceAtLeast(0L) + 500L) / 1000L
    val seconds = totalSeconds % 60L
    val minutes = totalSeconds / 60L
    return if (minutes > 0L) "${minutes}:${seconds.toString().padStart(2, '0')}" else "$seconds s"
}

private fun signedSeconds(ms: Long): String {
    val sign = if (ms < 0L) "−" else "+"
    val value = kotlin.math.abs(ms) / 1000.0
    return "$sign${String.format(java.util.Locale.getDefault(), "%.2f", value)} s"
}
