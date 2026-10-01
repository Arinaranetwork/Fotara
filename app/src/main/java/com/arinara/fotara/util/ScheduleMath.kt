// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class ScheduleValidationResult {
    VALID,
    PAST_TIME,
    TOO_FAR_IN_FUTURE
}

/**
 * Pure Kotlin calendar and schedule computation engine.
 * Computes exact epoch milliseconds, snooze offsets, formatted badge strings,
 * and past detection across time zones without Android framework dependencies.
 */
object ScheduleMath {

    const val DEFAULT_SNOOZE_MINUTES = 10
    private const val MAX_FUTURE_YEARS = 5

    fun isPast(triggerAtMs: Long, currentMs: Long = System.currentTimeMillis()): Boolean {
        return triggerAtMs <= currentMs
    }

    fun computeSnoozeTime(
        currentMs: Long = System.currentTimeMillis(),
        snoozeDurationMinutes: Int = DEFAULT_SNOOZE_MINUTES
    ): Long {
        return currentMs + (snoozeDurationMinutes.coerceAtLeast(1) * 60 * 1000L)
    }

    fun validateScheduleTime(
        targetMs: Long,
        currentMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): ScheduleValidationResult {
        if (targetMs <= currentMs) {
            return ScheduleValidationResult.PAST_TIME
        }
        val maxFutureCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = currentMs
            add(Calendar.YEAR, MAX_FUTURE_YEARS)
        }
        if (targetMs > maxFutureCal.timeInMillis) {
            return ScheduleValidationResult.TOO_FAR_IN_FUTURE
        }
        return ScheduleValidationResult.VALID
    }

    fun calculateEpochMs(
        year: Int,
        month: Int,
        dayOfMonth: Int,
        hourOfDay: Int,
        minute: Int,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, dayOfMonth)
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        return cal.timeInMillis
    }

    fun formatScheduleDateTime(
        triggerAtMs: Long,
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.US
    ): String {
        val sdf = SimpleDateFormat("MMM d, yyyy · h:mm a", locale).apply {
            this.timeZone = timeZone
        }
        return sdf.format(Date(triggerAtMs))
    }

    fun formatShortScheduleBadge(
        triggerAtMs: Long,
        currentMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.US
    ): String {
        val targetCal = Calendar.getInstance(timeZone).apply { timeInMillis = triggerAtMs }
        val nowCal = Calendar.getInstance(timeZone).apply { timeInMillis = currentMs }

        val timeFormat = SimpleDateFormat("h:mm a", locale).apply { this.timeZone = timeZone }
        val timeString = timeFormat.format(Date(triggerAtMs))

        val isSameDay = targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                targetCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)

        if (isSameDay) {
            return "Today, $timeString"
        }

        val tomorrowCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = currentMs
            add(Calendar.DAY_OF_YEAR, 1)
        }
        val isTomorrow = targetCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
                targetCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)

        if (isTomorrow) {
            return "Tomorrow, $timeString"
        }

        val dateFormat = SimpleDateFormat("MMM d, h:mm a", locale).apply { this.timeZone = timeZone }
        return dateFormat.format(Date(triggerAtMs))
    }

    fun isDueTomorrow(
        triggerAtMs: Long,
        currentMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        val targetCal = Calendar.getInstance(timeZone).apply { timeInMillis = triggerAtMs }
        val tomorrowCal = Calendar.getInstance(timeZone).apply {
            timeInMillis = currentMs
            add(Calendar.DAY_OF_YEAR, 1)
        }
        return targetCal.get(Calendar.YEAR) == tomorrowCal.get(Calendar.YEAR) &&
                targetCal.get(Calendar.DAY_OF_YEAR) == tomorrowCal.get(Calendar.DAY_OF_YEAR)
    }

    fun isDueToday(
        triggerAtMs: Long,
        currentMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): Boolean {
        val targetCal = Calendar.getInstance(timeZone).apply { timeInMillis = triggerAtMs }
        val nowCal = Calendar.getInstance(timeZone).apply { timeInMillis = currentMs }
        return targetCal.get(Calendar.YEAR) == nowCal.get(Calendar.YEAR) &&
                targetCal.get(Calendar.DAY_OF_YEAR) == nowCal.get(Calendar.DAY_OF_YEAR)
    }
}
