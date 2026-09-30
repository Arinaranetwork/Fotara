// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics

enum class GlowCorner {
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight
}

/**
 * Renders a subtle radial gradient corner glow indicating LinkIt spatial grouping.
 * Compatible from API 24 to 36 without relying on blur RenderEffect shaders.
 */
fun Modifier.linkItCornerGlow(
    isLinked: Boolean,
    glowColor: Color = Color(0xFFF77F00),
    corner: GlowCorner = GlowCorner.TopLeft,
    radiusRatio: Float = 0.42f
): Modifier {
    if (!isLinked) return this

    return this
        .semantics {
            contentDescription = "Linked item"
        }
        .drawWithCache {
            val radius = size.minDimension * radiusRatio
            val center = when (corner) {
                GlowCorner.TopLeft -> Offset(0f, 0f)
                GlowCorner.TopRight -> Offset(size.width, 0f)
                GlowCorner.BottomLeft -> Offset(0f, size.height)
                GlowCorner.BottomRight -> Offset(size.width, size.height)
            }
            val brush = Brush.radialGradient(
                colors = listOf(
                    glowColor.copy(alpha = 0.50f),
                    glowColor.copy(alpha = 0.18f),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            )
            onDrawWithContent {
                drawContent()
                drawCircle(
                    brush = brush,
                    radius = radius,
                    center = center
                )
            }
        }
}
