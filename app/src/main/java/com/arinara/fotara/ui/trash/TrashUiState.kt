// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.trash

import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo

enum class TrashTab {
    ALL,
    FOLDERS,
    PHOTOS
}

data class TrashUiState(
    val trashedFolders: List<Folder> = emptyList(),
    val trashedPhotos: List<Photo> = emptyList(),
    val activeFolders: List<Folder> = emptyList(),
    val selectedTab: TrashTab = TrashTab.ALL,
    val isLoading: Boolean = false,
    val showEmptyTrashDialog: Boolean = false,
    val orphanPhotoToRestore: Photo? = null,
    val parentFolderForOrphan: Folder? = null,
    val photoToDeletePermanently: Photo? = null,
    val folderToDeletePermanently: Folder? = null,
    val userMessage: String? = null
)
