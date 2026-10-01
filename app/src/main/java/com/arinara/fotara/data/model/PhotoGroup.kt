// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

import com.arinara.fotara.util.ScheduleNoteType

data class PhotoGroup(
    override val id: Long = 0,
    override val folderId: Long,
    override val subfolderId: Long? = null,
    val name: String,
    val tagColor: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val addedAt: Long = createdAt,
    val coverPhotoId: Long? = null,
    val isTrashed: Boolean = false,
    val deletedAt: Long? = null,
    val linkedDeadline: Long? = null,
    override val scheduledAt: Long? = null,
    override val alertType: String? = null,
    override val scheduleTitle: String? = null
) : SchedulableNote {
    override val title: String get() = name
    override val noteType: ScheduleNoteType get() = ScheduleNoteType.PHOTO_GROUP
    val tag: TagColor? get() = tagColor?.let { TagColor.fromHex(it) }
}
