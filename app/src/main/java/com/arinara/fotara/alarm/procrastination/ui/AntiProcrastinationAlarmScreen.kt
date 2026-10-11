// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.alarm.procrastination.ui

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.alarm.procrastination.AntiProcrastinationAlarmManager
import com.arinara.fotara.alarm.procrastination.model.AntiProcrastinationAlarm
import com.arinara.fotara.alarm.procrastination.model.AntiProcrastinationAlarmStore
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeMainButtonBlue
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.HomeSubtitleGray
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Composable
fun AntiProcrastinationAlarmScreen(
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val store = remember { AntiProcrastinationAlarmStore(context) }
    val alarmManager = remember { AntiProcrastinationAlarmManager(context) }

    var alarms by remember { mutableStateOf<List<AntiProcrastinationAlarm>>(emptyList()) }
    var showCreateDialog by remember { mutableStateOf(false) }

    fun refreshAlarms() {
        alarms = store.getAllAlarms()
    }

    LaunchedEffect(Unit) {
        refreshAlarms()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = HomeNearBlack,
        topBar = {
            AntiProcrastinationTopBar(onBackClick = onBackClick)
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = HomeMainButtonBlue,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Schedule Study Alarm"
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(4.dp))
                // Test Challenge Banner
                TestChallengeCard(
                    onTestClick = {
                        val testIntent = Intent(context, PhotoProofChallengeActivity::class.java).apply {
                            putExtra(AntiProcrastinationAlarmManager.EXTRA_ALARM_ID, 999999L)
                            putExtra(
                                AntiProcrastinationAlarmManager.EXTRA_ALARM_TITLE,
                                "Test Photo Challenge (Desk / Notes Proof)"
                            )
                            putExtra(AntiProcrastinationAlarmManager.EXTRA_EMERGENCY_PIN, "1234")
                        }
                        context.startActivity(testIntent)
                    }
                )
            }

            if (alarms.isEmpty()) {
                item {
                    EmptyAlarmState(
                        onScheduleClick = { showCreateDialog = true }
                    )
                }
            } else {
                item {
                    Text(
                        text = "Scheduled Alarms",
                        color = TextPrimary,
                        fontSize = 16.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                items(alarms, key = { it.id }) { alarm ->
                    AlarmCard(
                        alarm = alarm,
                        onToggle = { isEnabled ->
                            val updated = store.toggleAlarm(alarm.id, isEnabled)
                            if (updated != null) {
                                if (isEnabled) {
                                    val scheduled = alarmManager.scheduleAlarm(updated)
                                    if (scheduled) {
                                        Toast.makeText(context, "Alarm activated", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "Failed to schedule alarm", Toast.LENGTH_SHORT).show()
                                    }
                                } else {
                                    alarmManager.cancelAlarm(alarm.id)
                                    Toast.makeText(context, "Alarm turned off", Toast.LENGTH_SHORT).show()
                                }
                                refreshAlarms()
                            }
                        },
                        onDelete = {
                            alarmManager.cancelAlarm(alarm.id)
                            store.deleteAlarm(alarm.id)
                            refreshAlarms()
                            Toast.makeText(context, "Alarm deleted", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }

            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    if (showCreateDialog) {
        CreateStudyAlarmDialog(
            onDismiss = { showCreateDialog = false },
            onConfirm = { title, triggerTimeMs, pin ->
                val newAlarm = AntiProcrastinationAlarm(
                    id = System.currentTimeMillis(),
                    title = title,
                    triggerAtMillis = triggerTimeMs,
                    emergencyPin = pin,
                    isEnabled = true
                )
                store.saveAlarm(newAlarm)
                alarmManager.scheduleAlarm(newAlarm)
                refreshAlarms()
                showCreateDialog = false
                Toast.makeText(context, "Study alarm scheduled!", Toast.LENGTH_SHORT).show()
            }
        )
    }
}

@Composable
private fun AntiProcrastinationTopBar(onBackClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(HomeCardSurface)
                .border(1.dp, HomeCardBorder, CircleShape)
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = TextPrimary,
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column {
            Text(
                text = "Anti-Procrastination Alarms",
                color = TextPrimary,
                fontSize = 18.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "Photo proof challenge required to turn off",
                color = TextSecondary,
                fontSize = 12.sp,
                fontFamily = ElmsSans
            )
        }
    }
}

@Composable
private fun TestChallengeCard(onTestClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HomeCardSurface)
            .border(1.dp, HomeCardBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF2563EB).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PhotoCamera,
                        contentDescription = null,
                        tint = Color(0xFF60A5FA),
                        modifier = Modifier.size(20.dp)
                    )
                }

                Column {
                    Text(
                        text = "Test Photo Proof Challenge",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Experience the full-screen photo verification test now",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        fontFamily = ElmsSans
                    )
                }
            }

            Button(
                onClick = onTestClick,
                colors = ButtonDefaults.buttonColors(containerColor = HomeMainButtonBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Launch Test Challenge Now",
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun EmptyAlarmState(onScheduleClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(HomeCardSurface)
            .border(1.dp, HomeCardBorder, RoundedCornerShape(16.dp))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E293B)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Alarm,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(28.dp)
                )
            }

            Text(
                text = "No Study Alarms Scheduled",
                color = TextPrimary,
                fontSize = 16.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = "When an anti-procrastination alarm rings, it rings at maximum volume and requires you to snap a photo of your desk or textbook to silence it.",
                color = HomeSubtitleGray,
                fontSize = 12.sp,
                fontFamily = ElmsSans,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(4.dp))

            Button(
                onClick = onScheduleClick,
                colors = ButtonDefaults.buttonColors(containerColor = FolderTabCream),
                shape = RoundedCornerShape(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Schedule Study Alarm",
                    color = Color.Black,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun AlarmCard(
    alarm: AntiProcrastinationAlarm,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("EEE, MMM d • hh:mm a", Locale.getDefault()) }
    val formattedTime = remember(alarm.triggerAtMillis) {
        timeFormat.format(Date(alarm.triggerAtMillis))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(HomeCardSurface)
            .border(1.dp, HomeCardBorder, RoundedCornerShape(14.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = if (alarm.isEnabled) Color(0xFF60A5FA) else TextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = formattedTime,
                        color = if (alarm.isEnabled) Color(0xFF93C5FD) else TextMuted,
                        fontSize = 13.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = alarm.title,
                    color = if (alarm.isEnabled) TextPrimary else TextSecondary,
                    fontSize = 15.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.SemiBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = TextMuted,
                        modifier = Modifier.size(12.dp)
                    )
                    Text(
                        text = "Emergency PIN: ${alarm.emergencyPin}",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontFamily = ElmsSans
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = alarm.isEnabled,
                    onCheckedChange = onToggle,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = HomeMainButtonBlue,
                        uncheckedThumbColor = Color.Gray,
                        uncheckedTrackColor = HomeCardBorder
                    )
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Alarm",
                        tint = TagCrimson,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateStudyAlarmDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, triggerTimeMs: Long, emergencyPin: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var emergencyPin by remember { mutableStateOf("1234") }
    var selectedOffsetMinutes by remember { mutableStateOf(30) }
    var isCustomTomorrow by remember { mutableStateOf(false) }

    val presets = listOf(
        15 to "+15 min",
        30 to "+30 min",
        60 to "+1 hr",
        120 to "+2 hr"
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Schedule Study Alarm",
                color = TextPrimary,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.SemiBold
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Requires photo proof of your notes or study workspace to dismiss.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    fontFamily = ElmsSans
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Study Goal / Task Title", fontFamily = ElmsSans) },
                    placeholder = { Text("e.g. Physics Problem Set 3", fontFamily = ElmsSans) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HomeMainButtonBlue,
                        unfocusedBorderColor = HomeCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )

                Text(
                    text = "Trigger Time",
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium
                )

                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(presets) { (minutes, label) ->
                        val isSelected = !isCustomTomorrow && selectedOffsetMinutes == minutes
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) HomeMainButtonBlue else HomeCardSurface)
                                .border(1.dp, if (isSelected) HomeMainButtonBlue else HomeCardBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    isCustomTomorrow = false
                                    selectedOffsetMinutes = minutes
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    item {
                        val isSelected = isCustomTomorrow
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) HomeMainButtonBlue else HomeCardSurface)
                                .border(1.dp, if (isSelected) HomeMainButtonBlue else HomeCardBorder, RoundedCornerShape(8.dp))
                                .clickable {
                                    isCustomTomorrow = true
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Tomorrow 8 AM",
                                color = if (isSelected) Color.White else TextSecondary,
                                fontSize = 12.sp,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }

                OutlinedTextField(
                    value = emergencyPin,
                    onValueChange = {
                        if (it.length <= 6 && it.all { char -> char.isDigit() }) {
                            emergencyPin = it
                        }
                    },
                    label = { Text("Emergency Fallback PIN (4-6 digits)", fontFamily = ElmsSans) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = HomeMainButtonBlue,
                        unfocusedBorderColor = HomeCardBorder,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true
                )
            }
        },
        containerColor = HomeCardSurface,
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) return@Button
                    val pin = emergencyPin.ifBlank { "1234" }
                    if (pin.length < 4) return@Button

                    val triggerMs = if (isCustomTomorrow) {
                        val cal = Calendar.getInstance().apply {
                            add(Calendar.DAY_OF_YEAR, 1)
                            set(Calendar.HOUR_OF_DAY, 8)
                            set(Calendar.MINUTE, 0)
                            set(Calendar.SECOND, 0)
                            set(Calendar.MILLISECOND, 0)
                        }
                        cal.timeInMillis
                    } else {
                        System.currentTimeMillis() + (selectedOffsetMinutes * 60_000L)
                    }

                    onConfirm(title.trim(), triggerMs, pin)
                },
                enabled = title.isNotBlank() && emergencyPin.length in 4..6,
                colors = ButtonDefaults.buttonColors(containerColor = HomeMainButtonBlue)
            ) {
                Text("Schedule", fontFamily = ElmsSans, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextSecondary, fontFamily = ElmsSans)
            }
        }
    )
}
