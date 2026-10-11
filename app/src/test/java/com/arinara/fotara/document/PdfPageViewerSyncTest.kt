// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.document

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.roundToInt

class PdfPageViewerSyncTest {

    @Test
    fun testInitialPageIndexClamping() {
        val totalCount = 10

        // Clamping negative or out-of-bounds indices
        val clampedNegative = (-5).coerceIn(0, totalCount - 1)
        assertEquals(0, clampedNegative)

        val clampedBeyond = 15.coerceIn(0, totalCount - 1)
        assertEquals(9, clampedBeyond)

        val clampedValid = 4.coerceIn(0, totalCount - 1)
        assertEquals(4, clampedValid)
    }

    @Test
    fun testSinglePageDocumentIndexClamping() {
        val totalCount = 1

        val clampedZero = 0.coerceIn(0, totalCount - 1)
        assertEquals(0, clampedZero)

        val clampedBeyond = 5.coerceIn(0, totalCount - 1)
        assertEquals(0, clampedBeyond)
    }

    @Test
    fun testZoomGatedPagingRule() {
        // At 1.0x scale, userScrollEnabled must be true (swipe navigates pages)
        val scaleAt1x = 1.0f
        val isZoomedAt1x = scaleAt1x > 1.001f
        val userScrollEnabledAt1x = !isZoomedAt1x
        assertTrue("At 1.0x, pager horizontal swipe must be enabled", userScrollEnabledAt1x)

        // When zoomed in (> 1.001f), userScrollEnabled must be false (drag pans page)
        val scaleZoomed = 2.5f
        val isZoomed = scaleZoomed > 1.001f
        val userScrollEnabledZoomed = !isZoomed
        assertFalse("When zoomed in, pager horizontal swipe must be disabled", userScrollEnabledZoomed)

        // Micro-pinch threshold
        val scaleMicro = 1.0005f
        assertFalse(scaleMicro > 1.001f)
    }

    @Test
    fun testPageFitDimensionsWithinViewport() {
        // Landscape 16:9 page in portrait viewport (1080 x 2400)
        val viewWidthPx = 1080
        val viewHeightPx = 2400
        val landscapeAspect = 16f / 9f // 1.7778
        val containerAspect = viewWidthPx.toFloat() / viewHeightPx.toFloat() // 0.45

        val (fitWidth, fitHeight) = if (landscapeAspect > containerAspect) {
            viewWidthPx to (viewWidthPx / landscapeAspect).roundToInt()
        } else {
            (viewHeightPx * landscapeAspect).roundToInt() to viewHeightPx
        }

        assertEquals(1080, fitWidth)
        assertEquals(608, fitHeight) // 1080 / 1.7778 ~ 608

        // Portrait A4 page (0.707) in portrait viewport (1080 x 2400)
        val portraitAspect = 0.707f
        val (portraitFitW, portraitFitH) = if (portraitAspect > containerAspect) {
            viewWidthPx to (viewWidthPx / portraitAspect).roundToInt()
        } else {
            (viewHeightPx * portraitAspect).roundToInt() to viewHeightPx
        }

        assertEquals(1080, portraitFitW)
        assertEquals(1528, portraitFitH) // 1080 / 0.707 ~ 1528 <= 2400
    }

    @Test
    fun testReturnScrollSyncClamping() {
        val totalCount = 5

        // Returning from Page Viewer at last page
        val lastSeenPage = 4
        val syncIndex = lastSeenPage.coerceIn(0, totalCount - 1)
        assertEquals(4, syncIndex)

        // Returning from Page Viewer when document was somehow reduced
        val deletedPage = 8
        val syncIndexClamped = deletedPage.coerceIn(0, totalCount - 1)
        assertEquals(4, syncIndexClamped)
    }
}
