// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import com.arinara.fotara.canvas.engine.CanvasHistoryManager
import com.arinara.fotara.canvas.model.CanvasDocument
import com.arinara.fotara.canvas.model.CanvasLayer
import com.arinara.fotara.canvas.model.CanvasSelection
import com.arinara.fotara.canvas.model.SelectedElementReference
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.tool.CanvasToolController
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot

class TransformHandlesMathTest {

    private val box = CanvasRect(100f, 100f, 300f, 300f) // 200x200 box, center (200, 200)
    private val defaultViewport = ViewportState(scale = 1.0f, translateX = 0f, translateY = 0f)

    @Test
    fun testHitTest_44dpHitTarget_IdentifiesHandlesCorrectly() {
        val density = 1.0f

        // Top-left corner is at (100, 100)
        val hitTL = TransformHandlesMath.hitTest(102f, 98f, box, 0f, defaultViewport, density)
        assertEquals(0, hitTL)

        // Bottom-right corner is at (300, 300)
        val hitBR = TransformHandlesMath.hitTest(298f, 302f, box, 0f, defaultViewport, density)
        assertEquals(4, hitBR)

        // Rotation stem is 28dp above top-center (200, 100) -> around (200, 72)
        val hitRot = TransformHandlesMath.hitTest(200f, 72f, box, 0f, defaultViewport, density)
        assertEquals(8, hitRot)
    }

    @Test
    fun testHitTest_SuppressesSideHandlesWhenBoxTooSmall() {
        // Small box 50x50, smaller than 2 * 44dp (88dp threshold)
        val smallBox = CanvasRect(100f, 100f, 150f, 150f)
        val density = 1.0f

        // Top-center side handle (125, 100) should be suppressed
        val hitSide = TransformHandlesMath.hitTest(125f, 100f, smallBox, 0f, defaultViewport, density)
        // Should NOT return 1 (N midpoint handle)
        assertTrue(hitSide != 1)
    }

    @Test
    fun testHitTestPriority_HandleThenBodyThenOutside() {
        val density = 1.0f

        // 1. Point right on corner -> Handle 0
        val handle = TransformHandlesMath.hitTest(100f, 100f, box, 0f, defaultViewport, density)
        assertEquals(0, handle)

        // 2. Point inside box (not near handles) -> Body (-1)
        val inside = TransformHandlesMath.hitTest(200f, 200f, box, 0f, defaultViewport, density)
        assertEquals(-1, inside)

        // 3. Point far outside box -> Outside (null, which triggers lasso start)
        val outside = TransformHandlesMath.hitTest(500f, 500f, box, 0f, defaultViewport, density)
        assertNull(outside)
    }

    @Test
    fun testCornerStretch_OppositeAnchorStaysFixed() {
        val startPts = listOf(
            StrokePoint(100f, 100f), // NW anchor point
            StrokePoint(300f, 300f)  // SE handle point
        )
        val startState = TransformHandlesMath.TransformStartState(
            originalStrokes = mapOf("s1" to startPts),
            originalImageBounds = emptyMap(),
            originalImageRotations = emptyMap(),
            worldCenter = Pair(200f, 200f),
            worldWidth = 200f,
            worldHeight = 200f,
            rotationDegrees = 0f,
            startScreenX = 300f,
            startScreenY = 300f
        )

        // Drag SE corner (handle 4) by dx=+50, dy=+50
        val transformed = TransformHandlesMath.transformStrokePoints(
            originalPoints = startPts,
            handleId = 4,
            startState = startState,
            currentScreenX = 350f,
            currentScreenY = 350f,
            viewport = defaultViewport,
            density = 1.0f
        )

        // Opposite NW anchor (100, 100) must stay strictly fixed!
        assertEquals(100f, transformed[0].x, 0.01f)
        assertEquals(100f, transformed[0].y, 0.01f)

        // SE corner moved to (350, 350)
        assertEquals(350f, transformed[1].x, 0.01f)
        assertEquals(350f, transformed[1].y, 0.01f)
    }

    @Test
    fun testSideStretch_StretchesAlongOneAxisOnlyWithOppositeSideFixed() {
        val startPts = listOf(
            StrokePoint(100f, 200f), // W midpoint
            StrokePoint(300f, 200f)  // E midpoint
        )
        val startState = TransformHandlesMath.TransformStartState(
            originalStrokes = mapOf("s1" to startPts),
            originalImageBounds = emptyMap(),
            originalImageRotations = emptyMap(),
            worldCenter = Pair(200f, 200f),
            worldWidth = 200f,
            worldHeight = 200f,
            rotationDegrees = 0f,
            startScreenX = 300f,
            startScreenY = 200f
        )

        // Drag E side (handle 3) by dx=+40, dy=+30 (dy must be ignored)
        val transformed = TransformHandlesMath.transformStrokePoints(
            originalPoints = startPts,
            handleId = 3,
            startState = startState,
            currentScreenX = 340f,
            currentScreenY = 230f,
            viewport = defaultViewport,
            density = 1.0f
        )

        // Opposite W side (x=100) must stay fixed
        assertEquals(100f, transformed[0].x, 0.01f)
        assertEquals(200f, transformed[0].y, 0.01f) // height unchanged

        // E side stretched to 340
        assertEquals(340f, transformed[1].x, 0.01f)
        assertEquals(200f, transformed[1].y, 0.01f) // height unchanged
    }

    @Test
    fun testRotation_RotatesAroundBoxCentroid() {
        val startPts = listOf(
            StrokePoint(150f, 200f),
            StrokePoint(250f, 200f)
        )
        val startState = TransformHandlesMath.TransformStartState(
            originalStrokes = mapOf("s1" to startPts),
            originalImageBounds = emptyMap(),
            originalImageRotations = emptyMap(),
            worldCenter = Pair(200f, 200f),
            worldWidth = 200f,
            worldHeight = 200f,
            rotationDegrees = 0f,
            startScreenX = 200f,
            startScreenY = 72f
        )

        // Rotate using handle 8
        val transformed = TransformHandlesMath.transformStrokePoints(
            originalPoints = startPts,
            handleId = 8,
            startState = startState,
            currentScreenX = 250f,
            currentScreenY = 72f,
            viewport = defaultViewport,
            density = 1.0f
        )

        // Center must remain (200, 200)
        val cx = (transformed[0].x + transformed[1].x) / 2f
        val cy = (transformed[0].y + transformed[1].y) / 2f
        assertEquals(200f, cx, 0.1f)
        assertEquals(200f, cy, 0.1f)

        // Radial distance from center must remain 50
        val dist = hypot(transformed[0].x - 200f, transformed[0].y - 200f)
        assertEquals(50f, dist, 0.1f)
    }

    @Test
    fun testTransformMath_AllCoordinatesFinite_NoNaNOrInfinity() {
        val pts = listOf(
            StrokePoint(150f, 150f),
            StrokePoint(250f, 250f)
        )
        val startState = TransformHandlesMath.TransformStartState(
            originalStrokes = mapOf("s1" to pts),
            originalImageBounds = emptyMap(),
            originalImageRotations = emptyMap(),
            worldCenter = Pair(200f, 200f),
            worldWidth = 200f,
            worldHeight = 200f,
            rotationDegrees = 0f,
            startScreenX = 200f,
            startScreenY = 200f
        )

        val result = TransformHandlesMath.transformStrokePoints(
            originalPoints = pts,
            handleId = -1,
            startState = startState,
            currentScreenX = 250f,
            currentScreenY = 250f,
            viewport = defaultViewport,
            density = 1.0f
        )

        assertEquals(2, result.size)
        for (p in result) {
            assertFalse(p.x.isNaN())
            assertFalse(p.y.isNaN())
            assertFalse(p.x.isInfinite())
            assertFalse(p.y.isInfinite())
        }
    }

    @Test
    fun testHitTestHandles_AllEightHandlesAndStem() {
        val controller = CanvasToolController()
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
        val controller = CanvasToolController()
        val bounds = CanvasRect(100f, 100f, 200f, 200f)
        val viewport = ViewportState(scale = 2.0f, translateX = 50f, translateY = 50f)

        // Hit testing at screen coordinates (250, 250) should hit NW corner (0)
        assertEquals(0, controller.hitTestHandles(250f, 250f, bounds, viewport))
    }

    @Test
    fun testBodyDragTransform_TranslatesElementsAndSupportsUndoRedo() {
        val controller = CanvasToolController()
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
        val doc = CanvasDocument(layers = listOf(layer), elements = listOf(stroke))

        // Set selection
        controller.selection = CanvasSelection(
            references = mapOf(stroke.id to SelectedElementReference.Whole(stroke.id)),
            bounds = stroke.bounds
        )

        // Drag body (handleId = -1) by +30 screen X and +40 screen Y
        controller.startTransformGesture(0f, 0f, defaultViewport, doc)
        controller.updateTransformPreview(-1, 30f, 40f, defaultViewport, doc)
        val (updatedDoc, dirtyBounds) = controller.commitTransform(
            handleId = -1,
            totalDeltaX = 30f,
            totalDeltaY = 40f,
            viewport = defaultViewport,
            document = doc,
            historyManager = historyManager
        )
        assertNotNull(dirtyBounds)

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
        val controller = CanvasToolController()
        val historyManager = CanvasHistoryManager()
        val layer = CanvasLayer(id = "layer_1", name = "Layer 1")
        val stroke = StrokeElement(
            id = "stroke_1",
            layerId = "layer_1",
            points = listOf(StrokePoint(50f, 100f), StrokePoint(150f, 100f)),
            color = 0xFFFFFFFF,
            width = 2f,
            bounds = CanvasRect(50f, 100f, 150f, 100f)
        )
        val doc = CanvasDocument(layers = listOf(layer), elements = listOf(stroke))

        // Set selection
        controller.selection = CanvasSelection(
            references = mapOf(stroke.id to SelectedElementReference.Whole(stroke.id)),
            bounds = stroke.bounds
        )

        // Rotate using handleId = 8
        controller.startTransformGesture(100f, 72f, defaultViewport, doc)
        controller.updateTransformPreview(8, 150f, 72f, defaultViewport, doc)
        val (updatedDoc, _) = controller.commitTransform(
            handleId = 8,
            totalDeltaX = 50f,
            totalDeltaY = 0f,
            viewport = defaultViewport,
            document = doc,
            historyManager = historyManager
        )

        val rotatedStroke = updatedDoc.elements.first() as StrokeElement
        val cx = (rotatedStroke.points[0].x + rotatedStroke.points[1].x) / 2f
        val cy = (rotatedStroke.points[0].y + rotatedStroke.points[1].y) / 2f
        assertEquals(100f, cx, 0.1f)
        assertEquals(100f, cy, 0.1f)
        assertTrue(rotatedStroke.points[0].y != 100f)
    }
}
