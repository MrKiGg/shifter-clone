package com.lrhsoft.shifterclone.ui.screens

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lrhsoft.shifterclone.alarm.AlarmScheduler
import com.lrhsoft.shifterclone.widget.WidgetUpdater
import com.lrhsoft.shifterclone.export.CalendarExporter
import com.lrhsoft.shifterclone.export.DeviceCalendar
import com.lrhsoft.shifterclone.ui.components.ExportRange
import android.content.Intent

import com.lrhsoft.shifterclone.data.ShiftRepository
import com.lrhsoft.shifterclone.data.StatsCalculator
import com.lrhsoft.shifterclone.model.StatsSummary
import com.lrhsoft.shifterclone.ui.components.StatsPeriod
import com.lrhsoft.shifterclone.model.DayAssignment
import com.lrhsoft.shifterclone.model.DayNote
import com.lrhsoft.shifterclone.model.Shift
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth

enum class EditMode {
    VIEW,
    PAINT,
    EDIT
}

data class DateRange(
    val start: LocalDate,
    val end: LocalDate
) {
    fun contains(date: LocalDate): Boolean =
        !date.isBefore(start) && !date.isAfter(end)

    fun days(): List<LocalDate> {
        val list = mutableListOf<LocalDate>()
        var d = start
        while (!d.isAfter(end)) {
            list.add(d)
            d = d.plusDays(1)
        }
        return list
    }

    val size: Int get() = days().size
}

data class CalendarUiState(
    val currentMonth: YearMonth = YearMonth.now(),
    val shifts: List<Shift> = emptyList(),
    val assignments: Map<String, DayAssignment> = emptyMap(),
    val notes: Map<String, DayNote> = emptyMap(),
    val selectedShiftId: String? = null,
    val isEraserSelected: Boolean = false,
    val editMode: EditMode = EditMode.VIEW,
    val rangeStart: LocalDate? = null,
    val rangeEnd: LocalDate? = null,
    val showShiftEditor: Boolean = false,
    val editingShift: Shift? = null,
    val showShiftsManager: Boolean = false,
    val detailDate: LocalDate? = null,
    val showStats: Boolean = false,
    val statsPeriod: StatsPeriod = StatsPeriod.MONTH,
    val statsYearMonth: YearMonth = YearMonth.now(),
    val showExport: Boolean = false,
    val exportCalendars: List<DeviceCalendar> = emptyList(),
    val selectedCalendarId: Long? = null,
    val exportRange: ExportRange = ExportRange.CURRENT_MONTH,
    val exportIncludeRest: Boolean = false,
    val exportLoading: Boolean = false,
    val exportMessage: String? = null
) {
    val selectedRange: DateRange?
        get() {
            val s = rangeStart ?: return null
            val e = rangeEnd ?: return DateRange(s, s)
            return if (s.isBefore(e) || s == e) DateRange(s, e) else DateRange(e, s)
        }

    fun noteFor(date: LocalDate): DayNote? =
        notes[DayNote.key(date.year, date.monthValue, date.dayOfMonth)]
}

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val repo = ShiftRepository(application)
    private val alarmScheduler = AlarmScheduler(application)

    private fun refreshWidgets() {
        WidgetUpdater.refreshAll(getApplication())
    }

    private val _currentMonth = MutableStateFlow(YearMonth.now())
    private val _selectedShiftId = MutableStateFlow<String?>(null)
    private val _isEraserSelected = MutableStateFlow(false)
    private val _editMode = MutableStateFlow(EditMode.VIEW)
    private val _rangeStart = MutableStateFlow<LocalDate?>(null)
    private val _rangeEnd = MutableStateFlow<LocalDate?>(null)
    private val _showShiftEditor = MutableStateFlow(false)
    private val _editingShift = MutableStateFlow<Shift?>(null)
    private val _showShiftsManager = MutableStateFlow(false)
    private val _detailDate = MutableStateFlow<LocalDate?>(null)
    private val _showStats = MutableStateFlow(false)
    private val _statsPeriod = MutableStateFlow(StatsPeriod.MONTH)
    private val _statsYearMonth = MutableStateFlow(YearMonth.now())
    private val _showExport = MutableStateFlow(false)
    private val _exportCalendars = MutableStateFlow<List<DeviceCalendar>>(emptyList())
    private val _selectedCalendarId = MutableStateFlow<Long?>(null)
    private val _exportRange = MutableStateFlow(ExportRange.CURRENT_MONTH)
    private val _exportIncludeRest = MutableStateFlow(false)
    private val _exportLoading = MutableStateFlow(false)
    private val _exportMessage = MutableStateFlow<String?>(null)

    private data class ModeState(
        val selectedShiftId: String?,
        val isEraserSelected: Boolean,
        val editMode: EditMode,
        val rangeStart: LocalDate?,
        val rangeEnd: LocalDate?,
        val showShiftsManager: Boolean
    )

    private data class CoreState(
        val month: YearMonth,
        val shifts: List<Shift>,
        val assignments: Map<String, DayAssignment>,
        val notes: Map<String, DayNote>
    )

    val uiState: StateFlow<CalendarUiState> = combine(
        combine(_currentMonth, repo.shiftsFlow, repo.assignmentsFlow, repo.notesFlow) { month, shifts, assignments, notes ->
            CoreState(month, shifts, assignments, notes)
        },
        combine(
            combine(_selectedShiftId, _isEraserSelected, _editMode) { a, b, c -> Triple(a, b, c) },
            combine(_rangeStart, _rangeEnd, _showShiftsManager) { a, b, c -> Triple(a, b, c) }
        ) { t1, t2 ->
            ModeState(t1.first, t1.second, t1.third, t2.first, t2.second, t2.third)
        },
        combine(
            combine(_showShiftEditor, _editingShift, _detailDate) { show, editing, detail ->
                Triple(show, editing, detail)
            },
            combine(_showStats, _statsPeriod, _statsYearMonth) { showStats, period, ym ->
                Triple(showStats, period, ym)
            },
            combine(
                combine(_showExport, _exportCalendars, _selectedCalendarId) { a, b, c -> Triple(a, b, c) },
                combine(_exportRange, _exportIncludeRest, _exportLoading, _exportMessage) { r, ir, l, m ->
                    listOf(r, ir, l, m)
                }
            ) { t, extra -> t to extra }
        ) { editor, stats, exportPack ->
            Triple(editor, stats, exportPack)
        }
    ) { core, modeState, pack ->
        val (editor, stats, exportPack) = pack
        val (showEditor, editing, detail) = editor
        val (showStats, period, statsYm) = stats
        val (exportTriple, exportExtra) = exportPack
        val (showExport, calendars, selectedCalId) = exportTriple
        val exportRange = exportExtra[0] as ExportRange
        val includeRest = exportExtra[1] as Boolean
        val exportLoading = exportExtra[2] as Boolean
        val exportMessage = exportExtra[3] as String?
        CalendarUiState(
            currentMonth = core.month,
            shifts = core.shifts,
            assignments = core.assignments,
            notes = core.notes,
            selectedShiftId = modeState.selectedShiftId ?: core.shifts.firstOrNull()?.id,
            isEraserSelected = modeState.isEraserSelected,
            editMode = modeState.editMode,
            rangeStart = modeState.rangeStart,
            rangeEnd = modeState.rangeEnd,
            showShiftEditor = showEditor,
            editingShift = editing,
            showShiftsManager = modeState.showShiftsManager,
            detailDate = detail,
            showStats = showStats,
            statsPeriod = period,
            statsYearMonth = statsYm,
            showExport = showExport,
            exportCalendars = calendars,
            selectedCalendarId = selectedCalId,
            exportRange = exportRange,
            exportIncludeRest = includeRest,
            exportLoading = exportLoading,
            exportMessage = exportMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = CalendarUiState()
    )

    fun previousMonth() { _currentMonth.value = _currentMonth.value.minusMonths(1) }
    fun nextMonth() { _currentMonth.value = _currentMonth.value.plusMonths(1) }
    fun goToToday() { _currentMonth.value = YearMonth.now() }

    fun selectShift(shiftId: String) {
        _selectedShiftId.value = shiftId
        _isEraserSelected.value = false
    }

    fun selectEraser() {
        _isEraserSelected.value = true
        _selectedShiftId.value = null
    }

    fun setEditMode(mode: EditMode) {
        _editMode.value = mode
        if (mode != EditMode.EDIT) clearRange()
    }

    /** PINTAR: activa o desactiva la barra de turnos */
    fun togglePaintMode() {
        if (_editMode.value == EditMode.PAINT) {
            _editMode.value = EditMode.VIEW
        } else {
            _editMode.value = EditMode.PAINT
            clearRange()
        }
    }

    /** EDITAR: activa o desactiva el modo rango */
    fun toggleEditMode() {
        if (_editMode.value == EditMode.EDIT) {
            _editMode.value = EditMode.VIEW
            clearRange()
        } else {
            _editMode.value = EditMode.EDIT
        }
    }

    fun openShiftsManager() {
        _showShiftsManager.value = true
    }

    fun dismissShiftsManager() {
        _showShiftsManager.value = false
    }

    fun onDayClicked(year: Int, month: Int, day: Int) {
        val date = LocalDate.of(year, month, day)
        when (_editMode.value) {
            EditMode.PAINT -> paintDay(date)
            EditMode.EDIT -> selectRangeDay(date)
            EditMode.VIEW -> _detailDate.value = date
        }
    }

    fun onDayLongPressed(year: Int, month: Int, day: Int) {
        _detailDate.value = LocalDate.of(year, month, day)
    }

    fun dismissDayDetail() {
        _detailDate.value = null
    }

    private fun paintDay(date: LocalDate) {
        if (_isEraserSelected.value) {
            viewModelScope.launch {
                repo.setShiftOnDay(date.year, date.monthValue, date.dayOfMonth, null)
                refreshWidgets()
            }
            return
        }
        val selectedId = _selectedShiftId.value ?: return
        val state = uiState.value
        viewModelScope.launch {
            val key = DayAssignment.key(date.year, date.monthValue, date.dayOfMonth)
            val current = state.assignments[key]
            if (current != null && current.shiftIds.size == 1 && current.shiftIds[0] == selectedId) {
                repo.setShiftOnDay(date.year, date.monthValue, date.dayOfMonth, null)
                refreshWidgets()
            } else {
                repo.setShiftOnDay(date.year, date.monthValue, date.dayOfMonth, selectedId, replace = true)
                refreshWidgets()
            }
        }
    }

    private fun selectRangeDay(date: LocalDate) {
        val start = _rangeStart.value
        val end = _rangeEnd.value
        when {
            start == null -> {
                _rangeStart.value = date
                _rangeEnd.value = null
            }
            end == null -> _rangeEnd.value = date
            else -> {
                _rangeStart.value = date
                _rangeEnd.value = null
            }
        }
    }

    fun clearRange() {
        _rangeStart.value = null
        _rangeEnd.value = null
    }

    fun applyShiftToRange() {
        val range = uiState.value.selectedRange ?: return
        val shiftId = _selectedShiftId.value ?: return
        viewModelScope.launch {
            val updates = range.days().associate { d ->
                Triple(d.year, d.monthValue, d.dayOfMonth) to listOf(shiftId)
            }
            repo.applyBulk(updates)
            refreshWidgets()
        }
    }

    fun clearRangeShifts() {
        val range = uiState.value.selectedRange ?: return
        viewModelScope.launch {
            val updates = range.days().associate { d ->
                Triple(d.year, d.monthValue, d.dayOfMonth) to emptyList<String>()
            }
            repo.applyBulk(updates)
            refreshWidgets()
        }
    }

    fun repeatPattern(cycles: Int = -1) {
        val range = uiState.value.selectedRange ?: return
        val state = uiState.value
        val patternDays = range.days()
        if (patternDays.isEmpty()) return

        val pattern: List<List<String>> = patternDays.map { d ->
            val key = DayAssignment.key(d.year, d.monthValue, d.dayOfMonth)
            state.assignments[key]?.shiftIds ?: emptyList()
        }
        val patternLen = pattern.size
        val startAfter = range.end.plusDays(1)
        val endDate: LocalDate = if (cycles < 0) {
            YearMonth.from(range.end).atEndOfMonth()
        } else {
            startAfter.plusDays((patternLen * cycles).toLong() - 1)
        }
        if (startAfter.isAfter(endDate)) return

        viewModelScope.launch {
            val updates = mutableMapOf<Triple<Int, Int, Int>, List<String>>()
            var cursor = startAfter
            var index = 0
            while (!cursor.isAfter(endDate)) {
                updates[Triple(cursor.year, cursor.monthValue, cursor.dayOfMonth)] = pattern[index % patternLen]
                cursor = cursor.plusDays(1)
                index++
            }
            if (updates.isNotEmpty()) repo.applyBulk(updates)
            refreshWidgets()
        }
    }

    fun repeatPatternMonths(months: Int = 1) {
        val range = uiState.value.selectedRange ?: return
        val state = uiState.value
        val patternDays = range.days()
        if (patternDays.isEmpty()) return

        val pattern: List<List<String>> = patternDays.map { d ->
            val key = DayAssignment.key(d.year, d.monthValue, d.dayOfMonth)
            state.assignments[key]?.shiftIds ?: emptyList()
        }
        val patternLen = pattern.size
        val startAfter = range.end.plusDays(1)
        val endDate = range.end.plusMonths(months.toLong())

        viewModelScope.launch {
            val updates = mutableMapOf<Triple<Int, Int, Int>, List<String>>()
            var cursor = startAfter
            var index = 0
            while (!cursor.isAfter(endDate)) {
                updates[Triple(cursor.year, cursor.monthValue, cursor.dayOfMonth)] = pattern[index % patternLen]
                cursor = cursor.plusDays(1)
                index++
            }
            if (updates.isNotEmpty()) repo.applyBulk(updates)
            refreshWidgets()
        }
    }

    fun saveNote(note: DayNote) {
        viewModelScope.launch {
            repo.saveNote(note)
            if (note.hasAlarm) {
                alarmScheduler.schedule(note)
            } else {
                alarmScheduler.cancel(note)
            }
            dismissDayDetail()
        }
    }

    fun deleteNote(year: Int, month: Int, day: Int) {
        viewModelScope.launch {
            val key = DayNote.key(year, month, day)
            alarmScheduler.cancel(key)
            repo.deleteNote(year, month, day)
            dismissDayDetail()
        }
    }

    fun openCreateShift() {
        _editingShift.value = null
        _showShiftEditor.value = true
    }

    fun openEditShift(shift: Shift) {
        _editingShift.value = shift
        _showShiftEditor.value = true
    }

    fun dismissShiftEditor() {
        _showShiftEditor.value = false
        _editingShift.value = null
    }

    fun saveShift(shift: Shift) {
        viewModelScope.launch {
            val current = uiState.value.shifts.toMutableList()
            val index = current.indexOfFirst { it.id == shift.id }
            if (index >= 0) current[index] = shift else current.add(shift)
            repo.saveShifts(current)
            refreshWidgets()
            if (index < 0) _selectedShiftId.value = shift.id
            dismissShiftEditor()
        }
    }

    fun deleteShift(shift: Shift) {
        viewModelScope.launch {
            val current = uiState.value.shifts.filter { it.id != shift.id }
            repo.saveShifts(current)
            refreshWidgets()
            if (_selectedShiftId.value == shift.id) {
                _selectedShiftId.value = current.firstOrNull()?.id
            }
            dismissShiftEditor()
        }
    }

    fun duplicateShift(shift: Shift) {
        val copy = shift.copy(
            id = java.util.UUID.randomUUID().toString(),
            name = "${shift.name} (copia)"
        )
        viewModelScope.launch {
            val current = uiState.value.shifts.toMutableList()
            current.add(copy)
            repo.saveShifts(current)
            refreshWidgets()
        }
    }

    fun openStats() {
        _statsYearMonth.value = _currentMonth.value
        _showStats.value = true
    }

    fun dismissStats() {
        _showStats.value = false
    }

    fun setStatsPeriod(period: StatsPeriod) {
        _statsPeriod.value = period
    }

    fun statsPrevious() {
        val ym = _statsYearMonth.value
        _statsYearMonth.value = if (_statsPeriod.value == StatsPeriod.MONTH) {
            ym.minusMonths(1)
        } else {
            ym.minusYears(1)
        }
    }

    fun statsNext() {
        val ym = _statsYearMonth.value
        _statsYearMonth.value = if (_statsPeriod.value == StatsPeriod.MONTH) {
            ym.plusMonths(1)
        } else {
            ym.plusYears(1)
        }
    }

    fun computeStats(state: CalendarUiState = uiState.value): StatsSummary {
        val ym = state.statsYearMonth
        val (from, to) = if (state.statsPeriod == StatsPeriod.MONTH) {
            ym.atDay(1) to ym.atEndOfMonth()
        } else {
            LocalDate.of(ym.year, 1, 1) to LocalDate.of(ym.year, 12, 31)
        }
        return StatsCalculator.calculate(from, to, state.assignments, state.shifts)
    }

    fun openExport() {
        _exportMessage.value = null
        _showExport.value = true
        refreshCalendarList()
    }

    fun dismissExport() {
        _showExport.value = false
        _exportMessage.value = null
        _exportLoading.value = false
    }

    fun refreshCalendarList() {
        viewModelScope.launch {
            val exporter = CalendarExporter(getApplication())
            val list = exporter.listCalendars()
            _exportCalendars.value = list
            if (_selectedCalendarId.value == null) {
                _selectedCalendarId.value = list.firstOrNull()?.id
            }
        }
    }

    fun selectExportCalendar(id: Long) {
        _selectedCalendarId.value = id
    }

    fun setExportRange(range: ExportRange) {
        _exportRange.value = range
    }

    fun setExportIncludeRest(include: Boolean) {
        _exportIncludeRest.value = include
    }

    private fun exportBounds(): Pair<java.time.LocalDate, java.time.LocalDate> {
        val ym = uiState.value.currentMonth
        return when (_exportRange.value) {
            ExportRange.CURRENT_MONTH -> ym.atDay(1) to ym.atEndOfMonth()
            ExportRange.CURRENT_YEAR -> java.time.LocalDate.of(ym.year, 1, 1) to java.time.LocalDate.of(ym.year, 12, 31)
        }
    }

    fun exportToDeviceCalendar() {
        val calId = _selectedCalendarId.value ?: return
        val state = uiState.value
        viewModelScope.launch {
            _exportLoading.value = true
            _exportMessage.value = null
            try {
                val (from, to) = exportBounds()
                val exporter = CalendarExporter(getApplication())
                val result = exporter.exportToCalendar(
                    calendarId = calId,
                    from = from,
                    to = to,
                    assignments = state.assignments,
                    shifts = state.shifts,
                    includeRest = _exportIncludeRest.value
                )
                _exportMessage.value = result.message
            } catch (e: Exception) {
                _exportMessage.value = "Error: ${e.message}"
            } finally {
                _exportLoading.value = false
            }
        }
    }

    fun buildIcsShareIntent(): Intent? {
        val state = uiState.value
        return try {
            val (from, to) = exportBounds()
            CalendarExporter(getApplication()).createIcsShareIntent(
                from = from,
                to = to,
                assignments = state.assignments,
                shifts = state.shifts,
                includeRest = _exportIncludeRest.value
            )
        } catch (e: Exception) {
            _exportMessage.value = "Error al crear .ics: ${e.message}"
            null
        }
    }

}
