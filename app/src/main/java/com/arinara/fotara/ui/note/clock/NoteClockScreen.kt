// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.clock

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppButtonVariant
import com.arinara.fotara.ui.components.presets.AppPillChip
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.jigglePressClickable
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

enum class ClockScreenFace {
    CLOCK,
    TIMER,
    GLOBE
}

data class TimerLapRecord(
    val lapIndex: Int,
    val splitTimeMs: Long,
    val totalTimeMs: Long
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteClockScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 3D Flip state: CLOCK, TIMER, or GLOBE
    var currentFace by remember { mutableStateOf(ClockScreenFace.CLOCK) }
    var previousNonClockFace by remember { mutableStateOf(ClockScreenFace.TIMER) }

    val flipAngle by animateFloatAsState(
        targetValue = if (currentFace == ClockScreenFace.CLOCK) 0f else 180f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "clockCard3DFlip"
    )

    // Current Time state
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedLocation by remember { mutableStateOf(DEFAULT_GLOBE_LOCATIONS[4]) } // Default to JKT
    var showTimezoneDropdown by remember { mutableStateOf(false) }

    // Multi-Alarm state
    var alarms by remember { mutableStateOf<List<NoteClockAlarm>>(emptyList()) }
    var newAlarmHour by remember { mutableStateOf("08") }
    var newAlarmMinute by remember { mutableStateOf("00") }
    var newAlarmLabel by remember { mutableStateOf("Study Session") }

    LaunchedEffect(Unit) {
        alarms = NoteClockAlarmManager.loadAlarms(context)
    }

    // Timer / Stopwatch state
    var isTimerRunning by remember { mutableStateOf(false) }
    var timerStartEpochMs by remember { mutableLongStateOf(0L) }
    var timerAccumulatedMs by remember { mutableLongStateOf(0L) }
    var currentTimerElapsedMs by remember { mutableLongStateOf(0L) }
    val laps = remember { mutableStateListOf<TimerLapRecord>() }

    // Real-time ticking effect
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeMillis = System.currentTimeMillis()
            delay(40L)
        }
    }

    // Timer ticking effect
    LaunchedEffect(isTimerRunning, timerStartEpochMs, timerAccumulatedMs) {
        while (isTimerRunning) {
            val now = System.currentTimeMillis()
            currentTimerElapsedMs = timerAccumulatedMs + (now - timerStartEpochMs)
            delay(30L)
        }
    }

    BackHandler {
        if (currentFace != ClockScreenFace.CLOCK) {
            currentFace = ClockScreenFace.CLOCK
        } else {
            onBack()
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ElectricSlateTokens.BgCanvas)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Navigation Bar
        TopAppBar(
            title = {
                Text(
                    text = when (if (flipAngle >= 90f) previousNonClockFace else currentFace) {
                        ClockScreenFace.TIMER -> "Focus Timer"
                        ClockScreenFace.GLOBE -> "3D Tech Globe"
                        ClockScreenFace.CLOCK -> "World Clock & Timezone"
                    },
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 20.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = {
                    if (currentFace != ClockScreenFace.CLOCK) {
                        currentFace = ClockScreenFace.CLOCK
                    } else {
                        onBack()
                    }
                }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = ElectricSlateTokens.TextPrimary
                    )
                }
            },
            actions = {
                // Quick Flip mode toggle
                IconButton(
                    onClick = {
                        if (currentFace == ClockScreenFace.CLOCK) {
                            previousNonClockFace = ClockScreenFace.TIMER
                            currentFace = ClockScreenFace.TIMER
                        } else {
                            currentFace = ClockScreenFace.CLOCK
                        }
                    }
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Flip 3D Surface",
                        tint = ElectricSlateTokens.PrimaryAccent
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = ElectricSlateTokens.BgCanvas)
        )

        // 3D Flip Container with realistic depth camera distance
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .graphicsLayer {
                    rotationY = flipAngle
                    cameraDistance = 14f * density
                }
        ) {
            if (flipAngle < 90f) {
                // SIDE A: World Clock, Timezone Selector & Multi-Alarm Management
                ClockFaceView(
                    currentTimeMillis = currentTimeMillis,
                    selectedLocation = selectedLocation,
                    showTimezoneDropdown = showTimezoneDropdown,
                    alarms = alarms,
                    newAlarmHour = newAlarmHour,
                    newAlarmMinute = newAlarmMinute,
                    newAlarmLabel = newAlarmLabel,
                    onTimezoneDropdownToggle = { showTimezoneDropdown = it },
                    onSelectLocation = { loc ->
                        selectedLocation = loc
                        showTimezoneDropdown = false
                    },
                    onOpenGlobeMode = {
                        previousNonClockFace = ClockScreenFace.GLOBE
                        currentFace = ClockScreenFace.GLOBE
                    },
                    onOpenTimerMode = {
                        previousNonClockFace = ClockScreenFace.TIMER
                        currentFace = ClockScreenFace.TIMER
                    },
                    onNewAlarmHourChange = { newAlarmHour = it.filter { ch -> ch.isDigit() }.take(2) },
                    onNewAlarmMinuteChange = { newAlarmMinute = it.filter { ch -> ch.isDigit() }.take(2) },
                    onNewAlarmLabelChange = { newAlarmLabel = it },
                    onAddAlarm = {
                        val h = (newAlarmHour.toIntOrNull() ?: 8).coerceIn(0, 23)
                        val m = (newAlarmMinute.toIntOrNull() ?: 0).coerceIn(0, 59)
                        val label = if (newAlarmLabel.isBlank()) "Study Session" else newAlarmLabel.trim()
                        val alarm = NoteClockAlarm(
                            hour = h,
                            minute = m,
                            label = label,
                            isEnabled = true
                        )
                        alarms = NoteClockAlarmManager.addAlarm(context, alarm)
                        val nextTrigger = NoteClockAlarmManager.calculateNextTriggerMillis(h, m)
                        val triggerCal = Calendar.getInstance().apply { timeInMillis = nextTrigger }
                        val timeStr = String.format(Locale.US, "%02d:%02d", h, m)
                        val dayStr = if (triggerCal.get(Calendar.DAY_OF_YEAR) == Calendar.getInstance().get(Calendar.DAY_OF_YEAR)) "today" else "tomorrow"
                        Toast.makeText(context, "Alarm set for $timeStr $dayStr ($label)", Toast.LENGTH_SHORT).show()
                    },
                    onToggleAlarm = { alarmId, enabled ->
                        alarms = NoteClockAlarmManager.toggleAlarm(context, alarmId, enabled)
                    },
                    onDeleteAlarm = { alarmId ->
                        alarms = NoteClockAlarmManager.deleteAlarm(context, alarmId)
                        Toast.makeText(context, "Alarm removed", Toast.LENGTH_SHORT).show()
                    }
                )
            } else {
                // SIDE B: 3D Flipped Surface (Timer or Globe View)
                Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
                    when (previousNonClockFace) {
                        ClockScreenFace.TIMER -> {
                            TimerFaceView(
                                elapsedMs = currentTimerElapsedMs,
                                isRunning = isTimerRunning,
                                laps = laps,
                                onStartPause = {
                                    if (!isTimerRunning) {
                                        timerStartEpochMs = System.currentTimeMillis()
                                        isTimerRunning = true
                                    } else {
                                        timerAccumulatedMs = currentTimerElapsedMs
                                        isTimerRunning = false
                                    }
                                },
                                onReset = {
                                    isTimerRunning = false
                                    timerAccumulatedMs = 0L
                                    currentTimerElapsedMs = 0L
                                    laps.clear()
                                },
                                onLap = {
                                    val lastTotal = laps.lastOrNull()?.totalTimeMs ?: 0L
                                    val split = currentTimerElapsedMs - lastTotal
                                    laps.add(
                                        TimerLapRecord(
                                            lapIndex = laps.size + 1,
                                            splitTimeMs = split,
                                            totalTimeMs = currentTimerElapsedMs
                                        )
                                    )
                                },
                                onLaunchFloatingPill = {
                                    if (!Settings.canDrawOverlays(context)) {
                                        Toast.makeText(context, "Please grant Draw Over Other Apps permission to display Floating Pill", Toast.LENGTH_LONG).show()
                                        val intent = Intent(
                                            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                            Uri.parse("package:${context.packageName}")
                                        ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                        context.startActivity(intent)
                                    } else {
                                        TimerFloatingPillService.startService(context, currentTimerElapsedMs / 1000L)
                                        Toast.makeText(context, "Floating Pill active over applications", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                onFlipBack = { currentFace = ClockScreenFace.CLOCK }
                            )
                        }
                        ClockScreenFace.GLOBE -> {
                            GlobeFaceView(
                                selectedLocation = selectedLocation,
                                onSelectLocation = { loc ->
                                    selectedLocation = loc
                                },
                                onConfirmLocation = {
                                    currentFace = ClockScreenFace.CLOCK
                                }
                            )
                        }
                        ClockScreenFace.CLOCK -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun ClockFaceView(
    currentTimeMillis: Long,
    selectedLocation: GlobeTimezoneLocation,
    showTimezoneDropdown: Boolean,
    alarms: List<NoteClockAlarm>,
    newAlarmHour: String,
    newAlarmMinute: String,
    newAlarmLabel: String,
    onTimezoneDropdownToggle: (Boolean) -> Unit,
    onSelectLocation: (GlobeTimezoneLocation) -> Unit,
    onOpenGlobeMode: () -> Unit,
    onOpenTimerMode: () -> Unit,
    onNewAlarmHourChange: (String) -> Unit,
    onNewAlarmMinuteChange: (String) -> Unit,
    onNewAlarmLabelChange: (String) -> Unit,
    onAddAlarm: () -> Unit,
    onToggleAlarm: (String, Boolean) -> Unit,
    onDeleteAlarm: (String) -> Unit
) {
    val scrollState = rememberScrollState()

    val timeZone = remember(selectedLocation) {
        TimeZone.getTimeZone(selectedLocation.timeZoneId)
    }

    val cal = remember(currentTimeMillis, timeZone) {
        Calendar.getInstance(timeZone).apply {
            timeInMillis = currentTimeMillis
        }
    }

    val hours = cal.get(Calendar.HOUR_OF_DAY)
    val minutes = cal.get(Calendar.MINUTE)
    val seconds = cal.get(Calendar.SECOND)
    val millis = cal.get(Calendar.MILLISECOND)

    val dateStr = remember(currentTimeMillis, timeZone) {
        val sdf = SimpleDateFormat("EEEE, MMMM d, yyyy", Locale.US)
        sdf.timeZone = timeZone
        sdf.format(Date(currentTimeMillis))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        // Timezone Selector Header at Top
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box {
                Surface(
                    color = ElectricSlateTokens.SurfaceElevated,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .jigglePressClickable { onTimezoneDropdownToggle(true) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = "Timezone",
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "[${selectedLocation.countryCode}] ${selectedLocation.name}",
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 13.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                DropdownMenu(
                    expanded = showTimezoneDropdown,
                    onDismissRequest = { onTimezoneDropdownToggle(false) },
                    modifier = Modifier
                        .background(ElectricSlateTokens.SurfacePanel)
                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(12.dp))
                ) {
                    DEFAULT_GLOBE_LOCATIONS.forEach { loc ->
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = "[${loc.countryCode}] ${loc.name}",
                                    color = if (loc.id == selectedLocation.id) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextPrimary,
                                    fontFamily = ElmsSans
                                )
                            },
                            onClick = { onSelectLocation(loc) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // 3D Globe Visualizer Mode Pill
            Surface(
                color = ElectricSlateTokens.SurfaceElevated,
                shape = RoundedCornerShape(999.dp),
                border = BorderStroke(1.dp, ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.5f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .jigglePressClickable { onOpenGlobeMode() }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "3D Globe",
                        color = ElectricSlateTokens.PrimaryAccent,
                        fontSize = 12.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Digital Clock with Tabular Monospace Digits and Play Button Pill Beside Clock
        Surface(
            color = ElectricSlateTokens.SurfacePanel,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 22.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dateStr,
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 13.sp,
                    fontFamily = ElmsSans
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Digital Time Column
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        // Big Tabular Monospace HH:MM
                        Text(
                            text = String.format(Locale.US, "%02d:%02d", hours, minutes),
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 56.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            letterSpacing = (-1).sp
                        )

                        Spacer(modifier = Modifier.width(6.dp))

                        // Running Seconds & Faded Milliseconds Counter
                        Column(modifier = Modifier.padding(bottom = 6.dp)) {
                            Text(
                                text = String.format(Locale.US, ":%02d", seconds),
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = String.format(Locale.US, ".%03d", millis),
                                color = ElectricSlateTokens.TextMuted.copy(alpha = 0.5f),
                                fontSize = 13.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    // Play Button Pill Beside Clock Display (Triggers 3D Flip to Timer)
                    Surface(
                        color = ElectricSlateTokens.PrimaryAccent,
                        shape = RoundedCornerShape(999.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(999.dp))
                            .jigglePressClickable { onOpenTimerMode() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "3D Flip to Timer",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Timer",
                                color = Color.White,
                                fontSize = 13.sp,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Multi-Alarm Management Section
        Surface(
            color = ElectricSlateTokens.SurfacePanel,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Alarm,
                            contentDescription = null,
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Academic Alarms",
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 16.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Surface(
                        color = ElectricSlateTokens.SurfaceElevated,
                        shape = RoundedCornerShape(999.dp),
                        border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle)
                    ) {
                        Text(
                            text = "${alarms.count { it.isEnabled }} Active",
                            color = ElectricSlateTokens.PrimaryAccent,
                            fontSize = 11.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Time Picker Inputs (Hour & Minute)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = newAlarmHour,
                        onValueChange = onNewAlarmHourChange,
                        label = { Text("Hour (00-23)", color = ElectricSlateTokens.TextMuted) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                            unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                            focusedTextColor = ElectricSlateTokens.TextPrimary,
                            unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                            focusedContainerColor = ElectricSlateTokens.SurfaceElevated,
                            unfocusedContainerColor = ElectricSlateTokens.SurfaceElevated,
                            focusedLabelColor = ElectricSlateTokens.PrimaryAccent,
                            unfocusedLabelColor = ElectricSlateTokens.TextMuted
                        )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedTextField(
                        value = newAlarmMinute,
                        onValueChange = onNewAlarmMinuteChange,
                        label = { Text("Minute (00-59)", color = ElectricSlateTokens.TextMuted) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                            unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                            focusedTextColor = ElectricSlateTokens.TextPrimary,
                            unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                            focusedContainerColor = ElectricSlateTokens.SurfaceElevated,
                            unfocusedContainerColor = ElectricSlateTokens.SurfaceElevated,
                            focusedLabelColor = ElectricSlateTokens.PrimaryAccent,
                            unfocusedLabelColor = ElectricSlateTokens.TextMuted
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Quick Preset Suggestions
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val presets = listOf("Study Session", "Exam Prep", "Problem Set", "Formula Review")
                    items(presets) { preset ->
                        AppPillChip(
                            label = preset,
                            isSelected = newAlarmLabel == preset,
                            onClick = { onNewAlarmLabelChange(preset) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = newAlarmLabel,
                    onValueChange = onNewAlarmLabelChange,
                    label = { Text("Custom Alarm Label", color = ElectricSlateTokens.TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = ElectricSlateTokens.PrimaryAccent,
                        unfocusedBorderColor = ElectricSlateTokens.BorderSubtle,
                        focusedTextColor = ElectricSlateTokens.TextPrimary,
                        unfocusedTextColor = ElectricSlateTokens.TextPrimary,
                        focusedContainerColor = ElectricSlateTokens.SurfaceElevated,
                        unfocusedContainerColor = ElectricSlateTokens.SurfaceElevated,
                        focusedLabelColor = ElectricSlateTokens.PrimaryAccent,
                        unfocusedLabelColor = ElectricSlateTokens.TextMuted
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                AppButton(
                    text = "Schedule Alarm",
                    onClick = onAddAlarm,
                    variant = AppButtonVariant.PRIMARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Scheduled Alarms List
        if (alarms.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Configured Alarms (${alarms.size})",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 13.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier.padding(horizontal = 4.dp)
                )

                alarms.forEach { alarm ->
                    Surface(
                        color = ElectricSlateTokens.SurfaceElevated,
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (alarm.isEnabled) ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.4f) else ElectricSlateTokens.BorderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = alarm.getFormattedTime(),
                                    color = if (alarm.isEnabled) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = alarm.label,
                                    color = if (alarm.isEnabled) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted,
                                    fontSize = 13.sp,
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Medium
                                )
                            }

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Switch(
                                    checked = alarm.isEnabled,
                                    onCheckedChange = { onToggleAlarm(alarm.id, it) },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = ElectricSlateTokens.PrimaryAccent,
                                        uncheckedThumbColor = ElectricSlateTokens.TextMuted,
                                        uncheckedTrackColor = ElectricSlateTokens.TrackInactive
                                    )
                                )

                                Spacer(modifier = Modifier.width(6.dp))

                                IconButton(
                                    onClick = { onDeleteAlarm(alarm.id) },
                                    modifier = Modifier.jigglePressClickable { onDeleteAlarm(alarm.id) }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete Alarm",
                                        tint = ElectricSlateTokens.Destructive,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Surface(
                color = ElectricSlateTokens.SurfacePanel,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "No academic alarms configured yet. Create one above.",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 12.sp,
                    fontFamily = ElmsSans,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun TimerFaceView(
    elapsedMs: Long,
    isRunning: Boolean,
    laps: List<TimerLapRecord>,
    onStartPause: () -> Unit,
    onReset: () -> Unit,
    onLap: () -> Unit,
    onLaunchFloatingPill: () -> Unit,
    onFlipBack: () -> Unit
) {
    val totalSec = elapsedMs / 1000
    val m = totalSec / 60
    val s = totalSec % 60
    val ms = (elapsedMs % 1000) / 10

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))

        // Timer Display Capsule
        Surface(
            color = ElectricSlateTokens.SurfacePanel,
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, if (isRunning) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", m, s),
                        color = ElectricSlateTokens.TextPrimary,
                        fontSize = 62.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format(Locale.US, ".%02d", ms),
                        color = ElectricSlateTokens.TextMuted,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Control Actions: Start/Pause, Lap/Flag, Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset button
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                            .jigglePressClickable(enabled = elapsedMs > 0) { onReset() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = if (elapsedMs > 0) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    // Main Start / Pause button
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (isRunning) ElectricSlateTokens.Destructive else ElectricSlateTokens.PrimaryAccent)
                            .jigglePressClickable { onStartPause() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Start",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Lap / Flag button
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(ElectricSlateTokens.SurfaceElevated)
                            .border(1.dp, ElectricSlateTokens.BorderSubtle, CircleShape)
                            .jigglePressClickable(enabled = isRunning) { onLap() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Lap",
                            tint = if (isRunning) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.TextMuted,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Floating Pill Overlay Trigger
                Surface(
                    color = ElectricSlateTokens.SurfaceElevated,
                    shape = RoundedCornerShape(999.dp),
                    border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                    modifier = Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .jigglePressClickable { onLaunchFloatingPill() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.OpenInNew,
                            contentDescription = null,
                            tint = ElectricSlateTokens.PrimaryAccent,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Float Pill Over Apps",
                            color = ElectricSlateTokens.TextPrimary,
                            fontFamily = ElmsSans,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Laps Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Laps & Splits (${laps.size})",
                color = ElectricSlateTokens.TextMuted,
                fontSize = 13.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Flip back to Clock",
                color = ElectricSlateTokens.PrimaryAccent,
                fontSize = 12.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.jigglePressClickable { onFlipBack() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Laps List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            itemsIndexed(laps.reversed()) { _, lap ->
                val splitSec = lap.splitTimeMs / 1000
                val splitM = splitSec / 60
                val splitS = splitSec % 60
                val splitMs = (lap.splitTimeMs % 1000) / 10

                val totalSecL = lap.totalTimeMs / 1000
                val totalML = totalSecL / 60
                val totalSL = totalSecL % 60
                val totalMsL = (lap.totalTimeMs % 1000) / 10

                Surface(
                    color = ElectricSlateTokens.SurfaceElevated,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lap ${lap.lapIndex}",
                            color = ElectricSlateTokens.PrimaryAccent,
                            fontSize = 13.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale.US, "+%02d:%02d.%02d", splitM, splitS, splitMs),
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = String.format(Locale.US, "%02d:%02d.%02d", totalML, totalSL, totalMsL),
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobeFaceView(
    selectedLocation: GlobeTimezoneLocation,
    onSelectLocation: (GlobeTimezoneLocation) -> Unit,
    onConfirmLocation: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Interactive 3D Tech Globe",
                color = ElectricSlateTokens.TextPrimary,
                fontSize = 15.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = "Flip to Clock",
                color = ElectricSlateTokens.PrimaryAccent,
                fontSize = 12.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.jigglePressClickable { onConfirmLocation() }
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Full Interactive 3D Tech Globe Visualizer
        TechGlobeVisualizer(
            selectedLocation = selectedLocation,
            onSelectLocation = onSelectLocation,
            showLocationChips = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Selected Timezone Info Card
        Surface(
            color = ElectricSlateTokens.SurfacePanel,
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, ElectricSlateTokens.BorderSubtle),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Selected Timezone",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 12.sp,
                    fontFamily = ElmsSans
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "[${selectedLocation.countryCode}] ${selectedLocation.name}",
                    color = ElectricSlateTokens.TextPrimary,
                    fontSize = 17.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Zone ID: ${selectedLocation.timeZoneId} · Coordinates: ${selectedLocation.latitudeDeg}°N, ${selectedLocation.longitudeDeg}°E",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(14.dp))

                AppButton(
                    text = "Confirm & View Clock",
                    onClick = onConfirmLocation,
                    variant = AppButtonVariant.PRIMARY,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}
