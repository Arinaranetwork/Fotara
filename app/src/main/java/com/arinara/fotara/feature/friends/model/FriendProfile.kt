// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.friends.model

/**
 * Universal academic study presence status.
 */
enum class StudyPresenceStatus {
    OFFLINE,
    STUDYING,
    IN_LECTURE,
    OPEN_TO_COLLAB;

    val displayLabel: String
        get() = when (this) {
            OFFLINE -> "Offline"
            STUDYING -> "Studying"
            IN_LECTURE -> "In Lecture"
            OPEN_TO_COLLAB -> "Open to Collab"
        }
}

/**
 * Profile of an academic peer or study contact.
 */
data class FriendProfile(
    val id: String,
    val handle: String,
    val displayName: String,
    val avatarUrl: String? = null,
    val studyStatus: StudyPresenceStatus = StudyPresenceStatus.OFFLINE,
    val currentSubject: String? = null,
    val joinedAt: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val followersCount: Int = 0,
    val followingCount: Int = 0
) {
    val formattedHandle: String
        get() = if (handle.startsWith("@")) handle else "@$handle"

    val isActive: Boolean
        get() = studyStatus != StudyPresenceStatus.OFFLINE

    /**
     * Converts to compact JSON string representation without external library dependencies.
     */
    fun toJson(): String {
        val sb = StringBuilder()
        sb.append("{")
        sb.append("\"id\":\"").append(escapeJson(id)).append("\",")
        sb.append("\"handle\":\"").append(escapeJson(handle)).append("\",")
        sb.append("\"displayName\":\"").append(escapeJson(displayName)).append("\",")
        if (avatarUrl != null) {
            sb.append("\"avatarUrl\":\"").append(escapeJson(avatarUrl)).append("\",")
        } else {
            sb.append("\"avatarUrl\":null,")
        }
        sb.append("\"studyStatus\":\"").append(studyStatus.name).append("\",")
        if (currentSubject != null) {
            sb.append("\"currentSubject\":\"").append(escapeJson(currentSubject)).append("\",")
        } else {
            sb.append("\"currentSubject\":null,")
        }
        sb.append("\"joinedAt\":").append(joinedAt).append(",")
        sb.append("\"isFavorite\":").append(isFavorite).append(",")
        sb.append("\"followersCount\":").append(followersCount).append(",")
        sb.append("\"followingCount\":").append(followingCount)
        sb.append("}")
        return sb.toString()
    }

    companion object {
        private fun escapeJson(input: String): String {
            return input
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t")
        }

        private fun unescapeJson(input: String): String {
            return input
                .replace("\\n", "\n")
                .replace("\\r", "\r")
                .replace("\\t", "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\")
        }

        fun fromJson(json: String): FriendProfile? {
            return try {
                val trimmed = json.trim()
                if (!trimmed.startsWith("{") || !trimmed.endsWith("}")) return null

                fun extractField(fieldName: String): String? {
                    val pattern = "\"$fieldName\"\\s*:\\s*(\"[^\"]*\"|null|true|false|\\d+)".toRegex()
                    val match = pattern.find(trimmed) ?: return null
                    val rawVal = match.groupValues[1]
                    return when {
                        rawVal == "null" -> null
                        rawVal.startsWith("\"") && rawVal.endsWith("\"") -> {
                            unescapeJson(rawVal.substring(1, rawVal.length - 1))
                        }
                        else -> rawVal
                    }
                }

                val id = extractField("id") ?: return null
                val handle = extractField("handle") ?: return null
                val displayName = extractField("displayName") ?: return null
                val avatarUrl = extractField("avatarUrl")
                val statusStr = extractField("studyStatus") ?: StudyPresenceStatus.OFFLINE.name
                val studyStatus = try {
                    StudyPresenceStatus.valueOf(statusStr)
                } catch (_: Exception) {
                    StudyPresenceStatus.OFFLINE
                }
                val currentSubject = extractField("currentSubject")
                val joinedAt = extractField("joinedAt")?.toLongOrNull() ?: System.currentTimeMillis()
                val isFavorite = extractField("isFavorite")?.toBooleanStrictOrNull() ?: false
                val followersCount = extractField("followersCount")?.toIntOrNull() ?: 0
                val followingCount = extractField("followingCount")?.toIntOrNull() ?: 0

                FriendProfile(
                    id = id,
                    handle = handle,
                    displayName = displayName,
                    avatarUrl = avatarUrl,
                    studyStatus = studyStatus,
                    currentSubject = currentSubject,
                    joinedAt = joinedAt,
                    isFavorite = isFavorite,
                    followersCount = followersCount,
                    followingCount = followingCount
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}
