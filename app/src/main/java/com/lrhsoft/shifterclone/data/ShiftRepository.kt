package com.lrhsoft.shifterclone.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.lrhsoft.shifterclone.model.CeladorCategory
import com.lrhsoft.shifterclone.model.DayAssignment
import com.lrhsoft.shifterclone.model.DayNote
import com.lrhsoft.shifterclone.model.PayGroup
import com.lrhsoft.shifterclone.model.Shift
import com.lrhsoft.shifterclone.model.UserProfile
import com.lrhsoft.shifterclone.model.WorkContract
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "shifter_prefs")

class ShiftRepository(private val context: Context) {

    private val shiftsKey = stringPreferencesKey("shifts")
    private val assignmentsKey = stringPreferencesKey("assignments")
    private val notesKey = stringPreferencesKey("notes")
    private val profileKey = stringPreferencesKey("user_profile")
    private val contractsKey = stringPreferencesKey("work_contracts")

    val shiftsFlow: Flow<List<Shift>> = context.dataStore.data.map { prefs ->
        val json = prefs[shiftsKey] ?: return@map Shift.DEFAULT_SHIFTS
        parseShifts(json).ifEmpty { Shift.DEFAULT_SHIFTS }
    }

    suspend fun ensureDefaults() {
        context.dataStore.edit { prefs ->
            if (prefs[shiftsKey].isNullOrBlank()) {
                prefs[shiftsKey] = shiftsToJson(Shift.DEFAULT_SHIFTS)
            }
        }
    }

    suspend fun saveShifts(shifts: List<Shift>) {
        context.dataStore.edit { prefs ->
            prefs[shiftsKey] = shiftsToJson(shifts)
        }
    }

    val assignmentsFlow: Flow<Map<String, DayAssignment>> = context.dataStore.data.map { prefs ->
        val json = prefs[assignmentsKey] ?: return@map emptyMap()
        parseAssignments(json)
    }

    suspend fun saveAssignments(assignments: Map<String, DayAssignment>) {
        context.dataStore.edit { prefs ->
            prefs[assignmentsKey] = assignmentsToJson(assignments)
        }
    }

    suspend fun setShiftOnDay(year: Int, month: Int, day: Int, shiftId: String?, replace: Boolean = true) {
        context.dataStore.edit { prefs ->
            val current = parseAssignments(prefs[assignmentsKey] ?: "{}").toMutableMap()
            applyShiftToMap(current, year, month, day, shiftId, replace)
            prefs[assignmentsKey] = assignmentsToJson(current)
        }
    }

    suspend fun applyBulk(updates: Map<Triple<Int, Int, Int>, List<String>>) {
        context.dataStore.edit { prefs ->
            val current = parseAssignments(prefs[assignmentsKey] ?: "{}").toMutableMap()
            updates.forEach { (ymd, shiftIds) ->
                val (year, month, day) = ymd
                val key = DayAssignment.key(year, month, day)
                if (shiftIds.isEmpty()) {
                    current.remove(key)
                } else {
                    current[key] = DayAssignment(year, month, day, shiftIds)
                }
            }
            prefs[assignmentsKey] = assignmentsToJson(current)
        }
    }

    private fun applyShiftToMap(
        map: MutableMap<String, DayAssignment>,
        year: Int, month: Int, day: Int,
        shiftId: String?,
        replace: Boolean
    ) {
        val key = DayAssignment.key(year, month, day)
        val existing = map[key]?.shiftIds?.toMutableList() ?: mutableListOf()
        if (shiftId == null) {
            map.remove(key)
        } else if (replace || existing.isEmpty()) {
            map[key] = DayAssignment(year, month, day, listOf(shiftId))
        } else {
            if (existing.size < 2 && !existing.contains(shiftId)) {
                existing.add(shiftId)
                map[key] = DayAssignment(year, month, day, existing)
            } else {
                map[key] = DayAssignment(year, month, day, listOf(shiftId))
            }
        }
    }

    val notesFlow: Flow<Map<String, DayNote>> = context.dataStore.data.map { prefs ->
        val json = prefs[notesKey] ?: return@map emptyMap()
        parseNotes(json)
    }

    suspend fun saveNote(note: DayNote) {
        context.dataStore.edit { prefs ->
            val current = parseNotes(prefs[notesKey] ?: "{}").toMutableMap()
            if (note.isEmpty) {
                current.remove(note.key)
            } else {
                current[note.key] = note
            }
            prefs[notesKey] = notesToJson(current)
        }
    }

    suspend fun deleteNote(year: Int, month: Int, day: Int) {
        context.dataStore.edit { prefs ->
            val current = parseNotes(prefs[notesKey] ?: "{}").toMutableMap()
            current.remove(DayNote.key(year, month, day))
            prefs[notesKey] = notesToJson(current)
        }
    }

    val profileFlow: Flow<UserProfile> = context.dataStore.data.map { prefs ->
        val json = prefs[profileKey] ?: return@map UserProfile()
        parseProfile(json)
    }

    suspend fun saveProfile(profile: UserProfile) {
        context.dataStore.edit { prefs ->
            prefs[profileKey] = profileToJson(profile)
        }
    }

    val contractsFlow: Flow<List<WorkContract>> = context.dataStore.data.map { prefs ->
        val json = prefs[contractsKey] ?: return@map emptyList()
        parseContracts(json)
    }

    suspend fun saveContracts(contracts: List<WorkContract>) {
        context.dataStore.edit { prefs ->
            prefs[contractsKey] = contractsToJson(contracts)
        }
    }

    private fun shiftsToJson(shifts: List<Shift>): String {
        val arr = JSONArray()
        shifts.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("name", s.name)
                put("abbreviation", s.abbreviation)
                put("color", s.color)
                put("textColor", s.textColor)
                put("startHour", s.startHour)
                put("startMinute", s.startMinute)
                put("endHour", s.endHour)
                put("endMinute", s.endMinute)
                put("isRest", s.isRest)
                put("hourlyRate", s.hourlyRate)
            })
        }
        return arr.toString()
    }

    private fun parseShifts(json: String): List<Shift> {
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<Shift>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    Shift(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        abbreviation = o.getString("abbreviation"),
                        color = o.getLong("color"),
                        textColor = o.optLong("textColor", 0xFFFFFFFF),
                        startHour = if (o.isNull("startHour")) null else o.getInt("startHour"),
                        startMinute = if (o.isNull("startMinute")) null else o.getInt("startMinute"),
                        endHour = if (o.isNull("endHour")) null else o.getInt("endHour"),
                        endMinute = if (o.isNull("endMinute")) null else o.getInt("endMinute"),
                        isRest = o.optBoolean("isRest", false),
                        hourlyRate = if (o.isNull("hourlyRate")) null else o.getDouble("hourlyRate")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun assignmentsToJson(map: Map<String, DayAssignment>): String {
        val obj = JSONObject()
        map.forEach { (key, a) ->
            obj.put(key, JSONObject().apply {
                put("year", a.year)
                put("month", a.month)
                put("day", a.day)
                put("shiftIds", JSONArray(a.shiftIds))
            })
        }
        return obj.toString()
    }

    private fun parseAssignments(json: String): Map<String, DayAssignment> {
        return try {
            val obj = JSONObject(json)
            val result = mutableMapOf<String, DayAssignment>()
            obj.keys().forEach { key ->
                val o = obj.getJSONObject(key)
                val ids = mutableListOf<String>()
                val arr = o.getJSONArray("shiftIds")
                for (i in 0 until arr.length()) ids.add(arr.getString(i))
                result[key] = DayAssignment(
                    year = o.getInt("year"),
                    month = o.getInt("month"),
                    day = o.getInt("day"),
                    shiftIds = ids
                )
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun notesToJson(map: Map<String, DayNote>): String {
        val obj = JSONObject()
        map.forEach { (key, n) ->
            obj.put(key, JSONObject().apply {
                put("year", n.year)
                put("month", n.month)
                put("day", n.day)
                put("text", n.text)
                put("alarmHour", n.alarmHour)
                put("alarmMinute", n.alarmMinute)
                put("alarmDayBefore", n.alarmDayBefore)
                put("enabled", n.enabled)
            })
        }
        return obj.toString()
    }

    private fun parseNotes(json: String): Map<String, DayNote> {
        return try {
            val obj = JSONObject(json)
            val result = mutableMapOf<String, DayNote>()
            obj.keys().forEach { key ->
                val o = obj.getJSONObject(key)
                result[key] = DayNote(
                    year = o.getInt("year"),
                    month = o.getInt("month"),
                    day = o.getInt("day"),
                    text = o.optString("text", ""),
                    alarmHour = if (o.isNull("alarmHour")) null else o.getInt("alarmHour"),
                    alarmMinute = if (o.isNull("alarmMinute")) null else o.getInt("alarmMinute"),
                    alarmDayBefore = o.optBoolean("alarmDayBefore", false),
                    enabled = o.optBoolean("enabled", true)
                )
            }
            result
        } catch (e: Exception) {
            emptyMap()
        }
    }

    private fun profileToJson(p: UserProfile): String {
        return JSONObject().apply {
            put("category", p.category.name)
            put("group", p.group.name)
            put("destinoLevel", p.destinoLevel)
            put("trienios", p.trienios)
            put("hasTurnicidad", p.hasTurnicidad)
            put("horasNocheCompleta", p.horasNocheCompleta)
            put("countNightShiftAsFullNight", p.countNightShiftAsFullNight)
        }.toString()
    }

    private fun parseProfile(json: String): UserProfile {
        return try {
            val o = JSONObject(json)
            val cat = runCatching {
                CeladorCategory.valueOf(o.optString("category", CeladorCategory.POLIVALENTE.name))
            }.getOrDefault(CeladorCategory.POLIVALENTE)
            val group = runCatching {
                PayGroup.valueOf(o.optString("group", PayGroup.E.name))
            }.getOrDefault(PayGroup.E)
            UserProfile(
                category = cat,
                group = group,
                destinoLevel = o.optInt("destinoLevel", cat.destinoLevel),
                trienios = o.optInt("trienios", 0),
                hasTurnicidad = o.optBoolean("hasTurnicidad", true),
                horasNocheCompleta = o.optDouble("horasNocheCompleta", 10.0),
                countNightShiftAsFullNight = o.optBoolean("countNightShiftAsFullNight", true)
            )
        } catch (e: Exception) {
            UserProfile()
        }
    }

    private fun contractsToJson(list: List<WorkContract>): String {
        val arr = JSONArray()
        list.forEach { c ->
            arr.put(JSONObject().apply {
                put("id", c.id)
                put("name", c.name)
                put("startDate", c.startDate)
                put("endDate", c.endDate)
                put("center", c.center)
                put("hasTurnicidad", c.hasTurnicidad)
                put("notes", c.notes)
            })
        }
        return arr.toString()
    }

    private fun parseContracts(json: String): List<WorkContract> {
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<WorkContract>()
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                list.add(
                    WorkContract(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        startDate = o.getString("startDate"),
                        endDate = if (o.isNull("endDate") || !o.has("endDate")) null else o.getString("endDate").ifBlank { null },
                        center = o.optString("center", ""),
                        hasTurnicidad = o.optBoolean("hasTurnicidad", true),
                        notes = o.optString("notes", "")
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }
}
