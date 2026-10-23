// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

import com.arinara.fotara.util.ScheduleNoteType

data class CanvasNote(
    override val id: Long = 0,
    override val folderId: Long,
    override val subfolderId: Long? = null,
    override val title: String,
    val dataBlob: ByteArray? = null,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val addedAt: Long = createdAt,
    val tagColor: String? = null,
    val linkedDeadline: Long? = null,
    override val scheduledAt: Long? = null,
    override val alertType: String? = null,
    override val scheduleTitle: String? = null,
    override val isPinned: Boolean = false,
    val groupId: Long? = null,
    val isTrashed: Boolean = false,
    val deletedAt: Long? = null
) : SchedulableNote {
    override val noteType: ScheduleNoteType get() = ScheduleNoteType.CANVAS_NOTE
    val tag: TagColor? get() = tagColor?.let { TagColor.fromHex(it) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CanvasNote
        return id == other.id &&
            folderId == other.folderId &&
            subfolderId == other.subfolderId &&
            title == other.title &&
            thumbnailPath == other.thumbnailPath &&
            createdAt == other.createdAt &&
            updatedAt == other.updatedAt &&
            addedAt == other.addedAt &&
            tagColor == other.tagColor &&
            linkedDeadline == other.linkedDeadline &&
            scheduledAt == other.scheduledAt &&
            alertType == other.alertType &&
            scheduleTitle == other.scheduleTitle &&
            isPinned == other.isPinned &&
            groupId == other.groupId &&
            isTrashed == other.isTrashed &&
            deletedAt == other.deletedAt &&
            (dataBlob?.contentEquals(other.dataBlob) ?: (other.dataBlob == null))
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + folderId.hashCode()
        result = 31 * result + (subfolderId?.hashCode() ?: 0)
        result = 31 * result + title.hashCode()
        result = 31 * result + (dataBlob?.contentHashCode() ?: 0)
        result = 31 * result + (thumbnailPath?.hashCode() ?: 0)
        result = 31 * result + updatedAt.hashCode()
        result = 31 * result + (tagColor?.hashCode() ?: 0)
        result = 31 * result + (linkedDeadline?.hashCode() ?: 0)
        result = 31 * result + (scheduledAt?.hashCode() ?: 0)
        result = 31 * result + isPinned.hashCode()
        result = 31 * result + (groupId?.hashCode() ?: 0)
        result = 31 * result + isTrashed.hashCode()
        return result
    }
}
