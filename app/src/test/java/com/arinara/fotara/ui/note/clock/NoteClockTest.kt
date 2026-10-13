// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.clock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class NoteClockTest {

    @Test
    fun testAlarmFormattedTime() {
        val alarm1 = NoteClockAlarm(hour = 8, minute = 5, label = "Morning Study")
        assertEquals("08:05", alarm1.getFormattedTime())

        val alarm2 = NoteClockAlarm(hour = 23, minute = 59, label = "Night Session")
        assertEquals("23:59", alarm2.getFormattedTime())
    }

    @Test
    fun testCalculateNextTriggerMillis_FutureToday() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = cal.timeInMillis

        // Target at 14:30 today
        val triggerMillis = NoteClockAlarmManager.calculateNextTriggerMillis(14, 30, nowMillis)
        val triggerCal = Calendar.getInstance().apply { timeInMillis = triggerMillis }

        assertEquals(cal.get(Calendar.DAY_OF_YEAR), triggerCal.get(Calendar.DAY_OF_YEAR))
        assertEquals(14, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(30, triggerCal.get(Calendar.MINUTE))
        assertEquals(0, triggerCal.get(Calendar.SECOND))
    }

    @Test
    fun testCalculateNextTriggerMillis_PastToday_SchedulesForTomorrow() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 16)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val nowMillis = cal.timeInMillis

        // Target at 08:00 (which has already passed today)
        val triggerMillis = NoteClockAlarmManager.calculateNextTriggerMillis(8, 0, nowMillis)
        val triggerCal = Calendar.getInstance().apply { timeInMillis = triggerMillis }

        // Must be in the future
        assertTrue(triggerMillis > nowMillis)
        assertEquals(8, triggerCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(0, triggerCal.get(Calendar.MINUTE))
        assertEquals(0, triggerCal.get(Calendar.SECOND))
    }

    @Test
    fun testDefaultGlobeLocations_ValidTimeZonesAndCoordinates() {
        for (loc in DEFAULT_GLOBE_LOCATIONS) {
            val tz = TimeZone.getTimeZone(loc.timeZoneId)
            assertNotNull("TimeZone must not be null for ${loc.id}", tz)
            assertTrue("Latitude out of range for ${loc.id}", loc.latitudeDeg in -90f..90f)
            assertTrue("Longitude out of range for ${loc.id}", loc.longitudeDeg in -180f..180f)
            assertTrue("ID must not be empty", loc.id.isNotBlank())
            assertTrue("Country code must be 3 letters", loc.countryCode.length == 3)
        }
    }

    @Test
    fun testTimerLapMath() {
        val laps = mutableListOf<TimerLapRecord>()
        var elapsed = 5400L // 5.4s
        laps.add(TimerLapRecord(lapIndex = 1, splitTimeMs = elapsed, totalTimeMs = elapsed))

        elapsed = 11200L // 11.2s
        val lastTotal = laps.last().totalTimeMs
        val split2 = elapsed - lastTotal
        laps.add(TimerLapRecord(lapIndex = 2, splitTimeMs = split2, totalTimeMs = elapsed))

        assertEquals(5400L, laps[0].splitTimeMs)
        assertEquals(5800L, laps[1].splitTimeMs)
        assertEquals(11200L, laps[1].totalTimeMs)
        assertEquals(elapsed, laps.sumOf { it.splitTimeMs })
    }
}
