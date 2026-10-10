// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.collab.engine

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
 * Manages peer memberships, vector stroke buffering, cursor interpolation, and timeout eviction.
 */
class CollabSessionEngine(
    initialLocalPeerId: String = UUID.randomUUID().toString(),
    initialLocalUserTag: String = "Host",
    initialLocalColorHex: String = "#2563EB"
) {

    companion object {
        const val DEFAULT_PEER_TIMEOUT_MS: Long = 15_000L
    }

    var localPeerId: String = initialLocalPeerId
        private set

    var localUserTag: String = initialLocalUserTag
        private set

    var localColorHex: String = initialLocalColorHex
        private set

    private val _state = MutableStateFlow(CollabSessionState.DISCONNECTED)
    val state: StateFlow<CollabSessionState> = _state.asStateFlow()

    private val _roomCode = MutableStateFlow<String?>(null)
    val roomCode: StateFlow<String?> = _roomCode.asStateFlow()

    private val _activePeers = MutableStateFlow<Map<String, CollabPeer>>(emptyMap())
    val activePeers: StateFlow<Map<String, CollabPeer>> = _activePeers.asStateFlow()

    private val _outgoingMessages = MutableSharedFlow<CollabMessage>(extraBufferCapacity = 64)
    val outgoingMessages: SharedFlow<CollabMessage> = _outgoingMessages.asSharedFlow()

    // Thread-safe vector stroke merger buffer
    private val strokeBuffer = ConcurrentLinkedQueue<CollabStroke>()
    private val _bufferedStrokesCount = MutableStateFlow(0)
    val bufferedStrokesCount: StateFlow<Int> = _bufferedStrokesCount.asStateFlow()

    /**
     * Optional transport bridge callback for external networks (WebSockets / WebRTC data channels).
     */
    var transportBridge: ((CollabMessage) -> Unit)? = null

    /**
     * Creates a new collaborative study room, generating an official 6-digit room code `FT-XXXX`.
     */
    fun createRoom(
        userTag: String = localUserTag,
        colorHex: String = localColorHex
    ): String {
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
     */
    fun joinRoom(
        code: String,
        userTag: String = localUserTag,
        colorHex: String = localColorHex
    ): Boolean {
        if (!CollabRoomCode.isValid(code)) {
            return false
        }

        localUserTag = userTag
        localColorHex = colorHex

        val normalizedCode = CollabRoomCode.normalize(code)
        _state.value = CollabSessionState.CONNECTING
        _roomCode.value = normalizedCode

        strokeBuffer.clear()
        _bufferedStrokesCount.value = 0

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
    fun leaveRoom() {
        if (_state.value != CollabSessionState.DISCONNECTED) {
            val leaveMsg = CollabMessage.LeaveRoom(peerId = localPeerId)
            dispatchMessage(leaveMsg)
        }

        _state.value = CollabSessionState.DISCONNECTED
        _roomCode.value = null
        _activePeers.value = emptyMap()
        strokeBuffer.clear()
        _bufferedStrokesCount.value = 0
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
                // Inform the joiner about local peer existence if in room
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
                // Buffer the stroke for canvas ingestion
                val stroke = CollabStroke(
                    strokeId = message.strokeId,
                    peerId = "peer",
                    points = message.points,
                    colorHex = message.colorHex,
                    strokeWidth = message.strokeWidth,
                    timestamp = System.currentTimeMillis()
                )
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
                            colorHex = "#2563EB",
                            lastCursorX = message.x,
                            lastCursorY = message.y,
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

        // Restore to in-room
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
