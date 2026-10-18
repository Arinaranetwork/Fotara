// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.storage

import com.arinara.fotara.canvas.model.StrokeBlendMode
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType
import com.arinara.fotara.data.model.PhotoDrawing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PhotoFlattenerTest {

    private fun sampleStroke(): StrokeElement {
        return StrokeElement(
            layerId = "photo_layer",
            points = listOf(StrokePoint(10f, 10f), StrokePoint(20f, 20f)),
            color = 0xFF00FF00,
            width = 4f,
            toolType = StrokeToolType.PEN,
            blendMode = StrokeBlendMode.NORMAL,
            bounds = com.arinara.fotara.canvas.engine.CanvasRect(10f, 10f, 20f, 20f)
        )
    }

    @Test
    fun shouldFlatten_nullDrawing_returnsFalse() {
        assertFalse(PhotoFlattener.shouldFlatten(null))
    }

    @Test
    fun shouldFlatten_invisibleDrawing_returnsFalse() {
        val drawing = PhotoDrawing(
            photoId = 1L,
            strokes = listOf(sampleStroke()),
            widthPx = 1000,
            heightPx = 1000,
            isVisible = false
        )
        assertFalse(PhotoFlattener.shouldFlatten(drawing))
    }

    @Test
    fun shouldFlatten_emptyStrokes_returnsFalse() {
        val drawing = PhotoDrawing(
            photoId = 1L,
            strokes = emptyList(),
            widthPx = 1000,
            heightPx = 1000,
            isVisible = true
        )
        assertFalse(PhotoFlattener.shouldFlatten(drawing))
    }

    @Test
    fun shouldFlatten_visibleWithStrokes_returnsTrue() {
        val drawing = PhotoDrawing(
            photoId = 1L,
            strokes = listOf(sampleStroke()),
            widthPx = 1000,
            heightPx = 1000,
            isVisible = true
        )
        assertTrue(PhotoFlattener.shouldFlatten(drawing))
    }

    @Test
    fun calculateInSampleSize_standardSizes_returnsOne() {
        assertEquals(1, PhotoFlattener.calculateInSampleSize(1920, 1080))
        assertEquals(1, PhotoFlattener.calculateInSampleSize(4096, 4096))
        assertEquals(1, PhotoFlattener.calculateInSampleSize(100, 100))
    }

    @Test
    fun calculateInSampleSize_exceedingSizes_powersOfTwo() {
        assertEquals(2, PhotoFlattener.calculateInSampleSize(5000, 3000))
        assertEquals(2, PhotoFlattener.calculateInSampleSize(3000, 5000))
        assertEquals(4, PhotoFlattener.calculateInSampleSize(9000, 4000))
        assertEquals(4, PhotoFlattener.calculateInSampleSize(16000, 4000))
        assertEquals(8, PhotoFlattener.calculateInSampleSize(20000, 1000))
        assertEquals(16, PhotoFlattener.calculateInSampleSize(33000, 1000))
    }

    @Test
    fun calculateInSampleSize_zeroOrNegative_returnsAtLeastOne() {
        assertEquals(1, PhotoFlattener.calculateInSampleSize(0, 0))
        assertEquals(1, PhotoFlattener.calculateInSampleSize(-100, -200))
    }
}
