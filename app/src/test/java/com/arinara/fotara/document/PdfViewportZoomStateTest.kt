// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.document

import com.arinara.fotara.ui.document.PdfViewportZoomState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfViewportZoomStateTest {

    @Test
    fun testInitialState() {
        val state = PdfViewportZoomState()
        assertEquals(1.0f, state.scale, 0.001f)
        assertEquals(0.0f, state.panX, 0.001f)
        assertEquals(0.0f, state.panY, 0.001f)
        assertFalse(state.isZoomed)
    }

    @Test
    fun testScaleCoercionWithinLimits() {
        val state = PdfViewportZoomState(minScale = 1.0f, maxScale = 4.0f)
        state.updateViewport(1080f, 1920f)

        // Pinch in beyond maxScale
        state.onPinch(zoomChange = 6.0f)
        assertEquals(4.0f, state.scale, 0.001f)
        assertTrue(state.isZoomed)

        // Pinch out beyond minScale
        state.onPinch(zoomChange = 0.05f)
        assertEquals(1.0f, state.scale, 0.001f)
        assertFalse(state.isZoomed)
        assertEquals(0.0f, state.panX, 0.001f)
        assertEquals(0.0f, state.panY, 0.001f)
    }

    @Test
    fun testMaxPanCalculation() {
        assertEquals(0.0f, PdfViewportZoomState.calculateMaxPan(1.0f, 1000f), 0.001f)
        assertEquals(0.0f, PdfViewportZoomState.calculateMaxPan(0.9f, 1000f), 0.001f)
        assertEquals(0.0f, PdfViewportZoomState.calculateMaxPan(2.0f, 0f), 0.001f)

        // At 2.0x on 1000px viewport, content is 2000px wide -> max pan is 500px each way
        assertEquals(500f, PdfViewportZoomState.calculateMaxPan(2.0f, 1000f), 0.001f)

        // At 3.0x on 1000px viewport -> max pan is 1000px
        assertEquals(1000f, PdfViewportZoomState.calculateMaxPan(3.0f, 1000f), 0.001f)
    }

    @Test
    fun testPanGatedByZoom() {
        val state = PdfViewportZoomState()
        state.updateViewport(1000f, 2000f)

        // Not zoomed: panning is rejected
        state.onPan(dx = 100f, dy = 100f)
        assertEquals(0.0f, state.panX, 0.001f)
        assertEquals(0.0f, state.panY, 0.001f)

        // Zoom in to 2.0x
        state.onPinch(zoomChange = 2.0f)
        assertTrue(state.isZoomed)

        // Now panning is accepted and clamped
        state.onPan(dx = 200f, dy = 300f)
        assertEquals(200f, state.panX, 0.001f)
        assertEquals(300f, state.panY, 0.001f)

        // Pan beyond max boundary: maxPanX is (1000 * 1.0)/2 = 500
        state.onPan(dx = 500f, dy = 0f)
        assertEquals(500f, state.panX, 0.001f)
    }

    @Test
    fun testDoubleTapTogglesZoomAndReset() {
        val state = PdfViewportZoomState(doubleTapScale = 2.5f)
        state.updateViewport(1000f, 2000f)

        // First double tap: zooms in to 2.5x
        state.onDoubleTap(tapX = 500f, tapY = 1000f) // Tap dead center
        assertEquals(2.5f, state.scale, 0.001f)
        assertTrue(state.isZoomed)
        assertEquals(0.0f, state.panX, 0.001f)
        assertEquals(0.0f, state.panY, 0.001f)

        // Second double tap while zoomed: resets to 1.0x
        state.onDoubleTap(tapX = 500f, tapY = 1000f)
        assertEquals(1.0f, state.scale, 0.001f)
        assertFalse(state.isZoomed)
        assertEquals(0.0f, state.panX, 0.001f)
        assertEquals(0.0f, state.panY, 0.001f)
    }

    @Test
    fun testDoubleTapOffsetsTowardsTapPoint() {
        val state = PdfViewportZoomState(doubleTapScale = 2.0f)
        state.updateViewport(1000f, 2000f)

        // Tap on right side (x = 750, center is 500)
        state.onDoubleTap(tapX = 750f, tapY = 1000f)
        assertEquals(2.0f, state.scale, 0.001f)
        // targetPanX = (500 - 750) * (2.0 - 1.0) = -250f
        assertEquals(-250f, state.panX, 0.001f)
    }

    @Test
    fun testResetClearsOffsetsAndScale() {
        val state = PdfViewportZoomState()
        state.updateViewport(1000f, 2000f)
        state.onPinch(zoomChange = 3.0f)
        state.onPan(dx = 100f, dy = 100f)

        state.reset()
        assertEquals(1.0f, state.scale, 0.001f)
        assertEquals(0.0f, state.panX, 0.001f)
        assertEquals(0.0f, state.panY, 0.001f)
        assertFalse(state.isZoomed)
    }

    @Test
    fun testExitReadingModeReset() {
        val state = PdfViewportZoomState()
        state.updateViewport(1000f, 2000f)
        state.onPinch(zoomChange = 2.5f)
        state.onPan(dx = 150f, dy = 200f)
        assertTrue(state.isZoomed)

        // Reset resets zoom scale and pan back to default 1.0x
        state.reset()
        assertEquals(1.0f, state.scale, 0.001f)
        assertEquals(0.0f, state.panX, 0.001f)
        assertEquals(0.0f, state.panY, 0.001f)
        assertFalse(state.isZoomed)
    }
}
