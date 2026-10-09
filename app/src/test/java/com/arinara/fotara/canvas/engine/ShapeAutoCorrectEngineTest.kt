// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import com.arinara.fotara.canvas.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class ShapeAutoCorrectEngineTest {

    @Test
    fun testShortStroke_ReturnsNone() {
        val fewPoints = listOf(
            StrokePoint(10f, 10f, 0.5f),
            StrokePoint(12f, 12f, 0.5f),
            StrokePoint(14f, 14f, 0.5f)
        )
        val result = ShapeAutoCorrectEngine.recognizeAndSnap(fewPoints)
        assertTrue(result.shape is RecognizedShape.None)
        assertEquals(fewPoints.size, result.snappedPoints.size)
    }

    @Test
    fun testTinyStroke_ReturnsNone() {
        val tinyPoints = (0..10).map { i ->
            StrokePoint(10f + i * 0.2f, 10f + i * 0.1f, 0.5f)
        }
        val result = ShapeAutoCorrectEngine.recognizeAndSnap(tinyPoints)
        assertTrue(result.shape is RecognizedShape.None)
    }

    @Test
    fun testStraightLineRecognition() {
        val startX = 20f
        val startY = 50f
        val endX = 250f
        val endY = 55f

        val points = (0..20).map { i ->
            val t = i / 20f
            // Add tiny jitter <= 0.5f
            val jitterY = if (i % 2 == 0) 0.4f else -0.4f
            StrokePoint(startX + t * (endX - startX), startY + t * (endY - startY) + jitterY, 0.6f)
        }

        val result = ShapeAutoCorrectEngine.recognizeAndSnap(points)
        assertTrue("Expected StraightLine, got ${result.shape}", result.shape is RecognizedShape.StraightLine)
        val line = result.shape as RecognizedShape.StraightLine
        assertEquals(startX, line.start.x, 1.0f)
        assertEquals(endX, line.end.x, 1.0f)
        assertTrue(result.snappedPoints.isNotEmpty())
    }

    @Test
    fun testCircleRecognition() {
        val cx = 150f
        val cy = 150f
        val radius = 60f
        val segments = 36

        val points = (0..segments).map { i ->
            val angle = (2.0 * PI * i / segments).toFloat()
            // Tiny hand-drawn jitter
            val rJitter = radius + if (i % 3 == 0) 1.5f else -1.5f
            StrokePoint(cx + rJitter * cos(angle), cy + rJitter * sin(angle), 0.7f)
        }

        val result = ShapeAutoCorrectEngine.recognizeAndSnap(points)
        assertTrue("Expected Circle, got ${result.shape}", result.shape is RecognizedShape.Circle)
        val circle = result.shape as RecognizedShape.Circle
        assertEquals(cx, circle.centerX, 5f)
        assertEquals(cy, circle.centerY, 5f)
        assertEquals(radius, circle.radius, 5f)
    }

    @Test
    fun testEllipseRecognition() {
        val cx = 200f
        val cy = 200f
        val rx = 100f
        val ry = 40f
        val segments = 36

        val points = (0..segments).map { i ->
            val angle = (2.0 * PI * i / segments).toFloat()
            StrokePoint(cx + rx * cos(angle), cy + ry * sin(angle), 0.7f)
        }

        val result = ShapeAutoCorrectEngine.recognizeAndSnap(points)
        assertTrue("Expected Ellipse, got ${result.shape}", result.shape is RecognizedShape.Ellipse)
        val ellipse = result.shape as RecognizedShape.Ellipse
        assertEquals(cx, ellipse.centerX, 5f)
        assertEquals(cy, ellipse.centerY, 5f)
        assertEquals(rx, ellipse.radiusX, 5f)
        assertEquals(ry, ellipse.radiusY, 5f)
    }

    @Test
    fun testRectangleRecognition() {
        val corners = listOf(
            StrokePoint(50f, 50f, 0.5f),
            StrokePoint(250f, 50f, 0.5f),
            StrokePoint(250f, 150f, 0.5f),
            StrokePoint(50f, 150f, 0.5f)
        )

        val points = mutableListOf<StrokePoint>()
        for (i in 0 until 4) {
            val c1 = corners[i]
            val c2 = corners[(i + 1) % 4]
            for (step in 0..10) {
                val t = step / 10f
                points.add(StrokePoint(c1.x + t * (c2.x - c1.x), c1.y + t * (c2.y - c1.y), 0.5f))
            }
        }
        // Close the loop
        points.add(corners.first())

        val result = ShapeAutoCorrectEngine.recognizeAndSnap(points)
        assertTrue("Expected Rectangle, got ${result.shape}", result.shape is RecognizedShape.Rectangle)
        val rect = result.shape as RecognizedShape.Rectangle
        assertEquals(4, rect.corners.size)
    }

    @Test
    fun testTriangleRecognition() {
        val corners = listOf(
            StrokePoint(150f, 40f, 0.5f),
            StrokePoint(250f, 200f, 0.5f),
            StrokePoint(50f, 200f, 0.5f)
        )

        val points = mutableListOf<StrokePoint>()
        for (i in 0 until 3) {
            val c1 = corners[i]
            val c2 = corners[(i + 1) % 3]
            for (step in 0..10) {
                val t = step / 10f
                points.add(StrokePoint(c1.x + t * (c2.x - c1.x), c1.y + t * (c2.y - c1.y), 0.5f))
            }
        }
        points.add(corners.first())

        val result = ShapeAutoCorrectEngine.recognizeAndSnap(points)
        assertTrue("Expected Triangle, got ${result.shape}", result.shape is RecognizedShape.Triangle)
        val tri = result.shape as RecognizedShape.Triangle
        assertEquals(3, tri.corners.size)
    }

    @Test
    fun testStraightArrowRecognition() {
        val points = mutableListOf<StrokePoint>()
        // Shaft: (50, 100) -> (220, 100)
        for (i in 0..16) {
            val t = i / 16f
            points.add(StrokePoint(50f + t * 170f, 100f, 0.6f))
        }
        // Wing: head (220, 100) -> (190, 80)
        for (i in 1..4) {
            val t = i / 4f
            points.add(StrokePoint(220f - t * 30f, 100f - t * 20f, 0.6f))
        }

        val result = ShapeAutoCorrectEngine.recognizeAndSnap(points)
        assertTrue("Expected StraightArrow, got ${result.shape}", result.shape is RecognizedShape.StraightArrow)
        val arrow = result.shape as RecognizedShape.StraightArrow
        assertEquals(50f, arrow.start.x, 5f)
        assertEquals(220f, arrow.tip.x, 5f)
        assertTrue(result.snappedPoints.isNotEmpty())
    }

    @Test
    fun testBezierSmoothing_ProducesValidCurve() {
        val rawPoints = listOf(
            StrokePoint(10f, 10f, 0.5f),
            StrokePoint(20f, 35f, 0.5f),
            StrokePoint(40f, 20f, 0.5f),
            StrokePoint(60f, 50f, 0.5f),
            StrokePoint(80f, 15f, 0.5f),
            StrokePoint(100f, 40f, 0.5f)
        )

        val smoothed = ShapeAutoCorrectEngine.smoothPointsBezier(rawPoints, pressure = 0.5f, isClosed = false)
        assertTrue(smoothed.isNotEmpty())
        for (p in smoothed) {
            assertFalse(p.x.isNaN())
            assertFalse(p.y.isNaN())
        }
    }
}
