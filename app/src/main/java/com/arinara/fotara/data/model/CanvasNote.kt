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
    val isTrashed: Boolean = false,
    val deletedAt: Long? = null
) : SchedulableNote {
    override val noteType: ScheduleNoteType get() = ScheduleNoteType.CANVAS_NOTE
    val tag: TagColor? get() = tagColor?.let { TagColor.fromHex(it) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CanvasNote
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
