// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.academic

import com.arinara.fotara.feature.academic.dewarp.DewarpFilterMode
import com.arinara.fotara.feature.academic.dewarp.PerspectiveDewarpEngine
import com.arinara.fotara.feature.academic.dewarp.PerspectiveDewarpMatrix
import com.arinara.fotara.feature.academic.dewarp.PixelBuffer
import com.arinara.fotara.feature.academic.dewarp.QuadPoint
import com.arinara.fotara.feature.academic.dewarp.WhiteboardContrastFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class DocumentPerspectiveDewarperTest {

    private lateinit var whiteboardFilter: WhiteboardContrastFilter

    @Before
    fun setUp() {
        whiteboardFilter = WhiteboardContrastFilter()
    }

    @Test
    fun homography_identityMapping_preservesCoordinates() {
        val src = listOf(
            QuadPoint(0f, 0f),
            QuadPoint(200f, 0f),
            QuadPoint(200f, 300f),
            QuadPoint(0f, 300f)
        )
        val dst = src

        val matrix = PerspectiveDewarpMatrix.computeHomography(src, dst)

        for (pt in src) {
            val mapped = matrix.mapPoint(pt)
            assertEquals(pt.x, mapped.x, 0.01f)
            assertEquals(pt.y, mapped.y, 0.01f)
        }

        // Test midpoint
        val midMapped = matrix.mapPoint(100f, 150f)
        assertEquals(100f, midMapped.x, 0.01f)
        assertEquals(150f, midMapped.y, 0.01f)
    }

    @Test
    fun homography_skewedQuadToRectangle_mapsCornersAccurately() {
        // Perspective distortion quad (keystoned document)
        val srcCorners = listOf(
            QuadPoint(30f, 40f),   // Top-Left
            QuadPoint(220f, 20f),  // Top-Right
            QuadPoint(250f, 280f), // Bottom-Right
            QuadPoint(10f, 260f)   // Bottom-Left
        )

        val targetWidth = 300f
        val targetHeight = 400f
        val dstCorners = listOf(
            QuadPoint(0f, 0f),
            QuadPoint(targetWidth, 0f),
            QuadPoint(targetWidth, targetHeight),
            QuadPoint(0f, targetHeight)
        )

        val matrix = PerspectiveDewarpMatrix.computeRectangularDewarp(
            srcCorners = srcCorners,
            targetWidth = targetWidth,
            targetHeight = targetHeight
        )

        for (i in 0..3) {
            val mapped = matrix.mapPoint(srcCorners[i])
            assertEquals(dstCorners[i].x, mapped.x, 0.1f)
            assertEquals(dstCorners[i].y, mapped.y, 0.1f)
        }
    }

    @Test
    fun homography_invert_reversesPointMapping() {
        val src = listOf(
            QuadPoint(15f, 25f),
            QuadPoint(180f, 10f),
            QuadPoint(195f, 220f),
            QuadPoint(5f, 210f)
        )
        val dst = listOf(
            QuadPoint(0f, 0f),
            QuadPoint(200f, 0f),
            QuadPoint(200f, 200f),
            QuadPoint(0f, 200f)
        )

        val forwardMatrix = PerspectiveDewarpMatrix.computeHomography(src, dst)
        val invMatrix = forwardMatrix.invert()

        assertNotNull(invMatrix)

        // Map arbitrary interior point forward then backward
        val originalPt = QuadPoint(100f, 110f)
        val forwardPt = forwardMatrix.mapPoint(originalPt)
        val recoveredPt = invMatrix!!.mapPoint(forwardPt)

        assertEquals(originalPt.x, recoveredPt.x, 0.05f)
        assertEquals(originalPt.y, recoveredPt.y, 0.05f)
    }

    @Test
    fun whiteboardFilter_bleachesBackgroundShadows() {
        // 2x2 image representing gray ambient shadow on whiteboard
        val grayShadow = (0xFF shl 24) or (200 shl 16) or (200 shl 8) or 200
        val pixels = intArrayOf(grayShadow, grayShadow, grayShadow, grayShadow)
        val buffer = PixelBuffer(2, 2, pixels)

        val processed = whiteboardFilter.process(buffer, shadowReductionThreshold = 170)

        for (color in processed.pixels) {
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            // Should be bleached significantly closer to 255
            assertTrue("Red channel $r should be >= 240", r >= 240)
            assertTrue("Green channel $g should be >= 240", g >= 240)
            assertTrue("Blue channel $b should be >= 240", b >= 240)
        }
    }

    @Test
    fun whiteboardFilter_preservesAndHeightensDarkInk() {
        // Dark ink pixel (40, 40, 40)
        val darkInk = (0xFF shl 24) or (40 shl 16) or (40 shl 8) or 40
        val background = (0xFF shl 24) or (210 shl 16) or (210 shl 8) or 210
        val pixels = intArrayOf(darkInk, background, background, background)
        val buffer = PixelBuffer(2, 2, pixels)

        val processed = whiteboardFilter.process(buffer, contrastMultiplier = 1.5f)

        val inkOut = processed.pixels[0]
        val r = (inkOut shr 16) and 0xFF
        val g = (inkOut shr 8) and 0xFF
        val b = inkOut and 0xFF

        // Ink should remain dark and high-contrast
        assertTrue("Ink red $r should stay dark (<= 40)", r <= 40)
        assertTrue("Ink green $g should stay dark (<= 40)", g <= 40)
        assertTrue("Ink blue $b should stay dark (<= 40)", b <= 40)
    }

    @Test
    fun whiteboardFilter_preservesColoredMarkerInk() {
        // Saturated red marker: RGB(220, 20, 20) with high saturation
        val redMarker = (0xFF shl 24) or (220 shl 16) or (20 shl 8) or 20
        val whiteBg = (0xFF shl 24) or (230 shl 16) or (230 shl 8) or 230
        val pixels = intArrayOf(redMarker, whiteBg, whiteBg, whiteBg)
        val buffer = PixelBuffer(2, 2, pixels)

        val processed = whiteboardFilter.process(buffer)

        val markerOut = processed.pixels[0]
        val r = (markerOut shr 16) and 0xFF
        val g = (markerOut shr 8) and 0xFF
        val b = markerOut and 0xFF

        // Saturated marker should NOT be bleached to white (green/blue should stay low)
        assertTrue("Red channel $r should be high", r > 180)
        assertTrue("Green channel $g should stay low", g < 80)
        assertTrue("Blue channel $b should stay low", b < 80)
    }

    @Test
    fun perspectiveDewarpEngine_warpPixelBuffer_rectifiesKeystonedQuad() {
        // Create 10x10 pattern image with a distinct center pixel
        val width = 10
        val height = 10
        val pixels = IntArray(width * height) { -1 } // all white
        // Fill a dark 4x4 square in the middle (3..6, 3..6)
        for (y in 3..6) {
            for (x in 3..6) {
                pixels[y * width + x] = -0x1000000 // black
            }
        }
        val srcBuffer = PixelBuffer(width, height, pixels)

        // Exact corners
        val srcCorners = listOf(
            QuadPoint(0f, 0f),
            QuadPoint(10f, 0f),
            QuadPoint(10f, 10f),
            QuadPoint(0f, 10f)
        )

        val warped = PerspectiveDewarpEngine.warpPixelBuffer(
            src = srcBuffer,
            srcCorners = srcCorners,
            targetWidth = 10,
            targetHeight = 10
        )

        assertEquals(10, warped.width)
        assertEquals(10, warped.height)
        // Center pixel should be black
        val centerPixel = warped.pixels[5 * 10 + 5]
        val centerR = (centerPixel shr 16) and 0xFF
        assertTrue("Center pixel in warped result should be dark", centerR < 50)

        // Corner pixel (0, 0) should be white
        val cornerPixel = warped.pixels[0]
        val cornerR = (cornerPixel shr 16) and 0xFF
        assertTrue("Corner pixel in warped result should be white", cornerR > 200)
    }

    @Test
    fun perspectiveDewarpEngine_applyFilter_highContrastBw_createsBilevelOutput() {
        // Pixel buffer with varying grays and ink
        val pixels = intArrayOf(
            -0x1000000,                                         // black ink (0)
            (0xFF shl 24) or (50 shl 16) or (50 shl 8) or 50,   // dark gray (50)
            (0xFF shl 24) or (210 shl 16) or (210 shl 8) or 210,// light paper gray (210)
            -1                                                  // pure white (255)
        )
        val buffer = PixelBuffer(2, 2, pixels)

        val filtered = PerspectiveDewarpEngine.applyFilter(buffer, DewarpFilterMode.HIGH_CONTRAST_BW)

        // All output pixels must be strictly either pure black (-0x1000000) or pure white (-1)
        for (p in filtered.pixels) {
            assertTrue("Pixel must be pure black or pure white", p == -0x1000000 || p == -1)
        }
        // First pixel must be black, last pixel must be white
        assertEquals(-0x1000000, filtered.pixels[0])
        assertEquals(-1, filtered.pixels[3])
    }
}
