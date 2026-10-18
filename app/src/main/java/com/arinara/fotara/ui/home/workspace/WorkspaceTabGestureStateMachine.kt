// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.home.workspace

import com.arinara.fotara.data.model.Workspace
import com.arinara.fotara.data.model.WorkspaceKind
import kotlin.math.abs

enum class WorkspaceGestureState {
    IDLE,
    PRESSED,
    STAGE1_PANEL_OPEN,  // Tab lifted ~1.06x, action panel open
    STAGE2_MOVE_MODE,   // Tab elevated 1.1x, action panel faded out, direct drag ready
    DRAGGING,           // Actively moving horizontally
    SETTLING            // Finger lifted, animating to drop target slot
}

enum class WorkspaceHapticFeedbackSignal {
    NONE,
    STAGE1_LIFT,        // Standard long-press haptic
    STAGE2_MOVE_MODE,   // Stronger / distinct tick or pulse
    DRAG_REORDER_TICK,  // Slot switch tick
    DROP_SETTLE         // Final drop tick
}

data class WorkspaceGestureData(
    val state: WorkspaceGestureState = WorkspaceGestureState.IDLE,
    val activeTabId: Long? = null,
    val kind: WorkspaceKind = WorkspaceKind.CUSTOM,
    val startX: Float = 0f,
    val startY: Float = 0f,
    val currentX: Float = 0f,
    val currentY: Float = 0f,
    val dragDeltaX: Float = 0f,
    val dragDeltaY: Float = 0f,
    val pressTimestampMs: Long = 0L,
    val showActionPanel: Boolean = false,
    val panelAlpha: Float = 0f,
    val isElevatedForMove: Boolean = false,
    val isOverArchiveZone: Boolean = false,
    val hapticSignal: WorkspaceHapticFeedbackSignal = WorkspaceHapticFeedbackSignal.NONE
) {
    val isLifted: Boolean get() = state == WorkspaceGestureState.STAGE1_PANEL_OPEN ||
            state == WorkspaceGestureState.STAGE2_MOVE_MODE ||
            state == WorkspaceGestureState.DRAGGING ||
            state == WorkspaceGestureState.SETTLING

    val isDragging: Boolean get() = state == WorkspaceGestureState.DRAGGING
}

sealed class WorkspaceGestureEvent {
    data class Down(
        val tabId: Long,
        val kind: WorkspaceKind,
        val x: Float,
        val y: Float = 0f
    ) : WorkspaceGestureEvent()

    object AdvanceTime : WorkspaceGestureEvent()
    data class Move(
        val x: Float,
        val y: Float = 0f,
        val touchSlopPx: Float = 16f,
        val verticalCancelSlopPx: Float = 120f,
        val archiveZoneStartX: Float = Float.MAX_VALUE
    ) : WorkspaceGestureEvent()

    object Up : WorkspaceGestureEvent()
    object Cancel : WorkspaceGestureEvent()
    object DismissPanel : WorkspaceGestureEvent()
    object SettleFinished : WorkspaceGestureEvent()
}

class WorkspaceTabGestureStateMachine(
    private val clock: () -> Long = { System.currentTimeMillis() }
) {
    companion object {
        const val STAGE1_HOLD_MS = 400L
        const val STAGE2_TOTAL_HOLD_MS = 1000L // 400ms + 600ms
        const val PANEL_FADE_DURATION_MS = 120L
    }

    var current: WorkspaceGestureData = WorkspaceGestureData()
        private set

    fun processEvent(event: WorkspaceGestureEvent): WorkspaceGestureData {
        val now = clock()
        val next = when (event) {
            is WorkspaceGestureEvent.Down -> {
                if (event.kind == WorkspaceKind.HOME) {
                    // Home is pinned, no drag or hold
                    WorkspaceGestureData(state = WorkspaceGestureState.IDLE)
                } else {
                    WorkspaceGestureData(
                        state = WorkspaceGestureState.PRESSED,
                        activeTabId = event.tabId,
                        kind = event.kind,
                        startX = event.x,
                        startY = event.y,
                        currentX = event.x,
                        currentY = event.y,
                        pressTimestampMs = now,
                        hapticSignal = WorkspaceHapticFeedbackSignal.NONE
                    )
                }
            }

            is WorkspaceGestureEvent.AdvanceTime -> {
                when (current.state) {
                    WorkspaceGestureState.PRESSED -> {
                        val elapsed = now - current.pressTimestampMs
                        if (elapsed >= STAGE2_TOTAL_HOLD_MS) {
                            // Directly reached Stage 2
                            current.copy(
                                state = WorkspaceGestureState.STAGE2_MOVE_MODE,
                                showActionPanel = false,
                                panelAlpha = 0f,
                                isElevatedForMove = true,
                                hapticSignal = WorkspaceHapticFeedbackSignal.STAGE2_MOVE_MODE
                            )
                        } else if (elapsed >= STAGE1_HOLD_MS) {
                            // Stage 1 reached
                            val showPanel = current.kind == WorkspaceKind.CUSTOM
                            current.copy(
                                state = WorkspaceGestureState.STAGE1_PANEL_OPEN,
                                showActionPanel = showPanel,
                                panelAlpha = 1f,
                                isElevatedForMove = false,
                                hapticSignal = WorkspaceHapticFeedbackSignal.STAGE1_LIFT
                            )
                        } else {
                            current
                        }
                    }

                    WorkspaceGestureState.STAGE1_PANEL_OPEN -> {
                        val elapsed = now - current.pressTimestampMs
                        if (elapsed >= STAGE2_TOTAL_HOLD_MS) {
                            // Transition from Stage 1 to Stage 2: panel fades out, tab elevates
                            current.copy(
                                state = WorkspaceGestureState.STAGE2_MOVE_MODE,
                                showActionPanel = false,
                                panelAlpha = 0f,
                                isElevatedForMove = true,
                                hapticSignal = WorkspaceHapticFeedbackSignal.STAGE2_MOVE_MODE
                            )
                        } else {
                            current
                        }
                    }

                    else -> current
                }
            }

            is WorkspaceGestureEvent.Move -> {
                val deltaX = event.x - current.startX
                val deltaY = event.y - current.startY

                // Check vertical cancel slop
                if (abs(deltaY) > event.verticalCancelSlopPx) {
                    // Canceled due to vertical drag / shake
                    WorkspaceGestureData(state = WorkspaceGestureState.IDLE)
                } else {
                    when (current.state) {
                        WorkspaceGestureState.IDLE -> current
                        WorkspaceGestureState.PRESSED -> {
                            if (abs(deltaX) > event.touchSlopPx) {
                                // Scrolled tab bar before holding
                                WorkspaceGestureData(state = WorkspaceGestureState.IDLE)
                            } else {
                                current.copy(currentX = event.x, currentY = event.y)
                            }
                        }

                        WorkspaceGestureState.STAGE1_PANEL_OPEN -> {
                            if (abs(deltaX) > event.touchSlopPx) {
                                // Drag started from Stage 1: dismiss panel, enter DRAGGING
                                current.copy(
                                    state = WorkspaceGestureState.DRAGGING,
                                    showActionPanel = false,
                                    panelAlpha = 0f,
                                    isElevatedForMove = true,
                                    currentX = event.x,
                                    currentY = event.y,
                                    dragDeltaX = deltaX,
                                    dragDeltaY = deltaY,
                                    isOverArchiveZone = event.x >= event.archiveZoneStartX,
                                    hapticSignal = WorkspaceHapticFeedbackSignal.NONE
                                )
                            } else {
                                current.copy(currentX = event.x, currentY = event.y)
                            }
                        }

                        WorkspaceGestureState.STAGE2_MOVE_MODE,
                        WorkspaceGestureState.DRAGGING -> {
                            current.copy(
                                state = WorkspaceGestureState.DRAGGING,
                                showActionPanel = false,
                                panelAlpha = 0f,
                                isElevatedForMove = true,
                                currentX = event.x,
                                currentY = event.y,
                                dragDeltaX = deltaX,
                                dragDeltaY = deltaY,
                                isOverArchiveZone = event.x >= event.archiveZoneStartX,
                                hapticSignal = WorkspaceHapticFeedbackSignal.NONE
                            )
                        }

                        WorkspaceGestureState.SETTLING -> current
                    }
                }
            }

            is WorkspaceGestureEvent.Up -> {
                when (current.state) {
                    WorkspaceGestureState.STAGE1_PANEL_OPEN -> {
                        // User lifted finger in Stage 1: Action panel stays open!
                        current.copy(hapticSignal = WorkspaceHapticFeedbackSignal.NONE)
                    }

                    WorkspaceGestureState.STAGE2_MOVE_MODE,
                    WorkspaceGestureState.DRAGGING -> {
                        // Dropping: enters SETTLING with drop settle haptic
                        current.copy(
                            state = WorkspaceGestureState.SETTLING,
                            hapticSignal = WorkspaceHapticFeedbackSignal.DROP_SETTLE
                        )
                    }

                    else -> WorkspaceGestureData(state = WorkspaceGestureState.IDLE)
                }
            }

            is WorkspaceGestureEvent.Cancel -> {
                WorkspaceGestureData(state = WorkspaceGestureState.IDLE)
            }

            is WorkspaceGestureEvent.DismissPanel -> {
                WorkspaceGestureData(state = WorkspaceGestureState.IDLE)
            }

            is WorkspaceGestureEvent.SettleFinished -> {
                WorkspaceGestureData(state = WorkspaceGestureState.IDLE)
            }
        }
        current = next
        return next
    }
}
