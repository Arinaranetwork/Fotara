// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.data.model.SearchDateFilter
import com.arinara.fotara.data.repository.FolderRepository
import com.arinara.fotara.data.repository.PhotoRepository
import com.arinara.fotara.data.repository.SettingsRepository
import com.arinara.fotara.util.DeadlineNotificationManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar

class HomeViewModel(
    private val folderRepository: FolderRepository,
    private val photoRepository: PhotoRepository,
    private val deadlineNotificationManager: DeadlineNotificationManager? = null,
    private val settingsRepository: SettingsRepository? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }

    private fun loadData() {
        viewModelScope.launch {
            combine(
                folderRepository.getFolders(),
                photoRepository.getPhotosAddedToday(),
                photoRepository.getPhotosDueTomorrow()
            ) { folders, addedToday, dueTomorrow ->
                Triple(folders, addedToday, dueTomorrow)
            }.collect { (folders, addedToday, dueTomorrow) ->
                _uiState.update { current ->
                    current.copy(
                        folders = folders,
                        photosAddedToday = addedToday,
                        photosDueTomorrow = dueTomorrow,
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

    fun activateSearch() {
        val recents = settingsRepository?.getRecentSearches() ?: emptyList()
        _uiState.update { it.copy(isSearchActive = true, recentSearches = recents) }
    }

    fun deactivateSearch() {
        _uiState.update {
            it.copy(
                isSearchActive = false,
                searchQuery = "",
                folderSearchResults = emptyList(),
                groupSearchResults = emptyList(),
                searchResults = emptyList(),
                isSearchLoading = false,
                searchDateFilter = SearchDateFilter.ALL,
                searchColorFilter = null,
                selectedSmartTag = null
            )
        }
    }

    fun setDateFilter(filter: SearchDateFilter) {
        _uiState.update { it.copy(searchDateFilter = filter) }
        executeSearch(_uiState.value.searchQuery, filter, _uiState.value.searchColorFilter, _uiState.value.selectedSmartTag)
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
        executeSearch(query, _uiState.value.searchDateFilter, _uiState.value.searchColorFilter, _uiState.value.selectedSmartTag)
    }

    fun submitSearch(query: String) {
        val trimmed = query.trim()
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
        smartTag: String? = _uiState.value.selectedSmartTag
    ) {
        val trimmed = query.trim()
        val currentFolders = _uiState.value.folders
        val hasFilters = dateFilter != SearchDateFilter.ALL || colorFilter != null || smartTag != null

        val now = System.currentTimeMillis()
        val dateThreshold = when (dateFilter) {
            SearchDateFilter.ALL -> 0L
            SearchDateFilter.TODAY -> {
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                cal.timeInMillis
            }
            SearchDateFilter.THIS_WEEK -> now - (7L * 24 * 60 * 60 * 1000L)
            SearchDateFilter.THIS_MONTH -> now - (30L * 24 * 60 * 60 * 1000L)
        }

        val matchesTag: (Photo) -> Boolean = { photo ->
            if (smartTag == null) true
            else photo.getAllSmartTags().contains(smartTag.lowercase().removePrefix("#"))
        }

        val matchedFolders = if (trimmed.isNotBlank() || (hasFilters && smartTag == null)) {
            currentFolders.filter { folder ->
                val matchesQuery = if (trimmed.isBlank()) true else folder.name.contains(trimmed, ignoreCase = true)
                val matchesDate = folder.createdAt >= dateThreshold
                val matchesColor = if (colorFilter == null) true else folder.colorLabel.equals(colorFilter, ignoreCase = true)
                matchesQuery && matchesDate && matchesColor
            }
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
                        val matchesDate = photo.createdAt >= dateThreshold
                        val matchesColor = if (colorFilter == null) true else photo.tagColor.equals(colorFilter, ignoreCase = true)
                        matchesDate && matchesColor && matchesTag(photo)
                    }
                    _uiState.update {
                        it.copy(
                            searchResults = filtered,
                            isSearchLoading = false
                        )
                    }
                }
            }
            viewModelScope.launch {
                photoRepository.searchGroups(trimmed).collect { groups ->
                    val filtered = if (smartTag != null) emptyList() else groups.filter { group ->
                        val matchesDate = group.createdAt >= dateThreshold
                        val matchesColor = if (colorFilter == null) true else group.tagColor.equals(colorFilter, ignoreCase = true)
                        matchesDate && matchesColor
                    }
                    _uiState.update {
                        it.copy(groupSearchResults = filtered)
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
                    val matchesDate = photo.createdAt >= dateThreshold
                    val matchesColor = if (colorFilter == null) true else photo.tagColor.equals(colorFilter, ignoreCase = true)
                    matchesDate && matchesColor && matchesTag(photo)
                }
                _uiState.update {
                    it.copy(
                        searchResults = filtered,
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
                            val matchesDate = group.createdAt >= dateThreshold
                            val matchesColor = if (colorFilter == null) true else group.tagColor.equals(colorFilter, ignoreCase = true)
                            matchesDate && matchesColor
                        }
                        _uiState.update {
                            it.copy(groupSearchResults = filtered)
                        }
                    }
                }
            }
        } else {
            _uiState.update {
                it.copy(
                    searchResults = emptyList(),
                    groupSearchResults = emptyList(),
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

    fun enterMultiSelectMode(initialFolderId: Long) {
        _uiState.update {
            it.copy(
                isMultiSelectMode = true,
                selectedFolderIds = setOf(initialFolderId)
            )
        }
    }

    fun toggleFolderSelection(folderId: Long) {
        _uiState.update { current ->
            val updated = current.selectedFolderIds.toMutableSet()
            if (updated.contains(folderId)) {
                updated.remove(folderId)
            } else {
                updated.add(folderId)
            }
            if (updated.isEmpty()) {
                current.copy(isMultiSelectMode = false, selectedFolderIds = emptySet())
            } else {
                current.copy(selectedFolderIds = updated)
            }
        }
    }

    fun selectAllFolders() {
        val allIds = _uiState.value.folders.map { it.id }.toSet()
        _uiState.update { it.copy(selectedFolderIds = allIds) }
    }

    fun exitMultiSelectMode() {
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

    fun openNewFolderDialog() {
        _uiState.update { it.copy(showNewFolderDialog = true) }
    }

    fun closeNewFolderDialog() {
        _uiState.update { it.copy(showNewFolderDialog = false) }
    }

    fun createFolder(name: String, colorHex: String, isPinned: Boolean) {
        viewModelScope.launch {
            try {
                folderRepository.createFolder(name = name, colorLabel = colorHex, isPinned = isPinned)
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

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    companion object {
        fun provideFactory(
            folderRepository: FolderRepository,
            photoRepository: PhotoRepository,
            deadlineNotificationManager: DeadlineNotificationManager? = null,
            settingsRepository: SettingsRepository? = null
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return HomeViewModel(folderRepository, photoRepository, deadlineNotificationManager, settingsRepository) as T
            }
        }
    }
}
