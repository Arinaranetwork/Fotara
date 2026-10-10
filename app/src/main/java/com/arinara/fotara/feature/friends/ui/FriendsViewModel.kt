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
    val followers: List<FriendProfile> = emptyList(),
    val following: List<FriendProfile> = emptyList(),
    val followersCount: Int = 0,
    val followingCount: Int = 0,
    val cooldownDaysRemaining: Int = 0,
    val canChangeUsername: Boolean = true,
    val cooldownMessage: String? = null,
    val claimedUsername: String = "@scholar",
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

    val activeFriends: List<FriendProfile>
        get() = filteredFriends.filter { it.isActive }

    val offlineFriends: List<FriendProfile>
        get() = filteredFriends.filter { !it.isActive }

    val totalFriendsCount: Int
        get() = friends.size

    val activeFriendsCount: Int
        get() = friends.count { it.isActive }
}

class FriendsViewModel(
    private val repository: FriendsRepository
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _message = MutableStateFlow<String?>(null)
    private val _claimVersion = MutableStateFlow(0)

    val uiState: StateFlow<FriendsUiState> = combine(
        combine(
            repository.getFriendsFlow(),
            repository.getFollowersFlow(),
            repository.getFollowingFlow()
        ) { friends, followers, following ->
            Triple(friends, followers, following)
        },
        repository.getMyPresenceStatusFlow(),
        repository.getMySubjectFlow(),
        _searchQuery,
        combine(_message, _claimVersion) { msg, ver -> msg to ver }
    ) { (friends, followers, following), presence, subject, query, (message, _) ->
        val (canChange, cooldownMsg) = repository.canChangeUsername()
        val cooldownDays = repository.getUsernameCooldownDays()
        val claimed = repository.getClaimedUsername()
        FriendsUiState(
            friends = friends,
            followers = followers,
            following = following,
            followersCount = followers.size,
            followingCount = following.size,
            cooldownDaysRemaining = cooldownDays,
            canChangeUsername = canChange,
            cooldownMessage = cooldownMsg,
            claimedUsername = claimed,
            searchQuery = query,
            myHandle = claimed,
            myPresenceStatus = presence,
            mySubject = subject,
            isLoading = false,
            message = message
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.Eagerly,
        initialValue = FriendsUiState(
            myHandle = repository.getMyHandle(),
            claimedUsername = repository.getClaimedUsername(),
            canChangeUsername = repository.canChangeUsername().first,
            cooldownMessage = repository.canChangeUsername().second,
            cooldownDaysRemaining = repository.getUsernameCooldownDays()
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
                _message.value = "Added ${result.getOrNull()?.displayName} to friends"
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
            _message.value = "Friend removed"
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            repository.toggleFavorite(id)
        }
    }

    fun claimUsername(username: String, displayName: String, onResult: (Boolean, String) -> Unit) {
        viewModelScope.launch {
            val result = repository.claimUsername(username, displayName)
            if (result.isSuccess) {
                val claimed = result.getOrThrow()
                _message.value = "Username claimed: $claimed"
                _claimVersion.value += 1
                onResult(true, "Username claimed successfully: $claimed")
            } else {
                val errorMsg = result.exceptionOrNull()?.message ?: "Failed to claim username"
                _message.value = errorMsg
                onResult(false, errorMsg)
            }
        }
    }

    fun followUser(friend: FriendProfile) {
        viewModelScope.launch {
            repository.followUser(friend)
        }
    }

    fun unfollowUser(id: String) {
        viewModelScope.launch {
            repository.unfollowUser(id)
        }
    }

    fun removeFollower(id: String) {
        viewModelScope.launch {
            repository.removeFollower(id)
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
