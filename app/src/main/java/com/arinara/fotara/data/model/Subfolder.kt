// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

data class Subfolder(
    val id: Long = 0,
    val folderId: Long,
    val name: String,
    val colorLabel: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val isTrashed: Boolean = false,
    val deletedAt: Long? = null
)
