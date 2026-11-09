package com.lrhsoft.shifterclone.model

import androidx.compose.ui.graphics.Color
import java.util.UUID

/**
 * Representa un tipo de turno configurable (Mañana, Tarde, Noche, Libre, etc.)
 */
data class Shift(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val abbreviation: String,
    val color: Long,
    val textColor: Long = 0xFFFFFFFF,
    val startHour: Int? = null,
    val startMinute: Int? = null,
    val endHour: Int? = null,
    val endMinute: Int? = null,
    val isRest: Boolean = false,
    /** Tarifa por hora (€ u otra moneda). null = no calcular ingresos */
    val hourlyRate: Double? = null
) {
    fun toComposeColor(): Color = Color(color)
    fun toTextComposeColor(): Color = Color(textColor)

    /**
     * Duración en horas decimales.
     * Si el fin es anterior al inicio (turno nocturno), se asume que cruza medianoche.
     */
    fun durationHours(): Double? {
        if (startHour == null || endHour == null) return null
        val startMin = startHour * 60 + (startMinute ?: 0)
        var endMin = endHour * 60 + (endMinute ?: 0)
        if (endMin <= startMin) {
            endMin += 24 * 60 // cruza medianoche
        }
        return (endMin - startMin) / 60.0
    }

    companion object {
        val DEFAULT_SHIFTS = listOf(
            Shift(
                id = "default-manana",
                name = "Mañana",
                abbreviation = "M",
                color = 0xFF4CAF50,
                startHour = 7, startMinute = 0,
                endHour = 15, endMinute = 0,
                hourlyRate = 12.0
            ),
            Shift(
                id = "default-tarde",
                name = "Tarde",
                abbreviation = "T",
                color = 0xFFFF9800,
                startHour = 15, startMinute = 0,
                endHour = 23, endMinute = 0,
                hourlyRate = 12.0
            ),
            Shift(
                id = "default-noche",
                name = "Noche",
                abbreviation = "N",
                color = 0xFF3F51B5,
                textColor = 0xFFFFFFFF,
                startHour = 23, startMinute = 0,
                endHour = 7, endMinute = 0,
                hourlyRate = 14.0
            ),
            Shift(
                id = "default-libre",
                name = "Libre",
                abbreviation = "L",
                color = 0xFF9E9E9E,
                isRest = true
            ),
            Shift(
                id = "default-vacaciones",
                name = "Vacaciones",
                abbreviation = "V",
                color = 0xFF00BCD4,
                isRest = true
            )
        )
    }
}
