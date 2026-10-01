// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import com.arinara.fotara.MainActivity
import com.arinara.fotara.R
import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.data.model.SchedulableNote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

enum class ScheduleNoteType(val tableName: String, val typeCode: Int) {
    PHOTO("photos", 1),
    PHOTO_GROUP("photo_groups", 2),
    DOCUMENT("document_notes", 3),
    TEXT_NOTE("text_notes", 4),
    CANVAS_NOTE("canvas_notes", 5),
    FOLDER("folders", 6)
}

enum class ScheduleAlertType {
    NOTIFICATION,
    ALARM
}

class NoteScheduleManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        createChannels()
    }

    fun canScheduleExact(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun canPostNotifications(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }

    fun canUseFullScreen(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            notificationManager.canUseFullScreenIntent()
        } else {
            true
        }
    }

    private fun createChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                .build()

            val reminderChannel = NotificationChannel(
                CHANNEL_REMINDER_ID,
                "Study Note Reminders",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Standard scheduled notifications for course notes and tasks"
                enableVibration(true)
                setSound(defaultSound, audioAttributes)
            }
            notificationManager.createNotificationChannel(reminderChannel)

            val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            val alarmAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build()

            val alarmChannel = NotificationChannel(
                CHANNEL_ALARM_ID,
                "Study Note Urgent Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority alarms with continuous sound for deadlines"
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 800, 400, 800, 400, 800)
                setSound(alarmSound, alarmAttributes)
            }
            notificationManager.createNotificationChannel(alarmChannel)
        }
    }

    fun scheduleNote(
        noteType: ScheduleNoteType,
        noteId: Long,
        folderId: Long,
        title: String,
        triggerAtMillis: Long,
        alertType: ScheduleAlertType,
        scheduleTitle: String? = null
    ) {
        val dbHelper = FotaraDbHelper(context)
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("scheduled_at", triggerAtMillis)
            put("alert_type", alertType.name)
            put("schedule_title", scheduleTitle)
        }
        db.update(noteType.tableName, values, "id = ?", arrayOf(noteId.toString()))

        armAlarm(noteType, noteId, folderId, title, triggerAtMillis, alertType, scheduleTitle)

        com.arinara.fotara.widget.DueTomorrowWidgetProvider.notifyDataChanged(context)
    }

    private fun armAlarm(
        noteType: ScheduleNoteType,
        noteId: Long,
        folderId: Long,
        title: String,
        triggerAtMillis: Long,
        alertType: ScheduleAlertType,
        scheduleTitle: String? = null
    ) {
        val requestCode = computeRequestCode(noteType, noteId)
        val intent = Intent(context, NoteScheduleReceiver::class.java).apply {
            action = ACTION_FIRE_SCHEDULE
            putExtra("extra_note_type", noteType.name)
            putExtra("extra_note_id", noteId)
            putExtra("extra_folder_id", folderId)
            putExtra("extra_title", title)
            putExtra("extra_schedule_title", scheduleTitle)
            putExtra("extra_alert_type", alertType.name)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
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
            // Fallback for when exact alarm permission is missing
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }
    }

    fun cancelSchedule(noteType: ScheduleNoteType, noteId: Long) {
        val dbHelper = FotaraDbHelper(context)
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            putNull("scheduled_at")
            putNull("alert_type")
            putNull("schedule_title")
        }
        db.update(noteType.tableName, values, "id = ?", arrayOf(noteId.toString()))

        cancelAlarmOnly(noteType, noteId)

        com.arinara.fotara.widget.DueTomorrowWidgetProvider.notifyDataChanged(context)
    }

    fun cancelAlarmOnly(noteType: ScheduleNoteType, noteId: Long) {
        val requestCode = computeRequestCode(noteType, noteId)
        val intent = Intent(context, NoteScheduleReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    fun rearmAlarmIfFuture(note: SchedulableNote) {
        val scheduledAt = note.scheduledAt ?: return
        if (scheduledAt > System.currentTimeMillis()) {
            val alertType = try {
                ScheduleAlertType.valueOf(note.alertType ?: ScheduleAlertType.NOTIFICATION.name)
            } catch (_: Exception) {
                ScheduleAlertType.NOTIFICATION
            }
            armAlarm(
                noteType = note.noteType,
                noteId = note.id,
                folderId = note.folderId,
                title = note.title,
                triggerAtMillis = scheduledAt,
                alertType = alertType,
                scheduleTitle = note.scheduleTitle
            )
        }
    }

    fun rescheduleAllUpcoming() {
        val now = System.currentTimeMillis()
        val dbHelper = FotaraDbHelper(context)
        val upcoming = dbHelper.getUpcomingSchedules(now)

        for (item in upcoming) {
            val noteType = when (item.type) {
                FotaraDbHelper.NoteType.PHOTO -> ScheduleNoteType.PHOTO
                FotaraDbHelper.NoteType.GROUP -> ScheduleNoteType.PHOTO_GROUP
                FotaraDbHelper.NoteType.DOCUMENT -> ScheduleNoteType.DOCUMENT
                FotaraDbHelper.NoteType.TEXT -> ScheduleNoteType.TEXT_NOTE
                FotaraDbHelper.NoteType.CANVAS -> ScheduleNoteType.CANVAS_NOTE
            }
            val alertType = try {
                ScheduleAlertType.valueOf(item.alertType)
            } catch (_: Exception) {
                ScheduleAlertType.NOTIFICATION
            }
            armAlarm(
                noteType = noteType,
                noteId = item.id,
                folderId = item.folderId,
                title = item.title,
                triggerAtMillis = item.scheduledAt,
                alertType = alertType,
                scheduleTitle = item.scheduleTitle
            )
        }
    }

    fun sendTestAlert(alertType: ScheduleAlertType = ScheduleAlertType.NOTIFICATION) {
        val isAlarm = alertType == ScheduleAlertType.ALARM
        val channelId = if (isAlarm) CHANNEL_ALARM_ID else CHANNEL_REMINDER_ID

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            99991,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(if (isAlarm) "Test Alarm Alert" else "Test Reminder Notification")
            .setContentText("Notifications and alerts are working properly in Fotara.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)

        if (isAlarm) {
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
            builder.setVibrate(longArrayOf(0, 500, 200, 500))
        }

        notificationManager.notify(99991, builder.build())
    }

    private fun computeRequestCode(noteType: ScheduleNoteType, noteId: Long): Int {
        val base = noteType.typeCode.toLong() * 100_000_000L
        return ((base + (noteId % 100_000_000L)) % Int.MAX_VALUE).toInt()
    }

    companion object {
        const val CHANNEL_REMINDER_ID = "fotara_study_reminders"
        const val CHANNEL_ALARM_ID = "fotara_study_alarms"

        const val ACTION_FIRE_SCHEDULE = "com.arinara.fotara.action.FIRE_SCHEDULE"
        const val ACTION_SNOOZE = "com.arinara.fotara.action.SNOOZE_SCHEDULE"
        const val ACTION_DISMISS = "com.arinara.fotara.action.DISMISS_SCHEDULE"
    }
}

class NoteScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: NoteScheduleManager.ACTION_FIRE_SCHEDULE
        val noteTypeName = intent.getStringExtra("extra_note_type") ?: ScheduleNoteType.PHOTO.name
        val noteId = intent.getLongExtra("extra_note_id", 0L)
        val folderId = intent.getLongExtra("extra_folder_id", 0L)
        val title = intent.getStringExtra("extra_title") ?: "Scheduled Study Note"
        val scheduleTitle = intent.getStringExtra("extra_schedule_title")
        val alertTypeName = intent.getStringExtra("extra_alert_type") ?: ScheduleAlertType.NOTIFICATION.name
        val notificationId = (100000L + (noteId % 100000L)).toInt()

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        when (action) {
            NoteScheduleManager.ACTION_DISMISS -> {
                notificationManager.cancel(notificationId)
            }
            NoteScheduleManager.ACTION_SNOOZE -> {
                notificationManager.cancel(notificationId)
                val snoozeTime = ScheduleMath.computeSnoozeTime(System.currentTimeMillis(), 10)
                val noteType = try { ScheduleNoteType.valueOf(noteTypeName) } catch (_: Exception) { ScheduleNoteType.PHOTO }
                val alertType = try { ScheduleAlertType.valueOf(alertTypeName) } catch (_: Exception) { ScheduleAlertType.NOTIFICATION }

                val manager = NoteScheduleManager(context)
                manager.scheduleNote(
                    noteType = noteType,
                    noteId = noteId,
                    folderId = folderId,
                    title = title,
                    triggerAtMillis = snoozeTime,
                    alertType = alertType,
                    scheduleTitle = scheduleTitle
                )
            }
            else -> {
                // Fire notification or alarm alert
                val isAlarm = alertTypeName == ScheduleAlertType.ALARM.name
                val channelId = if (isAlarm) NoteScheduleManager.CHANNEL_ALARM_ID else NoteScheduleManager.CHANNEL_REMINDER_ID

                // 1. Open note pending intent
                val launchIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("scheduled_note_type", noteTypeName)
                    putExtra("scheduled_note_id", noteId)
                    putExtra("scheduled_folder_id", folderId)
                }
                val openPendingIntent = PendingIntent.getActivity(
                    context,
                    noteId.toInt(),
                    launchIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 2. Snooze pending intent (10 minutes)
                val snoozeIntent = Intent(context, NoteScheduleReceiver::class.java).apply {
                    this.action = NoteScheduleManager.ACTION_SNOOZE
                    putExtra("extra_note_type", noteTypeName)
                    putExtra("extra_note_id", noteId)
                    putExtra("extra_folder_id", folderId)
                    putExtra("extra_title", title)
                    putExtra("extra_schedule_title", scheduleTitle)
                    putExtra("extra_alert_type", alertTypeName)
                }
                val snoozePendingIntent = PendingIntent.getBroadcast(
                    context,
                    (noteId + 500000L).toInt(),
                    snoozeIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // 3. Dismiss pending intent
                val dismissIntent = Intent(context, NoteScheduleReceiver::class.java).apply {
                    this.action = NoteScheduleManager.ACTION_DISMISS
                    putExtra("extra_note_id", noteId)
                }
                val dismissPendingIntent = PendingIntent.getBroadcast(
                    context,
                    (noteId + 700000L).toInt(),
                    dismissIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                val displayTitle = scheduleTitle?.ifBlank { null } ?: if (isAlarm) "Urgent Alarm: $title" else "Scheduled Reminder"

                val builder = NotificationCompat.Builder(context, channelId)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(displayTitle)
                    .setContentText(title)
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setAutoCancel(true)
                    .setContentIntent(openPendingIntent)
                    .addAction(android.R.drawable.ic_menu_view, "Open", openPendingIntent)
                    .addAction(android.R.drawable.ic_popup_reminder, "Snooze (10m)", snoozePendingIntent)
                    .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Dismiss", dismissPendingIntent)

                if (isAlarm) {
                    builder.setCategory(NotificationCompat.CATEGORY_ALARM)
                    builder.setVibrate(longArrayOf(0, 800, 400, 800, 400, 800))
                    // Full-screen intent for screen-off alarm firing
                    builder.setFullScreenIntent(openPendingIntent, true)
                }

                notificationManager.notify(notificationId, builder.build())
            }
        }
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON" ||
            action == Intent.ACTION_TIME_CHANGED ||
            action == Intent.ACTION_TIMEZONE_CHANGED
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val manager = NoteScheduleManager(context)
                manager.rescheduleAllUpcoming()
            }
        }
    }
}
