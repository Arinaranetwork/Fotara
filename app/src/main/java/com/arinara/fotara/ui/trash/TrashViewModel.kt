// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.trash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.repository.FolderRepository
import com.arinara.fotara.data.repository.PhotoRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TrashViewModel(
    private val folderRepository: FolderRepository,
    private val photoRepository: PhotoRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashUiState())
    val uiState: StateFlow<TrashUiState> = _uiState.asStateFlow()

    init {
        loadTrashData()
    }

    private fun loadTrashData() {
        viewModelScope.launch {
            folderRepository.getTrashedFolders().collect { folders ->
                _uiState.update { it.copy(trashedFolders = folders) }
            }
        }

        viewModelScope.launch {
            photoRepository.getTrashedPhotos().collect { photos ->
                _uiState.update { it.copy(trashedPhotos = photos) }
            }
        }

        viewModelScope.launch {
            folderRepository.getFolders().collect { folders ->
                _uiState.update { it.copy(activeFolders = folders) }
            }
        }
    }

    fun selectTab(tab: TrashTab) {
        _uiState.update { it.copy(selectedTab = tab) }
    }

    fun requestRestoreFolder(folder: Folder) {
        viewModelScope.launch {
            folderRepository.restoreFolder(folder.id)
            _uiState.update { it.copy(userMessage = "Folder \"${folder.name}\" restored") }
        }
    }

    fun requestRestorePhoto(photo: Photo) {
        viewModelScope.launch {
            val isParentTrashed = folderRepository.isFolderTrashed(photo.folderId)
            if (isParentTrashed) {
                val parentFolder = folderRepository.getFolderById(photo.folderId).firstOrNull()
                _uiState.update {
                    it.copy(
                        orphanPhotoToRestore = photo,
                        parentFolderForOrphan = parentFolder
                    )
                }
            } else {
                photoRepository.restorePhoto(photo.id)
                _uiState.update { it.copy(userMessage = "Note restored to original folder") }
            }
        }
    }

    fun dismissOrphanDialog() {
        _uiState.update { it.copy(orphanPhotoToRestore = null, parentFolderForOrphan = null) }
    }

    fun confirmRestoreBoth(photo: Photo, parentFolderId: Long) {
        viewModelScope.launch {
            folderRepository.restoreFolder(parentFolderId)
            photoRepository.restorePhoto(photo.id)
            _uiState.update {
                it.copy(
                    orphanPhotoToRestore = null,
                    parentFolderForOrphan = null,
                    userMessage = "Folder and note restored"
                )
            }
        }
    }

    fun confirmRestorePhotoToActiveFolder(photo: Photo, targetFolderId: Long) {
        viewModelScope.launch {
            photoRepository.restorePhoto(photo.id, targetFolderId = targetFolderId)
            _uiState.update {
                it.copy(
                    orphanPhotoToRestore = null,
                    parentFolderForOrphan = null,
                    userMessage = "Note restored to selected folder"
                )
            }
        }
    }

    fun requestDeletePhotoPermanently(photo: Photo) {
        _uiState.update { it.copy(photoToDeletePermanently = photo) }
    }

    fun dismissDeletePhotoPermanentlyDialog() {
        _uiState.update { it.copy(photoToDeletePermanently = null) }
    }

    fun confirmDeletePhotoPermanently() {
        val photo = _uiState.value.photoToDeletePermanently ?: return
        viewModelScope.launch {
            photoRepository.purgePhotoPermanently(photo.id)
            _uiState.update {
                it.copy(
                    photoToDeletePermanently = null,
                    userMessage = "Note permanently deleted"
                )
            }
        }
    }

    fun requestDeleteFolderPermanently(folder: Folder) {
        _uiState.update { it.copy(folderToDeletePermanently = folder) }
    }

    fun dismissDeleteFolderPermanentlyDialog() {
        _uiState.update { it.copy(folderToDeletePermanently = null) }
    }

    fun confirmDeleteFolderPermanently() {
        val folder = _uiState.value.folderToDeletePermanently ?: return
        viewModelScope.launch {
            folderRepository.purgeFolderPermanently(folder.id)
            _uiState.update {
                it.copy(
                    folderToDeletePermanently = null,
                    userMessage = "Folder permanently deleted"
                )
            }
        }
    }

    fun requestEmptyTrash() {
        _uiState.update { it.copy(showEmptyTrashDialog = true) }
    }

    fun dismissEmptyTrashDialog() {
        _uiState.update { it.copy(showEmptyTrashDialog = false) }
    }

    fun confirmEmptyTrash() {
        viewModelScope.launch {
            photoRepository.emptyTrash()
            _uiState.update {
                it.copy(
                    showEmptyTrashDialog = false,
                    userMessage = "Trash emptied"
                )
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    companion object {
        fun provideFactory(
            folderRepository: FolderRepository,
            photoRepository: PhotoRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return TrashViewModel(folderRepository, photoRepository) as T
            }
        }
    }
}
