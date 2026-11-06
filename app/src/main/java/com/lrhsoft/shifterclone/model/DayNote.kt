package com.lrhsoft.shifterclone.model

data class DayNote(
    val year: Int,
    val month: Int,
    val day: Int,
    val text: String = "",
    val alarmHour: Int? = null,
    val alarmMinute: Int? = null,
    val alarmDayBefore: Boolean = false,
    val enabled: Boolean = true
) {
    val key: String get() = "$year-$month-$day"
    val hasAlarm: Boolean get() = alarmHour != null && alarmMinute != null && enabled
    val hasText: Boolean get() = text.isNotBlank()
    val isEmpty: Boolean get() = !hasText && !hasAlarm

    companion object {
        fun key(year: Int, month: Int, day: Int) = "$year-$month-$day"
    }
}
