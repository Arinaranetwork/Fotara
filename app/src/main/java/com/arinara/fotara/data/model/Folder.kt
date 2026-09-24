// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

data class Folder(
    val id: Long = 0,
    val name: String,
    val colorLabel: String = TagColor.SKY.hex,
    val isPinned: Boolean = false,
    val photoCount: Int = 0,
    val totalSizeBytes: Long = 0L,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val isTrashed: Boolean = false,
    val deletedAt: Long? = null,
    val isLocked: Boolean = false,
    val lockPin: String? = null
) {
    val tagColor: TagColor get() = TagColor.fromHex(colorLabel)
}
