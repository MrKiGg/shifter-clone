package com.lrhsoft.shifterclone.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lrhsoft.shifterclone.model.DayAssignment
import com.lrhsoft.shifterclone.model.DayNote
import com.lrhsoft.shifterclone.model.Shift
import com.lrhsoft.shifterclone.model.WorkContract
import com.lrhsoft.shifterclone.ui.components.CalendarDayCell
import java.time.LocalDate
import java.time.YearMonth

@Composable
fun MonthPane(
    state: CalendarUiState,
    viewModel: CalendarViewModel,
    modifier: Modifier = Modifier
) {
    var dragAccum by remember { mutableFloatStateOf(0f) }
    var displayedMonth by remember { mutableStateOf(state.currentMonth) }
    val rotationY = remember { Animatable(0f) }
    val density = LocalDensity.current.density
    val contracts by viewModel.contracts.collectAsState()

    LaunchedEffect(state.currentMonth) {
        if (state.currentMonth == displayedMonth) return@LaunchedEffect
        val forward = state.currentMonth > displayedMonth
        rotationY.animateTo(if (forward) -90f else 90f, tween(180))
        displayedMonth = state.currentMonth
        rotationY.snapTo(if (forward) 90f else -90f)
        rotationY.animateTo(0f, tween(220))
    }
    LaunchedEffect(state.topTab) {
        if (state.topTab == TopTab.MONTH) {
            displayedMonth = state.currentMonth
            rotationY.snapTo(0f)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White)
            .pointerInput(state.currentMonth) {
                detectHorizontalDragGestures(
                    onDragEnd = {
                        when {
                            dragAccum < -80f -> viewModel.nextMonth()
                            dragAccum > 80f -> viewModel.previousMonth()
                        }
                        dragAccum = 0f
                    },
                    onDragCancel = { dragAccum = 0f },
                    onHorizontalDrag = { _, a -> dragAccum += a }
                )
            }
    ) {
        MonthHeader(
            yearMonth = displayedMonth,
            onPrevious = { viewModel.previousMonth() },
            onNext = { viewModel.nextMonth() }
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 4.dp)
                .graphicsLayer {
                    this.rotationY = rotationY.value
                    cameraDistance = 18f * density
                    transformOrigin = TransformOrigin(0.5f, 0.5f)
                    val absRot = kotlin.math.abs(rotationY.value)
                    alpha = 1f - (absRot / 90f) * 0.15f
                    scaleX = 1f - (absRot / 90f) * 0.04f
                    scaleY = 1f - (absRot / 90f) * 0.04f
                }
                .border(2.dp, Color.Black)
                .background(Color.White)
        ) {
            MonthGrid(
                modifier = Modifier.fillMaxSize(),
                yearMonth = displayedMonth,
                assignments = state.assignments,
                shifts = state.shifts,
                notes = state.notes,
                selectedRange = state.selectedRange,
                contracts = contracts,
                onDayClick = { y, m, d -> viewModel.onDayClicked(y, m, d) },
                onDayLongClick = { y, m, d -> viewModel.onDayLongPressed(y, m, d) }
            )
        }
    }
}

@Composable
internal fun MonthGrid(
    yearMonth: YearMonth,
    assignments: Map<String, DayAssignment>,
    shifts: List<Shift>,
    notes: Map<String, DayNote>,
    selectedRange: DateRange?,
    contracts: List<WorkContract> = emptyList(),
    onDayClick: (Int, Int, Int) -> Unit,
    onDayLongClick: (Int, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val shiftMap = shifts.associateBy { it.id }
    val monthStart = yearMonth.atDay(1)
    val monthEnd = yearMonth.atEndOfMonth()
    val contractsInMonth = contracts.filter { c ->
        try {
            val start = LocalDate.parse(c.startDate)
            val end = c.endDate?.let { LocalDate.parse(it) }
            !start.isAfter(monthEnd) && (end == null || !end.isBefore(monthStart))
        } catch (_: Exception) {
            false
        }
    }
    val showContractHighlight = contractsInMonth.size > 1
    val today = LocalDate.now()
    val activeContract = if (showContractHighlight) {
        contractsInMonth.firstOrNull { it.contains(today) }
            ?: contractsInMonth.firstOrNull { it.contains(monthStart) }
            ?: contractsInMonth.firstOrNull()
    } else null

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
    val weekDays = listOf("LUN", "MAR", "MIE", "JUE", "VIE", "SAB", "DOM")
    val weeks = cells.chunked(7)
    Column(modifier = modifier) {
        Row(Modifier.fillMaxWidth().padding(bottom = 2.dp)) {
            weekDays.forEach { d ->
                Text(
                    d,
                    modifier = Modifier.weight(1f),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp,
                    color = Color(0xFF616161)
                )
            }
        }
        weeks.forEach { week ->
            Row(Modifier.weight(1f).fillMaxWidth()) {
                week.forEach { date ->
                    val inMonth = date != null && date.month == yearMonth.month
                    val key = if (date != null) "${date.year}-${date.monthValue}-${date.dayOfMonth}" else ""
                    val dayShifts = if (inMonth) {
                        assignments[key]?.shiftIds?.mapNotNull { shiftMap[it] }.orEmpty()
                    } else emptyList()
                    val note = if (inMonth) notes[key] else null
                    val isInRange = date != null && selectedRange?.contains(date) == true
                    val isEndpoint = date != null &&
                        (date == selectedRange?.start || date == selectedRange?.end)
                    val inActiveContract = showContractHighlight &&
                        inMonth && date != null && activeContract?.contains(date) == true
                    Box(Modifier.weight(1f).fillMaxHeight()) {
                        CalendarDayCell(
                            dayNumber = date?.dayOfMonth,
                            isToday = date == today,
                            isCurrentMonth = inMonth,
                            isInRange = isInRange,
                            isRangeEndpoint = isEndpoint,
                            isInActiveContract = inActiveContract,
                            hasNote = note?.hasText == true,
                            hasAlarm = note?.hasAlarm == true,
                            noteText = note?.text?.takeIf { it.isNotBlank() },
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
