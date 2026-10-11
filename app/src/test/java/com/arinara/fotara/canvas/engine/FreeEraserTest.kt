// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import com.arinara.fotara.canvas.gesture.PointerPoint
import com.arinara.fotara.canvas.model.CanvasDocument
import com.arinara.fotara.canvas.model.CanvasLayer
import com.arinara.fotara.canvas.model.ImageElement
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType
import com.arinara.fotara.canvas.tool.CanvasToolController
import com.arinara.fotara.canvas.tool.CanvasToolState
import com.arinara.fotara.canvas.tool.CanvasToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FreeEraserTest {

    private val defaultViewport = ViewportState(scale = 1.0f, translateX = 0f, translateY = 0f)

    @Test
    fun testCapsuleSweep_TouchPointsFarApart_LeavesNoGaps() {
        // Vertical line passing through y = 100 to y = 500 at x = 200
        val points = (10..50).map { i -> StrokePoint(200f, i * 10f) }
        val stroke = StrokeElement(
            id = "vert_stroke",
            layerId = "l1",
            points = points,
            color = 0xFFFFFFFF,
            width = 4f,
            bounds = CanvasRect(198f, 100f, 202f, 500f)
        )
        val doc = CanvasDocument(layers = listOf(CanvasLayer("l1", "L1")), elements = listOf(stroke))

        // Fast horizontal sweep from (100, 300) to (300, 300) with points far apart (200px apart)
        val splitParts = StrokeProcessor.eraseStrokeWithCapsule(
            stroke = stroke,
            ax = 100f, ay = 300f,
            bx = 300f, by = 300f,
            eraserRadius = 20f,
            minRemainder = 4f
        )

        // The capsule should cleanly slice the stroke into top and bottom surviving segments with no dotted remnants
        assertEquals(2, splitParts.size)
        val topPart = splitParts[0]
        val bottomPart = splitParts[1]
        assertTrue(topPart.points.last().y <= 280f)
        assertTrue(bottomPart.points.first().y >= 320f)
    }

    @Test
    fun testRemnantRule_RemovesShortSurvivingPieces() {
        // Short stroke of length 3dp (from x=100 to x=103)
        // With minRemainder = 4dp, this remnant must be completely discarded
        val shortStroke = StrokeElement(
            id = "speck_stroke",
            layerId = "l1",
            points = listOf(StrokePoint(100f, 100f), StrokePoint(103f, 100f)),
            color = 0xFFFFFFFF,
            width = 2f,
            bounds = CanvasRect(99f, 99f, 104f, 101f)
        )

        val splitParts = StrokeProcessor.eraseStrokeWithCapsule(
            stroke = shortStroke,
            ax = 101f, ay = 90f,
            bx = 101f, by = 110f,
            eraserRadius = 5f,
            minRemainder = 4f
        )

        // Nothing survives because remaining pieces are smaller than minRemainder
        assertTrue(splitParts.isEmpty())
    }

    @Test
    fun testOneDragOneUndoStep_RestoresExactOriginalStrokes() {
        val controller = CanvasToolController(
            toolState = CanvasToolState(activeTool = CanvasToolType.ERASER, eraserRadius = 15f)
        )
        val historyManager = CanvasHistoryManager()
        val originalStroke = StrokeElement(
            id = "orig_stroke",
            layerId = "l1",
            points = (0..20).map { i -> StrokePoint(i * 10f, 100f) },
            color = 0xFFFFFFFF,
            width = 4f,
            bounds = CanvasRect(0f, 98f, 200f, 102f)
        )
        val doc = CanvasDocument(layers = listOf(CanvasLayer("l1", "L1")), elements = listOf(originalStroke))

        // Start drag gesture
        controller.startEraser(50f, 100f)
        // Move through several points during the drag
        controller.sweepEraser(listOf(PointerPoint(60f, 100f)), defaultViewport, doc)
        controller.sweepEraser(listOf(PointerPoint(70f, 100f)), defaultViewport, doc)
        val (previewDoc, _) = controller.sweepEraser(listOf(PointerPoint(80f, 100f)), defaultViewport, doc)

        // Commit drag gesture as exactly ONE undo step
        val (committedDoc, _) = controller.finishEraser(previewDoc, historyManager)
        assertTrue(committedDoc.elements.size >= 2)

        // Single undo step restores the document exactly
        val undoneDoc = historyManager.undo(committedDoc)
        assertNotNull(undoneDoc)
        assertEquals(1, undoneDoc!!.elements.size)
        val restored = undoneDoc.elements[0] as StrokeElement
        assertEquals("orig_stroke", restored.id)
        assertEquals(originalStroke.points.size, restored.points.size)
        for (i in originalStroke.points.indices) {
            assertEquals(originalStroke.points[i].x, restored.points[i].x, 0.001f)
            assertEquals(originalStroke.points[i].y, restored.points[i].y, 0.001f)
        }
    }

    @Test
    fun testImagesAreImmuneToEraser() {
        val controller = CanvasToolController(
            toolState = CanvasToolState(activeTool = CanvasToolType.ERASER, eraserRadius = 30f)
        )
        val historyManager = CanvasHistoryManager()
        val image = ImageElement(
            id = "immune_img",
            layerId = "l1",
            assetId = "asset_1",
            x = 50f,
            y = 50f,
            width = 100f,
            height = 100f,
            bounds = CanvasRect(50f, 50f, 150f, 150f)
        )
        val doc = CanvasDocument(layers = listOf(CanvasLayer("l1", "L1")), elements = listOf(image))

        // Sweep directly across the image center
        controller.startEraser(50f, 100f)
        val (previewDoc, _) = controller.sweepEraser(listOf(PointerPoint(150f, 100f)), defaultViewport, doc)
        val (committedDoc, _) = controller.finishEraser(previewDoc, historyManager)

        // Image must remain completely untouched
        assertEquals(1, committedDoc.elements.size)
        assertEquals("immune_img", committedDoc.elements[0].id)
    }
}
