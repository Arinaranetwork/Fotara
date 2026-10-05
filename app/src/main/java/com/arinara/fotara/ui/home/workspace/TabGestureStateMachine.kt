// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.home.workspace

import com.arinara.fotara.data.model.Workspace
import com.arinara.fotara.data.model.WorkspaceKind

enum class TabGestureState {
    IDLE,
    PRESSED,
    HELD,      // Lifted; options panel open (if custom tab)
    MOVE_MODE, // Stage 2 hold reached (~1000ms): panel retracted, ready for immediate drag
    DRAGGING,  // Dragging horizontally
    SETTLING   // Finger lifted, animating to drop slot
}

sealed class TabGestureEvent {
    data class Down(
        val tabId: Long,
        val isHome: Boolean = false,
        val isArchive: Boolean = false,
        val initialX: Float = 0f
    ) : TabGestureEvent() {
        constructor(tabId: Long, kind: WorkspaceKind, x: Float = 0f) : this(
            tabId = tabId,
            isHome = kind == WorkspaceKind.HOME,
            isArchive = kind == WorkspaceKind.ARCHIVE,
            initialX = x
        )
    }
    object LongPressTimeout : TabGestureEvent()
    object MoveModeTimeout : TabGestureEvent()
    data class Move(val currentX: Float, val touchSlopPx: Float) : TabGestureEvent()
    object Up : TabGestureEvent()
    object Cancel : TabGestureEvent()
    object DismissPanel : TabGestureEvent()
}

data class TabDragState(
    val state: TabGestureState = TabGestureState.IDLE,
    val activeTabId: Long? = null,
    val isHome: Boolean = false,
    val isArchive: Boolean = false,
    val startX: Float = 0f,
    val startY: Float = 0f,
    val currentX: Float = 0f,
    val currentY: Float = 0f,
    val dragDeltaX: Float = 0f,
    val dragDeltaY: Float = 0f,
    val showRenamePanel: Boolean = false,
    val isOverArchiveZone: Boolean = false
) {
    val isLifted: Boolean get() = state == TabGestureState.HELD || state == TabGestureState.MOVE_MODE || state == TabGestureState.DRAGGING || state == TabGestureState.SETTLING
    val isDragging: Boolean get() = state == TabGestureState.DRAGGING
    val isMoveMode: Boolean get() = state == TabGestureState.MOVE_MODE
}

object TabGestureReducer {
    fun reduce(current: TabDragState, event: TabGestureEvent): TabDragState {
        return when (event) {
            is TabGestureEvent.Down -> {
                if (event.isHome) {
                    TabDragState(state = TabGestureState.IDLE)
                } else {
                    TabDragState(
                        state = TabGestureState.PRESSED,
                        activeTabId = event.tabId,
                        isHome = event.isHome,
                        isArchive = event.isArchive,
                        startX = event.initialX,
                        currentX = event.initialX,
                        dragDeltaX = 0f,
                        showRenamePanel = false
                    )
                }
            }
            is TabGestureEvent.LongPressTimeout -> {
                if (current.state != TabGestureState.PRESSED) return current
                // Rule: Long-press on Home does nothing (no haptic, no panel, no drag)
                if (current.isHome) {
                    return TabDragState(state = TabGestureState.IDLE)
                }
                // Rule: Long-press on Archive: haptic and lift, options panel appears with Delete
                if (current.isArchive) {
                    return current.copy(
                        state = TabGestureState.HELD,
                        showRenamePanel = true
                    )
                }
                // Custom tab: lifts and anchor panel appears
                return current.copy(
                    state = TabGestureState.HELD,
                    showRenamePanel = true
                )
            }
            is TabGestureEvent.MoveModeTimeout -> {
                if (current.state != TabGestureState.HELD && current.state != TabGestureState.PRESSED) return current
                if (current.isHome) return TabDragState(state = TabGestureState.IDLE)
                return current.copy(
                    state = TabGestureState.MOVE_MODE,
                    showRenamePanel = false
                )
            }
            is TabGestureEvent.Move -> {
                when (current.state) {
                    TabGestureState.IDLE -> current
                    TabGestureState.PRESSED -> {
                        val delta = kotlin.math.abs(event.currentX - current.startX)
                        if (delta > event.touchSlopPx) {
                            TabDragState(state = TabGestureState.IDLE)
                        } else {
                            current.copy(currentX = event.currentX)
                        }
                    }
                    TabGestureState.HELD -> {
                        val delta = kotlin.math.abs(event.currentX - current.startX)
                        if (delta > event.touchSlopPx) {
                            // Panel disappears immediately and drag-reorder starts
                            current.copy(
                                state = TabGestureState.DRAGGING,
                                showRenamePanel = false,
                                currentX = event.currentX,
                                dragDeltaX = event.currentX - current.startX
                            )
                        } else {
                            current.copy(currentX = event.currentX)
                        }
                    }
                    TabGestureState.MOVE_MODE,
                    TabGestureState.DRAGGING -> {
                        current.copy(
                            state = TabGestureState.DRAGGING,
                            currentX = event.currentX,
                            dragDeltaX = event.currentX - current.startX
                        )
                    }
                    TabGestureState.SETTLING -> current
                }
            }
            is TabGestureEvent.Up -> {
                when (current.state) {
                    TabGestureState.HELD -> {
                        if (current.isArchive) {
                            TabDragState(state = TabGestureState.IDLE)
                        } else {
                            // Panel stays open on custom tab until dismissed
                            current.copy(state = TabGestureState.HELD, showRenamePanel = true)
                        }
                    }
                    TabGestureState.MOVE_MODE,
                    TabGestureState.DRAGGING -> {
                        current.copy(state = TabGestureState.IDLE, showRenamePanel = false)
                    }
                    else -> TabDragState(state = TabGestureState.IDLE)
                }
            }
            is TabGestureEvent.Cancel -> {
                TabDragState(state = TabGestureState.IDLE)
            }
            is TabGestureEvent.DismissPanel -> {
                TabDragState(state = TabGestureState.IDLE)
            }
        }
    }
}

object WorkspaceReorderHelper {
    fun clampDropSlot(targetSlot: Int, listSize: Int): Int {
        if (listSize <= 1) return 0
        return targetSlot.coerceIn(1, listSize - 1)
    }

    fun reorderList(
        workspaces: List<Workspace>,
        movingWorkspaceId: Long,
        targetSlot: Int
    ): List<Workspace> {
        val fromIndex = workspaces.indexOfFirst { it.id == movingWorkspaceId }
        if (fromIndex <= 0 || workspaces.size <= 2) return workspaces
        val clamped = clampDropSlot(targetSlot, workspaces.size)
        if (fromIndex == clamped) return workspaces

        val mutable = workspaces.toMutableList()
        val item = mutable.removeAt(fromIndex)
        mutable.add(clamped, item)
        return mutable.mapIndexed { idx, ws -> ws.copy(position = idx) }
    }
}
