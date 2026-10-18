// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.alarm.procrastination.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import com.arinara.fotara.R
import com.arinara.fotara.alarm.procrastination.AntiProcrastinationAlarmManager
import com.arinara.fotara.alarm.procrastination.ui.PhotoProofChallengeActivity

class AntiProcrastinationRingtoneService : Service() {

    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP_ALARM) {
            stopSelf()
            return START_NOT_STICKY
        }

        val alarmId = intent?.getLongExtra(AntiProcrastinationAlarmManager.EXTRA_ALARM_ID, 0L) ?: 0L
        val title = intent?.getStringExtra(AntiProcrastinationAlarmManager.EXTRA_ALARM_TITLE) ?: "Urgent Study Alarm"
        val emergencyPin = intent?.getStringExtra(AntiProcrastinationAlarmManager.EXTRA_EMERGENCY_PIN) ?: "1234"
        val targetFolderId = intent?.getLongExtra(AntiProcrastinationAlarmManager.EXTRA_TARGET_FOLDER_ID, -1L) ?: -1L

        createNotificationChannel()

        val fullScreenIntent = Intent(this, PhotoProofChallengeActivity::class.java).apply {
            putExtra(AntiProcrastinationAlarmManager.EXTRA_ALARM_ID, alarmId)
            putExtra(AntiProcrastinationAlarmManager.EXTRA_ALARM_TITLE, title)
            putExtra(AntiProcrastinationAlarmManager.EXTRA_EMERGENCY_PIN, emergencyPin)
            putExtra(AntiProcrastinationAlarmManager.EXTRA_TARGET_FOLDER_ID, targetFolderId)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS)
        }

        val pendingFullScreenIntent = PendingIntent.getActivity(
            this,
            alarmId.toInt(),
            fullScreenIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("URGENT STUDY ALARM")
            .setContentText(title)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setFullScreenIntent(pendingFullScreenIntent, true)
            .setOngoing(true)
            .setAutoCancel(false)
            .build()

        startForeground(NOTIFICATION_ID, notification)

        startAlarmSoundAndVibration()

        // Also launch the full-screen activity immediately
        try {
            startActivity(fullScreenIntent)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to launch PhotoProofChallengeActivity directly from service", e)
        }

        return START_STICKY
    }

    private fun startAlarmSoundAndVibration() {
        // Start looped ringing
        try {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
            audioManager?.setStreamVolume(
                AudioManager.STREAM_ALARM,
                audioManager.getStreamMaxVolume(AudioManager.STREAM_ALARM),
                0
            )

            val alertUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
                ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

            mediaPlayer = MediaPlayer().apply {
                setDataSource(this@AntiProcrastinationRingtoneService, alertUri)
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                isLooping = true
                prepare()
                start()
            }
            Log.i(TAG, "Alarm sound looping initiated")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm sound playback", e)
        }

        // Start looped vibration
        try {
            vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vm = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vm?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            }

            val pattern = longArrayOf(0, 800, 400, 800, 400)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start alarm vibration", e)
        }
    }

    private fun acquireWakeLock() {
        try {
            val powerManager = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "Fotara:AntiProcrastinationWakeLock"
            )?.apply {
                acquire(10 * 60 * 1000L) // Safety cap: 10 minutes
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to acquire WakeLock", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Urgent Anti-Procrastination Alarm",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High-priority alarms requiring photo proof to dismiss"
                lockscreenVisibility = Notification.VISIBILITY_PUBLIC
                setBypassDnd(true)
            }
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            mediaPlayer?.apply {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping MediaPlayer in service", e)
        } finally {
            mediaPlayer = null
        }

        try {
            vibrator?.cancel()
        } catch (e: Exception) {
            Log.e(TAG, "Error cancelling vibration", e)
        } finally {
            vibrator = null
        }

        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing WakeLock", e)
        } finally {
            wakeLock = null
        }

        Log.i(TAG, "AntiProcrastinationRingtoneService stopped and cleaned up")
    }

    companion object {
        private const val TAG = "AntiProcrastinationSvc"
        private const val CHANNEL_ID = "urgent_anti_procrastination_alarm_channel"
        private const val NOTIFICATION_ID = 9001
        const val ACTION_STOP_ALARM = "com.arinara.fotara.alarm.ACTION_STOP_ALARM"

        fun startAlarm(
            context: Context,
            alarmId: Long,
            title: String,
            emergencyPin: String,
            targetFolderId: Long?
        ) {
            val intent = Intent(context, AntiProcrastinationRingtoneService::class.java).apply {
                putExtra(AntiProcrastinationAlarmManager.EXTRA_ALARM_ID, alarmId)
                putExtra(AntiProcrastinationAlarmManager.EXTRA_ALARM_TITLE, title)
                putExtra(AntiProcrastinationAlarmManager.EXTRA_EMERGENCY_PIN, emergencyPin)
                putExtra(AntiProcrastinationAlarmManager.EXTRA_TARGET_FOLDER_ID, targetFolderId ?: -1L)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stopAlarm(context: Context) {
            val intent = Intent(context, AntiProcrastinationRingtoneService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            context.startService(intent)
        }
    }
}
