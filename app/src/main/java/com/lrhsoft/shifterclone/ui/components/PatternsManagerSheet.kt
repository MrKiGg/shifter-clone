package com.lrhsoft.shifterclone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lrhsoft.shifterclone.model.SavedPattern
import com.lrhsoft.shifterclone.model.Shift
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PatternsManagerSheet(
    patterns: List<SavedPattern>,
    shifts: List<Shift>,
    onDismiss: () -> Unit,
    onSavePatterns: (List<SavedPattern>) -> Unit,
    onApplyPattern: (SavedPattern) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var local by remember { mutableStateOf(patterns) }
    var editing by remember { mutableStateOf<SavedPattern?>(null) }
    var creating by remember { mutableStateOf(false) }

    if (creating || editing != null) {
        PatternEditorSheet(
            initial = editing,
            shifts = shifts,
            onDismiss = { creating = false; editing = null },
            onSave = { pattern ->
                local = if (editing != null) local.map { if (it.id == pattern.id) pattern else it } else local + pattern
                onSavePatterns(local)
                creating = false
                editing = null
            },
            onApply = { pattern ->
                val updated = if (editing != null) local.map { if (it.id == pattern.id) pattern else it } else local + pattern
                local = updated
                onSavePatterns(updated)
                creating = false
                editing = null
                onApplyPattern(pattern)
            }
        )
        return
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().fillMaxHeight(0.92f).padding(horizontal = 16.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Patrones", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Cerrar") }
            }
            Text(
                "Crea secuencias de turnos y aplicalas desde una fecha del calendario.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))
            Button(onClick = { creating = true }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(6.dp))
                Text("Nuevo patron")
            }
            Spacer(Modifier.height(12.dp))
            if (local.isEmpty()) {
                Text("No hay patrones guardados.", color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 24.dp))
            }
            Column(
                modifier = Modifier.weight(1f).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                local.forEach { p ->
                    PatternListItem(
                        pattern = p,
                        shifts = shifts,
                        onEdit = { editing = p },
                        onDelete = {
                            local = local.filter { it.id != p.id }
                            onSavePatterns(local)
                        },
                        onApply = { onApplyPattern(p) }
                    )
                    HorizontalDivider()
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PatternListItem(
    pattern: SavedPattern,
    shifts: List<Shift>,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onApply: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(pattern.name, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Text("${pattern.length} dia${if (pattern.length == 1) "" else "s"}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF1565C0)) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color(0xFFC62828)) }
            IconButton(onClick = onApply) { Icon(Icons.Default.PlayArrow, contentDescription = "Aplicar", tint = Color(0xFF2E7D32)) }
        }
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            pattern.sequence.forEach { id ->
                val shift = id?.let { sid -> shifts.find { it.id == sid } }
                Box(
                    modifier = Modifier.size(28.dp).clip(RoundedCornerShape(6.dp)).background(shift?.toComposeColor() ?: Color(0xFFE0E0E0)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(shift?.abbreviation ?: "·", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = shift?.toTextComposeColor() ?: Color.Gray)
                }
            }
        }
        TextButton(onClick = onApply, modifier = Modifier.padding(top = 2.dp)) { Text("Aplicar patron") }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PatternEditorSheet(
    initial: SavedPattern?,
    shifts: List<Shift>,
    onDismiss: () -> Unit,
    onSave: (SavedPattern) -> Unit,
    onApply: (SavedPattern) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val initialLen = initial?.length?.coerceIn(1, 31) ?: 7
    var dayCount by remember { mutableIntStateOf(initialLen) }
    var cells by remember {
        mutableStateOf((initial?.sequence ?: List(initialLen) { null as String? }).toMutableList())
    }
    var selectedShiftId by remember { mutableStateOf<String?>(shifts.firstOrNull()?.id) }
    var isEraser by remember { mutableStateOf(false) }
    var showNameDialog by remember { mutableStateOf(false) }
    var pendingAction by remember { mutableStateOf("save") }
    var nameText by remember { mutableStateOf(initial?.name ?: "") }

    fun resize(n: Int) {
        val newCells = cells.toMutableList()
        while (newCells.size < n) newCells.add(null)
        while (newCells.size > n) newCells.removeAt(newCells.lastIndex)
        cells = newCells
        dayCount = n
    }

    fun buildPattern(name: String): SavedPattern =
        SavedPattern(
            id = initial?.id ?: UUID.randomUUID().toString(),
            name = name.trim().ifBlank { "Patron" },
            sequence = cells.toList()
        )

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(modifier = Modifier.fillMaxWidth().fillMaxHeight(0.95f).padding(horizontal = 12.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text(if (initial != null) "Editar patron" else "Nuevo patron", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Cerrar") }
            }
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Dias del patron:", fontWeight = FontWeight.Medium)
                OutlinedButton(onClick = { if (dayCount > 1) resize(dayCount - 1) }) { Text("-") }
                Text("$dayCount", fontWeight = FontWeight.Bold, fontSize = 18.sp, modifier = Modifier.width(36.dp), textAlign = TextAlign.Center)
                OutlinedButton(onClick = { if (dayCount < 31) resize(dayCount + 1) }) { Text("+") }
            }
            Spacer(Modifier.height(8.dp))
            Text("Pulsa un dia y pintalo con el turno de la barra inferior.", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))

            Column(modifier = Modifier.weight(1f).verticalScroll(rememberScrollState())) {
                val cols = 7
                val rows = (dayCount + cols - 1) / cols
                for (r in 0 until rows) {
                    Row(modifier = Modifier.fillMaxWidth()) {
                        for (c in 0 until cols) {
                            val index = r * cols + c
                            if (index < dayCount) {
                                val shiftId = cells.getOrNull(index)
                                val shift = shiftId?.let { id -> shifts.find { it.id == id } }
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                        .padding(2.dp)
                                        .border(1.dp, Color(0xFFBDBDBD), RoundedCornerShape(6.dp))
                                        .background(shift?.toComposeColor() ?: Color.White, RoundedCornerShape(6.dp))
                                        .clickable {
                                            val next = cells.toMutableList()
                                            next[index] = if (isEraser) null else selectedShiftId
                                            cells = next
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Text("${index + 1}", fontSize = 11.sp, color = if (shift != null) shift.toTextComposeColor().copy(alpha = 0.7f) else Color.Gray)
                                        if (shift != null) {
                                            Text(shift.abbreviation, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = shift.toTextComposeColor())
                                        }
                                    }
                                }
                            } else {
                                Spacer(Modifier.weight(1f).aspectRatio(1f).padding(2.dp))
                            }
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth().background(Color(0xFFECEFF1)).horizontalScroll(rememberScrollState()).padding(horizontal = 8.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(if (isEraser) Color(0xFFD32F2F) else Color(0xFFB0BEC5))
                        .border(if (isEraser) 2.dp else 0.dp, Color.Black, RoundedCornerShape(20.dp))
                        .clickable { isEraser = true; selectedShiftId = null }
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                ) { Text("Borrar", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) }
                shifts.forEach { shift ->
                    val selected = !isEraser && shift.id == selectedShiftId
                    Box(
                        modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(shift.toComposeColor())
                            .border(if (selected) 2.dp else 0.dp, Color.Black, RoundedCornerShape(20.dp))
                            .clickable { isEraser = false; selectedShiftId = shift.id }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("${shift.abbreviation} · ${shift.name}", color = shift.toTextComposeColor(), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { pendingAction = "save"; showNameDialog = true }, modifier = Modifier.weight(1f)) { Text("Guardar") }
                Button(
                    onClick = {
                        pendingAction = "apply"
                        if (initial?.name != null && nameText.isNotBlank()) onApply(buildPattern(nameText))
                        else showNameDialog = true
                    },
                    modifier = Modifier.weight(1f)
                ) { Text("Aplicar patron") }
            }
            Spacer(Modifier.height(20.dp))
        }
    }

    if (showNameDialog) {
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Nombre del patron") },
            text = {
                OutlinedTextField(value = nameText, onValueChange = { nameText = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                TextButton(onClick = {
                    val p = buildPattern(nameText)
                    showNameDialog = false
                    if (pendingAction == "apply") onApply(p) else onSave(p)
                }) { Text(if (pendingAction == "apply") "Guardar y aplicar" else "Guardar") }
            },
            dismissButton = { TextButton(onClick = { showNameDialog = false }) { Text("Cancelar") } }
        )
    }
}
