package com.lrhsoft.shifterclone.model

import java.time.LocalDate

enum class PayGroup(val label: String) {
    A("A / A1"),
    B("B / A2"),
    C("C / C1"),
    D("D / C2"),
    E("E")
}

enum class CeladorCategory(
    val label: String,
    val destinoLevel: Int,
    val especificoMensual: Double
) {
    POLIVALENTE("Celador/a polivalente", 14, 613.67),
    CON_ESP_JEFATURA("Celador/a con especialidad o jefatura", 14, 701.36),
    ENCARGADO_TURNO("Celador encargado de turno", 14, 701.36),
    AUTOPSIAS("Celador autopsias", 14, 613.67)
}

data class GroupPayRates(
    val sueldoMes: Double,
    val sueldoExtra: Double,
    val antiguedadMes: Double,
    val antiguedadExtra: Double,
    val diferenciaAntiguedadMes: Double,
    val turnicidadMes: Double,
    val nocturnidadHora: Double,
    val nocturnidadNoche: Double,
    val festivoHora: Double,
    val festivoDia: Double,
    val festivoNoche: Double
)

object PayrollTables {
    val destinoPorNivel: Map<Int, Double> = mapOf(
        14 to 340.74,
        15 to 365.80,
        16 to 390.92,
        13 to 315.62,
        12 to 290.53
    )

    val ratesByGroup: Map<PayGroup, GroupPayRates> = mapOf(
        PayGroup.E to GroupPayRates(
            sueldoMes = 582.11,
            sueldoExtra = 582.11,
            antiguedadMes = 14.32,
            antiguedadExtra = 14.32,
            diferenciaAntiguedadMes = 16.38,
            turnicidadMes = 50.00,
            nocturnidadHora = 3.04,
            nocturnidadNoche = 30.44,
            festivoHora = 6.54,
            festivoDia = 45.77,
            festivoNoche = 65.39
        ),
        PayGroup.D to GroupPayRates(
            sueldoMes = 636.01,
            sueldoExtra = 630.21,
            antiguedadMes = 19.02,
            antiguedadExtra = 18.84,
            diferenciaAntiguedadMes = 15.22,
            turnicidadMes = 50.00,
            nocturnidadHora = 3.04,
            nocturnidadNoche = 30.44,
            festivoHora = 6.54,
            festivoDia = 45.77,
            festivoNoche = 65.39
        )
    )

    fun rates(group: PayGroup): GroupPayRates =
        ratesByGroup[group] ?: ratesByGroup.getValue(PayGroup.E)
}

data class UserProfile(
    val category: CeladorCategory = CeladorCategory.POLIVALENTE,
    val group: PayGroup = PayGroup.E,
    val destinoLevel: Int = 14,
    val trienios: Int = 0,
    val hasTurnicidad: Boolean = true,
    val horasNocheCompleta: Double = 10.0,
    val countNightShiftAsFullNight: Boolean = true
) {
    val destinoMensual: Double
        get() = PayrollTables.destinoPorNivel[destinoLevel]
            ?: PayrollTables.destinoPorNivel.getValue(14)

    val especificoMensual: Double
        get() = category.especificoMensual
}

data class WorkContract(
    val id: String,
    val name: String,
    val startDate: String,
    val endDate: String?,
    val center: String = "",
    val hasTurnicidad: Boolean = true,
    val notes: String = ""
) {
    fun contains(date: LocalDate): Boolean {
        val start = LocalDate.parse(startDate)
        val end = endDate?.let { LocalDate.parse(it) }
        return !date.isBefore(start) && (end == null || !date.isAfter(end))
    }
}

data class PayrollEstimate(
    val fixedMonthly: Double,
    val variableFromShifts: Double,
    val totalEstimated: Double,
    val breakdown: List<Pair<String, Double>>,
    val nights: Int = 0,
    val festivoDays: Int = 0,
    val festivoNights: Int = 0,
    val workDays: Int = 0
)
