// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.clock

import android.app.AlarmManager
import android.app.PendingIntent
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.FlipCameraAndroid
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import com.arinara.fotara.MainActivity
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.TagCrimson
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private val AccentGold = Color(0xFFF77F00)
private val ScreenNavy = Color(0xFF03071E)

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

    // Flip state: false = Clock & Timezone Mode, true = Timer Mode
    var isTimerMode by remember { mutableStateOf(false) }
    val flipAngle by animateFloatAsState(
        targetValue = if (isTimerMode) 180f else 0f,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "clockCard3DFlip"
    )

    // Current Time state
    var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
    var selectedLocation by remember { mutableStateOf(DEFAULT_GLOBE_LOCATIONS[0]) }
    var showGlobeVisualizer by remember { mutableStateOf(false) }
    var showTimezoneDropdown by remember { mutableStateOf(false) }

    // Alarm configuration state
    var alarmHour by remember { mutableStateOf("08") }
    var alarmMinute by remember { mutableStateOf("00") }
    var alarmLabel by remember { mutableStateOf("Academic Focus Session") }

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
        onBack()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenNavy)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top Navigation Bar
        TopAppBar(
            title = {
                Text(
                    text = if (flipAngle >= 90f) "Focus Timer" else "World Clock & Timezone",
                    color = FolderTabCream,
                    fontSize = 22.sp,
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.SemiBold
                )
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = FolderTabCream
                    )
                }
            },
            actions = {
                // 3D Flip toggle button
                IconButton(
                    onClick = { isTimerMode = !isTimerMode }
                ) {
                    Icon(
                        imageVector = Icons.Default.FlipCameraAndroid,
                        contentDescription = "Flip 3D View",
                        tint = AccentGold
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = ScreenNavy)
        )

        // 3D Flip Container with realistic depth cameraDistance
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
                // SIDE A: World Clock, 3D Tech Globe & Alarm Settings
                ClockFaceView(
                    currentTimeMillis = currentTimeMillis,
                    selectedLocation = selectedLocation,
                    showGlobeVisualizer = showGlobeVisualizer,
                    showTimezoneDropdown = showTimezoneDropdown,
                    alarmHour = alarmHour,
                    alarmMinute = alarmMinute,
                    alarmLabel = alarmLabel,
                    onToggleGlobe = { showGlobeVisualizer = !showGlobeVisualizer },
                    onTimezoneDropdownToggle = { showTimezoneDropdown = it },
                    onSelectLocation = {
                        selectedLocation = it
                        showTimezoneDropdown = false
                    },
                    onAlarmHourChange = { alarmHour = it.filter { ch -> ch.isDigit() }.take(2) },
                    onAlarmMinuteChange = { alarmMinute = it.filter { ch -> ch.isDigit() }.take(2) },
                    onAlarmLabelChange = { alarmLabel = it },
                    onSetAlarm = {
                        val h = alarmHour.toIntOrNull() ?: 8
                        val m = alarmMinute.toIntOrNull() ?: 0
                        val cal = Calendar.getInstance().apply {
                            set(Calendar.HOUR_OF_DAY, h)
                            set(Calendar.MINUTE, m)
                            set(Calendar.SECOND, 0)
                            if (before(Calendar.getInstance())) {
                                add(Calendar.DATE, 1)
                            }
                        }
                        try {
                            val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                            val intent = Intent(context, MainActivity::class.java).apply {
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP
                            }
                            val pi = PendingIntent.getActivity(
                                context,
                                5050,
                                intent,
                                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                            )
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                                am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
                            } else {
                                am.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pi)
                            }
                            Toast.makeText(context, "Alarm set for ${String.format("%02d:%02d", h, m)}: $alarmLabel", Toast.LENGTH_SHORT).show()
                        } catch (_: Exception) {
                            Toast.makeText(context, "Scheduled alarm saved for ${String.format("%02d:%02d", h, m)}", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onStartTimerMode = { isTimerMode = true }
                )
            } else {
                // SIDE B: 3D Timer / Stopwatch with Floating Pill
                Box(modifier = Modifier.graphicsLayer { rotationY = 180f }) {
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
                                Toast.makeText(context, "Please permit Draw Over Other Apps to display Floating Pill", Toast.LENGTH_LONG).show()
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                    Uri.parse("package:${context.packageName}")
                                ).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
                                context.startActivity(intent)
                            } else {
                                TimerFloatingPillService.startService(context, currentTimerElapsedMs / 1000L)
                                Toast.makeText(context, "Floating Pill active over apps", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onFlipBack = { isTimerMode = false }
                    )
                }
            }
        }
    }
}

@Composable
private fun ClockFaceView(
    currentTimeMillis: Long,
    selectedLocation: GlobeTimezoneLocation,
    showGlobeVisualizer: Boolean,
    showTimezoneDropdown: Boolean,
    alarmHour: String,
    alarmMinute: String,
    alarmLabel: String,
    onToggleGlobe: () -> Unit,
    onTimezoneDropdownToggle: (Boolean) -> Unit,
    onSelectLocation: (GlobeTimezoneLocation) -> Unit,
    onAlarmHourChange: (String) -> Unit,
    onAlarmMinuteChange: (String) -> Unit,
    onAlarmLabelChange: (String) -> Unit,
    onSetAlarm: () -> Unit,
    onStartTimerMode: () -> Unit
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

        // Timezone Selector Header
        Box {
            Surface(
                color = HomeCardSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, HomeCardBorder),
                modifier = Modifier.clickable { onTimezoneDropdownToggle(true) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Public,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = selectedLocation.name,
                        color = FolderTabCream,
                        fontSize = 14.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            DropdownMenu(
                expanded = showTimezoneDropdown,
                onDismissRequest = { onTimezoneDropdownToggle(false) },
                modifier = Modifier
                    .background(HomeCardSurface)
                    .border(1.dp, HomeCardBorder, RoundedCornerShape(8.dp))
            ) {
                DEFAULT_GLOBE_LOCATIONS.forEach { loc ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = loc.name,
                                color = if (loc.id == selectedLocation.id) AccentGold else FolderTabCream,
                                fontFamily = ElmsSans
                            )
                        },
                        onClick = { onSelectLocation(loc) }
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Large Digital Clock with Small Faded Seconds & Milliseconds
        Surface(
            color = Color(0xFF0F1528),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFF1E2848)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(vertical = 24.dp, horizontal = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = dateStr,
                    color = TextMuted,
                    fontSize = 13.sp,
                    fontFamily = ElmsSans
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    // Big HH:MM
                    Text(
                        text = String.format(Locale.US, "%02d:%02d", hours, minutes),
                        color = FolderTabCream,
                        fontSize = 58.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-1).sp
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    // Small faded :SS .MMM
                    Column(modifier = Modifier.padding(bottom = 8.dp)) {
                        Text(
                            text = String.format(Locale.US, ":%02d", seconds),
                            color = FolderTabCream.copy(alpha = 0.55f),
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Medium,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = String.format(Locale.US, ".%03d", millis),
                            color = FolderTabCream.copy(alpha = 0.35f),
                            fontSize = 14.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Action buttons: 3D Globe Visualizer toggle & Flip to Timer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    Button(
                        onClick = onToggleGlobe,
                        colors = ButtonDefaults.buttonColors(containerColor = if (showGlobeVisualizer) AccentGold else HomeCardSurface),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, HomeCardBorder)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Public,
                            contentDescription = null,
                            tint = if (showGlobeVisualizer) Color.Black else AccentGold,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (showGlobeVisualizer) "Hide 3D Globe" else "3D Tech Globe",
                            color = if (showGlobeVisualizer) Color.Black else FolderTabCream,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Button(
                        onClick = onStartTimerMode,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("3D Timer", color = Color.White, fontFamily = ElmsSans, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3D Low-Poly Tech Globe Visualizer
        if (showGlobeVisualizer) {
            Spacer(modifier = Modifier.height(16.dp))
            TechGlobeVisualizer(
                selectedLocation = selectedLocation,
                onSelectLocation = onSelectLocation
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Custom Labeled Alarm Configuration
        Surface(
            color = HomeCardSurface,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, HomeCardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(18.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Alarm,
                        contentDescription = null,
                        tint = AccentGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Set Custom Academic Alarm",
                        color = FolderTabCream,
                        fontSize = 15.sp,
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedTextField(
                        value = alarmHour,
                        onValueChange = onAlarmHourChange,
                        label = { Text("Hour (00-23)", color = TextMuted) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = HomeCardBorder,
                            focusedTextColor = FolderTabCream,
                            unfocusedTextColor = FolderTabCream
                        )
                    )

                    Spacer(modifier = Modifier.width(12.dp))

                    OutlinedTextField(
                        value = alarmMinute,
                        onValueChange = onAlarmMinuteChange,
                        label = { Text("Minute (00-59)", color = TextMuted) },
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGold,
                            unfocusedBorderColor = HomeCardBorder,
                            focusedTextColor = FolderTabCream,
                            unfocusedTextColor = FolderTabCream
                        )
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                OutlinedTextField(
                    value = alarmLabel,
                    onValueChange = onAlarmLabelChange,
                    label = { Text("Custom Alarm Label", color = TextMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AccentGold,
                        unfocusedBorderColor = HomeCardBorder,
                        focusedTextColor = FolderTabCream,
                        unfocusedTextColor = FolderTabCream
                    )
                )

                Spacer(modifier = Modifier.height(14.dp))

                Button(
                    onClick = onSetAlarm,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Confirm Alarm", color = Color.Black, fontFamily = ElmsSans, fontWeight = FontWeight.Bold)
                }
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
            color = Color(0xFF0F1528),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.5.dp, if (isRunning) Color(0xFF2563EB) else Color(0xFF1E2848)),
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
                        color = FolderTabCream,
                        fontSize = 62.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        letterSpacing = (-1).sp
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = String.format(Locale.US, ".%02d", ms),
                        color = FolderTabCream.copy(alpha = 0.5f),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Medium,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(bottom = 10.dp)
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Control Actions: Start/Pause, Lap/Flag, Reset
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Reset button
                    IconButton(
                        onClick = onReset,
                        enabled = elapsedMs > 0,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(HomeCardSurface)
                            .border(1.dp, HomeCardBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reset",
                            tint = if (elapsedMs > 0) FolderTabCream else TextMuted
                        )
                    }

                    // Main Start / Pause button
                    IconButton(
                        onClick = onStartPause,
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(if (isRunning) TagCrimson else Color(0xFF2563EB))
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isRunning) "Pause" else "Start",
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    // Lap / Flag button
                    IconButton(
                        onClick = onLap,
                        enabled = isRunning,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(HomeCardSurface)
                            .border(1.dp, HomeCardBorder, CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Flag,
                            contentDescription = "Lap",
                            tint = if (isRunning) AccentGold else TextMuted
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Floating Pill Overlay Trigger
                Button(
                    onClick = onLaunchFloatingPill,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFF38BDF8))
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Float Pill Over Apps", color = Color(0xFF38BDF8), fontFamily = ElmsSans, fontWeight = FontWeight.Bold)
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
                color = TextMuted,
                fontSize = 13.sp,
                fontFamily = ElmsSans,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = "Tap 3D Flip to return to Clock",
                color = AccentGold,
                fontSize = 12.sp,
                fontFamily = ElmsSans,
                modifier = Modifier.clickable { onFlipBack() }
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Laps List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(6.dp)
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
                    color = HomeCardSurface,
                    shape = RoundedCornerShape(10.dp),
                    border = BorderStroke(1.dp, HomeCardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Lap ${lap.lapIndex}",
                            color = AccentGold,
                            fontSize = 13.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = String.format(Locale.US, "+%02d:%02d.%02d", splitM, splitS, splitMs),
                            color = TextMuted,
                            fontSize = 13.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = String.format(Locale.US, "%02d:%02d.%02d", totalML, totalSL, totalMsL),
                            color = FolderTabCream,
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
