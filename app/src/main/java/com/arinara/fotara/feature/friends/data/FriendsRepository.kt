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

    fun getClaimedUsername(): String
    suspend fun claimUsername(username: String, displayName: String): Result<String>
    fun getUsernameCooldownDays(): Int
    fun canChangeUsername(): Pair<Boolean, String?>
    fun getFollowersFlow(): Flow<List<FriendProfile>>
    fun getFollowingFlow(): Flow<List<FriendProfile>>
    suspend fun followUser(friend: FriendProfile)
    suspend fun unfollowUser(id: String)
    suspend fun removeFollower(id: String)
}

class LocalFriendsRepository(
    private val prefs: SharedPreferences,
    private val usernameManager: UsernameManager
) : FriendsRepository {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        UsernameManager(context)
    )

    constructor(prefs: SharedPreferences) : this(
        prefs,
        UsernameManager(prefs)
    )

    companion object {
        const val PREFS_NAME = "fotara_friends_prefs"
        const val KEY_FRIENDS_LIST = "key_friends_list"
        const val KEY_FOLLOWERS_LIST = "key_followers_list"
        const val KEY_FOLLOWING_LIST = "key_following_list"
        const val KEY_MY_HANDLE = "key_my_handle"
        const val KEY_MY_PRESENCE = "key_my_presence"
        const val KEY_MY_SUBJECT = "key_my_subject"
        const val DEFAULT_HANDLE = "scholar"
        private const val DELIMITER = "\n---FRIEND_RECORD---\n"
    }

    private val _friendsFlow = MutableStateFlow<List<FriendProfile>>(loadFriends())
    private val _followersFlow = MutableStateFlow<List<FriendProfile>>(loadFollowers())
    private val _followingFlow = MutableStateFlow<List<FriendProfile>>(loadFollowing())
    private val _myPresenceStatusFlow = MutableStateFlow(loadPresenceStatus())
    private val _mySubjectFlow = MutableStateFlow(loadSubject())

    private fun parseList(raw: String): List<FriendProfile> {
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

    private fun loadFriends(): List<FriendProfile> {
        val raw = prefs.getString(KEY_FRIENDS_LIST, null) ?: return emptyList()
        return parseList(raw)
    }

    private fun seedFollowers(): List<FriendProfile> = listOf(
        FriendProfile(
            id = "seed-follower-1",
            handle = "@jordan_b",
            displayName = "Jordan Baker",
            studyStatus = StudyPresenceStatus.STUDYING,
            currentSubject = "Linear Algebra & Vector Spaces",
            followersCount = 14,
            followingCount = 12
        ),
        FriendProfile(
            id = "seed-follower-2",
            handle = "@sarah_k",
            displayName = "Sarah Kim",
            studyStatus = StudyPresenceStatus.OPEN_TO_COLLAB,
            currentSubject = "Microeconomics",
            followersCount = 28,
            followingCount = 19
        ),
        FriendProfile(
            id = "seed-follower-3",
            handle = "@alex_w",
            displayName = "Alex Wong",
            studyStatus = StudyPresenceStatus.OFFLINE,
            currentSubject = null,
            followersCount = 9,
            followingCount = 15
        )
    )

    private fun seedFollowing(): List<FriendProfile> = listOf(
        FriendProfile(
            id = "seed-following-1",
            handle = "@jordan_b",
            displayName = "Jordan Baker",
            studyStatus = StudyPresenceStatus.STUDYING,
            currentSubject = "Linear Algebra & Vector Spaces",
            followersCount = 14,
            followingCount = 12
        ),
        FriendProfile(
            id = "seed-following-2",
            handle = "@taylor_m",
            displayName = "Taylor Miller",
            studyStatus = StudyPresenceStatus.IN_LECTURE,
            currentSubject = "Computer Architecture",
            followersCount = 31,
            followingCount = 24
        )
    )

    private fun saveFollowersToPrefs(list: List<FriendProfile>) {
        val serialized = list.joinToString(DELIMITER) { it.toJson() }
        prefs.edit().putString(KEY_FOLLOWERS_LIST, serialized).apply()
    }

    private fun saveFollowingToPrefs(list: List<FriendProfile>) {
        val serialized = list.joinToString(DELIMITER) { it.toJson() }
        prefs.edit().putString(KEY_FOLLOWING_LIST, serialized).apply()
    }

    private fun loadFollowers(): List<FriendProfile> {
        val raw = prefs.getString(KEY_FOLLOWERS_LIST, null)
        if (raw.isNullOrBlank()) {
            val seeded = seedFollowers()
            saveFollowersToPrefs(seeded)
            return seeded
        }
        val list = parseList(raw)
        return if (list.isEmpty()) {
            val seeded = seedFollowers()
            saveFollowersToPrefs(seeded)
            seeded
        } else {
            list
        }
    }

    private fun loadFollowing(): List<FriendProfile> {
        val raw = prefs.getString(KEY_FOLLOWING_LIST, null)
        if (raw.isNullOrBlank()) {
            val seeded = seedFollowing()
            saveFollowingToPrefs(seeded)
            return seeded
        }
        val list = parseList(raw)
        return if (list.isEmpty()) {
            val seeded = seedFollowing()
            saveFollowingToPrefs(seeded)
            seeded
        } else {
            list
        }
    }

    private fun persistFriends(list: List<FriendProfile>) {
        val serialized = list.joinToString(DELIMITER) { it.toJson() }
        prefs.edit().putString(KEY_FRIENDS_LIST, serialized).apply()
        _friendsFlow.value = list
    }

    private fun persistFollowers(list: List<FriendProfile>) {
        saveFollowersToPrefs(list)
        _followersFlow.value = list
    }

    private fun persistFollowing(list: List<FriendProfile>) {
        saveFollowingToPrefs(list)
        _followingFlow.value = list
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

    override fun getFollowersFlow(): Flow<List<FriendProfile>> = _followersFlow.asStateFlow()

    override fun getFollowingFlow(): Flow<List<FriendProfile>> = _followingFlow.asStateFlow()

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

    override suspend fun followUser(friend: FriendProfile) {
        val current = _followingFlow.value
        val normalizedFriendHandle = if (friend.handle.startsWith("@")) friend.handle.lowercase() else "@${friend.handle.lowercase()}"
        val exists = current.any { it.id == friend.id || it.handle.lowercase() == normalizedFriendHandle }
        if (!exists) {
            val updated = current + friend.copy(handle = normalizedFriendHandle)
            persistFollowing(updated)
        }
    }

    override suspend fun unfollowUser(id: String) {
        val current = _followingFlow.value
        val updated = current.filterNot { it.id == id || it.handle.equals(id, ignoreCase = true) }
        persistFollowing(updated)
    }

    override suspend fun removeFollower(id: String) {
        val current = _followersFlow.value
        val updated = current.filterNot { it.id == id || it.handle.equals(id, ignoreCase = true) }
        persistFollowers(updated)
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

    override fun getClaimedUsername(): String = usernameManager.getClaimedUsername()

    override suspend fun claimUsername(username: String, displayName: String): Result<String> {
        val result = usernameManager.claimUsername(username, displayName)
        if (result.isSuccess) {
            prefs.edit().putString(KEY_MY_HANDLE, result.getOrThrow()).apply()
        }
        return result
    }

    override fun getUsernameCooldownDays(): Int = usernameManager.getRemainingCooldownDays()

    override fun canChangeUsername(): Pair<Boolean, String?> = usernameManager.canChangeUsername()

    override fun getMyHandle(): String = usernameManager.getClaimedUsername()

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
