// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.util.NoteScheduleManager
import com.arinara.fotara.util.ScheduleAlertType
import com.arinara.fotara.util.ScheduleMath
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

    val badgeColor = if (isOverdue) ElectricSlateTokens.Destructive else ElectricSlateTokens.PrimaryAccent
    val badgeBg = if (isOverdue) ElectricSlateTokens.Destructive.copy(alpha = 0.15f) else ElectricSlateTokens.TrackInactive
    val badgeBorder = if (isOverdue) ElectricSlateTokens.Destructive.copy(alpha = 0.6f) else ElectricSlateTokens.BorderSubtle

    val text = ScheduleMath.formatShortScheduleBadge(scheduledAt, now)

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
                text = if (isOverdue) "Overdue: $text" else text,
                style = TextStyle(
                    color = badgeColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

/**
 * Universal schedule dialog for study notes of all types.
 * Supports date, time, custom title, alert style, past validation, and permission flow.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleNoteDialog(
    noteTitle: String,
    initialScheduledAt: Long? = null,
    initialAlertType: ScheduleAlertType = ScheduleAlertType.NOTIFICATION,
    initialScheduleTitle: String? = null,
    onDismiss: () -> Unit,
    onSaveSchedule: (scheduledAt: Long, alertType: ScheduleAlertType, scheduleTitle: String?) -> Unit,
    onClearSchedule: () -> Unit
) {
    val context = LocalContext.current
    val scheduleManager = remember { NoteScheduleManager(context) }

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
    var scheduleName by remember { mutableStateOf(initialScheduleTitle ?: "") }
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }
    var showPermissionDialog by remember { mutableStateOf(false) }

    val now = System.currentTimeMillis()
    val isPast = selectedCal.timeInMillis <= now

    val dateStr = remember(selectedCal.timeInMillis) {
        SimpleDateFormat("EEE, MMM d, yyyy", Locale.US).format(selectedCal.time)
    }
    val timeStr = remember(selectedCal.timeInMillis) {
        SimpleDateFormat("h:mm a", Locale.US).format(selectedCal.time)
    }

    // Permission explanation dialog
    if (showPermissionDialog) {
        AlertDialog(
            onDismissRequest = { showPermissionDialog = false },
            containerColor = ElectricSlateTokens.SurfaceElevated,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = ElectricSlateTokens.PrimaryAccent,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Allow Timely Study Alerts",
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "To ensure you never miss a study deadline, Fotara needs permission to schedule alarms and post alerts even when your device is locked or in battery saving mode.",
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 14.sp,
                        lineHeight = 18.sp
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !scheduleManager.canScheduleExact()) {
                        Text(
                            text = "• Exact Alarm Permission: Required for to-the-minute alarm precision.",
                            color = ElectricSlateTokens.PrimaryAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (!scheduleManager.canPostNotifications()) {
                        Text(
                            text = "• Notification Permission: Required to display study reminders on your screen.",
                            color = ElectricSlateTokens.PrimaryAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                AppButton(
                    text = "Open Settings",
                    onClick = {
                        showPermissionDialog = false
                        // Launch appropriate settings screen
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !scheduleManager.canScheduleExact()) {
                            try {
                                val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                                    data = Uri.parse("package:${context.packageName}")
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {
                                openAppSettings(context)
                            }
                        } else {
                            openAppSettings(context)
                        }
                    },
                    variant = AppButtonVariant.PRIMARY
                )
            },
            dismissButton = {
                AppButton(
                    text = "Continue Anyway",
                    onClick = {
                        showPermissionDialog = false
                        // Proceed anyway with available capabilities
                        onSaveSchedule(
                            selectedCal.timeInMillis,
                            selectedAlertType,
                            scheduleName.trim().ifBlank { null }
                        )
                        onDismiss()
                    },
                    variant = AppButtonVariant.SECONDARY
                )
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = selectedCal.timeInMillis
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                AppButton(
                    text = "Done",
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
                    },
                    variant = AppButtonVariant.PRIMARY
                )
            },
            dismissButton = {
                AppButton(
                    text = "Cancel",
                    onClick = { showDatePicker = false },
                    variant = AppButtonVariant.SECONDARY
                )
            },
            colors = DatePickerDefaults.colors(
                containerColor = ElectricSlateTokens.SurfaceElevated
            )
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = ElectricSlateTokens.SurfaceElevated,
                    titleContentColor = ElectricSlateTokens.TextPrimary,
                    headlineContentColor = ElectricSlateTokens.TextPrimary,
                    weekdayContentColor = ElectricSlateTokens.TextMuted,
                    subheadContentColor = ElectricSlateTokens.TextMuted,
                    yearContentColor = ElectricSlateTokens.TextPrimary,
                    currentYearContentColor = ElectricSlateTokens.PrimaryAccent,
                    selectedYearContentColor = ElectricSlateTokens.TextPrimary,
                    selectedYearContainerColor = ElectricSlateTokens.PrimaryAccent,
                    dayContentColor = ElectricSlateTokens.TextPrimary,
                    selectedDayContentColor = ElectricSlateTokens.TextPrimary,
                    selectedDayContainerColor = ElectricSlateTokens.PrimaryAccent,
                    todayContentColor = ElectricSlateTokens.PrimaryAccent,
                    todayDateBorderColor = ElectricSlateTokens.PrimaryAccent
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
            containerColor = ElectricSlateTokens.SurfaceElevated,
            title = {
                Text("Select Alert Time", color = ElectricSlateTokens.TextPrimary, fontWeight = FontWeight.Bold, fontSize = 16.sp)
            },
            text = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TimePicker(
                        state = timePickerState,
                        colors = TimePickerDefaults.colors(
                            clockDialColor = ElectricSlateTokens.SurfacePanel,
                            clockDialSelectedContentColor = ElectricSlateTokens.TextPrimary,
                            clockDialUnselectedContentColor = ElectricSlateTokens.TextMuted,
                            selectorColor = ElectricSlateTokens.PrimaryAccent,
                            containerColor = ElectricSlateTokens.SurfaceElevated,
                            periodSelectorBorderColor = ElectricSlateTokens.BorderSubtle,
                            periodSelectorSelectedContainerColor = ElectricSlateTokens.PrimaryAccent,
                            periodSelectorUnselectedContainerColor = ElectricSlateTokens.TrackInactive,
                            periodSelectorSelectedContentColor = ElectricSlateTokens.TextPrimary,
                            periodSelectorUnselectedContentColor = ElectricSlateTokens.TextPrimary,
                            timeSelectorSelectedContainerColor = ElectricSlateTokens.PrimaryAccent,
                            timeSelectorUnselectedContainerColor = ElectricSlateTokens.TrackInactive,
                            timeSelectorSelectedContentColor = ElectricSlateTokens.TextPrimary,
                            timeSelectorUnselectedContentColor = ElectricSlateTokens.TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                AppButton(
                    text = "Done",
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
                    },
                    variant = AppButtonVariant.PRIMARY
                )
            },
            dismissButton = {
                AppButton(
                    text = "Cancel",
                    onClick = { showTimePicker = false },
                    variant = AppButtonVariant.SECONDARY
                )
            }
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = ElectricSlateTokens.SurfaceElevated,
        shape = RoundedCornerShape(20.dp),
        title = {
            Column {
                Text(
                    text = "Schedule Note",
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = noteTitle,
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 14.sp,
                    maxLines = 1
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Custom Schedule Title field
                Column {
                    Text(
                        text = "SCHEDULE NAME",
                        style = TextStyle(color = ElectricSlateTokens.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = scheduleName,
                        onValueChange = { scheduleName = it },
                        placeholder = { Text("e.g. Quiz Preparation, Chapter 3", color = ElectricSlateTokens.TextMuted, fontSize = 14.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = ElectricSlateTokens.TextPrimary,
                            unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                            focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                            unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                            focusedContainerColor = ElectricSlateTokens.BgCanvas,
                            unfocusedContainerColor = ElectricSlateTokens.BgCanvas,
                            cursorColor = ElectricSlateTokens.PrimaryAccent
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 2. Date picker trigger button
                Column {
                    Text(
                        text = "DATE",
                        style = TextStyle(color = ElectricSlateTokens.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = ElectricSlateTokens.TrackInactive,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
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
                                tint = ElectricSlateTokens.PrimaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = dateStr,
                                color = ElectricSlateTokens.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // 3. Time picker trigger button
                Column {
                    Text(
                        text = "TIME",
                        style = TextStyle(color = ElectricSlateTokens.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = ElectricSlateTokens.TrackInactive,
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
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
                                tint = ElectricSlateTokens.PrimaryAccent,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = timeStr,
                                color = ElectricSlateTokens.TextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Inline past validation warning
                if (isPast) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(ElectricSlateTokens.Destructive.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .border(1.dp, ElectricSlateTokens.Destructive.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = ElectricSlateTokens.Destructive,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Selected time is in the past. Choose a future time.",
                            color = ElectricSlateTokens.Destructive,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                // Quick Presets
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    QuickPresetChip(
                        label = "+1 Hour",
                        onClick = {
                            selectedCal = Calendar.getInstance().apply {
                                add(Calendar.HOUR_OF_DAY, 1)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                        }
                    )
                    QuickPresetChip(
                        label = "Tomorrow 9 AM",
                        onClick = {
                            selectedCal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 1)
                                set(Calendar.HOUR_OF_DAY, 9)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                        }
                    )
                    QuickPresetChip(
                        label = "In 2 Days",
                        onClick = {
                            selectedCal = Calendar.getInstance().apply {
                                add(Calendar.DAY_OF_YEAR, 2)
                                set(Calendar.HOUR_OF_DAY, 9)
                                set(Calendar.MINUTE, 0)
                                set(Calendar.SECOND, 0)
                                set(Calendar.MILLISECOND, 0)
                            }
                        }
                    )
                }

                // 4. Alert Type selector (Notification vs Alarm)
                Column {
                    Text(
                        text = "ALERT STYLE",
                        style = TextStyle(color = ElectricSlateTokens.TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val notifSelected = selectedAlertType == ScheduleAlertType.NOTIFICATION
                        Surface(
                            color = if (notifSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TrackInactive,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (notifSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedAlertType = ScheduleAlertType.NOTIFICATION }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 9.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = if (notifSelected) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Notification",
                                    color = if (notifSelected) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = if (notifSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        val alarmSelected = selectedAlertType == ScheduleAlertType.ALARM
                        Surface(
                            color = if (alarmSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TrackInactive,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, if (alarmSelected) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { selectedAlertType = ScheduleAlertType.ALARM }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(vertical = 9.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = if (alarmSelected) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Alarm Ring",
                                    color = if (alarmSelected) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted,
                                    fontSize = 12.sp,
                                    fontWeight = if (alarmSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                text = "Save",
                onClick = {
                    if (isPast) return@AppButton
                    val needsExact = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !scheduleManager.canScheduleExact()
                    val needsNotif = !scheduleManager.canPostNotifications()

                    if (needsExact || needsNotif) {
                        showPermissionDialog = true
                    } else {
                        onSaveSchedule(
                            selectedCal.timeInMillis,
                            selectedAlertType,
                            scheduleName.trim().ifBlank { null }
                        )
                        onDismiss()
                    }
                },
                enabled = !isPast,
                variant = AppButtonVariant.PRIMARY
            )
        },
        dismissButton = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (initialScheduledAt != null && initialScheduledAt > 0L) {
                    AppButton(
                        text = "Remove",
                        onClick = {
                            onClearSchedule()
                            onDismiss()
                        },
                        variant = AppButtonVariant.DESTRUCTIVE
                    )
                }
                AppButton(
                    text = "Cancel",
                    onClick = onDismiss,
                    variant = AppButtonVariant.SECONDARY
                )
            }
        }
    )
}

/**
 * Backwards compatibility overload for callers providing the 2-parameter lambda.
 */
@Composable
fun ScheduleNoteDialog(
    noteTitle: String,
    initialScheduledAt: Long? = null,
    initialAlertType: ScheduleAlertType = ScheduleAlertType.NOTIFICATION,
    onDismiss: () -> Unit,
    onSaveSchedule: (scheduledAt: Long, alertType: ScheduleAlertType) -> Unit,
    onClearSchedule: () -> Unit
) {
    ScheduleNoteDialog(
        noteTitle = noteTitle,
        initialScheduledAt = initialScheduledAt,
        initialAlertType = initialAlertType,
        initialScheduleTitle = null,
        onDismiss = onDismiss,
        onSaveSchedule = { scheduledAt, alertType, _ ->
            onSaveSchedule(scheduledAt, alertType)
        },
        onClearSchedule = onClearSchedule
    )
}

@Composable
private fun QuickPresetChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = ElectricSlateTokens.TrackInactive,
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = ElectricSlateTokens.TextMuted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
        )
    }
}

private fun openAppSettings(context: Context) {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
        data = Uri.fromParts("package", context.packageName, null)
    }
    context.startActivity(intent)
}
