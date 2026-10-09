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
import com.arinara.fotara.feature.schedule.model.ClassSchedule
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
}

class SqliteScheduleRepository(
    private val dbHelper: FotaraDbHelper,
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : ScheduleRepository {

    private val scope = coroutineScope
    private val schedulesFlow = MutableStateFlow<List<ClassSchedule>>(emptyList())

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

    private fun refreshSync() {
        val items = loadAllFromDb()
        schedulesFlow.value = items
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
}
