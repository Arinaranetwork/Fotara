// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.folder

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoSource
import com.arinara.fotara.data.repository.FolderRepository
import com.arinara.fotara.data.repository.PhotoRepository
import com.arinara.fotara.data.repository.SettingsRepository
import com.arinara.fotara.data.repository.SubfolderDeleteResult
import com.arinara.fotara.data.storage.PhotoStorageManager
import com.arinara.fotara.ocr.FolderSuggestEngine
import com.arinara.fotara.ocr.OcrEngine
import com.arinara.fotara.util.DeadlineNotificationManager
import com.arinara.fotara.util.PdfExporter
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

class FolderDetailViewModel(
    val folderId: Long,
    private val folderRepository: FolderRepository,
    private val photoRepository: PhotoRepository,
    val photoStorageManager: PhotoStorageManager? = null,
    val ocrEngine: OcrEngine? = null,
    val folderSuggestEngine: FolderSuggestEngine? = null,
    val deadlineNotificationManager: DeadlineNotificationManager? = null,
    val initialSubfolderId: Long? = null,
    val targetPhotoId: Long? = null,
    val targetGroupId: Long? = null,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        FolderDetailUiState(
            selectedSubfolderId = initialSubfolderId,
            highlightedPhotoId = targetPhotoId,
            highlightedGroupId = targetGroupId
        )
    )
    val uiState: StateFlow<FolderDetailUiState> = _uiState.asStateFlow()

    init {
        loadFolderData()
    }

    private fun loadFolderData() {
        viewModelScope.launch {
            folderRepository.getFolderById(folderId).collect { folder ->
                _uiState.update { it.copy(folder = folder) }
            }
        }

        viewModelScope.launch {
            folderRepository.getSubfolders(folderId).collect { subfolders ->
                _uiState.update { it.copy(subfolders = subfolders) }
            }
        }

        viewModelScope.launch {
            folderRepository.getFolders().collect { folders ->
                _uiState.update { it.copy(availableFolders = folders) }
            }
        }

        settingsRepository?.let { repo ->
            viewModelScope.launch {
                repo.settingsFlow.collect { settings ->
                    _uiState.update { it.copy(gridDensity = settings.gridDensity) }
                }
            }
        }

        observePhotos()
    }

    private fun observePhotos() {
        viewModelScope.launch {
            combine(
                _uiState,
                photoRepository.getPhotosByFolder(folderId),
                photoRepository.getGroupsByFolder(folderId)
            ) { state, allPhotos, allGroups ->
                val currentSubfolder = state.selectedSubfolderId
                val filteredPhotos = if (currentSubfolder == null) allPhotos else allPhotos.filter { it.subfolderId == currentSubfolder }
                val filteredGroups = if (currentSubfolder == null) allGroups else allGroups.filter { it.subfolderId == currentSubfolder }

                val standalonePhotos = filteredPhotos.filter { it.groupId == null }
                val standaloneItems = standalonePhotos.map { FolderGridItem.StandalonePhoto(it) }

                val groupItems = filteredGroups.map { group ->
                    val members = allPhotos.filter { it.groupId == group.id }
                    val cover = allPhotos.firstOrNull { it.id == group.coverPhotoId }
                        ?: members.minByOrNull { it.addedAt }
                        ?: members.firstOrNull()
                    FolderGridItem.Group(
                        group = group,
                        memberPhotos = members,
                        coverPhoto = cover
                    )
                }

                val allItems = standaloneItems + groupItems

                val sortedItems = when (state.sortOption) {
                    PhotoSortOption.UPLOAD_DATE_DESC -> allItems.sortedByDescending { it.sortCreatedAt }
                    PhotoSortOption.UPLOAD_DATE_ASC -> allItems.sortedBy { it.sortCreatedAt }
                    PhotoSortOption.NEAREST_DEADLINE -> allItems.sortedBy { it.sortDeadline ?: Long.MAX_VALUE }
                    PhotoSortOption.COLOR_LABEL -> allItems.sortedBy { it.sortColor ?: "ZZZ" }
                }

                val sortedPhotos = when (state.sortOption) {
                    PhotoSortOption.UPLOAD_DATE_DESC -> filteredPhotos.sortedByDescending { it.addedAt }
                    PhotoSortOption.UPLOAD_DATE_ASC -> filteredPhotos.sortedBy { it.addedAt }
                    PhotoSortOption.NEAREST_DEADLINE -> filteredPhotos.sortedBy { it.linkedDeadline ?: Long.MAX_VALUE }
                    PhotoSortOption.COLOR_LABEL -> filteredPhotos.sortedBy { it.tagColor ?: "ZZZ" }
                }

                Triple(sortedPhotos, filteredGroups, sortedItems)
            }.collect { (sortedPhotos, filteredGroups, sortedItems) ->
                _uiState.update {
                    it.copy(
                        photos = sortedPhotos,
                        groups = filteredGroups,
                        gridItems = sortedItems
                    )
                }
            }
        }
    }

    fun selectSubfolder(subfolderId: Long?) {
        _uiState.update { it.copy(selectedSubfolderId = subfolderId) }
    }

    fun setSortOption(option: PhotoSortOption) {
        _uiState.update { it.copy(sortOption = option) }
    }

    fun openAddSubfolderDialog() {
        _uiState.update { it.copy(showAddSubfolderDialog = true) }
    }

    fun closeAddSubfolderDialog() {
        _uiState.update { it.copy(showAddSubfolderDialog = false) }
    }

    fun createSubfolder(name: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            folderRepository.createSubfolder(folderId, name.trim())
            _uiState.update {
                it.copy(
                    showAddSubfolderDialog = false,
                    userMessage = "Subfolder \"$name\" created"
                )
            }
        }
    }

    fun renameFolder(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(userMessage = "Folder name cannot be empty") }
            return
        }
        viewModelScope.launch {
            folderRepository.renameFolder(folderId, trimmed)
            _uiState.update { it.copy(userMessage = "Renamed to \"$trimmed\"") }
        }
    }

    fun renameSubfolder(subfolderId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(userMessage = "Subfolder name cannot be empty") }
            return
        }
        viewModelScope.launch {
            folderRepository.renameSubfolder(subfolderId, trimmed)
            _uiState.update { it.copy(userMessage = "Subfolder renamed to \"$trimmed\"") }
        }
    }

    fun updateFolderColor(colorHex: String) {
        viewModelScope.launch {
            folderRepository.updateFolderColor(folderId, colorHex)
            _uiState.update { it.copy(userMessage = "Folder label color updated") }
        }
    }

    fun deleteFolder(onDeleted: () -> Unit) {
        viewModelScope.launch {
            folderRepository.deleteFolder(folderId)
            onDeleted()
        }
    }

    suspend fun importGalleryUris(uris: List<Uri>): List<Photo> {
        val results = mutableListOf<Photo>()
        for (uri in uris) {
            try {
                val saved = photoStorageManager?.saveUriAsPhoto(uri) ?: continue
                val ocr = ocrEngine?.extractText(saved.filePath)
                val nextIndex = _uiState.value.photos.size + results.size + 1
                val newPhoto = Photo(
                    fileUri = saved.filePath,
                    thumbnailUri = saved.thumbnailPath,
                    folderId = folderId,
                    caption = "Imported Note #$nextIndex",
                    ocrText = ocr?.fullText,
                    source = PhotoSource.IMPORT,
                    fileSizeBytes = saved.fileSizeBytes
                )
                results.add(newPhoto)
            } catch (_: Exception) {}
        }
        return results
    }

    fun exportToPdf(context: Context, onReady: (File) -> Unit) {
        val gridItems = _uiState.value.gridItems
        val folderName = _uiState.value.folder?.name ?: "Folder"
        if (gridItems.isEmpty()) {
            _uiState.update { it.copy(userMessage = "No photos to export") }
            return
        }
        viewModelScope.launch {
            try {
                val pdfFile = PdfExporter.exportFolderGridToPdf(context, folderName, gridItems)
                onReady(pdfFile)
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Failed to export PDF: ${e.message}") }
            }
        }
    }

    fun saveCapturedBatch(photos: List<Photo>, targetSubfolderId: Long?) {
        viewModelScope.launch {
            photos.forEach { photo ->
                val toSave = photo.copy(
                    folderId = folderId,
                    subfolderId = targetSubfolderId ?: photo.subfolderId
                )
                photoRepository.addPhoto(toSave)
            }
            _uiState.update {
                it.copy(userMessage = "Saved ${photos.size} note${if (photos.size > 1) "s" else ""} to folder")
            }
        }
    }

    fun toggleBatchSelectMode() {
        _uiState.update {
            val newMode = !it.isBatchSelectMode
            it.copy(
                isBatchSelectMode = newMode,
                selectedPhotoIds = emptySet(),
                selectedGroupIds = emptySet(),
                isSubfolderMultiSelectMode = if (newMode) false else it.isSubfolderMultiSelectMode,
                selectedSubfolderIds = if (newMode) emptySet() else it.selectedSubfolderIds
            )
        }
    }

    fun exitBatchSelectMode() {
        _uiState.update {
            it.copy(
                isBatchSelectMode = false,
                selectedPhotoIds = emptySet(),
                selectedGroupIds = emptySet()
            )
        }
    }

    fun selectAllPhotos() {
        _uiState.update {
            it.copy(
                selectedPhotoIds = it.photos.filter { p -> p.groupId == null }.map { p -> p.id }.toSet(),
                selectedGroupIds = it.groups.map { g -> g.id }.toSet()
            )
        }
    }

    fun togglePhotoSelection(photoId: Long) {
        _uiState.update { current ->
            val set = current.selectedPhotoIds.toMutableSet()
            if (set.contains(photoId)) set.remove(photoId) else set.add(photoId)
            current.copy(selectedPhotoIds = set)
        }
    }

    fun toggleGroupSelection(groupId: Long) {
        _uiState.update { current ->
            val set = current.selectedGroupIds.toMutableSet()
            if (set.contains(groupId)) set.remove(groupId) else set.add(groupId)
            current.copy(selectedGroupIds = set)
        }
    }

    fun startBatchSelection(photoId: Long) {
        _uiState.update {
            it.copy(
                isBatchSelectMode = true,
                selectedPhotoIds = setOf(photoId),
                selectedGroupIds = emptySet(),
                isSubfolderMultiSelectMode = false,
                selectedSubfolderIds = emptySet()
            )
        }
    }

    fun startBatchSelectionWithGroup(groupId: Long) {
        _uiState.update {
            it.copy(
                isBatchSelectMode = true,
                selectedGroupIds = setOf(groupId),
                selectedPhotoIds = emptySet(),
                isSubfolderMultiSelectMode = false,
                selectedSubfolderIds = emptySet()
            )
        }
    }

    fun createGroupFromSelected(name: String) {
        val photoIds = _uiState.value.selectedPhotoIds.toList()
        if (photoIds.size < 2 || name.trim().isBlank()) return
        viewModelScope.launch {
            photoRepository.createGroup(
                folderId = folderId,
                subfolderId = _uiState.value.selectedSubfolderId,
                name = name.trim(),
                photoIds = photoIds
            )
            exitBatchSelectMode()
            _uiState.update { it.copy(userMessage = "Group created: $name") }
        }
    }

    fun renameGroup(groupId: Long, newName: String) {
        if (newName.trim().isBlank()) return
        viewModelScope.launch {
            photoRepository.renameGroup(groupId, newName.trim())
            _uiState.update { it.copy(userMessage = "Group renamed") }
        }
    }

    fun updateGroupTagColor(groupId: Long, colorHex: String?) {
        viewModelScope.launch {
            photoRepository.updateGroupTagColor(groupId, colorHex)
            _uiState.update { it.copy(userMessage = "Updated group color") }
        }
    }

    fun ungroup(groupId: Long) {
        viewModelScope.launch {
            photoRepository.ungroup(groupId)
            _uiState.update { it.copy(userMessage = "Group dissolved") }
        }
    }

    fun deleteGroup(groupId: Long) {
        viewModelScope.launch {
            photoRepository.deleteGroup(groupId)
            _uiState.update { it.copy(userMessage = "Group moved to Trash") }
        }
    }

    fun removePhotoFromGroup(photoId: Long) {
        viewModelScope.launch {
            photoRepository.removePhotoFromGroup(photoId)
            _uiState.update { it.copy(userMessage = "Photo removed from group") }
        }
    }

    fun addPhotosToGroup(groupId: Long, photoIds: List<Long>) {
        if (photoIds.isEmpty()) return
        viewModelScope.launch {
            photoRepository.addPhotosToGroup(groupId, photoIds)
            _uiState.update {
                it.copy(userMessage = "Added ${photoIds.size} note${if (photoIds.size > 1) "s" else ""} to group")
            }
        }
    }

    fun startSubfolderMultiSelect(subfolderId: Long) {
        _uiState.update {
            it.copy(
                isSubfolderMultiSelectMode = true,
                selectedSubfolderIds = setOf(subfolderId),
                isBatchSelectMode = false,
                selectedPhotoIds = emptySet(),
                selectedGroupIds = emptySet()
            )
        }
    }

    fun toggleSubfolderSelection(subfolderId: Long) {
        _uiState.update { current ->
            val set = current.selectedSubfolderIds.toMutableSet()
            if (set.contains(subfolderId)) set.remove(subfolderId) else set.add(subfolderId)
            current.copy(selectedSubfolderIds = set)
        }
    }

    fun exitSubfolderMultiSelect() {
        _uiState.update {
            it.copy(
                isSubfolderMultiSelectMode = false,
                selectedSubfolderIds = emptySet()
            )
        }
    }

    fun getSubfolderDeleteStats(subfolderIds: List<Long>, onResult: (SubfolderDeleteResult) -> Unit) {
        viewModelScope.launch {
            val stats = folderRepository.getSubfolderDeleteStats(subfolderIds)
            onResult(stats)
        }
    }

    fun deleteSubfolder(subfolderId: Long) {
        viewModelScope.launch {
            folderRepository.deleteSubfolder(subfolderId)
            _uiState.update {
                it.copy(
                    selectedSubfolderId = if (it.selectedSubfolderId == subfolderId) null else it.selectedSubfolderId,
                    userMessage = "Subfolder moved to Trash"
                )
            }
        }
    }

    fun deleteSelectedSubfolders() {
        val ids = _uiState.value.selectedSubfolderIds.toList()
        if (ids.isEmpty()) return
        viewModelScope.launch {
            val result = folderRepository.deleteSubfolders(ids)
            _uiState.update {
                it.copy(
                    isSubfolderMultiSelectMode = false,
                    selectedSubfolderIds = emptySet(),
                    selectedSubfolderId = if (it.selectedSubfolderId in ids) null else it.selectedSubfolderId,
                    userMessage = "Moved ${result.subfolderCount} subfolder${if (result.subfolderCount > 1) "s" else ""} to Trash"
                )
            }
        }
    }

    fun deleteSelectedPhotos() {
        val photoIds = _uiState.value.selectedPhotoIds.toList()
        val groupIds = _uiState.value.selectedGroupIds.toList()
        if (photoIds.isEmpty() && groupIds.isEmpty()) return
        viewModelScope.launch {
            if (photoIds.isNotEmpty()) {
                photoIds.forEach { deadlineNotificationManager?.cancelReminder(it) }
                photoRepository.deletePhotos(photoIds)
            }
            if (groupIds.isNotEmpty()) {
                photoRepository.deleteGroups(groupIds)
            }
            exitBatchSelectMode()
            val total = photoIds.size + groupIds.size
            _uiState.update {
                it.copy(userMessage = "Moved $total item${if (total > 1) "s" else ""} to Trash")
            }
        }
    }

    fun moveSelectedPhotos(targetFolderId: Long, targetSubfolderId: Long? = null) {
        val photoIds = _uiState.value.selectedPhotoIds.toList()
        val groupIds = _uiState.value.selectedGroupIds.toList()
        if (photoIds.isEmpty() && groupIds.isEmpty()) return
        viewModelScope.launch {
            if (photoIds.isNotEmpty()) {
                photoRepository.movePhotos(photoIds, targetFolderId, targetSubfolderId)
            }
            if (groupIds.isNotEmpty()) {
                photoRepository.moveGroups(groupIds, targetFolderId, targetSubfolderId)
            }
            exitBatchSelectMode()
            val total = photoIds.size + groupIds.size
            _uiState.update {
                it.copy(userMessage = "Moved $total item${if (total > 1) "s" else ""}")
            }
        }
    }

    fun updateSelectedPhotosTagColor(colorHex: String?) {
        val photoIds = _uiState.value.selectedPhotoIds.toList()
        val groupIds = _uiState.value.selectedGroupIds.toList()
        if (photoIds.isEmpty() && groupIds.isEmpty()) return
        viewModelScope.launch {
            if (photoIds.isNotEmpty()) {
                photoRepository.updatePhotosTagColor(photoIds, colorHex)
            }
            for (gId in groupIds) {
                photoRepository.updateGroupTagColor(gId, colorHex)
            }
            exitBatchSelectMode()
            _uiState.update { it.copy(userMessage = "Updated tag color") }
        }
    }

    fun movePhotoToSubfolder(photoId: Long, subfolderId: Long?) {
        viewModelScope.launch {
            val photo = _uiState.value.photos.firstOrNull { it.id == photoId }
            if (photo != null) {
                photoRepository.updatePhoto(photo.copy(subfolderId = subfolderId))
                _uiState.update { it.copy(userMessage = "Moved note") }
            }
        }
    }

    fun updatePhotoTagColor(photoId: Long, colorHex: String?) {
        viewModelScope.launch {
            val photo = _uiState.value.photos.firstOrNull { it.id == photoId }
            if (photo != null) {
                photoRepository.updatePhoto(photo.copy(tagColor = colorHex))
                _uiState.update { it.copy(userMessage = "Updated note tag color") }
            }
        }
    }

    fun setPhotoDeadline(photoId: Long, deadlineMs: Long?) {
        viewModelScope.launch {
            val photo = _uiState.value.photos.firstOrNull { it.id == photoId }
            if (photo != null) {
                photoRepository.updatePhoto(photo.copy(linkedDeadline = deadlineMs))
                val folderName = _uiState.value.folder?.name ?: "Coursework"
                if (deadlineMs != null) {
                    deadlineNotificationManager?.scheduleReminder(photo.id, folderName, photo.caption, deadlineMs)
                } else {
                    deadlineNotificationManager?.cancelReminder(photo.id)
                }
                val msg = if (deadlineMs != null) "Deadline attached" else "Deadline removed"
                _uiState.update { it.copy(userMessage = msg) }
            }
        }
    }

    fun deletePhoto(photoId: Long) {
        viewModelScope.launch {
            deadlineNotificationManager?.cancelReminder(photoId)
            photoRepository.deletePhoto(photoId)
            _uiState.update { it.copy(userMessage = "Note moved to Trash") }
        }
    }

    fun renamePhoto(photoId: Long, newCaption: String) {
        viewModelScope.launch {
            photoRepository.renamePhoto(photoId, newCaption)
            _uiState.update { it.copy(userMessage = "Photo renamed") }
        }
    }

    fun updatePhotoNote(photoId: Long, newNote: String?) {
        viewModelScope.launch {
            photoRepository.updatePhotoNote(photoId, newNote)
            _uiState.update { it.copy(userMessage = "Note updated") }
        }
    }

    fun addTagToPhoto(photoId: Long, tag: String) {
        viewModelScope.launch {
            photoRepository.addTagToPhoto(photoId, tag)
            _uiState.update { it.copy(userMessage = "Tag added") }
        }
    }

    fun removeTagFromPhoto(photoId: Long, tag: String) {
        viewModelScope.launch {
            photoRepository.removeTagFromPhoto(photoId, tag)
            _uiState.update { it.copy(userMessage = "Tag removed") }
        }
    }

    fun rotatePhoto(photoId: Long, onUpdated: ((Photo) -> Unit)? = null) {
        viewModelScope.launch {
            val updated = photoRepository.rotatePhotoClockwise(photoId)
            if (updated != null) {
                val newOcrText = try {
                    ocrEngine?.extractText(updated.fileUri)?.fullText
                } catch (_: Exception) { null } ?: updated.ocrText
                val finalPhoto = updated.copy(ocrText = newOcrText)
                photoRepository.updatePhoto(finalPhoto)
                _uiState.update { it.copy(userMessage = "Photo rotated 90°") }
                onUpdated?.invoke(finalPhoto)
            }
        }
    }

    fun cropPhoto(
        photoId: Long,
        leftFraction: Float,
        topFraction: Float,
        rightFraction: Float,
        bottomFraction: Float,
        onUpdated: ((Photo) -> Unit)? = null
    ) {
        viewModelScope.launch {
            val updated = photoRepository.cropPhoto(photoId, leftFraction, topFraction, rightFraction, bottomFraction)
            if (updated != null) {
                val newOcrText = try {
                    ocrEngine?.extractText(updated.fileUri)?.fullText
                } catch (_: Exception) { null } ?: updated.ocrText
                val finalPhoto = updated.copy(ocrText = newOcrText)
                photoRepository.updatePhoto(finalPhoto)
                _uiState.update { it.copy(userMessage = "Photo re-cropped and text re-indexed") }
                onUpdated?.invoke(finalPhoto)
            }
        }
    }

    fun clearHighlightedPhoto() {
        _uiState.update { it.copy(highlightedPhotoId = null) }
    }

    fun clearHighlightedGroup() {
        _uiState.update { it.copy(highlightedGroupId = null) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    companion object {
        fun provideFactory(
            folderId: Long,
            folderRepository: FolderRepository,
            photoRepository: PhotoRepository,
            photoStorageManager: PhotoStorageManager,
            ocrEngine: OcrEngine,
            folderSuggestEngine: FolderSuggestEngine,
            deadlineNotificationManager: DeadlineNotificationManager? = null,
            initialSubfolderId: Long? = null,
            targetPhotoId: Long? = null,
            targetGroupId: Long? = null,
            settingsRepository: SettingsRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FolderDetailViewModel(
                    folderId = folderId,
                    folderRepository = folderRepository,
                    photoRepository = photoRepository,
                    photoStorageManager = photoStorageManager,
                    ocrEngine = ocrEngine,
                    folderSuggestEngine = folderSuggestEngine,
                    deadlineNotificationManager = deadlineNotificationManager,
                    initialSubfolderId = initialSubfolderId,
                    targetPhotoId = targetPhotoId,
                    targetGroupId = targetGroupId,
                    settingsRepository = settingsRepository
                ) as T
            }
        }
    }
}
