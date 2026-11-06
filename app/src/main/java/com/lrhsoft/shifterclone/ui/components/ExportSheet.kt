package com.lrhsoft.shifterclone.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lrhsoft.shifterclone.export.DeviceCalendar
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

enum class ExportRange { CURRENT_MONTH, CURRENT_YEAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExportSheet(
    calendars: List<DeviceCalendar>,
    selectedCalendarId: Long?,
    range: ExportRange,
    yearMonth: YearMonth,
    includeRest: Boolean,
    isLoading: Boolean,
    statusMessage: String?,
    hasCalendarPermission: Boolean,
    onSelectCalendar: (Long) -> Unit,
    onRangeChange: (ExportRange) -> Unit,
    onIncludeRestChange: (Boolean) -> Unit,
    onRequestPermission: () -> Unit,
    onExportToCalendar: () -> Unit,
    onShareIcs: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val monthLabel = yearMonth.month
        .getDisplayName(TextStyle.FULL, Locale("es", "ES"))
        .replaceFirstChar { it.uppercase() } + " ${yearMonth.year}"

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
                text = "Exportar turnos",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Periodo del calendario actual: $monthLabel",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text("Rango", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = range == ExportRange.CURRENT_MONTH,
                    onClick = { onRangeChange(ExportRange.CURRENT_MONTH) },
                    label = { Text("Este mes") }
                )
                FilterChip(
                    selected = range == ExportRange.CURRENT_YEAR,
                    onClick = { onRangeChange(ExportRange.CURRENT_YEAR) },
                    label = { Text("Este año") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Checkbox(
                    checked = includeRest,
                    onCheckedChange = onIncludeRestChange
                )
                Text("Incluir días libres / vacaciones")
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text("Calendario del dispositivo", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Incluye Google Calendar si está sincronizado en el móvil",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (!hasCalendarPermission) {
                Text(
                    text = "Se necesita permiso para leer y escribir en el calendario.",
                    style = MaterialTheme.typography.bodySmall
                )
                Spacer(modifier = Modifier.height(8.dp))
                Button(onClick = onRequestPermission) {
                    Text("Conceder permiso")
                }
            } else if (calendars.isEmpty()) {
                Text(
                    text = "No se encontraron calendarios editables en el dispositivo.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            } else {
                calendars.forEach { cal ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectCalendar(cal.id) }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = selectedCalendarId == cal.id,
                            onClick = { onSelectCalendar(cal.id) }
                        )
                        Column {
                            Text(cal.name, fontWeight = FontWeight.Medium)
                            if (cal.accountName.isNotBlank()) {
                                Text(
                                    text = cal.accountName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onExportToCalendar,
                    enabled = selectedCalendarId != null && !isLoading,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.height(20.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text("Exportar a calendario")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            Text("Archivo .ics (compartir)", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Puedes abrirlo con Google Calendar, Outlook, etc. No requiere permisos.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedButton(
                onClick = onShareIcs,
                enabled = !isLoading,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Compartir archivo .ics")
            }

            if (!statusMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = statusMessage,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
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
