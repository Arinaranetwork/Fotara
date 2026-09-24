// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.folder

import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.data.model.Subfolder

enum class PhotoSortOption {
    UPLOAD_DATE_DESC,
    UPLOAD_DATE_ASC,
    NEAREST_DEADLINE,
    COLOR_LABEL
}

sealed interface FolderGridItem {
    val key: String
    val sortCreatedAt: Long
    val sortDeadline: Long?
    val sortColor: String?

    data class StandalonePhoto(val photo: Photo) : FolderGridItem {
        override val key: String get() = "photo_${photo.id}"
        override val sortCreatedAt: Long get() = photo.addedAt
        override val sortDeadline: Long? get() = photo.linkedDeadline
        override val sortColor: String? get() = photo.tagColor
    }

    data class Group(
        val group: PhotoGroup,
        val memberPhotos: List<Photo>,
        val coverPhoto: Photo?
    ) : FolderGridItem {
        override val key: String get() = "group_${group.id}"
        override val sortCreatedAt: Long get() = group.createdAt
        override val sortDeadline: Long? get() = memberPhotos.mapNotNull { it.linkedDeadline }.minOrNull()
        override val sortColor: String? get() = group.tagColor
    }
}

data class FolderDetailUiState(
    val folder: Folder? = null,
    val subfolders: List<Subfolder> = emptyList(),
    val selectedSubfolderId: Long? = null, // null means "All"
    val photos: List<Photo> = emptyList(),
    val groups: List<PhotoGroup> = emptyList(),
    val gridItems: List<FolderGridItem> = emptyList(),
    val sortOption: PhotoSortOption = PhotoSortOption.UPLOAD_DATE_DESC,
    val isBatchSelectMode: Boolean = false,
    val selectedPhotoIds: Set<Long> = emptySet(),
    val selectedGroupIds: Set<Long> = emptySet(),
    val isSubfolderMultiSelectMode: Boolean = false,
    val selectedSubfolderIds: Set<Long> = emptySet(),
    val availableFolders: List<Folder> = emptyList(),
    val gridDensity: Int = 3,
    val isLoading: Boolean = false,
    val showAddSubfolderDialog: Boolean = false,
    val highlightedPhotoId: Long? = null,
    val highlightedGroupId: Long? = null,
    val userMessage: String? = null
) {
    val totalSelectionCount: Int get() = selectedPhotoIds.size + selectedGroupIds.size
}
