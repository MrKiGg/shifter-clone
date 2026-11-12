package com.lrhsoft.shifterclone.ui.components

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
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
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
import com.lrhsoft.shifterclone.model.PayrollTables
import com.lrhsoft.shifterclone.model.UserProfile

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(
    profile: UserProfile,
    onSaveProfile: (UserProfile) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var category by remember { mutableStateOf(profile.category) }
    var destino by remember { mutableIntStateOf(profile.destinoLevel) }
    var trienios by remember { mutableIntStateOf(profile.trienios) }
    var catExpanded by remember { mutableStateOf(false) }

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
                Text(
                    "Perfil",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar")
                }
            }

            Text(
                "Categoria Celador · Grupo E. Importes orientativos (tabla 2019).",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(16.dp))

            Text("Categoria", fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(6.dp))
            ExposedDropdownMenuBox(
                expanded = catExpanded,
                onExpandedChange = { catExpanded = it }
            ) {
                OutlinedTextField(
                    value = category.label,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Categoria") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = catExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = catExpanded,
                    onDismissRequest = { catExpanded = false }
                ) {
                    CeladorCategory.entries.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat.label) },
                            onClick = {
                                category = cat
                                destino = cat.destinoLevel
                                catExpanded = false
                            }
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("Grupo profesional: E (Celador)", fontSize = 14.sp)
            Text(
                "Complemento de destino nivel $destino -> ${"%.2f".format(PayrollTables.destinoPorNivel[destino] ?: 0.0)} EUR/mes",
                fontSize = 13.sp
            )
            Text(
                "Complemento especifico -> ${"%.2f".format(category.especificoMensual)} EUR/mes",
                fontSize = 13.sp
            )

            Spacer(Modifier.height(16.dp))
            Text("Trienios (antiguedad)", fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = { if (trienios > 0) trienios-- }) { Text("-") }
                Text("$trienios", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 12.dp))
                TextButton(onClick = { if (trienios < 20) trienios++ }) { Text("+") }
            }

            Text(
                "La estimacion de sueldo se calcula en Estadisticas segun lo pintado en el mes (prorrateo, nocturnidad, festivos, turnicidad).",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 12.dp)
            )

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = {
                    onSaveProfile(
                        UserProfile(
                            category = category,
                            group = PayGroup.E,
                            destinoLevel = destino,
                            trienios = trienios
                        )
                    )
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth()
            ) { Text("Guardar perfil") }
            Spacer(Modifier.height(24.dp))
        }
    }
}
