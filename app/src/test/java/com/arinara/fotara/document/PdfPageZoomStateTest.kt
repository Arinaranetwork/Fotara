// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.document

import androidx.compose.ui.geometry.Offset
import com.arinara.fotara.ui.document.PdfPageZoomState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfPageZoomStateTest {

    @Test
    fun testInitialState() {
        val state = PdfPageZoomState()
        assertEquals(1.0f, state.scale, 0.001f)
        assertEquals(0.0f, state.offsetX, 0.001f)
        assertEquals(0.0f, state.offsetY, 0.001f)
        assertFalse(state.isZoomed)
    }

    @Test
    fun testScaleCoercionWithinLimits() {
        val state = PdfPageZoomState(minScale = 1.0f, maxScale = 4.0f)

        // Pinch in beyond maxScale
        state.onPinch(zoomChange = 5.0f, panChange = Offset.Zero, viewWidth = 1080f, viewHeight = 1920f)
        assertEquals(4.0f, state.scale, 0.001f)
        assertTrue(state.isZoomed)

        // Pinch out beyond minScale
        state.onPinch(zoomChange = 0.1f, panChange = Offset.Zero, viewWidth = 1080f, viewHeight = 1920f)
        assertEquals(1.0f, state.scale, 0.001f)
        assertFalse(state.isZoomed)
    }

    @Test
    fun testCalculateMaxPan() {
        // At scale 1.0x, content exactly fits viewport, pan margin is 0
        assertEquals(0.0f, PdfPageZoomState.calculateMaxPan(1.0f, 1000f), 0.001f)
        assertEquals(0.0f, PdfPageZoomState.calculateMaxPan(0.8f, 1000f), 0.001f)
        assertEquals(0.0f, PdfPageZoomState.calculateMaxPan(2.0f, 0f), 0.001f)

        // At scale 2.0x, content is 2000px wide in a 1000px viewport.
        // Left and right overflow is 500px each. Max pan = (1000 * (2.0 - 1.0)) / 2 = 500px.
        assertEquals(500f, PdfPageZoomState.calculateMaxPan(2.0f, 1000f), 0.001f)

        // At scale 3.0x, content is 3000px wide in a 1000px viewport.
        // Max pan = (1000 * 2.0) / 2 = 1000px.
        assertEquals(1000f, PdfPageZoomState.calculateMaxPan(3.0f, 1000f), 0.001f)
    }

    @Test
    fun testPanGatedByZoom() {
        val state = PdfPageZoomState()

        // Panning while at 1.0x (unzoomed) must be ignored so vertical scroll can pass through
        state.onPan(Offset(50f, 50f), viewWidth = 1000f, viewHeight = 2000f)
        assertEquals(0.0f, state.offsetX, 0.001f)
        assertEquals(0.0f, state.offsetY, 0.001f)

        // Zoom in to 2.0x
        state.onPinch(zoomChange = 2.0f, panChange = Offset.Zero, viewWidth = 1000f, viewHeight = 2000f)
        assertTrue(state.isZoomed)

        // Now panning is enabled and clamped to bounds
        state.onPan(Offset(200f, -300f), viewWidth = 1000f, viewHeight = 2000f)
        assertEquals(200f, state.offsetX, 0.001f)
        assertEquals(-300f, state.offsetY, 0.001f)

        // Exceeding bounds clamps strictly to maxPan
        val maxPanX = PdfPageZoomState.calculateMaxPan(2.0f, 1000f) // 500f
        state.onPan(Offset(1000f, 0f), viewWidth = 1000f, viewHeight = 2000f)
        assertEquals(maxPanX, state.offsetX, 0.001f)
    }

    @Test
    fun testDoubleTapToggleAndReset() {
        val state = PdfPageZoomState(doubleTapScale = 2.5f)
        val viewportWidth = 1000f
        val viewportHeight = 1600f

        // Center double-tap
        val centerTap = Offset(viewportWidth / 2f, viewportHeight / 2f)
        state.onDoubleTap(centerTap, viewportWidth, viewportHeight)
        assertEquals(2.5f, state.scale, 0.001f)
        assertTrue(state.isZoomed)
        assertEquals(0.0f, state.offsetX, 0.001f)
        assertEquals(0.0f, state.offsetY, 0.001f)

        // Double-tap again while zoomed resets to 1.0x and 0 offset
        state.onDoubleTap(centerTap, viewportWidth, viewportHeight)
        assertEquals(1.0f, state.scale, 0.001f)
        assertFalse(state.isZoomed)
        assertEquals(0.0f, state.offsetX, 0.001f)
        assertEquals(0.0f, state.offsetY, 0.001f)
    }

    @Test
    fun testResetFunctionClearsAllTransformations() {
        val state = PdfPageZoomState()
        state.scale = 3.0f
        state.offsetX = 250f
        state.offsetY = -180f
        assertTrue(state.isZoomed)

        state.reset()
        assertEquals(1.0f, state.scale, 0.001f)
        assertEquals(0.0f, state.offsetX, 0.001f)
        assertEquals(0.0f, state.offsetY, 0.001f)
        assertFalse(state.isZoomed)
    }
}
