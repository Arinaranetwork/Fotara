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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

@Composable
fun RamTimelineGraph(
    samples: List<RamSample>,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(16.dp)
    var scrubIndex by remember { mutableStateOf<Int?>(null) }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.US) }
    val scrollState = rememberScrollState()

    // Smoothly auto-scroll to the latest samples when not scrubbing
    LaunchedEffect(samples.size, scrubIndex) {
        if (scrubIndex == null && samples.size > 1) {
            scrollState.animateScrollTo(scrollState.maxValue)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(190.dp)
            .clip(shape)
            .background(ElectricSlateTokens.SurfacePanel)
            .border(1.dp, ElectricSlateTokens.BorderSubtle, shape)
            .padding(12.dp)
    ) {
        if (samples.size < 2) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Accumulating real-time RAM timeline samples...",
                    color = ElectricSlateTokens.TextMuted,
                    fontSize = 12.sp,
                    fontFamily = ElmsSans
                )
            }
        } else {
            val minVal = (samples.minOfOrNull { it.jvmUsedMb } ?: 0L).toFloat()
            val maxValRaw = (samples.maxOfOrNull { it.jvmUsedMb } ?: 100L).toFloat()
            val maxVal = max(maxValRaw, minVal + 10f)

            Column(modifier = Modifier.fillMaxSize()) {
                // Top Legend & Scrubbed Value Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val activeSample = scrubIndex?.let { samples.getOrNull(it) } ?: samples.lastOrNull()
                    if (activeSample != null) {
                        val timeStr = timeFormat.format(Date(activeSample.timestampMs))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${activeSample.jvmUsedMb} MB",
                                color = ElectricSlateTokens.PrimaryAccent,
                                fontSize = 15.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "• [${activeSample.activeScreen}] at $timeStr",
                                color = ElectricSlateTokens.TextMuted,
                                fontSize = 11.sp,
                                fontFamily = ElmsSans
                            )
                        }
                    } else {
                        Text(
                            text = "Live Heap Usage",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 12.sp,
                            fontFamily = ElmsSans
                        )
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
                            text = "Peak: ${maxVal.toInt()} MB",
                            color = ElectricSlateTokens.TextMuted,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                // Interactive Horizontally Scrollable Canvas Graph
                BoxWithConstraints(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(top = 8.dp)
                ) {
                    val containerWidth = maxWidth
                    // Dynamic width: min 14dp per sample for comfortable horizontal pan
                    val contentWidth = maxOf(containerWidth, (samples.size * 14).dp)

                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .horizontalScroll(scrollState)
                            .width(contentWidth)
                            .pointerInput(samples) {
                                detectTapGestures(
                                    onPress = { offset ->
                                        val stepX = size.width / (samples.size - 1).coerceAtLeast(1)
                                        val index = (offset.x / stepX).toInt().coerceIn(0, samples.lastIndex)
                                        scrubIndex = index
                                        tryAwaitRelease()
                                        scrubIndex = null
                                    }
                                )
                            }
                            .pointerInput(samples) {
                                detectDragGestures(
                                    onDragStart = { offset ->
                                        val stepX = size.width / (samples.size - 1).coerceAtLeast(1)
                                        val index = (offset.x / stepX).toInt().coerceIn(0, samples.lastIndex)
                                        scrubIndex = index
                                    },
                                    onDragEnd = { scrubIndex = null },
                                    onDragCancel = { scrubIndex = null },
                                    onDrag = { change, _ ->
                                        val stepX = size.width / (samples.size - 1).coerceAtLeast(1)
                                        val index = (change.position.x / stepX).toInt().coerceIn(0, samples.lastIndex)
                                        scrubIndex = index
                                    }
                                )
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val w = size.width
                            val h = size.height
                            val count = samples.size
                            val stepX = w / (count - 1).coerceAtLeast(1)
                            val rangeY = (maxVal - minVal).coerceAtLeast(1f)

                            // Draw background horizontal grid guides
                            val gridLineCount = 3
                            val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
                            for (i in 0..gridLineCount) {
                                val y = h * (i.toFloat() / gridLineCount)
                                drawLine(
                                    color = ElectricSlateTokens.BorderSubtle,
                                    start = Offset(0f, y),
                                    end = Offset(w, y),
                                    strokeWidth = 1f,
                                    pathEffect = dashedEffect
                                )
                            }

                            // Build curve path and fill path
                            val curvePath = Path()
                            val fillPath = Path()

                            val points = samples.mapIndexed { index, sample ->
                                val normY = ((sample.jvmUsedMb - minVal) / rangeY).coerceIn(0f, 1f)
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

                                // Draw gradient area fill
                                val fillBrush = Brush.verticalGradient(
                                    colors = listOf(
                                        ElectricSlateTokens.PrimaryAccent.copy(alpha = 0.35f),
                                        Color.Transparent
                                    ),
                                    startY = 0f,
                                    endY = h
                                )
                                drawPath(fillPath, brush = fillBrush)

                                // Draw line stroke
                                drawPath(
                                    path = curvePath,
                                    color = ElectricSlateTokens.PrimaryAccent,
                                    style = Stroke(
                                        width = 2.dp.toPx(),
                                        cap = StrokeCap.Round,
                                        join = StrokeJoin.Round
                                    )
                                )
                            }

                            // Draw scrubbing cursor if active
                            scrubIndex?.let { idx ->
                                if (idx in points.indices) {
                                    val pt = points[idx]
                                    drawLine(
                                        color = ElectricSlateTokens.TextPrimary.copy(alpha = 0.5f),
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
                                        color = ElectricSlateTokens.PrimaryAccent,
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
