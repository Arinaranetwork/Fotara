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

    val badgeColor = if (isOverdue) TagCrimson else FolderTabCream
    val badgeBg = if (isOverdue) TagCrimson.copy(alpha = 0.15f) else DockSlatePill.copy(alpha = 0.7f)
    val badgeBorder = if (isOverdue) TagCrimson.copy(alpha = 0.6f) else MidnightCardOutline

    val text = ScheduleMath.formatScheduleBadge(scheduledAt, now)

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
            containerColor = MidnightSurface,
            shape = RoundedCornerShape(20.dp),
            icon = {
                Icon(
                    imageVector = Icons.Default.Alarm,
                    contentDescription = null,
                    tint = FolderTabCream,
                    modifier = Modifier.size(32.dp)
                )
            },
            title = {
                Text(
                    text = "Allow Timely Study Alerts",
                    color = TextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "To ensure you never miss a study deadline, Fotara needs permission to schedule alarms and post alerts even when your device is locked or in battery saving mode.",
                        color = TextSecondary,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !scheduleManager.canScheduleExact()) {
                        Text(
                            text = "• Exact Alarm Permission: Required for to-the-minute alarm precision.",
                            color = TagAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (!scheduleManager.canPostNotifications()) {
                        Text(
                            text = "• Notification Permission: Required to display study reminders on your screen.",
                            color = TagAmber,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            },
            confirmButton = {
                Button(
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
                    colors = ButtonDefaults.buttonColors(
                        containerColor = FolderTabCream,
                        contentColor = MidnightNavy
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Open Settings", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showPermissionDialog = false
                        // Proceed anyway with available capabilities
                        onSaveSchedule(
                            selectedCal.timeInMillis,
                            selectedAlertType,
                            scheduleName.trim().ifBlank { null }
                        )
                        onDismiss()
                    }
                ) {
                    Text("Continue Anyway", color = TextSecondary)
                }
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
                    text = "Schedule Note",
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
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 1. Custom Schedule Title field
                Column {
                    Text(
                        text = "SCHEDULE NAME",
                        style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = scheduleName,
                        onValueChange = { scheduleName = it },
                        placeholder = { Text("e.g. Quiz Preparation, Chapter 3", color = TextMuted, fontSize = 13.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedBorderColor = FolderTabCream,
                            unfocusedBorderColor = MidnightCardOutline,
                            focusedContainerColor = DockSlatePill.copy(alpha = 0.35f),
                            unfocusedContainerColor = DockSlatePill.copy(alpha = 0.35f),
                            cursorColor = FolderTabCream
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // 2. Date picker trigger button
                Column {
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
                }

                // 3. Time picker trigger button
                Column {
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
                }

                // Inline past validation warning
                if (isPast) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(TagCrimson.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                            .border(1.dp, TagCrimson.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = TagCrimson,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Selected time is in the past. Choose a future time.",
                            color = TagCrimson,
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
                        style = TextStyle(color = TextMuted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
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
                                modifier = Modifier.padding(vertical = 9.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Notifications,
                                    contentDescription = null,
                                    tint = if (notifSelected) MidnightNavy else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Notification",
                                    color = if (notifSelected) MidnightNavy else TextPrimary,
                                    fontSize = 12.sp,
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
                                modifier = Modifier.padding(vertical = 9.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Alarm,
                                    contentDescription = null,
                                    tint = if (alarmSelected) MidnightNavy else TextSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Alarm Ring",
                                    color = if (alarmSelected) MidnightNavy else TextPrimary,
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
            Button(
                onClick = {
                    if (isPast) return@Button
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
                colors = ButtonDefaults.buttonColors(
                    containerColor = FolderTabCream,
                    contentColor = MidnightNavy,
                    disabledContainerColor = DockSlatePill.copy(alpha = 0.5f),
                    disabledContentColor = TextMuted
                ),
                shape = RoundedCornerShape(10.dp)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
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
        color = DockSlatePill.copy(alpha = 0.4f),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(0.8.dp, MidnightCardOutline),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            color = TextSecondary,
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
