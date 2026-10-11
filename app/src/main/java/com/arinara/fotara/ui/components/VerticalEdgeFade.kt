// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Reusable vertical edge fade modifier applying an alpha mask to the top and/or bottom edges
 * of scrollable content using offscreen compositing and [BlendMode.DstIn].
 *
 * Runs during the draw phase with zero recomposition overhead per scroll frame.
 */
fun Modifier.verticalEdgeFade(
    top: Dp = 0.dp,
    bottom: Dp = 0.dp
): Modifier {
    if (top <= 0.dp && bottom <= 0.dp) return this
    return this
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()
            val topFadePx = top.toPx()
            val bottomFadePx = bottom.toPx()
            val height = size.height

            if (topFadePx > 0f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = 0f,
                        endY = topFadePx
                    ),
                    blendMode = BlendMode.DstIn
                )
            }

            if (bottomFadePx > 0f && height > bottomFadePx) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startY = height - bottomFadePx,
                        endY = height
                    ),
                    blendMode = BlendMode.DstIn
                )
            }
        }
}
