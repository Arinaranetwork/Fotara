// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.audio.model

/**
 * Data model for voice and lecture audio annotations linked to notes or PDF document pages.
 */
data class AudioAnnotation(
    val id: Long = 0L,
    val noteId: Long? = null,
    val pdfDocId: Long? = null,
    val pdfPageIndex: Int? = null,
    val filePath: String,
    val durationMs: Long,
    val createdAt: Long = System.currentTimeMillis()
)
