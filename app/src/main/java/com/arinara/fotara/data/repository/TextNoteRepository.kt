// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.repository

import android.content.ContentValues
import android.content.Context
import android.util.Log
import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.data.model.TextNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface TextNoteRepository {
    fun getAllActiveTextNotes(): Flow<List<TextNote>>
    fun getTextNotesByFolder(folderId: Long, subfolderId: Long?): Flow<List<TextNote>>
    fun getTextNoteById(id: Long): Flow<TextNote?>
    suspend fun getTextNoteByIdOnce(id: Long): TextNote?
    suspend fun createTextNote(
        folderId: Long,
        subfolderId: Long?,
        title: String,
        bodyMarkdown: String,
        tagColor: String? = null,
        deadlineMs: Long? = null
    ): Long
    suspend fun updateTextNote(id: Long, title: String, bodyMarkdown: String)
    suspend fun renameTextNote(id: Long, newTitle: String)
    suspend fun updateTagColor(id: Long, colorHex: String?)
    suspend fun updateDeadline(id: Long, deadlineMs: Long?)
    suspend fun moveTextNote(id: Long, targetFolderId: Long, targetSubfolderId: Long?)
    suspend fun moveTextNotes(ids: List<Long>, targetFolderId: Long, targetSubfolderId: Long?)
    suspend fun deleteTextNote(id: Long)
    suspend fun deleteTextNotes(ids: List<Long>)
    suspend fun restoreTextNote(id: Long)
    suspend fun purgeTextNotePermanently(id: Long)
    fun getTrashedTextNotes(): Flow<List<TextNote>>
    fun searchNotes(query: String): Flow<List<TextNote>>
    suspend fun refresh()
}

class SqliteTextNoteRepository(
    private val dbHelper: FotaraDbHelper,
    private val folderRepository: FolderRepository? = null,
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : TextNoteRepository {

    private val scope = coroutineScope
    private val textNotesFlow = MutableStateFlow<List<TextNote>>(emptyList())
    private val trashedTextNotesFlow = MutableStateFlow<List<TextNote>>(emptyList())

    init {
        scope.launch {
            refreshSync()
        }
    }

    private fun refreshSync() {
        try {
            val db = dbHelper.getSafeReadableDatabase()
            val notes = mutableListOf<TextNote>()
            val trashed = mutableListOf<TextNote>()

            val cursor = db.rawQuery(
                """
                SELECT id, folder_id, subfolder_id, title, body_markdown,
                       created_at, updated_at, added_at, tag_color, linked_deadline,
                       is_trashed, deleted_at, scheduled_at, alert_type
                FROM text_notes
                ORDER BY updated_at DESC
                """.trimIndent(),
                null
            )

            cursor.use { c ->
                while (c.moveToNext()) {
                    val note = TextNote(
                        id = c.getLong(0),
                        folderId = c.getLong(1),
                        subfolderId = if (c.isNull(2)) null else c.getLong(2),
                        title = c.getString(3),
                        bodyMarkdown = c.getString(4),
                        createdAt = c.getLong(5),
                        updatedAt = c.getLong(6),
                        addedAt = c.getLong(7),
                        tagColor = if (c.isNull(8)) null else c.getString(8),
                        linkedDeadline = if (c.isNull(9)) null else c.getLong(9),
                        isTrashed = c.getInt(10) == 1,
                        deletedAt = if (c.isNull(11)) null else c.getLong(11),
                        scheduledAt = if (c.columnCount > 12 && !c.isNull(12)) c.getLong(12) else null,
                        alertType = if (c.columnCount > 13 && !c.isNull(13)) c.getString(13) else null
                    )
                    if (note.isTrashed) {
                        trashed.add(note)
                    } else {
                        notes.add(note)
                    }
                }
            }

            textNotesFlow.value = notes
            trashedTextNotesFlow.value = trashed
            try {
                com.arinara.fotara.widget.DueTomorrowWidgetProvider.notifyDataChanged(dbHelper.context)
            } catch (_: Exception) {}
        } catch (e: Exception) {
            Log.e("SqliteTextNoteRepo", "Error refreshing text notes: ${e.message}", e)
        }
    }

    override fun getAllActiveTextNotes(): Flow<List<TextNote>> = textNotesFlow.asStateFlow()

    override fun getTextNotesByFolder(folderId: Long, subfolderId: Long?): Flow<List<TextNote>> =
        textNotesFlow.map { list ->
            list.filter { it.folderId == folderId && (subfolderId == null || it.subfolderId == subfolderId) }
        }

    override fun getTextNoteById(id: Long): Flow<TextNote?> =
        textNotesFlow.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun getTextNoteByIdOnce(id: Long): TextNote? = withContext(Dispatchers.IO) {
        textNotesFlow.value.firstOrNull { it.id == id }
            ?: trashedTextNotesFlow.value.firstOrNull { it.id == id }
    }

    override suspend fun createTextNote(
        folderId: Long,
        subfolderId: Long?,
        title: String,
        bodyMarkdown: String,
        tagColor: String?,
        deadlineMs: Long?
    ): Long = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("folder_id", folderId)
            if (subfolderId != null) put("subfolder_id", subfolderId) else putNull("subfolder_id")
            put("title", title.trim())
            put("body_markdown", bodyMarkdown)
            put("created_at", now)
            put("updated_at", now)
            put("added_at", now)
            if (tagColor != null) put("tag_color", tagColor) else putNull("tag_color")
            if (deadlineMs != null) put("linked_deadline", deadlineMs) else putNull("linked_deadline")
            put("is_trashed", 0)
        }
        val id = db.insert("text_notes", null, values)
        refreshSync()
        folderRepository?.refresh()
        id
    }

    override suspend fun updateTextNote(id: Long, title: String, bodyMarkdown: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("title", title.trim())
            put("body_markdown", bodyMarkdown)
            put("updated_at", now)
        }
        db.update("text_notes", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun renameTextNote(id: Long, newTitle: String) = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("title", newTitle.trim())
            put("updated_at", now)
        }
        db.update("text_notes", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun updateTagColor(id: Long, colorHex: String?) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            if (colorHex != null) put("tag_color", colorHex) else putNull("tag_color")
            put("updated_at", System.currentTimeMillis())
        }
        db.update("text_notes", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun updateDeadline(id: Long, deadlineMs: Long?) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            if (deadlineMs != null) put("linked_deadline", deadlineMs) else putNull("linked_deadline")
            put("updated_at", System.currentTimeMillis())
        }
        db.update("text_notes", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun moveTextNote(id: Long, targetFolderId: Long, targetSubfolderId: Long?) {
        moveTextNotes(listOf(id), targetFolderId, targetSubfolderId)
    }

    override suspend fun moveTextNotes(ids: List<Long>, targetFolderId: Long, targetSubfolderId: Long?) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("folder_id", targetFolderId)
            if (targetSubfolderId != null) put("subfolder_id", targetSubfolderId) else putNull("subfolder_id")
            put("updated_at", System.currentTimeMillis())
        }
        val placeholders = ids.joinToString(",") { "?" }
        db.update("text_notes", values, "id IN ($placeholders)", ids.map { it.toString() }.toTypedArray())
        refreshSync()
        folderRepository?.refresh()
        Unit
    }

    override suspend fun deleteTextNote(id: Long) {
        deleteTextNotes(listOf(id))
    }

    override suspend fun deleteTextNotes(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val db = dbHelper.getSafeWritableDatabase()
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("is_trashed", 1)
            put("deleted_at", now)
        }
        val placeholders = ids.joinToString(",") { "?" }
        db.update("text_notes", values, "id IN ($placeholders)", ids.map { it.toString() }.toTypedArray())
        for (id in ids) {
            try {
                com.arinara.fotara.util.NoteScheduleManager(dbHelper.context).cancelAlarmOnly(com.arinara.fotara.util.ScheduleNoteType.TEXT_NOTE, id)
            } catch (_: Exception) {}
        }
        refreshSync()
        folderRepository?.refresh()
        Unit
    }

    override suspend fun restoreTextNote(id: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("is_trashed", 0)
            putNull("deleted_at")
        }
        db.update("text_notes", values, "id = ?", arrayOf(id.toString()))
        val note = getTextNoteByIdOnce(id)
        if (note != null) {
            try {
                com.arinara.fotara.util.NoteScheduleManager(dbHelper.context).rearmAlarmIfFuture(note)
            } catch (_: Exception) {}
        }
        refreshSync()
        folderRepository?.refresh()
        Unit
    }

    override suspend fun purgeTextNotePermanently(id: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        db.delete("text_notes", "id = ?", arrayOf(id.toString()))
        refreshSync()
        folderRepository?.refresh()
        Unit
    }

    override fun getTrashedTextNotes(): Flow<List<TextNote>> = trashedTextNotesFlow.asStateFlow()

    override fun searchNotes(query: String): Flow<List<TextNote>> =
        textNotesFlow.map { notes ->
            val trimmed = query.trim().lowercase()
            if (trimmed.isBlank()) emptyList()
            else {
                val terms = trimmed.split(Regex("\\s+")).filter { it.isNotBlank() }
                notes.filter { note ->
                    val titleNorm = note.title.lowercase()
                    val plainBody = TextNote.stripMarkdownFormatting(note.bodyMarkdown).lowercase()
                    val rawBody = note.bodyMarkdown.lowercase()
                    titleNorm.contains(trimmed) ||
                    plainBody.contains(trimmed) ||
                    rawBody.contains(trimmed) ||
                    (terms.isNotEmpty() && terms.all { term ->
                        titleNorm.contains(term) || plainBody.contains(term) || rawBody.contains(term)
                    })
                }
            }
        }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        refreshSync()
    }
}
