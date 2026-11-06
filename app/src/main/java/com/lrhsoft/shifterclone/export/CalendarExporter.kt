package com.lrhsoft.shifterclone.export

import android.content.ContentUris
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.CalendarContract
import androidx.core.content.FileProvider
import com.lrhsoft.shifterclone.model.DayAssignment
import com.lrhsoft.shifterclone.model.Shift
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.TimeZone

data class DeviceCalendar(
    val id: Long,
    val name: String,
    val accountName: String,
    val color: Int?
)

data class ExportResult(
    val eventsCreated: Int,
    val message: String
)

class CalendarExporter(private val context: Context) {

    fun listCalendars(): List<DeviceCalendar> {
        val result = mutableListOf<DeviceCalendar>()
        val projection = arrayOf(
            CalendarContract.Calendars._ID,
            CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            CalendarContract.Calendars.ACCOUNT_NAME,
            CalendarContract.Calendars.CALENDAR_COLOR,
            CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL
        )
        try {
            context.contentResolver.query(
                CalendarContract.Calendars.CONTENT_URI,
                projection,
                "${CalendarContract.Calendars.VISIBLE} = 1 AND ${CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL} >= ?",
                arrayOf(CalendarContract.Calendars.CAL_ACCESS_CONTRIBUTOR.toString()),
                null
            )?.use { cursor ->
                val idIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars._ID)
                val nameIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME)
                val accIdx = cursor.getColumnIndexOrThrow(CalendarContract.Calendars.ACCOUNT_NAME)
                val colorIdx = cursor.getColumnIndex(CalendarContract.Calendars.CALENDAR_COLOR)
                while (cursor.moveToNext()) {
                    result.add(
                        DeviceCalendar(
                            id = cursor.getLong(idIdx),
                            name = cursor.getString(nameIdx) ?: "Calendario",
                            accountName = cursor.getString(accIdx) ?: "",
                            color = if (colorIdx >= 0 && !cursor.isNull(colorIdx)) cursor.getInt(colorIdx) else null
                        )
                    )
                }
            }
        } catch (_: SecurityException) {
            // Sin permiso
        }
        return result
    }

    /**
     * Exporta los turnos del rango [from, to] al calendario indicado.
     * Solo crea eventos para días con turnos que no son de descanso (o todos si includeRest).
     */
    fun exportToCalendar(
        calendarId: Long,
        from: LocalDate,
        to: LocalDate,
        assignments: Map<String, DayAssignment>,
        shifts: List<Shift>,
        includeRest: Boolean = false
    ): ExportResult {
        val shiftMap = shifts.associateBy { it.id }
        var created = 0
        val zone = ZoneId.systemDefault()
        val tzId = TimeZone.getDefault().id

        var cursor = from
        while (!cursor.isAfter(to)) {
            val key = "${cursor.year}-${cursor.monthValue}-${cursor.dayOfMonth}"
            val ids = assignments[key]?.shiftIds.orEmpty()
            for (id in ids) {
                val shift = shiftMap[id] ?: continue
                if (shift.isRest && !includeRest) continue

                val (startMillis, endMillis, allDay) = eventTimes(cursor, shift, zone)

                val values = ContentValues().apply {
                    put(CalendarContract.Events.CALENDAR_ID, calendarId)
                    put(CalendarContract.Events.TITLE, "Turno: ${shift.name} (${shift.abbreviation})")
                    put(CalendarContract.Events.DESCRIPTION, buildDescription(shift))
                    put(CalendarContract.Events.EVENT_TIMEZONE, tzId)
                    put(CalendarContract.Events.DTSTART, startMillis)
                    put(CalendarContract.Events.DTEND, endMillis)
                    put(CalendarContract.Events.ALL_DAY, if (allDay) 1 else 0)
                    // Marca para poder localizar/borrar exports previos si se desea
                    put(CalendarContract.Events.CUSTOM_APP_PACKAGE, context.packageName)
                    put(CalendarContract.Events.CUSTOM_APP_URI, "shifter://day/$key/${shift.id}")
                }

                try {
                    val uri = context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
                    if (uri != null) created++
                } catch (_: SecurityException) {
                    return ExportResult(0, "Sin permiso de escritura en el calendario")
                } catch (e: Exception) {
                    return ExportResult(created, "Error: ${e.message}")
                }
            }
            cursor = cursor.plusDays(1)
        }

        return ExportResult(
            eventsCreated = created,
            message = if (created == 0) {
                "No hay turnos para exportar en este periodo"
            } else {
                "Se exportaron $created eventos al calendario"
            }
        )
    }

    /**
     * Genera un archivo .ics y devuelve un Intent para compartirlo
     * (WhatsApp, email, Google Calendar, etc.).
     */
    fun createIcsShareIntent(
        from: LocalDate,
        to: LocalDate,
        assignments: Map<String, DayAssignment>,
        shifts: List<Shift>,
        includeRest: Boolean = false
    ): Intent {
        val ics = buildIcs(from, to, assignments, shifts, includeRest)
        val dir = File(context.cacheDir, "exports").apply { mkdirs() }
        val file = File(dir, "shifter_${from}_a_${to}.ics")
        file.writeText(ics)

        val uri: Uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            file
        )

        return Intent(Intent.ACTION_SEND).apply {
            type = "text/calendar"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Turnos $from – $to")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun buildDescription(shift: Shift): String = buildString {
        append("Exportado desde Shifter Clone\n")
        append("Turno: ${shift.name}\n")
        if (shift.startHour != null && shift.endHour != null) {
            val sm = (shift.startMinute ?: 0).toString().padStart(2, '0')
            val em = (shift.endMinute ?: 0).toString().padStart(2, '0')
            append("Horario: ${shift.startHour}:$sm – ${shift.endHour}:$em\n")
        }
        shift.hourlyRate?.let { append("Tarifa: $it €/h\n") }
    }

    private fun eventTimes(
        date: LocalDate,
        shift: Shift,
        zone: ZoneId
    ): Triple<Long, Long, Boolean> {
        if (shift.startHour == null || shift.endHour == null) {
            // Todo el día
            val start = date.atStartOfDay(zone).toInstant().toEpochMilli()
            val end = date.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli()
            return Triple(start, end, true)
        }
        val startDt = LocalDateTime.of(
            date.year, date.monthValue, date.dayOfMonth,
            shift.startHour, shift.startMinute ?: 0
        )
        var endDt = LocalDateTime.of(
            date.year, date.monthValue, date.dayOfMonth,
            shift.endHour, shift.endMinute ?: 0
        )
        if (!endDt.isAfter(startDt)) {
            endDt = endDt.plusDays(1)
        }
        return Triple(
            startDt.atZone(zone).toInstant().toEpochMilli(),
            endDt.atZone(zone).toInstant().toEpochMilli(),
            false
        )
    }

    private fun buildIcs(
        from: LocalDate,
        to: LocalDate,
        assignments: Map<String, DayAssignment>,
        shifts: List<Shift>,
        includeRest: Boolean
    ): String {
        val shiftMap = shifts.associateBy { it.id }
        val fmt = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss")
        val sb = StringBuilder()
        sb.appendLine("BEGIN:VCALENDAR")
        sb.appendLine("VERSION:2.0")
        sb.appendLine("PRODID:-//Shifter Clone//ES")
        sb.appendLine("CALSCALE:GREGORIAN")
        sb.appendLine("METHOD:PUBLISH")

        var cursor = from
        while (!cursor.isAfter(to)) {
            val key = "${cursor.year}-${cursor.monthValue}-${cursor.dayOfMonth}"
            val ids = assignments[key]?.shiftIds.orEmpty()
            for (id in ids) {
                val shift = shiftMap[id] ?: continue
                if (shift.isRest && !includeRest) continue

                val (startDt, endDt) = icsDateTimes(cursor, shift)
                sb.appendLine("BEGIN:VEVENT")
                sb.appendLine("UID:shifter-$key-${shift.id}@shifterclone")
                sb.appendLine("DTSTAMP:${LocalDateTime.now().format(fmt)}Z")
                if (shift.startHour == null) {
                    sb.appendLine("DTSTART;VALUE=DATE:${cursor.format(DateTimeFormatter.BASIC_ISO_DATE)}")
                    sb.appendLine("DTEND;VALUE=DATE:${cursor.plusDays(1).format(DateTimeFormatter.BASIC_ISO_DATE)}")
                } else {
                    sb.appendLine("DTSTART:${startDt.format(fmt)}")
                    sb.appendLine("DTEND:${endDt.format(fmt)}")
                }
                sb.appendLine("SUMMARY:Turno: ${shift.name} (${shift.abbreviation})")
                sb.appendLine("DESCRIPTION:Exportado desde Shifter Clone")
                sb.appendLine("END:VEVENT")
            }
            cursor = cursor.plusDays(1)
        }
        sb.appendLine("END:VCALENDAR")
        return sb.toString()
    }

    private fun icsDateTimes(date: LocalDate, shift: Shift): Pair<LocalDateTime, LocalDateTime> {
        if (shift.startHour == null || shift.endHour == null) {
            return date.atStartOfDay() to date.plusDays(1).atStartOfDay()
        }
        val start = LocalDateTime.of(date.year, date.month, date.dayOfMonth, shift.startHour, shift.startMinute ?: 0)
        var end = LocalDateTime.of(date.year, date.month, date.dayOfMonth, shift.endHour, shift.endMinute ?: 0)
        if (!end.isAfter(start)) end = end.plusDays(1)
        return start to end
    }
}
