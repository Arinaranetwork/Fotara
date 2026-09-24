// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.group

import com.arinara.fotara.data.model.Folder
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoGroup

data class GroupDetailUiState(
    val group: PhotoGroup? = null,
    val folder: Folder? = null,
    val photos: List<Photo> = emptyList(),
    val availableFolderPhotos: List<Photo> = emptyList(),
    val gridDensity: Int = 3,
    val highlightedPhotoId: Long? = null,
    val userMessage: String? = null
)
