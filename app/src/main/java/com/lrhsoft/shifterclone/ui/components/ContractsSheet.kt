package com.lrhsoft.shifterclone.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lrhsoft.shifterclone.model.WorkContract
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeParseException
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContractsSheet(
    contracts: List<WorkContract>,
    onSaveContracts: (List<WorkContract>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var local by remember { mutableStateOf(contracts) }
    var showForm by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Contratos", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                IconButton(onClick = onDismiss) { Icon(Icons.Default.Close, contentDescription = "Cerrar") }
            }
            Text(
                "Define periodos de trabajo (centro / cartelera). Se usan en la estimacion de nomina.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            local.forEach { c ->
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(c.name, fontWeight = FontWeight.Medium)
                        Text(
                            "${c.startDate} -> ${c.endDate ?: "abierto"}${if (c.center.isNotBlank()) " · ${c.center}" else ""}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    TextButton(onClick = { local = local.filter { it.id != c.id } }) { Text("Borrar") }
                }
                HorizontalDivider()
            }

            Spacer(Modifier.height(8.dp))
            if (!showForm) {
                Button(onClick = { showForm = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("+ Anadir contrato")
                }
            } else {
                ContractForm(
                    onCancel = { showForm = false },
                    onAdd = { c -> local = local + c; showForm = false }
                )
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { onSaveContracts(local); onDismiss() },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Guardar contratos") }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ContractForm(onCancel: () -> Unit, onAdd: (WorkContract) -> Unit) {
    var name by remember { mutableStateOf("Contrato") }
    var start by remember { mutableStateOf(LocalDate.now().toString()) }
    var end by remember { mutableStateOf("") }
    var center by remember { mutableStateOf("") }
    var turnicidad by remember { mutableStateOf(true) }
    var pickStart by remember { mutableStateOf(false) }
    var pickEnd by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(vertical = 8.dp)) {
        OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        OutlinedTextField(value = center, onValueChange = { center = it }, label = { Text("Centro / cartelera") }, singleLine = true, modifier = Modifier.fillMaxWidth())
        DateField(label = "Inicio", value = start, onValueChange = { start = it }, onOpenPicker = { pickStart = true })
        DateField(label = "Fin (vacio = abierto)", value = end, onValueChange = { end = it }, onOpenPicker = { pickEnd = true })
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Turnicidad en este contrato", modifier = Modifier.weight(1f))
            Switch(checked = turnicidad, onCheckedChange = { turnicidad = it })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel) { Text("Cancelar") }
            Button(onClick = {
                if (name.isBlank() || !isValidDate(start)) return@Button
                if (end.isNotBlank() && !isValidDate(end)) return@Button
                onAdd(
                    WorkContract(
                        id = UUID.randomUUID().toString(),
                        name = name.trim(),
                        startDate = start.trim(),
                        endDate = end.trim().ifBlank { null },
                        center = center.trim(),
                        hasTurnicidad = turnicidad
                    )
                )
            }) { Text("Anadir") }
        }
    }

    if (pickStart) {
        DatePickDialog(initial = parseDateOrToday(start), onDismiss = { pickStart = false }, onConfirm = { start = it.toString(); pickStart = false })
    }
    if (pickEnd) {
        DatePickDialog(initial = parseDateOrToday(end.ifBlank { start }), onDismiss = { pickEnd = false }, onConfirm = { end = it.toString(); pickEnd = false })
    }
}

@Composable
private fun DateField(label: String, value: String, onValueChange: (String) -> Unit, onOpenPicker: () -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text("$label (aaaa-mm-dd)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
        trailingIcon = {
            IconButton(onClick = onOpenPicker) { Icon(Icons.Default.DateRange, contentDescription = "Calendario") }
        },
        supportingText = {
            if (value.isNotBlank() && !isValidDate(value)) {
                Text("Fecha no valida", color = MaterialTheme.colorScheme.error)
            }
        }
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
