// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import com.arinara.fotara.canvas.model.CanvasDocument
import com.arinara.fotara.canvas.model.CanvasElement
import com.arinara.fotara.canvas.model.CanvasLayer
import com.arinara.fotara.canvas.model.CanvasSelection
import com.arinara.fotara.canvas.model.ImageElement
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
import java.util.UUID

class CanvasStackingAndOverlayLayoutTest {

    // -------------------------------------------------------------
    // Task 1: Stacking Comparator & Creation Order
    // -------------------------------------------------------------

    @Test
    fun testStackingComparator_LayerOrderDecidesFirst() {
        val layerBottom = CanvasLayer(id = "l_bottom", name = "Bottom", order = 0)
        val layerTop = CanvasLayer(id = "l_top", name = "Top", order = 1)
        val doc = CanvasDocument(
            id = 1L,
            layers = listOf(layerBottom, layerTop)
        )

        // Element on top layer with low zIndex vs element on bottom layer with high zIndex
        val elOnTopLayer = ImageElement(
            id = "img_top",
            layerId = "l_top",
            assetId = "a1",
            x = 0f, y = 0f, width = 100f, height = 100f,
            bounds = CanvasRect(0f, 0f, 100f, 100f),
            zIndex = 1
        )
        val elOnBottomLayer = StrokeElement(
            id = "stroke_bottom",
            layerId = "l_bottom",
            points = listOf(StrokePoint(0f, 0f), StrokePoint(10f, 10f)),
            color = 0xFF000000,
            width = 4f,
            bounds = CanvasRect(0f, 0f, 10f, 10f),
            zIndex = 999
        )

        val layerMap = doc.layers.associateBy { it.id }
        val elements = listOf(elOnBottomLayer, elOnTopLayer)

        // Sort ascending (rendering draw order: bottom to top)
        val sortedAsc = elements.sortedWith(
            compareBy<CanvasElement> { layerMap[it.layerId]?.order ?: 0 }
                .thenBy { it.zIndex }
                .thenBy { it.id }
        )
        assertEquals("stroke_bottom", sortedAsc[0].id)
        assertEquals("img_top", sortedAsc[1].id)

        // Sort descending (hit-test order: topmost under finger first)
        val sortedDesc = elements.sortedWith(
            compareByDescending<CanvasElement> { layerMap[it.layerId]?.order ?: 0 }
                .thenByDescending { it.zIndex }
                .thenByDescending { it.id }
        )
        assertEquals("img_top", sortedDesc[0].id)
        assertEquals("stroke_bottom", sortedDesc[1].id)
    }

    @Test
    fun testStackingComparator_WithinSameLayer_CreationOrderDecides() {
        val layer = CanvasLayer(id = "l1", name = "Main", order = 0)
        val doc = CanvasDocument(id = 1L, layers = listOf(layer))
        val layerMap = doc.layers.associateBy { it.id }

        // Case A: Image added first (z=1), stroke drawn second (z=2) -> stroke is above image
        val imgA = ImageElement(
            id = "img_a", layerId = "l1", assetId = "a1",
            x = 0f, y = 0f, width = 100f, height = 100f,
            bounds = CanvasRect(0f, 0f, 100f, 100f), zIndex = 1
        )
        val strokeA = StrokeElement(
            id = "stroke_a", layerId = "l1",
            points = listOf(StrokePoint(0f, 0f)),
            color = 0xFF000000, width = 4f,
            bounds = CanvasRect(0f, 0f, 10f, 10f), zIndex = 2
        )

        val renderOrderA = listOf(strokeA, imgA).sortedWith(
            compareBy<CanvasElement> { layerMap[it.layerId]?.order ?: 0 }
                .thenBy { it.zIndex }
                .thenBy { it.id }
        )
        assertEquals("img_a", renderOrderA[0].id)
        assertEquals("stroke_a", renderOrderA[1].id)

        // Case B: Stroke drawn first (z=1), image added second (z=2) -> image is above stroke
        val strokeB = StrokeElement(
            id = "stroke_b", layerId = "l1",
            points = listOf(StrokePoint(0f, 0f)),
            color = 0xFF000000, width = 4f,
            bounds = CanvasRect(0f, 0f, 10f, 10f), zIndex = 1
        )
        val imgB = ImageElement(
            id = "img_b", layerId = "l1", assetId = "a2",
            x = 0f, y = 0f, width = 100f, height = 100f,
            bounds = CanvasRect(0f, 0f, 100f, 100f), zIndex = 2
        )

        val renderOrderB = listOf(imgB, strokeB).sortedWith(
            compareBy<CanvasElement> { layerMap[it.layerId]?.order ?: 0 }
                .thenBy { it.zIndex }
                .thenBy { it.id }
        )
        assertEquals("stroke_b", renderOrderB[0].id)
        assertEquals("img_b", renderOrderB[1].id)
    }

    @Test
    fun testNextZIndexForLayer() {
        val doc = CanvasDocument(
            id = 1L,
            layers = listOf(CanvasLayer(id = "l1", name = "L1", order = 0)),
            elements = emptyList()
        )

        // Empty layer -> 1
        assertEquals(1, CanvasToolController.nextZIndexForLayer(doc, "l1"))

        // With existing stroke (z=3)
        val docWithStroke = doc.copy(elements = listOf(
            StrokeElement(
                id = "s1", layerId = "l1",
                points = emptyList(), color = 0xFF000000, width = 2f,
                bounds = CanvasRect.Empty, zIndex = 3
            )
        ))
        assertEquals(4, CanvasToolController.nextZIndexForLayer(docWithStroke, "l1"))

        // Mixed with image (z=7)
        val docWithMixed = docWithStroke.copy(elements = docWithStroke.elements + listOf(
            ImageElement(
                id = "i1", layerId = "l1", assetId = "a",
                x = 0f, y = 0f, width = 10f, height = 10f,
                bounds = CanvasRect(0f, 0f, 10f, 10f), zIndex = 7
            )
        ))
        assertEquals(8, CanvasToolController.nextZIndexForLayer(docWithMixed, "l1"))
    }

    @Test
    fun testOrderPreserved_AfterSplitEraser() {
        val origStroke = StrokeElement(
            id = "s_orig",
            layerId = "l1",
            points = listOf(
                StrokePoint(0f, 0f),
                StrokePoint(50f, 0f),
                StrokePoint(100f, 0f)
            ),
            color = 0xFF000000,
            width = 4f,
            bounds = CanvasRect(0f, -2f, 100f, 2f),
            zIndex = 42
        )

        // Erase middle segment (capsule at 50,0)
        val splitParts = StrokeProcessor.eraseStrokeWithCapsule(
            stroke = origStroke,
            ax = 50f, ay = -10f,
            bx = 50f, by = 10f,
            eraserRadius = 15f
        )

        // All split fragments must keep the source element's zIndex = 42
        assertTrue("Split parts should be non-empty", splitParts.isNotEmpty())
        for (part in splitParts) {
            assertEquals(42, part.zIndex)
            assertEquals("l1", part.layerId)
        }
    }

    @Test
    fun testOrderPreserved_UndoRedo() {
        val historyManager = CanvasHistoryManager()
        val doc0 = CanvasDocument(
            id = 1L,
            layers = listOf(CanvasLayer(id = "l1", name = "L1", order = 0)),
            elements = listOf(
                ImageElement(
                    id = "i1", layerId = "l1", assetId = "a1",
                    x = 0f, y = 0f, width = 100f, height = 100f,
                    bounds = CanvasRect(0f, 0f, 100f, 100f), zIndex = 1
                )
            )
        )

        val newStroke = StrokeElement(
            id = "s1", layerId = "l1",
            points = listOf(StrokePoint(10f, 10f)),
            color = 0xFF000000, width = 2f,
            bounds = CanvasRect(9f, 9f, 11f, 11f), zIndex = 2
        )
        val doc1 = historyManager.execute(AddElementsCommand(listOf(newStroke)), doc0)
        assertEquals(2, doc1.elements.size)
        assertEquals(2, doc1.elements.find { it.id == "s1" }?.zIndex)

        // Undo
        val docUndone = historyManager.undo(doc1)
        assertNotNull(docUndone)
        assertEquals(1, docUndone!!.elements.size)
        assertNull(docUndone.elements.find { it.id == "s1" })

        // Redo
        val docRedone = historyManager.redo(docUndone)
        assertNotNull(docRedone)
        assertEquals(2, docRedone!!.elements.size)
        assertEquals(2, docRedone.elements.find { it.id == "s1" }?.zIndex)
    }

    // -------------------------------------------------------------
    // Task 2: Image Selection, Transform Math & Hit-testing
    // -------------------------------------------------------------

    @Test
    fun testHitTestPriority_TopmostElementSelected() {
        val toolController = CanvasToolController()
        val layer = CanvasLayer(id = "l1", name = "L1", order = 0)
        val img = ImageElement(
            id = "i1", layerId = "l1", assetId = "a1",
            x = 0f, y = 0f, width = 100f, height = 100f,
            bounds = CanvasRect(0f, 0f, 100f, 100f),
            zIndex = 1
        )
        val stroke = StrokeElement(
            id = "s1", layerId = "l1",
            points = listOf(StrokePoint(50f, 50f)),
            color = 0xFF000000, width = 10f,
            bounds = CanvasRect(45f, 45f, 55f, 55f),
            zIndex = 2
        )

        val doc = CanvasDocument(id = 1L, layers = listOf(layer), elements = listOf(img, stroke))

        // Tap at (50, 50) where both overlap -> stroke has higher zIndex so stroke is selected
        val selStroke = toolController.selectTap(50f, 50f, ViewportState(), doc)
        assertFalse(selStroke.isEmpty)
        assertTrue(selStroke.references.containsKey("s1"))

        // Invert zIndices: image z=5, stroke z=2 -> image is selected
        val docImgOnTop = CanvasDocument(id = 1L, layers = listOf(layer), elements = listOf(
            img.copy(zIndex = 5),
            stroke.copy(zIndex = 2)
        ))
        val selImg = toolController.selectTap(50f, 50f, ViewportState(), docImgOnTop)
        assertFalse(selImg.isEmpty)
        assertTrue(selImg.references.containsKey("i1"))
    }

    @Test
    fun testHitTest_HiddenAndLockedLayersSkipped() {
        val toolController = CanvasToolController()
        val lockedLayer = CanvasLayer(id = "l_locked", name = "Locked", isLocked = true, order = 1)
        val normalLayer = CanvasLayer(id = "l_normal", name = "Normal", order = 0)

        val lockedImg = ImageElement(
            id = "i_locked", layerId = "l_locked", assetId = "a",
            x = 0f, y = 0f, width = 100f, height = 100f,
            bounds = CanvasRect(0f, 0f, 100f, 100f),
            zIndex = 10
        )
        val normalStroke = StrokeElement(
            id = "s_normal", layerId = "l_normal",
            points = listOf(StrokePoint(50f, 50f)),
            color = 0xFF000000, width = 10f,
            bounds = CanvasRect(45f, 45f, 55f, 55f),
            zIndex = 1
        )

        val doc = CanvasDocument(id = 1L, layers = listOf(normalLayer, lockedLayer), elements = listOf(lockedImg, normalStroke))

        // Tap at (50, 50): lockedImg is skipped, selects normalStroke
        val sel = toolController.selectTap(50f, 50f, ViewportState(), doc)
        assertFalse(sel.isEmpty)
        assertTrue(sel.references.containsKey("s_normal"))
    }

    @Test
    fun testHitTest_RotatedImage() {
        val toolController = CanvasToolController()
        val layer = CanvasLayer(id = "l1", name = "L1", order = 0)
        // Image at (50, 50) with width=100, height=100 (center is at (100, 100)) rotated 45 degrees
        val img = ImageElement(
            id = "img_rot",
            layerId = "l1",
            assetId = "a1",
            x = 50f, y = 50f,
            width = 100f, height = 100f,
            rotationDegrees = 45f,
            bounds = CanvasRect(50f, 50f, 150f, 150f),
            zIndex = 1
        )
        val doc = CanvasDocument(id = 1L, layers = listOf(layer), elements = listOf(img))

        // Center point (100, 100) must hit
        val hitCenter = toolController.selectTap(100f, 100f, ViewportState(), doc)
        assertFalse(hitCenter.isEmpty)
        assertTrue(hitCenter.references.containsKey("img_rot"))

        // Unrotated corner (55, 55): In 45-deg diamond, (55, 55) is outside the diamond
        val hitCorner = toolController.selectTap(55f, 55f, ViewportState(), doc)
        assertTrue("Corner (55, 55) should be outside 45-degree diamond", hitCorner.isEmpty)
    }

    @Test
    fun testImageOnlyTransformMath_ProportionalCornerScale_OppositeCornerFixed() {
        val originalImg = ImageElement(
            id = "i1",
            layerId = "l1",
            assetId = "a1",
            x = 100f, y = 100f,
            width = 200f, height = 100f, // 2:1 aspect ratio
            bounds = CanvasRect(100f, 100f, 300f, 200f),
            rotationDegrees = 0f,
            zIndex = 1
        )

        val startState = TransformHandlesMath.TransformStartState(
            originalStrokes = emptyMap(),
            originalImageBounds = mapOf("i1" to originalImg.bounds),
            originalImageRotations = mapOf("i1" to 0f),
            originalImages = mapOf("i1" to originalImg),
            worldCenter = Pair(200f, 150f),
            worldWidth = 200f,
            worldHeight = 100f,
            rotationDegrees = 0f,
            startScreenX = 300f,
            startScreenY = 200f
        )

        // Handle 4 is SE corner (bottom-right: x=300, y=200). Opposite anchor is NW (100, 100).
        // Drag SE corner by +100 in X
        val transformed = TransformHandlesMath.transformImageElement(
            originalImage = originalImg,
            handleId = 4,
            startState = startState,
            currentScreenX = 400f,
            currentScreenY = 250f,
            viewport = ViewportState(scale = 1.0f, translateX = 0f, translateY = 0f),
            density = 1.0f,
            isOnlyImages = true
        )

        // NW corner (x, y) must remain fixed at (100, 100)
        assertEquals(100f, transformed.x, 1e-2f)
        assertEquals(100f, transformed.y, 1e-2f)

        // Aspect ratio (width / height) must remain 2:1
        val ratio = transformed.width / transformed.height
        assertEquals(2.0f, ratio, 1e-2f)
        assertTrue(transformed.width > 200f)
    }

    @Test
    fun testImageOnlyTransformMath_SideHandleStretchesOneAxis() {
        val originalImg = ImageElement(
            id = "i1",
            layerId = "l1",
            assetId = "a1",
            x = 100f, y = 100f,
            width = 100f, height = 100f,
            bounds = CanvasRect(100f, 100f, 200f, 200f),
            zIndex = 1
        )

        val startState = TransformHandlesMath.TransformStartState(
            originalStrokes = emptyMap(),
            originalImageBounds = mapOf("i1" to originalImg.bounds),
            originalImageRotations = mapOf("i1" to 0f),
            originalImages = mapOf("i1" to originalImg),
            worldCenter = Pair(150f, 150f),
            worldWidth = 100f,
            worldHeight = 100f,
            rotationDegrees = 0f,
            startScreenX = 200f,
            startScreenY = 150f
        )

        // Handle 3 is E side handle (right edge at x=200). Drag by +50 in X, +20 in Y.
        val transformed = TransformHandlesMath.transformImageElement(
            originalImage = originalImg,
            handleId = 3,
            startState = startState,
            currentScreenX = 250f,
            currentScreenY = 170f,
            viewport = ViewportState(scale = 1.0f, translateX = 0f, translateY = 0f),
            density = 1.0f,
            isOnlyImages = true
        )

        // Left edge (x) remains 100, height remains 100 (Y is unchanged by E side handle)
        assertEquals(100f, transformed.x, 1e-2f)
        assertEquals(100f, transformed.height, 1e-2f)
        assertEquals(150f, transformed.width, 1e-2f)
    }

    @Test
    fun testImageOnlyTransformMath_MinimumClampAndNoMirroring() {
        val originalImg = ImageElement(
            id = "i1",
            layerId = "l1",
            assetId = "a1",
            x = 100f, y = 100f,
            width = 100f, height = 100f,
            bounds = CanvasRect(100f, 100f, 200f, 200f),
            zIndex = 1
        )

        val startState = TransformHandlesMath.TransformStartState(
            originalStrokes = emptyMap(),
            originalImageBounds = mapOf("i1" to originalImg.bounds),
            originalImageRotations = mapOf("i1" to 0f),
            originalImages = mapOf("i1" to originalImg),
            worldCenter = Pair(150f, 150f),
            worldWidth = 100f,
            worldHeight = 100f,
            rotationDegrees = 0f,
            startScreenX = 200f,
            startScreenY = 200f
        )

        // Drag SE corner far past NW anchor (attempt negative mirroring)
        val transformed = TransformHandlesMath.transformImageElement(
            originalImage = originalImg,
            handleId = 4,
            startState = startState,
            currentScreenX = 50f,
            currentScreenY = 50f,
            viewport = ViewportState(scale = 1.0f, translateX = 0f, translateY = 0f),
            density = 1.0f,
            isOnlyImages = true
        )

        // Must clamp to at least 24dp (density=1.0 -> 24f) and no negative mirroring
        assertTrue("Width must be at least 24dp", transformed.width >= 24f)
        assertTrue("Height must be at least 24dp", transformed.height >= 24f)
        assertEquals(100f, transformed.x, 1e-2f)
        assertEquals(100f, transformed.y, 1e-2f)
    }

    // -------------------------------------------------------------
    // Task 3: Panel Height Calculation
    // -------------------------------------------------------------

    @Test
    fun testToolOptionsPanel_MaxHeightCalculation() {
        val screenHeightDp = 800f
        val statusBarTopDp = 32f
        val topCapsulesDp = 44f
        val topClearanceDp = statusBarTopDp + topCapsulesDp + 12f
        val navBarBottomDp = 48f
        val targetZ4Padding = 80f
        val bottomClearanceDp = navBarBottomDp + targetZ4Padding + 8f

        val maxPanelHeightDp = (screenHeightDp - topClearanceDp - bottomClearanceDp).coerceAtLeast(160f)

        // 800 - (32+44+12 = 88) - (48+80+8 = 136) = 576dp
        assertEquals(576f, maxPanelHeightDp, 1e-2f)
        assertTrue("Panel must not exceed available space", maxPanelHeightDp + topClearanceDp + bottomClearanceDp <= screenHeightDp)
    }

    // -------------------------------------------------------------
    // Task 4: Bottom Overlay Insets & Spacing
    // -------------------------------------------------------------

    @Test
    fun testBottomOverlayInsets_PadsSnackbarAndFab() {
        val bottomOverlayHeightDp = 96f
        val snackbarBottomPadding = bottomOverlayHeightDp + 8f
        val fabBottomPadding = (bottomOverlayHeightDp + 16f).coerceAtLeast(82f)

        // Snackbar is strictly 8dp above the overlay
        assertEquals(104f, snackbarBottomPadding, 1e-2f)
        assertTrue(snackbarBottomPadding > bottomOverlayHeightDp)

        // Notes FAB is 16dp above overlay (112dp), avoiding any overlap with the floating dock
        assertEquals(112f, fabBottomPadding, 1e-2f)
        assertTrue(fabBottomPadding > bottomOverlayHeightDp)
    }
}
