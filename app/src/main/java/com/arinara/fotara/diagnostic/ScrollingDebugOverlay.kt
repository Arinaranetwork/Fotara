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
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.theme.ElectricSlateTokens

@Composable
fun ScrollingDebugOverlay(
    onOpenFullInfo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isMasterEnabled by AppDiagnosticLogger.isDiagnosticSystemEnabled.collectAsState()
    val isEnabled by AppDiagnosticLogger.isOverlayEnabled.collectAsState()
    val startupMetrics by AppDiagnosticLogger.startupMetricsFlow.collectAsState()
    val events by AppDiagnosticLogger.eventsFlow.collectAsState()
    val currentScreen by AppDiagnosticLogger.currentScreen.collectAsState()
    val framePacing by AppDiagnosticLogger.framePacingFlow.collectAsState()
    val latestStuck by AppDiagnosticLogger.latestStuckAlertFlow.collectAsState()

    val tickerScrollState = rememberScrollState()

    // Auto-scroll ticker to the latest event
    LaunchedEffect(events.lastOrNull()?.id) {
        if (events.isNotEmpty()) {
            tickerScrollState.animateScrollTo(tickerScrollState.maxValue)
        }
    }

    AnimatedVisibility(
        visible = isMasterEnabled && isEnabled,
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
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Row 1: Cold Start, Memory, Active Screen, Info link
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

                    Text(
                        text = "INFO",
                        color = ElectricSlateTokens.PrimaryAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = ElmsSans
                    )
                }

                // Row 2: Live Frame Pacing Telemetry & Stuck Error Alert Badge
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        // Live FPS & 1% Low Badge
                        val badgeShape = RoundedCornerShape(6.dp)
                        Box(
                            modifier = Modifier
                                .clip(badgeShape)
                                .background(ElectricSlateTokens.SurfacePanel)
                                .border(1.dp, ElectricSlateTokens.BorderSubtle, badgeShape)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "${framePacing.currentFps} FPS",
                                color = ElectricSlateTokens.PrimaryAccent,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "1% Low: ${framePacing.onePercentLowFps}",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )

                        Text(
                            text = "•",
                            color = ElectricSlateTokens.BorderSubtle,
                            fontSize = 10.sp
                        )

                        Text(
                            text = "${framePacing.pacingScorePct.toInt()}% Pace",
                            color = if (framePacing.pacingScorePct >= 90f) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.Destructive,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    // Stuck Warning Alert
                    if (latestStuck != null) {
                        val stuck = latestStuck!!
                        val alertShape = RoundedCornerShape(6.dp)
                        val alertColor = if (stuck.isSevereFreeze) ElectricSlateTokens.Destructive else ElectricSlateTokens.PrimaryAccent
                        Box(
                            modifier = Modifier
                                .clip(alertShape)
                                .background(alertColor.copy(alpha = 0.18f))
                                .border(1.dp, alertColor, alertShape)
                                .clickable {
                                    AppDiagnosticLogger.clearStuckAlert()
                                    onOpenFullInfo()
                                }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "STUCK: ${stuck.durationMs}ms",
                                color = alertColor,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Row 3: Mini Canvas Sparkline Graph (30dp height)
                if (framePacing.rollingDurationsMs.size >= 2) {
                    OverlaySparklineGraph(
                        durationsMs = framePacing.rollingDurationsMs,
                        targetIntervalMs = framePacing.targetFrameIntervalMs,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(28.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(ElectricSlateTokens.BgCanvas)
                            .padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }

                // Row 4: Scrolling Activity Log Ticker
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

@Composable
private fun OverlaySparklineGraph(
    durationsMs: List<Float>,
    targetIntervalMs: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val count = durationsMs.size
        val stepX = w / (count - 1).coerceAtLeast(1)
        val maxDuration = maxOf(40f, durationsMs.maxOrNull() ?: 33.3f)

        // Draw 16.6ms and 33.3ms dashed guide lines
        val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
        val y16 = (h - (16.67f / maxDuration) * h).coerceIn(0f, h)
        drawLine(
            color = ElectricSlateTokens.BorderSubtle,
            start = Offset(0f, y16),
            end = Offset(w, y16),
            strokeWidth = 1f,
            pathEffect = dashedEffect
        )

        val y33 = (h - (33.33f / maxDuration) * h).coerceIn(0f, h)
        drawLine(
            color = ElectricSlateTokens.Destructive.copy(alpha = 0.35f),
            start = Offset(0f, y33),
            end = Offset(w, y33),
            strokeWidth = 1f,
            pathEffect = dashedEffect
        )

        val curvePath = Path()
        val fillPath = Path()

        val points = durationsMs.mapIndexed { index, dur ->
            val normY = (dur / maxDuration).coerceIn(0f, 1f)
            val x = index * stepX
            val y = (h - (normY * (h - 6f)) - 3f).coerceIn(0f, h)
            Offset(x, y)
        }

        if (points.isNotEmpty()) {
            curvePath.moveTo(points.first().x, points.first().y)
            fillPath.moveTo(points.first().x, h)
            fillPath.lineTo(points.first().x, points.first().y)

            for (i in 1 until points.size) {
                val p0 = points[i - 1]
                val p1 = points[i]
                val cx = (p0.x + p1.x) / 2f
                curvePath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                fillPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
            }

            fillPath.lineTo(points.last().x, h)
            fillPath.close()

            // Area gradient
            val fillBrush = Brush.verticalGradient(
                colors = listOf(
                    ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.25f),
                    Color.Transparent
                ),
                startY = 0f,
                endY = h
            )
            drawPath(fillPath, brush = fillBrush)

            // Curve stroke
            drawPath(
                path = curvePath,
                color = ElectricSlateTokens.PrimaryAccent,
                style = Stroke(
                    width = 1.5.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )

            // Draw jank spike dots for frames > 24ms
            for (i in points.indices) {
                val dur = durationsMs[i]
                if (dur > 24f) {
                    val dotColor = if (dur > 33.3f) ElectricSlateTokens.Destructive else ElectricSlateTokens.TextPrimary
                    drawCircle(
                        color = dotColor,
                        radius = 2.dp.toPx(),
                        center = points[i]
                    )
                }
            }
        }
    }
}
