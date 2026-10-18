// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

import com.arinara.fotara.util.ScheduleNoteType

enum class DocumentType {
    PDF,
    DOCX
}

data class DocumentNote(
    override val id: Long = 0L,
    override val folderId: Long,
    override val subfolderId: Long? = null,
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
    val pageCount: Int = 0,
    override val scheduledAt: Long? = null,
    override val alertType: String? = null,
    override val scheduleTitle: String? = null,
    override val isPinned: Boolean = false
) : SchedulableNote {
    override val title: String get() = name
    override val noteType: ScheduleNoteType get() = ScheduleNoteType.DOCUMENT
}
