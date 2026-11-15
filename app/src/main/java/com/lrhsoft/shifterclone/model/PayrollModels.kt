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
    // Específico de puesto orientativo; en el recibo 2024 C.R.C.G. ~656 €/mes
    POLIVALENTE("Celador/a polivalente", 14, 656.30),
    CON_ESP_JEFATURA("Celador/a con especialidad o jefatura", 14, 701.36),
    ENCARGADO_TURNO("Celador encargado de turno", 14, 701.36),
    AUTOPSIAS("Celador autopsias", 14, 656.30)
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
    /**
     * Complemento de destino por nivel.
     * Nivel 14: 364,40 €/mes deducido del recibo 13-15/05/2024 (36,44 × 30/3).
     */
    val destinoPorNivel: Map<Int, Double> = mapOf(
        14 to 364.40,
        15 to 365.80,
        16 to 390.92,
        13 to 315.62,
        12 to 290.53
    )

    /**
     * Tarifas Grupo E actualizadas con recibo real Osakidetza 2024 cuando es posible;
     * resto según manual de conceptos 2019.
     *
     * Sueldo 622,60 = 62,26 × 30/3 (recibo 3 días).
     * Turnicidad 53,50 = 5,35 × 30/3.
     * Nocturnidad hora 3,37 (recibo); noche completa ≈ 10 h × 3,37 = 33,70.
     */
    val ratesByGroup: Map<PayGroup, GroupPayRates> = mapOf(
        PayGroup.E to GroupPayRates(
            sueldoMes = 622.60,
            sueldoExtra = 622.60,
            antiguedadMes = 14.32,
            antiguedadExtra = 14.32,
            diferenciaAntiguedadMes = 16.38,
            turnicidadMes = 53.50,
            nocturnidadHora = 3.37,
            nocturnidadNoche = 33.70,
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
            turnicidadMes = 53.50,
            nocturnidadHora = 3.37,
            nocturnidadNoche = 33.70,
            festivoHora = 6.54,
            festivoDia = 45.77,
            festivoNoche = 65.39
        )
    )

    /** Divisor de prorrateo de conceptos fijos (confirmado con recibo de 3 días). */
    const val PRORATE_DIVISOR_DAYS = 30

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
    val workDays: Int = 0,
    /** Días de alta / contrato usados en el prorrateo de fijos. */
    val contractDays: Int = 0,
    /** Ratio aplicado a fijos (días_alta / 30, máx. 1 si mes completo). */
    val fixedRatio: Double = 1.0
)
