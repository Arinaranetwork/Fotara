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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
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
    suspend fun findUserByHandle(handle: String): FriendProfile?

    fun syncMyProfile(displayName: String, pronouns: String = "", avatarUrl: String? = null) {}
}

class LocalFriendsRepository(
    private val prefs: SharedPreferences,
    private val usernameManager: UsernameManager,
    private val enableLanTransport: Boolean = false
) : FriendsRepository, FriendPeerRegistry.PeerListener, AutoCloseable {

    constructor(context: Context) : this(
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE),
        UsernameManager(context),
        enableLanTransport = true
    )

    constructor(prefs: SharedPreferences) : this(
        prefs,
        UsernameManager(prefs),
        enableLanTransport = false
    )

    companion object {
        const val PREFS_NAME = "fotara_friends_prefs"
        const val KEY_FRIENDS_LIST = "key_friends_list"
        const val KEY_FOLLOWERS_LIST = "key_followers_list"
        const val KEY_FOLLOWING_LIST = "key_following_list"
        const val KEY_MY_HANDLE = "key_my_handle"
        const val KEY_MY_DISPLAY_NAME = "key_my_display_name"
        const val KEY_MY_PRONOUNS = "key_my_pronouns"
        const val KEY_MY_AVATAR_URL = "key_my_avatar_url"
        const val KEY_MY_PRESENCE = "key_my_presence"
        const val KEY_MY_PRESENCE_EXPLICIT_OFFLINE = "key_my_presence_explicit_offline"
        const val KEY_MY_SUBJECT = "key_my_subject"
        const val DEFAULT_HANDLE = ""
        private const val DELIMITER = "\n---FRIEND_RECORD---\n"
    }

    private val _friendsFlow = MutableStateFlow<List<FriendProfile>>(loadFriends())
    private val _followersFlow = MutableStateFlow<List<FriendProfile>>(loadFollowers())
    private val _followingFlow = MutableStateFlow<List<FriendProfile>>(loadFollowing())
    private val _myPresenceStatusFlow = MutableStateFlow(loadPresenceStatus())
    private val _mySubjectFlow = MutableStateFlow(loadSubject())

    @Volatile
    private var myDisplayName: String = prefs.getString(KEY_MY_DISPLAY_NAME, null)
        ?.trim()
        ?.takeIf { it.isNotBlank() && !it.equals("Fotara User", ignoreCase = true) && !it.equals("My Profile", ignoreCase = true) }
        ?: usernameManager.getClaimedDisplayName().trim().takeIf {
            it.isNotBlank() && !it.equals("Fotara User", ignoreCase = true) && !it.equals("My Profile", ignoreCase = true)
        }
        ?: usernameManager.getClaimedUsername().removePrefix("@")

    @Volatile
    private var myPronouns: String = prefs.getString(KEY_MY_PRONOUNS, null).orEmpty()

    @Volatile
    private var myAvatarUrl: String? = prefs.getString(KEY_MY_AVATAR_URL, null)?.takeIf { it.isNotBlank() }

    private val lanTransport: LanFriendTransport? = if (enableLanTransport) {
        LanFriendTransport(this)
    } else {
        FriendPeerRegistry.registerListener(this)
        null
    }

    override val localHandle: String
        get() = resolveActiveLocalHandle()

    init {
        if (enableLanTransport) {
            val explicitOffline = prefs.getBoolean(KEY_MY_PRESENCE_EXPLICIT_OFFLINE, false)
            if (!explicitOffline && _myPresenceStatusFlow.value == StudyPresenceStatus.OFFLINE) {
                _myPresenceStatusFlow.value = StudyPresenceStatus.OPEN_TO_COLLAB
                prefs.edit().putString(KEY_MY_PRESENCE, StudyPresenceStatus.OPEN_TO_COLLAB.name).apply()
            }
            announceLocalPresence()
        } else {
            val snap = getLocalProfileSnapshot()
            if (snap != null && snap.handle.removePrefix("@").isNotBlank()) {
                FriendPeerRegistry.recordPeer(snap)
            }
        }
    }

    private fun resolveActiveLocalHandle(): String {
        val claimed = usernameManager.getClaimedUsername()
        if (claimed.isNotBlank()) return claimed
        val stored = prefs.getString(KEY_MY_HANDLE, null).orEmpty().trim()
        if (stored.isNotBlank()) return if (stored.startsWith("@")) stored.lowercase() else "@${stored.lowercase()}"
        val cleanName = myDisplayName.trim()
            .takeIf { !it.equals("Fotara User", ignoreCase = true) && !it.equals("My Profile", ignoreCase = true) }
            .orEmpty()
            .lowercase()
            .replace(Regex("[^a-z0-9_]"), "_")
            .trim('_')
        return if (cleanName.length >= 3) "@${cleanName.take(20)}" else ""
    }

    override fun getLocalProfileSnapshot(): FriendProfile? {
        val handle = resolveActiveLocalHandle()
        if (handle.removePrefix("@").isBlank()) return null
        val resolvedName = myDisplayName.trim()
            .takeIf { it.isNotBlank() && !it.equals("Fotara User", ignoreCase = true) && !it.equals("My Profile", ignoreCase = true) }
            ?: usernameManager.getClaimedDisplayName().trim()
                .takeIf { it.isNotBlank() && !it.equals("Fotara User", ignoreCase = true) && !it.equals("My Profile", ignoreCase = true) }
            ?: handle.removePrefix("@")
        val effectiveStatus = if (_myPresenceStatusFlow.value == StudyPresenceStatus.OFFLINE &&
            !prefs.getBoolean(KEY_MY_PRESENCE_EXPLICIT_OFFLINE, false)
        ) {
            StudyPresenceStatus.OPEN_TO_COLLAB
        } else {
            _myPresenceStatusFlow.value
        }
        return FriendProfile(
            id = usernameManager.getInstallUuid(),
            handle = handle,
            displayName = resolvedName,
            avatarUrl = myAvatarUrl,
            pronouns = myPronouns,
            studyStatus = effectiveStatus,
            currentSubject = _mySubjectFlow.value,
            followersCount = _followersFlow.value.size,
            followingCount = _followingFlow.value.size
        )
    }

    override fun syncMyProfile(displayName: String, pronouns: String, avatarUrl: String?) {
        val trimmedName = displayName.trim()
        val trimmedPronouns = pronouns.trim()
        val editor = prefs.edit()
        if (trimmedName.isNotBlank() &&
            !trimmedName.equals("My Profile", ignoreCase = true) &&
            !trimmedName.equals("Fotara User", ignoreCase = true)
        ) {
            myDisplayName = trimmedName
            editor.putString(KEY_MY_DISPLAY_NAME, trimmedName)
        } else {
            val fallbackClaimed = usernameManager.getClaimedDisplayName().trim()
                .takeIf { it.isNotBlank() && !it.equals("Fotara User", ignoreCase = true) && !it.equals("My Profile", ignoreCase = true) }
                ?: usernameManager.getClaimedUsername().removePrefix("@")
            if (fallbackClaimed.isNotBlank()) {
                myDisplayName = fallbackClaimed
                editor.putString(KEY_MY_DISPLAY_NAME, fallbackClaimed)
            }
        }
        myPronouns = trimmedPronouns
        editor.putString(KEY_MY_PRONOUNS, trimmedPronouns)
        myAvatarUrl = avatarUrl?.takeIf { it.isNotBlank() }
        editor.putString(KEY_MY_AVATAR_URL, myAvatarUrl)

        val explicitOffline = prefs.getBoolean(KEY_MY_PRESENCE_EXPLICIT_OFFLINE, false)
        if (!explicitOffline && _myPresenceStatusFlow.value == StudyPresenceStatus.OFFLINE) {
            _myPresenceStatusFlow.value = StudyPresenceStatus.OPEN_TO_COLLAB
            editor.putString(KEY_MY_PRESENCE, StudyPresenceStatus.OPEN_TO_COLLAB.name)
        }
        editor.apply()
        announceLocalPresence()
    }

    private fun announceLocalPresence() {
        val snap = getLocalProfileSnapshot() ?: return
        FriendPeerRegistry.recordPeer(snap)
        lanTransport?.announceNow()
    }

    @Synchronized
    override fun onRemotePeerAnnounced(profile: FriendProfile) {
        val normHandle = FriendPeerRegistry.normalizeHandle(profile.handle)
        if (normHandle.length <= 1) return

        fun mergeProfile(existing: FriendProfile): FriendProfile {
            return existing.copy(
                displayName = profile.displayName.ifBlank { existing.displayName },
                pronouns = profile.pronouns.ifBlank { existing.pronouns },
                studyStatus = profile.studyStatus,
                currentSubject = profile.currentSubject,
                followersCount = maxOf(profile.followersCount, existing.followersCount),
                followingCount = maxOf(profile.followingCount, existing.followingCount)
            )
        }

        val currentFriends = _friendsFlow.value
        if (currentFriends.any { it.handle.equals(normHandle, ignoreCase = true) || it.id == profile.id }) {
            val updated = currentFriends.map { item ->
                if (item.handle.equals(normHandle, ignoreCase = true) || item.id == profile.id) {
                    mergeProfile(item)
                } else {
                    item
                }
            }
            if (updated != currentFriends) {
                persistFriends(updated)
            }
        }

        val currentFollowers = _followersFlow.value
        if (currentFollowers.any { it.handle.equals(normHandle, ignoreCase = true) || it.id == profile.id }) {
            val updated = currentFollowers.map { item ->
                if (item.handle.equals(normHandle, ignoreCase = true) || item.id == profile.id) {
                    mergeProfile(item)
                } else {
                    item
                }
            }
            if (updated != currentFollowers) {
                persistFollowers(updated)
            }
        }

        val currentFollowing = _followingFlow.value
        if (currentFollowing.any { it.handle.equals(normHandle, ignoreCase = true) || it.id == profile.id }) {
            val updated = currentFollowing.map { item ->
                if (item.handle.equals(normHandle, ignoreCase = true) || item.id == profile.id) {
                    mergeProfile(item)
                } else {
                    item
                }
            }
            if (updated != currentFollowing) {
                persistFollowing(updated)
            }
        }
    }

    @Synchronized
    override fun onRemoteConnectEvent(
        eventType: String,
        senderProfile: FriendProfile,
        targetHandle: String
    ) {
        val normSender = FriendPeerRegistry.normalizeHandle(senderProfile.handle)
        if (normSender.length <= 1) return
        val normalizedSender = senderProfile.copy(handle = normSender)

        when (eventType) {
            "ADD_FRIEND", "FOLLOW" -> {
                val currentFollowers = _followersFlow.value
                val existsInFollowers = currentFollowers.any {
                    it.id == normalizedSender.id || it.handle.equals(normSender, ignoreCase = true)
                }
                val updatedFollowers = if (existsInFollowers) {
                    currentFollowers.map {
                        if (it.id == normalizedSender.id || it.handle.equals(normSender, ignoreCase = true)) {
                            normalizedSender.copy(isFavorite = it.isFavorite, joinedAt = it.joinedAt)
                        } else {
                            it
                        }
                    }
                } else {
                    currentFollowers + normalizedSender
                }
                persistFollowers(updatedFollowers)

                val isMutualOrFriend = eventType == "ADD_FRIEND" ||
                    _followingFlow.value.any { it.handle.equals(normSender, ignoreCase = true) } ||
                    _friendsFlow.value.any { it.handle.equals(normSender, ignoreCase = true) }
                if (isMutualOrFriend) {
                    val currentFriends = _friendsFlow.value
                    val existsInFriends = currentFriends.any {
                        it.id == normalizedSender.id || it.handle.equals(normSender, ignoreCase = true)
                    }
                    val updatedFriends = if (existsInFriends) {
                        currentFriends.map {
                            if (it.id == normalizedSender.id || it.handle.equals(normSender, ignoreCase = true)) {
                                normalizedSender.copy(isFavorite = it.isFavorite, joinedAt = it.joinedAt)
                            } else {
                                it
                            }
                        }
                    } else {
                        currentFriends + normalizedSender
                    }
                    persistFriends(updatedFriends)
                }
                announceLocalPresence()
            }
            "UNFOLLOW" -> {
                val updated = _followersFlow.value.filterNot {
                    it.id == normalizedSender.id || it.handle.equals(normSender, ignoreCase = true)
                }
                persistFollowers(updated)
            }
            "REMOVE_FOLLOWER" -> {
                val updated = _followingFlow.value.filterNot {
                    it.id == normalizedSender.id || it.handle.equals(normSender, ignoreCase = true)
                }
                persistFollowing(updated)
            }
        }
    }

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

    private fun isSeedProfile(profile: FriendProfile): Boolean {
        return profile.id.startsWith("seed-") ||
            profile.id.startsWith("seed_") ||
            profile.handle in setOf("@jordan_b", "@sarah_k", "@alex_w", "@taylor_m")
    }

    private fun saveFriendsToPrefs(list: List<FriendProfile>) {
        val serialized = list.joinToString(DELIMITER) { it.toJson() }
        prefs.edit().putString(KEY_FRIENDS_LIST, serialized).apply()
    }

    private fun loadFriends(): List<FriendProfile> {
        val raw = prefs.getString(KEY_FRIENDS_LIST, null) ?: return emptyList()
        val list = parseList(raw)
        val sanitized = list.filterNot { isSeedProfile(it) }
        if (sanitized.size != list.size) {
            saveFriendsToPrefs(sanitized)
        }
        return sanitized
    }

    private fun saveFollowersToPrefs(list: List<FriendProfile>) {
        val serialized = list.joinToString(DELIMITER) { it.toJson() }
        prefs.edit().putString(KEY_FOLLOWERS_LIST, serialized).apply()
    }

    private fun saveFollowingToPrefs(list: List<FriendProfile>) {
        val serialized = list.joinToString(DELIMITER) { it.toJson() }
        prefs.edit().putString(KEY_FOLLOWING_LIST, serialized).apply()
    }

    private fun loadFollowers(): List<FriendProfile> {
        val raw = prefs.getString(KEY_FOLLOWERS_LIST, null) ?: return emptyList()
        val list = parseList(raw)
        val sanitized = list.filterNot { isSeedProfile(it) }
        if (sanitized.size != list.size) {
            saveFollowersToPrefs(sanitized)
        }
        return sanitized
    }

    private fun loadFollowing(): List<FriendProfile> {
        val raw = prefs.getString(KEY_FOLLOWING_LIST, null) ?: return emptyList()
        val list = parseList(raw)
        val sanitized = list.filterNot { isSeedProfile(it) }
        if (sanitized.size != list.size) {
            saveFollowingToPrefs(sanitized)
        }
        return sanitized
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
        val (extractedHandle, codeProfile) = FriendShareCodeCodec.decode(handle)
        val rawHandle = extractedHandle.trim()
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

        // 1. Discover live peer on LAN or in FriendPeerRegistry
        val lanDiscovered = FriendPeerRegistry.findByHandle(cleanHandle)
            ?: if (lanTransport != null) {
                withContext(Dispatchers.IO) {
                    lanTransport.queryPeerByHandle(cleanHandle, timeoutMs = 450L)
                }
            } else {
                null
            }
            ?: codeProfile

        var trimmedDisplay = displayName.trim()
        if (trimmedDisplay.isEmpty()) {
            if (lanDiscovered != null && lanDiscovered.displayName.isNotBlank()) {
                trimmedDisplay = lanDiscovered.displayName
            } else {
                val remoteRes = usernameManager.fetchProfileByUsername(handleBody)
                val remoteName = remoteRes.getOrNull()?.displayName?.trim()
                trimmedDisplay = if (!remoteName.isNullOrBlank()) {
                    remoteName
                } else {
                    handleBody.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
            }
        }

        val newFriend = if (lanDiscovered != null) {
            lanDiscovered.copy(
                id = lanDiscovered.id.ifBlank { UUID.randomUUID().toString() },
                handle = cleanHandle,
                displayName = trimmedDisplay.ifBlank { lanDiscovered.displayName },
                joinedAt = System.currentTimeMillis(),
                isFavorite = false
            )
        } else {
            FriendProfile(
                id = UUID.randomUUID().toString(),
                handle = cleanHandle,
                displayName = trimmedDisplay,
                avatarUrl = null,
                studyStatus = StudyPresenceStatus.OFFLINE,
                currentSubject = null,
                joinedAt = System.currentTimeMillis(),
                isFavorite = false
            )
        }

        val updated = currentList + newFriend
        persistFriends(updated)

        if (lanDiscovered != null || lanTransport != null) {
            val currentFollowing = _followingFlow.value
            if (currentFollowing.none { it.handle.equals(cleanHandle, ignoreCase = true) }) {
                persistFollowing(currentFollowing + newFriend)
            }
            val snap = getLocalProfileSnapshot()
            if (snap != null) {
                FriendPeerRegistry.dispatchConnectEvent("ADD_FRIEND", snap, cleanHandle)
            }
            lanTransport?.sendConnectEvent("ADD_FRIEND", cleanHandle)
        }

        return Result.success(newFriend)
    }

    override suspend fun findUserByHandle(handle: String): FriendProfile? {
        val (extractedHandle, codeProfile) = FriendShareCodeCodec.decode(handle)
        val clean = usernameManager.cleanUsername(extractedHandle)
        if (clean.isBlank()) return null
        val normalized = "@$clean"

        // 1. Check live LAN / FriendPeerRegistry first so online peers always return live status & data
        val livePeer = FriendPeerRegistry.findByHandle(normalized)
            ?: if (lanTransport != null) {
                withContext(Dispatchers.IO) {
                    lanTransport.queryPeerByHandle(normalized, timeoutMs = 450L)
                }
            } else {
                null
            }
            ?: codeProfile
        if (livePeer != null) {
            onRemotePeerAnnounced(livePeer)
            return livePeer.copy(handle = normalized)
        }

        // 2. Check local friends, following, followers
        _friendsFlow.value.find { it.handle.equals(normalized, ignoreCase = true) }?.let { return it }
        _followingFlow.value.find { it.handle.equals(normalized, ignoreCase = true) }?.let { return it }
        _followersFlow.value.find { it.handle.equals(normalized, ignoreCase = true) }?.let { return it }

        // 3. Query remote Supabase registry
        val remoteRes = usernameManager.fetchProfileByUsername(clean)
        if (remoteRes.isSuccess && remoteRes.getOrNull() != null) {
            val remote = remoteRes.getOrNull()!!
            return FriendProfile(
                id = remote.id,
                handle = "@${remote.username}",
                displayName = remote.displayName,
                studyStatus = StudyPresenceStatus.OFFLINE
            )
        }

        // 4. Fallback discovered representation if valid username format
        if (usernameManager.validateUsernameFormat(clean).first) {
            return FriendProfile(
                id = "discovered_${clean}",
                handle = normalized,
                displayName = clean.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() },
                studyStatus = StudyPresenceStatus.OFFLINE
            )
        }
        return null
    }

    override suspend fun removeFriend(id: String) {
        val currentList = _friendsFlow.value
        val updated = currentList.filterNot { it.id == id }
        persistFriends(updated)
    }

    override suspend fun followUser(friend: FriendProfile) {
        val current = _followingFlow.value
        val normalizedFriendHandle = if (friend.handle.startsWith("@")) friend.handle.lowercase() else "@${friend.handle.lowercase()}"
        val livePeer = FriendPeerRegistry.findByHandle(normalizedFriendHandle) ?: friend
        val resolvedFriend = livePeer.copy(handle = normalizedFriendHandle)
        val exists = current.any { it.id == friend.id || it.handle.lowercase() == normalizedFriendHandle }
        if (!exists) {
            val updated = current + resolvedFriend
            persistFollowing(updated)
        }
        // If they already follow us (or in live discovery), also add/sync in Friends list
        val isFollower = _followersFlow.value.any { it.handle.equals(normalizedFriendHandle, ignoreCase = true) }
        if (isFollower || FriendPeerRegistry.findByHandle(normalizedFriendHandle) != null) {
            val currentFriends = _friendsFlow.value
            if (currentFriends.none { it.handle.equals(normalizedFriendHandle, ignoreCase = true) }) {
                persistFriends(currentFriends + resolvedFriend)
            }
        }
        val snap = getLocalProfileSnapshot()
        if (snap != null) {
            FriendPeerRegistry.dispatchConnectEvent("FOLLOW", snap, normalizedFriendHandle)
        }
        lanTransport?.sendConnectEvent("FOLLOW", normalizedFriendHandle)
    }

    override suspend fun unfollowUser(id: String) {
        val current = _followingFlow.value
        val removed = current.firstOrNull { it.id == id || it.handle.equals(id, ignoreCase = true) }
        val updated = current.filterNot { it.id == id || it.handle.equals(id, ignoreCase = true) }
        persistFollowing(updated)
        if (removed != null) {
            val snap = getLocalProfileSnapshot()
            if (snap != null) {
                FriendPeerRegistry.dispatchConnectEvent("UNFOLLOW", snap, removed.handle)
            }
            lanTransport?.sendConnectEvent("UNFOLLOW", removed.handle)
        }
    }

    override suspend fun removeFollower(id: String) {
        val current = _followersFlow.value
        val removed = current.firstOrNull { it.id == id || it.handle.equals(id, ignoreCase = true) }
        val updated = current.filterNot { it.id == id || it.handle.equals(id, ignoreCase = true) }
        persistFollowers(updated)
        if (removed != null) {
            val snap = getLocalProfileSnapshot()
            if (snap != null) {
                FriendPeerRegistry.dispatchConnectEvent("REMOVE_FOLLOWER", snap, removed.handle)
            }
            lanTransport?.sendConnectEvent("REMOVE_FOLLOWER", removed.handle)
        }
    }

    override suspend fun updatePresence(status: StudyPresenceStatus, subject: String?) {
        val cleanSubject = subject?.trim()?.takeIf { it.isNotEmpty() }
        prefs.edit()
            .putString(KEY_MY_PRESENCE, status.name)
            .putBoolean(KEY_MY_PRESENCE_EXPLICIT_OFFLINE, status == StudyPresenceStatus.OFFLINE)
            .putString(KEY_MY_SUBJECT, cleanSubject)
            .apply()
        _myPresenceStatusFlow.value = status
        _mySubjectFlow.value = cleanSubject
        announceLocalPresence()
    }

    override fun getClaimedUsername(): String = usernameManager.getClaimedUsername()

    override suspend fun claimUsername(username: String, displayName: String): Result<String> {
        val result = usernameManager.claimUsername(username, displayName)
        if (result.isSuccess) {
            val claimed = result.getOrThrow()
            val trimmedName = displayName.trim()
                .takeIf { it.isNotBlank() && !it.equals("Fotara User", ignoreCase = true) && !it.equals("My Profile", ignoreCase = true) }
                ?: claimed.removePrefix("@")
            val editor = prefs.edit().putString(KEY_MY_HANDLE, claimed)
            if (trimmedName.isNotBlank()) {
                myDisplayName = trimmedName
                editor.putString(KEY_MY_DISPLAY_NAME, trimmedName)
            }
            editor.apply()
            announceLocalPresence()
        }
        return result
    }

    override fun getUsernameCooldownDays(): Int = usernameManager.getRemainingCooldownDays()

    override fun canChangeUsername(): Pair<Boolean, String?> = usernameManager.canChangeUsername()

    override fun getMyHandle(): String = resolveActiveLocalHandle()

    override fun generateShareCode(): String {
        val snap = getLocalProfileSnapshot()
        if (snap != null) {
            return FriendShareCodeCodec.encode(snap)
        }
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

    override fun close() {
        FriendPeerRegistry.unregisterListener(this)
        lanTransport?.close()
    }
}
