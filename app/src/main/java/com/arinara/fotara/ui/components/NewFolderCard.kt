// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.NewFolderBody
import com.arinara.fotara.theme.NewFolderBorder
import com.arinara.fotara.theme.NewFolderText

@Composable
fun NewFolderCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cornerRadiusDp = 24.dp

    Box(
        modifier = modifier
            .aspectRatio(0.82f)
            .clip(RoundedCornerShape(cornerRadiusDp))
            .clickable(onClick = onClick)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val r = cornerRadiusDp.toPx()

            // 1. Draw top cream tab base
            drawRoundRect(
                color = FolderTabCream,
                size = size,
                cornerRadius = CornerRadius(r, r)
            )

            // 2. Draw dark navy lower body with tab shoulder curve
            val topH = h * 0.18f
            val shoulderH = h * 0.28f
            val curveStart = w * 0.72f

            val bodyPath = Path().apply {
                moveTo(0f, topH)
                lineTo(curveStart, topH)
                cubicTo(
                    x1 = curveStart + (w - curveStart) * 0.4f,
                    y1 = topH,
                    x2 = curveStart + (w - curveStart) * 0.4f,
                    y2 = shoulderH,
                    x3 = w,
                    y3 = shoulderH
                )
                lineTo(w, h - r)
                arcTo(
                    rect = Rect(w - 2 * r, h - 2 * r, w, h),
                    startAngleDegrees = 0f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                lineTo(r, h)
                arcTo(
                    rect = Rect(0f, h - 2 * r, 2 * r, h),
                    startAngleDegrees = 90f,
                    sweepAngleDegrees = 90f,
                    forceMoveTo = false
                )
                close()
            }
            drawPath(path = bodyPath, color = NewFolderBody)

            // Subtle border outline on the body
            drawPath(
                path = bodyPath,
                color = NewFolderBorder,
                style = Stroke(width = 1.5.dp.toPx())
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "+New",
                color = NewFolderText,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                fontStyle = FontStyle.Italic,
                letterSpacing = (-0.3).sp
            )

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
