package com.lrhsoft.shifterclone.ui.components

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lrhsoft.shifterclone.model.DayNote
import com.lrhsoft.shifterclone.model.Shift
import java.time.LocalDate
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayDetailSheet(
    date: LocalDate,
    shifts: List<Shift>,
    note: DayNote?,
    onDismiss: () -> Unit,
    onSave: (DayNote) -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var text by remember { mutableStateOf(note?.text ?: "") }
    var alarmEnabled by remember { mutableStateOf(note?.hasAlarm == true) }
    var alarmHour by remember { mutableStateOf(note?.alarmHour?.toString() ?: "7") }
    var alarmMinute by remember { mutableStateOf(note?.alarmMinute?.toString()?.padStart(2, '0') ?: "00") }
    var dayBefore by remember { mutableStateOf(note?.alarmDayBefore ?: false) }

    val dayName = date.dayOfWeek
        .getDisplayName(TextStyle.FULL, Locale("es", "ES"))
        .replaceFirstChar { it.uppercase() }
    val monthName = date.month
        .getDisplayName(TextStyle.FULL, Locale("es", "ES"))

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = "$dayName, ${date.dayOfMonth} de $monthName",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )

            if (shifts.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Turnos: " + shifts.joinToString(", ") { "${it.abbreviation} (${it.name})" },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Text("Nota", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp),
                placeholder = { Text("Escribe una nota o recordatorio…") },
                maxLines = 6
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    Icons.Default.Alarm,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Alarma", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                Spacer(modifier = Modifier.weight(1f))
                Switch(
                    checked = alarmEnabled,
                    onCheckedChange = { alarmEnabled = it }
                )
            }

            if (alarmEnabled) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = alarmHour,
                        onValueChange = {
                            alarmHour = it.filter { c -> c.isDigit() }.take(2)
                        },
                        label = { Text("Hora") },
                        singleLine = true,
                        modifier = Modifier.width(80.dp)
                    )
                    Text(":", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    OutlinedTextField(
                        value = alarmMinute,
                        onValueChange = {
                            alarmMinute = it.filter { c -> c.isDigit() }.take(2)
                        },
                        label = { Text("Min") },
                        singleLine = true,
                        modifier = Modifier.width(80.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = dayBefore,
                        onCheckedChange = { dayBefore = it }
                    )
                    Text("Sonar el día anterior")
                }
                Text(
                    text = if (dayBefore) {
                        "Útil para turnos de mañana: te avisa la noche de antes"
                    } else {
                        "La alarma sonará el mismo día a esa hora"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(start = 12.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        val h = alarmHour.toIntOrNull()?.coerceIn(0, 23)
                        val m = alarmMinute.toIntOrNull()?.coerceIn(0, 59)
                        val saved = DayNote(
                            year = date.year,
                            month = date.monthValue,
                            day = date.dayOfMonth,
                            text = text.trim(),
                            alarmHour = if (alarmEnabled) h else null,
                            alarmMinute = if (alarmEnabled) m else null,
                            alarmDayBefore = dayBefore,
                            enabled = alarmEnabled
                        )
                        onSave(saved)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Guardar")
                }

                if (note != null && !note.isEmpty) {
                    OutlinedButton(
                        onClick = onDelete,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Eliminar")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerrar")
            }
        }
    }
}
