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
import com.arinara.fotara.data.model.Workspace
import com.arinara.fotara.data.model.WorkspaceKind
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

sealed class WorkspaceError {
    object LimitReached : WorkspaceError()
    object NameEmpty : WorkspaceError()
    object NameTooLong : WorkspaceError()
    object NameDuplicate : WorkspaceError()
    object NameReserved : WorkspaceError()
    object BuiltInImmutable : WorkspaceError()
    object InvalidReorder : WorkspaceError()
    object NotFound : WorkspaceError()
    object TrashedFolderCannotMove : WorkspaceError()
    data class DeletionInterrupted(val remainingCount: Int) : WorkspaceError()
}

sealed class WorkspaceResult<out T> {
    data class Success<out T>(val data: T) : WorkspaceResult<T>()
    data class Error(val error: WorkspaceError) : WorkspaceResult<Nothing>()
}

data class WorkspaceContentStats(
    val folderCount: Int,
    val noteCount: Int,
    val totalSizeBytes: Long
)

object WorkspaceValidator {
    const val MAX_CUSTOM_WORKSPACES = 10
    const val MIN_NAME_LENGTH = 1
    const val MAX_NAME_LENGTH = 20

    fun normalizeName(name: String): String {
        return name.trim().replace(Regex("\\s+"), " ")
    }

    fun validateName(
        rawName: String,
        existingWorkspaces: List<Workspace>,
        editingWorkspaceId: Long? = null
    ): WorkspaceError? {
        val normalized = normalizeName(rawName)
        if (normalized.isEmpty()) return WorkspaceError.NameEmpty
        if (normalized.length > MAX_NAME_LENGTH) return WorkspaceError.NameTooLong

        if (normalized.equals("Home", ignoreCase = true) ||
            normalized.equals("Archive", ignoreCase = true) ||
            normalized.equals("Arsip", ignoreCase = true)
        ) {
            return WorkspaceError.NameReserved
        }

        val duplicate = existingWorkspaces.any { ws ->
            ws.id != editingWorkspaceId && ws.name.equals(normalized, ignoreCase = true)
        }
        if (duplicate) return WorkspaceError.NameDuplicate

        return null
    }
}

interface WorkspaceRepository {
    val selectedWorkspaceId: StateFlow<Long>
    fun selectWorkspace(workspaceId: Long)
    fun observeWorkspaces(): Flow<List<Workspace>>
    fun observeFoldersIn(workspaceId: Long): Flow<List<Folder>>
    suspend fun getWorkspacesSync(): List<Workspace>
    suspend fun getHomeWorkspace(): Workspace
    suspend fun getArchiveWorkspace(): Workspace
    suspend fun createWorkspace(name: String): WorkspaceResult<Workspace>
    suspend fun renameWorkspace(workspaceId: Long, newName: String): WorkspaceResult<Unit>
    suspend fun reorderWorkspaces(workspaceIds: List<Long>): WorkspaceResult<Unit>
    suspend fun moveFolders(folderIds: List<Long>, targetWorkspaceId: Long): WorkspaceResult<Unit>
    suspend fun getWorkspaceStats(workspaceId: Long): WorkspaceContentStats
    suspend fun deleteWorkspaceMoveFoldersToHome(workspaceId: Long): WorkspaceResult<Unit>
    suspend fun deleteWorkspaceWithContents(
        workspaceId: Long,
        permanent: Boolean,
        onProgress: (current: Int, total: Int) -> Unit
    ): WorkspaceResult<Unit>
    suspend fun repairDanglingWorkspaces(): Int
    suspend fun refresh()
}

class SqliteWorkspaceRepository(
    private val dbHelper: FotaraDbHelper,
    private val folderRepository: FolderRepository,
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
) : WorkspaceRepository {

    private val scope = coroutineScope
    private val workspacesFlow = MutableStateFlow<List<Workspace>>(emptyList())
    private val _selectedWorkspaceId = MutableStateFlow(FotaraDbHelper.HOME_WORKSPACE_ID)
    override val selectedWorkspaceId: StateFlow<Long> = _selectedWorkspaceId.asStateFlow()

    override fun selectWorkspace(workspaceId: Long) {
        _selectedWorkspaceId.value = workspaceId
    }

    init {
        scope.launch {
            try {
                repairDanglingWorkspaces()
            } catch (_: Exception) {}
            refreshSync()
        }
    }

    private fun refreshSync() {
        try {
            val db = dbHelper.getSafeReadableDatabase()
            val list = mutableListOf<Workspace>()
            val cursor = db.rawQuery(
                "SELECT id, uuid, kind, name, position, created_at FROM workspaces ORDER BY position ASC",
                null
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    list.add(
                        Workspace(
                            id = c.getLong(0),
                            uuid = c.getString(1),
                            kind = try { WorkspaceKind.valueOf(c.getString(2)) } catch (_: Exception) { WorkspaceKind.CUSTOM },
                            name = c.getString(3) ?: "",
                            position = c.getInt(4),
                            createdAt = c.getLong(5)
                        )
                    )
                }
            }

            // Invariant guard: if table was empty, seed Home & Archive immediately
            if (list.none { it.kind == WorkspaceKind.HOME }) {
                val now = System.currentTimeMillis()
                val writeDb = dbHelper.getSafeWritableDatabase()
                writeDb.execSQL(
                    "INSERT OR IGNORE INTO workspaces (id, uuid, kind, name, position, created_at) VALUES (1, '${FotaraDbHelper.HOME_WORKSPACE_UUID}', 'HOME', '', 0, $now)"
                )
                writeDb.execSQL(
                    "INSERT OR IGNORE INTO workspaces (id, uuid, kind, name, position, created_at) VALUES (2, '${FotaraDbHelper.ARCHIVE_WORKSPACE_UUID}', 'ARCHIVE', '', 1, $now)"
                )
                refreshSync()
                return
            }

            workspacesFlow.value = list
        } catch (e: Exception) {
            android.util.Log.e("SqliteWorkspaceRepo", "Error refreshing workspaces: ${e.message}", e)
        }
    }

    override fun observeWorkspaces(): Flow<List<Workspace>> = workspacesFlow.asStateFlow()

    override fun observeFoldersIn(workspaceId: Long): Flow<List<Folder>> {
        return folderRepository.getFoldersByWorkspace(workspaceId)
    }

    override suspend fun getWorkspacesSync(): List<Workspace> = withContext(Dispatchers.IO) {
        if (workspacesFlow.value.isEmpty()) {
            refreshSync()
        }
        workspacesFlow.value
    }

    override suspend fun getHomeWorkspace(): Workspace = withContext(Dispatchers.IO) {
        val list = getWorkspacesSync()
        list.firstOrNull { it.kind == WorkspaceKind.HOME }
            ?: Workspace(id = 1L, uuid = FotaraDbHelper.HOME_WORKSPACE_UUID, kind = WorkspaceKind.HOME, position = 0)
    }

    override suspend fun getArchiveWorkspace(): Workspace = withContext(Dispatchers.IO) {
        val list = getWorkspacesSync()
        list.firstOrNull { it.kind == WorkspaceKind.ARCHIVE }
            ?: Workspace(id = 2L, uuid = FotaraDbHelper.ARCHIVE_WORKSPACE_UUID, kind = WorkspaceKind.ARCHIVE, position = 1)
    }

    override suspend fun createWorkspace(name: String): WorkspaceResult<Workspace> = withContext(Dispatchers.IO) {
        val current = getWorkspacesSync()
        val customCount = current.count { it.kind == WorkspaceKind.CUSTOM }
        if (customCount >= WorkspaceValidator.MAX_CUSTOM_WORKSPACES) {
            return@withContext WorkspaceResult.Error(WorkspaceError.LimitReached)
        }

        val validationErr = WorkspaceValidator.validateName(name, current)
        if (validationErr != null) {
            return@withContext WorkspaceResult.Error(validationErr)
        }

        val normalizedName = WorkspaceValidator.normalizeName(name)
        val nextPos = current.size
        val uuid = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("uuid", uuid)
            put("kind", WorkspaceKind.CUSTOM.name)
            put("name", normalizedName)
            put("position", nextPos)
            put("created_at", now)
        }
        val insertedId = db.insert("workspaces", null, values)
        refreshSync()

        val created = Workspace(
            id = insertedId,
            uuid = uuid,
            kind = WorkspaceKind.CUSTOM,
            name = normalizedName,
            position = nextPos,
            createdAt = now
        )
        WorkspaceResult.Success(created)
    }

    override suspend fun renameWorkspace(workspaceId: Long, newName: String): WorkspaceResult<Unit> = withContext(Dispatchers.IO) {
        val current = getWorkspacesSync()
        val target = current.firstOrNull { it.id == workspaceId }
            ?: return@withContext WorkspaceResult.Error(WorkspaceError.NotFound)

        if (target.kind != WorkspaceKind.CUSTOM) {
            return@withContext WorkspaceResult.Error(WorkspaceError.BuiltInImmutable)
        }

        val validationErr = WorkspaceValidator.validateName(newName, current, editingWorkspaceId = workspaceId)
        if (validationErr != null) {
            return@withContext WorkspaceResult.Error(validationErr)
        }

        val normalized = WorkspaceValidator.normalizeName(newName)
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("name", normalized)
        }
        db.update("workspaces", values, "id = ?", arrayOf(workspaceId.toString()))
        refreshSync()
        WorkspaceResult.Success(Unit)
    }

    override suspend fun reorderWorkspaces(workspaceIds: List<Long>): WorkspaceResult<Unit> = withContext(Dispatchers.IO) {
        val current = getWorkspacesSync()
        if (workspaceIds.isEmpty() || current.isEmpty()) {
            return@withContext WorkspaceResult.Error(WorkspaceError.InvalidReorder)
        }

        val homeWs = current.firstOrNull { it.kind == WorkspaceKind.HOME }
            ?: return@withContext WorkspaceResult.Error(WorkspaceError.NotFound)

        // Invariant: Home must remain leftmost at index 0
        if (workspaceIds.first() != homeWs.id) {
            return@withContext WorkspaceResult.Error(WorkspaceError.InvalidReorder)
        }

        // Must contain all existing workspace ids exactly once
        val currentIds = current.map { it.id }.toSet()
        if (workspaceIds.toSet() != currentIds || workspaceIds.size != current.size) {
            return@withContext WorkspaceResult.Error(WorkspaceError.InvalidReorder)
        }

        val db = dbHelper.getSafeWritableDatabase()
        db.beginTransaction()
        try {
            workspaceIds.forEachIndexed { index, wsId ->
                val cv = ContentValues().apply {
                    put("position", index)
                }
                db.update("workspaces", cv, "id = ?", arrayOf(wsId.toString()))
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            android.util.Log.e("SqliteWorkspaceRepo", "Reorder transaction failed: ${e.message}", e)
            return@withContext WorkspaceResult.Error(WorkspaceError.InvalidReorder)
        } finally {
            db.endTransaction()
        }

        refreshSync()
        WorkspaceResult.Success(Unit)
    }

    override suspend fun moveFolders(folderIds: List<Long>, targetWorkspaceId: Long): WorkspaceResult<Unit> = withContext(Dispatchers.IO) {
        if (folderIds.isEmpty()) return@withContext WorkspaceResult.Success(Unit)

        val current = getWorkspacesSync()
        val targetExists = current.any { it.id == targetWorkspaceId }
        if (!targetExists) {
            return@withContext WorkspaceResult.Error(WorkspaceError.NotFound)
        }

        val db = dbHelper.getSafeWritableDatabase()

        // Check if any folder is trashed
        val placeholders = folderIds.joinToString(",") { "?" }
        val trashedCursor = db.rawQuery(
            "SELECT COUNT(*) FROM folders WHERE id IN ($placeholders) AND is_trashed = 1",
            folderIds.map { it.toString() }.toTypedArray()
        )
        val hasTrashed = trashedCursor.use { if (it.moveToNext()) it.getInt(0) > 0 else false }
        if (hasTrashed) {
            return@withContext WorkspaceResult.Error(WorkspaceError.TrashedFolderCannotMove)
        }

        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put("workspace_id", targetWorkspaceId)
            }
            db.update("folders", cv, "id IN ($placeholders)", folderIds.map { it.toString() }.toTypedArray())
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        folderRepository.refresh()
        refreshSync()
        WorkspaceResult.Success(Unit)
    }

    override suspend fun getWorkspaceStats(workspaceId: Long): WorkspaceContentStats = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeReadableDatabase()
        val args = arrayOf(workspaceId.toString())

        val foldersCursor = db.rawQuery(
            "SELECT id FROM folders WHERE workspace_id = ? AND is_trashed = 0",
            args
        )
        val folderIds = mutableListOf<Long>()
        foldersCursor.use { c ->
            while (c.moveToNext()) {
                folderIds.add(c.getLong(0))
            }
        }

        if (folderIds.isEmpty()) {
            return@withContext WorkspaceContentStats(0, 0, 0L)
        }

        val fPlaceholders = folderIds.joinToString(",") { "?" }
        val fArgs = folderIds.map { it.toString() }.toTypedArray()

        var totalNotes = 0
        val photosCountCursor = db.rawQuery(
            "SELECT COUNT(*) FROM photos WHERE folder_id IN ($fPlaceholders) AND is_trashed = 0",
            fArgs
        )
        totalNotes += photosCountCursor.use { if (it.moveToNext()) it.getInt(0) else 0 }

        val textCountCursor = db.rawQuery(
            "SELECT COUNT(*) FROM text_notes WHERE folder_id IN ($fPlaceholders) AND is_trashed = 0",
            fArgs
        )
        totalNotes += textCountCursor.use { if (it.moveToNext()) it.getInt(0) else 0 }

        val canvasCountCursor = db.rawQuery(
            "SELECT COUNT(*) FROM canvas_notes WHERE folder_id IN ($fPlaceholders) AND is_trashed = 0",
            fArgs
        )
        totalNotes += canvasCountCursor.use { if (it.moveToNext()) it.getInt(0) else 0 }

        val docCountCursor = db.rawQuery(
            "SELECT COUNT(*) FROM document_notes WHERE folder_id IN ($fPlaceholders) AND is_trashed = 0",
            fArgs
        )
        totalNotes += docCountCursor.use { if (it.moveToNext()) it.getInt(0) else 0 }

        var totalBytes = 0L
        try {
            val photoFilesCursor = db.rawQuery(
                "SELECT file_path, thumbnail_path, file_size_bytes FROM photos WHERE folder_id IN ($fPlaceholders) AND is_trashed = 0",
                fArgs
            )
            photoFilesCursor.use { c ->
                while (c.moveToNext()) {
                    val filePath = c.getString(0)
                    val thumbPath = if (c.isNull(1)) null else c.getString(1)
                    val dbSize = if (c.isNull(2)) 0L else c.getLong(2)
                    if (dbSize > 0) {
                        totalBytes += dbSize
                    } else if (filePath != null) {
                        val f = java.io.File(filePath)
                        if (f.exists()) totalBytes += f.length()
                    }
                    if (thumbPath != null) {
                        val tf = java.io.File(thumbPath)
                        if (tf.exists()) totalBytes += tf.length()
                    }
                }
            }

            val docFilesCursor = db.rawQuery(
                "SELECT origin_file_uri, file_size_bytes FROM document_notes WHERE folder_id IN ($fPlaceholders) AND is_trashed = 0",
                fArgs
            )
            docFilesCursor.use { c ->
                while (c.moveToNext()) {
                    val uri = c.getString(0)
                    val dbSize = if (c.isNull(1)) 0L else c.getLong(1)
                    if (dbSize > 0) {
                        totalBytes += dbSize
                    } else if (uri != null) {
                        val f = java.io.File(uri)
                        if (f.exists()) totalBytes += f.length()
                    }
                }
            }

            val canvasAssetsCursor = db.rawQuery(
                """
                SELECT a.file_path, a.file_size
                FROM canvas_assets a
                JOIN canvas_notes n ON a.canvas_id = n.id
                WHERE n.folder_id IN ($fPlaceholders) AND n.is_trashed = 0
                """.trimIndent(),
                fArgs
            )
            canvasAssetsCursor.use { c ->
                while (c.moveToNext()) {
                    val path = c.getString(0)
                    val dbSize = if (c.isNull(1)) 0L else c.getLong(1)
                    if (dbSize > 0) {
                        totalBytes += dbSize
                    } else if (path != null) {
                        val f = java.io.File(path)
                        if (f.exists()) totalBytes += f.length()
                    }
                }
            }
        } catch (_: Exception) {}

        WorkspaceContentStats(
            folderCount = folderIds.size,
            noteCount = totalNotes,
            totalSizeBytes = totalBytes
        )
    }

    override suspend fun deleteWorkspaceMoveFoldersToHome(workspaceId: Long): WorkspaceResult<Unit> = withContext(Dispatchers.IO) {
        val current = getWorkspacesSync()
        val target = current.firstOrNull { it.id == workspaceId }
            ?: return@withContext WorkspaceResult.Error(WorkspaceError.NotFound)

        if (target.kind != WorkspaceKind.CUSTOM) {
            return@withContext WorkspaceResult.Error(WorkspaceError.BuiltInImmutable)
        }

        val db = dbHelper.getSafeWritableDatabase()
        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put("workspace_id", FotaraDbHelper.HOME_WORKSPACE_ID)
            }
            db.update("folders", cv, "workspace_id = ?", arrayOf(workspaceId.toString()))
            db.delete("workspaces", "id = ?", arrayOf(workspaceId.toString()))

            val remainingCursor = db.rawQuery("SELECT id FROM workspaces ORDER BY position ASC", null)
            val remainingIds = mutableListOf<Long>()
            remainingCursor.use { c ->
                while (c.moveToNext()) {
                    remainingIds.add(c.getLong(0))
                }
            }
            remainingIds.forEachIndexed { index, id ->
                val posCv = ContentValues().apply {
                    put("position", index)
                }
                db.update("workspaces", posCv, "id = ?", arrayOf(id.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        folderRepository.refresh()
        if (_selectedWorkspaceId.value == workspaceId) {
            _selectedWorkspaceId.value = FotaraDbHelper.HOME_WORKSPACE_ID
        }
        refreshSync()
        WorkspaceResult.Success(Unit)
    }

    override suspend fun deleteWorkspaceWithContents(
        workspaceId: Long,
        permanent: Boolean,
        onProgress: (current: Int, total: Int) -> Unit
    ): WorkspaceResult<Unit> = withContext(Dispatchers.IO) {
        val current = getWorkspacesSync()
        val target = current.firstOrNull { it.id == workspaceId }
            ?: return@withContext WorkspaceResult.Error(WorkspaceError.NotFound)

        if (target.kind != WorkspaceKind.CUSTOM) {
            return@withContext WorkspaceResult.Error(WorkspaceError.BuiltInImmutable)
        }

        val db = dbHelper.getSafeWritableDatabase()
        val liveFoldersCursor = db.rawQuery(
            "SELECT id FROM folders WHERE workspace_id = ? AND is_trashed = 0",
            arrayOf(workspaceId.toString())
        )
        val liveFolderIds = mutableListOf<Long>()
        liveFoldersCursor.use { c ->
            while (c.moveToNext()) {
                liveFolderIds.add(c.getLong(0))
            }
        }

        val total = liveFolderIds.size
        var processedCount = 0

        try {
            for (folderId in liveFolderIds) {
                if (permanent) {
                    folderRepository.purgeFolderPermanently(folderId)
                } else {
                    folderRepository.deleteFolders(listOf(folderId))
                }
                processedCount++
                onProgress(processedCount, total)
            }
        } catch (e: Exception) {
            android.util.Log.e("SqliteWorkspaceRepo", "Error deleting workspace contents: ${e.message}", e)
            folderRepository.refresh()
            refreshSync()
            return@withContext WorkspaceResult.Error(WorkspaceError.DeletionInterrupted(total - processedCount))
        }

        db.beginTransaction()
        try {
            val cv = ContentValues().apply {
                put("workspace_id", FotaraDbHelper.HOME_WORKSPACE_ID)
            }
            db.update("folders", cv, "workspace_id = ?", arrayOf(workspaceId.toString()))
            db.delete("workspaces", "id = ?", arrayOf(workspaceId.toString()))

            val remainingCursor = db.rawQuery("SELECT id FROM workspaces ORDER BY position ASC", null)
            val remainingIds = mutableListOf<Long>()
            remainingCursor.use { c ->
                while (c.moveToNext()) {
                    remainingIds.add(c.getLong(0))
                }
            }
            remainingIds.forEachIndexed { index, id ->
                val posCv = ContentValues().apply {
                    put("position", index)
                }
                db.update("workspaces", posCv, "id = ?", arrayOf(id.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        folderRepository.refresh()
        if (_selectedWorkspaceId.value == workspaceId) {
            _selectedWorkspaceId.value = FotaraDbHelper.HOME_WORKSPACE_ID
        }
        refreshSync()
        WorkspaceResult.Success(Unit)
    }

    override suspend fun repairDanglingWorkspaces(): Int = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val cursor = db.rawQuery(
            "SELECT COUNT(*) FROM folders WHERE workspace_id NOT IN (SELECT id FROM workspaces)",
            null
        )
        val count = cursor.use { if (it.moveToNext()) it.getInt(0) else 0 }
        if (count > 0) {
            db.execSQL("UPDATE folders SET workspace_id = 1 WHERE workspace_id NOT IN (SELECT id FROM workspaces)")
            folderRepository.refresh()
        }
        count
    }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        refreshSync()
    }
}
