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
import com.arinara.fotara.data.model.Workspace
import com.arinara.fotara.data.model.WorkspaceKind
import com.arinara.fotara.data.repository.WorkspaceContentStats
import com.arinara.fotara.data.repository.WorkspaceError
import com.arinara.fotara.data.repository.WorkspaceRepository
import com.arinara.fotara.data.repository.WorkspaceResult
import com.arinara.fotara.data.repository.WorkspaceValidator
import com.arinara.fotara.ui.home.WorkspaceDeleteStep

data class NotesUiState(
    val dateGroups: List<DateGroup> = emptyList(),
    val totalItemCount: Int = 0,
    val selectedFilter: NoteFilterChip = NoteFilterChip.ALL,
    val isFiltersVisible: Boolean = false,
    val searchQuery: String = "",
    val isSearchActive: Boolean = false,
    val folders: List<Folder> = emptyList(),
    val workspaces: List<Workspace> = emptyList(),
    val selectedWorkspaceId: Long = 1L,
    val selectedWorkspaceName: String = "Home",
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val userMessage: String? = null,
    val showAddWorkspaceDialog: Boolean = false,
    val workspaceToRename: Workspace? = null,
    val workspaceToDelete: Workspace? = null,
    val workspaceDeleteStep: WorkspaceDeleteStep = WorkspaceDeleteStep.NONE,
    val workspaceDeleteStats: WorkspaceContentStats? = null,
    val workspaceDeleteProgress: Pair<Int, Int>? = null
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
    private val folderRepository: FolderRepository,
    private val workspaceRepository: WorkspaceRepository? = null
) : ViewModel() {

    private val selectedFilter = MutableStateFlow(NoteFilterChip.ALL)
    private val searchQuery = MutableStateFlow("")
    private val isSearchActive = MutableStateFlow(false)
    private val _isFiltersVisible = MutableStateFlow(false)

    private val selectedWorkspaceIdFlow: StateFlow<Long> =
        workspaceRepository?.selectedWorkspaceId ?: MutableStateFlow(1L)

    private val workspacesFlow: Flow<List<Workspace>> =
        workspaceRepository?.observeWorkspaces() ?: kotlinx.coroutines.flow.flowOf(
            listOf(
                Workspace(id = 1L, kind = WorkspaceKind.HOME, name = "Home", position = 0),
                Workspace(id = 2L, kind = WorkspaceKind.ARCHIVE, name = "Archive", position = 1)
            )
        )

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
        val notesAndFolders = combine(
            rawNotesFlow,
            folderRepository.getFolders()
        ) { bundle, folders -> Pair(bundle, folders) }

        val filterAndSearch = combine(
            selectedFilter,
            searchQuery,
            _isFiltersVisible
        ) { filter, query, filtersVisible -> Triple(filter, query, filtersVisible) }

        val workspaceState = combine(
            selectedWorkspaceIdFlow,
            workspacesFlow
        ) { selectedWsId, wsList -> Pair(selectedWsId, wsList) }

        viewModelScope.launch {
            combine(
                notesAndFolders,
                filterAndSearch,
                workspaceState
            ) { (bundle, folders), (filter, query, filtersVisible), (selectedWsId, wsList) ->

                val activeFolders = folders.filter { !it.isTrashed }
                val folderMap = activeFolders.associateBy { it.id }

                // 1. Map to UnifiedNoteItem for items belonging to selected workspace
                val allItems = mutableListOf<UnifiedNoteItem>()

                // Photos
                for (photo in bundle.photos) {
                    if (!photo.isTrashed) {
                        val folder = folderMap[photo.folderId]
                        if (folder != null && folder.workspaceId == selectedWsId) {
                            val title = photo.caption?.let { if (it.isNotBlank()) it else null } ?: "Photo Note"
                            allItems.add(
                                UnifiedNoteItem(
                                    id = photo.id,
                                    type = UnifiedNoteType.PHOTO,
                                    title = title,
                                    folderId = photo.folderId,
                                    folderName = folder.name,
                                    subfolderId = photo.subfolderId,
                                    addedAt = photo.addedAt,
                                    previewUri = photo.thumbnailUri ?: photo.fileUri,
                                    rawItem = photo
                                )
                            )
                        }
                    }
                }

                // Documents
                for (doc in bundle.documents) {
                    if (!doc.isTrashed) {
                        val folder = folderMap[doc.folderId]
                        if (folder != null && folder.workspaceId == selectedWsId) {
                            allItems.add(
                                UnifiedNoteItem(
                                    id = doc.id,
                                    type = UnifiedNoteType.DOCUMENT,
                                    title = doc.name,
                                    folderId = doc.folderId,
                                    folderName = folder.name,
                                    subfolderId = doc.subfolderId,
                                    addedAt = doc.addedAt,
                                    documentType = doc.docType,
                                    rawItem = doc
                                )
                            )
                        }
                    }
                }

                // Text Notes
                for (note in bundle.textNotes) {
                    if (!note.isTrashed) {
                        val folder = folderMap[note.folderId]
                        if (folder != null && folder.workspaceId == selectedWsId) {
                            val title = if (note.title.isNotBlank()) note.title else "Text Note"
                            allItems.add(
                                UnifiedNoteItem(
                                    id = note.id,
                                    type = UnifiedNoteType.TEXT,
                                    title = title,
                                    folderId = note.folderId,
                                    folderName = folder.name,
                                    subfolderId = note.subfolderId,
                                    addedAt = note.addedAt,
                                    rawItem = note
                                )
                            )
                        }
                    }
                }

                // Canvas Notes
                for (canvas in bundle.canvasNotes) {
                    if (!canvas.isTrashed) {
                        val folder = folderMap[canvas.folderId]
                        if (folder != null && folder.workspaceId == selectedWsId) {
                            val title = if (canvas.title.isNotBlank()) canvas.title else "Canvas Note"
                            allItems.add(
                                UnifiedNoteItem(
                                    id = canvas.id,
                                    type = UnifiedNoteType.CANVAS,
                                    title = title,
                                    folderId = canvas.folderId,
                                    folderName = folder.name,
                                    subfolderId = canvas.subfolderId,
                                    addedAt = canvas.addedAt,
                                    previewUri = canvas.thumbnailPath,
                                    rawItem = canvas
                                )
                            )
                        }
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

                val currentWs = wsList.firstOrNull { it.id == selectedWsId }
                val wsDisplayName = when {
                    currentWs == null -> "Home"
                    currentWs.kind == WorkspaceKind.HOME -> "Home"
                    currentWs.kind == WorkspaceKind.ARCHIVE -> "Archive"
                    else -> currentWs.name
                }

                _uiState.update { prev ->
                    prev.copy(
                        dateGroups = dateGroups,
                        totalItemCount = sortedItems.size,
                        selectedFilter = filter,
                        isFiltersVisible = filtersVisible,
                        searchQuery = query,
                        folders = activeFolders,
                        workspaces = wsList,
                        selectedWorkspaceId = selectedWsId,
                        selectedWorkspaceName = wsDisplayName,
                        isLoading = false
                    )
                }
            }.collect {}
        }
    }

    fun setFilter(filter: NoteFilterChip) {
        selectedFilter.value = filter
    }

    fun toggleFilters() {
        val next = !_isFiltersVisible.value
        _isFiltersVisible.value = next
        if (!next) {
            selectedFilter.value = NoteFilterChip.ALL
        }
    }

    fun setSearchQuery(query: String) {
        searchQuery.value = query
    }

    fun toggleSearch() {
        toggleFilters()
    }

    fun selectWorkspace(workspaceId: Long) {
        workspaceRepository?.selectWorkspace(workspaceId)
    }

    fun openAddWorkspaceDialog() {
        val customCount = _uiState.value.workspaces.count { it.kind == WorkspaceKind.CUSTOM }
        if (customCount >= WorkspaceValidator.MAX_CUSTOM_WORKSPACES) {
            _uiState.update { it.copy(userMessage = "You can have up to 10 workspaces.") }
            return
        }
        _uiState.update { it.copy(showAddWorkspaceDialog = true) }
    }

    fun closeAddWorkspaceDialog() {
        _uiState.update { it.copy(showAddWorkspaceDialog = false) }
    }

    fun createWorkspace(name: String, iconKey: String? = null) {
        viewModelScope.launch {
            val repo = workspaceRepository ?: return@launch
            when (val result = repo.createWorkspace(name, iconKey)) {
                is WorkspaceResult.Success -> {
                    closeAddWorkspaceDialog()
                    selectWorkspace(result.data.id)
                }
                is WorkspaceResult.Error -> {
                    val msg = when (result.error) {
                        is WorkspaceError.LimitReached -> "You can have up to 10 workspaces."
                        is WorkspaceError.NameEmpty -> "Workspace name cannot be empty."
                        is WorkspaceError.NameTooLong -> "Workspace name is too long."
                        is WorkspaceError.NameDuplicate -> "A workspace with this name already exists."
                        is WorkspaceError.NameReserved -> "This name is reserved."
                        else -> "Failed to create workspace."
                    }
                    _uiState.update { it.copy(userMessage = msg) }
                }
            }
        }
    }

    fun openRenameWorkspaceDialog(workspace: Workspace) {
        _uiState.update { it.copy(workspaceToRename = workspace) }
    }

    fun closeRenameWorkspaceDialog() {
        _uiState.update { it.copy(workspaceToRename = null) }
    }

    fun updateWorkspace(workspaceId: Long, newName: String, iconKey: String? = null) {
        viewModelScope.launch {
            val repo = workspaceRepository ?: return@launch
            when (val result = repo.updateWorkspace(workspaceId, newName, iconKey)) {
                is WorkspaceResult.Success -> {
                    closeRenameWorkspaceDialog()
                }
                is WorkspaceResult.Error -> {
                    val msg = when (result.error) {
                        is WorkspaceError.NameEmpty -> "Workspace name cannot be empty."
                        is WorkspaceError.NameTooLong -> "Workspace name is too long."
                        is WorkspaceError.NameDuplicate -> "A workspace with this name already exists."
                        is WorkspaceError.NameReserved -> "This name is reserved."
                        else -> "Failed to update workspace."
                    }
                    _uiState.update { it.copy(userMessage = msg) }
                }
            }
        }
    }

    fun renameWorkspace(workspaceId: Long, newName: String) {
        updateWorkspace(workspaceId, newName, null)
    }

    fun reorderWorkspaces(workspaceIds: List<Long>) {
        viewModelScope.launch {
            workspaceRepository?.reorderWorkspaces(workspaceIds)
        }
    }

    fun initiateDeleteWorkspace(workspace: Workspace) {
        if (workspace.kind == WorkspaceKind.HOME) return
        viewModelScope.launch {
            val repo = workspaceRepository ?: return@launch
            val stats = repo.getWorkspaceStats(workspace.id)
            _uiState.update {
                it.copy(
                    workspaceToDelete = workspace,
                    workspaceDeleteStats = stats,
                    workspaceDeleteStep = WorkspaceDeleteStep.CONFIRM
                )
            }
        }
    }

    fun dismissDeleteWorkspace() {
        _uiState.update {
            it.copy(
                workspaceToDelete = null,
                workspaceDeleteStep = WorkspaceDeleteStep.NONE,
                workspaceDeleteStats = null,
                workspaceDeleteProgress = null
            )
        }
    }

    fun proceedDeleteChoice() {
        _uiState.update { it.copy(workspaceDeleteStep = WorkspaceDeleteStep.CHOICE) }
    }

    fun proceedDeletePermanentConfirm() {
        _uiState.update { it.copy(workspaceDeleteStep = WorkspaceDeleteStep.PERMANENT_CONFIRM) }
    }

    fun deleteWorkspaceMoveFoldersToHome(workspace: Workspace) {
        viewModelScope.launch {
            val repo = workspaceRepository ?: return@launch
            when (repo.deleteWorkspaceMoveFoldersToHome(workspace.id)) {
                is WorkspaceResult.Success -> {
                    dismissDeleteWorkspace()
                    _uiState.update { it.copy(userMessage = "Workspace deleted. Folders moved to Home.") }
                }
                is WorkspaceResult.Error -> {
                    dismissDeleteWorkspace()
                    _uiState.update { it.copy(userMessage = "Failed to delete workspace") }
                }
            }
        }
    }

    fun executeDeleteWorkspaceContents(workspace: Workspace, permanent: Boolean) {
        val total = _uiState.value.workspaceDeleteStats?.folderCount ?: 0
        _uiState.update {
            it.copy(
                workspaceDeleteStep = WorkspaceDeleteStep.PROGRESS,
                workspaceDeleteProgress = Pair(0, total)
            )
        }
        viewModelScope.launch {
            val repo = workspaceRepository ?: return@launch
            when (val result = repo.deleteWorkspaceWithContents(workspace.id, permanent) { current, count ->
                _uiState.update { it.copy(workspaceDeleteProgress = Pair(current, count)) }
            }) {
                is WorkspaceResult.Success -> {
                    dismissDeleteWorkspace()
                    val msg = if (permanent) {
                        "Workspace deleted permanently."
                    } else {
                        "Workspace deleted. $total folders moved to Trash."
                    }
                    _uiState.update { it.copy(userMessage = msg) }
                }
                is WorkspaceResult.Error -> {
                    dismissDeleteWorkspace()
                    val err = result.error
                    val msg = if (err is WorkspaceError.DeletionInterrupted) {
                        "Could not finish deleting. ${err.remainingCount} folders remain."
                    } else {
                        "Failed to delete workspace contents"
                    }
                    _uiState.update { it.copy(userMessage = msg) }
                }
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
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
            folderRepository: FolderRepository,
            workspaceRepository: WorkspaceRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return NotesViewModel(
                    photoRepository = photoRepository,
                    documentRepository = documentRepository,
                    textNoteRepository = textNoteRepository,
                    canvasNoteRepository = canvasNoteRepository,
                    folderRepository = folderRepository,
                    workspaceRepository = workspaceRepository
                ) as T
            }
        }
    }
}
