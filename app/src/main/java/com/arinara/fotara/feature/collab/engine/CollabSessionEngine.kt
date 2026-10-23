// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.collab.engine

import com.arinara.fotara.feature.collab.model.CollabChatMessage
import com.arinara.fotara.feature.collab.model.CollabMessage
import com.arinara.fotara.feature.collab.model.CollabPeer
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
 * Manages peer memberships, vector stroke buffering, real-time room chat, active session guarding,
 * cursor interpolation, and timeout eviction.
 */
class CollabSessionEngine(
    initialLocalPeerId: String = UUID.randomUUID().toString(),
    initialLocalUserTag: String = "Host",
    initialLocalColorHex: String = "#7A5AF8"
) : AutoCloseable {

    companion object {
        const val DEFAULT_PEER_TIMEOUT_MS: Long = 15_000L
        const val MAX_STROKE_BUFFER_SIZE: Int = 1_000
        const val MAX_CHAT_HISTORY_SIZE: Int = 200
    }

    @Volatile
    var localPeerId: String = initialLocalPeerId
        private set

    @Volatile
    var localUserTag: String = initialLocalUserTag
        private set

    @Volatile
    var localColorHex: String = initialLocalColorHex
        private set

    private val _state = MutableStateFlow(CollabSessionState.DISCONNECTED)
    val state: StateFlow<CollabSessionState> = _state.asStateFlow()

    private val _roomCode = MutableStateFlow<String?>(null)
    val roomCode: StateFlow<String?> = _roomCode.asStateFlow()

    private val _activePeers = MutableStateFlow<Map<String, CollabPeer>>(emptyMap())
    val activePeers: StateFlow<Map<String, CollabPeer>> = _activePeers.asStateFlow()

    private val _chatMessages = MutableStateFlow<List<CollabChatMessage>>(emptyList())
    val chatMessages: StateFlow<List<CollabChatMessage>> = _chatMessages.asStateFlow()

    private val _unreadChatCount = MutableStateFlow(0)
    val unreadChatCount: StateFlow<Int> = _unreadChatCount.asStateFlow()

    @Volatile
    var isChatSheetOpen: Boolean = false
        private set

    private val _outgoingMessages = MutableSharedFlow<CollabMessage>(extraBufferCapacity = 64)
    val outgoingMessages: SharedFlow<CollabMessage> = _outgoingMessages.asSharedFlow()

    // Thread-safe vector stroke merger buffer
    private val strokeBuffer = ConcurrentLinkedQueue<CollabStroke>()
    private val _bufferedStrokesCount = MutableStateFlow(0)
    val bufferedStrokesCount: StateFlow<Int> = _bufferedStrokesCount.asStateFlow()

    /**
     * Returns true if a collaborative room is currently active.
     */
    val isSessionActive: Boolean
        get() = _state.value == CollabSessionState.IN_ROOM && _roomCode.value != null

    /**
     * Optional transport bridge callback for external networks (WebSockets / WebRTC data channels).
     */
    @Volatile
    var transportBridge: ((CollabMessage) -> Unit)? = null

    override fun close() {
        leaveRoom()
        transportBridge = null
        strokeBuffer.clear()
        _bufferedStrokesCount.value = 0
    }

    /**
     * Creates a new collaborative study room, generating an official 6-digit room code `FT-XXXX`.
     * Guarded: if a session is already active, returns the existing room code without overwriting
     * or resetting the active session. Call [leaveRoom] first to start a different session.
     */
    @Synchronized
    fun createRoom(
        userTag: String = localUserTag,
        colorHex: String = localColorHex
    ): String {
        val existingCode = _roomCode.value
        if (_state.value == CollabSessionState.IN_ROOM && !existingCode.isNullOrBlank()) {
            return existingCode
        }

        localUserTag = userTag
        localColorHex = colorHex

        _state.value = CollabSessionState.CONNECTING
        val code = CollabRoomCode.generateRandomCode()
        _roomCode.value = code

        _activePeers.value = mapOf(
            localPeerId to CollabPeer(
                peerId = localPeerId,
                userTag = localUserTag,
                colorHex = localColorHex,
                lastActiveMs = System.currentTimeMillis()
            )
        )

        strokeBuffer.clear()
        _bufferedStrokesCount.value = 0
        _chatMessages.value = emptyList()
        _unreadChatCount.value = 0

        _state.value = CollabSessionState.IN_ROOM

        val peerJoinedMessage = CollabMessage.PeerJoined(
            peerId = localPeerId,
            userTag = localUserTag,
            colorHex = localColorHex
        )
        dispatchMessage(peerJoinedMessage)

        return code
    }

    /**
     * Joins an existing collaborative study room by validating the 6-digit room code `FT-XXXX`.
     * Guarded: if another session is already active, rejects joining until [leaveRoom] is called.
     */
    @Synchronized
    fun joinRoom(
        code: String,
        userTag: String = localUserTag,
        colorHex: String = localColorHex
    ): Boolean {
        if (!CollabRoomCode.isValid(code)) {
            return false
        }

        val normalizedCode = CollabRoomCode.normalize(code)
        val existingCode = _roomCode.value
        if (_state.value == CollabSessionState.IN_ROOM && !existingCode.isNullOrBlank()) {
            // Cannot overwrite an already active session with a new session
            return existingCode == normalizedCode
        }

        localUserTag = userTag
        localColorHex = colorHex

        _state.value = CollabSessionState.CONNECTING
        _roomCode.value = normalizedCode

        strokeBuffer.clear()
        _bufferedStrokesCount.value = 0
        _chatMessages.value = emptyList()
        _unreadChatCount.value = 0

        _activePeers.value = mapOf(
            localPeerId to CollabPeer(
                peerId = localPeerId,
                userTag = localUserTag,
                colorHex = localColorHex,
                lastActiveMs = System.currentTimeMillis()
            )
        )

        _state.value = CollabSessionState.IN_ROOM

        val joinMessage = CollabMessage.JoinRoom(
            roomCode = normalizedCode,
            userTag = localUserTag,
            colorHex = localColorHex
        )
        dispatchMessage(joinMessage)

        val peerJoinedMessage = CollabMessage.PeerJoined(
            peerId = localPeerId,
            userTag = localUserTag,
            colorHex = localColorHex
        )
        dispatchMessage(peerJoinedMessage)

        return true
    }

    /**
     * Leaves the active collaborative room and resets session state to DISCONNECTED.
     */
    @Synchronized
    fun leaveRoom() {
        if (_state.value != CollabSessionState.DISCONNECTED) {
            val leaveMsg = CollabMessage.LeaveRoom(peerId = localPeerId)
            dispatchMessage(leaveMsg)
        }

        _state.value = CollabSessionState.DISCONNECTED
        _roomCode.value = null
        _activePeers.value = emptyMap()
        _chatMessages.value = emptyList()
        _unreadChatCount.value = 0
        isChatSheetOpen = false
        strokeBuffer.clear()
        _bufferedStrokesCount.value = 0
    }

    /**
     * Sends a real-time chat message to all collaborators in the active room.
     */
    fun sendChatMessage(rawText: String): CollabChatMessage? {
        if (!isSessionActive) return null
        val sanitized = rawText.trim().take(500)
        if (sanitized.isEmpty()) return null

        val now = System.currentTimeMillis()
        val msgId = UUID.randomUUID().toString()
        val chatEntry = CollabChatMessage(
            messageId = msgId,
            peerId = localPeerId,
            senderTag = localUserTag,
            senderColorHex = localColorHex,
            text = sanitized,
            timestamp = now,
            isLocal = true
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
            timestamp = now
        )
        dispatchMessage(broadcast)
        return chatEntry
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
     * Broadcasts a drawn vector stroke to all room participants.
     */
    fun broadcastStroke(
        points: List<Float>,
        colorHex: String = localColorHex,
        strokeWidth: Float = 4.0f,
        strokeId: String = UUID.randomUUID().toString()
    ): CollabMessage.StrokeBroadcast {
        val msg = CollabMessage.StrokeBroadcast(
            strokeId = strokeId,
            points = points,
            colorHex = colorHex,
            strokeWidth = strokeWidth
        )
        dispatchMessage(msg)
        return msg
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
            timestamp = now
        )

        // Update local peer state as well
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

        dispatchMessage(msg)
        return msg
    }

    /**
     * Ingests and processes incoming messages from peers or the network transport.
     */
    fun receiveMessage(message: CollabMessage) {
        when (message) {
            is CollabMessage.JoinRoom -> {
                if (_state.value == CollabSessionState.IN_ROOM) {
                    val echo = CollabMessage.PeerJoined(
                        peerId = localPeerId,
                        userTag = localUserTag,
                        colorHex = localColorHex
                    )
                    dispatchMessage(echo)
                }
            }
            is CollabMessage.PeerJoined -> {
                if (message.peerId != localPeerId) {
                    _activePeers.update { current ->
                        val existing = current[message.peerId]
                        val updated = existing?.copy(
                            userTag = message.userTag,
                            colorHex = message.colorHex,
                            lastActiveMs = System.currentTimeMillis()
                        ) ?: CollabPeer(
                            peerId = message.peerId,
                            userTag = message.userTag,
                            colorHex = message.colorHex,
                            lastActiveMs = System.currentTimeMillis()
                        )
                        current + (message.peerId to updated)
                    }
                }
            }
            is CollabMessage.StrokeBroadcast -> {
                val stroke = CollabStroke(
                    strokeId = message.strokeId,
                    peerId = "peer",
                    points = message.points,
                    colorHex = message.colorHex,
                    strokeWidth = message.strokeWidth,
                    timestamp = System.currentTimeMillis()
                )
                while (strokeBuffer.size >= MAX_STROKE_BUFFER_SIZE) {
                    strokeBuffer.poll()
                }
                strokeBuffer.add(stroke)
                _bufferedStrokesCount.value = strokeBuffer.size
            }
            is CollabMessage.CursorBroadcast -> {
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
                }
            }
            is CollabMessage.ChatBroadcast -> {
                val isFromSelf = message.peerId == localPeerId
                var addedNew = false
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
                            isLocal = isFromSelf
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
                            lastActiveMs = message.timestamp
                        ) ?: CollabPeer(
                            peerId = message.peerId,
                            userTag = message.senderTag,
                            colorHex = message.senderColorHex,
                            lastActiveMs = message.timestamp
                        )
                        current + (message.peerId to updated)
                    }
                }
            }
            is CollabMessage.LeaveRoom -> {
                if (message.peerId != localPeerId) {
                    _activePeers.update { current ->
                        current - message.peerId
                    }
                }
            }
        }
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
        _bufferedStrokesCount.value = 0
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
            colorHex = localColorHex
        )
        dispatchMessage(rejoin)
        return true
    }

    private fun dispatchMessage(message: CollabMessage) {
        _outgoingMessages.tryEmit(message)
        transportBridge?.invoke(message)
    }
}
