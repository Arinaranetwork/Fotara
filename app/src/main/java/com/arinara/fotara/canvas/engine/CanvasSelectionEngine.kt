// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import com.arinara.fotara.canvas.model.CanvasDocument
import com.arinara.fotara.canvas.model.CanvasElement
import com.arinara.fotara.canvas.model.CanvasSelection
import com.arinara.fotara.canvas.model.ImageElement
import com.arinara.fotara.canvas.model.SelectedElementReference
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokeInsideSegment
import com.arinara.fotara.canvas.model.StrokePoint
import java.util.UUID
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

/**
 * Pure functions executing non-destructive lasso selection, winding number geometry,
 * partial stroke slicing, remnant absorption, and split materialization.
 */
object CanvasSelectionEngine {

    /**
     * Determines whether a point is inside an arbitrary polygon using the non-zero winding number rule.
     * Guarantees that self-crossing loops (figure-8, overlapping loops) select everything enclosed.
     */
    fun isPointInPolygonWinding(px: Float, py: Float, polygon: List<Pair<Float, Float>>): Boolean {
        if (polygon.size < 3 || px.isNaN() || py.isNaN()) return false
        var wn = 0
        val n = polygon.size

        for (i in 0 until n) {
            val p1 = polygon[i]
            val p2 = polygon[(i + 1) % n]

            if (p1.second <= py) {
                if (p2.second > py) {
                    val isLeft = (p2.first - p1.first) * (py - p1.second) - (px - p1.first) * (p2.second - p1.second)
                    if (isLeft > 0f) wn++
                }
            } else {
                if (p2.second <= py) {
                    val isLeft = (p2.first - p1.first) * (py - p1.second) - (px - p1.first) * (p2.second - p1.second)
                    if (isLeft < 0f) wn--
                }
            }
        }
        return wn != 0
    }

    /**
     * Computes the line segment intersection parameter t in [0, 1] between S1->S2 and E1->E2.
     * Returns null if lines are parallel or do not intersect within their bounds.
     */
    fun segmentIntersection(
        x1: Float, y1: Float, x2: Float, y2: Float,
        x3: Float, y3: Float, x4: Float, y4: Float
    ): Float? {
        val dx12 = x1 - x2
        val dy12 = y1 - y2
        val dx34 = x3 - x4
        val dy34 = y3 - y4

        val denom = dx12 * dy34 - dy12 * dx34
        if (kotlin.math.abs(denom) < 1e-6f) return null

        val t = ((x1 - x3) * dy34 - (y1 - y3) * dx34) / denom
        val u = -((x1 - x2) * (y1 - y3) - (y1 - y2) * (x1 - x3)) / denom

        return if (t in 0.0f..1.0f && u in 0.0f..1.0f) t else null
    }

    /**
     * Non-destructively evaluates a stroke against a lasso polygon.
     * Returns null if completely outside.
     * Returns SelectedElementReference.Whole if completely inside.
     * Returns SelectedElementReference.PartialStroke with inside segments if crossed.
     */
    fun sliceStrokeWithLasso(
        stroke: StrokeElement,
        lassoPolygon: List<Pair<Float, Float>>,
        viewportScale: Float = 1.0f,
        density: Float = 1.0f
    ): SelectedElementReference? {
        val pts = stroke.points
        if (pts.isEmpty() || lassoPolygon.size < 3) return null

        // Single-point dot case
        if (pts.size == 1) {
            val p = pts[0]
            return if (isPointInPolygonWinding(p.x, p.y, lassoPolygon)) {
                SelectedElementReference.Whole(stroke.id)
            } else {
                null
            }
        }

        // Subdivide stroke into inside and outside runs
        val polyEdges = mutableListOf<Pair<Pair<Float, Float>, Pair<Float, Float>>>()
        for (i in lassoPolygon.indices) {
            polyEdges.add(Pair(lassoPolygon[i], lassoPolygon[(i + 1) % lassoPolygon.size]))
        }

        // Build list of contiguous sub-pieces with their inside/outside status
        data class SubPiece(val isInside: Boolean, val points: List<StrokePoint>)
        val subPieces = mutableListOf<SubPiece>()

        var currentIsInside: Boolean? = null
        var currentPoints = mutableListOf<StrokePoint>()

        fun flushRun(isInside: Boolean) {
            if (currentPoints.isNotEmpty()) {
                subPieces.add(SubPiece(isInside, currentPoints.toList()))
                currentPoints = mutableListOf()
            }
        }

        for (i in 0 until (pts.size - 1)) {
            val pA = pts[i]
            val pB = pts[i + 1]

            // Find all intersections of segment pA -> pB with polygon edges
            val intersections = mutableListOf<Float>()
            for (edge in polyEdges) {
                val t = segmentIntersection(
                    pA.x, pA.y, pB.x, pB.y,
                    edge.first.first, edge.first.second, edge.second.first, edge.second.second
                )
                if (t != null && t > 1e-4f && t < 0.9999f) {
                    intersections.add(t)
                }
            }
            intersections.sort()

            // Deduplicate close intersections
            val sortedT = mutableListOf(0.0f)
            for (t in intersections) {
                if (t - sortedT.last() > 1e-4f) {
                    sortedT.add(t)
                }
            }
            if (1.0f - sortedT.last() > 1e-4f) {
                sortedT.add(1.0f)
            }

            for (k in 0 until (sortedT.size - 1)) {
                val tStart = sortedT[k]
                val tEnd = sortedT[k + 1]
                val tMid = (tStart + tEnd) / 2.0f

                val midX = pA.x + tMid * (pB.x - pA.x)
                val midY = pA.y + tMid * (pB.y - pA.y)
                val intervalInside = isPointInPolygonWinding(midX, midY, lassoPolygon)

                val ptStart = if (tStart == 0.0f) pA else StrokePoint(
                    pA.x + tStart * (pB.x - pA.x),
                    pA.y + tStart * (pB.y - pA.y),
                    pA.pressure + tStart * (pB.pressure - pA.pressure)
                )
                val ptEnd = if (tEnd == 1.0f) pB else StrokePoint(
                    pA.x + tEnd * (pB.x - pA.x),
                    pA.y + tEnd * (pB.y - pA.y),
                    pA.pressure + tEnd * (pB.pressure - pA.pressure)
                )

                if (currentIsInside != intervalInside) {
                    if (currentIsInside != null) {
                        flushRun(currentIsInside)
                    }
                    currentIsInside = intervalInside
                    currentPoints.add(ptStart)
                } else if (currentPoints.isEmpty()) {
                    currentPoints.add(ptStart)
                }
                currentPoints.add(ptEnd)
            }
        }

        if (currentIsInside != null && currentPoints.isNotEmpty()) {
            flushRun(currentIsInside)
        }

        if (subPieces.isEmpty()) return null

        // Check if fully inside
        if (subPieces.all { it.isInside }) {
            return SelectedElementReference.Whole(stroke.id)
        }
        // Check if fully outside
        if (subPieces.none { it.isInside }) {
            return null
        }

        // Apply short remainder rule:
        // An outside leftover shorter than 4dp on screen adjacent to a selected portion is absorbed.
        val minRemainderScreen = 4.0f * density
        val minRemainderWorld = (minRemainderScreen / viewportScale.coerceAtLeast(0.01f))

        fun segmentLength(ptsList: List<StrokePoint>): Float {
            var len = 0f
            for (idx in 0 until (ptsList.size - 1)) {
                len += hypot(ptsList[idx + 1].x - ptsList[idx].x, ptsList[idx + 1].y - ptsList[idx].y)
            }
            return len
        }

        val processedPieces = subPieces.map { it.copy() }.toMutableList()

        // Pass: absorb short outside pieces into adjacent inside pieces
        var changed = true
        while (changed) {
            changed = false
            for (idx in processedPieces.indices) {
                val piece = processedPieces[idx]
                if (!piece.isInside) {
                    val len = segmentLength(piece.points)
                    val prevIsInside = if (idx > 0) processedPieces[idx - 1].isInside else false
                    val nextIsInside = if (idx < processedPieces.size - 1) processedPieces[idx + 1].isInside else false

                    if (len <= minRemainderWorld && (prevIsInside || nextIsInside)) {
                        // Mark as inside to absorb
                        processedPieces[idx] = piece.copy(isInside = true)
                        changed = true
                        break
                    }
                }
            }
        }

        // Merge adjacent inside pieces
        val consolidatedInside = mutableListOf<List<StrokePoint>>()
        var curRun: MutableList<StrokePoint>? = null

        for (piece in processedPieces) {
            if (piece.isInside) {
                if (curRun == null) {
                    curRun = piece.points.toMutableList()
                } else {
                    // Avoid duplicating juncture point
                    val first = piece.points.firstOrNull()
                    val last = curRun.lastOrNull()
                    if (first != null && last != null && hypot(first.x - last.x, first.y - last.y) < 1e-3f) {
                        curRun.addAll(piece.points.drop(1))
                    } else {
                        curRun.addAll(piece.points)
                    }
                }
            } else {
                if (curRun != null) {
                    consolidatedInside.add(curRun)
                    curRun = null
                }
            }
        }
        if (curRun != null) {
            consolidatedInside.add(curRun)
        }

        if (consolidatedInside.isEmpty()) return null

        // If after absorption all pieces merged into the full stroke
        if (processedPieces.all { it.isInside }) {
            return SelectedElementReference.Whole(stroke.id)
        }

        val insideSegments = consolidatedInside.map { StrokeInsideSegment(it) }
        return SelectedElementReference.PartialStroke(stroke.id, insideSegments)
    }

    /**
     * Evaluates whether an image element is selected whole by a lasso polygon (center inside).
     */
    fun evaluateImageSelection(
        image: ImageElement,
        lassoPolygon: List<Pair<Float, Float>>
    ): SelectedElementReference? {
        return if (isPointInPolygonWinding(image.bounds.centerX, image.bounds.centerY, lassoPolygon)) {
            SelectedElementReference.Whole(image.id)
        } else {
            null
        }
    }

    /**
     * Computes the bounding box of a selection from selected portions only (plus half stroke width).
     */
    fun computeSelectionBounds(
        references: Map<String, SelectedElementReference>,
        elements: List<CanvasElement>
    ): CanvasRect {
        if (references.isEmpty()) return CanvasRect.Empty
        val elMap = elements.associateBy { it.id }

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for ((id, ref) in references) {
            val el = elMap[id] ?: continue
            val r: CanvasRect = when (ref) {
                is SelectedElementReference.Whole -> el.bounds
                is SelectedElementReference.PartialStroke -> {
                    if (el is StrokeElement) {
                        ref.insideSegments.map { it.computeBounds(el.width) }
                            .fold(CanvasRect.Empty) { acc, cur -> acc.union(cur) }
                    } else {
                        el.bounds
                    }
                }
            }
            if (r.isEmpty) continue
            if (r.left < minX) minX = r.left
            if (r.top < minY) minY = r.top
            if (r.right > maxX) maxX = r.right
            if (r.bottom > maxY) maxY = r.bottom
        }

        return if (minX > maxX || minY > maxY) CanvasRect.Empty else CanvasRect(minX, minY, maxX, maxY)
    }

    /**
     * Materializes the split of a partially selected stroke.
     * Returns:
     * - outsideStrokes: list of surviving unselected StrokeElements
     * - insideStrokes: list of selected StrokeElements
     */
    fun materializePartialSplit(
        original: StrokeElement,
        ref: SelectedElementReference.PartialStroke,
        viewportScale: Float = 1.0f,
        density: Float = 1.0f
    ): Pair<List<StrokeElement>, List<StrokeElement>> {
        val minRemnant = max(original.width, (4.0f * density) / viewportScale.coerceAtLeast(0.01f))

        fun strokeLength(pts: List<StrokePoint>): Float {
            var sum = 0f
            for (i in 0 until (pts.size - 1)) {
                sum += hypot(pts[i + 1].x - pts[i].x, pts[i + 1].y - pts[i].y)
            }
            return sum
        }

        // Inside segments become new StrokeElements
        val insideStrokes = mutableListOf<StrokeElement>()
        for (seg in ref.insideSegments) {
            if (seg.points.isEmpty()) continue
            if (seg.points.size > 1 && strokeLength(seg.points) < 0.5f) continue
            val b = StrokeProcessor.computeBounds(seg.points, original.width)
            insideStrokes.add(
                original.copy(
                    id = UUID.randomUUID().toString(),
                    points = seg.points,
                    bounds = b
                )
            )
        }

        // Outside segments: compute by subtracting inside segments from original points
        val outsideStrokes = mutableListOf<StrokeElement>()
        val outsideSegments = computeOutsideSegments(original.points, ref.insideSegments)

        for (pts in outsideSegments) {
            if (pts.isEmpty()) continue
            val len = strokeLength(pts)
            // Remnant rule: drop pieces shorter than max(stroke.width, 4dp) unless it's a single dot
            if (pts.size > 1 && len < minRemnant) {
                continue
            }
            val b = StrokeProcessor.computeBounds(pts, original.width)
            outsideStrokes.add(
                original.copy(
                    id = UUID.randomUUID().toString(),
                    points = pts,
                    bounds = b
                )
            )
        }

        return Pair(outsideStrokes, insideStrokes)
    }

    private fun computeOutsideSegments(
        originalPoints: List<StrokePoint>,
        insideSegments: List<StrokeInsideSegment>
    ): List<List<StrokePoint>> {
        if (insideSegments.isEmpty()) return listOf(originalPoints)

        // Find matches for each inside segment along original points
        val result = mutableListOf<List<StrokePoint>>()
        var remainingPoints = originalPoints

        for (inside in insideSegments) {
            val inPts = inside.points
            if (inPts.isEmpty()) continue
            val inStart = inPts.first()
            val inEnd = inPts.last()

            // Find closest index for start and end
            val startIdx = findClosestPointIndex(remainingPoints, inStart.x, inStart.y)
            val endIdx = findClosestPointIndex(remainingPoints, inEnd.x, inEnd.y)

            val minIdx = min(startIdx, endIdx)
            val maxIdx = max(startIdx, endIdx)

            val before = remainingPoints.take(minIdx + 1).toMutableList()
            val lastPt = before.lastOrNull()
            if (lastPt != null && hypot(lastPt.x - inStart.x, lastPt.y - inStart.y) > 1e-3f) {
                before.add(inStart)
            }
            if (before.size >= 2) {
                result.add(before)
            }

            val after = remainingPoints.drop(maxIdx).toMutableList()
            val firstPt = after.firstOrNull()
            if (firstPt != null && hypot(firstPt.x - inEnd.x, firstPt.y - inEnd.y) > 1e-3f) {
                after.add(0, inEnd)
            }
            remainingPoints = after
        }

        if (remainingPoints.size >= 2) {
            result.add(remainingPoints)
        }

        return result
    }

    private fun findClosestPointIndex(pts: List<StrokePoint>, x: Float, y: Float): Int {
        var bestIdx = 0
        var bestDist = Float.MAX_VALUE
        for (i in pts.indices) {
            val d = hypot(pts[i].x - x, pts[i].y - y)
            if (d < bestDist) {
                bestDist = d
                bestIdx = i
            }
        }
        return bestIdx
    }
}
