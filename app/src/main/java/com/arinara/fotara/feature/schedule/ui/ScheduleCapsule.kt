// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.schedule.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bedtime
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.feature.schedule.engine.ScheduleCapsuleState
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.TextPrimary

/**
 * 1-Line Dynamic Schedule Capsule.
 * Location Invariant: Positioned strictly below WorkspaceTabBar and immediately above NotesFilterChipsRow.
 * Height: Compact capsule (~36dp).
 * Skill Rule U-16: Uses tinted vector drawables instead of raw unicode emoji.
 */
@Composable
fun ScheduleCapsule(
    state: ScheduleCapsuleState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(36.dp)
            .clip(CircleShape)
            .background(HomeCardSurface)
            .border(1.dp, HomeCardBorder, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Leading icon / indicator dot based on state
                when (state) {
                    is ScheduleCapsuleState.ActiveNow -> {
                        // Green active dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                    }
                    is ScheduleCapsuleState.UpcomingToday -> {
                        // Amber upcoming dot
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(Color(0xFFF59E0B))
                        )
                    }
                    is ScheduleCapsuleState.RolloverTomorrow -> {
                        // Moon / Bedtime vector icon for tomorrow rollover
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Next-Day Prep",
                            tint = Color(0xFF818CF8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    is ScheduleCapsuleState.TodayFinished -> {
                        // Blue completed icon
                        Icon(
                            imageVector = Icons.Default.Bedtime,
                            contentDescription = "Classes Finished",
                            tint = Color(0xFF60A5FA),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    is ScheduleCapsuleState.Empty -> {
                        // Calendar icon
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Class Schedule",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // Headline text
                val headlineText = when (state) {
                    is ScheduleCapsuleState.ActiveNow -> state.displayHeadline
                    is ScheduleCapsuleState.UpcomingToday -> state.displayHeadline
                    is ScheduleCapsuleState.RolloverTomorrow -> state.displayHeadline
                    is ScheduleCapsuleState.TodayFinished -> state.displayHeadline
                    is ScheduleCapsuleState.Empty -> state.message
                }

                Text(
                    text = headlineText,
                    color = TextPrimary,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Trailing down chevron
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Open Class Schedule",
                tint = Color(0xFF94A3B8),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}
