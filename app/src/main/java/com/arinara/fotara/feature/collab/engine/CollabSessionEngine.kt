// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.collab.engine

import com.arinara.fotara.feature.collab.model.CollabAccessMode
import com.arinara.fotara.feature.collab.model.CollabAudioBitrate
import com.arinara.fotara.feature.collab.model.CollabChatMessage
import com.arinara.fotara.feature.collab.model.CollabJoinPolicy
import com.arinara.fotara.feature.collab.model.CollabMessage
import com.arinara.fotara.feature.collab.model.CollabPeer
import com.arinara.fotara.feature.collab.model.CollabPrivacyPreset
import com.arinara.fotara.feature.collab.model.CollabRole
import com.arinara.fotara.feature.collab.model.CollabRoomCapacity
import com.arinara.fotara.feature.collab.model.CollabRoomCode
import com.arinara.fotara.feature.collab.model.CollabStroke
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import java.util.concurrent.ConcurrentLinkedQueue

/**
 * Lifecycle states of the live collaborative canvas engine.
 */
enum class CollabSessionState {
    DISCONNECTED,
    CONNECTING,
    IN_ROOM,
    RECONNECTING
}

/**
 * Pure state machine and real-time synchronization engine for live collaborative canvas rooms.
 * Implements Central Host authority, user-defined custom room codes (`FT-XXXX`) with collision checks,
 * real UserProfile identity display, 3-tier roles (`HOST` / `ADMIN` / `MEMBER`), Host-only pinned messages,
 * per-user volume (`0%–200%`) & bitrate settings, Host kick with permanent session ban, Host ownership transfer
 * (demoting previous Host to normal `MEMBER`), Host-leave room termination for all peers, and Global + Per-User
 * canvas drawing and sheet-creation restrictions with bidirectional auto-sync.
 */
class CollabSessionEngine(
    initialLocalPeerId: String = UUID.randomUUID().toString(),
    initialLocalUserTag: String = "Host",
    initialLocalColorHex: String = "#7A5AF8",
    initialLocalDeviceId: String = initialLocalPeerId,
    initialLocalPronouns: String = "",
    initialLocalUserHandle: String = "",
    enableLanTransport: Boolean = false
) : AutoCloseable {

    companion object {
        const val DEFAULT_PEER_TIMEOUT_MS: Long = 15_000L
        const val MAX_STROKE_BUFFER_SIZE: Int = 1_000
        const val MAX_CHAT_HISTORY_SIZE: Int = 200
        const val MIN_PEER_VOLUME: Float = 0.0f
        const val MAX_PEER_VOLUME: Float = 2.0f
        const val DEFAULT_PEER_VOLUME: Float = 1.0f
    }

    @Volatile
    var localPeerId: String = initialLocalPeerId
        private set

    @Volatile
    var localDeviceId: String = initialLocalDeviceId.ifBlank { initialLocalPeerId }
        private set

    @Volatile
    var localUserTag: String = initialLocalUserTag
        private set

    @Volatile
    var localPronouns: String = initialLocalPronouns
        private set

    @Volatile
    var localUserHandle: String = CollabPrivacyPreset.normalizeHandle(initialLocalUserHandle)
        private set

    @Volatile
    var localColorHex: String = initialLocalColorHex
        private set

    private val _state = MutableStateFlow(CollabSessionState.DISCONNECTED)
    val state: StateFlow<CollabSessionState> = _state.asStateFlow()

    private val _roomCode = MutableStateFlow<String?>(null)
    val roomCode: StateFlow<String?> = _roomCode.asStateFlow()

    private val _sessionToken = MutableStateFlow("")
    val sessionToken: StateFlow<String> = _sessionToken.asStateFlow()

    private val _hostPeerId = MutableStateFlow<String?>(null)
    val hostPeerId: StateFlow<String?> = _hostPeerId.asStateFlow()

    private val _adminPeerIds = MutableStateFlow<Set<String>>(emptySet())
    val adminPeerIds: StateFlow<Set<String>> = _adminPeerIds.asStateFlow()

    private val _localRole = MutableStateFlow(CollabRole.MEMBER)
    val localRole: StateFlow<CollabRole> = _localRole.asStateFlow()

    private val _activePeers = MutableStateFlow<Map<String, CollabPeer>>(emptyMap())
    val activePeers: StateFlow<Map<String, CollabPeer>> = _activePeers.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<CollabChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<CollabChatMessage>> = _chatMessages.asStateFlow()

    private val _pinnedChatMessageId = MutableStateFlow<String?>(null)
    val pinnedChatMessageId: StateFlow<String?> = _pinnedChatMessageId.asStateFlow()

    private val _pinnedChatMessage = MutableStateFlow<CollabChatMessage?>(null)
    val pinnedChatMessage: StateFlow<CollabChatMessage?> = _pinnedChatMessage.asStateFlow()

    private val _unreadChatCount = MutableStateFlow(0)
    val unreadChatCount: StateFlow<Int> = _unreadChatCount.asStateFlow()

    // Moderation & Ban state
    private val _bannedPeerIds = MutableStateFlow<Set<String>>(emptySet())
    val bannedPeerIds: StateFlow<Set<String>> = _bannedPeerIds.asStateFlow()

    private val _bannedDeviceIds = MutableStateFlow<Set<String>>(emptySet())
    val bannedDeviceIds: StateFlow<Set<String>> = _bannedDeviceIds.asStateFlow()

    private val _kickedFromRoomCodes = MutableStateFlow<Set<String>>(emptySet())
    val kickedFromRoomCodes: StateFlow<Set<String>> = _kickedFromRoomCodes.asStateFlow()

    private val _roomMutedPeerIds = MutableStateFlow<Set<String>>(emptySet())
    val roomMutedPeerIds: StateFlow<Set<String>> = _roomMutedPeerIds.asStateFlow()

    private val _chatBlockedPeerIds = MutableStateFlow<Set<String>>(emptySet())
    val chatBlockedPeerIds: StateFlow<Set<String>> = _chatBlockedPeerIds.asStateFlow()

    private val _isLocalUserChatBlocked = MutableStateFlow(false)
    val isLocalUserChatBlocked: StateFlow<Boolean> = _isLocalUserChatBlocked.asStateFlow()

    // Global & Per-User Canvas Drawing & Sheet Creation Restrictions
    private val _accessMode = MutableStateFlow(CollabAccessMode.HOST_AND_ADMIN)
    val accessMode: StateFlow<CollabAccessMode> = _accessMode.asStateFlow()

    private val _isGlobalDrawingDisabled = MutableStateFlow(false)
    val isGlobalDrawingDisabled: StateFlow<Boolean> = _isGlobalDrawingDisabled.asStateFlow()

    private val _isGlobalSheetCreationDisabled = MutableStateFlow(false)
    val isGlobalSheetCreationDisabled: StateFlow<Boolean> = _isGlobalSheetCreationDisabled.asStateFlow()

    private val _drawingDisabledPeerIds = MutableStateFlow<Set<String>>(emptySet())
    val drawingDisabledPeerIds: StateFlow<Set<String>> = _drawingDisabledPeerIds.asStateFlow()

    private val _sheetCreationDisabledPeerIds = MutableStateFlow<Set<String>>(emptySet())
    val sheetCreationDisabledPeerIds: StateFlow<Set<String>> = _sheetCreationDisabledPeerIds.asStateFlow()

    private val _canLocalUserDraw = MutableStateFlow(true)
    val canLocalUserDraw: StateFlow<Boolean> = _canLocalUserDraw.asStateFlow()

    private val _canLocalUserCreateSheets = MutableStateFlow(true)
    val canLocalUserCreateSheets: StateFlow<Boolean> = _canLocalUserCreateSheets.asStateFlow()

    // Audio & Per-User Volume Settings (0% - 200% gain + bitrate)
    private val _peerVolumes = MutableStateFlow<Map<String, Float>>(emptyMap())
    val peerVolumes: StateFlow<Map<String, Float>> = _peerVolumes.asStateFlow()

    private val _locallyMutedPeerIds = MutableStateFlow<Set<String>>(emptySet())
    val locallyMutedPeerIds: StateFlow<Set<String>> = _locallyMutedPeerIds.asStateFlow()

    private val _isLocalMicMuted = MutableStateFlow(false)
    val isLocalMicMuted: StateFlow<Boolean> = _isLocalMicMuted.asStateFlow()

    private val _audioBitrateKbps = MutableStateFlow(CollabAudioBitrate.BALANCED_32_KBPS)
    val audioBitrateKbps: StateFlow<Int> = _audioBitrateKbps.asStateFlow()

    private val _roomMaxBitrateKbps = MutableStateFlow(CollabAudioBitrate.HIGH_CLARITY_64_KBPS)
    val roomMaxBitrateKbps: StateFlow<Int> = _roomMaxBitrateKbps.asStateFlow()

    // Privacy & Join Access Control Settings (2.1 Update)
    private val _maxParticipants = MutableStateFlow(CollabRoomCapacity.DEFAULT_MAX_PARTICIPANTS)
    val maxParticipants: StateFlow<Int> = _maxParticipants.asStateFlow()

    private val _joinPolicy = MutableStateFlow(CollabJoinPolicy.OPEN)
    val joinPolicy: StateFlow<CollabJoinPolicy> = _joinPolicy.asStateFlow()

    private val _allowedUserHandles = MutableStateFlow<Set<String>>(emptySet())
    val allowedUserHandles: StateFlow<Set<String>> = _allowedUserHandles.asStateFlow()

    private val _invitedUserHandles = MutableStateFlow<Set<String>>(emptySet())
    val invitedUserHandles: StateFlow<Set<String>> = _invitedUserHandles.asStateFlow()

    private val _hostFriendHandles = MutableStateFlow<Set<String>>(emptySet())
    val hostFriendHandles: StateFlow<Set<String>> = _hostFriendHandles.asStateFlow()

    private val _savedPrivacyPresets = MutableStateFlow<List<CollabPrivacyPreset>>(emptyList())
    val savedPrivacyPresets: StateFlow<List<CollabPrivacyPreset>> = _savedPrivacyPresets.asStateFlow()

    private val _activePresetId = MutableStateFlow<String?>(null)
    val activePresetId: StateFlow<String?> = _activePresetId.asStateFlow()

    private val _lastErrorMessage = MutableStateFlow<String?>(null)
    val lastErrorMessage: StateFlow<String?> = _lastErrorMessage.asStateFlow()

    @Volatile
    var isChatSheetOpen: Boolean = false
        private set

    private val _outgoingMessages = MutableSharedFlow<CollabMessage>(extraBufferCapacity = 64)
    val outgoingMessages: SharedFlow<CollabMessage> = _outgoingMessages.asSharedFlow()

    // Thread-safe vector stroke and canvas element merger buffers
    private val strokeBuffer = ConcurrentLinkedQueue<CollabStroke>()
    private val textSyncBuffer = ConcurrentLinkedQueue<CollabMessage.TextElementSync>()
    private val imageSyncBuffer = ConcurrentLinkedQueue<CollabMessage.ImageElementSync>()
    private val removeSyncBuffer = ConcurrentLinkedQueue<CollabMessage.ElementsRemoveSync>()
    private val _bufferedStrokesCount = MutableStateFlow(0)
    val bufferedStrokesCount: StateFlow<Int> = _bufferedStrokesCount.asStateFlow()

    // Deduplication ring for processed stroke and element sync payloads
    private val processedStrokeIds = LinkedHashSet<String>()

    /**
     * Optional callback invoked when a remote voice audio frame arrives for real-time playback.
     */
    @Volatile
    var onAudioFrameReceived: ((CollabMessage.AudioStreamFrame) -> Unit)? = null

    /**
     * Optional callback invoked on the Host when a new participant joins so the Host can push
     * a snapshot of existing canvas elements (strokes, texts, images).
     */
    @Volatile
    var onPeerJoinedNeedSnapshot: ((String) -> Unit)? = null

    private val lanTransport: LanCollabTransport? = if (enableLanTransport) {
        LanCollabTransport(
            onMessageReceived = { incoming -> receiveMessage(incoming) },
            onHostConnectionLost = { handleHostConnectionLost() },
            onConnectedToHost = { handleConnectedToHost() }
        )
    } else {
        null
    }

    @Synchronized
    private fun handleConnectedToHost() {
        val currentCode = _roomCode.value ?: return
        if (!isSessionActive || _localRole.value == CollabRole.HOST) return
        dispatchMessage(
            CollabMessage.JoinRoom(
                roomCode = currentCode,
                userTag = localUserTag,
                colorHex = localColorHex,
                peerId = localPeerId,
                deviceId = localDeviceId,
                pronouns = localPronouns,
                userHandle = localUserHandle,
                audioBitrateKbps = _audioBitrateKbps.value,
                sessionToken = _sessionToken.value
            )
        )
        dispatchMessage(
            CollabMessage.PeerJoined(
                peerId = localPeerId,
                userTag = localUserTag,
                colorHex = localColorHex,
                deviceId = localDeviceId,
                pronouns = localPronouns,
                userHandle = localUserHandle,
                role = _localRole.value,
                audioBitrateKbps = _audioBitrateKbps.value,
                sessionToken = _sessionToken.value
            )
        )
    }

    /**
     * Returns true if a collaborative room is currently active.
     */
    val isSessionActive: Boolean
        get() = _state.value == CollabSessionState.IN_ROOM && _roomCode.value != null

    /**
     * Returns true if the local user is allowed to transmit live microphone audio.
     */
    val canLocalUserSpeak: Boolean
        get() = isSessionActive && !_isLocalMicMuted.value && localPeerId !in _roomMutedPeerIds.value

    /**
     * Optional transport bridge callback for external networks (WebSockets / WebRTC data channels).
     */
    @Volatile
    var transportBridge: ((CollabMessage) -> Unit)? = null

    /**
     * Direct low-latency callback invoked when remote or local peer cursors move or peers change,
     * allowing native canvas rendering at 60fps without Compose full-screen recomposition.
     */
    @Volatile
    var onPeerCursorsUpdated: ((Collection<CollabPeer>) -> Unit)? = null

    override fun close() {
        leaveRoom()
        lanTransport?.close()
        transportBridge = null
        onAudioFrameReceived = null
        onPeerJoinedNeedSnapshot = null
        onPeerCursorsUpdated = null
        strokeBuffer.clear()
        textSyncBuffer.clear()
        imageSyncBuffer.clear()
        removeSyncBuffer.clear()
        _bufferedStrokesCount.value = 0
    }

    private fun resolveEffectiveUserTag(rawTag: String, rawHandle: String): String {
        val cleanTag = rawTag.trim()
        val cleanHandle = rawHandle.trim().removePrefix("@")
        val existingTag = localUserTag.trim()
        return when {
            cleanTag.isNotBlank() &&
                !cleanTag.equals("Fotara User", ignoreCase = true) &&
                !cleanTag.equals("My Profile", ignoreCase = true) -> cleanTag
            cleanHandle.isNotBlank() -> cleanHandle
            existingTag.isNotBlank() &&
                !existingTag.equals("Fotara User", ignoreCase = true) &&
                !existingTag.equals("My Profile", ignoreCase = true) -> existingTag
            cleanTag.isNotBlank() -> cleanTag
            existingTag.isNotBlank() -> existingTag
            else -> "Fotara User"
        }
    }

    fun clearErrorMessage() {
        _lastErrorMessage.value = null
    }

    /**
     * Synchronizes the local user's real [com.arinara.fotara.data.model.UserProfile] display name
     * and pronouns into the active collaborative session.
     */
    @Synchronized
    fun syncUserProfile(
        realName: String,
        pronouns: String = localPronouns,
        deviceId: String = localDeviceId,
        userHandle: String = localUserHandle
    ) {
        if (userHandle.isNotBlank()) {
            localUserHandle = CollabPrivacyPreset.normalizeHandle(userHandle)
        }
        val resolved = resolveEffectiveUserTag(realName, localUserHandle)
        localUserTag = resolved
        localPronouns = pronouns.trim()
        if (deviceId.isNotBlank()) {
            localDeviceId = deviceId.trim()
        }

        val currentPeer = _activePeers.value[localPeerId]
        if (currentPeer != null) {
            val updated = currentPeer.copy(
                userTag = localUserTag,
                pronouns = localPronouns,
                deviceId = localDeviceId,
                userHandle = localUserHandle
            )
            _activePeers.update { it + (localPeerId to updated) }
            val currentCode = _roomCode.value
            if (!currentCode.isNullOrBlank() && _hostPeerId.value == localPeerId) {
                CollabRoomRegistry.updateHostOwner(currentCode, localPeerId, localUserTag)
            }
            if (isSessionActive) {
                dispatchMessage(
                    CollabMessage.PeerJoined(
                        peerId = localPeerId,
                        userTag = localUserTag,
                        colorHex = localColorHex,
                        deviceId = localDeviceId,
                        pronouns = localPronouns,
                        userHandle = localUserHandle,
                        role = _localRole.value,
                        audioBitrateKbps = _audioBitrateKbps.value,
                        sessionToken = _sessionToken.value
                    )
                )
            }
        }
    }

    /**
     * Creates a new collaborative study room with a user-defined custom code `FT-XXXX`
     * (or generates a random code if [customCode] is null/blank).
     * Guarded:
     * 1. If a session is already active, returns the existing room code without overwriting.
     * 2. If [customCode] is already hosted by another active Host on the network, sets [lastErrorMessage]
     *    and returns `""` (empty string) to indicate collision rejection.
     */
    @Synchronized
    fun createRoom(
        userTag: String = localUserTag,
        colorHex: String = localColorHex,
        customCode: String? = null,
        pronouns: String = localPronouns,
        userHandle: String = localUserHandle
    ): String {
        if (userHandle.isNotBlank()) {
            localUserHandle = CollabPrivacyPreset.normalizeHandle(userHandle)
        }
        val existingCode = _roomCode.value
        if (_state.value == CollabSessionState.IN_ROOM && !existingCode.isNullOrBlank()) {
            return existingCode
        }

        val candidateCode = if (!customCode.isNullOrBlank()) {
            val normalized = CollabRoomCode.normalize(customCode)
            if (!CollabRoomCode.isValid(normalized)) {
                _lastErrorMessage.value = "Invalid room code format. Enter 4 letters or digits (FT-XXXX)."
                return ""
            }
            // Pre-creation collision check against active hosted rooms
            val existingHost = CollabRoomRegistry.findRoom(normalized)
            if (existingHost != null && existingHost.hostPeerId != localPeerId) {
                _lastErrorMessage.value =
                    "Room code $normalized is already active (hosted by ${existingHost.hostUserName}). Please choose another code."
                return ""
            }
            normalized
        } else {
            var generated = CollabRoomCode.generateRandomCode()
            var attempts = 0
            while (CollabRoomRegistry.findRoom(generated) != null && attempts < 10) {
                generated = CollabRoomCode.generateRandomCode()
                attempts++
            }
            generated
        }

        localUserTag = resolveEffectiveUserTag(userTag, localUserHandle)
        localPronouns = pronouns.trim()
        localColorHex = colorHex
        _lastErrorMessage.value = null

        _state.value = CollabSessionState.CONNECTING
        val newToken = UUID.randomUUID().toString()
        val now = System.currentTimeMillis()

        _roomCode.value = candidateCode
        _sessionToken.value = newToken
        _hostPeerId.value = localPeerId
        _adminPeerIds.value = emptySet()
        _localRole.value = CollabRole.HOST

        _bannedPeerIds.value = emptySet()
        _bannedDeviceIds.value = emptySet()
        _roomMutedPeerIds.value = emptySet()
        _chatBlockedPeerIds.value = emptySet()
        _isLocalUserChatBlocked.value = false
        _accessMode.value = CollabAccessMode.HOST_AND_ADMIN
        _isGlobalDrawingDisabled.value = false
        _isGlobalSheetCreationDisabled.value = false
        _drawingDisabledPeerIds.value = emptySet()
        _sheetCreationDisabledPeerIds.value = emptySet()
        _canLocalUserDraw.value = true
        _canLocalUserCreateSheets.value = true
        _pinnedChatMessageId.value = null
        _pinnedChatMessage.value = null

        lanTransport?.startHostServer()
        val boundLanPort = lanTransport?.localTcpPort ?: 0

        CollabRoomRegistry.registerHostRoom(
            roomCode = candidateCode,
            sessionToken = newToken,
            hostPeerId = localPeerId,
            hostUserName = localUserTag,
            createdAt = now,
            lanPort = boundLanPort,
            engineRef = this
        )

        _activePeers.value = mapOf(
            localPeerId to CollabPeer(
                peerId = localPeerId,
                userTag = localUserTag,
                colorHex = localColorHex,
                lastActiveMs = now,
                deviceId = localDeviceId,
                pronouns = localPronouns,
                userHandle = localUserHandle,
                role = CollabRole.HOST,
                audioBitrateKbps = _audioBitrateKbps.value
            )
        )

        strokeBuffer.clear()
        textSyncBuffer.clear()
        imageSyncBuffer.clear()
        removeSyncBuffer.clear()
        processedStrokeIds.clear()
        _bufferedStrokesCount.value = 0
        _chatMessages.value = emptyList()
        _unreadChatCount.value = 0

        _state.value = CollabSessionState.IN_ROOM
        onPeerCursorsUpdated?.invoke(_activePeers.value.values)

        val announce = CollabMessage.HostAnnounce(
            roomCode = candidateCode,
            sessionToken = newToken,
            hostPeerId = localPeerId,
            hostUserName = localUserTag,
            lanPort = boundLanPort,
            maxBitrateKbps = _roomMaxBitrateKbps.value,
            createdAt = now
        )
        dispatchMessage(announce)

        val peerJoinedMessage = CollabMessage.PeerJoined(
            peerId = localPeerId,
            userTag = localUserTag,
            colorHex = localColorHex,
            deviceId = localDeviceId,
            pronouns = localPronouns,
            userHandle = localUserHandle,
            role = CollabRole.HOST,
            audioBitrateKbps = _audioBitrateKbps.value,
            sessionToken = newToken
        )
        dispatchMessage(peerJoinedMessage)

        return candidateCode
    }

    /**
     * Convenience helper for creating a room with a user-entered custom code `FT-XXXX`.
     * Returns true if the room was created, or false if the code is invalid or already in use.
     */
    @Synchronized
    fun createCustomRoom(
        customCode: String,
        userTag: String = localUserTag,
        colorHex: String = localColorHex,
        pronouns: String = localPronouns,
        userHandle: String = localUserHandle
    ): Boolean {
        val created = createRoom(
            userTag = userTag,
            colorHex = colorHex,
            customCode = customCode,
            pronouns = pronouns,
            userHandle = userHandle
        )
        return created.isNotBlank()
    }

    /**
     * Joins an existing collaborative study room by validating the 6-digit room code `FT-XXXX`.
     * Guarded:
     * 1. If another session is already active, rejects joining until [leaveRoom] is called.
     * 2. If the local user/device was kicked and banned from [code], rejects joining with a clear error.
     * 3. If [requireActiveHost] is true and no Host is hosting [code], rejects joining with Room Not Found.
     */
    @Synchronized
    fun joinRoom(
        code: String,
        userTag: String = localUserTag,
        colorHex: String = localColorHex,
        pronouns: String = localPronouns,
        requireActiveHost: Boolean = true,
        userHandle: String = localUserHandle
    ): Boolean {
        if (userHandle.isNotBlank()) {
            localUserHandle = CollabPrivacyPreset.normalizeHandle(userHandle)
        }
        if (!CollabRoomCode.isValid(code)) {
            _lastErrorMessage.value = "Invalid room code format."
            return false
        }

        val normalizedCode = CollabRoomCode.normalize(code)
        val existingCode = _roomCode.value
        if (_state.value == CollabSessionState.IN_ROOM && !existingCode.isNullOrBlank()) {
            return existingCode == normalizedCode
        }

        // Permanent Session Ban Check ("Cannot Join Again")
        if (normalizedCode in _kickedFromRoomCodes.value ||
            CollabRoomRegistry.isPeerBanned(normalizedCode, localPeerId, localDeviceId)
        ) {
            _lastErrorMessage.value =
                "You cannot join $normalizedCode because the host removed you from this session."
            return false
        }

        val resolvedUserTag = resolveEffectiveUserTag(userTag, localUserHandle)
        var hostedRecord = CollabRoomRegistry.findRoom(normalizedCode)
        if (hostedRecord != null && hostedRecord.hostPeerId == localPeerId && _state.value != CollabSessionState.IN_ROOM) {
            CollabRoomRegistry.unregisterRoom(normalizedCode, localPeerId)
            hostedRecord = null
        }
        if (hostedRecord == null && lanTransport != null) {
            hostedRecord = lanTransport.probeRoomHost(
                roomCode = normalizedCode,
                joinerPeerId = localPeerId,
                joinerDeviceId = localDeviceId,
                joinerUserName = resolvedUserTag,
                timeoutMs = 650L
            )
        }
        if (requireActiveHost && hostedRecord == null) {
            _lastErrorMessage.value =
                "Room $normalizedCode not found. No active host has created this room yet — tap Create to host it."
            return false
        }

        // Pre-join Host Privacy & Capacity Verification when an in-process Host is available
        hostedRecord?.engineRef?.let { hostEngine ->
            if (hostEngine !== this) {
                val rejectionReason = hostEngine.evaluateJoinAttempt(
                    peerId = localPeerId,
                    deviceId = localDeviceId,
                    userTag = resolvedUserTag,
                    userHandle = localUserHandle
                )
                if (rejectionReason != null) {
                    if (rejectionReason == CollabMessage.JoinRejected.REASON_BANNED_BY_HOST) {
                        _kickedFromRoomCodes.update { it + normalizedCode }
                    }
                    _lastErrorMessage.value = formatJoinRejectionMessage(normalizedCode, rejectionReason)
                    return false
                }
            }
        }

        localUserTag = resolvedUserTag
        localPronouns = pronouns.trim()
        localColorHex = colorHex
        _lastErrorMessage.value = null

        _state.value = CollabSessionState.CONNECTING
        _roomCode.value = normalizedCode
        val resolvedToken = hostedRecord?.sessionToken ?: ""
        _sessionToken.value = resolvedToken
        _hostPeerId.value = hostedRecord?.hostPeerId
        _adminPeerIds.value = emptySet()
        _localRole.value = CollabRole.MEMBER

        _roomMutedPeerIds.value = emptySet()
        _chatBlockedPeerIds.value = emptySet()
        _isLocalUserChatBlocked.value = false
        _accessMode.value = CollabAccessMode.HOST_AND_ADMIN
        _isGlobalDrawingDisabled.value = false
        _isGlobalSheetCreationDisabled.value = false
        _drawingDisabledPeerIds.value = emptySet()
        _sheetCreationDisabledPeerIds.value = emptySet()
        _canLocalUserDraw.value = true
        _canLocalUserCreateSheets.value = true
        _pinnedChatMessageId.value = null
        _pinnedChatMessage.value = null

        strokeBuffer.clear()
        textSyncBuffer.clear()
        imageSyncBuffer.clear()
        removeSyncBuffer.clear()
        processedStrokeIds.clear()
        _bufferedStrokesCount.value = 0
        _chatMessages.value = emptyList()
        _unreadChatCount.value = 0

        val now = System.currentTimeMillis()
        val initialPeers = mutableMapOf(
            localPeerId to CollabPeer(
                peerId = localPeerId,
                userTag = localUserTag,
                colorHex = localColorHex,
                lastActiveMs = now,
                deviceId = localDeviceId,
                pronouns = localPronouns,
                userHandle = localUserHandle,
                role = CollabRole.MEMBER,
                audioBitrateKbps = _audioBitrateKbps.value
            )
        )
        if (hostedRecord != null && hostedRecord.hostPeerId != localPeerId) {
            initialPeers[hostedRecord.hostPeerId] = CollabPeer(
                peerId = hostedRecord.hostPeerId,
                userTag = hostedRecord.hostUserName,
                colorHex = "#7A5AF8",
                lastActiveMs = now,
                role = CollabRole.HOST
            )
        }
        _activePeers.value = initialPeers

        _state.value = CollabSessionState.IN_ROOM
        onPeerCursorsUpdated?.invoke(_activePeers.value.values)
        lanTransport?.startJoinerDiscovery()
        if (hostedRecord != null && hostedRecord.lanIp.isNotBlank() && hostedRecord.lanPort > 0) {
            lanTransport?.connectToHost(hostedRecord.lanIp, hostedRecord.lanPort)
        }

        val whoHas = CollabMessage.WhoHasRoom(
            roomCode = normalizedCode,
            joinerPeerId = localPeerId,
            joinerDeviceId = localDeviceId,
            joinerUserName = localUserTag
        )
        dispatchMessage(whoHas)

        val joinMessage = CollabMessage.JoinRoom(
            roomCode = normalizedCode,
            userTag = localUserTag,
            colorHex = localColorHex,
            peerId = localPeerId,
            deviceId = localDeviceId,
            pronouns = localPronouns,
            userHandle = localUserHandle,
            audioBitrateKbps = _audioBitrateKbps.value,
            sessionToken = resolvedToken
        )
        dispatchMessage(joinMessage)

        val peerJoinedMessage = CollabMessage.PeerJoined(
            peerId = localPeerId,
            userTag = localUserTag,
            colorHex = localColorHex,
            deviceId = localDeviceId,
            pronouns = localPronouns,
            userHandle = localUserHandle,
            role = CollabRole.MEMBER,
            audioBitrateKbps = _audioBitrateKbps.value,
            sessionToken = resolvedToken
        )
        dispatchMessage(peerJoinedMessage)

        // If an in-process Host engine is registered for this room code, deliver the join directly
        hostedRecord?.engineRef?.let { hostEngine ->
            if (hostEngine !== this) {
                hostEngine.receiveMessage(joinMessage)
                hostEngine.receiveMessage(peerJoinedMessage)
            }
        }

        return true
    }

    /**
     * Leaves the active collaborative room and resets session state to DISCONNECTED.
     * Basic Host-Leave Logic: If the local user is the current [CollabRole.HOST], broadcasts
     * [CollabMessage.RoomClosed] so that every connected participant automatically leaves as well.
     */
    @Synchronized
    fun leaveRoom() {
        val currentCode = _roomCode.value
        val currentToken = _sessionToken.value
        val wasHost = _hostPeerId.value == localPeerId || _localRole.value == CollabRole.HOST

        if (_state.value != CollabSessionState.DISCONNECTED) {
            val closingMessages = mutableListOf<CollabMessage>()
            if (wasHost && !currentCode.isNullOrBlank()) {
                CollabRoomRegistry.unregisterRoom(currentCode, localPeerId)
                closingMessages.add(
                    CollabMessage.RoomClosed(
                        roomCode = currentCode,
                        sessionToken = currentToken,
                        hostPeerId = localPeerId,
                        reason = "HOST_LEFT"
                    )
                )
            }
            closingMessages.add(
                CollabMessage.LeaveRoom(
                    peerId = localPeerId,
                    sessionToken = currentToken
                )
            )
            for (msg in closingMessages) {
                _outgoingMessages.tryEmit(msg)
                transportBridge?.invoke(msg)
            }
            if (lanTransport != null) {
                lanTransport.flushAndDisconnectSession(closingMessages)
            }
        }

        resetRoomStateToDisconnected()
    }

    /**
     * Invoked when the joiner's TCP connection to the Central Host closes unexpectedly
     * (e.g., Host exited the room or closed the app).
     */
    @Synchronized
    fun handleHostConnectionLost() {
        if (_state.value == CollabSessionState.IN_ROOM && _localRole.value != CollabRole.HOST) {
            _roomCode.value?.let { code ->
                CollabRoomRegistry.unregisterRoom(code, _hostPeerId.value)
            }
            _lastErrorMessage.value = "Session ended because the host left the room."
            resetRoomStateToDisconnected()
        }
    }

    private fun resetRoomStateToDisconnected() {
        lanTransport?.disconnectSessionSockets()
        _state.value = CollabSessionState.DISCONNECTED
        _roomCode.value = null
        _sessionToken.value = ""
        _hostPeerId.value = null
        _adminPeerIds.value = emptySet()
        _localRole.value = CollabRole.MEMBER
        _activePeers.value = emptyMap()
        _chatMessages.value = emptyList()
        _pinnedChatMessageId.value = null
        _pinnedChatMessage.value = null
        _unreadChatCount.value = 0
        _roomMutedPeerIds.value = emptySet()
        _chatBlockedPeerIds.value = emptySet()
        _isLocalUserChatBlocked.value = false
        _isLocalMicMuted.value = false
        _isGlobalDrawingDisabled.value = false
        _isGlobalSheetCreationDisabled.value = false
        _drawingDisabledPeerIds.value = emptySet()
        _sheetCreationDisabledPeerIds.value = emptySet()
        _canLocalUserDraw.value = true
        _canLocalUserCreateSheets.value = true
        isChatSheetOpen = false
        strokeBuffer.clear()
        textSyncBuffer.clear()
        imageSyncBuffer.clear()
        removeSyncBuffer.clear()
        processedStrokeIds.clear()
        _bufferedStrokesCount.value = 0
        onPeerCursorsUpdated?.invoke(emptyList())
    }

    // =========================================================================
    // ROLE MANAGEMENT: ADMIN PROMOTION & HOST OWNERSHIP TRANSFER
    // =========================================================================

    /**
     * Promotes a Member to [CollabRole.ADMIN] or demotes an Admin back to [CollabRole.MEMBER].
     * Strictly restricted to the current [CollabRole.HOST].
     */
    @Synchronized
    fun setPeerAdmin(targetPeerId: String, isAdmin: Boolean): Boolean {
        if (!isSessionActive) return false
        if (_localRole.value != CollabRole.HOST) return false
        if (targetPeerId == localPeerId || targetPeerId == _hostPeerId.value) return false
        val targetPeer = _activePeers.value[targetPeerId] ?: return false

        val newRole = if (isAdmin) CollabRole.ADMIN else CollabRole.MEMBER
        if (isAdmin) {
            _adminPeerIds.update { it + targetPeerId }
            // When accessMode is HOST_AND_ADMIN, promoting to Admin exempts them from Member-only global locks
            if (_accessMode.value == CollabAccessMode.HOST_AND_ADMIN) {
                _drawingDisabledPeerIds.update { it - targetPeerId }
                _sheetCreationDisabledPeerIds.update { it - targetPeerId }
            }
        } else {
            _adminPeerIds.update { it - targetPeerId }
            // If global lock is active, demoting to Member subjects them to the global lock
            if (_isGlobalDrawingDisabled.value) {
                _drawingDisabledPeerIds.update { it + targetPeerId }
            }
            if (_isGlobalSheetCreationDisabled.value) {
                _sheetCreationDisabledPeerIds.update { it + targetPeerId }
            }
        }

        refreshPeersAndPermissions()
        appendSystemChatNotice(
            if (isAdmin) "${targetPeer.userTag} was promoted to Admin by $localUserTag."
            else "${targetPeer.userTag} is no longer an Admin."
        )

        dispatchMessage(
            CollabMessage.SetPeerRole(
                sessionToken = _sessionToken.value,
                hostPeerId = localPeerId,
                targetPeerId = targetPeerId,
                newRole = newRole
            )
        )
        broadcastCanvasPermissionSync()
        return true
    }

    /**
     * Transfers Central Host ownership to [newHostPeerId].
     * Mandatory contract:
     * 1. [newHostPeerId] becomes the new [CollabRole.HOST].
     * 2. The previous Host ([localPeerId]) immediately becomes a normal [CollabRole.MEMBER] (NOT [CollabRole.ADMIN]).
     */
    @Synchronized
    fun transferHostOwnership(newHostPeerId: String): Boolean {
        if (!isSessionActive) return false
        if (_localRole.value != CollabRole.HOST) return false
        if (newHostPeerId == localPeerId) return false
        val newHostPeer = _activePeers.value[newHostPeerId] ?: return false

        val oldHostPeerId = localPeerId
        val now = System.currentTimeMillis()

        // 1. Assign new Host
        _hostPeerId.value = newHostPeerId
        // 2. Ensure neither newHostPeerId nor oldHostPeerId is in adminPeerIds
        //    (old Host becomes normal MEMBER, not ADMIN; new Host is HOST)
        _adminPeerIds.update { it - newHostPeerId - oldHostPeerId }
        _localRole.value = CollabRole.MEMBER

        // 3. Clear any moderation/canvas restrictions on the new Host
        _roomMutedPeerIds.update { it - newHostPeerId }
        _chatBlockedPeerIds.update { it - newHostPeerId }
        _drawingDisabledPeerIds.update { it - newHostPeerId }
        _sheetCreationDisabledPeerIds.update { it - newHostPeerId }

        // 4. If global locks are active, old Host (now a normal MEMBER) inherits the global lock state
        if (_isGlobalDrawingDisabled.value) {
            _drawingDisabledPeerIds.update { it + oldHostPeerId }
        }
        if (_isGlobalSheetCreationDisabled.value) {
            _sheetCreationDisabledPeerIds.update { it + oldHostPeerId }
        }

        _roomCode.value?.let { code ->
            CollabRoomRegistry.updateHostOwner(code, newHostPeerId, newHostPeer.userTag)
        }

        refreshPeersAndPermissions()
        appendSystemChatNotice("$localUserTag transferred host ownership to ${newHostPeer.userTag}.")

        dispatchMessage(
            CollabMessage.TransferHostOwnership(
                sessionToken = _sessionToken.value,
                oldHostPeerId = oldHostPeerId,
                newHostPeerId = newHostPeerId,
                timestamp = now
            )
        )
        return true
    }

    // =========================================================================
    // MODERATION: ROOM MUTE, BLOCK CHAT, DELETE CHAT, PIN CHAT, KICK & BAN
    // =========================================================================

    /**
     * Returns true if [actorPeerId] has authority to moderate (Room-Mute / Block Chat) [targetPeerId].
     * - Host can moderate any non-Host participant (Admins and Members).
     * - Admin can moderate normal Members only (cannot moderate Host or other Admins).
     */
    fun canModeratePeer(
        targetPeerId: String,
        actorPeerId: String = localPeerId
    ): Boolean {
        if (!isSessionActive) return false
        if (actorPeerId == targetPeerId) return false
        val currentHost = _hostPeerId.value
        if (targetPeerId == currentHost) return false
        val actorRole = _activePeers.value[actorPeerId]?.role
        val targetRole = _activePeers.value[targetPeerId]?.role

        return when {
            actorPeerId == currentHost ||
                actorRole == CollabRole.HOST ||
                (actorPeerId == localPeerId && _localRole.value == CollabRole.HOST) -> true
            actorPeerId in _adminPeerIds.value ||
                actorRole == CollabRole.ADMIN ||
                (actorPeerId == localPeerId && _localRole.value == CollabRole.ADMIN) ->
                targetPeerId !in _adminPeerIds.value &&
                    targetRole != CollabRole.ADMIN &&
                    targetRole != CollabRole.HOST
            currentHost == null && actorPeerId != localPeerId -> true
            else -> false
        }
    }

    /**
     * Returns true if [actorPeerId] has authority to toggle canvas drawing/sheet restrictions for [targetPeerId].
     */
    fun canRestrictPeerCanvas(
        targetPeerId: String,
        actorPeerId: String = localPeerId
    ): Boolean {
        if (!isSessionActive) return false
        if (actorPeerId == targetPeerId) return false
        val currentHost = _hostPeerId.value
        if (targetPeerId == currentHost) return false
        val actorRole = _activePeers.value[actorPeerId]?.role
        val targetRole = _activePeers.value[targetPeerId]?.role

        return when {
            actorPeerId == currentHost ||
                actorRole == CollabRole.HOST ||
                (actorPeerId == localPeerId && _localRole.value == CollabRole.HOST) -> true
            actorPeerId in _adminPeerIds.value ||
                actorRole == CollabRole.ADMIN ||
                (actorPeerId == localPeerId && _localRole.value == CollabRole.ADMIN) -> {
                _accessMode.value == CollabAccessMode.HOST_AND_ADMIN &&
                    targetPeerId !in _adminPeerIds.value &&
                    targetRole != CollabRole.ADMIN &&
                    targetRole != CollabRole.HOST
            }
            currentHost == null && actorPeerId != localPeerId -> true
            else -> false
        }
    }

    /**
     * Server-mutes or unmutes [targetPeerId]'s microphone across the entire room.
     * Allowed for [CollabRole.HOST] and [CollabRole.ADMIN] (subject to hierarchy check).
     */
    @Synchronized
    fun setPeerRoomMuted(targetPeerId: String, isMuted: Boolean): Boolean {
        if (!canModeratePeer(targetPeerId = targetPeerId, actorPeerId = localPeerId)) return false
        if (isMuted) {
            _roomMutedPeerIds.update { it + targetPeerId }
        } else {
            _roomMutedPeerIds.update { it - targetPeerId }
        }
        refreshPeersAndPermissions()
        dispatchMessage(
            CollabMessage.ModMutePeer(
                sessionToken = _sessionToken.value,
                actorPeerId = localPeerId,
                targetPeerId = targetPeerId,
                isMuted = isMuted
            )
        )
        return true
    }

    /**
     * Blocks or unblocks [targetPeerId] from sending chat messages in the room.
     * Allowed for [CollabRole.HOST] and [CollabRole.ADMIN] (subject to hierarchy check).
     */
    @Synchronized
    fun setPeerChatBlocked(targetPeerId: String, isBlocked: Boolean): Boolean {
        if (!canModeratePeer(targetPeerId = targetPeerId, actorPeerId = localPeerId)) return false
        if (isBlocked) {
            _chatBlockedPeerIds.update { it + targetPeerId }
        } else {
            _chatBlockedPeerIds.update { it - targetPeerId }
        }
        refreshPeersAndPermissions()
        dispatchMessage(
            CollabMessage.BlockPeerChat(
                sessionToken = _sessionToken.value,
                actorPeerId = localPeerId,
                targetPeerId = targetPeerId,
                isBlocked = isBlocked
            )
        )
        return true
    }

    /**
     * Deletes a chat message for all participants in the room.
     * Allowed for both [CollabRole.HOST] and [CollabRole.ADMIN].
     */
    @Synchronized
    fun deleteChatMessage(messageId: String): Boolean {
        if (!isSessionActive) return false
        val role = _localRole.value
        if (role != CollabRole.HOST && role != CollabRole.ADMIN) return false
        if (_chatMessages.value.none { it.messageId == messageId }) return false

        _chatMessages.update { list -> list.filterNot { it.messageId == messageId } }
        if (_pinnedChatMessageId.value == messageId) {
            _pinnedChatMessageId.value = null
            _pinnedChatMessage.value = null
        }

        dispatchMessage(
            CollabMessage.DeleteChatMessage(
                sessionToken = _sessionToken.value,
                actorPeerId = localPeerId,
                messageId = messageId
            )
        )
        return true
    }

    /**
     * Pins or unpins (`null`) a chat message at the top of `CollabChatSheet`.
     * Strictly restricted to [CollabRole.HOST] ("set a message as pin [ only host ]").
     */
    @Synchronized
    fun pinChatMessage(messageId: String?): Boolean {
        if (!isSessionActive) return false
        if (_localRole.value != CollabRole.HOST) return false

        if (messageId != null) {
            val targetMsg = _chatMessages.value.firstOrNull { it.messageId == messageId } ?: return false
            _pinnedChatMessageId.value = messageId
            _pinnedChatMessage.value = targetMsg
        } else {
            _pinnedChatMessageId.value = null
            _pinnedChatMessage.value = null
        }

        dispatchMessage(
            CollabMessage.PinChatMessage(
                sessionToken = _sessionToken.value,
                hostPeerId = localPeerId,
                pinnedMessageId = messageId
            )
        )
        return true
    }

    /**
     * Kicks [targetPeerId] from the room and permanently bans both their `peerId` and `deviceId`
     * from rejoining this room session ("kick member [ cannot joined again ]").
     * Strictly restricted to [CollabRole.HOST].
     */
    @Synchronized
    fun kickPeer(targetPeerId: String, reason: String = "Removed by host"): Boolean {
        if (!isSessionActive) return false
        if (_localRole.value != CollabRole.HOST) return false
        if (targetPeerId == localPeerId || targetPeerId == _hostPeerId.value) return false

        val targetPeer = _activePeers.value[targetPeerId]
        val resolvedDeviceId = targetPeer?.deviceId?.ifBlank { targetPeerId } ?: targetPeerId
        val resolvedName = targetPeer?.userTag ?: targetPeerId

        _bannedPeerIds.update { it + targetPeerId }
        _bannedDeviceIds.update { it + resolvedDeviceId }
        _adminPeerIds.update { it - targetPeerId }
        _activePeers.update { it - targetPeerId }

        _roomCode.value?.let { code ->
            CollabRoomRegistry.updateRoomBanList(code, _bannedPeerIds.value, _bannedDeviceIds.value)
        }

        refreshPeersAndPermissions()
        appendSystemChatNotice("$resolvedName was removed from the room by the host.")

        dispatchMessage(
            CollabMessage.KickPeer(
                sessionToken = _sessionToken.value,
                hostPeerId = localPeerId,
                targetPeerId = targetPeerId,
                targetDeviceId = resolvedDeviceId,
                targetUserName = resolvedName,
                reason = reason
            )
        )
        return true
    }

    // =========================================================================
    // GLOBAL & PER-USER CANVAS RESTRICTIONS (DRAWING & NEW SHEETS)
    // =========================================================================

    /**
     * Sets who retains access when canvas restrictions are active:
     * - [CollabAccessMode.HOST_AND_ADMIN]: Only Host and Admins have access.
     * - [CollabAccessMode.HOST_ONLY]: Only the Host has access.
     * Strictly restricted to [CollabRole.HOST].
     */
    @Synchronized
    fun setAccessMode(mode: CollabAccessMode): Boolean {
        if (!isSessionActive) return false
        if (_localRole.value != CollabRole.HOST) return false
        if (_accessMode.value == mode) return true

        _accessMode.value = mode
        val eligibleTargets = getEligibleTargetPeerIds(mode)

        if (_isGlobalDrawingDisabled.value) {
            _drawingDisabledPeerIds.value = eligibleTargets
        } else if (mode == CollabAccessMode.HOST_AND_ADMIN) {
            // Admins regain access when switching back to HOST_AND_ADMIN
            _drawingDisabledPeerIds.update { it - _adminPeerIds.value }
        }

        if (_isGlobalSheetCreationDisabled.value) {
            _sheetCreationDisabledPeerIds.value = eligibleTargets
        } else if (mode == CollabAccessMode.HOST_AND_ADMIN) {
            _sheetCreationDisabledPeerIds.update { it - _adminPeerIds.value }
        }

        refreshPeersAndPermissions()
        broadcastCanvasPermissionSync()
        return true
    }

    /**
     * Toggles the Global Disable Drawing switch.
     * When turned ON, disables drawing for all eligible users governed by [accessMode].
     * When turned OFF, re-enables drawing for all users.
     */
    @Synchronized
    fun setGlobalDrawingDisabled(disabled: Boolean): Boolean {
        if (!canManageGlobalCanvasLocks()) return false
        val eligibleTargets = getEligibleTargetPeerIds()
        _isGlobalDrawingDisabled.value = disabled
        _drawingDisabledPeerIds.value = if (disabled) eligibleTargets else emptySet()
        refreshPeersAndPermissions()
        broadcastCanvasPermissionSync()
        return true
    }

    /**
     * Toggles the Global Disable Creating New Sheets switch.
     * When turned ON, disables creating new sheets for all eligible users governed by [accessMode].
     * When turned OFF, re-enables creating new sheets for all users.
     */
    @Synchronized
    fun setGlobalSheetCreationDisabled(disabled: Boolean): Boolean {
        if (!canManageGlobalCanvasLocks()) return false
        val eligibleTargets = getEligibleTargetPeerIds()
        _isGlobalSheetCreationDisabled.value = disabled
        _sheetCreationDisabledPeerIds.value = if (disabled) eligibleTargets else emptySet()
        refreshPeersAndPermissions()
        broadcastCanvasPermissionSync()
        return true
    }

    /**
     * Toggles Disable Drawing for a specific [targetPeerId].
     * Basic Auto-Sync Logic:
     * - When all eligible users are disabled individually, [isGlobalDrawingDisabled] automatically becomes `true`.
     * - When a user is re-enabled while [isGlobalDrawingDisabled] is `true`, [isGlobalDrawingDisabled] flips to `false`
     *   while keeping the remaining users disabled.
     */
    @Synchronized
    fun setPeerDrawingDisabled(targetPeerId: String, disabled: Boolean): Boolean {
        if (!canRestrictPeerCanvas(targetPeerId = targetPeerId, actorPeerId = localPeerId)) return false
        if (disabled) {
            _drawingDisabledPeerIds.update { it + targetPeerId }
        } else {
            _drawingDisabledPeerIds.update { it - targetPeerId }
        }
        val eligibleTargets = getEligibleTargetPeerIds()
        _isGlobalDrawingDisabled.value =
            eligibleTargets.isNotEmpty() && eligibleTargets.all { it in _drawingDisabledPeerIds.value }

        refreshPeersAndPermissions()
        broadcastCanvasPermissionSync()
        return true
    }

    /**
     * Toggles Disable Creating New Sheets for a specific [targetPeerId].
     * Basic Auto-Sync Logic:
     * - When all eligible users are disabled individually, [isGlobalSheetCreationDisabled] automatically becomes `true`.
     * - When a user is re-enabled while [isGlobalSheetCreationDisabled] is `true`, [isGlobalSheetCreationDisabled] flips to `false`
     *   while keeping the remaining users disabled.
     */
    @Synchronized
    fun setPeerSheetCreationDisabled(targetPeerId: String, disabled: Boolean): Boolean {
        if (!canRestrictPeerCanvas(targetPeerId = targetPeerId, actorPeerId = localPeerId)) return false
        if (disabled) {
            _sheetCreationDisabledPeerIds.update { it + targetPeerId }
        } else {
            _sheetCreationDisabledPeerIds.update { it - targetPeerId }
        }
        val eligibleTargets = getEligibleTargetPeerIds()
        _isGlobalSheetCreationDisabled.value =
            eligibleTargets.isNotEmpty() && eligibleTargets.all { it in _sheetCreationDisabledPeerIds.value }

        refreshPeersAndPermissions()
        broadcastCanvasPermissionSync()
        return true
    }

    private fun canManageGlobalCanvasLocks(): Boolean {
        if (!isSessionActive) return false
        return when (_localRole.value) {
            CollabRole.HOST -> true
            CollabRole.ADMIN -> _accessMode.value == CollabAccessMode.HOST_AND_ADMIN
            CollabRole.MEMBER -> false
        }
    }

    private fun getEligibleTargetPeerIds(mode: CollabAccessMode = _accessMode.value): Set<String> {
        val currentHost = _hostPeerId.value
        val admins = _adminPeerIds.value
        return _activePeers.value.keys.filterTo(LinkedHashSet()) { peerId ->
            if (peerId == currentHost) {
                false
            } else if (mode == CollabAccessMode.HOST_AND_ADMIN && peerId in admins) {
                false
            } else {
                true
            }
        }
    }

    private fun broadcastCanvasPermissionSync() {
        dispatchMessage(
            CollabMessage.CanvasPermissionSync(
                sessionToken = _sessionToken.value,
                actorPeerId = localPeerId,
                accessMode = _accessMode.value,
                isGlobalDrawingDisabled = _isGlobalDrawingDisabled.value,
                isGlobalSheetCreationDisabled = _isGlobalSheetCreationDisabled.value,
                drawingDisabledPeerIds = _drawingDisabledPeerIds.value,
                sheetCreationDisabledPeerIds = _sheetCreationDisabledPeerIds.value
            )
        )
    }

    // =========================================================================
    // PER-USER VOLUME CONTROL (0% - 200%) & AUDIO BITRATE SETTINGS
    // =========================================================================

    /**
     * Adjusts the local listening volume for [peerId] between `0.0f` (`0%`) and `2.0f` (`200%`).
     */
    fun setPeerVolume(peerId: String, volume: Float) {
        val clamped = if (volume.isNaN()) DEFAULT_PEER_VOLUME else volume.coerceIn(MIN_PEER_VOLUME, MAX_PEER_VOLUME)
        _peerVolumes.update { it + (peerId to clamped) }
        if (clamped == 0.0f) {
            _locallyMutedPeerIds.update { it + peerId }
        } else {
            _locallyMutedPeerIds.update { it - peerId }
        }
    }

    /**
     * Toggles local listening mute for [peerId].
     */
    fun toggleLocalPeerMute(peerId: String) {
        _locallyMutedPeerIds.update { current ->
            if (peerId in current) {
                if ((_peerVolumes.value[peerId] ?: DEFAULT_PEER_VOLUME) == 0.0f) {
                    _peerVolumes.update { it + (peerId to DEFAULT_PEER_VOLUME) }
                }
                current - peerId
            } else {
                current + peerId
            }
        }
    }

    /**
     * Returns the effective playback gain (`0.0f..2.0f`) for [peerId], accounting for both
     * Room-Mute and Local-Mute states.
     */
    fun getEffectivePeerVolume(peerId: String): Float {
        if (peerId in _roomMutedPeerIds.value || peerId in _locallyMutedPeerIds.value) {
            return 0.0f
        }
        return (_peerVolumes.value[peerId] ?: DEFAULT_PEER_VOLUME).coerceIn(MIN_PEER_VOLUME, MAX_PEER_VOLUME)
    }

    /**
     * Sets the local user's outgoing voice/audio bitrate in kbps (`16`, `32`, `64`, or `96`),
     * automatically clamped to [roomMaxBitrateKbps].
     */
    fun setAudioBitrateKbps(kbps: Int) {
        val sanitized = CollabAudioBitrate.sanitize(kbps)
        val clamped = sanitized.coerceAtMost(_roomMaxBitrateKbps.value)
        _audioBitrateKbps.value = clamped
        _activePeers.update { current ->
            val self = current[localPeerId] ?: return@update current
            current + (localPeerId to self.copy(audioBitrateKbps = clamped))
        }
    }

    /**
     * Sets the room-wide maximum audio bitrate cap (`16`, `32`, `64`, or `96` kbps).
     * Strictly restricted to [CollabRole.HOST].
     */
    @Synchronized
    fun setRoomMaxBitrateKbps(maxKbps: Int): Boolean {
        if (!isSessionActive) return false
        if (_localRole.value != CollabRole.HOST) return false
        val sanitized = CollabAudioBitrate.sanitize(maxKbps)
        _roomMaxBitrateKbps.value = sanitized
        if (_audioBitrateKbps.value > sanitized) {
            setAudioBitrateKbps(sanitized)
        }
        broadcastRoomSettingsSync()
        return true
    }

    // =========================================================================
    // PRIVACY & JOIN ACCESS CONTROL SETTINGS (2.1 UPDATE)
    // =========================================================================

    /**
     * Sets the maximum number of participants allowed in the room (`2..32`).
     * Strictly restricted to [CollabRole.HOST].
     */
    @Synchronized
    fun setMaxParticipants(limit: Int): Boolean {
        if (_localRole.value != CollabRole.HOST && isSessionActive) return false
        val sanitized = CollabRoomCapacity.sanitize(limit)
        _maxParticipants.value = sanitized
        if (isSessionActive) {
            broadcastRoomSettingsSync()
        }
        return true
    }

    /**
     * Sets the join access policy ([CollabJoinPolicy.OPEN], [CollabJoinPolicy.FRIENDS_ONLY],
     * [CollabJoinPolicy.CUSTOM_PRESET], or [CollabJoinPolicy.INVITED_ONLY]).
     * Strictly restricted to [CollabRole.HOST].
     */
    @Synchronized
    fun setJoinPolicy(policy: CollabJoinPolicy): Boolean {
        if (_localRole.value != CollabRole.HOST && isSessionActive) return false
        _joinPolicy.value = policy
        if (isSessionActive) {
            broadcastRoomSettingsSync()
        }
        return true
    }

    /**
     * Synchronizes the local Host's friend list identifiers (`@handle` and display names)
     * from [com.arinara.fotara.feature.friends.data.FriendsRepository].
     */
    @Synchronized
    fun syncHostFriends(friendIdentifiers: Collection<String>) {
        val normalized = friendIdentifiers
            .map { CollabPrivacyPreset.normalizeHandle(it) }
            .filter { it.length > 1 }
            .toCollection(LinkedHashSet())
        _hostFriendHandles.value = normalized
    }

    /**
     * Adds a `@username` handle or friend identifier to the active [CollabJoinPolicy.CUSTOM_PRESET] whitelist.
     */
    @Synchronized
    fun addAllowedUserHandle(handleOrName: String): Boolean {
        if (_localRole.value != CollabRole.HOST && isSessionActive) return false
        val normalized = CollabPrivacyPreset.normalizeHandle(handleOrName)
        if (normalized.length <= 1) return false
        _allowedUserHandles.update { it + normalized }
        if (isSessionActive) {
            broadcastRoomSettingsSync()
        }
        return true
    }

    /**
     * Removes a `@username` handle or friend identifier from the active [CollabJoinPolicy.CUSTOM_PRESET] whitelist.
     */
    @Synchronized
    fun removeAllowedUserHandle(handleOrName: String): Boolean {
        if (_localRole.value != CollabRole.HOST && isSessionActive) return false
        val normalized = CollabPrivacyPreset.normalizeHandle(handleOrName)
        if (normalized.length <= 1) return false
        _allowedUserHandles.update { it - normalized }
        if (isSessionActive) {
            broadcastRoomSettingsSync()
        }
        return true
    }

    /**
     * Replaces the entire [CollabJoinPolicy.CUSTOM_PRESET] whitelist of allowed `@username` handles.
     */
    @Synchronized
    fun setAllowedUserHandles(handles: Collection<String>): Boolean {
        if (_localRole.value != CollabRole.HOST && isSessionActive) return false
        val normalized = handles
            .map { CollabPrivacyPreset.normalizeHandle(it) }
            .filter { it.length > 1 }
            .toCollection(LinkedHashSet())
        _allowedUserHandles.value = normalized
        if (isSessionActive) {
            broadcastRoomSettingsSync()
        }
        return true
    }

    /**
     * Invites a specific `@username` handle or friend to join when [CollabJoinPolicy.INVITED_ONLY] is active.
     */
    @Synchronized
    fun inviteUser(handleOrName: String): Boolean {
        if (_localRole.value != CollabRole.HOST && isSessionActive) return false
        val normalized = CollabPrivacyPreset.normalizeHandle(handleOrName)
        if (normalized.length <= 1) return false
        _invitedUserHandles.update { it + normalized }
        if (isSessionActive) {
            broadcastRoomSettingsSync()
        }
        return true
    }

    /**
     * Revokes a session invitation for [handleOrName].
     */
    @Synchronized
    fun revokeInvitation(handleOrName: String): Boolean {
        if (_localRole.value != CollabRole.HOST && isSessionActive) return false
        val normalized = CollabPrivacyPreset.normalizeHandle(handleOrName)
        if (normalized.length <= 1) return false
        _invitedUserHandles.update { it - normalized }
        if (isSessionActive) {
            broadcastRoomSettingsSync()
        }
        return true
    }

    /**
     * Loads persisted custom privacy presets into the engine.
     */
    @Synchronized
    fun loadSavedPrivacyPresets(presets: List<CollabPrivacyPreset>) {
        _savedPrivacyPresets.value = presets
    }

    /**
     * Saves the given [handles] (or current [allowedUserHandles]) as a named [CollabPrivacyPreset].
     */
    @Synchronized
    fun savePrivacyPreset(
        name: String,
        handles: Collection<String> = _allowedUserHandles.value
    ): CollabPrivacyPreset? {
        val cleanName = name.trim().take(40)
        if (cleanName.isEmpty()) return null
        val normalizedHandles = handles
            .map { CollabPrivacyPreset.normalizeHandle(it) }
            .filter { it.length > 1 }
            .toCollection(LinkedHashSet())
        val existing = _savedPrivacyPresets.value.firstOrNull {
            it.name.equals(cleanName, ignoreCase = true)
        }
        val preset = if (existing != null) {
            existing.copy(name = cleanName, allowedHandles = normalizedHandles)
        } else {
            CollabPrivacyPreset(name = cleanName, allowedHandles = normalizedHandles)
        }
        _savedPrivacyPresets.update { current ->
            val filtered = current.filterNot { it.id == preset.id }
            filtered + preset
        }
        _activePresetId.value = preset.id
        _allowedUserHandles.value = normalizedHandles
        return preset
    }

    /**
     * Applies a saved [CollabPrivacyPreset] by its [presetId] and activates [CollabJoinPolicy.CUSTOM_PRESET].
     */
    @Synchronized
    fun applyPrivacyPreset(presetId: String): Boolean {
        if (_localRole.value != CollabRole.HOST && isSessionActive) return false
        val preset = _savedPrivacyPresets.value.firstOrNull { it.id == presetId } ?: return false
        _activePresetId.value = preset.id
        _allowedUserHandles.value = preset.allowedHandles
        _joinPolicy.value = CollabJoinPolicy.CUSTOM_PRESET
        if (isSessionActive) {
            broadcastRoomSettingsSync()
        }
        return true
    }

    /**
     * Deletes a saved [CollabPrivacyPreset] by its [presetId].
     */
    @Synchronized
    fun deletePrivacyPreset(presetId: String): Boolean {
        val before = _savedPrivacyPresets.value.size
        _savedPrivacyPresets.update { list -> list.filterNot { it.id == presetId } }
        if (_activePresetId.value == presetId) {
            _activePresetId.value = null
        }
        return _savedPrivacyPresets.value.size < before
    }

    /**
     * Evaluates whether a joiner with ([peerId], [deviceId], [userTag], [userHandle]) is allowed to join
     * this Host's room. Returns `null` if permitted, or a [CollabMessage.JoinRejected] reason constant if rejected.
     */
    @Synchronized
    fun evaluateJoinAttempt(
        peerId: String,
        deviceId: String,
        userTag: String,
        userHandle: String
    ): String? {
        if (peerId == localPeerId || peerId == _hostPeerId.value) return null
        if (peerId in _bannedPeerIds.value || (deviceId.isNotBlank() && deviceId in _bannedDeviceIds.value)) {
            return CollabMessage.JoinRejected.REASON_BANNED_BY_HOST
        }
        // Capacity limit check (if not already an active peer in the room)
        if (peerId !in _activePeers.value && _activePeers.value.size >= _maxParticipants.value) {
            return CollabMessage.JoinRejected.REASON_ROOM_FULL
        }
        // Explicit session invite always grants entry under any join policy
        if (matchesHandleOrTag(_invitedUserHandles.value, userHandle, userTag)) {
            return null
        }
        return when (_joinPolicy.value) {
            CollabJoinPolicy.OPEN -> null
            CollabJoinPolicy.FRIENDS_ONLY -> {
                if (matchesHandleOrTag(_hostFriendHandles.value, userHandle, userTag)) {
                    null
                } else {
                    CollabMessage.JoinRejected.REASON_FRIENDS_ONLY
                }
            }
            CollabJoinPolicy.CUSTOM_PRESET -> {
                if (matchesHandleOrTag(_allowedUserHandles.value, userHandle, userTag)) {
                    null
                } else {
                    CollabMessage.JoinRejected.REASON_NOT_IN_PRESET
                }
            }
            CollabJoinPolicy.INVITED_ONLY -> {
                CollabMessage.JoinRejected.REASON_INVITE_ONLY
            }
        }
    }

    private fun matchesHandleOrTag(
        allowedSet: Set<String>,
        userHandle: String,
        userTag: String
    ): Boolean {
        if (allowedSet.isEmpty()) return false
        val normHandle = CollabPrivacyPreset.normalizeHandle(userHandle)
        if (normHandle.length > 1 && normHandle in allowedSet) return true
        val normTag = CollabPrivacyPreset.normalizeHandle(userTag)
        if (normTag.length > 1 && normTag in allowedSet) return true
        val compactTag = "@" + userTag.trim().lowercase().replace(" ", "_")
        if (compactTag.length > 1 && compactTag in allowedSet) return true
        return false
    }

    private fun formatJoinRejectionMessage(roomCode: String, reason: String): String {
        return when (reason) {
            CollabMessage.JoinRejected.REASON_BANNED_BY_HOST ->
                "You cannot join $roomCode because the host removed you from this session."
            CollabMessage.JoinRejected.REASON_ROOM_FULL ->
                "Cannot join $roomCode: This room is full."
            CollabMessage.JoinRejected.REASON_FRIENDS_ONLY ->
                "Cannot join $roomCode: Only the host's friends can join this room."
            CollabMessage.JoinRejected.REASON_NOT_IN_PRESET ->
                "Cannot join $roomCode: Your username is not in the host's allowed preset list."
            CollabMessage.JoinRejected.REASON_INVITE_ONLY ->
                "Cannot join $roomCode: This room is Invite-Only. Ask the host for an invitation."
            else ->
                "Unable to join $roomCode: $reason"
        }
    }

    private fun broadcastRoomSettingsSync() {
        dispatchMessage(
            CollabMessage.RoomSettingsSync(
                sessionToken = _sessionToken.value,
                hostPeerId = localPeerId,
                maxBitrateKbps = _roomMaxBitrateKbps.value,
                maxParticipants = _maxParticipants.value,
                joinPolicy = _joinPolicy.value,
                allowedUserHandles = _allowedUserHandles.value,
                invitedUserHandles = _invitedUserHandles.value
            )
        )
    }

    // =========================================================================
    // CHAT, STROKES & CURSOR STREAMING
    // =========================================================================

    /**
     * Sends a real-time chat message to all collaborators in the active room.
     * Guarded: returns `null` if the local user's chat has been blocked by a moderator.
     */
    fun sendChatMessage(rawText: String): CollabChatMessage? {
        if (!isSessionActive) return null
        if (localPeerId in _chatBlockedPeerIds.value || _isLocalUserChatBlocked.value) {
            _lastErrorMessage.value = "Your chat access has been blocked by a moderator."
            return null
        }
        val sanitized = rawText.trim().take(500)
        if (sanitized.isEmpty()) return null

        val now = System.currentTimeMillis()
        val msgId = UUID.randomUUID().toString()
        val currentRole = _localRole.value
        val chatEntry = CollabChatMessage(
            messageId = msgId,
            peerId = localPeerId,
            senderTag = localUserTag,
            senderColorHex = localColorHex,
            text = sanitized,
            timestamp = now,
            isLocal = true,
            senderPronouns = localPronouns,
            senderRole = currentRole
        )

        _chatMessages.update { current ->
            (current + chatEntry).takeLast(MAX_CHAT_HISTORY_SIZE)
        }

        val broadcast = CollabMessage.ChatBroadcast(
            messageId = msgId,
            peerId = localPeerId,
            senderTag = localUserTag,
            senderColorHex = localColorHex,
            text = sanitized,
            timestamp = now,
            senderPronouns = localPronouns,
            senderRole = currentRole,
            sessionToken = _sessionToken.value
        )
        dispatchMessage(broadcast)
        return chatEntry
    }

    private fun appendSystemChatNotice(noticeText: String) {
        val entry = CollabChatMessage(
            messageId = UUID.randomUUID().toString(),
            peerId = "system",
            senderTag = "Room Notice",
            senderColorHex = "#7A5AF8",
            text = noticeText,
            timestamp = System.currentTimeMillis(),
            isLocal = false,
            isSystemNotice = true
        )
        _chatMessages.update { current ->
            (current + entry).takeLast(MAX_CHAT_HISTORY_SIZE)
        }
    }

    /**
     * Updates whether the chat sheet UI is currently visible and clears unread badge when opened.
     */
    fun setChatSheetOpen(isOpen: Boolean) {
        isChatSheetOpen = isOpen
        if (isOpen) {
            _unreadChatCount.value = 0
        }
    }

    /**
     * Clears the unread chat badge counter manually.
     */
    fun markChatRead() {
        _unreadChatCount.value = 0
    }

    /**
     * Toggles the local user's microphone mute state and returns the new muted boolean.
     */
    fun toggleLocalMicMuted(): Boolean {
        if (localPeerId in _roomMutedPeerIds.value) {
            _isLocalMicMuted.value = true
            _lastErrorMessage.value = "You are muted in this room by the host or admin."
            return true
        }
        val next = !_isLocalMicMuted.value
        _isLocalMicMuted.value = next
        return next
    }

    /**
     * Explicitly sets the local user's microphone mute state.
     */
    fun setLocalMicMuted(muted: Boolean) {
        if (!muted && localPeerId in _roomMutedPeerIds.value) {
            _isLocalMicMuted.value = true
            _lastErrorMessage.value = "You are muted in this room by the host or admin."
            return
        }
        _isLocalMicMuted.value = muted
    }

    /**
     * Broadcasts a drawn vector stroke to all room participants.
     * Guarded: if [canLocalUserDraw] is false, blocks outgoing network dispatch and sets [lastErrorMessage].
     */
    fun broadcastStroke(
        points: List<Float>,
        colorHex: String = localColorHex,
        strokeWidth: Float = 4.0f,
        strokeId: String = UUID.randomUUID().toString(),
        toolType: String = "PEN",
        blendMode: String = "NORMAL",
        layerId: String = "layer_default",
        zIndex: Int = 0
    ): CollabMessage.StrokeBroadcast {
        val msg = CollabMessage.StrokeBroadcast(
            strokeId = strokeId,
            points = points,
            colorHex = colorHex,
            strokeWidth = strokeWidth,
            peerId = localPeerId,
            sessionToken = _sessionToken.value,
            toolType = toolType,
            blendMode = blendMode,
            layerId = layerId,
            zIndex = zIndex
        )
        if (!_canLocalUserDraw.value) {
            _lastErrorMessage.value = "Drawing is currently disabled by the host or admin."
            return msg
        }
        dispatchMessage(msg)
        return msg
    }

    /**
     * Broadcasts a text annotation element (`TextLayerElement`) creation, edit, or transform.
     */
    fun broadcastTextElement(msg: CollabMessage.TextElementSync): Boolean {
        if (!_canLocalUserDraw.value) {
            _lastErrorMessage.value = "Drawing is currently disabled by the host or admin."
            return false
        }
        val stamped = msg.copy(
            peerId = localPeerId,
            sessionToken = _sessionToken.value
        )
        dispatchMessage(stamped)
        return true
    }

    /**
     * Broadcasts an image element (`ImageElement`) insertion, transform, or VFX adjustment.
     */
    fun broadcastImageElement(msg: CollabMessage.ImageElementSync): Boolean {
        if (!_canLocalUserDraw.value) {
            _lastErrorMessage.value = "Drawing is currently disabled by the host or admin."
            return false
        }
        val stamped = msg.copy(
            peerId = localPeerId,
            sessionToken = _sessionToken.value
        )
        dispatchMessage(stamped)
        return true
    }

    /**
     * Broadcasts removal of canvas elements (erased strokes, deleted text/images/selections).
     */
    fun broadcastElementsRemoved(elementIds: List<String>): Boolean {
        if (elementIds.isEmpty()) return false
        if (!_canLocalUserDraw.value) {
            _lastErrorMessage.value = "Drawing is currently disabled by the host or admin."
            return false
        }
        val msg = CollabMessage.ElementsRemoveSync(
            elementIds = elementIds,
            peerId = localPeerId,
            sessionToken = _sessionToken.value
        )
        dispatchMessage(msg)
        return true
    }

    /**
     * Broadcasts a live PCM voice audio frame when the local microphone is unmuted.
     */
    fun broadcastAudioFrame(
        pcmDataBase64: String,
        sequenceNumber: Long,
        sampleRateHz: Int = 16000
    ): CollabMessage.AudioStreamFrame? {
        if (!canLocalUserSpeak || pcmDataBase64.isEmpty()) return null
        val frame = CollabMessage.AudioStreamFrame(
            peerId = localPeerId,
            sequenceNumber = sequenceNumber,
            sampleRateHz = sampleRateHz,
            pcmDataBase64 = pcmDataBase64,
            bitrateKbps = _audioBitrateKbps.value,
            sessionToken = _sessionToken.value
        )
        dispatchMessage(frame)
        return frame
    }

    /**
     * Broadcasts the local stylus/pointer cursor position with an accurate timestamp.
     */
    fun updateCursor(x: Float, y: Float): CollabMessage.CursorBroadcast {
        val now = System.currentTimeMillis()
        val msg = CollabMessage.CursorBroadcast(
            peerId = localPeerId,
            x = x,
            y = y,
            timestamp = now,
            sessionToken = _sessionToken.value
        )

        _activePeers.update { current ->
            val localPeer = current[localPeerId]
            if (localPeer != null) {
                current + (localPeerId to localPeer.copy(
                    lastCursorX = x,
                    lastCursorY = y,
                    lastActiveMs = now
                ))
            } else {
                current
            }
        }
        onPeerCursorsUpdated?.invoke(_activePeers.value.values)

        dispatchMessage(msg)
        return msg
    }

    /**
     * Ingests and processes incoming messages from peers or the network transport.
     */
    @Synchronized
    fun receiveMessage(message: CollabMessage) {
        when (message) {
            is CollabMessage.RoomProbe -> {
                val activeCode = _roomCode.value
                if (isSessionActive &&
                    _localRole.value == CollabRole.HOST &&
                    activeCode != null &&
                    activeCode.equals(CollabRoomCode.normalize(message.roomCode), ignoreCase = true)
                ) {
                    dispatchMessage(
                        CollabMessage.HostAnnounce(
                            roomCode = activeCode,
                            sessionToken = _sessionToken.value,
                            hostPeerId = localPeerId,
                            hostUserName = localUserTag,
                            lanPort = lanTransport?.localTcpPort ?: 0,
                            maxBitrateKbps = _roomMaxBitrateKbps.value
                        )
                    )
                }
            }
            is CollabMessage.WhoHasRoom -> {
                val activeCode = _roomCode.value
                if (isSessionActive &&
                    _localRole.value == CollabRole.HOST &&
                    activeCode != null &&
                    activeCode.equals(CollabRoomCode.normalize(message.roomCode), ignoreCase = true)
                ) {
                    if (message.joinerPeerId in _bannedPeerIds.value ||
                        message.joinerDeviceId in _bannedDeviceIds.value
                    ) {
                        dispatchMessage(
                            CollabMessage.JoinRejected(
                                roomCode = activeCode,
                                sessionToken = _sessionToken.value,
                                targetPeerId = message.joinerPeerId,
                                targetDeviceId = message.joinerDeviceId,
                                reason = CollabMessage.JoinRejected.REASON_BANNED_BY_HOST
                            )
                        )
                    } else {
                        dispatchMessage(
                            CollabMessage.HostAnnounce(
                                roomCode = activeCode,
                                sessionToken = _sessionToken.value,
                                hostPeerId = localPeerId,
                                hostUserName = localUserTag,
                                lanPort = lanTransport?.localTcpPort ?: 0,
                                maxBitrateKbps = _roomMaxBitrateKbps.value
                            )
                        )
                    }
                }
            }
            is CollabMessage.HostAnnounce -> {
                if (message.hostPeerId != localPeerId) {
                    CollabRoomRegistry.registerRemoteHostAnnouncement(message)
                }
                val activeCode = _roomCode.value
                if (activeCode != null && activeCode.equals(message.roomCode, ignoreCase = true)) {
                    if (_localRole.value != CollabRole.HOST && message.sessionToken.isNotBlank()) {
                        _sessionToken.value = message.sessionToken
                    } else if (_sessionToken.value.isBlank()) {
                        _sessionToken.value = message.sessionToken
                    }
                    if (_localRole.value != CollabRole.HOST && message.hostPeerId.isNotBlank()) {
                        _hostPeerId.value = message.hostPeerId
                    } else if (_hostPeerId.value == null) {
                        _hostPeerId.value = message.hostPeerId
                    }
                    _roomMaxBitrateKbps.value = CollabAudioBitrate.sanitize(message.maxBitrateKbps)
                    if (message.hostPeerId != localPeerId) {
                        _activePeers.update { current ->
                            val existing = current[message.hostPeerId]
                            val hostPeer = existing?.copy(
                                userTag = message.hostUserName,
                                role = CollabRole.HOST,
                                lastActiveMs = System.currentTimeMillis()
                            ) ?: CollabPeer(
                                peerId = message.hostPeerId,
                                userTag = message.hostUserName,
                                colorHex = "#7A5AF8",
                                role = CollabRole.HOST,
                                lastActiveMs = System.currentTimeMillis()
                            )
                            current + (message.hostPeerId to hostPeer)
                        }
                        if (message.lanIp.isNotBlank() && message.lanPort > 0) {
                            lanTransport?.connectToHost(message.lanIp, message.lanPort)
                        }
                    }
                }
            }
            is CollabMessage.JoinRoom -> {
                if (_state.value == CollabSessionState.IN_ROOM) {
                    val joinerId = message.peerId
                    val joinerDevId = message.deviceId.ifBlank { joinerId }
                    if (_localRole.value == CollabRole.HOST && joinerId.isNotBlank()) {
                        val rejectionReason = evaluateJoinAttempt(
                            peerId = joinerId,
                            deviceId = joinerDevId,
                            userTag = message.userTag,
                            userHandle = message.userHandle
                        )
                        if (rejectionReason != null) {
                            dispatchMessage(
                                CollabMessage.JoinRejected(
                                    roomCode = _roomCode.value ?: message.roomCode,
                                    sessionToken = _sessionToken.value,
                                    targetPeerId = joinerId,
                                    targetDeviceId = joinerDevId,
                                    reason = rejectionReason
                                )
                            )
                            return
                        }
                    }

                    if (joinerId.isNotBlank() && joinerId != localPeerId) {
                        if (_isGlobalDrawingDisabled.value) {
                            _drawingDisabledPeerIds.update { it + joinerId }
                        }
                        if (_isGlobalSheetCreationDisabled.value) {
                            _sheetCreationDisabledPeerIds.update { it + joinerId }
                        }
                        _activePeers.update { current ->
                            val existing = current[joinerId]
                            val effectiveJoinerHandle = message.userHandle.ifBlank { existing?.userHandle.orEmpty() }
                            val effectiveJoinerTag = message.userTag.trim()
                                .takeIf { it.isNotBlank() && !it.equals("Fotara User", ignoreCase = true) && !it.equals("My Profile", ignoreCase = true) }
                                ?: effectiveJoinerHandle.trim().removePrefix("@").ifBlank {
                                    existing?.userTag?.ifBlank { "Fotara User" } ?: message.userTag.ifBlank { "Fotara User" }
                                }
                            val updated = existing?.copy(
                                userTag = effectiveJoinerTag,
                                colorHex = message.colorHex,
                                deviceId = joinerDevId,
                                pronouns = message.pronouns,
                                userHandle = effectiveJoinerHandle,
                                audioBitrateKbps = message.audioBitrateKbps,
                                lastActiveMs = System.currentTimeMillis()
                            ) ?: CollabPeer(
                                peerId = joinerId,
                                userTag = effectiveJoinerTag,
                                colorHex = message.colorHex,
                                deviceId = joinerDevId,
                                pronouns = message.pronouns,
                                userHandle = effectiveJoinerHandle,
                                role = CollabRole.MEMBER,
                                audioBitrateKbps = message.audioBitrateKbps,
                                lastActiveMs = System.currentTimeMillis()
                            )
                            current + (joinerId to updated)
                        }
                        refreshPeersAndPermissions()
                    }

                    val echo = CollabMessage.PeerJoined(
                        peerId = localPeerId,
                        userTag = localUserTag,
                        colorHex = localColorHex,
                        deviceId = localDeviceId,
                        pronouns = localPronouns,
                        userHandle = localUserHandle,
                        role = _localRole.value,
                        audioBitrateKbps = _audioBitrateKbps.value,
                        sessionToken = _sessionToken.value
                    )
                    dispatchMessage(echo)

                    if (_localRole.value == CollabRole.HOST) {
                        broadcastCanvasPermissionSync()
                        broadcastRoomSettingsSync()
                        for (adminId in _adminPeerIds.value) {
                            dispatchMessage(
                                CollabMessage.SetPeerRole(
                                    sessionToken = _sessionToken.value,
                                    hostPeerId = localPeerId,
                                    targetPeerId = adminId,
                                    newRole = CollabRole.ADMIN
                                )
                            )
                        }
                        for (mutedId in _roomMutedPeerIds.value) {
                            dispatchMessage(
                                CollabMessage.ModMutePeer(
                                    sessionToken = _sessionToken.value,
                                    actorPeerId = localPeerId,
                                    targetPeerId = mutedId,
                                    isMuted = true
                                )
                            )
                        }
                        for (blockedId in _chatBlockedPeerIds.value) {
                            dispatchMessage(
                                CollabMessage.BlockPeerChat(
                                    sessionToken = _sessionToken.value,
                                    actorPeerId = localPeerId,
                                    targetPeerId = blockedId,
                                    isBlocked = true
                                )
                            )
                        }
                        if (_pinnedChatMessageId.value != null) {
                            dispatchMessage(
                                CollabMessage.PinChatMessage(
                                    sessionToken = _sessionToken.value,
                                    hostPeerId = localPeerId,
                                    pinnedMessageId = _pinnedChatMessageId.value
                                )
                            )
                        }
                        if (joinerId.isNotBlank() && joinerId != localPeerId) {
                            onPeerJoinedNeedSnapshot?.invoke(joinerId)
                        }
                    }
                }
            }
            is CollabMessage.JoinRejected -> {
                if (message.targetPeerId == localPeerId || message.targetDeviceId == localDeviceId) {
                    if (message.reason == CollabMessage.JoinRejected.REASON_BANNED_BY_HOST) {
                        _kickedFromRoomCodes.update { it + CollabRoomCode.normalize(message.roomCode) }
                    }
                    _lastErrorMessage.value = formatJoinRejectionMessage(message.roomCode, message.reason)
                    resetRoomStateToDisconnected()
                }
            }
            is CollabMessage.PeerJoined -> {
                if (message.role == CollabRole.HOST && _localRole.value != CollabRole.HOST) {
                    if (_hostPeerId.value == null || _hostPeerId.value == message.peerId) {
                        _hostPeerId.value = message.peerId
                        if (message.sessionToken.isNotBlank()) {
                            _sessionToken.value = message.sessionToken
                        }
                    }
                }
                if (!isSessionTokenCompatible(message.sessionToken)) return
                if (message.peerId in _bannedPeerIds.value || message.deviceId in _bannedDeviceIds.value) {
                    return
                }
                if (_localRole.value == CollabRole.HOST && message.peerId != localPeerId) {
                    val rejectionReason = evaluateJoinAttempt(
                        peerId = message.peerId,
                        deviceId = message.deviceId,
                        userTag = message.userTag,
                        userHandle = message.userHandle
                    )
                    if (rejectionReason != null) {
                        dispatchMessage(
                            CollabMessage.JoinRejected(
                                roomCode = _roomCode.value ?: "",
                                sessionToken = _sessionToken.value,
                                targetPeerId = message.peerId,
                                targetDeviceId = message.deviceId,
                                reason = rejectionReason
                            )
                        )
                        return
                    }
                }
                if (message.peerId != localPeerId) {
                    if (message.role == CollabRole.HOST && _hostPeerId.value == null) {
                        _hostPeerId.value = message.peerId
                    } else if (message.role == CollabRole.ADMIN) {
                        _adminPeerIds.update { it + message.peerId }
                    }
                    if (_localRole.value == CollabRole.HOST) {
                        if (_isGlobalDrawingDisabled.value && message.role != CollabRole.HOST) {
                            if (_accessMode.value == CollabAccessMode.HOST_ONLY || message.role == CollabRole.MEMBER) {
                                _drawingDisabledPeerIds.update { it + message.peerId }
                            }
                        }
                        if (_isGlobalSheetCreationDisabled.value && message.role != CollabRole.HOST) {
                            if (_accessMode.value == CollabAccessMode.HOST_ONLY || message.role == CollabRole.MEMBER) {
                                _sheetCreationDisabledPeerIds.update { it + message.peerId }
                            }
                        }
                    }
                    _activePeers.update { current ->
                        val existing = current[message.peerId]
                        val effectivePeerHandle = message.userHandle.ifBlank { existing?.userHandle.orEmpty() }
                        val effectivePeerTag = message.userTag.trim()
                            .takeIf { it.isNotBlank() && !it.equals("Fotara User", ignoreCase = true) && !it.equals("My Profile", ignoreCase = true) }
                            ?: effectivePeerHandle.trim().removePrefix("@").ifBlank {
                                existing?.userTag?.ifBlank { "Fotara User" } ?: message.userTag.ifBlank { "Fotara User" }
                            }
                        val updated = existing?.copy(
                            userTag = effectivePeerTag,
                            colorHex = message.colorHex,
                            deviceId = message.deviceId.ifBlank { message.peerId },
                            pronouns = message.pronouns,
                            userHandle = effectivePeerHandle,
                            role = if (message.peerId == _hostPeerId.value) CollabRole.HOST
                            else if (message.peerId in _adminPeerIds.value) CollabRole.ADMIN
                            else message.role,
                            audioBitrateKbps = message.audioBitrateKbps,
                            lastActiveMs = System.currentTimeMillis()
                        ) ?: CollabPeer(
                            peerId = message.peerId,
                            userTag = effectivePeerTag,
                            colorHex = message.colorHex,
                            deviceId = message.deviceId.ifBlank { message.peerId },
                            pronouns = message.pronouns,
                            userHandle = effectivePeerHandle,
                            role = message.role,
                            audioBitrateKbps = message.audioBitrateKbps,
                            lastActiveMs = System.currentTimeMillis()
                        )
                        current + (message.peerId to updated)
                    }
                    refreshPeersAndPermissions()
                }
            }
            is CollabMessage.StrokeBroadcast -> {
                if (!isSessionTokenCompatible(message.sessionToken)) return
                val senderId = message.peerId
                if (senderId.isNotBlank()) {
                    if (senderId in _bannedPeerIds.value || senderId in _drawingDisabledPeerIds.value) {
                        return
                    }
                    if (_isGlobalDrawingDisabled.value && !canPeerDrawUnderGlobalLock(senderId)) {
                        return
                    }
                }
                val dedupKey = "${message.strokeId}:${message.points.hashCode()}:${message.zIndex}:${message.layerId}"
                if (!processedStrokeIds.add(dedupKey)) {
                    return
                }
                if (processedStrokeIds.size > MAX_STROKE_BUFFER_SIZE * 2) {
                    val first = processedStrokeIds.iterator().next()
                    processedStrokeIds.remove(first)
                }

                val stroke = CollabStroke(
                    strokeId = message.strokeId,
                    peerId = senderId.ifBlank { "peer" },
                    points = message.points,
                    colorHex = message.colorHex,
                    strokeWidth = message.strokeWidth,
                    timestamp = System.currentTimeMillis(),
                    toolType = message.toolType,
                    blendMode = message.blendMode,
                    layerId = message.layerId,
                    zIndex = message.zIndex
                )
                while (strokeBuffer.size >= MAX_STROKE_BUFFER_SIZE) {
                    strokeBuffer.poll()
                }
                strokeBuffer.add(stroke)
                refreshBufferedEventsCount()
            }
            is CollabMessage.TextElementSync -> {
                if (!isSessionTokenCompatible(message.sessionToken)) return
                val senderId = message.peerId
                if (senderId == localPeerId) return
                if (senderId.isNotBlank()) {
                    if (senderId in _bannedPeerIds.value || senderId in _drawingDisabledPeerIds.value) {
                        return
                    }
                    if (_isGlobalDrawingDisabled.value && !canPeerDrawUnderGlobalLock(senderId)) {
                        return
                    }
                }
                while (textSyncBuffer.size >= MAX_STROKE_BUFFER_SIZE) {
                    textSyncBuffer.poll()
                }
                textSyncBuffer.add(message)
                refreshBufferedEventsCount()
            }
            is CollabMessage.ImageElementSync -> {
                if (!isSessionTokenCompatible(message.sessionToken)) return
                val senderId = message.peerId
                if (senderId == localPeerId) return
                if (senderId.isNotBlank()) {
                    if (senderId in _bannedPeerIds.value || senderId in _drawingDisabledPeerIds.value) {
                        return
                    }
                    if (_isGlobalDrawingDisabled.value && !canPeerDrawUnderGlobalLock(senderId)) {
                        return
                    }
                }
                while (imageSyncBuffer.size >= 128) {
                    imageSyncBuffer.poll()
                }
                imageSyncBuffer.add(message)
                refreshBufferedEventsCount()
            }
            is CollabMessage.ElementsRemoveSync -> {
                if (!isSessionTokenCompatible(message.sessionToken)) return
                val senderId = message.peerId
                if (senderId == localPeerId) return
                if (senderId.isNotBlank()) {
                    if (senderId in _bannedPeerIds.value || senderId in _drawingDisabledPeerIds.value) {
                        return
                    }
                    if (_isGlobalDrawingDisabled.value && !canPeerDrawUnderGlobalLock(senderId)) {
                        return
                    }
                }
                while (removeSyncBuffer.size >= MAX_STROKE_BUFFER_SIZE) {
                    removeSyncBuffer.poll()
                }
                removeSyncBuffer.add(message)
                refreshBufferedEventsCount()
            }
            is CollabMessage.AudioStreamFrame -> {
                if (!isSessionTokenCompatible(message.sessionToken)) return
                val senderId = message.peerId
                if (senderId.isBlank() || senderId == localPeerId) return
                if (senderId in _bannedPeerIds.value ||
                    senderId in _roomMutedPeerIds.value ||
                    senderId in _locallyMutedPeerIds.value
                ) {
                    return
                }
                _activePeers.update { current ->
                    val existing = current[senderId] ?: return@update current
                    current + (senderId to existing.copy(lastActiveMs = System.currentTimeMillis()))
                }
                onAudioFrameReceived?.invoke(message)
            }
            is CollabMessage.CursorBroadcast -> {
                if (!isSessionTokenCompatible(message.sessionToken)) return
                if (message.peerId in _bannedPeerIds.value) return
                if (message.peerId != localPeerId) {
                    _activePeers.update { current ->
                        val existing = current[message.peerId]
                        val updated = existing?.copy(
                            lastCursorX = message.x,
                            lastCursorY = message.y,
                            lastActiveMs = message.timestamp
                        ) ?: CollabPeer(
                            peerId = message.peerId,
                            userTag = "Peer ${message.peerId.take(4)}",
                            colorHex = "#7A5AF8",
                            lastCursorX = message.x,
                            lastCursorY = message.y,
                            lastActiveMs = message.timestamp
                        )
                        current + (message.peerId to updated)
                    }
                    onPeerCursorsUpdated?.invoke(_activePeers.value.values)
                }
            }
            is CollabMessage.ChatBroadcast -> {
                if (!isSessionTokenCompatible(message.sessionToken)) return
                if (message.peerId in _bannedPeerIds.value || message.peerId in _chatBlockedPeerIds.value) {
                    return
                }
                val isFromSelf = message.peerId == localPeerId
                var addedNew = false
                val resolvedRole = when {
                    message.peerId == _hostPeerId.value -> CollabRole.HOST
                    message.peerId in _adminPeerIds.value -> CollabRole.ADMIN
                    else -> message.senderRole
                }
                _chatMessages.update { current ->
                    if (current.any { it.messageId == message.messageId }) {
                        current
                    } else {
                        addedNew = true
                        val entry = CollabChatMessage(
                            messageId = message.messageId,
                            peerId = message.peerId,
                            senderTag = message.senderTag,
                            senderColorHex = message.senderColorHex,
                            text = message.text,
                            timestamp = message.timestamp,
                            isLocal = isFromSelf,
                            senderPronouns = message.senderPronouns,
                            senderRole = resolvedRole
                        )
                        (current + entry).takeLast(MAX_CHAT_HISTORY_SIZE)
                    }
                }
                if (addedNew && !isFromSelf) {
                    if (!isChatSheetOpen) {
                        _unreadChatCount.update { it + 1 }
                    }
                    _activePeers.update { current ->
                        val existing = current[message.peerId]
                        val updated = existing?.copy(
                            userTag = message.senderTag,
                            colorHex = message.senderColorHex,
                            pronouns = message.senderPronouns.ifBlank { existing.pronouns },
                            role = resolvedRole,
                            lastActiveMs = message.timestamp
                        ) ?: CollabPeer(
                            peerId = message.peerId,
                            userTag = message.senderTag,
                            colorHex = message.senderColorHex,
                            pronouns = message.senderPronouns,
                            role = resolvedRole,
                            lastActiveMs = message.timestamp
                        )
                        current + (message.peerId to updated)
                    }
                }
            }
            is CollabMessage.PinChatMessage -> {
                if (!isSessionTokenCompatible(message.sessionToken)) return
                // Only accept PinChatMessage from the current Host
                val currentHost = _hostPeerId.value
                if (currentHost != null && message.hostPeerId != currentHost) return
                val pinId = message.pinnedMessageId
                _pinnedChatMessageId.value = pinId
                _pinnedChatMessage.value = if (pinId == null) {
                    null
                } else {
                    _chatMessages.value.firstOrNull { it.messageId == pinId }
                }
            }
            is CollabMessage.DeleteChatMessage -> {
                if (!isSessionTokenCompatible(message.sessionToken)) return
                val isAuthorized = message.actorPeerId == _hostPeerId.value ||
                    message.actorPeerId in _adminPeerIds.value
                if (!isAuthorized) return

                _chatMessages.update { list -> list.filterNot { it.messageId == message.messageId } }
                if (_pinnedChatMessageId.value == message.messageId) {
                    _pinnedChatMessageId.value = null
                    _pinnedChatMessage.value = null
                }
            }
            is CollabMessage.KickPeer -> {
                if (!isSessionTokenCompatible(message.sessionToken)) return
                val currentHost = _hostPeerId.value
                if (currentHost != null && message.hostPeerId != currentHost) return

                _bannedPeerIds.update { it + message.targetPeerId }
                _bannedDeviceIds.update { it + message.targetDeviceId }

                if (message.targetPeerId == localPeerId || message.targetDeviceId == localDeviceId) {
                    val code = _roomCode.value ?: ""
                    if (code.isNotBlank()) {
                        _kickedFromRoomCodes.update { it + code }
                    }
                    _lastErrorMessage.value =
                        "You were removed from room ${code.ifBlank { "session" }} by the host and cannot rejoin this session."
                    resetRoomStateToDisconnected()
                } else {
                    _adminPeerIds.update { it - message.targetPeerId }
                    _activePeers.update { it - message.targetPeerId }
                    refreshPeersAndPermissions()
                    val kickedName = message.targetUserName.ifBlank { message.targetPeerId }
                    appendSystemChatNotice("$kickedName was removed from the room by the host.")
                }
            }
            is CollabMessage.SetPeerRole -> {
                if (!isSessionTokenCompatible(message.sessionToken)) return
                val currentHost = _hostPeerId.value
                if (currentHost != null && message.hostPeerId != currentHost) return

                if (message.newRole == CollabRole.ADMIN) {
                    _adminPeerIds.update { it + message.targetPeerId }
                } else if (message.newRole == CollabRole.MEMBER) {
                    _adminPeerIds.update { it - message.targetPeerId }
                }
                refreshPeersAndPermissions()
            }
            is CollabMessage.TransferHostOwnership -> {
                if (!isSessionTokenCompatible(message.sessionToken)) return
                val currentHost = _hostPeerId.value
                if (currentHost != null && message.oldHostPeerId != currentHost) return

                _hostPeerId.value = message.newHostPeerId
                // Old Host becomes normal MEMBER (not Admin); new Host becomes HOST
                _adminPeerIds.update { it - message.newHostPeerId - message.oldHostPeerId }
                _roomMutedPeerIds.update { it - message.newHostPeerId }
                _chatBlockedPeerIds.update { it - message.newHostPeerId }
                _drawingDisabledPeerIds.update { it - message.newHostPeerId }
                _sheetCreationDisabledPeerIds.update { it - message.newHostPeerId }

                val oldName = _activePeers.value[message.oldHostPeerId]?.userTag ?: "Previous Host"
                val newName = _activePeers.value[message.newHostPeerId]?.userTag ?: "New Host"
                _roomCode.value?.let { code ->
                    CollabRoomRegistry.updateHostOwner(code, message.newHostPeerId, newName)
                }

                refreshPeersAndPermissions()
                appendSystemChatNotice("$oldName transferred host ownership to $newName.")
            }
            is CollabMessage.ModMutePeer -> {
                if (!isSessionTokenCompatible(message.sessionToken)) return
                if (!canModeratePeer(targetPeerId = message.targetPeerId, actorPeerId = message.actorPeerId)) return

                if (message.isMuted) {
                    _roomMutedPeerIds.update { it + message.targetPeerId }
                    if (message.targetPeerId == localPeerId) {
                        _isLocalMicMuted.value = true
                    }
                } else {
                    _roomMutedPeerIds.update { it - message.targetPeerId }
                }
                refreshPeersAndPermissions()
            }
            is CollabMessage.BlockPeerChat -> {
                if (!isSessionTokenCompatible(message.sessionToken)) return
                if (!canModeratePeer(targetPeerId = message.targetPeerId, actorPeerId = message.actorPeerId)) return

                if (message.isBlocked) {
                    _chatBlockedPeerIds.update { it + message.targetPeerId }
                } else {
                    _chatBlockedPeerIds.update { it - message.targetPeerId }
                }
                refreshPeersAndPermissions()
            }
            is CollabMessage.CanvasPermissionSync -> {
                if (!isSessionTokenCompatible(message.sessionToken)) return
                val actorRole = _activePeers.value[message.actorPeerId]?.role
                val isAuthorized = message.actorPeerId == _hostPeerId.value ||
                    message.actorPeerId in _adminPeerIds.value ||
                    actorRole == CollabRole.HOST ||
                    actorRole == CollabRole.ADMIN ||
                    (_hostPeerId.value == null && message.actorPeerId != localPeerId)
                if (!isAuthorized) return

                if (message.actorPeerId == _hostPeerId.value || actorRole == CollabRole.HOST || _hostPeerId.value == null) {
                    if (_hostPeerId.value == null && message.actorPeerId != localPeerId) {
                        _hostPeerId.value = message.actorPeerId
                    }
                    _accessMode.value = message.accessMode
                }
                _isGlobalDrawingDisabled.value = message.isGlobalDrawingDisabled
                _isGlobalSheetCreationDisabled.value = message.isGlobalSheetCreationDisabled
                _drawingDisabledPeerIds.value = message.drawingDisabledPeerIds - (_hostPeerId.value ?: "")
                _sheetCreationDisabledPeerIds.value = message.sheetCreationDisabledPeerIds - (_hostPeerId.value ?: "")
                refreshPeersAndPermissions()
            }
            is CollabMessage.RoomSettingsSync -> {
                if (!isSessionTokenCompatible(message.sessionToken)) return
                val currentHost = _hostPeerId.value
                if (currentHost != null && message.hostPeerId != currentHost) return
                if (currentHost == null && message.hostPeerId.isNotBlank()) {
                    _hostPeerId.value = message.hostPeerId
                }
                val sanitized = CollabAudioBitrate.sanitize(message.maxBitrateKbps)
                _roomMaxBitrateKbps.value = sanitized
                if (_audioBitrateKbps.value > sanitized) {
                    setAudioBitrateKbps(sanitized)
                }
                _maxParticipants.value = CollabRoomCapacity.sanitize(message.maxParticipants)
                _joinPolicy.value = message.joinPolicy
                _allowedUserHandles.value = message.allowedUserHandles
                _invitedUserHandles.value = message.invitedUserHandles
            }
            is CollabMessage.RoomClosed -> {
                val activeCode = _roomCode.value
                val matchesRoom = activeCode != null &&
                    message.roomCode.isNotBlank() &&
                    activeCode.equals(message.roomCode, ignoreCase = true)
                if (!matchesRoom && !isSessionTokenCompatible(message.sessionToken)) return
                val currentHost = _hostPeerId.value
                if (currentHost != null && message.hostPeerId.isNotBlank() && message.hostPeerId != currentHost) {
                    return
                }
                if (message.roomCode.isNotBlank()) {
                    CollabRoomRegistry.unregisterRoom(message.roomCode, message.hostPeerId.ifBlank { null })
                }
                _lastErrorMessage.value = "Session ended because the host left the room."
                resetRoomStateToDisconnected()
            }
            is CollabMessage.LeaveRoom -> {
                if (!isSessionTokenCompatible(message.sessionToken)) return
                if (message.peerId != localPeerId) {
                    // Basic Host-Leave Rule: when the current Host leaves, everybody leaves
                    if (message.peerId == _hostPeerId.value) {
                        _roomCode.value?.let { code ->
                            CollabRoomRegistry.unregisterRoom(code, message.peerId)
                        }
                        _lastErrorMessage.value = "Session ended because the host left the room."
                        resetRoomStateToDisconnected()
                    } else {
                        _adminPeerIds.update { it - message.peerId }
                        _activePeers.update { it - message.peerId }
                        _drawingDisabledPeerIds.update { it - message.peerId }
                        _sheetCreationDisabledPeerIds.update { it - message.peerId }
                        refreshPeersAndPermissions()
                    }
                }
            }
        }
    }

    private fun refreshBufferedEventsCount() {
        _bufferedStrokesCount.value =
            strokeBuffer.size + textSyncBuffer.size + imageSyncBuffer.size + removeSyncBuffer.size
    }

    private fun isSessionTokenCompatible(incomingToken: String): Boolean {
        val localToken = _sessionToken.value
        return localToken.isBlank() || incomingToken.isBlank() || localToken == incomingToken
    }

    private fun canPeerDrawUnderGlobalLock(peerId: String): Boolean {
        if (peerId == _hostPeerId.value) return true
        if (_accessMode.value == CollabAccessMode.HOST_AND_ADMIN && peerId in _adminPeerIds.value) {
            return peerId !in _drawingDisabledPeerIds.value
        }
        return false
    }

    private fun refreshPeersAndPermissions() {
        val currentHost = _hostPeerId.value
        val admins = _adminPeerIds.value
        val muted = _roomMutedPeerIds.value
        val chatBlocked = _chatBlockedPeerIds.value

        // Reconcile local role
        val resolvedLocalRole = when {
            localPeerId == currentHost -> CollabRole.HOST
            localPeerId in admins -> CollabRole.ADMIN
            else -> CollabRole.MEMBER
        }
        _localRole.value = resolvedLocalRole

        // Reconcile global switches when all eligible targets are disabled
        val eligibleTargets = getEligibleTargetPeerIds(_accessMode.value)
        if (eligibleTargets.isNotEmpty()) {
            _isGlobalDrawingDisabled.value = eligibleTargets.all { it in _drawingDisabledPeerIds.value }
            _isGlobalSheetCreationDisabled.value = eligibleTargets.all { it in _sheetCreationDisabledPeerIds.value }
        }

        val drawDisabled = _drawingDisabledPeerIds.value
        val sheetDisabled = _sheetCreationDisabledPeerIds.value
        val globalDrawOff = _isGlobalDrawingDisabled.value
        val globalSheetOff = _isGlobalSheetCreationDisabled.value
        val mode = _accessMode.value

        // Update each CollabPeer snapshot with its resolved role & restriction flags
        _activePeers.update { current ->
            current.mapValues { (peerId, peer) ->
                val pRole = when {
                    peerId == currentHost -> CollabRole.HOST
                    peerId in admins -> CollabRole.ADMIN
                    else -> CollabRole.MEMBER
                }
                val pDrawDisabled = when (pRole) {
                    CollabRole.HOST -> false
                    CollabRole.ADMIN -> (mode == CollabAccessMode.HOST_ONLY && globalDrawOff) || (peerId in drawDisabled)
                    CollabRole.MEMBER -> globalDrawOff || (peerId in drawDisabled)
                }
                val pSheetDisabled = when (pRole) {
                    CollabRole.HOST -> false
                    CollabRole.ADMIN -> (mode == CollabAccessMode.HOST_ONLY && globalSheetOff) || (peerId in sheetDisabled)
                    CollabRole.MEMBER -> globalSheetOff || (peerId in sheetDisabled)
                }
                peer.copy(
                    role = pRole,
                    isRoomMuted = peerId in muted,
                    isChatBlocked = peerId in chatBlocked,
                    isDrawingDisabled = pDrawDisabled,
                    isSheetCreationDisabled = pSheetDisabled
                )
            }
        }

        _isLocalUserChatBlocked.value = (resolvedLocalRole != CollabRole.HOST) && (localPeerId in chatBlocked)
        if (localPeerId in muted) {
            _isLocalMicMuted.value = true
        }

        _canLocalUserDraw.value = when (resolvedLocalRole) {
            CollabRole.HOST -> true
            CollabRole.ADMIN -> !((mode == CollabAccessMode.HOST_ONLY && globalDrawOff) || (localPeerId in drawDisabled))
            CollabRole.MEMBER -> !(globalDrawOff || (localPeerId in drawDisabled))
        }

        _canLocalUserCreateSheets.value = when (resolvedLocalRole) {
            CollabRole.HOST -> true
            CollabRole.ADMIN -> !((mode == CollabAccessMode.HOST_ONLY && globalSheetOff) || (localPeerId in sheetDisabled))
            CollabRole.MEMBER -> !(globalSheetOff || (localPeerId in sheetDisabled))
        }

        // Keep pinned message object in sync with chat history
        val pinId = _pinnedChatMessageId.value
        _pinnedChatMessage.value = if (pinId == null) {
            null
        } else {
            _chatMessages.value.firstOrNull { it.messageId == pinId }
        }
        onPeerCursorsUpdated?.invoke(_activePeers.value.values)
    }

    /**
     * Drains and returns all incoming strokes accumulated in the vector merger buffer.
     */
    fun consumeBufferedStrokes(): List<CollabStroke> {
        val list = mutableListOf<CollabStroke>()
        while (true) {
            val stroke = strokeBuffer.poll() ?: break
            list.add(stroke)
        }
        refreshBufferedEventsCount()
        return list
    }

    /**
     * Drains and returns all incoming text annotation element sync messages.
     */
    fun consumeBufferedTextElements(): List<CollabMessage.TextElementSync> {
        val list = mutableListOf<CollabMessage.TextElementSync>()
        while (true) {
            val item = textSyncBuffer.poll() ?: break
            list.add(item)
        }
        refreshBufferedEventsCount()
        return list
    }

    /**
     * Drains and returns all incoming image element sync messages.
     */
    fun consumeBufferedImageElements(): List<CollabMessage.ImageElementSync> {
        val list = mutableListOf<CollabMessage.ImageElementSync>()
        while (true) {
            val item = imageSyncBuffer.poll() ?: break
            list.add(item)
        }
        refreshBufferedEventsCount()
        return list
    }

    /**
     * Drains and returns all incoming element removal sync messages.
     */
    fun consumeBufferedRemovedElements(): List<CollabMessage.ElementsRemoveSync> {
        val list = mutableListOf<CollabMessage.ElementsRemoveSync>()
        while (true) {
            val item = removeSyncBuffer.poll() ?: break
            list.add(item)
        }
        refreshBufferedEventsCount()
        return list
    }

    /**
     * Scans for peers that have been inactive longer than the given timeout threshold, evicting them.
     */
    fun evictTimedOutPeers(
        nowMs: Long = System.currentTimeMillis(),
        timeoutMs: Long = DEFAULT_PEER_TIMEOUT_MS
    ): List<String> {
        val timedOut = mutableListOf<String>()
        _activePeers.update { current ->
            val mutable = current.toMutableMap()
            for ((peerId, peer) in current) {
                if (peerId != localPeerId && (nowMs - peer.lastActiveMs > timeoutMs)) {
                    timedOut.add(peerId)
                    mutable.remove(peerId)
                }
            }
            mutable
        }
        if (timedOut.isNotEmpty()) {
            refreshPeersAndPermissions()
        }
        return timedOut
    }

    /**
     * Transitions session into RECONNECTING state to recover lost socket or network connectivity.
     */
    fun reconnect(): Boolean {
        val currentCode = _roomCode.value ?: return false
        _state.value = CollabSessionState.RECONNECTING

        _state.value = CollabSessionState.IN_ROOM
        val rejoin = CollabMessage.JoinRoom(
            roomCode = currentCode,
            userTag = localUserTag,
            colorHex = localColorHex,
            peerId = localPeerId,
            deviceId = localDeviceId,
            pronouns = localPronouns,
            audioBitrateKbps = _audioBitrateKbps.value,
            sessionToken = _sessionToken.value
        )
        dispatchMessage(rejoin)
        return true
    }

    private fun dispatchMessage(message: CollabMessage) {
        _outgoingMessages.tryEmit(message)
        lanTransport?.send(message)
        transportBridge?.invoke(message)
    }
}
