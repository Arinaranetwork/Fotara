// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.group

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arinara.fotara.data.model.DestinationType
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoSource
import com.arinara.fotara.data.model.RecentDestination
import com.arinara.fotara.data.repository.FolderRepository
import com.arinara.fotara.data.repository.PhotoRepository
import com.arinara.fotara.data.repository.SettingsRepository
import com.arinara.fotara.data.storage.PhotoStorageManager
import com.arinara.fotara.ocr.OcrEngine
import com.arinara.fotara.util.DeadlineNotificationManager
import com.arinara.fotara.util.PdfExporter
import com.arinara.fotara.util.ZipExporter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class GroupDetailViewModel(
    val groupId: Long,
    val folderId: Long,
    val targetPhotoId: Long? = null,
    private val photoRepository: PhotoRepository,
    private val folderRepository: FolderRepository,
    private val settingsRepository: SettingsRepository? = null,
    private val ocrEngine: OcrEngine? = null,
    private val photoStorageManager: PhotoStorageManager? = null,
    private val deadlineNotificationManager: DeadlineNotificationManager? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        GroupDetailUiState(
            highlightedPhotoId = targetPhotoId
        )
    )
    val uiState: StateFlow<GroupDetailUiState> = _uiState.asStateFlow()

    init {
        loadGroupData()
    }

    private fun loadGroupData() {
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

        viewModelScope.launch {
            combine(
                photoRepository.getGroupsByFolder(folderId),
                photoRepository.getPhotosByFolder(folderId)
            ) { groups, photos ->
                val currentGroup = groups.firstOrNull { it.id == groupId }
                // Sort by order added to group (earliest added_at first, matching cover photo logic)
                val memberPhotos = photos
                    .filter { it.groupId == groupId }
                    .sortedBy { it.addedAt }
                val standalonePhotos = photos.filter { it.groupId == null }

                Triple(currentGroup, memberPhotos, standalonePhotos)
            }.collect { (currentGroup, memberPhotos, standalonePhotos) ->
                _uiState.update {
                    it.copy(
                        group = currentGroup,
                        photos = memberPhotos,
                        availableFolderPhotos = standalonePhotos
                    )
                }
            }
        }

        settingsRepository?.let { repo ->
            viewModelScope.launch {
                repo.settingsFlow.collect { settings ->
                    _uiState.update { it.copy(gridDensity = settings.gridDensity) }
                }
            }
            refreshRecentDestinations()
        }
    }

    fun refreshRecentDestinations() {
        settingsRepository?.let { repo ->
            viewModelScope.launch {
                val recents = repo.getRecentDestinations()
                _uiState.update { it.copy(recentDestinations = recents) }
            }
        }
    }

    fun renameGroup(newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            photoRepository.renameGroup(groupId, trimmed)
            _uiState.update { it.copy(userMessage = "Group renamed to \"$trimmed\"") }
        }
    }

    fun updateGroupColor(colorHex: String?) {
        viewModelScope.launch {
            photoRepository.updateGroupTagColor(groupId, colorHex)
            _uiState.update { it.copy(userMessage = "Group color updated") }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            photoRepository.refresh()
            refreshRecentDestinations()
        }
    }

    fun ungroup(onComplete: () -> Unit) {
        viewModelScope.launch {
            photoRepository.ungroup(groupId)
            onComplete()
        }
    }

    fun deleteGroup(onComplete: () -> Unit) {
        viewModelScope.launch {
            photoRepository.deleteGroup(groupId)
            onComplete()
        }
    }

    fun removePhotoFromGroup(photoId: Long, onAutoDissolved: (() -> Unit)? = null) {
        viewModelScope.launch {
            val remainingCount = _uiState.value.photos.count { it.id != photoId }
            photoRepository.removePhotoFromGroup(photoId)
            if (remainingCount <= 1) {
                // Group dissolved per Addendum 5 single-member auto-dissolve rule
                onAutoDissolved?.invoke()
            } else {
                _uiState.update { it.copy(userMessage = "Photo removed from group") }
            }
        }
    }

    fun addPhotosToGroup(photoIds: List<Long>) {
        if (photoIds.isEmpty()) return
        viewModelScope.launch {
            photoRepository.addPhotosToGroup(groupId, photoIds)
            _uiState.update {
                it.copy(userMessage = "Added ${photoIds.size} note${if (photoIds.size > 1) "s" else ""} to group")
            }
        }
    }

    fun updatePhotoNote(photoId: Long, note: String?) {
        viewModelScope.launch {
            photoRepository.updatePhotoNote(photoId, note)
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

    fun setCoverPhoto(photoId: Long) {
        viewModelScope.launch {
            photoRepository.setGroupCoverPhoto(groupId, photoId)
            _uiState.update { it.copy(userMessage = "Cover photo updated") }
        }
    }

    fun setGroupDeadline(deadlineMs: Long?) {
        viewModelScope.launch {
            photoRepository.updateGroupDeadline(groupId, deadlineMs)
            val folderName = _uiState.value.folder?.name ?: "Folder"
            val groupName = _uiState.value.group?.name ?: "Group"
            if (deadlineMs != null) {
                deadlineNotificationManager?.scheduleGroupReminder(groupId, folderName, groupName, deadlineMs)
                _uiState.update { it.copy(userMessage = "Group deadline set") }
            } else {
                deadlineNotificationManager?.cancelGroupReminder(groupId)
                _uiState.update { it.copy(userMessage = "Group deadline removed") }
            }
        }
    }

    fun moveGroup(targetFolderId: Long, targetSubfolderId: Long?, onMoved: (() -> Unit)? = null) {
        viewModelScope.launch {
            photoRepository.moveGroup(groupId, targetFolderId, targetSubfolderId)
            settingsRepository?.let { repo ->
                val folderName = _uiState.value.availableFolders.firstOrNull { it.id == targetFolderId }?.name ?: "Folder"
                repo.addRecentDestination(
                    RecentDestination(
                        type = if (targetSubfolderId != null) DestinationType.SUBFOLDER else DestinationType.FOLDER,
                        folderId = targetFolderId,
                        subfolderId = targetSubfolderId,
                        title = folderName
                    )
                )
            }
            onMoved?.invoke()
        }
    }

    fun saveCapturedBatch(photos: List<Photo>, targetSubfolderId: Long? = null) {
        viewModelScope.launch {
            val groupSubfolderId = targetSubfolderId ?: _uiState.value.group?.subfolderId
            photos.forEach { photo ->
                val toSave = photo.copy(
                    folderId = folderId,
                    subfolderId = groupSubfolderId,
                    groupId = groupId,
                    addedAt = System.currentTimeMillis()
                )
                photoRepository.addPhoto(toSave)
            }
            _uiState.update {
                it.copy(userMessage = "Added ${photos.size} note${if (photos.size > 1) "s" else ""} to group")
            }
        }
    }

    suspend fun importGalleryUris(uris: List<Uri>): List<Photo> {
        val results = mutableListOf<Photo>()
        val currentSubfolder = _uiState.value.group?.subfolderId
        for (uri in uris) {
            try {
                val saved = photoStorageManager?.saveUriAsPhoto(uri) ?: continue
                val ocr = ocrEngine?.extractText(saved.filePath)
                val nextIndex = _uiState.value.photos.size + results.size + 1
                val newPhoto = Photo(
                    fileUri = saved.filePath,
                    thumbnailUri = saved.thumbnailPath,
                    folderId = folderId,
                    subfolderId = currentSubfolder,
                    groupId = groupId,
                    caption = "Imported Note #$nextIndex",
                    ocrText = ocr?.fullText,
                    source = PhotoSource.IMPORT,
                    fileSizeBytes = saved.fileSizeBytes,
                    addedAt = System.currentTimeMillis()
                )
                results.add(newPhoto)
            } catch (_: Exception) {}
        }
        return results
    }

    fun exportGroup(context: Context, isZip: Boolean, onReady: (File) -> Unit) {
        val currentGroup = _uiState.value.group ?: return
        val memberPhotos = _uiState.value.photos
        if (memberPhotos.isEmpty()) {
            _uiState.update { it.copy(userMessage = "No photos to export") }
            return
        }
        viewModelScope.launch {
            try {
                val exportedFile = if (isZip) {
                    ZipExporter.exportPhotosToZip(context, currentGroup.name, memberPhotos)
                } else {
                    PdfExporter.exportPhotosToPdf(context, currentGroup.name, memberPhotos)
                }
                onReady(exportedFile)
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Failed to export group: ${e.message}") }
            }
        }
    }

    fun clearHighlightedPhoto() {
        _uiState.update { it.copy(highlightedPhotoId = null) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    companion object {
        fun provideFactory(
            groupId: Long,
            folderId: Long,
            targetPhotoId: Long? = null,
            photoRepository: PhotoRepository,
            folderRepository: FolderRepository,
            settingsRepository: SettingsRepository? = null,
            ocrEngine: OcrEngine? = null,
            photoStorageManager: PhotoStorageManager? = null,
            deadlineNotificationManager: DeadlineNotificationManager? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return GroupDetailViewModel(
                    groupId = groupId,
                    folderId = folderId,
                    targetPhotoId = targetPhotoId,
                    photoRepository = photoRepository,
                    folderRepository = folderRepository,
                    settingsRepository = settingsRepository,
                    ocrEngine = ocrEngine,
                    photoStorageManager = photoStorageManager,
                    deadlineNotificationManager = deadlineNotificationManager
                ) as T
            }
        }
    }
}
