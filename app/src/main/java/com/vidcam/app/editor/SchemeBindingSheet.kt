package com.vidcam.app.editor

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vidcam.app.model.scheme.BackgroundBinding
import com.vidcam.app.model.scheme.SchemeBinding
import com.vidcam.app.model.scheme.SchemeSlot
import com.vidcam.app.model.scheme.SchemeTiming
import com.vidcam.app.model.scheme.SlotBinding
import com.vidcam.app.model.scheme.SlotKind

/**
 * Hoja modal interactiva para vincular objetos reales (fondos, PNGs, textos y doblaje)
 * con los slots definidos en un [com.vidcam.app.model.scheme.UsageScheme].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemeBindingSheet(
    initialBinding: SchemeBinding,
    onDismiss: () -> Unit,
    onApplyBinding: (SchemeBinding) -> Unit,
) {
    var binding by remember { mutableStateOf(initialBinding) }
    val scheme = binding.scheme

    // Estado para saber cuál slot está esperando selección de imagen individual
    var pendingSlotId by remember { mutableStateOf<String?>(null) }
    var isSelectingBackground by remember { mutableStateOf(false) }

    val imageSlots = remember(scheme) {
        scheme.slots.filter { it.kind == SlotKind.IMAGE || it.kind == SlotKind.GIF }
    }

    // Picker individual
    val singleImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia(),
    ) { uri ->
        if (uri != null) {
            val uriStr = uri.toString()
            if (isSelectingBackground) {
                binding = binding.withBackground { it.copy(uri = uriStr, omitted = false) }
                isSelectingBackground = false
            } else if (pendingSlotId != null) {
                val targetId = pendingSlotId!!
                binding = binding.withSlot(targetId) { it.copy(uri = uriStr, omitted = false) }
                pendingSlotId = null
            }
        }
    }

    // Picker múltiple en lote
    val batchImagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickMultipleVisualMedia(
            maxItems = (imageSlots.size + (if (scheme.background != null) 1 else 0)).coerceAtLeast(2),
        ),
    ) { uris ->
        if (uris.isNotEmpty()) {
            val strUris = uris.map { it.toString() }
            // Si el fondo estaba vacío y seleccionó suficientes imágenes, asignar la primera al fondo
            val hasBgSlot = scheme.background != null && binding.background.uri.isNullOrBlank()
            val (forBg, forSlots) = if (hasBgSlot && strUris.size > imageSlots.size) {
                Pair(strUris.first(), strUris.drop(1))
            } else {
                Pair(null, strUris)
            }

            var updated = binding
            if (forBg != null) {
                updated = updated.withBackground { it.copy(uri = forBg, omitted = false) }
            }
            updated = SchemeTiming.withBatchImages(updated, forSlots)
            binding = updated
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
        ) {
            // Cabecera
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column {
                    Text(
                        text = "Vincular Esquema",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${scheme.name} (${scheme.totalDurationSec}s · ${scheme.stagesCount} etapas)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Atajo de selección en lote si hay ranuras de imagen
            if (imageSlots.isNotEmpty()) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                    ),
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "⚡ Asignar imágenes en lote",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                            )
                            Text(
                                text = "Elige ${imageSlots.size} fotos de una vez para auto-rellenar las ranuras",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Button(
                            onClick = {
                                batchImagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                        ) {
                            Text("Elegir fotos")
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }

            // Lista desplazable de slots
            LazyColumn(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                // 1. Slot de Fondo
                if (scheme.background != null) {
                    item {
                        BackgroundSlotRow(
                            binding = binding.background,
                            kenBurnsLabel = scheme.background.kenBurns.name.lowercase(),
                            onPick = {
                                isSelectingBackground = true
                                pendingSlotId = null
                                singleImagePicker.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                )
                            },
                            onClear = {
                                binding = binding.withBackground { it.copy(uri = null) }
                            },
                            onToggleOmitted = { omitted ->
                                binding = binding.withBackground { it.copy(omitted = omitted) }
                            },
                        )
                    }
                }

                // 2. Slots de elementos del esquema
                items(scheme.slots, key = { it.id }) { slot ->
                    val slotBinding = binding.getSlot(slot.id)
                    when (slot.kind) {
                        SlotKind.IMAGE, SlotKind.GIF -> {
                            ImageSlotRow(
                                slot = slot,
                                binding = slotBinding,
                                onPick = {
                                    isSelectingBackground = false
                                    pendingSlotId = slot.id
                                    singleImagePicker.launch(
                                        PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                                    )
                                },
                                onClear = {
                                    binding = binding.withSlot(slot.id) { it.copy(uri = null) }
                                },
                                onToggleOmitted = { omitted ->
                                    binding = binding.withSlot(slot.id) { it.copy(omitted = omitted) }
                                },
                            )
                        }
                        SlotKind.TEXT -> {
                            TextSlotRow(
                                slot = slot,
                                binding = slotBinding,
                                onTextChange = { newText ->
                                    binding = binding.withSlot(slot.id) { it.copy(text = newText) }
                                },
                                onToggleOmitted = { omitted ->
                                    binding = binding.withSlot(slot.id) { it.copy(omitted = omitted) }
                                },
                            )
                        }
                    }
                }

                // 3. Paso opcional: Doblaje de voz en off
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        ),
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "🎙️ Grabar voz en off al aplicar",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                )
                                Text(
                                    text = "Abre directamente la grabadora de voz sincronizada tras aplicar el esquema",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Switch(
                                checked = binding.openVoiceoverOnApply,
                                onCheckedChange = { checked ->
                                    binding = binding.copy(openVoiceoverOnApply = checked)
                                },
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))
            HorizontalDivider()
            Spacer(Modifier.height(10.dp))

            // Botones de acción final
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
                Spacer(Modifier.width(10.dp))
                Button(
                    onClick = {
                        onApplyBinding(binding)
                    },
                ) {
                    Text("Aplicar al proyecto")
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun BackgroundSlotRow(
    binding: BackgroundBinding,
    kenBurnsLabel: String,
    onPick: () -> Unit,
    onClear: () -> Unit,
    onToggleOmitted: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (binding.omitted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = !binding.omitted,
                onCheckedChange = { onToggleOmitted(!it) },
            )
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "🖼️ Fondo (${kenBurnsLabel})",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (binding.omitted) {
                        "Omitido"
                    } else if (!binding.uri.isNullOrBlank()) {
                        "✓ Asignado (${binding.label})"
                    } else {
                        "Sin asignar (usará color neutro)"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!binding.uri.isNullOrBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!binding.omitted) {
                if (!binding.uri.isNullOrBlank()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Delete, contentDescription = "Quitar fondo")
                    }
                }
                OutlinedButton(onClick = onPick) {
                    Text(if (binding.uri.isNullOrBlank()) "Elegir" else "Cambiar")
                }
            }
        }
    }
}

@Composable
private fun ImageSlotRow(
    slot: SchemeSlot,
    binding: SlotBinding,
    onPick: () -> Unit,
    onClear: () -> Unit,
    onToggleOmitted: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (binding.omitted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            },
        ),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Checkbox(
                checked = !binding.omitted,
                onCheckedChange = { onToggleOmitted(!it) },
            )
            Spacer(Modifier.width(6.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "📌 ${slot.label.ifBlank { "Imagen ${slot.id}" }}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = if (binding.omitted) {
                        "Omitido"
                    } else if (!binding.uri.isNullOrBlank()) {
                        "✓ Asignado (Etapas ${slot.stageStart}..${slot.stageEnd})"
                    } else {
                        "Pendiente (Etapas ${slot.stageStart}..${slot.stageEnd})"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = if (!binding.uri.isNullOrBlank()) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (!binding.omitted) {
                if (!binding.uri.isNullOrBlank()) {
                    IconButton(onClick = onClear) {
                        Icon(Icons.Default.Delete, contentDescription = "Quitar imagen")
                    }
                }
                OutlinedButton(onClick = onPick) {
                    Text(if (binding.uri.isNullOrBlank()) "Elegir" else "Cambiar")
                }
            }
        }
    }
}

@Composable
private fun TextSlotRow(
    slot: SchemeSlot,
    binding: SlotBinding,
    onTextChange: (String) -> Unit,
    onToggleOmitted: (Boolean) -> Unit,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (binding.omitted) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
            },
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Checkbox(
                    checked = !binding.omitted,
                    onCheckedChange = { onToggleOmitted(!it) },
                )
                Spacer(Modifier.width(6.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "✍️ ${slot.label.ifBlank { "Texto ${slot.id}" }}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        text = "Etapas ${slot.stageStart}..${slot.stageEnd}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            if (!binding.omitted) {
                Spacer(Modifier.height(6.dp))
                OutlinedTextField(
                    value = binding.text ?: slot.text,
                    onValueChange = onTextChange,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Contenido del titular") },
                    singleLine = true,
                )
            }
        }
    }
}
