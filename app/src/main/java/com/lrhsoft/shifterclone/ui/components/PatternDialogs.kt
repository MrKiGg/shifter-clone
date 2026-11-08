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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lrhsoft.shifterclone.model.Shift
import java.time.LocalDate
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
    var selectedShiftId by remember { mutableStateOf(shifts.firstOrNull()?.id) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Crear patr\u00f3n", fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    "Define la secuencia de turnos. Se repetir\u00e1 a partir de la fecha que elijas.",
                    fontSize = 13.sp,
                    color = Color.Gray
                )

                Text("Turno a a\u00f1adir", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val emptySelected = selectedShiftId == null
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(16.dp))
                            .background(if (emptySelected) Color(0xFFB0BEC5) else Color(0xFFECEFF1))
                            .border(
                                width = if (emptySelected) 2.dp else 0.dp,
                                color = Color.Black,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable { selectedShiftId = null }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text("Vac\u00edo", fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                    shifts.forEach { shift ->
                        val sel = selectedShiftId == shift.id
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(16.dp))
                                .background(shift.toComposeColor())
                                .border(
                                    width = if (sel) 2.5.dp else 0.dp,
                                    color = Color.Black,
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .clickable { selectedShiftId = shift.id }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = shift.abbreviation,
                                color = shift.toTextComposeColor(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(onClick = {
                        sequence = sequence + selectedShiftId
                    }) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("A\u00f1adir d\u00eda")
                    }
                    if (sequence.isNotEmpty()) {
                        TextButton(onClick = {
                            sequence = sequence.dropLast(1)
                        }) {
                            Text("Quitar \u00faltimo")
                        }
                    }
                }

                Text(
                    "Secuencia (${sequence.size} d\u00eda${if (sequence.size == 1) "" else "s"})",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
                if (sequence.isEmpty()) {
                    Text("A\u00fan no hay d\u00edas en el patr\u00f3n.", color = Color.Gray, fontSize = 13.sp)
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        sequence.forEachIndexed { index, day ->
                            val shift = day?.let { id -> shifts.find { it.id == id } }
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(shift?.toComposeColor() ?: Color(0xFFE0E0E0))
                                    .clickable {
                                        sequence = sequence.toMutableList().also { it.removeAt(index) }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = shift?.abbreviation ?: "\u00b7",
                                    color = shift?.toTextComposeColor() ?: Color.Gray,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp
                                )
                            }
                            if (index < sequence.lastIndex) {
                                Text("\u2192", color = Color.Gray, fontSize = 12.sp)
                            }
                        }
                    }
                    Text(
                        "Toca un d\u00eda de la secuencia para eliminarlo",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(sequence) },
                enabled = sequence.isNotEmpty()
            ) {
                Text("Elegir fecha de inicio")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

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
    var untilText by remember { mutableStateOf("") }
    val fmt = DateTimeFormatter.ofPattern("d/M/yyyy")

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Aplicar patr\u00f3n", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Inicio: ${startDate.format(DateTimeFormatter.ofPattern("d MMMM yyyy", Locale("es", "ES")))}",
                    fontWeight = FontWeight.Medium
                )
                Text(
                    "Longitud del patr\u00f3n: $patternLength d\u00eda(s)",
                    fontSize = 13.sp,
                    color = Color.Gray
                )
                Spacer(Modifier.height(4.dp))
                Text("Repetir hasta:", fontWeight = FontWeight.SemiBold)

                EndOption(
                    selected = mode == PatternEndMode.END_OF_MONTH,
                    label = "Fin de este mes",
                    onClick = { mode = PatternEndMode.END_OF_MONTH }
                )
                EndOption(
                    selected = mode == PatternEndMode.CYCLES,
                    label = "N\u00famero de ciclos",
                    onClick = { mode = PatternEndMode.CYCLES }
                )
                if (mode == PatternEndMode.CYCLES) {
                    OutlinedTextField(
                        value = cycles,
                        onValueChange = { cycles = it.filter { c -> c.isDigit() }.take(3) },
                        label = { Text("Ciclos") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                EndOption(
                    selected = mode == PatternEndMode.MONTHS,
                    label = "N\u00famero de meses",
                    onClick = { mode = PatternEndMode.MONTHS }
                )
                if (mode == PatternEndMode.MONTHS) {
                    OutlinedTextField(
                        value = months,
                        onValueChange = { months = it.filter { c -> c.isDigit() }.take(2) },
                        label = { Text("Meses") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                EndOption(
                    selected = mode == PatternEndMode.UNTIL_DATE,
                    label = "Hasta una fecha (d/m/aaaa)",
                    onClick = { mode = PatternEndMode.UNTIL_DATE }
                )
                if (mode == PatternEndMode.UNTIL_DATE) {
                    OutlinedTextField(
                        value = untilText,
                        onValueChange = { untilText = it },
                        label = { Text("Fecha fin") },
                        placeholder = { Text("31/12/2026") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cyclesInt = cycles.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    val monthsInt = months.toIntOrNull()?.coerceAtLeast(1) ?: 1
                    val until = if (mode == PatternEndMode.UNTIL_DATE) {
                        try {
                            LocalDate.parse(untilText.trim(), fmt)
                        } catch (_: Exception) {
                            null
                        }
                    } else null
                    if (mode == PatternEndMode.UNTIL_DATE && until == null) return@Button
                    val param = when (mode) {
                        PatternEndMode.CYCLES -> cyclesInt
                        PatternEndMode.MONTHS -> monthsInt
                        else -> 0
                    }
                    onApply(mode, param, until)
                }
            ) {
                Text("Aplicar")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun EndOption(
    selected: Boolean,
    label: String,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 2.dp)
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
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF00897B))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Patr\u00f3n de $patternLength d\u00eda(s). Toca el d\u00eda de inicio en el calendario.",
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
