package com.lrhsoft.shifterclone.model

data class DayAssignment(
    val year: Int,
    val month: Int,
    val day: Int,
    val shiftIds: List<String> = emptyList()
) {
    val key: String get() = "$year-$month-$day"

    companion object {
        fun key(year: Int, month: Int, day: Int) = "$year-$month-$day"
    }
}
