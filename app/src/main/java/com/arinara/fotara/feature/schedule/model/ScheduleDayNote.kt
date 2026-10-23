// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.schedule.model

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Persisted per-day note attached to a specific day in the academic schedule.
 *
 * @param id Unique SQLite primary key
 * @param dayOfWeek ISO-8601 day of week: 1 = Monday .. 7 = Sunday
 * @param dateMillis Calendar date epoch millisecond (start of day) for one-time notes
 * @param noteText User-written note content (e.g. "Bring lab coat")
 * @param isRepeatingWeekly Whether this note repeats every week on [dayOfWeek] or only applies to [dateMillis]
 * @param createdAt Creation epoch millisecond
 * @param updatedAt Last update epoch millisecond
 */
data class ScheduleDayNote(
    val id: Long = 0L,
    val dayOfWeek: Int,
    val dateMillis: Long,
    val noteText: String,
    val isRepeatingWeekly: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun formatDateLabel(): String {
        return if (isRepeatingWeekly) {
            "Repeats weekly"
        } else {
            val dateStr = SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(dateMillis))
            "One-time: $dateStr"
        }
    }
}
