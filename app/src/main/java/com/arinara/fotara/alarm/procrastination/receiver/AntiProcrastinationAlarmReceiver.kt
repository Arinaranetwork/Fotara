// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.alarm.procrastination.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.arinara.fotara.alarm.procrastination.AntiProcrastinationAlarmManager
import com.arinara.fotara.alarm.procrastination.service.AntiProcrastinationRingtoneService

class AntiProcrastinationAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val alarmId = intent.getLongExtra(AntiProcrastinationAlarmManager.EXTRA_ALARM_ID, 0L)
        val title = intent.getStringExtra(AntiProcrastinationAlarmManager.EXTRA_ALARM_TITLE) ?: "Urgent Study Alarm"
        val emergencyPin = intent.getStringExtra(AntiProcrastinationAlarmManager.EXTRA_EMERGENCY_PIN) ?: "1234"
        val targetFolderId = intent.getLongExtra(AntiProcrastinationAlarmManager.EXTRA_TARGET_FOLDER_ID, -1L)

        Log.i(TAG, "Anti-procrastination alarm fired! id=$alarmId, title='$title'")

        try {
            AntiProcrastinationRingtoneService.startAlarm(
                context = context,
                alarmId = alarmId,
                title = title,
                emergencyPin = emergencyPin,
                targetFolderId = if (targetFolderId == -1L) null else targetFolderId
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start AntiProcrastinationRingtoneService from receiver", e)
        }
    }

    companion object {
        private const val TAG = "AntiProcrastinationRecv"
    }
}
