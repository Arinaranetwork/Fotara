// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.clock

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar

object NoteClockAlarmManager {

    private const val PREFS_NAME = "fotara_note_clock_alarms"
    private const val KEY_ALARMS = "saved_alarms_json"

    const val ACTION_ALARM_TRIGGER = "com.arinara.fotara.ACTION_NOTE_CLOCK_ALARM"
    const val EXTRA_ALARM_ID = "extra_alarm_id"
    const val EXTRA_ALARM_LABEL = "extra_alarm_label"
    const val EXTRA_ALARM_HOUR = "extra_alarm_hour"
    const val EXTRA_ALARM_MINUTE = "extra_alarm_minute"

    fun loadAlarms(context: Context): List<NoteClockAlarm> {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jsonStr = prefs.getString(KEY_ALARMS, null) ?: return emptyList()
        val list = mutableListOf<NoteClockAlarm>()
        try {
            val array = JSONArray(jsonStr)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                list.add(
                    NoteClockAlarm(
                        id = obj.getString("id"),
                        hour = obj.getInt("hour"),
                        minute = obj.getInt("minute"),
                        label = obj.getString("label"),
                        isEnabled = obj.optBoolean("isEnabled", true),
                        createdAtMillis = obj.optLong("createdAtMillis", System.currentTimeMillis())
                    )
                )
            }
        } catch (_: Exception) {}
        return list.sortedBy { it.hour * 60 + it.minute }
    }

    fun saveAlarms(context: Context, alarms: List<NoteClockAlarm>) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val array = JSONArray()
        for (alarm in alarms) {
            val obj = JSONObject().apply {
                put("id", alarm.id)
                put("hour", alarm.hour)
                put("minute", alarm.minute)
                put("label", alarm.label)
                put("isEnabled", alarm.isEnabled)
                put("createdAtMillis", alarm.createdAtMillis)
            }
            array.put(obj)
        }
        prefs.edit().putString(KEY_ALARMS, array.toString()).apply()
    }

    fun addAlarm(context: Context, alarm: NoteClockAlarm): List<NoteClockAlarm> {
        val current = loadAlarms(context).toMutableList()
        current.removeAll { it.id == alarm.id }
        current.add(alarm)
        saveAlarms(context, current)
        if (alarm.isEnabled) {
            scheduleAlarmWithSystem(context, alarm)
        }
        return current.sortedBy { it.hour * 60 + it.minute }
    }

    fun toggleAlarm(context: Context, alarmId: String, isEnabled: Boolean): List<NoteClockAlarm> {
        val current = loadAlarms(context).toMutableList()
        val index = current.indexOfFirst { it.id == alarmId }
        if (index != -1) {
            val updated = current[index].copy(isEnabled = isEnabled)
            current[index] = updated
            saveAlarms(context, current)
            if (isEnabled) {
                scheduleAlarmWithSystem(context, updated)
            } else {
                cancelAlarmWithSystem(context, alarmId)
            }
        }
        return current.sortedBy { it.hour * 60 + it.minute }
    }

    fun deleteAlarm(context: Context, alarmId: String): List<NoteClockAlarm> {
        val current = loadAlarms(context).toMutableList()
        current.removeAll { it.id == alarmId }
        saveAlarms(context, current)
        cancelAlarmWithSystem(context, alarmId)
        return current.sortedBy { it.hour * 60 + it.minute }
    }

    fun calculateNextTriggerMillis(hour: Int, minute: Int, nowMillis: Long = System.currentTimeMillis()): Long {
        val now = Calendar.getInstance().apply { timeInMillis = nowMillis }
        val target = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        if (target.timeInMillis <= now.timeInMillis) {
            target.add(Calendar.DAY_OF_YEAR, 1)
        }
        return target.timeInMillis
    }

    fun scheduleAlarmWithSystem(context: Context, alarm: NoteClockAlarm) {
        val triggerAtMillis = calculateNextTriggerMillis(alarm.hour, alarm.minute)
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NoteClockAlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
            putExtra(EXTRA_ALARM_ID, alarm.id)
            putExtra(EXTRA_ALARM_LABEL, alarm.label)
            putExtra(EXTRA_ALARM_HOUR, alarm.hour)
            putExtra(EXTRA_ALARM_MINUTE, alarm.minute)
        }
        val requestCode = alarm.id.hashCode()
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } else {
                am.setExact(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            }
        } catch (_: Exception) {
            try {
                am.set(AlarmManager.RTC_WAKEUP, triggerAtMillis, pi)
            } catch (_: Exception) {}
        }
    }

    fun cancelAlarmWithSystem(context: Context, alarmId: String) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, NoteClockAlarmReceiver::class.java).apply {
            action = ACTION_ALARM_TRIGGER
        }
        val requestCode = alarmId.hashCode()
        val pi = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pi != null) {
            am.cancel(pi)
            pi.cancel()
        }
    }
}
