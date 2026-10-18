// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Unit tests verifying exact round-trip precision of [DrawingViewTransform].
 * Verifies that screen -> content -> screen and content -> screen -> content
 * preserve coordinates with zero offset across zoom scales 1.0 to 6.0 and arbitrary pan offsets.
 */
class DrawingViewTransformTest {

    private fun assertOffsetEquals(expected: Offset, actual: Offset, epsilon: Float = 0.001f) {
        val dx = abs(expected.x - actual.x)
        val dy = abs(expected.y - actual.y)
        assertTrue(
            "Expected $expected but got $actual (dx=$dx, dy=$dy, epsilon=$epsilon)",
            dx <= epsilon && dy <= epsilon
        )
    }

    @Test
    fun testIdentityTransformAtScale1() {
        val params = DrawingViewTransform.createParams(
            viewportWidth = 1080f,
            viewportHeight = 1920f,
            contentWidth = 1080f,
            contentHeight = 1920f,
            scale = 1.0f,
            panOffset = Offset.Zero
        )

        val testContent = Offset(350f, 850f)
        val screen = params.contentToScreen(testContent)
        val roundTrip = params.screenToContent(screen)

        assertOffsetEquals(testContent, screen)
        assertOffsetEquals(testContent, roundTrip)
    }

    @Test
    fun testRoundTripAtVariousZoomScalesAndPans() {
        val scales = listOf(1.0f, 1.5f, 2.0f, 2.75f, 3.0f, 4.5f, 5.0f, 6.0f)
        val pans = listOf(
            Offset.Zero,
            Offset(50f, -80f),
            Offset(-120f, 300f),
            Offset(240f, -150f)
        )

        val viewportW = 1080f
        val viewportH = 2160f
        val contentW = 1200f
        val contentH = 1600f

        val samplePoints = listOf(
            Offset(100f, 100f),
            Offset(600f, 800f),
            Offset(1150f, 1550f),
            Offset(0f, 0f),
            Offset(1200f, 1600f)
        )

        for (scale in scales) {
            for (pan in pans) {
                val params = DrawingViewTransform.createParams(
                    viewportWidth = viewportW,
                    viewportHeight = viewportH,
                    contentWidth = contentW,
                    contentHeight = contentH,
                    scale = scale,
                    panOffset = pan
                )

                for (pt in samplePoints) {
                    val screen = params.contentToScreen(pt)
                    val roundTrip = params.screenToContent(screen, clamp = false)
                    assertOffsetEquals(pt, roundTrip, 0.005f)
                }
            }
        }
    }

    @Test
    fun testScreenToLocalRoundTrip() {
        val params = DrawingViewTransform.createParams(
            viewportWidth = 1000f,
            viewportHeight = 1500f,
            contentWidth = 800f,
            contentHeight = 1200f,
            scale = 3.5f,
            panOffset = Offset(120f, -90f)
        )

        val testScreens = listOf(
            Offset(500f, 750f),
            Offset(100f, 200f),
            Offset(900f, 1400f)
        )

        for (screen in testScreens) {
            val local = params.screenToLocal(screen)
            val backToScreen = params.localToScreen(local)
            assertOffsetEquals(screen, backToScreen, 0.001f)
        }
    }

    @Test
    fun testPdfPagePointsAspectRatios() {
        // Standard US Letter: 612 x 792 points
        val letterParams = DrawingViewTransform.createParams(
            viewportWidth = 1080f,
            viewportHeight = 2200f,
            contentWidth = 612f,
            contentHeight = 792f,
            scale = 2.5f,
            panOffset = Offset(-45f, 60f)
        )

        val ptInPoints = Offset(306f, 396f) // Center of page
        val screen = letterParams.contentToScreen(ptInPoints)
        val recovered = letterParams.screenToContent(screen)
        assertOffsetEquals(ptInPoints, recovered, 0.002f)

        // Landscape A4: 842 x 595 points
        val a4LandscapeParams = DrawingViewTransform.createParams(
            viewportWidth = 1080f,
            viewportHeight = 2200f,
            contentWidth = 842f,
            contentHeight = 595f,
            scale = 4.0f,
            panOffset = Offset(100f, -100f)
        )
        val ptA4 = Offset(421f, 297.5f)
        val screenA4 = a4LandscapeParams.contentToScreen(ptA4)
        val recoveredA4 = a4LandscapeParams.screenToContent(screenA4)
        assertOffsetEquals(ptA4, recoveredA4, 0.002f)
    }

    @Test
    fun testClampingBehavior() {
        val params = DrawingViewTransform.createParams(
            viewportWidth = 1000f,
            viewportHeight = 1000f,
            contentWidth = 500f,
            contentHeight = 500f,
            scale = 1.0f,
            panOffset = Offset.Zero
        )

        // Far outside content
        val outsideScreen = Offset(-500f, -500f)
        val clampedContent = params.screenToContent(outsideScreen, clamp = true)
        assertEquals(0f, clampedContent.x, 0.001f)
        assertEquals(0f, clampedContent.y, 0.001f)

        val farBottomRight = Offset(2000f, 2000f)
        val clampedBottomRight = params.screenToContent(farBottomRight, clamp = true)
        assertEquals(500f, clampedBottomRight.x, 0.001f)
        assertEquals(500f, clampedBottomRight.y, 0.001f)
    }
}
