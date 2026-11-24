package com.lrhsoft.shifterclone.data

import com.lrhsoft.shifterclone.model.DayAssignment
import com.lrhsoft.shifterclone.model.Shift
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
import java.util.Locale
import kotlin.math.round

/**
 * Computo horario Osakidetza segun la logica del Excel moderno:
 * - Horas por codigo de turno (con/sin solape)
 * - Ajustes de noche y festivo por grupo de edad
 * - 1.592 h anuales, 45 h AP, 28 dias vacaciones
 *
 * Fuente de cartelera:
 * 1) Turnos del calendario de la app (abbreviation del Shift)
 * 2) Cartelera pegada / importada como Map fecha → codigo
 */
object HourlyComputeCalculator {

    const val ANNUAL_HOURS = 1592.0
    const val AP_HOURS = 45.0
    const val VACATION_DAYS = 28.0

    enum class AgeGroup {
        UNDER_55,
        AGE_55_60,
        OVER_60
    }

    data class ShiftHourTable(
        val code: String,
        val description: String,
        val hoursNoOverlap: Double,
        val hoursWithOverlap: Double,
        val isNight: Boolean = false,
        val isRest: Boolean = false,
        val isVacation: Boolean = false
    )

    /** Tabla por defecto (hoja Configuracion del Excel). */
    val DEFAULT_HOUR_TABLE: List<ShiftHourTable> = listOf(
        ShiftHourTable("M", "Manana", 7.0, 7.17),
        ShiftHourTable("T", "Tarde", 7.0, 7.17),
        ShiftHourTable("N", "Noche", 11.0, 11.17, isNight = true),
        ShiftHourTable("BT", "BT (especial)", 7.0, 7.17),
        ShiftHourTable("IG", "IG (especial)", 7.0, 7.17),
        ShiftHourTable("M119", "M119", 7.0, 7.17),
        ShiftHourTable("RQ", "RQ", 7.05, 7.05),
        ShiftHourTable("R1", "R1", 7.075, 7.075),
        ShiftHourTable("R2", "R2", 7.125, 7.125),
        ShiftHourTable("D", "Descanso", 0.0, 0.0, isRest = true),
        ShiftHourTable("L", "Libre", 0.0, 0.0, isRest = true),
        ShiftHourTable("V", "Vacaciones", 0.0, 0.0, isVacation = true)
    )

    data class Config(
        val year: Int,
        val solape: Boolean = false,
        val ageGroup: AgeGroup = AgeGroup.UNDER_55,
        val fullYearContract: Boolean = true,
        val contractStart: LocalDate? = null,
        val contractEnd: LocalDate? = null,
        val hasReduction: Boolean = false,
        val reductionFactor: Double = 0.0,
        val reductionStart: LocalDate? = null,
        val reductionEnd: LocalDate? = null,
        val hourTable: List<ShiftHourTable> = DEFAULT_HOUR_TABLE,
        val center: String? = null
    )

    data class ShiftBreakdown(
        val code: String,
        val description: String,
        val count: Int,
        val unitHours: Double,
        val totalHours: Double
    )

    data class Result(
        val year: Int,
        val workedHours: Double,
        val obligatoryHours: Double,
        val difference: Double,
        val situation: String,
        val vacationRemaining: Double,
        val vacationUsed: Int,
        val apHours: Double,
        val byCode: List<ShiftBreakdown>,
        val daysCounted: Int,
        val sourceLabel: String,
        val notes: List<String>
    )

    private fun nightHours(solape: Boolean, age: AgeGroup): Double = when (age) {
        AgeGroup.UNDER_55 -> if (solape) 11.17 else 11.0
        AgeGroup.AGE_55_60 -> if (solape) 11.67 else 11.5
        AgeGroup.OVER_60 -> if (solape) 12.67 else 12.5
    }

    private fun festiveExtra(age: AgeGroup): Double =
        if (age == AgeGroup.OVER_60) 1.75 else 0.0

    private fun normalizeCode(raw: String?): String? {
        if (raw.isNullOrBlank()) return null
        val c = raw.trim().uppercase(Locale.ROOT)
            .replace("MANANA", "M")
            .replace("MAÑANA", "M")
            .replace("TARDE", "T")
            .replace("NOCHE", "N")
            .replace("LIBRE", "L")
            .replace("DESCANSO", "D")
            .replace("VACACIONES", "V")
            .replace("VAC.", "V")
            .replace("VAC", "V")
        return c
    }

    fun carteleraFromCalendar(
        year: Int,
        assignments: Map<String, DayAssignment>,
        shifts: List<Shift>
    ): Map<LocalDate, String> {
        val byId = shifts.associateBy { it.id }
        val result = linkedMapOf<LocalDate, String>()
        val start = LocalDate.of(year, 1, 1)
        val end = LocalDate.of(year, 12, 31)
        var d = start
        while (!d.isAfter(end)) {
            val key = DayAssignment.key(d.year, d.monthValue, d.dayOfMonth)
            val dayShifts = assignments[key]?.shiftIds?.mapNotNull { byId[it] }.orEmpty()
            if (dayShifts.isNotEmpty()) {
                val preferred = dayShifts.firstOrNull { !it.isRest } ?: dayShifts.first()
                val code = normalizeCode(preferred.abbreviation)
                    ?: preferred.abbreviation.uppercase(Locale.ROOT)
                result[d] = code
            }
            d = d.plusDays(1)
        }
        return result
    }

    fun parseCarteleraText(year: Int, text: String): Map<LocalDate, String> {
        val monthNames = mapOf(
            "enero" to 1, "febrero" to 2, "marzo" to 3, "abril" to 4,
            "mayo" to 5, "junio" to 6, "julio" to 7, "agosto" to 8,
            "septiembre" to 9, "octubre" to 10, "noviembre" to 11, "diciembre" to 12,
            "ene" to 1, "feb" to 2, "mar" to 3, "abr" to 4, "may" to 5, "jun" to 6,
            "jul" to 7, "ago" to 8, "sep" to 9, "oct" to 10, "nov" to 11, "dic" to 12
        )
        val result = linkedMapOf<LocalDate, String>()
        val lines = text.lines().map { it.trim() }.filter { it.isNotEmpty() }
        for (line in lines) {
            val parts = line.split(Regex("[\t,;|/]+|\\s{2,}")).map { it.trim() }.filter { it.isNotEmpty() }
            if (parts.isEmpty()) continue
            val first = parts[0].lowercase(Locale.ROOT)
            val month = monthNames[first]
                ?: first.toIntOrNull()?.takeIf { it in 1..12 }
            if (month == null) continue
            val codes = parts.drop(1)
            val daysInMonth = YearMonth.of(year, month).lengthOfMonth()
            codes.forEachIndexed { idx, raw ->
                val day = idx + 1
                if (day > daysInMonth) return@forEachIndexed
                val code = normalizeCode(raw) ?: return@forEachIndexed
                if (code == "-" || code == "." || code == "X") return@forEachIndexed
                result[LocalDate.of(year, month, day)] = code
            }
        }
        return result
    }

    fun compute(
        config: Config,
        cartelera: Map<LocalDate, String>,
        sourceLabel: String = "Calendario"
    ): Result {
        val tableByCode = config.hourTable.associateBy { it.code.uppercase(Locale.ROOT) }
        val holidays = HolidayCalendar.holidaysFor(config.center)
            .filter { it.year == config.year }
            .toSet()

        val contractStart = if (config.fullYearContract) LocalDate.of(config.year, 1, 1)
        else config.contractStart ?: LocalDate.of(config.year, 1, 1)
        val contractEnd = if (config.fullYearContract) LocalDate.of(config.year, 12, 31)
        else config.contractEnd ?: LocalDate.of(config.year, 12, 31)

        val counts = linkedMapOf<String, Int>()
        var worked = 0.0
        var vacationUsed = 0
        var daysCounted = 0
        val unknown = mutableSetOf<String>()

        cartelera.forEach { (date, rawCode) ->
            if (date.year != config.year) return@forEach
            if (date.isBefore(contractStart) || date.isAfter(contractEnd)) return@forEach

            val code = normalizeCode(rawCode) ?: return@forEach
            val row = tableByCode[code]
            if (row == null) {
                unknown.add(code)
                return@forEach
            }
            counts[code] = (counts[code] ?: 0) + 1
            daysCounted++

            if (row.isVacation) {
                vacationUsed++
                return@forEach
            }
            if (row.isRest) return@forEach

            var hours = if (row.isNight) {
                nightHours(config.solape, config.ageGroup)
            } else {
                if (config.solape) row.hoursWithOverlap else row.hoursNoOverlap
            }

            if (!row.isNight && date in holidays) {
                hours += festiveExtra(config.ageGroup)
            }

            if (config.hasReduction && config.reductionFactor > 0) {
                val inRange = (config.reductionStart == null || !date.isBefore(config.reductionStart)) &&
                    (config.reductionEnd == null || !date.isAfter(config.reductionEnd))
                if (inRange) {
                    hours *= (1.0 - config.reductionFactor.coerceIn(0.0, 1.0))
                }
            }

            worked += hours
        }

        worked = round2(worked)

        val yearStart = LocalDate.of(config.year, 1, 1)
        val yearEnd = LocalDate.of(config.year, 12, 31)
        val daysInYear = ChronoUnit.DAYS.between(yearStart, yearEnd).toInt() + 1
        val contractDays = maxOf(0, ChronoUnit.DAYS.between(contractStart, minOf(contractEnd, yearEnd)).toInt() + 1)
            .coerceAtMost(daysInYear)
        var obligatory = ANNUAL_HOURS * (contractDays.toDouble() / daysInYear.toDouble())
        if (config.hasReduction && config.reductionFactor > 0) {
            val redStart = (config.reductionStart ?: contractStart).coerceAtLeast(contractStart)
            val redEnd = (config.reductionEnd ?: contractEnd).coerceAtMost(contractEnd)
            if (!redStart.isAfter(redEnd)) {
                val redDays = ChronoUnit.DAYS.between(redStart, redEnd).toInt() + 1
                val fullDays = (contractDays - redDays).coerceAtLeast(0)
                obligatory = ANNUAL_HOURS * (
                    fullDays / daysInYear.toDouble() +
                        redDays / daysInYear.toDouble() * (1.0 - config.reductionFactor.coerceIn(0.0, 1.0))
                    )
            }
        }
        obligatory = round2(obligatory)

        val difference = round2(worked - obligatory)
        val situation = when {
            difference > 0.5 -> "Te deben horas"
            difference < -0.5 -> "Debes horas"
            else -> "Equilibrado"
        }

        val byCode = config.hourTable.map { row ->
            val n = counts[row.code] ?: 0
            val unit = when {
                row.isNight -> nightHours(config.solape, config.ageGroup)
                config.solape -> row.hoursWithOverlap
                else -> row.hoursNoOverlap
            }
            ShiftBreakdown(
                code = row.code,
                description = row.description,
                count = n,
                unitHours = unit,
                totalHours = round2(n * unit)
            )
        }.filter { it.count > 0 || it.code in listOf("M", "T", "N", "BT", "IG", "D", "V") }

        val notes = mutableListOf<String>()
        notes.add("Fuente: $sourceLabel · ${cartelera.size} dias con codigo")
        if (unknown.isNotEmpty()) {
            notes.add("Codigos no reconocidos ignorados: ${unknown.sorted().joinToString(", ")}")
        }
        if (config.solape) notes.add("Solape activado (valores +0,17 h en diurnos/noche base)")
        when (config.ageGroup) {
            AgeGroup.UNDER_55 -> notes.add("Grupo edad: <55 · noche 11 h")
            AgeGroup.AGE_55_60 -> notes.add("Grupo edad: 55–60 · noche 11,5 h")
            AgeGroup.OVER_60 -> notes.add("Grupo edad: >60 · noche 12,5 h + festivo +1,75 h")
        }
        if (!config.fullYearContract) {
            notes.add("Contrato parcial: $contractStart → $contractEnd ($contractDays dias)")
        }
        if (config.hasReduction) {
            notes.add("Reduccion ${(config.reductionFactor * 100).toInt()}% en tramo indicado")
        }

        val ap = if (config.fullYearContract) AP_HOURS
        else round2(AP_HOURS * contractDays / daysInYear.toDouble())
        val vacRemaining = (VACATION_DAYS * contractDays / daysInYear.toDouble() - vacationUsed)
            .coerceAtLeast(0.0).let { round2(it) }

        return Result(
            year = config.year,
            workedHours = worked,
            obligatoryHours = obligatory,
            difference = difference,
            situation = situation,
            vacationRemaining = vacRemaining,
            vacationUsed = vacationUsed,
            apHours = ap,
            byCode = byCode,
            daysCounted = daysCounted,
            sourceLabel = sourceLabel,
            notes = notes
        )
    }

    private fun round2(v: Double): Double = round(v * 100.0) / 100.0
}
