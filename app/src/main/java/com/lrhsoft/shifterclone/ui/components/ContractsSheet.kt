package com.lrhsoft.shifterclone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lrhsoft.shifterclone.model.WorkContract
import java.time.Instant
import java.time.LocalDate
import java.time.YearMonth
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.time.format.TextStyle
import java.util.Locale
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractsSheet(
    contracts: List<WorkContract>,
    /** Mes visible en el calendario → filtro y fechas por defecto del formulario */
    defaultYearMonth: YearMonth = YearMonth.now(),
    onSaveContracts: (List<WorkContract>) -> Unit,
    onOpenContract: (WorkContract) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var local by remember { mutableStateOf(contracts) }
    var editing by remember { mutableStateOf<WorkContract?>(null) }
    var showForm by remember { mutableStateOf(false) }
    var filterYear by remember(defaultYearMonth) { mutableIntStateOf(defaultYearMonth.year) }
    var filterMonth by remember(defaultYearMonth) { mutableIntStateOf(defaultYearMonth.monthValue) }
    var yearExpanded by remember { mutableStateOf(false) }
    var monthExpanded by remember { mutableStateOf(false) }

    val years = remember(local, defaultYearMonth) {
        val ys = mutableSetOf(defaultYearMonth.year, defaultYearMonth.year - 1, defaultYearMonth.year + 1)
        local.forEach { c ->
            try {
                ys.add(LocalDate.parse(c.startDate).year)
                c.endDate?.let { ys.add(LocalDate.parse(it).year) }
            } catch (_: Exception) {}
        }
        ys.sortedDescending()
    }

    val monthNames = remember {
        (1..12).map { m ->
            java.time.Month.of(m).getDisplayName(TextStyle.FULL, Locale("es", "ES")).replaceFirstChar { it.uppercase() }
        }
    }

    val filtered = remember(local, filterYear, filterMonth) {
        if (filterMonth == 0) {
            local.filter { c ->
                try {
                    val start = LocalDate.parse(c.startDate)
                    val end = c.endDate?.let { LocalDate.parse(it) }
                    start.year == filterYear || (end != null && end.year == filterYear) ||
                        (start.year < filterYear && (end == null || end.year >= filterYear))
                } catch (_: Exception) { false }
            }
        } else {
            val ym = YearMonth.of(filterYear, filterMonth)
            val monthStart = ym.atDay(1)
            val monthEnd = ym.atEndOfMonth()
            local.filter { c ->
                try {
                    val start = LocalDate.parse(c.startDate)
                    val end = c.endDate?.let { LocalDate.parse(it) }
                    !start.isAfter(monthEnd) && (end == null || !end.isBefore(monthStart))
                } catch (_: Exception) { false }
            }
        }
    }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Text("Contratos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Cerrar") }
            }
            Text(
                "Filtra por mes. Pulsa el nombre para verlo en el calendario, o Editar para cambiarlo.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                ExposedDropdownMenuBox(expanded = yearExpanded, onExpandedChange = { yearExpanded = it }, modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = filterYear.toString(), onValueChange = {}, readOnly = true, label = { Text("Ano") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(yearExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = yearExpanded, onDismissRequest = { yearExpanded = false }) {
                        years.forEach { y -> DropdownMenuItem(text = { Text(y.toString()) }, onClick = { filterYear = y; yearExpanded = false }) }
                    }
                }
                ExposedDropdownMenuBox(expanded = monthExpanded, onExpandedChange = { monthExpanded = it }, modifier = Modifier.weight(1.4f)) {
                    val monthLabel = if (filterMonth == 0) "Todos" else monthNames[filterMonth - 1]
                    OutlinedTextField(
                        value = monthLabel, onValueChange = {}, readOnly = true, label = { Text("Mes") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(monthExpanded) },
                        modifier = Modifier.fillMaxWidth().menuAnchor()
                    )
                    ExposedDropdownMenu(expanded = monthExpanded, onDismissRequest = { monthExpanded = false }) {
                        DropdownMenuItem(text = { Text("Todos") }, onClick = { filterMonth = 0; monthExpanded = false })
                        monthNames.forEachIndexed { idx, name ->
                            DropdownMenuItem(text = { Text(name) }, onClick = { filterMonth = idx + 1; monthExpanded = false })
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            Text(
                when {
                    filterMonth == 0 -> "${filtered.size} contrato(s) en $filterYear"
                    else -> "${filtered.size} contrato(s) en ${monthNames[filterMonth - 1]} $filterYear"
                },
                fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.height(8.dp))

            if (filtered.isEmpty() && !showForm) {
                Text("No hay contratos en este periodo.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(vertical = 12.dp))
            }

            if (!showForm) {
                filtered.forEach { c ->
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1565C0).copy(alpha = 0.12f))
                            .clickable {
                                onSaveContracts(local)
                                onOpenContract(c)
                            }
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Work,
                                contentDescription = null,
                                tint = Color(0xFF1565C0),
                                modifier = Modifier.size(22.dp)
                            )
                            Text(
                                c.name,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF1565C0),
                                fontSize = 15.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(start = 10.dp)
                            )
                        }
                        Text(
                            "${c.startDate} -> ${c.endDate ?: "abierto"}",
                            fontSize = 12.sp,
                            color = Color(0xFF37474F),
                            modifier = Modifier.padding(top = 4.dp, start = 32.dp)
                        )
                        if (c.center.isNotBlank()) {
                            Text(
                                c.center,
                                fontSize = 12.sp,
                                color = Color(0xFF37474F),
                                modifier = Modifier.padding(start = 32.dp)
                            )
                        }
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 4.dp, start = 20.dp)
                        ) {
                            TextButton(onClick = { editing = c; showForm = true }) { Text("Editar") }
                            TextButton(onClick = { local = local.filter { it.id != c.id } }) { Text("Borrar") }
                            TextButton(onClick = { onSaveContracts(local); onOpenContract(c) }) { Text("Ver en calendario") }
                        }
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            if (!showForm) {
                Button(onClick = { editing = null; showForm = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("+ Anadir contrato")
                }
            } else {
                Text(if (editing != null) "Editar contrato" else "Nuevo contrato", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Spacer(Modifier.height(6.dp))
                ContractForm(
                    initial = editing,
                    defaultYearMonth = defaultYearMonth,
                    onCancel = { showForm = false; editing = null },
                    onSave = { contract ->
                        local = if (editing != null) local.map { if (it.id == contract.id) contract else it } else local + contract
                        showForm = false
                        editing = null
                    }
                )
            }

            Spacer(Modifier.height(16.dp))
            if (!showForm) {
                Button(onClick = { onSaveContracts(local); onDismiss() }, modifier = Modifier.fillMaxWidth()) {
                    Text("Guardar contratos")
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContractForm(
    initial: WorkContract?,
    defaultYearMonth: YearMonth,
    onCancel: () -> Unit,
    onSave: (WorkContract) -> Unit
) {
    val defaultStart = defaultYearMonth.atDay(1).toString()
    val defaultEnd = defaultYearMonth.atEndOfMonth().toString()
    var name by remember { mutableStateOf(initial?.name ?: "Contrato") }
    var start by remember(initial, defaultYearMonth) {
        mutableStateOf(initial?.startDate ?: defaultStart)
    }
    var end by remember(initial, defaultYearMonth) {
        mutableStateOf(initial?.endDate ?: defaultEnd)
    }
    var center by remember {
        mutableStateOf(initial?.center?.takeIf { it.isNotBlank() } ?: "Basurto")
    }
    var centerExpanded by remember { mutableStateOf(false) }
    var turnicidad by remember { mutableStateOf(initial?.hasTurnicidad ?: true) }
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }
    val centerOptions = listOf("Basurto", "Cruces")

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        ExposedDropdownMenuBox(expanded = centerExpanded, onExpandedChange = { centerExpanded = it }) {
            OutlinedTextField(
                value = center,
                onValueChange = {},
                readOnly = true,
                label = { Text("Centro") },
                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(centerExpanded) },
                modifier = Modifier.fillMaxWidth().menuAnchor()
            )
            ExposedDropdownMenu(expanded = centerExpanded, onDismissRequest = { centerExpanded = false }) {
                centerOptions.forEach { opt ->
                    DropdownMenuItem(
                        text = { Text(opt) },
                        onClick = { center = opt; centerExpanded = false }
                    )
                }
            }
        }
        DateField("Inicio", start, { start = it }) { pickStart = true }
        DateField("Fin (vacio = abierto)", end, { end = it }) { pickEnd = true }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Turnicidad en este contrato", modifier = Modifier.weight(1f))
            Switch(checked = turnicidad, onCheckedChange = { turnicidad = it })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel) { Text("Cancelar") }
            Button(onClick = {
                if (name.isBlank() || !isValidDate(start)) return@Button
                if (end.isNotBlank() && !isValidDate(end)) return@Button
                onSave(
                    WorkContract(
                        id = initial?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        startDate = start.trim(),
                        endDate = end.trim().ifBlank { null },
                        center = center.trim(),
                        hasTurnicidad = turnicidad,
                        notes = initial?.notes ?: ""
                    )
                )
            }) { Text(if (initial != null) "Guardar cambios" else "Anadir") }
        }
    }

    if (pickStart) DatePickDialog(parseDateOrToday(start), { pickStart = false }) { start = it.toString(); pickStart = false }
    if (pickEnd) DatePickDialog(parseDateOrToday(end.ifBlank { start }), { pickEnd = false }) { end = it.toString(); pickEnd = false }
}

@Composable
private fun DateField(label: String, value: String, onValueChange: (String) -> Unit, onOpenPicker: () -> Unit) {
    OutlinedTextField(
        value = value, onValueChange = onValueChange, label = { Text("$label (aaaa-mm-dd)") }, singleLine = true, modifier = Modifier.fillMaxWidth(),
        trailingIcon = { IconButton(onClick = onOpenPicker) { Icon(Icons.Default.DateRange, contentDescription = "Calendario") } },
        supportingText = { if (value.isNotBlank() && !isValidDate(value)) Text("Fecha no valida", color = MaterialTheme.colorScheme.error) }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickDialog(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    val millis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(initialSelectedDateMillis = millis)
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                val selected = state.selectedDateMillis ?: return@TextButton
                onConfirm(Instant.ofEpochMilli(selected).atZone(ZoneOffset.UTC).toLocalDate())
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    ) { DatePicker(state = state) }
}

private fun isValidDate(s: String): Boolean = try { LocalDate.parse(s.trim()); true } catch (_: DateTimeParseException) { false }
private fun parseDateOrToday(s: String): LocalDate = try { LocalDate.parse(s.trim()) } catch (_: Exception) { LocalDate.now() }
