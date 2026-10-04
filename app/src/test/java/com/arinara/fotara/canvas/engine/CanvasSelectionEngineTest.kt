// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import com.arinara.fotara.canvas.model.CanvasDocument
import com.arinara.fotara.canvas.model.CanvasLayer
import com.arinara.fotara.canvas.model.CanvasSelection
import com.arinara.fotara.canvas.model.ImageElement
import com.arinara.fotara.canvas.model.SelectedElementReference
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokeInsideSegment
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasSelectionEngineTest {

    private val squarePolygon = listOf(
        Pair(100f, 100f),
        Pair(200f, 100f),
        Pair(200f, 200f),
        Pair(100f, 200f)
    )

    @Test
    fun testPointInPolygon_NonZeroWinding_SelfCrossingLoop() {
        // Figure-eight loop (0,0)->(100,100)->(0,100)->(100,0)
        val figureEight = listOf(
            Pair(0f, 0f),
            Pair(100f, 100f),
            Pair(0f, 100f),
            Pair(100f, 0f)
        )
        // Top lobe (50, 80) and bottom lobe (50, 20)
        assertTrue(CanvasSelectionEngine.isPointInPolygonWinding(50f, 80f, figureEight))
        assertTrue(CanvasSelectionEngine.isPointInPolygonWinding(50f, 20f, figureEight))
        assertFalse(CanvasSelectionEngine.isPointInPolygonWinding(150f, 150f, figureEight))
        assertFalse(CanvasSelectionEngine.isPointInPolygonWinding(25f, 50f, figureEight))
    }

    @Test
    fun testStroke_FullyInside_SelectedWhole() {
        val stroke = StrokeElement(
            id = "stroke_in",
            layerId = "l1",
            points = listOf(StrokePoint(120f, 120f), StrokePoint(150f, 150f), StrokePoint(180f, 180f)),
            color = 0xFFFFFFFF,
            width = 4f,
            bounds = CanvasRect(120f, 120f, 180f, 180f)
        )
        val ref = CanvasSelectionEngine.sliceStrokeWithLasso(stroke, squarePolygon, 1.0f)
        assertNotNull(ref)
        assertTrue(ref is SelectedElementReference.Whole)
        assertEquals("stroke_in", ref!!.elementId)
    }

    @Test
    fun testStroke_FullyOutside_NotSelected() {
        val stroke = StrokeElement(
            id = "stroke_out",
            layerId = "l1",
            points = listOf(StrokePoint(10f, 10f), StrokePoint(20f, 20f)),
            color = 0xFFFFFFFF,
            width = 4f,
            bounds = CanvasRect(10f, 10f, 20f, 20f)
        )
        val ref = CanvasSelectionEngine.sliceStrokeWithLasso(stroke, squarePolygon, 1.0f)
        assertNull(ref)
    }

    @Test
    fun testStroke_CrossingOnce_SlicedAtExactBoundary() {
        // Line from (50, 150) [outside] to (150, 150) [inside]
        val stroke = StrokeElement(
            id = "stroke_cross_1",
            layerId = "l1",
            points = listOf(StrokePoint(50f, 150f), StrokePoint(150f, 150f)),
            color = 0xFFFFFFFF,
            width = 2f,
            bounds = CanvasRect(50f, 150f, 150f, 150f)
        )
        val ref = CanvasSelectionEngine.sliceStrokeWithLasso(stroke, squarePolygon, 1.0f)
        assertNotNull(ref)
        assertTrue(ref is SelectedElementReference.PartialStroke)
        val partial = ref as SelectedElementReference.PartialStroke
        assertEquals(1, partial.insideSegments.size)
        val seg = partial.insideSegments[0]
        assertEquals(2, seg.points.size)
        // First point should be exactly on polygon boundary x = 100
        assertEquals(100f, seg.points[0].x, 0.01f)
        assertEquals(150f, seg.points[0].y, 0.01f)
        // Second point should be the original inside point
        assertEquals(150f, seg.points[1].x, 0.01f)
        assertEquals(150f, seg.points[1].y, 0.01f)
    }

    @Test
    fun testStroke_CrossingMultipleTimes_YieldsInsidePortion() {
        // Horizontal line passing all the way through: (50, 150) -> (250, 150)
        val stroke = StrokeElement(
            id = "stroke_cross_through",
            layerId = "l1",
            points = listOf(StrokePoint(50f, 150f), StrokePoint(250f, 150f)),
            color = 0xFFFFFFFF,
            width = 2f,
            bounds = CanvasRect(50f, 150f, 250f, 150f)
        )
        val ref = CanvasSelectionEngine.sliceStrokeWithLasso(stroke, squarePolygon, 1.0f)
        assertNotNull(ref)
        assertTrue(ref is SelectedElementReference.PartialStroke)
        val partial = ref as SelectedElementReference.PartialStroke
        assertEquals(1, partial.insideSegments.size)
        val seg = partial.insideSegments[0]
        // Bounded between x=100 and x=200
        assertEquals(100f, seg.points.first().x, 0.01f)
        assertEquals(200f, seg.points.last().x, 0.01f)
    }

    @Test
    fun testStroke_SinglePointDot_InsideVsOutside() {
        val dotInside = StrokeElement(
            id = "dot_in",
            layerId = "l1",
            points = listOf(StrokePoint(150f, 150f)),
            color = 0xFFFFFFFF,
            width = 4f,
            bounds = CanvasRect(148f, 148f, 152f, 152f)
        )
        val dotOutside = StrokeElement(
            id = "dot_out",
            layerId = "l1",
            points = listOf(StrokePoint(50f, 50f)),
            color = 0xFFFFFFFF,
            width = 4f,
            bounds = CanvasRect(48f, 48f, 52f, 52f)
        )

        val refIn = CanvasSelectionEngine.sliceStrokeWithLasso(dotInside, squarePolygon, 1.0f)
        val refOut = CanvasSelectionEngine.sliceStrokeWithLasso(dotOutside, squarePolygon, 1.0f)

        assertNotNull(refIn)
        assertTrue(refIn is SelectedElementReference.Whole)
        assertNull(refOut)
    }

    @Test
    fun testStroke_ShortRemainderRule_AbsorbedIntoSelection() {
        // Line from (98f, 150f) to (180f, 150f)
        // Outside remainder from 98f to 100f is only 2dp long (less than 4dp threshold)
        val stroke = StrokeElement(
            id = "stroke_short_rem",
            layerId = "l1",
            points = listOf(StrokePoint(98f, 150f), StrokePoint(180f, 150f)),
            color = 0xFFFFFFFF,
            width = 2f,
            bounds = CanvasRect(98f, 150f, 180f, 150f)
        )
        val ref = CanvasSelectionEngine.sliceStrokeWithLasso(stroke, squarePolygon, 1.0f)
        assertNotNull(ref)
        // Short outside remainder should be absorbed: selected whole
        assertTrue(ref is SelectedElementReference.Whole)
    }

    @Test
    fun testImageElement_SelectedWholeWhenCenterInside() {
        val imageInside = ImageElement(
            id = "img_in",
            layerId = "l1",
            assetId = "asset_1",
            x = 120f,
            y = 120f,
            width = 60f,
            height = 60f,
            bounds = CanvasRect(120f, 120f, 180f, 180f) // Center is (150, 150)
        )
        val imageOutside = ImageElement(
            id = "img_out",
            layerId = "l1",
            assetId = "asset_2",
            x = 10f,
            y = 10f,
            width = 50f,
            height = 50f,
            bounds = CanvasRect(10f, 10f, 60f, 60f) // Center is (35, 35)
        )

        val refIn = CanvasSelectionEngine.evaluateImageSelection(imageInside, squarePolygon)
        val refOut = CanvasSelectionEngine.evaluateImageSelection(imageOutside, squarePolygon)

        assertNotNull(refIn)
        assertTrue(refIn is SelectedElementReference.Whole)
        assertNull(refOut)
    }

    @Test
    fun testMaterializePartialSplit_MaintainsPropertiesAndExactContinuity() {
        val original = StrokeElement(
            id = "s_split",
            layerId = "l1",
            points = listOf(StrokePoint(50f, 150f), StrokePoint(250f, 150f)),
            color = 0xFF123456L,
            width = 6f,
            toolType = StrokeToolType.PEN,
            bounds = CanvasRect(50f, 150f, 250f, 150f),
            zIndex = 5
        )
        val insideSeg = StrokeInsideSegment(listOf(StrokePoint(100f, 150f), StrokePoint(200f, 150f)))
        val ref = SelectedElementReference.PartialStroke("s_split", listOf(insideSeg))

        val (outsideStrokes, insideStrokes) = CanvasSelectionEngine.materializePartialSplit(original, ref, 1.0f)

        // Should produce outside pieces and inside pieces
        assertTrue(outsideStrokes.isNotEmpty())
        assertEquals(1, insideStrokes.size)

        val allPieces = outsideStrokes + insideStrokes
        assertTrue(allPieces.all { it.color == 0xFF123456L })
        assertTrue(allPieces.all { it.width == 6f })
        assertTrue(allPieces.all { it.toolType == StrokeToolType.PEN })
        assertTrue(allPieces.all { it.zIndex == 5 })

        // Check exact boundary continuity between outside and inside
        val leftOutside = outsideStrokes.find { it.points.last().x == 100f }
        assertNotNull(leftOutside)
        val insidePiece = insideStrokes.first()
        assertEquals(100f, insidePiece.points.first().x, 0.001f)
    }
}
