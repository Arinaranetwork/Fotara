// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import com.arinara.fotara.canvas.model.StrokePoint
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin
import kotlin.math.sqrt

sealed class RecognizedShape {
    data class StraightLine(val start: StrokePoint, val end: StrokePoint) : RecognizedShape()
    data class StraightArrow(val start: StrokePoint, val tip: StrokePoint, val wing1: StrokePoint, val wing2: StrokePoint) : RecognizedShape()
    data class Circle(val centerX: Float, val centerY: Float, val radius: Float) : RecognizedShape()
    data class Ellipse(val centerX: Float, val centerY: Float, val radiusX: Float, val radiusY: Float) : RecognizedShape()
    data class Rectangle(val corners: List<StrokePoint>) : RecognizedShape()
    data class Triangle(val corners: List<StrokePoint>) : RecognizedShape()
    data class SmoothCurve(val smoothedPoints: List<StrokePoint>) : RecognizedShape()
    object None : RecognizedShape()
}

data class ShapeAutoCorrectResult(
    val shape: RecognizedShape,
    val snappedPoints: List<StrokePoint>
)

/**
 * Pure mathematical shape recognition and auto-correction engine.
 * Detects geometric primitives (circle, ellipse, rectangle, triangle, straight line, arrow)
 * and applies cubic Bézier smoothing to freehand strokes on a 400ms draw-and-hold gesture.
 */
object ShapeAutoCorrectEngine {

    const val HOLD_THRESHOLD_MS = 400L

    /**
     * Analyzes raw stroke points and snaps to a recognized geometric primitive
     * or smoothed Bézier curve.
     */
    fun recognizeAndSnap(points: List<StrokePoint>): ShapeAutoCorrectResult {
        if (points.size < 5) {
            return ShapeAutoCorrectResult(RecognizedShape.None, points)
        }

        val p0 = points.first()
        val pn = points.last()
        val avgPressure = points.map { it.pressure }.average().toFloat().coerceIn(0.1f, 1.0f)

        // 1. Compute arc length and bounding box
        var arcLength = 0f
        var minX = points[0].x
        var maxX = points[0].x
        var minY = points[0].y
        var maxY = points[0].y

        for (i in 1 until points.size) {
            val d = hypot(points[i].x - points[i - 1].x, points[i].y - points[i - 1].y)
            arcLength += d
            if (points[i].x < minX) minX = points[i].x
            if (points[i].x > maxX) maxX = points[i].x
            if (points[i].y < minY) minY = points[i].y
            if (points[i].y > maxY) maxY = points[i].y
        }

        val width = maxX - minX
        val height = maxY - minY
        val diagonal = hypot(width, height)

        if (diagonal < 12f || arcLength < 16f) {
            return ShapeAutoCorrectResult(RecognizedShape.None, points)
        }

        val endDistance = hypot(pn.x - p0.x, pn.y - p0.y)
        val isClosed = endDistance < 0.28f * diagonal || (arcLength > 0 && endDistance < 0.22f * arcLength)

        // --- Branch A: Closed Shapes (Rectangle, Triangle, Circle, Ellipse) ---
        if (isClosed) {
            val centerX = (minX + maxX) / 2f
            val centerY = (minY + maxY) / 2f
            val aspect = if (maxOf(width, height) > 0f) minOf(width, height) / maxOf(width, height) else 0f

            // A1. Polygons via Douglas-Peucker simplification (sharp corners check)
            val epsilon = (diagonal * 0.07f).coerceIn(4f, 30f)
            val simplified = douglasPeucker(points, epsilon)
            // Remove closing duplicate if present
            val vertices = if (simplified.size > 2 && hypot(simplified.first().x - simplified.last().x, simplified.first().y - simplified.last().y) < epsilon * 1.5f) {
                simplified.dropLast(1)
            } else {
                simplified
            }

            // Triangle: 3 dominant corners
            if (vertices.size == 3) {
                val triPoints = generatePolygonPoints(vertices, avgPressure)
                return ShapeAutoCorrectResult(RecognizedShape.Triangle(vertices), triPoints)
            }

            // Rectangle: 4 dominant corners with roughly right angles
            if (vertices.size == 4) {
                val isRect = areAnglesApproximatelyRight(vertices)
                if (isRect) {
                    val rectPoints = generatePolygonPoints(vertices, avgPressure)
                    return ShapeAutoCorrectResult(RecognizedShape.Rectangle(vertices), rectPoints)
                }
            }

            // A2. Smooth Conics (Circle & Ellipse)
            val area = computePolygonArea(points)
            val circularity = if (arcLength > 0f) (4f * PI.toFloat() * area) / (arcLength * arcLength) else 0f

            // Circle
            if (circularity > 0.72f && aspect >= 0.75f) {
                val radius = (width + height) / 4f
                val circlePoints = generateCirclePoints(centerX, centerY, radius, avgPressure)
                return ShapeAutoCorrectResult(RecognizedShape.Circle(centerX, centerY, radius), circlePoints)
            }

            // Ellipse
            if (circularity > 0.58f && aspect < 0.75f) {
                val rx = width / 2f
                val ry = height / 2f
                val ellipsePoints = generateEllipsePoints(centerX, centerY, rx, ry, avgPressure)
                return ShapeAutoCorrectResult(RecognizedShape.Ellipse(centerX, centerY, rx, ry), ellipsePoints)
            }

            // If not recognized as polygon primitive, apply smooth closed Bézier
            val smoothed = smoothPointsBezier(points, avgPressure, isClosed = true)
            return ShapeAutoCorrectResult(RecognizedShape.SmoothCurve(smoothed), smoothed)
        }

        // --- Branch B: Open Shapes (Straight Line, Straight Arrow, Smooth Curve) ---
        // B1. Straight Line Check
        val lineRatio = if (arcLength > 0f) endDistance / arcLength else 0f
        val maxDeviation = maxPerpendicularDeviation(points, p0, pn)

        if (lineRatio >= 0.90f && maxDeviation < diagonal * 0.12f) {
            val linePoints = generateLinePoints(p0, pn, avgPressure)
            return ShapeAutoCorrectResult(RecognizedShape.StraightLine(p0, pn), linePoints)
        }

        // B2. Straight Arrow Check (Straight shaft + V-head at end)
        val arrowResult = detectArrow(points, avgPressure)
        if (arrowResult != null) {
            return arrowResult
        }

        // B3. Organic Handwriting Curve with Bézier Smoothing
        val smoothed = smoothPointsBezier(points, avgPressure, isClosed = false)
        return ShapeAutoCorrectResult(RecognizedShape.SmoothCurve(smoothed), smoothed)
    }

    private fun computePolygonArea(points: List<StrokePoint>): Float {
        var sum = 0f
        for (i in points.indices) {
            val next = (i + 1) % points.size
            sum += (points[i].x * points[next].y) - (points[next].x * points[i].y)
        }
        return abs(sum) / 2f
    }

    private fun maxPerpendicularDeviation(points: List<StrokePoint>, a: StrokePoint, b: StrokePoint): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len = hypot(dx, dy)
        if (len < 0.001f) return 0f

        var maxDev = 0f
        for (p in points) {
            val dev = abs(dy * p.x - dx * p.y + b.x * a.y - b.y * a.x) / len
            if (dev > maxDev) maxDev = dev
        }
        return maxDev
    }

    private fun areAnglesApproximatelyRight(corners: List<StrokePoint>): Boolean {
        if (corners.size != 4) return false
        for (i in 0 until 4) {
            val pPrev = corners[(i + 3) % 4]
            val pCurr = corners[i]
            val pNext = corners[(i + 1) % 4]

            val v1x = pPrev.x - pCurr.x
            val v1y = pPrev.y - pCurr.y
            val v2x = pNext.x - pCurr.x
            val v2y = pNext.y - pCurr.y

            val dot = v1x * v2x + v1y * v2y
            val mag = hypot(v1x, v1y) * hypot(v2x, v2y)
            if (mag < 0.001f) return false
            val cosAngle = abs(dot / mag)
            // 90 deg -> cos = 0. Allow cosAngle < 0.45 (angles between 63° and 117°)
            if (cosAngle > 0.45f) return false
        }
        return true
    }

    private fun detectArrow(points: List<StrokePoint>, avgPressure: Float): ShapeAutoCorrectResult? {
        if (points.size < 12) return null
        val totalCount = points.size
        val shaftEndIndex = (totalCount * 0.75f).toInt()
        val shaftPoints = points.subList(0, shaftEndIndex)
        val tipPoints = points.subList(shaftEndIndex, totalCount)

        val p0 = shaftPoints.first()
        val pShaftEnd = shaftPoints.last()
        val shaftDist = hypot(pShaftEnd.x - p0.x, pShaftEnd.y - p0.y)
        var shaftArc = 0f
        for (i in 1 until shaftPoints.size) {
            shaftArc += hypot(shaftPoints[i].x - shaftPoints[i - 1].x, shaftPoints[i].y - shaftPoints[i - 1].y)
        }
        val shaftRatio = if (shaftArc > 0f) shaftDist / shaftArc else 0f
        if (shaftRatio < 0.88f) return null

        // Check if head points turn back significantly relative to shaft direction
        val shaftAngle = atan2(pShaftEnd.y - p0.y, pShaftEnd.x - p0.x)
        val tip = points.maxByOrNull { hypot(it.x - p0.x, it.y - p0.y) } ?: pShaftEnd

        val wingLength = (shaftDist * 0.22f).coerceIn(16f, 48f)
        val wingAngle = 0.52f // ~30 degrees

        val w1x = tip.x - wingLength * cos(shaftAngle - wingAngle)
        val w1y = tip.y - wingLength * sin(shaftAngle - wingAngle)
        val w2x = tip.x - wingLength * cos(shaftAngle + wingAngle)
        val w2y = tip.y - wingLength * sin(shaftAngle + wingAngle)

        val wing1 = StrokePoint(w1x, w1y, avgPressure)
        val wing2 = StrokePoint(w2x, w2y, avgPressure)

        val arrowPoints = mutableListOf<StrokePoint>()
        // Shaft: p0 -> tip
        arrowPoints.addAll(generateLinePoints(p0, tip, avgPressure))
        // Wing 1: tip -> wing1
        arrowPoints.addAll(generateLinePoints(tip, wing1, avgPressure))
        // Wing 2: tip -> wing2
        arrowPoints.addAll(generateLinePoints(tip, wing2, avgPressure))

        return ShapeAutoCorrectResult(
            RecognizedShape.StraightArrow(p0, tip, wing1, wing2),
            arrowPoints
        )
    }

    private fun generateLinePoints(a: StrokePoint, b: StrokePoint, pressure: Float, segments: Int = 12): List<StrokePoint> {
        val list = mutableListOf<StrokePoint>()
        for (i in 0..segments) {
            val t = i.toFloat() / segments
            list.add(StrokePoint(a.x + t * (b.x - a.x), a.y + t * (b.y - a.y), pressure))
        }
        return list
    }

    private fun generateCirclePoints(cx: Float, cy: Float, radius: Float, pressure: Float, segments: Int = 40): List<StrokePoint> {
        val list = mutableListOf<StrokePoint>()
        for (i in 0..segments) {
            val angle = (2.0 * PI * i / segments).toFloat()
            val x = cx + radius * cos(angle)
            val y = cy + radius * sin(angle)
            list.add(StrokePoint(x, y, pressure))
        }
        return list
    }

    private fun generateEllipsePoints(cx: Float, cy: Float, rx: Float, ry: Float, pressure: Float, segments: Int = 40): List<StrokePoint> {
        val list = mutableListOf<StrokePoint>()
        for (i in 0..segments) {
            val angle = (2.0 * PI * i / segments).toFloat()
            val x = cx + rx * cos(angle)
            val y = cy + ry * sin(angle)
            list.add(StrokePoint(x, y, pressure))
        }
        return list
    }

    private fun generatePolygonPoints(vertices: List<StrokePoint>, pressure: Float): List<StrokePoint> {
        val list = mutableListOf<StrokePoint>()
        for (i in vertices.indices) {
            val v1 = vertices[i]
            val v2 = vertices[(i + 1) % vertices.size]
            list.addAll(generateLinePoints(v1, v2, pressure, segments = 8).dropLast(1))
        }
        list.add(StrokePoint(vertices.first().x, vertices.first().y, pressure))
        return list
    }

    /**
     * Ramer-Douglas-Peucker polyline simplification algorithm.
     */
    private fun douglasPeucker(points: List<StrokePoint>, epsilon: Float): List<StrokePoint> {
        if (points.size < 3) return points

        var maxDist = 0f
        var index = 0
        val first = points.first()
        val last = points.last()

        for (i in 1 until points.size - 1) {
            val dist = perpendicularDistance(points[i], first, last)
            if (dist > maxDist) {
                maxDist = dist
                index = i
            }
        }

        return if (maxDist > epsilon) {
            val left = douglasPeucker(points.subList(0, index + 1), epsilon)
            val right = douglasPeucker(points.subList(index, points.size), epsilon)
            left.dropLast(1) + right
        } else {
            listOf(first, last)
        }
    }

    private fun perpendicularDistance(pt: StrokePoint, a: StrokePoint, b: StrokePoint): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        val len = hypot(dx, dy)
        if (len < 0.0001f) return hypot(pt.x - a.x, pt.y - a.y)
        return abs(dy * pt.x - dx * pt.y + b.x * a.y - b.y * a.x) / len
    }

    /**
     * Cubic Bézier smoothing for organic handwriting strokes.
     * Uses Catmull-Rom to cubic Bézier spline interpolation.
     */
    fun smoothPointsBezier(points: List<StrokePoint>, pressure: Float, isClosed: Boolean): List<StrokePoint> {
        if (points.size < 4) return points

        // Decimate first to extract dominant structural guide points
        val step = maxOf(1, points.size / 24)
        val controlPoints = points.filterIndexed { index, _ -> index % step == 0 || index == points.lastIndex }
        if (controlPoints.size < 3) return points

        val smoothed = mutableListOf<StrokePoint>()
        val count = controlPoints.size

        for (i in 0 until (if (isClosed) count else count - 1)) {
            val p0 = controlPoints[(i - 1 + count) % count]
            val p1 = controlPoints[i % count]
            val p2 = controlPoints[(i + 1) % count]
            val p3 = controlPoints[(i + 2) % count]

            for (stepIdx in 0 until 6) {
                val t = stepIdx / 6f
                val t2 = t * t
                val t3 = t2 * t

                // Catmull-Rom basis matrix evaluation
                val x = 0.5f * ((2f * p1.x) +
                        (-p0.x + p2.x) * t +
                        (2f * p0.x - 5f * p1.x + 4f * p2.x - p3.x) * t2 +
                        (-p0.x + 3f * p1.x - 3f * p2.x + p3.x) * t3)
                val y = 0.5f * ((2f * p1.y) +
                        (-p0.y + p2.y) * t +
                        (2f * p0.y - 5f * p1.y + 4f * p2.y - p3.y) * t2 +
                        (-p0.y + 3f * p1.y - 3f * p2.y + p3.y) * t3)

                smoothed.add(StrokePoint(x, y, pressure))
            }
        }
        if (isClosed) {
            smoothed.add(smoothed.first())
        } else {
            smoothed.add(controlPoints.last())
        }
        return smoothed
    }
}
