// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.schedule

import com.arinara.fotara.feature.schedule.model.ClassSchedule
import com.arinara.fotara.feature.schedule.parser.RawTableData
import com.arinara.fotara.feature.schedule.parser.ScheduleColumnMapping
import com.arinara.fotara.feature.schedule.parser.XlsxDocxScheduleParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XlsxDocxScheduleParserTest {

    private val parser = XlsxDocxScheduleParser()

    @Test
    fun autoDetectColumnMapping_detectsIndonesianHeadersCorrectly() {
        val table = RawTableData(
            headers = listOf("No", "Hari", "Jam", "Mata Kuliah", "Ruang", "Dosen Pengajar"),
            rows = listOf(
                listOf("1", "Senin", "08:00 - 09:40", "Kalkulus II", "R. 302", "Dr. Budi"),
                listOf("2", "Senin", "10:00 - 11:40", "Fisika Dasar", "Lab Fisika", "Dr. Siti")
            )
        )

        val mapping = parser.autoDetectColumnMapping(table)

        assertEquals(1, mapping.dayColumnIndex)
        assertEquals(2, mapping.timeColumnIndex)
        assertEquals(3, mapping.subjectColumnIndex)
        assertEquals(4, mapping.roomColumnIndex)
        assertEquals(5, mapping.instructorColumnIndex)
        assertTrue(mapping.isValid)
    }

    @Test
    fun autoDetectColumnMapping_detectsEnglishHeadersCorrectly() {
        val table = RawTableData(
            headers = listOf("Day", "Time", "Course Name", "Room", "Lecturer"),
            rows = listOf(
                listOf("Monday", "08:00-10:00", "Data Structures", "Auditorium", "Prof. John")
            )
        )

        val mapping = parser.autoDetectColumnMapping(table)

        assertEquals(0, mapping.dayColumnIndex)
        assertEquals(1, mapping.timeColumnIndex)
        assertEquals(2, mapping.subjectColumnIndex)
        assertEquals(3, mapping.roomColumnIndex)
        assertEquals(4, mapping.instructorColumnIndex)
        assertTrue(mapping.isValid)
    }

    @Test
    fun mapToSchedules_propagatesMergedDayCells() {
        val table = RawTableData(
            headers = listOf("Hari", "Waktu", "Mata Kuliah", "Ruangan"),
            rows = listOf(
                listOf("Senin", "08:00 - 09:40", "Kalkulus II", "R. 302"),
                listOf("", "10:00 - 11:40", "Fisika Dasar", "Lab 1"), // Empty day = Merged from Senin
                listOf("Selasa", "13:00 - 14:40", "Basis Data", "R. 201"),
                listOf("", "15:00 - 16:40", "Jaringan Komputer", "Lab Komputer") // Empty day = Merged from Selasa
            )
        )

        val mapping = ScheduleColumnMapping(
            dayColumnIndex = 0,
            timeColumnIndex = 1,
            subjectColumnIndex = 2,
            roomColumnIndex = 3
        )

        val schedules = parser.mapToSchedules(table, mapping)

        assertEquals(4, schedules.size)

        // Class 1: Senin
        assertEquals(1, schedules[0].dayOfWeek)
        assertEquals("Kalkulus II", schedules[0].subjectName)
        assertEquals(480, schedules[0].startMinute) // 08:00
        assertEquals(580, schedules[0].endMinute)   // 09:40

        // Class 2: Merged Senin
        assertEquals(1, schedules[1].dayOfWeek)
        assertEquals("Fisika Dasar", schedules[1].subjectName)
        assertEquals(600, schedules[1].startMinute) // 10:00
        assertEquals(700, schedules[1].endMinute)   // 11:40

        // Class 3: Selasa
        assertEquals(2, schedules[2].dayOfWeek)
        assertEquals("Basis Data", schedules[2].subjectName)

        // Class 4: Merged Selasa
        assertEquals(2, schedules[3].dayOfWeek)
        assertEquals("Jaringan Komputer", schedules[3].subjectName)
    }

    @Test
    fun parseTimeFormats_parsesDotAndColonNotations() {
        val range1 = ClassSchedule.parseTimeRange("08:00 - 09:40")
        assertNotNull(range1)
        assertEquals(480, range1!!.first)
        assertEquals(580, range1.second)

        val range2 = ClassSchedule.parseTimeRange("08.00 s/d 10.30")
        assertNotNull(range2)
        assertEquals(480, range2!!.first)
        assertEquals(630, range2.second)

        val range3 = ClassSchedule.parseTimeRange("13:15 – 15:00") // En-dash
        assertNotNull(range3)
        assertEquals(13 * 60 + 15, range3!!.first)
        assertEquals(15 * 60, range3.second)
    }

    @Test
    fun parseDays_parsesAllIndonesianDayNames() {
        assertEquals(1, ClassSchedule.parseDayOfWeek("Senin"))
        assertEquals(2, ClassSchedule.parseDayOfWeek("Selasa"))
        assertEquals(3, ClassSchedule.parseDayOfWeek("Rabu"))
        assertEquals(4, ClassSchedule.parseDayOfWeek("Kamis"))
        assertEquals(5, ClassSchedule.parseDayOfWeek("Jumat"))
        assertEquals(5, ClassSchedule.parseDayOfWeek("Jum'at"))
        assertEquals(6, ClassSchedule.parseDayOfWeek("Sabtu"))
        assertEquals(7, ClassSchedule.parseDayOfWeek("Minggu"))
    }
}
