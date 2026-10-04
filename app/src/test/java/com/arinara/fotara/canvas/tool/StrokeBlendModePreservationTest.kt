// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.tool

import com.arinara.fotara.canvas.engine.CanvasHistoryManager
import com.arinara.fotara.canvas.engine.CanvasRect
import com.arinara.fotara.canvas.engine.CanvasSelectionEngine
import com.arinara.fotara.canvas.engine.ViewportState
import com.arinara.fotara.canvas.model.CanvasDocument
import com.arinara.fotara.canvas.model.CanvasLayer
import com.arinara.fotara.canvas.model.CanvasSelection
import com.arinara.fotara.canvas.model.SelectedElementReference
import com.arinara.fotara.canvas.model.StrokeBlendMode
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType
import com.arinara.fotara.canvas.gesture.PointerPoint
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class StrokeBlendModePreservationTest {

    private val defaultViewport = ViewportState(scale = 1.0f, translateX = 0f, translateY = 0f)

    @Test
    fun testLassoSplit_PreservesBlendModeOnAllPieces() {
        val stroke = StrokeElement(
            id = "stroke_blend_1",
            layerId = "layer_1",
            points = listOf(
                StrokePoint(10f, 10f),
                StrokePoint(50f, 50f),
                StrokePoint(100f, 100f),
                StrokePoint(150f, 150f),
                StrokePoint(200f, 200f)
            ),
            color = 0xFF336699,
            width = 16f,
            toolType = StrokeToolType.HIGHLIGHTER,
            blendMode = StrokeBlendMode.MULTIPLY,
            bounds = CanvasRect(10f, 10f, 200f, 200f)
        )

        // Lasso polygon selecting the middle part (40..120)
        val lassoPolygon = listOf(
            Pair(40f, 40f),
            Pair(120f, 40f),
            Pair(120f, 120f),
            Pair(40f, 120f)
        )

        val splitResult = CanvasSelectionEngine.sliceStrokeWithLasso(stroke, lassoPolygon, 1.0f, 1.0f)
        assertNotNull(splitResult)
        assertTrue(splitResult is SelectedElementReference.PartialStroke)
        val partialRef = splitResult as SelectedElementReference.PartialStroke
        assertTrue(partialRef.insideSegments.isNotEmpty())

        val (remnantElements, selectedElements) = CanvasSelectionEngine.materializePartialSplit(
            stroke,
            partialRef,
            1.0f,
            1.0f
        )

        // Verify selected pieces preserve MULTIPLY
        for (el in selectedElements) {
            val s = el as StrokeElement
            assertEquals(StrokeBlendMode.MULTIPLY, s.blendMode)
            assertEquals(StrokeToolType.HIGHLIGHTER, s.toolType)
        }

        // Verify remnant pieces preserve MULTIPLY
        for (el in remnantElements) {
            val s = el as StrokeElement
            assertEquals(StrokeBlendMode.MULTIPLY, s.blendMode)
            assertEquals(StrokeToolType.HIGHLIGHTER, s.toolType)
        }
    }

    @Test
    fun testEraserSweep_PreservesBlendModeOnRemnantPieces() {
        val controller = CanvasToolController()
        val historyManager = CanvasHistoryManager()
        val layer = CanvasLayer(id = "layer_1", name = "Layer 1")

        // Long stroke from x=0 to x=200 with DARKEN blend mode
        val points = (0..20).map { i -> StrokePoint(i * 10f, 100f) }
        val stroke = StrokeElement(
            id = "darken_stroke",
            layerId = "layer_1",
            points = points,
            color = 0xFF556677,
            width = 10f,
            toolType = StrokeToolType.HIGHLIGHTER,
            blendMode = StrokeBlendMode.DARKEN,
            bounds = CanvasRect(0f, 95f, 200f, 105f)
        )
        val doc = CanvasDocument(layers = listOf(layer), elements = listOf(stroke))

        // Sweep eraser right through the middle at (100, 100) with radius 20
        controller.toolState = controller.toolState.copy(eraserRadius = 20f)
        controller.startEraser(100f, 100f)
        val (sweepDoc, _) = controller.sweepEraser(
            points = listOf(PointerPoint(100f, 100f)),
            viewport = defaultViewport,
            document = doc
        )
        val (updatedDoc, _) = controller.finishEraser(sweepDoc, historyManager)

        // Should result in split pieces
        val strokes = updatedDoc.elements.filterIsInstance<StrokeElement>()
        assertTrue(strokes.isNotEmpty())
        for (s in strokes) {
            assertEquals("Eraser must preserve DARKEN blend mode", StrokeBlendMode.DARKEN, s.blendMode)
            assertEquals(StrokeToolType.HIGHLIGHTER, s.toolType)
        }
    }

    @Test
    fun testDuplicateSelection_PreservesBlendMode() {
        val controller = CanvasToolController()
        val historyManager = CanvasHistoryManager()
        val layer = CanvasLayer(id = "layer_1", name = "Layer 1")
        val stroke = StrokeElement(
            id = "screen_stroke",
            layerId = "layer_1",
            points = listOf(StrokePoint(50f, 50f), StrokePoint(150f, 150f)),
            color = 0xFFAABBCC,
            width = 14f,
            toolType = StrokeToolType.HIGHLIGHTER,
            blendMode = StrokeBlendMode.SCREEN,
            bounds = CanvasRect(50f, 50f, 150f, 150f)
        )
        val doc = CanvasDocument(layers = listOf(layer), elements = listOf(stroke))

        controller.selection = CanvasSelection(
            references = mapOf(stroke.id to SelectedElementReference.Whole(stroke.id)),
            bounds = stroke.bounds
        )

        val (updatedDoc, _) = controller.duplicateSelection(doc, historyManager)
        assertEquals(2, updatedDoc.elements.size)

        val duplicated = updatedDoc.elements.find { it.id != stroke.id } as StrokeElement
        assertEquals(StrokeBlendMode.SCREEN, duplicated.blendMode)
        assertEquals(StrokeToolType.HIGHLIGHTER, duplicated.toolType)
    }

    @Test
    fun testMoveSelectionToLayer_PreservesBlendMode() {
        val controller = CanvasToolController()
        val historyManager = CanvasHistoryManager()
        val layer1 = CanvasLayer(id = "layer_1", name = "Layer 1")
        val layer2 = CanvasLayer(id = "layer_2", name = "Layer 2")
        val stroke = StrokeElement(
            id = "multiply_stroke",
            layerId = "layer_1",
            points = listOf(StrokePoint(10f, 10f), StrokePoint(20f, 20f)),
            color = 0xFF112233,
            width = 8f,
            toolType = StrokeToolType.HIGHLIGHTER,
            blendMode = StrokeBlendMode.MULTIPLY,
            bounds = CanvasRect(10f, 10f, 20f, 20f)
        )
        val doc = CanvasDocument(layers = listOf(layer1, layer2), elements = listOf(stroke))

        controller.selection = CanvasSelection(
            references = mapOf(stroke.id to SelectedElementReference.Whole(stroke.id)),
            bounds = stroke.bounds
        )

        val (updatedDoc, _) = controller.moveSelectionToLayer("layer_2", doc, historyManager)
        val movedStroke = updatedDoc.elements.first() as StrokeElement
        assertEquals("layer_2", movedStroke.layerId)
        assertEquals(StrokeBlendMode.MULTIPLY, movedStroke.blendMode)
    }

    @Test
    fun testTransformCommitAndUndoRedo_PreservesBlendMode() {
        val controller = CanvasToolController()
        val historyManager = CanvasHistoryManager()
        val layer = CanvasLayer(id = "layer_1", name = "Layer 1")
        val stroke = StrokeElement(
            id = "darken_stroke",
            layerId = "layer_1",
            points = listOf(StrokePoint(100f, 100f), StrokePoint(200f, 200f)),
            color = 0xFF667788,
            width = 12f,
            toolType = StrokeToolType.HIGHLIGHTER,
            blendMode = StrokeBlendMode.DARKEN,
            bounds = CanvasRect(100f, 100f, 200f, 200f)
        )
        val doc = CanvasDocument(layers = listOf(layer), elements = listOf(stroke))

        controller.selection = CanvasSelection(
            references = mapOf(stroke.id to SelectedElementReference.Whole(stroke.id)),
            bounds = stroke.bounds
        )

        // Transform body drag
        controller.startTransformGesture(0f, 0f, defaultViewport, doc)
        controller.updateTransformPreview(-1, 20f, 20f, defaultViewport, doc)
        val (transformedDoc, _) = controller.commitTransform(
            handleId = -1,
            totalDeltaX = 20f,
            totalDeltaY = 20f,
            viewport = defaultViewport,
            document = doc,
            historyManager = historyManager
        )

        val transformedStroke = transformedDoc.elements.first() as StrokeElement
        assertEquals(StrokeBlendMode.DARKEN, transformedStroke.blendMode)

        // Undo
        val undoneDoc = historyManager.undo(transformedDoc)
        assertNotNull(undoneDoc)
        val restoredStroke = undoneDoc!!.elements.first() as StrokeElement
        assertEquals(StrokeBlendMode.DARKEN, restoredStroke.blendMode)

        // Redo
        val redoneDoc = historyManager.redo(undoneDoc)
        assertNotNull(redoneDoc)
        val reTransformedStroke = redoneDoc!!.elements.first() as StrokeElement
        assertEquals(StrokeBlendMode.DARKEN, reTransformedStroke.blendMode)
    }
}
