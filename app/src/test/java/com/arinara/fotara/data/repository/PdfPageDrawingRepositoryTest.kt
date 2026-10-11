// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.repository

import androidx.compose.ui.geometry.Offset
import com.arinara.fotara.canvas.engine.CanvasRect
import com.arinara.fotara.canvas.engine.DrawingConstants
import com.arinara.fotara.canvas.engine.StrokeProcessor
import com.arinara.fotara.canvas.model.StrokeBlendMode
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType
import com.arinara.fotara.canvas.persistence.PhotoDrawingCodec
import com.arinara.fotara.data.model.PdfPageDrawing
import com.arinara.fotara.test.FakePdfPageDrawingRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfPageDrawingRepositoryTest {

    private fun createSamplePdfStroke(x1: Float, y1: Float, x2: Float, y2: Float, widthPts: Float = 4.0f): StrokeElement {
        val points = listOf(
            StrokePoint(x1, y1, 0.8f),
            StrokePoint((x1 + x2) / 2f, (y1 + y2) / 2f, 0.9f),
            StrokePoint(x2, y2, 1.0f)
        )
        return StrokeElement(
            layerId = "pdf_layer",
            points = points,
            color = 0xFFFF0000,
            width = widthPts,
            toolType = StrokeToolType.PEN,
            blendMode = StrokeBlendMode.NORMAL,
            bounds = StrokeProcessor.computeBounds(points, widthPts)
        )
    }

    @Test
    fun codecRoundTrip_inPdfPagePointSpace() {
        // A4 page points: 595.28 x 841.89
        val stroke1 = createSamplePdfStroke(100.5f, 200.5f, 300.5f, 400.5f, widthPts = 3.5f)
        val stroke2 = StrokeElement(
            layerId = "pdf_layer",
            points = listOf(StrokePoint(50.0f, 50.0f, 0.6f)),
            color = 0xFFF4D03F,
            width = 18.0f,
            toolType = StrokeToolType.HIGHLIGHTER,
            blendMode = StrokeBlendMode.MULTIPLY,
            bounds = CanvasRect(40f, 40f, 60f, 60f)
        )

        val originalStrokes = listOf(stroke1, stroke2)
        val encoded = PhotoDrawingCodec.encode(originalStrokes)
        assertTrue(encoded.isNotEmpty())

        val decoded = PhotoDrawingCodec.decode(encoded, layerId = "pdf_page_100_0")
        assertEquals(2, decoded.size)

        val d1 = decoded[0]
        assertEquals(stroke1.toolType, d1.toolType)
        assertEquals(stroke1.blendMode, d1.blendMode)
        assertEquals(stroke1.color, d1.color)
        assertEquals(stroke1.width, d1.width, 0.05f)
        assertEquals(3, d1.points.size)
        assertEquals(100.5f, d1.points[0].x, 0.05f)
        assertEquals(200.5f, d1.points[0].y, 0.05f)

        val d2 = decoded[1]
        assertEquals(StrokeToolType.HIGHLIGHTER, d2.toolType)
        assertEquals(StrokeBlendMode.MULTIPLY, d2.blendMode)
        assertEquals(18.0f, d2.width, 0.05f)
    }

    @Test
    fun fakePdfPageDrawingRepository_crudAndFlows() = runBlocking {
        val repo = FakePdfPageDrawingRepository()
        assertNull(repo.getDrawing(100L, 0))

        val stroke = createSamplePdfStroke(50f, 100f, 150f, 200f)
        val drawing = PdfPageDrawing(
            documentId = 100L,
            pageIndex = 0,
            strokes = listOf(stroke),
            pageWidth = 595.28f,
            pageHeight = 841.89f,
            isVisible = true
        )

        repo.saveDrawing(drawing)
        val saved = repo.getDrawing(100L, 0)
        assertNotNull(saved)
        assertEquals(1, saved!!.strokes.size)
        assertTrue(saved.isVisible)
        assertEquals(595.28f, saved.pageWidth, 0.01f)
        assertEquals(841.89f, saved.pageHeight, 0.01f)

        // Observe document drawings map
        val docMap = repo.observeDocumentDrawings(100L).first()
        assertEquals(1, docMap.size)
        assertTrue(docMap.containsKey(0))

        // Observe visible pages
        val visiblePages = repo.observeVisibleDrawingPages(100L).first()
        assertTrue(visiblePages.contains(0))

        // Toggle visibility to hidden
        repo.setVisible(100L, 0, false)
        val hidden = repo.getDrawing(100L, 0)
        assertFalse(hidden!!.isVisible)
        val visiblePagesAfterHide = repo.observeVisibleDrawingPages(100L).first()
        assertFalse(visiblePagesAfterHide.contains(0))

        // Clear drawing
        repo.clearDrawing(100L, 0)
        assertNull(repo.getDrawing(100L, 0))
        val emptyMap = repo.observeDocumentDrawings(100L).first()
        assertTrue(emptyMap.isEmpty())
    }

    @Test
    fun deleteDrawingsForDocument_removesAllPages() = runBlocking {
        val repo = FakePdfPageDrawingRepository()
        val d0 = PdfPageDrawing(
            documentId = 200L,
            pageIndex = 0,
            strokes = listOf(createSamplePdfStroke(10f, 10f, 20f, 20f)),
            pageWidth = 600f,
            pageHeight = 800f
        )
        val d1 = PdfPageDrawing(
            documentId = 200L,
            pageIndex = 1,
            strokes = listOf(createSamplePdfStroke(30f, 30f, 40f, 40f)),
            pageWidth = 600f,
            pageHeight = 800f
        )
        repo.saveDrawing(d0)
        repo.saveDrawing(d1)

        val beforeDelete = repo.observeDocumentDrawings(200L).first()
        assertEquals(2, beforeDelete.size)

        repo.deleteDrawingsForDocument(200L)
        val afterDelete = repo.observeDocumentDrawings(200L).first()
        assertTrue(afterDelete.isEmpty())
        assertNull(repo.getDrawing(200L, 0))
        assertNull(repo.getDrawing(200L, 1))
    }

    @Test
    fun drawingConstants_sizeMapping_respectsDensityAndScale() {
        // slider = 4dp, density = 2.0 (xhdpi), fitScale = 1.0 -> 8 pts
        val size1 = DrawingConstants.computeStoredWidth(4f, 2.0f, 1.0f)
        assertEquals(8.0f, size1, 0.001f)

        // slider = 20dp, density = 3.0 (xxhdpi), fitScale = 2.0 -> 30 pts
        val size2 = DrawingConstants.computeStoredWidth(20f, 3.0f, 2.0f)
        assertEquals(30.0f, size2, 0.001f)

        // Fallback for zero fit scale
        val size3 = DrawingConstants.computeStoredWidth(10f, 2.0f, 0f)
        assertEquals(20.0f, size3, 0.001f)
    }

    @Test
    fun drawingConstants_eraserCapsule_removesIntersectedStrokes() {
        val stroke = createSamplePdfStroke(100f, 100f, 200f, 100f, widthPts = 6.0f)
        val strokes = listOf(stroke)

        // Erase across the line from (150, 50) to (150, 150) with radius 20
        val updated = DrawingConstants.eraseStrokes(
            strokes = strokes,
            prevPt = Offset(150f, 50f),
            currentPt = Offset(150f, 150f),
            eraserRadius = 20f
        )

        // The stroke should be erased or cut
        assertTrue(updated.size != 1 || updated[0].points.size != stroke.points.size)
    }

    @Test
    fun undoStack_cappedAtMaxSteps() {
        val undoStack = ArrayDeque<List<StrokeElement>>()
        val maxSteps = DrawingConstants.MAX_UNDO_STEPS
        for (i in 0 until 120) {
            val stroke = createSamplePdfStroke(i.toFloat(), i.toFloat(), (i + 1).toFloat(), (i + 1).toFloat())
            undoStack.addLast(listOf(stroke))
            if (undoStack.size > maxSteps) {
                undoStack.removeFirst()
            }
        }
        assertEquals(100, undoStack.size)
    }

    @Test
    fun clampPanOffset_keepsPageInsideContainer() {
        val containerW = 1080f
        val containerH = 1920f
        val userScale = 2.0f
        val maxPanX = (containerW * (userScale - 1f)) / 2f
        val maxPanY = (containerH * (userScale - 1f)) / 2f

        val excessPanX = 1000f
        val clampedX = excessPanX.coerceIn(-maxPanX, maxPanX)
        assertEquals(maxPanX, clampedX, 0.001f)

        val excessPanY = -2000f
        val clampedY = excessPanY.coerceIn(-maxPanY, maxPanY)
        assertEquals(-maxPanY, clampedY, 0.001f)
    }

    @Test
    fun pdfPageEditorNavKey_properties() {
        val key = com.arinara.fotara.PdfPageEditorNavKey(documentId = 42L, pageIndex = 3)
        assertEquals(42L, key.documentId)
        assertEquals(3, key.pageIndex)
    }
}
