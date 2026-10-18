// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ViewportTransformTest {

    @Test
    fun testWorldToScreenAndScreenToWorldRoundTrip() {
        val viewport = ViewportState(scale = 2.5f, translateX = 100f, translateY = -50f)
        val worldX = 42.5f
        val worldY = 88.0f

        val (screenX, screenY) = ViewportTransform.worldToScreen(worldX, worldY, viewport)
        val (roundtripWorldX, roundtripWorldY) = ViewportTransform.screenToWorld(screenX, screenY, viewport)

        assertEquals(worldX, roundtripWorldX, 0.001f)
        assertEquals(worldY, roundtripWorldY, 0.001f)
    }

    @Test
    fun testZoomAboutFocalPoint_PreservesWorldCoordinateUnderFocalPoint() {
        val initialViewport = ViewportState(scale = 1.0f, translateX = 0f, translateY = 0f)
        val focalScreenX = 540f
        val focalScreenY = 960f

        val (initialWorldX, initialWorldY) = ViewportTransform.screenToWorld(focalScreenX, focalScreenY, initialViewport)

        // Zoom in 2.0x about the focal point
        val zoomedViewport = ViewportTransform.zoomAboutFocalPoint(
            viewport = initialViewport,
            focalScreenX = focalScreenX,
            focalScreenY = focalScreenY,
            zoomDelta = 2.0f
        )

        assertEquals(2.0f, zoomedViewport.scale, 0.001f)

        val (newWorldX, newWorldY) = ViewportTransform.screenToWorld(focalScreenX, focalScreenY, zoomedViewport)
        assertEquals(initialWorldX, newWorldX, 0.001f)
        assertEquals(initialWorldY, newWorldY, 0.001f)
    }

    @Test
    fun testZoomLimits_ClampsToSensibleMinAndMax() {
        val viewport = ViewportState(scale = 1.0f, translateX = 0f, translateY = 0f)

        // Attempt excessive zoom out (e.g. 0.0001x)
        val zoomedOut = ViewportTransform.zoomAboutFocalPoint(
            viewport = viewport,
            focalScreenX = 100f,
            focalScreenY = 100f,
            zoomDelta = 0.001f
        )
        assertEquals(ViewportTransform.MIN_ZOOM, zoomedOut.scale, 0.0001f)

        // Attempt excessive zoom in (e.g. 1000x)
        val zoomedIn = ViewportTransform.zoomAboutFocalPoint(
            viewport = viewport,
            focalScreenX = 100f,
            focalScreenY = 100f,
            zoomDelta = 1000f
        )
        assertEquals(ViewportTransform.MAX_ZOOM, zoomedIn.scale, 0.0001f)
    }

    @Test
    fun testPan_TranslatesCorrectly() {
        val viewport = ViewportState(scale = 1.5f, translateX = 50f, translateY = 50f)
        val panned = ViewportTransform.pan(viewport, deltaScreenX = 20f, deltaScreenY = -30f)

        assertEquals(1.5f, panned.scale, 0.001f)
        assertEquals(70f, panned.translateX, 0.001f)
        assertEquals(20f, panned.translateY, 0.001f)
    }

    @Test
    fun testFitToContent_CentersAndScalesProperly() {
        val contentBounds = CanvasRect(left = 0f, top = 0f, right = 1000f, bottom = 2000f)
        val screenW = 1080f
        val screenH = 2400f
        val padding = 40f

        val fitted = ViewportTransform.fitToContent(contentBounds, screenW, screenH, padding)

        assertTrue(fitted.scale > 0f)
        assertFalse(fitted.scale.isNaN())
        assertFalse(fitted.translateX.isNaN())
        assertFalse(fitted.translateY.isNaN())

        // The center of content in screen space should coincide with center of screen
        val (screenCenterX, screenCenterY) = ViewportTransform.worldToScreen(
            contentBounds.centerX,
            contentBounds.centerY,
            fitted
        )
        assertEquals(screenW / 2f, screenCenterX, 0.5f)
        assertEquals(screenH / 2f, screenCenterY, 0.5f)
    }

    @Test
    fun testDegenerateInputs_NeverThrowAndReturnSafeValues() {
        // Zero scale
        val zeroScaleViewport = ViewportState(scale = 0.0f, translateX = 10f, translateY = 10f)
        val (sx, sy) = ViewportTransform.worldToScreen(10f, 10f, zeroScaleViewport)
        assertFalse(sx.isNaN())
        assertFalse(sy.isNaN())

        val (wx, wy) = ViewportTransform.screenToWorld(10f, 10f, zeroScaleViewport)
        assertFalse(wx.isNaN())
        assertFalse(wy.isNaN())

        // NaN inputs
        val nanViewport = ViewportState(scale = Float.NaN, translateX = Float.NaN, translateY = Float.NaN)
        val (nanSx, nanSy) = ViewportTransform.worldToScreen(Float.NaN, Float.NaN, nanViewport)
        assertFalse(nanSx.isNaN())
        assertFalse(nanSy.isNaN())

        // Infinity inputs
        val infViewport = ViewportState(scale = Float.POSITIVE_INFINITY, translateX = Float.POSITIVE_INFINITY, translateY = 0f)
        val panned = ViewportTransform.pan(infViewport, Float.NaN, Float.POSITIVE_INFINITY)
        assertFalse(panned.scale.isNaN())
        assertFalse(panned.translateX.isNaN())
        assertFalse(panned.translateY.isNaN())

        // Empty content bounds in fitToContent
        val fittedEmpty = ViewportTransform.fitToContent(CanvasRect.Empty, 1080f, 1920f)
        assertEquals(1.0f, fittedEmpty.scale, 0.001f)
    }
}
