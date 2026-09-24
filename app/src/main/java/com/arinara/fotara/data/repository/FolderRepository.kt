// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.repository

import android.content.ContentValues
import android.database.Cursor
import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Subfolder
import com.arinara.fotara.data.model.TagColor
import com.arinara.fotara.data.storage.PhotoStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class FolderBulkDeleteResult(
    val folderCount: Int,
    val photoCount: Int,
    val totalSizeBytes: Long
)

data class SubfolderDeleteResult(
    val subfolderCount: Int,
    val photoCount: Int,
    val totalSizeBytes: Long
)

interface FolderRepository {
    fun getFolders(): Flow<List<Folder>>
    fun getFolderById(id: Long): Flow<Folder?>
    suspend fun createFolder(name: String, colorLabel: String = TagColor.SKY.hex, isPinned: Boolean = false): Long
    suspend fun updateFolder(folder: Folder)
    suspend fun renameFolder(id: Long, newName: String)
    suspend fun updateFolderColor(id: Long, colorHex: String)
    suspend fun togglePin(id: Long)
    suspend fun deleteFolder(id: Long)
    suspend fun getBulkDeleteStats(folderIds: List<Long>): FolderBulkDeleteResult
    suspend fun deleteFolders(folderIds: List<Long>): FolderBulkDeleteResult
    fun getSubfolders(folderId: Long): Flow<List<Subfolder>>
    suspend fun createSubfolder(folderId: Long, name: String, colorLabel: String? = null): Long
    suspend fun renameSubfolder(id: Long, newName: String)
    suspend fun getSubfolderDeleteStats(subfolderIds: List<Long>): SubfolderDeleteResult
    suspend fun deleteSubfolder(id: Long)
    suspend fun deleteSubfolders(subfolderIds: List<Long>): SubfolderDeleteResult
    fun getTrashedFolders(): Flow<List<Folder>>
    suspend fun restoreFolder(id: Long)
    suspend fun purgeFolderPermanently(id: Long)
    suspend fun isFolderTrashed(id: Long): Boolean
    suspend fun lockFolder(id: Long, pin: String)
    suspend fun unlockFolder(id: Long)
    suspend fun updateFolderPin(id: Long, newPin: String)
    suspend fun refresh()
}

class SqliteFolderRepository(
    private val dbHelper: FotaraDbHelper,
    private val photoStorageManager: PhotoStorageManager? = null,
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : FolderRepository {

    private val scope = coroutineScope
    private val foldersFlow = MutableStateFlow<List<Folder>>(emptyList())
    private val subfoldersFlow = MutableStateFlow<Map<Long, List<Subfolder>>>(emptyMap())
    private val trashedFoldersFlow = MutableStateFlow<List<Folder>>(emptyList())

    init {
        scope.launch {
            refreshSync()
        }
    }

    private fun refreshSync() {
        try {
            val db = dbHelper.getSafeReadableDatabase()
            val folderList = mutableListOf<Folder>()

            val cursor = db.rawQuery(
                """
                SELECT f.id, f.name, f.color_label, f.is_pinned, f.created_at,
                       COUNT(p.id) as photo_count,
                       COALESCE(SUM(p.file_size_bytes), 0) as total_size,
                       f.is_locked, f.lock_pin
                FROM folders f
                LEFT JOIN photos p ON f.id = p.folder_id AND p.is_trashed = 0
                WHERE f.is_trashed = 0
                GROUP BY f.id
                ORDER BY f.is_pinned DESC, f.created_at DESC
                """.trimIndent(),
                null
            )

            cursor.use { c ->
                while (c.moveToNext()) {
                    folderList.add(
                        Folder(
                            id = c.getLong(0),
                            name = c.getString(1),
                            colorLabel = c.getString(2),
                            isPinned = c.getInt(3) == 1,
                            createdAt = c.getLong(4),
                            photoCount = c.getInt(5),
                            totalSizeBytes = c.getLong(6),
                            isLocked = c.getInt(7) == 1,
                            lockPin = if (c.isNull(8)) null else c.getString(8)
                        )
                    )
                }
            }
            foldersFlow.value = folderList

            // Load subfolders
            val subCursor = db.rawQuery(
                "SELECT id, folder_id, name, color_label, created_at FROM subfolders WHERE is_trashed = 0 ORDER BY created_at ASC",
                null
            )
            val subMap = mutableMapOf<Long, MutableList<Subfolder>>()
            subCursor.use { sc ->
                while (sc.moveToNext()) {
                    val fId = sc.getLong(1)
                    val sub = Subfolder(
                        id = sc.getLong(0),
                        folderId = fId,
                        name = sc.getString(2),
                        colorLabel = if (sc.isNull(3)) null else sc.getString(3)
                    )
                    subMap.getOrPut(fId) { mutableListOf() }.add(sub)
                }
            }
            subfoldersFlow.value = subMap

            // Load trashed folders
            val trashedList = mutableListOf<Folder>()
            val trashedCursor = db.rawQuery(
                """
                SELECT f.id, f.name, f.color_label, f.is_pinned, f.created_at,
                       COUNT(p.id) as photo_count,
                       COALESCE(SUM(p.file_size_bytes), 0) as total_size,
                       f.deleted_at, f.is_locked, f.lock_pin
                FROM folders f
                LEFT JOIN photos p ON f.id = p.folder_id
                WHERE f.is_trashed = 1
                GROUP BY f.id
                ORDER BY f.deleted_at DESC
                """.trimIndent(),
                null
            )
            trashedCursor.use { c ->
                while (c.moveToNext()) {
                    trashedList.add(
                        Folder(
                            id = c.getLong(0),
                            name = c.getString(1),
                            colorLabel = c.getString(2),
                            isPinned = c.getInt(3) == 1,
                            createdAt = c.getLong(4),
                            photoCount = c.getInt(5),
                            totalSizeBytes = c.getLong(6),
                            isTrashed = true,
                            deletedAt = if (c.isNull(7)) null else c.getLong(7),
                            isLocked = c.getInt(8) == 1,
                            lockPin = if (c.isNull(9)) null else c.getString(9)
                        )
                    )
                }
            }
            trashedFoldersFlow.value = trashedList
        } catch (e: Exception) {
            android.util.Log.e("SqliteFolderRepo", "Failed to refresh folders cleanly: ${e.message}", e)
        }
    }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        refreshSync()
    }

    override fun getFolders(): Flow<List<Folder>> = foldersFlow.asStateFlow()

    override fun getFolderById(id: Long): Flow<Folder?> =
        foldersFlow.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun createFolder(name: String, colorLabel: String, isPinned: Boolean): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("name", name.trim())
            put("color_label", colorLabel)
            put("is_pinned", if (isPinned) 1 else 0)
            put("created_at", System.currentTimeMillis())
        }
        val insertedId = db.insert("folders", null, values)
        refreshSync()
        insertedId
    }

    override suspend fun updateFolder(folder: Folder) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("name", folder.name.trim())
            put("color_label", folder.colorLabel)
            put("is_pinned", if (folder.isPinned) 1 else 0)
        }
        db.update("folders", values, "id = ?", arrayOf(folder.id.toString()))
        refreshSync()
    }

    override suspend fun renameFolder(id: Long, newName: String) = withContext(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return@withContext
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply { put("name", trimmed) }
        db.update("folders", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun updateFolderColor(id: Long, colorHex: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply { put("color_label", colorHex) }
        db.update("folders", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun togglePin(id: Long) = withContext(Dispatchers.IO) {
        val current = foldersFlow.value.firstOrNull { it.id == id } ?: return@withContext
        val newPinned = if (current.isPinned) 0 else 1
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply { put("is_pinned", newPinned) }
        db.update("folders", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun deleteFolder(id: Long) = withContext(Dispatchers.IO) {
        deleteFolders(listOf(id))
        Unit
    }

    override suspend fun getBulkDeleteStats(folderIds: List<Long>): FolderBulkDeleteResult = withContext(Dispatchers.IO) {
        if (folderIds.isEmpty()) return@withContext FolderBulkDeleteResult(0, 0, 0L)
        val db = dbHelper.getSafeReadableDatabase()
        val inClause = folderIds.joinToString(",") { it.toString() }

        var photoCount = 0
        var totalBytes = 0L

        val cursor = db.rawQuery(
            "SELECT COUNT(id), COALESCE(SUM(file_size_bytes), 0) FROM photos WHERE folder_id IN ($inClause) AND is_trashed = 0",
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                photoCount = it.getInt(0)
                totalBytes = it.getLong(1)
            }
        }

        FolderBulkDeleteResult(
            folderCount = folderIds.size,
            photoCount = photoCount,
            totalSizeBytes = totalBytes
        )
    }

    override suspend fun deleteFolders(folderIds: List<Long>): FolderBulkDeleteResult = withContext(Dispatchers.IO) {
        if (folderIds.isEmpty()) return@withContext FolderBulkDeleteResult(0, 0, 0L)
        val stats = getBulkDeleteStats(folderIds)
        val db = dbHelper.getSafeWritableDatabase()
        val inClause = folderIds.joinToString(",") { it.toString() }
        val now = System.currentTimeMillis()

        db.beginTransaction()
        try {
            // Soft delete: move photos, subfolders, and folders to trash
            db.execSQL("UPDATE photos SET is_trashed = 1, deleted_at = $now WHERE folder_id IN ($inClause)")
            db.execSQL("UPDATE subfolders SET is_trashed = 1, deleted_at = $now WHERE folder_id IN ($inClause)")
            db.execSQL("UPDATE folders SET is_trashed = 1, deleted_at = $now WHERE id IN ($inClause)")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        refreshSync()
        stats
    }

    override fun getSubfolders(folderId: Long): Flow<List<Subfolder>> =
        subfoldersFlow.map { map -> map[folderId] ?: emptyList() }

    override suspend fun createSubfolder(folderId: Long, name: String, colorLabel: String?): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("folder_id", folderId)
            put("name", name.trim())
            put("color_label", colorLabel)
            put("created_at", System.currentTimeMillis())
        }
        val insertedId = db.insert("subfolders", null, values)
        refreshSync()
        insertedId
    }

    override suspend fun renameSubfolder(id: Long, newName: String) = withContext(Dispatchers.IO) {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) return@withContext
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply { put("name", trimmed) }
        db.update("subfolders", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun getSubfolderDeleteStats(subfolderIds: List<Long>): SubfolderDeleteResult = withContext(Dispatchers.IO) {
        if (subfolderIds.isEmpty()) return@withContext SubfolderDeleteResult(0, 0, 0L)
        val db = dbHelper.getSafeReadableDatabase()
        val inClause = subfolderIds.joinToString(",") { it.toString() }

        var photoCount = 0
        var totalBytes = 0L

        val cursor = db.rawQuery(
            "SELECT COUNT(id), COALESCE(SUM(file_size_bytes), 0) FROM photos WHERE subfolder_id IN ($inClause) AND is_trashed = 0",
            null
        )
        cursor.use {
            if (it.moveToFirst()) {
                photoCount = it.getInt(0)
                totalBytes = it.getLong(1)
            }
        }

        SubfolderDeleteResult(
            subfolderCount = subfolderIds.size,
            photoCount = photoCount,
            totalSizeBytes = totalBytes
        )
    }

    override suspend fun deleteSubfolders(subfolderIds: List<Long>): SubfolderDeleteResult = withContext(Dispatchers.IO) {
        if (subfolderIds.isEmpty()) return@withContext SubfolderDeleteResult(0, 0, 0L)
        val stats = getSubfolderDeleteStats(subfolderIds)
        val db = dbHelper.getSafeWritableDatabase()
        val inClause = subfolderIds.joinToString(",") { it.toString() }
        val now = System.currentTimeMillis()

        db.beginTransaction()
        try {
            // Soft delete: move photos and subfolders to trash
            db.execSQL("UPDATE photos SET is_trashed = 1, deleted_at = $now WHERE subfolder_id IN ($inClause)")
            db.execSQL("UPDATE subfolders SET is_trashed = 1, deleted_at = $now WHERE id IN ($inClause)")
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        refreshSync()
        stats
    }

    override suspend fun deleteSubfolder(id: Long) = withContext(Dispatchers.IO) {
        deleteSubfolders(listOf(id))
        Unit
    }

    override fun getTrashedFolders(): Flow<List<Folder>> = trashedFoldersFlow.asStateFlow()

    override suspend fun restoreFolder(id: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        db.beginTransaction()
        try {
            db.execSQL("UPDATE folders SET is_trashed = 0, deleted_at = NULL WHERE id = ?", arrayOf(id.toString()))
            db.execSQL("UPDATE subfolders SET is_trashed = 0, deleted_at = NULL WHERE folder_id = ?", arrayOf(id.toString()))
            db.execSQL("UPDATE photos SET is_trashed = 0, deleted_at = NULL WHERE folder_id = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshSync()
    }

    override suspend fun purgeFolderPermanently(id: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val photoCursor = db.rawQuery("SELECT file_path, thumbnail_path, id FROM photos WHERE folder_id = ?", arrayOf(id.toString()))
        photoCursor.use { c ->
            while (c.moveToNext()) {
                val filePath = c.getString(0)
                val thumbPath = if (c.isNull(1)) null else c.getString(1)
                val photoId = c.getLong(2)
                photoStorageManager?.deletePhotoFiles(filePath, thumbPath)
                try {
                    db.execSQL("DELETE FROM photos_fts WHERE photo_id = ?", arrayOf(photoId.toString()))
                } catch (_: Exception) {}
            }
        }
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM photos WHERE folder_id = ?", arrayOf(id.toString()))
            db.execSQL("DELETE FROM subfolders WHERE folder_id = ?", arrayOf(id.toString()))
            db.execSQL("DELETE FROM folders WHERE id = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshSync()
    }

    override suspend fun lockFolder(id: Long, pin: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("is_locked", 1)
            put("lock_pin", pin)
        }
        db.update("folders", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun unlockFolder(id: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("is_locked", 0)
            putNull("lock_pin")
        }
        db.update("folders", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun updateFolderPin(id: Long, newPin: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("lock_pin", newPin)
        }
        db.update("folders", values, "id = ?", arrayOf(id.toString()))
        refreshSync()
    }

    override suspend fun isFolderTrashed(id: Long): Boolean = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeReadableDatabase()
        val cursor = db.rawQuery("SELECT is_trashed FROM folders WHERE id = ?", arrayOf(id.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                it.getInt(0) == 1
            } else {
                true // If folder doesn't exist, treat as orphan/trashed
            }
        }
    }
}
