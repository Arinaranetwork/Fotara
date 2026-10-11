// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import com.arinara.fotara.data.model.DateRange
import com.arinara.fotara.data.model.SearchDateFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone

class DateRangeCalculatorTest {

    private val utcZone = TimeZone.getTimeZone("UTC")
    private val nyZone = TimeZone.getTimeZone("America/New_York")
    private val londonZone = TimeZone.getTimeZone("Europe/London")

    @Test
    fun testDayBoundariesAtMidnight() {
        val cal = Calendar.getInstance(utcZone, Locale.US).apply {
            set(2026, Calendar.OCTOBER, 15, 14, 30, 45)
            set(Calendar.MILLISECOND, 500)
        }
        val epochMs = cal.timeInMillis

        val (startMs, endMs) = DateRangeCalculator.getDayBoundaries(epochMs, utcZone)

        val startCal = Calendar.getInstance(utcZone, Locale.US).apply { timeInMillis = startMs }
        assertEquals(2026, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, startCal.get(Calendar.MONTH))
        assertEquals(15, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))
        assertEquals(0, startCal.get(Calendar.SECOND))
        assertEquals(0, startCal.get(Calendar.MILLISECOND))

        val endCal = Calendar.getInstance(utcZone, Locale.US).apply { timeInMillis = endMs }
        assertEquals(2026, endCal.get(Calendar.YEAR))
        assertEquals(Calendar.OCTOBER, endCal.get(Calendar.MONTH))
        assertEquals(15, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(59, endCal.get(Calendar.SECOND))
        assertEquals(999, endCal.get(Calendar.MILLISECOND))

        assertEquals(86400000L - 1L, endMs - startMs)
    }

    @Test
    fun testWeekStart_SundayFirstVsMondayFirst() {
        // Wednesday, October 14, 2026
        val cal = Calendar.getInstance(utcZone, Locale.US).apply {
            set(2026, Calendar.OCTOBER, 14, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMs = cal.timeInMillis

        // US locale: Sunday first (Sunday, Oct 11, 2026)
        val usRange = DateRangeCalculator.getThisWeekRange(nowMs, utcZone, Locale.US)
        val usStartCal = Calendar.getInstance(utcZone, Locale.US).apply { timeInMillis = usRange.startMs }
        assertEquals(Calendar.SUNDAY, usStartCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(11, usStartCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, usStartCal.get(Calendar.HOUR_OF_DAY))

        val usEndCal = Calendar.getInstance(utcZone, Locale.US).apply { timeInMillis = usRange.endMs }
        assertEquals(Calendar.SATURDAY, usEndCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(17, usEndCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, usEndCal.get(Calendar.HOUR_OF_DAY))

        // UK locale: Monday first (Monday, Oct 12, 2026)
        val ukRange = DateRangeCalculator.getThisWeekRange(nowMs, utcZone, Locale.UK)
        val ukStartCal = Calendar.getInstance(utcZone, Locale.UK).apply { timeInMillis = ukRange.startMs }
        assertEquals(Calendar.MONDAY, ukStartCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(12, ukStartCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, ukStartCal.get(Calendar.HOUR_OF_DAY))

        val ukEndCal = Calendar.getInstance(utcZone, Locale.UK).apply { timeInMillis = ukRange.endMs }
        assertEquals(Calendar.SUNDAY, ukEndCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(18, ukEndCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, ukEndCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testWeekStart_OnExactFirstDay() {
        // Sunday, Oct 11, 2026 (Sunday-first locale)
        val calSun = Calendar.getInstance(utcZone, Locale.US).apply {
            set(2026, Calendar.OCTOBER, 11, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val rangeSun = DateRangeCalculator.getThisWeekRange(calSun.timeInMillis, utcZone, Locale.US)
        val startCalSun = Calendar.getInstance(utcZone, Locale.US).apply { timeInMillis = rangeSun.startMs }
        assertEquals(11, startCalSun.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCalSun.get(Calendar.HOUR_OF_DAY))

        // Monday, Oct 12, 2026 (Monday-first locale)
        val calMon = Calendar.getInstance(utcZone, Locale.UK).apply {
            set(2026, Calendar.OCTOBER, 12, 10, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val rangeMon = DateRangeCalculator.getThisWeekRange(calMon.timeInMillis, utcZone, Locale.UK)
        val startCalMon = Calendar.getInstance(utcZone, Locale.UK).apply { timeInMillis = rangeMon.startMs }
        assertEquals(12, startCalMon.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCalMon.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testMonthEdges_AndLeapYear() {
        // Leap year: February 2024 has 29 days
        val leapCal = Calendar.getInstance(utcZone, Locale.US).apply {
            set(2024, Calendar.FEBRUARY, 15, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val leapRange = DateRangeCalculator.getThisMonthRange(leapCal.timeInMillis, utcZone)

        val leapStartCal = Calendar.getInstance(utcZone, Locale.US).apply { timeInMillis = leapRange.startMs }
        assertEquals(1, leapStartCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, leapStartCal.get(Calendar.HOUR_OF_DAY))

        val leapEndCal = Calendar.getInstance(utcZone, Locale.US).apply { timeInMillis = leapRange.endMs }
        assertEquals(29, leapEndCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, leapEndCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, leapEndCal.get(Calendar.MINUTE))

        // Non-leap year: February 2025 has 28 days
        val nonLeapCal = Calendar.getInstance(utcZone, Locale.US).apply {
            set(2025, Calendar.FEBRUARY, 10, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nonLeapRange = DateRangeCalculator.getThisMonthRange(nonLeapCal.timeInMillis, utcZone)
        val nonLeapEndCal = Calendar.getInstance(utcZone, Locale.US).apply { timeInMillis = nonLeapRange.endMs }
        assertEquals(28, nonLeapEndCal.get(Calendar.DAY_OF_MONTH))
    }

    @Test
    fun testYearEdges() {
        val cal = Calendar.getInstance(utcZone, Locale.US).apply {
            set(2026, Calendar.JUNE, 15, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val range = DateRangeCalculator.getThisYearRange(cal.timeInMillis, utcZone)

        val startCal = Calendar.getInstance(utcZone, Locale.US).apply { timeInMillis = range.startMs }
        assertEquals(2026, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.JANUARY, startCal.get(Calendar.MONTH))
        assertEquals(1, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))

        val endCal = Calendar.getInstance(utcZone, Locale.US).apply { timeInMillis = range.endMs }
        assertEquals(2026, endCal.get(Calendar.YEAR))
        assertEquals(Calendar.DECEMBER, endCal.get(Calendar.MONTH))
        assertEquals(31, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))
        assertEquals(59, endCal.get(Calendar.SECOND))
        assertEquals(999, endCal.get(Calendar.MILLISECOND))
    }

    @Test
    fun testDaylightSavingTransitions() {
        // US Daylight Saving Time transition in 2026: March 8 (Spring forward)
        // Midday March 8, 2026 in New York
        val dstCal = Calendar.getInstance(nyZone, Locale.US).apply {
            set(2026, Calendar.MARCH, 8, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val (startMs, endMs) = DateRangeCalculator.getDayBoundaries(dstCal.timeInMillis, nyZone)

        val startCal = Calendar.getInstance(nyZone, Locale.US).apply { timeInMillis = startMs }
        assertEquals(2026, startCal.get(Calendar.YEAR))
        assertEquals(Calendar.MARCH, startCal.get(Calendar.MONTH))
        assertEquals(8, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, startCal.get(Calendar.MINUTE))

        val endCal = Calendar.getInstance(nyZone, Locale.US).apply { timeInMillis = endMs }
        assertEquals(8, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, endCal.get(Calendar.MINUTE))

        // Autumn fallback in 2026: November 1
        val fallCal = Calendar.getInstance(nyZone, Locale.US).apply {
            set(2026, Calendar.NOVEMBER, 1, 15, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val (fallStartMs, fallEndMs) = DateRangeCalculator.getDayBoundaries(fallCal.timeInMillis, nyZone)
        val fallStartCal = Calendar.getInstance(nyZone, Locale.US).apply { timeInMillis = fallStartMs }
        assertEquals(0, fallStartCal.get(Calendar.HOUR_OF_DAY))
        val fallEndCal = Calendar.getInstance(nyZone, Locale.US).apply { timeInMillis = fallEndMs }
        assertEquals(23, fallEndCal.get(Calendar.HOUR_OF_DAY))
    }

    @Test
    fun testCalculateRange_AllFilters() {
        val nowCal = Calendar.getInstance(utcZone, Locale.US).apply {
            set(2026, Calendar.OCTOBER, 1, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMs = nowCal.timeInMillis

        // ALL
        val allRange = DateRangeCalculator.calculateRange(SearchDateFilter.ALL, timeZone = utcZone, locale = Locale.US)
        assertEquals(0L, allRange.startMs)
        assertEquals(Long.MAX_VALUE, allRange.endMs)

        // TODAY
        val todayRange = DateRangeCalculator.calculateRange(SearchDateFilter.TODAY, timeZone = utcZone, locale = Locale.US)
        assertTrue(todayRange.endMs > todayRange.startMs)

        // YESTERDAY
        val yesterdayRange = DateRangeCalculator.calculateRange(SearchDateFilter.YESTERDAY, timeZone = utcZone, locale = Locale.US)
        assertTrue(yesterdayRange.endMs > yesterdayRange.startMs)
        assertTrue(todayRange.startMs > yesterdayRange.startMs)

        // SINGLE DAY
        val singleDayRange = DateRangeCalculator.calculateRange(
            SearchDateFilter.SINGLE_DAY,
            singleDayEpochMs = nowMs,
            timeZone = utcZone,
            locale = Locale.US
        )
        val startCal = Calendar.getInstance(utcZone, Locale.US).apply { timeInMillis = singleDayRange.startMs }
        assertEquals(1, startCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(0, startCal.get(Calendar.HOUR_OF_DAY))
        val endCal = Calendar.getInstance(utcZone, Locale.US).apply { timeInMillis = singleDayRange.endMs }
        assertEquals(1, endCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, endCal.get(Calendar.HOUR_OF_DAY))

        // CUSTOM RANGE
        val custom = DateRange(1000L, 5000L)
        val customRange = DateRangeCalculator.calculateRange(
            SearchDateFilter.CUSTOM_RANGE,
            customRange = custom,
            timeZone = utcZone,
            locale = Locale.US
        )
        val expectedCustom = DateRangeCalculator.getCustomRange(1000L, 5000L, utcZone)
        assertEquals(expectedCustom.startMs, customRange.startMs)
        assertEquals(expectedCustom.endMs, customRange.endMs)
    }
}
