// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeProcessorTest {

    @Test
    fun testPressureNormalization() {
        assertEquals(1.0f, StrokeProcessor.normalizePressure(0.0f), 0.001f)
        assertEquals(1.0f, StrokeProcessor.normalizePressure(Float.NaN), 0.001f)
        assertEquals(0.05f, StrokeProcessor.normalizePressure(0.01f), 0.001f)
        assertEquals(0.85f, StrokeProcessor.normalizePressure(0.85f), 0.001f)
        assertEquals(1.0f, StrokeProcessor.normalizePressure(1.5f), 0.001f)
    }

    @Test
    fun testDegenerateSingleTapDot_ProducesValidBounds() {
        val singlePoint = listOf(StrokePoint(100f, 200f, 1.0f))
        val bounds = StrokeProcessor.computeBounds(singlePoint, width = 6.0f)

        assertFalse(bounds.isEmpty)
        // With width=6.0, halfW is 3.0 + 1.0 = 4.0
        assertEquals(96f, bounds.left, 0.01f)
        assertEquals(196f, bounds.top, 0.01f)
        assertEquals(104f, bounds.right, 0.01f)
        assertEquals(204f, bounds.bottom, 0.01f)
        assertTrue(bounds.contains(100f, 200f))
    }

    @Test
    fun testZeroLengthStroke_DoesNotCrash() {
        val zeroLengthPoints = listOf(
            StrokePoint(50f, 50f, 0.5f),
            StrokePoint(50f, 50f, 0.5f)
        )
        val bounds = StrokeProcessor.computeBounds(zeroLengthPoints, width = 4.0f)
        assertFalse(bounds.isEmpty)
        assertTrue(bounds.contains(50f, 50f))
    }

    @Test
    fun testDecimatePoints_ReducesCollinearPoints() {
        val collinearPoints = listOf(
            StrokePoint(0f, 0f),
            StrokePoint(10f, 10f),
            StrokePoint(20f, 20f),
            StrokePoint(30f, 30f),
            StrokePoint(40f, 40f)
        )
        val decimated = StrokeProcessor.decimatePoints(collinearPoints, tolerance = 1.0f)

        // All intermediate points are exactly on the line between (0,0) and (40,40),
        // so RDP should simplify down to endpoints
        assertEquals(2, decimated.size)
        assertEquals(0f, decimated.first().x, 0.01f)
        assertEquals(40f, decimated.last().x, 0.01f)
    }

    @Test
    fun testDistanceToSegment_CalculatesCorrectPerpendicularDistance() {
        // Line segment from (0, 0) to (100, 0)
        val pX = 50f
        val pY = 25f
        val dist = StrokeProcessor.distanceToSegment(pX, pY, 0f, 0f, 100f, 0f)
        assertEquals(25f, dist, 0.001f)

        // Point beyond the end of the segment (150, 0) -> distance to (100, 0) is 50
        val distEnd = StrokeProcessor.distanceToSegment(150f, 0f, 0f, 0f, 100f, 0f)
        assertEquals(50f, distEnd, 0.001f)
    }

    @Test
    fun testHitTestStroke_ConsidersStrokeWidth() {
        val points = listOf(
            StrokePoint(0f, 0f),
            StrokePoint(100f, 0f)
        )
        val stroke = StrokeElement(
            id = "stroke_1",
            layerId = "layer_1",
            points = points,
            color = 0xFF000000,
            width = 10.0f,
            bounds = StrokeProcessor.computeBounds(points, 10.0f)
        )

        // Point at (50, 4) is within 5px half-width -> hit
        assertTrue(StrokeProcessor.hitTestStroke(50f, 4f, stroke))

        // Point at (50, 8) is beyond half-width without tolerance -> miss
        assertFalse(StrokeProcessor.hitTestStroke(50f, 8f, stroke, hitRadius = 0f))

        // Point at (50, 8) with hitRadius 5f -> hit
        assertTrue(StrokeProcessor.hitTestStroke(50f, 8f, stroke, hitRadius = 5f))
    }

    @Test
    fun testLassoPolygonSelection() {
        val polygon = listOf(
            Pair(0f, 0f),
            Pair(100f, 0f),
            Pair(100f, 100f),
            Pair(0f, 100f)
        )

        // Inside point
        assertTrue(StrokeProcessor.isPointInPolygon(50f, 50f, polygon))

        // Outside point
        assertFalse(StrokeProcessor.isPointInPolygon(150f, 50f, polygon))

        // Stroke completely inside
        val insideStroke = StrokeElement(
            layerId = "layer_1",
            points = listOf(StrokePoint(20f, 20f), StrokePoint(80f, 80f)),
            color = 0xFF000000,
            width = 2f,
            bounds = CanvasRect(20f, 20f, 80f, 80f)
        )
        assertTrue(StrokeProcessor.isStrokeInsideLasso(insideStroke, polygon))

        // Stroke completely outside
        val outsideStroke = StrokeElement(
            layerId = "layer_1",
            points = listOf(StrokePoint(200f, 200f), StrokePoint(250f, 250f)),
            color = 0xFF000000,
            width = 2f,
            bounds = CanvasRect(200f, 200f, 250f, 250f)
        )
        assertFalse(StrokeProcessor.isStrokeInsideLasso(outsideStroke, polygon))
    }

    @Test
    fun testAreaErase_SplitsStrokeIntoMultipleSegments() {
        // Horizontal line from X=0 to X=100 with points every 10 units
        val points = (0..10).map { i -> StrokePoint(i * 10f, 50f) }
        val stroke = StrokeElement(
            id = "original_stroke",
            layerId = "layer_1",
            points = points,
            color = 0xFF000000,
            width = 4.0f,
            bounds = StrokeProcessor.computeBounds(points, 4.0f)
        )

        // Erase circle centered at (50, 50) with radius 12
        // This will delete point at 50 (dist=0) and point at 40 (dist=10) and point at 60 (dist=10)
        // Leaving segment 1: [0, 10, 20, 30] and segment 2: [70, 80, 90, 100]
        val result = StrokeProcessor.areaEraseStroke(
            stroke = stroke,
            eraserX = 50f,
            eraserY = 50f,
            eraserRadius = 12f
        )

        assertEquals(2, result.size)
        // First segment
        assertEquals(4, result[0].points.size)
        assertEquals(0f, result[0].points.first().x, 0.01f)
        assertEquals(30f, result[0].points.last().x, 0.01f)

        // Second segment
        assertEquals(4, result[1].points.size)
        assertEquals(70f, result[1].points.first().x, 0.01f)
        assertEquals(100f, result[1].points.last().x, 0.01f)
    }

    @Test
    fun testAreaErase_UntouchedReturnsOriginal() {
        val points = listOf(StrokePoint(0f, 0f), StrokePoint(10f, 10f))
        val stroke = StrokeElement(
            id = "untouched",
            layerId = "layer_1",
            points = points,
            color = 0xFF000000,
            width = 2.0f,
            bounds = StrokeProcessor.computeBounds(points, 2.0f)
        )

        val result = StrokeProcessor.areaEraseStroke(stroke, 500f, 500f, 10f)
        assertEquals(1, result.size)
        assertEquals("untouched", result[0].id)
    }

    @Test
    fun testAreaErase_CompleteEraseReturnsEmpty() {
        val points = listOf(StrokePoint(50f, 50f), StrokePoint(52f, 52f))
        val stroke = StrokeElement(
            id = "tiny",
            layerId = "layer_1",
            points = points,
            color = 0xFF000000,
            width = 2.0f,
            bounds = StrokeProcessor.computeBounds(points, 2.0f)
        )

        val result = StrokeProcessor.areaEraseStroke(stroke, 50f, 50f, 30f)
        assertTrue(result.isEmpty())
    }
}
