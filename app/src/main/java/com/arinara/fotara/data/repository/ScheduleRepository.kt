// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.repository

import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.feature.schedule.engine.DisableDuration
import com.arinara.fotara.feature.schedule.engine.ScheduleDisableEngine
import com.arinara.fotara.feature.schedule.model.ClassSchedule
import com.arinara.fotara.feature.schedule.model.DisabledDaySchedule
import com.arinara.fotara.feature.schedule.model.ScheduleDayNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Calendar

interface ScheduleRepository {
    fun getAllSchedules(): Flow<List<ClassSchedule>>
    fun getSchedulesByDay(dayOfWeek: Int): Flow<List<ClassSchedule>>
    suspend fun getAllSchedulesOnce(): List<ClassSchedule>
    suspend fun getSchedulesByDayOnce(dayOfWeek: Int): List<ClassSchedule>
    suspend fun getScheduleById(id: Long): ClassSchedule?
    suspend fun insertSchedule(schedule: ClassSchedule): Long
    suspend fun updateSchedule(schedule: ClassSchedule): Boolean
    suspend fun deleteSchedule(id: Long): Boolean
    suspend fun batchInsert(schedules: List<ClassSchedule>): Int
    suspend fun deleteAllSchedules(): Boolean
    suspend fun refresh()

    // Phase 99: Per-day Notes
    fun getAllDayNotes(): Flow<List<ScheduleDayNote>> = flowOf(emptyList())
    fun getDayNotes(dayOfWeek: Int): Flow<List<ScheduleDayNote>> = flowOf(emptyList())
    suspend fun getDayNotesOnce(dayOfWeek: Int): List<ScheduleDayNote> = emptyList()
    suspend fun saveDayNote(note: ScheduleDayNote): Long = 0L
    suspend fun deleteDayNote(id: Long): Boolean = false

    // Phase 99: Disabled Schedule Days
    fun getDisabledDays(): Flow<Map<Int, DisabledDaySchedule>> = flowOf(emptyMap())
    suspend fun getDisabledDaysOnce(): Map<Int, DisabledDaySchedule> = emptyMap()
    suspend fun isDayDisabled(dayOfWeek: Int, nowMs: Long = System.currentTimeMillis()): Boolean = false
    suspend fun disableDay(
        dayOfWeek: Int,
        duration: DisableDuration,
        nowCalendar: Calendar = Calendar.getInstance(),
        cutoffMinute: Int = 18 * 60
    ): Boolean = false
    suspend fun reenableDay(dayOfWeek: Int): Boolean = false
}

class SqliteScheduleRepository(
    private val dbHelper: FotaraDbHelper,
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : ScheduleRepository {

    private val scope = coroutineScope
    private val schedulesFlow = MutableStateFlow<List<ClassSchedule>>(emptyList())
    private val dayNotesFlow = MutableStateFlow<List<ScheduleDayNote>>(emptyList())
    private val disabledDaysFlow = MutableStateFlow<Map<Int, DisabledDaySchedule>>(emptyMap())

    init {
        scope.launch {
            refreshSync()
        }
    }

    override fun getAllSchedules(): Flow<List<ClassSchedule>> = schedulesFlow.asStateFlow()

    override fun getSchedulesByDay(dayOfWeek: Int): Flow<List<ClassSchedule>> {
        return schedulesFlow.map { list ->
            list.filter { it.dayOfWeek == dayOfWeek }
                .sortedBy { it.startMinute }
        }
    }

    override suspend fun getAllSchedulesOnce(): List<ClassSchedule> = withContext(Dispatchers.IO) {
        loadAllFromDb()
    }

    override suspend fun getSchedulesByDayOnce(dayOfWeek: Int): List<ClassSchedule> = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeReadableDatabase()
        val result = mutableListOf<ClassSchedule>()
        db.rawQuery(
            """
            SELECT id, day_of_week, start_minute, end_minute, subject_name,
                   room_name, instructor_name, linked_folder_id, color_hex,
                   created_at, updated_at
            FROM class_schedules
            WHERE day_of_week = ?
            ORDER BY start_minute ASC
            """.trimIndent(),
            arrayOf(dayOfWeek.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                result.add(cursorToSchedule(cursor))
            }
        }
        result
    }

    override suspend fun getScheduleById(id: Long): ClassSchedule? = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeReadableDatabase()
        db.rawQuery(
            """
            SELECT id, day_of_week, start_minute, end_minute, subject_name,
                   room_name, instructor_name, linked_folder_id, color_hex,
                   created_at, updated_at
            FROM class_schedules
            WHERE id = ?
            LIMIT 1
            """.trimIndent(),
            arrayOf(id.toString())
        ).use { cursor ->
            if (cursor.moveToNext()) {
                cursorToSchedule(cursor)
            } else {
                null
            }
        }
    }

    override suspend fun insertSchedule(schedule: ClassSchedule): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val cv = ContentValues().apply {
            put("day_of_week", schedule.dayOfWeek)
            put("start_minute", schedule.startMinute)
            put("end_minute", schedule.endMinute)
            put("subject_name", schedule.subjectName)
            put("room_name", schedule.roomName)
            put("instructor_name", schedule.instructorName)
            put("linked_folder_id", schedule.linkedFolderId)
            put("color_hex", schedule.colorHex ?: "#2563EB")
            put("created_at", schedule.createdAt)
            put("updated_at", schedule.updatedAt)
        }
        val id = db.insert("class_schedules", null, cv)
        if (id > 0) {
            refreshSync()
        }
        id
    }

    override suspend fun updateSchedule(schedule: ClassSchedule): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val cv = ContentValues().apply {
            put("day_of_week", schedule.dayOfWeek)
            put("start_minute", schedule.startMinute)
            put("end_minute", schedule.endMinute)
            put("subject_name", schedule.subjectName)
            put("room_name", schedule.roomName)
            put("instructor_name", schedule.instructorName)
            put("linked_folder_id", schedule.linkedFolderId)
            put("color_hex", schedule.colorHex ?: "#2563EB")
            put("updated_at", System.currentTimeMillis())
        }
        val affected = db.update("class_schedules", cv, "id = ?", arrayOf(schedule.id.toString()))
        if (affected > 0) {
            refreshSync()
            true
        } else {
            false
        }
    }

    override suspend fun deleteSchedule(id: Long): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val affected = db.delete("class_schedules", "id = ?", arrayOf(id.toString()))
        if (affected > 0) {
            refreshSync()
            true
        } else {
            false
        }
    }

    override suspend fun batchInsert(schedules: List<ClassSchedule>): Int = withContext(Dispatchers.IO) {
        if (schedules.isEmpty()) return@withContext 0
        val db = dbHelper.getSafeWritableDatabase()
        var insertedCount = 0
        db.beginTransaction()
        try {
            for (schedule in schedules) {
                val cv = ContentValues().apply {
                    put("day_of_week", schedule.dayOfWeek)
                    put("start_minute", schedule.startMinute)
                    put("end_minute", schedule.endMinute)
                    put("subject_name", schedule.subjectName)
                    put("room_name", schedule.roomName)
                    put("instructor_name", schedule.instructorName)
                    put("linked_folder_id", schedule.linkedFolderId)
                    put("color_hex", schedule.colorHex ?: "#2563EB")
                    put("created_at", schedule.createdAt)
                    put("updated_at", schedule.updatedAt)
                }
                val rowId = db.insert("class_schedules", null, cv)
                if (rowId > 0) insertedCount++
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            Log.e("SqliteScheduleRepo", "Failed batch insert", e)
            insertedCount = 0
        } finally {
            db.endTransaction()
        }
        if (insertedCount > 0) {
            refreshSync()
        }
        insertedCount
    }

    override suspend fun deleteAllSchedules(): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val affected = db.delete("class_schedules", null, null)
        refreshSync()
        affected > 0
    }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        refreshSync()
    }

    // -------------------------------------------------------------
    // Phase 99: Per-day Notes Implementation
    // -------------------------------------------------------------

    override fun getAllDayNotes(): Flow<List<ScheduleDayNote>> = dayNotesFlow

    override fun getDayNotes(dayOfWeek: Int): Flow<List<ScheduleDayNote>> {
        return dayNotesFlow.map { list ->
            list.filter { it.dayOfWeek == dayOfWeek }
                .sortedByDescending { it.updatedAt }
        }
    }

    override suspend fun getDayNotesOnce(dayOfWeek: Int): List<ScheduleDayNote> = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeReadableDatabase()
        val results = mutableListOf<ScheduleDayNote>()
        try {
            db.rawQuery(
                """
                SELECT id, day_of_week, date_millis, note_text, is_repeating_weekly, created_at, updated_at
                FROM schedule_day_notes
                WHERE day_of_week = ?
                ORDER BY updated_at DESC
                """.trimIndent(),
                arrayOf(dayOfWeek.toString())
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    results.add(cursorToDayNote(cursor))
                }
            }
        } catch (e: Exception) {
            Log.e("SqliteScheduleRepo", "Failed to query schedule_day_notes", e)
        }
        results
    }

    override suspend fun saveDayNote(note: ScheduleDayNote): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val cv = ContentValues().apply {
            put("day_of_week", note.dayOfWeek)
            put("date_millis", note.dateMillis)
            put("note_text", note.noteText)
            put("is_repeating_weekly", if (note.isRepeatingWeekly) 1 else 0)
            put("created_at", note.createdAt)
            put("updated_at", System.currentTimeMillis())
        }
        val id: Long
        if (note.id > 0) {
            val affected = db.update("schedule_day_notes", cv, "id = ?", arrayOf(note.id.toString()))
            id = if (affected > 0) note.id else 0L
        } else {
            id = db.insert("schedule_day_notes", null, cv)
        }
        if (id > 0) {
            refreshSync()
        }
        id
    }

    override suspend fun deleteDayNote(id: Long): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val affected = db.delete("schedule_day_notes", "id = ?", arrayOf(id.toString()))
        if (affected > 0) {
            refreshSync()
            true
        } else {
            false
        }
    }

    // -------------------------------------------------------------
    // Phase 99: Disabled Days Implementation
    // -------------------------------------------------------------

    override fun getDisabledDays(): Flow<Map<Int, DisabledDaySchedule>> = disabledDaysFlow.asStateFlow()

    override suspend fun getDisabledDaysOnce(): Map<Int, DisabledDaySchedule> = withContext(Dispatchers.IO) {
        loadDisabledDaysFromDb()
    }

    override suspend fun isDayDisabled(dayOfWeek: Int, nowMs: Long): Boolean = withContext(Dispatchers.IO) {
        val map = loadDisabledDaysFromDb()
        val record = map[dayOfWeek]
        ScheduleDisableEngine.isDayDisabled(record, nowMs)
    }

    override suspend fun disableDay(
        dayOfWeek: Int,
        duration: DisableDuration,
        nowCalendar: Calendar,
        cutoffMinute: Int
    ): Boolean = withContext(Dispatchers.IO) {
        val expiryMs = ScheduleDisableEngine.calculateExpiry(
            duration = duration,
            targetDayOfWeek = dayOfWeek,
            nowCalendar = nowCalendar,
            cutoffMinute = cutoffMinute
        )
        val db = dbHelper.getSafeWritableDatabase()
        val cv = ContentValues().apply {
            put("day_of_week", dayOfWeek)
            put("disabled_until_ms", expiryMs)
            put("mode", duration.name)
            put("updated_at", System.currentTimeMillis())
        }
        val rowId = db.insertWithOnConflict(
            "disabled_schedule_days",
            null,
            cv,
            android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE
        )
        if (rowId >= 0) {
            refreshSync()
            true
        } else {
            false
        }
    }

    override suspend fun reenableDay(dayOfWeek: Int): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val affected = db.delete("disabled_schedule_days", "day_of_week = ?", arrayOf(dayOfWeek.toString()))
        if (affected > 0) {
            refreshSync()
            true
        } else {
            false
        }
    }

    // -------------------------------------------------------------
    // Internal Sync & Cursors
    // -------------------------------------------------------------

    private fun refreshSync() {
        val items = loadAllFromDb()
        val notes = loadAllNotesFromDb()
        val disabled = loadDisabledDaysFromDb()

        schedulesFlow.value = items
        dayNotesFlow.value = notes
        disabledDaysFlow.value = disabled

        try {
            com.arinara.fotara.widget.TimetableGlanceWidgetReceiver.triggerUpdate(dbHelper.context)
        } catch (_: Exception) {}
    }

    private fun loadAllFromDb(): List<ClassSchedule> {
        val db = dbHelper.getSafeReadableDatabase()
        val results = mutableListOf<ClassSchedule>()
        try {
            db.rawQuery(
                """
                SELECT id, day_of_week, start_minute, end_minute, subject_name,
                       room_name, instructor_name, linked_folder_id, color_hex,
                       created_at, updated_at
                FROM class_schedules
                ORDER BY day_of_week ASC, start_minute ASC
                """.trimIndent(),
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    results.add(cursorToSchedule(cursor))
                }
            }
        } catch (e: Exception) {
            Log.e("SqliteScheduleRepo", "Error reading class_schedules", e)
        }
        return results
    }

    private fun loadAllNotesFromDb(): List<ScheduleDayNote> {
        val db = dbHelper.getSafeReadableDatabase()
        val results = mutableListOf<ScheduleDayNote>()
        try {
            db.rawQuery(
                """
                SELECT id, day_of_week, date_millis, note_text, is_repeating_weekly, created_at, updated_at
                FROM schedule_day_notes
                ORDER BY day_of_week ASC, updated_at DESC
                """.trimIndent(),
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    results.add(cursorToDayNote(cursor))
                }
            }
        } catch (e: Exception) {
            Log.e("SqliteScheduleRepo", "Error reading schedule_day_notes", e)
        }
        return results
    }

    private fun loadDisabledDaysFromDb(): Map<Int, DisabledDaySchedule> {
        val db = dbHelper.getSafeReadableDatabase()
        val results = mutableMapOf<Int, DisabledDaySchedule>()
        val expiredDays = mutableListOf<Int>()
        val now = System.currentTimeMillis()

        try {
            db.rawQuery(
                """
                SELECT day_of_week, disabled_until_ms, mode, updated_at
                FROM disabled_schedule_days
                """.trimIndent(),
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val record = cursorToDisabledDay(cursor)
                    if (record.isExpired(now)) {
                        expiredDays.add(record.dayOfWeek)
                    } else {
                        results[record.dayOfWeek] = record
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SqliteScheduleRepo", "Error reading disabled_schedule_days", e)
        }

        // Clean up expired records automatically
        if (expiredDays.isNotEmpty()) {
            scope.launch {
                try {
                    val writeDb = dbHelper.getSafeWritableDatabase()
                    for (day in expiredDays) {
                        writeDb.delete("disabled_schedule_days", "day_of_week = ?", arrayOf(day.toString()))
                    }
                } catch (_: Exception) {}
            }
        }

        return results
    }

    private fun cursorToSchedule(cursor: Cursor): ClassSchedule {
        return ClassSchedule(
            id = cursor.getLong(0),
            dayOfWeek = cursor.getInt(1),
            startMinute = cursor.getInt(2),
            endMinute = cursor.getInt(3),
            subjectName = cursor.getString(4) ?: "",
            roomName = cursor.getString(5),
            instructorName = cursor.getString(6),
            linkedFolderId = if (cursor.isNull(7)) null else cursor.getLong(7),
            colorHex = cursor.getString(8) ?: "#2563EB",
            createdAt = cursor.getLong(9),
            updatedAt = cursor.getLong(10)
        )
    }

    private fun cursorToDayNote(cursor: Cursor): ScheduleDayNote {
        return ScheduleDayNote(
            id = cursor.getLong(0),
            dayOfWeek = cursor.getInt(1),
            dateMillis = cursor.getLong(2),
            noteText = cursor.getString(3) ?: "",
            isRepeatingWeekly = cursor.getInt(4) == 1,
            createdAt = cursor.getLong(5),
            updatedAt = cursor.getLong(6)
        )
    }

    private fun cursorToDisabledDay(cursor: Cursor): DisabledDaySchedule {
        return DisabledDaySchedule(
            dayOfWeek = cursor.getInt(0),
            disabledUntilMs = cursor.getLong(1),
            mode = cursor.getString(2) ?: "ONLY_THIS_DAY",
            updatedAt = cursor.getLong(3)
        )
    }
}
