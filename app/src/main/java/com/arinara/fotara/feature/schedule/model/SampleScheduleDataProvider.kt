// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.schedule.model

/**
 * Provides production-grade sample class schedule data conforming to the Fotara
 * academic timetable format (ISO-8601 dayOfWeek 1..7, startMinute, endMinute, course name,
 * room, instructor, and palette colors).
 */
object SampleScheduleDataProvider {

    /**
     * Generates a realistic weekly academic schedule template across Monday through Friday.
     */
    fun createSampleSchedules(): List<ClassSchedule> {
        val now = System.currentTimeMillis()
        return listOf(
            // Monday (1)
            ClassSchedule(
                id = 0L,
                dayOfWeek = 1,
                startMinute = 8 * 60, // 08:00
                endMinute = 9 * 60 + 40, // 09:40
                subjectName = "Calculus & Analytical Geometry",
                roomName = "Hall 301",
                instructorName = "Dr. Aris Thorne",
                linkedFolderId = null,
                colorHex = "#2563EB",
                createdAt = now,
                updatedAt = now
            ),
            ClassSchedule(
                id = 0L,
                dayOfWeek = 1,
                startMinute = 10 * 60, // 10:00
                endMinute = 11 * 60 + 40, // 11:40
                subjectName = "Linear Algebra & Matrices",
                roomName = "Lab 102",
                instructorName = "Prof. Salim Mubarok",
                linkedFolderId = null,
                colorHex = "#3B82F6",
                createdAt = now,
                updatedAt = now
            ),

            // Tuesday (2)
            ClassSchedule(
                id = 0L,
                dayOfWeek = 2,
                startMinute = 9 * 60, // 09:00
                endMinute = 11 * 60 + 30, // 11:30
                subjectName = "Classical Mechanics & Thermodynamics",
                roomName = "Physics Lab B",
                instructorName = "Dr. Maya Hawthorne",
                linkedFolderId = null,
                colorHex = "#F59E0B",
                createdAt = now,
                updatedAt = now
            ),

            // Wednesday (3)
            ClassSchedule(
                id = 0L,
                dayOfWeek = 3,
                startMinute = 13 * 60, // 13:00
                endMinute = 15 * 60, // 15:00
                subjectName = "Data Structures & Algorithms",
                roomName = "CompSci Hall 204",
                instructorName = "Prof. Hendra Saputra",
                linkedFolderId = null,
                colorHex = "#10B981",
                createdAt = now,
                updatedAt = now
            ),

            // Thursday (4)
            ClassSchedule(
                id = 0L,
                dayOfWeek = 4,
                startMinute = 10 * 60, // 10:00
                endMinute = 11 * 60 + 40, // 11:40
                subjectName = "Discrete Mathematics & Logic",
                roomName = "Seminar Room 105",
                instructorName = "Dr. Surya Pratama",
                linkedFolderId = null,
                colorHex = "#8B5CF6",
                createdAt = now,
                updatedAt = now
            ),

            // Friday (5)
            ClassSchedule(
                id = 0L,
                dayOfWeek = 5,
                startMinute = 14 * 60, // 14:00
                endMinute = 15 * 60 + 30, // 15:30
                subjectName = "Academic Scientific Writing",
                roomName = "Auditorium C",
                instructorName = "Ms. Ratna Wardhani",
                linkedFolderId = null,
                colorHex = "#EC4899",
                createdAt = now,
                updatedAt = now
            )
        )
    }
}
