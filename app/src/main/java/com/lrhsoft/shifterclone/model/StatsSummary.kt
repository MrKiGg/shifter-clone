package com.lrhsoft.shifterclone.model

import java.time.LocalDate

data class ShiftBreakdown(
    val shift: Shift,
    val count: Int,
    val totalHours: Double,
    val totalEarnings: Double
)

data class StatsSummary(
    val from: LocalDate,
    val to: LocalDate,
    val totalDays: Int,
    val workDays: Int,
    val restDays: Int,
    val emptyDays: Int,
    val totalHours: Double,
    val totalEarnings: Double,
    val byShift: List<ShiftBreakdown>
) {
    val hoursFormatted: String
        get() {
            val h = totalHours.toInt()
            val m = ((totalHours - h) * 60).toInt()
            return if (m == 0) "${h}h" else "${h}h ${m}m"
        }
}
