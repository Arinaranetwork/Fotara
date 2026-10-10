// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.collab.model

import java.util.UUID

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

    data class JoinRoom(
        val roomCode: String,
        val userTag: String,
        val colorHex: String
    ) : CollabMessage() {
        override val messageType: String = TYPE_JOIN_ROOM
    }

    data class PeerJoined(
        val peerId: String,
        val userTag: String,
        val colorHex: String
    ) : CollabMessage() {
        override val messageType: String = TYPE_PEER_JOINED
    }

    data class StrokeBroadcast(
        val strokeId: String,
        val points: List<Float>,
        val colorHex: String,
        val strokeWidth: Float
    ) : CollabMessage() {
        override val messageType: String = TYPE_STROKE_BROADCAST
    }

    data class CursorBroadcast(
        val peerId: String,
        val x: Float,
        val y: Float,
        val timestamp: Long
    ) : CollabMessage() {
        override val messageType: String = TYPE_CURSOR_BROADCAST
    }

    data class LeaveRoom(
        val peerId: String
    ) : CollabMessage() {
        override val messageType: String = TYPE_LEAVE_ROOM
    }

    companion object {
        const val TYPE_JOIN_ROOM = "JOIN_ROOM"
        const val TYPE_PEER_JOINED = "PEER_JOINED"
        const val TYPE_STROKE_BROADCAST = "STROKE_BROADCAST"
        const val TYPE_CURSOR_BROADCAST = "CURSOR_BROADCAST"
        const val TYPE_LEAVE_ROOM = "LEAVE_ROOM"
    }
}

/**
 * Active participant in a collaborative study canvas session.
 */
data class CollabPeer(
    val peerId: String,
    val userTag: String,
    val colorHex: String,
    val lastCursorX: Float = 0f,
    val lastCursorY: Float = 0f,
    val lastActiveMs: Long = System.currentTimeMillis()
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
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Lightweight, dependency-free codec for serializing and deserializing CollabMessage frames.
 */
object CollabProtocolCodec {

    fun encode(message: CollabMessage): String {
        return when (message) {
            is CollabMessage.JoinRoom -> {
                buildString {
                    append("{\"type\":\"").append(CollabMessage.TYPE_JOIN_ROOM).append("\",")
                    append("\"roomCode\":\"").append(escapeJson(message.roomCode)).append("\",")
                    append("\"userTag\":\"").append(escapeJson(message.userTag)).append("\",")
                    append("\"colorHex\":\"").append(escapeJson(message.colorHex)).append("\"}")
                }
            }
            is CollabMessage.PeerJoined -> {
                buildString {
                    append("{\"type\":\"").append(CollabMessage.TYPE_PEER_JOINED).append("\",")
                    append("\"peerId\":\"").append(escapeJson(message.peerId)).append("\",")
                    append("\"userTag\":\"").append(escapeJson(message.userTag)).append("\",")
                    append("\"colorHex\":\"").append(escapeJson(message.colorHex)).append("\"}")
                }
            }
            is CollabMessage.StrokeBroadcast -> {
                buildString {
                    append("{\"type\":\"").append(CollabMessage.TYPE_STROKE_BROADCAST).append("\",")
                    append("\"strokeId\":\"").append(escapeJson(message.strokeId)).append("\",")
                    append("\"points\":[").append(message.points.joinToString(",")).append("],")
                    append("\"colorHex\":\"").append(escapeJson(message.colorHex)).append("\",")
                    append("\"strokeWidth\":").append(message.strokeWidth).append("}")
                }
            }
            is CollabMessage.CursorBroadcast -> {
                buildString {
                    append("{\"type\":\"").append(CollabMessage.TYPE_CURSOR_BROADCAST).append("\",")
                    append("\"peerId\":\"").append(escapeJson(message.peerId)).append("\",")
                    append("\"x\":").append(message.x).append(",")
                    append("\"y\":").append(message.y).append(",")
                    append("\"timestamp\":").append(message.timestamp).append("}")
                }
            }
            is CollabMessage.LeaveRoom -> {
                buildString {
                    append("{\"type\":\"").append(CollabMessage.TYPE_LEAVE_ROOM).append("\",")
                    append("\"peerId\":\"").append(escapeJson(message.peerId)).append("\"}")
                }
            }
        }
    }

    fun decode(raw: String): CollabMessage? {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null

        val type = extractStringField(trimmed, "type") ?: return null
        return when (type) {
            CollabMessage.TYPE_JOIN_ROOM -> {
                val roomCode = extractStringField(trimmed, "roomCode") ?: return null
                val userTag = extractStringField(trimmed, "userTag") ?: ""
                val colorHex = extractStringField(trimmed, "colorHex") ?: "#2563EB"
                CollabMessage.JoinRoom(roomCode, userTag, colorHex)
            }
            CollabMessage.TYPE_PEER_JOINED -> {
                val peerId = extractStringField(trimmed, "peerId") ?: return null
                val userTag = extractStringField(trimmed, "userTag") ?: ""
                val colorHex = extractStringField(trimmed, "colorHex") ?: "#2563EB"
                CollabMessage.PeerJoined(peerId, userTag, colorHex)
            }
            CollabMessage.TYPE_STROKE_BROADCAST -> {
                val strokeId = extractStringField(trimmed, "strokeId") ?: UUID.randomUUID().toString()
                val points = extractFloatFieldList(trimmed, "points")
                val colorHex = extractStringField(trimmed, "colorHex") ?: "#2563EB"
                val strokeWidth = extractFloatField(trimmed, "strokeWidth") ?: 4.0f
                CollabMessage.StrokeBroadcast(strokeId, points, colorHex, strokeWidth)
            }
            CollabMessage.TYPE_CURSOR_BROADCAST -> {
                val peerId = extractStringField(trimmed, "peerId") ?: return null
                val x = extractFloatField(trimmed, "x") ?: 0.0f
                val y = extractFloatField(trimmed, "y") ?: 0.0f
                val timestamp = extractLongField(trimmed, "timestamp") ?: System.currentTimeMillis()
                CollabMessage.CursorBroadcast(peerId, x, y, timestamp)
            }
            CollabMessage.TYPE_LEAVE_ROOM -> {
                val peerId = extractStringField(trimmed, "peerId") ?: return null
                CollabMessage.LeaveRoom(peerId)
            }
            else -> null
        }
    }

    private fun escapeJson(value: String): String {
        return value.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t")
    }

    private fun extractStringField(json: String, fieldName: String): String? {
        val pattern = Regex("\"$fieldName\"\\s*:\\s*\"([^\"]*)\"")
        return pattern.find(json)?.groupValues?.get(1)
    }

    private fun extractFloatField(json: String, fieldName: String): Float? {
        val pattern = Regex("\"$fieldName\"\\s*:\\s*([0-9.-]+)")
        return pattern.find(json)?.groupValues?.get(1)?.toFloatOrNull()
    }

    private fun extractLongField(json: String, fieldName: String): Long? {
        val pattern = Regex("\"$fieldName\"\\s*:\\s*([0-9-]+)")
        return pattern.find(json)?.groupValues?.get(1)?.toLongOrNull()
    }

    private fun extractFloatFieldList(json: String, fieldName: String): List<Float> {
        val pattern = Regex("\"$fieldName\"\\s*:\\s*\\[([^\\]]*)\\]")
        val match = pattern.find(json)?.groupValues?.get(1) ?: return emptyList()
        if (match.isBlank()) return emptyList()
        return match.split(",").mapNotNull { it.trim().toFloatOrNull() }
    }
}
