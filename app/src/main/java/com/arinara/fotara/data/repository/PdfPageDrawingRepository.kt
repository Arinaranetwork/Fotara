// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.repository

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.arinara.fotara.canvas.persistence.PhotoDrawingCodec
import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.data.model.PdfPageDrawing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

interface PdfPageDrawingRepository {
    fun observeDocumentDrawings(documentId: Long): Flow<Map<Int, PdfPageDrawing>>
    fun observeVisibleDrawingPages(documentId: Long): Flow<Set<Int>>
    fun observeDrawing(documentId: Long, pageIndex: Int): Flow<PdfPageDrawing?>
    suspend fun getDrawing(documentId: Long, pageIndex: Int): PdfPageDrawing?
    suspend fun saveDrawing(drawing: PdfPageDrawing)
    suspend fun setVisible(documentId: Long, pageIndex: Int, isVisible: Boolean)
    suspend fun clearDrawing(documentId: Long, pageIndex: Int)
    suspend fun deleteDrawingsForDocument(documentId: Long)
}

class SqlitePdfPageDrawingRepository(
    private val dbHelper: FotaraDbHelper,
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : PdfPageDrawingRepository {

    private val scope = coroutineScope
    private val documentFlows = ConcurrentHashMap<Long, MutableStateFlow<Map<Int, PdfPageDrawing>>>()
    private val writeMutex = Mutex()

    private fun getFlowForDocument(documentId: Long): MutableStateFlow<Map<Int, PdfPageDrawing>> {
        return documentFlows.computeIfAbsent(documentId) {
            val initial = loadDrawingsSync(documentId)
            MutableStateFlow(initial)
        }
    }

    private fun loadDrawingsSync(documentId: Long): Map<Int, PdfPageDrawing> {
        val map = mutableMapOf<Int, PdfPageDrawing>()
        try {
            val db = dbHelper.getSafeReadableDatabase()
            val cursor = db.rawQuery(
                """
                SELECT page_index, data, page_width, page_height, is_visible, updated_at
                FROM pdf_page_drawings
                WHERE document_id = ?
                ORDER BY page_index ASC
                """.trimIndent(),
                arrayOf(documentId.toString())
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    val pageIndex = c.getInt(0)
                    val blob = c.getBlob(1)
                    val pageWidth = c.getFloat(2)
                    val pageHeight = c.getFloat(3)
                    val isVisible = c.getInt(4) == 1
                    val updatedAt = c.getLong(5)
                    val strokes = PhotoDrawingCodec.decode(blob, layerId = "pdf_page_${documentId}_$pageIndex")
                    map[pageIndex] = PdfPageDrawing(
                        documentId = documentId,
                        pageIndex = pageIndex,
                        strokes = strokes,
                        pageWidth = pageWidth,
                        pageHeight = pageHeight,
                        isVisible = isVisible,
                        updatedAt = updatedAt
                    )
                }
            }
        } catch (_: Exception) {}
        return map
    }

    override fun observeDocumentDrawings(documentId: Long): Flow<Map<Int, PdfPageDrawing>> {
        val flow = getFlowForDocument(documentId)
        scope.launch(Dispatchers.IO) {
            flow.value = loadDrawingsSync(documentId)
        }
        return flow.asStateFlow()
    }

    override fun observeVisibleDrawingPages(documentId: Long): Flow<Set<Int>> {
        return observeDocumentDrawings(documentId).map { drawingsMap ->
            drawingsMap.filterValues { it.isVisible && it.hasStrokes }.keys
        }
    }

    override fun observeDrawing(documentId: Long, pageIndex: Int): Flow<PdfPageDrawing?> {
        return observeDocumentDrawings(documentId).map { it[pageIndex] }
    }

    override suspend fun getDrawing(documentId: Long, pageIndex: Int): PdfPageDrawing? = withContext(Dispatchers.IO) {
        getFlowForDocument(documentId).value[pageIndex] ?: loadDrawingsSync(documentId)[pageIndex]
    }

    override suspend fun saveDrawing(drawing: PdfPageDrawing) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = dbHelper.getSafeWritableDatabase()
            val encodedBytes = PhotoDrawingCodec.encode(drawing.strokes)
            val cv = ContentValues().apply {
                put("document_id", drawing.documentId)
                put("page_index", drawing.pageIndex)
                put("data", encodedBytes)
                put("page_width", drawing.pageWidth)
                put("page_height", drawing.pageHeight)
                put("is_visible", if (drawing.isVisible) 1 else 0)
                put("updated_at", drawing.updatedAt)
            }
            db.insertWithOnConflict("pdf_page_drawings", null, cv, SQLiteDatabase.CONFLICT_REPLACE)
            val fresh = loadDrawingsSync(drawing.documentId)
            getFlowForDocument(drawing.documentId).value = fresh
        }
    }

    override suspend fun setVisible(documentId: Long, pageIndex: Int, isVisible: Boolean) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = dbHelper.getSafeWritableDatabase()
            val cv = ContentValues().apply {
                put("is_visible", if (isVisible) 1 else 0)
                put("updated_at", System.currentTimeMillis())
            }
            db.update(
                "pdf_page_drawings",
                cv,
                "document_id = ? AND page_index = ?",
                arrayOf(documentId.toString(), pageIndex.toString())
            )
            val fresh = loadDrawingsSync(documentId)
            getFlowForDocument(documentId).value = fresh
        }
    }

    override suspend fun clearDrawing(documentId: Long, pageIndex: Int) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = dbHelper.getSafeWritableDatabase()
            db.delete(
                "pdf_page_drawings",
                "document_id = ? AND page_index = ?",
                arrayOf(documentId.toString(), pageIndex.toString())
            )
            val fresh = loadDrawingsSync(documentId)
            getFlowForDocument(documentId).value = fresh
        }
    }

    override suspend fun deleteDrawingsForDocument(documentId: Long) {
        withContext(Dispatchers.IO) {
            writeMutex.withLock {
                val db = dbHelper.getSafeWritableDatabase()
                db.delete("pdf_page_drawings", "document_id = ?", arrayOf(documentId.toString()))
                documentFlows[documentId]?.value = emptyMap()
                documentFlows.remove(documentId)
            }
        }
    }
}
