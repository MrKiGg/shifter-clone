package com.lrhsoft.shifterclone.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lrhsoft.shifterclone.model.Shift
import java.util.UUID
import kotlin.math.roundToInt

private val PRESET_COLORS = listOf(
    0xFF4CAF50, 0xFFFF9800, 0xFF3F51B5, 0xFF9E9E9E, 0xFF00BCD4,
    0xFFE91E63, 0xFF9C27B0, 0xFFF44336, 0xFF795548, 0xFF607D8B,
    0xFF8BC34A, 0xFFFFEB3B, 0xFF2196F3, 0xFFFF5722, 0xFF000000, 0xFFFFFFFF
)

@Composable
fun ShiftEditorDialog(
    shift: Shift?,
    onDismiss: () -> Unit,
    onSave: (Shift) -> Unit,
    onDelete: ((Shift) -> Unit)? = null
) {
    val isEdit = shift != null

    var name by remember { mutableStateOf(shift?.name ?: "") }
    var abbreviation by remember { mutableStateOf(shift?.abbreviation ?: "") }
    var color by remember { mutableLongStateOf(shift?.color ?: 0xFF4CAF50) }
    var textColor by remember { mutableLongStateOf(shift?.textColor ?: 0xFFFFFFFF) }
    var startHour by remember { mutableStateOf(shift?.startHour?.toString() ?: "") }
    var startMinute by remember { mutableStateOf(shift?.startMinute?.toString()?.padStart(2, '0') ?: "") }
    var endHour by remember { mutableStateOf(shift?.endHour?.toString() ?: "") }
    var endMinute by remember { mutableStateOf(shift?.endMinute?.toString()?.padStart(2, '0') ?: "") }
    var isRest by remember { mutableStateOf(shift?.isRest ?: false) }
    var hourlyRate by remember {
        mutableStateOf(shift?.hourlyRate?.let { if (it % 1.0 == 0.0) it.toInt().toString() else it.toString() } ?: "")
    }

    var showBgColorPicker by remember { mutableStateOf(false) }
    var showTextColorPicker by remember { mutableStateOf(false) }

    val isValid = name.isNotBlank() && abbreviation.isNotBlank()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isEdit) "Editar turno" else "Nuevo turno",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(color)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (abbreviation.isNotBlank()) abbreviation else "?",
                        color = Color(textColor),
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = abbreviation,
                    onValueChange = { if (it.length <= 3) abbreviation = it.uppercase() },
                    label = { Text("Abreviatura (máx 3)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Color de fondo", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                ColorPickerRow(
                    colors = PRESET_COLORS,
                    selected = color,
                    onSelect = { color = it },
                    onCustomClick = { showBgColorPicker = true }
                )

                Text("Color del texto", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                ColorPickerRow(
                    colors = listOf(0xFFFFFFFF, 0xFF000000, 0xFFFFEB3B, 0xFFE91E63),
                    selected = textColor,
                    onSelect = { textColor = it },
                    onCustomClick = { showTextColorPicker = true }
                )

                Text("Horario (opcional)", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TimeField("H", startHour) { startHour = it.filter { c -> c.isDigit() }.take(2) }
                    Text(":")
                    TimeField("M", startMinute) { startMinute = it.filter { c -> c.isDigit() }.take(2) }
                    Text("→", modifier = Modifier.padding(horizontal = 4.dp))
                    TimeField("H", endHour) { endHour = it.filter { c -> c.isDigit() }.take(2) }
                    Text(":")
                    TimeField("M", endMinute) { endMinute = it.filter { c -> c.isDigit() }.take(2) }
                }

                OutlinedTextField(
                    value = hourlyRate,
                    onValueChange = {
                        hourlyRate = it.filter { c -> c.isDigit() || c == '.' || c == ',' }
                            .replace(',', '.')
                    },
                    label = { Text("Tarifa €/hora (opcional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isRest
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = isRest,
                        onCheckedChange = { isRest = it }
                    )
                    Text("Es día de descanso / libre")
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val newShift = Shift(
                        id = shift?.id ?: UUID.randomUUID().toString(),
                        name = name.trim(),
                        abbreviation = abbreviation.trim().uppercase(),
                        color = color,
                        textColor = textColor,
                        startHour = startHour.toIntOrNull()?.coerceIn(0, 23),
                        startMinute = startMinute.toIntOrNull()?.coerceIn(0, 59),
                        endHour = endHour.toIntOrNull()?.coerceIn(0, 23),
                        endMinute = endMinute.toIntOrNull()?.coerceIn(0, 59),
                        isRest = isRest,
                        hourlyRate = if (isRest) null else hourlyRate.toDoubleOrNull()
                    )
                    onSave(newShift)
                },
                enabled = isValid
            ) {
                Text(if (isEdit) "Guardar" else "Crear")
            }
        },
        dismissButton = {
            Row {
                if (isEdit && onDelete != null) {
                    TextButton(
                        onClick = { onDelete(shift!!) },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Eliminar")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        }
    )

    if (showBgColorPicker) {
        RgbColorPickerDialog(
            initialColor = color,
            title = "Color de fondo",
            onDismiss = { showBgColorPicker = false },
            onConfirm = {
                color = it
                showBgColorPicker = false
            }
        )
    }

    if (showTextColorPicker) {
        RgbColorPickerDialog(
            initialColor = textColor,
            title = "Color del texto",
            onDismiss = { showTextColorPicker = false },
            onConfirm = {
                textColor = it
                showTextColorPicker = false
            }
        )
    }
}

@Composable
private fun TimeField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        modifier = Modifier.width(64.dp)
    )
}

@Composable
private fun ColorPickerRow(
    colors: List<Long>,
    selected: Long,
    onSelect: (Long) -> Unit,
    onCustomClick: () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            colors.forEach { c ->
                val isSelected = c == selected
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color(c))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Gray.copy(alpha = 0.4f),
                            shape = CircleShape
                        )
                        .clickable { onSelect(c) }
                )
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(selected))
                    .border(2.dp, Color.Black, CircleShape)
            )
            OutlinedButton(onClick = onCustomClick) {
                Text("Personalizado…")
            }
            Text(
                text = "#${selected.toString(16).uppercase().padStart(8, '0').takeLast(6)}",
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun RgbColorPickerDialog(
    initialColor: Long,
    title: String,
    onDismiss: () -> Unit,
    onConfirm: (Long) -> Unit
) {
    val init = Color(initialColor)
    val (ih, isat, iv) = rgbToHsv(init.red, init.green, init.blue)

    var hue by remember { mutableFloatStateOf(ih) }
    var saturation by remember { mutableFloatStateOf(isat) }
    var value by remember { mutableFloatStateOf(iv) }

    val current = hsvToColor(hue, saturation, value)
    val argb = (0xFF000000L or (current.toArgb().toLong() and 0xFFFFFFL))

    val r = (current.red * 255).roundToInt()
    val g = (current.green * 255).roundToInt()
    val b = (current.blue * 255).roundToInt()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(current)
                        .border(1.dp, Color.Gray.copy(alpha = 0.4f), RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "#${argb.toString(16).uppercase().padStart(8, '0').takeLast(6)}  ·  RGB($r, $g, $b)",
                        color = if (value > 0.55f && saturation < 0.5f) Color.Black else Color.White,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 13.sp
                    )
                }

                Text("Matiz (ruleta)", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                HueBar(
                    hue = hue,
                    onHueChange = { hue = it }
                )

                Text("Saturación", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Slider(
                    value = saturation,
                    onValueChange = { saturation = it },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = current,
                        activeTrackColor = current
                    )
                )

                Text("Brillo", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                Slider(
                    value = value,
                    onValueChange = { value = it },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(
                        thumbColor = current,
                        activeTrackColor = Color.Gray
                    )
                )

                Text("RGB", fontWeight = FontWeight.Medium, fontSize = 13.sp)
                RgbSlider("R", r, Color.Red) { nr ->
                    val (nh, ns, nv) = rgbToHsv(nr / 255f, g / 255f, b / 255f)
                    hue = nh; saturation = ns; value = nv
                }
                RgbSlider("G", g, Color(0xFF4CAF50)) { ng ->
                    val (nh, ns, nv) = rgbToHsv(r / 255f, ng / 255f, b / 255f)
                    hue = nh; saturation = ns; value = nv
                }
                RgbSlider("B", b, Color(0xFF2196F3)) { nb ->
                    val (nh, ns, nv) = rgbToHsv(r / 255f, g / 255f, nb / 255f)
                    hue = nh; saturation = ns; value = nv
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(argb) }) {
                Text("Usar color")
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun RgbSlider(
    label: String,
    value: Int,
    tint: Color,
    onChange: (Int) -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(label, modifier = Modifier.width(20.dp), fontWeight = FontWeight.Bold)
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt().coerceIn(0, 255)) },
            valueRange = 0f..255f,
            modifier = Modifier.weight(1f),
            colors = SliderDefaults.colors(
                thumbColor = tint,
                activeTrackColor = tint
            )
        )
        Text(
            text = value.toString().padStart(3, ' '),
            modifier = Modifier.width(36.dp),
            fontSize = 13.sp
        )
    }
}

@Composable
private fun HueBar(
    hue: Float,
    onHueChange: (Float) -> Unit
) {
    val spectrum = remember {
        listOf(
            Color.Red, Color.Yellow, Color.Green,
            Color.Cyan, Color.Blue, Color.Magenta, Color.Red
        )
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(36.dp)
            .clip(RoundedCornerShape(8.dp))
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val h = (offset.x / size.width).coerceIn(0f, 1f) * 360f
                    onHueChange(h)
                }
            }
            .pointerInput(Unit) {
                detectHorizontalDragGestures { change, _ ->
                    change.consume()
                    val h = (change.position.x / size.width).coerceIn(0f, 1f) * 360f
                    onHueChange(h)
                }
            }
    ) {
        Canvas(modifier = Modifier.fillMaxWidth().height(36.dp)) {
            drawRect(brush = Brush.horizontalGradient(spectrum))
            val x = (hue / 360f) * size.width
            drawCircle(
                color = Color.White,
                radius = 10.dp.toPx(),
                center = Offset(x, size.height / 2f)
            )
            drawCircle(
                color = Color.Black,
                radius = 10.dp.toPx(),
                center = Offset(x, size.height / 2f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
            )
            drawCircle(
                color = hsvToColor(hue, 1f, 1f),
                radius = 6.dp.toPx(),
                center = Offset(x, size.height / 2f)
            )
        }
    }
}

private fun hsvToColor(h: Float, s: Float, v: Float): Color {
    val hue = ((h % 360f) + 360f) % 360f
    val c = v * s
    val x = c * (1f - kotlin.math.abs((hue / 60f) % 2f - 1f))
    val m = v - c
    val (rp, gp, bp) = when {
        hue < 60f -> Triple(c, x, 0f)
        hue < 120f -> Triple(x, c, 0f)
        hue < 180f -> Triple(0f, c, x)
        hue < 240f -> Triple(0f, x, c)
        hue < 300f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    return Color(red = rp + m, green = gp + m, blue = bp + m)
}

private fun rgbToHsv(r: Float, g: Float, b: Float): Triple<Float, Float, Float> {
    val max = maxOf(r, g, b)
    val min = minOf(r, g, b)
    val delta = max - min
    val h = when {
        delta == 0f -> 0f
        max == r -> 60f * (((g - b) / delta) % 6f)
        max == g -> 60f * (((b - r) / delta) + 2f)
        else -> 60f * (((r - g) / delta) + 4f)
    }.let { if (it < 0f) it + 360f else it }
    val s = if (max == 0f) 0f else delta / max
    return Triple(h, s, max)
}
