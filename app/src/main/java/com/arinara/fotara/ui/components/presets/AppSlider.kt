// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components.presets

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.SpringPhysics
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AppSlider(
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    valueRange: ClosedFloatingPointRange<Float> = 0f..1f,
    steps: Int = 4, // Number of discrete intermediate steps (total dots = steps + 2)
    enabled: Boolean = true,
    activeTrackColor: Color = ElectricSlateTokens.PrimaryAccent,
    inactiveTrackColor: Color = ElectricSlateTokens.TrackInactive,
    dotInactiveColor: Color = ElectricSlateTokens.BorderSubtle,
    dotActiveColor: Color = ElectricSlateTokens.PrimaryAccent,
    thumbColor: Color = ElectricSlateTokens.PrimaryAccent
) {
    val coroutineScope = rememberCoroutineScope()
    var isDragging by remember { mutableStateOf(false) }
    val thumbScaleAnim = remember { Animatable(1.0f) }
    val thumbSnapBounceAnim = remember { Animatable(0f) } // horizontal magnetic snap overshoot

    val totalPoints = (steps + 2).coerceAtLeast(2)
    val stepSize = (valueRange.endInclusive - valueRange.start) / (totalPoints - 1)

    LaunchedEffect(isDragging) {
        if (isDragging) {
            thumbScaleAnim.animateTo(1.25f, SpringPhysics.JigglePressSpec)
        } else {
            thumbScaleAnim.animateTo(1.0f, SpringPhysics.JigglePressSpec)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(48.dp)
            .pointerInput(enabled, totalPoints) {
                if (!enabled) return@pointerInput
                detectTapGestures(
                    onPress = { offset ->
                        isDragging = true
                        val fraction = (offset.x / size.width).coerceIn(0f, 1f)
                        val rawVal = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                        val stepIndex = ((rawVal - valueRange.start) / stepSize).roundToInt().coerceIn(0, totalPoints - 1)
                        val snappedVal = valueRange.start + stepIndex * stepSize

                        coroutineScope.launch {
                            thumbSnapBounceAnim.snapTo(-4f)
                            thumbSnapBounceAnim.animateTo(0f, SpringPhysics.JigglePressSpec)
                        }
                        onValueChange(snappedVal)
                        tryAwaitRelease()
                        isDragging = false
                    }
                )
            }
            .pointerInput(enabled, totalPoints) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDragStart = {
                        isDragging = true
                    },
                    onDragEnd = {
                        isDragging = false
                    },
                    onDragCancel = {
                        isDragging = false
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val fraction = (change.position.x / size.width).coerceIn(0f, 1f)
                        val rawVal = valueRange.start + fraction * (valueRange.endInclusive - valueRange.start)
                        val stepIndex = ((rawVal - valueRange.start) / stepSize).roundToInt().coerceIn(0, totalPoints - 1)
                        val snappedVal = valueRange.start + stepIndex * stepSize

                        if (snappedVal != value) {
                            coroutineScope.launch {
                                val dir = if (snappedVal > value) 6f else -6f
                                thumbSnapBounceAnim.snapTo(dir)
                                thumbSnapBounceAnim.animateTo(0f, SpringPhysics.JigglePressSpec)
                            }
                            onValueChange(snappedVal)
                        }
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val canvasWidth = size.width
            val canvasHeight = size.height
            val centerY = canvasHeight / 2f
            val trackHeight = 6.dp.toPx()
            val thumbRadius = 10.dp.toPx()

            val normalizedFraction = ((value - valueRange.start) / (valueRange.endInclusive - valueRange.start)).coerceIn(0f, 1f)
            val thumbX = (normalizedFraction * canvasWidth) + thumbSnapBounceAnim.value

            // 1. Draw Inactive Track
            drawRoundRect(
                color = inactiveTrackColor,
                topLeft = Offset(0f, centerY - trackHeight / 2f),
                size = Size(canvasWidth, trackHeight),
                cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
            )

            // 2. Draw Active Track Fill
            if (thumbX > 0f) {
                drawRoundRect(
                    color = activeTrackColor,
                    topLeft = Offset(0f, centerY - trackHeight / 2f),
                    size = Size(thumbX.coerceAtMost(canvasWidth), trackHeight),
                    cornerRadius = CornerRadius(trackHeight / 2f, trackHeight / 2f)
                )
            }

            // 3. Draw Discrete Step Dots (4.dp)
            val dotRadius = 2.dp.toPx()
            for (i in 0 until totalPoints) {
                val dotX = (i.toFloat() / (totalPoints - 1)) * canvasWidth
                val isPassed = dotX <= thumbX + 1f
                drawCircle(
                    color = if (isPassed) dotActiveColor else dotInactiveColor,
                    radius = dotRadius,
                    center = Offset(dotX, centerY)
                )
            }

            // 4. Draw Scaled Tactile Thumb with Ambient Glow
            val currentThumbRadius = thumbRadius * thumbScaleAnim.value
            // Subtle ambient glow
            drawCircle(
                color = activeTrackColor.copy(alpha = 0.25f),
                radius = currentThumbRadius + 6.dp.toPx(),
                center = Offset(thumbX.coerceIn(0f, canvasWidth), centerY)
            )
            // Core thumb
            drawCircle(
                color = thumbColor,
                radius = currentThumbRadius,
                center = Offset(thumbX.coerceIn(0f, canvasWidth), centerY)
            )
            // Inner white dot
            drawCircle(
                color = Color.White,
                radius = currentThumbRadius * 0.35f,
                center = Offset(thumbX.coerceIn(0f, canvasWidth), centerY)
            )
        }
    }
}
