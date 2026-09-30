// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

data class PhotoGroup(
    val id: Long = 0,
    val folderId: Long,
    val subfolderId: Long? = null,
    val name: String,
    val tagColor: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val addedAt: Long = createdAt,
    val coverPhotoId: Long? = null,
    val isTrashed: Boolean = false,
    val deletedAt: Long? = null,
    val linkedDeadline: Long? = null,
    val scheduledAt: Long? = null,
    val alertType: String? = null
) {
    val tag: TagColor? get() = tagColor?.let { TagColor.fromHex(it) }
}
