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

    /**
     * Estimación de nómina del mes.
     *
     * Conceptos fijos (sueldo, destino, específico, turnicidad, antigüedad):
     *   importe_mes × días_de_alta / 30
     * Si el periodo cubre el mes entero → 100 % del importe mensual.
     * Confirmado con recibo Osakidetza 13-15/05/2024 (3 días → ×3/30).
     *
     * Variables (nocturnidad, festivos): solo días/turnos pintados en el calendario.
     */
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

        // Días de alta en el mes (unión de contratos). Sin contratos → se asume mes completo.
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
            // Solo contar actividad variable si el día está en contrato (o no hay contratos)
            if (!inContract) continue

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

        // Turnicidad: varios tipos de turno pintados o marcado en contrato/perfil
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

        // Prorrateo Osakidetza: días_alta / 30; mes completo → 100 %
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
            breakdown += "$label" to amount * ratio
        }
        if (nights > 0) {
            breakdown += "Nocturnidad ($nights noches)" to
                (if (profile.countNightShiftAsFullNight) rates.nocturnidadNoche else rates.nocturnidadHora * profile.horasNocheCompleta) * nights
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
