package com.lrhsoft.shifterclone.data

import com.lrhsoft.shifterclone.model.DayAssignment
import com.lrhsoft.shifterclone.model.Shift
import com.lrhsoft.shifterclone.model.WorkContract
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.max
import kotlin.math.round

/**
 * Contadores de derechos laborales Osakidetza (estatutario).
 *
 * Fuentes: Acuerdo condiciones de trabajo / guías sindicales ESK-SATSE-UGT:
 * - Vacaciones: 28 días laborables/año; disfrute si contrato > 90 días; proporcional días/365.
 * - AP (asuntos particulares): 6 días = 45 h/año; proporcional.
 *   **Interrupción:** si entre dos contratos hay ≥1 día sin contrato, se pierde el saldo
 *   de AP del tramo anterior y se empieza a contar desde 0 en la nueva cadena contigua
 *   (solo se acumula si el siguiente empieza el día siguiente al fin del anterior o se solapan).
 * - Jornada anual: 1.592 h. Teórica: días×4,36 (con vacaciones) o ×4,75 (sin disfrutar).
 * - Factor noche: 1,10 (<55); 1,15 (≥55); 1,25 (≥60, también festivos).
 * - Descanso no disfrutado / liquidación vacaciones: en fin de contrato si no se disfrutaron.
 */
object LaborRightsCalculator {

    const val ANNUAL_HOURS = 1592.0
    const val VACATION_DAYS_YEAR = 28.0
    const val VACATION_MIN_DAYS_TO_ENJOY = 90
    const val AP_HOURS_YEAR = 45.0
    const val AP_DAYS_YEAR = 6.0
    const val AP_HOURS_PER_DAY = 7.5 // 45/6
    const val HOURS_PER_DAY_WITH_VAC = 4.36 // 1592/365
    const val HOURS_PER_DAY_NO_VAC = 4.75

    /**
     * Días naturales de separación máximos entre fin de un contrato e inicio del siguiente
     * para seguir en la misma cadena de AP.
     * gap = ChronoUnit.DAYS.between(end, nextStart):
     *  - 0 = solape / mismo día
     *  - 1 = empieza al día siguiente (sin día libre) → contiguo
     *  - ≥2 = al menos 1 día sin contrato → se rompe la cadena de AP
     */
    const val AP_MAX_GAP_DAYS = 1

    data class RightsSummary(
        val contractDays: Int,
        val consecutiveDays: Int,
        val currentChainDays: Int,
        val vacationDaysGenerated: Double,
        val vacationCanEnjoy: Boolean,
        val vacationLiquidationNote: String,
        val apHoursGenerated: Double,
        val apDaysEquivalent: Double,
        val theoreticalHoursWithVac: Double,
        val theoreticalHoursNoVac: Double,
        val workedHoursEstimated: Double,
        val nightHoursRaw: Double,
        val nightHoursComputed: Double,
        val balanceWithVac: Double,
        val balanceNoVac: Double,
        val nightFactor: Double,
        val notes: List<String>
    )

    fun summarize(
        contracts: List<WorkContract>,
        assignments: Map<String, DayAssignment>,
        shifts: List<Shift>,
        ageYears: Int? = null,
        referenceEnd: LocalDate = LocalDate.now()
    ): RightsSummary {
        val parsed = contracts.mapNotNull { c ->
            try {
                val start = LocalDate.parse(c.startDate)
                val end = c.endDate?.let { LocalDate.parse(it) } ?: referenceEnd
                Triple(start, end, c)
            } catch (_: Exception) {
                null
            }
        }.sortedBy { it.first }

        var totalDays = 0
        parsed.forEach { (start, end, _) ->
            if (!end.isBefore(start)) {
                totalDays += (ChronoUnit.DAYS.between(start, end) + 1).toInt()
            }
        }

        val ranges = parsed.map { it.first to it.second }
        val consecutiveDays = longestContiguousDays(ranges)
        val currentChainDays = currentContiguousDays(ranges)

        val vacationDays = round1(VACATION_DAYS_YEAR * totalDays / 365.0)
        val canEnjoy = consecutiveDays >= VACATION_MIN_DAYS_TO_ENJOY || totalDays >= VACATION_MIN_DAYS_TO_ENJOY
        val vacationNote = when {
            totalDays == 0 -> "Sin contratos"
            canEnjoy -> "Contrato(s) ≥ 90 días: derecho a disfrutar vacaciones (o liquidar si no se disfrutan)"
            else -> "Contrato(s) < 90 días: se generan días proporcionales y suelen liquidarse en finiquito"
        }

        // AP solo sobre la cadena actual (tras reinicio por interrupción ≥1 día)
        val apHours = round1(AP_HOURS_YEAR * currentChainDays / 365.0)
        val apDaysEq = round1(apHours / AP_HOURS_PER_DAY)

        val nightFactor = when {
            ageYears != null && ageYears >= 60 -> 1.25
            ageYears != null && ageYears >= 55 -> 1.15
            else -> 1.10
        }

        val shiftMap = shifts.associateBy { it.id }
        var workedRaw = 0.0
        var nightRaw = 0.0
        parsed.forEach { (start, end, _) ->
            var d = start
            while (!d.isAfter(end)) {
                val key = DayAssignment.key(d.year, d.monthValue, d.dayOfMonth)
                val dayShifts = assignments[key]?.shiftIds
                    ?.mapNotNull { shiftMap[it] }
                    ?.filter { !it.isRest }
                    .orEmpty()
                dayShifts.forEach { s ->
                    val hours = shiftHours(s)
                    workedRaw += hours
                    if (isNight(s)) nightRaw += hours
                }
                d = d.plusDays(1)
            }
        }
        val nightComputed = nightRaw * nightFactor
        val workedComputed = (workedRaw - nightRaw) + nightComputed

        val theoWith = round1(totalDays * HOURS_PER_DAY_WITH_VAC)
        val theoNo = round1(totalDays * HOURS_PER_DAY_NO_VAC)

        val notes = buildList {
            add("Jornada anual referencia: ${ANNUAL_HOURS.toInt()} h")
            add(
                "Factor noche aplicado: ×$nightFactor" +
                    if (ageYears != null) " (edad $ageYears)" else " (edad <55 por defecto)"
            )
            add("AP anuales: ${AP_DAYS_YEAR.toInt()} días = ${AP_HOURS_YEAR.toInt()} h (≈ ${AP_HOURS_PER_DAY} h/día)")
            add("Vacaciones anuales: ${VACATION_DAYS_YEAR.toInt()} días laborables")
            add(
                "AP cadena actual: $currentChainDays días · " +
                    "si hay ≥1 día sin contrato entre nombramientos el AP se reinicia a 0"
            )
            if (parsed.size > 1) {
                add("Servicio continuo más largo (cualquier cadena): $consecutiveDays días")
            }
        }

        return RightsSummary(
            contractDays = totalDays,
            consecutiveDays = consecutiveDays,
            currentChainDays = currentChainDays,
            vacationDaysGenerated = vacationDays,
            vacationCanEnjoy = canEnjoy,
            vacationLiquidationNote = vacationNote,
            apHoursGenerated = apHours,
            apDaysEquivalent = apDaysEq,
            theoreticalHoursWithVac = theoWith,
            theoreticalHoursNoVac = theoNo,
            workedHoursEstimated = round1(workedComputed),
            nightHoursRaw = round1(nightRaw),
            nightHoursComputed = round1(nightComputed),
            balanceWithVac = round1(workedComputed - theoWith),
            balanceNoVac = round1(workedComputed - theoNo),
            nightFactor = nightFactor,
            notes = notes
        )
    }

    /** Cadena contigua más larga (para umbral 90 días de disfrute de vacaciones). */
    private fun longestContiguousDays(ranges: List<Pair<LocalDate, LocalDate>>): Int {
        if (ranges.isEmpty()) return 0
        val chains = contiguousChains(ranges)
        return chains.maxOf { chainDays(it) }
    }

    /** Última cadena contigua (la vigente para AP). */
    private fun currentContiguousDays(ranges: List<Pair<LocalDate, LocalDate>>): Int {
        if (ranges.isEmpty()) return 0
        return chainDays(contiguousChains(ranges).last())
    }

    private fun contiguousChains(
        ranges: List<Pair<LocalDate, LocalDate>>
    ): List<List<Pair<LocalDate, LocalDate>>> {
        val sorted = ranges.sortedBy { it.first }
        val chains = mutableListOf<MutableList<Pair<LocalDate, LocalDate>>>()
        var current = mutableListOf(sorted.first())
        var blockEnd = sorted.first().second

        for (i in 1 until sorted.size) {
            val (s, e) = sorted[i]
            val gap = ChronoUnit.DAYS.between(blockEnd, s)
            if (gap <= AP_MAX_GAP_DAYS) {
                current.add(s to e)
                if (e.isAfter(blockEnd)) blockEnd = e
            } else {
                chains.add(current)
                current = mutableListOf(s to e)
                blockEnd = e
            }
        }
        chains.add(current)
        return chains
    }

    private fun chainDays(chain: List<Pair<LocalDate, LocalDate>>): Int {
        if (chain.isEmpty()) return 0
        // Unión de intervalos (puede haber solapes)
        val sorted = chain.sortedBy { it.first }
        var start = sorted.first().first
        var end = sorted.first().second
        var total = 0
        for (i in 1 until sorted.size) {
            val (s, e) = sorted[i]
            if (!s.isAfter(end.plusDays(1))) {
                if (e.isAfter(end)) end = e
            } else {
                total += (ChronoUnit.DAYS.between(start, end) + 1).toInt()
                start = s
                end = e
            }
        }
        total += (ChronoUnit.DAYS.between(start, end) + 1).toInt()
        return total
    }

    private fun shiftHours(s: Shift): Double {
        val start = s.startHour
        val end = s.endHour
        if (start != null && end != null) {
            var h = end - start
            if (h <= 0) h += 24
            return h.toDouble()
        }
        return if (isNight(s)) 10.0 else 7.5
    }

    private fun isNight(s: Shift): Boolean {
        val abbr = s.abbreviation.uppercase()
        if (abbr == "N" || abbr.contains("NOCH")) return true
        val name = s.name.lowercase()
        if (name.contains("noche") || name.contains("noct")) return true
        val start = s.startHour ?: return false
        return start >= 21 || start <= 6
    }

    private fun round1(v: Double): Double = round(v * 10.0) / 10.0
}
