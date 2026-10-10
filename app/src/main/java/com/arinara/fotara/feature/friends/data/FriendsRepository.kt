// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.friends.data

import android.content.Context
import android.content.SharedPreferences
import com.arinara.fotara.feature.friends.model.FriendProfile
import com.arinara.fotara.feature.friends.model.StudyPresenceStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

interface FriendsRepository {
    fun getFriendsFlow(): Flow<List<FriendProfile>>
    suspend fun addFriend(handle: String, displayName: String): Result<FriendProfile>
    suspend fun removeFriend(id: String)
    suspend fun updatePresence(status: StudyPresenceStatus, subject: String?)
    fun getMyHandle(): String
    fun generateShareCode(): String
    suspend fun toggleFavorite(id: String)
    fun getMyPresenceStatusFlow(): Flow<StudyPresenceStatus>
    fun getMySubjectFlow(): Flow<String?>
}

class LocalFriendsRepository(
    private val prefs: SharedPreferences
) : FriendsRepository {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    )

    companion object {
        const val PREFS_NAME = "fotara_friends_prefs"
        const val KEY_FRIENDS_LIST = "key_friends_list"
        const val KEY_MY_HANDLE = "key_my_handle"
        const val KEY_MY_PRESENCE = "key_my_presence"
        const val KEY_MY_SUBJECT = "key_my_subject"
        const val DEFAULT_HANDLE = "scholar"
        private const val DELIMITER = "\n---FRIEND_RECORD---\n"
    }

    private val _friendsFlow = MutableStateFlow<List<FriendProfile>>(loadFriends())
    private val _myPresenceStatusFlow = MutableStateFlow(loadPresenceStatus())
    private val _mySubjectFlow = MutableStateFlow(loadSubject())

    private fun loadFriends(): List<FriendProfile> {
        val raw = prefs.getString(KEY_FRIENDS_LIST, null) ?: return emptyList()
        if (raw.isBlank()) return emptyList()
        val items = raw.split(DELIMITER)
        val list = mutableListOf<FriendProfile>()
        for (item in items) {
            val trimmed = item.trim()
            if (trimmed.isNotEmpty()) {
                val profile = FriendProfile.fromJson(trimmed)
                if (profile != null) {
                    list.add(profile)
                }
            }
        }
        return list
    }

    private fun persistFriends(list: List<FriendProfile>) {
        val serialized = list.joinToString(DELIMITER) { it.toJson() }
        prefs.edit().putString(KEY_FRIENDS_LIST, serialized).apply()
        _friendsFlow.value = list
    }

    private fun loadPresenceStatus(): StudyPresenceStatus {
        val raw = prefs.getString(KEY_MY_PRESENCE, null) ?: return StudyPresenceStatus.OFFLINE
        return try {
            StudyPresenceStatus.valueOf(raw)
        } catch (_: Exception) {
            StudyPresenceStatus.OFFLINE
        }
    }

    private fun loadSubject(): String? {
        val sub = prefs.getString(KEY_MY_SUBJECT, null)
        return if (sub.isNullOrBlank()) null else sub
    }

    override fun getFriendsFlow(): Flow<List<FriendProfile>> = _friendsFlow.asStateFlow()

    override fun getMyPresenceStatusFlow(): Flow<StudyPresenceStatus> = _myPresenceStatusFlow.asStateFlow()

    override fun getMySubjectFlow(): Flow<String?> = _mySubjectFlow.asStateFlow()

    override suspend fun addFriend(handle: String, displayName: String): Result<FriendProfile> {
        val trimmedDisplay = displayName.trim()
        if (trimmedDisplay.isEmpty()) {
            return Result.failure(IllegalArgumentException("Display name cannot be empty"))
        }

        val rawHandle = handle.trim()
        val normalizedHandle = if (rawHandle.startsWith("@")) rawHandle else "@$rawHandle"
        if (normalizedHandle.length <= 1) {
            return Result.failure(IllegalArgumentException("Handle cannot be empty"))
        }

        // Validate characters: only lowercase letters, digits, underscores, and periods
        val cleanHandle = normalizedHandle.lowercase()
        val handleBody = cleanHandle.removePrefix("@")
        if (!handleBody.matches("^[a-z0-9._]+$".toRegex())) {
            return Result.failure(IllegalArgumentException("Handle may only contain letters, numbers, underscores, and dots"))
        }

        val currentList = _friendsFlow.value
        if (currentList.any { it.handle.equals(cleanHandle, ignoreCase = true) }) {
            return Result.failure(IllegalStateException("Friend with handle $cleanHandle already exists"))
        }

        val newFriend = FriendProfile(
            id = UUID.randomUUID().toString(),
            handle = cleanHandle,
            displayName = trimmedDisplay,
            avatarUrl = null,
            studyStatus = StudyPresenceStatus.OFFLINE,
            currentSubject = null,
            joinedAt = System.currentTimeMillis(),
            isFavorite = false
        )

        val updated = currentList + newFriend
        persistFriends(updated)
        return Result.success(newFriend)
    }

    override suspend fun removeFriend(id: String) {
        val currentList = _friendsFlow.value
        val updated = currentList.filterNot { it.id == id }
        persistFriends(updated)
    }

    override suspend fun updatePresence(status: StudyPresenceStatus, subject: String?) {
        val cleanSubject = subject?.trim()?.takeIf { it.isNotEmpty() }
        prefs.edit()
            .putString(KEY_MY_PRESENCE, status.name)
            .putString(KEY_MY_SUBJECT, cleanSubject)
            .apply()
        _myPresenceStatusFlow.value = status
        _mySubjectFlow.value = cleanSubject
    }

    override fun getMyHandle(): String {
        val handle = prefs.getString(KEY_MY_HANDLE, null)
        if (!handle.isNullOrBlank()) {
            return if (handle.startsWith("@")) handle else "@$handle"
        }
        val defaultHandle = "@$DEFAULT_HANDLE"
        prefs.edit().putString(KEY_MY_HANDLE, defaultHandle).apply()
        return defaultHandle
    }

    override fun generateShareCode(): String {
        val myHandle = getMyHandle().removePrefix("@")
        val timestamp = System.currentTimeMillis()
        val suffix = Integer.toHexString((myHandle.hashCode() xor timestamp.toInt()) and 0xFFFF).uppercase()
        return "FOTARA-FRIEND-$myHandle-$suffix"
    }

    override suspend fun toggleFavorite(id: String) {
        val currentList = _friendsFlow.value
        val updated = currentList.map { friend ->
            if (friend.id == id) {
                friend.copy(isFavorite = !friend.isFavorite)
            } else {
                friend
            }
        }
        persistFriends(updated)
    }
}
