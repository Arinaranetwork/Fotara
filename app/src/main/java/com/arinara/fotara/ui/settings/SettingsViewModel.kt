// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arinara.fotara.data.model.DownsampleQuality
import com.arinara.fotara.data.model.SortOrder
import com.arinara.fotara.data.model.StorageLocation
import com.arinara.fotara.data.model.ThemeMode
import com.arinara.fotara.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(userSettings = settings) }
            }
        }
        refreshStorageBreakdown()
    }

    fun refreshStorageBreakdown() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingStorage = true) }
            try {
                val breakdown = settingsRepository.getStorageBreakdown()
                _uiState.update { it.copy(storageBreakdown = breakdown, isLoadingStorage = false) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoadingStorage = false) }
            }
        }
    }

    fun updateSortOrder(sortOrder: SortOrder) {
        viewModelScope.launch {
            settingsRepository.updateSortOrder(sortOrder)
        }
    }

    fun updateGridDensity(density: Int) {
        viewModelScope.launch {
            settingsRepository.updateGridDensity(density)
        }
    }

    fun updateThemeMode(themeMode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.updateThemeMode(themeMode)
        }
    }

    fun updateAutoOcr(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateAutoOcr(enabled)
        }
    }

    fun updateOcrLanguage(language: String) {
        viewModelScope.launch {
            settingsRepository.updateOcrLanguage(language)
        }
    }

    fun updateDownsampleQuality(quality: DownsampleQuality) {
        viewModelScope.launch {
            settingsRepository.updateDownsampleQuality(quality)
        }
    }

    fun updateReminderLeadTime(hours: Int) {
        viewModelScope.launch {
            settingsRepository.updateReminderLeadTime(hours)
        }
    }

    fun updateDueTomorrowRibbon(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDueTomorrowRibbon(enabled)
        }
    }

    fun updateStorageLocation(location: StorageLocation) {
        viewModelScope.launch {
            settingsRepository.updateStorageLocation(location)
        }
    }

    fun rebuildThumbnails() {
        if (_uiState.value.isRebuildingThumbnails) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRebuildingThumbnails = true) }
            try {
                val count = settingsRepository.rebuildThumbnails()
                refreshStorageBreakdown()
                _uiState.update {
                    it.copy(
                        isRebuildingThumbnails = false,
                        feedbackMessage = "Thumbnails rebuilt successfully ($count notes updated)."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRebuildingThumbnails = false,
                        feedbackMessage = "Failed to rebuild thumbnails: ${e.message}"
                    )
                }
            }
        }
    }

    fun rebuildSearchIndex() {
        if (_uiState.value.isRebuildingSearchIndex) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRebuildingSearchIndex = true) }
            try {
                val count = settingsRepository.rebuildSearchIndex()
                _uiState.update {
                    it.copy(
                        isRebuildingSearchIndex = false,
                        feedbackMessage = "Search index rebuilt successfully ($count items indexed)."
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isRebuildingSearchIndex = false,
                        feedbackMessage = "Failed to rebuild search index: ${e.message}"
                    )
                }
            }
        }
    }

    fun requestExportBackup() {
        if (_uiState.value.isExportingBackup) return
        viewModelScope.launch {
            _uiState.update { it.copy(isExportingBackup = true) }
            try {
                val json = settingsRepository.exportDataBackup()
                _uiState.update {
                    it.copy(
                        isExportingBackup = false,
                        exportedJsonString = json,
                        showExportDialog = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExportingBackup = false,
                        feedbackMessage = "Failed to export backup: ${e.message}"
                    )
                }
            }
        }
    }

    fun dismissExportDialog() {
        _uiState.update { it.copy(showExportDialog = false, exportedJsonString = null) }
    }

    fun requestImportBackup() {
        _uiState.update { it.copy(showImportDialog = true) }
    }

    fun dismissImportDialog() {
        _uiState.update { it.copy(showImportDialog = false) }
    }

    fun executeImportBackup(jsonString: String) {
        if (_uiState.value.isImportingBackup) return
        viewModelScope.launch {
            _uiState.update { it.copy(isImportingBackup = true, showImportDialog = false) }
            try {
                val result = settingsRepository.importDataBackup(jsonString)
                refreshStorageBreakdown()
                _uiState.update {
                    it.copy(
                        isImportingBackup = false,
                        feedbackMessage = result.message
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImportingBackup = false,
                        feedbackMessage = "Import failed: ${e.message}"
                    )
                }
            }
        }
    }

    fun setLicensesDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showLicensesDialog = visible) }
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    companion object {
        fun provideFactory(settingsRepository: SettingsRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return SettingsViewModel(settingsRepository) as T
                }
            }
    }
}
