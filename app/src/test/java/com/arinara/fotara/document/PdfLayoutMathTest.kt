// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.document

import com.arinara.fotara.util.PdfLayoutMath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfLayoutMathTest {

    @Test
    fun testA4PortraitDimensions() {
        val a4Ratio = 595f / 842f // ~0.70665
        val target = PdfLayoutMath.computeTargetSize(
            cardWidthPx = 1000,
            aspectRatio = a4Ratio,
            zoomFactor = 1.0f
        )

        assertEquals(1000, target.widthPx)
        // 1000 / 0.70665 ≈ 1415
        assertEquals(1415, target.heightPx)
        assertEquals(a4Ratio, target.aspectRatio, 0.005f)
    }

    @Test
    fun test16x9LandscapeSlideDimensions() {
        val slideRatio = 16f / 9f // 1.7777778
        val target = PdfLayoutMath.computeTargetSize(
            cardWidthPx = 1000,
            aspectRatio = slideRatio,
            zoomFactor = 1.0f
        )

        assertEquals(1000, target.widthPx)
        // 1000 / (16/9) = 562.5 ≈ 563
        assertEquals(563, target.heightPx)
        assertEquals(slideRatio, target.aspectRatio, 0.005f)
    }

    @Test
    fun test4x3StandardDimensions() {
        val ratio4x3 = 4f / 3f // 1.3333334
        val target = PdfLayoutMath.computeTargetSize(
            cardWidthPx = 1200,
            aspectRatio = ratio4x3,
            zoomFactor = 1.0f
        )

        assertEquals(1200, target.widthPx)
        assertEquals(900, target.heightPx)
        assertEquals(ratio4x3, target.aspectRatio, 0.005f)
    }

    @Test
    fun testSquareDimensions() {
        val target = PdfLayoutMath.computeTargetSize(
            cardWidthPx = 800,
            aspectRatio = 1.0f,
            zoomFactor = 1.0f
        )

        assertEquals(800, target.widthPx)
        assertEquals(800, target.heightPx)
        assertEquals(1.0f, target.aspectRatio, 0.001f)
    }

    @Test
    fun testRotated90DegreesDimensions() {
        val rotatedA4 = 842f / 595f // Landscape orientation of A4: 1.415126
        val target = PdfLayoutMath.computeTargetSize(
            cardWidthPx = 1000,
            aspectRatio = rotatedA4,
            zoomFactor = 1.0f
        )

        assertEquals(1000, target.widthPx)
        assertEquals(707, target.heightPx)
        assertEquals(rotatedA4, target.aspectRatio, 0.005f)
    }

    @Test
    fun testZoomFactorScalesProportionally() {
        val slideRatio = 16f / 9f
        val base = PdfLayoutMath.computeTargetSize(
            cardWidthPx = 500,
            aspectRatio = slideRatio,
            zoomFactor = 1.0f
        )
        val zoomed = PdfLayoutMath.computeTargetSize(
            cardWidthPx = 500,
            aspectRatio = slideRatio,
            zoomFactor = 2.0f
        )

        assertEquals(500, base.widthPx)
        assertEquals(281, base.heightPx)

        assertEquals(1000, zoomed.widthPx)
        assertEquals(563, zoomed.heightPx)

        assertEquals(base.aspectRatio, zoomed.aspectRatio, 0.01f)
    }

    @Test
    fun testMaximumDimensionSafetyClamp() {
        val a4Ratio = 595f / 842f
        // Extreme zoom on large display would exceed 2560px
        val target = PdfLayoutMath.computeTargetSize(
            cardWidthPx = 2000,
            aspectRatio = a4Ratio,
            zoomFactor = 3.0f,
            maxDimensionPx = 2560
        )

        // Height is the larger dimension for portrait, so height is clamped to 2560px
        assertEquals(2560, target.heightPx)
        // Width is scaled down proportionally: 2560 * a4Ratio ≈ 1809
        assertEquals(1809, target.widthPx)
        assertEquals(a4Ratio, target.aspectRatio, 0.005f)
    }

    @Test
    fun testMinimumDimensionSafetyClamp() {
        val target = PdfLayoutMath.computeTargetSize(
            cardWidthPx = 20,
            aspectRatio = 1.0f,
            zoomFactor = 0.5f,
            minDimensionPx = 100
        )

        assertTrue(target.widthPx >= 100)
        assertTrue(target.heightPx >= 100)
    }

    @Test
    fun testDensitySweepAndCardWidths() {
        val densities = listOf(1.0f, 1.5f, 2.0f, 2.625f, 3.0f, 3.5f, 4.0f)
        val cardWidthsDp = listOf(320f, 360f, 400f, 600f, 800f)
        val slideRatio = 16f / 9f

        for (density in densities) {
            for (widthDp in cardWidthsDp) {
                val target = PdfLayoutMath.computeTargetSizeFromDp(
                    cardWidthDp = widthDp,
                    aspectRatio = slideRatio,
                    density = density,
                    zoomFactor = 1.0f
                )

                assertTrue(target.widthPx in 100..2560)
                assertTrue(target.heightPx in 100..2560)
                assertEquals(slideRatio, target.aspectRatio, 0.01f)
            }
        }
    }

    @Test
    fun testSanitizeInvalidAspectRatios() {
        val targetNaN = PdfLayoutMath.computeTargetSize(1000, Float.NaN)
        assertEquals(PdfLayoutMath.FALLBACK_ASPECT_RATIO, targetNaN.aspectRatio, 0.01f)

        val targetZero = PdfLayoutMath.computeTargetSize(1000, 0f)
        assertEquals(PdfLayoutMath.FALLBACK_ASPECT_RATIO, targetZero.aspectRatio, 0.01f)

        val targetNeg = PdfLayoutMath.computeTargetSize(1000, -1.5f)
        assertEquals(PdfLayoutMath.FALLBACK_ASPECT_RATIO, targetNeg.aspectRatio, 0.01f)
    }

    @Test
    fun testCardHeightDpCalculation() {
        val a4Ratio = 595f / 842f
        val height = PdfLayoutMath.computeCardHeightDp(360f, a4Ratio)
        assertEquals(360f / a4Ratio, height, 0.01f)
    }
}
