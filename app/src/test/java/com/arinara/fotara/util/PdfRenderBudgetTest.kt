// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfRenderBudgetTest {

    @Test
    fun testDimensionsAt1xZoom_WithinBudget() {
        val (w, h) = PdfRenderBudget.calculateBoundedDimensions(600, 800, 1.0f)
        assertEquals(600, w)
        assertEquals(800, h)
        assertTrue(w.toLong() * h.toLong() <= PdfRenderBudget.MAX_PIXEL_BUDGET)
    }

    @Test
    fun testDimensionsAt2xZoom_WithinBudget() {
        // 1200 * 1600 = 1,920,000 px <= 4,000,000 px
        val (w, h) = PdfRenderBudget.calculateBoundedDimensions(600, 800, 2.0f)
        assertEquals(1200, w)
        assertEquals(1600, h)
        assertTrue(w.toLong() * h.toLong() <= PdfRenderBudget.MAX_PIXEL_BUDGET)
    }

    @Test
    fun testDimensionsAt4xZoom_ScaledDownToFitBudget() {
        // 600x800 at 4x would be 2400x3200 = 7,680,000 px (> 4,000,000 px)
        val (w, h) = PdfRenderBudget.calculateBoundedDimensions(600, 800, 4.0f)

        // Pixel count must stay strictly within budget
        val totalPixels = w.toLong() * h.toLong()
        assertTrue("Total pixels $totalPixels exceeds budget", totalPixels <= PdfRenderBudget.MAX_PIXEL_BUDGET)

        // Aspect ratio must be preserved
        val originalAspect = 600f / 800f
        val boundedAspect = w.toFloat() / h.toFloat()
        assertEquals(originalAspect, boundedAspect, 0.02f)

        // Bounded dimensions should be greater than 2x (1200x1600) but less than 4x (2400x3200)
        assertTrue(w > 1200)
        assertTrue(w < 2400)
    }

    @Test
    fun testGiantPage_AlwaysCappedWithinBudget() {
        val (w, h) = PdfRenderBudget.calculateBoundedDimensions(5000, 8000, 3.0f)
        val totalPixels = w.toLong() * h.toLong()
        assertTrue(totalPixels <= PdfRenderBudget.MAX_PIXEL_BUDGET)
    }

    @Test
    fun testDegenerateZeroDimensions_HandledSafely() {
        val (w, h) = PdfRenderBudget.calculateBoundedDimensions(0, 0, 1.0f)
        assertTrue(w >= 100)
        assertTrue(h >= 100)
    }
}
