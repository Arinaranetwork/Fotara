// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.schedule.notification

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.arinara.fotara.MainActivity
import com.arinara.fotara.R
import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.feature.schedule.engine.ScheduleCutoffEngine
import com.arinara.fotara.feature.schedule.engine.ScheduleDisableEngine
import com.arinara.fotara.feature.schedule.model.ClassSchedule
import com.arinara.fotara.feature.schedule.model.DisabledDaySchedule
import com.arinara.fotara.feature.schedule.model.ScheduleDayNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Calendar

class ScheduleNotificationScheduler(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        createChannels()
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Channel 1: Morning digest and daily prep notes
            val morningChannel = NotificationChannel(
                CHANNEL_ID_MORNING,
                "Morning Schedule & Notes",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Daily morning timetable summaries, class briefings, and attached per-day reminders"
            }
            notificationManager.createNotificationChannel(morningChannel)

            // Channel 2: Real-time pre-class alerts and reminders
            val remindersChannel = NotificationChannel(
                CHANNEL_ID_REMINDERS,
                "Class Reminders & Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Pre-class start notifications (10 minutes prior) and urgent timetable reminders"
            }
            notificationManager.createNotificationChannel(remindersChannel)
        }
    }

    /**
     * Reads all schedules from SQLite and reschedules upcoming alarms,
     * called on device reboot or system time change.
     */
    fun rescheduleAllClassAlarms() {
        try {
            purgeLegacyAntiProcrastinationAlarmsAndPrefs()
            val db = FotaraDbHelper.getInstance(context).getSafeReadableDatabase()
            val schedules = mutableListOf<ClassSchedule>()
            db.rawQuery(
                """
                SELECT id, day_of_week, start_minute, end_minute, subject_name,
                       room_name, instructor_name, linked_folder_id, color_hex,
                       created_at, updated_at
                FROM class_schedules
                ORDER BY day_of_week ASC, start_minute ASC
                """.trimIndent(),
                null
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    schedules.add(
                        ClassSchedule(
                            id = cursor.getLong(0),
                            dayOfWeek = cursor.getInt(1),
                            startMinute = cursor.getInt(2),
                            endMinute = cursor.getInt(3),
                            subjectName = cursor.getString(4) ?: "",
                            roomName = cursor.getString(5),
                            instructorName = cursor.getString(6),
                            linkedFolderId = if (cursor.isNull(7)) null else cursor.getLong(7),
                            colorHex = cursor.getString(8) ?: "#2563EB",
                            createdAt = cursor.getLong(9),
                            updatedAt = cursor.getLong(10)
                        )
                    )
                }
            }
            rescheduleAll(schedules)
        } catch (e: Exception) {
            Log.e("ScheduleNotificationScheduler", "Failed to rescheduleAllClassAlarms", e)
        }
    }

    /**
     * Cancels any leftover scheduled alarms and purges stored preferences from the removed
     * Anti-Procrastination Study Alarm (photo proof challenge) feature on app update or migration.
     */
    fun purgeLegacyAntiProcrastinationAlarmsAndPrefs() {
        try {
            val legacyPrefNames = listOf(
                "anti_procrastination_alarms",
                "anti_procrastination_prefs",
                "procrastination_alarm_prefs"
            )
            for (prefName in legacyPrefNames) {
                context.getSharedPreferences(prefName, Context.MODE_PRIVATE).edit().clear().apply()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    context.deleteSharedPreferences(prefName)
                }
            }

            val settingsPrefs = context.getSharedPreferences("fotara_settings", Context.MODE_PRIVATE)
            val legacyKeys = settingsPrefs.all.keys.filter {
                it.contains("procrastination", ignoreCase = true) ||
                    it.contains("photo_challenge", ignoreCase = true)
            }
            if (legacyKeys.isNotEmpty()) {
                val editor = settingsPrefs.edit()
                legacyKeys.forEach { editor.remove(it) }
                editor.apply()
            }

            val legacyActions = listOf(
                "com.arinara.fotara.ACTION_ANTI_PROCRASTINATION_ALARM",
                "com.arinara.fotara.alarm.ACTION_ANTI_PROCRASTINATION_ALARM",
                "com.arinara.fotara.alarm.ACTION_TRIGGER_CHALLENGE"
            )
            for (action in legacyActions) {
                for (reqCode in 0..20) {
                    val intent = Intent(action).setPackage(context.packageName)
                    val pi = PendingIntent.getBroadcast(
                        context,
                        reqCode,
                        intent,
                        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                    )
                    if (pi != null) {
                        alarmManager.cancel(pi)
                        pi.cancel()
                    }
                }
                for (reqCode in 7000..7025) {
                    val intent = Intent(action).setPackage(context.packageName)
                    val pi = PendingIntent.getBroadcast(
                        context,
                        reqCode,
                        intent,
                        PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
                    )
                    if (pi != null) {
                        alarmManager.cancel(pi)
                        pi.cancel()
                    }
                }
            }
        } catch (e: Exception) {
            Log.w("ScheduleNotificationScheduler", "Legacy anti-procrastination cleanup warning", e)
        }
    }

    fun canScheduleExactAlarms(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                alarmManager.canScheduleExactAlarms()
            } catch (_: Exception) {
                false
            }
        } else {
            true
        }
    }

    fun areNotificationsEnabled(): Boolean {
        return try {
            notificationManager.areNotificationsEnabled()
        } catch (_: Exception) {
            true
        }
    }

    /**
     * Reschedules all alarms for today & evening based on the full list of schedules,
     * respecting disabled days and attaching per-day notes.
     */
    fun rescheduleAll(schedules: List<ClassSchedule>, cutoffTimeStr: String = "18:00") {
        val now = Calendar.getInstance()
        val currentDay = ScheduleCutoffEngine.getCurrentDayOfWeek(now)

        // Check if today is disabled
        val isTodayDisabled = isDayDisabledSync(currentDay, now.timeInMillis)
        val hasNotesToday = getNotesForTodaySync(currentDay, now.timeInMillis).isNotEmpty()

        // 1. Morning Digest (06:30 today or tomorrow)
        if (schedules.isNotEmpty() || hasNotesToday) {
            scheduleDailyAlarm(
                alarmType = TYPE_MORNING_DIGEST,
                hour = 6,
                minute = 30,
                requestCode = REQ_MORNING_DIGEST
            )
        }

        // 2. Evening Rollover Digest (at cutoff time)
        if (schedules.isNotEmpty()) {
            val cutoffMin = ClassSchedule.parseTimeToMinutes(cutoffTimeStr) ?: (18 * 60)
            scheduleDailyAlarm(
                alarmType = TYPE_EVENING_ROLLOVER,
                hour = cutoffMin / 60,
                minute = cutoffMin % 60,
                requestCode = REQ_EVENING_ROLLOVER
            )
        }

        // 3. Pre-class heads up for today's remaining classes (H-10 min) - Suppressed if today is disabled!
        val todayClasses = schedules.filter { it.dayOfWeek == currentDay }
        val nowMinute = ScheduleCutoffEngine.getCurrentMinuteOfDay(now)

        for (cls in todayClasses) {
            val alertMinute = cls.startMinute - 10
            val reqCode = (cls.id.toInt() * 100) + 1

            if (!isTodayDisabled && alertMinute > nowMinute) {
                val alertCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, alertMinute / 60)
                    set(Calendar.MINUTE, alertMinute % 60)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                scheduleClassAlert(cls, alertCal.timeInMillis, reqCode)
            } else {
                // Cancel existing alert if day is disabled or past
                cancelClassAlert(reqCode)
            }
        }
    }

    private fun scheduleDailyAlarm(alarmType: String, hour: Int, minute: Int, requestCode: Int) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ACTION_SCHEDULE_ALARM
            putExtra(EXTRA_ALARM_TYPE, alarmType)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        armAlarmSafely(cal.timeInMillis, pendingIntent)
    }

    private fun scheduleClassAlert(schedule: ClassSchedule, triggerAtMillis: Long, reqCode: Int) {
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ACTION_SCHEDULE_ALARM
            putExtra(EXTRA_ALARM_TYPE, TYPE_PRE_CLASS)
            putExtra(EXTRA_SUBJECT_NAME, schedule.subjectName)
            putExtra(EXTRA_ROOM_NAME, schedule.roomName ?: "")
            putExtra(EXTRA_START_TIME, schedule.startTimeFormatted)
            putExtra(EXTRA_LINKED_FOLDER_ID, schedule.linkedFolderId ?: -1L)
            putExtra(EXTRA_DAY_OF_WEEK, schedule.dayOfWeek)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        armAlarmSafely(triggerAtMillis, pendingIntent)
    }

    private fun cancelClassAlert(reqCode: Int) {
        try {
            val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
                action = ACTION_SCHEDULE_ALARM
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                reqCode,
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                alarmManager.cancel(pendingIntent)
                pendingIntent.cancel()
            }
        } catch (_: Exception) {}
    }

    fun showPreClassNotification(subject: String, room: String, startTime: String, folderId: Long) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_schedule_folder_id", folderId)
        }
        val safeFolderKey = kotlin.math.abs(folderId % 1000).toInt()
        val reqCode = (REQ_TAP_CLASS * 1000) + safeFolderKey
        val pendingTap = PendingIntent.getActivity(
            context,
            reqCode,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val roomDetail = if (room.isNotBlank()) " in $room" else ""
        val notification = NotificationCompat.Builder(context, CHANNEL_ID_REMINDERS)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Class Starting Soon (10 min): $subject")
            .setContentText("Starts at $startTime$roomDetail. Tap to prepare notes.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingTap)
            .build()

        val notifId = (NOTIFICATION_ID_PRE_CLASS * 1000) + safeFolderKey
        notificationManager.notify(notifId, notification)
    }

    fun showDigestNotification(title: String, message: String, isMorning: Boolean = true) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingTap = PendingIntent.getActivity(
            context,
            REQ_TAP_DIGEST,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = if (isMorning) CHANNEL_ID_MORNING else CHANNEL_ID_MORNING
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingTap)
            .build()

        notificationManager.notify(NOTIFICATION_ID_DIGEST, notification)
    }

    fun scheduleConfigurableReminder(hour: Int, minute: Int, dayText: String) {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis()) {
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ACTION_SCHEDULE_ALARM
            putExtra(EXTRA_ALARM_TYPE, TYPE_CONFIGURABLE_REMINDER)
            putExtra(EXTRA_DAY_TEXT, dayText)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            REQ_CONFIGURABLE_REMINDER,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        armAlarmSafely(cal.timeInMillis, pendingIntent)
    }

    private fun armAlarmSafely(triggerAtMillis: Long, pendingIntent: PendingIntent) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                } else {
                    alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
                }
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    private fun isDayDisabledSync(dayOfWeek: Int, nowMs: Long): Boolean {
        return try {
            val db = FotaraDbHelper.getInstance(context).getSafeReadableDatabase()
            var disabled = false
            db.rawQuery(
                "SELECT disabled_until_ms, mode FROM disabled_schedule_days WHERE day_of_week = ? LIMIT 1",
                arrayOf(dayOfWeek.toString())
            ).use { cursor ->
                if (cursor.moveToNext()) {
                    val untilMs = cursor.getLong(0)
                    val mode = cursor.getString(1) ?: "ONLY_THIS_DAY"
                    val record = DisabledDaySchedule(dayOfWeek, untilMs, mode)
                    disabled = ScheduleDisableEngine.isDayDisabled(record, nowMs)
                }
            }
            disabled
        } catch (_: Exception) {
            false
        }
    }

    fun getNotesForTodaySync(dayOfWeek: Int, todayStartMillis: Long): List<ScheduleDayNote> {
        val notes = mutableListOf<ScheduleDayNote>()
        try {
            val db = FotaraDbHelper.getInstance(context).getSafeReadableDatabase()
            db.rawQuery(
                """
                SELECT id, day_of_week, date_millis, note_text, is_repeating_weekly, created_at, updated_at
                FROM schedule_day_notes
                WHERE day_of_week = ?
                ORDER BY updated_at DESC
                """.trimIndent(),
                arrayOf(dayOfWeek.toString())
            ).use { cursor ->
                while (cursor.moveToNext()) {
                    val id = cursor.getLong(0)
                    val day = cursor.getInt(1)
                    val dateMs = cursor.getLong(2)
                    val text = cursor.getString(3) ?: ""
                    val isRepeating = cursor.getInt(4) == 1
                    val created = cursor.getLong(5)
                    val updated = cursor.getLong(6)

                    // Applies if repeating weekly OR date matches today
                    val sameDay = isRepeating || isSameCalendarDay(dateMs, todayStartMillis)
                    if (sameDay) {
                        notes.add(ScheduleDayNote(id, day, dateMs, text, isRepeating, created, updated))
                    }
                }
            }
        } catch (_: Exception) {}
        return notes
    }

    private fun isSameCalendarDay(ms1: Long, ms2: Long): Boolean {
        val c1 = Calendar.getInstance().apply { timeInMillis = ms1 }
        val c2 = Calendar.getInstance().apply { timeInMillis = ms2 }
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR) &&
               c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR)
    }

    companion object {
        const val CHANNEL_ID_MORNING = "fotara_morning_summary"
        const val CHANNEL_ID_REMINDERS = "fotara_class_reminders"
        const val CHANNEL_ID = CHANNEL_ID_MORNING // Legacy compatibility alias

        const val ACTION_SCHEDULE_ALARM = "com.arinara.fotara.ACTION_SCHEDULE_ALARM"

        const val EXTRA_ALARM_TYPE = "alarm_type"
        const val EXTRA_SUBJECT_NAME = "subject_name"
        const val EXTRA_ROOM_NAME = "room_name"
        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_LINKED_FOLDER_ID = "linked_folder_id"
        const val EXTRA_DAY_TEXT = "day_text"
        const val EXTRA_DAY_OF_WEEK = "day_of_week"

        const val TYPE_MORNING_DIGEST = "MORNING_DIGEST"
        const val TYPE_PRE_CLASS = "PRE_CLASS"
        const val TYPE_EVENING_ROLLOVER = "EVENING_ROLLOVER"
        const val TYPE_CONFIGURABLE_REMINDER = "CONFIGURABLE_REMINDER"

        const val REQ_MORNING_DIGEST = 5001
        const val REQ_EVENING_ROLLOVER = 5002
        const val REQ_TAP_CLASS = 5003
        const val REQ_TAP_DIGEST = 5004
        const val REQ_CONFIGURABLE_REMINDER = 5005

        const val NOTIFICATION_ID_PRE_CLASS = 6001
        const val NOTIFICATION_ID_DIGEST = 6002
    }
}

class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        val scheduler = ScheduleNotificationScheduler(context)
        val alarmType = intent.getStringExtra(ScheduleNotificationScheduler.EXTRA_ALARM_TYPE) ?: run {
            pendingResult.finish()
            return
        }

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val now = Calendar.getInstance()
                val currentDay = ScheduleCutoffEngine.getCurrentDayOfWeek(now)

                when (alarmType) {
                    ScheduleNotificationScheduler.TYPE_PRE_CLASS -> {
                        val dayOfWeek = intent.getIntExtra(ScheduleNotificationScheduler.EXTRA_DAY_OF_WEEK, currentDay)
                        // Verify day is not disabled
                        val isDayDisabled = try {
                            val db = FotaraDbHelper.getInstance(context).getSafeReadableDatabase()
                            var disabled = false
                            db.rawQuery(
                                "SELECT disabled_until_ms, mode FROM disabled_schedule_days WHERE day_of_week = ? LIMIT 1",
                                arrayOf(dayOfWeek.toString())
                            ).use { c ->
                                if (c.moveToNext()) {
                                    val untilMs = c.getLong(0)
                                    val mode = c.getString(1) ?: "ONLY_THIS_DAY"
                                    disabled = ScheduleDisableEngine.isDayDisabled(
                                        DisabledDaySchedule(dayOfWeek, untilMs, mode),
                                        now.timeInMillis
                                    )
                                }
                            }
                            disabled
                        } catch (_: Exception) { false }

                        if (!isDayDisabled) {
                            val subject = intent.getStringExtra(ScheduleNotificationScheduler.EXTRA_SUBJECT_NAME) ?: "Class"
                            val room = intent.getStringExtra(ScheduleNotificationScheduler.EXTRA_ROOM_NAME) ?: ""
                            val startTime = intent.getStringExtra(ScheduleNotificationScheduler.EXTRA_START_TIME) ?: ""
                            val folderId = intent.getLongExtra(ScheduleNotificationScheduler.EXTRA_LINKED_FOLDER_ID, -1L)
                            scheduler.showPreClassNotification(subject, room, startTime, folderId)
                        }
                    }

                    ScheduleNotificationScheduler.TYPE_MORNING_DIGEST -> {
                        // Check if today is disabled
                        val isTodayDisabled = try {
                            val db = FotaraDbHelper.getInstance(context).getSafeReadableDatabase()
                            var disabled = false
                            db.rawQuery(
                                "SELECT disabled_until_ms, mode FROM disabled_schedule_days WHERE day_of_week = ? LIMIT 1",
                                arrayOf(currentDay.toString())
                            ).use { c ->
                                if (c.moveToNext()) {
                                    val untilMs = c.getLong(0)
                                    val mode = c.getString(1) ?: "ONLY_THIS_DAY"
                                    disabled = ScheduleDisableEngine.isDayDisabled(
                                        DisabledDaySchedule(currentDay, untilMs, mode),
                                        now.timeInMillis
                                    )
                                }
                            }
                            disabled
                        } catch (_: Exception) { false }

                        if (!isTodayDisabled) {
                            val todayNotes = scheduler.getNotesForTodaySync(currentDay, now.timeInMillis)
                            val notePrefix = if (todayNotes.isNotEmpty()) {
                                val firstNote = todayNotes.first().noteText
                                "📌 Note: $firstNote\n"
                            } else ""

                            scheduler.showDigestNotification(
                                title = "Today's Class Schedule",
                                message = "${notePrefix}Good morning! Open Fotara to review today's schedule and course notes.",
                                isMorning = true
                            )
                        }
                    }

                    ScheduleNotificationScheduler.TYPE_EVENING_ROLLOVER -> {
                        scheduler.showDigestNotification(
                            title = "Tomorrow's Class Preparation",
                            message = "Tomorrow's class schedule is ready. Ensure your books and notes are prepared tonight.",
                            isMorning = false
                        )
                    }

                    ScheduleNotificationScheduler.TYPE_CONFIGURABLE_REMINDER -> {
                        val dayText = intent.getStringExtra(ScheduleNotificationScheduler.EXTRA_DAY_TEXT) ?: "Today"
                        scheduler.showDigestNotification(
                            title = "Class Schedule Reminder",
                            message = dayText,
                            isMorning = true
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ScheduleAlarmReceiver", "Error processing alarm", e)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
