// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.util.ScheduleMath

/**
 * Compact schedule indicator badge docked at the bottom-right corner of card bodies.
 * Never collides with the top-right tag color dot or bottom-left LinkIt glow.
 */
@Composable
fun CardScheduleBadge(
    scheduledAt: Long?,
    alertType: String? = null,
    modifier: Modifier = Modifier
) {
    if (scheduledAt == null || scheduledAt <= 0L) return

    val now = System.currentTimeMillis()
    val isPast = scheduledAt <= now
    val isDueSoon = ScheduleMath.isDueToday(scheduledAt, now) || ScheduleMath.isDueTomorrow(scheduledAt, now)
    val isAlarm = alertType == "ALARM"

    val text = ScheduleMath.formatShortScheduleBadge(scheduledAt, now)
    val textColor = when {
        isPast -> ElectricSlateTokens.TextMuted
        isDueSoon -> ElectricSlateTokens.PrimaryAccent
        else -> ElectricSlateTokens.TextPrimary
    }
    val iconTint = when {
        isPast -> ElectricSlateTokens.TextMuted
        isAlarm -> ElectricSlateTokens.PrimaryAccent
        else -> ElectricSlateTokens.TextPrimary
    }

    Box(
        modifier = modifier
            .background(ElectricSlateTokens.SurfaceElevated.copy(alpha = 0.90f), RoundedCornerShape(999.dp))
            .border(
                1.dp,
                if (isDueSoon && !isPast) ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.5f) else ElectricSlateTokens.BorderSubtle,
                RoundedCornerShape(999.dp)
            )
            .padding(horizontal = 6.dp, vertical = 2.5.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = if (isAlarm) Icons.Default.Alarm else Icons.Default.Schedule,
                contentDescription = "Schedule",
                tint = iconTint,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(3.5.dp))
            Text(
                text = text,
                color = textColor,
                fontSize = 10.sp,
                fontWeight = if (isDueSoon && !isPast) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}
