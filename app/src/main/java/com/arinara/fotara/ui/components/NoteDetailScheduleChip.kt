// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.util.ScheduleMath

private val ChipBg = Color(0xFF0F1535)
private val ChipBorder = Color(0xFF28325E)
private val AccentGold = Color(0xFFF77F00)
private val TabCream = Color(0xFFEAE3D2)
private val TextMuted = Color(0xFF8E9AAF)

/**
 * Schedule chip placed directly under note titles on detail screens.
 * Shows schedule title (if set) and formatted date/time.
 * Clicking triggers editing of the schedule.
 */
@Composable
fun NoteDetailScheduleChip(
    scheduledAt: Long?,
    alertType: String? = null,
    scheduleTitle: String? = null,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (scheduledAt == null || scheduledAt <= 0L) return

    val now = System.currentTimeMillis()
    val isPast = scheduledAt <= now
    val isDueSoon = ScheduleMath.isDueToday(scheduledAt, now) || ScheduleMath.isDueTomorrow(scheduledAt, now)
    val isAlarm = alertType == "ALARM"

    val timeText = ScheduleMath.formatShortScheduleBadge(scheduledAt, now)
    val displayText = if (!scheduleTitle.isNullOrBlank()) {
        "$scheduleTitle ($timeText)"
    } else {
        timeText
    }

    val textColor = when {
        isPast -> TextMuted
        isDueSoon -> AccentGold
        else -> TabCream
    }
    val iconTint = when {
        isPast -> TextMuted
        isAlarm -> AccentGold
        else -> TabCream
    }

    Box(
        modifier = modifier
            .background(ChipBg, RoundedCornerShape(8.dp))
            .border(
                width = 1.dp,
                color = if (isDueSoon && !isPast) AccentGold.copy(alpha = 0.6f) else ChipBorder,
                shape = RoundedCornerShape(8.dp)
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isAlarm) Icons.Default.Alarm else Icons.Default.Schedule,
                contentDescription = "Schedule Details",
                tint = iconTint,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = displayText,
                color = textColor,
                fontSize = 11.sp,
                fontWeight = if (isDueSoon && !isPast) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
