package com.vidcam.app.editor

import android.content.Context
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vidcam.app.data.SavedScheme
import com.vidcam.app.export.ShareUtils
import com.vidcam.app.model.scheme.SchemeSlot
import com.vidcam.app.model.scheme.SlotKind
import com.vidcam.app.model.scheme.UsageScheme

/**
 * Hoja modal para explorar, previsualizar por etapas, aplicar y guardar
 * esquemas de uso (templates).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SchemesSheet(
    schemes: List<SavedScheme>,
    onDismiss: () -> Unit,
    onApply: (UsageScheme) -> Unit,
    onSaveCurrent: (name: String, stagesCount: Int) -> Unit,
    onDelete: (id: String) -> Unit,
    onExportJson: (UsageScheme) -> String,
    onImportJson: (String) -> Boolean,
) {
    val context = LocalContext.current
    var selectedFilter by remember { mutableStateOf("TODOS") } // "TODOS", "FABRICA", "MIS_ESQUEMAS"
    var showSaveDialog by remember { mutableStateOf(false) }
    var showImportDialog by remember { mutableStateOf(false) }

    val filteredSchemes = remember(schemes, selectedFilter) {
        when (selectedFilter) {
            "FABRICA" -> schemes.filter { !it.isCustom }
            "MIS_ESQUEMAS" -> schemes.filter { it.isCustom }
            else -> schemes
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
                        text = "Esquemas y Plantillas",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "Coreografías temporales modulares por etapas",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Chips de filtrado y acciones rápidas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilterChip(
                    selected = selectedFilter == "TODOS",
                    onClick = { selectedFilter = "TODOS" },
                    label = { Text("Todos") },
                )
                FilterChip(
                    selected = selectedFilter == "FABRICA",
                    onClick = { selectedFilter = "FABRICA" },
                    label = { Text("De fábrica") },
                )
                FilterChip(
                    selected = selectedFilter == "MIS_ESQUEMAS",
                    onClick = { selectedFilter = "MIS_ESQUEMAS" },
                    label = { Text("Mis esquemas") },
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Button(
                    onClick = { showSaveDialog = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Guardar actual…")
                }
                OutlinedButton(
                    onClick = { showImportDialog = true },
                    modifier = Modifier.weight(1f),
                ) {
                    Text("Pegar JSON…")
                }
            }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(8.dp))

            // Lista de esquemas
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f, fill = false),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                items(filteredSchemes, key = { it.id }) { item ->
                    SchemeCard(
                        saved = item,
                        onApply = {
                            onApply(item.scheme)
                            onDismiss()
                        },
                        onShare = {
                            val jsonString = onExportJson(item.scheme)
                            ShareUtils.shareText(context, jsonString, "Esquema: ${item.scheme.name}")
                        },
                        onDelete = { onDelete(item.id) },
                    )
                }

                if (filteredSchemes.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = "No hay esquemas en esta categoría",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        SaveSchemeDialog(
            onDismiss = { showSaveDialog = false },
            onConfirm = { name, stages ->
                onSaveCurrent(name, stages)
                showSaveDialog = false
            },
        )
    }

    if (showImportDialog) {
        ImportSchemeDialog(
            onDismiss = { showImportDialog = false },
            onImport = { jsonText ->
                val ok = onImportJson(jsonText)
                if (ok) showImportDialog = false
            },
        )
    }
}

/**
 * Tarjeta de esquema con visualización de etapas y acciones.
 */
@Composable
private fun SchemeCard(
    saved: SavedScheme,
    onApply: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    val scheme = saved.scheme
    val stages = scheme.stagesCount.coerceAtLeast(1)
    val stageDurationSec = scheme.totalDurationSec.toFloat() / stages

    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = scheme.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text = "${scheme.totalDurationSec}s · $stages etapas (~${String.format("%.1f", stageDurationSec)}s c/u)",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Row {
                    IconButton(onClick = onShare) {
                        Icon(Icons.Default.Share, contentDescription = "Compartir JSON")
                    }
                    if (saved.isCustom) {
                        IconButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Eliminar esquema")
                        }
                    }
                }
            }

            if (scheme.description.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = scheme.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Spacer(Modifier.height(10.dp))

            // Diagrama visual de etapas
            StagesVisualizer(scheme = scheme)

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onApply,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Aplicar esquema")
            }
        }
    }
}

/**
 * Diagrama de barras de tiempo que ilustra visualmente la presencia de cada capa y fondo.
 */
@Composable
private fun StagesVisualizer(scheme: UsageScheme) {
    val count = scheme.stagesCount.coerceAtLeast(1)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        // Regla de etapas
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            for (i in 1..count) {
                Text(
                    text = "E$i",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        // Barra del fondo
        scheme.background?.let { bg ->
            StageBar(
                label = "Fondo",
                stageStart = bg.stageStart,
                stageEnd = bg.stageEnd,
                stagesCount = count,
                color = MaterialTheme.colorScheme.tertiary,
            )
        }

        // Barras de cada slot
        scheme.slots.forEach { slot ->
            val color = when (slot.kind) {
                SlotKind.IMAGE -> MaterialTheme.colorScheme.primary
                SlotKind.TEXT -> MaterialTheme.colorScheme.secondary
                SlotKind.GIF -> MaterialTheme.colorScheme.error
            }
            StageBar(
                label = slot.label.ifBlank { slot.id },
                stageStart = slot.stageStart,
                stageEnd = slot.stageEnd,
                stagesCount = count,
                color = color,
            )
        }
    }
}

@Composable
private fun StageBar(
    label: String,
    stageStart: Int,
    stageEnd: Int,
    stagesCount: Int,
    color: Color,
) {
    val count = stagesCount.coerceAtLeast(1)
    val start = stageStart.coerceIn(1, count)
    val end = stageEnd.coerceIn(start, count)

    val leftWeight = (start - 1).toFloat()
    val barWeight = (end - start + 1).toFloat()
    val rightWeight = (count - end).toFloat()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (leftWeight > 0f) {
            Spacer(Modifier.weight(leftWeight))
        }
        Box(
            modifier = Modifier
                .weight(barWeight)
                .height(16.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(color.copy(alpha = 0.85f)),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.White,
                maxLines = 1,
            )
        }
        if (rightWeight > 0f) {
            Spacer(Modifier.weight(rightWeight))
        }
    }
}

@Composable
private fun SaveSchemeDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, stagesCount: Int) -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var stagesCount by remember { mutableIntStateOf(5) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Guardar como esquema") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Crea una plantilla reutilizable a partir de las capas y fondos actuales.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre del esquema") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Cantidad de etapas modulares:",
                    style = MaterialTheme.typography.labelMedium,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf(3, 4, 5, 6).forEach { n ->
                        FilterChip(
                            selected = stagesCount == n,
                            onClick = { stagesCount = n },
                            label = { Text("$n etapas") },
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.ifBlank { "Mi Esquema" }, stagesCount) },
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}

@Composable
private fun ImportSchemeDialog(
    onDismiss: () -> Unit,
    onImport: (String) -> Unit,
) {
    var jsonText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Importar esquema JSON") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Pega aquí el código JSON de un esquema compartido.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = jsonText,
                    onValueChange = { jsonText = it },
                    label = { Text("Código JSON") },
                    maxLines = 8,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onImport(jsonText) },
                enabled = jsonText.isNotBlank(),
            ) {
                Text("Importar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        },
    )
}
