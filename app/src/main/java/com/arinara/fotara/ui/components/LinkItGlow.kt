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

/**
 * Anchor positions for LinkIt glows on cards.
 * Can be at the middle of a side (TOP, BOTTOM, LEFT, RIGHT)
 * or at a corner (TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT).
 */
enum class GlowAnchor {
    TOP,
    BOTTOM,
    LEFT,
    RIGHT,
    TOP_LEFT,
    TOP_RIGHT,
    BOTTOM_LEFT,
    BOTTOM_RIGHT
}

/**
 * Legacy corner and edge representations for backward compatibility.
 */
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

fun GlowAnchor.toGlowCorner(): GlowCorner = when (this) {
    GlowAnchor.TOP -> GlowCorner.TopEdge
    GlowAnchor.BOTTOM -> GlowCorner.BottomEdge
    GlowAnchor.LEFT -> GlowCorner.LeftEdge
    GlowAnchor.RIGHT -> GlowCorner.RightEdge
    GlowAnchor.TOP_LEFT -> GlowCorner.TopLeft
    GlowAnchor.TOP_RIGHT -> GlowCorner.TopRight
    GlowAnchor.BOTTOM_LEFT -> GlowCorner.BottomLeft
    GlowAnchor.BOTTOM_RIGHT -> GlowCorner.BottomRight
}

fun GlowCorner.toGlowAnchor(): GlowAnchor = when (this) {
    GlowCorner.TopEdge -> GlowAnchor.TOP
    GlowCorner.BottomEdge -> GlowAnchor.BOTTOM
    GlowCorner.LeftEdge -> GlowAnchor.LEFT
    GlowCorner.RightEdge -> GlowAnchor.RIGHT
    GlowCorner.TopLeft -> GlowAnchor.TOP_LEFT
    GlowCorner.TopRight -> GlowAnchor.TOP_RIGHT
    GlowCorner.BottomLeft -> GlowAnchor.BOTTOM_LEFT
    GlowCorner.BottomRight -> GlowAnchor.BOTTOM_RIGHT
}

/**
 * Pure function: computes the set of glow anchors for items in a 2D grid across densities (2, 3, 4).
 * Rules:
 * 1. Exactly two horizontal or vertical neighbors: middle of facing sides (TOP/BOTTOM/LEFT/RIGHT).
 * 2. Cluster rule: for every 2x2 block of grid cells in which 3 or 4 cells contain members of the same group,
 *    every member in that block glows at its CORNER touching the block's center point.
 * 3. Any adjacent member pair not inside a qualifying 2x2 block keeps rule 1.
 *    An adjacent pair whose two members are both inside a qualifying block gets only the corner glow, not side glow.
 *    A card can have several anchors (e.g. groups of 5+).
 */
fun computeGridGlowAnchors(
    items: List<Pair<Long, Long?>>,
    columns: Int = 2
): Map<Long, Set<GlowAnchor>> {
    if (items.isEmpty() || columns <= 0) return emptyMap()

    val resultMap = mutableMapOf<Long, MutableSet<GlowAnchor>>()
    val itemIndexMap = items.mapIndexed { index, pair -> pair.first to index }.toMap()

    // Group items by linkGroupId (excluding null)
    val groupedItems = items.filter { it.second != null }.groupBy { it.second!! }

    for ((_, groupMembers) in groupedItems) {
        if (groupMembers.size < 2) continue

        // Map member coordinates
        val posToId = mutableMapOf<Pair<Int, Int>, Long>()
        val idToPos = mutableMapOf<Long, Pair<Int, Int>>()
        for ((id, _) in groupMembers) {
            val idx = itemIndexMap[id] ?: continue
            val r = idx / columns
            val c = idx % columns
            posToId[r to c] = id
            idToPos[id] = r to c
        }

        // Pairs of item IDs (sorted) that belong to the same qualifying 2x2 block
        val suppressedSidePairs = mutableSetOf<Pair<Long, Long>>()

        // Step 1: Scan all possible 2x2 blocks containing members of this group
        if (columns >= 2 && posToId.isNotEmpty()) {
            val minR = posToId.keys.minOf { it.first }
            val maxR = posToId.keys.maxOf { it.first }

            for (r in minR..maxR) {
                for (c in 0 until (columns - 1)) {
                    val pTL = r to c
                    val pTR = r to (c + 1)
                    val pBL = (r + 1) to c
                    val pBR = (r + 1) to (c + 1)

                    val idTL = posToId[pTL]
                    val idTR = posToId[pTR]
                    val idBL = posToId[pBL]
                    val idBR = posToId[pBR]

                    val present = listOfNotNull(idTL, idTR, idBL, idBR)
                    if (present.size >= 3) {
                        // Qualifying 2x2 block (3 or 4 members)
                        if (idTL != null) {
                            resultMap.getOrPut(idTL) { mutableSetOf() }.add(GlowAnchor.BOTTOM_RIGHT)
                        }
                        if (idTR != null) {
                            resultMap.getOrPut(idTR) { mutableSetOf() }.add(GlowAnchor.BOTTOM_LEFT)
                        }
                        if (idBL != null) {
                            resultMap.getOrPut(idBL) { mutableSetOf() }.add(GlowAnchor.TOP_RIGHT)
                        }
                        if (idBR != null) {
                            resultMap.getOrPut(idBR) { mutableSetOf() }.add(GlowAnchor.TOP_LEFT)
                        }

                        // Suppress side glows between members in this qualifying block
                        fun markSuppressed(id1: Long?, id2: Long?) {
                            if (id1 != null && id2 != null) {
                                val pair = if (id1 < id2) id1 to id2 else id2 to id1
                                suppressedSidePairs.add(pair)
                            }
                        }
                        markSuppressed(idTL, idTR)
                        markSuppressed(idBL, idBR)
                        markSuppressed(idTL, idBL)
                        markSuppressed(idTR, idBR)
                    }
                }
            }
        }

        // Step 2: Handle adjacent pairs (horizontal and vertical)
        val memberList = groupMembers.map { it.first }
        for (i in 0 until memberList.size) {
            val idA = memberList[i]
            val posA = idToPos[idA] ?: continue

            for (j in (i + 1) until memberList.size) {
                val idB = memberList[j]
                val posB = idToPos[idB] ?: continue

                val pairKey = if (idA < idB) idA to idB else idB to idA
                val dr = posB.first - posA.first
                val dc = posB.second - posA.second

                // Horizontal adjacency (same row, adjacent columns)
                if (dr == 0 && kotlin.math.abs(dc) == 1) {
                    if (!suppressedSidePairs.contains(pairKey)) {
                        if (dc == 1) {
                            // A is left, B is right
                            resultMap.getOrPut(idA) { mutableSetOf() }.add(GlowAnchor.RIGHT)
                            resultMap.getOrPut(idB) { mutableSetOf() }.add(GlowAnchor.LEFT)
                        } else {
                            // B is left, A is right
                            resultMap.getOrPut(idA) { mutableSetOf() }.add(GlowAnchor.LEFT)
                            resultMap.getOrPut(idB) { mutableSetOf() }.add(GlowAnchor.RIGHT)
                        }
                    }
                }
                // Vertical adjacency (same column, adjacent rows)
                else if (dc == 0 && kotlin.math.abs(dr) == 1) {
                    if (!suppressedSidePairs.contains(pairKey)) {
                        if (dr == 1) {
                            // A is top, B is bottom
                            resultMap.getOrPut(idA) { mutableSetOf() }.add(GlowAnchor.BOTTOM)
                            resultMap.getOrPut(idB) { mutableSetOf() }.add(GlowAnchor.TOP)
                        } else {
                            // B is top, A is bottom
                            resultMap.getOrPut(idA) { mutableSetOf() }.add(GlowAnchor.TOP)
                            resultMap.getOrPut(idB) { mutableSetOf() }.add(GlowAnchor.BOTTOM)
                        }
                    }
                }
                // Standalone diagonal pair (only if exactly 2 members and not in cluster)
                else if (kotlin.math.abs(dr) == 1 && kotlin.math.abs(dc) == 1) {
                    val anchorsA = resultMap[idA]
                    val anchorsB = resultMap[idB]
                    if (anchorsA.isNullOrEmpty() && anchorsB.isNullOrEmpty() && groupMembers.size == 2) {
                        if (dr == 1 && dc == 1) {
                            resultMap.getOrPut(idA) { mutableSetOf() }.add(GlowAnchor.BOTTOM_RIGHT)
                            resultMap.getOrPut(idB) { mutableSetOf() }.add(GlowAnchor.TOP_LEFT)
                        } else if (dr == 1 && dc == -1) {
                            resultMap.getOrPut(idA) { mutableSetOf() }.add(GlowAnchor.BOTTOM_LEFT)
                            resultMap.getOrPut(idB) { mutableSetOf() }.add(GlowAnchor.TOP_RIGHT)
                        }
                    }
                }
            }
        }
    }

    return resultMap
}

/**
 * Backward-compatible single-corner resolver.
 */
fun computeGridFacingGlowCorners(
    items: List<Pair<Long, Long?>>,
    columns: Int = 2
): Map<Long, GlowCorner> {
    val anchorsMap = computeGridGlowAnchors(items, columns)
    return anchorsMap.mapNotNull { (id, anchors) ->
        val firstAnchor = anchors.firstOrNull() ?: return@mapNotNull null
        id to firstAnchor.toGlowCorner()
    }.toMap()
}

fun computeFolderGlowAnchors(
    folders: List<com.arinara.fotara.data.model.Folder>,
    columns: Int = 2
): Map<Long, Set<GlowAnchor>> = computeGridGlowAnchors(
    folders.map { it.id to it.linkGroupId },
    columns
)

fun computeFolderGlowOrientations(
    folders: List<com.arinara.fotara.data.model.Folder>,
    columns: Int = 2
): Map<Long, GlowCorner> = computeGridFacingGlowCorners(
    folders.map { it.id to it.linkGroupId },
    columns
)

/**
 * Renders LinkIt spatial glows for a set of anchors.
 * Supports multiple anchors per card (e.g. corner cluster + side pair).
 */
fun Modifier.linkItGlow(
    isLinked: Boolean,
    glowColor: Color = Color(0xFFF77F00),
    anchors: Set<GlowAnchor>,
    linkedDescription: String = "Linked item",
    cornerRadiusDp: Float = 24f,
    strokeWidthDp: Float = 2f
): Modifier {
    if (!isLinked || anchors.isEmpty()) return this

    return this
        .semantics {
            contentDescription = linkedDescription
        }
        .drawWithCache {
            val r = minOf(cornerRadiusDp.dp.toPx(), size.minDimension * 0.28f)
            val strokePx = strokeWidthDp.dp.toPx()
            val fadeLength = 55.dp.toPx()
            val radius = r + fadeLength * 0.75f
            val stroke = Stroke(width = strokePx, cap = StrokeCap.Round)
            val strokeColor = glowColor.copy(alpha = 0.85f)

            onDrawWithContent {
                drawContent()

                for (anchor in anchors) {
                    val center = when (anchor) {
                        GlowAnchor.TOP_LEFT -> Offset(0f, 0f)
                        GlowAnchor.TOP_RIGHT -> Offset(size.width, 0f)
                        GlowAnchor.BOTTOM_LEFT -> Offset(0f, size.height)
                        GlowAnchor.BOTTOM_RIGHT -> Offset(size.width, size.height)
                        GlowAnchor.TOP -> Offset(size.width / 2f, 0f)
                        GlowAnchor.BOTTOM -> Offset(size.width / 2f, size.height)
                        GlowAnchor.LEFT -> Offset(0f, size.height / 2f)
                        GlowAnchor.RIGHT -> Offset(size.width, size.height / 2f)
                    }

                    val glowBrush = Brush.radialGradient(
                        colors = listOf(
                            glowColor.copy(alpha = 0.28f),
                            glowColor.copy(alpha = 0.08f),
                            Color.Transparent
                        ),
                        center = center,
                        radius = radius
                    )

                    // 1. Soft radial glow
                    drawCircle(
                        brush = glowBrush,
                        radius = radius,
                        center = center
                    )

                    // 2. Corner arc stroke or side line
                    when (anchor) {
                        GlowAnchor.BOTTOM_LEFT -> {
                            drawArc(
                                color = strokeColor,
                                startAngle = 90f,
                                sweepAngle = 90f,
                                useCenter = false,
                                topLeft = Offset(strokePx / 2f, size.height - 2f * r + strokePx / 2f),
                                size = Size(2f * r - strokePx, 2f * r - strokePx),
                                style = stroke
                            )
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
                        GlowAnchor.TOP_LEFT -> {
                            drawArc(
                                color = strokeColor,
                                startAngle = 180f,
                                sweepAngle = 90f,
                                useCenter = false,
                                topLeft = Offset(strokePx / 2f, strokePx / 2f),
                                size = Size(2f * r - strokePx, 2f * r - strokePx),
                                style = stroke
                            )
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
                        GlowAnchor.TOP_RIGHT -> {
                            drawArc(
                                color = strokeColor,
                                startAngle = 270f,
                                sweepAngle = 90f,
                                useCenter = false,
                                topLeft = Offset(size.width - 2f * r + strokePx / 2f, strokePx / 2f),
                                size = Size(2f * r - strokePx, 2f * r - strokePx),
                                style = stroke
                            )
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
                        GlowAnchor.BOTTOM_RIGHT -> {
                            drawArc(
                                color = strokeColor,
                                startAngle = 0f,
                                sweepAngle = 90f,
                                useCenter = false,
                                topLeft = Offset(size.width - 2f * r + strokePx / 2f, size.height - 2f * r + strokePx / 2f),
                                size = Size(2f * r - strokePx, 2f * r - strokePx),
                                style = stroke
                            )
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
                        GlowAnchor.TOP -> {
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
                        GlowAnchor.BOTTOM -> {
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
                        GlowAnchor.LEFT -> {
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
                        GlowAnchor.RIGHT -> {
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
}

/**
 * Backward-compatible modifier taking a single GlowCorner.
 */
fun Modifier.linkItCornerGlow(
    isLinked: Boolean,
    glowColor: Color = Color(0xFFF77F00),
    corner: GlowCorner = GlowCorner.BottomLeft,
    linkedDescription: String = "Linked item",
    radiusRatio: Float = 0.38f,
    cornerRadiusDp: Float = 24f,
    strokeWidthDp: Float = 2f
): Modifier = linkItGlow(
    isLinked = isLinked,
    glowColor = glowColor,
    anchors = setOf(corner.toGlowAnchor()),
    linkedDescription = linkedDescription,
    cornerRadiusDp = cornerRadiusDp,
    strokeWidthDp = strokeWidthDp
)
