package com.lrhsoft.shifterclone.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lrhsoft.shifterclone.model.StatsSummary
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

enum class StatsPeriod { MONTH, YEAR }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsSheet(
    summary: StatsSummary,
    period: StatsPeriod,
    yearMonth: YearMonth,
    onPeriodChange: (StatsPeriod) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val title = when (period) {
        StatsPeriod.MONTH -> {
            val name = yearMonth.month
                .getDisplayName(TextStyle.FULL, Locale("es", "ES"))
                .replaceFirstChar { it.uppercase() }
            "$name ${yearMonth.year}"
        }
        StatsPeriod.YEAR -> "${yearMonth.year}"
    }

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
                text = "Estadísticas",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = period == StatsPeriod.MONTH,
                    onClick = { onPeriodChange(StatsPeriod.MONTH) },
                    label = { Text("Mes") }
                )
                FilterChip(
                    selected = period == StatsPeriod.YEAR,
                    onClick = { onPeriodChange(StatsPeriod.YEAR) },
                    label = { Text("Año") }
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onPrevious) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Anterior")
                }
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                IconButton(onClick = onNext) {
                    Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Siguiente")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Horas",
                    value = summary.hoursFormatted,
                    modifier = Modifier.weight(1f),
                    accent = MaterialTheme.colorScheme.primary
                )
                StatCard(
                    label = "Ingresos",
                    value = if (summary.totalEarnings > 0) {
                        String.format(Locale.US, "%.2f €", summary.totalEarnings)
                    } else "—",
                    modifier = Modifier.weight(1f),
                    accent = Color(0xFF2E7D32)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    label = "Días trabajo",
                    value = summary.workDays.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Libres",
                    value = summary.restDays.toString(),
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Sin marcar",
                    value = summary.emptyDays.toString(),
                    modifier = Modifier.weight(1f)
                )
            }

            if (summary.byShift.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "Desglose por turno",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
                Spacer(modifier = Modifier.height(10.dp))

                summary.byShift.forEach { item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(28.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(item.shift.color)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = item.shift.abbreviation,
                                color = Color(item.shift.textColor),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(item.shift.name, fontWeight = FontWeight.Medium)
                            val hoursText = if (item.totalHours > 0) {
                                val h = item.totalHours.toInt()
                                val m = ((item.totalHours - h) * 60).toInt()
                                if (m == 0) "${h}h" else "${h}h ${m}m"
                            } else "—"
                            Text(
                                text = "${item.count} días · $hoursText",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        if (item.totalEarnings > 0) {
                            Text(
                                text = String.format(Locale.US, "%.2f €", item.totalEarnings),
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
                }
            } else {
                Spacer(modifier = Modifier.height(24.dp))
                Text(
                    text = "No hay turnos en este periodo.\nPinta algunos días y vuelve aquí.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 20.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Las horas se calculan con el horario de cada turno. Configura la tarifa €/h al editar un turno.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    accent: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = accent
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
