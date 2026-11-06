package com.lrhsoft.shifterclone.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lrhsoft.shifterclone.model.DayNote
import com.lrhsoft.shifterclone.model.Shift
import com.lrhsoft.shifterclone.ui.components.CalendarDayCell
import com.lrhsoft.shifterclone.ui.components.DayDetailSheet
import com.lrhsoft.shifterclone.ui.components.ShiftChip
import com.lrhsoft.shifterclone.ui.components.ShiftEditorDialog
import com.lrhsoft.shifterclone.ui.components.StatsSheet
import com.lrhsoft.shifterclone.ui.components.ExportSheet
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(
    viewModel: CalendarViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calendario de turnos", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { viewModel.openStats() }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Estadísticas")
                    }
                    IconButton(onClick = { viewModel.openExport() }) {
                        Icon(Icons.Default.Upload, contentDescription = "Exportar")
                    }
                    IconButton(onClick = { viewModel.goToToday() }) {
                        Icon(Icons.Default.Today, contentDescription = "Hoy")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.openCreateShift() },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Nuevo turno", tint = Color.White)
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Navegación mes
            MonthHeader(
                yearMonth = state.currentMonth,
                onPrevious = { viewModel.previousMonth() },
                onNext = { viewModel.nextMonth() }
            )

            // Selector de modo
            ModeSelector(
                mode = state.editMode,
                onModeChange = { viewModel.setEditMode(it) }
            )

            // Paleta de turnos
            ShiftPalette(
                shifts = state.shifts,
                selectedId = state.selectedShiftId,
                onSelect = { viewModel.selectShift(it) },
                onLongClick = { viewModel.openEditShift(it) }
            )

            // Acciones de rango (solo en modo EDIT)
            if (state.editMode == EditMode.EDIT) {
                RangeActions(
                    hasRange = state.selectedRange != null,
                    rangeSize = state.selectedRange?.size ?: 0,
                    onApply = { viewModel.applyShiftToRange() },
                    onClear = { viewModel.clearRangeShifts() },
                    onRepeat = { viewModel.repeatPattern() },
                    onRepeatMonth = { viewModel.repeatPatternMonths(1) },
                    onClearSelection = { viewModel.clearRange() }
                )
            }

            // Cuadrícula del mes
            MonthGrid(
                yearMonth = state.currentMonth,
                assignments = state.assignments,
                shifts = state.shifts,
                notes = state.notes,
                selectedRange = state.selectedRange,
                onDayClick = { y, m, d -> viewModel.onDayClicked(y, m, d) },
                onDayLongClick = { y, m, d -> viewModel.onDayLongPressed(y, m, d) }
            )
        }
    }

    // Diálogos / sheets
    if (state.showShiftEditor) {
        ShiftEditorDialog(
            shift = state.editingShift,
            onDismiss = { viewModel.dismissShiftEditor() },
            onSave = { viewModel.saveShift(it) },
            onDelete = if (state.editingShift != null) {{ viewModel.deleteShift(it) }} else null
        )
    }

    state.detailDate?.let { date ->
        val note = state.noteFor(date)
        val dayShifts = state.assignments[
            "${date.year}-${date.monthValue}-${date.dayOfMonth}"
        ]?.shiftIds?.mapNotNull { id -> state.shifts.find { it.id == id } }.orEmpty()
        DayDetailSheet(
            date = date,
            shifts = dayShifts,
            note = note,
            onDismiss = { viewModel.dismissDayDetail() },
            onSave = { viewModel.saveNote(it) },
            onDelete = { viewModel.deleteNote(date.year, date.monthValue, date.dayOfMonth) }
        )
    }

    if (state.showStats) {
        val summary = viewModel.computeStats(state)
        StatsSheet(
            summary = summary,
            period = state.statsPeriod,
            yearMonth = state.statsYearMonth,
            onPeriodChange = { viewModel.setStatsPeriod(it) },
            onPrevious = { viewModel.statsPrevious() },
            onNext = { viewModel.statsNext() },
            onDismiss = { viewModel.dismissStats() }
        )
    }

    if (state.showExport) {
        ExportSheet(
            calendars = state.exportCalendars,
            selectedCalendarId = state.selectedCalendarId,
            range = state.exportRange,
            yearMonth = state.currentMonth,
            includeRest = state.exportIncludeRest,
            isLoading = state.exportLoading,
            statusMessage = state.exportMessage,
            hasCalendarPermission = true,
            onSelectCalendar = { viewModel.selectExportCalendar(it) },
            onRangeChange = { viewModel.setExportRange(it) },
            onIncludeRestChange = { viewModel.setExportIncludeRest(it) },
            onRequestPermission = { },
            onExportToCalendar = { viewModel.exportToDeviceCalendar() },
            onShareIcs = {
                viewModel.buildIcsShareIntent()?.let { /* handled by Activity if needed */ }
            },
            onDismiss = { viewModel.dismissExport() }
        )
    }
}

@Composable
private fun MonthHeader(
    yearMonth: YearMonth,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    val monthName = yearMonth.month
        .getDisplayName(TextStyle.FULL, Locale("es", "ES"))
        .replaceFirstChar { it.uppercase() }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mes anterior")
        }
        Text(
            text = "$monthName ${yearMonth.year}",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mes siguiente")
        }
    }
}

@Composable
private fun ModeSelector(
    mode: EditMode,
    onModeChange: (EditMode) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = mode == EditMode.PAINT,
            onClick = { onModeChange(EditMode.PAINT) },
            label = { Text("Pintar") },
            leadingIcon = { Icon(Icons.Default.Brush, contentDescription = null, modifier = Modifier.height(16.dp)) }
        )
        FilterChip(
            selected = mode == EditMode.EDIT,
            onClick = { onModeChange(EditMode.EDIT) },
            label = { Text("Editar rango") },
            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.height(16.dp)) }
        )
    }
}

@Composable
private fun ShiftPalette(
    shifts: List<Shift>,
    selectedId: String?,
    onSelect: (String) -> Unit,
    onLongClick: (Shift) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        shifts.forEach { shift ->
            ShiftChip(
                shift = shift,
                isSelected = shift.id == selectedId,
                onClick = { onSelect(shift.id) },
                onLongClick = { onLongClick(shift) }
            )
        }
    }
}

@Composable
private fun RangeActions(
    hasRange: Boolean,
    rangeSize: Int,
    onApply: () -> Unit,
    onClear: () -> Unit,
    onRepeat: () -> Unit,
    onRepeatMonth: () -> Unit,
    onClearSelection: () -> Unit
) {
    Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
        if (hasRange) {
            Text(
                text = "$rangeSize día(s) seleccionado(s)",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            OutlinedButton(onClick = onApply, enabled = hasRange, modifier = Modifier.weight(1f)) {
                Text("Aplicar", fontSize = 12.sp)
            }
            OutlinedButton(onClick = onClear, enabled = hasRange, modifier = Modifier.weight(1f)) {
                Text("Borrar", fontSize = 12.sp)
            }
            OutlinedButton(onClick = onRepeat, enabled = hasRange, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.height(14.dp))
                Spacer(Modifier.width(2.dp))
                Text("Repetir", fontSize = 12.sp)
            }
            IconButton(onClick = onClearSelection, enabled = hasRange) {
                Icon(Icons.Default.Clear, contentDescription = "Quitar selección")
            }
        }
    }
}

@Composable
private fun MonthGrid(
    yearMonth: YearMonth,
    assignments: Map<String, com.lrhsoft.shifterclone.model.DayAssignment>,
    shifts: List<Shift>,
    notes: Map<String, DayNote>,
    selectedRange: DateRange?,
    onDayClick: (Int, Int, Int) -> Unit,
    onDayLongClick: (Int, Int, Int) -> Unit
) {
    val shiftMap = shifts.associateBy { it.id }
    val first = yearMonth.atDay(1)
    val startOffset = (first.dayOfWeek.value - 1) % 7
    val daysInMonth = yearMonth.lengthOfMonth()
    val cells = mutableListOf<LocalDate?>()
    val prev = yearMonth.minusMonths(1)
    val daysInPrev = prev.lengthOfMonth()
    for (i in startOffset downTo 1) {
        cells.add(prev.atDay(daysInPrev - i + 1))
    }
    for (d in 1..daysInMonth) {
        cells.add(yearMonth.atDay(d))
    }
    val remaining = (7 - cells.size % 7) % 7
    for (d in 1..remaining) {
        cells.add(yearMonth.plusMonths(1).atDay(d))
    }

    val today = LocalDate.now()
    val weekDays = listOf("L", "M", "X", "J", "V", "S", "D")

    Column(modifier = Modifier.padding(horizontal = 8.dp)) {
        Row(modifier = Modifier.fillMaxWidth()) {
            weekDays.forEach { d ->
                Text(
                    text = d,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        cells.chunked(7).forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                week.forEach { date ->
                    val inMonth = date != null && date.month == yearMonth.month
                    val key = if (date != null) "${date.year}-${date.monthValue}-${date.dayOfMonth}" else ""
                    val dayShifts = if (inMonth) {
                        assignments[key]?.shiftIds?.mapNotNull { shiftMap[it] }.orEmpty()
                    } else emptyList()
                    val note = if (inMonth) notes[key] else null
                    val isInRange = date != null && selectedRange?.contains(date) == true
                    val isEndpoint = date != null && (date == selectedRange?.start || date == selectedRange?.end)

                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                    ) {
                        CalendarDayCell(
                            dayNumber = date?.dayOfMonth,
                            isToday = date == today,
                            isCurrentMonth = inMonth,
                            isInRange = isInRange,
                            isRangeEndpoint = isEndpoint,
                            hasNote = note?.hasText == true,
                            hasAlarm = note?.hasAlarm == true,
                            shifts = dayShifts,
                            onClick = {
                                if (date != null && inMonth) {
                                    onDayClick(date.year, date.monthValue, date.dayOfMonth)
                                }
                            },
                            onLongClick = {
                                if (date != null && inMonth) {
                                    onDayLongClick(date.year, date.monthValue, date.dayOfMonth)
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}
