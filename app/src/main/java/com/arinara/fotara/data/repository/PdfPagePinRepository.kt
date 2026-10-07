// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.repository

import android.content.ContentValues
import com.arinara.fotara.data.db.FotaraDbHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

interface PdfPagePinRepository {
    companion object {
        const val MAX_PINS_PER_DOCUMENT = 3
    }
    fun observePinnedPages(documentId: Long): Flow<List<Int>>
    suspend fun getPinnedPagesSync(documentId: Long): List<Int>
    suspend fun pinPage(documentId: Long, pageIndex: Int): Boolean
    suspend fun unpinPage(documentId: Long, pageIndex: Int)
    suspend fun deletePinsForDocument(documentId: Long)
}

class SqlitePdfPagePinRepository(
    private val dbHelper: FotaraDbHelper,
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : PdfPagePinRepository {

    private val scope = coroutineScope
    private val documentFlows = ConcurrentHashMap<Long, MutableStateFlow<List<Int>>>()
    private val mutex = Mutex()

    private fun getFlowForDocument(documentId: Long): MutableStateFlow<List<Int>> {
        return documentFlows.computeIfAbsent(documentId) {
            val initial = loadPinsSync(documentId)
            MutableStateFlow(initial)
        }
    }

    private fun loadPinsSync(documentId: Long): List<Int> {
        val list = mutableListOf<Int>()
        try {
            val db = dbHelper.getSafeReadableDatabase()
            val cursor = db.rawQuery(
                "SELECT page_index FROM pdf_page_pins WHERE document_id = ? ORDER BY page_index ASC",
                arrayOf(documentId.toString())
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    list.add(c.getInt(0))
                }
            }
        } catch (_: Exception) {}
        return list
    }

    override fun observePinnedPages(documentId: Long): Flow<List<Int>> {
        val flow = getFlowForDocument(documentId)
        scope.launch(Dispatchers.IO) {
            val fresh = loadPinsSync(documentId)
            flow.value = fresh
        }
        return flow.asStateFlow()
    }

    override suspend fun getPinnedPagesSync(documentId: Long): List<Int> = withContext(Dispatchers.IO) {
        loadPinsSync(documentId)
    }

    override suspend fun pinPage(documentId: Long, pageIndex: Int): Boolean = withContext(Dispatchers.IO) {
        mutex.withLock {
            val currentPins = loadPinsSync(documentId)
            if (currentPins.contains(pageIndex)) {
                return@withContext true
            }
            if (currentPins.size >= PdfPagePinRepository.MAX_PINS_PER_DOCUMENT) {
                return@withContext false
            }

            val db = dbHelper.getSafeWritableDatabase()
            val cv = ContentValues().apply {
                put("document_id", documentId)
                put("page_index", pageIndex)
                put("pinned_at", System.currentTimeMillis())
            }
            db.insertWithOnConflict("pdf_page_pins", null, cv, android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE)
            val updated = loadPinsSync(documentId)
            getFlowForDocument(documentId).value = updated
            true
        }
    }

    override suspend fun unpinPage(documentId: Long, pageIndex: Int) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val db = dbHelper.getSafeWritableDatabase()
            db.delete(
                "pdf_page_pins",
                "document_id = ? AND page_index = ?",
                arrayOf(documentId.toString(), pageIndex.toString())
            )
            val updated = loadPinsSync(documentId)
            getFlowForDocument(documentId).value = updated
        }
    }

    override suspend fun deletePinsForDocument(documentId: Long) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val db = dbHelper.getSafeWritableDatabase()
            db.delete("pdf_page_pins", "document_id = ?", arrayOf(documentId.toString()))
            getFlowForDocument(documentId).value = emptyList()
        }
    }
}
