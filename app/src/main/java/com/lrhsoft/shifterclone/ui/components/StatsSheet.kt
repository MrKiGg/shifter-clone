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
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
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
import com.lrhsoft.shifterclone.data.HourlyComputeCalculator
import com.lrhsoft.shifterclone.data.LaborRightsCalculator
import com.lrhsoft.shifterclone.model.DayAssignment
import com.lrhsoft.shifterclone.model.PayrollEstimate
import com.lrhsoft.shifterclone.model.Shift
import com.lrhsoft.shifterclone.model.StatsSummary
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

enum class StatsPeriod { MONTH, YEAR, RIGHTS, COMPUTE }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsSheet(
    summary: StatsSummary,
    period: StatsPeriod,
    yearMonth: YearMonth,
    payroll: PayrollEstimate? = null,
    laborRights: LaborRightsCalculator.RightsSummary? = null,
    assignments: Map<String, DayAssignment> = emptyMap(),
    shifts: List<Shift> = emptyList(),
    selectedCenter: String? = null,
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
        StatsPeriod.RIGHTS -> "Derechos laborales"
        StatsPeriod.COMPUTE -> "Computo horario"
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
                FilterChip(
                    selected = period == StatsPeriod.RIGHTS,
                    onClick = { onPeriodChange(StatsPeriod.RIGHTS) },
                    label = { Text("Derechos") }
                )
                FilterChip(
                    selected = period == StatsPeriod.COMPUTE,
                    onClick = { onPeriodChange(StatsPeriod.COMPUTE) },
                    label = { Text("Computo") }
                )
            }

            when (period) {
                StatsPeriod.MONTH, StatsPeriod.YEAR -> MonthYearStatsBody(
                    summary = summary,
                    period = period,
                    title = title,
                    payroll = payroll,
                    onPrevious = onPrevious,
                    onNext = onNext
                )
                StatsPeriod.RIGHTS -> RightsStatsBody(laborRights)
                StatsPeriod.COMPUTE -> ComputeHourlySection(
                    yearMonth = yearMonth,
                    assignments = assignments,
                    shifts = shifts,
                    selectedCenter = selectedCenter
                )
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun MonthYearStatsBody(
    summary: StatsSummary,
    period: StatsPeriod,
    title: String,
    payroll: PayrollEstimate?,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
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

    if (payroll != null && period == StatsPeriod.MONTH) {
        Spacer(modifier = Modifier.height(20.dp))
        HorizontalDivider()
        Spacer(modifier = Modifier.height(12.dp))
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
}

@Composable
private fun RightsStatsBody(laborRights: LaborRightsCalculator.RightsSummary?) {
    Spacer(Modifier.height(12.dp))
    Text(
        "Osakidetza · vacaciones, AP y balance de jornada",
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.75f)
    )
    Spacer(Modifier.height(12.dp))
    HorizontalDivider()
    Spacer(Modifier.height(12.dp))

    if (laborRights == null || laborRights.contractDays <= 0) {
        Text(
            text = "No hay contratos. Abre Contratos (icono maletin arriba) y anade el periodo de alta para ver vacaciones, AP y balance de jornada.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            lineHeight = 18.sp
        )
        return
    }

    Text(
        text = laborRights.vacationLiquidationNote,
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    )
    Spacer(modifier = Modifier.height(10.dp))

    Text("Vacaciones", fontWeight = FontWeight.Medium, fontSize = 14.sp)
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "Generados",
            value = "${laborRights.vacationDaysGenerated} d",
            modifier = Modifier.weight(1f),
            accent = Color(0xFF00838F)
        )
        StatCard(
            label = if (laborRights.vacationCanEnjoy) "Disfrute" else "Liquidacion",
            value = if (laborRights.vacationCanEnjoy) "Si (>90d)" else "Finiquito",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Dias alta",
            value = "${laborRights.contractDays}",
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(14.dp))
    Text("Asuntos Particulares (AP)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
    Text(
        text = "6 dias = 45 h/ano · ~7,5 h/2 meses · si hay ≥1 dia sin contrato se reinicia a 0",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "Generadas",
            value = "${laborRights.apHoursGenerated} h",
            modifier = Modifier.weight(1f),
            accent = Color(0xFF5E35B1)
        )
        StatCard(
            label = "Equiv. dias",
            value = "${laborRights.apDaysEquivalent} d",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Cadena actual",
            value = "${laborRights.currentChainDays} d",
            modifier = Modifier.weight(1f)
        )
    }

    Spacer(modifier = Modifier.height(14.dp))
    Text("Balance de jornada (1.592 h/ano)", fontWeight = FontWeight.Medium, fontSize = 14.sp)
    Text(
        text = "Factor noche x${laborRights.nightFactor} · teorica con vac ${laborRights.theoreticalHoursWithVac} h",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
    )
    Spacer(modifier = Modifier.height(6.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "Trabajadas",
            value = "${laborRights.workedHoursEstimated} h",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Balance",
            value = when {
                laborRights.balanceWithVac > 0.5 -> "+${laborRights.balanceWithVac} h"
                laborRights.balanceWithVac < -0.5 -> "${laborRights.balanceWithVac} h"
                else -> "0 h"
            },
            modifier = Modifier.weight(1f),
            accent = when {
                laborRights.balanceWithVac > 0.5 -> Color(0xFF2E7D32)
                laborRights.balanceWithVac < -0.5 -> Color(0xFFC62828)
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
    Spacer(modifier = Modifier.height(8.dp))
    laborRights.notes.forEach { note ->
        Text(
            text = "· $note",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
        )
    }
}

@Composable
private fun ComputeHourlySection(
    yearMonth: YearMonth,
    assignments: Map<String, DayAssignment>,
    shifts: List<Shift>,
    selectedCenter: String?
) {
    var year by remember(yearMonth) { mutableStateOf(yearMonth.year) }
    var solape by remember { mutableStateOf(false) }
    var ageGroup by remember { mutableStateOf(HourlyComputeCalculator.AgeGroup.UNDER_55) }
    var pasteOpen by remember { mutableStateOf(false) }
    var pasteText by remember { mutableStateOf("") }
    var usePaste by remember { mutableStateOf(false) }

    val config = HourlyComputeCalculator.Config(
        year = year,
        solape = solape,
        ageGroup = ageGroup,
        center = selectedCenter
    )
    val result = remember(year, solape, ageGroup, selectedCenter, assignments, shifts, usePaste, pasteText) {
        if (usePaste && pasteText.isNotBlank()) {
            val cart = HourlyComputeCalculator.parseCarteleraText(year, pasteText)
            HourlyComputeCalculator.compute(config, cart, "Cartelera pegada")
        } else {
            val cart = HourlyComputeCalculator.carteleraFromCalendar(year, assignments, shifts)
            HourlyComputeCalculator.compute(
                config, cart, "Calendario ${selectedCenter ?: ""}".trim()
            )
        }
    }

    Spacer(Modifier.height(12.dp))
    Text(
        "Computo horario Osakidetza (1.592 h)",
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp
    )
    Text(
        "Por defecto lee los turnos del calendario del centro activo (abreviatura del turno). Opcional: pegar cartelera anual.",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
    )
    Spacer(Modifier.height(12.dp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text("Ano $year", fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
        Row {
            IconButton(onClick = { year -= 1; usePaste = false }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Ano anterior")
            }
            IconButton(onClick = { year += 1; usePaste = false }) {
                Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Ano siguiente")
            }
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text("Solape (+0,17 h)", modifier = Modifier.weight(1f), fontSize = 14.sp)
        Switch(checked = solape, onCheckedChange = { solape = it })
    }
    Text("Grupo de edad", fontWeight = FontWeight.Medium, fontSize = 13.sp)
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(vertical = 6.dp)) {
        listOf(
            HourlyComputeCalculator.AgeGroup.UNDER_55 to "<55",
            HourlyComputeCalculator.AgeGroup.AGE_55_60 to "55-60",
            HourlyComputeCalculator.AgeGroup.OVER_60 to ">60"
        ).forEach { (g, label) ->
            FilterChip(
                selected = ageGroup == g,
                onClick = { ageGroup = g },
                label = { Text(label, fontSize = 12.sp) }
            )
        }
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TextButton(onClick = { usePaste = false; pasteOpen = false }) {
            Text("Usar calendario")
        }
        TextButton(onClick = { pasteOpen = !pasteOpen }) {
            Text(if (pasteOpen) "Cerrar pegado" else "Pegar cartelera")
        }
    }

    if (pasteOpen) {
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = pasteText,
            onValueChange = { pasteText = it },
            modifier = Modifier.fillMaxWidth().height(140.dp),
            label = { Text("Cartelera (filas: Enero  D BT D …)") },
            placeholder = { Text("Enero\tD\tBT\tD\tM\t…") }
        )
        TextButton(onClick = { usePaste = true }) { Text("Calcular cartelera pegada") }
        Text(
            "Formato: una fila por mes, primera columna el nombre del mes y luego los codigos de cada dia.",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
    }

    Spacer(Modifier.height(12.dp))
    HorizontalDivider()
    Spacer(Modifier.height(12.dp))

    if (result.daysCounted == 0) {
        Text(
            "No hay turnos en el calendario de este ano (ni cartelera pegada).\nPinta turnos en el calendario o pega la cartelera anual tipo Excel.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.65f),
            lineHeight = 18.sp
        )
        return
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "Horas reales",
            value = String.format(Locale.US, "%.2f", result.workedHours),
            modifier = Modifier.weight(1f),
            accent = Color(0xFF1565C0)
        )
        StatCard(
            label = "Obligatorias",
            value = String.format(Locale.US, "%.0f", result.obligatoryHours),
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Diferencia",
            value = String.format(Locale.US, "%+.2f", result.difference),
            modifier = Modifier.weight(1f),
            accent = when {
                result.difference > 0.5 -> Color(0xFF2E7D32)
                result.difference < -0.5 -> Color(0xFFC62828)
                else -> MaterialTheme.colorScheme.onSurface
            }
        )
    }
    Spacer(Modifier.height(8.dp))
    StatCard(
        label = "Situacion",
        value = result.situation,
        modifier = Modifier.fillMaxWidth(),
        accent = when {
            result.difference > 0.5 -> Color(0xFF2E7D32)
            result.difference < -0.5 -> Color(0xFFC62828)
            else -> MaterialTheme.colorScheme.onSurface
        }
    )
    Spacer(Modifier.height(8.dp))
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        StatCard(
            label = "Vac. restantes",
            value = String.format(Locale.US, "%.1f d", result.vacationRemaining),
            modifier = Modifier.weight(1f),
            accent = Color(0xFF00838F)
        )
        StatCard(
            label = "Vac. usadas",
            value = "${result.vacationUsed}",
            modifier = Modifier.weight(1f)
        )
        StatCard(
            label = "Horas AP",
            value = String.format(Locale.US, "%.0f", result.apHours),
            modifier = Modifier.weight(1f),
            accent = Color(0xFF5E35B1)
        )
    }

    Spacer(Modifier.height(16.dp))
    Text("Desglose por codigo", fontWeight = FontWeight.SemiBold)
    Spacer(Modifier.height(6.dp))
    result.byCode.filter { it.count > 0 }.forEach { row ->
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                row.code,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(48.dp),
                color = Color(0xFF1565C0)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(row.description, fontSize = 13.sp)
                Text(
                    "${row.count} × ${row.unitHours} h",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
                )
            }
            Text(
                String.format(Locale.US, "%.2f h", row.totalHours),
                fontWeight = FontWeight.SemiBold
            )
        }
        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f))
    }

    Spacer(Modifier.height(10.dp))
    Text(
        "Fuente: ${result.sourceLabel} · ${result.daysCounted} dias · Ano ${result.year}",
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.55f)
    )
    result.notes.forEach { note ->
        Text(
            "· $note",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
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
