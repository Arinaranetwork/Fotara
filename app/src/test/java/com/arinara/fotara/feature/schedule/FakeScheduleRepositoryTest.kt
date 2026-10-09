// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.schedule

import com.arinara.fotara.feature.schedule.model.ClassSchedule
import com.arinara.fotara.test.FakeScheduleRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FakeScheduleRepositoryTest {

    @Test
    fun repository_crudOperations_workCorrectly() = runBlocking {
        val repo = FakeScheduleRepository()

        val item1 = ClassSchedule(
            dayOfWeek = 1,
            startMinute = 480,
            endMinute = 580,
            subjectName = "Kalkulus II",
            roomName = "R. 302"
        )

        val id = repo.insertSchedule(item1)
        assertTrue(id > 0)

        val fetched = repo.getScheduleById(id)
        assertNotNull(fetched)
        assertEquals("Kalkulus II", fetched!!.subjectName)

        val updated = fetched.copy(roomName = "R. 305")
        val updateSuccess = repo.updateSchedule(updated)
        assertTrue(updateSuccess)
        assertEquals("R. 305", repo.getScheduleById(id)?.roomName)

        val deleteSuccess = repo.deleteSchedule(id)
        assertTrue(deleteSuccess)
        assertNull(repo.getScheduleById(id))
    }

    @Test
    fun repository_batchInsert_persistsMultipleItems() = runBlocking {
        val repo = FakeScheduleRepository()

        val list = listOf(
            ClassSchedule(dayOfWeek = 1, startMinute = 480, endMinute = 580, subjectName = "A"),
            ClassSchedule(dayOfWeek = 1, startMinute = 600, endMinute = 700, subjectName = "B"),
            ClassSchedule(dayOfWeek = 2, startMinute = 480, endMinute = 580, subjectName = "C")
        )

        val count = repo.batchInsert(list)
        assertEquals(3, count)

        val all = repo.getAllSchedules().first()
        assertEquals(3, all.size)

        val monday = repo.getSchedulesByDay(1).first()
        assertEquals(2, monday.size)
        assertEquals("A", monday[0].subjectName)
        assertEquals("B", monday[1].subjectName)

        val tuesday = repo.getSchedulesByDay(2).first()
        assertEquals(1, tuesday.size)
        assertEquals("C", tuesday[0].subjectName)
    }

    @Test
    fun repository_dayFilter_sortsChronologicallyByStartMinute() = runBlocking {
        val repo = FakeScheduleRepository()

        val lateClass = ClassSchedule(dayOfWeek = 1, startMinute = 780, endMinute = 880, subjectName = "Sore")
        val earlyClass = ClassSchedule(dayOfWeek = 1, startMinute = 480, endMinute = 580, subjectName = "Pagi")
        val midClass = ClassSchedule(dayOfWeek = 1, startMinute = 600, endMinute = 700, subjectName = "Siang")

        repo.insertSchedule(lateClass)
        repo.insertSchedule(earlyClass)
        repo.insertSchedule(midClass)

        val monday = repo.getSchedulesByDay(1).first()
        assertEquals(3, monday.size)
        assertEquals("Pagi", monday[0].subjectName)
        assertEquals("Siang", monday[1].subjectName)
        assertEquals("Sore", monday[2].subjectName)
    }
}
