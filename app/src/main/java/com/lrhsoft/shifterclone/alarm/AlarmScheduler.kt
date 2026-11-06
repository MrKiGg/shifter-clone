package com.lrhsoft.shifterclone.alarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.lrhsoft.shifterclone.model.DayNote
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun schedule(note: DayNote) {
        if (!note.hasAlarm) {
            cancel(note)
            return
        }

        val hour = note.alarmHour!!
        val minute = note.alarmMinute!!

        var targetDate = LocalDate.of(note.year, note.month, note.day)
        if (note.alarmDayBefore) {
            targetDate = targetDate.minusDays(1)
        }

        var dateTime = LocalDateTime.of(targetDate.year, targetDate.monthValue, targetDate.dayOfMonth, hour, minute)

        // Si la hora ya pasó, no programar (o programar al año siguiente si quieres; aquí solo futuras)
        val now = LocalDateTime.now()
        if (dateTime.isBefore(now)) {
            // No programar alarmas pasadas
            cancel(note)
            return
        }

        val triggerAt = dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val title = if (note.alarmDayBefore) {
            "Recordatorio (día anterior)"
        } else {
            "Recordatorio"
        }
        val body = buildString {
            append("${note.day}/${note.month}/${note.year}")
            if (note.hasText) {
                append(" · ")
                append(note.text.take(80))
            } else {
                append(" · Alarma de turno/nota")
            }
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra(AlarmReceiver.EXTRA_TITLE, title)
            putExtra(AlarmReceiver.EXTRA_BODY, body)
            putExtra(AlarmReceiver.EXTRA_NOTE_KEY, note.key)
        }

        val pending = PendingIntent.getBroadcast(
            context,
            note.key.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAt,
                    pending
                )
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAt, pending)
            }
        } catch (e: SecurityException) {
            // En Android 12+ puede hacer falta SCHEDULE_EXACT_ALARM
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAt, pending)
        }
    }

    fun cancel(note: DayNote) {
        cancel(note.key)
    }

    fun cancel(noteKey: String) {
        val intent = Intent(context, AlarmReceiver::class.java)
        val pending = PendingIntent.getBroadcast(
            context,
            noteKey.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(pending)
    }

    fun rescheduleAll(notes: Collection<DayNote>) {
        notes.filter { it.hasAlarm }.forEach { schedule(it) }
    }
}
