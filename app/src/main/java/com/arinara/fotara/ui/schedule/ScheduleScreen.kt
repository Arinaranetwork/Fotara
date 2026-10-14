// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.schedule

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.data.repository.ScheduleRepository
import com.arinara.fotara.feature.schedule.model.ClassSchedule
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.components.presets.AppCard
import com.arinara.fotara.ui.components.presets.AppPillChip
import com.arinara.fotara.ui.components.presets.AppScreen
import com.arinara.fotara.ui.components.presets.AppTopBar
import com.arinara.fotara.ui.components.presets.BottomClearanceSpacer
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import java.util.Calendar

@Composable
fun ScheduleScreen(
    scheduleRepository: ScheduleRepository? = null,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val currentDayOfWeek = remember {
        val cal = Calendar.getInstance()
        val dow = cal.get(Calendar.DAY_OF_WEEK)
        // Calendar.MONDAY = 2, Calendar.SUNDAY = 1 -> convert to 1=Mon .. 7=Sun
        if (dow == Calendar.SUNDAY) 7 else dow - 1
    }

    var selectedDay by remember { mutableIntStateOf(currentDayOfWeek) }

    val days = remember {
        listOf(
            1 to "Mon",
            2 to "Tue",
            3 to "Wed",
            4 to "Thu",
            5 to "Fri",
            6 to "Sat",
            7 to "Sun"
        )
    }

    val schedules by scheduleRepository?.getAllSchedules()?.collectAsState(initial = emptyList())
        ?: remember { androidx.compose.runtime.mutableStateOf(emptyList()) }

    val daySchedules = remember(schedules, selectedDay) {
        schedules.filter { it.dayOfWeek == selectedDay }.sortedBy { it.startMinute }
    }

    AppScreen(
        modifier = modifier,
        containerColor = ElectricSlateTokens.BgCanvas
    ) {
        AppTopBar(
            title = "Schedule",
            onBackClick = onBack,
            containerColor = ElectricSlateTokens.BgCanvas
        )

        // Day selector row
        LazyRow(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(days) { (dayIndex, dayLabel) ->
                AppPillChip(
                    label = dayLabel,
                    isSelected = selectedDay == dayIndex,
                    onClick = { selectedDay = dayIndex }
                )
            }
        }

        // Timetable Cards
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (daySchedules.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = ElectricSlateTokens.TextMuted.copy(alpha = 0.5f),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No classes scheduled for this day",
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 14.sp,
                                fontFamily = ElmsSans
                            )
                        }
                    }
                }
            } else {
                items(daySchedules, key = { it.id }) { schedule ->
                    ScheduleItemCard(schedule = schedule)
                }
            }

            item {
                BottomClearanceSpacer(100.dp)
            }
        }
    }
}

@Composable
private fun ScheduleItemCard(
    schedule: ClassSchedule,
    modifier: Modifier = Modifier
) {
    AppCard(
        cornerRadius = 24.dp,
        containerColor = ElectricSlateTokens.SurfacePanel,
        borderColor = ElectricSlateTokens.BorderSubtle,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = schedule.subjectName,
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                fontFamily = ElmsSans,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = schedule.timeRangeFormatted,
                color = ElectricSlateTokens.PrimaryAccent,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = ElmsSans
            )
        }

        if (!schedule.roomName.isNullOrBlank() || !schedule.instructorName.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!schedule.roomName.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = ElectricSlateTokens.TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = schedule.roomName,
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 12.sp,
                            fontFamily = ElmsSans
                        )
                    }
                }

                if (!schedule.instructorName.isNullOrBlank()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = null,
                            tint = ElectricSlateTokens.TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = schedule.instructorName,
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 12.sp,
                            fontFamily = ElmsSans
                        )
                    }
                }
            }
        }
    }
}
