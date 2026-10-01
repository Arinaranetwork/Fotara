// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class ScheduleMathTest {

    @Test
    fun testPastDetection_IdentifiesPastAndFutureCorrectly() {
        val now = 1728000000000L // arbitrary fixed epoch

        // Future schedule
        val future = now + 60000L
        assertFalse(ScheduleMath.isPast(future, now))

        // Past schedule
        val past = now - 60000L
        assertTrue(ScheduleMath.isPast(past, now))

        // Exact match (considered elapsed / past)
        assertTrue(ScheduleMath.isPast(now, now))

        // Zero or negative schedule
        assertTrue(ScheduleMath.isPast(0L, now))
        assertTrue(ScheduleMath.isPast(-100L, now))
    }

    @Test
    fun testComputeSnoozeTime_AddsExactDuration() {
        val baseTime = 1728000000000L
        val snooze10m = ScheduleMath.computeSnoozeTime(baseTime, 10)
        assertEquals(baseTime + 10 * 60 * 1000L, snooze10m)

        val snooze15m = ScheduleMath.computeSnoozeTime(baseTime, 15)
        assertEquals(baseTime + 15 * 60 * 1000L, snooze15m)

        // Default 10 minutes
        val snoozeDefault = ScheduleMath.computeSnoozeTime(baseTime)
        assertEquals(baseTime + 600000L, snoozeDefault)
    }

    @Test
    fun testIsDueToday_IdentifiesSameDayTimes() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 5)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis

        // 4 hours later same day
        cal.set(Calendar.HOUR_OF_DAY, 14)
        val sameDayLater = cal.timeInMillis
        assertTrue(ScheduleMath.isDueToday(sameDayLater, now))
        assertFalse(ScheduleMath.isDueTomorrow(sameDayLater, now))

        // Next day 9 AM
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 9)
        val nextDay = cal.timeInMillis
        assertFalse(ScheduleMath.isDueToday(nextDay, now))
        assertTrue(ScheduleMath.isDueTomorrow(nextDay, now))

        // 3 days later
        cal.add(Calendar.DAY_OF_YEAR, 2)
        val threeDaysLater = cal.timeInMillis
        assertFalse(ScheduleMath.isDueToday(threeDaysLater, now))
        assertFalse(ScheduleMath.isDueTomorrow(threeDaysLater, now))
    }

    @Test
    fun testFormatScheduleBadge_FormatsTodayAndTomorrowCorrectly() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 10)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis

        // Set to 3:00 PM today
        cal.set(Calendar.HOUR_OF_DAY, 15)
        cal.set(Calendar.MINUTE, 0)
        val today3pm = cal.timeInMillis
        val formattedToday = ScheduleMath.formatShortScheduleBadge(today3pm, now)
        assertTrue("Expected 'Today, 3:00 PM', got '$formattedToday'", formattedToday.startsWith("Today, 3:00"))

        // Set to 9:00 AM tomorrow
        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 9)
        val tomorrow9am = cal.timeInMillis
        val formattedTomorrow = ScheduleMath.formatShortScheduleBadge(tomorrow9am, now)
        assertTrue("Expected 'Tomorrow, 9:00 AM', got '$formattedTomorrow'", formattedTomorrow.startsWith("Tomorrow, 9:00"))
    }

    @Test
    fun testFormatShortScheduleBadge_ReturnsCompactStrings() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.OCTOBER)
            set(Calendar.DAY_OF_MONTH, 10)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val now = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 16)
        val today4pm = cal.timeInMillis
        val shortToday = ScheduleMath.formatShortScheduleBadge(today4pm, now)
        assertTrue("Expected 'Today, 4:00 PM', got '$shortToday'", shortToday.startsWith("Today, 4:00"))

        cal.add(Calendar.DAY_OF_YEAR, 1)
        cal.set(Calendar.HOUR_OF_DAY, 11)
        val tmrw11am = cal.timeInMillis
        val shortTmrw = ScheduleMath.formatShortScheduleBadge(tmrw11am, now)
        assertTrue("Expected 'Tomorrow, 11:00 AM', got '$shortTmrw'", shortTmrw.startsWith("Tomorrow, 11:00"))
    }

    @Test
    fun testValidateScheduleTime_RejectsPastTimes() {
        val now = 1728000000000L
        assertEquals(ScheduleValidationResult.PAST_TIME, ScheduleMath.validateScheduleTime(now - 1000L, now))
        assertEquals(ScheduleValidationResult.PAST_TIME, ScheduleMath.validateScheduleTime(now, now))
        assertEquals(ScheduleValidationResult.VALID, ScheduleMath.validateScheduleTime(now + 1000L, now))
    }

    @Test
    fun testTimeZoneAdjustment_EpochRemainsIndependentOfZone() {
        val utcCal = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(2026, Calendar.OCTOBER, 1, 12, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val utcMillis = utcCal.timeInMillis

        // When queried in Tokyo (UTC+9), absolute UTC millis is identical
        val tokyoCal = Calendar.getInstance(TimeZone.getTimeZone("Asia/Tokyo")).apply {
            timeInMillis = utcMillis
        }
        assertEquals(utcMillis, tokyoCal.timeInMillis)
        // Tokyo hour is 12 + 9 = 21:00
        assertEquals(21, tokyoCal.get(Calendar.HOUR_OF_DAY))
    }
}
