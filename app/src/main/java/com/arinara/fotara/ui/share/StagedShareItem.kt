// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.share

import java.io.File

enum class StagedItemType {
    IMAGE,
    PDF,
    DOCX,
    TEXT
}

data class StagedShareItem(
    val id: String,
    val displayName: String,
    val itemType: StagedItemType,
    val mimeType: String,
    val stagedFile: File,
    val fileSizeBytes: Long,
    val textBody: String? = null,
    val thumbnailPath: String? = null,
    val isSelected: Boolean = true
)
