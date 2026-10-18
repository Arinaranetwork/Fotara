// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

import java.util.UUID

data class Space(
    val id: Long = 0L,
    val uuid: String = UUID.randomUUID().toString(),
    val name: String,
    val iconKey: String = "school",
    val colorHex: String = "#2563EB",
    val isPrivate: Boolean = false,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val DEFAULT_SPACE_ID = 1L
        const val DEFAULT_SPACE_UUID = "00000000-0000-4000-8000-000000000101"
        
        val DEFAULT_SPACE = Space(
            id = DEFAULT_SPACE_ID,
            uuid = DEFAULT_SPACE_UUID,
            name = "Default Space",
            iconKey = "school",
            colorHex = "#2563EB",
            isPrivate = false,
            sortOrder = 0
        )
    }
}
