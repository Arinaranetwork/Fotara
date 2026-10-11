// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

enum class DestinationType {
    FOLDER,
    SUBFOLDER,
    GROUP
}

data class RecentDestination(
    val type: DestinationType,
    val folderId: Long,
    val subfolderId: Long? = null,
    val groupId: Long? = null,
    val title: String,
    val subtitle: String? = null,
    val timestamp: Long = System.currentTimeMillis()
)
