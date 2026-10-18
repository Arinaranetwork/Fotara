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
import com.arinara.fotara.canvas.model.ImageElement
import com.arinara.fotara.canvas.model.StrokeBlendMode
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokeToolType
import com.arinara.fotara.canvas.model.TextLayerElement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasEraserStrokeCarveTest {

    @Test
    fun testEraserDeletesImageAlongStrokeArea_RemovesImageEntity() {
        val layer = CanvasLayer(id = "layer_1", name = "Layer 1")
        val image = ImageElement(
            id = "img_1",
            layerId = "layer_1",
            assetId = "dummy_asset",
            x = 100f,
            y = 100f,
            width = 300f,
            height = 300f,
            bounds = CanvasRect(100f, 100f, 400f, 400f)
        )
        val initialDoc = CanvasDocument(
            id = 1L,
            layers = listOf(layer),
            elements = listOf(image)
        )
        val controller = CanvasToolController(
            toolState = CanvasToolState(activeTool = CanvasToolType.ERASER, eraserRadius = 20f)
        )
        val historyManager = CanvasHistoryManager()
        val viewport = ViewportState(scale = 1.0f)

        controller.startEraser(200f, 200f)

        // Drag across the center of the image
        val (previewDoc, dirtyBounds) = controller.sweepEraser(
            listOf(
                PointerPoint(x = 200f, y = 200f, pressure = 1.0f),
                PointerPoint(x = 250f, y = 250f, pressure = 1.0f)
            ),
            viewport = viewport,
            document = initialDoc
        )

        // Live preview must preserve the image and provide live carve stroke
        assertNotNull(dirtyBounds)
        assertTrue("Live preview must preserve image", previewDoc.elements.any { it.id == "img_1" })
        assertTrue("Live carve overlay present", previewDoc.elements.any { it.id == "__eraser_live_carve__" })

        // Finish the eraser sweep
        val (finalDoc, _) = controller.finishEraser(previewDoc, historyManager)

        // The image entity must be preserved and a DST_OUT carve stroke added along the path
        assertTrue("ImageElement must be preserved in document", finalDoc.elements.any { it.id == "img_1" })
        assertTrue("Carve stroke added along path", finalDoc.elements.any { it is StrokeElement && it.blendMode == StrokeBlendMode.DST_OUT })

        // Undo must remove carve stroke and restore original uncarved document
        val undoneDoc = historyManager.undo(finalDoc)
        assertNotNull(undoneDoc)
        assertEquals(1, undoneDoc?.elements?.size)
        assertEquals("img_1", undoneDoc?.elements?.first()?.id)
        assertTrue("No carve stroke after undo", undoneDoc?.elements?.none { it is StrokeElement } == true)
    }

    @Test
    fun testEraserDeletesTextAlongStrokeArea_RemovesTextEntity() {
        val layer = CanvasLayer(id = "layer_1", name = "Layer 1")
        val text = TextLayerElement(
            id = "txt_1",
            layerId = "layer_1",
            text = "Fotara Note Header",
            x = 50f,
            y = 50f,
            width = 200f,
            height = 80f,
            bounds = CanvasRect(50f, 50f, 250f, 130f)
        )
        val initialDoc = CanvasDocument(
            id = 1L,
            layers = listOf(layer),
            elements = listOf(text)
        )
        val controller = CanvasToolController(
            toolState = CanvasToolState(activeTool = CanvasToolType.ERASER, eraserRadius = 15f)
        )
        val historyManager = CanvasHistoryManager()
        val viewport = ViewportState(scale = 1.0f)

        controller.startEraser(100f, 80f)
        val (previewDoc, _) = controller.sweepEraser(
            listOf(
                PointerPoint(x = 100f, y = 80f, pressure = 1.0f),
                PointerPoint(x = 120f, y = 90f, pressure = 1.0f)
            ),
            viewport = viewport,
            document = initialDoc
        )
        val (finalDoc, _) = controller.finishEraser(previewDoc, historyManager)

        // Text element must be preserved in document with carve stroke
        assertTrue("TextLayerElement must be preserved in document", finalDoc.elements.any { it.id == "txt_1" })
        assertTrue("Carve stroke added along path", finalDoc.elements.any { it is StrokeElement && it.blendMode == StrokeBlendMode.DST_OUT })

        // Undo must restore text element
        val undoneDoc = historyManager.undo(finalDoc)
        assertNotNull(undoneDoc)
        assertEquals(1, undoneDoc?.elements?.size)
        assertEquals("txt_1", undoneDoc?.elements?.first()?.id)
        assertTrue("No carve stroke after undo", undoneDoc?.elements?.none { it is StrokeElement } == true)
    }
}
