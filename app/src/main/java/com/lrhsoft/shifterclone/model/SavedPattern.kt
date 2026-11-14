package com.lrhsoft.shifterclone.model

/**
 * Patrón reutilizable: secuencia de turnos (null = día libre / sin turno).
 */
data class SavedPattern(
    val id: String,
    val name: String,
    val sequence: List<String?>
) {
    val length: Int get() = sequence.size
}
