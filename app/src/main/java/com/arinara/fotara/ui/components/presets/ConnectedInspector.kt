// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components.presets

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.SpringPhysics

@Composable
fun ConnectedInspector(
    visible: Boolean,
    modifier: Modifier = Modifier,
    neckAnchorOffsetRatio: Float = 0.5f, // 0.0 = left, 0.5 = center, 1.0 = right
    containerColor: Color = ElectricSlateTokens.SurfaceElevated,
    borderColor: Color = ElectricSlateTokens.BorderSubtle,
    panelWidth: Dp = 280.dp,
    neckHeight: Dp = 20.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val scaleAnim = remember { Animatable(if (visible) 1f else 0f) }
    val alphaAnim = remember { Animatable(if (visible) 1f else 0f) }

    LaunchedEffect(visible) {
        if (visible) {
            scaleAnim.animateTo(1.0f, animationSpec = SpringPhysics.ViscousGooeySpec)
            alphaAnim.animateTo(1.0f, animationSpec = SpringPhysics.ViscousGooeySpec)
        } else {
            scaleAnim.animateTo(0.0f, animationSpec = SpringPhysics.DismissFadeSpec)
            alphaAnim.animateTo(0.0f, animationSpec = SpringPhysics.DismissFadeSpec)
        }
    }

    if (scaleAnim.value > 0.01f) {
        Column(
            modifier = modifier
                .graphicsLayer {
                    scaleX = scaleAnim.value
                    scaleY = scaleAnim.value
                    alpha = alphaAnim.value
                    transformOrigin = androidx.compose.ui.graphics.TransformOrigin(neckAnchorOffsetRatio, 1f)
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Main Inspector Panel Card
            Box(
                modifier = Modifier
                    .width(panelWidth)
                    .clip(RoundedCornerShape(20.dp))
                    .background(containerColor)
                    .border(1.dp, borderColor, RoundedCornerShape(20.dp))
                    .padding(16.dp)
            ) {
                Column {
                    content()
                }
            }

            // Organic Droplet Neck bridging panel to floating bar below
            Canvas(
                modifier = Modifier
                    .width(panelWidth)
                    .height(neckHeight)
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height
                val anchorCenterX = canvasWidth * neckAnchorOffsetRatio.coerceIn(0.15f, 0.85f)

                val topNeckHalfWidth = 24.dp.toPx()
                val bottomNeckHalfWidth = 14.dp.toPx()

                val path = Path().apply {
                    // Top attachment point (broad base at bottom of card)
                    moveTo(anchorCenterX - topNeckHalfWidth, 0f)
                    lineTo(anchorCenterX + topNeckHalfWidth, 0f)

                    // Right organic bezier curve down to bottom tool anchor
                    cubicTo(
                        anchorCenterX + topNeckHalfWidth * 0.7f, canvasHeight * 0.45f,
                        anchorCenterX + bottomNeckHalfWidth * 1.3f, canvasHeight * 0.65f,
                        anchorCenterX + bottomNeckHalfWidth, canvasHeight
                    )

                    // Bottom attachment line
                    lineTo(anchorCenterX - bottomNeckHalfWidth, canvasHeight)

                    // Left organic bezier curve up to card
                    cubicTo(
                        anchorCenterX - bottomNeckHalfWidth * 1.3f, canvasHeight * 0.65f,
                        anchorCenterX - topNeckHalfWidth * 0.7f, canvasHeight * 0.45f,
                        anchorCenterX - topNeckHalfWidth, 0f
                    )
                    close()
                }

                drawPath(path = path, color = containerColor)
            }
        }
    }
}
