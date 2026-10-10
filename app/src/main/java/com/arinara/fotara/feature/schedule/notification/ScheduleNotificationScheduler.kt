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
import androidx.core.app.NotificationCompat
import com.arinara.fotara.MainActivity
import com.arinara.fotara.R
import com.arinara.fotara.feature.schedule.engine.ScheduleCutoffEngine
import com.arinara.fotara.feature.schedule.model.ClassSchedule
import java.util.Calendar

class ScheduleNotificationScheduler(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Class Schedule & Timetable",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Morning schedule notification, pre-class reminders (10 min prior), and next-day evening prep"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Reschedules all alarms for today & evening based on the full list of schedules.
     */
    fun rescheduleAll(schedules: List<ClassSchedule>, cutoffTimeStr: String = "18:00") {
        if (schedules.isEmpty()) return

        val now = Calendar.getInstance()
        val currentDay = ScheduleCutoffEngine.getCurrentDayOfWeek(now)

        // 1. Morning Digest (06:30 today or tomorrow)
        scheduleDailyAlarm(
            alarmType = TYPE_MORNING_DIGEST,
            hour = 6,
            minute = 30,
            requestCode = REQ_MORNING_DIGEST
        )

        // 2. Evening Rollover Digest (at cutoff time)
        val cutoffMin = ClassSchedule.parseTimeToMinutes(cutoffTimeStr) ?: (18 * 60)
        scheduleDailyAlarm(
            alarmType = TYPE_EVENING_ROLLOVER,
            hour = cutoffMin / 60,
            minute = cutoffMin % 60,
            requestCode = REQ_EVENING_ROLLOVER
        )

        // 3. Pre-class heads up for today's remaining classes (H-10 min)
        val todayClasses = schedules.filter { it.dayOfWeek == currentDay }
        val nowMinute = ScheduleCutoffEngine.getCurrentMinuteOfDay(now)

        for (cls in todayClasses) {
            val alertMinute = cls.startMinute - 10
            if (alertMinute > nowMinute) {
                val alertCal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, alertMinute / 60)
                    set(Calendar.MINUTE, alertMinute % 60)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                scheduleClassAlert(cls, alertCal.timeInMillis)
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

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
        }
    }

    private fun scheduleClassAlert(schedule: ClassSchedule, triggerAtMillis: Long) {
        val intent = Intent(context, ScheduleAlarmReceiver::class.java).apply {
            action = ACTION_SCHEDULE_ALARM
            putExtra(EXTRA_ALARM_TYPE, TYPE_PRE_CLASS)
            putExtra(EXTRA_SUBJECT_NAME, schedule.subjectName)
            putExtra(EXTRA_ROOM_NAME, schedule.roomName ?: "")
            putExtra(EXTRA_START_TIME, schedule.startTimeFormatted)
            putExtra(EXTRA_LINKED_FOLDER_ID, schedule.linkedFolderId ?: -1L)
        }
        val reqCode = (schedule.id.toInt() * 100) + 1
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reqCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
            }
        } catch (_: SecurityException) {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun showPreClassNotification(subject: String, room: String, startTime: String, folderId: Long) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("open_schedule_folder_id", folderId)
        }
        val pendingTap = PendingIntent.getActivity(
            context,
            REQ_TAP_CLASS,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val roomDetail = if (room.isNotBlank()) " in $room" else ""
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle("Class Starting Soon (10 min): $subject")
            .setContentText("Starts at $startTime$roomDetail. Tap to prepare notes.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingTap)
            .build()

        notificationManager.notify(NOTIFICATION_ID_PRE_CLASS, notification)
    }

    fun showDigestNotification(title: String, message: String) {
        val tapIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingTap = PendingIntent.getActivity(
            context,
            REQ_TAP_DIGEST,
            tapIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
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

    companion object {
        const val CHANNEL_ID = "fotara_class_schedule"
        const val ACTION_SCHEDULE_ALARM = "com.arinara.fotara.ACTION_SCHEDULE_ALARM"

        const val EXTRA_ALARM_TYPE = "alarm_type"
        const val EXTRA_SUBJECT_NAME = "subject_name"
        const val EXTRA_ROOM_NAME = "room_name"
        const val EXTRA_START_TIME = "start_time"
        const val EXTRA_LINKED_FOLDER_ID = "linked_folder_id"

        const val TYPE_MORNING_DIGEST = "MORNING_DIGEST"
        const val TYPE_PRE_CLASS = "PRE_CLASS"
        const val TYPE_EVENING_ROLLOVER = "EVENING_ROLLOVER"

        const val REQ_MORNING_DIGEST = 5001
        const val REQ_EVENING_ROLLOVER = 5002
        const val REQ_TAP_CLASS = 5003
        const val REQ_TAP_DIGEST = 5004

        const val NOTIFICATION_ID_PRE_CLASS = 6001
        const val NOTIFICATION_ID_DIGEST = 6002
    }
}

class ScheduleAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val scheduler = ScheduleNotificationScheduler(context)
        val alarmType = intent.getStringExtra(ScheduleNotificationScheduler.EXTRA_ALARM_TYPE) ?: return

        when (alarmType) {
            ScheduleNotificationScheduler.TYPE_PRE_CLASS -> {
                val subject = intent.getStringExtra(ScheduleNotificationScheduler.EXTRA_SUBJECT_NAME) ?: "Class"
                val room = intent.getStringExtra(ScheduleNotificationScheduler.EXTRA_ROOM_NAME) ?: ""
                val startTime = intent.getStringExtra(ScheduleNotificationScheduler.EXTRA_START_TIME) ?: ""
                val folderId = intent.getLongExtra(ScheduleNotificationScheduler.EXTRA_LINKED_FOLDER_ID, -1L)
                scheduler.showPreClassNotification(subject, room, startTime, folderId)
            }
            ScheduleNotificationScheduler.TYPE_MORNING_DIGEST -> {
                scheduler.showDigestNotification(
                    title = "Today's Class Schedule",
                    message = "Good morning! Open Fotara to review today's class schedule and coursework notes."
                )
            }
            ScheduleNotificationScheduler.TYPE_EVENING_ROLLOVER -> {
                scheduler.showDigestNotification(
                    title = "Tomorrow's Class Preparation",
                    message = "Tomorrow's class schedule is ready. Ensure your books and notes are prepared tonight."
                )
            }
        }
    }
}
