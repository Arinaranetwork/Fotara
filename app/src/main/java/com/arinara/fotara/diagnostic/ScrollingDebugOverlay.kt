// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.diagnostic

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import kotlinx.coroutines.delay

@Composable
fun ScrollingDebugOverlay(
    onOpenFullInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isEnabled by AppDiagnosticLogger.isOverlayEnabled.collectAsState()
    val startupMetrics by AppDiagnosticLogger.startupMetricsFlow.collectAsState()
    val events by AppDiagnosticLogger.eventsFlow.collectAsState()
    val currentScreen by AppDiagnosticLogger.currentScreen.collectAsState()

    val tickerScrollState = rememberScrollState()

    // Auto-scroll ticker to the latest event
    LaunchedEffect(events.size) {
        if (events.isNotEmpty()) {
            tickerScrollState.animateScrollTo(tickerScrollState.maxValue)
        }
    }

    AnimatedVisibility(
        visible = isEnabled,
        enter = slideInVertically { -it } + fadeIn(),
        exit = slideOutVertically { -it } + fadeOut(),
        modifier = modifier
    ) {
        val runtime = Runtime.getRuntime()
        val heapUsedMb = (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024)
        val heapMaxMb = runtime.maxMemory() / (1024 * 1024)
        val coldStartMs = startupMetrics.totalColdStartMs

        val shape = RoundedCornerShape(16.dp)

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.statusBars)
                .padding(horizontal = 12.dp, vertical = 4.dp)
                .clip(shape)
                .background(ElectricSlateTokens.SurfaceElevated)
                .border(1.dp, ElectricSlateTokens.BorderSubtle, shape)
                .clickable { onOpenFullInfo() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                // Top Row: Cold Start, Memory, Active Screen, Close/Info action
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(7.dp)
                                .clip(CircleShape)
                                .background(ElectricSlateTokens.PrimaryAccent)
                        )
                        Text(
                            text = if (coldStartMs > 0) "Cold: ${coldStartMs}ms" else "Loading...",
                            color = ElectricSlateTokens.TextPrimary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "|",
                            color = ElectricSlateTokens.BorderSubtle,
                            fontSize = 11.sp
                        )
                        Text(
                            text = "Heap: ${heapUsedMb}/${heapMaxMb}MB",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                        Text(
                            text = "|",
                            color = ElectricSlateTokens.BorderSubtle,
                            fontSize = 11.sp
                        )
                        Text(
                            text = currentScreen,
                            color = ElectricSlateTokens.PrimaryAccent,
                            fontSize = 11.sp,
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = "INFO",
                            color = ElectricSlateTokens.PrimaryAccent,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = ElmsSans
                        )
                    }
                }

                // Bottom Row: Scrolling Real-time Activity Log Ticker
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(tickerScrollState),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val displayEvents = events.takeLast(10)
                    if (displayEvents.isEmpty()) {
                        Text(
                            text = "Recording app activity...",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    } else {
                        for (ev in displayEvents) {
                            Text(
                                text = "[+${ev.elapsedSinceAppStartMs}ms ${ev.tag}: ${ev.message}]",
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }
    }
}
