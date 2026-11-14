package com.lrhsoft.shifterclone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

typealias PatternDay = String?

enum class PatternEndMode {
    END_OF_MONTH,
    CYCLES,
    MONTHS,
    UNTIL_DATE
}

@Composable
fun PatternBuilderDialog(
    shifts: List<Shift>,
    initial: List<PatternDay> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (List<PatternDay>) -> Unit
) {
    var sequence by remember { mutableStateOf(initial.ifEmpty { listOf(null as PatternDay) }) }
    var selectedShiftId by remember { mutableStateOf<String?>(shifts.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Constructor de patron", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("Selecciona turno y anade dias a la secuencia.", fontSize = 13.sp, color = Color.Gray)
                Row(
                    modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                            .background(if (selectedShiftId == null) Color(0xFFBDBDBD) else Color(0xFFE0E0E0))
                            .border(if (selectedShiftId == null) 2.dp else 0.dp, Color.Black, RoundedCornerShape(8.dp))
                            .clickable { selectedShiftId = null },
                        contentAlignment = Alignment.Center
                    ) { Text("Libre", fontSize = 10.sp) }
                    shifts.forEach { shift ->
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                .background(shift.toComposeColor())
                                .border(if (selectedShiftId == shift.id) 2.dp else 0.dp, Color.Black, RoundedCornerShape(8.dp))
                                .clickable { selectedShiftId = shift.id },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(shift.abbreviation, color = shift.toTextComposeColor(), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    Button(onClick = { sequence = sequence + selectedShiftId }) {
                        Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Anadir dia")
                    }
                    if (sequence.isNotEmpty()) {
                        TextButton(onClick = { sequence = sequence.dropLast(1) }) { Text("Quitar ultimo") }
                    }
                }
                Text("Secuencia (${sequence.size} dias)", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    sequence.forEachIndexed { index, day ->
                        val shift = day?.let { id -> shifts.find { it.id == id } }
                        Box(
                            modifier = Modifier.size(40.dp).clip(RoundedCornerShape(8.dp))
                                .background(shift?.toComposeColor() ?: Color(0xFFE0E0E0))
                                .clickable {
                                    val m = sequence.toMutableList()
                                    m[index] = selectedShiftId
                                    sequence = m
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(shift?.abbreviation ?: "·", color = shift?.toTextComposeColor() ?: Color.Gray, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { if (sequence.isNotEmpty()) onConfirm(sequence) }, enabled = sequence.isNotEmpty()) {
                Text("Elegir fecha de inicio")
            }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyPatternDialog(
    startDate: LocalDate,
    patternLength: Int,
    onDismiss: () -> Unit,
    onApply: (PatternEndMode, Int, LocalDate?) -> Unit
) {
    var mode by remember { mutableStateOf(PatternEndMode.END_OF_MONTH) }
    var cycles by remember { mutableStateOf("4") }
    var months by remember { mutableStateOf("1") }
    var untilDate by remember { mutableStateOf<LocalDate?>(null) }
    var showDatePicker by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aplicar patron", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Inicio: ${startDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("es", "ES")))}",
                    fontWeight = FontWeight.Medium
                )
                Text("Longitud del patron: $patternLength dias", fontSize = 13.sp, color = Color.Gray)

                ModeRow(selected = mode == PatternEndMode.END_OF_MONTH, label = "Hasta fin de mes") {
                    mode = PatternEndMode.END_OF_MONTH
                }
                ModeRow(selected = mode == PatternEndMode.CYCLES, label = "Numero de ciclos") {
                    mode = PatternEndMode.CYCLES
                }
                if (mode == PatternEndMode.CYCLES) {
                    OutlinedTextField(
                        value = cycles,
                        onValueChange = { cycles = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Ciclos") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                ModeRow(selected = mode == PatternEndMode.MONTHS, label = "Numero de meses") {
                    mode = PatternEndMode.MONTHS
                }
                if (mode == PatternEndMode.MONTHS) {
                    OutlinedTextField(
                        value = months,
                        onValueChange = { months = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Meses") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                ModeRow(selected = mode == PatternEndMode.UNTIL_DATE, label = "Hasta una fecha") {
                    mode = PatternEndMode.UNTIL_DATE
                }
                if (mode == PatternEndMode.UNTIL_DATE) {
                    OutlinedButton(onClick = { showDatePicker = true }, modifier = Modifier.fillMaxWidth()) {
                        Text(
                            untilDate?.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("es", "ES")))
                                ?: "Elegir fecha final"
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = {
                val cyclesInt = cycles.toIntOrNull() ?: 1
                val monthsInt = months.toIntOrNull() ?: 1
                if (mode == PatternEndMode.UNTIL_DATE && untilDate == null) return@Button
                val count = when (mode) {
                    PatternEndMode.CYCLES -> cyclesInt
                    PatternEndMode.MONTHS -> monthsInt
                    else -> 1
                }
                onApply(mode, count, untilDate)
            }) { Text("Aplicar") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Cancelar") } }
    )

    if (showDatePicker) {
        val initial = untilDate ?: startDate.plusMonths(1)
        val millis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val state = rememberDatePickerState(initialSelectedDateMillis = millis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val selected = state.selectedDateMillis ?: return@TextButton
                    untilDate = Instant.ofEpochMilli(selected).atZone(ZoneOffset.UTC).toLocalDate()
                    showDatePicker = false
                }) { Text("OK") }
            },
            dismissButton = { TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") } }
        ) { DatePicker(state = state) }
    }
}

@Composable
private fun ModeRow(selected: Boolean, label: String, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 4.dp)
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Text(label, fontSize = 14.sp)
    }
}

@Composable
fun PatternPickStartBanner(
    patternLength: Int,
    onCancel: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFF00897B)).padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Patron de $patternLength dia(s). Toca el dia de inicio en el calendario.",
            color = Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onCancel) {
            Icon(Icons.Default.Close, contentDescription = "Cancelar", tint = Color.White)
        }
    }
}
