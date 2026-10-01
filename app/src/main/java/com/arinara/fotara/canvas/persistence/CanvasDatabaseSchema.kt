// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.persistence

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.arinara.fotara.data.db.FotaraDbHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Database entities representing Canvas structures in persistent storage.
 */
data class CanvasLayerEntity(
    val id: String,
    val canvasId: Long,
    val name: String,
    val isVisible: Boolean,
    val isLocked: Boolean,
    val opacity: Float,
    val sortOrder: Int
)

data class CanvasElementEntity(
    val id: String,
    val canvasId: Long,
    val layerId: String,
    val elementType: String,
    val boundsLeft: Float,
    val boundsTop: Float,
    val boundsRight: Float,
    val boundsBottom: Float,
    val zIndex: Int,
    val dataChunk: ByteArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CanvasElementEntity
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}

data class CanvasAssetEntity(
    val assetId: String,
    val canvasId: Long,
    val filePath: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val fileSize: Long,
    val createdAt: Long
)

/**
 * Data Access Object for persisting and progressively loading infinite canvas data.
 */
interface CanvasDao {
    suspend fun loadLayers(canvasId: Long): List<CanvasLayerEntity>
    suspend fun loadAllElements(canvasId: Long): List<CanvasElementEntity>
    suspend fun loadElementsChunk(canvasId: Long, offset: Int, limit: Int): List<CanvasElementEntity>
    suspend fun countElements(canvasId: Long): Int
    suspend fun saveDocumentTransaction(
        canvasId: Long,
        layers: List<CanvasLayerEntity>,
        elements: List<CanvasElementEntity>
    )
    suspend fun recordAsset(asset: CanvasAssetEntity)
    suspend fun loadAssets(canvasId: Long): List<CanvasAssetEntity>
    suspend fun purgeCanvasData(canvasId: Long)
}

/**
 * Robust SQLite implementation of CanvasDao leveraging SQLiteOpenHelper.
 */
class SqliteCanvasDao(private val dbHelper: FotaraDbHelper) : CanvasDao {

    override suspend fun loadLayers(canvasId: Long): List<CanvasLayerEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<CanvasLayerEntity>()
        val db = dbHelper.getSafeReadableDatabase()
        db.rawQuery(
            """
            SELECT id, canvas_id, name, is_visible, is_locked, opacity, sort_order
            FROM canvas_layers
            WHERE canvas_id = ?
            ORDER BY sort_order ASC
            """.trimIndent(),
            arrayOf(canvasId.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    CanvasLayerEntity(
                        id = cursor.getString(0),
                        canvasId = cursor.getLong(1),
                        name = cursor.getString(2),
                        isVisible = cursor.getInt(3) == 1,
                        isLocked = cursor.getInt(4) == 1,
                        opacity = cursor.getFloat(5),
                        sortOrder = cursor.getInt(6)
                    )
                )
            }
        }
        list
    }

    override suspend fun loadAllElements(canvasId: Long): List<CanvasElementEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<CanvasElementEntity>()
        val db = dbHelper.getSafeReadableDatabase()
        db.rawQuery(
            """
            SELECT id, canvas_id, layer_id, element_type, bounds_left, bounds_top, bounds_right, bounds_bottom, z_index, data_chunk
            FROM canvas_elements
            WHERE canvas_id = ?
            ORDER BY z_index ASC
            """.trimIndent(),
            arrayOf(canvasId.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursorToElement(cursor))
            }
        }
        list
    }

    override suspend fun loadElementsChunk(
        canvasId: Long,
        offset: Int,
        limit: Int
    ): List<CanvasElementEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<CanvasElementEntity>()
        val db = dbHelper.getSafeReadableDatabase()
        db.rawQuery(
            """
            SELECT id, canvas_id, layer_id, element_type, bounds_left, bounds_top, bounds_right, bounds_bottom, z_index, data_chunk
            FROM canvas_elements
            WHERE canvas_id = ?
            ORDER BY z_index ASC
            LIMIT ? OFFSET ?
            """.trimIndent(),
            arrayOf(canvasId.toString(), limit.toString(), offset.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(cursorToElement(cursor))
            }
        }
        list
    }

    override suspend fun countElements(canvasId: Long): Int = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeReadableDatabase()
        db.rawQuery(
            "SELECT COUNT(*) FROM canvas_elements WHERE canvas_id = ?",
            arrayOf(canvasId.toString())
        ).use { cursor ->
            if (cursor.moveToFirst()) cursor.getInt(0) else 0
        }
    }

    override suspend fun saveDocumentTransaction(
        canvasId: Long,
        layers: List<CanvasLayerEntity>,
        elements: List<CanvasElementEntity>
    ) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        db.beginTransaction()
        try {
            // 1. Sync Layers
            db.delete("canvas_layers", "canvas_id = ?", arrayOf(canvasId.toString()))
            for (layer in layers) {
                val values = ContentValues().apply {
                    put("id", layer.id)
                    put("canvas_id", canvasId)
                    put("name", layer.name)
                    put("is_visible", if (layer.isVisible) 1 else 0)
                    put("is_locked", if (layer.isLocked) 1 else 0)
                    put("opacity", layer.opacity)
                    put("sort_order", layer.sortOrder)
                }
                db.insertWithOnConflict("canvas_layers", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }

            // 2. Sync Elements
            db.delete("canvas_elements", "canvas_id = ?", arrayOf(canvasId.toString()))
            for (element in elements) {
                val values = ContentValues().apply {
                    put("id", element.id)
                    put("canvas_id", canvasId)
                    put("layer_id", element.layerId)
                    put("element_type", element.elementType)
                    put("bounds_left", element.boundsLeft)
                    put("bounds_top", element.boundsTop)
                    put("bounds_right", element.boundsRight)
                    put("bounds_bottom", element.boundsBottom)
                    put("z_index", element.zIndex)
                    put("data_chunk", element.dataChunk)
                }
                db.insertWithOnConflict("canvas_elements", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            }

            // 3. Update canvas_notes updated_at timestamp
            val noteValues = ContentValues().apply {
                put("updated_at", System.currentTimeMillis())
            }
            db.update("canvas_notes", noteValues, "id = ?", arrayOf(canvasId.toString()))

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    override suspend fun recordAsset(asset: CanvasAssetEntity): Unit = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("asset_id", asset.assetId)
            put("canvas_id", asset.canvasId)
            put("file_path", asset.filePath)
            put("mime_type", asset.mimeType)
            put("width", asset.width)
            put("height", asset.height)
            put("file_size", asset.fileSize)
            put("created_at", asset.createdAt)
        }
        db.insertWithOnConflict("canvas_assets", null, values, SQLiteDatabase.CONFLICT_REPLACE)
        Unit
    }

    override suspend fun loadAssets(canvasId: Long): List<CanvasAssetEntity> = withContext(Dispatchers.IO) {
        val list = mutableListOf<CanvasAssetEntity>()
        val db = dbHelper.getSafeReadableDatabase()
        db.rawQuery(
            """
            SELECT asset_id, canvas_id, file_path, mime_type, width, height, file_size, created_at
            FROM canvas_assets
            WHERE canvas_id = ?
            """.trimIndent(),
            arrayOf(canvasId.toString())
        ).use { cursor ->
            while (cursor.moveToNext()) {
                list.add(
                    CanvasAssetEntity(
                        assetId = cursor.getString(0),
                        canvasId = cursor.getLong(1),
                        filePath = cursor.getString(2),
                        mimeType = cursor.getString(3),
                        width = cursor.getInt(4),
                        height = cursor.getInt(5),
                        fileSize = cursor.getLong(6),
                        createdAt = cursor.getLong(7)
                    )
                )
            }
        }
        list
    }

    override suspend fun purgeCanvasData(canvasId: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        db.beginTransaction()
        try {
            db.delete("canvas_elements", "canvas_id = ?", arrayOf(canvasId.toString()))
            db.delete("canvas_layers", "canvas_id = ?", arrayOf(canvasId.toString()))
            db.delete("canvas_assets", "canvas_id = ?", arrayOf(canvasId.toString()))
            db.delete("canvas_notes", "id = ?", arrayOf(canvasId.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
    }

    private fun cursorToElement(cursor: Cursor): CanvasElementEntity {
        return CanvasElementEntity(
            id = cursor.getString(0),
            canvasId = cursor.getLong(1),
            layerId = cursor.getString(2),
            elementType = cursor.getString(3),
            boundsLeft = cursor.getFloat(4),
            boundsTop = cursor.getFloat(5),
            boundsRight = cursor.getFloat(6),
            boundsBottom = cursor.getFloat(7),
            zIndex = cursor.getInt(8),
            dataChunk = cursor.getBlob(9)
        )
    }
}
