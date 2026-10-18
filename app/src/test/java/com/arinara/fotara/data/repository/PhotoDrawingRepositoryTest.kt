// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.repository

import com.arinara.fotara.canvas.engine.CanvasRect
import com.arinara.fotara.canvas.engine.PhotoDrawingTransform
import com.arinara.fotara.canvas.engine.StrokeProcessor
import com.arinara.fotara.canvas.model.StrokeBlendMode
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType
import com.arinara.fotara.canvas.persistence.PhotoDrawingCodec
import com.arinara.fotara.data.model.PhotoDrawing
import com.arinara.fotara.test.FakePhotoDrawingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PhotoDrawingRepositoryTest {

    private fun createSampleStroke(x1: Float, y1: Float, x2: Float, y2: Float): StrokeElement {
        val points = listOf(
            StrokePoint(x1, y1, 0.8f),
            StrokePoint((x1 + x2) / 2f, (y1 + y2) / 2f, 0.9f),
            StrokePoint(x2, y2, 1.0f)
        )
        return StrokeElement(
            layerId = "photo_layer",
            points = points,
            color = 0xFFFF0000,
            width = 12.0f,
            toolType = StrokeToolType.PEN,
            blendMode = StrokeBlendMode.NORMAL,
            bounds = StrokeProcessor.computeBounds(points, 12.0f)
        )
    }

    @Test
    fun codecRoundTrip_preservesStrokesAndProperties() {
        val stroke1 = createSampleStroke(100f, 150f, 200f, 250f)
        val stroke2 = StrokeElement(
            layerId = "photo_layer",
            points = listOf(StrokePoint(50f, 50f, 0.5f)),
            color = 0xFF00FF00,
            width = 24.0f,
            toolType = StrokeToolType.HIGHLIGHTER,
            blendMode = StrokeBlendMode.MULTIPLY,
            bounds = CanvasRect(40f, 40f, 60f, 60f)
        )

        val originalStrokes = listOf(stroke1, stroke2)
        val encoded = PhotoDrawingCodec.encode(originalStrokes)
        assertTrue(encoded.isNotEmpty())

        val decoded = PhotoDrawingCodec.decode(encoded)
        assertEquals(2, decoded.size)

        val d1 = decoded[0]
        assertEquals(stroke1.toolType, d1.toolType)
        assertEquals(stroke1.blendMode, d1.blendMode)
        assertEquals(stroke1.color, d1.color)
        assertEquals(stroke1.width, d1.width, 0.1f)
        assertEquals(3, d1.points.size)
        assertEquals(100f, d1.points[0].x, 0.1f)
        assertEquals(150f, d1.points[0].y, 0.1f)

        val d2 = decoded[1]
        assertEquals(StrokeToolType.HIGHLIGHTER, d2.toolType)
        assertEquals(StrokeBlendMode.MULTIPLY, d2.blendMode)
    }

    @Test
    fun codecGracefulDegradation_onMalformedInput() {
        assertEquals(emptyList<StrokeElement>(), PhotoDrawingCodec.decode(null))
        assertEquals(emptyList<StrokeElement>(), PhotoDrawingCodec.decode(ByteArray(0)))
        assertEquals(emptyList<StrokeElement>(), PhotoDrawingCodec.decode(byteArrayOf(0x02, 0x01)))
        assertEquals(emptyList<StrokeElement>(), PhotoDrawingCodec.decode(byteArrayOf(0x01, 0xFF.toByte())))
    }

    @Test
    fun rotate90Clockwise_transformsCoordinatesCorrectly() {
        val stroke = createSampleStroke(100f, 200f, 300f, 400f)
        val drawing = PhotoDrawing(
            photoId = 1L,
            strokes = listOf(stroke),
            widthPx = 1000,
            heightPx = 800
        )

        // Rotate 90 CW: (x, y) -> (H - y, x). oldH = 800, oldW = 1000
        val r1 = PhotoDrawingTransform.rotate90Clockwise(drawing, 1000, 800)
        assertEquals(800, r1.widthPx)
        assertEquals(1000, r1.heightPx)

        // Point (100, 200) -> (800 - 200 = 600, 100)
        val p0 = r1.strokes[0].points[0]
        assertEquals(600f, p0.x, 0.001f)
        assertEquals(100f, p0.y, 0.001f)

        // Point (300, 400) -> (800 - 400 = 400, 300)
        val p2 = r1.strokes[0].points[2]
        assertEquals(400f, p2.x, 0.001f)
        assertEquals(300f, p2.y, 0.001f)
    }

    @Test
    fun rotateFourTimes_returnsToExactOriginalCoordinates() {
        val stroke = createSampleStroke(123.45f, 678.90f, 543.21f, 98.76f)
        val original = PhotoDrawing(
            photoId = 42L,
            strokes = listOf(stroke),
            widthPx = 1920,
            heightPx = 1080
        )

        var current = original
        for (i in 1..4) {
            current = PhotoDrawingTransform.rotate90Clockwise(current, current.widthPx, current.heightPx)
        }

        assertEquals(original.widthPx, current.widthPx)
        assertEquals(original.heightPx, current.heightPx)
        assertEquals(original.strokes.size, current.strokes.size)

        val origPoints = original.strokes[0].points
        val finalPoints = current.strokes[0].points
        assertEquals(origPoints.size, finalPoints.size)

        for (i in origPoints.indices) {
            assertTrue(abs(origPoints[i].x - finalPoints[i].x) < 0.01f)
            assertTrue(abs(origPoints[i].y - finalPoints[i].y) < 0.01f)
            assertEquals(origPoints[i].pressure, finalPoints[i].pressure, 0.001f)
        }
    }

    @Test
    fun cropTransform_shiftsPointsByWindowOffset() {
        val stroke = createSampleStroke(300f, 400f, 500f, 600f)
        val drawing = PhotoDrawing(
            photoId = 1L,
            strokes = listOf(stroke),
            widthPx = 1000,
            heightPx = 1000
        )

        // Crop: crop window starts at (200, 250), new dimensions 400x500
        val cropped = PhotoDrawingTransform.crop(
            drawing = drawing,
            cropLeft = 200f,
            cropTop = 250f,
            newW = 400,
            newH = 500
        )

        assertEquals(400, cropped.widthPx)
        assertEquals(500, cropped.heightPx)

        // (300, 400) -> (300 - 200 = 100, 400 - 250 = 150)
        val p0 = cropped.strokes[0].points[0]
        assertEquals(100f, p0.x, 0.001f)
        assertEquals(150f, p0.y, 0.001f)

        // (500, 600) -> (500 - 200 = 300, 600 - 250 = 350)
        val p2 = cropped.strokes[0].points[2]
        assertEquals(300f, p2.x, 0.001f)
        assertEquals(350f, p2.y, 0.001f)
    }

    @Test
    fun fakePhotoDrawingRepository_crudAndVisibilityFlow() = runBlocking {
        val repo = FakePhotoDrawingRepository()
        assertNull(repo.getDrawing(10L))

        val stroke = createSampleStroke(10f, 20f, 30f, 40f)
        val drawing = PhotoDrawing(
            photoId = 10L,
            strokes = listOf(stroke),
            widthPx = 800,
            heightPx = 600,
            isVisible = true
        )

        repo.saveDrawing(drawing)
        val saved = repo.getDrawing(10L)
        assertNotNull(saved)
        assertEquals(1, saved!!.strokes.size)
        assertTrue(saved.isVisible)

        // Toggle visibility
        repo.setVisible(10L, false)
        val hidden = repo.getDrawing(10L)
        assertFalse(hidden!!.isVisible)

        // Clear drawing
        repo.clearDrawing(10L)
        assertNull(repo.getDrawing(10L))
        assertNull(repo.observeDrawing(10L).first())
    }
}
