package com.vidcam.app.editor

import android.os.SystemClock
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.FilterChip
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
import com.vidcam.app.model.VoiceDoublerConfig
import com.vidcam.app.model.VoiceDoublerPreset
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
    paused: Boolean = false,
    processing: Boolean,
    level: Float,
    totalMs: Long,
    onStart: () -> Unit,
    onPauseToggle: () -> Unit = {},
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
    onVoiceFades: (Long, Long) -> Unit,
    onMusicFades: (Long, Long) -> Unit,
    onDucking: (Float) -> Unit,
    onDoublerPreset: (VoiceDoublerPreset) -> Unit = {},
    onDoublerConfig: (VoiceDoublerConfig) -> Unit = {},
) {
    val voice = project.voiceover
    var elapsedMs by remember { mutableStateOf(0L) }

    LaunchedEffect(recording, paused) {
        if (!recording) {
            elapsedMs = 0L
            return@LaunchedEffect
        }
        if (paused) return@LaunchedEffect
        var lastTick = SystemClock.elapsedRealtime()
        while (true) {
            delay(50L)
            val now = SystemClock.elapsedRealtime()
            elapsedMs += (now - lastTick)
            lastTick = now
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (recording) {
                    Button(
                        onClick = onPauseToggle,
                        enabled = !processing,
                    ) {
                        Text(if (paused) "Continuar" else "Pausar")
                    }
                    Button(
                        onClick = onStop,
                        enabled = !processing,
                    ) {
                        Text("Detener")
                    }
                } else {
                    Button(
                        onClick = onStart,
                        enabled = totalMs > 0L && !processing,
                    ) {
                        Text(
                            when {
                                voice == null -> "Grabar"
                                else -> "Regrabar"
                            },
                        )
                    }
                }
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    LinearProgressIndicator(
                        progress = { if (recording && !paused) level else 0f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        text = when {
                            recording && paused -> "${formatMs(elapsedMs)} / ${formatMs(totalMs)} (Pausa)"
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

                Spacer(Modifier.height(10.dp))
                Text("Doblaje automático y murmullo", style = MaterialTheme.typography.titleSmall)
                Text(
                    text = "Añade clones de voz en otro tono o genera un murmullo de multitud con control total",
                    style = MaterialTheme.typography.bodySmall,
                )
                Spacer(Modifier.height(6.dp))

                val doublerConfig = voice.doubler ?: VoiceDoublerConfig.OFF
                val currentPreset = if (!doublerConfig.enabled) VoiceDoublerPreset.OFF else doublerConfig.preset

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    VoiceDoublerPreset.entries.forEach { p ->
                        if (p != VoiceDoublerPreset.PERSONALIZADO || currentPreset == VoiceDoublerPreset.PERSONALIZADO) {
                            FilterChip(
                                selected = currentPreset == p,
                                onClick = { onDoublerPreset(p) },
                                label = { Text(p.label) },
                                enabled = !processing,
                            )
                        }
                    }
                }

                var showAdvanced by remember { mutableStateOf(false) }
                if (doublerConfig.enabled) {
                    Spacer(Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (showAdvanced) "Ocultar ajustes avanzados" else "Ajustes avanzados de doblaje",
                            style = MaterialTheme.typography.labelMedium,
                        )
                        TextButton(onClick = { showAdvanced = !showAdvanced }) {
                            Text(if (showAdvanced) "Cerrar" else "Personalizar")
                        }
                    }

                    if (showAdvanced || currentPreset == VoiceDoublerPreset.PERSONALIZADO) {
                        SliderRow(
                            label = "Cantidad de voces (${doublerConfig.voiceCount})",
                            value = doublerConfig.voiceCount.toFloat(),
                            range = 1f..5f,
                            onChange = {
                                onDoublerConfig(
                                    doublerConfig.copy(
                                        voiceCount = it.toInt(),
                                        preset = VoiceDoublerPreset.PERSONALIZADO,
                                    )
                                )
                            },
                            enabled = !processing,
                        )
                        SliderRow(
                            label = "Tono (${if (doublerConfig.pitchSemitones > 0) "+${doublerConfig.pitchSemitones}" else "${doublerConfig.pitchSemitones}"} semitonos)",
                            value = doublerConfig.pitchSemitones.toFloat(),
                            range = -12f..12f,
                            onChange = {
                                onDoublerConfig(
                                    doublerConfig.copy(
                                        pitchSemitones = it.toInt(),
                                        preset = VoiceDoublerPreset.PERSONALIZADO,
                                    )
                                )
                            },
                            enabled = !processing,
                        )
                        SliderRow(
                            label = "Desfase temporal (${doublerConfig.delayMs} ms)",
                            value = doublerConfig.delayMs.toFloat(),
                            range = 10f..80f,
                            onChange = {
                                onDoublerConfig(
                                    doublerConfig.copy(
                                        delayMs = it.toLong(),
                                        preset = VoiceDoublerPreset.PERSONALIZADO,
                                    )
                                )
                            },
                            enabled = !processing,
                        )
                        SliderRow(
                            label = "Mezcla de doblaje (${(doublerConfig.wetMix * 100).toInt()} %)",
                            value = doublerConfig.wetMix,
                            range = 0f..1f,
                            onChange = {
                                onDoublerConfig(
                                    doublerConfig.copy(
                                        wetMix = it,
                                        preset = VoiceDoublerPreset.PERSONALIZADO,
                                    )
                                )
                            },
                            enabled = !processing,
                        )
                    }
                }
                SliderRow(
                    label = "Volumen de la voz",
                    value = voice.volume,
                    range = 0f..1f,
                    onChange = onVolume,
                )
                val maxFade = totalMs.coerceAtLeast(0L).coerceAtMost(1_500L).toFloat()
                SliderRow(
                    label = "Fundido de entrada de la voz (${voice.fadeInMs} ms)",
                    value = voice.fadeInMs.toFloat(),
                    range = 0f..maxFade.coerceAtLeast(1f),
                    onChange = { onVoiceFades(it.toLong(), voice.fadeOutMs) },
                )
                SliderRow(
                    label = "Fundido de salida de la voz (${voice.fadeOutMs} ms)",
                    value = voice.fadeOutMs.toFloat(),
                    range = 0f..maxFade.coerceAtLeast(1f),
                    onChange = { onVoiceFades(voice.fadeInMs, it.toLong()) },
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
                val maxMusicFade = totalMs.coerceAtLeast(0L).coerceAtMost(3_000L).toFloat()
                SliderRow(
                    label = "Fundido de entrada de la música (${project.musicFadeInMs} ms)",
                    value = project.musicFadeInMs.toFloat(),
                    range = 0f..maxMusicFade.coerceAtLeast(1f),
                    onChange = { onMusicFades(it.toLong(), project.musicFadeOutMs) },
                )
                SliderRow(
                    label = "Fundido de salida de la música (${project.musicFadeOutMs} ms)",
                    value = project.musicFadeOutMs.toFloat(),
                    range = 0f..maxMusicFade.coerceAtLeast(1f),
                    onChange = { onMusicFades(project.musicFadeInMs, it.toLong()) },
                )
                SliderRow(
                    label = "Bajar la música con la voz (${(project.duckingAmount * 100).toInt()} %)",
                    value = project.duckingAmount,
                    range = 0f..0.9f,
                    onChange = onDucking,
                    enabled = voice != null,
                )
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
