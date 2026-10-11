// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

data class DocumentPage(
    val id: Long = 0L,
    val documentNoteId: Long,
    val pageIndex: Int,
    val imageUri: String,
    val ocrText: String? = null
)
