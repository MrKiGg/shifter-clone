package com.lrhsoft.shifterclone.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/**
 * Selector de centros al estilo del original (panel CALENDARIOS).
 * Hasta 5 slots: 1 Basurto, 2 Cruces, resto "Sin nombre".
 */
@Composable
fun CentersPickerSheet(
    centers: List<String>,
    selectedCenter: String,
    onSelect: (String) -> Unit,
    onRename: (index: Int, newName: String) -> Unit,
    onDismiss: () -> Unit
) {
    val slots = centers.take(5).let { list ->
        if (list.size >= 5) list else list + List(5 - list.size) { "Sin nombre" }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .clip(RoundedCornerShape(12.dp)),
            color = Color.White,
            shadowElevation = 8.dp
        ) {
            Column {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color(0xFFD32F2F))
                        .padding(vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "CENTROS",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        letterSpacing = 1.sp
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.align(Alignment.CenterEnd)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.White)
                    }
                }

                Spacer(Modifier.height(10.dp))

                slots.forEachIndexed { index, name ->
                    var editing by remember(name, index) { mutableStateOf(false) }
                    var draft by remember(name, index) { mutableStateOf(name) }
                    val isSelected = name == selectedCenter ||
                        (selectedCenter.isBlank() && index == 0)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(
                                when {
                                    isSelected -> Color(0xFF00897B)
                                    else -> Color.White
                                }
                            )
                            .border(
                                width = 1.dp,
                                color = if (isSelected) Color(0xFF00897B) else Color(0xFFBDBDBD),
                                shape = RoundedCornerShape(24.dp)
                            )
                            .clickable(enabled = !editing) {
                                onSelect(name)
                                onDismiss()
                            }
                            .padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Editar nombre",
                            tint = if (isSelected) Color.White else Color(0xFF757575),
                            modifier = Modifier
                                .size(22.dp)
                                .clickable {
                                    editing = true
                                    draft = name
                                }
                        )
                        Spacer(Modifier.width(10.dp))
                        if (editing) {
                            BasicTextField(
                                value = draft,
                                onValueChange = { draft = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = if (isSelected) Color.White else Color(0xFF212121),
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold
                                ),
                                cursorBrush = SolidColor(if (isSelected) Color.White else Color(0xFF00897B)),
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        (if (isSelected) Color.White else Color(0xFF00897B)).copy(alpha = 0.15f),
                                        RoundedCornerShape(6.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                decorationBox = { inner ->
                                    if (draft.isEmpty()) {
                                        Text("Sin nombre", color = Color.Gray, fontSize = 16.sp)
                                    }
                                    inner()
                                }
                            )
                            Text(
                                "OK",
                                color = if (isSelected) Color.White else Color(0xFF00897B),
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clickable {
                                        val trimmed = draft.trim().ifBlank { "Sin nombre" }
                                        onRename(index, trimmed)
                                        editing = false
                                    }
                                    .padding(8.dp)
                            )
                        } else {
                            Text(
                                text = "${index + 1}.-  $name",
                                color = if (isSelected) Color.White else Color(0xFF212121),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                Icons.Default.Menu,
                                contentDescription = null,
                                tint = if (isSelected) Color.White else Color(0xFF9E9E9E),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))
            }
        }
    }
}
