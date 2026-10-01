// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.gesture

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PointerStateMachineTest {

    @Test
    fun testSingleFingerDraw_StartsAndFinishesStroke() {
        val sm = PointerStateMachine(activeMode = ActiveMode.DRAW)

        // Down
        val downActions = sm.processEvent(PointerInputEvent.Down(pointerId = 1, x = 100f, y = 100f))
        assertEquals(1, downActions.size)
        assertTrue(downActions.first() is PointerAction.StartStroke)
        assertTrue(sm.state is PointerState.Drawing)

        // Move
        val moveActions = sm.processEvent(PointerInputEvent.Move(pointerId = 1, x = 110f, y = 110f))
        assertEquals(1, moveActions.size)
        assertTrue(moveActions.first() is PointerAction.AppendPoints)

        // Up
        val upActions = sm.processEvent(PointerInputEvent.Up(pointerId = 1, x = 110f, y = 110f))
        assertEquals(1, upActions.size)
        assertTrue(upActions.first() is PointerAction.FinishStroke)
        assertTrue(sm.state is PointerState.Idle)
    }

    @Test
    fun testDrawToPanTransition_SecondFingerCancelsStrokeAndSwitchesToPanZoom() {
        val sm = PointerStateMachine(activeMode = ActiveMode.DRAW)

        // Finger 1 draws
        sm.processEvent(PointerInputEvent.Down(pointerId = 1, x = 100f, y = 100f))
        sm.processEvent(PointerInputEvent.Move(pointerId = 1, x = 120f, y = 120f))
        assertTrue(sm.state is PointerState.Drawing)

        // Finger 2 lands while drawing
        val actions = sm.processEvent(PointerInputEvent.Down(pointerId = 2, x = 300f, y = 300f))

        // Must cancel the in-progress stroke without committing
        assertEquals(1, actions.size)
        assertTrue(actions.first() is PointerAction.CancelStroke)
        assertTrue(sm.state is PointerState.PanZoom)

        // Both fingers move -> produces pan/zoom delta
        val panActions = sm.processEvent(PointerInputEvent.Move(pointerId = 1, x = 130f, y = 130f))
        assertEquals(1, panActions.size)
        assertTrue(panActions.first() is PointerAction.PanZoomDelta)
    }

    @Test
    fun testMultiTouchEdgeCases_ThirdFingerIgnoredSafely() {
        val sm = PointerStateMachine(activeMode = ActiveMode.DRAW)

        // Two fingers pan/zoom
        sm.processEvent(PointerInputEvent.Down(pointerId = 1, x = 100f, y = 100f))
        sm.processEvent(PointerInputEvent.Down(pointerId = 2, x = 200f, y = 200f))
        assertTrue(sm.state is PointerState.PanZoom)

        // Third finger lands -> must not crash or leave PanZoom state
        val thirdActions = sm.processEvent(PointerInputEvent.Down(pointerId = 3, x = 500f, y = 500f))
        assertTrue(thirdActions.isEmpty())
        assertTrue(sm.state is PointerState.PanZoom)
    }

    @Test
    fun testUpWithoutDownAndCancel_HandledSafely() {
        val sm = PointerStateMachine(activeMode = ActiveMode.DRAW)

        // Stray Up event without prior Down
        val actions = sm.processEvent(PointerInputEvent.Up(pointerId = 99, x = 50f, y = 50f))
        assertTrue(actions.isEmpty())
        assertTrue(sm.state is PointerState.Idle)

        // Cancel mid-draw cancels stroke
        sm.processEvent(PointerInputEvent.Down(pointerId = 1, x = 50f, y = 50f))
        val cancelActions = sm.processEvent(PointerInputEvent.Cancel(pointerId = 1))
        assertEquals(1, cancelActions.size)
        assertTrue(cancelActions.first() is PointerAction.CancelStroke)
        assertTrue(sm.state is PointerState.Idle)
    }

    @Test
    fun testPalmRejection_RejectsLargeContactFingerTouchWhenStylusActive() {
        val sm = PointerStateMachine(
            activeMode = ActiveMode.DRAW,
            palmRejectionRadiusThreshold = 50.0f
        )

        // Stylus touch registered
        sm.processEvent(PointerInputEvent.Down(pointerId = 1, x = 100f, y = 100f, toolType = PointerToolType.STYLUS))
        sm.processEvent(PointerInputEvent.Up(pointerId = 1, x = 100f, y = 100f))

        // Large palm touch arrives
        val palmActions = sm.processEvent(
            PointerInputEvent.Down(
                pointerId = 2,
                x = 200f,
                y = 200f,
                toolType = PointerToolType.FINGER,
                touchMajor = 80.0f // Exceeds 50.0f threshold
            )
        )

        // Palm touch rejected completely
        assertTrue(palmActions.isEmpty())
        assertTrue(sm.state is PointerState.Idle)
    }

    @Test
    fun testStylusOnlyMode_FingerCannotDrawButCanPan() {
        val sm = PointerStateMachine(
            activeMode = ActiveMode.DRAW,
            stylusOnlyDrawing = true
        )

        // 1 finger touch with stylusOnlyDrawing = true -> does not start stroke
        val fingerActions = sm.processEvent(
            PointerInputEvent.Down(pointerId = 1, x = 50f, y = 50f, toolType = PointerToolType.FINGER)
        )
        assertTrue(fingerActions.isEmpty())
        assertTrue(sm.state is PointerState.Idle)

        // Stylus touch starts drawing
        val stylusActions = sm.processEvent(
            PointerInputEvent.Down(pointerId = 2, x = 50f, y = 50f, toolType = PointerToolType.STYLUS)
        )
        assertEquals(1, stylusActions.size)
        assertTrue(stylusActions.first() is PointerAction.StartStroke)
        assertTrue(sm.state is PointerState.Drawing)
    }
}
