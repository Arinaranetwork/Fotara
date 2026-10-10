// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.friends.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.arinara.fotara.feature.friends.data.FriendsRepository
import com.arinara.fotara.feature.friends.model.FriendProfile
import com.arinara.fotara.feature.friends.model.StudyPresenceStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class FriendsUiState(
    val friends: List<FriendProfile> = emptyList(),
    val searchQuery: String = "",
    val myHandle: String = "@scholar",
    val myPresenceStatus: StudyPresenceStatus = StudyPresenceStatus.OFFLINE,
    val mySubject: String? = null,
    val isLoading: Boolean = false,
    val message: String? = null
) {
    val filteredFriends: List<FriendProfile>
        get() = if (searchQuery.isBlank()) {
            friends
        } else {
            val q = searchQuery.trim().lowercase()
            friends.filter {
                it.displayName.lowercase().contains(q) ||
                it.handle.lowercase().contains(q) ||
                (it.currentSubject?.lowercase()?.contains(q) == true)
            }
        }

    val activeBuddies: List<FriendProfile>
        get() = filteredFriends.filter { it.isActive }

    val offlineBuddies: List<FriendProfile>
        get() = filteredFriends.filter { !it.isActive }

    val totalBuddiesCount: Int
        get() = friends.size

    val activeBuddiesCount: Int
        get() = friends.count { it.isActive }
}

class FriendsViewModel(
    private val repository: FriendsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _message = MutableStateFlow<String?>(null)

    val uiState: StateFlow<FriendsUiState> = combine(
        repository.getFriendsFlow(),
        repository.getMyPresenceStatusFlow(),
        repository.getMySubjectFlow(),
        _searchQuery,
        _message
    ) { friends, presence, subject, query, message ->
        FriendsUiState(
            friends = friends,
            searchQuery = query,
            myHandle = repository.getMyHandle(),
            myPresenceStatus = presence,
            mySubject = subject,
            isLoading = false,
            message = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FriendsUiState(
            myHandle = repository.getMyHandle()
        )
    )

    fun onSearchQueryChanged(query: String) {
        _searchQuery.value = query
    }

    fun updatePresence(status: StudyPresenceStatus, subject: String?) {
        viewModelScope.launch {
            repository.updatePresence(status, subject)
        }
    }

    fun addFriend(handle: String, displayName: String, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            val result = repository.addFriend(handle, displayName)
            if (result.isSuccess) {
                _message.value = "Added ${result.getOrNull()?.displayName} to study buddies"
                onComplete?.invoke(true, "Successfully added friend")
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to add friend"
                _message.value = errorMsg
                onComplete?.invoke(false, errorMsg)
            }
        }
    }

    fun removeFriend(id: String) {
        viewModelScope.launch {
            repository.removeFriend(id)
            _message.value = "Study buddy removed"
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    fun getShareCode(): String {
        return repository.generateShareCode()
    }

    fun getMyHandle(): String {
        return repository.getMyHandle()
    }

    fun clearMessage() {
        _message.value = null
    }

    companion object {
        fun provideFactory(
            repository: FriendsRepository
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return FriendsViewModel(repository) as T
            }
        }
    }
}
