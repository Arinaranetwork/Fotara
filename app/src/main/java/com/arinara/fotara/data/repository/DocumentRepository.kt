// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.repository

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.Xml
import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.DocumentPage
import com.arinara.fotara.data.model.DocumentType
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoSource
import com.arinara.fotara.data.storage.PhotoStorageManager
import com.arinara.fotara.ocr.OcrEngine
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.CancellationException
import com.arinara.fotara.util.PdfPasswordException
import com.arinara.fotara.util.PdfCorruptException
import org.xmlpull.v1.XmlPullParser
import java.io.File
import java.io.InputStream
import java.util.zip.ZipInputStream

interface DocumentRepository {
    fun getDocumentNotesByFolder(folderId: Long, subfolderId: Long?): Flow<List<DocumentNote>>
    fun getDocumentPages(documentNoteId: Long): Flow<List<DocumentPage>>
    suspend fun getDocumentNoteById(id: Long): DocumentNote?
    suspend fun importPdf(
        uri: Uri,
        folderId: Long,
        subfolderId: Long?,
        name: String,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): Long
    suspend fun importDocx(
        uri: Uri,
        folderId: Long,
        subfolderId: Long?,
        name: String
    ): Long
    suspend fun splitPdfToImages(documentNoteId: Long): List<Long>
    suspend fun renameDocumentNote(id: Long, newName: String)
    suspend fun deleteDocumentNote(id: Long)
    suspend fun deleteDocumentNotes(ids: List<Long>)
    suspend fun restoreDocumentNote(id: Long)
    suspend fun purgeDocumentNotePermanently(id: Long)
    suspend fun moveDocumentNote(id: Long, targetFolderId: Long, targetSubfolderId: Long?)
    suspend fun moveDocumentNotes(ids: List<Long>, targetFolderId: Long, targetSubfolderId: Long?)
    suspend fun updateDocumentTagColor(id: Long, colorHex: String?)
    suspend fun updateDocumentDeadline(id: Long, deadlineMs: Long?)
    fun getTrashedDocumentNotes(): Flow<List<DocumentNote>>
    suspend fun refresh()
}

class SqliteDocumentRepository(
    private val context: Context,
    private val dbHelper: FotaraDbHelper,
    private val photoRepository: PhotoRepository,
    private val photoStorageManager: PhotoStorageManager,
    private val ocrEngine: OcrEngine,
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : DocumentRepository {

    private val scope = coroutineScope
    private val documentNotesFlow = MutableStateFlow<List<DocumentNote>>(emptyList())
    private val documentPagesFlow = MutableStateFlow<Map<Long, List<DocumentPage>>>(emptyMap())
    private val trashedDocumentNotesFlow = MutableStateFlow<List<DocumentNote>>(emptyList())

    init {
        scope.launch {
            refreshSync()
        }
    }

    private fun refreshSync() {
        try {
            val db = dbHelper.getSafeReadableDatabase()
            val notes = mutableListOf<DocumentNote>()
            val trashedNotes = mutableListOf<DocumentNote>()

            val cursor = db.rawQuery(
                """
                SELECT id, folder_id, subfolder_id, name, doc_type, origin_file_uri,
                       extracted_text, created_at, added_at, tag_color, linked_deadline,
                       is_trashed, deleted_at, scheduled_at, alert_type
                FROM document_notes
                ORDER BY added_at DESC
                """.trimIndent(),
                null
            )

            cursor.use { c ->
                while (c.moveToNext()) {
                    val note = DocumentNote(
                        id = c.getLong(0),
                        folderId = c.getLong(1),
                        subfolderId = if (c.isNull(2)) null else c.getLong(2),
                        name = c.getString(3),
                        docType = try { DocumentType.valueOf(c.getString(4)) } catch (_: Exception) { DocumentType.PDF },
                        originFileUri = c.getString(5),
                        extractedText = if (c.isNull(6)) null else c.getString(6),
                        createdAt = c.getLong(7),
                        addedAt = c.getLong(8),
                        tagColor = if (c.isNull(9)) null else c.getString(9),
                        linkedDeadline = if (c.isNull(10)) null else c.getLong(10),
                        isTrashed = c.getInt(11) == 1,
                        deletedAt = if (c.isNull(12)) null else c.getLong(12),
                        scheduledAt = if (c.columnCount > 13 && !c.isNull(13)) c.getLong(13) else null,
                        alertType = if (c.columnCount > 14 && !c.isNull(14)) c.getString(14) else null
                    )
                    if (note.isTrashed) {
                        trashedNotes.add(note)
                    } else {
                        notes.add(note)
                    }
                }
            }

            // Load pages map
            val pagesMap = mutableMapOf<Long, MutableList<DocumentPage>>()
            val pagesCursor = db.rawQuery(
                """
                SELECT id, document_note_id, page_index, image_uri, ocr_text
                FROM document_pages
                ORDER BY page_index ASC
                """.trimIndent(),
                null
            )
            pagesCursor.use { pc ->
                while (pc.moveToNext()) {
                    val page = DocumentPage(
                        id = pc.getLong(0),
                        documentNoteId = pc.getLong(1),
                        pageIndex = pc.getInt(2),
                        imageUri = pc.getString(3),
                        ocrText = if (pc.isNull(4)) null else pc.getString(4)
                    )
                    pagesMap.getOrPut(page.documentNoteId) { mutableListOf() }.add(page)
                }
            }

            // Populate pageCount in notes
            val notesWithCounts = notes.map { n ->
                n.copy(pageCount = pagesMap[n.id]?.size ?: if (n.docType == DocumentType.DOCX) 1 else 0)
            }
            val trashedWithCounts = trashedNotes.map { n ->
                n.copy(pageCount = pagesMap[n.id]?.size ?: if (n.docType == DocumentType.DOCX) 1 else 0)
            }

            documentNotesFlow.value = notesWithCounts
            documentPagesFlow.value = pagesMap
            trashedDocumentNotesFlow.value = trashedWithCounts
        } catch (e: Exception) {
            android.util.Log.e("SqliteDocRepo", "Error refreshing document notes: ${e.message}")
        }
    }

    override fun getDocumentNotesByFolder(folderId: Long, subfolderId: Long?): Flow<List<DocumentNote>> =
        documentNotesFlow.map { list ->
            list.filter { it.folderId == folderId && (subfolderId == null || it.subfolderId == subfolderId) }
        }

    override fun getDocumentPages(documentNoteId: Long): Flow<List<DocumentPage>> =
        documentPagesFlow.map { it[documentNoteId] ?: emptyList() }

    override suspend fun getDocumentNoteById(id: Long): DocumentNote? = withContext(Dispatchers.IO) {
        documentNotesFlow.value.firstOrNull { it.id == id }
            ?: trashedDocumentNotesFlow.value.firstOrNull { it.id == id }
    }

    override suspend fun importPdf(
        uri: Uri,
        folderId: Long,
        subfolderId: Long?,
        name: String,
        onProgress: ((current: Int, total: Int) -> Unit)?
    ): Long = withContext(Dispatchers.IO) {
        val targetFile = photoStorageManager.copyUriToDocuments(uri, name, "pdf")
        val now = System.currentTimeMillis()
        val db = dbHelper.getSafeWritableDatabase()

        val values = ContentValues().apply {
            put("folder_id", folderId)
            put("subfolder_id", subfolderId)
            put("name", name.trim())
            put("doc_type", DocumentType.PDF.name)
            put("origin_file_uri", targetFile.absolutePath)
            put("created_at", now)
            put("added_at", now)
            put("is_trashed", 0)
        }
        val docId = db.insert("document_notes", null, values)

        // Native PdfRenderer processing with password & corruption detection
        val pfd = try {
            ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY)
        } catch (e: Exception) {
            targetFile.delete()
            db.delete("document_notes", "id = ?", arrayOf(docId.toString()))
            throw e
        }

        val renderer = try {
            PdfRenderer(pfd)
        } catch (e: SecurityException) {
            pfd.close()
            targetFile.delete()
            db.delete("document_notes", "id = ?", arrayOf(docId.toString()))
            throw PdfPasswordException("Password required to open this PDF document.")
        } catch (e: Exception) {
            pfd.close()
            targetFile.delete()
            db.delete("document_notes", "id = ?", arrayOf(docId.toString()))
            throw PdfCorruptException("Failed to read PDF document: ${e.message}")
        }

        val totalPages = renderer.pageCount
        val pageFiles = mutableListOf<File>()

        try {
            for (i in 0 until totalPages) {
                currentCoroutineContext().ensureActive()

                val page = renderer.openPage(i)
                val renderWidth = (page.width * 2).coerceAtMost(2048)
                val renderHeight = (page.height * 2).coerceAtMost(2048)
                val bitmap = Bitmap.createBitmap(renderWidth, renderHeight, Bitmap.Config.ARGB_8888)

                // CRITICAL DEFECT FIX: Fill destination canvas with opaque white
                bitmap.eraseColor(Color.WHITE)

                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                val pageFile = photoStorageManager.saveDocumentPageBitmap(bitmap, docId, i)
                pageFiles.add(pageFile)
                bitmap.recycle()

                val pageValues = ContentValues().apply {
                    put("document_note_id", docId)
                    put("page_index", i)
                    put("image_uri", pageFile.absolutePath)
                    putNull("ocr_text")
                }
                db.insert("document_pages", null, pageValues)

                // Pipelined: Page 0 viewable immediately
                if (i == 0) {
                    refreshSync()
                }

                onProgress?.invoke(i + 1, totalPages)
            }
        } catch (e: CancellationException) {
            // Cancelled: clean up all partial output immediately
            targetFile.delete()
            pageFiles.forEach { it.delete() }
            db.delete("document_pages", "document_note_id = ?", arrayOf(docId.toString()))
            db.delete("document_notes", "id = ?", arrayOf(docId.toString()))
            refreshSync()
            throw e
        } catch (e: Exception) {
            targetFile.delete()
            pageFiles.forEach { it.delete() }
            db.delete("document_pages", "document_note_id = ?", arrayOf(docId.toString()))
            db.delete("document_notes", "id = ?", arrayOf(docId.toString()))
            refreshSync()
            throw e
        } finally {
            try { renderer.close() } catch (_: Exception) {}
            try { pfd.close() } catch (_: Exception) {}
        }

        refreshSync()

        // Background OCR: runs asynchronously so import completes immediately without blocking first view
        scope.launch(Dispatchers.IO) {
            for ((idx, file) in pageFiles.withIndex()) {
                try {
                    val ocrResult = ocrEngine.extractText(file.absolutePath)
                    if (ocrResult.fullText.isNotBlank()) {
                        val upDb = dbHelper.getSafeWritableDatabase()
                        val cv = ContentValues().apply {
                            put("ocr_text", ocrResult.fullText)
                        }
                        upDb.update("document_pages", cv, "document_note_id = ? AND page_index = ?", arrayOf(docId.toString(), idx.toString()))
                    }
                } catch (e: Exception) {
                    Log.w("SqliteDocRepo", "Background OCR error for page $idx: ${e.message}")
                }
            }
            refreshSync()
        }

        docId
    }

    override suspend fun importDocx(
        uri: Uri,
        folderId: Long,
        subfolderId: Long?,
        name: String
    ): Long = withContext(Dispatchers.IO) {
        val targetFile = photoStorageManager.copyUriToDocuments(uri, name, "docx")
        val now = System.currentTimeMillis()

        // Extract raw XML text from word/document.xml
        val extractedText = try {
            targetFile.inputStream().use { stream -> extractDocxText(stream) }
        } catch (e: Exception) {
            targetFile.delete()
            throw PdfCorruptException("Failed to read Word document: ${e.message}")
        }

        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("folder_id", folderId)
            put("subfolder_id", subfolderId)
            put("name", name.trim())
            put("doc_type", DocumentType.DOCX.name)
            put("origin_file_uri", targetFile.absolutePath)
            put("extracted_text", extractedText.ifBlank { null })
            put("created_at", now)
            put("added_at", now)
            put("is_trashed", 0)
        }
        val docId = db.insert("document_notes", null, values)

        refreshSync()
        docId
    }

    private fun extractDocxText(inputStream: InputStream): String {
        val zip = ZipInputStream(inputStream)
        val sb = StringBuilder()
        var entry = zip.nextEntry
        while (entry != null) {
            if (entry.name == "word/document.xml") {
                val parser = Xml.newPullParser()
                parser.setInput(zip, "UTF-8")
                var eventType = parser.eventType
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG && parser.name == "t") {
                        sb.append(parser.nextText()).append(" ")
                    } else if (eventType == XmlPullParser.START_TAG && parser.name == "p") {
                        sb.append("\n")
                    }
                    eventType = parser.next()
                }
                break
            }
            entry = zip.nextEntry
        }
        return sb.toString().trim()
    }

    override suspend fun splitPdfToImages(documentNoteId: Long): List<Long> = withContext(Dispatchers.IO) {
        val note = getDocumentNoteById(documentNoteId) ?: return@withContext emptyList()
        val pages = documentPagesFlow.value[documentNoteId] ?: emptyList()

        val createdPhotoIds = mutableListOf<Long>()
        val now = System.currentTimeMillis()

        for (page in pages) {
            val photo = Photo(
                fileUri = page.imageUri,
                thumbnailUri = page.imageUri,
                folderId = note.folderId,
                subfolderId = note.subfolderId,
                caption = "${note.name} P${page.pageIndex + 1}",
                ocrText = page.ocrText,
                source = PhotoSource.IMPORT,
                addedAt = now + page.pageIndex,
                tagColor = note.tagColor,
                linkedDeadline = note.linkedDeadline
            )
            val newPhotoId = photoRepository.addPhoto(photo)
            createdPhotoIds.add(newPhotoId)
        }

        // If PDF has >= 5 pages, package into a new Photo Group named after the PDF (page 1 is cover)
        if (createdPhotoIds.size >= 5) {
            photoRepository.createGroup(
                folderId = note.folderId,
                subfolderId = note.subfolderId,
                name = note.name,
                photoIds = createdPhotoIds,
                tagColor = note.tagColor
            )
        }

        // Permanently delete original PDF file
        try {
            val file = File(note.originFileUri)
            if (file.exists()) file.delete()
        } catch (_: Exception) {}

        // Delete document note and pages records
        val db = dbHelper.getSafeWritableDatabase()
        db.delete("document_pages", "document_note_id = ?", arrayOf(documentNoteId.toString()))
        db.delete("document_notes", "id = ?", arrayOf(documentNoteId.toString()))

        refreshSync()
        createdPhotoIds
    }

    override suspend fun renameDocumentNote(id: Long, newName: String) = withContext(Dispatchers.IO) {
        val note = getDocumentNoteById(id) ?: return@withContext
        val cleanName = newName.trim()
        val newPath = photoStorageManager.renameDocumentFile(note.originFileUri, cleanName)

        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("name", cleanName)
            put("origin_file_uri", newPath)
        }
        db.update("document_notes", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun deleteDocumentNote(id: Long) {
        deleteDocumentNotes(listOf(id))
    }

    override suspend fun deleteDocumentNotes(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val db = dbHelper.getSafeWritableDatabase()
        val now = System.currentTimeMillis()
        val values = ContentValues().apply {
            put("is_trashed", 1)
            put("deleted_at", now)
        }
        val placeholders = ids.joinToString(",") { "?" }
        db.update("document_notes", values, "id IN ($placeholders)", ids.map { it.toString() }.toTypedArray())
        refreshSync()
    }

    override suspend fun restoreDocumentNote(id: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("is_trashed", 0)
            putNull("deleted_at")
        }
        db.update("document_notes", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun purgeDocumentNotePermanently(id: Long) = withContext(Dispatchers.IO) {
        val note = getDocumentNoteById(id) ?: return@withContext
        val pages = documentPagesFlow.value[id] ?: emptyList()

        try {
            File(note.originFileUri).delete()
            pages.forEach { File(it.imageUri).delete() }
        } catch (_: Exception) {}

        val db = dbHelper.getSafeWritableDatabase()
        db.delete("document_pages", "document_note_id = ?", arrayOf(id.toString()))
        db.delete("document_notes", "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun moveDocumentNote(id: Long, targetFolderId: Long, targetSubfolderId: Long?) {
        moveDocumentNotes(listOf(id), targetFolderId, targetSubfolderId)
    }

    override suspend fun moveDocumentNotes(ids: List<Long>, targetFolderId: Long, targetSubfolderId: Long?) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("folder_id", targetFolderId)
            if (targetSubfolderId != null) {
                put("subfolder_id", targetSubfolderId)
            } else {
                putNull("subfolder_id")
            }
        }
        val placeholders = ids.joinToString(",") { "?" }
        db.update("document_notes", values, "id IN ($placeholders)", ids.map { it.toString() }.toTypedArray())
        refreshSync()
    }

    override suspend fun updateDocumentTagColor(id: Long, colorHex: String?) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            if (colorHex != null) put("tag_color", colorHex) else putNull("tag_color")
        }
        db.update("document_notes", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun updateDocumentDeadline(id: Long, deadlineMs: Long?) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            if (deadlineMs != null) put("linked_deadline", deadlineMs) else putNull("linked_deadline")
        }
        db.update("document_notes", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override fun getTrashedDocumentNotes(): Flow<List<DocumentNote>> = trashedDocumentNotesFlow.asStateFlow()

    override suspend fun refresh() {
        withContext(Dispatchers.IO) {
            refreshSync()
        }
    }
}
