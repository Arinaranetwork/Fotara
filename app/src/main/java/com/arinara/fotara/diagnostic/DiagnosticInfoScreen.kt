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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Memory
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.components.presets.AppButton
import com.arinara.fotara.ui.components.presets.AppCard
import com.arinara.fotara.ui.components.presets.AppScreen
import com.arinara.fotara.ui.components.presets.AppTopBar
import com.arinara.fotara.ui.theme.ElectricSlateTokens
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
    val startupMetrics by AppDiagnosticLogger.startupMetricsFlow.collectAsState()
    val events by AppDiagnosticLogger.eventsFlow.collectAsState()
    val hardwareInfo = remember(context) { AppDiagnosticLogger.getHardwareInfo(context) }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss.SSS", Locale.US) }

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
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
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

            // Section 2: Device Hardware & System Specifications
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
                        DiagnosticMetricRow("Board", hardwareInfo.board)
                        DiagnosticMetricRow("Android OS", "Android ${hardwareInfo.androidVersion} (API ${hardwareInfo.apiLevel})")
                        DiagnosticMetricRow("CPU Cores", "${hardwareInfo.cpuCores} active cores")
                        DiagnosticMetricRow("System RAM", "${hardwareInfo.systemRamAvailableMb} MB free / ${hardwareInfo.systemRamTotalMb} MB total")
                        DiagnosticMetricRow("JVM Heap", "${hardwareInfo.jvmHeapUsedMb} MB used / ${hardwareInfo.jvmHeapMaxMb} MB max")
                        DiagnosticMetricRow("Internal Storage", "${hardwareInfo.storageFreeMb} MB free / ${hardwareInfo.storageTotalMb} MB total")
                    }
                }
            }

            // Section 3: Live Top Scrolling Overlay Toggle
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

            // Section 4: Export Diagnostic Report Button
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

            // Section 5: Recent Activity Event Log
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
                }
            }

            items(events.reversed(), key = { it.id }) { ev ->
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
                                color = ElectricSlateTokens.PrimaryAccent,
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
