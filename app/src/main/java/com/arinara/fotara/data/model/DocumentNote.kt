// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

enum class DocumentType {
    PDF,
    DOCX
}

data class DocumentNote(
    val id: Long = 0L,
    val folderId: Long,
    val subfolderId: Long? = null,
    val name: String,
    val docType: DocumentType,
    val originFileUri: String,
    val extractedText: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val addedAt: Long = System.currentTimeMillis(),
    val tagColor: String? = null,
    val linkedDeadline: Long? = null,
    val isTrashed: Boolean = false,
    val deletedAt: Long? = null,
    val pageCount: Int = 0
)
