// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.onboarding

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arinara.fotara.data.repository.FolderRepository
import com.arinara.fotara.data.repository.SettingsRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class OnboardingViewModel(
    private val folderRepository: FolderRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    fun updatePermissions(cameraGranted: Boolean, storageGranted: Boolean) {
        _uiState.update {
            it.copy(
                isCameraPermissionGranted = cameraGranted,
                isStoragePermissionGranted = storageGranted
            )
        }
    }

    fun proceedToStarterFolders() {
        _uiState.update { it.copy(currentStep = OnboardingStep.STARTER_FOLDERS) }
    }

    fun createDefaultFolders(onComplete: () -> Unit) {
        if (_uiState.value.isCreatingFolders) return
        viewModelScope.launch {
            _uiState.update { it.copy(isCreatingFolders = true) }
            try {
                for (starter in _uiState.value.starterFolders) {
                    folderRepository.createFolder(
                        name = starter.name,
                        colorLabel = starter.colorHex
                    )
                }
                settingsRepository.setOnboardingCompleted(true)
                _uiState.update { it.copy(isCreatingFolders = false) }
                onComplete()
            } catch (_: Exception) {
                settingsRepository.setOnboardingCompleted(true)
                _uiState.update { it.copy(isCreatingFolders = false) }
                onComplete()
            }
        }
    }

    fun startEmpty(onComplete: () -> Unit) {
        viewModelScope.launch {
            settingsRepository.setOnboardingCompleted(true)
            onComplete()
        }
    }

    companion object {
        fun provideFactory(
            folderRepository: FolderRepository,
            settingsRepository: SettingsRepository
        ): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return OnboardingViewModel(folderRepository, settingsRepository) as T
                }
            }
    }
}
