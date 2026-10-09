// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.alarm.procrastination

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.arinara.fotara.alarm.procrastination.model.AntiProcrastinationAlarm
import com.arinara.fotara.alarm.procrastination.receiver.AntiProcrastinationAlarmReceiver

class AntiProcrastinationAlarmManager(
    private val context: Context
) {

    private val alarmManager: AlarmManager? by lazy {
        context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
    }

    /**
     * Schedules an exact anti-procrastination study alarm.
     */
    fun scheduleAlarm(alarm: AntiProcrastinationAlarm): Boolean {
        if (!alarm.isEnabled) {
            cancelAlarm(alarm.id)
            return false
        }

        val manager = alarmManager
        if (manager == null) {
            Log.e(TAG, "AlarmManager system service is unavailable")
            return false
        }

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (!manager.canScheduleExactAlarms()) {
                    Log.w(TAG, "SCHEDULE_EXACT_ALARM permission not granted, falling back to inexact alarm")
                    manager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarm.triggerAtMillis,
                        createPendingIntent(alarm)
                    )
                    return true
                }
            }

            manager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                alarm.triggerAtMillis,
                createPendingIntent(alarm)
            )
            Log.i(TAG, "Exact anti-procrastination alarm scheduled: id=${alarm.id}, at=${alarm.triggerAtMillis}")
            return true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to schedule exact alarm for id=${alarm.id}", e)
            return false
        }
    }

    /**
     * Cancels a previously scheduled alarm.
     */
    fun cancelAlarm(alarmId: Long) {
        val manager = alarmManager ?: return
        try {
            val intent = Intent(context, AntiProcrastinationAlarmReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                alarmId.toInt(),
                intent,
                PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
            )
            if (pendingIntent != null) {
                manager.cancel(pendingIntent)
                pendingIntent.cancel()
                Log.i(TAG, "Cancelled anti-procrastination alarm: id=$alarmId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to cancel alarm: id=$alarmId", e)
        }
    }

    private fun createPendingIntent(alarm: AntiProcrastinationAlarm): PendingIntent {
        val intent = Intent(context, AntiProcrastinationAlarmReceiver::class.java).apply {
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_TITLE, alarm.title)
            putExtra(EXTRA_TARGET_FOLDER_ID, alarm.targetFolderId ?: -1L)
            putExtra(EXTRA_EMERGENCY_PIN, alarm.emergencyPin)
        }
        return PendingIntent.getBroadcast(
            context,
            alarm.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        private const val TAG = "AntiProcrastinationAM"
        const val EXTRA_ALARM_ID = "extra_anti_procrastination_alarm_id"
        const val EXTRA_ALARM_TITLE = "extra_anti_procrastination_alarm_title"
        const val EXTRA_TARGET_FOLDER_ID = "extra_anti_procrastination_target_folder_id"
        const val EXTRA_EMERGENCY_PIN = "extra_anti_procrastination_emergency_pin"
    }
}
