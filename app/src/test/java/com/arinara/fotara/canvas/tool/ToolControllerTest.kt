// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.tool

import com.arinara.fotara.canvas.engine.CanvasHistoryManager
import com.arinara.fotara.canvas.engine.CanvasRect
import com.arinara.fotara.canvas.engine.ViewportState
import com.arinara.fotara.canvas.gesture.PointerPoint
import com.arinara.fotara.canvas.model.CanvasDocument
import com.arinara.fotara.canvas.model.CanvasLayer
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ToolControllerTest {

    private val defaultViewport = ViewportState(scale = 1.0f, translateX = 0f, translateY = 0f)

    @Test
    fun testPenDrawing_CommitsThroughHistoryWithUndoRedo() {
        val controller = CanvasToolController()
        val historyManager = CanvasHistoryManager()
        val doc = CanvasDocument()

        // 1. Draw stroke
        controller.startStroke(10f, 10f, 0.8f)
        controller.appendPoints(
            listOf(PointerPoint(20f, 20f, 0.9f), PointerPoint(30f, 30f, 1.0f)),
            defaultViewport
        )
        assertTrue(controller.isDrawing())

        // 2. Finish stroke
        val (committedDoc, bounds) = controller.finishStroke(doc, historyManager)
        assertFalse(controller.isDrawing())
        assertEquals(1, committedDoc.elements.size)
        assertTrue(bounds.width > 0f)

        // 3. Undo stroke
        val undoneDoc = historyManager.undo(committedDoc)
        assertNotNull(undoneDoc)
        assertEquals(0, undoneDoc!!.elements.size)

        // 4. Redo stroke
        val redoneDoc = historyManager.redo(undoneDoc)
        assertNotNull(redoneDoc)
        assertEquals(1, redoneDoc!!.elements.size)
    }

    @Test
    fun testFreeEraser_SweepRemovesEntireStrokeViaCommand() {
        val controller = CanvasToolController(
            toolState = CanvasToolState(activeTool = CanvasToolType.ERASER, eraserRadius = 25f)
        )
        val historyManager = CanvasHistoryManager()
        val layer = CanvasLayer(id = "layer_1", name = "Layer 1")
        val stroke = StrokeElement(
            id = "stroke_1",
            layerId = "layer_1",
            points = listOf(StrokePoint(100f, 100f), StrokePoint(150f, 150f), StrokePoint(200f, 200f)),
            color = 0xFFFFFFFF,
            width = 4f,
            bounds = CanvasRect(100f, 100f, 200f, 200f)
        )
        val doc = CanvasDocument(layers = listOf(layer), elements = listOf(stroke))

        // Sweep over entire stroke from (90,90) to (210,210)
        controller.startEraser(90f, 90f)
        val (previewDoc, _) = controller.sweepEraser(
            listOf(PointerPoint(150f, 150f), PointerPoint(210f, 210f)),
            defaultViewport,
            doc
        )
        val (erasedDoc, dirtyBounds) = controller.finishEraser(previewDoc, historyManager)
        assertEquals(0, erasedDoc.elements.size)
        assertNotNull(dirtyBounds)

        // Undo restores stroke
        val restoredDoc = historyManager.undo(erasedDoc)
        assertNotNull(restoredDoc)
        assertEquals(1, restoredDoc!!.elements.size)
    }

    @Test
    fun testAreaEraser_SplitsIntersectedStrokeIntoSurvivingParts() {
        val controller = CanvasToolController(
            toolState = CanvasToolState(activeTool = CanvasToolType.ERASER, eraserRadius = 15f)
        )
        val historyManager = CanvasHistoryManager()
        val layer = CanvasLayer(id = "layer_1", name = "Layer 1")

        // Long stroke along horizontal line from 0 to 400
        val points = (0..40).map { i -> StrokePoint(i * 10f, 100f) }
        val stroke = StrokeElement(
            id = "long_stroke",
            layerId = "layer_1",
            points = points,
            color = 0xFFFFFFFF,
            width = 4f,
            bounds = CanvasRect(0f, 98f, 400f, 102f)
        )
        val doc = CanvasDocument(layers = listOf(layer), elements = listOf(stroke))

        // Erase area at (200, 100) with radius 15
        val (splitDoc, dirtyBounds) = controller.eraseAt(200f, 100f, defaultViewport, doc, historyManager)
        assertNotNull(dirtyBounds)
        // Stroke should have been split into at least 2 surviving parts
        assertTrue("Expected stroke to be split into 2 parts, got ${splitDoc.elements.size}", splitDoc.elements.size >= 2)

        // Undo restores the single intact original stroke
        val restoredDoc = historyManager.undo(splitDoc)
        assertNotNull(restoredDoc)
        assertEquals(1, restoredDoc!!.elements.size)
        assertEquals("long_stroke", restoredDoc.elements.first().id)
    }

    @Test
    fun testLockedAndHiddenLayers_CannotBeErasedOrSelected() {
        val controller = CanvasToolController(
            toolState = CanvasToolState(activeTool = CanvasToolType.ERASER, eraserRadius = 30f)
        )
        val historyManager = CanvasHistoryManager()

        val lockedLayer = CanvasLayer(id = "locked_layer", name = "Locked", isLocked = true)
        val hiddenLayer = CanvasLayer(id = "hidden_layer", name = "Hidden", isVisible = false)

        val strokeLocked = StrokeElement(
            id = "s_locked",
            layerId = "locked_layer",
            points = listOf(StrokePoint(50f, 50f), StrokePoint(60f, 60f)),
            color = 0xFFFFFFFF,
            width = 4f,
            bounds = CanvasRect(50f, 50f, 60f, 60f)
        )
        val strokeHidden = StrokeElement(
            id = "s_hidden",
            layerId = "hidden_layer",
            points = listOf(StrokePoint(100f, 100f), StrokePoint(120f, 120f)),
            color = 0xFFFFFFFF,
            width = 4f,
            bounds = CanvasRect(100f, 100f, 120f, 120f)
        )
        val doc = CanvasDocument(
            layers = listOf(lockedLayer, hiddenLayer),
            elements = listOf(strokeLocked, strokeHidden)
        )

        // Attempt to erase locked stroke
        val (afterLockedErase, _) = controller.eraseAt(55f, 55f, defaultViewport, doc, historyManager)
        assertEquals(2, afterLockedErase.elements.size)

        // Attempt to erase hidden stroke
        val (afterHiddenErase, _) = controller.eraseAt(110f, 110f, defaultViewport, doc, historyManager)
        assertEquals(2, afterHiddenErase.elements.size)

        // Attempt to tap-select locked stroke
        val selectedLocked = controller.selectTap(55f, 55f, defaultViewport, doc)
        assertTrue(selectedLocked.isEmpty)

        // Attempt to tap-select hidden stroke
        val selectedHidden = controller.selectTap(110f, 110f, defaultViewport, doc)
        assertTrue(selectedHidden.isEmpty)
    }

    @Test
    fun testTapSelection_PicksHighestZIndexElement() {
        val controller = CanvasToolController()
        val layer = CanvasLayer(id = "layer_1", name = "Layer 1")

        val strokeBottom = StrokeElement(
            id = "bottom_stroke",
            layerId = "layer_1",
            points = listOf(StrokePoint(100f, 100f), StrokePoint(150f, 150f)),
            color = 0xFF000000,
            width = 8f,
            bounds = CanvasRect(100f, 100f, 150f, 150f),
            zIndex = 1
        )
        val strokeTop = StrokeElement(
            id = "top_stroke",
            layerId = "layer_1",
            points = listOf(StrokePoint(100f, 100f), StrokePoint(150f, 150f)),
            color = 0xFFFFFFFF,
            width = 8f,
            bounds = CanvasRect(100f, 100f, 150f, 150f),
            zIndex = 2
        )
        val doc = CanvasDocument(layers = listOf(layer), elements = listOf(strokeBottom, strokeTop))

        val selected = controller.selectTap(125f, 125f, defaultViewport, doc)
        assertEquals(1, selected.elementIds.size)
        assertEquals("top_stroke", selected.elementIds.first())
    }

    @Test
    fun testLassoSelection_PicksOnlyElementsInsidePolygon() {
        val controller = CanvasToolController()
        val layer = CanvasLayer(id = "layer_1", name = "Layer 1")

        val strokeInside = StrokeElement(
            id = "inside_stroke",
            layerId = "layer_1",
            points = listOf(StrokePoint(50f, 50f), StrokePoint(60f, 60f)),
            color = 0xFFFFFFFF,
            width = 2f,
            bounds = CanvasRect(50f, 50f, 60f, 60f)
        )
        val strokeOutside = StrokeElement(
            id = "outside_stroke",
            layerId = "layer_1",
            points = listOf(StrokePoint(500f, 500f), StrokePoint(510f, 510f)),
            color = 0xFFFFFFFF,
            width = 2f,
            bounds = CanvasRect(500f, 500f, 510f, 510f)
        )
        val doc = CanvasDocument(layers = listOf(layer), elements = listOf(strokeInside, strokeOutside))

        // Polygon encircling (0,0) to (100,100)
        val polygon = listOf(
            Pair(0f, 0f),
            Pair(100f, 0f),
            Pair(100f, 100f),
            Pair(0f, 100f)
        )

        val selected = controller.selectLasso(polygon, defaultViewport, doc)
        assertEquals(1, selected.elementIds.size)
        assertEquals("inside_stroke", selected.elementIds.first())
    }
}
