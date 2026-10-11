// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import com.arinara.fotara.data.model.DateRange
import com.arinara.fotara.data.model.SearchDateFilter
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

/**
 * Pure business logic class for computing calendar day and range boundaries.
 * Enforces strict midnight start (00:00:00.000) and end-of-day (23:59:59.999)
 * boundaries, locale-aware first day of week, leap years, and DST transitions.
 */
object DateRangeCalculator {

    fun getTodayRange(
        referenceMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): DateRange {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = referenceMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMs = cal.timeInMillis

        return DateRange(startMs, endMs)
    }

    fun getYesterdayRange(
        referenceMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): DateRange {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = referenceMs
            add(Calendar.DAY_OF_YEAR, -1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMs = cal.timeInMillis

        return DateRange(startMs, endMs)
    }

    fun getThisWeekRange(
        referenceMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.getDefault()
    ): DateRange {
        val cal = Calendar.getInstance(timeZone, locale).apply {
            timeInMillis = referenceMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK)
        val firstDay = cal.firstDayOfWeek
        val daysFromStart = (currentDayOfWeek - firstDay + 7) % 7
        cal.add(Calendar.DAY_OF_MONTH, -daysFromStart)
        val startMs = cal.timeInMillis

        cal.add(Calendar.DAY_OF_MONTH, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMs = cal.timeInMillis

        return DateRange(startMs, endMs)
    }

    fun getThisMonthRange(
        referenceMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): DateRange {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = referenceMs
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis

        val maxDay = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        cal.set(Calendar.DAY_OF_MONTH, maxDay)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMs = cal.timeInMillis

        return DateRange(startMs, endMs)
    }

    fun getThisYearRange(
        referenceMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): DateRange {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = referenceMs
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis

        cal.set(Calendar.MONTH, Calendar.DECEMBER)
        cal.set(Calendar.DAY_OF_MONTH, 31)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMs = cal.timeInMillis

        return DateRange(startMs, endMs)
    }

    fun getSingleDayRange(
        dayMs: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): DateRange {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = dayMs
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startMs = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endMs = cal.timeInMillis

        return DateRange(startMs, endMs)
    }

    fun getCustomRange(
        startDayMs: Long,
        endDayMs: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): DateRange {
        val calStart = Calendar.getInstance(timeZone).apply {
            timeInMillis = minOf(startDayMs, endDayMs)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val calEnd = Calendar.getInstance(timeZone).apply {
            timeInMillis = maxOf(startDayMs, endDayMs)
            set(Calendar.HOUR_OF_DAY, 23)
            set(Calendar.MINUTE, 59)
            set(Calendar.SECOND, 59)
            set(Calendar.MILLISECOND, 999)
        }

        return DateRange(calStart.timeInMillis, calEnd.timeInMillis)
    }

    fun getDayBoundaries(
        epochMs: Long,
        timeZone: TimeZone = TimeZone.getDefault()
    ): DateRange {
        return getSingleDayRange(epochMs, timeZone)
    }

    fun calculateRange(
        filter: SearchDateFilter,
        referenceMs: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault(),
        locale: Locale = Locale.getDefault(),
        customRange: DateRange? = null,
        singleDayEpochMs: Long? = null
    ): DateRange {
        return when (filter) {
            SearchDateFilter.ALL -> DateRange(0L, Long.MAX_VALUE)
            SearchDateFilter.TODAY -> getTodayRange(referenceMs, timeZone)
            SearchDateFilter.YESTERDAY -> getYesterdayRange(referenceMs, timeZone)
            SearchDateFilter.THIS_WEEK -> getThisWeekRange(referenceMs, timeZone, locale)
            SearchDateFilter.THIS_MONTH -> getThisMonthRange(referenceMs, timeZone)
            SearchDateFilter.THIS_YEAR -> getThisYearRange(referenceMs, timeZone)
            SearchDateFilter.SINGLE_DAY -> {
                val targetDay = singleDayEpochMs ?: customRange?.startMs ?: referenceMs
                getSingleDayRange(targetDay, timeZone)
            }
            SearchDateFilter.CUSTOM_RANGE -> {
                if (customRange != null) getCustomRange(customRange.startMs, customRange.endMs, timeZone)
                else DateRange(0L, Long.MAX_VALUE)
            }
        }
    }
}
