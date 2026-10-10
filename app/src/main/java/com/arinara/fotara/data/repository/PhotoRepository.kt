// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.repository

import android.content.ContentValues
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.data.model.LinkGroup
import com.arinara.fotara.data.model.LinkItemType
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.data.model.PhotoSource
import com.arinara.fotara.data.storage.PhotoStorageManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

interface PhotoRepository {
    fun getPhotosByFolder(folderId: Long, subfolderId: Long? = null): Flow<List<Photo>>
    fun getPhotosAddedToday(): Flow<List<Photo>>
    fun getPhotosDueTomorrow(): Flow<List<Photo>>
    fun searchPhotos(query: String): Flow<List<Photo>>
    fun getGroupsByFolder(folderId: Long, subfolderId: Long? = null): Flow<List<PhotoGroup>>
    fun getAllActiveGroups(): Flow<List<PhotoGroup>>
    fun searchGroups(query: String): Flow<List<PhotoGroup>>
    suspend fun getGroupById(groupId: Long): PhotoGroup?
    suspend fun createGroup(folderId: Long, subfolderId: Long?, name: String, photoIds: List<Long>, tagColor: String? = null): Long
    suspend fun renameGroup(groupId: Long, newName: String)
    suspend fun updateGroupTagColor(groupId: Long, colorHex: String?)
    suspend fun updateGroupDeadline(groupId: Long, deadlineMs: Long?)
    suspend fun setGroupCoverPhoto(groupId: Long, photoId: Long)
    suspend fun ungroup(groupId: Long)
    suspend fun deleteGroup(groupId: Long)
    suspend fun deleteGroups(groupIds: List<Long>)
    suspend fun removePhotoFromGroup(photoId: Long)
    suspend fun addPhotosToGroup(groupId: Long, photoIds: List<Long>)
    suspend fun addPhotosToExistingGroup(groupId: Long, photoIds: List<Long>)
    suspend fun moveGroup(groupId: Long, targetFolderId: Long, targetSubfolderId: Long? = null)
    suspend fun moveGroups(groupIds: List<Long>, targetFolderId: Long, targetSubfolderId: Long? = null)
    suspend fun mergeGroups(sourceGroupIds: List<Long>, newName: String, targetFolderId: Long, targetSubfolderId: Long? = null): Long
    suspend fun addPhoto(photo: Photo): Long
    suspend fun updatePhoto(photo: Photo)
    suspend fun renamePhoto(id: Long, newCaption: String)
    suspend fun updatePhotoNote(id: Long, newNote: String?)
    suspend fun deletePhoto(id: Long)
    suspend fun deletePhotos(ids: List<Long>)
    suspend fun movePhotos(ids: List<Long>, targetFolderId: Long, targetSubfolderId: Long? = null)
    suspend fun copyPhoto(photoId: Long, targetFolderId: Long, targetSubfolderId: Long? = null, targetGroupId: Long? = null): Long
    suspend fun updatePhotosTagColor(ids: List<Long>, colorHex: String?)
    suspend fun getPhotoById(id: Long): Photo?
    fun getTrashedPhotos(): Flow<List<Photo>>
    suspend fun restorePhoto(id: Long, targetFolderId: Long? = null, targetSubfolderId: Long? = null)
    suspend fun restoreGroup(id: Long)
    suspend fun purgePhotoPermanently(id: Long)
    suspend fun emptyTrash()
    suspend fun purgeOldTrashedItems(retentionDays: Int = 30)
    suspend fun getAllActivePhotos(): List<Photo>
    fun getAllActivePhotosFlow(): Flow<List<Photo>>
    suspend fun rebuildSearchIndex(): Int
    suspend fun rotatePhotoClockwise(id: Long): Photo?
    suspend fun cropPhoto(id: Long, left: Float, top: Float, right: Float, bottom: Float): Photo?
    fun getAllSmartTags(): Flow<List<String>>
    fun getPhotosByTag(tag: String): Flow<List<Photo>>
    suspend fun addTagToPhoto(id: Long, tag: String)
    suspend fun removeTagFromPhoto(id: Long, tag: String)
    fun getGridLinkGroups(): Flow<List<LinkGroup>>
    suspend fun createGridLinkGroup(memberIds: List<Long>): Long
    suspend fun unlinkGridItem(itemId: Long)
    suspend fun refresh()
}

class SqlitePhotoRepository(
    private val dbHelper: FotaraDbHelper,
    private val folderRepository: FolderRepository,
    private val photoStorageManager: PhotoStorageManager? = null,
    coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
    private val photoDrawingRepository: PhotoDrawingRepository? = null
) : PhotoRepository {

    private val scope = coroutineScope
    private val photosFlow = MutableStateFlow<List<Photo>>(emptyList())
    private val trashedPhotosFlow = MutableStateFlow<List<Photo>>(emptyList())
    private val groupsFlow = MutableStateFlow<List<PhotoGroup>>(emptyList())
    private val gridLinkGroupsFlow = MutableStateFlow<List<LinkGroup>>(emptyList())

    init {
        scope.launch {
            refreshSync()
        }
    }

    private fun refreshSync() {
        try {
            val db = dbHelper.getSafeReadableDatabase()
            val list = mutableListOf<Photo>()
            val cursor = db.rawQuery(
                """
                SELECT id, file_path, thumbnail_path, folder_id, subfolder_id,
                       created_at, added_at, tag_color, caption, ocr_text,
                       source, linked_deadline, file_size_bytes, note,
                       is_trashed, deleted_at, group_id, tags, scheduled_at, alert_type
                FROM photos
                WHERE is_trashed = 0
                ORDER BY added_at DESC
                """.trimIndent(),
                null
            )

            cursor.use { c ->
                while (c.moveToNext()) {
                    list.add(
                        Photo(
                            id = c.getLong(0),
                            fileUri = c.getString(1),
                            thumbnailUri = if (c.isNull(2)) null else c.getString(2),
                            folderId = c.getLong(3),
                            subfolderId = if (c.isNull(4)) null else c.getLong(4),
                            createdAt = c.getLong(5),
                            addedAt = c.getLong(6),
                            tagColor = if (c.isNull(7)) null else c.getString(7),
                            caption = if (c.isNull(8)) null else c.getString(8),
                            ocrText = if (c.isNull(9)) null else c.getString(9),
                            source = try { PhotoSource.valueOf(c.getString(10)) } catch (_: Exception) { PhotoSource.CAMERA },
                            linkedDeadline = if (c.isNull(11)) null else c.getLong(11),
                            fileSizeBytes = c.getLong(12),
                            note = if (c.isNull(13)) null else c.getString(13),
                            isTrashed = c.getInt(14) == 1,
                            deletedAt = if (c.isNull(15)) null else c.getLong(15),
                            groupId = if (c.isNull(16)) null else c.getLong(16),
                            tags = if (c.isNull(17)) null else c.getString(17),
                            scheduledAt = if (c.columnCount > 18 && !c.isNull(18)) c.getLong(18) else null,
                            alertType = if (c.columnCount > 19 && !c.isNull(19)) c.getString(19) else null
                        )
                    )
                }
            }
            photosFlow.value = list

            // Load trashed photos
            val trashedList = mutableListOf<Photo>()
            val trashedCursor = db.rawQuery(
                """
                SELECT id, file_path, thumbnail_path, folder_id, subfolder_id,
                       created_at, added_at, tag_color, caption, ocr_text,
                       source, linked_deadline, file_size_bytes, note,
                       is_trashed, deleted_at, group_id, tags, scheduled_at, alert_type
                FROM photos
                WHERE is_trashed = 1
                ORDER BY deleted_at DESC
                """.trimIndent(),
                null
            )
            trashedCursor.use { tc ->
                while (tc.moveToNext()) {
                    trashedList.add(
                        Photo(
                            id = tc.getLong(0),
                            fileUri = tc.getString(1),
                            thumbnailUri = if (tc.isNull(2)) null else tc.getString(2),
                            folderId = tc.getLong(3),
                            subfolderId = if (tc.isNull(4)) null else tc.getLong(4),
                            createdAt = tc.getLong(5),
                            addedAt = tc.getLong(6),
                            tagColor = if (tc.isNull(7)) null else tc.getString(7),
                            caption = if (tc.isNull(8)) null else tc.getString(8),
                            ocrText = if (tc.isNull(9)) null else tc.getString(9),
                            source = try { PhotoSource.valueOf(tc.getString(10)) } catch (_: Exception) { PhotoSource.CAMERA },
                            linkedDeadline = if (tc.isNull(11)) null else tc.getLong(11),
                            fileSizeBytes = tc.getLong(12),
                            note = if (tc.isNull(13)) null else tc.getString(13),
                            isTrashed = true,
                            deletedAt = if (tc.isNull(15)) null else tc.getLong(15),
                            groupId = if (tc.isNull(16)) null else tc.getLong(16),
                            tags = if (tc.isNull(17)) null else tc.getString(17),
                            scheduledAt = if (tc.columnCount > 18 && !tc.isNull(18)) tc.getLong(18) else null,
                            alertType = if (tc.columnCount > 19 && !tc.isNull(19)) tc.getString(19) else null
                        )
                    )
                }
            }
            trashedPhotosFlow.value = trashedList

            // Load active photo groups
            val groupList = mutableListOf<PhotoGroup>()
            val groupCursor = db.rawQuery(
                """
                SELECT id, folder_id, subfolder_id, name, tag_color,
                       created_at, cover_photo_id, is_trashed, deleted_at,
                       linked_deadline, added_at, scheduled_at, alert_type
                FROM photo_groups
                WHERE is_trashed = 0
                ORDER BY created_at DESC
                """.trimIndent(),
                null
            )
            groupCursor.use { gc ->
                while (gc.moveToNext()) {
                    val createdAt = gc.getLong(5)
                    val addedAt = if (gc.columnCount > 10 && !gc.isNull(10)) gc.getLong(10) else createdAt
                    groupList.add(
                        PhotoGroup(
                            id = gc.getLong(0),
                            folderId = gc.getLong(1),
                            subfolderId = if (gc.isNull(2)) null else gc.getLong(2),
                            name = gc.getString(3),
                            tagColor = if (gc.isNull(4)) null else gc.getString(4),
                            createdAt = createdAt,
                            addedAt = if (addedAt == 0L) createdAt else addedAt,
                            coverPhotoId = if (gc.isNull(6)) null else gc.getLong(6),
                            isTrashed = gc.getInt(7) == 1,
                            deletedAt = if (gc.isNull(8)) null else gc.getLong(8),
                            linkedDeadline = if (gc.isNull(9)) null else gc.getLong(9),
                            scheduledAt = if (gc.columnCount > 11 && !gc.isNull(11)) gc.getLong(11) else null,
                            alertType = if (gc.columnCount > 12 && !gc.isNull(12)) gc.getString(12) else null
                        )
                    )
                }
            }
            groupsFlow.value = groupList

            // Load grid link groups
            val linkList = mutableListOf<LinkGroup>()
            try {
                val linkCursor = db.rawQuery(
                    "SELECT id, item_type, member_ids, created_at FROM link_groups WHERE item_type = 'GRID_ITEM' ORDER BY created_at ASC",
                    null
                )
                linkCursor.use { lc ->
                    while (lc.moveToNext()) {
                        val lgId = lc.getLong(0)
                        val type = LinkItemType.valueOf(lc.getString(1))
                        val rawIds = lc.getString(2)
                        val memberIds = rawIds.split(",").mapNotNull { it.trim().toLongOrNull() }
                        linkList.add(LinkGroup(id = lgId, itemType = type, memberIds = memberIds, createdAt = lc.getLong(3)))
                    }
                }
            } catch (_: Exception) {}
            gridLinkGroupsFlow.value = linkList
        } catch (e: Exception) {
            android.util.Log.e("SqlitePhotoRepo", "Failed to refresh photos cleanly: ${e.message}", e)
        }
    }

    override suspend fun refresh() = withContext(Dispatchers.IO) {
        refreshSync()
    }

    override suspend fun getPhotoById(id: Long): Photo? = withContext(Dispatchers.IO) {
        photosFlow.value.firstOrNull { it.id == id && !it.isTrashed }
    }

    override fun getPhotosByFolder(folderId: Long, subfolderId: Long?): Flow<List<Photo>> =
        photosFlow.map { list ->
            list.filter { photo ->
                photo.folderId == folderId && (subfolderId == null || photo.subfolderId == subfolderId)
            }
        }

    override fun getGroupsByFolder(folderId: Long, subfolderId: Long?): Flow<List<PhotoGroup>> =
        groupsFlow.map { list ->
            list.filter { group ->
                group.folderId == folderId && (subfolderId == null || group.subfolderId == subfolderId)
            }
        }

    override fun getAllActiveGroups(): Flow<List<PhotoGroup>> = groupsFlow.asStateFlow()

    override fun searchGroups(query: String): Flow<List<PhotoGroup>> =
        groupsFlow.map { list ->
            val trimmed = query.trim()
            if (trimmed.isBlank()) emptyList()
            else list.filter { it.name.contains(trimmed, ignoreCase = true) }
        }.flowOn(Dispatchers.IO)

    override suspend fun getGroupById(groupId: Long): PhotoGroup? = withContext(Dispatchers.IO) {
        groupsFlow.value.firstOrNull { it.id == groupId && !it.isTrashed }
    }

    override suspend fun createGroup(
        folderId: Long,
        subfolderId: Long?,
        name: String,
        photoIds: List<Long>,
        tagColor: String?
    ): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val memberPhotos = photosFlow.value.filter { photoIds.contains(it.id) }
        val earliestPhoto = memberPhotos.minByOrNull { it.addedAt } ?: memberPhotos.firstOrNull()
        val coverPhotoId = earliestPhoto?.id

        db.beginTransaction()
        val insertedGroupId: Long
        try {
            val now = System.currentTimeMillis()
            val groupValues = ContentValues().apply {
                put("folder_id", folderId)
                put("subfolder_id", subfolderId)
                put("name", name.trim())
                put("tag_color", tagColor)
                put("created_at", now)
                put("added_at", now)
                put("cover_photo_id", coverPhotoId)
                put("is_trashed", 0)
            }
            insertedGroupId = db.insert("photo_groups", null, groupValues)

            if (photoIds.isNotEmpty()) {
                val inClause = photoIds.joinToString(",") { it.toString() }
                db.execSQL("UPDATE photos SET group_id = $insertedGroupId WHERE id IN ($inClause)")
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        refreshSync()
        folderRepository.refresh()
        insertedGroupId
    }

    override suspend fun renameGroup(groupId: Long, newName: String) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("name", newName.trim())
        }
        db.update("photo_groups", values, "id = ?", arrayOf(groupId.toString()))
        refreshSync()
    }

    override suspend fun updateGroupTagColor(groupId: Long, colorHex: String?) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("tag_color", colorHex)
        }
        db.update("photo_groups", values, "id = ?", arrayOf(groupId.toString()))
        refreshSync()
    }

    override suspend fun updateGroupDeadline(groupId: Long, deadlineMs: Long?) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            if (deadlineMs != null) put("linked_deadline", deadlineMs) else putNull("linked_deadline")
        }
        db.update("photo_groups", values, "id = ?", arrayOf(groupId.toString()))
        refreshSync()
    }

    override suspend fun setGroupCoverPhoto(groupId: Long, photoId: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("cover_photo_id", photoId)
        }
        db.update("photo_groups", values, "id = ?", arrayOf(groupId.toString()))
        refreshSync()
    }

    override suspend fun ungroup(groupId: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        db.beginTransaction()
        try {
            db.execSQL("UPDATE photos SET group_id = NULL WHERE group_id = ?", arrayOf(groupId.toString()))
            db.delete("photo_groups", "id = ?", arrayOf(groupId.toString()))
            removeGridItemFromLinkGroupsInternal(db, groupId)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshSync()
        folderRepository.refresh()
    }

    override suspend fun deleteGroups(groupIds: List<Long>) = withContext(Dispatchers.IO) {
        if (groupIds.isEmpty()) return@withContext
        val db = dbHelper.getSafeWritableDatabase()
        val inClause = groupIds.joinToString(",") { it.toString() }
        val now = System.currentTimeMillis()
        db.beginTransaction()
        try {
            db.execSQL("UPDATE photo_groups SET is_trashed = 1, deleted_at = $now WHERE id IN ($inClause)")
            db.execSQL("UPDATE photos SET is_trashed = 1, deleted_at = $now, group_id = NULL WHERE group_id IN ($inClause)")
            for (gId in groupIds) {
                removeGridItemFromLinkGroupsInternal(db, gId)
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshSync()
        folderRepository.refresh()
    }

    override suspend fun deleteGroup(groupId: Long) = withContext(Dispatchers.IO) {
        deleteGroups(listOf(groupId))
    }

    override suspend fun removePhotoFromGroup(photoId: Long) = withContext(Dispatchers.IO) {
        val targetPhoto = photosFlow.value.firstOrNull { it.id == photoId } ?: return@withContext
        val gId = targetPhoto.groupId ?: return@withContext
        val db = dbHelper.getSafeWritableDatabase()
        val currentMembers = photosFlow.value.filter { it.groupId == gId && !it.isTrashed }

        db.beginTransaction()
        try {
            // Remove photo from group
            db.execSQL("UPDATE photos SET group_id = NULL WHERE id = ?", arrayOf(photoId.toString()))

            val remainingMembers = currentMembers.filter { it.id != photoId }
            if (remainingMembers.size <= 1) {
                // Auto-dissolve group if drops to 1 or 0 members
                if (remainingMembers.isNotEmpty()) {
                    val remId = remainingMembers.first().id
                    db.execSQL("UPDATE photos SET group_id = NULL WHERE id = ?", arrayOf(remId.toString()))
                }
                db.delete("photo_groups", "id = ?", arrayOf(gId.toString()))
                removeGridItemFromLinkGroupsInternal(db, gId)
            } else {
                // Update cover_photo_id if needed
                val earliestRemaining = remainingMembers.minByOrNull { it.addedAt }?.id
                val values = ContentValues().apply {
                    put("cover_photo_id", earliestRemaining)
                }
                db.update("photo_groups", values, "id = ?", arrayOf(gId.toString()))
            }
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshSync()
        folderRepository.refresh()
    }

    override suspend fun addPhotosToGroup(groupId: Long, photoIds: List<Long>) = withContext(Dispatchers.IO) {
        if (photoIds.isEmpty()) return@withContext
        val targetGroup = getGroupById(groupId) ?: return@withContext
        val db = dbHelper.getSafeWritableDatabase()
        val inClause = photoIds.joinToString(",") { it.toString() }
        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("group_id", groupId)
                put("folder_id", targetGroup.folderId)
                if (targetGroup.subfolderId != null) {
                    put("subfolder_id", targetGroup.subfolderId)
                } else {
                    putNull("subfolder_id")
                }
            }
            db.update("photos", values, "id IN ($inClause)", null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshSync()
        folderRepository.refresh()
    }

    override suspend fun addPhotosToExistingGroup(groupId: Long, photoIds: List<Long>) = withContext(Dispatchers.IO) {
        if (photoIds.isEmpty()) return@withContext
        val targetGroup = getGroupById(groupId) ?: return@withContext
        val db = dbHelper.getSafeWritableDatabase()
        val inClause = photoIds.joinToString(",") { it.toString() }

        db.beginTransaction()
        try {
            val values = ContentValues().apply {
                put("group_id", groupId)
                put("folder_id", targetGroup.folderId)
                if (targetGroup.subfolderId != null) {
                    put("subfolder_id", targetGroup.subfolderId)
                } else {
                    putNull("subfolder_id")
                }
            }
            db.update("photos", values, "id IN ($inClause)", null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshSync()
        folderRepository.refresh()
    }

    override suspend fun moveGroup(groupId: Long, targetFolderId: Long, targetSubfolderId: Long?) = withContext(Dispatchers.IO) {
        moveGroups(listOf(groupId), targetFolderId, targetSubfolderId)
    }

    override suspend fun moveGroups(
        groupIds: List<Long>,
        targetFolderId: Long,
        targetSubfolderId: Long?
    ) = withContext(Dispatchers.IO) {
        if (groupIds.isEmpty()) return@withContext
        val db = dbHelper.getSafeWritableDatabase()
        val inClause = groupIds.joinToString(",") { it.toString() }
        db.beginTransaction()
        try {
            val gValues = ContentValues().apply {
                put("folder_id", targetFolderId)
                if (targetSubfolderId != null) put("subfolder_id", targetSubfolderId) else putNull("subfolder_id")
            }
            db.update("photo_groups", gValues, "id IN ($inClause)", null)
            val pValues = ContentValues().apply {
                put("folder_id", targetFolderId)
                if (targetSubfolderId != null) put("subfolder_id", targetSubfolderId) else putNull("subfolder_id")
            }
            db.update("photos", pValues, "group_id IN ($inClause)", null)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshSync()
        folderRepository.refresh()
    }

    override suspend fun mergeGroups(
        sourceGroupIds: List<Long>,
        newName: String,
        targetFolderId: Long,
        targetSubfolderId: Long?
    ): Long = withContext(Dispatchers.IO) {
        if (sourceGroupIds.size < 2 || newName.isBlank()) return@withContext -1L
        val db = dbHelper.getSafeWritableDatabase()
        val inClause = sourceGroupIds.joinToString(",") { it.toString() }

        db.beginTransaction()
        val newGroupId: Long
        try {
            var coverPhotoId: Long? = null
            val coverCursor = db.rawQuery(
                "SELECT id FROM photos WHERE group_id IN ($inClause) AND is_trashed = 0 ORDER BY added_at ASC LIMIT 1",
                null
            )
            coverCursor.use {
                if (it.moveToFirst()) coverPhotoId = it.getLong(0)
            }

            val gValues = ContentValues().apply {
                put("folder_id", targetFolderId)
                if (targetSubfolderId != null) put("subfolder_id", targetSubfolderId) else putNull("subfolder_id")
                put("name", newName.trim())
                put("created_at", System.currentTimeMillis())
                if (coverPhotoId != null) put("cover_photo_id", coverPhotoId) else putNull("cover_photo_id")
                put("is_trashed", 0)
            }
            newGroupId = db.insert("photo_groups", null, gValues)

            val pValues = ContentValues().apply {
                put("group_id", newGroupId)
                put("folder_id", targetFolderId)
                if (targetSubfolderId != null) put("subfolder_id", targetSubfolderId) else putNull("subfolder_id")
            }
            db.update("photos", pValues, "group_id IN ($inClause)", null)
            db.delete("photo_groups", "id IN ($inClause)", null)

            for (sgId in sourceGroupIds) {
                removeGridItemFromLinkGroupsInternal(db, sgId)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshSync()
        folderRepository.refresh()
        newGroupId
    }

    override fun getPhotosAddedToday(): Flow<List<Photo>> =
        photosFlow.map { list ->
            val now = System.currentTimeMillis()
            val oneDayMs = 24 * 60 * 60 * 1000L
            val startOfDay = now - (now % oneDayMs)
            list.filter { it.addedAt >= startOfDay }
        }

    override fun getPhotosDueTomorrow(): Flow<List<Photo>> =
        photosFlow.map { list ->
            val now = System.currentTimeMillis()
            val twoDaysMs = 48 * 60 * 60 * 1000L
            list.filter { photo ->
                val deadline = photo.linkedDeadline
                deadline != null && deadline in now..(now + twoDaysMs)
            }
        }

    override fun searchPhotos(query: String): Flow<List<Photo>> =
        photosFlow.map { allPhotos ->
            val trimmed = query.trim()
            if (trimmed.isBlank()) return@map emptyList()

            val matchedIds = mutableSetOf<Long>()
            var ftsSuccess = false

            try {
                val db = dbHelper.getSafeReadableDatabase()
                val clean = trimmed.replace("\"", "").replace("'", "")
                val tokens = clean.split("\\s+".toRegex()).filter { it.isNotBlank() }
                if (tokens.isNotEmpty()) {
                    val ftsQuery = tokens.joinToString(" ") { "$it*" }
                    val cursor = db.rawQuery(
                        "SELECT photo_id FROM photos_fts WHERE photos_fts MATCH ? LIMIT 50",
                        arrayOf(ftsQuery)
                    )
                    cursor.use { c ->
                        while (c.moveToNext()) {
                            matchedIds.add(c.getLong(0))
                        }
                    }
                    ftsSuccess = true
                }
            } catch (_: Exception) {
                ftsSuccess = false
            }

            if (ftsSuccess && matchedIds.isNotEmpty()) {
                allPhotos.filter { matchedIds.contains(it.id) }
            } else {
                // Fallback to substring match on caption, OCR text, user note, and tags
                allPhotos.filter { p ->
                    (p.caption?.contains(trimmed, ignoreCase = true) == true) ||
                    (p.ocrText?.contains(trimmed, ignoreCase = true) == true) ||
                    (p.note?.contains(trimmed, ignoreCase = true) == true) ||
                    (p.tags?.contains(trimmed, ignoreCase = true) == true)
                }
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun addPhoto(photo: Photo): Long = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("file_path", photo.fileUri)
            put("thumbnail_path", photo.thumbnailUri)
            put("folder_id", photo.folderId)
            put("subfolder_id", photo.subfolderId)
            put("created_at", photo.createdAt)
            put("added_at", photo.addedAt)
            put("tag_color", photo.tagColor)
            put("caption", photo.caption)
            put("ocr_text", photo.ocrText)
            put("source", photo.source.name)
            put("linked_deadline", photo.linkedDeadline)
            put("file_size_bytes", photo.fileSizeBytes)
            put("note", photo.note)
            put("group_id", photo.groupId)
            put("tags", photo.tags)
        }

        db.beginTransaction()
        val insertedId: Long
        try {
            insertedId = db.insert("photos", null, values)

            // Index in FTS4 safely
            try {
                val ftsValues = ContentValues().apply {
                    put("photo_id", insertedId)
                    put("folder_name", "")
                    put("subfolder_name", "")
                    put("caption", photo.caption ?: "")
                    put("ocr_text", photo.ocrText ?: "")
                    put("note", photo.note ?: "")
                }
                db.insert("photos_fts", null, ftsValues)
            } catch (_: Exception) {}

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        refreshSync()
        folderRepository.refresh()
        try {
            com.arinara.fotara.widget.PhotoCarouselGlanceWidgetReceiver.triggerUpdate(dbHelper.context)
        } catch (_: Exception) {}
        if (photo.linkedDeadline != null) {
            try {
                com.arinara.fotara.widget.DueTomorrowWidgetProvider.notifyDataChanged(dbHelper.context)
            } catch (_: Exception) {}
        }
        insertedId
    }

    override suspend fun updatePhoto(photo: Photo) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("subfolder_id", photo.subfolderId)
            put("tag_color", photo.tagColor)
            put("caption", photo.caption)
            put("ocr_text", photo.ocrText)
            put("linked_deadline", photo.linkedDeadline)
            put("note", photo.note)
            put("group_id", photo.groupId)
            put("tags", photo.tags)
        }

        db.beginTransaction()
        try {
            db.update("photos", values, "id = ?", arrayOf(photo.id.toString()))
            // Update FTS safely
            try {
                db.execSQL(
                    "UPDATE photos_fts SET caption = ?, ocr_text = ?, note = ? WHERE photo_id = ?",
                    arrayOf(photo.caption ?: "", photo.ocrText ?: "", photo.note ?: "", photo.id.toString())
                )
            } catch (_: Exception) {}
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        refreshSync()
        folderRepository.refresh()
        try {
            com.arinara.fotara.widget.PhotoCarouselGlanceWidgetReceiver.triggerUpdate(dbHelper.context)
            com.arinara.fotara.widget.DueTomorrowWidgetProvider.notifyDataChanged(dbHelper.context)
        } catch (_: Exception) {}
    }

    override suspend fun renamePhoto(id: Long, newCaption: String) = withContext(Dispatchers.IO) {
        val target = photosFlow.value.firstOrNull { it.id == id } ?: return@withContext
        updatePhoto(target.copy(caption = newCaption.trim()))
    }

    override suspend fun updatePhotoNote(id: Long, newNote: String?) = withContext(Dispatchers.IO) {
        val target = photosFlow.value.firstOrNull { it.id == id } ?: return@withContext
        val cleanNote = newNote?.trim()?.ifBlank { null }
        updatePhoto(target.copy(note = cleanNote))
    }

    override suspend fun deletePhotos(ids: List<Long>) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val db = dbHelper.getSafeWritableDatabase()
        val inClause = ids.joinToString(",") { it.toString() }
        val now = System.currentTimeMillis()

        db.beginTransaction()
        try {
            // Check which groups are affected before severing membership
            val affectedGroupIds = mutableSetOf<Long>()
            val groupCheckCursor = db.rawQuery(
                "SELECT DISTINCT group_id FROM photos WHERE id IN ($inClause) AND group_id IS NOT NULL",
                null
            )
            groupCheckCursor.use { gc ->
                while (gc.moveToNext()) {
                    affectedGroupIds.add(gc.getLong(0))
                }
            }

            // Universal rule 2.11: Move to trash immediately removes group membership
            db.execSQL("UPDATE photos SET is_trashed = 1, deleted_at = $now, group_id = NULL WHERE id IN ($inClause)")

            for (id in ids) {
                try {
                    com.arinara.fotara.util.NoteScheduleManager(dbHelper.context).cancelAlarmOnly(com.arinara.fotara.util.ScheduleNoteType.PHOTO, id)
                } catch (_: Exception) {}
            }

            // Check affected groups for auto-dissolve if <= 1 member remains
            for (gId in affectedGroupIds) {
                val memberCursor = db.rawQuery(
                    "SELECT id FROM photos WHERE group_id = ? AND is_trashed = 0",
                    arrayOf(gId.toString())
                )
                val remainingIds = mutableListOf<Long>()
                memberCursor.use { mc ->
                    while (mc.moveToNext()) {
                        remainingIds.add(mc.getLong(0))
                    }
                }
                if (remainingIds.size <= 1) {
                    if (remainingIds.isNotEmpty()) {
                        db.execSQL("UPDATE photos SET group_id = NULL WHERE id = ?", arrayOf(remainingIds.first().toString()))
                    }
                    db.delete("photo_groups", "id = ?", arrayOf(gId.toString()))
                    removeGridItemFromLinkGroupsInternal(db, gId)
                } else {
                    val earliestCursor = db.rawQuery(
                        "SELECT id FROM photos WHERE group_id = ? AND is_trashed = 0 ORDER BY added_at ASC LIMIT 1",
                        arrayOf(gId.toString())
                    )
                    var earliestId: Long? = null
                    earliestCursor.use { ec ->
                        if (ec.moveToFirst()) earliestId = ec.getLong(0)
                    }
                    val values = ContentValues().apply { put("cover_photo_id", earliestId) }
                    db.update("photo_groups", values, "id = ?", arrayOf(gId.toString()))
                }
            }

            for (pId in ids) {
                removeGridItemFromLinkGroupsInternal(db, pId)
            }

            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }

        refreshSync()
        folderRepository.refresh()
    }

    override suspend fun deletePhoto(id: Long) = withContext(Dispatchers.IO) {
        deletePhotos(listOf(id))
    }

    override suspend fun movePhotos(ids: List<Long>, targetFolderId: Long, targetSubfolderId: Long?) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val db = dbHelper.getSafeWritableDatabase()
        val inClause = ids.joinToString(",") { it.toString() }
        val values = ContentValues().apply {
            put("folder_id", targetFolderId)
            if (targetSubfolderId != null) put("subfolder_id", targetSubfolderId) else putNull("subfolder_id")
        }
        db.update("photos", values, "id IN ($inClause)", null)
        refreshSync()
        folderRepository.refresh()
    }

    override suspend fun copyPhoto(
        photoId: Long,
        targetFolderId: Long,
        targetSubfolderId: Long?,
        targetGroupId: Long?
    ): Long = withContext(Dispatchers.IO) {
        val source = getPhotoById(photoId) ?: return@withContext -1L
        val manager = photoStorageManager ?: return@withContext -1L

        val copiedFiles = manager.duplicatePhotoFiles(source.fileUri, source.thumbnailUri)
        val now = System.currentTimeMillis()
        val newPhoto = source.copy(
            id = 0,
            fileUri = copiedFiles.filePath,
            thumbnailUri = copiedFiles.thumbnailPath,
            folderId = targetFolderId,
            subfolderId = targetSubfolderId,
            groupId = targetGroupId,
            createdAt = now,
            addedAt = now,
            fileSizeBytes = copiedFiles.fileSizeBytes
        )
        val newPhotoId = addPhoto(newPhoto)
        if (newPhotoId > 0 && photoDrawingRepository != null) {
            val srcDrawing = photoDrawingRepository.getDrawing(photoId)
            if (srcDrawing != null && srcDrawing.hasStrokes) {
                photoDrawingRepository.saveDrawing(srcDrawing.copy(photoId = newPhotoId, updatedAt = now))
            }
        }
        newPhotoId
    }

    override suspend fun updatePhotosTagColor(ids: List<Long>, colorHex: String?) = withContext(Dispatchers.IO) {
        if (ids.isEmpty()) return@withContext
        val db = dbHelper.getSafeWritableDatabase()
        val inClause = ids.joinToString(",") { it.toString() }
        val values = ContentValues().apply {
            put("tag_color", colorHex)
        }
        db.update("photos", values, "id IN ($inClause)", null)
        refreshSync()
    }

    override fun getTrashedPhotos(): Flow<List<Photo>> = trashedPhotosFlow.asStateFlow()

    override suspend fun restorePhoto(id: Long, targetFolderId: Long?, targetSubfolderId: Long?) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("is_trashed", 0)
            putNull("deleted_at")
            if (targetFolderId != null) {
                put("folder_id", targetFolderId)
                if (targetSubfolderId != null) {
                    put("subfolder_id", targetSubfolderId)
                } else {
                    putNull("subfolder_id")
                }
            }
        }
        db.update("photos", values, "id = ?", arrayOf(id.toString()))
        val restoredPhoto = getPhotoById(id)
        if (restoredPhoto != null) {
            try {
                com.arinara.fotara.util.NoteScheduleManager(dbHelper.context).rearmAlarmIfFuture(restoredPhoto)
            } catch (_: Exception) {}
        }
        refreshSync()
        folderRepository.refresh()
    }

    override suspend fun restoreGroup(id: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        db.beginTransaction()
        try {
            db.execSQL("UPDATE photo_groups SET is_trashed = 0, deleted_at = NULL WHERE id = ?", arrayOf(id.toString()))
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        val group = getGroupById(id)
        if (group != null) {
            try {
                com.arinara.fotara.util.NoteScheduleManager(dbHelper.context).rearmAlarmIfFuture(group)
            } catch (_: Exception) {}
        }
        refreshSync()
        folderRepository.refresh()
    }

    override suspend fun purgePhotoPermanently(id: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val cursor = db.rawQuery("SELECT file_path, thumbnail_path FROM photos WHERE id = ?", arrayOf(id.toString()))
        cursor.use {
            if (it.moveToFirst()) {
                val filePath = it.getString(0)
                val thumbPath = if (it.isNull(1)) null else it.getString(1)
                photoStorageManager?.deletePhotoFiles(filePath, thumbPath)
            }
        }
        try {
            db.execSQL("DELETE FROM photos_fts WHERE photo_id = ?", arrayOf(id.toString()))
        } catch (_: Exception) {}
        try {
            db.execSQL("DELETE FROM photo_drawings WHERE photo_id = ?", arrayOf(id.toString()))
        } catch (_: Exception) {}
        db.delete("photos", "id = ?", arrayOf(id.toString()))
        refreshSync()
        folderRepository.refresh()
    }

    override suspend fun emptyTrash() = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        // 1. Delete all trashed photos' files
        val cursor = db.rawQuery("SELECT file_path, thumbnail_path, id FROM photos WHERE is_trashed = 1", null)
        cursor.use { c ->
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
            db.execSQL("DELETE FROM photos WHERE is_trashed = 1")
            db.execSQL("DELETE FROM photo_groups WHERE is_trashed = 1")
            db.execSQL("DELETE FROM subfolders WHERE is_trashed = 1")
            db.execSQL("DELETE FROM folders WHERE is_trashed = 1")
            try {
                db.execSQL("DELETE FROM photo_drawings WHERE photo_id NOT IN (SELECT id FROM photos)")
                db.execSQL("DELETE FROM pdf_page_pins WHERE document_id NOT IN (SELECT id FROM document_notes)")
                db.execSQL("DELETE FROM pdf_page_drawings WHERE document_id NOT IN (SELECT id FROM document_notes)")
            } catch (_: Exception) {}
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshSync()
        folderRepository.refresh()
    }

    override suspend fun purgeOldTrashedItems(retentionDays: Int) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        val cutoff = System.currentTimeMillis() - (retentionDays.toLong() * 24 * 60 * 60 * 1000L)
        // 1. Delete expired photos' files
        val cursor = db.rawQuery("SELECT file_path, thumbnail_path, id FROM photos WHERE is_trashed = 1 AND deleted_at < $cutoff", null)
        cursor.use { c ->
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
            db.execSQL("DELETE FROM photos WHERE is_trashed = 1 AND deleted_at < $cutoff")
            db.execSQL("DELETE FROM photo_groups WHERE is_trashed = 1 AND deleted_at < $cutoff")
            db.execSQL("DELETE FROM subfolders WHERE is_trashed = 1 AND deleted_at < $cutoff")
            db.execSQL("DELETE FROM folders WHERE is_trashed = 1 AND deleted_at < $cutoff")
            try {
                db.execSQL("DELETE FROM photo_drawings WHERE photo_id NOT IN (SELECT id FROM photos)")
                db.execSQL("DELETE FROM pdf_page_pins WHERE document_id NOT IN (SELECT id FROM document_notes)")
                db.execSQL("DELETE FROM pdf_page_drawings WHERE document_id NOT IN (SELECT id FROM document_notes)")
            } catch (_: Exception) {}
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshSync()
        folderRepository.refresh()
    }

    override suspend fun getAllActivePhotos(): List<Photo> = withContext(Dispatchers.IO) {
        photosFlow.value
    }

    override fun getAllActivePhotosFlow(): Flow<List<Photo>> = photosFlow.asStateFlow()

    override suspend fun rebuildSearchIndex(): Int = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        var count = 0
        db.beginTransaction()
        try {
            db.execSQL("DELETE FROM photos_fts")
            val cursor = db.rawQuery(
                """
                SELECT p.id, f.name, COALESCE(s.name, ''), p.caption, p.ocr_text, p.note
                FROM photos p
                JOIN folders f ON p.folder_id = f.id
                LEFT JOIN subfolders s ON p.subfolder_id = s.id
                WHERE p.is_trashed = 0 AND f.is_trashed = 0
                """.trimIndent(),
                null
            )
            cursor.use { c ->
                while (c.moveToNext()) {
                    val photoId = c.getLong(0)
                    val folderName = c.getString(1) ?: ""
                    val subfolderName = c.getString(2) ?: ""
                    val caption = c.getString(3) ?: ""
                    val ocrText = c.getString(4) ?: ""
                    val note = c.getString(5) ?: ""

                    val values = ContentValues().apply {
                        put("photo_id", photoId)
                        put("folder_name", folderName)
                        put("subfolder_name", subfolderName)
                        put("caption", caption)
                        put("ocr_text", ocrText)
                        put("note", note)
                    }
                    db.insert("photos_fts", null, values)
                    count++
                }
            }
            db.setTransactionSuccessful()
        } catch (e: Exception) {
            android.util.Log.e("SqlitePhotoRepository", "Rebuild FTS index error: ${e.message}", e)
        } finally {
            db.endTransaction()
        }
        count
    }

    override suspend fun rotatePhotoClockwise(id: Long): Photo? = withContext(Dispatchers.IO) {
        val photo = getPhotoById(id) ?: return@withContext null
        val manager = photoStorageManager ?: return@withContext null
        try {
            val drawing = photoDrawingRepository?.getDrawing(id)
            val saved = manager.rotatePhotoClockwise(photo.fileUri, photo.thumbnailUri)
            if (drawing != null && drawing.hasStrokes) {
                photoDrawingRepository.transformRotate(id, drawing.widthPx, drawing.heightPx)
            }
            val db = dbHelper.getSafeWritableDatabase()
            val values = ContentValues().apply {
                put("file_size_bytes", saved.fileSizeBytes)
            }
            db.update("photos", values, "id = ?", arrayOf(id.toString()))
            refreshSync()
            getPhotoById(id)
        } catch (e: Exception) {
            android.util.Log.e("SqlitePhotoRepository", "Failed to rotate photo: ${e.message}", e)
            null
        }
    }

    override suspend fun cropPhoto(
        id: Long,
        left: Float,
        top: Float,
        right: Float,
        bottom: Float
    ): Photo? = withContext(Dispatchers.IO) {
        val photo = getPhotoById(id) ?: return@withContext null
        val manager = photoStorageManager ?: return@withContext null
        try {
            val drawing = photoDrawingRepository?.getDrawing(id)
            val saved = manager.cropPhoto(photo.fileUri, photo.thumbnailUri, left, top, right, bottom)
            if (drawing != null && drawing.hasStrokes) {
                val cropLeft = left * drawing.widthPx
                val cropTop = top * drawing.heightPx
                val newW = ((right - left) * drawing.widthPx).toInt().coerceAtLeast(1)
                val newH = ((bottom - top) * drawing.heightPx).toInt().coerceAtLeast(1)
                photoDrawingRepository.transformCrop(id, cropLeft, cropTop, newW, newH)
            }
            val db = dbHelper.getSafeWritableDatabase()
            val values = ContentValues().apply {
                put("file_size_bytes", saved.fileSizeBytes)
            }
            db.update("photos", values, "id = ?", arrayOf(id.toString()))
            refreshSync()
            getPhotoById(id)
        } catch (e: Exception) {
            android.util.Log.e("SqlitePhotoRepository", "Failed to crop photo: ${e.message}", e)
            null
        }
    }

    override fun getAllSmartTags(): Flow<List<String>> =
        photosFlow.map { photos ->
            val tagCounts = mutableMapOf<String, Int>()
            photos.filter { !it.isTrashed }.forEach { p ->
                p.getAllSmartTags().forEach { tag ->
                    tagCounts[tag] = (tagCounts[tag] ?: 0) + 1
                }
            }
            tagCounts.entries
                .sortedWith(compareByDescending<Map.Entry<String, Int>> { it.value }.thenBy { it.key })
                .map { it.key }
        }.flowOn(Dispatchers.IO)

    override fun getPhotosByTag(tag: String): Flow<List<Photo>> =
        photosFlow.map { photos ->
            val normalized = tag.trim().removePrefix("#").lowercase()
            if (normalized.isBlank()) emptyList()
            else photos.filter { photo ->
                !photo.isTrashed && photo.getAllSmartTags().contains(normalized)
            }
        }.flowOn(Dispatchers.IO)

    override suspend fun addTagToPhoto(id: Long, tag: String) = withContext(Dispatchers.IO) {
        val target = photosFlow.value.firstOrNull { it.id == id && !it.isTrashed } ?: return@withContext
        val cleanTag = tag.trim().removePrefix("#").lowercase()
        if (cleanTag.isBlank()) return@withContext

        val existing = target.tags?.split(',', ' ')?.map { it.trim().removePrefix("#").lowercase() }?.filter { it.isNotBlank() } ?: emptyList()
        if (!existing.contains(cleanTag)) {
            val updated = (existing + cleanTag).joinToString(",")
            updatePhoto(target.copy(tags = updated))
        }
    }

    override suspend fun removeTagFromPhoto(id: Long, tag: String) = withContext(Dispatchers.IO) {
        val target = photosFlow.value.firstOrNull { it.id == id && !it.isTrashed } ?: return@withContext
        val cleanTag = tag.trim().removePrefix("#").lowercase()
        val existing = target.tags?.split(',', ' ')?.map { it.trim().removePrefix("#").lowercase() }?.filter { it.isNotBlank() } ?: emptyList()
        val remaining = existing.filter { it != cleanTag }
        val updated = if (remaining.isEmpty()) null else remaining.joinToString(",")
        updatePhoto(target.copy(tags = updated))
    }

    override fun getGridLinkGroups(): Flow<List<LinkGroup>> = gridLinkGroupsFlow.asStateFlow()

    override suspend fun createGridLinkGroup(memberIds: List<Long>): Long = withContext(Dispatchers.IO) {
        if (memberIds.size < 2) return@withContext -1L
        val cappedIds = memberIds.distinct().take(4)
        val db = dbHelper.getSafeWritableDatabase()
        db.beginTransaction()
        val insertedId: Long
        try {
            for (mId in cappedIds) {
                removeGridItemFromLinkGroupsInternal(db, mId)
            }
            val values = ContentValues().apply {
                put("item_type", LinkItemType.GRID_ITEM.name)
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

    override suspend fun unlinkGridItem(itemId: Long) = withContext(Dispatchers.IO) {
        val db = dbHelper.getSafeWritableDatabase()
        db.beginTransaction()
        try {
            removeGridItemFromLinkGroupsInternal(db, itemId)
            db.setTransactionSuccessful()
        } finally {
            db.endTransaction()
        }
        refreshSync()
    }

    private fun removeGridItemFromLinkGroupsInternal(db: SQLiteDatabase, itemId: Long) {
        try {
            val cursor = db.rawQuery("SELECT id, member_ids FROM link_groups WHERE item_type = 'GRID_ITEM'", null)
            val toUpdate = mutableListOf<Pair<Long, List<Long>>>()
            val toDelete = mutableListOf<Long>()
            cursor.use { c ->
                while (c.moveToNext()) {
                    val lgId = c.getLong(0)
                    val members = c.getString(1).split(",").mapNotNull { it.trim().toLongOrNull() }
                    if (members.contains(itemId) || members.contains(-itemId)) {
                        val rem = members.filter { it != itemId && it != -itemId }
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
}
