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
    fun getMyHandleFlow(): Flow<String> = MutableStateFlow(getMyHandle())
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

    fun syncMyProfile(
        displayName: String,
        pronouns: String = "",
        avatarUrl: String? = null,
        bannerUrl: String? = null
    ) {}
    fun refreshNetworkPeers() {}
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
        const val KEY_MY_BANNER_URL = "key_my_banner_url"
        const val KEY_MY_PRESENCE = "key_my_presence"
        const val KEY_MY_PRESENCE_EXPLICIT_OFFLINE = "key_my_presence_explicit_offline"
        const val KEY_MY_SUBJECT = "key_my_subject"
        const val DEFAULT_HANDLE = ""
        private const val DELIMITER = "\n---FRIEND_RECORD---\n"
        private val mediaDataUriCache = java.util.concurrent.ConcurrentHashMap<String, String>()

        fun encodeImageFileOrKeepUri(
            rawPath: String?,
            maxWidth: Int,
            maxHeight: Int,
            quality: Int = 55
        ): String? {
            val trimmed = rawPath?.trim()?.takeIf { it.isNotBlank() } ?: return null
            if (trimmed.startsWith("data:image/", ignoreCase = true) ||
                trimmed.startsWith("http://", ignoreCase = true) ||
                trimmed.startsWith("https://", ignoreCase = true)
            ) {
                return trimmed
            }
            return try {
                val file = java.io.File(trimmed)
                if (!file.exists() || !file.isFile || file.length() <= 0L) {
                    return trimmed
                }
                val cacheKey = "${file.absolutePath}:${file.lastModified()}:${maxWidth}x${maxHeight}"
                mediaDataUriCache[cacheKey]?.let { return it }

                val bounds = android.graphics.BitmapFactory.Options().apply { inJustDecodeBounds = true }
                android.graphics.BitmapFactory.decodeFile(file.absolutePath, bounds)
                if (bounds.outWidth <= 0 || bounds.outHeight <= 0) {
                    return trimmed
                }
                var sample = 1
                while ((bounds.outWidth / sample) > maxWidth * 2 || (bounds.outHeight / sample) > maxHeight * 2) {
                    sample *= 2
                }
                val decodeOpts = android.graphics.BitmapFactory.Options().apply { inSampleSize = sample }
                val decoded = android.graphics.BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
                    ?: return trimmed
                val scale = minOf(
                    maxWidth.toFloat() / decoded.width.coerceAtLeast(1),
                    maxHeight.toFloat() / decoded.height.coerceAtLeast(1),
                    1.0f
                )
                val targetW = (decoded.width * scale).toInt().coerceAtLeast(1)
                val targetH = (decoded.height * scale).toInt().coerceAtLeast(1)
                val scaled = if (targetW != decoded.width || targetH != decoded.height) {
                    android.graphics.Bitmap.createScaledBitmap(decoded, targetW, targetH, true).also {
                        if (it !== decoded) decoded.recycle()
                    }
                } else {
                    decoded
                }
                val out = java.io.ByteArrayOutputStream()
                @Suppress("DEPRECATION")
                scaled.compress(android.graphics.Bitmap.CompressFormat.WEBP, quality, out)
                scaled.recycle()
                val bytes = out.toByteArray()
                if (bytes.isEmpty()) return trimmed
                val dataUri = "data:image/webp;base64," + java.util.Base64.getEncoder().encodeToString(bytes)
                mediaDataUriCache[cacheKey] = dataUri
                dataUri
            } catch (_: Throwable) {
                trimmed
            }
        }
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

    @Volatile
    private var myBannerUrl: String? = prefs.getString(KEY_MY_BANNER_URL, null)?.takeIf { it.isNotBlank() }

    private val _myHandleFlow = MutableStateFlow(resolveActiveLocalHandle())

    private val lanTransport: LanFriendTransport? = if (enableLanTransport) {
        LanFriendTransport(this)
    } else {
        FriendPeerRegistry.registerListener(this)
        null
    }

    override val localHandle: String
        get() = resolveActiveLocalHandle().ifBlank {
            if (enableLanTransport) fallbackDeviceHandle() else ""
        }

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

    private fun fallbackDeviceHandle(): String {
        val shortUuid = usernameManager.getInstallUuid().replace("-", "").take(6).lowercase()
        return "@scholar_${shortUuid.ifBlank { "peer01" }}"
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
        val rawHandle = resolveActiveLocalHandle()
        val handle = if (rawHandle.removePrefix("@").isNotBlank()) {
            rawHandle
        } else if (enableLanTransport) {
            fallbackDeviceHandle()
        } else {
            return null
        }
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
            bannerUrl = myBannerUrl,
            pronouns = myPronouns,
            studyStatus = effectiveStatus,
            currentSubject = _mySubjectFlow.value,
            followersCount = _followersFlow.value.size,
            followingCount = _followingFlow.value.size
        )
    }

    override fun syncMyProfile(
        displayName: String,
        pronouns: String,
        avatarUrl: String?,
        bannerUrl: String?
    ) {
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
        myAvatarUrl = encodeImageFileOrKeepUri(avatarUrl, maxWidth = 56, maxHeight = 56, quality = 55)
        editor.putString(KEY_MY_AVATAR_URL, myAvatarUrl)
        if (bannerUrl != null) {
            myBannerUrl = encodeImageFileOrKeepUri(bannerUrl, maxWidth = 140, maxHeight = 78, quality = 55)
            editor.putString(KEY_MY_BANNER_URL, myBannerUrl)
        }

        val explicitOffline = prefs.getBoolean(KEY_MY_PRESENCE_EXPLICIT_OFFLINE, false)
        if (!explicitOffline && _myPresenceStatusFlow.value == StudyPresenceStatus.OFFLINE) {
            _myPresenceStatusFlow.value = StudyPresenceStatus.OPEN_TO_COLLAB
            editor.putString(KEY_MY_PRESENCE, StudyPresenceStatus.OPEN_TO_COLLAB.name)
        }
        editor.apply()
        _myHandleFlow.value = resolveActiveLocalHandle()
        announceLocalPresence()
    }

    override fun refreshNetworkPeers() {
        announceLocalPresence()
        lanTransport?.refreshPeersNow()
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
                avatarUrl = profile.avatarUrl?.takeIf { it.isNotBlank() } ?: existing.avatarUrl,
                bannerUrl = profile.bannerUrl?.takeIf { it.isNotBlank() } ?: existing.bannerUrl,
                studyStatus = profile.studyStatus,
                currentSubject = profile.currentSubject,
                followersCount = profile.followersCount,
                followingCount = profile.followingCount
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

                val isMutual = _followingFlow.value.any {
                    it.id == normalizedSender.id || it.handle.equals(normSender, ignoreCase = true)
                }
                if (isMutual) {
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
                val updatedFollowers = _followersFlow.value.filterNot {
                    it.id == normalizedSender.id || it.handle.equals(normSender, ignoreCase = true)
                }
                persistFollowers(updatedFollowers)
                val updatedFriends = _friendsFlow.value.filterNot {
                    it.id == normalizedSender.id || it.handle.equals(normSender, ignoreCase = true)
                }
                if (updatedFriends != _friendsFlow.value) {
                    persistFriends(updatedFriends)
                }
            }
            "REMOVE_FOLLOWER" -> {
                val updatedFollowing = _followingFlow.value.filterNot {
                    it.id == normalizedSender.id || it.handle.equals(normSender, ignoreCase = true)
                }
                persistFollowing(updatedFollowing)
                val updatedFriends = _friendsFlow.value.filterNot {
                    it.id == normalizedSender.id || it.handle.equals(normSender, ignoreCase = true)
                }
                if (updatedFriends != _friendsFlow.value) {
                    persistFriends(updatedFriends)
                }
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

    override fun getMyHandleFlow(): Flow<String> = _myHandleFlow.asStateFlow()

    override suspend fun addFriend(handle: String, displayName: String): Result<FriendProfile> {
        val (extractedHandle, codeProfile) = FriendShareCodeCodec.decode(handle)
        if (FriendShareCodeCodec.isShareCode(handle) && codeProfile == null) {
            return Result.failure(IllegalArgumentException("Invalid or corrupted friend share code"))
        }
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

        val myNorm = resolveActiveLocalHandle()
        if (myNorm.isNotBlank() && cleanHandle.equals(myNorm, ignoreCase = true)) {
            return Result.failure(IllegalArgumentException("You cannot follow your own username"))
        }

        val currentFollowing = _followingFlow.value
        if (currentFollowing.any { it.handle.equals(cleanHandle, ignoreCase = true) }) {
            return Result.failure(IllegalStateException("Already following $cleanHandle"))
        }

        // 1. Discover live peer on LAN or in FriendPeerRegistry, or via Share Code
        val lanDiscovered = FriendPeerRegistry.findByHandle(cleanHandle)
            ?: if (lanTransport != null) {
                withContext(Dispatchers.IO) {
                    lanTransport.queryPeerByHandle(cleanHandle, timeoutMs = 750L)
                }
            } else {
                null
            }
            ?: codeProfile

        // 2. Check existing local followers / following or remote Supabase registry
        val knownLocalPeer = _followersFlow.value.find { it.handle.equals(cleanHandle, ignoreCase = true) }
            ?: _followingFlow.value.find { it.handle.equals(cleanHandle, ignoreCase = true) }

        val remotePeer = if (lanDiscovered == null && knownLocalPeer == null) {
            usernameManager.fetchProfileByUsername(handleBody).getOrNull()?.let { remote ->
                FriendProfile(
                    id = remote.id,
                    handle = "@${remote.username.lowercase()}",
                    displayName = remote.displayName,
                    studyStatus = StudyPresenceStatus.OFFLINE
                )
            }
        } else {
            null
        }

        val verifiedTarget = lanDiscovered ?: knownLocalPeer ?: remotePeer
            ?: return Result.failure(
                IllegalArgumentException("User $cleanHandle does not exist or is not online")
            )

        val trimmedDisplay = displayName.trim()
            .ifBlank { verifiedTarget.displayName.trim() }
            .ifBlank {
                handleBody.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
            }

        val newFollowed = verifiedTarget.copy(
            id = verifiedTarget.id.ifBlank { UUID.randomUUID().toString() },
            handle = cleanHandle,
            displayName = trimmedDisplay,
            joinedAt = System.currentTimeMillis(),
            isFavorite = false
        )

        persistFollowing(currentFollowing + newFollowed)

        // Mutual follow check: only add to Friends if they already follow us
        val isFollower = _followersFlow.value.any {
            it.id == newFollowed.id || it.handle.equals(cleanHandle, ignoreCase = true)
        }
        if (isFollower) {
            val currentFriends = _friendsFlow.value
            if (currentFriends.none { it.id == newFollowed.id || it.handle.equals(cleanHandle, ignoreCase = true) }) {
                persistFriends(currentFriends + newFollowed)
            }
        }

        val snap = getLocalProfileSnapshot()
        if (snap != null) {
            FriendPeerRegistry.dispatchConnectEvent("FOLLOW", snap, cleanHandle)
        }
        lanTransport?.sendConnectEvent("FOLLOW", cleanHandle)
        announceLocalPresence()

        return Result.success(newFollowed)
    }

    override suspend fun findUserByHandle(handle: String): FriendProfile? {
        val (extractedHandle, codeProfile) = FriendShareCodeCodec.decode(handle)
        if (FriendShareCodeCodec.isShareCode(handle) && codeProfile == null) {
            return null
        }
        val clean = usernameManager.cleanUsername(extractedHandle)
        if (clean.isBlank()) return null
        val normalized = "@$clean"

        // 1. Check live LAN / FriendPeerRegistry first so online peers always return live status & data
        val livePeer = FriendPeerRegistry.findByHandle(normalized)
            ?: if (lanTransport != null) {
                withContext(Dispatchers.IO) {
                    lanTransport.queryPeerByHandle(normalized, timeoutMs = 750L)
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
        // Only add to Friends list when the follow is mutual (they already follow us)
        val isFollower = _followersFlow.value.any {
            it.id == resolvedFriend.id || it.handle.equals(normalizedFriendHandle, ignoreCase = true)
        }
        if (isFollower) {
            val currentFriends = _friendsFlow.value
            if (currentFriends.none { it.id == resolvedFriend.id || it.handle.equals(normalizedFriendHandle, ignoreCase = true) }) {
                persistFriends(currentFriends + resolvedFriend)
            }
        }
        val snap = getLocalProfileSnapshot()
        if (snap != null) {
            FriendPeerRegistry.dispatchConnectEvent("FOLLOW", snap, normalizedFriendHandle)
        }
        lanTransport?.sendConnectEvent("FOLLOW", normalizedFriendHandle)
        announceLocalPresence()
    }

    override suspend fun unfollowUser(id: String) {
        val current = _followingFlow.value
        val removed = current.firstOrNull { it.id == id || it.handle.equals(id, ignoreCase = true) }
            ?: _friendsFlow.value.firstOrNull { it.id == id || it.handle.equals(id, ignoreCase = true) }
        val updated = current.filterNot {
            it.id == id ||
                it.handle.equals(id, ignoreCase = true) ||
                (removed != null && it.handle.equals(removed.handle, ignoreCase = true))
        }
        persistFollowing(updated)
        val updatedFriends = _friendsFlow.value.filterNot {
            it.id == id ||
                it.handle.equals(id, ignoreCase = true) ||
                (removed != null && it.handle.equals(removed.handle, ignoreCase = true))
        }
        if (updatedFriends != _friendsFlow.value) {
            persistFriends(updatedFriends)
        }
        if (removed != null) {
            val snap = getLocalProfileSnapshot()
            if (snap != null) {
                FriendPeerRegistry.dispatchConnectEvent("UNFOLLOW", snap, removed.handle)
            }
            lanTransport?.sendConnectEvent("UNFOLLOW", removed.handle)
            announceLocalPresence()
        }
    }

    override suspend fun removeFollower(id: String) {
        val current = _followersFlow.value
        val removed = current.firstOrNull { it.id == id || it.handle.equals(id, ignoreCase = true) }
        val updated = current.filterNot {
            it.id == id ||
                it.handle.equals(id, ignoreCase = true) ||
                (removed != null && it.handle.equals(removed.handle, ignoreCase = true))
        }
        persistFollowers(updated)
        val updatedFriends = _friendsFlow.value.filterNot {
            it.id == id ||
                it.handle.equals(id, ignoreCase = true) ||
                (removed != null && it.handle.equals(removed.handle, ignoreCase = true))
        }
        if (updatedFriends != _friendsFlow.value) {
            persistFriends(updatedFriends)
        }
        if (removed != null) {
            val snap = getLocalProfileSnapshot()
            if (snap != null) {
                FriendPeerRegistry.dispatchConnectEvent("REMOVE_FOLLOWER", snap, removed.handle)
            }
            lanTransport?.sendConnectEvent("REMOVE_FOLLOWER", removed.handle)
            announceLocalPresence()
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
            _myHandleFlow.value = resolveActiveLocalHandle()
            announceLocalPresence()
        }
        return result
    }

    override fun getUsernameCooldownDays(): Int = usernameManager.getRemainingCooldownDays()

    override fun canChangeUsername(): Pair<Boolean, String?> = usernameManager.canChangeUsername()

    override fun getMyHandle(): String = resolveActiveLocalHandle()

    override fun generateShareCode(): String {
        val snap = getLocalProfileSnapshot() ?: FriendProfile(
            id = usernameManager.getInstallUuid(),
            handle = resolveActiveLocalHandle().ifBlank { fallbackDeviceHandle() },
            displayName = myDisplayName.ifBlank { "Scholar" },
            pronouns = myPronouns,
            studyStatus = _myPresenceStatusFlow.value,
            currentSubject = _mySubjectFlow.value,
            followersCount = _followersFlow.value.size,
            followingCount = _followingFlow.value.size
        )
        return FriendShareCodeCodec.encode(snap)
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
