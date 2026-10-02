// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

enum class GlowCorner {
    TopLeft,
    TopRight,
    BottomLeft,
    BottomRight,
    TopEdge,
    BottomEdge,
    LeftEdge,
    RightEdge
}

/**
 * Computes the optimal facing glow orientation for each folder in a 2D grid.
 * If adjacent cards have an active glow (LinkIt spatial group), the glow is oriented
 * to face the nearest neighbor:
 * - Cardinal neighbors (up/down/left/right) -> TopEdge, BottomEdge, LeftEdge, RightEdge
 * - Diagonal neighbors (corners) -> TopLeft, TopRight, BottomLeft, BottomRight
 */
fun computeFolderGlowOrientations(
    folders: List<com.arinara.fotara.data.model.Folder>,
    columns: Int = 2
): Map<Long, GlowCorner> {
    if (folders.isEmpty() || columns <= 0) return emptyMap()

    val resultMap = mutableMapOf<Long, GlowCorner>()

    for ((index, folder) in folders.withIndex()) {
        if (folder.linkGroupId == null) continue

        val row = index / columns
        val col = index % columns

        // Check 8 directions: cardinal first (edges), then diagonal (corners)
        // Priority 1: neighbor in SAME linkGroupId
        // Priority 2: any linked neighbor
        var sameGroupOrientation: GlowCorner? = null
        var anyLinkedOrientation: GlowCorner? = null

        val directions = listOf(
            // Cardinal (edges)
            Triple(-1, 0, GlowCorner.TopEdge),
            Triple(1, 0, GlowCorner.BottomEdge),
            Triple(0, -1, GlowCorner.LeftEdge),
            Triple(0, 1, GlowCorner.RightEdge),
            // Diagonal (corners)
            Triple(-1, -1, GlowCorner.TopLeft),
            Triple(-1, 1, GlowCorner.TopRight),
            Triple(1, -1, GlowCorner.BottomLeft),
            Triple(1, 1, GlowCorner.BottomRight)
        )

        for ((dRow, dCol, orientation) in directions) {
            val nRow = row + dRow
            val nCol = col + dCol

            if (nCol in 0 until columns) {
                val nIndex = nRow * columns + nCol
                if (nIndex in folders.indices) {
                    val neighbor = folders[nIndex]
                    if (neighbor.linkGroupId != null) {
                        if (neighbor.linkGroupId == folder.linkGroupId && sameGroupOrientation == null) {
                            sameGroupOrientation = orientation
                        } else if (anyLinkedOrientation == null) {
                            anyLinkedOrientation = orientation
                        }
                    }
                }
            }
        }

        resultMap[folder.id] = sameGroupOrientation ?: anyLinkedOrientation ?: GlowCorner.BottomLeft
    }

    return resultMap
}

/**
 * Renders an elegant corner glow indicating LinkIt spatial grouping.
 * Combines a soft radial glow bleeding into the card corner with a thin accent stroke
 * following the card's rounded corner arc and fading out along the edges.
 *
 * 100% compatible from API 24 to 36 without relying on blur RenderEffect shaders.
 * Cached in drawWithCache for smooth 60/120fps scrolling at any grid density.
 */
fun Modifier.linkItCornerGlow(
    isLinked: Boolean,
    glowColor: Color = Color(0xFFF77F00),
    corner: GlowCorner = GlowCorner.BottomLeft,
    linkedDescription: String = "Linked item",
    radiusRatio: Float = 0.38f,
    cornerRadiusDp: Float = 14f,
    strokeWidthDp: Float = 1.8f
): Modifier {
    if (!isLinked) return this

    return this
        .semantics {
            contentDescription = linkedDescription
        }
        .drawWithCache {
            val radius = size.minDimension * radiusRatio
            val center = when (corner) {
                GlowCorner.TopLeft -> Offset(0f, 0f)
                GlowCorner.TopRight -> Offset(size.width, 0f)
                GlowCorner.BottomLeft -> Offset(0f, size.height)
                GlowCorner.BottomRight -> Offset(size.width, size.height)
                GlowCorner.TopEdge -> Offset(size.width / 2f, 0f)
                GlowCorner.BottomEdge -> Offset(size.width / 2f, size.height)
                GlowCorner.LeftEdge -> Offset(0f, size.height / 2f)
                GlowCorner.RightEdge -> Offset(size.width, size.height / 2f)
            }
            val glowBrush = Brush.radialGradient(
                colors = listOf(
                    glowColor.copy(alpha = 0.38f),
                    glowColor.copy(alpha = 0.14f),
                    Color.Transparent
                ),
                center = center,
                radius = radius
            )

            val r = minOf(cornerRadiusDp.dp.toPx(), size.minDimension * 0.28f)
            val strokePx = strokeWidthDp.dp.toPx()
            val fadeLength = r * 1.25f

            onDrawWithContent {
                drawContent()

                // 1. Soft radial glow bleeding into corner
                drawCircle(
                    brush = glowBrush,
                    radius = radius,
                    center = center
                )

                // 2. Corner arc stroke & fading edge strokes
                val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)
                val strokeColor = glowColor.copy(alpha = 0.85f)

                when (corner) {
                    GlowCorner.BottomLeft -> {
                        // Arc from 90° (bottom) to 180° (left)
                        drawArc(
                            color = strokeColor,
                            startAngle = 90f,
                            sweepAngle = 90f,
                            useCenter = false,
                            topLeft = Offset(strokePx / 2f, size.height - 2f * r + strokePx / 2f),
                            size = Size(2f * r - strokePx, 2f * r - strokePx),
                            style = stroke
                        )
                        // Fade along bottom edge
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(strokeColor, Color.Transparent),
                                startX = r,
                                endX = r + fadeLength
                            ),
                            start = Offset(r, size.height - strokePx / 2f),
                            end = Offset(r + fadeLength, size.height - strokePx / 2f),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                        // Fade along left edge
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, strokeColor),
                                startY = size.height - r - fadeLength,
                                endY = size.height - r
                            ),
                            start = Offset(strokePx / 2f, size.height - r - fadeLength),
                            end = Offset(strokePx / 2f, size.height - r),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                    }
                    GlowCorner.TopLeft -> {
                        // Arc from 180° (left) to 270° (top)
                        drawArc(
                            color = strokeColor,
                            startAngle = 180f,
                            sweepAngle = 90f,
                            useCenter = false,
                            topLeft = Offset(strokePx / 2f, strokePx / 2f),
                            size = Size(2f * r - strokePx, 2f * r - strokePx),
                            style = stroke
                        )
                        // Fade along top edge
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(strokeColor, Color.Transparent),
                                startX = r,
                                endX = r + fadeLength
                            ),
                            start = Offset(r, strokePx / 2f),
                            end = Offset(r + fadeLength, strokePx / 2f),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                        // Fade along left edge
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(strokeColor, Color.Transparent),
                                startY = r,
                                endY = r + fadeLength
                            ),
                            start = Offset(strokePx / 2f, r),
                            end = Offset(strokePx / 2f, r + fadeLength),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                    }
                    GlowCorner.TopRight -> {
                        // Arc from 270° (top) to 360° (right)
                        drawArc(
                            color = strokeColor,
                            startAngle = 270f,
                            sweepAngle = 90f,
                            useCenter = false,
                            topLeft = Offset(size.width - 2f * r + strokePx / 2f, strokePx / 2f),
                            size = Size(2f * r - strokePx, 2f * r - strokePx),
                            style = stroke
                        )
                        // Fade along top edge
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, strokeColor),
                                startX = size.width - r - fadeLength,
                                endX = size.width - r
                            ),
                            start = Offset(size.width - r - fadeLength, strokePx / 2f),
                            end = Offset(size.width - r, strokePx / 2f),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                        // Fade along right edge
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(strokeColor, Color.Transparent),
                                startY = r,
                                endY = r + fadeLength
                            ),
                            start = Offset(size.width - strokePx / 2f, r),
                            end = Offset(size.width - strokePx / 2f, r + fadeLength),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                    }
                    GlowCorner.BottomRight -> {
                        // Arc from 0° (right) to 90° (bottom)
                        drawArc(
                            color = strokeColor,
                            startAngle = 0f,
                            sweepAngle = 90f,
                            useCenter = false,
                            topLeft = Offset(size.width - 2f * r + strokePx / 2f, size.height - 2f * r + strokePx / 2f),
                            size = Size(2f * r - strokePx, 2f * r - strokePx),
                            style = stroke
                        )
                        // Fade along bottom edge
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, strokeColor),
                                startX = size.width - r - fadeLength,
                                endX = size.width - r
                            ),
                            start = Offset(size.width - r - fadeLength, size.height - strokePx / 2f),
                            end = Offset(size.width - r, size.height - strokePx / 2f),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                        // Fade along right edge
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, strokeColor),
                                startY = size.height - r - fadeLength,
                                endY = size.height - r
                            ),
                            start = Offset(size.width - strokePx / 2f, size.height - r - fadeLength),
                            end = Offset(size.width - strokePx / 2f, size.height - r),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                    }
                    GlowCorner.TopEdge -> {
                        val edgeStartX = size.width * 0.15f
                        val edgeEndX = size.width * 0.85f
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, strokeColor, Color.Transparent),
                                startX = edgeStartX,
                                endX = edgeEndX
                            ),
                            start = Offset(edgeStartX, strokePx / 2f),
                            end = Offset(edgeEndX, strokePx / 2f),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                    }
                    GlowCorner.BottomEdge -> {
                        val edgeStartX = size.width * 0.15f
                        val edgeEndX = size.width * 0.85f
                        drawLine(
                            brush = Brush.horizontalGradient(
                                colors = listOf(Color.Transparent, strokeColor, Color.Transparent),
                                startX = edgeStartX,
                                endX = edgeEndX
                            ),
                            start = Offset(edgeStartX, size.height - strokePx / 2f),
                            end = Offset(edgeEndX, size.height - strokePx / 2f),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                    }
                    GlowCorner.LeftEdge -> {
                        val edgeStartY = size.height * 0.15f
                        val edgeEndY = size.height * 0.85f
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, strokeColor, Color.Transparent),
                                startY = edgeStartY,
                                endY = edgeEndY
                            ),
                            start = Offset(strokePx / 2f, edgeStartY),
                            end = Offset(strokePx / 2f, edgeEndY),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                    }
                    GlowCorner.RightEdge -> {
                        val edgeStartY = size.height * 0.15f
                        val edgeEndY = size.height * 0.85f
                        drawLine(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color.Transparent, strokeColor, Color.Transparent),
                                startY = edgeStartY,
                                endY = edgeEndY
                            ),
                            start = Offset(size.width - strokePx / 2f, edgeStartY),
                            end = Offset(size.width - strokePx / 2f, edgeEndY),
                            strokeWidth = strokePx,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
        }
}
