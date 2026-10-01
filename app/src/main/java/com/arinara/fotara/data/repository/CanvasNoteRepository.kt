// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.repository

import android.content.ContentValues
import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.data.model.CanvasNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface CanvasNoteRepository {
    fun getAllActiveCanvasNotes(): Flow<List<CanvasNote>>
    fun searchCanvasNotes(query: String): Flow<List<CanvasNote>>
    fun getCanvasNotesByFolder(folderId: Long, subfolderId: Long?): Flow<List<CanvasNote>>
    fun getCanvasNoteById(id: Long): Flow<CanvasNote?>
    suspend fun getCanvasNoteByIdOnce(id: Long): CanvasNote?
    suspend fun createCanvasNote(
        folderId: Long,
        subfolderId: Long?,
        title: String,
        dataBlob: ByteArray? = null,
        tagColor: String? = null,
        scheduledAt: Long? = null,
        alertType: String? = null
    ): Long
    suspend fun updateCanvasNoteData(id: Long, dataBlob: ByteArray, thumbnailPath: String?)
    suspend fun renameCanvasNote(id: Long, newTitle: String)
    suspend fun updateTagColor(id: Long, colorHex: String?)
    suspend fun updateDeadline(id: Long, deadlineMs: Long?)
    suspend fun deleteCanvasNote(id: Long)
    suspend fun deleteCanvasNotes(ids: List<Long>)
    suspend fun restoreCanvasNote(id: Long)
    suspend fun purgeCanvasNotePermanently(id: Long)
    suspend fun moveCanvasNote(id: Long, targetFolderId: Long, targetSubfolderId: Long?)
    suspend fun moveCanvasNotes(ids: List<Long>, targetFolderId: Long, targetSubfolderId: Long?)
    suspend fun refresh()
}

class SqliteCanvasNoteRepository(
    private val dbHelper: FotaraDbHelper,
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : CanvasNoteRepository {

    private val scope = coroutineScope
    private val canvasNotesFlow = MutableStateFlow<List<CanvasNote>>(emptyList())
    private val trashedCanvasNotesFlow = MutableStateFlow<List<CanvasNote>>(emptyList())

    init {
        scope.launch {
            refreshSync()
        }
    }

    private fun refreshSync() {
        try {
            val db = dbHelper.getSafeReadableDatabase()
            val notes = mutableListOf<CanvasNote>()
            val trashed = mutableListOf<CanvasNote>()

            val cursor = db.rawQuery(
                """
                SELECT id, folder_id, subfolder_id, title, data_blob, thumbnail_path,
                       created_at, updated_at, added_at, tag_color, linked_deadline,
                       scheduled_at, alert_type, is_trashed, deleted_at
                FROM canvas_notes
                ORDER BY updated_at DESC
                """.trimIndent(),
                null
            )

            cursor.use { c ->
                while (c.moveToNext()) {
                    val note = CanvasNote(
                        id = c.getLong(0),
                        folderId = c.getLong(1),
                        subfolderId = if (c.isNull(2)) null else c.getLong(2),
                        title = c.getString(3),
                        dataBlob = if (c.isNull(4)) null else c.getBlob(4),
                        thumbnailPath = if (c.isNull(5)) null else c.getString(5),
                        createdAt = c.getLong(6),
                        updatedAt = c.getLong(7),
                        addedAt = c.getLong(8),
                        tagColor = if (c.isNull(9)) null else c.getString(9),
                        linkedDeadline = if (c.isNull(10)) null else c.getLong(10),
                        scheduledAt = if (!c.isNull(11)) c.getLong(11) else null,
                        alertType = if (!c.isNull(12)) c.getString(12) else null,
                        isTrashed = c.getInt(13) == 1,
                        deletedAt = if (c.isNull(14)) null else c.getLong(14)
                    )
                    if (note.isTrashed) {
                        trashed.add(note)
                    } else {
                        notes.add(note)
                    }
                }
            }

            canvasNotesFlow.value = notes
            trashedCanvasNotesFlow.value = trashed
        } catch (e: Exception) {
            android.util.Log.e("SqliteCanvasRepo", "Failed to refresh canvas notes: ${e.message}", e)
        }
    }

    override fun getAllActiveCanvasNotes(): Flow<List<CanvasNote>> = canvasNotesFlow.asStateFlow()

    override fun searchCanvasNotes(query: String): Flow<List<CanvasNote>> =
        canvasNotesFlow.map { notes ->
            val trimmed = query.trim().lowercase()
            if (trimmed.isBlank()) emptyList()
            else {
                notes.filter { note ->
                    note.title.lowercase().contains(trimmed)
                }
            }
        }

    override fun getCanvasNotesByFolder(folderId: Long, subfolderId: Long?): Flow<List<CanvasNote>> {
        return canvasNotesFlow.map { list ->
            list.filter { note ->
                note.folderId == folderId && (subfolderId == null || note.subfolderId == subfolderId)
            }
        }
    }

    override fun getCanvasNoteById(id: Long): Flow<CanvasNote?> {
        return canvasNotesFlow.map { list -> list.find { it.id == id } }
    }

    override suspend fun getCanvasNoteByIdOnce(id: Long): CanvasNote? = withContext(Dispatchers.IO) {
        canvasNotesFlow.value.find { it.id == id }
    }

    override suspend fun createCanvasNote(
        folderId: Long,
        subfolderId: Long?,
        title: String,
        dataBlob: ByteArray?,
        tagColor: String?,
        scheduledAt: Long?,
        alertType: String?
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("folder_id", folderId)
            put("subfolder_id", subfolderId)
            put("title", title.trim())
            if (dataBlob != null) put("data_blob", dataBlob) else putNull("data_blob")
            put("created_at", now)
            put("updated_at", now)
            put("added_at", now)
            put("tag_color", tagColor)
            put("scheduled_at", scheduledAt)
            put("alert_type", alertType)
            put("is_trashed", 0)
        }
        val db = dbHelper.getSafeWritableDatabase()
        val id = db.insert("canvas_notes", null, values)
        refreshSync()
        id
    }

    override suspend fun updateCanvasNoteData(
        id: Long,
        dataBlob: ByteArray,
        thumbnailPath: String?
    ) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("data_blob", dataBlob)
            if (thumbnailPath != null) put("thumbnail_path", thumbnailPath)
            put("updated_at", now)
        }
        val db = dbHelper.getSafeWritableDatabase()
        db.update("canvas_notes", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun renameCanvasNote(id: Long, newTitle: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("title", newTitle.trim())
            put("updated_at", now)
        }
        val db = dbHelper.getSafeWritableDatabase()
        db.update("canvas_notes", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun updateTagColor(id: Long, colorHex: String?) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            if (colorHex != null) put("tag_color", colorHex) else putNull("tag_color")
        }
        val db = dbHelper.getSafeWritableDatabase()
        db.update("canvas_notes", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun updateDeadline(id: Long, deadlineMs: Long?) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            if (deadlineMs != null) put("linked_deadline", deadlineMs) else putNull("linked_deadline")
        }
        val db = dbHelper.getSafeWritableDatabase()
        db.update("canvas_notes", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun deleteCanvasNote(id: Long) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("is_trashed", 1)
            put("deleted_at", System.currentTimeMillis())
        }
        val db = dbHelper.getSafeWritableDatabase()
        db.update("canvas_notes", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun deleteCanvasNotes(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val db = dbHelper.getSafeWritableDatabase()
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            for (id in ids) {
                val values = ContentValues().apply {
                    put("is_trashed", 1)
                    put("deleted_at", now)
                }
                db.update("canvas_notes", values, "id = ?", arrayOf(id.toString()))
                try {
                    com.arinara.fotara.util.NoteScheduleManager(dbHelper.context).cancelAlarmOnly(com.arinara.fotara.util.ScheduleNoteType.CANVAS_NOTE, id)
                } catch (_: Exception) {}
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshSync()
    }

    override suspend fun restoreCanvasNote(id: Long) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("is_trashed", 0)
            putNull("deleted_at")
        }
        val db = dbHelper.getSafeWritableDatabase()
        db.update("canvas_notes", values, "id = ?", arrayOf(id.toString()))
        val note = getCanvasNoteById(id)
        if (note != null) {
            try {
                com.arinara.fotara.util.NoteScheduleManager(dbHelper.context).rearmAlarmIfFuture(note)
            } catch (_: Exception) {}
        }
        refreshSync()
    }

    override suspend fun purgeCanvasNotePermanently(id: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        db.beginTransaction()
        try {
            // Delete disk asset files
            try {
                db.rawQuery("SELECT file_path FROM canvas_assets WHERE canvas_id = ?", arrayOf(id.toString())).use { cursor ->
                    while (cursor.moveToNext()) {
                        val path = cursor.getString(0)
                        try { java.io.File(path).delete() } catch (_: Exception) {}
                    }
                }
            } catch (_: Exception) {}

            db.delete("canvas_elements", "canvas_id = ?", arrayOf(id.toString()))
            db.delete("canvas_layers", "canvas_id = ?", arrayOf(id.toString()))
            db.delete("canvas_assets", "canvas_id = ?", arrayOf(id.toString()))
            db.delete("canvas_notes", "id = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshSync()
    }

    override suspend fun moveCanvasNote(id: Long, targetFolderId: Long, targetSubfolderId: Long?) = withContext(Dispatchers.IO) {
        val values = ContentValues().apply {
            put("folder_id", targetFolderId)
            put("subfolder_id", targetSubfolderId)
            put("updated_at", System.currentTimeMillis())
        }
        val db = dbHelper.getSafeWritableDatabase()
        db.update("canvas_notes", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun moveCanvasNotes(ids: List<Long>, targetFolderId: Long, targetSubfolderId: Long?) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val db = dbHelper.getSafeWritableDatabase()
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            for (id in ids) {
                val values = ContentValues().apply {
                    put("folder_id", targetFolderId)
                    put("subfolder_id", targetSubfolderId)
                    put("updated_at", now)
                }
                db.update("canvas_notes", values, "id = ?", arrayOf(id.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshSync()
    }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        refreshSync()
    }
}
