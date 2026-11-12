package com.lrhsoft.shifterclone.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Today
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lrhsoft.shifterclone.data.StatsCalculator
import com.lrhsoft.shifterclone.model.DayNote
import com.lrhsoft.shifterclone.model.Shift
import com.lrhsoft.shifterclone.ui.components.ApplyPatternDialog
import com.lrhsoft.shifterclone.ui.components.CalendarDayCell
import com.lrhsoft.shifterclone.ui.components.DayDetailSheet
import com.lrhsoft.shifterclone.ui.components.ExportSheet
import com.lrhsoft.shifterclone.ui.components.PatternBuilderDialog
import com.lrhsoft.shifterclone.ui.components.PatternPickStartBanner
import com.lrhsoft.shifterclone.ui.components.ProfileSheet
import com.lrhsoft.shifterclone.ui.components.ShiftEditorDialog
import com.lrhsoft.shifterclone.ui.components.ShiftsManagerScreen
import com.lrhsoft.shifterclone.ui.components.StatsSheet
import com.lrhsoft.shifterclone.ui.components.YearOverview
import com.lrhsoft.shifterclone.ui.components.YearSummary
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun CalendarScreen(viewModel: CalendarViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val showProfile by viewModel.showProfile.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()
    val contracts by viewModel.contracts.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text("Calendario de turnos", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF37474F),
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White
                ),
                actions = {
                    IconButton(onClick = { viewModel.openProfile() }) {
                        Icon(Icons.Default.Person, contentDescription = "Perfil")
                    }
                    IconButton(onClick = { viewModel.openStats() }) {
                        Icon(Icons.Default.BarChart, contentDescription = "Estadisticas")
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
        bottomBar = {
            if (state.topTab == TopTab.MONTH) Column {
                if (state.editMode == EditMode.PAINT) {
                    PaintPaletteBar(
                        shifts = state.shifts,
                        selectedId = state.selectedShiftId,
                        isEraser = state.isEraserSelected,
                        onSelectShift = { viewModel.selectShift(it) },
                        onSelectEraser = { viewModel.selectEraser() }
                    )
                }
                if (state.editMode == EditMode.EDIT) {
                    RangeActions(
                        hasRange = state.selectedRange != null,
                        rangeSize = state.selectedRange?.size ?: 0,
                        onApply = { viewModel.applyShiftToRange() },
                        onClear = { viewModel.clearRangeShifts() },
                        onRepeat = { viewModel.repeatPattern() },
                        onClearSelection = { viewModel.clearRange() }
                    )
                }
                BottomModeBar(
                    mode = state.editMode,
                    patternActive = state.patternAwaitingStart || state.showPatternBuilder,
                    onPaint = { viewModel.togglePaintMode() },
                    onEdit = { viewModel.toggleEditMode() },
                    onPattern = { viewModel.openPatternBuilder() },
                    onShifts = { viewModel.openShiftsManager() }
                )
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).background(Color.White)
        ) {
            TopTabBar(
                selected = state.topTab,
                currentMonth = state.currentMonth,
                year = state.yearViewYear,
                onSelect = { viewModel.setTopTab(it) }
            )
            if (state.patternAwaitingStart) {
                PatternPickStartBanner(
                    patternLength = state.pendingPattern.size,
                    onCancel = { viewModel.cancelPatternAwaiting() }
                )
            }
            when (state.topTab) {
                TopTab.MONTH -> MonthPane(state, viewModel, Modifier.weight(1f))
                TopTab.YEAR -> YearOverview(
                    year = state.yearViewYear,
                    assignments = state.assignments,
                    shifts = state.shifts,
                    onPreviousYear = { viewModel.previousYearView() },
                    onNextYear = { viewModel.nextYearView() },
                    onMonthClick = { y, m -> viewModel.openMonthFromYear(y, m) },
                    modifier = Modifier.weight(1f)
                )
                TopTab.SUMMARY -> {
                    val from = LocalDate.of(state.yearViewYear, 1, 1)
                    val to = LocalDate.of(state.yearViewYear, 12, 31)
                    val summary = StatsCalculator.calculate(from, to, state.assignments, state.shifts)
                    YearSummary(
                        year = state.yearViewYear,
                        summary = summary,
                        onPreviousYear = { viewModel.previousYearView() },
                        onNextYear = { viewModel.nextYearView() },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    if (state.showShiftsManager) {
        ShiftsManagerScreen(
            shifts = state.shifts,
            onDismiss = { viewModel.dismissShiftsManager() },
            onEdit = { viewModel.openEditShift(it) },
            onDuplicate = { viewModel.duplicateShift(it) },
            onDelete = { viewModel.deleteShift(it) },
            onCreate = { viewModel.openCreateShift() }
        )
    }
    if (state.showShiftEditor) {
        ShiftEditorDialog(
            shift = state.editingShift,
            onDismiss = { viewModel.dismissShiftEditor() },
            onSave = { viewModel.saveShift(it) },
            onDelete = if (state.editingShift != null) {
                { shift: Shift -> viewModel.deleteShift(shift) }
            } else null
        )
    }
    state.detailDate?.let { date ->
        val note = state.noteFor(date)
        val dayShifts = state.assignments["${date.year}-${date.monthValue}-${date.dayOfMonth}"]
            ?.shiftIds?.mapNotNull { id -> state.shifts.find { it.id == id } }.orEmpty()
        DayDetailSheet(
            date = date, shifts = dayShifts, note = note,
            onDismiss = { viewModel.dismissDayDetail() },
            onSave = { viewModel.saveNote(it) },
            onDelete = { viewModel.deleteNote(date.year, date.monthValue, date.dayOfMonth) }
        )
    }
    if (state.showStats) {
        val summary = viewModel.computeStats(state)
        StatsSheet(
            summary = summary, period = state.statsPeriod, yearMonth = state.statsYearMonth,
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
            onShareIcs = { viewModel.buildIcsShareIntent()?.let { } },
            onDismiss = { viewModel.dismissExport() }
        )
    }
    if (state.showPatternBuilder) {
        PatternBuilderDialog(
            shifts = state.shifts,
            onDismiss = { viewModel.dismissPatternBuilder() },
            onConfirm = { viewModel.confirmPatternSequence(it) }
        )
    }
    if (state.showApplyPattern && state.patternStartDate != null) {
        ApplyPatternDialog(
            startDate = state.patternStartDate!!,
            patternLength = state.pendingPattern.size,
            onDismiss = { viewModel.dismissApplyPattern() },
            onApply = { mode, count, until -> viewModel.applyPendingPattern(mode.name, count, until) }
        )
    }
    if (showProfile) {
        ProfileSheet(
            profile = userProfile,
            contracts = contracts,
            monthEstimate = viewModel.estimatePayroll(state),
            onSaveProfile = { viewModel.saveProfile(it) },
            onSaveContracts = { viewModel.saveContracts(it) },
            onDismiss = { viewModel.dismissProfile() }
        )
    }
}

@Composable
internal fun MonthHeader(yearMonth: YearMonth, onPrevious: () -> Unit, onNext: () -> Unit) {
    val monthName = yearMonth.month.getDisplayName(TextStyle.FULL, Locale("es", "ES")).replaceFirstChar { it.uppercase() }
    Row(
        modifier = Modifier.fillMaxWidth().background(Color.White).padding(horizontal = 4.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Mes anterior", tint = Color(0xFF212121))
        }
        Text("$monthName ${yearMonth.year}".uppercase(Locale("es", "ES")), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF212121))
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Mes siguiente", tint = Color(0xFF212121))
        }
    }
}

@Composable
internal fun BottomModeBar(
    mode: EditMode, patternActive: Boolean = false,
    onPaint: () -> Unit, onEdit: () -> Unit, onPattern: () -> Unit, onShifts: () -> Unit
) {
    val selectedColor = Color(0xFFD32F2F)
    val unselectedColor = Color(0xFF757575)
    NavigationBar(containerColor = Color(0xFFFAFAFA), contentColor = selectedColor, tonalElevation = 4.dp) {
        NavigationBarItem(
            selected = mode == EditMode.PAINT, onClick = onPaint,
            icon = { Icon(Icons.Default.Brush, contentDescription = null) },
            label = { Text("PINTAR", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor, selectedTextColor = selectedColor,
                unselectedIconColor = unselectedColor, unselectedTextColor = unselectedColor,
                indicatorColor = selectedColor.copy(alpha = 0.12f)
            )
        )
        NavigationBarItem(
            selected = mode == EditMode.EDIT, onClick = onEdit,
            icon = { Icon(Icons.Default.Edit, contentDescription = null) },
            label = { Text("EDITAR", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor, selectedTextColor = selectedColor,
                unselectedIconColor = unselectedColor, unselectedTextColor = unselectedColor,
                indicatorColor = selectedColor.copy(alpha = 0.12f)
            )
        )
        NavigationBarItem(
            selected = patternActive, onClick = onPattern,
            icon = { Icon(Icons.Default.ContentCopy, contentDescription = null) },
            label = { Text("PATRON", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor, selectedTextColor = selectedColor,
                unselectedIconColor = unselectedColor, unselectedTextColor = unselectedColor,
                indicatorColor = selectedColor.copy(alpha = 0.12f)
            )
        )
        NavigationBarItem(
            selected = false, onClick = onShifts,
            icon = { Icon(Icons.Default.List, contentDescription = null) },
            label = { Text("TURNOS", fontWeight = FontWeight.Bold, fontSize = 11.sp) },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = selectedColor, selectedTextColor = selectedColor,
                unselectedIconColor = unselectedColor, unselectedTextColor = unselectedColor,
                indicatorColor = selectedColor.copy(alpha = 0.12f)
            )
        )
    }
}

@Composable
internal fun PaintPaletteBar(
    shifts: List<Shift>, selectedId: String?, isEraser: Boolean,
    onSelectShift: (String) -> Unit, onSelectEraser: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().background(Color(0xFFECEFF1))
            .horizontalScroll(rememberScrollState()).padding(horizontal = 10.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.clip(RoundedCornerShape(20.dp))
                .background(if (isEraser) Color(0xFFD32F2F) else Color(0xFFB0BEC5))
                .border(if (isEraser) 2.5.dp else 0.dp, Color.Black, RoundedCornerShape(20.dp))
                .clickable(onClick = onSelectEraser).padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Text("Borrar turno", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
        }
        shifts.forEach { shift ->
            val selected = !isEraser && shift.id == selectedId
            Box(
                modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(shift.toComposeColor())
                    .border(if (selected) 2.5.dp else 0.dp, Color.Black, RoundedCornerShape(20.dp))
                    .clickable { onSelectShift(shift.id) }.padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text("${shift.abbreviation} · ${shift.name}", color = shift.toTextComposeColor(),
                    fontSize = 13.sp, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
internal fun RangeActions(
    hasRange: Boolean, rangeSize: Int,
    onApply: () -> Unit, onClear: () -> Unit, onRepeat: () -> Unit, onClearSelection: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(Color(0xFFECEFF1)).padding(horizontal = 12.dp, vertical = 8.dp)) {
        if (hasRange) {
            Text("$rangeSize dia(s) seleccionado(s)", style = MaterialTheme.typography.labelMedium,
                color = Color(0xFFD32F2F), modifier = Modifier.padding(bottom = 4.dp))
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = onApply, enabled = hasRange, modifier = Modifier.weight(1f)) {
                Text("Aplicar", fontSize = 12.sp)
            }
            OutlinedButton(onClick = onClear, enabled = hasRange, modifier = Modifier.weight(1f)) {
                Text("Borrar", fontSize = 12.sp)
            }
            OutlinedButton(onClick = onRepeat, enabled = hasRange, modifier = Modifier.weight(1f)) {
                Icon(Icons.Default.ContentCopy, null, Modifier.size(14.dp))
                Spacer(Modifier.width(2.dp))
                Text("Repetir", fontSize = 12.sp)
            }
            IconButton(onClick = onClearSelection, enabled = hasRange) {
                Icon(Icons.Default.Clear, contentDescription = "Quitar seleccion")
            }
        }
    }
}

@Composable
internal fun MonthGrid(
    yearMonth: YearMonth,
    assignments: Map<String, com.lrhsoft.shifterclone.model.DayAssignment>,
    shifts: List<Shift>,
    notes: Map<String, DayNote>,
    selectedRange: DateRange?,
    onDayClick: (Int, Int, Int) -> Unit,
    onDayLongClick: (Int, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shiftMap = shifts.associateBy { it.id }
    val first = yearMonth.atDay(1)
    val startOffset = (first.dayOfWeek.value - 1) % 7
    val daysInMonth = yearMonth.lengthOfMonth()
    val cells = mutableListOf<LocalDate?>()
    val prev = yearMonth.minusMonths(1)
    val daysInPrev = prev.lengthOfMonth()
    for (i in startOffset downTo 1) cells.add(prev.atDay(daysInPrev - i + 1))
    for (d in 1..daysInMonth) cells.add(yearMonth.atDay(d))
    val remaining = (7 - cells.size % 7) % 7
    for (d in 1..remaining) cells.add(yearMonth.plusMonths(1).atDay(d))
    val today = LocalDate.now()
    val weekDays = listOf("LUN", "MAR", "MIE", "JUE", "VIE", "SAB", "DOM")
    val weeks = cells.chunked(7)
    Column(modifier = modifier) {
        Row(Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
            weekDays.forEach { d ->
                Text(d, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = Color(0xFF616161))
            }
        }
        weeks.forEach { week ->
            Row(Modifier.weight(1f).fillMaxWidth()) {
                week.forEach { date ->
                    val inMonth = date != null && date.month == yearMonth.month
                    val key = if (date != null) "${date.year}-${date.monthValue}-${date.dayOfMonth}" else ""
                    val dayShifts = if (inMonth) assignments[key]?.shiftIds?.mapNotNull { shiftMap[it] }.orEmpty() else emptyList()
                    val note = if (inMonth) notes[key] else null
                    val isInRange = date != null && selectedRange?.contains(date) == true
                    val isEndpoint = date != null && (date == selectedRange?.start || date == selectedRange?.end)
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        CalendarDayCell(
                            dayNumber = date?.dayOfMonth,
                            isToday = date == today,
                            isCurrentMonth = inMonth,
                            isInRange = isInRange,
                            isRangeEndpoint = isEndpoint,
                            hasNote = note?.hasText == true,
                            hasAlarm = note?.hasAlarm == true,
                            noteText = note?.text?.takeIf { it.isNotBlank() },
                            shifts = dayShifts,
                            onClick = { if (date != null && inMonth) onDayClick(date.year, date.monthValue, date.dayOfMonth) },
                            onLongClick = { if (date != null && inMonth) onDayLongClick(date.year, date.monthValue, date.dayOfMonth) }
                        )
                    }
                }
            }
        }
    }
}
