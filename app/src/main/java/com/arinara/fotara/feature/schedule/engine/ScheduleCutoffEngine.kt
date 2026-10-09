// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.schedule.engine

import com.arinara.fotara.feature.schedule.model.ClassSchedule
import java.util.Calendar

/**
 * Capsule display state computed by [ScheduleCutoffEngine].
 */
sealed interface ScheduleCapsuleState {
    data class Empty(val message: String = "Jadwal kosong") : ScheduleCapsuleState

    data class ActiveNow(
        val ongoing: ClassSchedule,
        val upcomingNext: ClassSchedule?,
        val minutesRemaining: Int
    ) : ScheduleCapsuleState {
        val displayHeadline: String
            get() {
                val roomText = if (!ongoing.roomName.isNullOrBlank()) " (${ongoing.roomName})" else ""
                val nextText = if (upcomingNext != null) " | ${upcomingNext.startTimeFormatted} ${upcomingNext.subjectName}" else ""
                return "Sekarang: ${ongoing.subjectName}$roomText s.d ${ongoing.endTimeFormatted}$nextText"
            }
    }

    data class UpcomingToday(
        val nextClass: ClassSchedule,
        val remainingClassesCount: Int
    ) : ScheduleCapsuleState {
        val displayHeadline: String
            get() {
                val roomText = if (!nextClass.roomName.isNullOrBlank()) " (${nextClass.roomName})" else ""
                val countText = if (remainingClassesCount > 1) " • $remainingClassesCount Kelas" else ""
                return "Berikutnya: ${nextClass.startTimeFormatted} ${nextClass.subjectName}$roomText$countText"
            }
    }

    data class TodayFinished(
        val nextScheduleDayName: String?,
        val nextFirstClass: ClassSchedule?,
        val nextClassCount: Int
    ) : ScheduleCapsuleState {
        val displayHeadline: String
            get() {
                if (nextFirstClass != null && nextScheduleDayName != null) {
                    val roomText = if (!nextFirstClass.roomName.isNullOrBlank()) " (${nextFirstClass.roomName})" else ""
                    return "Kelas Hari Ini Selesai • $nextScheduleDayName: ${nextFirstClass.startTimeFormatted} ${nextFirstClass.subjectName}$roomText"
                }
                return "Kelas Hari Ini Selesai"
            }
    }

    data class RolloverTomorrow(
        val targetDayOfWeek: Int,
        val targetDayName: String,
        val firstClass: ClassSchedule,
        val totalClassesCount: Int
    ) : ScheduleCapsuleState {
        val displayHeadline: String
            get() {
                val roomText = if (!firstClass.roomName.isNullOrBlank()) " (${firstClass.roomName})" else ""
                val countText = if (totalClassesCount > 1) " • $totalClassesCount Kelas" else " • 1 Kelas"
                val prefix = if (targetDayOfWeek == (firstClass.dayOfWeek)) {
                    if (targetDayName.equals("Besok", ignoreCase = true)) "Persiapan Besok" else "Persiapan $targetDayName"
                } else "Persiapan $targetDayName"
                return "$prefix: ${firstClass.startTimeFormatted} ${firstClass.subjectName}$roomText$countText"
            }
    }
}

class ScheduleCutoffEngine {

    /**
     * Resolves the current schedule state for capsule display given all weekly schedules,
     * the configured rollover time (e.g. "18:00"), and the current simulated or live time.
     */
    fun evaluateCapsuleState(
        allSchedules: List<ClassSchedule>,
        cutoffTimeStr: String = "18:00",
        currentDayOfWeek: Int = getCurrentDayOfWeek(),
        currentMinuteOfDay: Int = getCurrentMinuteOfDay()
    ): ScheduleCapsuleState {
        if (allSchedules.isEmpty()) {
            return ScheduleCapsuleState.Empty("Belum ada jadwal kuliah")
        }

        val cutoffMinute = ClassSchedule.parseTimeToMinutes(cutoffTimeStr) ?: (18 * 60)
        val isPastCutoff = currentMinuteOfDay >= cutoffMinute

        if (isPastCutoff) {
            // Cutoff passed: roll over to tomorrow (or upcoming next academic day, e.g. Monday)
            return computeRolloverState(
                allSchedules = allSchedules,
                currentDayOfWeek = currentDayOfWeek
            )
        } else {
            // Daytime pre-cutoff: evaluate today's classes
            val todayClasses = allSchedules
                .filter { it.dayOfWeek == currentDayOfWeek }
                .sortedBy { it.startMinute }

            if (todayClasses.isEmpty()) {
                // No classes today: look ahead to next day
                return computeRolloverState(
                    allSchedules = allSchedules,
                    currentDayOfWeek = currentDayOfWeek,
                    defaultPrefixBesok = false
                )
            }

            // Check if any class is currently ongoing
            val ongoing = todayClasses.firstOrNull { it.isActiveAt(currentMinuteOfDay) }
            if (ongoing != null) {
                val upcomingNext = todayClasses.firstOrNull { it.startMinute >= ongoing.endMinute }
                val remaining = ongoing.remainingMinutes(currentMinuteOfDay)
                return ScheduleCapsuleState.ActiveNow(
                    ongoing = ongoing,
                    upcomingNext = upcomingNext,
                    minutesRemaining = remaining
                )
            }

            // Check if there are upcoming classes today
            val upcomingToday = todayClasses.filter { it.startMinute > currentMinuteOfDay }
            if (upcomingToday.isNotEmpty()) {
                val nextClass = upcomingToday.first()
                return ScheduleCapsuleState.UpcomingToday(
                    nextClass = nextClass,
                    remainingClassesCount = upcomingToday.size
                )
            }

            // All classes today are finished
            val nextAcademicDay = findNextAcademicDay(allSchedules, currentDayOfWeek)
            return ScheduleCapsuleState.TodayFinished(
                nextScheduleDayName = nextAcademicDay?.dayName,
                nextFirstClass = nextAcademicDay?.classes?.firstOrNull(),
                nextClassCount = nextAcademicDay?.classes?.size ?: 0
            )
        }
    }

    private data class NextDayInfo(
        val dayOfWeek: Int,
        val dayName: String,
        val classes: List<ClassSchedule>
    )

    private fun findNextAcademicDay(
        allSchedules: List<ClassSchedule>,
        fromDayOfWeek: Int
    ): NextDayInfo? {
        for (offset in 1..7) {
            var targetDay = (fromDayOfWeek + offset)
            if (targetDay > 7) targetDay -= 7

            val classesForDay = allSchedules
                .filter { it.dayOfWeek == targetDay }
                .sortedBy { it.startMinute }

            if (classesForDay.isNotEmpty()) {
                val dayName = if (offset == 1) "Besok" else ClassSchedule.getIndonesianDayName(targetDay)
                return NextDayInfo(
                    dayOfWeek = targetDay,
                    dayName = dayName,
                    classes = classesForDay
                )
            }
        }
        return null
    }

    private fun computeRolloverState(
        allSchedules: List<ClassSchedule>,
        currentDayOfWeek: Int,
        defaultPrefixBesok: Boolean = true
    ): ScheduleCapsuleState {
        val nextDayInfo = findNextAcademicDay(allSchedules, currentDayOfWeek)
            ?: return ScheduleCapsuleState.Empty("Tidak ada jadwal tersisa minggu ini")

        val targetDayName = if (nextDayInfo.dayName == "Besok" && !defaultPrefixBesok) {
            ClassSchedule.getIndonesianDayName(nextDayInfo.dayOfWeek)
        } else {
            nextDayInfo.dayName
        }

        return ScheduleCapsuleState.RolloverTomorrow(
            targetDayOfWeek = nextDayInfo.dayOfWeek,
            targetDayName = targetDayName,
            firstClass = nextDayInfo.classes.first(),
            totalClassesCount = nextDayInfo.classes.size
        )
    }

    companion object {
        /**
         * ISO Day of week: 1 = Monday .. 7 = Sunday
         */
        fun getCurrentDayOfWeek(calendar: Calendar = Calendar.getInstance()): Int {
            return when (calendar.get(Calendar.DAY_OF_WEEK)) {
                Calendar.MONDAY -> 1
                Calendar.TUESDAY -> 2
                Calendar.WEDNESDAY -> 3
                Calendar.THURSDAY -> 4
                Calendar.FRIDAY -> 5
                Calendar.SATURDAY -> 6
                Calendar.SUNDAY -> 7
                else -> 1
            }
        }

        fun getCurrentMinuteOfDay(calendar: Calendar = Calendar.getInstance()): Int {
            val hour = calendar.get(Calendar.HOUR_OF_DAY)
            val min = calendar.get(Calendar.MINUTE)
            return hour * 60 + min
        }
    }
}
