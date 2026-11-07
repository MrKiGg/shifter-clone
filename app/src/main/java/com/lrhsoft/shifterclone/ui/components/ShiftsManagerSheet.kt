package com.lrhsoft.shifterclone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.lrhsoft.shifterclone.model.Shift

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShiftsManagerScreen(
    shifts: List<Shift>,
    onDismiss: () -> Unit,
    onEdit: (Shift) -> Unit,
    onDuplicate: (Shift) -> Unit,
    onDelete: (Shift) -> Unit,
    onCreate: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text("Turnos", fontWeight = FontWeight.Bold)
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color(0xFF37474F),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            floatingActionButton = {
                FloatingActionButton(
                    onClick = onCreate,
                    containerColor = Color(0xFFD32F2F)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Nuevo turno", tint = Color.White)
                }
            }
        ) { padding ->
            if (shifts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "No hay turnos. Pulsa + para crear uno.",
                        color = Color.Gray,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(shifts, key = { it.id }) { shift ->
                        ShiftManagerRow(
                            shift = shift,
                            onEdit = { onEdit(shift) },
                            onDuplicate = { onDuplicate(shift) },
                            onDelete = { onDelete(shift) }
                        )
                    }
                    item { Spacer(Modifier.height(72.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ShiftManagerRow(
    shift: Shift,
    onEdit: () -> Unit,
    onDuplicate: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(Color(0xFFF7F7F7))
            .clickable(onClick = onEdit)
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(shift.toComposeColor())
                .border(1.dp, Color(0x22000000), RoundedCornerShape(8.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = shift.abbreviation,
                color = shift.toTextComposeColor(),
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }

        Spacer(Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = shift.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp
            )
            val schedule = buildString {
                if (shift.startHour != null && shift.endHour != null) {
                    val sm = (shift.startMinute ?: 0).toString().padStart(2, '0')
                    val em = (shift.endMinute ?: 0).toString().padStart(2, '0')
                    append("${shift.startHour}:$sm – ${shift.endHour}:$em")
                }
                if (shift.isRest) {
                    if (isNotEmpty()) append(" · ")
                    append("Descanso")
                }
                if (shift.hourlyRate != null) {
                    if (isNotEmpty()) append(" · ")
                    append("${shift.hourlyRate} €/h")
                }
            }
            if (schedule.isNotEmpty()) {
                Text(text = schedule, fontSize = 12.sp, color = Color.Gray)
            }
        }

        IconButton(onClick = onEdit) {
            Icon(Icons.Default.Edit, contentDescription = "Editar", tint = Color(0xFF455A64))
        }
        IconButton(onClick = onDuplicate) {
            Icon(Icons.Default.ContentCopy, contentDescription = "Duplicar", tint = Color(0xFF455A64))
        }
        IconButton(onClick = onDelete) {
            Icon(Icons.Default.Delete, contentDescription = "Borrar", tint = Color(0xFFD32F2F))
        }
    }
}
