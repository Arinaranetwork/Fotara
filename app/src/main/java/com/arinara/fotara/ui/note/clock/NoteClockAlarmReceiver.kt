// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.clock

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.arinara.fotara.MainActivity
import java.util.Locale

class NoteClockAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NoteClockAlarmManager.ACTION_ALARM_TRIGGER) return

        val label = intent.getStringExtra(NoteClockAlarmManager.EXTRA_ALARM_LABEL) ?: "Study Session"
        val hour = intent.getIntExtra(NoteClockAlarmManager.EXTRA_ALARM_HOUR, 8)
        val minute = intent.getIntExtra(NoteClockAlarmManager.EXTRA_ALARM_MINUTE, 0)
        val timeFormatted = String.format(Locale.US, "%02d:%02d", hour, minute)

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        val channelId = "fotara_note_clock_alarm_channel"
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Academic Clock Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled academic alarms in Fotara Clock"
                enableVibration(true)
            }
            nm.createNotificationChannel(channel)
        }

        val launchIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra("OPEN_CLOCK_SCREEN", true)
        }
        val contentPi = PendingIntent.getActivity(
            context,
            timeFormatted.hashCode(),
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val defaultSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
            .setContentTitle("Fotara Academic Alarm")
            .setContentText("$label ($timeFormatted)")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setSound(defaultSound)
            .setAutoCancel(true)
            .setContentIntent(contentPi)
            .build()

        val notificationId = 7000 + (hour * 60 + minute)
        nm.notify(notificationId, notification)
    }
}
