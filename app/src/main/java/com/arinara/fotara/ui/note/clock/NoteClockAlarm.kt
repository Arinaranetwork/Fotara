// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.clock

import java.util.UUID

/**
 * Academic alarm model managed directly within the Clock screen.
 */
data class NoteClockAlarm(
    val id: String = UUID.randomUUID().toString(),
    val hour: Int,
    val minute: Int,
    val label: String,
    val isEnabled: Boolean = true,
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun getFormattedTime(): String {
        return String.format(java.util.Locale.US, "%02d:%02d", hour, minute)
    }
}
