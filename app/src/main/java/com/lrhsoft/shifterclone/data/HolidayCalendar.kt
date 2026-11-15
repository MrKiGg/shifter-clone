package com.lrhsoft.shifterclone.data

import java.time.LocalDate

/**
 * Festivos laborales de Euskadi (CAV) + Territorio Histórico de Bizkaia.
 *
 * - CAV: decretos BOPV (2025 Decreto 49/2024, 2026 Decreto 82/2025, 2027 Decreto 90/2026).
 * - Bizkaia (todo el territorio): 31 de julio, San Ignacio de Loyola
 *   (Resoluciones Delegación Territorial Trabajo Bizkaia).
 * - Bilbao (2.ª local típica): Viernes de la Semana Grande (agosto).
 *
 * Domingos se tratan aparte en [PayrollCalculator].
 */
object HolidayCalendar {

    private val euskadiHolidays: Set<LocalDate> = setOf(
        // ——— 2025 ———
        LocalDate.of(2025, 1, 1),   // Año Nuevo
        LocalDate.of(2025, 1, 6),   // Epifanía
        LocalDate.of(2025, 4, 17),  // Jueves Santo
        LocalDate.of(2025, 4, 18),  // Viernes Santo
        LocalDate.of(2025, 4, 21),  // Lunes de Pascua
        LocalDate.of(2025, 5, 1),   // Fiesta del Trabajo
        LocalDate.of(2025, 7, 25),  // Santiago Apóstol
        LocalDate.of(2025, 8, 15),  // Asunción
        LocalDate.of(2025, 11, 1),  // Todos los Santos
        LocalDate.of(2025, 12, 6),  // Constitución
        LocalDate.of(2025, 12, 8),  // Inmaculada
        LocalDate.of(2025, 12, 25), // Navidad

        // ——— 2026 ———
        LocalDate.of(2026, 1, 1),   // Año Nuevo
        LocalDate.of(2026, 1, 6),   // Epifanía
        LocalDate.of(2026, 3, 19),  // San José
        LocalDate.of(2026, 4, 2),   // Jueves Santo
        LocalDate.of(2026, 4, 3),   // Viernes Santo
        LocalDate.of(2026, 4, 6),   // Lunes de Pascua
        LocalDate.of(2026, 5, 1),   // Fiesta del Trabajo
        LocalDate.of(2026, 7, 25),  // Santiago Apóstol
        LocalDate.of(2026, 8, 15),  // Asunción
        LocalDate.of(2026, 10, 12), // Fiesta Nacional
        // 1 nov y 6 dic 2026 caen en domingo
        LocalDate.of(2026, 12, 8),  // Inmaculada
        LocalDate.of(2026, 12, 25), // Navidad

        // ——— 2027 (Decreto 90/2026) ———
        LocalDate.of(2027, 1, 1),   // Año Nuevo
        LocalDate.of(2027, 1, 6),   // Epifanía
        LocalDate.of(2027, 3, 25),  // Jueves Santo
        LocalDate.of(2027, 3, 26),  // Viernes Santo
        LocalDate.of(2027, 3, 29),  // Lunes de Pascua
        LocalDate.of(2027, 5, 1),   // Fiesta del Trabajo
        LocalDate.of(2027, 10, 7),  // Aniversario primer Gobierno de Euskadi
        LocalDate.of(2027, 10, 12), // Fiesta Nacional
        LocalDate.of(2027, 11, 1),  // Todos los Santos
        LocalDate.of(2027, 12, 6),  // Constitución
        LocalDate.of(2027, 12, 8),  // Inmaculada
        LocalDate.of(2027, 12, 25)  // Navidad
    )

    /** Festivo local común a todo el Territorio Histórico de Bizkaia. */
    private val bizkaiaTerritory: Set<LocalDate> = setOf(
        LocalDate.of(2025, 7, 31), // San Ignacio de Loyola
        LocalDate.of(2026, 7, 31), // San Ignacio de Loyola
        LocalDate.of(2027, 7, 31)  // San Ignacio de Loyola
    )

    /**
     * 2.ª fiesta local de Bilbao (Viernes de la Semana Grande).
     * 2026: 28 ago · 2027: 27 ago (según resoluciones BOB).
     */
    private val bilbaoLocal: Set<LocalDate> = setOf(
        LocalDate.of(2025, 8, 22), // Viernes Semana Grande 2025 (aprox. / habitual)
        LocalDate.of(2026, 8, 28), // Viernes Semana Grande 2026
        LocalDate.of(2027, 8, 27)  // Viernes Semana Grande 2027
    )

    private val allHolidays: Set<LocalDate> =
        euskadiHolidays + bizkaiaTerritory + bilbaoLocal

    fun isHoliday(date: LocalDate): Boolean = date in allHolidays

    fun isEuskadiHoliday(date: LocalDate): Boolean = date in euskadiHolidays

    fun isBizkaiaHoliday(date: LocalDate): Boolean =
        date in bizkaiaTerritory || date in bilbaoLocal

    fun holidaysIn(year: Int): List<LocalDate> =
        allHolidays.filter { it.year == year }.sorted()

    fun all(): Set<LocalDate> = allHolidays

    /**
     * Manual Osakidetza: noches del 24 y 31 de diciembre → doble nocturnidad.
     */
    fun isDoubleNightDate(date: LocalDate): Boolean =
        date.monthValue == 12 && (date.dayOfMonth == 24 || date.dayOfMonth == 31)

    /**
     * Manual Osakidetza: turnos de mañana y tarde del 25 de diciembre y del 1 de enero → doble festivo.
     */
    fun isDoubleFestivoDayShift(date: LocalDate): Boolean =
        (date.monthValue == 12 && date.dayOfMonth == 25) ||
            (date.monthValue == 1 && date.dayOfMonth == 1)
}
