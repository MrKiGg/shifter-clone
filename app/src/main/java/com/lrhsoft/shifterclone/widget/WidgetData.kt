package com.lrhsoft.shifterclone.widget

import android.content.Context
import com.lrhsoft.shifterclone.data.ShiftRepository
import com.lrhsoft.shifterclone.model.DayAssignment
import com.lrhsoft.shifterclone.model.Shift
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.TemporalAdjusters

data class WidgetSnapshot(
    val shifts: List<Shift>,
    val assignments: Map<String, DayAssignment>
) {
    private val shiftMap by lazy { shifts.associateBy { it.id }

    fun shiftsFor(date: LocalDate): List<Shift> {
        val key = "${date.year}-${date.monthValue}-${date.dayOfMonth}"
        val ids = assignments[key]?.shiftIds.orEmpty()
        return ids.mapNotNull { shiftMap[it] }
    }

    fun labelFor(date: LocalDate): String {
        val list = shiftsFor(date)
        return if (list.isEmpty()) "—" else list.joinToString("+") { it.abbreviation }
    }

    fun colorFor(date: LocalDate): Long? =
        shiftsFor(date).firstOrNull()?.color
}

object WidgetDataLoader {
    suspend fun load(context: Context): WidgetSnapshot {
        val repo = ShiftRepository(context.applicationContext)
        val shifts = repo.shiftsFlow.first()
        val assignments = repo.assignmentsFlow.first()
        return WidgetSnapshot(shifts, assignments)
    }
}

fun weekDaysAround(today: LocalDate = LocalDate.now()): List<LocalDate> {
    val monday = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    return (0..6).map { monday.plusDays(it.toLong()) }
}

fun monthCells(yearMonth: YearMonth): List<LocalDate?> {
    val first = yearMonth.atDay(1)
    val startOffset = (first.dayOfWeek.value - 1) % 7
    val cells = mutableListOf<LocalDate?>()
    val prev = yearMonth.minusMonths(1)
    val daysInPrev = prev.lengthOfMonth()
    for (i in startOffset downTo 1) {
        cells.add(prev.atDay(daysInPrev - i + 1))
    }
    for (d in 1..yearMonth.lengthOfMonth()) {
        cells.add(yearMonth.atDay(d))
    }
    val remaining = (7 - cells.size % 7) % 7
    for (d in 1..remaining) {
        cells.add(yearMonth.plusMonths(1).atDay(d))
    }
    return cells
}
