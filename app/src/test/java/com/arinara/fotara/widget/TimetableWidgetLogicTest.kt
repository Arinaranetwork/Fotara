// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.widget

import com.arinara.fotara.feature.schedule.engine.ScheduleCapsuleState
import com.arinara.fotara.feature.schedule.engine.ScheduleCutoffEngine
import com.arinara.fotara.feature.schedule.model.ClassSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TimetableWidgetLogicTest {

    private val engine = ScheduleCutoffEngine()

    private val sampleSchedules = listOf(
        // Monday (1)
        ClassSchedule(
            id = 1L,
            dayOfWeek = 1,
            startMinute = 8 * 60, // 08:00
            endMinute = 9 * 60 + 40, // 09:40
            subjectName = "Kalkulus I",
            roomName = "R. 301"
        ),
        ClassSchedule(
            id = 2L,
            dayOfWeek = 1,
            startMinute = 10 * 60, // 10:00
            endMinute = 11 * 60 + 40, // 11:40
            subjectName = "Fisika Dasar",
            roomName = "Lab Fisika"
        ),
        // Tuesday (2)
        ClassSchedule(
            id = 3L,
            dayOfWeek = 2,
            startMinute = 9 * 60, // 09:00
            endMinute = 11 * 60, // 11:00
            subjectName = "Algoritma & Pemrograman",
            roomName = "Lab Komputer 2"
        ),
        // Friday (5)
        ClassSchedule(
            id = 4L,
            dayOfWeek = 5,
            startMinute = 13 * 60, // 13:00
            endMinute = 15 * 60, // 15:00
            subjectName = "Sistem Operasi",
            roomName = "R. 402"
        )
    )

    @Test
    fun evaluateCapsuleState_returnsEmpty_whenNoSchedules() {
        val state = engine.evaluateCapsuleState(
            allSchedules = emptyList(),
            cutoffTimeStr = "18:00",
            currentDayOfWeek = 1,
            currentMinuteOfDay = 10 * 60
        )
        assertTrue(state is ScheduleCapsuleState.Empty)
    }

    @Test
    fun evaluateCapsuleState_showsOngoingClass_preCutoff() {
        // Monday at 08:30 (ongoing Kalkulus I)
        val state = engine.evaluateCapsuleState(
            allSchedules = sampleSchedules,
            cutoffTimeStr = "18:00",
            currentDayOfWeek = 1,
            currentMinuteOfDay = 8 * 60 + 30
        )
        assertTrue(state is ScheduleCapsuleState.ActiveNow)
        val active = state as ScheduleCapsuleState.ActiveNow
        assertEquals("Kalkulus I", active.ongoing.subjectName)
        assertEquals(70, active.minutesRemaining) // 09:40 - 08:30 = 70 min
        assertNotNull(active.upcomingNext)
        assertEquals("Fisika Dasar", active.upcomingNext?.subjectName)
    }

    @Test
    fun evaluateCapsuleState_showsUpcomingClass_preCutoff() {
        // Monday at 07:30 (before first class)
        val state = engine.evaluateCapsuleState(
            allSchedules = sampleSchedules,
            cutoffTimeStr = "18:00",
            currentDayOfWeek = 1,
            currentMinuteOfDay = 7 * 60 + 30
        )
        assertTrue(state is ScheduleCapsuleState.UpcomingToday)
        val upcoming = state as ScheduleCapsuleState.UpcomingToday
        assertEquals("Kalkulus I", upcoming.nextClass.subjectName)
        assertEquals(2, upcoming.remainingClassesCount)
    }

    @Test
    fun evaluateCapsuleState_rollsOverToTomorrow_postCutoff() {
        // Monday at 18:30 (post 18:00 cutoff) -> rolls over to Tuesday
        val state = engine.evaluateCapsuleState(
            allSchedules = sampleSchedules,
            cutoffTimeStr = "18:00",
            currentDayOfWeek = 1,
            currentMinuteOfDay = 18 * 60 + 30
        )
        assertTrue(state is ScheduleCapsuleState.RolloverTomorrow)
        val rollover = state as ScheduleCapsuleState.RolloverTomorrow
        assertEquals(2, rollover.targetDayOfWeek) // Tuesday
        assertEquals("Algoritma & Pemrograman", rollover.firstClass.subjectName)
        assertEquals(1, rollover.totalClassesCount)
    }

    @Test
    fun evaluateCapsuleState_fridayPostCutoff_rollsOverToMonday() {
        // Friday at 19:00 -> no weekend classes -> skips to Monday
        val state = engine.evaluateCapsuleState(
            allSchedules = sampleSchedules,
            cutoffTimeStr = "18:00",
            currentDayOfWeek = 5, // Friday
            currentMinuteOfDay = 19 * 60
        )
        assertTrue(state is ScheduleCapsuleState.RolloverTomorrow)
        val rollover = state as ScheduleCapsuleState.RolloverTomorrow
        assertEquals(1, rollover.targetDayOfWeek) // Monday
        assertEquals("Kalkulus I", rollover.firstClass.subjectName)
        assertEquals(2, rollover.totalClassesCount)
    }

    @Test
    fun timetableWidgetItem_timeFormatting_isAccurate() {
        val item = TimetableWidgetItem(
            id = 10L,
            dayOfWeek = 1,
            startMinute = 8 * 60 + 15,
            endMinute = 10 * 60,
            subjectName = "Struktur Data",
            roomName = "R. 101",
            instructorName = "Dr. Arinara",
            linkedFolderId = 5L,
            colorHex = "#2563EB"
        )
        assertEquals("08:15", item.startTimeFormatted)
        assertEquals("10:00", item.endTimeFormatted)
        assertEquals("08:15 - 10:00", item.timeRangeFormatted)
    }
}
