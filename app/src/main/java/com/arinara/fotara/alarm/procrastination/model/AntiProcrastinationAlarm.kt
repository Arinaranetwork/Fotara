// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.alarm.procrastination.model

/**
 * Model representing an urgent anti-procrastination study alarm requiring photo proof to dismiss.
 */
data class AntiProcrastinationAlarm(
    val id: Long,
    val title: String,
    val targetFolderId: Long? = null,
    val triggerAtMillis: Long,
    val emergencyPin: String = "1234",
    val isEnabled: Boolean = true
) {
    init {
        require(title.isNotBlank()) { "Alarm title must not be blank" }
        require(emergencyPin.length in 4..6) { "Emergency PIN must be 4 to 6 characters" }
    }
}
