// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.workspace

import com.arinara.fotara.data.model.WorkspaceKind
import com.arinara.fotara.ui.home.workspace.WorkspaceGestureEvent
import com.arinara.fotara.ui.home.workspace.WorkspaceGestureState
import com.arinara.fotara.ui.home.workspace.WorkspaceHapticFeedbackSignal
import com.arinara.fotara.ui.home.workspace.WorkspaceTabGestureStateMachine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceTabGestureStateMachineTest {

    private var currentTime = 0L
    private val stateMachine = WorkspaceTabGestureStateMachine(clock = { currentTime })

    @Test
    fun tapBelowHoldThreshold_selectsTabAndReturnsToIdleOnUp() {
        currentTime = 1000L
        stateMachine.processEvent(
            WorkspaceGestureEvent.Down(tabId = 5L, kind = WorkspaceKind.CUSTOM, x = 120f)
        )
        assertEquals(WorkspaceGestureState.PRESSED, stateMachine.current.state)
        assertFalse(stateMachine.current.showActionPanel)
        assertFalse(stateMachine.current.isElevatedForMove)

        // Advance 200ms (< 400ms)
        currentTime = 1200L
        stateMachine.processEvent(WorkspaceGestureEvent.AdvanceTime)
        assertEquals(WorkspaceGestureState.PRESSED, stateMachine.current.state)

        // Finger lifts
        stateMachine.processEvent(WorkspaceGestureEvent.Up)
        assertEquals(WorkspaceGestureState.IDLE, stateMachine.current.state)
        assertFalse(stateMachine.current.showActionPanel)
    }

    @Test
    fun stage1Hold_liftsTabAndOpensPanel_liftKeepsPanelOpen() {
        currentTime = 1000L
        stateMachine.processEvent(
            WorkspaceGestureEvent.Down(tabId = 5L, kind = WorkspaceKind.CUSTOM, x = 120f)
        )

        // Advance to 400ms -> Stage 1 triggered
        currentTime = 1400L
        stateMachine.processEvent(WorkspaceGestureEvent.AdvanceTime)

        assertEquals(WorkspaceGestureState.STAGE1_PANEL_OPEN, stateMachine.current.state)
        assertTrue(stateMachine.current.showActionPanel)
        assertEquals(1f, stateMachine.current.panelAlpha, 0.01f)
        assertFalse(stateMachine.current.isElevatedForMove)
        assertEquals(WorkspaceHapticFeedbackSignal.STAGE1_LIFT, stateMachine.current.hapticSignal)

        // Finger up at this stage preserves panel open (existing behavior)
        stateMachine.processEvent(WorkspaceGestureEvent.Up)
        assertEquals(WorkspaceGestureState.STAGE1_PANEL_OPEN, stateMachine.current.state)
        assertTrue(stateMachine.current.showActionPanel)
    }

    @Test
    fun stage2Hold_continuesPast1000ms_entersDirectMoveMode() {
        currentTime = 1000L
        stateMachine.processEvent(
            WorkspaceGestureEvent.Down(tabId = 5L, kind = WorkspaceKind.CUSTOM, x = 120f)
        )

        // Stage 1 at 400ms
        currentTime = 1400L
        stateMachine.processEvent(WorkspaceGestureEvent.AdvanceTime)
        assertEquals(WorkspaceGestureState.STAGE1_PANEL_OPEN, stateMachine.current.state)
        assertTrue(stateMachine.current.showActionPanel)

        // Continue holding past 1000ms total (+600ms = 2000ms)
        currentTime = 2000L
        stateMachine.processEvent(WorkspaceGestureEvent.AdvanceTime)

        assertEquals(WorkspaceGestureState.STAGE2_MOVE_MODE, stateMachine.current.state)
        assertFalse("Panel must fade out in Move Mode", stateMachine.current.showActionPanel)
        assertEquals(0f, stateMachine.current.panelAlpha, 0.01f)
        assertTrue("Tab must elevate for direct move mode", stateMachine.current.isElevatedForMove)
        assertEquals(WorkspaceHapticFeedbackSignal.STAGE2_MOVE_MODE, stateMachine.current.hapticSignal)

        // Dragging follows immediately without lifting finger
        stateMachine.processEvent(
            WorkspaceGestureEvent.Move(x = 180f, y = 0f, touchSlopPx = 16f, archiveZoneStartX = 300f)
        )
        assertEquals(WorkspaceGestureState.DRAGGING, stateMachine.current.state)
        assertEquals(60f, stateMachine.current.dragDeltaX, 0.01f)
        assertFalse(stateMachine.current.isOverArchiveZone)

        // Drag over Archive zone
        stateMachine.processEvent(
            WorkspaceGestureEvent.Move(x = 320f, y = 0f, touchSlopPx = 16f, archiveZoneStartX = 300f)
        )
        assertTrue("Should detect drag over archive zone", stateMachine.current.isOverArchiveZone)

        // Dropping enters SETTLING
        stateMachine.processEvent(WorkspaceGestureEvent.Up)
        assertEquals(WorkspaceGestureState.SETTLING, stateMachine.current.state)
        assertEquals(WorkspaceHapticFeedbackSignal.DROP_SETTLE, stateMachine.current.hapticSignal)

        // Animation completion returns to IDLE
        stateMachine.processEvent(WorkspaceGestureEvent.SettleFinished)
        assertEquals(WorkspaceGestureState.IDLE, stateMachine.current.state)
    }

    @Test
    fun verticalDragBeyondCancelSlop_cancelsReorderToIdle() {
        currentTime = 1000L
        stateMachine.processEvent(
            WorkspaceGestureEvent.Down(tabId = 5L, kind = WorkspaceKind.CUSTOM, x = 120f, y = 50f)
        )
        currentTime = 2000L
        stateMachine.processEvent(WorkspaceGestureEvent.AdvanceTime)
        assertEquals(WorkspaceGestureState.STAGE2_MOVE_MODE, stateMachine.current.state)

        // Drag vertically by 150px (> verticalCancelSlopPx = 120f)
        stateMachine.processEvent(
            WorkspaceGestureEvent.Move(x = 130f, y = 205f, touchSlopPx = 16f, verticalCancelSlopPx = 120f)
        )

        assertEquals(WorkspaceGestureState.IDLE, stateMachine.current.state)
        assertFalse(stateMachine.current.isElevatedForMove)
    }

    @Test
    fun homeTabIsPinned_cannotBeHeldOrReordered() {
        currentTime = 1000L
        stateMachine.processEvent(
            WorkspaceGestureEvent.Down(tabId = 1L, kind = WorkspaceKind.HOME, x = 30f)
        )
        assertEquals(WorkspaceGestureState.IDLE, stateMachine.current.state)

        currentTime = 2000L
        stateMachine.processEvent(WorkspaceGestureEvent.AdvanceTime)
        assertEquals(WorkspaceGestureState.IDLE, stateMachine.current.state)
    }

    @Test
    fun archiveTabStage1_liftsWithoutPanel() {
        currentTime = 1000L
        stateMachine.processEvent(
            WorkspaceGestureEvent.Down(tabId = 2L, kind = WorkspaceKind.ARCHIVE, x = 200f)
        )
        currentTime = 1400L
        stateMachine.processEvent(WorkspaceGestureEvent.AdvanceTime)

        assertEquals(WorkspaceGestureState.STAGE1_PANEL_OPEN, stateMachine.current.state)
        assertFalse("Archive tab must not show rename/delete options panel", stateMachine.current.showActionPanel)
    }
}
