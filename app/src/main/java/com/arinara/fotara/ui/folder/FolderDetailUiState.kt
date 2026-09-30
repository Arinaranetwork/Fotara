// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.folder

import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.DocumentPage
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.data.model.RecentDestination
import com.arinara.fotara.data.model.Subfolder

import com.arinara.fotara.data.model.TextNote

sealed interface UndoAction {
    data class Delete(
        val photos: List<Photo>,
        val groups: List<PhotoGroup>,
        val documents: List<DocumentNote> = emptyList(),
        val textNotes: List<TextNote> = emptyList()
    ) : UndoAction

    data class Move(
        val photoMoves: List<Triple<Long, Long, Long?>>, // photoId, originalFolderId, originalSubfolderId
        val groupMoves: List<Triple<Long, Long, Long?>>,  // groupId, originalFolderId, originalSubfolderId
        val documentMoves: List<Triple<Long, Long, Long?>> = emptyList(), // docId, originalFolderId, originalSubfolderId
        val textNoteMoves: List<Triple<Long, Long, Long?>> = emptyList() // textNoteId, originalFolderId, originalSubfolderId
    ) : UndoAction

    data class Ungroup(
        val originalGroup: PhotoGroup,
        val memberPhotoIds: List<Long>
    ) : UndoAction
}

enum class PhotoSortOption {
    UPLOAD_DATE_DESC,
    UPLOAD_DATE_ASC,
    NEAREST_DEADLINE,
    COLOR_LABEL
}

sealed interface FolderGridItem {
    val key: String
    val itemId: Long
    val linkGroupId: Long?
    val isLinked: Boolean get() = linkGroupId != null
    val sortCreatedAt: Long
    val sortDeadline: Long?
    val sortColor: String?

    data class StandalonePhoto(
        val photo: Photo,
        override val linkGroupId: Long? = null
    ) : FolderGridItem {
        override val key: String get() = "photo_${photo.id}"
        override val itemId: Long get() = photo.id
        override val sortCreatedAt: Long get() = photo.addedAt
        override val sortDeadline: Long? get() = photo.linkedDeadline
        override val sortColor: String? get() = photo.tagColor
    }

    data class Group(
        val group: PhotoGroup,
        val memberPhotos: List<Photo>,
        val coverPhoto: Photo?,
        override val linkGroupId: Long? = null
    ) : FolderGridItem {
        override val key: String get() = "group_${group.id}"
        override val itemId: Long get() = -group.id
        override val sortCreatedAt: Long get() = group.createdAt
        override val sortDeadline: Long? get() = group.linkedDeadline ?: memberPhotos.mapNotNull { it.linkedDeadline }.minOrNull()
        override val sortColor: String? get() = group.tagColor
    }

    data class Document(
        val documentNote: DocumentNote,
        val pages: List<DocumentPage> = emptyList(),
        override val linkGroupId: Long? = null
    ) : FolderGridItem {
        override val key: String get() = "doc_${documentNote.id}"
        override val itemId: Long get() = documentNote.id + 1_000_000_000L
        override val sortCreatedAt: Long get() = documentNote.addedAt
        override val sortDeadline: Long? get() = documentNote.linkedDeadline
        override val sortColor: String? get() = documentNote.tagColor
    }

    data class TextNoteItem(
        val textNote: TextNote,
        override val linkGroupId: Long? = null
    ) : FolderGridItem {
        override val key: String get() = "text_note_${textNote.id}"
        override val itemId: Long get() = textNote.id + 2_000_000_000L
        override val sortCreatedAt: Long get() = textNote.addedAt
        override val sortDeadline: Long? get() = textNote.linkedDeadline
        override val sortColor: String? get() = textNote.tagColor
    }

    data class CanvasNoteItem(
        val canvasNote: com.arinara.fotara.data.model.CanvasNote,
        override val linkGroupId: Long? = null
    ) : FolderGridItem {
        override val key: String get() = "canvas_note_${canvasNote.id}"
        override val itemId: Long get() = canvasNote.id + 3_000_000_000L
        override val sortCreatedAt: Long get() = canvasNote.addedAt
        override val sortDeadline: Long? get() = canvasNote.linkedDeadline
        override val sortColor: String? get() = canvasNote.tagColor
    }
}

data class FolderDetailUiState(
    val folder: Folder? = null,
    val subfolders: List<Subfolder> = emptyList(),
    val selectedSubfolderId: Long? = null, // null means "All"
    val photos: List<Photo> = emptyList(),
    val groups: List<PhotoGroup> = emptyList(),
    val documents: List<DocumentNote> = emptyList(),
    val textNotes: List<TextNote> = emptyList(),
    val canvasNotes: List<com.arinara.fotara.data.model.CanvasNote> = emptyList(),
    val gridItems: List<FolderGridItem> = emptyList(),
    val sortOption: PhotoSortOption = PhotoSortOption.UPLOAD_DATE_DESC,
    val isBatchSelectMode: Boolean = false,
    val selectedPhotoIds: Set<Long> = emptySet(),
    val selectedGroupIds: Set<Long> = emptySet(),
    val selectedDocumentIds: Set<Long> = emptySet(),
    val selectedTextNoteIds: Set<Long> = emptySet(),
    val selectedCanvasNoteIds: Set<Long> = emptySet(),
    val isSubfolderMultiSelectMode: Boolean = false,
    val selectedSubfolderIds: Set<Long> = emptySet(),
    val availableFolders: List<Folder> = emptyList(),
    val availableGroups: List<PhotoGroup> = emptyList(),
    val recentDestinations: List<RecentDestination> = emptyList(),
    val gridDensity: Int = 3,
    val isLoading: Boolean = false,
    val importProgress: Pair<Int, Int>? = null,
    val showAddSubfolderDialog: Boolean = false,
    val highlightedPhotoId: Long? = null,
    val highlightedGroupId: Long? = null,
    val userMessage: String? = null,
    val pendingUndoAction: UndoAction? = null
) {
    val totalSelectionCount: Int get() = selectedPhotoIds.size + selectedGroupIds.size + selectedDocumentIds.size + selectedTextNoteIds.size + selectedCanvasNoteIds.size
}
