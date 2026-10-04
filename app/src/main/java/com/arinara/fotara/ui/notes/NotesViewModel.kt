// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arinara.fotara.data.model.CanvasNote
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.TextNote
import com.arinara.fotara.data.repository.CanvasNoteRepository
import com.arinara.fotara.data.repository.DocumentRepository
import com.arinara.fotara.data.repository.FolderRepository
import com.arinara.fotara.data.repository.PhotoRepository
import com.arinara.fotara.data.repository.TextNoteRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

data class NotesUiState(
    val dateGroups: List<DateGroup> = emptyList(),
    val totalItemCount: Int = 0,
    val selectedFilter: NoteFilterChip = NoteFilterChip.ALL,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val folders: List<Folder> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false
)

private data class RawNotesBundle(
    val photos: List<Photo>,
    val documents: List<DocumentNote>,
    val textNotes: List<TextNote>,
    val canvasNotes: List<CanvasNote>
)

class NotesViewModel(
    private val photoRepository: PhotoRepository,
    private val documentRepository: DocumentRepository,
    private val textNoteRepository: TextNoteRepository,
    private val canvasNoteRepository: CanvasNoteRepository,
    private val folderRepository: FolderRepository
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(NoteFilterChip.ALL)
    private val searchQuery = MutableStateFlow("")
    private val isSearchActive = MutableStateFlow(false)

    private val rawNotesFlow: Flow<RawNotesBundle> = combine(
        photoRepository.getAllActivePhotosFlow(),
        documentRepository.getAllActiveDocumentNotes(),
        textNoteRepository.getAllActiveTextNotes(),
        canvasNoteRepository.getAllActiveCanvasNotes()
    ) { photos, documents, textNotes, canvasNotes ->
        RawNotesBundle(photos, documents, textNotes, canvasNotes)
    }

    private val _uiState = MutableStateFlow(NotesUiState(isLoading = true))
    val uiState: StateFlow<NotesUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                rawNotesFlow,
                folderRepository.getFolders(),
                selectedFilter,
                searchQuery,
                isSearchActive
            ) { bundle, folders, filter, query, searchActive ->

                val activeFolders = folders.filter { !it.isTrashed }
                val folderMap = activeFolders.associateBy { it.id }

                // 1. Map to UnifiedNoteItem
                val allItems = mutableListOf<UnifiedNoteItem>()

                // Photos
                for (photo in bundle.photos) {
                    if (!photo.isTrashed) {
                        val title = photo.caption?.let { if (it.isNotBlank()) it else null } ?: "Photo Note"
                        allItems.add(
                            UnifiedNoteItem(
                                id = photo.id,
                                type = UnifiedNoteType.PHOTO,
                                title = title,
                                folderId = photo.folderId,
                                folderName = folderMap[photo.folderId]?.name ?: "General",
                                subfolderId = photo.subfolderId,
                                addedAt = photo.addedAt,
                                previewUri = photo.thumbnailUri ?: photo.fileUri,
                                rawItem = photo
                            )
                        )
                    }
                }

                // Documents
                for (doc in bundle.documents) {
                    if (!doc.isTrashed) {
                        allItems.add(
                            UnifiedNoteItem(
                                id = doc.id,
                                type = UnifiedNoteType.DOCUMENT,
                                title = doc.name,
                                folderId = doc.folderId,
                                folderName = folderMap[doc.folderId]?.name ?: "General",
                                subfolderId = doc.subfolderId,
                                addedAt = doc.addedAt,
                                documentType = doc.docType,
                                rawItem = doc
                            )
                        )
                    }
                }

                // Text Notes
                for (note in bundle.textNotes) {
                    if (!note.isTrashed) {
                        val title = if (note.title.isNotBlank()) note.title else "Text Note"
                        allItems.add(
                            UnifiedNoteItem(
                                id = note.id,
                                type = UnifiedNoteType.TEXT,
                                title = title,
                                folderId = note.folderId,
                                folderName = folderMap[note.folderId]?.name ?: "General",
                                subfolderId = note.subfolderId,
                                addedAt = note.addedAt,
                                rawItem = note
                            )
                        )
                    }
                }

                // Canvas Notes
                for (canvas in bundle.canvasNotes) {
                    if (!canvas.isTrashed) {
                        val title = if (canvas.title.isNotBlank()) canvas.title else "Canvas Note"
                        allItems.add(
                            UnifiedNoteItem(
                                id = canvas.id,
                                type = UnifiedNoteType.CANVAS,
                                title = title,
                                folderId = canvas.folderId,
                                folderName = folderMap[canvas.folderId]?.name ?: "General",
                                subfolderId = canvas.subfolderId,
                                addedAt = canvas.addedAt,
                                previewUri = canvas.thumbnailPath,
                                rawItem = canvas
                            )
                        )
                    }
                }

                // 2. Filter by chip
                val typeFiltered = when (filter) {
                    NoteFilterChip.ALL -> allItems
                    NoteFilterChip.PHOTOS -> allItems.filter { it.type == UnifiedNoteType.PHOTO }
                    NoteFilterChip.DOCUMENTS -> allItems.filter { it.type == UnifiedNoteType.DOCUMENT }
                    NoteFilterChip.TEXT -> allItems.filter { it.type == UnifiedNoteType.TEXT }
                    NoteFilterChip.CANVAS -> allItems.filter { it.type == UnifiedNoteType.CANVAS }
                }

                // 3. Filter by search query
                val trimmedQuery = query.trim()
                val queryFiltered = if (trimmedQuery.isBlank()) {
                    typeFiltered
                } else {
                    typeFiltered.filter { item ->
                        item.title.contains(trimmedQuery, ignoreCase = true) ||
                            item.folderName.contains(trimmedQuery, ignoreCase = true) ||
                            (item.rawItem is TextNote && item.rawItem.bodyMarkdown.contains(trimmedQuery, ignoreCase = true))
                    }
                }

                // 4. Sort all items descending by addedAt
                val sortedItems = queryFiltered.sortedByDescending { it.addedAt }

                // 5. Group by calendar date
                val zone = ZoneId.systemDefault()
                val today = LocalDate.now(zone)
                val yesterday = today.minusDays(1)

                val groupsMap = linkedMapOf<LocalDate, MutableList<UnifiedNoteItem>>()
                for (item in sortedItems) {
                    val date = Instant.ofEpochMilli(item.addedAt).atZone(zone).toLocalDate()
                    groupsMap.getOrPut(date) { mutableListOf() }.add(item)
                }

                val dateGroups = groupsMap.map { (date, items) ->
                    DateGroup(
                        date = date,
                        label = NotesDateUtils.formatGroupDateLabel(date, today, yesterday),
                        dotColor = NotesDateUtils.getGroupDotColor(date, today, yesterday),
                        items = items
                    )
                }

                NotesUiState(
                    dateGroups = dateGroups,
                    totalItemCount = sortedItems.size,
                    selectedFilter = filter,
                    searchQuery = query,
                    isSearchActive = searchActive,
                    folders = activeFolders,
                    isLoading = false
                )
            }.collect { newState ->
                _uiState.value = newState
            }
        }
    }

    fun setFilter(filter: NoteFilterChip) {
        selectedFilter.value = filter
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun toggleSearch() {
        val next = !isSearchActive.value
        isSearchActive.value = next
        if (!next) {
            searchQuery.value = ""
        }
    }

    fun renameNote(item: UnifiedNoteItem, newTitle: String) {
        val trimmed = newTitle.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            when (item.type) {
                UnifiedNoteType.PHOTO -> photoRepository.renamePhoto(item.id, trimmed)
                UnifiedNoteType.DOCUMENT -> documentRepository.renameDocumentNote(item.id, trimmed)
                UnifiedNoteType.TEXT -> textNoteRepository.renameTextNote(item.id, trimmed)
                UnifiedNoteType.CANVAS -> canvasNoteRepository.renameCanvasNote(item.id, trimmed)
            }
        }
    }

    fun moveNote(item: UnifiedNoteItem, targetFolderId: Long) {
        viewModelScope.launch {
            when (item.type) {
                UnifiedNoteType.PHOTO -> photoRepository.movePhotos(listOf(item.id), targetFolderId, null)
                UnifiedNoteType.DOCUMENT -> documentRepository.moveDocumentNote(item.id, targetFolderId, null)
                UnifiedNoteType.TEXT -> textNoteRepository.moveTextNote(item.id, targetFolderId, null)
                UnifiedNoteType.CANVAS -> canvasNoteRepository.moveCanvasNote(item.id, targetFolderId, null)
            }
        }
    }

    fun deleteNote(item: UnifiedNoteItem) {
        viewModelScope.launch {
            when (item.type) {
                UnifiedNoteType.PHOTO -> photoRepository.deletePhoto(item.id)
                UnifiedNoteType.DOCUMENT -> documentRepository.deleteDocumentNote(item.id)
                UnifiedNoteType.TEXT -> textNoteRepository.deleteTextNote(item.id)
                UnifiedNoteType.CANVAS -> canvasNoteRepository.deleteCanvasNote(item.id)
            }
        }
    }

    private var isRefreshingData = false

    fun refresh() {
        if (isRefreshingData) return
        isRefreshingData = true
        _uiState.update { it.copy(isRefreshing = true) }
        viewModelScope.launch {
            try {
                kotlinx.coroutines.withTimeoutOrNull(com.arinara.fotara.ui.components.PullToRefreshHelper.REFRESH_TIMEOUT_MS) {
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        folderRepository.refresh()
                        photoRepository.refresh()
                        documentRepository.refresh()
                        textNoteRepository.refresh()
                        canvasNoteRepository.refresh()
                    }
                }
            } catch (_: Exception) {
            } finally {
                isRefreshingData = false
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    companion object {
        fun provideFactory(
            photoRepository: PhotoRepository,
            documentRepository: DocumentRepository,
            textNoteRepository: TextNoteRepository,
            canvasNoteRepository: CanvasNoteRepository,
            folderRepository: FolderRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NotesViewModel(
                    photoRepository = photoRepository,
                    documentRepository = documentRepository,
                    textNoteRepository = textNoteRepository,
                    canvasNoteRepository = canvasNoteRepository,
                    folderRepository = folderRepository
                ) as T
            }
        }
    }
}
