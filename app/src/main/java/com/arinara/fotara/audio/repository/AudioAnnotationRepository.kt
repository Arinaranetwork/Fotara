// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.audio.repository

import android.content.ContentValues
import android.database.Cursor
import android.util.Log
import com.arinara.fotara.audio.model.AudioAnnotation
import com.arinara.fotara.data.db.FotaraDbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import java.io.File

interface AudioAnnotationRepository {
    fun getAnnotationsForNote(noteId: Long): Flow<List<AudioAnnotation>>
    fun getAnnotationsForPdfPage(pdfDocId: Long, pageIndex: Int): Flow<List<AudioAnnotation>>
    fun getAllAnnotationsForPdf(pdfDocId: Long): Flow<List<AudioAnnotation>>
    suspend fun getAnnotationById(id: Long): AudioAnnotation?
    suspend fun insertAnnotation(annotation: AudioAnnotation): Long
    suspend fun deleteAnnotation(id: Long)
    suspend fun deleteAnnotationFile(filePath: String)
}

class SqliteAudioAnnotationRepository(
    private val dbHelper: FotaraDbHelper
) : AudioAnnotationRepository {

    private val updateNotifier = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    init {
        ensureTableExists()
    }

    private fun ensureTableExists() {
        try {
            val db = dbHelper.writableDatabase
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS audio_annotations (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    note_id INTEGER,
                    pdf_doc_id INTEGER,
                    pdf_page_index INTEGER,
                    file_path TEXT NOT NULL,
                    duration_ms INTEGER NOT NULL,
                    created_at INTEGER NOT NULL
                );
                """.trimIndent()
            )
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_audio_annotations_note_id ON audio_annotations(note_id)")
            db.execSQL("CREATE INDEX IF NOT EXISTS idx_audio_annotations_pdf ON audio_annotations(pdf_doc_id, pdf_page_index)")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to ensure audio_annotations table exists", e)
        }
    }

    override fun getAnnotationsForNote(noteId: Long): Flow<List<AudioAnnotation>> = flow {
        updateNotifier.onStart { emit(Unit) }.collect {
            emit(queryAnnotations("note_id = ?", arrayOf(noteId.toString())))
        }
    }.flowOn(Dispatchers.IO)

    override fun getAnnotationsForPdfPage(pdfDocId: Long, pageIndex: Int): Flow<List<AudioAnnotation>> = flow {
        updateNotifier.onStart { emit(Unit) }.collect {
            emit(
                queryAnnotations(
                    "pdf_doc_id = ? AND pdf_page_index = ?",
                    arrayOf(pdfDocId.toString(), pageIndex.toString())
                )
            )
        }
    }.flowOn(Dispatchers.IO)

    override fun getAllAnnotationsForPdf(pdfDocId: Long): Flow<List<AudioAnnotation>> = flow {
        updateNotifier.onStart { emit(Unit) }.collect {
            emit(queryAnnotations("pdf_doc_id = ?", arrayOf(pdfDocId.toString())))
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getAnnotationById(id: Long): AudioAnnotation? = withContext(Dispatchers.IO) {
        val list = queryAnnotations("id = ?", arrayOf(id.toString()))
        list.firstOrNull()
    }

    override suspend fun insertAnnotation(annotation: AudioAnnotation): Long = withContext(Dispatchers.IO) {
        try {
            ensureTableExists()
            val db = dbHelper.writableDatabase
            val values = ContentValues().apply {
                if (annotation.noteId != null) put("note_id", annotation.noteId) else putNull("note_id")
                if (annotation.pdfDocId != null) put("pdf_doc_id", annotation.pdfDocId) else putNull("pdf_doc_id")
                if (annotation.pdfPageIndex != null) put("pdf_page_index", annotation.pdfPageIndex) else putNull("pdf_page_index")
                put("file_path", annotation.filePath)
                put("duration_ms", annotation.durationMs)
                put("created_at", annotation.createdAt)
            }
            val rowId = db.insert("audio_annotations", null, values)
            if (rowId != -1L) {
                updateNotifier.tryEmit(Unit)
            }
            rowId
        } catch (e: Exception) {
            Log.e(TAG, "Failed to insert audio annotation", e)
            -1L
        }
    }

    override suspend fun deleteAnnotation(id: Long): Unit = withContext(Dispatchers.IO) {
        try {
            val existing = getAnnotationById(id)
            if (existing != null) {
                deleteAnnotationFile(existing.filePath)
            }
            val db = dbHelper.writableDatabase
            val deleted = db.delete("audio_annotations", "id = ?", arrayOf(id.toString()))
            if (deleted > 0) {
                updateNotifier.tryEmit(Unit)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete audio annotation with id $id", e)
        }
    }

    override suspend fun deleteAnnotationFile(filePath: String): Unit = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (file.exists()) {
                file.delete()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete audio file at $filePath", e)
        }
    }

    private fun queryAnnotations(selection: String, selectionArgs: Array<String>): List<AudioAnnotation> {
        val results = mutableListOf<AudioAnnotation>()
        try {
            ensureTableExists()
            val db = dbHelper.readableDatabase
            db.query(
                "audio_annotations",
                arrayOf("id", "note_id", "pdf_doc_id", "pdf_page_index", "file_path", "duration_ms", "created_at"),
                selection,
                selectionArgs,
                null,
                null,
                "created_at ASC"
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    results.add(mapCursor(cursor))
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query audio annotations", e)
        }
        return results
    }

    private fun mapCursor(cursor: Cursor): AudioAnnotation {
        val id = cursor.getLong(cursor.getColumnIndexOrThrow("id"))
        val noteId = if (cursor.isNull(cursor.getColumnIndexOrThrow("note_id"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("note_id"))
        val pdfDocId = if (cursor.isNull(cursor.getColumnIndexOrThrow("pdf_doc_id"))) null else cursor.getLong(cursor.getColumnIndexOrThrow("pdf_doc_id"))
        val pdfPageIndex = if (cursor.isNull(cursor.getColumnIndexOrThrow("pdf_page_index"))) null else cursor.getInt(cursor.getColumnIndexOrThrow("pdf_page_index"))
        val filePath = cursor.getString(cursor.getColumnIndexOrThrow("file_path"))
        val durationMs = cursor.getLong(cursor.getColumnIndexOrThrow("duration_ms"))
        val createdAt = cursor.getLong(cursor.getColumnIndexOrThrow("created_at"))
        return AudioAnnotation(
            id = id,
            noteId = noteId,
            pdfDocId = pdfDocId,
            pdfPageIndex = pdfPageIndex,
            filePath = filePath,
            durationMs = durationMs,
            createdAt = createdAt
        )
    }

    companion object {
        private const val TAG = "AudioAnnotationRepo"
    }
}
