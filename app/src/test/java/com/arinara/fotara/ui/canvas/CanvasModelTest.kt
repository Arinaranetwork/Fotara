// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.canvas

import com.arinara.fotara.data.model.BackgroundStyle
import com.arinara.fotara.data.model.CanvasDocument
import com.arinara.fotara.data.model.CanvasImage
import com.arinara.fotara.data.model.CanvasLayer
import com.arinara.fotara.data.model.CanvasPoint
import com.arinara.fotara.data.model.CanvasStroke
import com.arinara.fotara.data.model.CanvasTool
import com.arinara.fotara.data.model.EraserMode
import com.arinara.fotara.data.model.NibProfile
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CanvasModelTest {

    @Test
    fun defaultDocument_hasExpectedProperties() {
        val doc = CanvasDocument()
        assertEquals(1, doc.version)
        assertEquals(BackgroundStyle.GRID, doc.backgroundStyle)
        assertEquals(1, doc.layers.size)

        val defaultLayer = doc.layers.first()
        assertEquals(1, defaultLayer.id)
        assertEquals("Layer 1", defaultLayer.name)
        assertTrue(defaultLayer.isVisible)
        assertFalse(defaultLayer.isLocked)
        assertEquals(1.0f, defaultLayer.opacity, 0.001f)
        assertTrue(defaultLayer.strokes.isEmpty())
        assertTrue(defaultLayer.images.isEmpty())
    }

    @Test
    fun serializeAndDeserialize_maintainsAllStrokeData() {
        val points = listOf(
            CanvasPoint(x = 10f, y = 20f, pressure = 0.5f),
            CanvasPoint(x = 15f, y = 25f, pressure = 0.7f),
            CanvasPoint(x = 22f, y = 35f, pressure = 0.9f)
        )
        val stroke = CanvasStroke(
            id = "stroke-test-1",
            tool = CanvasTool.PEN,
            color = 0xFFEBD8B8,
            size = 6.0f,
            alpha = 0.85f,
            nibProfile = NibProfile.BALLPOINT,
            points = points,
            isLocked = false
        )
        val layer = CanvasLayer(
            id = 1,
            name = "Drawing Layer",
            strokes = listOf(stroke)
        )
        val doc = CanvasDocument(
            version = 1,
            layers = listOf(layer),
            backgroundStyle = BackgroundStyle.DOTS
        )

        val bytes = doc.serialize()
        assertNotNull(bytes)
        assertTrue(bytes.isNotEmpty())

        val restored = CanvasDocument.deserialize(bytes)
        assertEquals(doc.version, restored.version)
        assertEquals(BackgroundStyle.DOTS, restored.backgroundStyle)
        assertEquals(1, restored.layers.size)

        val restoredLayer = restored.layers.first()
        assertEquals("Drawing Layer", restoredLayer.name)
        assertEquals(1, restoredLayer.strokes.size)

        val restoredStroke = restoredLayer.strokes.first()
        assertEquals("stroke-test-1", restoredStroke.id)
        assertEquals(CanvasTool.PEN, restoredStroke.tool)
        assertEquals(0xFFEBD8B8, restoredStroke.color)
        assertEquals(6.0f, restoredStroke.size, 0.001f)
        assertEquals(0.85f, restoredStroke.alpha, 0.001f)
        assertEquals(NibProfile.BALLPOINT, restoredStroke.nibProfile)
        assertEquals(3, restoredStroke.points.size)
        assertEquals(10f, restoredStroke.points[0].x, 0.001f)
        assertEquals(20f, restoredStroke.points[0].y, 0.001f)
        assertEquals(0.5f, restoredStroke.points[0].pressure, 0.001f)
    }

    @Test
    fun serializeAndDeserialize_maintainsImagesAndMultipleLayers() {
        val image = CanvasImage(
            id = "img-1",
            imageUri = "content://media/external/images/media/42",
            x = 100f,
            y = 150f,
            width = 300f,
            height = 200f,
            rotation = 45f,
            isLocked = true
        )
        val layer1 = CanvasLayer(
            id = 1,
            name = "Background Layer",
            isVisible = true,
            isLocked = false,
            opacity = 1.0f,
            images = listOf(image)
        )
        val layer2 = CanvasLayer(
            id = 2,
            name = "Overlay Layer",
            isVisible = false,
            isLocked = true,
            opacity = 0.6f,
            strokes = emptyList()
        )
        val doc = CanvasDocument(
            version = 1,
            layers = listOf(layer1, layer2),
            backgroundStyle = BackgroundStyle.RULED
        )

        val bytes = doc.serialize()
        val restored = CanvasDocument.deserialize(bytes)

        assertEquals(BackgroundStyle.RULED, restored.backgroundStyle)
        assertEquals(2, restored.layers.size)

        val restoredL1 = restored.layers[0]
        assertEquals(1, restoredL1.id)
        assertEquals("Background Layer", restoredL1.name)
        assertTrue(restoredL1.isVisible)
        assertFalse(restoredL1.isLocked)
        assertEquals(1, restoredL1.images.size)

        val restoredImg = restoredL1.images[0]
        assertEquals("img-1", restoredImg.id)
        assertEquals("content://media/external/images/media/42", restoredImg.imageUri)
        assertEquals(100f, restoredImg.x, 0.001f)
        assertEquals(150f, restoredImg.y, 0.001f)
        assertEquals(300f, restoredImg.width, 0.001f)
        assertEquals(200f, restoredImg.height, 0.001f)
        assertEquals(45f, restoredImg.rotation, 0.001f)
        assertTrue(restoredImg.isLocked)

        val restoredL2 = restored.layers[1]
        assertEquals(2, restoredL2.id)
        assertEquals("Overlay Layer", restoredL2.name)
        assertFalse(restoredL2.isVisible)
        assertTrue(restoredL2.isLocked)
        assertEquals(0.6f, restoredL2.opacity, 0.001f)
    }

    @Test
    fun deserialize_handlesNullOrEmptyBytesGracefully() {
        val docNull = CanvasDocument.deserialize(null)
        assertEquals(1, docNull.version)
        assertEquals(1, docNull.layers.size)

        val docEmpty = CanvasDocument.deserialize(ByteArray(0))
        assertEquals(1, docEmpty.version)
        assertEquals(1, docEmpty.layers.size)
    }

    @Test
    fun canvasEnums_verifyAllValues() {
        val bgStyles = BackgroundStyle.entries
        assertEquals(4, bgStyles.size)
        assertTrue(bgStyles.contains(BackgroundStyle.BLANK))
        assertTrue(bgStyles.contains(BackgroundStyle.GRID))
        assertTrue(bgStyles.contains(BackgroundStyle.DOTS))
        assertTrue(bgStyles.contains(BackgroundStyle.RULED))

        val nibs = NibProfile.entries
        assertEquals(3, nibs.size)
        assertTrue(nibs.contains(NibProfile.BALLPOINT))
        assertTrue(nibs.contains(NibProfile.CHISEL))
        assertTrue(nibs.contains(NibProfile.BRUSH))

        val tools = CanvasTool.entries
        assertEquals(4, tools.size)
        assertTrue(tools.contains(CanvasTool.SELECT))
        assertTrue(tools.contains(CanvasTool.PEN))
        assertTrue(tools.contains(CanvasTool.HIGHLIGHTER))
        assertTrue(tools.contains(CanvasTool.ERASER))

        val eraserModes = EraserMode.entries
        assertEquals(2, eraserModes.size)
        assertTrue(eraserModes.contains(EraserMode.STROKE))
        assertTrue(eraserModes.contains(EraserMode.AREA))
    }
}
