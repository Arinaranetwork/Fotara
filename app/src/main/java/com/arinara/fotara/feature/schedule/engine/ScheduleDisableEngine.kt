// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.schedule.engine

import com.arinara.fotara.feature.schedule.model.DisabledDaySchedule
import java.util.Calendar

enum class DisableDuration(val label: String) {
    ONLY_THIS_DAY("Only this day"),
    THIS_WEEK("This week"),
    ONE_MONTH("1 month"),
    NEVER("Never, until I manually change it")
}

object ScheduleDisableEngine {

    /**
     * Computes the exact expiration timestamp in epoch milliseconds for a given duration.
     *
     * @param duration The chosen duration mode
     * @param targetDayOfWeek The ISO-8601 day of week being disabled (1 = Mon .. 7 = Sun)
     * @param nowCalendar The current calendar instant
     * @param cutoffMinute Minutes from midnight defining next-day rollover (default 18:00 = 1080)
     */
    fun calculateExpiry(
        duration: DisableDuration,
        targetDayOfWeek: Int,
        nowCalendar: Calendar = Calendar.getInstance(),
        cutoffMinute: Int = 18 * 60
    ): Long {
        return when (duration) {
            DisableDuration.NEVER -> Long.MAX_VALUE

            DisableDuration.ONE_MONTH -> {
                // Exactly 30 calendar days from the current instant
                nowCalendar.timeInMillis + (30L * 24 * 60 * 60 * 1000L)
            }

            DisableDuration.THIS_WEEK -> {
                // Expires at the end of the current week (Sunday 23:59:59.999)
                val cal = nowCalendar.clone() as Calendar
                val currentDayIso = ScheduleCutoffEngine.getCurrentDayOfWeek(cal)
                val daysUntilSunday = (7 - currentDayIso).coerceAtLeast(0)

                cal.add(Calendar.DAY_OF_YEAR, daysUntilSunday)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                cal.timeInMillis
            }

            DisableDuration.ONLY_THIS_DAY -> {
                val cal = nowCalendar.clone() as Calendar
                val currentDayIso = ScheduleCutoffEngine.getCurrentDayOfWeek(cal)
                val currentMinute = cal.get(Calendar.HOUR_OF_DAY) * 60 + cal.get(Calendar.MINUTE)

                val daysOffset: Int
                if (targetDayOfWeek == currentDayIso) {
                    // Same day: if current time is past rollover cutoff, "this day" has already rolled over
                    // to tomorrow, so the next occurrence of this day is 7 days from now.
                    daysOffset = if (currentMinute >= cutoffMinute) 7 else 0
                } else {
                    // Future day in the week
                    daysOffset = (targetDayOfWeek - currentDayIso + 7) % 7
                }

                cal.add(Calendar.DAY_OF_YEAR, daysOffset)
                // Expires at the end of that day (23:59:59.999)
                cal.set(Calendar.HOUR_OF_DAY, 23)
                cal.set(Calendar.MINUTE, 59)
                cal.set(Calendar.SECOND, 59)
                cal.set(Calendar.MILLISECOND, 999)
                cal.timeInMillis
            }
        }
    }

    /**
     * Checks if a day is currently disabled, considering expiration against [nowMs].
     */
    fun isDayDisabled(
        record: DisabledDaySchedule?,
        nowMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (record == null) return false
        return !record.isExpired(nowMs)
    }
}
