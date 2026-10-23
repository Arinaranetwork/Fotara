// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.schedule

import com.arinara.fotara.feature.schedule.engine.DisableDuration
import com.arinara.fotara.feature.schedule.engine.ScheduleCapsuleState
import com.arinara.fotara.feature.schedule.engine.ScheduleCutoffEngine
import com.arinara.fotara.feature.schedule.engine.ScheduleDisableEngine
import com.arinara.fotara.feature.schedule.model.ClassSchedule
import com.arinara.fotara.feature.schedule.model.DisabledDaySchedule
import com.arinara.fotara.feature.schedule.model.ScheduleDayNote
import com.arinara.fotara.test.FakeScheduleRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar

class ScheduleDisableAndNoteLogicTest {

    private val cutoffEngine = ScheduleCutoffEngine()

    @Test
    fun testDisableDurationNever_returnsLongMaxValue() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 22, 10, 0, 0)
        }
        val expiry = ScheduleDisableEngine.calculateExpiry(
            duration = DisableDuration.NEVER,
            targetDayOfWeek = 4, // Thursday
            nowCalendar = cal
        )
        assertEquals(Long.MAX_VALUE, expiry)

        val record = DisabledDaySchedule(4, expiry, "NEVER")
        assertFalse(record.isExpired(cal.timeInMillis))
        assertTrue(ScheduleDisableEngine.isDayDisabled(record, cal.timeInMillis))
    }

    @Test
    fun testDisableDurationOneMonth_expiresIn30Days() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 22, 10, 0, 0)
        }
        val expiry = ScheduleDisableEngine.calculateExpiry(
            duration = DisableDuration.ONE_MONTH,
            targetDayOfWeek = 4,
            nowCalendar = cal
        )
        val expected = cal.timeInMillis + (30L * 24 * 60 * 60 * 1000L)
        assertEquals(expected, expiry)

        val record = DisabledDaySchedule(4, expiry, "ONE_MONTH")
        assertTrue(ScheduleDisableEngine.isDayDisabled(record, cal.timeInMillis + 1000L))
        // After 31 days
        assertFalse(ScheduleDisableEngine.isDayDisabled(record, cal.timeInMillis + (31L * 24 * 60 * 60 * 1000L)))
    }

    @Test
    fun testDisableDurationThisWeek_expiresOnSundayEndOfDay() {
        // Thursday Oct 22, 2026
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 22, 14, 0, 0)
        }
        val expiry = ScheduleDisableEngine.calculateExpiry(
            duration = DisableDuration.THIS_WEEK,
            targetDayOfWeek = 4,
            nowCalendar = cal
        )
        val expiryCal = Calendar.getInstance().apply { timeInMillis = expiry }
        assertEquals(Calendar.SUNDAY, expiryCal.get(Calendar.DAY_OF_WEEK))
        assertEquals(23, expiryCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, expiryCal.get(Calendar.MINUTE))
        assertEquals(59, expiryCal.get(Calendar.SECOND))
    }

    @Test
    fun testDisableDurationOnlyThisDay_beforeCutoff_expiresToday() {
        // Thursday Oct 22, 2026 at 10:00 (before 18:00 cutoff)
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 22, 10, 0, 0)
        }
        val expiry = ScheduleDisableEngine.calculateExpiry(
            duration = DisableDuration.ONLY_THIS_DAY,
            targetDayOfWeek = 4, // Thursday
            nowCalendar = cal,
            cutoffMinute = 18 * 60
        )
        val expiryCal = Calendar.getInstance().apply { timeInMillis = expiry }
        assertEquals(22, expiryCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, expiryCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, expiryCal.get(Calendar.MINUTE))
    }

    @Test
    fun testDisableDurationOnlyThisDay_afterCutoff_rollsOverToNextWeek() {
        // Thursday Oct 22, 2026 at 19:30 (after 18:00 cutoff)
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 22, 19, 30, 0)
        }
        val expiry = ScheduleDisableEngine.calculateExpiry(
            duration = DisableDuration.ONLY_THIS_DAY,
            targetDayOfWeek = 4, // Thursday
            nowCalendar = cal,
            cutoffMinute = 18 * 60
        )
        val expiryCal = Calendar.getInstance().apply { timeInMillis = expiry }
        // Next Thursday is Oct 29, 2026
        assertEquals(29, expiryCal.get(Calendar.DAY_OF_MONTH))
        assertEquals(23, expiryCal.get(Calendar.HOUR_OF_DAY))
        assertEquals(59, expiryCal.get(Calendar.MINUTE))
    }

    @Test
    fun testCapsuleState_skipsDisabledToday() {
        // Monday (1) has a class at 09:00
        val mondayClass = ClassSchedule(
            id = 1L,
            dayOfWeek = 1,
            startMinute = 9 * 60,
            endMinute = 11 * 60,
            subjectName = "Calculus"
        )
        // Tuesday (2) has a class at 10:00
        val tuesdayClass = ClassSchedule(
            id = 2L,
            dayOfWeek = 2,
            startMinute = 10 * 60,
            endMinute = 12 * 60,
            subjectName = "Physics"
        )
        val schedules = listOf(mondayClass, tuesdayClass)

        // Pre-cutoff on Monday at 08:00, but Monday is disabled!
        val state = cutoffEngine.evaluateCapsuleState(
            allSchedules = schedules,
            cutoffTimeStr = "18:00",
            currentDayOfWeek = 1,
            currentMinuteOfDay = 8 * 60,
            disabledDays = setOf(1)
        )

        // Must roll over to Tuesday!
        assertTrue(state is ScheduleCapsuleState.RolloverTomorrow)
        val rollover = state as ScheduleCapsuleState.RolloverTomorrow
        assertEquals(2, rollover.targetDayOfWeek)
        assertEquals("Physics", rollover.firstClass.subjectName)
    }

    @Test
    fun testCapsuleState_allDaysDisabled_returnsEmpty() {
        val mondayClass = ClassSchedule(
            id = 1L,
            dayOfWeek = 1,
            startMinute = 9 * 60,
            endMinute = 11 * 60,
            subjectName = "Calculus"
        )
        val schedules = listOf(mondayClass)

        val state = cutoffEngine.evaluateCapsuleState(
            allSchedules = schedules,
            cutoffTimeStr = "18:00",
            currentDayOfWeek = 1,
            currentMinuteOfDay = 8 * 60,
            disabledDays = setOf(1)
        )

        assertTrue(state is ScheduleCapsuleState.Empty)
    }

    @Test
    fun testScheduleDayNote_dateMatchingAndLabels() {
        val cal = Calendar.getInstance().apply {
            set(2026, Calendar.OCTOBER, 22, 0, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val oneTimeNote = ScheduleDayNote(
            id = 1L,
            dayOfWeek = 4,
            dateMillis = cal.timeInMillis,
            noteText = "Bring lab coat",
            isRepeatingWeekly = false
        )
        assertEquals("One-time: Oct 22, 2026", oneTimeNote.formatDateLabel())

        val repeatingNote = ScheduleDayNote(
            id = 2L,
            dayOfWeek = 4,
            dateMillis = cal.timeInMillis,
            noteText = "Weekly assignment check",
            isRepeatingWeekly = true
        )
        assertEquals("Repeats weekly", repeatingNote.formatDateLabel())
    }

    @Test
    fun testRepositoryDisableAndReenableSync() = runBlocking {
        val repo = FakeScheduleRepository()
        assertEquals(0, repo.getDisabledDaysOnce().size)

        val cal = Calendar.getInstance()
        repo.disableDay(dayOfWeek = 2, duration = DisableDuration.THIS_WEEK, nowCalendar = cal)
        assertTrue(repo.isDayDisabled(2))
        assertEquals(1, repo.getDisabledDaysOnce().size)

        repo.reenableDay(2)
        assertFalse(repo.isDayDisabled(2))
        assertEquals(0, repo.getDisabledDaysOnce().size)
    }

    @Test
    fun testRepositoryDayNotesSync() = runBlocking {
        val repo = FakeScheduleRepository()
        assertEquals(0, repo.getDayNotesOnce(3).size)

        val note = ScheduleDayNote(
            dayOfWeek = 3,
            dateMillis = System.currentTimeMillis(),
            noteText = "Submit draft proposal",
            isRepeatingWeekly = true
        )
        val id = repo.saveDayNote(note)
        assertTrue(id > 0)

        val notes = repo.getDayNotesOnce(3)
        assertEquals(1, notes.size)
        assertEquals("Submit draft proposal", notes.first().noteText)

        repo.deleteDayNote(id)
        assertEquals(0, repo.getDayNotesOnce(3).size)
    }
}
