// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.DockSlatePill
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.MidnightCardOutline
import com.arinara.fotara.theme.MidnightNavy
import com.arinara.fotara.theme.MidnightSurface
import com.arinara.fotara.theme.TagAmber
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import com.arinara.fotara.util.ScheduleAlertType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun ScheduleBadge(
    scheduledAt: Long?,
    alertType: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (scheduledAt == null || scheduledAt <= 0L) return

    val now = System.currentTimeMillis()
    val isOverdue = scheduledAt < now
    val isAlarm = alertType == ScheduleAlertType.ALARM.name

    val badgeColor = if (isOverdue) TagCrimson else FolderTabCream
    val badgeBg = if (isOverdue) TagCrimson.copy(alpha = 0.15f) else DockSlatePill.copy(alpha = 0.7f)
    val badgeBorder = if (isOverdue) TagCrimson.copy(alpha = 0.6f) else MidnightCardOutline

    val sdf = remember(scheduledAt) {
        val calSchedule = Calendar.getInstance().apply { timeInMillis = scheduledAt }
        val calNow = Calendar.getInstance()
        val isToday = calSchedule.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calSchedule.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)

        calNow.add(Calendar.DAY_OF_YEAR, 1)
        val isTomorrow = calSchedule.get(Calendar.YEAR) == calNow.get(Calendar.YEAR) &&
                calSchedule.get(Calendar.DAY_OF_YEAR) == calNow.get(Calendar.DAY_OF_YEAR)

        val timePart = SimpleDateFormat("h:mm a", Locale.US).format(Date(scheduledAt))

        when {
            isToday -> "Today, $timePart"
            isTomorrow -> "Tomorrow, $timePart"
            else -> SimpleDateFormat("MMM d, h:mm a", Locale.US).format(Date(scheduledAt))
        }
    }

    Surface(
        color = badgeBg,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, badgeBorder),
        modifier = modifier.then(
            if (onClick != null) Modifier.clickable { onClick() } else Modifier
        )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
        ) {
            Icon(
                imageVector = if (isAlarm) Icons.Default.Alarm else Icons.Default.AccessTime,
                contentDescription = if (isAlarm) "Alarm Alert" else "Reminder",
                tint = badgeColor,
                modifier = Modifier.size(11.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = if (isOverdue) "Overdue: $sdf" else sdf,
                style = TextStyle(
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleNoteDialog(
    noteTitle: String,
    initialScheduledAt: Long? = null,
    initialAlertType: ScheduleAlertType = ScheduleAlertType.NOTIFICATION,
    onDismiss: () -> Unit,
    onSaveSchedule: (scheduledAt: Long, alertType: ScheduleAlertType) -> Unit,
    onClearSchedule: () -> Unit
) {
    val initialCal = remember(initialScheduledAt) {
        Calendar.getInstance().apply {
            if (initialScheduledAt != null && initialScheduledAt > System.currentTimeMillis()) {
                timeInMillis = initialScheduledAt
            } else {
                add(Calendar.HOUR_OF_DAY, 1)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
        }
    }

    var selectedCal by remember { mutableStateOf(initialCal) }
    var selectedAlertType by remember { mutableStateOf(initialAlertType) }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    val dateStr = remember(selectedCal.timeInMillis) {
        SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).format(selectedCal.time)
    }
    val timeStr = remember(selectedCal.timeInMillis) {
        SimpleDateFormat("h:mm a", Locale.US).format(selectedCal.time)
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedCal.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { dateMs ->
                            val updatedCal = Calendar.getInstance().apply {
                                timeInMillis = dateMs
                                set(Calendar.HOUR_OF_DAY, selectedCal.get(Calendar.HOUR_OF_DAY))
                                set(Calendar.MINUTE, selectedCal.get(Calendar.MINUTE))
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                            selectedCal = updatedCal
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Done", color = FolderTabCream)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(
                containerColor = MidnightSurface
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = MidnightSurface,
                    titleContentColor = TextPrimary,
                    headlineContentColor = TextPrimary,
                    weekdayContentColor = TextSecondary,
                    subheadContentColor = TextSecondary,
                    yearContentColor = TextPrimary,
                    currentYearContentColor = FolderTabCream,
                    selectedYearContentColor = MidnightNavy,
                    selectedYearContainerColor = FolderTabCream,
                    dayContentColor = TextPrimary,
                    selectedDayContentColor = MidnightNavy,
                    selectedDayContainerColor = FolderTabCream,
                    todayContentColor = TagAmber,
                    todayDateBorderColor = TagAmber
                )
            )
        }
    }

    if (showTimePicker) {
        val timePickerState = rememberTimePickerState(
            initialHour = selectedCal.get(Calendar.HOUR_OF_DAY),
            initialMinute = selectedCal.get(Calendar.MINUTE),
            is24Hour = false
        )
        AlertDialog(
            onDismissRequest = { showTimePicker = false },
            containerColor = MidnightSurface,
            title = {
                Text("Select Alert Time", color = TextPrimary, fontWeight = FontWeight.Bold, fontSize = 17.sp)
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = MidnightNavy,
                            clockDialSelectedContentColor = MidnightNavy,
                            clockDialUnselectedContentColor = TextSecondary,
                            selectorColor = FolderTabCream,
                            containerColor = MidnightSurface,
                            periodSelectorBorderColor = MidnightCardOutline,
                            periodSelectorSelectedContainerColor = FolderTabCream,
                            periodSelectorUnselectedContainerColor = DockSlatePill,
                            periodSelectorSelectedContentColor = MidnightNavy,
                            periodSelectorUnselectedContentColor = TextPrimary,
                            timeSelectorSelectedContainerColor = FolderTabCream,
                            timeSelectorUnselectedContainerColor = DockSlatePill,
                            timeSelectorSelectedContentColor = MidnightNavy,
                            timeSelectorUnselectedContentColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val updatedCal = Calendar.getInstance().apply {
                            timeInMillis = selectedCal.timeInMillis
                            set(Calendar.HOUR_OF_DAY, timePickerState.hour)
                            set(Calendar.MINUTE, timePickerState.minute)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        selectedCal = updatedCal
                        showTimePicker = false
                    }
                ) {
                    Text("Done", color = FolderTabCream)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MidnightSurface,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = "Schedule Study Note",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = noteTitle,
                    color = TextSecondary,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Date picker trigger button
                Text(
                    text = "DATE",
                    style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = DockSlatePill.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MidnightCardOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            tint = FolderTabCream,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = dateStr,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Time picker trigger button
                Text(
                    text = "TIME",
                    style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Surface(
                    color = DockSlatePill.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, MidnightCardOutline),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showTimePicker = true }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccessTime,
                            contentDescription = null,
                            tint = FolderTabCream,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = timeStr,
                            color = TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Alert Type selector
                Text(
                    text = "ALERT TYPE",
                    style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val notifSelected = selectedAlertType == ScheduleAlertType.NOTIFICATION
                    Surface(
                        color = if (notifSelected) FolderTabCream else DockSlatePill.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (notifSelected) FolderTabCream else MidnightCardOutline),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedAlertType = ScheduleAlertType.NOTIFICATION }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = null,
                                tint = if (notifSelected) MidnightNavy else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Reminder",
                                color = if (notifSelected) MidnightNavy else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (notifSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }

                    val alarmSelected = selectedAlertType == ScheduleAlertType.ALARM
                    Surface(
                        color = if (alarmSelected) FolderTabCream else DockSlatePill.copy(alpha = 0.4f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, if (alarmSelected) FolderTabCream else MidnightCardOutline),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedAlertType = ScheduleAlertType.ALARM }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.padding(vertical = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = if (alarmSelected) MidnightNavy else TextSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Urgent Alarm",
                                color = if (alarmSelected) MidnightNavy else TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = if (alarmSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSaveSchedule(selectedCal.timeInMillis, selectedAlertType)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = FolderTabCream,
                    contentColor = MidnightNavy
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Set Schedule", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row {
                if (initialScheduledAt != null && initialScheduledAt > 0L) {
                    TextButton(
                        onClick = {
                            onClearSchedule()
                            onDismiss()
                        }
                    ) {
                        Text("Remove", color = TagCrimson)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = TextSecondary)
                }
            }
        }
    )
}
