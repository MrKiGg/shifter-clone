package com.lrhsoft.shifterclone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lrhsoft.shifterclone.model.Shift
import java.util.UUID

private val PRESET_COLORS = listOf(
    0xFF4CAF50, 0xFFFF9800, 0xFF3F51B5, 0xFF9E9E9E, 0xFF00BCD4,
    0xFFE91E63, 0xFF9C27B0, 0xFFF44336, 0xFF795548, 0xFF607D8B,
    0xFF8BC34A, 0xFFFFEB3B, 0xFF2196F3, 0xFFFF5722, 0xFF000000, 0xFFFFFFFF
)

@Composable
fun ShiftEditorDialog(
    shift: Shift?,
    onDismiss: () -> Unit,
    onSave: (Shift) -> Unit,
    onDelete: ((Shift) -> Unit)? = null
) {
    val isEdit = shift != null

    var name by remember { mutableStateOf(shift?.name ?: "") }
    var abbreviation by remember { mutableStateOf(shift?.abbreviation ?: "") }
    var color by remember { mutableLongStateOf(shift?.color ?: 0xFF4CAF50) }
    var textColor by remember { mutableLongStateOf(shift?.textColor ?: 0xFFFFFFFF) }
    var startHour by remember { mutableStateOf(shift?.startHour?.toString() ?: "") }
    var startMinute by remember { mutableStateOf(shift?.startMinute?.toString()?.padStart(2, '0') ?: "") }
    var endHour by remember { mutableStateOf(shift?.endHour?.toString() ?: "") }
    var endMinute by remember { mutableStateOf(shift?.endMinute?.toString()?.padStart(2, '0') ?: "") }
    var isRest by remember { mutableStateOf(shift?.isRest ?: false) }
    var hourlyRate by remember {
        mutableStateOf(shift?.hourlyRate?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }

    val isValid = name.isNotBlank() && abbreviation.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Editar turno" else "Nuevo turno",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(color)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (abbreviation.isNotBlank()) abbreviation else "?",
                        color = Color(textColor),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = abbreviation,
                    onValueChange = { if (it.length <= 3) abbreviation = it.uppercase() },
                    label = { Text("Abreviatura (máx 3)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Color de fondo", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                ColorPickerRow(
                    colors = PRESET_COLORS,
                    selected = color,
                    onSelect = { color = it }
                )

                Text("Color del texto", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                ColorPickerRow(
                    colors = listOf(0xFFFFFFFF, 0xFF000000, 0xFFFFEB3B, 0xFFE91E63),
                    selected = textColor,
                    onSelect = { textColor = it }
                )

                Text("Horario (opcional)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeField("H", startHour) { startHour = it.filter { c -> c.isDigit() }.take(2) }
                    Text(":")
                    TimeField("M", startMinute) { startMinute = it.filter { c -> c.isDigit() }.take(2) }
                    Text("→", modifier = Modifier.padding(horizontal = 4.dp))
                    TimeField("H", endHour) { endHour = it.filter { c -> c.isDigit() }.take(2) }
                    Text(":")
                    TimeField("M", endMinute) { endMinute = it.filter { c -> c.isDigit() }.take(2) }
                }

                OutlinedTextField(
                    value = hourlyRate,
                    onValueChange = {
                        hourlyRate = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                            .replace(',', '.')
                    },
                    label = { Text("Tarifa €/hora (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRest
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isRest,
                        onCheckedChange = { isRest = it }
                    )
                    Text("Es día de descanso / libre")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newShift = Shift(
                        id = shift?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        abbreviation = abbreviation.trim().uppercase(),
                        color = color,
                        textColor = textColor,
                        startHour = startHour.toIntOrNull()?.coerceIn(0, 23),
                        startMinute = startMinute.toIntOrNull()?.coerceIn(0, 59),
                        endHour = endHour.toIntOrNull()?.coerceIn(0, 23),
                        endMinute = endMinute.toIntOrNull()?.coerceIn(0, 59),
                        isRest = isRest,
                        hourlyRate = if (isRest) null else hourlyRate.toDoubleOrNull()
                    )
                    onSave(newShift)
                },
                enabled = isValid
            ) {
                Text(if (isEdit) "Guardar" else "Crear")
            }
        },
        dismissButton = {
            Row {
                if (isEdit && onDelete != null) {
                    TextButton(
                        onClick = { onDelete(shift!!) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )
}

@Composable
private fun TimeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.width(64.dp)
    )
}

@Composable
private fun ColorPickerRow(
    colors: List<Long>,
    selected: Long,
    onSelect: (Long) -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        colors.forEach { c ->
            val isSelected = c == selected
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(c))
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                        shape = CircleShape
                    )
                    .clickable { onSelect(c) }
            )
        }
    }
}
