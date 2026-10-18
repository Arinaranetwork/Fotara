// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType
import java.util.UUID
import kotlin.math.hypot
import kotlin.math.sqrt

/**
 * Pure functions for stroke smoothing, decimation, bounds computation,
 * hit testing, lasso selection, and area erasing (stroke splitting).
 */
object StrokeProcessor {

    /**
     * Normalizes raw stylus or touch pressure to a stable [0.05f .. 1.0f] range.
     * When device hardware reports 0.0f (non-pressure-sensitive stylus or mouse),
     * defaults safely to 1.0f.
     */
    fun normalizePressure(rawPressure: Float): Float {
        if (rawPressure.isNaN() || rawPressure.isInfinite() || rawPressure <= 0.0f) {
            return 1.0f
        }
        return rawPressure.coerceIn(0.05f, 1.0f)
    }

    /**
     * Computes tight axis-aligned bounding box for a series of stroke points,
     * including stroke width expansion and safety margin.
     * Handles single tap (dot) and zero-length strokes safely.
     */
    fun computeBounds(points: List<StrokePoint>, width: Float): CanvasRect {
        val safeWidth = if (width.isNaN() || width <= 0f) 2.0f else width
        val halfW = (safeWidth / 2.0f) + 1.0f

        if (points.isEmpty()) {
            return CanvasRect.Empty
        }

        if (points.size == 1) {
            val p = points[0]
            return CanvasRect(
                left = p.x - halfW,
                top = p.y - halfW,
                right = p.x + halfW,
                bottom = p.y + halfW
            )
        }

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (p in points) {
            if (p.x.isNaN() || p.y.isNaN()) continue
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }

        if (minX > maxX || minY > maxY) {
            return CanvasRect.Empty
        }

        return CanvasRect(
            left = minX - halfW,
            top = minY - halfW,
            right = maxX + halfW,
            bottom = maxY + halfW
        )
    }

    /**
     * Ramer-Douglas-Peucker (RDP) algorithm for point decimation.
     * Reduces redundant collinear points while preserving significant curvature.
     */
    fun decimatePoints(points: List<StrokePoint>, tolerance: Float = 1.0f): List<StrokePoint> {
        if (points.size <= 2 || tolerance <= 0f) {
            return points
        }
        return rdpRecursive(points, 0, points.size - 1, tolerance)
    }

    private fun rdpRecursive(
        points: List<StrokePoint>,
        startIndex: Int,
        endIndex: Int,
        tolerance: Float
    ): List<StrokePoint> {
        var maxDist = 0.0f
        var indexWithMaxDist = startIndex

        val start = points[startIndex]
        val end = points[endIndex]

        for (i in (startIndex + 1) until endIndex) {
            val dist = distanceToSegment(points[i].x, points[i].y, start.x, start.y, end.x, end.y)
            if (dist > maxDist) {
                maxDist = dist
                indexWithMaxDist = i
            }
        }

        return if (maxDist > tolerance) {
            val left = rdpRecursive(points, startIndex, indexWithMaxDist, tolerance)
            val right = rdpRecursive(points, indexWithMaxDist, endIndex, tolerance)
            left.dropLast(1) + right
        } else {
            listOf(start, end)
        }
    }


    /**
     * Computes the perpendicular distance from point (px, py) to line segment (x1, y1)-(x2, y2).
     */
    fun distanceToSegment(
        px: Float, py: Float,
        x1: Float, y1: Float,
        x2: Float, y2: Float
    ): Float {
        val dx = x2 - x1
        val dy = y2 - y1
        val lengthSq = dx * dx + dy * dy

        if (lengthSq == 0.0f) {
            // Segment is a single point
            val sx = px - x1
            val sy = py - y1
            return sqrt(sx * sx + sy * sy)
        }

        // Projection factor t clamped to [0, 1]
        val t = (((px - x1) * dx + (py - y1) * dy) / lengthSq).coerceIn(0.0f, 1.0f)
        val projX = x1 + t * dx
        val projY = y1 + t * dy

        val rx = px - projX
        val ry = py - projY
        return sqrt(rx * rx + ry * ry)
    }

    /**
     * Hit tests whether a point (e.g. tap or eraser centroid) intersects a stroke.
     * Takes stroke width into consideration.
     */
    fun hitTestStroke(
        pointX: Float,
        pointY: Float,
        stroke: StrokeElement,
        hitRadius: Float = 0.0f
    ): Boolean {
        val threshold = (stroke.width / 2.0f) + hitRadius
        val boundsWithThreshold = stroke.bounds.expanded(threshold)

        // Fast bounding-box rejection
        if (!boundsWithThreshold.contains(pointX, pointY)) {
            return false
        }

        val pts = stroke.points
        if (pts.isEmpty()) return false

        if (pts.size == 1) {
            val p = pts[0]
            val dist = hypot(pointX - p.x, pointY - p.y)
            return dist <= threshold
        }

        for (i in 0 until (pts.size - 1)) {
            val p1 = pts[i]
            val p2 = pts[i + 1]
            val dist = distanceToSegment(pointX, pointY, p1.x, p1.y, p2.x, p2.y)
            if (dist <= threshold) {
                return true
            }
        }

        return false
    }

    /**
     * Determines whether a point is inside an arbitrary polygon via ray casting.
     */
    fun isPointInPolygon(px: Float, py: Float, polygon: List<Pair<Float, Float>>): Boolean {
        if (polygon.size < 3) return false
        var inside = false
        var j = polygon.size - 1

        for (i in polygon.indices) {
            val xi = polygon[i].first
            val yi = polygon[i].second
            val xj = polygon[j].first
            val yj = polygon[j].second

            val intersect = ((yi > py) != (yj > py)) &&
                    (px < (xj - xi) * (py - yi) / (yj - yi) + xi)
            if (intersect) {
                inside = !inside
            }
            j = i
        }

        return inside
    }

    /**
     * Lasso polygon selection test.
     * Returns true if the stroke has significant overlap with the lasso polygon.
     */
    fun isStrokeInsideLasso(
        stroke: StrokeElement,
        lassoPolygon: List<Pair<Float, Float>>
    ): Boolean {
        if (lassoPolygon.size < 3 || stroke.points.isEmpty()) return false

        // Quick check: if centroid or majority of points are inside
        var insideCount = 0
        for (p in stroke.points) {
            if (isPointInPolygon(p.x, p.y, lassoPolygon)) {
                insideCount++
            }
        }

        return insideCount > (stroke.points.size / 3) || insideCount >= 2
    }

    /**
     * Area erasing: splits a stroke into surviving contiguous sub-strokes
     * when the eraser circle passes through it.
     *
     * If the eraser cuts the stroke in half, returns two StrokeElements.
     * If the stroke is untouched, returns the original stroke.
     * If completely erased, returns an empty list.
     */
    fun areaEraseStroke(
        stroke: StrokeElement,
        eraserX: Float,
        eraserY: Float,
        eraserRadius: Float
    ): List<StrokeElement> {
        return eraseStrokeWithCapsule(stroke, eraserX, eraserY, eraserX, eraserY, eraserRadius, stroke.width)
    }

    /**
     * Erases content along the capsule swept between consecutive points (ax, ay) and (bx, by).
     * Cuts strokes at exact boundary points, eliminates gaps during fast drags,
     * and drops surviving fragments shorter than max(stroke.width, minRemainder).
     */
    fun eraseStrokeWithCapsule(
        stroke: StrokeElement,
        ax: Float,
        ay: Float,
        bx: Float,
        by: Float,
        eraserRadius: Float,
        minRemainder: Float = 0.0f
    ): List<StrokeElement> {
        val totalEraseRadius = eraserRadius + (stroke.width / 2.0f)
        val expandedBounds = stroke.bounds.expanded(totalEraseRadius)

        // Fast bounding-box check against capsule bounding box
        val capMinX = minOf(ax, bx) - totalEraseRadius
        val capMaxX = maxOf(ax, bx) + totalEraseRadius
        val capMinY = minOf(ay, by) - totalEraseRadius
        val capMaxY = maxOf(ay, by) + totalEraseRadius

        if (expandedBounds.right < capMinX || expandedBounds.left > capMaxX ||
            expandedBounds.bottom < capMinY || expandedBounds.top > capMaxY) {
            return listOf(stroke)
        }

        val pts = stroke.points
        if (pts.isEmpty()) return emptyList()

        if (pts.size == 1) {
            val d = distanceToSegment(pts[0].x, pts[0].y, ax, ay, bx, by)
            return if (d <= totalEraseRadius) emptyList() else listOf(stroke)
        }

        val survivingSegments = mutableListOf<MutableList<StrokePoint>>()
        var currentSegment = mutableListOf<StrokePoint>()

        fun distanceToCap(x: Float, y: Float): Float {
            return distanceToSegment(x, y, ax, ay, bx, by)
        }

        fun findBoundaryPoint(p1: StrokePoint, p2: StrokePoint, p1Erased: Boolean): StrokePoint {
            var low = 0.0f
            var high = 1.0f
            for (step in 0 until 12) {
                val mid = (low + high) / 2.0f
                val mx = p1.x + mid * (p2.x - p1.x)
                val my = p1.y + mid * (p2.y - p1.y)
                val d = distanceToCap(mx, my)
                val midErased = d <= totalEraseRadius
                if (p1Erased) {
                    if (midErased) low = mid else high = mid
                } else {
                    if (midErased) high = mid else low = mid
                }
            }
            val t = (low + high) / 2.0f
            return StrokePoint(
                p1.x + t * (p2.x - p1.x),
                p1.y + t * (p2.y - p1.y),
                p1.pressure + t * (p2.pressure - p1.pressure)
            )
        }

        var prevPt = pts[0]
        var prevErased = distanceToCap(prevPt.x, prevPt.y) <= totalEraseRadius

        if (!prevErased) {
            currentSegment.add(prevPt)
        }

        for (i in 1 until pts.size) {
            val curPt = pts[i]
            val curErased = distanceToCap(curPt.x, curPt.y) <= totalEraseRadius

            if (!prevErased && curErased) {
                // Leaving surviving zone, entering erased zone: find boundary point
                val boundary = findBoundaryPoint(prevPt, curPt, p1Erased = false)
                currentSegment.add(boundary)
                if (currentSegment.isNotEmpty()) {
                    survivingSegments.add(currentSegment)
                    currentSegment = mutableListOf()
                }
            } else if (prevErased && !curErased) {
                // Leaving erased zone, entering surviving zone: find boundary point
                val boundary = findBoundaryPoint(prevPt, curPt, p1Erased = true)
                currentSegment.add(boundary)
                currentSegment.add(curPt)
            } else if (!curErased) {
                // Both outside: check if segment dips through capsule
                val midX = (prevPt.x + curPt.x) / 2.0f
                val midY = (prevPt.y + curPt.y) / 2.0f
                if (distanceToCap(midX, midY) <= totalEraseRadius) {
                    val midPt = StrokePoint(midX, midY, (prevPt.pressure + curPt.pressure) / 2.0f)
                    val b1 = findBoundaryPoint(prevPt, midPt, p1Erased = false)
                    val b2 = findBoundaryPoint(midPt, curPt, p1Erased = true)
                    currentSegment.add(b1)
                    survivingSegments.add(currentSegment)
                    currentSegment = mutableListOf(b2, curPt)
                } else {
                    currentSegment.add(curPt)
                }
            }

            prevPt = curPt
            prevErased = curErased
        }

        if (currentSegment.isNotEmpty()) {
            survivingSegments.add(currentSegment)
        }

        if (survivingSegments.isEmpty()) return emptyList()

        // Check if untouched
        if (survivingSegments.size == 1 && survivingSegments[0].size == stroke.points.size) {
            return listOf(stroke)
        }

        val remnantThreshold = maxOf(stroke.width, minRemainder)

        fun segmentLength(ptsList: List<StrokePoint>): Float {
            var sum = 0.0f
            for (j in 0 until (ptsList.size - 1)) {
                sum += hypot(ptsList[j + 1].x - ptsList[j].x, ptsList[j + 1].y - ptsList[j].y)
            }
            return sum
        }

        val filteredSegments = survivingSegments.filter { sub ->
            if (sub.size <= 1) {
                false // Drop single dot remnants created by cutting
            } else {
                segmentLength(sub) >= remnantThreshold
            }
        }

        return filteredSegments.map { subPoints ->
            val subBounds = computeBounds(subPoints, stroke.width)
            stroke.copy(
                id = UUID.randomUUID().toString(),
                points = subPoints,
                bounds = subBounds
            )
        }
    }
}
