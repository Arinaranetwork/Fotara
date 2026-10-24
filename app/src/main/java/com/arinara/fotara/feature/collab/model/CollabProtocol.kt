// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.collab.model

import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Three-tier role hierarchy inside a collaborative study room.
 * - [HOST]: Single room owner and central authority. Has all moderation, pin, kick, admin promotion,
 *   canvas lock, and ownership transfer privileges. When the Host leaves, the room closes for everyone.
 * - [ADMIN]: Promoted by the Host. Can room-mute members, block member chat, delete any chat message,
 *   and toggle member drawing/sheet restrictions (when access mode is [CollabAccessMode.HOST_AND_ADMIN]).
 * - [MEMBER]: Standard participant.
 */
enum class CollabRole {
    HOST,
    ADMIN,
    MEMBER;

    companion object {
        fun fromString(value: String?): CollabRole {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: MEMBER
        }
    }
}

/**
 * Defines who retains drawing and sheet-creation access when global or room restrictions are active.
 * - [HOST_AND_ADMIN]: Only Host and Admins have access (normal Members are restricted).
 * - [HOST_ONLY]: Only the Host has access (both Admins and Members are restricted).
 */
enum class CollabAccessMode {
    HOST_AND_ADMIN,
    HOST_ONLY;

    companion object {
        fun fromString(value: String?): CollabAccessMode {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: HOST_AND_ADMIN
        }
    }
}

/**
 * Defines who is permitted to join the collaborative study room (`2.1` Privacy Settings).
 * - [OPEN]: Anyone with the `FT-XXXX` room code can join (up to [CollabRoomCapacity.MAX_PARTICIPANTS]).
 * - [FRIENDS_ONLY]: Only users present in the Host's Friend List can join.
 * - [CUSTOM_PRESET]: Only users matching the Host's custom preset of usernames or selected friends can join.
 * - [INVITED_ONLY]: Only users explicitly invited by the Host for the current session can join.
 */
enum class CollabJoinPolicy(val displayLabel: String) {
    OPEN("Anyone"),
    FRIENDS_ONLY("Friends Only"),
    CUSTOM_PRESET("Custom Preset"),
    INVITED_ONLY("Invited Only");

    companion object {
        fun fromString(value: String?): CollabJoinPolicy {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: OPEN
        }
    }
}

/**
 * Participant capacity constraints and quick-select presets for a collaborative study room.
 */
object CollabRoomCapacity {
    const val MIN_PARTICIPANTS = 2
    const val MAX_PARTICIPANTS = 32
    const val DEFAULT_MAX_PARTICIPANTS = 16

    val PRESETS = listOf(2, 4, 8, 16, 32)

    fun sanitize(limit: Int): Int {
        return limit.coerceIn(MIN_PARTICIPANTS, MAX_PARTICIPANTS)
    }
}

/**
 * Reusable named preset of allowed `@username` handles or friend identifiers for [CollabJoinPolicy.CUSTOM_PRESET].
 */
data class CollabPrivacyPreset(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val allowedHandles: Set<String>
) {
    companion object {
        private const val RECORD_DELIMITER = "\n---PRESET---\n"
        private const val FIELD_DELIMITER = "\t"

        fun normalizeHandle(raw: String): String {
            val trimmed = raw.trim().lowercase()
            if (trimmed.isEmpty()) return ""
            return if (trimmed.startsWith("@")) trimmed else "@$trimmed"
        }

        fun encodeList(presets: List<CollabPrivacyPreset>): String {
            if (presets.isEmpty()) return ""
            return presets.joinToString(RECORD_DELIMITER) { preset ->
                val cleanName = preset.name.replace(FIELD_DELIMITER, " ").replace("\n", " ").trim()
                val handlesCsv = preset.allowedHandles
                    .map { normalizeHandle(it) }
                    .filter { it.length > 1 }
                    .joinToString(",")
                "${preset.id}$FIELD_DELIMITER$cleanName$FIELD_DELIMITER$handlesCsv"
            }
        }

        fun decodeList(serialized: String?): List<CollabPrivacyPreset> {
            if (serialized.isNullOrBlank()) return emptyList()
            val result = ArrayList<CollabPrivacyPreset>()
            for (record in serialized.split(RECORD_DELIMITER)) {
                val trimmed = record.trim()
                if (trimmed.isEmpty()) continue
                val parts = trimmed.split(FIELD_DELIMITER)
                if (parts.size >= 2) {
                    val id = parts[0].trim().ifEmpty { UUID.randomUUID().toString() }
                    val name = parts[1].trim()
                    if (name.isEmpty()) continue
                    val handles = if (parts.size >= 3 && parts[2].isNotBlank()) {
                        parts[2].split(",")
                            .map { normalizeHandle(it) }
                            .filter { it.length > 1 }
                            .toCollection(LinkedHashSet())
                    } else {
                        emptySet()
                    }
                    result.add(CollabPrivacyPreset(id = id, name = name, allowedHandles = handles))
                }
            }
            return result
        }
    }
}

/**
 * Supported voice/audio bitrate presets (in kbps) for low-CPU smartphone audio streaming.
 */
object CollabAudioBitrate {
    const val LOW_DATA_16_KBPS = 16
    const val BALANCED_32_KBPS = 32
    const val HIGH_CLARITY_64_KBPS = 64
    const val STUDIO_LAN_96_KBPS = 96

    val PRESETS = listOf(
        LOW_DATA_16_KBPS,
        BALANCED_32_KBPS,
        HIGH_CLARITY_64_KBPS,
        STUDIO_LAN_96_KBPS
    )

    fun sanitize(kbps: Int): Int {
        return PRESETS.minByOrNull { kotlin.math.abs(it - kbps) } ?: BALANCED_32_KBPS
    }
}

/**
 * Validation and generation utilities for Fotara Collaborative Study Room codes.
 * Formats codes according to the standard pattern `FT-[0-9A-Z]{4}`.
 */
object CollabRoomCode {
    const val PREFIX = "FT-"
    val REGEX = Regex("^FT-[0-9A-Z]{4}$")
    private val CHAR_POOL = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray()

    /**
     * Checks if the given room code strictly adheres to the `FT-[0-9A-Z]{4}` format.
     */
    fun isValid(code: String?): Boolean {
        if (code == null) return false
        val trimmed = code.trim().uppercase()
        return REGEX.matches(trimmed)
    }

    /**
     * Normalizes arbitrary user text or partial input into the canonical `FT-XXXX` representation.
     */
    fun normalize(input: String?): String {
        if (input.isNullOrBlank()) return ""
        val sanitized = input.trim().uppercase().replace(" ", "")

        val suffix = when {
            sanitized.startsWith(PREFIX) -> {
                sanitized.removePrefix(PREFIX).filter { it.isLetterOrDigit() }.take(4)
            }
            sanitized.startsWith("FT") && sanitized.length > 2 && sanitized[2] != '-' -> {
                sanitized.substring(2).filter { it.isLetterOrDigit() }.take(4)
            }
            sanitized.startsWith("FT") -> {
                sanitized.substring(2).filter { it.isLetterOrDigit() }.take(4)
            }
            else -> {
                sanitized.filter { it.isLetterOrDigit() }.take(4)
            }
        }

        return if (suffix.isEmpty()) "" else "$PREFIX$suffix"
    }

    /**
     * Generates a random 6-character study session room code formatted as `FT-XXXX`.
     */
    fun generateRandomCode(): String {
        val randomSuffix = (1..4)
            .map { CHAR_POOL.random() }
            .joinToString("")
        return "$PREFIX$randomSuffix"
    }
}

/**
 * Protocol messages exchanged between collaborators in a live study canvas room.
 */
sealed class CollabMessage {
    abstract val messageType: String

    data class RoomProbe(
        val roomCode: String,
        val requesterPeerId: String
    ) : CollabMessage() {
        override val messageType: String = TYPE_ROOM_PROBE
    }

    data class WhoHasRoom(
        val roomCode: String,
        val joinerPeerId: String,
        val joinerDeviceId: String = joinerPeerId,
        val joinerUserName: String = ""
    ) : CollabMessage() {
        override val messageType: String = TYPE_WHO_HAS_ROOM
    }

    data class HostAnnounce(
        val roomCode: String,
        val sessionToken: String,
        val hostPeerId: String,
        val hostUserName: String,
        val lanIp: String = "",
        val lanPort: Int = 0,
        val maxBitrateKbps: Int = CollabAudioBitrate.HIGH_CLARITY_64_KBPS,
        val createdAt: Long = System.currentTimeMillis()
    ) : CollabMessage() {
        override val messageType: String = TYPE_HOST_ANNOUNCE
    }

    data class JoinRoom(
        val roomCode: String,
        val userTag: String,
        val colorHex: String,
        val peerId: String = "",
        val deviceId: String = peerId,
        val pronouns: String = "",
        val userHandle: String = "",
        val audioBitrateKbps: Int = CollabAudioBitrate.BALANCED_32_KBPS,
        val sessionToken: String = ""
    ) : CollabMessage() {
        override val messageType: String = TYPE_JOIN_ROOM
    }

    data class JoinRejected(
        val roomCode: String,
        val sessionToken: String,
        val targetPeerId: String,
        val targetDeviceId: String = targetPeerId,
        val reason: String = REASON_BANNED_BY_HOST
    ) : CollabMessage() {
        override val messageType: String = TYPE_JOIN_REJECTED

        companion object {
            const val REASON_BANNED_BY_HOST = "BANNED_BY_HOST"
            const val REASON_ROOM_NOT_FOUND = "ROOM_NOT_FOUND"
            const val REASON_CODE_ALREADY_IN_USE = "CODE_ALREADY_IN_USE"
            const val REASON_ROOM_CLOSED = "ROOM_CLOSED"
            const val REASON_ROOM_FULL = "ROOM_FULL"
            const val REASON_FRIENDS_ONLY = "FRIENDS_ONLY"
            const val REASON_NOT_IN_PRESET = "NOT_IN_CUSTOM_PRESET"
            const val REASON_INVITE_ONLY = "INVITE_ONLY"
        }
    }

    data class PeerJoined(
        val peerId: String,
        val userTag: String,
        val colorHex: String,
        val deviceId: String = peerId,
        val pronouns: String = "",
        val userHandle: String = "",
        val role: CollabRole = CollabRole.MEMBER,
        val audioBitrateKbps: Int = CollabAudioBitrate.BALANCED_32_KBPS,
        val sessionToken: String = ""
    ) : CollabMessage() {
        override val messageType: String = TYPE_PEER_JOINED
    }

    data class StrokeBroadcast(
        val strokeId: String,
        val points: List<Float>,
        val colorHex: String,
        val strokeWidth: Float,
        val peerId: String = "",
        val sessionToken: String = "",
        val toolType: String = "PEN",
        val blendMode: String = "NORMAL",
        val layerId: String = "layer_default",
        val zIndex: Int = 0
    ) : CollabMessage() {
        override val messageType: String = TYPE_STROKE_BROADCAST
    }

    data class TextElementSync(
        val elementId: String,
        val layerId: String = "layer_default",
        val text: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val fontSizeSp: Float = 18f,
        val color: Long = 0xFFF8FAFC,
        val fontWeight: Int = 500,
        val backgroundStyle: String = "TRANSPARENT",
        val alignment: String = "LEFT",
        val rotationDegrees: Float = 0f,
        val zIndex: Int = 0,
        val peerId: String = "",
        val sessionToken: String = ""
    ) : CollabMessage() {
        override val messageType: String = TYPE_TEXT_ELEMENT_SYNC
    }

    data class ImageElementSync(
        val elementId: String,
        val layerId: String = "layer_default",
        val assetId: String,
        val x: Float,
        val y: Float,
        val width: Float,
        val height: Float,
        val scale: Float = 1f,
        val rotationDegrees: Float = 0f,
        val zIndex: Int = 0,
        val imageBase64: String = "",
        val chromaKeyEnabled: Boolean = false,
        val keyColor: Long = 0xFF00FF00,
        val tolerance: Float = 0.25f,
        val edgeSoftening: Float = 0.08f,
        val brightness: Float = 0f,
        val contrast: Float = 1f,
        val saturation: Float = 1f,
        val temperature: Float = 0f,
        val redBalance: Float = 1f,
        val greenBalance: Float = 1f,
        val blueBalance: Float = 1f,
        val peerId: String = "",
        val sessionToken: String = ""
    ) : CollabMessage() {
        override val messageType: String = TYPE_IMAGE_ELEMENT_SYNC
    }

    data class ElementsRemoveSync(
        val elementIds: List<String>,
        val peerId: String = "",
        val sessionToken: String = ""
    ) : CollabMessage() {
        override val messageType: String = TYPE_ELEMENTS_REMOVE_SYNC
    }

    data class AudioStreamFrame(
        val peerId: String,
        val sequenceNumber: Long,
        val sampleRateHz: Int = 16000,
        val pcmDataBase64: String,
        val bitrateKbps: Int = CollabAudioBitrate.BALANCED_32_KBPS,
        val sessionToken: String = ""
    ) : CollabMessage() {
        override val messageType: String = TYPE_AUDIO_STREAM_FRAME
    }

    data class CursorBroadcast(
        val peerId: String,
        val x: Float,
        val y: Float,
        val timestamp: Long,
        val sessionToken: String = ""
    ) : CollabMessage() {
        override val messageType: String = TYPE_CURSOR_BROADCAST
    }

    data class ChatBroadcast(
        val messageId: String = UUID.randomUUID().toString(),
        val peerId: String,
        val senderTag: String,
        val senderColorHex: String,
        val text: String,
        val timestamp: Long = System.currentTimeMillis(),
        val senderPronouns: String = "",
        val senderRole: CollabRole = CollabRole.MEMBER,
        val sessionToken: String = ""
    ) : CollabMessage() {
        override val messageType: String = TYPE_CHAT_BROADCAST
    }

    data class PinChatMessage(
        val sessionToken: String = "",
        val hostPeerId: String,
        val pinnedMessageId: String?,
        val timestamp: Long = System.currentTimeMillis()
    ) : CollabMessage() {
        override val messageType: String = TYPE_PIN_CHAT_MESSAGE
    }

    data class DeleteChatMessage(
        val sessionToken: String = "",
        val actorPeerId: String,
        val messageId: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : CollabMessage() {
        override val messageType: String = TYPE_DELETE_CHAT_MESSAGE
    }

    data class KickPeer(
        val sessionToken: String = "",
        val hostPeerId: String,
        val targetPeerId: String,
        val targetDeviceId: String = targetPeerId,
        val targetUserName: String = "",
        val reason: String = "KICKED_BY_HOST"
    ) : CollabMessage() {
        override val messageType: String = TYPE_KICK_PEER
    }

    data class SetPeerRole(
        val sessionToken: String = "",
        val hostPeerId: String,
        val targetPeerId: String,
        val newRole: CollabRole
    ) : CollabMessage() {
        override val messageType: String = TYPE_SET_PEER_ROLE
    }

    data class TransferHostOwnership(
        val sessionToken: String = "",
        val oldHostPeerId: String,
        val newHostPeerId: String,
        val timestamp: Long = System.currentTimeMillis()
    ) : CollabMessage() {
        override val messageType: String = TYPE_TRANSFER_HOST_OWNERSHIP
    }

    data class ModMutePeer(
        val sessionToken: String = "",
        val actorPeerId: String,
        val targetPeerId: String,
        val isMuted: Boolean
    ) : CollabMessage() {
        override val messageType: String = TYPE_MOD_MUTE_PEER
    }

    data class BlockPeerChat(
        val sessionToken: String = "",
        val actorPeerId: String,
        val targetPeerId: String,
        val isBlocked: Boolean
    ) : CollabMessage() {
        override val messageType: String = TYPE_BLOCK_PEER_CHAT
    }

    data class CanvasPermissionSync(
        val sessionToken: String = "",
        val actorPeerId: String,
        val accessMode: CollabAccessMode = CollabAccessMode.HOST_AND_ADMIN,
        val isGlobalDrawingDisabled: Boolean = false,
        val isGlobalSheetCreationDisabled: Boolean = false,
        val drawingDisabledPeerIds: Set<String> = emptySet(),
        val sheetCreationDisabledPeerIds: Set<String> = emptySet()
    ) : CollabMessage() {
        override val messageType: String = TYPE_CANVAS_PERMISSION_SYNC
    }

    data class RoomSettingsSync(
        val sessionToken: String = "",
        val hostPeerId: String,
        val maxBitrateKbps: Int,
        val maxParticipants: Int = CollabRoomCapacity.DEFAULT_MAX_PARTICIPANTS,
        val joinPolicy: CollabJoinPolicy = CollabJoinPolicy.OPEN,
        val allowedUserHandles: Set<String> = emptySet(),
        val invitedUserHandles: Set<String> = emptySet()
    ) : CollabMessage() {
        override val messageType: String = TYPE_ROOM_SETTINGS_SYNC
    }

    data class RoomClosed(
        val roomCode: String,
        val sessionToken: String = "",
        val hostPeerId: String,
        val reason: String = "HOST_LEFT"
    ) : CollabMessage() {
        override val messageType: String = TYPE_ROOM_CLOSED
    }

    data class LeaveRoom(
        val peerId: String,
        val sessionToken: String = ""
    ) : CollabMessage() {
        override val messageType: String = TYPE_LEAVE_ROOM
    }

    companion object {
        const val TYPE_ROOM_PROBE = "ROOM_PROBE"
        const val TYPE_WHO_HAS_ROOM = "WHO_HAS_ROOM"
        const val TYPE_HOST_ANNOUNCE = "HOST_ANNOUNCE"
        const val TYPE_JOIN_ROOM = "JOIN_ROOM"
        const val TYPE_JOIN_REJECTED = "JOIN_REJECTED"
        const val TYPE_PEER_JOINED = "PEER_JOINED"
        const val TYPE_STROKE_BROADCAST = "STROKE_BROADCAST"
        const val TYPE_TEXT_ELEMENT_SYNC = "TEXT_ELEMENT_SYNC"
        const val TYPE_IMAGE_ELEMENT_SYNC = "IMAGE_ELEMENT_SYNC"
        const val TYPE_ELEMENTS_REMOVE_SYNC = "ELEMENTS_REMOVE_SYNC"
        const val TYPE_AUDIO_STREAM_FRAME = "AUDIO_STREAM_FRAME"
        const val TYPE_CURSOR_BROADCAST = "CURSOR_BROADCAST"
        const val TYPE_CHAT_BROADCAST = "CHAT_BROADCAST"
        const val TYPE_PIN_CHAT_MESSAGE = "PIN_CHAT_MESSAGE"
        const val TYPE_DELETE_CHAT_MESSAGE = "DELETE_CHAT_MESSAGE"
        const val TYPE_KICK_PEER = "KICK_PEER"
        const val TYPE_SET_PEER_ROLE = "SET_PEER_ROLE"
        const val TYPE_TRANSFER_HOST_OWNERSHIP = "TRANSFER_HOST_OWNERSHIP"
        const val TYPE_MOD_MUTE_PEER = "MOD_MUTE_PEER"
        const val TYPE_BLOCK_PEER_CHAT = "BLOCK_PEER_CHAT"
        const val TYPE_CANVAS_PERMISSION_SYNC = "CANVAS_PERMISSION_SYNC"
        const val TYPE_ROOM_SETTINGS_SYNC = "ROOM_SETTINGS_SYNC"
        const val TYPE_ROOM_CLOSED = "ROOM_CLOSED"
        const val TYPE_LEAVE_ROOM = "LEAVE_ROOM"
    }
}

/**
 * Active participant in a collaborative study canvas session.
 * [userTag] carries the participant's real [com.arinara.fotara.data.model.UserProfile] display name.
 */
data class CollabPeer(
    val peerId: String,
    val userTag: String,
    val colorHex: String,
    val lastCursorX: Float = 0f,
    val lastCursorY: Float = 0f,
    val lastActiveMs: Long = System.currentTimeMillis(),
    val deviceId: String = peerId,
    val pronouns: String = "",
    val userHandle: String = "",
    val role: CollabRole = CollabRole.MEMBER,
    val audioBitrateKbps: Int = CollabAudioBitrate.BALANCED_32_KBPS,
    val isRoomMuted: Boolean = false,
    val isChatBlocked: Boolean = false,
    val isDrawingDisabled: Boolean = false,
    val isSheetCreationDisabled: Boolean = false
)

/**
 * Vector stroke received from a collaborator, buffered for canvas merging.
 */
data class CollabStroke(
    val strokeId: String,
    val peerId: String,
    val points: List<Float>,
    val colorHex: String,
    val strokeWidth: Float,
    val timestamp: Long = System.currentTimeMillis(),
    val toolType: String = "PEN",
    val blendMode: String = "NORMAL",
    val layerId: String = "layer_default",
    val zIndex: Int = 0
)

/**
 * Real-time chat message exchanged inside an active collaborative study room.
 * [senderTag] carries the sender's real [com.arinara.fotara.data.model.UserProfile] display name.
 */
data class CollabChatMessage(
    val messageId: String = UUID.randomUUID().toString(),
    val peerId: String,
    val senderTag: String,
    val senderColorHex: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isLocal: Boolean = false,
    val senderPronouns: String = "",
    val senderRole: CollabRole = CollabRole.MEMBER,
    val isSystemNotice: Boolean = false
)

/**
 * Lightweight, zero-reflection codec for serializing and deserializing CollabMessage frames
 * with minimal CPU and heap overhead on mobile devices.
 */
object CollabProtocolCodec {

    fun encode(message: CollabMessage): String {
        return when (message) {
            is CollabMessage.RoomProbe -> buildString(96) {
                append("{\"type\":\"").append(CollabMessage.TYPE_ROOM_PROBE).append("\",")
                append("\"roomCode\":\"").append(escapeJson(message.roomCode)).append("\",")
                append("\"requesterPeerId\":\"").append(escapeJson(message.requesterPeerId)).append("\"}")
            }
            is CollabMessage.WhoHasRoom -> buildString(140) {
                append("{\"type\":\"").append(CollabMessage.TYPE_WHO_HAS_ROOM).append("\",")
                append("\"roomCode\":\"").append(escapeJson(message.roomCode)).append("\",")
                append("\"joinerPeerId\":\"").append(escapeJson(message.joinerPeerId)).append("\",")
                append("\"joinerDeviceId\":\"").append(escapeJson(message.joinerDeviceId)).append("\",")
                append("\"joinerUserName\":\"").append(escapeJson(message.joinerUserName)).append("\"}")
            }
            is CollabMessage.HostAnnounce -> buildString(220) {
                append("{\"type\":\"").append(CollabMessage.TYPE_HOST_ANNOUNCE).append("\",")
                append("\"roomCode\":\"").append(escapeJson(message.roomCode)).append("\",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\",")
                append("\"hostPeerId\":\"").append(escapeJson(message.hostPeerId)).append("\",")
                append("\"hostUserName\":\"").append(escapeJson(message.hostUserName)).append("\",")
                append("\"lanIp\":\"").append(escapeJson(message.lanIp)).append("\",")
                append("\"lanPort\":").append(message.lanPort).append(",")
                append("\"maxBitrateKbps\":").append(message.maxBitrateKbps).append(",")
                append("\"createdAt\":").append(message.createdAt).append("}")
            }
            is CollabMessage.JoinRoom -> buildString(210) {
                append("{\"type\":\"").append(CollabMessage.TYPE_JOIN_ROOM).append("\",")
                append("\"roomCode\":\"").append(escapeJson(message.roomCode)).append("\",")
                append("\"userTag\":\"").append(escapeJson(message.userTag)).append("\",")
                append("\"colorHex\":\"").append(escapeJson(message.colorHex)).append("\",")
                append("\"peerId\":\"").append(escapeJson(message.peerId)).append("\",")
                append("\"deviceId\":\"").append(escapeJson(message.deviceId)).append("\",")
                append("\"pronouns\":\"").append(escapeJson(message.pronouns)).append("\",")
                append("\"userHandle\":\"").append(escapeJson(message.userHandle)).append("\",")
                append("\"audioBitrateKbps\":").append(message.audioBitrateKbps).append(",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\"}")
            }
            is CollabMessage.JoinRejected -> buildString(160) {
                append("{\"type\":\"").append(CollabMessage.TYPE_JOIN_REJECTED).append("\",")
                append("\"roomCode\":\"").append(escapeJson(message.roomCode)).append("\",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\",")
                append("\"targetPeerId\":\"").append(escapeJson(message.targetPeerId)).append("\",")
                append("\"targetDeviceId\":\"").append(escapeJson(message.targetDeviceId)).append("\",")
                append("\"reason\":\"").append(escapeJson(message.reason)).append("\"}")
            }
            is CollabMessage.PeerJoined -> buildString(210) {
                append("{\"type\":\"").append(CollabMessage.TYPE_PEER_JOINED).append("\",")
                append("\"peerId\":\"").append(escapeJson(message.peerId)).append("\",")
                append("\"userTag\":\"").append(escapeJson(message.userTag)).append("\",")
                append("\"colorHex\":\"").append(escapeJson(message.colorHex)).append("\",")
                append("\"deviceId\":\"").append(escapeJson(message.deviceId)).append("\",")
                append("\"pronouns\":\"").append(escapeJson(message.pronouns)).append("\",")
                append("\"userHandle\":\"").append(escapeJson(message.userHandle)).append("\",")
                append("\"role\":\"").append(message.role.name).append("\",")
                append("\"audioBitrateKbps\":").append(message.audioBitrateKbps).append(",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\"}")
            }
            is CollabMessage.StrokeBroadcast -> buildString(320) {
                append("{\"type\":\"").append(CollabMessage.TYPE_STROKE_BROADCAST).append("\",")
                append("\"strokeId\":\"").append(escapeJson(message.strokeId)).append("\",")
                append("\"points\":[").append(message.points.joinToString(",")).append("],")
                append("\"colorHex\":\"").append(escapeJson(message.colorHex)).append("\",")
                append("\"strokeWidth\":").append(message.strokeWidth).append(",")
                append("\"peerId\":\"").append(escapeJson(message.peerId)).append("\",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\",")
                append("\"toolType\":\"").append(escapeJson(message.toolType)).append("\",")
                append("\"blendMode\":\"").append(escapeJson(message.blendMode)).append("\",")
                append("\"layerId\":\"").append(escapeJson(message.layerId)).append("\",")
                append("\"zIndex\":").append(message.zIndex).append("}")
            }
            is CollabMessage.TextElementSync -> buildString(360) {
                append("{\"type\":\"").append(CollabMessage.TYPE_TEXT_ELEMENT_SYNC).append("\",")
                append("\"elementId\":\"").append(escapeJson(message.elementId)).append("\",")
                append("\"layerId\":\"").append(escapeJson(message.layerId)).append("\",")
                append("\"text\":\"").append(escapeJson(message.text)).append("\",")
                append("\"x\":").append(message.x).append(",")
                append("\"y\":").append(message.y).append(",")
                append("\"width\":").append(message.width).append(",")
                append("\"height\":").append(message.height).append(",")
                append("\"fontSizeSp\":").append(message.fontSizeSp).append(",")
                append("\"color\":").append(message.color).append(",")
                append("\"fontWeight\":").append(message.fontWeight).append(",")
                append("\"backgroundStyle\":\"").append(escapeJson(message.backgroundStyle)).append("\",")
                append("\"alignment\":\"").append(escapeJson(message.alignment)).append("\",")
                append("\"rotationDegrees\":").append(message.rotationDegrees).append(",")
                append("\"zIndex\":").append(message.zIndex).append(",")
                append("\"peerId\":\"").append(escapeJson(message.peerId)).append("\",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\"}")
            }
            is CollabMessage.ImageElementSync -> buildString(512 + message.imageBase64.length) {
                append("{\"type\":\"").append(CollabMessage.TYPE_IMAGE_ELEMENT_SYNC).append("\",")
                append("\"elementId\":\"").append(escapeJson(message.elementId)).append("\",")
                append("\"layerId\":\"").append(escapeJson(message.layerId)).append("\",")
                append("\"assetId\":\"").append(escapeJson(message.assetId)).append("\",")
                append("\"x\":").append(message.x).append(",")
                append("\"y\":").append(message.y).append(",")
                append("\"width\":").append(message.width).append(",")
                append("\"height\":").append(message.height).append(",")
                append("\"scale\":").append(message.scale).append(",")
                append("\"rotationDegrees\":").append(message.rotationDegrees).append(",")
                append("\"zIndex\":").append(message.zIndex).append(",")
                append("\"imageBase64\":\"").append(escapeJson(message.imageBase64)).append("\",")
                append("\"chromaKeyEnabled\":").append(message.chromaKeyEnabled).append(",")
                append("\"keyColor\":").append(message.keyColor).append(",")
                append("\"tolerance\":").append(message.tolerance).append(",")
                append("\"edgeSoftening\":").append(message.edgeSoftening).append(",")
                append("\"brightness\":").append(message.brightness).append(",")
                append("\"contrast\":").append(message.contrast).append(",")
                append("\"saturation\":").append(message.saturation).append(",")
                append("\"temperature\":").append(message.temperature).append(",")
                append("\"redBalance\":").append(message.redBalance).append(",")
                append("\"greenBalance\":").append(message.greenBalance).append(",")
                append("\"blueBalance\":").append(message.blueBalance).append(",")
                append("\"peerId\":\"").append(escapeJson(message.peerId)).append("\",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\"}")
            }
            is CollabMessage.ElementsRemoveSync -> buildString(180) {
                append("{\"type\":\"").append(CollabMessage.TYPE_ELEMENTS_REMOVE_SYNC).append("\",")
                append("\"elementIds\":\"").append(escapeJson(message.elementIds.joinToString(","))).append("\",")
                append("\"peerId\":\"").append(escapeJson(message.peerId)).append("\",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\"}")
            }
            is CollabMessage.AudioStreamFrame -> buildString(180 + message.pcmDataBase64.length) {
                append("{\"type\":\"").append(CollabMessage.TYPE_AUDIO_STREAM_FRAME).append("\",")
                append("\"peerId\":\"").append(escapeJson(message.peerId)).append("\",")
                append("\"sequenceNumber\":").append(message.sequenceNumber).append(",")
                append("\"sampleRateHz\":").append(message.sampleRateHz).append(",")
                append("\"pcmDataBase64\":\"").append(escapeJson(message.pcmDataBase64)).append("\",")
                append("\"bitrateKbps\":").append(message.bitrateKbps).append(",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\"}")
            }
            is CollabMessage.CursorBroadcast -> buildString(128) {
                append("{\"type\":\"").append(CollabMessage.TYPE_CURSOR_BROADCAST).append("\",")
                append("\"peerId\":\"").append(escapeJson(message.peerId)).append("\",")
                append("\"x\":").append(message.x).append(",")
                append("\"y\":").append(message.y).append(",")
                append("\"timestamp\":").append(message.timestamp).append(",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\"}")
            }
            is CollabMessage.ChatBroadcast -> buildString(240) {
                append("{\"type\":\"").append(CollabMessage.TYPE_CHAT_BROADCAST).append("\",")
                append("\"messageId\":\"").append(escapeJson(message.messageId)).append("\",")
                append("\"peerId\":\"").append(escapeJson(message.peerId)).append("\",")
                append("\"senderTag\":\"").append(escapeJson(message.senderTag)).append("\",")
                append("\"senderColorHex\":\"").append(escapeJson(message.senderColorHex)).append("\",")
                append("\"text\":\"").append(escapeJson(message.text)).append("\",")
                append("\"timestamp\":").append(message.timestamp).append(",")
                append("\"senderPronouns\":\"").append(escapeJson(message.senderPronouns)).append("\",")
                append("\"senderRole\":\"").append(message.senderRole.name).append("\",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\"}")
            }
            is CollabMessage.PinChatMessage -> buildString(140) {
                append("{\"type\":\"").append(CollabMessage.TYPE_PIN_CHAT_MESSAGE).append("\",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\",")
                append("\"hostPeerId\":\"").append(escapeJson(message.hostPeerId)).append("\",")
                append("\"pinnedMessageId\":\"").append(escapeJson(message.pinnedMessageId ?: "")).append("\",")
                append("\"timestamp\":").append(message.timestamp).append("}")
            }
            is CollabMessage.DeleteChatMessage -> buildString(140) {
                append("{\"type\":\"").append(CollabMessage.TYPE_DELETE_CHAT_MESSAGE).append("\",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\",")
                append("\"actorPeerId\":\"").append(escapeJson(message.actorPeerId)).append("\",")
                append("\"messageId\":\"").append(escapeJson(message.messageId)).append("\",")
                append("\"timestamp\":").append(message.timestamp).append("}")
            }
            is CollabMessage.KickPeer -> buildString(180) {
                append("{\"type\":\"").append(CollabMessage.TYPE_KICK_PEER).append("\",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\",")
                append("\"hostPeerId\":\"").append(escapeJson(message.hostPeerId)).append("\",")
                append("\"targetPeerId\":\"").append(escapeJson(message.targetPeerId)).append("\",")
                append("\"targetDeviceId\":\"").append(escapeJson(message.targetDeviceId)).append("\",")
                append("\"targetUserName\":\"").append(escapeJson(message.targetUserName)).append("\",")
                append("\"reason\":\"").append(escapeJson(message.reason)).append("\"}")
            }
            is CollabMessage.SetPeerRole -> buildString(140) {
                append("{\"type\":\"").append(CollabMessage.TYPE_SET_PEER_ROLE).append("\",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\",")
                append("\"hostPeerId\":\"").append(escapeJson(message.hostPeerId)).append("\",")
                append("\"targetPeerId\":\"").append(escapeJson(message.targetPeerId)).append("\",")
                append("\"newRole\":\"").append(message.newRole.name).append("\"}")
            }
            is CollabMessage.TransferHostOwnership -> buildString(150) {
                append("{\"type\":\"").append(CollabMessage.TYPE_TRANSFER_HOST_OWNERSHIP).append("\",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\",")
                append("\"oldHostPeerId\":\"").append(escapeJson(message.oldHostPeerId)).append("\",")
                append("\"newHostPeerId\":\"").append(escapeJson(message.newHostPeerId)).append("\",")
                append("\"timestamp\":").append(message.timestamp).append("}")
            }
            is CollabMessage.ModMutePeer -> buildString(140) {
                append("{\"type\":\"").append(CollabMessage.TYPE_MOD_MUTE_PEER).append("\",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\",")
                append("\"actorPeerId\":\"").append(escapeJson(message.actorPeerId)).append("\",")
                append("\"targetPeerId\":\"").append(escapeJson(message.targetPeerId)).append("\",")
                append("\"isMuted\":").append(message.isMuted).append("}")
            }
            is CollabMessage.BlockPeerChat -> buildString(140) {
                append("{\"type\":\"").append(CollabMessage.TYPE_BLOCK_PEER_CHAT).append("\",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\",")
                append("\"actorPeerId\":\"").append(escapeJson(message.actorPeerId)).append("\",")
                append("\"targetPeerId\":\"").append(escapeJson(message.targetPeerId)).append("\",")
                append("\"isBlocked\":").append(message.isBlocked).append("}")
            }
            is CollabMessage.CanvasPermissionSync -> buildString(256) {
                append("{\"type\":\"").append(CollabMessage.TYPE_CANVAS_PERMISSION_SYNC).append("\",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\",")
                append("\"actorPeerId\":\"").append(escapeJson(message.actorPeerId)).append("\",")
                append("\"accessMode\":\"").append(message.accessMode.name).append("\",")
                append("\"isGlobalDrawingDisabled\":").append(message.isGlobalDrawingDisabled).append(",")
                append("\"isGlobalSheetCreationDisabled\":").append(message.isGlobalSheetCreationDisabled).append(",")
                append("\"drawingDisabledPeers\":\"").append(escapeJson(message.drawingDisabledPeerIds.joinToString(","))).append("\",")
                append("\"sheetDisabledPeers\":\"").append(escapeJson(message.sheetCreationDisabledPeerIds.joinToString(","))).append("\"}")
            }
            is CollabMessage.RoomSettingsSync -> buildString(240) {
                append("{\"type\":\"").append(CollabMessage.TYPE_ROOM_SETTINGS_SYNC).append("\",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\",")
                append("\"hostPeerId\":\"").append(escapeJson(message.hostPeerId)).append("\",")
                append("\"maxBitrateKbps\":").append(message.maxBitrateKbps).append(",")
                append("\"maxParticipants\":").append(message.maxParticipants).append(",")
                append("\"joinPolicy\":\"").append(message.joinPolicy.name).append("\",")
                append("\"allowedHandles\":\"").append(escapeJson(message.allowedUserHandles.joinToString(","))).append("\",")
                append("\"invitedHandles\":\"").append(escapeJson(message.invitedUserHandles.joinToString(","))).append("\"}")
            }
            is CollabMessage.RoomClosed -> buildString(140) {
                append("{\"type\":\"").append(CollabMessage.TYPE_ROOM_CLOSED).append("\",")
                append("\"roomCode\":\"").append(escapeJson(message.roomCode)).append("\",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\",")
                append("\"hostPeerId\":\"").append(escapeJson(message.hostPeerId)).append("\",")
                append("\"reason\":\"").append(escapeJson(message.reason)).append("\"}")
            }
            is CollabMessage.LeaveRoom -> buildString(96) {
                append("{\"type\":\"").append(CollabMessage.TYPE_LEAVE_ROOM).append("\",")
                append("\"peerId\":\"").append(escapeJson(message.peerId)).append("\",")
                append("\"sessionToken\":\"").append(escapeJson(message.sessionToken)).append("\"}")
            }
        }
    }

    fun decode(raw: String): CollabMessage? {
        if (raw.length > 2_500_000) return null
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null

        val type = extractStringField(trimmed, "type") ?: return null
        return when (type) {
            CollabMessage.TYPE_ROOM_PROBE -> {
                val roomCode = extractStringField(trimmed, "roomCode")?.take(16) ?: return null
                val requesterPeerId = extractStringField(trimmed, "requesterPeerId")?.take(100) ?: return null
                CollabMessage.RoomProbe(roomCode, requesterPeerId)
            }
            CollabMessage.TYPE_WHO_HAS_ROOM -> {
                val roomCode = extractStringField(trimmed, "roomCode")?.take(16) ?: return null
                val joinerPeerId = extractStringField(trimmed, "joinerPeerId")?.take(100) ?: return null
                val joinerDeviceId = extractStringField(trimmed, "joinerDeviceId")?.take(100) ?: joinerPeerId
                val joinerUserName = extractStringField(trimmed, "joinerUserName")?.take(50) ?: ""
                CollabMessage.WhoHasRoom(roomCode, joinerPeerId, joinerDeviceId, joinerUserName)
            }
            CollabMessage.TYPE_HOST_ANNOUNCE -> {
                val roomCode = extractStringField(trimmed, "roomCode")?.take(16) ?: return null
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                val hostPeerId = extractStringField(trimmed, "hostPeerId")?.take(100) ?: return null
                val hostUserName = extractStringField(trimmed, "hostUserName")?.take(50) ?: "Host"
                val lanIp = extractStringField(trimmed, "lanIp")?.take(64) ?: ""
                val lanPort = extractLongField(trimmed, "lanPort")?.toInt() ?: 0
                val maxBitrate = extractLongField(trimmed, "maxBitrateKbps")?.toInt() ?: CollabAudioBitrate.HIGH_CLARITY_64_KBPS
                val createdAt = extractLongField(trimmed, "createdAt") ?: System.currentTimeMillis()
                CollabMessage.HostAnnounce(
                    roomCode = roomCode,
                    sessionToken = sessionToken,
                    hostPeerId = hostPeerId,
                    hostUserName = hostUserName,
                    lanIp = lanIp,
                    lanPort = lanPort,
                    maxBitrateKbps = CollabAudioBitrate.sanitize(maxBitrate),
                    createdAt = createdAt
                )
            }
            CollabMessage.TYPE_JOIN_ROOM -> {
                val roomCode = extractStringField(trimmed, "roomCode")?.take(16) ?: return null
                val userTag = extractStringField(trimmed, "userTag")?.take(50) ?: ""
                val colorHex = extractStringField(trimmed, "colorHex")?.take(10) ?: "#7A5AF8"
                val peerId = extractStringField(trimmed, "peerId")?.take(100) ?: ""
                val deviceId = extractStringField(trimmed, "deviceId")?.take(100)?.ifEmpty { peerId } ?: peerId
                val pronouns = extractStringField(trimmed, "pronouns")?.take(40) ?: ""
                val userHandle = extractStringField(trimmed, "userHandle")?.take(40) ?: ""
                val bitrate = extractLongField(trimmed, "audioBitrateKbps")?.toInt() ?: CollabAudioBitrate.BALANCED_32_KBPS
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                CollabMessage.JoinRoom(
                    roomCode = roomCode,
                    userTag = userTag,
                    colorHex = colorHex,
                    peerId = peerId,
                    deviceId = deviceId,
                    pronouns = pronouns,
                    userHandle = userHandle,
                    audioBitrateKbps = CollabAudioBitrate.sanitize(bitrate),
                    sessionToken = sessionToken
                )
            }
            CollabMessage.TYPE_JOIN_REJECTED -> {
                val roomCode = extractStringField(trimmed, "roomCode")?.take(16) ?: return null
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                val targetPeerId = extractStringField(trimmed, "targetPeerId")?.take(100) ?: return null
                val targetDeviceId = extractStringField(trimmed, "targetDeviceId")?.take(100) ?: targetPeerId
                val reason = extractStringField(trimmed, "reason")?.take(100) ?: CollabMessage.JoinRejected.REASON_BANNED_BY_HOST
                CollabMessage.JoinRejected(roomCode, sessionToken, targetPeerId, targetDeviceId, reason)
            }
            CollabMessage.TYPE_PEER_JOINED -> {
                val peerId = extractStringField(trimmed, "peerId")?.take(100) ?: return null
                val userTag = extractStringField(trimmed, "userTag")?.take(50) ?: ""
                val colorHex = extractStringField(trimmed, "colorHex")?.take(10) ?: "#7A5AF8"
                val deviceId = extractStringField(trimmed, "deviceId")?.take(100)?.ifEmpty { peerId } ?: peerId
                val pronouns = extractStringField(trimmed, "pronouns")?.take(40) ?: ""
                val userHandle = extractStringField(trimmed, "userHandle")?.take(40) ?: ""
                val role = CollabRole.fromString(extractStringField(trimmed, "role"))
                val bitrate = extractLongField(trimmed, "audioBitrateKbps")?.toInt() ?: CollabAudioBitrate.BALANCED_32_KBPS
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                CollabMessage.PeerJoined(
                    peerId = peerId,
                    userTag = userTag,
                    colorHex = colorHex,
                    deviceId = deviceId,
                    pronouns = pronouns,
                    userHandle = userHandle,
                    role = role,
                    audioBitrateKbps = CollabAudioBitrate.sanitize(bitrate),
                    sessionToken = sessionToken
                )
            }
            CollabMessage.TYPE_STROKE_BROADCAST -> {
                val strokeId = extractStringField(trimmed, "strokeId")?.take(100) ?: UUID.randomUUID().toString()
                val points = extractFloatFieldList(trimmed, "points").take(10_000)
                val colorHex = extractStringField(trimmed, "colorHex")?.take(10) ?: "#7A5AF8"
                val rawStrokeWidth = extractFloatField(trimmed, "strokeWidth") ?: 4.0f
                val strokeWidth = if (rawStrokeWidth.isNaN()) 4.0f else rawStrokeWidth.coerceIn(0.1f, 200f)
                val peerId = extractStringField(trimmed, "peerId")?.take(100) ?: ""
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                val toolType = extractStringField(trimmed, "toolType")?.take(32) ?: "PEN"
                val blendMode = extractStringField(trimmed, "blendMode")?.take(32) ?: "NORMAL"
                val layerId = extractStringField(trimmed, "layerId")?.take(100) ?: "layer_default"
                val zIndex = extractLongField(trimmed, "zIndex")?.toInt() ?: 0
                CollabMessage.StrokeBroadcast(
                    strokeId = strokeId,
                    points = points,
                    colorHex = colorHex,
                    strokeWidth = strokeWidth,
                    peerId = peerId,
                    sessionToken = sessionToken,
                    toolType = toolType,
                    blendMode = blendMode,
                    layerId = layerId,
                    zIndex = zIndex
                )
            }
            CollabMessage.TYPE_TEXT_ELEMENT_SYNC -> {
                val elementId = extractStringField(trimmed, "elementId")?.take(100) ?: return null
                val layerId = extractStringField(trimmed, "layerId")?.take(100) ?: "layer_default"
                val text = extractStringField(trimmed, "text")?.take(2_000) ?: ""
                val x = (extractFloatField(trimmed, "x") ?: 0f).let { if (it.isNaN() || it.isInfinite()) 0f else it }
                val y = (extractFloatField(trimmed, "y") ?: 0f).let { if (it.isNaN() || it.isInfinite()) 0f else it }
                val width = (extractFloatField(trimmed, "width") ?: 260f).coerceIn(20f, 10_000f)
                val height = (extractFloatField(trimmed, "height") ?: 80f).coerceIn(20f, 10_000f)
                val fontSizeSp = (extractFloatField(trimmed, "fontSizeSp") ?: 18f).coerceIn(6f, 200f)
                val color = extractLongField(trimmed, "color") ?: 0xFFF8FAFC
                val fontWeight = (extractLongField(trimmed, "fontWeight")?.toInt() ?: 500).coerceIn(100, 900)
                val backgroundStyle = extractStringField(trimmed, "backgroundStyle")?.take(32) ?: "TRANSPARENT"
                val alignment = extractStringField(trimmed, "alignment")?.take(32) ?: "LEFT"
                val rotationDegrees = (extractFloatField(trimmed, "rotationDegrees") ?: 0f).let { if (it.isNaN() || it.isInfinite()) 0f else it }
                val zIndex = extractLongField(trimmed, "zIndex")?.toInt() ?: 0
                val peerId = extractStringField(trimmed, "peerId")?.take(100) ?: ""
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                CollabMessage.TextElementSync(
                    elementId = elementId,
                    layerId = layerId,
                    text = text,
                    x = x,
                    y = y,
                    width = width,
                    height = height,
                    fontSizeSp = fontSizeSp,
                    color = color,
                    fontWeight = fontWeight,
                    backgroundStyle = backgroundStyle,
                    alignment = alignment,
                    rotationDegrees = rotationDegrees,
                    zIndex = zIndex,
                    peerId = peerId,
                    sessionToken = sessionToken
                )
            }
            CollabMessage.TYPE_IMAGE_ELEMENT_SYNC -> {
                val elementId = extractStringField(trimmed, "elementId")?.take(100) ?: return null
                val layerId = extractStringField(trimmed, "layerId")?.take(100) ?: "layer_default"
                val assetId = extractStringField(trimmed, "assetId")?.take(100) ?: return null
                val x = (extractFloatField(trimmed, "x") ?: 0f).let { if (it.isNaN() || it.isInfinite()) 0f else it }
                val y = (extractFloatField(trimmed, "y") ?: 0f).let { if (it.isNaN() || it.isInfinite()) 0f else it }
                val width = (extractFloatField(trimmed, "width") ?: 200f).coerceIn(10f, 20_000f)
                val height = (extractFloatField(trimmed, "height") ?: 200f).coerceIn(10f, 20_000f)
                val scale = (extractFloatField(trimmed, "scale") ?: 1f).coerceIn(0.05f, 50f)
                val rotationDegrees = (extractFloatField(trimmed, "rotationDegrees") ?: 0f).let { if (it.isNaN() || it.isInfinite()) 0f else it }
                val zIndex = extractLongField(trimmed, "zIndex")?.toInt() ?: 0
                val imageBase64 = extractStringField(trimmed, "imageBase64")?.take(2_000_000) ?: ""
                val chromaKeyEnabled = extractBooleanField(trimmed, "chromaKeyEnabled") ?: false
                val keyColor = extractLongField(trimmed, "keyColor") ?: 0xFF00FF00
                val tolerance = (extractFloatField(trimmed, "tolerance") ?: 0.25f).coerceIn(0f, 1f)
                val edgeSoftening = (extractFloatField(trimmed, "edgeSoftening") ?: 0.08f).coerceIn(0f, 1f)
                val brightness = (extractFloatField(trimmed, "brightness") ?: 0f).coerceIn(-1f, 1f)
                val contrast = (extractFloatField(trimmed, "contrast") ?: 1f).coerceIn(0f, 4f)
                val saturation = (extractFloatField(trimmed, "saturation") ?: 1f).coerceIn(0f, 4f)
                val temperature = (extractFloatField(trimmed, "temperature") ?: 0f).coerceIn(-1f, 1f)
                val redBalance = (extractFloatField(trimmed, "redBalance") ?: 1f).coerceIn(0f, 4f)
                val greenBalance = (extractFloatField(trimmed, "greenBalance") ?: 1f).coerceIn(0f, 4f)
                val blueBalance = (extractFloatField(trimmed, "blueBalance") ?: 1f).coerceIn(0f, 4f)
                val peerId = extractStringField(trimmed, "peerId")?.take(100) ?: ""
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                CollabMessage.ImageElementSync(
                    elementId = elementId,
                    layerId = layerId,
                    assetId = assetId,
                    x = x,
                    y = y,
                    width = width,
                    height = height,
                    scale = scale,
                    rotationDegrees = rotationDegrees,
                    zIndex = zIndex,
                    imageBase64 = imageBase64,
                    chromaKeyEnabled = chromaKeyEnabled,
                    keyColor = keyColor,
                    tolerance = tolerance,
                    edgeSoftening = edgeSoftening,
                    brightness = brightness,
                    contrast = contrast,
                    saturation = saturation,
                    temperature = temperature,
                    redBalance = redBalance,
                    greenBalance = greenBalance,
                    blueBalance = blueBalance,
                    peerId = peerId,
                    sessionToken = sessionToken
                )
            }
            CollabMessage.TYPE_ELEMENTS_REMOVE_SYNC -> {
                val rawIds = extractStringField(trimmed, "elementIds") ?: ""
                val elementIds = rawIds.split(",")
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
                    .take(500)
                if (elementIds.isEmpty()) return null
                val peerId = extractStringField(trimmed, "peerId")?.take(100) ?: ""
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                CollabMessage.ElementsRemoveSync(
                    elementIds = elementIds,
                    peerId = peerId,
                    sessionToken = sessionToken
                )
            }
            CollabMessage.TYPE_AUDIO_STREAM_FRAME -> {
                val peerId = extractStringField(trimmed, "peerId")?.take(100) ?: return null
                val sequenceNumber = extractLongField(trimmed, "sequenceNumber") ?: 0L
                val sampleRateHz = (extractLongField(trimmed, "sampleRateHz")?.toInt() ?: 16000).coerceIn(8000, 48000)
                val pcmDataBase64 = extractStringField(trimmed, "pcmDataBase64")?.take(64_000) ?: return null
                if (pcmDataBase64.isEmpty()) return null
                val bitrateKbps = CollabAudioBitrate.sanitize(
                    extractLongField(trimmed, "bitrateKbps")?.toInt() ?: CollabAudioBitrate.BALANCED_32_KBPS
                )
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                CollabMessage.AudioStreamFrame(
                    peerId = peerId,
                    sequenceNumber = sequenceNumber,
                    sampleRateHz = sampleRateHz,
                    pcmDataBase64 = pcmDataBase64,
                    bitrateKbps = bitrateKbps,
                    sessionToken = sessionToken
                )
            }
            CollabMessage.TYPE_CURSOR_BROADCAST -> {
                val peerId = extractStringField(trimmed, "peerId")?.take(100) ?: return null
                val rawX = extractFloatField(trimmed, "x") ?: 0.0f
                val rawY = extractFloatField(trimmed, "y") ?: 0.0f
                val x = if (rawX.isNaN() || rawX.isInfinite()) 0.0f else rawX.coerceIn(-500_000f, 500_000f)
                val y = if (rawY.isNaN() || rawY.isInfinite()) 0.0f else rawY.coerceIn(-500_000f, 500_000f)
                val timestamp = extractLongField(trimmed, "timestamp") ?: System.currentTimeMillis()
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                CollabMessage.CursorBroadcast(peerId, x, y, timestamp, sessionToken)
            }
            CollabMessage.TYPE_CHAT_BROADCAST -> {
                val messageId = extractStringField(trimmed, "messageId")?.take(100) ?: UUID.randomUUID().toString()
                val peerId = extractStringField(trimmed, "peerId")?.take(100) ?: return null
                val senderTag = extractStringField(trimmed, "senderTag")?.take(50) ?: "Peer"
                val senderColorHex = extractStringField(trimmed, "senderColorHex")?.take(10) ?: "#7A5AF8"
                val text = extractStringField(trimmed, "text")?.take(500)?.trim() ?: return null
                if (text.isEmpty()) return null
                val timestamp = extractLongField(trimmed, "timestamp") ?: System.currentTimeMillis()
                val senderPronouns = extractStringField(trimmed, "senderPronouns")?.take(40) ?: ""
                val senderRole = CollabRole.fromString(extractStringField(trimmed, "senderRole"))
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                CollabMessage.ChatBroadcast(
                    messageId = messageId,
                    peerId = peerId,
                    senderTag = senderTag,
                    senderColorHex = senderColorHex,
                    text = text,
                    timestamp = timestamp,
                    senderPronouns = senderPronouns,
                    senderRole = senderRole,
                    sessionToken = sessionToken
                )
            }
            CollabMessage.TYPE_PIN_CHAT_MESSAGE -> {
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                val hostPeerId = extractStringField(trimmed, "hostPeerId")?.take(100) ?: return null
                val rawPinned = extractStringField(trimmed, "pinnedMessageId")?.take(100)
                val pinnedMessageId = if (rawPinned.isNullOrBlank()) null else rawPinned
                val timestamp = extractLongField(trimmed, "timestamp") ?: System.currentTimeMillis()
                CollabMessage.PinChatMessage(sessionToken, hostPeerId, pinnedMessageId, timestamp)
            }
            CollabMessage.TYPE_DELETE_CHAT_MESSAGE -> {
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                val actorPeerId = extractStringField(trimmed, "actorPeerId")?.take(100) ?: return null
                val messageId = extractStringField(trimmed, "messageId")?.take(100) ?: return null
                val timestamp = extractLongField(trimmed, "timestamp") ?: System.currentTimeMillis()
                CollabMessage.DeleteChatMessage(sessionToken, actorPeerId, messageId, timestamp)
            }
            CollabMessage.TYPE_KICK_PEER -> {
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                val hostPeerId = extractStringField(trimmed, "hostPeerId")?.take(100) ?: return null
                val targetPeerId = extractStringField(trimmed, "targetPeerId")?.take(100) ?: return null
                val targetDeviceId = extractStringField(trimmed, "targetDeviceId")?.take(100)?.ifEmpty { targetPeerId } ?: targetPeerId
                val targetUserName = extractStringField(trimmed, "targetUserName")?.take(50) ?: ""
                val reason = extractStringField(trimmed, "reason")?.take(100) ?: "KICKED_BY_HOST"
                CollabMessage.KickPeer(sessionToken, hostPeerId, targetPeerId, targetDeviceId, targetUserName, reason)
            }
            CollabMessage.TYPE_SET_PEER_ROLE -> {
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                val hostPeerId = extractStringField(trimmed, "hostPeerId")?.take(100) ?: return null
                val targetPeerId = extractStringField(trimmed, "targetPeerId")?.take(100) ?: return null
                val newRole = CollabRole.fromString(extractStringField(trimmed, "newRole"))
                CollabMessage.SetPeerRole(sessionToken, hostPeerId, targetPeerId, newRole)
            }
            CollabMessage.TYPE_TRANSFER_HOST_OWNERSHIP -> {
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                val oldHostPeerId = extractStringField(trimmed, "oldHostPeerId")?.take(100) ?: return null
                val newHostPeerId = extractStringField(trimmed, "newHostPeerId")?.take(100) ?: return null
                val timestamp = extractLongField(trimmed, "timestamp") ?: System.currentTimeMillis()
                CollabMessage.TransferHostOwnership(sessionToken, oldHostPeerId, newHostPeerId, timestamp)
            }
            CollabMessage.TYPE_MOD_MUTE_PEER -> {
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                val actorPeerId = extractStringField(trimmed, "actorPeerId")?.take(100) ?: return null
                val targetPeerId = extractStringField(trimmed, "targetPeerId")?.take(100) ?: return null
                val isMuted = extractBooleanField(trimmed, "isMuted") ?: false
                CollabMessage.ModMutePeer(sessionToken, actorPeerId, targetPeerId, isMuted)
            }
            CollabMessage.TYPE_BLOCK_PEER_CHAT -> {
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                val actorPeerId = extractStringField(trimmed, "actorPeerId")?.take(100) ?: return null
                val targetPeerId = extractStringField(trimmed, "targetPeerId")?.take(100) ?: return null
                val isBlocked = extractBooleanField(trimmed, "isBlocked") ?: false
                CollabMessage.BlockPeerChat(sessionToken, actorPeerId, targetPeerId, isBlocked)
            }
            CollabMessage.TYPE_CANVAS_PERMISSION_SYNC -> {
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                val actorPeerId = extractStringField(trimmed, "actorPeerId")?.take(100) ?: return null
                val accessMode = CollabAccessMode.fromString(extractStringField(trimmed, "accessMode"))
                val isGlobalDrawingDisabled = extractBooleanField(trimmed, "isGlobalDrawingDisabled") ?: false
                val isGlobalSheetCreationDisabled = extractBooleanField(trimmed, "isGlobalSheetCreationDisabled") ?: false
                val drawingDisabledPeers = extractStringField(trimmed, "drawingDisabledPeers")
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.toSet() ?: emptySet()
                val sheetDisabledPeers = extractStringField(trimmed, "sheetDisabledPeers")
                    ?.split(",")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    ?.toSet() ?: emptySet()
                CollabMessage.CanvasPermissionSync(
                    sessionToken = sessionToken,
                    actorPeerId = actorPeerId,
                    accessMode = accessMode,
                    isGlobalDrawingDisabled = isGlobalDrawingDisabled,
                    isGlobalSheetCreationDisabled = isGlobalSheetCreationDisabled,
                    drawingDisabledPeerIds = drawingDisabledPeers,
                    sheetCreationDisabledPeerIds = sheetDisabledPeers
                )
            }
            CollabMessage.TYPE_ROOM_SETTINGS_SYNC -> {
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                val hostPeerId = extractStringField(trimmed, "hostPeerId")?.take(100) ?: return null
                val maxBitrate = extractLongField(trimmed, "maxBitrateKbps")?.toInt() ?: CollabAudioBitrate.HIGH_CLARITY_64_KBPS
                val maxParticipants = extractLongField(trimmed, "maxParticipants")?.toInt()
                    ?.let { CollabRoomCapacity.sanitize(it) } ?: CollabRoomCapacity.DEFAULT_MAX_PARTICIPANTS
                val joinPolicy = CollabJoinPolicy.fromString(extractStringField(trimmed, "joinPolicy"))
                val allowedHandles = extractStringField(trimmed, "allowedHandles")
                    ?.split(",")
                    ?.map { CollabPrivacyPreset.normalizeHandle(it) }
                    ?.filter { it.length > 1 }
                    ?.toSet() ?: emptySet()
                val invitedHandles = extractStringField(trimmed, "invitedHandles")
                    ?.split(",")
                    ?.map { CollabPrivacyPreset.normalizeHandle(it) }
                    ?.filter { it.length > 1 }
                    ?.toSet() ?: emptySet()
                CollabMessage.RoomSettingsSync(
                    sessionToken = sessionToken,
                    hostPeerId = hostPeerId,
                    maxBitrateKbps = CollabAudioBitrate.sanitize(maxBitrate),
                    maxParticipants = maxParticipants,
                    joinPolicy = joinPolicy,
                    allowedUserHandles = allowedHandles,
                    invitedUserHandles = invitedHandles
                )
            }
            CollabMessage.TYPE_ROOM_CLOSED -> {
                val roomCode = extractStringField(trimmed, "roomCode")?.take(16) ?: ""
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                val hostPeerId = extractStringField(trimmed, "hostPeerId")?.take(100) ?: return null
                val reason = extractStringField(trimmed, "reason")?.take(100) ?: "HOST_LEFT"
                CollabMessage.RoomClosed(roomCode, sessionToken, hostPeerId, reason)
            }
            CollabMessage.TYPE_LEAVE_ROOM -> {
                val peerId = extractStringField(trimmed, "peerId")?.take(100) ?: return null
                val sessionToken = extractStringField(trimmed, "sessionToken")?.take(100) ?: ""
                CollabMessage.LeaveRoom(peerId, sessionToken)
            }
            else -> null
        }
    }

    private val stringRegexCache = ConcurrentHashMap<String, Regex>()
    private val floatRegexCache = ConcurrentHashMap<String, Regex>()
    private val longRegexCache = ConcurrentHashMap<String, Regex>()
    private val booleanRegexCache = ConcurrentHashMap<String, Regex>()
    private val floatListRegexCache = ConcurrentHashMap<String, Regex>()

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun unescapeJson(value: String): String {
        return value.replace("\\n", "\n")
            .replace("\\r", "\r")
            .replace("\\t", "\t")
            .replace("\\\"", "\"")
            .replace("\\\\", "\\")
    }

    private fun extractStringField(json: String, fieldName: String): String? {
        val pattern = stringRegexCache.getOrPut(fieldName) {
            Regex("\"$fieldName\"\\s*:\\s*\"((?:[^\"\\\\]|\\\\.)*)\"")
        }
        val rawMatch = pattern.find(json)?.groupValues?.get(1) ?: return null
        return unescapeJson(rawMatch)
    }

    private fun extractFloatField(json: String, fieldName: String): Float? {
        val pattern = floatRegexCache.getOrPut(fieldName) {
            Regex("\"$fieldName\"\\s*:\\s*([0-9.-]+)")
        }
        return pattern.find(json)?.groupValues?.get(1)?.toFloatOrNull()
    }

    private fun extractLongField(json: String, fieldName: String): Long? {
        val pattern = longRegexCache.getOrPut(fieldName) {
            Regex("\"$fieldName\"\\s*:\\s*([0-9-]+)")
        }
        return pattern.find(json)?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun extractBooleanField(json: String, fieldName: String): Boolean? {
        val pattern = booleanRegexCache.getOrPut(fieldName) {
            Regex("\"$fieldName\"\\s*:\\s*(true|false)", RegexOption.IGNORE_CASE)
        }
        return pattern.find(json)?.groupValues?.get(1)?.lowercase()?.toBooleanStrictOrNull()
    }

    private fun extractFloatFieldList(json: String, fieldName: String): List<Float> {
        val pattern = floatListRegexCache.getOrPut(fieldName) {
            Regex("\"$fieldName\"\\s*:\\s*\\[([^\\]]*)\\]")
        }
        val match = pattern.find(json)?.groupValues?.get(1) ?: return emptyList()
        if (match.isBlank()) return emptyList()
        val result = ArrayList<Float>(64)
        var start = 0
        val len = match.length
        while (start < len) {
            var end = match.indexOf(',', start)
            if (end == -1) end = len
            val token = match.substring(start, end).trim()
            if (token.isNotEmpty()) {
                token.toFloatOrNull()?.let { result.add(it) }
            }
            start = end + 1
        }
        return result
    }
}
