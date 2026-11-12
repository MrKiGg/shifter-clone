package com.lrhsoft.shifterclone.data

import com.lrhsoft.shifterclone.model.DayAssignment
import com.lrhsoft.shifterclone.model.PayrollEstimate
import com.lrhsoft.shifterclone.model.PayrollTables
import com.lrhsoft.shifterclone.model.Shift
import com.lrhsoft.shifterclone.model.UserProfile
import com.lrhsoft.shifterclone.model.WorkContract
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import java.time.YearMonth

object PayrollCalculator {

    fun estimateMonth(
        yearMonth: YearMonth,
        profile: UserProfile,
        assignments: Map<String, DayAssignment>,
        shifts: List<Shift>,
        contracts: List<WorkContract> = emptyList(),
        holidays: Set<LocalDate> = emptySet()
    ): PayrollEstimate {
        val rates = PayrollTables.rates(profile.group)
        val shiftMap = shifts.associateBy { it.id }
        val daysInMonth = yearMonth.lengthOfMonth()

        var nights = 0
        var festivoDays = 0
        var festivoNights = 0
        var workDays = 0
        val distinctWorkShiftIds = mutableSetOf<String>()
        var variable = 0.0

        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val key = DayAssignment.key(date.year, date.monthValue, date.dayOfMonth)
            val dayShifts = assignments[key]?.shiftIds
                ?.mapNotNull { shiftMap[it] }
                ?.filter { !it.isRest }
                .orEmpty()
            if (dayShifts.isEmpty()) continue
            workDays++
            dayShifts.forEach { distinctWorkShiftIds.add(it.id) }

            val isFestivo = date.dayOfWeek == DayOfWeek.SUNDAY || holidays.contains(date)
            dayShifts.forEach { shift ->
                val isNight = isNightShift(shift)
                when {
                    isFestivo && isNight -> {
                        festivoNights++
                        variable += rates.festivoNoche
                    }
                    isFestivo && !isNight -> {
                        festivoDays++
                        variable += rates.festivoDia
                    }
                    isNight -> {
                        nights++
                        val amount = rates.nocturnidadNoche
                        val mult = if (date.month == Month.DECEMBER &&
                            (date.dayOfMonth == 24 || date.dayOfMonth == 31)
                        ) 2.0 else 1.0
                        variable += amount * mult
                    }
                }
            }
        }

        val hasTurnicidad = distinctWorkShiftIds.size >= 2 ||
            contracts.any { c -> c.hasTurnicidad && monthOverlapsContract(yearMonth, c) }

        val fixedFull = mutableListOf<Pair<String, Double>>()
        fixedFull += "Sueldo base" to rates.sueldoMes
        fixedFull += "Complemento destino (nivel ${profile.destinoLevel})" to profile.destinoMensual
        fixedFull += "Complemento especifico (${profile.category.label})" to profile.especificoMensual
        if (profile.trienios > 0) {
            fixedFull += "Antiguedad (${profile.trienios} trienios)" to rates.antiguedadMes * profile.trienios
            fixedFull += "Diferencia antiguedad" to rates.diferenciaAntiguedadMes * profile.trienios
        }
        if (hasTurnicidad) {
            fixedFull += "Turnicidad" to rates.turnicidadMes
        }

        val fullFixedTotal = fixedFull.sumOf { it.second }
        val ratio = if (daysInMonth > 0) workDays.toDouble() / daysInMonth else 0.0
        val proratedFixed = fullFixedTotal * ratio

        val breakdown = mutableListOf<Pair<String, Double>>()
        fixedFull.forEach { (label, amount) ->
            breakdown += "$label (prorrateado)" to amount * ratio
        }
        if (nights > 0) {
            breakdown += "Nocturnidad ($nights noches)" to rates.nocturnidadNoche * nights
        }
        if (festivoDays > 0) {
            breakdown += "Festivo/domingo dia ($festivoDays)" to rates.festivoDia * festivoDays
        }
        if (festivoNights > 0) {
            breakdown += "Festivo/domingo noche ($festivoNights)" to rates.festivoNoche * festivoNights
        }

        return PayrollEstimate(
            fixedMonthly = proratedFixed,
            variableFromShifts = variable,
            totalEstimated = proratedFixed + variable,
            breakdown = breakdown,
            nights = nights,
            festivoDays = festivoDays,
            festivoNights = festivoNights,
            workDays = workDays
        )
    }

    private fun monthOverlapsContract(yearMonth: YearMonth, c: WorkContract): Boolean {
        val end = yearMonth.atEndOfMonth()
        var cur = yearMonth.atDay(1)
        while (!cur.isAfter(end)) {
            if (c.contains(cur)) return true
            cur = cur.plusDays(1)
        }
        return false
    }

    private fun isNightShift(shift: Shift): Boolean {
        val abbr = shift.abbreviation.uppercase()
        if (abbr == "N" || abbr.contains("NOCH")) return true
        val name = shift.name.lowercase()
        if (name.contains("noche") || name.contains("noct")) return true
        val start = shift.startHour ?: return false
        return start >= 21 || start <= 6
    }
}
