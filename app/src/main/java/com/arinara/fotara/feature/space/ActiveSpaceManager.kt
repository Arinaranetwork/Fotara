// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.space

import com.arinara.fotara.data.model.Space
import com.arinara.fotara.data.repository.SpaceRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ActiveSpaceManager(
    private val spaceRepository: SpaceRepository,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
) {
    val activeSpaceId: StateFlow<Long> = spaceRepository.activeSpaceId
    val activeSpace: StateFlow<Space> = spaceRepository.activeSpace
    val isPrivateSpaceUnlocked: StateFlow<Boolean> = spaceRepository.isPrivateSpaceUnlocked

    // Phase 47 Level 1: Private Folder Vault (pull-to-refresh & hold 2s)
    private val _isPrivateFolderUnlocked = MutableStateFlow(false)
    val isPrivateFolderUnlocked: StateFlow<Boolean> = _isPrivateFolderUnlocked.asStateFlow()

    // Phase 47 Level 2: Private Workspace Ghost Mode (Home tab long-press 1.5s)
    private val _isGhostWorkspaceUnlocked = MutableStateFlow(false)
    val isGhostWorkspaceUnlocked: StateFlow<Boolean> = _isGhostWorkspaceUnlocked.asStateFlow()

    fun switchSpace(spaceId: Long) {
        scope.launch {
            spaceRepository.setActiveSpaceId(spaceId)
        }
    }

    fun unlockPrivateFolders() {
        _isPrivateFolderUnlocked.value = true
    }

    fun lockPrivateFolders() {
        _isPrivateFolderUnlocked.value = false
    }

    fun toggleGhostWorkspaces(): Boolean {
        val newState = !_isGhostWorkspaceUnlocked.value
        _isGhostWorkspaceUnlocked.value = newState
        return newState
    }

    fun toggleGhostWorkspaceVisibility(): Boolean = toggleGhostWorkspaces()

    fun unlockStealthSpaceVault() {
        scope.launch {
            spaceRepository.setPrivateSpaceUnlocked(true)
        }
    }

    fun lockStealthSpaceVault() {
        scope.launch {
            spaceRepository.setPrivateSpaceUnlocked(false)
        }
    }

    fun toggleStealthVault(): Boolean {
        val newState = !isPrivateSpaceUnlocked.value
        scope.launch {
            spaceRepository.setPrivateSpaceUnlocked(newState)
        }
        return newState
    }
}
