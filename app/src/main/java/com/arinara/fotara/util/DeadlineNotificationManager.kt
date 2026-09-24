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
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.arinara.fotara.MainActivity

class DeadlineNotificationManager(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    init {
        createChannel()
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Coursework Deadlines",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminders for assignments and notes due tomorrow"
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    fun scheduleReminder(photoId: Long, folderName: String, caption: String?, deadlineMs: Long) {
        val triggerTime = deadlineMs - 24 * 60 * 60 * 1000L // 24 hours prior
        if (triggerTime <= System.currentTimeMillis()) {
            // Already within 24 hours or past, show or skip
            return
        }

        val intent = Intent(context, DeadlineAlarmReceiver::class.java).apply {
            putExtra("photo_id", photoId)
            putExtra("folder_name", folderName)
            putExtra("caption", caption ?: "Coursework assignment due tomorrow")
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            photoId.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            alarmManager.set(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
        } catch (_: SecurityException) {
            // Android 12+ exact alarm permission fallback
            alarmManager.set(AlarmManager.RTC, triggerTime, pendingIntent)
        }
        com.arinara.fotara.widget.DueTomorrowWidgetProvider.notifyDataChanged(context)
    }

    fun cancelReminder(photoId: Long) {
        val intent = Intent(context, DeadlineAlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            photoId.toInt(),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
        com.arinara.fotara.widget.DueTomorrowWidgetProvider.notifyDataChanged(context)
    }

    fun showDirectNotification(photoId: Long, folderName: String, caption: String?) {
        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("photo_id", photoId)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            photoId.toInt(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val viewNoteIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("photo_id", photoId)
            putExtra("direct_view_note", true)
        }
        val viewNotePendingIntent = PendingIntent.getActivity(
            context,
            (photoId + 100000).toInt(),
            viewNoteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Due Tomorrow: $folderName")
            .setContentText(caption ?: "You have a coursework note due tomorrow")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_view,
                "View Note",
                viewNotePendingIntent
            )
            .build()

        notificationManager.notify(photoId.toInt(), notification)
    }

    companion object {
        const val CHANNEL_ID = "fotara_deadlines"
    }
}

class DeadlineAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val photoId = intent.getLongExtra("photo_id", 0L)
        val folderName = intent.getStringExtra("folder_name") ?: "Coursework"
        val caption = intent.getStringExtra("caption")

        val manager = DeadlineNotificationManager(context)
        manager.showDirectNotification(photoId, folderName, caption)
    }
}
