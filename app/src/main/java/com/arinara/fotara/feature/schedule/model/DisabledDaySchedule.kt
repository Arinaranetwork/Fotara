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
 * Represents the disabled state of a specific day of the week in the class timetable.
 *
 * @param dayOfWeek ISO-8601 day of week: 1 = Monday .. 7 = Sunday
 * @param disabledUntilMs Epoch timestamp in milliseconds until which this day remains disabled
 * @param mode The selected duration mode string (ONLY_THIS_DAY, THIS_WEEK, ONE_MONTH, NEVER)
 * @param updatedAt Epoch timestamp in milliseconds when this record was last modified
 */
data class DisabledDaySchedule(
    val dayOfWeek: Int,
    val disabledUntilMs: Long,
    val mode: String,
    val updatedAt: Long = System.currentTimeMillis()
) {
    fun isExpired(nowMs: Long = System.currentTimeMillis()): Boolean =
        disabledUntilMs <= nowMs

    fun formatBadgeText(): String {
        return if (mode == "NEVER" || disabledUntilMs == Long.MAX_VALUE) {
            "Disabled indefinitely"
        } else {
            val dateStr = SimpleDateFormat("MMM d", Locale.US).format(Date(disabledUntilMs))
            "Disabled until $dateStr"
        }
    }

    fun formatStatusLabel(): String = formatBadgeText()
}
