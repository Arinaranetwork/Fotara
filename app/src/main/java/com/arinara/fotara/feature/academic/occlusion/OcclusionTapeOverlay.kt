// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.academic.occlusion

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Quiz
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.HomeAddButtonBlue
import com.arinara.fotara.theme.HomeCardBorder
import com.arinara.fotara.theme.HomeCardSurface
import com.arinara.fotara.theme.HomeNearBlack
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary
import com.arinara.fotara.theme.TextSecondary
import java.util.Locale

@Composable
fun OcclusionTapeOverlay(
    engine: OcclusionTapeEngine,
    modifier: Modifier = Modifier,
    isQuizModeActive: Boolean = true,
    onTapeTapped: ((OcclusionTape) -> Unit)? = null
) {
    val tapes by engine.tapes.collectAsState()
    val textMeasurer = rememberTextMeasurer()

    Column(modifier = modifier.fillMaxSize()) {
        // Study Bar if Quiz Mode Active
        if (isQuizModeActive && tapes.isNotEmpty()) {
            OcclusionQuizBar(
                revealedCount = engine.revealedCount,
                totalCount = engine.totalCount,
                onRevealAll = { engine.revealAll() },
                onHideAll = { engine.hideAll() },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }

        // Interactive Tape Canvas Layer
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val containerWidth = constraints.maxWidth.toFloat()
            val containerHeight = constraints.maxHeight.toFloat()

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(tapes, containerWidth, containerHeight) {
                        detectTapGestures { offset ->
                            if (containerWidth > 0f && containerHeight > 0f) {
                                val normX = offset.x / containerWidth
                                val normY = offset.y / containerHeight
                                val tappedTape = engine.findTapeAt(normX, normY)
                                if (tappedTape != null) {
                                    engine.toggleReveal(tappedTape.id)
                                    onTapeTapped?.invoke(tappedTape)
                                }
                            }
                        }
                    }
            ) {
                for (tape in tapes) {
                    drawOcclusionTape(
                        tape = tape,
                        containerWidth = containerWidth,
                        containerHeight = containerHeight,
                        textMeasurer = textMeasurer
                    )
                }
            }
        }
    }
}

private fun DrawScope.drawOcclusionTape(
    tape: OcclusionTape,
    containerWidth: Float,
    containerHeight: Float,
    textMeasurer: TextMeasurer
) {
    val left = tape.x * containerWidth
    val top = tape.y * containerHeight
    val width = tape.width * containerWidth
    val height = tape.height * containerHeight

    val tapeSize = Size(width, height)
    val tapeTopLeft = Offset(left, top)
    val cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())

    if (tape.isRevealed) {
        // Revealed State: Translucent fill with dashed border
        drawRoundRect(
            color = Color(0xFF2563EB).copy(alpha = 0.08f),
            topLeft = tapeTopLeft,
            size = tapeSize,
            cornerRadius = cornerRadius,
            style = Fill
        )

        val dashedEffect = PathEffect.dashPathEffect(floatArrayOf(12f, 10f), 0f)
        drawRoundRect(
            color = Color(0xFFEFE8DA).copy(alpha = 0.45f),
            topLeft = tapeTopLeft,
            size = tapeSize,
            cornerRadius = cornerRadius,
            style = Stroke(width = 1.5.dp.toPx(), pathEffect = dashedEffect)
        )

        // Draw revealed label if available
        if (tape.label.isNotBlank()) {
            val textLayoutResult = textMeasurer.measure(
                text = tape.label,
                style = TextStyle(
                    fontFamily = ElmsSans,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp,
                    color = Color(0xFFEFE8DA)
                )
            )
            val textX = left + (width - textLayoutResult.size.width) / 2f
            val textY = top + (height - textLayoutResult.size.height) / 2f

            if (textLayoutResult.size.width <= width && textLayoutResult.size.height <= height) {
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(textX, textY)
                )
            }
        }
    } else {
        // Hidden State: Opaque dark-accent study tape
        val tapeFill = Color(0xFF141B2A)
        val tapeBorder = Color(0xFFEFE8DA).copy(alpha = 0.85f)

        drawRoundRect(
            color = tapeFill,
            topLeft = tapeTopLeft,
            size = tapeSize,
            cornerRadius = cornerRadius,
            style = Fill
        )

        drawRoundRect(
            color = tapeBorder,
            topLeft = tapeTopLeft,
            size = tapeSize,
            cornerRadius = cornerRadius,
            style = Stroke(width = 1.5.dp.toPx())
        )

        // Subtle diagonal grip hatch lines across the tape
        val hatchSpacing = 16.dp.toPx()
        val hatchColor = Color(0xFF2563EB).copy(alpha = 0.22f)
        var xOffset = 0f
        while (xOffset < width + height) {
            val startX = (left + xOffset).coerceIn(left, left + width)
            val startY = (top + xOffset - width).coerceIn(top, top + height)
            val endX = (left + xOffset - height).coerceIn(left, left + width)
            val endY = (top + xOffset).coerceIn(top, top + height)

            if (startX != endX && startY != endY) {
                drawLine(
                    color = hatchColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = 1.5.dp.toPx()
                )
            }
            xOffset += hatchSpacing
        }
    }
}

@Composable
private fun OcclusionQuizBar(
    revealedCount: Int,
    totalCount: Int,
    onRevealAll: () -> Unit,
    onHideAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(HomeCardSurface)
            .border(1.dp, HomeCardBorder, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Quiz,
                    contentDescription = null,
                    tint = Color(0xFFEFE8DA),
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Active Recall Quiz Mode",
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        color = TextPrimary
                    )
                    Text(
                        text = String.format(Locale.US, "Revealed: %d / %d", revealedCount, totalCount),
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Light,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        color = TextMuted
                    )
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF141B2A))
                        .border(1.dp, HomeCardBorder, RoundedCornerShape(8.dp))
                        .clickable { onHideAll() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.VisibilityOff,
                            contentDescription = "Hide All",
                            tint = TextSecondary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Hide All",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = TextSecondary
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(HomeAddButtonBlue.copy(alpha = 0.22f))
                        .border(1.dp, HomeAddButtonBlue.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
                        .clickable { onRevealAll() }
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Visibility,
                            contentDescription = "Reveal All",
                            tint = Color(0xFFEFE8DA),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Reveal All",
                            fontFamily = ElmsSans,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            color = Color(0xFFEFE8DA)
                        )
                    }
                }
            }
        }
    }
}
