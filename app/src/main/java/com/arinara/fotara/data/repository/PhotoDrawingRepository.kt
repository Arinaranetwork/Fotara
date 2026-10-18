// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.repository

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.arinara.fotara.canvas.engine.PhotoDrawingTransform
import com.arinara.fotara.canvas.persistence.PhotoDrawingCodec
import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.data.model.PhotoDrawing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

interface PhotoDrawingRepository {
    fun observeDrawing(photoId: Long): Flow<PhotoDrawing?>
    suspend fun getDrawing(photoId: Long): PhotoDrawing?
    suspend fun saveDrawing(drawing: PhotoDrawing)
    suspend fun setVisible(photoId: Long, isVisible: Boolean)
    suspend fun clearDrawing(photoId: Long)
    suspend fun deleteDrawing(photoId: Long)
    suspend fun transformRotate(photoId: Long, oldW: Int, oldH: Int)
    suspend fun transformCrop(photoId: Long, cropLeft: Float, cropTop: Float, newW: Int, newH: Int)
    suspend fun cleanOrphanDrawings()
}

class SqlitePhotoDrawingRepository(
    private val dbHelper: FotaraDbHelper
) : PhotoDrawingRepository {

    private val writeMutex = Mutex()
    private val updateSignal = MutableSharedFlow<Long>(extraBufferCapacity = 64)

    override fun observeDrawing(photoId: Long): Flow<PhotoDrawing?> = flow {
        emit(getDrawing(photoId))
        updateSignal.collect { changedId ->
            if (changedId == photoId || changedId == -1L) {
                emit(getDrawing(photoId))
            }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getDrawing(photoId: Long): PhotoDrawing? = withContext(Dispatchers.IO) {
        getDrawingInternal(photoId)
    }

    private fun getDrawingInternal(photoId: Long): PhotoDrawing? {
        val db = dbHelper.getSafeReadableDatabase()
        val cursor = db.rawQuery(
            """
            SELECT data, width_px, height_px, is_visible, updated_at
            FROM photo_drawings
            WHERE photo_id = ?
            """.trimIndent(),
            arrayOf(photoId.toString())
        )

        return cursor.use { c ->
            if (c.moveToFirst()) {
                val data = c.getBlob(0)
                val widthPx = c.getInt(1)
                val heightPx = c.getInt(2)
                val isVisible = c.getInt(3) == 1
                val updatedAt = c.getLong(4)
                val strokes = PhotoDrawingCodec.decode(data)
                PhotoDrawing(
                    photoId = photoId,
                    strokes = strokes,
                    widthPx = widthPx,
                    heightPx = heightPx,
                    isVisible = isVisible,
                    updatedAt = updatedAt
                )
            } else {
                null
            }
        }
    }

    override suspend fun saveDrawing(drawing: PhotoDrawing) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            saveDrawingInternal(drawing)
        }
        updateSignal.emit(drawing.photoId)
    }

    private fun saveDrawingInternal(drawing: PhotoDrawing) {
        val db = dbHelper.getSafeWritableDatabase()
        val dataBlob = PhotoDrawingCodec.encode(drawing.strokes)
        val values = ContentValues().apply {
            put("photo_id", drawing.photoId)
            put("data", dataBlob)
            put("width_px", drawing.widthPx)
            put("height_px", drawing.heightPx)
            put("is_visible", if (drawing.isVisible) 1 else 0)
            put("updated_at", drawing.updatedAt)
        }
        db.insertWithOnConflict("photo_drawings", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    override suspend fun setVisible(photoId: Long, isVisible: Boolean) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = dbHelper.getSafeWritableDatabase()
            val values = ContentValues().apply {
                put("is_visible", if (isVisible) 1 else 0)
                put("updated_at", System.currentTimeMillis())
            }
            db.update("photo_drawings", values, "photo_id = ?", arrayOf(photoId.toString()))
        }
        updateSignal.emit(photoId)
    }

    override suspend fun clearDrawing(photoId: Long) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = dbHelper.getSafeWritableDatabase()
            db.delete("photo_drawings", "photo_id = ?", arrayOf(photoId.toString()))
        }
        updateSignal.emit(photoId)
    }

    override suspend fun deleteDrawing(photoId: Long) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = dbHelper.getSafeWritableDatabase()
            db.delete("photo_drawings", "photo_id = ?", arrayOf(photoId.toString()))
        }
        updateSignal.emit(photoId)
    }

    override suspend fun transformRotate(photoId: Long, oldW: Int, oldH: Int) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val existing = getDrawingInternal(photoId) ?: return@withLock
            val transformed = PhotoDrawingTransform.rotate90Clockwise(existing, oldW, oldH)
            saveDrawingInternal(transformed)
        }
        updateSignal.emit(photoId)
    }

    override suspend fun transformCrop(
        photoId: Long,
        cropLeft: Float,
        cropTop: Float,
        newW: Int,
        newH: Int
    ) = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val existing = getDrawingInternal(photoId) ?: return@withLock
            val transformed = PhotoDrawingTransform.crop(existing, cropLeft, cropTop, newW, newH)
            saveDrawingInternal(transformed)
        }
        updateSignal.emit(photoId)
    }

    override suspend fun cleanOrphanDrawings() = withContext(Dispatchers.IO) {
        writeMutex.withLock {
            val db = dbHelper.getSafeWritableDatabase()
            db.execSQL("DELETE FROM photo_drawings WHERE photo_id NOT IN (SELECT id FROM photos)")
        }
        updateSignal.emit(-1L)
    }
}
