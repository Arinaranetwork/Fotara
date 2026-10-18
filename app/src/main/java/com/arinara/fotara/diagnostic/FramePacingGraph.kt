// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.diagnostic

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import java.util.Locale

@Composable
fun FramePacingGraph(
    snapshot: FramePacingSnapshot,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    var scrubIndex by remember { mutableStateOf<Int?>(null) }
    val scrollState = rememberScrollState()
    val durations = snapshot.rollingDurationsMs

    // Auto-scroll to the latest frames when user is not scrubbing
    LaunchedEffect(durations.size, scrubIndex) {
        if (scrubIndex == null && durations.size > 1) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(200.dp)
            .clip(shape)
            .background(ElectricSlateTokens.SurfacePanel)
            .border(1.dp, ElectricSlateTokens.BorderSubtle, shape)
            .padding(12.dp)
    ) {
        if (durations.size < 2) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Measuring live vsync frame intervals...",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 12.sp,
                    fontFamily = ElmsSans
                )
            }
        } else {
            val maxDurationRaw = durations.maxOrNull() ?: 33.3f
            val maxDuration = maxOf(45f, maxDurationRaw)

            Column(modifier = Modifier.fillMaxSize()) {
                // Header: Metrics & Scrubbed details
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeDuration = scrubIndex?.let { durations.getOrNull(it) }
                    if (activeDuration != null) {
                        val equivFps = if (activeDuration > 0f) (1000f / activeDuration).toInt() else 0
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = String.format(Locale.US, "%.1f ms", activeDuration),
                                color = if (activeDuration > 33.3f) ElectricSlateTokens.Destructive else ElectricSlateTokens.PrimaryAccent,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "($equivFps FPS)",
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    } else {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${snapshot.currentFps} FPS",
                                color = ElectricSlateTokens.PrimaryAccent,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• 1% Low: ${snapshot.onePercentLowFps} FPS",
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace
                            )
                            Text(
                                text = "• ${snapshot.pacingScorePct.toInt()}% Pace",
                                color = if (snapshot.pacingScorePct >= 90f) ElectricSlateTokens.TextPrimary else ElectricSlateTokens.Destructive,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (scrollState.maxValue > 0) {
                            Text(
                                text = "Scrollable",
                                color = ElectricSlateTokens.PrimaryAccent,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                        Text(
                            text = "Peak: ${String.format(Locale.US, "%.1f", maxDurationRaw)}ms",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Horizontally Scrollable Frame Duration Canvas
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 8.dp)
                ) {
                    val containerWidth = maxWidth
                    val contentWidth = maxOf(containerWidth, (durations.size * 12).dp)

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .horizontalScroll(scrollState)
                            .width(contentWidth)
                            .pointerInput(durations) {
                                detectTapGestures(
                                    onPress = { offset ->
                                        val stepX = size.width / (durations.size - 1).coerceAtLeast(1)
                                        val index = (offset.x / stepX).toInt().coerceIn(0, durations.lastIndex)
                                        scrubIndex = index
                                        tryAwaitRelease()
                                        scrubIndex = null
                                    }
                                )
                            }
                            .pointerInput(durations) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val stepX = size.width / (durations.size - 1).coerceAtLeast(1)
                                        val index = (offset.x / stepX).toInt().coerceIn(0, durations.lastIndex)
                                        scrubIndex = index
                                    },
                                    onDragEnd = { scrubIndex = null },
                                    onDragCancel = { scrubIndex = null },
                                    onDrag = { change, _ ->
                                        val stepX = size.width / (durations.size - 1).coerceAtLeast(1)
                                        val index = (change.position.x / stepX).toInt().coerceIn(0, durations.lastIndex)
                                        scrubIndex = index
                                    }
                                )
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val count = durations.size
                            val stepX = w / (count - 1).coerceAtLeast(1)

                            // 16.6ms guide line (dashed BorderSubtle)
                            val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f), 0f)
                            val y16 = (h - (16.67f / maxDuration) * h).coerceIn(0f, h)
                            drawLine(
                                color = ElectricSlateTokens.BorderSubtle,
                                start = Offset(0f, y16),
                                end = Offset(w, y16),
                                strokeWidth = 1f,
                                pathEffect = dashedEffect
                            )

                            // 33.3ms guide line (dashed Destructive)
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

                            val points = durations.mapIndexed { index, dur ->
                                val normY = (dur / maxDuration).coerceIn(0f, 1f)
                                val x = index * stepX
                                val y = (h - (normY * (h - 16f)) - 8f).coerceIn(0f, h)
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

                                val fillBrush = Brush.verticalGradient(
                                    colors = listOf(
                                        ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.35f),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = h
                                )
                                drawPath(fillPath, brush = fillBrush)

                                drawPath(
                                    path = curvePath,
                                    color = ElectricSlateTokens.PrimaryAccent,
                                    style = Stroke(
                                        width = 2.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )

                                // Highlight frame spikes > 24ms
                                for (i in points.indices) {
                                    val dur = durations[i]
                                    if (dur > 24f) {
                                        val spikeColor = if (dur > 33.3f) ElectricSlateTokens.Destructive else ElectricSlateTokens.TextPrimary
                                        drawCircle(
                                            color = spikeColor,
                                            radius = 3.dp.toPx(),
                                            center = points[i]
                                        )
                                    }
                                }
                            }

                            // Scrubbing cursor
                            scrubIndex?.let { idx ->
                                if (idx in points.indices) {
                                    val pt = points[idx]
                                    drawLine(
                                        color = ElectricSlateTokens.TextPrimary.copy(alpha = 0.6f),
                                        start = Offset(pt.x, 0f),
                                        end = Offset(pt.x, h),
                                        strokeWidth = 1.5.dp.toPx()
                                    )
                                    drawCircle(
                                        color = ElectricSlateTokens.BgCanvas,
                                        radius = 6.dp.toPx(),
                                        center = pt
                                    )
                                    drawCircle(
                                        color = if (durations[idx] > 33.3f) ElectricSlateTokens.Destructive else ElectricSlateTokens.PrimaryAccent,
                                        radius = 4.dp.toPx(),
                                        center = pt
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
