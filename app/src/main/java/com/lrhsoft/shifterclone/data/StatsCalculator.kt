package com.lrhsoft.shifterclone.data

import com.lrhsoft.shifterclone.model.DayAssignment
import com.lrhsoft.shifterclone.model.Shift
import com.lrhsoft.shifterclone.model.ShiftBreakdown
import com.lrhsoft.shifterclone.model.StatsSummary
import java.time.LocalDate

object StatsCalculator {

    fun calculate(
        from: LocalDate,
        to: LocalDate,
        assignments: Map<String, DayAssignment>,
        shifts: List<Shift>
    ): StatsSummary {
        val shiftMap = shifts.associateBy { it.id }
        val countByShift = mutableMapOf<String, Int>()
        val hoursByShift = mutableMapOf<String, Double>()
        val earningsByShift = mutableMapOf<String, Double>()

        var workDays = 0
        var restDays = 0
        var emptyDays = 0
        var totalHours = 0.0
        var totalEarnings = 0.0

        var cursor = from
        var totalDays = 0
        while (!cursor.isAfter(to)) {
            totalDays++
            val key = "${cursor.year}-${cursor.monthValue}-${cursor.dayOfMonth}"
            val assignment = assignments[key]
            val ids = assignment?.shiftIds.orEmpty()

            if (ids.isEmpty()) {
                emptyDays++
            } else {
                var dayIsOnlyRest = true
                var dayHasWork = false

                for (id in ids) {
                    val shift = shiftMap[id] ?: continue
                    countByShift[id] = (countByShift[id] ?: 0) + 1

                    if (shift.isRest) {
                        // solo cuenta como rest si no hay trabajo ese día
                    } else {
                        dayIsOnlyRest = false
                        dayHasWork = true
                        val hours = shift.durationHours() ?: 0.0
                        hoursByShift[id] = (hoursByShift[id] ?: 0.0) + hours
                        totalHours += hours
                        val rate = shift.hourlyRate
                        if (rate != null && hours > 0) {
                            val earn = hours * rate
                            earningsByShift[id] = (earningsByShift[id] ?: 0.0) + earn
                            totalEarnings += earn
                        }
                    }
                }

                when {
                    dayHasWork -> workDays++
                    dayIsOnlyRest -> restDays++
                    else -> emptyDays++
                }
            }
            cursor = cursor.plusDays(1)
        }

        val byShift = countByShift.mapNotNull { (id, count) ->
            val shift = shiftMap[id] ?: return@mapNotNull null
            ShiftBreakdown(
                shift = shift,
                count = count,
                totalHours = hoursByShift[id] ?: 0.0,
                totalEarnings = earningsByShift[id] ?: 0.0
            )
        }.sortedByDescending { it.count }

        return StatsSummary(
            from = from,
            to = to,
            totalDays = totalDays,
            workDays = workDays,
            restDays = restDays,
            emptyDays = emptyDays,
            totalHours = totalHours,
            totalEarnings = totalEarnings,
            byShift = byShift
        )
    }
}
