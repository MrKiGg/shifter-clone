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

        val fixed = mutableListOf<Pair<String, Double>>()
        fixed += "Sueldo base" to rates.sueldoMes
        fixed += "Complemento destino (nivel ${profile.destinoLevel})" to profile.destinoMensual
        fixed += "Complemento especifico (${profile.category.label})" to profile.especificoMensual

        if (profile.trienios > 0) {
            fixed += "Antiguedad (${profile.trienios} trienios)" to rates.antiguedadMes * profile.trienios
            fixed += "Diferencia antiguedad" to rates.diferenciaAntiguedadMes * profile.trienios
        }

        val hasTurnicidad = profile.hasTurnicidad || contracts.any { c ->
            c.hasTurnicidad && run {
                val end = yearMonth.atEndOfMonth()
                var cur = yearMonth.atDay(1)
                var hit = false
                while (!cur.isAfter(end)) {
                    if (c.contains(cur)) { hit = true; break }
                    cur = cur.plusDays(1)
                }
                hit
            }
        }
        if (hasTurnicidad) {
            fixed += "Turnicidad" to rates.turnicidadMes
        }

        val fixedTotal = fixed.sumOf { it.second }

        var nights = 0
        var festivoDays = 0
        var festivoNights = 0
        var workDays = 0
        var variable = 0.0
        val variableLines = mutableListOf<Pair<String, Double>>()

        val daysInMonth = yearMonth.lengthOfMonth()
        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val key = DayAssignment.key(date.year, date.monthValue, date.dayOfMonth)
            val dayShifts = assignments[key]?.shiftIds
                ?.mapNotNull { shiftMap[it] }
                ?.filter { !it.isRest }
                .orEmpty()
            if (dayShifts.isEmpty()) continue
            workDays++

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
                        val amount = if (profile.countNightShiftAsFullNight) {
                            rates.nocturnidadNoche
                        } else {
                            rates.nocturnidadHora * profile.horasNocheCompleta
                        }
                        val mult = if (date.month == Month.DECEMBER &&
                            (date.dayOfMonth == 24 || date.dayOfMonth == 31)
                        ) 2.0 else 1.0
                        variable += amount * mult
                    }
                }
            }
        }

        if (nights > 0) {
            val unit = if (profile.countNightShiftAsFullNight) rates.nocturnidadNoche
            else rates.nocturnidadHora * profile.horasNocheCompleta
            variableLines += "Nocturnidad ($nights noches)" to unit * nights
        }
        if (festivoDays > 0) {
            variableLines += "Festivo/domingo dia ($festivoDays)" to rates.festivoDia * festivoDays
        }
        if (festivoNights > 0) {
            variableLines += "Festivo/domingo noche ($festivoNights)" to rates.festivoNoche * festivoNights
        }

        return PayrollEstimate(
            fixedMonthly = fixedTotal,
            variableFromShifts = variable,
            totalEstimated = fixedTotal + variable,
            breakdown = fixed + variableLines,
            nights = nights,
            festivoDays = festivoDays,
            festivoNights = festivoNights,
            workDays = workDays
        )
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
