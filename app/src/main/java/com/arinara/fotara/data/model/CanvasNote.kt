// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

data class CanvasNote(
    val id: Long = 0,
    val folderId: Long,
    val subfolderId: Long? = null,
    val title: String,
    val dataBlob: ByteArray? = null,
    val thumbnailPath: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val addedAt: Long = createdAt,
    val tagColor: String? = null,
    val linkedDeadline: Long? = null,
    val scheduledAt: Long? = null,
    val alertType: String? = null,
    val isTrashed: Boolean = false,
    val deletedAt: Long? = null
) {
    val tag: TagColor? get() = tagColor?.let { TagColor.fromHex(it) }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as CanvasNote
        return id == other.id
    }

    override fun hashCode(): Int = id.hashCode()
}
