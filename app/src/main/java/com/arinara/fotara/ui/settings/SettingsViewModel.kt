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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SettingsViewModel(
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _eventChannel = Channel<String>(Channel.BUFFERED)
    val eventFlow: Flow<String> = _eventChannel.receiveAsFlow()

    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settingsFlow.collect { settings ->
                _uiState.update { it.copy(userSettings = settings) }
            }
        }
        viewModelScope.launch {
            settingsRepository.profileFlow.collect { profile ->
                _uiState.update { it.copy(userProfile = profile) }
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

    fun updateDeviceCountEnabled(enabled: Boolean) {
        viewModelScope.launch {
            settingsRepository.updateDeviceCountEnabled(enabled)
        }
    }

    fun updateStorageLocation(location: StorageLocation) {
        viewModelScope.launch {
            settingsRepository.updateStorageLocation(location)
        }
    }

    fun updateCombineFileNamePreset(preset: String) {
        viewModelScope.launch {
            settingsRepository.updateCombineFileNamePreset(preset)
        }
    }

    fun updateSavedImageLocation(locationKey: String, customName: String) {
        viewModelScope.launch {
            settingsRepository.updateSavedImageLocation(locationKey, customName)
        }
    }

    fun rebuildThumbnails() {
        if (_uiState.value.isRebuildingThumbnails) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRebuildingThumbnails = true) }
            try {
                val count = settingsRepository.rebuildThumbnails()
                refreshStorageBreakdown()
                val msg = "Thumbnails rebuilt successfully ($count notes updated)."
                _uiState.update {
                    it.copy(
                        isRebuildingThumbnails = false,
                        feedbackMessage = msg
                    )
                }
                _eventChannel.trySend(msg)
            } catch (e: Exception) {
                val msg = "Failed to rebuild thumbnails: ${e.message}"
                _uiState.update {
                    it.copy(
                        isRebuildingThumbnails = false,
                        feedbackMessage = msg
                    )
                }
                _eventChannel.trySend(msg)
            }
        }
    }

    fun rebuildSearchIndex() {
        if (_uiState.value.isRebuildingSearchIndex) return
        viewModelScope.launch {
            _uiState.update { it.copy(isRebuildingSearchIndex = true) }
            try {
                val count = settingsRepository.rebuildSearchIndex()
                val msg = "Search index rebuilt successfully ($count items indexed)."
                _uiState.update {
                    it.copy(
                        isRebuildingSearchIndex = false,
                        feedbackMessage = msg
                    )
                }
                _eventChannel.trySend(msg)
            } catch (e: Exception) {
                val msg = "Failed to rebuild search index: ${e.message}"
                _uiState.update {
                    it.copy(
                        isRebuildingSearchIndex = false,
                        feedbackMessage = msg
                    )
                }
                _eventChannel.trySend(msg)
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
                val msg = "Failed to export backup: ${e.message}"
                _uiState.update {
                    it.copy(
                        isExportingBackup = false,
                        feedbackMessage = msg
                    )
                }
                _eventChannel.trySend(msg)
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
                _eventChannel.trySend(result.message)
            } catch (e: Exception) {
                val msg = "Import failed: ${e.message}"
                _uiState.update {
                    it.copy(
                        isImportingBackup = false,
                        feedbackMessage = msg
                    )
                }
                _eventChannel.trySend(msg)
            }
        }
    }

    fun setLicensesDialogVisible(visible: Boolean) {
        _uiState.update { it.copy(showLicensesDialog = visible) }
    }

    fun updateProfileName(name: String) {
        viewModelScope.launch {
            settingsRepository.updateProfileName(name)
        }
    }

    fun updateProfileEmail(email: String) {
        viewModelScope.launch {
            settingsRepository.updateProfileEmail(email)
        }
    }

    fun updateProfileBorder(borderId: String) {
        viewModelScope.launch {
            settingsRepository.updateProfileBorder(borderId)
        }
    }

    fun saveProfileAvatar(bitmap: android.graphics.Bitmap, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val path = settingsRepository.saveProfileAvatar(bitmap)
            val success = path != null
            if (!success) {
                val msg = "Failed to save profile picture"
                _eventChannel.trySend(msg)
            }
            onResult(success)
        }
    }

    fun removeProfileAvatar() {
        viewModelScope.launch {
            settingsRepository.removeProfileAvatar()
        }
    }

    fun saveProfileBanner(bitmap: android.graphics.Bitmap, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val path = settingsRepository.saveProfileBanner(bitmap)
            val success = path != null
            if (!success) {
                val msg = "Failed to save banner"
                _eventChannel.trySend(msg)
            }
            onResult(success)
        }
    }

    fun saveProfileBannerGif(bytes: ByteArray, crop: String?, onResult: (Boolean) -> Unit = {}) {
        viewModelScope.launch {
            val path = settingsRepository.saveProfileBannerGif(bytes, crop)
            val success = path != null
            if (!success) {
                val msg = "Failed to save banner"
                _eventChannel.trySend(msg)
            }
            onResult(success)
        }
    }

    fun removeProfileBanner() {
        viewModelScope.launch {
            settingsRepository.removeProfileBanner()
        }
    }

    fun postErrorMessage(message: String) {
        _eventChannel.trySend(message)
    }

    fun clearFeedbackMessage() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    fun consumeFeedbackMessage(): String? {
        val msg = _uiState.value.feedbackMessage
        if (msg != null) {
            _uiState.update { it.copy(feedbackMessage = null) }
        }
        return msg
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
