// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.schedule.model

import java.text.SimpleDateFormat
import java.util.Calendar
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

    fun isActiveForDate(targetDateMillis: Long): Boolean {
        if (isRepeatingWeekly) return true
        val c1 = Calendar.getInstance().apply { timeInMillis = dateMillis }
        val c2 = Calendar.getInstance().apply { timeInMillis = targetDateMillis }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
            c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    companion object {
        fun normalizeToStartOfDay(millis: Long): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = millis
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }
    }
}
