// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arinara.fotara.data.model.DateRange
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.LinkGroup
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.data.model.SearchDateFilter
import com.arinara.fotara.data.model.SearchSortOrder
import com.arinara.fotara.data.model.TextNote
import com.arinara.fotara.data.model.CanvasNote
import com.arinara.fotara.data.repository.DocumentRepository
import com.arinara.fotara.data.repository.FolderRepository
import com.arinara.fotara.data.repository.PhotoRepository
import com.arinara.fotara.data.repository.SettingsRepository
import com.arinara.fotara.data.repository.TextNoteRepository
import com.arinara.fotara.data.repository.CanvasNoteRepository
import com.arinara.fotara.util.DateRangeCalculator
import com.arinara.fotara.util.DeadlineNotificationManager
import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Calendar
import com.arinara.fotara.data.model.Workspace
import com.arinara.fotara.data.model.WorkspaceKind
import com.arinara.fotara.data.repository.WorkspaceRepository
import com.arinara.fotara.data.repository.WorkspaceValidator
import com.arinara.fotara.data.repository.WorkspaceResult
import com.arinara.fotara.data.repository.WorkspaceError
import com.arinara.fotara.data.db.FotaraDbHelper

private data class WorkspacesAndFolders(
    val workspaces: List<Workspace>,
    val selectedWorkspaceId: Long,
    val folders: List<Folder>,
    val addedToday: List<Photo>,
    val dueTomorrow: List<Photo>
)

class HomeViewModel(
    private val folderRepository: FolderRepository,
    private val photoRepository: PhotoRepository,
    private val deadlineNotificationManager: DeadlineNotificationManager? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val textNoteRepository: TextNoteRepository? = null,
    private val documentRepository: DocumentRepository? = null,
    private val canvasNoteRepository: CanvasNoteRepository? = null,
    private val workspaceRepository: WorkspaceRepository? = null
) : ViewModel() {

    private val _fallbackSelectedWorkspaceId = MutableStateFlow(1L)
    val selectedWorkspaceIdFlow: StateFlow<Long>
        get() = workspaceRepository?.selectedWorkspaceId ?: _fallbackSelectedWorkspaceId.asStateFlow()
    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()
    val selectedFolderMap = mutableStateMapOf<Long, Boolean>()

    private var allFoldersCache: List<Folder> = emptyList()
    private var searchDebounceJob: Job? = null

    init {
        loadData()
    }

    private fun loadData() {
        val workspacesFlow = workspaceRepository?.observeWorkspaces() ?: flowOf(
            listOf(
                Workspace(
                    id = 1L,
                    uuid = FotaraDbHelper.HOME_WORKSPACE_UUID,
                    kind = WorkspaceKind.HOME,
                    name = "",
                    position = 0
                ),
                Workspace(
                    id = 2L,
                    uuid = FotaraDbHelper.ARCHIVE_WORKSPACE_UUID,
                    kind = WorkspaceKind.ARCHIVE,
                    name = "",
                    position = 1
                )
            )
        )

        val workspaceSelectionFlow = combine(workspacesFlow, selectedWorkspaceIdFlow) { workspaces, selectedWsId ->
            val homeWs = workspaces.firstOrNull { it.kind == WorkspaceKind.HOME }
            val fallbackId = homeWs?.id ?: 1L
            val effectiveWsId = if (workspaces.any { it.id == selectedWsId }) {
                selectedWsId
            } else {
                fallbackId
            }
            if (effectiveWsId != selectedWsId) {
                if (workspaceRepository != null) {
                    workspaceRepository.selectWorkspace(effectiveWsId)
                } else {
                    _fallbackSelectedWorkspaceId.value = effectiveWsId
                }
            }
            Pair(workspaces, effectiveWsId)
        }

        viewModelScope.launch {
            combine(
                workspaceSelectionFlow,
                folderRepository.getFolders(),
                folderRepository.getFolderLinkGroups(),
                photoRepository.getPhotosAddedToday(),
                photoRepository.getPhotosDueTomorrow()
            ) { wsSelection, folders, linkGroups, addedToday, dueTomorrow ->
                allFoldersCache = folders
                val (workspaces, effectiveWsId) = wsSelection
                val workspaceFolders = folders.filter { it.workspaceId == effectiveWsId }
                val arranged = arrangeFoldersWithLinks(workspaceFolders, linkGroups)

                WorkspacesAndFolders(
                    workspaces = workspaces,
                    selectedWorkspaceId = effectiveWsId,
                    folders = arranged,
                    addedToday = addedToday,
                    dueTomorrow = dueTomorrow
                )
            }.collect { data ->
                _uiState.update { current ->
                    val currentScope = current.searchWorkspaceScopeId
                    val validScope = if (currentScope != null && data.workspaces.none { it.id == currentScope }) {
                        null
                    } else currentScope
                    current.copy(
                        workspaces = data.workspaces,
                        selectedWorkspaceId = data.selectedWorkspaceId,
                        searchWorkspaceScopeId = validScope,
                        folders = data.folders,
                        photosAddedToday = data.addedToday,
                        photosDueTomorrow = data.dueTomorrow,
                        isLoading = false
                    )
                }
            }
        }
        viewModelScope.launch {
            photoRepository.getAllSmartTags().collect { tags ->
                _uiState.update { it.copy(availableSmartTags = tags) }
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
                        workspaceRepository?.refresh()
                        photoRepository.refresh()
                        documentRepository?.refresh()
                        textNoteRepository?.refresh()
                        canvasNoteRepository?.refresh()
                    }
                }
            } catch (_: Exception) {
            } finally {
                isRefreshingData = false
                _uiState.update { it.copy(isRefreshing = false) }
            }
        }
    }

    fun activateSearch() {
        val recents = settingsRepository?.getRecentSearches() ?: emptyList()
        _uiState.update { it.copy(isSearchActive = true, recentSearches = recents, searchWorkspaceScopeId = null) }
    }

    fun setSearchWorkspaceScope(workspaceId: Long?) {
        _uiState.update { it.copy(searchWorkspaceScopeId = workspaceId) }
        executeSearch(
            _uiState.value.searchQuery,
            _uiState.value.searchDateFilter,
            _uiState.value.searchColorFilter,
            _uiState.value.selectedSmartTag
        )
    }

    fun deactivateSearch() {
        _uiState.update {
            it.copy(
                isSearchActive = false,
                searchQuery = "",
                folderSearchResults = emptyList(),
                groupSearchResults = emptyList(),
                searchResults = emptyList(),
                textNoteSearchResults = emptyList(),
                documentSearchResults = emptyList(),
                canvasNoteSearchResults = emptyList(),
                isSearchLoading = false,
                searchDateFilter = SearchDateFilter.ALL,
                searchColorFilter = null,
                selectedSmartTag = null,
                customDateRange = null
            )
        }
    }

    fun setDateFilter(filter: SearchDateFilter) {
        _uiState.update { it.copy(searchDateFilter = filter) }
        executeSearch(_uiState.value.searchQuery, filter, _uiState.value.searchColorFilter, _uiState.value.selectedSmartTag)
    }

    fun setSortOrder(order: SearchSortOrder) {
        _uiState.update { it.copy(searchSortOrder = order) }
        executeSearch(
            _uiState.value.searchQuery,
            _uiState.value.searchDateFilter,
            _uiState.value.searchColorFilter,
            _uiState.value.selectedSmartTag,
            _uiState.value.customDateRange,
            order
        )
    }

    fun setCustomDateRange(range: DateRange) {
        _uiState.update { it.copy(searchDateFilter = SearchDateFilter.CUSTOM_RANGE, customDateRange = range) }
        executeSearch(
            _uiState.value.searchQuery,
            SearchDateFilter.CUSTOM_RANGE,
            _uiState.value.searchColorFilter,
            _uiState.value.selectedSmartTag,
            range,
            _uiState.value.searchSortOrder
        )
    }

    fun setColorFilter(colorHex: String?) {
        val newColor = if (_uiState.value.searchColorFilter == colorHex) null else colorHex
        _uiState.update { it.copy(searchColorFilter = newColor) }
        executeSearch(_uiState.value.searchQuery, _uiState.value.searchDateFilter, newColor, _uiState.value.selectedSmartTag)
    }

    fun selectSmartTag(tag: String?) {
        val newTag = if (_uiState.value.selectedSmartTag == tag) null else tag
        _uiState.update { it.copy(selectedSmartTag = newTag) }
        executeSearch(_uiState.value.searchQuery, _uiState.value.searchDateFilter, _uiState.value.searchColorFilter, newTag)
    }

    fun clearFilters() {
        _uiState.update {
            it.copy(
                searchDateFilter = SearchDateFilter.ALL,
                searchColorFilter = null,
                selectedSmartTag = null
            )
        }
        executeSearch(_uiState.value.searchQuery, SearchDateFilter.ALL, null, null)
    }

    fun onSearchQueryChanged(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchDebounceJob?.cancel()
        searchDebounceJob = viewModelScope.launch {
            delay(250)
            executeSearch(query, _uiState.value.searchDateFilter, _uiState.value.searchColorFilter, _uiState.value.selectedSmartTag)
        }
    }

    fun submitSearch(query: String) {
        val trimmed = query.trim()
        searchDebounceJob?.cancel()
        executeSearch(query, _uiState.value.searchDateFilter, _uiState.value.searchColorFilter, _uiState.value.selectedSmartTag)
        if (trimmed.isNotBlank()) {
            viewModelScope.launch {
                settingsRepository?.addRecentSearch(trimmed)
                val recents = settingsRepository?.getRecentSearches() ?: emptyList()
                _uiState.update { it.copy(recentSearches = recents) }
            }
        }
    }

    fun removeRecentSearch(query: String) {
        viewModelScope.launch {
            settingsRepository?.removeRecentSearch(query)
            val recents = settingsRepository?.getRecentSearches() ?: emptyList()
            _uiState.update { it.copy(recentSearches = recents) }
        }
    }

    fun clearRecentSearches() {
        viewModelScope.launch {
            settingsRepository?.clearRecentSearches()
            _uiState.update { it.copy(recentSearches = emptyList()) }
        }
    }

    private fun executeSearch(
        query: String,
        dateFilter: SearchDateFilter,
        colorFilter: String?,
        smartTag: String? = _uiState.value.selectedSmartTag,
        customRange: DateRange? = _uiState.value.customDateRange,
        sortOrder: SearchSortOrder = _uiState.value.searchSortOrder
    ) {
        val trimmed = query.trim()
        val currentFolders = allFoldersCache.ifEmpty { _uiState.value.folders }
        val targetWorkspaceId = _uiState.value.searchWorkspaceScopeId
        val workspaceFolderIds = if (targetWorkspaceId != null) {
            currentFolders.filter { it.workspaceId == targetWorkspaceId }.map { it.id }.toSet()
        } else null
        val hasFilters = dateFilter != SearchDateFilter.ALL || colorFilter != null || smartTag != null

        val computedRange = DateRangeCalculator.calculateRange(
            filter = dateFilter,
            customRange = customRange ?: _uiState.value.customDateRange
        )
        val startTime = computedRange.startMs
        val endTime = computedRange.endMs

        fun <T> sortResults(
            items: List<T>,
            titleSelector: ((T) -> String)? = null,
            timeSelector: (T) -> Long
        ): List<T> {
            val q = trimmed.lowercase()
            val comparator = if (q.isNotBlank() && titleSelector != null) {
                compareBy<T> { item ->
                    val title = titleSelector(item).lowercase()
                    when {
                        title == q -> 0
                        title.startsWith(q) -> 1
                        title.contains(q) -> 2
                        else -> 3
                    }
                }
            } else {
                null
            }

            val timeComparator = if (sortOrder == SearchSortOrder.OLDEST_ADDED) {
                compareBy<T> { timeSelector(it) }
            } else {
                compareByDescending<T> { timeSelector(it) }
            }

            return if (comparator != null) {
                items.sortedWith(comparator.thenComparing(timeComparator))
            } else {
                items.sortedWith(timeComparator)
            }
        }

        val matchesTag: (Photo) -> Boolean = { photo ->
            if (smartTag == null) true
            else photo.getAllSmartTags().contains(smartTag.lowercase().removePrefix("#"))
        }

        val matchedFolders = if (trimmed.isNotBlank() || (hasFilters && smartTag == null)) {
            val list = currentFolders.filter { folder ->
                val matchesWorkspace = targetWorkspaceId == null || folder.workspaceId == targetWorkspaceId
                val matchesQuery = if (trimmed.isBlank()) true else folder.name.contains(trimmed, ignoreCase = true)
                val matchesDate = folder.createdAt in startTime..endTime
                val matchesColor = if (colorFilter == null) true else folder.colorLabel.equals(colorFilter, ignoreCase = true)
                matchesWorkspace && matchesQuery && matchesDate && matchesColor
            }
            sortResults(list, { it.name }, { it.createdAt })
        } else {
            emptyList()
        }

        _uiState.update {
            it.copy(
                folderSearchResults = matchedFolders,
                isSearchLoading = trimmed.isNotBlank() || hasFilters
            )
        }

        if (trimmed.isNotBlank()) {
            viewModelScope.launch {
                photoRepository.searchPhotos(trimmed).collect { results ->
                    val filtered = results.filter { photo ->
                        val matchesWorkspace = workspaceFolderIds == null || photo.folderId in workspaceFolderIds
                        val matchesDate = photo.addedAt in startTime..endTime
                        val matchesColor = if (colorFilter == null) true else photo.tagColor.equals(colorFilter, ignoreCase = true)
                        matchesWorkspace && matchesDate && matchesColor && matchesTag(photo)
                    }
                    _uiState.update {
                        it.copy(
                            searchResults = sortResults(filtered, { it.caption ?: "" }, { p -> p.addedAt }),
                            isSearchLoading = false
                        )
                    }
                }
            }
            viewModelScope.launch {
                photoRepository.searchGroups(trimmed).collect { groups ->
                    val filtered = if (smartTag != null) emptyList() else groups.filter { group ->
                        val matchesWorkspace = workspaceFolderIds == null || group.folderId in workspaceFolderIds
                        val matchesDate = group.addedAt in startTime..endTime
                        val matchesColor = if (colorFilter == null) true else group.tagColor.equals(colorFilter, ignoreCase = true)
                        matchesWorkspace && matchesDate && matchesColor
                    }
                    _uiState.update {
                        it.copy(groupSearchResults = sortResults(filtered, { it.name }, { g -> g.addedAt }))
                    }
                }
            }
            textNoteRepository?.let { repo ->
                viewModelScope.launch {
                    repo.searchNotes(trimmed).collect { notes ->
                        val filtered = if (smartTag != null) emptyList() else notes.filter { note ->
                            val matchesWorkspace = workspaceFolderIds == null || note.folderId in workspaceFolderIds
                            val matchesDate = note.addedAt in startTime..endTime
                            val matchesColor = if (colorFilter == null) true else note.tagColor.equals(colorFilter, ignoreCase = true)
                            matchesWorkspace && matchesDate && matchesColor
                        }
                        _uiState.update {
                            it.copy(textNoteSearchResults = sortResults(filtered, { it.title }, { n -> n.addedAt }))
                        }
                    }
                }
            }
            documentRepository?.let { repo ->
                viewModelScope.launch {
                    repo.searchDocuments(trimmed).collect { docs ->
                        val filtered = if (smartTag != null) emptyList() else docs.filter { doc ->
                            val matchesWorkspace = workspaceFolderIds == null || doc.folderId in workspaceFolderIds
                            val matchesDate = doc.addedAt in startTime..endTime
                            val matchesColor = if (colorFilter == null) true else doc.tagColor.equals(colorFilter, ignoreCase = true)
                            matchesWorkspace && matchesDate && matchesColor
                        }
                        _uiState.update {
                            it.copy(documentSearchResults = sortResults(filtered, { it.name }, { d -> d.addedAt }))
                        }
                    }
                }
            }
            canvasNoteRepository?.let { repo ->
                viewModelScope.launch {
                    repo.searchCanvasNotes(trimmed).collect { canvases ->
                        val filtered = if (smartTag != null) emptyList() else canvases.filter { c ->
                            val matchesWorkspace = workspaceFolderIds == null || c.folderId in workspaceFolderIds
                            val matchesDate = c.addedAt in startTime..endTime
                            val matchesColor = if (colorFilter == null) true else c.tagColor.equals(colorFilter, ignoreCase = true)
                            matchesWorkspace && matchesDate && matchesColor
                        }
                        _uiState.update {
                            it.copy(canvasNoteSearchResults = sortResults(filtered, { it.title }, { c -> c.addedAt }))
                        }
                    }
                }
            }
        } else if (hasFilters) {
            viewModelScope.launch {
                val basePhotos = if (smartTag != null) {
                    photoRepository.getPhotosByTag(smartTag).firstOrNull() ?: emptyList()
                } else {
                    photoRepository.getAllActivePhotos()
                }
                val filtered = basePhotos.filter { photo ->
                    val matchesWorkspace = workspaceFolderIds == null || photo.folderId in workspaceFolderIds
                    val matchesDate = photo.addedAt in startTime..endTime
                    val matchesColor = if (colorFilter == null) true else photo.tagColor.equals(colorFilter, ignoreCase = true)
                    matchesWorkspace && matchesDate && matchesColor && matchesTag(photo)
                }
                _uiState.update {
                    it.copy(
                        searchResults = sortResults(filtered) { p -> p.addedAt },
                        isSearchLoading = false
                    )
                }
            }
            viewModelScope.launch {
                if (smartTag != null) {
                    _uiState.update { it.copy(groupSearchResults = emptyList()) }
                } else {
                    photoRepository.getAllActiveGroups().collect { groups ->
                        val filtered = groups.filter { group ->
                            val matchesWorkspace = workspaceFolderIds == null || group.folderId in workspaceFolderIds
                            val matchesDate = group.addedAt in startTime..endTime
                            val matchesColor = if (colorFilter == null) true else group.tagColor.equals(colorFilter, ignoreCase = true)
                            matchesWorkspace && matchesDate && matchesColor
                        }
                        _uiState.update {
                            it.copy(groupSearchResults = sortResults(filtered) { g -> g.addedAt })
                        }
                    }
                }
            }
            textNoteRepository?.let { repo ->
                viewModelScope.launch {
                    if (smartTag != null) {
                        _uiState.update { it.copy(textNoteSearchResults = emptyList()) }
                    } else {
                        repo.getAllActiveTextNotes().collect { notes ->
                            val filtered = notes.filter { note ->
                                val matchesWorkspace = workspaceFolderIds == null || note.folderId in workspaceFolderIds
                                val matchesDate = note.addedAt in startTime..endTime
                                val matchesColor = if (colorFilter == null) true else note.tagColor.equals(colorFilter, ignoreCase = true)
                                matchesWorkspace && matchesDate && matchesColor
                            }
                            _uiState.update {
                                it.copy(textNoteSearchResults = sortResults(filtered) { n -> n.addedAt })
                            }
                        }
                    }
                }
            }
            documentRepository?.let { repo ->
                viewModelScope.launch {
                    if (smartTag != null) {
                        _uiState.update { it.copy(documentSearchResults = emptyList()) }
                    } else {
                        repo.getAllActiveDocumentNotes().collect { docs ->
                            val filtered = docs.filter { doc ->
                                val matchesWorkspace = workspaceFolderIds == null || doc.folderId in workspaceFolderIds
                                val matchesDate = doc.addedAt in startTime..endTime
                                val matchesColor = if (colorFilter == null) true else doc.tagColor.equals(colorFilter, ignoreCase = true)
                                matchesWorkspace && matchesDate && matchesColor
                            }
                            _uiState.update {
                                it.copy(documentSearchResults = sortResults(filtered) { d -> d.addedAt })
                            }
                        }
                    }
                }
            }
            canvasNoteRepository?.let { repo ->
                viewModelScope.launch {
                    if (smartTag != null) {
                        _uiState.update { it.copy(canvasNoteSearchResults = emptyList()) }
                    } else {
                        repo.getAllActiveCanvasNotes().collect { canvases ->
                            val filtered = canvases.filter { c ->
                                val matchesWorkspace = workspaceFolderIds == null || c.folderId in workspaceFolderIds
                                val matchesDate = c.addedAt in startTime..endTime
                                val matchesColor = if (colorFilter == null) true else c.tagColor.equals(colorFilter, ignoreCase = true)
                                matchesWorkspace && matchesDate && matchesColor
                            }
                            _uiState.update {
                                it.copy(canvasNoteSearchResults = sortResults(filtered) { c -> c.addedAt })
                            }
                        }
                    }
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    groupSearchResults = emptyList(),
                    textNoteSearchResults = emptyList(),
                    documentSearchResults = emptyList(),
                    canvasNoteSearchResults = emptyList(),
                    isSearchLoading = false
                )
            }
        }
    }

    fun onGroupSearchResultClicked(
        group: PhotoGroup,
        onNavigate: (folderId: Long, subfolderId: Long?, groupId: Long) -> Unit
    ) {
        if (_uiState.value.searchQuery.isNotBlank()) {
            submitSearch(_uiState.value.searchQuery)
        }
        onNavigate(group.folderId, group.subfolderId, group.id)
    }

    fun onPhotoSearchResultClicked(
        photo: Photo,
        onNavigate: (folderId: Long, subfolderId: Long?, photoId: Long) -> Unit
    ) {
        if (_uiState.value.searchQuery.isNotBlank()) {
            submitSearch(_uiState.value.searchQuery)
        }
        viewModelScope.launch {
            val freshPhoto = photoRepository.getPhotoById(photo.id)
            val parentFolder = folderRepository.getFolderById(photo.folderId).firstOrNull()
            if (freshPhoto != null && !freshPhoto.isTrashed && parentFolder != null && !parentFolder.isTrashed) {
                deactivateSearch()
                onNavigate(photo.folderId, freshPhoto.subfolderId, photo.id)
            } else {
                _uiState.update { it.copy(userMessage = "Photo is no longer in this folder") }
                executeSearch(_uiState.value.searchQuery, _uiState.value.searchDateFilter, _uiState.value.searchColorFilter)
            }
        }
    }

    fun onDocumentSearchResultClicked(
        doc: DocumentNote,
        onNavigate: (folderId: Long, subfolderId: Long?, docId: Long, targetPageIndex: Int?) -> Unit
    ) {
        val query = _uiState.value.searchQuery
        if (query.isNotBlank()) {
            submitSearch(query)
        }
        viewModelScope.launch {
            val freshDoc = documentRepository?.getDocumentNoteById(doc.id)
            val parentFolder = folderRepository.getFolderById(doc.folderId).firstOrNull()
            if (freshDoc != null && !freshDoc.isTrashed && parentFolder != null && !parentFolder.isTrashed) {
                val targetPageIndex = if (freshDoc.docType == com.arinara.fotara.data.model.DocumentType.PDF) {
                    val pages = documentRepository.getDocumentPages(freshDoc.id).firstOrNull() ?: emptyList()
                    com.arinara.fotara.util.PdfPageMatcher.findMatchingPageIndex(pages, query)
                } else null
                deactivateSearch()
                onNavigate(doc.folderId, freshDoc.subfolderId, doc.id, targetPageIndex)
            } else {
                _uiState.update { it.copy(userMessage = "Document is no longer in this folder") }
                executeSearch(_uiState.value.searchQuery, _uiState.value.searchDateFilter, _uiState.value.searchColorFilter)
            }
        }
    }

    fun onTextNoteSearchResultClicked(
        note: TextNote,
        onNavigate: (folderId: Long, subfolderId: Long?, noteId: Long) -> Unit
    ) {
        if (_uiState.value.searchQuery.isNotBlank()) {
            submitSearch(_uiState.value.searchQuery)
        }
        viewModelScope.launch {
            val freshNote = textNoteRepository?.getTextNoteByIdOnce(note.id)
            val parentFolder = folderRepository.getFolderById(note.folderId).firstOrNull()
            if (freshNote != null && !freshNote.isTrashed && parentFolder != null && !parentFolder.isTrashed) {
                deactivateSearch()
                onNavigate(note.folderId, freshNote.subfolderId, note.id)
            } else {
                _uiState.update { it.copy(userMessage = "Note is no longer in this folder") }
                executeSearch(_uiState.value.searchQuery, _uiState.value.searchDateFilter, _uiState.value.searchColorFilter)
            }
        }
    }

    fun onCanvasSearchResultClicked(
        canvas: CanvasNote,
        onNavigate: (folderId: Long, subfolderId: Long?, canvasId: Long) -> Unit
    ) {
        if (_uiState.value.searchQuery.isNotBlank()) {
            submitSearch(_uiState.value.searchQuery)
        }
        viewModelScope.launch {
            val freshCanvas = canvasNoteRepository?.getCanvasNoteByIdOnce(canvas.id)
            val parentFolder = folderRepository.getFolderById(canvas.folderId).firstOrNull()
            if (freshCanvas != null && !freshCanvas.isTrashed && parentFolder != null && !parentFolder.isTrashed) {
                deactivateSearch()
                onNavigate(canvas.folderId, freshCanvas.subfolderId, canvas.id)
            } else {
                _uiState.update { it.copy(userMessage = "Canvas is no longer in this folder") }
                executeSearch(_uiState.value.searchQuery, _uiState.value.searchDateFilter, _uiState.value.searchColorFilter)
            }
        }
    }

    fun renameFolder(folderId: Long, newName: String) {
        val trimmed = newName.trim()
        if (trimmed.isBlank()) {
            _uiState.update { it.copy(userMessage = "Folder name cannot be empty") }
            return
        }
        viewModelScope.launch {
            try {
                folderRepository.renameFolder(folderId, trimmed)
                _uiState.update { it.copy(userMessage = "Renamed to \"$trimmed\"") }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Failed to rename folder: ${e.message}") }
            }
        }
    }

    fun togglePinFolder(folderId: Long) {
        viewModelScope.launch {
            try {
                folderRepository.togglePin(folderId)
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Failed to toggle pin: ${e.message}") }
            }
        }
    }

    fun updateFolderColor(folderId: Long, colorHex: String) {
        viewModelScope.launch {
            try {
                folderRepository.updateFolderColor(folderId, colorHex)
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Failed to update color: ${e.message}") }
            }
        }
    }

    fun lockFolder(folderId: Long, pin: String) {
        viewModelScope.launch {
            try {
                folderRepository.lockFolder(folderId, pin)
                _uiState.update { it.copy(userMessage = "Folder locked with PIN") }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Failed to lock folder: ${e.message}") }
            }
        }
    }

    fun unlockFolder(folderId: Long) {
        viewModelScope.launch {
            try {
                folderRepository.unlockFolder(folderId)
                _uiState.update { it.copy(userMessage = "Folder lock removed") }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Failed to unlock folder: ${e.message}") }
            }
        }
    }

    fun updateFolderPin(folderId: Long, newPin: String) {
        viewModelScope.launch {
            try {
                folderRepository.updateFolderPin(folderId, newPin)
                _uiState.update { it.copy(userMessage = "Folder PIN updated") }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Failed to update PIN: ${e.message}") }
            }
        }
    }

    // --- Multi-Select & Bulk Delete ---

    fun enterMultiSelectMode(initialFolderId: Long? = null) {
        selectedFolderMap.clear()
        if (initialFolderId != null) {
            selectedFolderMap[initialFolderId] = true
        }
        _uiState.update {
            it.copy(
                isMultiSelectMode = true,
                selectedFolderIds = if (initialFolderId != null) setOf(initialFolderId) else emptySet()
            )
        }
    }

    fun toggleFolderSelection(folderId: Long) {
        val currentlySelected = selectedFolderMap[folderId] == true
        if (currentlySelected) {
            selectedFolderMap.remove(folderId)
        } else {
            selectedFolderMap[folderId] = true
        }
        val updated = selectedFolderMap.keys.toSet()
        if (updated.isEmpty()) {
            _uiState.update { it.copy(isMultiSelectMode = false, selectedFolderIds = emptySet()) }
        } else {
            _uiState.update { it.copy(isMultiSelectMode = true, selectedFolderIds = updated) }
        }
    }

    fun selectAllFolders() {
        val allIds = _uiState.value.folders.map { it.id }.toSet()
        selectedFolderMap.clear()
        allIds.forEach { selectedFolderMap[it] = true }
        _uiState.update { it.copy(isMultiSelectMode = allIds.isNotEmpty(), selectedFolderIds = allIds) }
    }

    fun invertFolderSelection() {
        val allIds = _uiState.value.folders.map { it.id }.toSet()
        val currentSelected = selectedFolderMap.keys.toSet()
        val inverted = allIds - currentSelected
        selectedFolderMap.clear()
        inverted.forEach { selectedFolderMap[it] = true }
        if (inverted.isEmpty()) {
            _uiState.update { it.copy(isMultiSelectMode = false, selectedFolderIds = emptySet()) }
        } else {
            _uiState.update { it.copy(isMultiSelectMode = true, selectedFolderIds = inverted) }
        }
    }

    fun exitMultiSelectMode() {
        selectedFolderMap.clear()
        _uiState.update {
            it.copy(
                isMultiSelectMode = false,
                selectedFolderIds = emptySet(),
                showBulkDeleteDialog = false,
                bulkDeleteStats = null
            )
        }
    }

    fun requestBulkDelete() {
        val selectedIds = _uiState.value.selectedFolderIds.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            val stats = folderRepository.getBulkDeleteStats(selectedIds)
            _uiState.update {
                it.copy(
                    bulkDeleteStats = stats,
                    showBulkDeleteDialog = true
                )
            }
        }
    }

    fun dismissBulkDeleteDialog() {
        _uiState.update { it.copy(showBulkDeleteDialog = false, bulkDeleteStats = null) }
    }

    fun confirmBulkDelete() {
        val selectedIds = _uiState.value.selectedFolderIds.toList()
        if (selectedIds.isEmpty()) return

        viewModelScope.launch {
            try {
                // Cancel pending deadline reminders for all photos in these folders
                deadlineNotificationManager?.let { mgr ->
                    for (folderId in selectedIds) {
                        // Alarms are cleaned up per photo
                    }
                }

                val result = folderRepository.deleteFolders(selectedIds)
                val formattedMb = "%.1f MB".format(result.totalSizeBytes / (1024f * 1024f))

                selectedFolderMap.clear()
                _uiState.update {
                    it.copy(
                        isMultiSelectMode = false,
                        selectedFolderIds = emptySet(),
                        showBulkDeleteDialog = false,
                        bulkDeleteStats = null,
                        userMessage = "Deleted ${result.folderCount} folder${if (result.folderCount > 1) "s" else ""} ($formattedMb freed)"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        showBulkDeleteDialog = false,
                        userMessage = "Failed to delete folders: ${e.message}"
                    )
                }
            }
        }
    }

    fun deleteFolder(folderId: Long) {
        viewModelScope.launch {
            try {
                folderRepository.deleteFolders(listOf(folderId))
                _uiState.update { it.copy(userMessage = "Folder moved to Trash") }
            } catch (e: Exception) {
                _uiState.update { it.copy(userMessage = "Failed to delete folder: ${e.message}") }
            }
        }
    }

    fun openNewFolderDialog() {
        _uiState.update { it.copy(showNewFolderDialog = true) }
    }

    fun closeNewFolderDialog() {
        _uiState.update { it.copy(showNewFolderDialog = false) }
    }

    fun createFolder(name: String, colorHex: String, isPinned: Boolean) {
        viewModelScope.launch {
            try {
                val currentWorkspaceId = selectedWorkspaceIdFlow.value
                folderRepository.createFolder(
                    name = name,
                    colorLabel = colorHex,
                    isPinned = isPinned,
                    workspaceId = currentWorkspaceId
                )
                _uiState.update {
                    it.copy(
                        showNewFolderDialog = false,
                        userMessage = "Subject \"$name\" created"
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(userMessage = "Failed to create folder: ${e.message}")
                }
            }
        }
    }

    fun linkSelectedFolders() {
        val selectedIds = _uiState.value.selectedFolderIds.toList()
        if (selectedIds.size !in 2..4) {
            _uiState.update { it.copy(userMessage = "Select between 2 and 4 folders to link") }
            return
        }
        viewModelScope.launch {
            folderRepository.createFolderLinkGroup(selectedIds)
            exitMultiSelectMode()
            _uiState.update { it.copy(userMessage = "Folders linked") }
        }
    }

    fun unlinkFolder(folderId: Long) {
        viewModelScope.launch {
            folderRepository.unlinkFolder(folderId)
            _uiState.update { it.copy(userMessage = "Folder unlinked") }
        }
    }

    fun batchRenameFolders(baseName: String) {
        if (baseName.isBlank()) return
        val selectedIds = _uiState.value.selectedFolderIds
        if (selectedIds.isEmpty()) return
        viewModelScope.launch {
            val selectedFolders = _uiState.value.folders.filter { it.id in selectedIds }
            if (selectedFolders.size == 1) {
                folderRepository.renameFolder(selectedFolders.first().id, baseName.trim())
            } else {
                var index = 1
                for (folder in selectedFolders) {
                    folderRepository.renameFolder(folder.id, "${baseName.trim()} $index")
                    index++
                }
            }
            exitMultiSelectMode()
            _uiState.update { it.copy(userMessage = "Renamed ${selectedFolders.size} folder${if (selectedFolders.size > 1) "s" else ""}") }
        }
    }

    private fun arrangeFoldersWithLinks(rawFolders: List<Folder>, linkGroups: List<LinkGroup>): List<Folder> {
        if (rawFolders.isEmpty()) return emptyList()
        val linkGroupMap = linkGroups.associateBy { it.id }
        val folderMap = rawFolders.associateBy { it.id }

        // Pinned folder rule: If any folder in a link is pinned, the entire link block moves to the pinned section
        val isEffectivelyPinned = { folder: Folder ->
            if (folder.isPinned) true
            else {
                val lg = folder.linkGroupId?.let { linkGroupMap[it] }
                lg?.memberIds?.any { mId -> folderMap[mId]?.isPinned == true } ?: false
            }
        }

        val pinnedFolders = rawFolders.filter { isEffectivelyPinned(it) }
        val unpinnedFolders = rawFolders.filter { !isEffectivelyPinned(it) }

        fun arrangeSection(sectionFolders: List<Folder>): List<Folder> {
            val result = mutableListOf<Folder>()
            val visitedFolderIds = mutableSetOf<Long>()
            val visitedLinkGroupIds = mutableSetOf<Long>()

            for (folder in sectionFolders) {
                if (visitedFolderIds.contains(folder.id)) continue
                val lgId = folder.linkGroupId
                val lg = lgId?.let { linkGroupMap[it] }

                if (lg != null && !visitedLinkGroupIds.contains(lg.id)) {
                    visitedLinkGroupIds.add(lg.id)
                    // Add all members of this link group in order of lg.memberIds
                    for (mId in lg.memberIds) {
                        val memberFolder = folderMap[mId]
                        if (memberFolder != null && !visitedFolderIds.contains(mId)) {
                            result.add(memberFolder)
                            visitedFolderIds.add(mId)
                        }
                    }
                } else if (lg == null) {
                    result.add(folder)
                    visitedFolderIds.add(folder.id)
                }
            }
            return result
        }

        return arrangeSection(pinnedFolders) + arrangeSection(unpinnedFolders)
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    // --- Workspace Management (Phase 27 - Batch 2A) ---

    fun selectWorkspace(workspaceId: Long) {
        if (workspaceRepository != null) {
            workspaceRepository.selectWorkspace(workspaceId)
        } else {
            _fallbackSelectedWorkspaceId.value = workspaceId
        }
        selectedFolderMap.clear()
        _uiState.update { current ->
            current.copy(
                selectedWorkspaceId = workspaceId,
                selectedFolderIds = emptySet()
            )
        }
    }

    fun openMoveFoldersDialog(folders: List<Folder>) {
        _uiState.update { it.copy(foldersToMoveWorkspace = folders) }
    }

    fun closeMoveFoldersDialog() {
        _uiState.update { it.copy(foldersToMoveWorkspace = null) }
    }

    fun moveFoldersToWorkspace(folderIds: List<Long>, targetWorkspaceId: Long) {
        viewModelScope.launch {
            val repo = workspaceRepository ?: return@launch
            when (val result = repo.moveFolders(folderIds, targetWorkspaceId)) {
                is WorkspaceResult.Success -> {
                    closeMoveFoldersDialog()
                    exitMultiSelectMode()
                    val target = _uiState.value.workspaces.firstOrNull { it.id == targetWorkspaceId }
                    val targetName = when (target?.kind) {
                        WorkspaceKind.HOME -> "Home"
                        WorkspaceKind.ARCHIVE -> "Archive"
                        WorkspaceKind.CUSTOM -> target.name
                        null -> "Workspace"
                    }
                    val msg = if (folderIds.size == 1) {
                        "Moved to $targetName"
                    } else {
                        "Moved ${folderIds.size} folders to $targetName"
                    }
                    _uiState.update { it.copy(userMessage = msg) }
                }
                is WorkspaceResult.Error -> {
                    _uiState.update { it.copy(userMessage = "Failed to move folders") }
                }
            }
        }
    }

    fun initiateDeleteWorkspace(workspace: Workspace) {
        _uiState.update {
            it.copy(
                workspaceToDelete = workspace,
                workspaceDeleteStep = WorkspaceDeleteStep.CONFIRM,
                workspaceDeleteStats = null
            )
        }
        viewModelScope.launch {
            val repo = workspaceRepository ?: return@launch
            val stats = repo.getWorkspaceStats(workspace.id)
            _uiState.update { it.copy(workspaceDeleteStats = stats) }
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
            when (val result = repo.deleteWorkspaceMoveFoldersToHome(workspace.id)) {
                is WorkspaceResult.Success -> {
                    if (selectedWorkspaceIdFlow.value == workspace.id) {
                        selectWorkspace(1L)
                    }
                    if (_uiState.value.searchWorkspaceScopeId == workspace.id) {
                        _uiState.update { it.copy(searchWorkspaceScopeId = null) }
                    }
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
                    if (selectedWorkspaceIdFlow.value == workspace.id) {
                        selectWorkspace(1L)
                    }
                    if (_uiState.value.searchWorkspaceScopeId == workspace.id) {
                        _uiState.update { it.copy(searchWorkspaceScopeId = null) }
                    }
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

    fun openRenameWorkspaceDialog(workspace: Workspace) {
        _uiState.update { it.copy(workspaceToRename = workspace) }
    }

    fun closeRenameWorkspaceDialog() {
        _uiState.update { it.copy(workspaceToRename = null) }
    }

    fun createWorkspace(name: String, iconKey: String? = null, onCreated: ((Long) -> Unit)? = null) {
        viewModelScope.launch {
            val repo = workspaceRepository ?: return@launch
            when (val result = repo.createWorkspace(name, iconKey)) {
                is WorkspaceResult.Success -> {
                    selectWorkspace(result.data.id)
                    closeAddWorkspaceDialog()
                    onCreated?.invoke(result.data.id)
                }
                is WorkspaceResult.Error -> {
                    val msg = when (result.error) {
                        WorkspaceError.LimitReached -> "You can have up to 10 workspaces."
                        WorkspaceError.NameEmpty -> "Workspace name cannot be empty"
                        WorkspaceError.NameTooLong -> "Name must be between 1 and 20 characters"
                        WorkspaceError.NameDuplicate -> "A workspace with this name already exists"
                        WorkspaceError.NameReserved -> "Name is reserved for built-in workspaces"
                        else -> "Failed to create workspace"
                    }
                    _uiState.update { it.copy(userMessage = msg) }
                }
            }
        }
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
                        WorkspaceError.NameEmpty -> "Workspace name cannot be empty"
                        WorkspaceError.NameTooLong -> "Name must be between 1 and 20 characters"
                        WorkspaceError.NameDuplicate -> "A workspace with this name already exists"
                        WorkspaceError.NameReserved -> "Name is reserved for built-in workspaces"
                        else -> "Failed to update workspace"
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

    companion object {
        fun provideFactory(
            folderRepository: FolderRepository,
            photoRepository: PhotoRepository,
            deadlineNotificationManager: DeadlineNotificationManager? = null,
            settingsRepository: SettingsRepository? = null,
            textNoteRepository: TextNoteRepository? = null,
            documentRepository: DocumentRepository? = null,
            canvasNoteRepository: CanvasNoteRepository? = null,
            workspaceRepository: WorkspaceRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(
                    folderRepository,
                    photoRepository,
                    deadlineNotificationManager,
                    settingsRepository,
                    textNoteRepository,
                    documentRepository,
                    canvasNoteRepository,
                    workspaceRepository
                ) as T
            }
        }
    }
}
