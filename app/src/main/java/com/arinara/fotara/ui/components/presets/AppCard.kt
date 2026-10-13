// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components.presets

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.arinara.fotara.ui.theme.ElectricSlateTokens
import com.arinara.fotara.ui.theme.jigglePressClickable

@Composable
fun AppCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    isGlow: Boolean = false,
    glowColor: Color = ElectricSlateTokens.PrimaryAccent,
    containerColor: Color = ElectricSlateTokens.SurfacePanel,
    borderColor: Color = if (isGlow) ElectricSlateTokens.PrimaryAccent else ElectricSlateTokens.BorderSubtle,
    cornerRadius: Dp = 24.dp,
    contentPadding: Dp = 16.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(cornerRadius)

    val glowModifier = if (isGlow) {
        Modifier.drawBehind {
            val transparentColor = glowColor.copy(alpha = 0f).toArgb()
            val shadowColor = glowColor.copy(alpha = 0.28f).toArgb()
            this.drawIntoCanvas {
                val paint = Paint()
                val frameworkPaint = paint.asFrameworkPaint()
                frameworkPaint.color = transparentColor
                frameworkPaint.setShadowLayer(
                    20.dp.toPx(),
                    0f,
                    0f,
                    shadowColor
                )
                it.drawRoundRect(
                    0f,
                    0f,
                    this.size.width,
                    this.size.height,
                    cornerRadius.toPx(),
                    cornerRadius.toPx(),
                    paint
                )
            }
        }
    } else Modifier

    val clickableModifier = if (onClick != null) {
        Modifier.jigglePressClickable(onClick = onClick)
    } else Modifier

    Box(
        modifier = modifier
            .then(glowModifier)
            .then(clickableModifier)
            .clip(shape)
            .background(containerColor)
            .border(
                border = BorderStroke(
                    width = if (isGlow) 1.5.dp else 1.dp,
                    color = borderColor
                ),
                shape = shape
            )
            .padding(contentPadding)
    ) {
        Column {
            content()
        }
    }
}
