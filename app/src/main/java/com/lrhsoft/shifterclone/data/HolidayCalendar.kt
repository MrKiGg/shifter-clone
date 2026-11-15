package com.lrhsoft.shifterclone.data

import java.time.LocalDate

/**
 * Festivos laborales de Euskadi (CAV) + Bizkaia + locales por centro hospitalario.
 *
 * Centros de calendario:
 * - Basurto (Bilbao): CAV + San Ignacio + Semana Grande Bilbao
 * - Cruces (Barakaldo): CAV + San Ignacio + Carmen 16 jul
 */
object HolidayCalendar {

    /** Centros disponibles en el selector del calendario principal. */
    val CENTERS = listOf("Basurto", "Cruces")

    private val euskadiFixed: Set<LocalDate> = setOf(
        // 2025
        LocalDate.of(2025, 1, 1), LocalDate.of(2025, 1, 6),
        LocalDate.of(2025, 4, 17), LocalDate.of(2025, 4, 18),
        LocalDate.of(2025, 5, 1), LocalDate.of(2025, 8, 15),
        LocalDate.of(2025, 10, 12), LocalDate.of(2025, 11, 1),
        LocalDate.of(2025, 12, 6), LocalDate.of(2025, 12, 8), LocalDate.of(2025, 12, 25),
        // 2026
        LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 6),
        LocalDate.of(2026, 4, 2), LocalDate.of(2026, 4, 3),
        LocalDate.of(2026, 5, 1), LocalDate.of(2026, 8, 15),
        LocalDate.of(2026, 10, 12), LocalDate.of(2026, 11, 1),
        LocalDate.of(2026, 12, 6), LocalDate.of(2026, 12, 8), LocalDate.of(2026, 12, 25),
        // 2027
        LocalDate.of(2027, 1, 1), LocalDate.of(2027, 1, 6),
        LocalDate.of(2027, 3, 25), LocalDate.of(2027, 3, 26),
        LocalDate.of(2027, 5, 1), LocalDate.of(2027, 8, 15),
        LocalDate.of(2027, 10, 12), LocalDate.of(2027, 11, 1),
        LocalDate.of(2027, 12, 6), LocalDate.of(2027, 12, 8), LocalDate.of(2027, 12, 25)
    )

    /** San Ignacio — territorio histórico de Bizkaia (31 julio). */
    private val bizkaiaLocal: Set<LocalDate> = setOf(
        LocalDate.of(2025, 7, 31),
        LocalDate.of(2026, 7, 31),
        LocalDate.of(2027, 7, 31)
    )

    /** Viernes de Semana Grande de Bilbao (agosto). */
    private val bilbaoLocal: Set<LocalDate> = setOf(
        LocalDate.of(2025, 8, 22),
        LocalDate.of(2026, 8, 21),
        LocalDate.of(2027, 8, 20)
    )

    /** Nuestra Señora del Carmen — fiesta local de Barakaldo / zona Cruces (16 julio). */
    private val barakaldoLocal: Set<LocalDate> = setOf(
        LocalDate.of(2025, 7, 16),
        LocalDate.of(2026, 7, 16),
        LocalDate.of(2027, 7, 16)
    )

    private val baseBizkaia: Set<LocalDate> = euskadiFixed + bizkaiaLocal

    private val allHolidays: Set<LocalDate> = baseBizkaia + bilbaoLocal + barakaldoLocal

    fun isHoliday(date: LocalDate): Boolean = date in allHolidays

    fun isHoliday(date: LocalDate, center: String?): Boolean =
        date in holidaysFor(center)

    /**
     * Festivos según centro hospitalario.
     * Basurto / Bilbao → Semana Grande; Cruces / Barakaldo → Carmen.
     */
    fun holidaysFor(center: String?): Set<LocalDate> {
        val c = center?.trim().orEmpty()
        return when {
            c.equals("Cruces", ignoreCase = true) ||
                c.equals("Barakaldo", ignoreCase = true) -> baseBizkaia + barakaldoLocal
            c.equals("Basurto", ignoreCase = true) ||
                c.equals("Bilbao", ignoreCase = true) ||
                c.isBlank() -> baseBizkaia + bilbaoLocal
            else -> baseBizkaia + bilbaoLocal
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
