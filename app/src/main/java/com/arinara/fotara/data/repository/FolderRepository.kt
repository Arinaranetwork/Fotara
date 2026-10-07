// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.repository

import android.content.ContentValues
import android.database.Cursor
import com.arinara.fotara.data.db.FotaraDbHelper
import android.database.sqlite.SQLiteDatabase
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.LinkGroup
import com.arinara.fotara.data.model.LinkItemType
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
import java.io.File

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
    fun getFoldersByWorkspace(workspaceId: Long): Flow<List<Folder>>
    fun getFolderById(id: Long): Flow<Folder?>
    suspend fun createFolder(name: String, colorLabel: String = TagColor.SKY.hex, isPinned: Boolean = false, workspaceId: Long = 1L): Long
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
    suspend fun restoreFolder(id: Long, targetWorkspaceId: Long? = null)
    suspend fun purgeFolderPermanently(id: Long)
    suspend fun isFolderTrashed(id: Long): Boolean
    suspend fun lockFolder(id: Long, pin: String)
    suspend fun unlockFolder(id: Long)
    suspend fun updateFolderPin(id: Long, newPin: String)
    fun getFolderLinkGroups(): Flow<List<LinkGroup>>
    suspend fun createFolderLinkGroup(folderIds: List<Long>): Long
    suspend fun unlinkFolder(folderId: Long)
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
    private val folderLinkGroupsFlow = MutableStateFlow<List<LinkGroup>>(emptyList())

    init {
        scope.launch {
            refreshSync()
        }
    }

    private fun refreshSync() {
        try {
            val db = dbHelper.getSafeReadableDatabase()

            // Load folder link groups
            val linkList = mutableListOf<LinkGroup>()
            val folderToLinkGroupMap = mutableMapOf<Long, Long>()
            try {
                val linkCursor = db.rawQuery(
                    "SELECT id, item_type, member_ids, created_at FROM link_groups WHERE item_type = 'FOLDER' ORDER BY created_at ASC",
                    null
                )
                linkCursor.use { lc ->
                    while (lc.moveToNext()) {
                        val lgId = lc.getLong(0)
                        val type = LinkItemType.valueOf(lc.getString(1))
                        val rawIds = lc.getString(2)
                        val memberIds = rawIds.split(",").mapNotNull { it.trim().toLongOrNull() }
                        linkList.add(LinkGroup(id = lgId, itemType = type, memberIds = memberIds, createdAt = lc.getLong(3)))
                        memberIds.forEach { fId -> folderToLinkGroupMap[fId] = lgId }
                    }
                }
            } catch (_: Exception) {}
            folderLinkGroupsFlow.value = linkList

            val folderList = mutableListOf<Folder>()

            val cursor = db.rawQuery(
                """
                SELECT f.id, f.name, f.color_label, f.is_pinned, f.created_at,
                       (
                           (SELECT COUNT(*) FROM photos p WHERE p.folder_id = f.id AND p.is_trashed = 0) +
                           (SELECT COUNT(*) FROM text_notes tn WHERE tn.folder_id = f.id AND tn.is_trashed = 0) +
                           (SELECT COUNT(*) FROM canvas_notes cn WHERE cn.folder_id = f.id AND cn.is_trashed = 0) +
                           (SELECT COUNT(*) FROM document_notes dn WHERE dn.folder_id = f.id AND dn.is_trashed = 0)
                       ) as item_count,
                       COALESCE((SELECT SUM(p.file_size_bytes) FROM photos p WHERE p.folder_id = f.id AND p.is_trashed = 0), 0) as total_size,
                       f.is_locked, f.lock_pin, f.workspace_id
                FROM folders f
                WHERE f.is_trashed = 0
                ORDER BY f.is_pinned DESC, f.created_at DESC
                """.trimIndent(),
                null
            )

            cursor.use { c ->
                while (c.moveToNext()) {
                    val fId = c.getLong(0)
                    folderList.add(
                        Folder(
                            id = fId,
                            name = c.getString(1),
                            colorLabel = c.getString(2),
                            isPinned = c.getInt(3) == 1,
                            createdAt = c.getLong(4),
                            photoCount = c.getInt(5),
                            totalSizeBytes = c.getLong(6),
                            isLocked = c.getInt(7) == 1,
                            lockPin = if (c.isNull(8)) null else c.getString(8),
                            linkGroupId = folderToLinkGroupMap[fId],
                            workspaceId = if (c.columnCount > 9 && !c.isNull(9)) c.getLong(9) else 1L
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
                       (
                           (SELECT COUNT(*) FROM photos p WHERE p.folder_id = f.id) +
                           (SELECT COUNT(*) FROM text_notes tn WHERE tn.folder_id = f.id) +
                           (SELECT COUNT(*) FROM canvas_notes cn WHERE cn.folder_id = f.id) +
                           (SELECT COUNT(*) FROM document_notes dn WHERE dn.folder_id = f.id)
                       ) as item_count,
                       COALESCE((SELECT SUM(p.file_size_bytes) FROM photos p WHERE p.folder_id = f.id), 0) as total_size,
                       f.deleted_at, f.is_locked, f.lock_pin, f.workspace_id
                FROM folders f
                WHERE f.is_trashed = 1
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
                            lockPin = if (c.isNull(9)) null else c.getString(9),
                            workspaceId = if (c.columnCount > 10 && !c.isNull(10)) c.getLong(10) else 1L
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

    override fun getFoldersByWorkspace(workspaceId: Long): Flow<List<Folder>> =
        foldersFlow.map { list -> list.filter { it.workspaceId == workspaceId } }

    override fun getFolderById(id: Long): Flow<Folder?> =
        foldersFlow.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun createFolder(name: String, colorLabel: String, isPinned: Boolean, workspaceId: Long): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("name", name.trim())
            put("color_label", colorLabel)
            put("is_pinned", if (isPinned) 1 else 0)
            put("created_at", System.currentTimeMillis())
            put("workspace_id", workspaceId)
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
            """
            SELECT 
                (
                    (SELECT COUNT(id) FROM photos WHERE folder_id IN ($inClause) AND is_trashed = 0) +
                    (SELECT COUNT(id) FROM text_notes WHERE folder_id IN ($inClause) AND is_trashed = 0) +
                    (SELECT COUNT(id) FROM canvas_notes WHERE folder_id IN ($inClause) AND is_trashed = 0) +
                    (SELECT COUNT(id) FROM document_notes WHERE folder_id IN ($inClause) AND is_trashed = 0)
                ),
                COALESCE((SELECT SUM(file_size_bytes) FROM photos WHERE folder_id IN ($inClause) AND is_trashed = 0), 0)
            """.trimIndent(),
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
            // Soft delete: move photos, text notes, canvas notes, document notes, subfolders, and folders to trash
            db.execSQL("UPDATE photos SET is_trashed = 1, deleted_at = $now WHERE folder_id IN ($inClause)")
            db.execSQL("UPDATE text_notes SET is_trashed = 1, deleted_at = $now WHERE folder_id IN ($inClause)")
            db.execSQL("UPDATE canvas_notes SET is_trashed = 1, deleted_at = $now WHERE folder_id IN ($inClause)")
            db.execSQL("UPDATE document_notes SET is_trashed = 1, deleted_at = $now WHERE folder_id IN ($inClause)")
            db.execSQL("UPDATE subfolders SET is_trashed = 1, deleted_at = $now WHERE folder_id IN ($inClause)")
            db.execSQL("UPDATE folders SET is_trashed = 1, deleted_at = $now WHERE id IN ($inClause)")
            for (fId in folderIds) {
                removeFolderFromLinkGroupsInternal(db, fId)
            }
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
            """
            SELECT 
                (
                    (SELECT COUNT(id) FROM photos WHERE subfolder_id IN ($inClause) AND is_trashed = 0) +
                    (SELECT COUNT(id) FROM text_notes WHERE subfolder_id IN ($inClause) AND is_trashed = 0) +
                    (SELECT COUNT(id) FROM canvas_notes WHERE subfolder_id IN ($inClause) AND is_trashed = 0) +
                    (SELECT COUNT(id) FROM document_notes WHERE subfolder_id IN ($inClause) AND is_trashed = 0)
                ),
                COALESCE((SELECT SUM(file_size_bytes) FROM photos WHERE subfolder_id IN ($inClause) AND is_trashed = 0), 0)
            """.trimIndent(),
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
            // Soft delete: move photos, text notes, canvas notes, document notes, and subfolders to trash
            db.execSQL("UPDATE photos SET is_trashed = 1, deleted_at = $now WHERE subfolder_id IN ($inClause)")
            db.execSQL("UPDATE text_notes SET is_trashed = 1, deleted_at = $now WHERE subfolder_id IN ($inClause)")
            db.execSQL("UPDATE canvas_notes SET is_trashed = 1, deleted_at = $now WHERE subfolder_id IN ($inClause)")
            db.execSQL("UPDATE document_notes SET is_trashed = 1, deleted_at = $now WHERE subfolder_id IN ($inClause)")
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

    override suspend fun restoreFolder(id: Long, targetWorkspaceId: Long?) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        db.beginTransaction()
        try {
            if (targetWorkspaceId != null) {
                db.execSQL(
                    "UPDATE folders SET is_trashed = 0, deleted_at = NULL, workspace_id = ? WHERE id = ?",
                    arrayOf(targetWorkspaceId.toString(), id.toString())
                )
            } else {
                db.execSQL("UPDATE folders SET is_trashed = 0, deleted_at = NULL WHERE id = ?", arrayOf(id.toString()))
            }
            db.execSQL("UPDATE subfolders SET is_trashed = 0, deleted_at = NULL WHERE folder_id = ?", arrayOf(id.toString()))
            db.execSQL("UPDATE photos SET is_trashed = 0, deleted_at = NULL WHERE folder_id = ?", arrayOf(id.toString()))
            db.execSQL("UPDATE text_notes SET is_trashed = 0, deleted_at = NULL WHERE folder_id = ?", arrayOf(id.toString()))
            db.execSQL("UPDATE canvas_notes SET is_trashed = 0, deleted_at = NULL WHERE folder_id = ?", arrayOf(id.toString()))
            db.execSQL("UPDATE document_notes SET is_trashed = 0, deleted_at = NULL WHERE folder_id = ?", arrayOf(id.toString()))
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

        // Clean up document note original files and page image files to prevent disk leaks
        val docIds = mutableListOf<Long>()
        try {
            val docCursor = db.rawQuery("SELECT origin_file_uri, id FROM document_notes WHERE folder_id = ?", arrayOf(id.toString()))
            docCursor.use { c ->
                while (c.moveToNext()) {
                    val originUri = c.getString(0)
                    docIds.add(c.getLong(1))
                    try { File(originUri).delete() } catch (_: Exception) {}
                }
            }
            if (docIds.isNotEmpty()) {
                val placeholders = docIds.joinToString(",") { "?" }
                val pagesCursor = db.rawQuery("SELECT image_uri FROM document_pages WHERE document_note_id IN ($placeholders)", docIds.map { it.toString() }.toTypedArray())
                pagesCursor.use { pc ->
                    while (pc.moveToNext()) {
                        try { File(pc.getString(0)).delete() } catch (_: Exception) {}
                    }
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("FolderRepository", "Error cleaning up document files for folder $id: ${e.message}")
        }

        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM photos WHERE folder_id = ?", arrayOf(id.toString()))
            try {
                if (docIds.isNotEmpty()) {
                    val placeholders = docIds.joinToString(",") { "?" }
                    val args = docIds.map { it.toString() }.toTypedArray()
                    db.execSQL("DELETE FROM pdf_page_pins WHERE document_id IN ($placeholders)", args)
                    db.execSQL("DELETE FROM pdf_page_drawings WHERE document_id IN ($placeholders)", args)
                }
            } catch (_: Exception) {}
            db.execSQL("DELETE FROM document_notes WHERE folder_id = ?", arrayOf(id.toString()))
            db.execSQL("DELETE FROM text_notes WHERE folder_id = ?", arrayOf(id.toString()))
            db.execSQL("DELETE FROM canvas_notes WHERE folder_id = ?", arrayOf(id.toString()))
            db.execSQL("DELETE FROM subfolders WHERE folder_id = ?", arrayOf(id.toString()))
            db.execSQL("DELETE FROM folders WHERE id = ?", arrayOf(id.toString()))
            removeFolderFromLinkGroupsInternal(db, id)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshSync()
    }

    override fun getFolderLinkGroups(): Flow<List<LinkGroup>> = folderLinkGroupsFlow.asStateFlow()

    override suspend fun createFolderLinkGroup(folderIds: List<Long>): Long = withContext(Dispatchers.IO) {
        if (folderIds.size < 2) return@withContext -1L
        val cappedIds = folderIds.distinct().take(4)
        val db = dbHelper.getSafeWritableDatabase()
        db.beginTransaction()
        val insertedId: Long
        try {
            for (fId in cappedIds) {
                removeFolderFromLinkGroupsInternal(db, fId)
            }
            val values = ContentValues().apply {
                put("item_type", LinkItemType.FOLDER.name)
                put("member_ids", cappedIds.joinToString(","))
                put("created_at", System.currentTimeMillis())
            }
            insertedId = db.insert("link_groups", null, values)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshSync()
        insertedId
    }

    override suspend fun unlinkFolder(folderId: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        db.beginTransaction()
        try {
            removeFolderFromLinkGroupsInternal(db, folderId)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshSync()
    }

    private fun removeFolderFromLinkGroupsInternal(db: SQLiteDatabase, folderId: Long) {
        try {
            val cursor = db.rawQuery("SELECT id, member_ids FROM link_groups WHERE item_type = 'FOLDER'", null)
            val toUpdate = mutableListOf<Pair<Long, List<Long>>>()
            val toDelete = mutableListOf<Long>()
            cursor.use { c ->
                while (c.moveToNext()) {
                    val lgId = c.getLong(0)
                    val members = c.getString(1).split(",").mapNotNull { it.trim().toLongOrNull() }
                    if (members.contains(folderId)) {
                        val rem = members.filter { it != folderId }
                        if (rem.size <= 1) {
                            toDelete.add(lgId)
                        } else {
                            toUpdate.add(lgId to rem)
                        }
                    }
                }
            }
            for (id in toDelete) {
                db.delete("link_groups", "id = ?", arrayOf(id.toString()))
            }
            for ((id, rem) in toUpdate) {
                val vals = ContentValues().apply { put("member_ids", rem.joinToString(",")) }
                db.update("link_groups", vals, "id = ?", arrayOf(id.toString()))
            }
        } catch (_: Exception) {}
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
