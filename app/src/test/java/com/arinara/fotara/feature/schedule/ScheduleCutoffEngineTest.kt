// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.schedule

import com.arinara.fotara.feature.schedule.engine.ScheduleCapsuleState
import com.arinara.fotara.feature.schedule.engine.ScheduleCutoffEngine
import com.arinara.fotara.feature.schedule.model.ClassSchedule
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ScheduleCutoffEngineTest {

    private val engine = ScheduleCutoffEngine()

    private fun createSampleSchedule(
        id: Long,
        dayOfWeek: Int,
        startStr: String,
        endStr: String,
        subject: String,
        room: String? = null
    ): ClassSchedule {
        val startMin = ClassSchedule.parseTimeToMinutes(startStr) ?: 0
        val endMin = ClassSchedule.parseTimeToMinutes(endStr) ?: 0
        return ClassSchedule(
            id = id,
            dayOfWeek = dayOfWeek,
            startMinute = startMin,
            endMinute = endMin,
            subjectName = subject,
            roomName = room
        )
    }

    @Test
    fun evaluateCapsuleState_emptySchedules_returnsEmptyState() {
        val state = engine.evaluateCapsuleState(
            allSchedules = emptyList(),
            cutoffTimeStr = "18:00",
            currentDayOfWeek = 1,
            currentMinuteOfDay = 10 * 60
        )
        assertTrue(state is ScheduleCapsuleState.Empty)
    }

    @Test
    fun evaluateCapsuleState_preCutoff_activeClass_returnsActiveNow() {
        val schedules = listOf(
            createSampleSchedule(1, 1, "08:00", "09:40", "Kalkulus II", "R. 302"),
            createSampleSchedule(2, 1, "10:00", "11:40", "Fisika Dasar", "Lab 1")
        )

        // Simulated time: Monday 08:30 (510 min)
        val state = engine.evaluateCapsuleState(
            allSchedules = schedules,
            cutoffTimeStr = "18:00",
            currentDayOfWeek = 1,
            currentMinuteOfDay = 8 * 60 + 30
        )

        assertTrue(state is ScheduleCapsuleState.ActiveNow)
        val activeState = state as ScheduleCapsuleState.ActiveNow
        assertEquals("Kalkulus II", activeState.ongoing.subjectName)
        assertEquals(70, activeState.minutesRemaining) // 09:40 = 580, 580 - 510 = 70
        assertEquals("Fisika Dasar", activeState.upcomingNext?.subjectName)
        assertTrue(activeState.displayHeadline.contains("Now: Kalkulus II (R. 302)"))
        assertTrue(activeState.displayHeadline.contains("until 09:40"))
        assertTrue(activeState.displayHeadline.contains("10:00 Fisika Dasar"))
    }

    @Test
    fun evaluateCapsuleState_preCutoff_upcomingClass_returnsUpcomingToday() {
        val schedules = listOf(
            createSampleSchedule(1, 1, "10:00", "11:40", "Struktur Data", "R. 101"),
            createSampleSchedule(2, 1, "13:00", "14:40", "Algoritma", "R. 102")
        )

        // Simulated time: Monday 09:00 (540 min)
        val state = engine.evaluateCapsuleState(
            allSchedules = schedules,
            cutoffTimeStr = "18:00",
            currentDayOfWeek = 1,
            currentMinuteOfDay = 9 * 60
        )

        assertTrue(state is ScheduleCapsuleState.UpcomingToday)
        val upcoming = state as ScheduleCapsuleState.UpcomingToday
        assertEquals("Struktur Data", upcoming.nextClass.subjectName)
        assertEquals(2, upcoming.remainingClassesCount)
        assertTrue(upcoming.displayHeadline.contains("Next: 10:00 Struktur Data (R. 101)"))
        assertTrue(upcoming.displayHeadline.contains("2 Classes"))
    }

    @Test
    fun evaluateCapsuleState_preCutoff_allFinishedToday_returnsTodayFinished() {
        val schedules = listOf(
            createSampleSchedule(1, 1, "08:00", "09:40", "Kalkulus II", "R. 302"),
            createSampleSchedule(2, 2, "08:00", "09:40", "Pemrograman Web", "Lab 2")
        )

        // Simulated time: Monday 14:00 (before 18:00 cutoff, but classes done)
        val state = engine.evaluateCapsuleState(
            allSchedules = schedules,
            cutoffTimeStr = "18:00",
            currentDayOfWeek = 1,
            currentMinuteOfDay = 14 * 60
        )

        assertTrue(state is ScheduleCapsuleState.TodayFinished)
        val finished = state as ScheduleCapsuleState.TodayFinished
        assertEquals("Tomorrow", finished.nextScheduleDayName)
        assertEquals("Pemrograman Web", finished.nextFirstClass?.subjectName)
        assertTrue(finished.displayHeadline.contains("Classes Finished for Today"))
    }

    @Test
    fun evaluateCapsuleState_postCutoff_rollsOverToTomorrow() {
        val schedules = listOf(
            createSampleSchedule(1, 1, "08:00", "09:40", "Kalkulus II", "R. 302"),
            createSampleSchedule(2, 2, "08:00", "10:00", "Basis Data", "R. 401"),
            createSampleSchedule(3, 2, "10:30", "12:00", "Jaringan Komputer", "Lab 3")
        )

        // Simulated time: Monday 19:30 (after 18:00 cutoff)
        val state = engine.evaluateCapsuleState(
            allSchedules = schedules,
            cutoffTimeStr = "18:00",
            currentDayOfWeek = 1,
            currentMinuteOfDay = 19 * 60 + 30
        )

        assertTrue(state is ScheduleCapsuleState.RolloverTomorrow)
        val rollover = state as ScheduleCapsuleState.RolloverTomorrow
        assertEquals(2, rollover.targetDayOfWeek)
        assertEquals("Basis Data", rollover.firstClass.subjectName)
        assertEquals(2, rollover.totalClassesCount)
        assertTrue(rollover.displayHeadline.contains("Next-Day Prep: 08:00 Basis Data (R. 401)"))
        assertTrue(rollover.displayHeadline.contains("2 Classes"))
    }

    @Test
    fun evaluateCapsuleState_fridayPostCutoff_rollsOverToMonday() {
        val schedules = listOf(
            createSampleSchedule(1, 5, "08:00", "09:40", "Pancasila", "R. 101"), // Friday
            createSampleSchedule(2, 1, "08:00", "09:40", "Kalkulus II", "R. 302") // Monday
        )

        // Simulated time: Friday 19:00 (after 18:00 cutoff)
        val state = engine.evaluateCapsuleState(
            allSchedules = schedules,
            cutoffTimeStr = "18:00",
            currentDayOfWeek = 5,
            currentMinuteOfDay = 19 * 60
        )

        assertTrue(state is ScheduleCapsuleState.RolloverTomorrow)
        val rollover = state as ScheduleCapsuleState.RolloverTomorrow
        assertEquals(1, rollover.targetDayOfWeek) // Monday
        assertEquals("Monday", rollover.targetDayName)
        assertEquals("Kalkulus II", rollover.firstClass.subjectName)
        assertTrue(rollover.displayHeadline.contains("Prep for Monday: 08:00 Kalkulus II"))
    }

    @Test
    fun evaluateCapsuleState_customCutoffTime_respectsUserSetting() {
        val schedules = listOf(
            createSampleSchedule(1, 1, "08:00", "10:00", "Kelas Pagi", "R. 1"),
            createSampleSchedule(2, 2, "08:00", "10:00", "Kelas Besok", "R. 2")
        )

        // Cutoff set to 15:00. Time is 16:00 -> should roll over
        val state = engine.evaluateCapsuleState(
            allSchedules = schedules,
            cutoffTimeStr = "15:00",
            currentDayOfWeek = 1,
            currentMinuteOfDay = 16 * 60
        )

        assertTrue(state is ScheduleCapsuleState.RolloverTomorrow)
    }
}
