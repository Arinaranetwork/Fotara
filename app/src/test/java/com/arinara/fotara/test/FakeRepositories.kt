// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.test

import com.arinara.fotara.data.model.DownsampleQuality
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.ImportResult
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.data.model.SortOrder
import com.arinara.fotara.data.model.StorageBreakdown
import com.arinara.fotara.data.model.StorageLocation
import com.arinara.fotara.data.model.Subfolder
import com.arinara.fotara.data.model.TagColor
import com.arinara.fotara.data.model.ThemeMode
import com.arinara.fotara.data.model.UserSettings
import com.arinara.fotara.data.repository.FolderBulkDeleteResult
import com.arinara.fotara.data.repository.FolderRepository
import com.arinara.fotara.data.repository.PhotoRepository
import com.arinara.fotara.data.repository.SettingsRepository
import com.arinara.fotara.data.repository.SubfolderDeleteResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import java.util.concurrent.atomic.AtomicLong

class FakeFolderRepository(
    initialFolders: List<Folder> = listOf(
        Folder(id = 1L, name = "Biology", colorLabel = TagColor.EMERALD.hex, isPinned = true, photoCount = 2, totalSizeBytes = 2048L),
        Folder(id = 2L, name = "Calculus", colorLabel = TagColor.SKY.hex, isPinned = false, photoCount = 1, totalSizeBytes = 1024L)
    ),
    initialSubfolders: List<Subfolder> = listOf(
        Subfolder(id = 1L, folderId = 1L, name = "Cell Structure"),
        Subfolder(id = 2L, folderId = 1L, name = "Genetics"),
        Subfolder(id = 3L, folderId = 2L, name = "Derivatives")
    )
) : FolderRepository {

    private val foldersFlow = MutableStateFlow(initialFolders)
    private val subfoldersFlow = MutableStateFlow(initialSubfolders)
    private val nextFolderId = AtomicLong(100L)
    private val nextSubfolderId = AtomicLong(100L)

    override fun getFolders(): Flow<List<Folder>> = foldersFlow.asStateFlow()

    override fun getFolderById(id: Long): Flow<Folder?> =
        foldersFlow.map { list -> list.firstOrNull { it.id == id } }

    override suspend fun createFolder(name: String, colorLabel: String, isPinned: Boolean): Long {
        val id = nextFolderId.incrementAndGet()
        val newFolder = Folder(
            id = id,
            name = name,
            colorLabel = colorLabel,
            isPinned = isPinned,
            createdAt = System.currentTimeMillis()
        )
        val current = foldersFlow.value.toMutableList()
        if (isPinned) {
            current.add(0, newFolder)
        } else {
            current.add(newFolder)
        }
        foldersFlow.value = current
        return id
    }

    override suspend fun updateFolder(folder: Folder) {
        val current = foldersFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == folder.id }
        if (index != -1) {
            current[index] = folder
            foldersFlow.value = current
        }
    }

    override suspend fun renameFolder(id: Long, newName: String) {
        val current = foldersFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(name = newName)
            foldersFlow.value = current
        }
    }

    override suspend fun updateFolderColor(id: Long, colorHex: String) {
        val current = foldersFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(colorLabel = colorHex)
            foldersFlow.value = current
        }
    }

    override suspend fun togglePin(id: Long) {
        val current = foldersFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            val updated = current[index].copy(isPinned = !current[index].isPinned)
            current.removeAt(index)
            if (updated.isPinned) {
                current.add(0, updated)
            } else {
                current.add(updated)
            }
            foldersFlow.value = current
        }
    }

    override suspend fun deleteFolder(id: Long) {
        foldersFlow.value = foldersFlow.value.filterNot { it.id == id }
        subfoldersFlow.value = subfoldersFlow.value.filterNot { it.folderId == id }
    }

    override suspend fun getBulkDeleteStats(folderIds: List<Long>): FolderBulkDeleteResult {
        val matched = foldersFlow.value.filter { it.id in folderIds }
        val count = matched.size
        val photos = matched.sumOf { it.photoCount }
        val bytes = matched.sumOf { it.totalSizeBytes }
        return FolderBulkDeleteResult(folderCount = count, photoCount = photos, totalSizeBytes = bytes)
    }

    override suspend fun deleteFolders(folderIds: List<Long>): FolderBulkDeleteResult {
        val stats = getBulkDeleteStats(folderIds)
        val now = System.currentTimeMillis()
        val toTrash = foldersFlow.value.filter { it.id in folderIds }.map { it.copy(isTrashed = true, deletedAt = now) }
        foldersFlow.value = foldersFlow.value.filterNot { it.id in folderIds }
        trashedFoldersFlow.value = trashedFoldersFlow.value + toTrash
        subfoldersFlow.value = subfoldersFlow.value.filterNot { it.folderId in folderIds }
        return stats
    }

    override fun getSubfolders(folderId: Long): Flow<List<Subfolder>> =
        subfoldersFlow.map { list -> list.filter { it.folderId == folderId } }

    override suspend fun createSubfolder(folderId: Long, name: String, colorLabel: String?): Long {
        val id = nextSubfolderId.incrementAndGet()
        val sub = Subfolder(id = id, folderId = folderId, name = name, colorLabel = colorLabel)
        subfoldersFlow.value = subfoldersFlow.value + sub
        return id
    }

    override suspend fun renameSubfolder(id: Long, newName: String) {
        val current = subfoldersFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(name = newName)
            subfoldersFlow.value = current
        }
    }

    override suspend fun getSubfolderDeleteStats(subfolderIds: List<Long>): SubfolderDeleteResult {
        return SubfolderDeleteResult(
            subfolderCount = subfolderIds.size,
            photoCount = 0,
            totalSizeBytes = 0L
        )
    }

    override suspend fun deleteSubfolders(subfolderIds: List<Long>): SubfolderDeleteResult {
        val stats = getSubfolderDeleteStats(subfolderIds)
        subfoldersFlow.value = subfoldersFlow.value.filterNot { it.id in subfolderIds }
        return stats
    }

    override suspend fun deleteSubfolder(id: Long) {
        deleteSubfolders(listOf(id))
    }

    private val trashedFoldersFlow = MutableStateFlow<List<Folder>>(emptyList())

    override fun getTrashedFolders(): Flow<List<Folder>> = trashedFoldersFlow.asStateFlow()

    override suspend fun restoreFolder(id: Long) {
        val trashed = trashedFoldersFlow.value.firstOrNull { it.id == id }
        if (trashed != null) {
            trashedFoldersFlow.value = trashedFoldersFlow.value.filterNot { it.id == id }
            foldersFlow.value = foldersFlow.value + trashed.copy(isTrashed = false, deletedAt = null)
        }
    }

    override suspend fun purgeFolderPermanently(id: Long) {
        trashedFoldersFlow.value = trashedFoldersFlow.value.filterNot { it.id == id }
        foldersFlow.value = foldersFlow.value.filterNot { it.id == id }
    }

    override suspend fun isFolderTrashed(id: Long): Boolean {
        return trashedFoldersFlow.value.any { it.id == id } || !foldersFlow.value.any { it.id == id }
    }

    fun incrementPhotoCount(folderId: Long) {
        val current = foldersFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == folderId }
        if (index != -1) {
            current[index] = current[index].copy(photoCount = current[index].photoCount + 1)
            foldersFlow.value = current
        }
    }

    override suspend fun lockFolder(id: Long, pin: String) {
        val current = foldersFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(isLocked = true, lockPin = pin)
            foldersFlow.value = current
        }
    }

    override suspend fun unlockFolder(id: Long) {
        val current = foldersFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(isLocked = false, lockPin = null)
            foldersFlow.value = current
        }
    }

    override suspend fun updateFolderPin(id: Long, newPin: String) {
        val current = foldersFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(lockPin = newPin)
            foldersFlow.value = current
        }
    }

    override suspend fun refresh() {}
}

class FakePhotoRepository(
    private val folderRepository: FolderRepository? = null,
    initialPhotos: List<Photo> = listOf(
        Photo(
            id = 1L,
            fileUri = "file:///data/photo1.jpg",
            thumbnailUri = "file:///data/photo1_thumb.jpg",
            folderId = 1L,
            subfolderId = 1L,
            caption = "Cell division under microscope",
            ocrText = "Mitosis prophase metaphase anaphase telophase mitochondria",
            tagColor = TagColor.EMERALD.hex,
            fileSizeBytes = 1024L
        ),
        Photo(
            id = 2L,
            fileUri = "file:///data/photo2.jpg",
            thumbnailUri = "file:///data/photo2_thumb.jpg",
            folderId = 1L,
            subfolderId = 2L,
            caption = "DNA replication fork",
            ocrText = "Helicase polymerase okazaki fragments adenine thymine guanine cytosine",
            tagColor = TagColor.SKY.hex,
            fileSizeBytes = 1024L
        ),
        Photo(
            id = 3L,
            fileUri = "file:///data/photo3.jpg",
            thumbnailUri = "file:///data/photo3_thumb.jpg",
            folderId = 2L,
            subfolderId = 3L,
            caption = "Chain rule derivative proof",
            ocrText = "Derivative dy/dx dy/du du/dx chain rule limits",
            tagColor = TagColor.AMBER.hex,
            fileSizeBytes = 1024L
        )
    )
) : PhotoRepository {

    private val photosFlow = MutableStateFlow(initialPhotos)
    private val nextPhotoId = AtomicLong(100L)

    override fun getPhotosByFolder(folderId: Long, subfolderId: Long?): Flow<List<Photo>> =
        photosFlow.map { list ->
            list.filter { photo ->
                photo.folderId == folderId && (subfolderId == null || photo.subfolderId == subfolderId)
            }
        }

    override fun getPhotosAddedToday(): Flow<List<Photo>> =
        photosFlow.map { list -> list.take(5) }

    override fun getPhotosDueTomorrow(): Flow<List<Photo>> =
        photosFlow.map { list -> list.filter { it.linkedDeadline != null } }

    override fun searchPhotos(query: String): Flow<List<Photo>> =
        photosFlow.map { list ->
            if (query.isBlank()) emptyList()
            else list.filter { photo ->
                photo.ocrText?.contains(query, ignoreCase = true) == true ||
                photo.caption?.contains(query, ignoreCase = true) == true ||
                photo.note?.contains(query, ignoreCase = true) == true
            }
        }

    override suspend fun addPhoto(photo: Photo): Long {
        val id = nextPhotoId.incrementAndGet()
        val saved = photo.copy(id = id)
        photosFlow.value = photosFlow.value + saved
        (folderRepository as? FakeFolderRepository)?.incrementPhotoCount(photo.folderId)
        return id
    }

    override suspend fun updatePhoto(photo: Photo) {
        val current = photosFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == photo.id }
        if (index != -1) {
            current[index] = photo
            photosFlow.value = current
        }
    }

    override suspend fun renamePhoto(id: Long, newCaption: String) {
        val current = photosFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(caption = newCaption.trim())
            photosFlow.value = current
        }
    }

    override suspend fun updatePhotoNote(id: Long, newNote: String?) {
        val current = photosFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == id }
        if (index != -1) {
            current[index] = current[index].copy(note = newNote?.trim()?.ifBlank { null })
            photosFlow.value = current
        }
    }

    override suspend fun deletePhoto(id: Long) {
        deletePhotos(listOf(id))
    }

    override suspend fun deletePhotos(ids: List<Long>) {
        val now = System.currentTimeMillis()
        val toTrash = photosFlow.value.filter { it.id in ids }.map { it.copy(isTrashed = true, deletedAt = now) }
        photosFlow.value = photosFlow.value.filterNot { it.id in ids }
        trashedPhotosFlow.value = trashedPhotosFlow.value + toTrash
    }

    override suspend fun movePhotos(ids: List<Long>, targetFolderId: Long, targetSubfolderId: Long?) {
        val current = photosFlow.value.map { photo ->
            if (photo.id in ids) {
                photo.copy(folderId = targetFolderId, subfolderId = targetSubfolderId)
            } else {
                photo
            }
        }
        photosFlow.value = current
    }

    override suspend fun updatePhotosTagColor(ids: List<Long>, colorHex: String?) {
        val current = photosFlow.value.map { photo ->
            if (photo.id in ids) {
                photo.copy(tagColor = colorHex)
            } else {
                photo
            }
        }
        photosFlow.value = current
    }

    override suspend fun getPhotoById(id: Long): Photo? =
        photosFlow.value.firstOrNull { it.id == id && !it.isTrashed }

    private val trashedPhotosFlow = MutableStateFlow<List<Photo>>(emptyList())

    override fun getTrashedPhotos(): Flow<List<Photo>> = trashedPhotosFlow.asStateFlow()

    override suspend fun restorePhoto(id: Long, targetFolderId: Long?, targetSubfolderId: Long?) {
        val trashed = trashedPhotosFlow.value.firstOrNull { it.id == id }
        if (trashed != null) {
            trashedPhotosFlow.value = trashedPhotosFlow.value.filterNot { it.id == id }
            val restored = trashed.copy(
                isTrashed = false,
                deletedAt = null,
                folderId = targetFolderId ?: trashed.folderId,
                subfolderId = if (targetFolderId != null) targetSubfolderId else trashed.subfolderId
            )
            photosFlow.value = photosFlow.value + restored
        }
    }

    override suspend fun purgePhotoPermanently(id: Long) {
        trashedPhotosFlow.value = trashedPhotosFlow.value.filterNot { it.id == id }
        photosFlow.value = photosFlow.value.filterNot { it.id == id }
    }

    override suspend fun emptyTrash() {
        trashedPhotosFlow.value = emptyList()
    }

    override suspend fun purgeOldTrashedItems(retentionDays: Int) {
        val cutoff = System.currentTimeMillis() - retentionDays * 24 * 60 * 60 * 1000L
        trashedPhotosFlow.value = trashedPhotosFlow.value.filterNot { (it.deletedAt ?: 0L) < cutoff }
    }

    override suspend fun getAllActivePhotos(): List<Photo> = photosFlow.value

    override suspend fun rebuildSearchIndex(): Int = photosFlow.value.size

    override suspend fun rotatePhotoClockwise(id: Long): Photo? {
        val photo = getPhotoById(id) ?: return null
        val updated = photo.copy(addedAt = System.currentTimeMillis())
        val current = photosFlow.value.map { if (it.id == id) updated else it }
        photosFlow.value = current
        return updated
    }

    override suspend fun cropPhoto(id: Long, left: Float, top: Float, right: Float, bottom: Float): Photo? {
        val photo = getPhotoById(id) ?: return null
        val updated = photo.copy(addedAt = System.currentTimeMillis())
        val current = photosFlow.value.map { if (it.id == id) updated else it }
        photosFlow.value = current
        return updated
    }

    private val groupsFlow = MutableStateFlow<List<PhotoGroup>>(emptyList())
    private var nextGroupId = 100L

    override fun getGroupsByFolder(folderId: Long, subfolderId: Long?): Flow<List<PhotoGroup>> =
        groupsFlow.map { list ->
            list.filter { it.folderId == folderId && (subfolderId == null || it.subfolderId == subfolderId) && !it.isTrashed }
        }

    override fun getAllActiveGroups(): Flow<List<PhotoGroup>> =
        groupsFlow.map { list -> list.filter { !it.isTrashed } }

    override fun searchGroups(query: String): Flow<List<PhotoGroup>> =
        groupsFlow.map { list -> list.filter { !it.isTrashed && it.name.contains(query, ignoreCase = true) } }

    override suspend fun getGroupById(groupId: Long): PhotoGroup? =
        groupsFlow.value.firstOrNull { it.id == groupId }

    override suspend fun createGroup(
        folderId: Long,
        subfolderId: Long?,
        name: String,
        photoIds: List<Long>,
        tagColor: String?
    ): Long {
        val gId = nextGroupId++
        val selectedPhotos = photosFlow.value.filter { it.id in photoIds }
        val earliestPhoto = selectedPhotos.minByOrNull { it.addedAt } ?: selectedPhotos.firstOrNull()
        val newGroup = PhotoGroup(
            id = gId,
            folderId = folderId,
            subfolderId = subfolderId,
            name = name.trim(),
            tagColor = tagColor,
            createdAt = System.currentTimeMillis(),
            coverPhotoId = earliestPhoto?.id
        )
        groupsFlow.value = groupsFlow.value + newGroup
        photosFlow.value = photosFlow.value.map {
            if (it.id in photoIds) it.copy(groupId = gId) else it
        }
        return gId
    }

    override suspend fun renameGroup(groupId: Long, newName: String) {
        groupsFlow.value = groupsFlow.value.map {
            if (it.id == groupId) it.copy(name = newName.trim()) else it
        }
    }

    override suspend fun updateGroupTagColor(groupId: Long, colorHex: String?) {
        groupsFlow.value = groupsFlow.value.map {
            if (it.id == groupId) it.copy(tagColor = colorHex) else it
        }
    }

    override suspend fun ungroup(groupId: Long) {
        photosFlow.value = photosFlow.value.map {
            if (it.groupId == groupId) it.copy(groupId = null) else it
        }
        groupsFlow.value = groupsFlow.value.filterNot { it.id == groupId }
    }

    override suspend fun deleteGroup(groupId: Long) {
        deleteGroups(listOf(groupId))
    }

    override suspend fun deleteGroups(groupIds: List<Long>) {
        val now = System.currentTimeMillis()
        groupsFlow.value = groupsFlow.value.map {
            if (it.id in groupIds) it.copy(isTrashed = true, deletedAt = now) else it
        }
        val memberPhotoIds = photosFlow.value.filter { it.groupId in groupIds }.map { it.id }
        if (memberPhotoIds.isNotEmpty()) {
            deletePhotos(memberPhotoIds)
        }
    }

    override suspend fun removePhotoFromGroup(photoId: Long) {
        val photo = getPhotoById(photoId) ?: return
        val gId = photo.groupId ?: return

        photosFlow.value = photosFlow.value.map {
            if (it.id == photoId) it.copy(groupId = null) else it
        }

        val remaining = photosFlow.value.filter { it.groupId == gId && !it.isTrashed }
        if (remaining.size <= 1) {
            photosFlow.value = photosFlow.value.map {
                if (it.groupId == gId) it.copy(groupId = null) else it
            }
            groupsFlow.value = groupsFlow.value.filterNot { it.id == gId }
        } else {
            val group = getGroupById(gId)
            if (group?.coverPhotoId == photoId) {
                val newCover = remaining.minByOrNull { it.addedAt } ?: remaining.first()
                groupsFlow.value = groupsFlow.value.map {
                    if (it.id == gId) it.copy(coverPhotoId = newCover.id) else it
                }
            }
        }
    }

    override suspend fun addPhotosToGroup(groupId: Long, photoIds: List<Long>) {
        photosFlow.value = photosFlow.value.map {
            if (it.id in photoIds) it.copy(groupId = groupId) else it
        }
    }

    override suspend fun moveGroups(groupIds: List<Long>, targetFolderId: Long, targetSubfolderId: Long?) {
        groupsFlow.value = groupsFlow.value.map {
            if (it.id in groupIds) it.copy(folderId = targetFolderId, subfolderId = targetSubfolderId) else it
        }
        val memberPhotoIds = photosFlow.value.filter { it.groupId in groupIds }.map { it.id }
        if (memberPhotoIds.isNotEmpty()) {
            movePhotos(memberPhotoIds, targetFolderId, targetSubfolderId)
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
        }

    override fun getPhotosByTag(tag: String): Flow<List<Photo>> =
        photosFlow.map { photos ->
            val normalized = tag.trim().removePrefix("#").lowercase()
            if (normalized.isBlank()) emptyList()
            else photos.filter { photo ->
                !photo.isTrashed && photo.getAllSmartTags().contains(normalized)
            }
        }

    override suspend fun addTagToPhoto(id: Long, tag: String) {
        val target = photosFlow.value.firstOrNull { it.id == id && !it.isTrashed } ?: return
        val cleanTag = tag.trim().removePrefix("#").lowercase()
        if (cleanTag.isBlank()) return

        val existing = target.tags?.split(',', ' ')?.map { it.trim().removePrefix("#").lowercase() }?.filter { it.isNotBlank() } ?: emptyList()
        if (!existing.contains(cleanTag)) {
            val updated = (existing + cleanTag).joinToString(",")
            photosFlow.value = photosFlow.value.map {
                if (it.id == id) it.copy(tags = updated) else it
            }
        }
    }

    override suspend fun removeTagFromPhoto(id: Long, tag: String) {
        val target = photosFlow.value.firstOrNull { it.id == id && !it.isTrashed } ?: return
        val cleanTag = tag.trim().removePrefix("#").lowercase()
        val existing = target.tags?.split(',', ' ')?.map { it.trim().removePrefix("#").lowercase() }?.filter { it.isNotBlank() } ?: emptyList()
        val remaining = existing.filter { it != cleanTag }
        val updated = if (remaining.isEmpty()) null else remaining.joinToString(",")
        photosFlow.value = photosFlow.value.map {
            if (it.id == id) it.copy(tags = updated) else it
        }
    }

    override suspend fun refresh() {}
}

class FakeSettingsRepository : SettingsRepository {
    private val _settingsFlow = MutableStateFlow(UserSettings())
    override val settingsFlow: StateFlow<UserSettings> = _settingsFlow.asStateFlow()

    var storageBreakdown = StorageBreakdown(
        photosSizeBytes = 1048576L,
        thumbnailsSizeBytes = 204800L,
        databaseSizeBytes = 51200L
    )

    var rebuildThumbnailsCount = 5
    var rebuildSearchIndexCount = 10
    var exportedJson = "{\"app\":\"Fotara\",\"folders\":[],\"photos\":[]}"
    var importResult = ImportResult(success = true, foldersImported = 2, photosImported = 4, message = "Import success")
    var onboardingCompleted = false
    private val recentSearches = mutableListOf<String>()

    override suspend fun updateSortOrder(sortOrder: SortOrder) {
        _settingsFlow.value = _settingsFlow.value.copy(defaultSortOrder = sortOrder)
    }

    override suspend fun updateGridDensity(density: Int) {
        _settingsFlow.value = _settingsFlow.value.copy(gridDensity = density.coerceIn(2, 4))
    }

    override suspend fun updateThemeMode(themeMode: ThemeMode) {
        _settingsFlow.value = _settingsFlow.value.copy(themeMode = themeMode)
    }

    override suspend fun updateAutoOcr(enabled: Boolean) {
        _settingsFlow.value = _settingsFlow.value.copy(autoOcrEnabled = enabled)
    }

    override suspend fun updateOcrLanguage(language: String) {
        _settingsFlow.value = _settingsFlow.value.copy(ocrLanguage = language)
    }

    override suspend fun updateDownsampleQuality(quality: DownsampleQuality) {
        _settingsFlow.value = _settingsFlow.value.copy(downsampleQuality = quality)
    }

    override suspend fun updateReminderLeadTime(hours: Int) {
        _settingsFlow.value = _settingsFlow.value.copy(reminderLeadTimeHours = hours)
    }

    override suspend fun updateDueTomorrowRibbon(enabled: Boolean) {
        _settingsFlow.value = _settingsFlow.value.copy(dueTomorrowRibbonEnabled = enabled)
    }

    override suspend fun updateStorageLocation(location: StorageLocation) {
        _settingsFlow.value = _settingsFlow.value.copy(storageLocation = location)
    }

    override suspend fun getStorageBreakdown(): StorageBreakdown = storageBreakdown

    override suspend fun rebuildThumbnails(): Int = rebuildThumbnailsCount

    override suspend fun rebuildSearchIndex(): Int = rebuildSearchIndexCount

    override suspend fun exportDataBackup(): String = exportedJson

    override suspend fun importDataBackup(jsonString: String): ImportResult = importResult

    override fun isOnboardingCompleted(): Boolean = onboardingCompleted

    override suspend fun setOnboardingCompleted(completed: Boolean) {
        onboardingCompleted = completed
    }

    override fun getRecentSearches(): List<String> = recentSearches.toList()

    override suspend fun addRecentSearch(query: String) {
        val trimmed = query.trim()
        if (trimmed.isBlank()) return
        recentSearches.removeAll { it.equals(trimmed, ignoreCase = true) }
        recentSearches.add(0, trimmed)
        while (recentSearches.size > 10) {
            recentSearches.removeLast()
        }
    }

    override suspend fun removeRecentSearch(query: String) {
        val trimmed = query.trim()
        recentSearches.removeAll { it.equals(trimmed, ignoreCase = true) }
    }

    override suspend fun clearRecentSearches() {
        recentSearches.clear()
    }
}

