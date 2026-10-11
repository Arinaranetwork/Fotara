// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.alarm

import com.arinara.fotara.alarm.procrastination.model.AntiProcrastinationAlarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AntiProcrastinationAlarmTest {

    @Test
    fun validAlarm_instantiatesSuccessfully() {
        val triggerTime = System.currentTimeMillis() + 3600000L
        val alarm = AntiProcrastinationAlarm(
            id = 1L,
            title = "Review Thermodynamics Lecture 5",
            targetFolderId = 10L,
            triggerAtMillis = triggerTime,
            emergencyPin = "9876",
            isEnabled = true
        )

        assertEquals(1L, alarm.id)
        assertEquals("Review Thermodynamics Lecture 5", alarm.title)
        assertEquals(10L, alarm.targetFolderId)
        assertEquals(triggerTime, alarm.triggerAtMillis)
        assertEquals("9876", alarm.emergencyPin)
        assertTrue(alarm.isEnabled)
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankTitle_throwsException() {
        AntiProcrastinationAlarm(
            id = 2L,
            title = "   ",
            triggerAtMillis = System.currentTimeMillis()
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun shortEmergencyPin_throwsException() {
        AntiProcrastinationAlarm(
            id = 3L,
            title = "Problem Set",
            triggerAtMillis = System.currentTimeMillis(),
            emergencyPin = "12"
        )
    }

    @Test
    fun emergencyPinVerification_matchesExpectedPin() {
        val alarm = AntiProcrastinationAlarm(
            id = 4L,
            title = "Linear Algebra Final Review",
            triggerAtMillis = System.currentTimeMillis() + 60000L,
            emergencyPin = "4321"
        )

        assertTrue(alarm.emergencyPin == "4321")
        assertFalse(alarm.emergencyPin == "0000")
    }

    @Test
    fun alarmDisabling_updatesState() {
        val alarm = AntiProcrastinationAlarm(
            id = 5L,
            title = "Organic Chemistry Lab",
            triggerAtMillis = System.currentTimeMillis(),
            isEnabled = true
        )

        val disabledAlarm = alarm.copy(isEnabled = false)
        assertFalse(disabledAlarm.isEnabled)
    }
}
