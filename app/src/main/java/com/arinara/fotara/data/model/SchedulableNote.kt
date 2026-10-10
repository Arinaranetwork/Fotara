// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

import com.arinara.fotara.util.ScheduleNoteType

/**
 * Universal interface for all note types supporting scheduling, reminders, and alarm alerts.
 * Implemented by Photos, Groups, Document Notes, Text Notes, and Canvas Notes.
 */
interface SchedulableNote {
    val id: Long
    val folderId: Long
    val subfolderId: Long?
    val title: String
    val scheduledAt: Long?
    val alertType: String?
    val scheduleTitle: String?
    val noteType: ScheduleNoteType
    val isPinned: Boolean get() = false

    val hasActiveSchedule: Boolean
        get() = scheduledAt != null && scheduledAt!! > System.currentTimeMillis()

    val displayScheduleTitle: String
        get() = scheduleTitle?.ifBlank { null } ?: title
}
