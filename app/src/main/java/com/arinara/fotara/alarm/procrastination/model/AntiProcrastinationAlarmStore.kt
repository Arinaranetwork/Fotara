// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.alarm.procrastination.model

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import org.json.JSONArray
import org.json.JSONObject

/**
 * Lightweight persistence store for AntiProcrastination alarms.
 */
class AntiProcrastinationAlarmStore(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    @Synchronized
    fun getAllAlarms(): List<AntiProcrastinationAlarm> {
        val jsonString = prefs.getString(KEY_ALARMS, null) ?: return emptyList()
        val alarms = mutableListOf<AntiProcrastinationAlarm>()
        try {
            val array = JSONArray(jsonString)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val alarm = AntiProcrastinationAlarm(
                    id = obj.getLong("id"),
                    title = obj.getString("title"),
                    targetFolderId = if (obj.has("targetFolderId") && !obj.isNull("targetFolderId")) obj.getLong("targetFolderId") else null,
                    triggerAtMillis = obj.getLong("triggerAtMillis"),
                    emergencyPin = obj.optString("emergencyPin", "1234"),
                    isEnabled = obj.optBoolean("isEnabled", true)
                )
                alarms.add(alarm)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse anti-procrastination alarms from preferences", e)
        }
        return alarms.sortedBy { it.triggerAtMillis }
    }

    @Synchronized
    fun saveAlarm(alarm: AntiProcrastinationAlarm) {
        val current = getAllAlarms().filterNot { it.id == alarm.id }.toMutableList()
        current.add(alarm)
        saveList(current)
    }

    @Synchronized
    fun deleteAlarm(alarmId: Long) {
        val updated = getAllAlarms().filterNot { it.id == alarmId }
        saveList(updated)
    }

    @Synchronized
    fun toggleAlarm(alarmId: Long, isEnabled: Boolean): AntiProcrastinationAlarm? {
        val list = getAllAlarms().toMutableList()
        val index = list.indexOfFirst { it.id == alarmId }
        if (index == -1) return null

        val updated = list[index].copy(isEnabled = isEnabled)
        list[index] = updated
        saveList(list)
        return updated
    }

    private fun saveList(alarms: List<AntiProcrastinationAlarm>) {
        try {
            val array = JSONArray()
            for (alarm in alarms) {
                val obj = JSONObject().apply {
                    put("id", alarm.id)
                    put("title", alarm.title)
                    if (alarm.targetFolderId != null) put("targetFolderId", alarm.targetFolderId)
                    put("triggerAtMillis", alarm.triggerAtMillis)
                    put("emergencyPin", alarm.emergencyPin)
                    put("isEnabled", alarm.isEnabled)
                }
                array.put(obj)
            }
            prefs.edit().putString(KEY_ALARMS, array.toString()).apply()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to persist anti-procrastination alarms", e)
        }
    }

    companion object {
        private const val TAG = "AntiProcrastinationStore"
        private const val PREFS_NAME = "anti_procrastination_alarms_pref"
        private const val KEY_ALARMS = "saved_anti_procrastination_alarms"
    }
}
