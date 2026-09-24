// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.home

import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoGroup
import com.arinara.fotara.data.model.SearchDateFilter
import com.arinara.fotara.data.repository.FolderBulkDeleteResult

data class HomeUiState(
    val folders: List<Folder> = emptyList(),
    val photosAddedToday: List<Photo> = emptyList(),
    val photosDueTomorrow: List<Photo> = emptyList(),
    val isSearchActive: Boolean = false,
    val searchQuery: String = "",
    val isSearchLoading: Boolean = false,
    val folderSearchResults: List<Folder> = emptyList(),
    val groupSearchResults: List<PhotoGroup> = emptyList(),
    val searchResults: List<Photo> = emptyList(),
    val recentSearches: List<String> = emptyList(),
    val searchDateFilter: SearchDateFilter = SearchDateFilter.ALL,
    val searchColorFilter: String? = null,
    val availableSmartTags: List<String> = emptyList(),
    val selectedSmartTag: String? = null,
    val showNewFolderDialog: Boolean = false,
    val isLoading: Boolean = false,
    val userMessage: String? = null,
    val isMultiSelectMode: Boolean = false,
    val selectedFolderIds: Set<Long> = emptySet(),
    val showBulkDeleteDialog: Boolean = false,
    val bulkDeleteStats: FolderBulkDeleteResult? = null
)
