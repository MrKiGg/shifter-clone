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
import com.lrhsoft.shifterclone.data.LaborRightsCalculator
import com.lrhsoft.shifterclone.model.PayrollEstimate
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
    payroll: PayrollEstimate? = null,
    laborRights: LaborRightsCalculator.RightsSummary? = null,
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
        StatsPeriod.YEAR -> "Ano ${yearMonth.year}"
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Text("Estadisticas", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = period == StatsPeriod.MONTH,
                    onClick = { onPeriodChange(StatsPeriod.MONTH) },
                    label = { Text("Mes") }
                )
                FilterChip(
                    selected = period == StatsPeriod.YEAR,
                    onClick = { onPeriodChange(StatsPeriod.YEAR) },
                    label = { Text("Ano") }
                )
            }

            Spacer(Modifier.height(8.dp))
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

            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(label = "Dias trabajo", value = "${summary.workDays}", modifier = Modifier.weight(1f))
                StatCard(label = "Dias libre", value = "${summary.restDays}", modifier = Modifier.weight(1f))
                StatCard(
                    label = "Horas",
                    value = String.format(Locale.US, "%.1f", summary.totalHours),
                    modifier = Modifier.weight(1f)
                )
            }
            if (summary.totalEarnings > 0) {
                Spacer(Modifier.height(8.dp))
                StatCard(
                    label = "Ingresos (tarifa turnos)",
                    value = String.format(Locale.US, "%.2f EUR", summary.totalEarnings),
                    modifier = Modifier.fillMaxWidth(),
                    accent = Color(0xFF2E7D32)
                )
            }

            Spacer(Modifier.height(16.dp))
            Text("Por turno", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(8.dp))

            if (summary.byShift.isNotEmpty()) {
                summary.byShift.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
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
                            } else "-"
                            Text(
                                text = "${item.count} dias · $hoursText",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        if (item.totalEarnings > 0) {
                            Text(
                                text = String.format(Locale.US, "%.2f EUR", item.totalEarnings),
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
                    text = "No hay turnos en este periodo.\nPinta algunos dias y vuelve aqui.",
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    lineHeight = 20.sp
                )
            }

            Spacer(Modifier.height(12.dp))
            Text(
                text = "Las horas se calculan con el horario de cada turno. Configura la tarifa EUR/h al editar un turno.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
            )

            // ── Derechos laborales Osakidetza (siempre visible) ──
            Spacer(modifier = Modifier.height(20.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                text = "Derechos laborales (Osakidetza)",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Spacer(Modifier.height(4.dp))

            val rights = laborRights
            if (rights == null || rights.contractDays <= 0) {
                Text(
                    text = "No hay contratos. Abre Contratos (icono maletin arriba) y anade el periodo de alta para ver vacaciones, AP y balance de jornada.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
                    lineHeight = 18.sp
                )
            } else {
                Text(
                    text = rights.vacationLiquidationNote,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
                Spacer(Modifier.height(10.dp))

                Text("Vacaciones", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        label = "Generados",
                        value = "${rights.vacationDaysGenerated} d",
                        modifier = Modifier.weight(1f),
                        accent = Color(0xFF00838F)
                    )
                    StatCard(
                        label = "Dias alta",
                        value = "${rights.contractDays}",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = if (rights.vacationCanEnjoy) "Disfrute" else "Liquidacion",
                        value = if (rights.vacationCanEnjoy) "Si (>90d)" else "Finiquito",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(14.dp))
                Text("Asuntos Particulares (AP)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "6 dias = 45 h/ano · ~7,5 h/2 meses · si hay ≥1 dia sin contrato se reinicia a 0",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        label = "Generadas",
                        value = "${rights.apHoursGenerated} h",
                        modifier = Modifier.weight(1f),
                        accent = Color(0xFF5E35B1)
                    )
                    StatCard(
                        label = "Equiv. dias",
                        value = "${rights.apDaysEquivalent} d",
                        modifier = Modifier.weight(1f),
                        accent = Color(0xFF4527A0)
                    )
                    StatCard(
                        label = "Cadena actual",
                        value = "${rights.currentChainDays} d",
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(14.dp))
                Text("Balance de jornada (1.592 h/ano)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = "Factor noche ×${rights.nightFactor} · teorica con vac ${rights.theoreticalHoursWithVac} h",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
                Spacer(Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        label = "Trabajadas",
                        value = "${rights.workedHoursEstimated} h",
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Balance",
                        value = when {
                            rights.balanceWithVac > 0.5 -> "+${rights.balanceWithVac} h"
                            rights.balanceWithVac < -0.5 -> "${rights.balanceWithVac} h"
                            else -> "0 h"
                        },
                        modifier = Modifier.weight(1f),
                        accent = when {
                            rights.balanceWithVac > 0.5 -> Color(0xFF2E7D32)
                            rights.balanceWithVac < -0.5 -> Color(0xFFC62828)
                            else -> MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                Spacer(Modifier.height(8.dp))
                rights.notes.forEach { note ->
                    Text(
                        text = "· $note",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f),
                        lineHeight = 16.sp
                    )
                }
            }

            if (payroll != null && period == StatsPeriod.MONTH) {
                Spacer(Modifier.height(20.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "Estimacion nomina (bruto orientativo)",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    text = "Fijos prorrateados por dias trabajados (${payroll.workDays} / dias del mes). Variables segun turnos pintados.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    StatCard(
                        label = "Fijo prorrateado",
                        value = String.format(Locale.US, "%.2f EUR", payroll.fixedMonthly),
                        modifier = Modifier.weight(1f)
                    )
                    StatCard(
                        label = "Variables",
                        value = String.format(Locale.US, "%.2f EUR", payroll.variableFromShifts),
                        modifier = Modifier.weight(1f),
                        accent = Color(0xFF1565C0)
                    )
                }
                Spacer(Modifier.height(8.dp))
                StatCard(
                    label = "Total estimado",
                    value = String.format(Locale.US, "%.2f EUR", payroll.totalEstimated),
                    modifier = Modifier.fillMaxWidth(),
                    accent = Color(0xFF2E7D32)
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Noches: ${payroll.nights} · Fest. dia: ${payroll.festivoDays} · Fest. noche: ${payroll.festivoNights}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(8.dp))
                Text("Desglose", fontWeight = FontWeight.SemiBold)
                payroll.breakdown.forEach { (label, amount) ->
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp)) {
                        Text(label, modifier = Modifier.weight(1f), fontSize = 12.sp)
                        Text(String.format(Locale.US, "%.2f EUR", amount), fontSize = 12.sp, fontWeight = FontWeight.Medium)
                    }
                }
                Text(
                    text = "No incluye IRPF, Seguridad Social ni actualizaciones posteriores a 2019.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.45f),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }
            Spacer(Modifier.height(24.dp))
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
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = accent)
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}
