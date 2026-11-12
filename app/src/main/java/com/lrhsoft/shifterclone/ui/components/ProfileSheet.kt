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
import androidx.compose.material3.Button
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lrhsoft.shifterclone.model.CeladorCategory
import com.lrhsoft.shifterclone.model.PayGroup
import com.lrhsoft.shifterclone.model.PayrollEstimate
import com.lrhsoft.shifterclone.model.PayrollTables
import com.lrhsoft.shifterclone.model.UserProfile
import com.lrhsoft.shifterclone.model.WorkContract
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(
    profile: UserProfile,
    contracts: List<WorkContract>,
    monthEstimate: PayrollEstimate?,
    onSaveProfile: (UserProfile) -> Unit,
    onSaveContracts: (List<WorkContract>) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var category by remember { mutableStateOf(profile.category) }
    var destino by remember { mutableIntStateOf(profile.destinoLevel) }
    var trienios by remember { mutableIntStateOf(profile.trienios) }
    var turnicidad by remember { mutableStateOf(profile.hasTurnicidad) }
    var fullNight by remember { mutableStateOf(profile.countNightShiftAsFullNight) }
    var localContracts by remember { mutableStateOf(contracts) }
    var showAddContract by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
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
                Text(
                    "Perfil / Nomina",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            Text(
                "De momento solo categoria Celador (Grupo E). Importes orientativos tabla 2019 del manual Osakidetza.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(12.dp))

            Text("Categoria", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            CeladorCategory.entries.forEach { cat ->
                FilterChip(
                    selected = category == cat,
                    onClick = {
                        category = cat
                        destino = cat.destinoLevel
                    },
                    label = { Text(cat.label, fontSize = 13.sp) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 2.dp)
                )
            }

            Spacer(Modifier.height(12.dp))
            Text("Grupo profesional", fontWeight = FontWeight.SemiBold)
            Text("Celador -> Grupo E (fijo por ahora)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "Complemento de destino nivel $destino -> ${"%.2f".format(PayrollTables.destinoPorNivel[destino] ?: 0.0)} EUR/mes",
                fontSize = 13.sp
            )
            Text(
                "Complemento especifico -> ${"%.2f".format(category.especificoMensual)} EUR/mes",
                fontSize = 13.sp
            )

            Spacer(Modifier.height(12.dp))
            Text("Trienios (antiguedad)", fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { if (trienios > 0) trienios-- }) { Text("-") }
                Text("$trienios", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
                TextButton(onClick = { if (trienios < 20) trienios++ }) { Text("+") }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Turnicidad", modifier = Modifier.weight(1f))
                Switch(checked = turnicidad, onCheckedChange = { turnicidad = it })
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Turno noche = noche completa (30,44 EUR)", modifier = Modifier.weight(1f), fontSize = 13.sp)
                Switch(checked = fullNight, onCheckedChange = { fullNight = it })
            }

            Spacer(Modifier.height(8.dp))
            Text(
                "Nocturnidad G.E: 3,04 EUR/h · 30,44 EUR/noche\nDomingos/festivos G.E: 6,54 EUR/h · 45,77 EUR dia · 65,39 EUR noche",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text("Contratos", fontWeight = FontWeight.SemiBold)
            Text(
                "Define periodos en el calendario. La turnicidad y el centro se asocian al contrato.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            localContracts.forEach { c ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
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
                    TextButton(onClick = {
                        localContracts = localContracts.filter { it.id != c.id }
                    }) { Text("Borrar") }
                }
            }
            TextButton(onClick = { showAddContract = true }) {
                Text("+ Anadir contrato")
            }

            if (showAddContract) {
                AddContractForm(
                    onCancel = { showAddContract = false },
                    onAdd = { contract ->
                        localContracts = localContracts + contract
                        showAddContract = false
                    }
                )
            }

            if (monthEstimate != null) {
                Spacer(Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))
                Text("Estimacion mes actual (bruto orientativo)", fontWeight = FontWeight.SemiBold)
                Text(
                    "Fijo: ${"%.2f".format(monthEstimate.fixedMonthly)} EUR  ·  Variables: ${"%.2f".format(monthEstimate.variableFromShifts)} EUR",
                    fontSize = 13.sp
                )
                Text(
                    "Total estimado: ${"%.2f".format(monthEstimate.totalEstimated)} EUR",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
                Text(
                    "Dias trabajados: ${monthEstimate.workDays} · Noches: ${monthEstimate.nights} · Fest. dia: ${monthEstimate.festivoDays} · Fest. noche: ${monthEstimate.festivoNights}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                monthEstimate.breakdown.forEach { (label, amount) ->
                    Row(Modifier.fillMaxWidth()) {
                        Text(label, modifier = Modifier.weight(1f), fontSize = 12.sp)
                        Text("${"%.2f".format(amount)} EUR", fontSize = 12.sp)
                    }
                }
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    val updated = UserProfile(
                        category = category,
                        group = PayGroup.E,
                        destinoLevel = destino,
                        trienios = trienios,
                        hasTurnicidad = turnicidad,
                        countNightShiftAsFullNight = fullNight
                    )
                    onSaveProfile(updated)
                    onSaveContracts(localContracts)
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Guardar perfil")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun AddContractForm(
    onCancel: () -> Unit,
    onAdd: (WorkContract) -> Unit
) {
    var name by remember { mutableStateOf("Contrato") }
    var start by remember { mutableStateOf("2026-11-01") }
    var end by remember { mutableStateOf("") }
    var center by remember { mutableStateOf("") }
    var turnicidad by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = start,
            onValueChange = { start = it },
            label = { Text("Inicio (aaaa-mm-dd)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = end,
            onValueChange = { end = it },
            label = { Text("Fin (opcional, aaaa-mm-dd)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        OutlinedTextField(
            value = center,
            onValueChange = { center = it },
            label = { Text("Centro / cartelera") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Turnicidad en este contrato", modifier = Modifier.weight(1f))
            Switch(checked = turnicidad, onCheckedChange = { turnicidad = it })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = onCancel) { Text("Cancelar") }
            Button(onClick = {
                if (name.isBlank() || start.isBlank()) return@Button
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
}
