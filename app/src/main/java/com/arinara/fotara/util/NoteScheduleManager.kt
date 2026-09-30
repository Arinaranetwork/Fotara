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
import androidx.core.app.NotificationCompat
import com.arinara.fotara.MainActivity
import com.arinara.fotara.R
import com.arinara.fotara.data.db.FotaraDbHelper
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
                vibrationPattern = longArrayOf(0, 500, 200, 500, 200, 800)
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
        alertType: ScheduleAlertType
    ) {
        val dbHelper = FotaraDbHelper(context)
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            put("scheduled_at", triggerAtMillis)
            put("alert_type", alertType.name)
        }
        db.update(noteType.tableName, values, "id = ?", arrayOf(noteId.toString()))

        val requestCode = computeRequestCode(noteType, noteId)
        val intent = Intent(context, NoteScheduleReceiver::class.java).apply {
            putExtra("extra_note_type", noteType.name)
            putExtra("extra_note_id", noteId)
            putExtra("extra_folder_id", folderId)
            putExtra("extra_title", title)
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
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent)
        }

        com.arinara.fotara.widget.DueTomorrowWidgetProvider.notifyDataChanged(context)
    }

    fun cancelSchedule(noteType: ScheduleNoteType, noteId: Long) {
        val dbHelper = FotaraDbHelper(context)
        val db = dbHelper.getSafeWritableDatabase()
        val values = ContentValues().apply {
            putNull("scheduled_at")
            putNull("alert_type")
        }
        db.update(noteType.tableName, values, "id = ?", arrayOf(noteId.toString()))

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

        com.arinara.fotara.widget.DueTomorrowWidgetProvider.notifyDataChanged(context)
    }

    fun rescheduleAllUpcoming() {
        val now = System.currentTimeMillis()
        val dbHelper = FotaraDbHelper(context)
        val db = dbHelper.getSafeReadableDatabase()

        for (type in ScheduleNoteType.entries) {
            try {
                val titleColumn = when (type) {
                    ScheduleNoteType.PHOTO -> "caption"
                    ScheduleNoteType.PHOTO_GROUP -> "name"
                    ScheduleNoteType.DOCUMENT -> "name"
                    ScheduleNoteType.TEXT_NOTE -> "title"
                    ScheduleNoteType.CANVAS_NOTE -> "title"
                    ScheduleNoteType.FOLDER -> "name"
                }
                val folderColumn = if (type == ScheduleNoteType.FOLDER) "id" else "folder_id"

                val cursor = db.rawQuery(
                    """
                    SELECT id, $folderColumn, $titleColumn, scheduled_at, alert_type
                    FROM ${type.tableName}
                    WHERE is_trashed = 0 AND scheduled_at IS NOT NULL AND scheduled_at > ?
                    """.trimIndent(),
                    arrayOf(now.toString())
                )

                cursor.use { c ->
                    while (c.moveToNext()) {
                        val id = c.getLong(0)
                        val folderId = c.getLong(1)
                        val title = if (c.isNull(2) || c.getString(2).isBlank()) "Study Note" else c.getString(2)
                        val scheduledAt = c.getLong(3)
                        val alertTypeName = if (c.isNull(4)) ScheduleAlertType.NOTIFICATION.name else c.getString(4)
                        val alertType = try {
                            ScheduleAlertType.valueOf(alertTypeName)
                        } catch (_: Exception) {
                            ScheduleAlertType.NOTIFICATION
                        }

                        val requestCode = computeRequestCode(type, id)
                        val intent = Intent(context, NoteScheduleReceiver::class.java).apply {
                            putExtra("extra_note_type", type.name)
                            putExtra("extra_note_id", id)
                            putExtra("extra_folder_id", folderId)
                            putExtra("extra_title", title)
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
                                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, scheduledAt, pendingIntent)
                            } else {
                                alarmManager.setExact(AlarmManager.RTC_WAKEUP, scheduledAt, pendingIntent)
                            }
                        } catch (_: SecurityException) {
                            alarmManager.set(AlarmManager.RTC_WAKEUP, scheduledAt, pendingIntent)
                        }
                    }
                }
            } catch (_: Exception) {}
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
    }
}

class NoteScheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val noteTypeName = intent.getStringExtra("extra_note_type") ?: ScheduleNoteType.PHOTO.name
        val noteId = intent.getLongExtra("extra_note_id", 0L)
        val folderId = intent.getLongExtra("extra_folder_id", 0L)
        val title = intent.getStringExtra("extra_title") ?: "Scheduled Study Note"
        val alertTypeName = intent.getStringExtra("extra_alert_type") ?: ScheduleAlertType.NOTIFICATION.name

        val isAlarm = alertTypeName == ScheduleAlertType.ALARM.name
        val channelId = if (isAlarm) NoteScheduleManager.CHANNEL_ALARM_ID else NoteScheduleManager.CHANNEL_REMINDER_ID

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("scheduled_note_type", noteTypeName)
            putExtra("scheduled_note_id", noteId)
            putExtra("scheduled_folder_id", folderId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            noteId.toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(if (isAlarm) "Urgent Alarm: $title" else "Scheduled Reminder")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .addAction(
                android.R.drawable.ic_menu_view,
                "Open Note",
                pendingIntent
            )

        if (isAlarm) {
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
            builder.setVibrate(longArrayOf(0, 500, 300, 500, 300, 800))
        }

        notificationManager.notify((100000L + (noteId % 100000L)).toInt(), builder.build())
    }
}

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED ||
            action == Intent.ACTION_MY_PACKAGE_REPLACED ||
            action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            CoroutineScope(Dispatchers.IO).launch {
                val manager = NoteScheduleManager(context)
                manager.rescheduleAllUpcoming()
            }
        }
    }
}
