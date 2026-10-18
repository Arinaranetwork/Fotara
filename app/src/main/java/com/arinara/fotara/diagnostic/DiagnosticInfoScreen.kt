// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.diagnostic

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import com.arinara.fotara.ui.components.presets.AppPillChip
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppCard
import com.arinara.fotara.ui.components.presets.AppScreen
import com.arinara.fotara.ui.components.presets.AppTopBar
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun DiagnosticInfoScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isOverlayEnabled by AppDiagnosticLogger.isOverlayEnabled.collectAsState()
    val isDeveloperModeEnabled by AppDiagnosticLogger.isDeveloperModeEnabled.collectAsState()
    val startupMetrics by AppDiagnosticLogger.startupMetricsFlow.collectAsState()
    val events by AppDiagnosticLogger.eventsFlow.collectAsState()
    val errors by AppDiagnosticLogger.errorsFlow.collectAsState()
    val ramSamples by AppDiagnosticLogger.ramHistoryFlow.collectAsState()
    val workerRecords by AppDiagnosticLogger.workerMemoryFlow.collectAsState()
    val framePacing by AppDiagnosticLogger.framePacingFlow.collectAsState()
    val stuckErrors by AppDiagnosticLogger.stuckErrorsFlow.collectAsState()

    var lastGcResult by remember { mutableStateOf<GcResult?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var selectedTagFilter by remember { mutableStateOf("All") }
    var expandedStuckId by remember { mutableStateOf<Long?>(null) }

    // Auto-sample RAM when Developer Mode is active
    LaunchedEffect(isDeveloperModeEnabled) {
        if (isDeveloperModeEnabled) {
            while (true) {
                AppDiagnosticLogger.recordRamSample("DiagnosticScreen")
                delay(1500L)
            }
        }
    }

    val hardwareInfo by produceState(initialValue = remember { AppDiagnosticLogger.getHardwareInfo(null) }, context) {
        value = withContext(Dispatchers.IO) {
            AppDiagnosticLogger.getHardwareInfo(context)
        }
    }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

    val leakHints = remember(ramSamples, workerRecords, events, hardwareInfo.jvmHeapMaxMb) {
        MemoryLeakDetector.analyze(ramSamples, workerRecords, events, hardwareInfo.jvmHeapMaxMb)
    }

    AppScreen(
        modifier = modifier.fillMaxSize(),
        topBar = {
            AppTopBar(
                title = "Diagnostic & Performance",
                onBackClick = onBack
            )
        }
    ) { screenPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(screenPadding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 120.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Section 0: Developer Mode Master Toggle
            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "Extreme Developer Mode",
                                    color = ElectricSlateTokens.TextPrimary,
                                    fontSize = 16.sp,
                                    fontFamily = ElmsSans,
                                    fontWeight = FontWeight.Bold
                                )
                                if (isDeveloperModeEnabled) {
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.2f))
                                            .border(1.dp, ElectricSlateTokens.PrimaryAccent, RoundedCornerShape(8.dp))
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = "ACTIVE",
                                            color = ElectricSlateTokens.PrimaryAccent,
                                            fontSize = 10.sp,
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }
                            }
                            Text(
                                text = "Unlocks live RAM graph, worker memory delta tracking, leak hints & manual GC",
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 12.sp,
                                fontFamily = ElmsSans,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Switch(
                            checked = isDeveloperModeEnabled,
                            onCheckedChange = { enabled ->
                                AppDiagnosticLogger.setDeveloperModeEnabled(context, enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ElectricSlateTokens.TextPrimary,
                                checkedTrackColor = ElectricSlateTokens.PrimaryAccent,
                                uncheckedThumbColor = ElectricSlateTokens.TextMuted,
                                uncheckedTrackColor = ElectricSlateTokens.TrackInactive
                            )
                        )
                    }
                }
            }

            // Developer Mode Section: Real-Time Frame Pacing & RAM Graphs
            if (isDeveloperModeEnabled) {
                // Section: Frame Pacing & Vsync Consistency Studio
                item {
                    AppCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DiagnosticSectionHeader(
                                title = "Frame Pacing & Vsync Consistency",
                                icon = Icons.Default.Speed
                            )
                            FramePacingGraph(snapshot = framePacing)
                        }
                    }
                }

                // Section: Main-Thread Hang & Stuck Errors Inspector
                item {
                    AppCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DiagnosticSectionHeader(
                                title = "Main-Thread Hang & Stuck Errors (${stuckErrors.size})",
                                icon = Icons.Default.Warning
                            )

                            if (stuckErrors.isEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = ElectricSlateTokens.PrimaryAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "0 Main-Thread Freezes (No hitches > 200ms)",
                                        color = ElectricSlateTokens.TextPrimary,
                                        fontSize = 13.sp,
                                        fontFamily = ElmsSans
                                    )
                                }
                            } else {
                                for (stuck in stuckErrors.take(5)) {
                                    val rowShape = RoundedCornerShape(10.dp)
                                    val isSevere = stuck.isSevereFreeze
                                    val borderCol = if (isSevere) ElectricSlateTokens.Destructive else ElectricSlateTokens.BorderSubtle
                                    val isExpanded = expandedStuckId == stuck.id

                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(rowShape)
                                            .background(ElectricSlateTokens.SurfaceElevated)
                                            .border(1.dp, borderCol, rowShape)
                                            .clickable {
                                                expandedStuckId = if (isExpanded) null else stuck.id
                                            }
                                            .padding(10.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = if (isSevere) "SEVERE FREEZE: ${stuck.durationMs}ms" else "HITCH: ${stuck.durationMs}ms",
                                                    color = if (isSevere) ElectricSlateTokens.Destructive else ElectricSlateTokens.PrimaryAccent,
                                                    fontSize = 12.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                )
                                                Text(
                                                    text = "[${stuck.activeScreen}]",
                                                    color = ElectricSlateTokens.TextMuted,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            Text(
                                                text = stuck.topMethod,
                                                color = ElectricSlateTokens.TextPrimary,
                                                fontSize = 12.sp,
                                                fontFamily = FontFamily.Monospace,
                                                maxLines = if (isExpanded) 4 else 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            if (isExpanded && stuck.fullStackTrace.isNotBlank()) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(ElectricSlateTokens.BgCanvas)
                                                        .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(6.dp))
                                                        .padding(8.dp)
                                                ) {
                                                    Text(
                                                        text = stuck.fullStackTrace.take(1500),
                                                        color = ElectricSlateTokens.TextMuted,
                                                        fontSize = 10.sp,
                                                        fontFamily = FontFamily.Monospace
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Developer Mode Section: Real-Time RAM Graph
                item {
                    AppCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            DiagnosticSectionHeader(
                                title = "Real-Time RAM Usage Timeline",
                                icon = Icons.AutoMirrored.Filled.ShowChart
                            )
                            RamTimelineGraph(samples = ramSamples)
                        }
                    }
                }

                // Developer Mode Section: Worker Memory Allocation Timeline
                item {
                    AppCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DiagnosticSectionHeader(
                                title = "Worker & Subsystem Memory Timeline (${workerRecords.size})",
                                icon = Icons.Default.Memory
                            )

                            if (workerRecords.isEmpty()) {
                                Text(
                                    text = "No worker executions recorded yet (Canvas autosave, DB maintenance, TileCache).",
                                    color = ElectricSlateTokens.TextMuted,
                                    fontSize = 12.sp,
                                    fontFamily = ElmsSans
                                )
                            } else {
                                for (rec in workerRecords.takeLast(6).reversed()) {
                                    val rowShape = RoundedCornerShape(10.dp)
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(rowShape)
                                            .background(ElectricSlateTokens.SurfaceElevated)
                                            .border(1.dp, ElectricSlateTokens.BorderSubtle, rowShape)
                                            .padding(10.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(
                                                    text = rec.workerName,
                                                    color = ElectricSlateTokens.TextPrimary,
                                                    fontSize = 13.sp,
                                                    fontFamily = ElmsSans,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                                Text(
                                                    text = "${rec.startHeapMb} MB → ${rec.endHeapMb} MB in ${rec.durationMs}ms",
                                                    color = ElectricSlateTokens.TextMuted,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace
                                                )
                                            }

                                            val isPositive = rec.deltaHeapMb > 0
                                            val badgeBg = if (isPositive) ElectricSlateTokens.Destructive.copy(alpha = 0.15f) else ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.15f)
                                            val badgeColor = if (isPositive) ElectricSlateTokens.Destructive else ElectricSlateTokens.PrimaryAccent
                                            val deltaStr = if (rec.deltaHeapMb >= 0) "+${rec.deltaHeapMb} MB" else "${rec.deltaHeapMb} MB"

                                            Box(
                                                modifier = Modifier
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(badgeBg)
                                                    .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(6.dp))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = deltaStr,
                                                    color = badgeColor,
                                                    fontSize = 11.sp,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Developer Mode Section: Memory Leak Hints & Anomalies
                item {
                    AppCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DiagnosticSectionHeader(
                                title = "Memory Leak Insights & Anomaly Detection",
                                icon = Icons.Default.Warning
                            )

                            if (leakHints.isEmpty()) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = ElectricSlateTokens.PrimaryAccent,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "0 Memory Leaks Detected (Heap trajectory healthy)",
                                        color = ElectricSlateTokens.TextPrimary,
                                        fontSize = 13.sp,
                                        fontFamily = ElmsSans
                                    )
                                }
                            } else {
                                for (hint in leakHints) {
                                    val hintShape = RoundedCornerShape(10.dp)
                                    val isCritical = hint.severity == LeakSeverity.CRITICAL
                                    val borderCol = if (isCritical) ElectricSlateTokens.Destructive else ElectricSlateTokens.BorderSubtle
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(hintShape)
                                            .background(ElectricSlateTokens.SurfaceElevated)
                                            .border(1.dp, borderCol, hintShape)
                                            .padding(10.dp)
                                    ) {
                                        Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                                            Text(
                                                text = hint.title,
                                                color = if (isCritical) ElectricSlateTokens.Destructive else ElectricSlateTokens.TextPrimary,
                                                fontSize = 13.sp,
                                                fontFamily = ElmsSans,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                text = hint.description,
                                                color = ElectricSlateTokens.TextMuted,
                                                fontSize = 11.sp,
                                                fontFamily = ElmsSans
                                            )
                                            Text(
                                                text = "Fix: ${hint.suggestedFix}",
                                                color = ElectricSlateTokens.PrimaryAccent,
                                                fontSize = 11.sp,
                                                fontFamily = ElmsSans
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Developer Mode Section: Force Garbage Collection Tester
                item {
                    AppCard(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            DiagnosticSectionHeader(
                                title = "Garbage Collection & Reclamation Test",
                                icon = Icons.Default.DeleteSweep
                            )

                            Text(
                                text = "Forces System.gc() and measures immediate heap reduction before and after finalization.",
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 12.sp,
                                fontFamily = ElmsSans
                            )

                            AppButton(
                                text = "Run System.gc() & Measure Freed RAM",
                                onClick = {
                                    lastGcResult = AppDiagnosticLogger.runManualGcAndMeasureDelta()
                                    Toast.makeText(context, "GC complete: freed ${lastGcResult?.freedMb}MB in ${lastGcResult?.durationMs}ms", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.fillMaxWidth()
                            )

                            lastGcResult?.let { res ->
                                Text(
                                    text = "Last GC freed ${res.freedMb} MB in ${res.durationMs}ms (${res.beforeUsedMb} MB → ${res.afterUsedMb} MB)",
                                    color = ElectricSlateTokens.PrimaryAccent,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }

            // Section 1: Cold Start Performance Metrics
            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        DiagnosticSectionHeader(
                            title = "Cold Startup Latency",
                            icon = Icons.Default.Speed
                        )

                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "${startupMetrics.totalColdStartMs}",
                                color = ElectricSlateTokens.PrimaryAccent,
                                fontSize = 32.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "ms total launch duration",
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 14.sp,
                                fontFamily = ElmsSans,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                        }

                        DiagnosticMetricRow("Application.onCreate", "${startupMetrics.appCreateDurationMs} ms")
                        DiagnosticMetricRow("AppContainer Init", "${startupMetrics.containerInitDurationMs} ms")
                        val setContentToFrame = if (startupMetrics.setContentStartMs > 0 && startupMetrics.firstFrameRenderedMs > 0) {
                            startupMetrics.firstFrameRenderedMs - startupMetrics.setContentStartMs
                        } else 0L
                        DiagnosticMetricRow("SetContent to First Frame", "$setContentToFrame ms")
                    }
                }
            }

            // Section 2: Memory & RAM Specifications
            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DiagnosticSectionHeader(
                            title = "Memory & RAM Specifications",
                            icon = Icons.Default.Memory
                        )

                        val heapPercent = if (hardwareInfo.jvmHeapMaxMb > 0) (hardwareInfo.jvmHeapUsedMb * 100 / hardwareInfo.jvmHeapMaxMb) else 0
                        DiagnosticMetricRow("JVM Heap Used", "${hardwareInfo.jvmHeapUsedMb} MB / ${hardwareInfo.jvmHeapMaxMb} MB ($heapPercent%)")
                        DiagnosticMetricRow("JVM Allocated Heap", "${hardwareInfo.jvmHeapTotalMb} MB allocated")
                        DiagnosticMetricRow("Native Heap", "${hardwareInfo.nativeHeapAllocatedMb} MB alloc / ${hardwareInfo.nativeHeapSizeMb} MB total")
                        DiagnosticMetricRow("System RAM", "${hardwareInfo.systemRamAvailableMb} MB free / ${hardwareInfo.systemRamTotalMb} MB total")
                        DiagnosticMetricRow("Low Memory State", if (hardwareInfo.isLowMemory) "ACTIVE (Low)" else "False (Normal)")
                        if (hardwareInfo.memoryClassMb > 0) {
                            DiagnosticMetricRow("App Memory Class", "Std ${hardwareInfo.memoryClassMb} MB / Large ${hardwareInfo.largeMemoryClassMb} MB")
                        }
                        DiagnosticMetricRow("ART GC Runs / Time", "${hardwareInfo.artGcCount} runs / ${hardwareInfo.artGcTimeMs} ms")
                    }
                }
            }

            // Section 3: Disk & Storage Footprint
            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DiagnosticSectionHeader(
                            title = "Disk & Storage Footprint",
                            icon = Icons.Default.Storage
                        )

                        DiagnosticMetricRow("Internal Storage", "${hardwareInfo.storageFreeMb} MB free / ${hardwareInfo.storageTotalMb} MB total")
                        DiagnosticMetricRow("App Files (filesDir)", "${hardwareInfo.appFilesKb} KB")
                        DiagnosticMetricRow("App Cache (cacheDir)", "${hardwareInfo.appCacheKb} KB")
                        DiagnosticMetricRow("SQLite Database (DB+WAL)", "${hardwareInfo.appDatabaseKb} KB")
                        val totalAppKb = hardwareInfo.appFilesKb + hardwareInfo.appCacheKb + hardwareInfo.appDatabaseKb
                        DiagnosticMetricRow("Total App Footprint", "${totalAppKb} KB (~${totalAppKb / 1024} MB)")
                    }
                }
            }

            // Section 4: Device Hardware & System Environment
            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        DiagnosticSectionHeader(
                            title = "Hardware & Environment",
                            icon = Icons.Default.PhoneAndroid
                        )

                        DiagnosticMetricRow("Device", "${hardwareInfo.brand} ${hardwareInfo.model} (${hardwareInfo.manufacturer})")
                        DiagnosticMetricRow("Board / Hardware", hardwareInfo.board)
                        DiagnosticMetricRow("Android OS", "Android ${hardwareInfo.androidVersion} (API ${hardwareInfo.apiLevel})")
                        DiagnosticMetricRow("Security Patch", hardwareInfo.securityPatch)
                        DiagnosticMetricRow("CPU Cores / ABIs", "${hardwareInfo.cpuCores} cores (${hardwareInfo.supportedAbis.take(24)})")
                        DiagnosticMetricRow("Display", "${hardwareInfo.displayResolution} @ ${hardwareInfo.displayDensityDpi} dpi (${String.format(Locale.US, "%.0f", hardwareInfo.displayRefreshRate)} Hz)")
                        DiagnosticMetricRow("Battery / Thermal", "${if (hardwareInfo.batteryLevelPct >= 0) "${hardwareInfo.batteryLevelPct}%" else "N/A"} / ${hardwareInfo.thermalStatus}")
                        DiagnosticMetricRow("Active Threads", "${hardwareInfo.activeThreadCount} runtime threads")
                    }
                }
            }

            // Section 5: Recorded Code Errors & Exceptions
            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            DiagnosticSectionHeader(
                                title = "Code Errors & Exceptions",
                                icon = Icons.Default.BugReport
                            )

                            val badgeBg = if (errors.isEmpty()) ElectricSlateTokens.SurfaceElevated else ElectricSlateTokens.Destructive.copy(alpha = 0.15f)
                            val badgeTextColor = if (errors.isEmpty()) ElectricSlateTokens.TextMuted else ElectricSlateTokens.Destructive
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(badgeBg)
                                    .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (errors.isEmpty()) "0 Errors (Clean)" else "${errors.size} Errors",
                                    color = badgeTextColor,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        val crashLog = hardwareInfo.previousCrashLog
                        if (crashLog != null) {
                            Text(
                                text = "Previous Crash Detected:",
                                color = ElectricSlateTokens.Destructive,
                                fontSize = 12.sp,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold
                            )
                            val boxShape = RoundedCornerShape(8.dp)
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(boxShape)
                                    .background(ElectricSlateTokens.SurfaceElevated)
                                    .border(1.dp, ElectricSlateTokens.BorderSubtle, boxShape)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = crashLog.take(800),
                                    color = ElectricSlateTokens.TextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace,
                                    maxLines = 8,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        if (errors.isEmpty() && crashLog == null) {
                            Text(
                                text = "No runtime errors or exceptions recorded during this session.",
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 13.sp,
                                fontFamily = ElmsSans
                            )
                        } else {
                            for (err in errors.take(5)) {
                                val errShape = RoundedCornerShape(8.dp)
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(errShape)
                                        .background(ElectricSlateTokens.SurfaceElevated)
                                        .border(1.dp, ElectricSlateTokens.BorderSubtle, errShape)
                                        .padding(8.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                        Text(
                                            text = "[${err.tag}] ${err.message}",
                                            color = ElectricSlateTokens.Destructive,
                                            fontSize = 12.sp,
                                            fontFamily = ElmsSans,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${err.exceptionClass} on ${err.threadName}",
                                            color = ElectricSlateTokens.TextMuted,
                                            fontSize = 11.sp,
                                            fontFamily = FontFamily.Monospace
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Section 6: Live Top Scrolling Overlay Toggle
            item {
                AppCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Top Scrolling Debug Ticker",
                                color = ElectricSlateTokens.TextPrimary,
                                fontSize = 16.sp,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Shows real-time startup latency, RAM, and scrolling activity logs at the top of the app",
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 12.sp,
                                fontFamily = ElmsSans,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }

                        Switch(
                            checked = isOverlayEnabled,
                            onCheckedChange = { enabled ->
                                AppDiagnosticLogger.setOverlayEnabled(context, enabled)
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = ElectricSlateTokens.TextPrimary,
                                checkedTrackColor = ElectricSlateTokens.PrimaryAccent,
                                uncheckedThumbColor = ElectricSlateTokens.TextMuted,
                                uncheckedTrackColor = ElectricSlateTokens.TrackInactive
                            )
                        )
                    }
                }
            }

            // Section 7: Export Diagnostic Report Button
            item {
                AppButton(
                    text = "Copy Full Diagnostic Report",
                    onClick = {
                        val report = AppDiagnosticLogger.generateFullReport(context)
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                        clipboard?.setPrimaryClip(ClipData.newPlainText("Fotara Diagnostic Report", report))
                        Toast.makeText(context, "Full diagnostic report copied to clipboard!", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Section 8: Recent Activity Event Log
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    DiagnosticSectionHeader(
                        title = "Activity Event Log (${events.size})",
                        icon = Icons.Default.Timeline
                    )
                    if (events.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(ElectricSlateTokens.SurfaceElevated)
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, RoundedCornerShape(8.dp))
                                .clickable { AppDiagnosticLogger.clearEvents() }
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "Clear Log",
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 11.sp,
                                fontFamily = ElmsSans,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Search Bar & Tag Filter Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val searchShape = RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(searchShape)
                            .background(ElectricSlateTokens.SurfacePanel)
                            .border(1.dp, ElectricSlateTokens.BorderSubtle, searchShape)
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = ElectricSlateTokens.TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = { searchQuery = it },
                                textStyle = TextStyle(
                                    color = ElectricSlateTokens.TextPrimary,
                                    fontSize = 13.sp,
                                    fontFamily = ElmsSans
                                ),
                                cursorBrush = SolidColor(ElectricSlateTokens.PrimaryAccent),
                                singleLine = true,
                                modifier = Modifier.weight(1f),
                                decorationBox = { innerTextField ->
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search event logs...",
                                            color = ElectricSlateTokens.TextMuted,
                                            fontSize = 13.sp,
                                            fontFamily = ElmsSans
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                            if (searchQuery.isNotEmpty()) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear search",
                                    tint = ElectricSlateTokens.TextMuted,
                                    modifier = Modifier
                                        .size(16.dp)
                                        .clickable { searchQuery = "" }
                                )
                            }
                        }
                    }

                    // Filter chips
                    val tagCategories = listOf("All", "Error", "Lifecycle", "Navigation", "Memory", "Pacing", "Worker")
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        for (tag in tagCategories) {
                            AppPillChip(
                                label = tag,
                                isSelected = selectedTagFilter == tag,
                                onClick = { selectedTagFilter = tag }
                            )
                        }
                    }
                }
            }

            val filteredEvents = events.reversed().filter { ev ->
                val matchesTag = when (selectedTagFilter) {
                    "All" -> true
                    "Error" -> ev.tag.contains("Error", ignoreCase = true) || ev.tag.contains("Crash", ignoreCase = true)
                    "Lifecycle" -> ev.tag.contains("Lifecycle", ignoreCase = true) || ev.tag.contains("Startup", ignoreCase = true)
                    "Navigation" -> ev.tag.contains("Navigation", ignoreCase = true)
                    "Memory" -> ev.tag.contains("Memory", ignoreCase = true) || ev.tag.contains("RAM", ignoreCase = true) || ev.tag.contains("GC", ignoreCase = true)
                    "Pacing" -> ev.tag.contains("Pacing", ignoreCase = true) || ev.tag.contains("Stuck", ignoreCase = true) || ev.tag.contains("Frame", ignoreCase = true)
                    "Worker" -> ev.tag.contains("Worker", ignoreCase = true) || ev.tag.contains("Tile", ignoreCase = true) || ev.tag.contains("Autosave", ignoreCase = true)
                    else -> ev.tag.equals(selectedTagFilter, ignoreCase = true)
                }
                val matchesSearch = if (searchQuery.isBlank()) true else {
                    ev.message.contains(searchQuery, ignoreCase = true) || ev.tag.contains(searchQuery, ignoreCase = true)
                }
                matchesTag && matchesSearch
            }

            if (filteredEvents.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 16.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank() || selectedTagFilter != "All") {
                                "No events matching \"$searchQuery\" in [$selectedTagFilter]"
                            } else {
                                "No activity events recorded yet."
                            },
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 12.sp,
                            fontFamily = ElmsSans
                        )
                    }
                }
            } else {
                items(filteredEvents, key = { it.id }) { ev ->
                    val timeStr = remember(ev.timestampMs) { timeFormat.format(Date(ev.timestampMs)) }
                    val rowShape = RoundedCornerShape(12.dp)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(rowShape)
                            .background(ElectricSlateTokens.SurfacePanel)
                            .border(1.dp, ElectricSlateTokens.BorderSubtle, rowShape)
                            .padding(12.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = ev.tag,
                                    color = if (ev.tag == "Error" || ev.tag == "StuckError") ElectricSlateTokens.Destructive else ElectricSlateTokens.PrimaryAccent,
                                    fontSize = 12.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "+${ev.elapsedSinceAppStartMs}ms ($timeStr)",
                                    color = ElectricSlateTokens.TextMuted,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                            Text(
                                text = ev.message,
                                color = ElectricSlateTokens.TextPrimary,
                                fontSize = 13.sp,
                                fontFamily = ElmsSans
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticSectionHeader(
    title: String,
    icon: ImageVector
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = ElectricSlateTokens.PrimaryAccent,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            color = ElectricSlateTokens.TextPrimary,
            fontSize = 16.sp,
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun DiagnosticMetricRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            color = ElectricSlateTokens.TextMuted,
            fontSize = 13.sp,
            fontFamily = ElmsSans
        )
        Text(
            text = value,
            color = ElectricSlateTokens.TextPrimary,
            fontSize = 13.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
