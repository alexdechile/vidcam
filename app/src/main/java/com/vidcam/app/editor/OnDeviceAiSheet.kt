package com.vidcam.app.editor

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp

private enum class AiPromptPreset(
    val label: String,
    val template: String,
) {
    TITLE(
        label = "Titular corto",
        template = "Escribe 1 titular corto y llamativo (máximo 8 palabras) en español para un vídeo sobre: ",
    ),
    VOICEOVER(
        label = "Guion voice-over",
        template = "Redacta un guion breve de voice-over (20 segundos, tono natural) en español para un vídeo sobre: ",
    ),
    HOOK(
        label = "Gancho / subtítulo",
        template = "Sugiere una frase inicial de gancho (1 línea) en español para captar atención sobre: ",
    ),
}

/**
 * Hoja del asistente creativo offline basado en Google AI Edge (MediaPipe GenAI).
 * Permite seleccionar un modelo externo `.task` o `.litertlm`, generar titulares o
 * guiones de voice-over sin conexión, e insertarlos como capa de texto o copiarlos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnDeviceAiSheet(
    aiState: OnDeviceAiUiState,
    onSelectModelUri: (Uri) -> Unit,
    onClearModel: () -> Unit,
    onReleaseEngine: () -> Unit,
    onGenerate: (String) -> Unit,
    onInsertAsTextLayer: (String) -> Unit,
    onCopied: () -> Unit,
    onDismiss: () -> Unit,
) {
    val clipboard = LocalClipboardManager.current
    var selectedPreset by remember { mutableStateOf(AiPromptPreset.TITLE) }
    var topicInput by remember { mutableStateOf("") }

    val modelPicker = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        uri?.let(onSelectModelUri)
    }

    val busy = aiState.isLoading || aiState.isGenerating

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Asistente IA Offline",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        text = "Google AI Edge · 100% local en el dispositivo",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                TextButton(onClick = onDismiss) {
                    Text("Cerrar")
                }
            }

            if (busy) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(
                    text = if (aiState.isLoading) {
                        "Importando modelo local..."
                    } else {
                        "Generando texto en el dispositivo..."
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            if (aiState.activeModelName == null) {
                Surface(
                    tonalElevation = 2.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Text(
                            text = "Sin modelo configurado",
                            style = MaterialTheme.typography.titleSmall,
                        )
                        Text(
                            text = "Selecciona un archivo .task o .litertlm (por ejemplo Gemma) descargado en tu teléfono. Los pesos no se incluyen en la APK.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Button(
                            onClick = { modelPicker.launch(arrayOf("*/*")) },
                            enabled = !busy,
                        ) {
                            Text("Seleccionar .task / .litertlm")
                        }
                    }
                }
            } else {
                Surface(
                    tonalElevation = 1.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                    ) {
                        Text(
                            text = "Modelo: ${aiState.activeModelName}",
                            style = MaterialTheme.typography.labelLarge,
                        )
                        Text(
                            text = "${formatModelSize(aiState.activeModelSizeBytes)} · " +
                                if (aiState.isModelLoadedInMemory) "En memoria" else "En espera",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            OutlinedButton(
                                onClick = { modelPicker.launch(arrayOf("*/*")) },
                                enabled = !busy,
                            ) {
                                Text("Cambiar")
                            }
                            if (aiState.isModelLoadedInMemory) {
                                OutlinedButton(
                                    onClick = onReleaseEngine,
                                    enabled = !busy,
                                ) {
                                    Text("Liberar RAM")
                                }
                            }
                            TextButton(
                                onClick = onClearModel,
                                enabled = !busy,
                            ) {
                                Text("Quitar")
                            }
                        }
                    }
                }
            }

            if (aiState.error != null) {
                Text(
                    text = aiState.error,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }

            HorizontalDivider()

            Text(
                text = "Tipo de asistencia",
                style = MaterialTheme.typography.labelLarge,
            )

            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                AiPromptPreset.entries.forEach { preset ->
                    FilterChip(
                        selected = selectedPreset == preset,
                        onClick = { selectedPreset = preset },
                        label = { Text(preset.label) },
                    )
                }
            }

            OutlinedTextField(
                value = topicInput,
                onValueChange = { topicInput = it },
                label = { Text("Tema o idea del vídeo") },
                placeholder = { Text("Ej: receta rápida de café frío en casa") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 2,
                maxLines = 4,
                enabled = !busy && aiState.activeModelName != null,
            )

            Button(
                onClick = {
                    val prompt = "${selectedPreset.template}${topicInput.trim()}"
                    onGenerate(prompt)
                },
                enabled = !busy && aiState.activeModelName != null && topicInput.isNotBlank(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Generar con IA local")
            }

            if (aiState.generatedText.isNotBlank()) {
                HorizontalDivider()
                Text(
                    text = "Resultado",
                    style = MaterialTheme.typography.labelLarge,
                )
                Surface(
                    tonalElevation = 2.dp,
                    shape = MaterialTheme.shapes.medium,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        text = aiState.generatedText,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(12.dp),
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Button(
                        onClick = { onInsertAsTextLayer(aiState.generatedText) },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Insertar como texto")
                    }
                    Spacer(Modifier.width(4.dp))
                    OutlinedButton(
                        onClick = {
                            clipboard.setText(AnnotatedString(aiState.generatedText))
                            onCopied()
                        },
                        modifier = Modifier.weight(1f),
                    ) {
                        Text("Copiar")
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
        }
    }
}

private fun formatModelSize(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val mb = bytes / (1024.0 * 1024.0)
    return if (mb >= 1024.0) {
        String.format("%.2f GB", mb / 1024.0)
    } else {
        String.format("%.1f MB", mb)
    }
}
