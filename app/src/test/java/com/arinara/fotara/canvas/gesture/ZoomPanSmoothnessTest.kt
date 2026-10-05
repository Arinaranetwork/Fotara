// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.gesture

import com.arinara.fotara.canvas.engine.ViewportState
import com.arinara.fotara.canvas.engine.ViewportTransform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ZoomPanSmoothnessTest {

    @Test
    fun testFocalPointAnchoring_PreservesWorldCoordinateUnderFocalPoint() {
        val initialViewport = ViewportState(scale = 1.0f, translateX = 0f, translateY = 0f)
        val focalX = 300f
        val focalY = 400f

        val (worldXBefore, worldYBefore) = ViewportTransform.screenToWorld(focalX, focalY, initialViewport)

        // Zoom 2x about focal point (300, 400)
        val zoomedViewport = ViewportTransform.zoomAboutFocalPoint(
            viewport = initialViewport,
            focalScreenX = focalX,
            focalScreenY = focalY,
            zoomDelta = 2.0f
        )

        val (worldXAfter, worldYAfter) = ViewportTransform.screenToWorld(focalX, focalY, zoomedViewport)

        assertEquals("World X at focal point must remain stationary", worldXBefore, worldXAfter, 0.001f)
        assertEquals("World Y at focal point must remain stationary", worldYBefore, worldYAfter, 0.001f)
        assertEquals(2.0f, zoomedViewport.scale, 0.001f)
    }

    @Test
    fun testCentroidContinuityOnPointerCountChange() {
        val sm = PointerStateMachine(activeMode = ActiveMode.DRAW)

        // Finger 1 down at (100, 100), finger 2 down at (200, 200) -> initial center (150, 150)
        sm.processEvent(PointerInputEvent.Down(pointerId = 1, x = 100f, y = 100f))
        sm.processEvent(PointerInputEvent.Down(pointerId = 2, x = 200f, y = 200f))
        assertTrue(sm.state is PointerState.PanZoom)
        val panState1 = sm.state as PointerState.PanZoom
        assertEquals(150f, panState1.lastCenterX, 0.01f)
        assertEquals(150f, panState1.lastCenterY, 0.01f)

        // Finger 3 lands at (300, 300) -> must re-anchor centroid without generating jump delta
        val down3Actions = sm.processEvent(PointerInputEvent.Down(pointerId = 3, x = 300f, y = 300f))
        assertTrue("No delta dispatched on pointer down", down3Actions.isEmpty())
        assertTrue(sm.state is PointerState.PanZoom)

        // Finger 1 lifts -> remaining fingers (2 and 3) smoothly become primary tracked pointers
        val up1Actions = sm.processEvent(PointerInputEvent.Up(pointerId = 1, x = 100f, y = 100f))
        assertTrue("Lifting 1 finger with 2 remaining must not terminate to Idle or trigger stray fling", up1Actions.isEmpty())
        assertTrue(sm.state is PointerState.PanZoom)
        val panStateAfterLift = sm.state as PointerState.PanZoom
        assertEquals(2, panStateAfterLift.pointer1Id)
        assertEquals(3, panStateAfterLift.pointer2Id)
        assertEquals(250f, panStateAfterLift.lastCenterX, 0.01f)
        assertEquals(250f, panStateAfterLift.lastCenterY, 0.01f)
    }

    @Test
    fun testClampingAndFlingBounds() {
        val viewport = ViewportState(scale = 2.0f, translateX = -50000f, translateY = -50000f)
        val clamped = ViewportTransform.clampToExtent(viewport, screenWidth = 1080f, screenHeight = 1920f)

        assertTrue(clamped.translateX >= -25000f)
        assertTrue(clamped.translateY >= -25000f)
        assertTrue(clamped.scale in 0.1f..10.0f)
    }

    @Test
    fun testViewportPublishThrottlingLogic() {
        var publishedCount = 0
        var lastPublishedTime = -1L
        val intervalMs = 100L

        fun throttledPublish(currentTime: Long, force: Boolean = false) {
            if (force || lastPublishedTime < 0L || currentTime - lastPublishedTime >= intervalMs) {
                lastPublishedTime = currentTime
                publishedCount++
            }
        }

        // Simulate 60 frames in 100ms (1.6ms interval)
        for (frame in 0 until 60) {
            val t = (frame * 1.6).toLong()
            throttledPublish(t, force = false)
        }
        // At t=0 it publishes once. At t=96ms it should not have published second time yet
        assertEquals(1, publishedCount)

        // At t=105ms, it should publish second time
        throttledPublish(105L, force = false)
        assertEquals(2, publishedCount)

        // Settle event (force = true) publishes immediately
        throttledPublish(110L, force = true)
        assertEquals(3, publishedCount)
    }
}
