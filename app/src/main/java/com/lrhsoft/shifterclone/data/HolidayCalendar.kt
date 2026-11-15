package com.lrhsoft.shifterclone.data

import java.time.LocalDate

/**
 * Festivos laborales de Euskadi (CAV) + Bizkaia + locales Bilbao / Barakaldo.
 *
 * - CAV: decretos BOPV 2025–2027
 * - Bizkaia (territorio): 31 julio San Ignacio
 * - Bilbao: Viernes Semana Grande (agosto)
 * - Barakaldo: 16 julio Nuestra Señora del Carmen
 */
object HolidayCalendar {

    val CENTERS = listOf("Bilbao", "Barakaldo")

    private val euskadiHolidays: Set<LocalDate> = setOf(
        // 2025
        LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 6),
        LocalDate.of(2025, 4, 17), LocalDate.of(2025, 4, 18), LocalDate.of(2025, 4, 21),
        LocalDate.of(2025, 5, 1), LocalDate.of(2025, 7, 25), LocalDate.of(2025, 8, 15),
        LocalDate.of(2025, 11, 1), LocalDate.of(2025, 12, 6), LocalDate.of(2025, 12, 8),
        LocalDate.of(2025, 12, 25),
        // 2026
        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 6), LocalDate.of(2026, 3, 19),
        LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 3), LocalDate.of(2026, 4, 6),
        LocalDate.of(2026, 5, 1), LocalDate.of(2026, 7, 25), LocalDate.of(2026, 8, 15),
        LocalDate.of(2026, 10, 12), LocalDate.of(2026, 12, 8), LocalDate.of(2026, 12, 25),
        // 2027
        LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 6),
        LocalDate.of(2027, 3, 25), LocalDate.of(2027, 3, 26), LocalDate.of(2027, 3, 29),
        LocalDate.of(2027, 5, 1), LocalDate.of(2027, 10, 7), LocalDate.of(2027, 10, 12),
        LocalDate.of(2027, 11, 1), LocalDate.of(2027, 12, 6), LocalDate.of(2027, 12, 8),
        LocalDate.of(2027, 12, 25)
    )

    private val bizkaiaTerritory: Set<LocalDate> = setOf(
        LocalDate.of(2025, 7, 31),
        LocalDate.of(2026, 7, 31),
        LocalDate.of(2027, 7, 31)
    )

    private val bilbaoLocal: Set<LocalDate> = setOf(
        LocalDate.of(2025, 8, 22),
        LocalDate.of(2026, 8, 28),
        LocalDate.of(2027, 8, 27)
    )

    /** Nuestra Señora del Carmen — fiesta local de Barakaldo (16 de julio). */
    private val barakaldoLocal: Set<LocalDate> = setOf(
        LocalDate.of(2025, 7, 16),
        LocalDate.of(2026, 7, 16),
        LocalDate.of(2027, 7, 16)
    )

    /** Conjunto amplio (CAV + Bizkaia + Bilbao + Barakaldo). */
    private val allHolidays: Set<LocalDate> =
        euskadiHolidays + bizkaiaTerritory + bilbaoLocal + barakaldoLocal

    fun isHoliday(date: LocalDate): Boolean = date in allHolidays

    fun isHoliday(date: LocalDate, center: String?): Boolean =
        date in holidaysFor(center)

    /**
     * Festivos aplicables según centro del contrato.
     * Por defecto (vacío / Bilbao): CAV + San Ignacio + Semana Grande Bilbao.
     * Barakaldo: CAV + San Ignacio + Carmen 16 jul (sin Semana Grande Bilbao).
     */
    fun holidaysFor(center: String?): Set<LocalDate> {
        val c = center?.trim().orEmpty()
        val base = euskadiHolidays + bizkaiaTerritory
        return when {
            c.equals("Barakaldo", ignoreCase = true) -> base + barakaldoLocal
            c.equals("Bilbao", ignoreCase = true) || c.isBlank() -> base + bilbaoLocal
            else -> base + bilbaoLocal + barakaldoLocal
        }
    }

    fun holidaysForCenters(centers: Collection<String>): Set<LocalDate> {
        if (centers.isEmpty()) return holidaysFor(null)
        return centers.fold(emptySet()) { acc, c -> acc + holidaysFor(c) }
    }

    fun holidaysIn(year: Int): List<LocalDate> =
        allHolidays.filter { it.year == year }.sorted()

    fun all(): Set<LocalDate> = allHolidays

    fun isDoubleNightDate(date: LocalDate): Boolean =
        date.monthValue == 12 && (date.dayOfMonth == 24 || date.dayOfMonth == 31)

    fun isDoubleFestivoDayShift(date: LocalDate): Boolean =
        (date.monthValue == 12 && date.dayOfMonth == 25) ||
            (date.monthValue == 1 && date.dayOfMonth == 1)
}
