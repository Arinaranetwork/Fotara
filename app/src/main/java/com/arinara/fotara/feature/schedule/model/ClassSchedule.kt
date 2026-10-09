// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.schedule.model

import java.util.Locale

/**
 * Persisted academic schedule slot.
 *
 * @param id Unique SQLite primary key
 * @param dayOfWeek 1 = Monday, 2 = Tuesday, ..., 7 = Sunday (ISO-8601 day of week)
 * @param startMinute Minutes from midnight (e.g. 8 * 60 = 480 for 08:00)
 * @param endMinute Minutes from midnight (e.g. 9 * 60 + 40 = 580 for 09:40)
 * @param subjectName Name of course / subject
 * @param roomName Room or location code (optional, e.g. "R. 302")
 * @param instructorName Lecturer or instructor name (optional)
 * @param linkedFolderId Optional Fotara folder ID associated with this course
 * @param colorHex Hex color code for display (e.g. "#2563EB")
 * @param createdAt Creation epoch millisecond
 * @param updatedAt Last update epoch millisecond
 */
data class ClassSchedule(
    val id: Long = 0L,
    val dayOfWeek: Int,
    val startMinute: Int,
    val endMinute: Int,
    val subjectName: String,
    val roomName: String? = null,
    val instructorName: String? = null,
    val linkedFolderId: Long? = null,
    val colorHex: String? = "#2563EB",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    init {
        require(dayOfWeek in 1..7) { "dayOfWeek must be between 1 (Monday) and 7 (Sunday), was: $dayOfWeek" }
        require(startMinute in 0..1439) { "startMinute must be between 0 and 1439, was: $startMinute" }
        require(endMinute in 0..1440) { "endMinute must be between 0 and 1440, was: $endMinute" }
    }

    val startTimeFormatted: String
        get() = formatMinutesToTime(startMinute)

    val endTimeFormatted: String
        get() = formatMinutesToTime(endMinute)

    val timeRangeFormatted: String
        get() = "$startTimeFormatted - $endTimeFormatted"

    val dayNameIndonesian: String
        get() = getIndonesianDayName(dayOfWeek)

    val dayNameShortIndonesian: String
        get() = getIndonesianDayShortName(dayOfWeek)

    fun isActiveAt(minuteOfDay: Int): Boolean =
        minuteOfDay in startMinute until endMinute

    fun isUpcomingAt(minuteOfDay: Int): Boolean =
        startMinute > minuteOfDay

    fun remainingMinutes(minuteOfDay: Int): Int =
        (endMinute - minuteOfDay).coerceAtLeast(0)

    companion object {
        fun formatMinutesToTime(totalMinutes: Int): String {
            val hours = (totalMinutes / 60) % 24
            val minutes = totalMinutes % 60
            return String.format(Locale.US, "%02d:%02d", hours, minutes)
        }

        fun parseTimeToMinutes(timeStr: String): Int? {
            val cleaned = timeStr.trim().replace('.', ':')
            val parts = cleaned.split(':')
            if (parts.size >= 2) {
                val h = parts[0].trim().toIntOrNull() ?: return null
                val m = parts[1].trim().take(2).toIntOrNull() ?: return null
                if (h in 0..23 && m in 0..59) {
                    return h * 60 + m
                }
            }
            return null
        }

        fun parseTimeRange(timeStr: String): Pair<Int, Int>? {
            val cleaned = timeStr.trim()
            val separators = listOf(" - ", "-", " s/d ", " s.d ", " sd ", " – ")
            for (sep in separators) {
                if (cleaned.contains(sep)) {
                    val parts = cleaned.split(sep)
                    if (parts.size >= 2) {
                        val s = parseTimeToMinutes(parts[0])
                        val e = parseTimeToMinutes(parts[1])
                        if (s != null && e != null) {
                            return Pair(s, e)
                        }
                    }
                }
            }
            val single = parseTimeToMinutes(cleaned) ?: return null
            return Pair(single, (single + 90) % (24 * 60))
        }

        fun getIndonesianDayName(dayOfWeek: Int): String = when (dayOfWeek) {
            1 -> "Senin"
            2 -> "Selasa"
            3 -> "Rabu"
            4 -> "Kamis"
            5 -> "Jumat"
            6 -> "Sabtu"
            7 -> "Minggu"
            else -> "Senin"
        }

        fun getIndonesianDayShortName(dayOfWeek: Int): String = when (dayOfWeek) {
            1 -> "Sen"
            2 -> "Sel"
            3 -> "Rab"
            4 -> "Kam"
            5 -> "Jum"
            6 -> "Sab"
            7 -> "Min"
            else -> "Sen"
        }

        fun parseDayOfWeek(text: String): Int? {
            val normalized = text.trim().lowercase(Locale.ROOT)
            return when {
                normalized.contains("senin") || normalized == "sen" || normalized.contains("mon") -> 1
                normalized.contains("selasa") || normalized == "sel" || normalized.contains("tue") -> 2
                normalized.contains("rabu") || normalized == "rab" || normalized.contains("wed") -> 3
                normalized.contains("kamis") || normalized == "kam" || normalized.contains("thu") -> 4
                normalized.contains("jumat") || normalized.contains("jum'at") || normalized == "jum" || normalized.contains("fri") -> 5
                normalized.contains("sabtu") || normalized == "sab" || normalized.contains("sat") -> 6
                normalized.contains("minggu") || normalized == "min" || normalized.contains("sun") -> 7
                else -> null
            }
        }
    }
}
