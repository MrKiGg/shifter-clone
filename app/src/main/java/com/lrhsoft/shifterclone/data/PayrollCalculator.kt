package com.lrhsoft.shifterclone.data

import com.lrhsoft.shifterclone.model.DayAssignment
import com.lrhsoft.shifterclone.model.PayrollEstimate
import com.lrhsoft.shifterclone.model.PayrollTables
import com.lrhsoft.shifterclone.model.Shift
import com.lrhsoft.shifterclone.model.UserProfile
import com.lrhsoft.shifterclone.model.WorkContract
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth

object PayrollCalculator {

    /**
     * Estimación de nómina del mes.
     *
     * Conceptos fijos: importe_mes × días_de_alta / 30 (mes completo → 100 %).
     * Variables: según turnos pintados.
     * Festivos: domingos + festivos del centro del contrato (Bilbao / Barakaldo).
     * Dobles (manual Osakidetza):
     *  - Noches 24 y 31 de diciembre → ×2 nocturnidad
     *  - Mañana/tarde del 25 de diciembre y del 1 de enero → ×2 festivo día
     */
    fun estimateMonth(
        yearMonth: YearMonth,
        profile: UserProfile,
        assignments: Map<String, DayAssignment>,
        shifts: List<Shift>,
        contracts: List<WorkContract> = emptyList(),
        holidays: Set<LocalDate> = HolidayCalendar.all()
    ): PayrollEstimate {
        val rates = PayrollTables.rates(profile.group)
        val shiftMap = shifts.associateBy { it.id }
        val daysInMonth = yearMonth.lengthOfMonth()
        // Festivos según centros de los contratos del mes (por defecto Bilbao)
        val monthCenters = contracts.filter { monthOverlapsContract(yearMonth, it) }
            .map { it.center }.filter { it.isNotBlank() }.distinct()
        val effectiveHolidays = if (holidays !== HolidayCalendar.all() && holidays.isNotEmpty()) {
            holidays
        } else {
            HolidayCalendar.holidaysForCenters(monthCenters.ifEmpty { listOf("Bilbao") })
        }

        var nights = 0
        var festivoDays = 0
        var festivoNights = 0
        var workDays = 0
        var doubleNightCount = 0
        var doubleFestivoDayCount = 0
        val distinctWorkShiftIds = mutableSetOf<String>()
        var variable = 0.0
        var nocturnidadAmount = 0.0
        var festivoDiaAmount = 0.0
        var festivoNocheAmount = 0.0

        var contractDays = 0
        for (day in 1..daysInMonth) {
            val date = yearMonth.atDay(day)
            val inContract = if (contracts.isEmpty()) {
                true
            } else {
                contracts.any { it.contains(date) }
            }
            if (inContract) contractDays++

            val key = DayAssignment.key(date.year, date.monthValue, date.dayOfMonth)
            val dayShifts = assignments[key]?.shiftIds
                ?.mapNotNull { shiftMap[it] }
                ?.filter { !it.isRest }
                .orEmpty()
            if (dayShifts.isEmpty()) continue
            if (!inContract) continue

            workDays++
            dayShifts.forEach { distinctWorkShiftIds.add(it.id) }

            val isFestivo = date.dayOfWeek == DayOfWeek.SUNDAY ||
                effectiveHolidays.contains(date)

            dayShifts.forEach { shift ->
                val isNight = isNightShift(shift)
                when {
                    isFestivo && isNight -> {
                        festivoNights++
                        val mult = if (HolidayCalendar.isDoubleNightDate(date)) 2.0 else 1.0
                        if (mult > 1.0) doubleNightCount++
                        val amount = rates.festivoNoche * mult
                        festivoNocheAmount += amount
                        variable += amount
                    }
                    isFestivo && !isNight -> {
                        festivoDays++
                        val mult = if (HolidayCalendar.isDoubleFestivoDayShift(date)) 2.0 else 1.0
                        if (mult > 1.0) doubleFestivoDayCount++
                        val amount = rates.festivoDia * mult
                        festivoDiaAmount += amount
                        variable += amount
                    }
                    isNight -> {
                        nights++
                        val base = if (profile.countNightShiftAsFullNight) {
                            rates.nocturnidadNoche
                        } else {
                            rates.nocturnidadHora * profile.horasNocheCompleta
                        }
                        val mult = if (HolidayCalendar.isDoubleNightDate(date)) 2.0 else 1.0
                        if (mult > 1.0) doubleNightCount++
                        val amount = base * mult
                        nocturnidadAmount += amount
                        variable += amount
                    }
                }
            }
        }

        val hasTurnicidad = profile.hasTurnicidad && (
            distinctWorkShiftIds.size >= 2 ||
                contracts.any { c -> c.hasTurnicidad && monthOverlapsContract(yearMonth, c) } ||
                (contracts.isEmpty() && distinctWorkShiftIds.size >= 2)
            )

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

        val ratio = when {
            contractDays <= 0 -> 0.0
            contractDays >= daysInMonth -> 1.0
            else -> contractDays.toDouble() / PayrollTables.PRORATE_DIVISOR_DAYS
        }

        val proratedFixed = fixedFull.sumOf { it.second } * ratio

        val breakdown = mutableListOf<Pair<String, Double>>()
        breakdown += "Dias de alta / contrato" to contractDays.toDouble()
        breakdown += "Prorrateo fijos (dias/30)" to ratio
        fixedFull.forEach { (label, amount) ->
            breakdown += label to amount * ratio
        }
        if (nocturnidadAmount > 0) {
            val label = if (doubleNightCount > 0)
                "Nocturnidad ($nights noches, $doubleNightCount x2)"
            else
                "Nocturnidad ($nights noches)"
            breakdown += label to nocturnidadAmount
        }
        if (festivoDiaAmount > 0) {
            val label = if (doubleFestivoDayCount > 0)
                "Festivo/domingo dia ($festivoDays, $doubleFestivoDayCount x2)"
            else
                "Festivo/domingo dia ($festivoDays)"
            breakdown += label to festivoDiaAmount
        }
        if (festivoNocheAmount > 0) {
            breakdown += "Festivo/domingo noche ($festivoNights)" to festivoNocheAmount
        }

        return PayrollEstimate(
            fixedMonthly = proratedFixed,
            variableFromShifts = variable,
            totalEstimated = proratedFixed + variable,
            breakdown = breakdown,
            nights = nights,
            festivoDays = festivoDays,
            festivoNights = festivoNights,
            workDays = workDays,
            contractDays = contractDays,
            fixedRatio = ratio
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
