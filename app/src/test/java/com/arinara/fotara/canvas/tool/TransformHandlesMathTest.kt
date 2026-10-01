// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.tool

import com.arinara.fotara.canvas.engine.CanvasHistoryManager
import com.arinara.fotara.canvas.engine.CanvasRect
import com.arinara.fotara.canvas.engine.ViewportState
import com.arinara.fotara.canvas.model.CanvasDocument
import com.arinara.fotara.canvas.model.CanvasLayer
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TransformHandlesMathTest {

    private val controller = CanvasToolController()
    private val defaultViewport = ViewportState(scale = 1.0f, translateX = 0f, translateY = 0f)

    @Test
    fun testHitTestHandles_AllEightHandlesAndStem() {
        // Selection bounds from (100, 100) to (300, 300)
        val bounds = CanvasRect(100f, 100f, 300f, 300f)

        // 0: NW corner at (100, 100)
        assertEquals(0, controller.hitTestHandles(102f, 98f, bounds, defaultViewport))

        // 1: N midpoint at (200, 100)
        assertEquals(1, controller.hitTestHandles(200f, 101f, bounds, defaultViewport))

        // 2: NE corner at (300, 100)
        assertEquals(2, controller.hitTestHandles(299f, 102f, bounds, defaultViewport))

        // 3: E midpoint at (300, 200)
        assertEquals(3, controller.hitTestHandles(300f, 200f, bounds, defaultViewport))

        // 4: SE corner at (300, 300)
        assertEquals(4, controller.hitTestHandles(301f, 299f, bounds, defaultViewport))

        // 5: S midpoint at (200, 300)
        assertEquals(5, controller.hitTestHandles(200f, 300f, bounds, defaultViewport))

        // 6: SW corner at (100, 300)
        assertEquals(6, controller.hitTestHandles(100f, 300f, bounds, defaultViewport))

        // 7: W midpoint at (100, 200)
        assertEquals(7, controller.hitTestHandles(101f, 199f, bounds, defaultViewport))

        // 8: Rotation handle stem above top-center: midX = 200, rotY = 100 - 28 = 72
        assertEquals(8, controller.hitTestHandles(200f, 72f, bounds, defaultViewport))

        // -1: Inside body drag
        assertEquals(-1, controller.hitTestHandles(200f, 200f, bounds, defaultViewport))

        // null: Outside far away
        assertNull(controller.hitTestHandles(20f, 20f, bounds, defaultViewport))
        assertNull(controller.hitTestHandles(500f, 500f, bounds, defaultViewport))
    }

    @Test
    fun testHitTestHandles_WithScaledAndTranslatedViewport() {
        val bounds = CanvasRect(100f, 100f, 200f, 200f)
        // Zoom = 2.0x, Pan = (+50, +50)
        // World (100, 100) -> Screen (100*2 + 50, 100*2 + 50) = (250, 250)
        val viewport = ViewportState(scale = 2.0f, translateX = 50f, translateY = 50f)

        // Hit testing at screen coordinates (250, 250) should hit NW corner (0)
        assertEquals(0, controller.hitTestHandles(250f, 250f, bounds, viewport))
    }

    @Test
    fun testBodyDragTransform_TranslatesElementsAndSupportsUndoRedo() {
        val historyManager = CanvasHistoryManager()
        val layer = CanvasLayer(id = "layer_1", name = "Layer 1")
        val stroke = StrokeElement(
            id = "stroke_1",
            layerId = "layer_1",
            points = listOf(StrokePoint(100f, 100f), StrokePoint(200f, 200f)),
            color = 0xFFFFFFFF,
            width = 4f,
            bounds = CanvasRect(100f, 100f, 200f, 200f)
        )
        var doc = CanvasDocument(layers = listOf(layer), elements = listOf(stroke))

        // Drag body (handleId = -1) by +30 screen X and +40 screen Y
        val updatedDoc = controller.applyTransformDelta(
            handleId = -1,
            deltaScreenX = 30f,
            deltaScreenY = 40f,
            viewport = defaultViewport,
            selectedIds = setOf("stroke_1"),
            document = doc,
            historyManager = historyManager
        )

        val movedStroke = updatedDoc.elements.first() as StrokeElement
        assertEquals(130f, movedStroke.points[0].x, 0.001f)
        assertEquals(140f, movedStroke.points[0].y, 0.001f)
        assertEquals(230f, movedStroke.points[1].x, 0.001f)
        assertEquals(240f, movedStroke.points[1].y, 0.001f)

        // Undo transform
        val undoneDoc = historyManager.undo(updatedDoc)
        assertNotNull(undoneDoc)
        val restoredStroke = undoneDoc!!.elements.first() as StrokeElement
        assertEquals(100f, restoredStroke.points[0].x, 0.001f)
        assertEquals(100f, restoredStroke.points[0].y, 0.001f)

        // Redo transform
        val redoneDoc = historyManager.redo(undoneDoc)
        assertNotNull(redoneDoc)
        val reMovedStroke = redoneDoc!!.elements.first() as StrokeElement
        assertEquals(130f, reMovedStroke.points[0].x, 0.001f)
        assertEquals(140f, reMovedStroke.points[0].y, 0.001f)
    }

    @Test
    fun testRotationHandle_RotatesPointsAroundCentroid() {
        val historyManager = CanvasHistoryManager()
        val layer = CanvasLayer(id = "layer_1", name = "Layer 1")
        // Stroke centered at (100, 100) extending from (50, 100) to (150, 100) (horizontal line)
        val stroke = StrokeElement(
            id = "stroke_1",
            layerId = "layer_1",
            points = listOf(StrokePoint(50f, 100f), StrokePoint(150f, 100f)),
            color = 0xFFFFFFFF,
            width = 2f,
            bounds = CanvasRect(50f, 100f, 150f, 100f)
        )
        val doc = CanvasDocument(layers = listOf(layer), elements = listOf(stroke))

        // Rotate using handleId = 8
        val updatedDoc = controller.applyTransformDelta(
            handleId = 8,
            deltaScreenX = 50f,
            deltaScreenY = 0f,
            viewport = defaultViewport,
            selectedIds = setOf("stroke_1"),
            document = doc,
            historyManager = historyManager
        )

        val rotatedStroke = updatedDoc.elements.first() as StrokeElement
        // Center of stroke should remain near (100, 100)
        val cx = (rotatedStroke.points[0].x + rotatedStroke.points[1].x) / 2f
        val cy = (rotatedStroke.points[0].y + rotatedStroke.points[1].y) / 2f
        assertEquals(100f, cx, 0.1f)
        assertEquals(100f, cy, 0.1f)

        // Points should have rotated off the horizontal line
        assertTrue(rotatedStroke.points[0].y != 100f)
    }
}
