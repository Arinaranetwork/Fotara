// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.gesture

import com.arinara.fotara.canvas.engine.ViewportState
import com.arinara.fotara.canvas.engine.ViewportTransform
import com.arinara.fotara.canvas.model.StrokePoint
import kotlin.math.hypot

enum class PointerToolType {
    STYLUS,
    FINGER,
    MOUSE
}

enum class ActiveMode {
    DRAW,
    SELECT,
    ERASE
}

data class PointerPoint(
    val x: Float,
    val y: Float,
    val pressure: Float = 1.0f
)

sealed class PointerInputEvent {
    data class Down(
        val pointerId: Int,
        val x: Float,
        val y: Float,
        val pressure: Float = 1.0f,
        val toolType: PointerToolType = PointerToolType.FINGER,
        val touchMajor: Float = 0.0f
    ) : PointerInputEvent()

    data class Move(
        val pointerId: Int,
        val x: Float,
        val y: Float,
        val pressure: Float = 1.0f,
        val toolType: PointerToolType = PointerToolType.FINGER,
        val touchMajor: Float = 0.0f,
        val historicalPoints: List<PointerPoint> = emptyList()
    ) : PointerInputEvent()

    data class Up(
        val pointerId: Int,
        val x: Float,
        val y: Float
    ) : PointerInputEvent()

    data class Cancel(
        val pointerId: Int? = null
    ) : PointerInputEvent()
}

sealed class PointerState {
    object Idle : PointerState()

    data class Drawing(
        val pointerId: Int,
        val toolType: PointerToolType,
        val lastX: Float,
        val lastY: Float
    ) : PointerState()

    data class PanZoom(
        val pointer1Id: Int,
        val pointer2Id: Int,
        val lastCenterX: Float,
        val lastCenterY: Float,
        val lastDistance: Float
    ) : PointerState()

    data class Selecting(
        val pointerId: Int,
        val startX: Float,
        val startY: Float,
        val currentPoints: List<Pair<Float, Float>>,
        val isLasso: Boolean = false
    ) : PointerState()

    data class Transforming(
        val pointerId: Int,
        val handleId: Int, // 0..7 handles, 8 = rotate, -1 = body drag
        val startScreenX: Float,
        val startScreenY: Float,
        val lastScreenX: Float,
        val lastScreenY: Float
    ) : PointerState()
}

sealed class PointerAction {
    data class StartStroke(val screenX: Float, val screenY: Float, val pressure: Float) : PointerAction()
    data class AppendPoints(val points: List<PointerPoint>) : PointerAction()
    object FinishStroke : PointerAction()
    object CancelStroke : PointerAction()
    data class EraseAt(val screenX: Float, val screenY: Float) : PointerAction()
    data class StartEraser(val screenX: Float, val screenY: Float) : PointerAction()
    data class EraseSweep(val points: List<PointerPoint>) : PointerAction()
    object FinishEraser : PointerAction()
    object CancelEraser : PointerAction()

    data class PanZoomDelta(
        val deltaScreenX: Float,
        val deltaScreenY: Float,
        val zoomFactor: Float,
        val focalScreenX: Float,
        val focalScreenY: Float
    ) : PointerAction()

    data class TapSelect(val screenX: Float, val screenY: Float) : PointerAction()
    data class LassoProgress(val polygon: List<Pair<Float, Float>>) : PointerAction()
    data class LassoComplete(val polygon: List<Pair<Float, Float>>) : PointerAction()

    data class StartTransform(val handleId: Int, val screenX: Float, val screenY: Float) : PointerAction()
    data class TransformDelta(
        val handleId: Int,
        val deltaScreenX: Float,
        val deltaScreenY: Float,
        val totalDeltaX: Float = deltaScreenX,
        val totalDeltaY: Float = deltaScreenY,
        val currentScreenX: Float = 0f,
        val currentScreenY: Float = 0f
    ) : PointerAction()
    data class FinishTransform(
        val handleId: Int = -1,
        val totalDeltaX: Float = 0f,
        val totalDeltaY: Float = 0f
    ) : PointerAction()
    object CancelTransform : PointerAction()

    data class Fling(val velocityX: Float, val velocityY: Float) : PointerAction()
}

/**
 * Pure Kotlin pointer state machine managing input transitions and palm rejection.
 * Guaranteed to never throw under any combination of concurrent pointers, up-without-down,
 * or unexpected cancellation events.
 */
class PointerStateMachine(
    var activeMode: ActiveMode = ActiveMode.DRAW,
    var stylusOnlyDrawing: Boolean = false,
    var palmRejectionRadiusThreshold: Float = 60.0f
) {

    var state: PointerState = PointerState.Idle
        private set

    /**
     * Hit-test hook for active selection handles and bounding box body.
     * Returns handle index (0..7 for resize, 8 for rotation, -1 for body drag), or null for outside.
     */
    var onHitTestSelection: ((screenX: Float, screenY: Float) -> Int?)? = null

    // Active pointer tracking
    private val activePointers = mutableMapOf<Int, PointerPoint>()
    private var stylusDetected = false

    // Simple velocity tracker for smooth flings
    private var lastMoveTimeMs = 0L
    private var lastDeltaX = 0f
    private var lastDeltaY = 0f

    /**
     * Processes an incoming pointer event and returns a list of resulting actions.
     */
    fun processEvent(event: PointerInputEvent): List<PointerAction> {
        return try {
            when (event) {
                is PointerInputEvent.Down -> handleDown(event)
                is PointerInputEvent.Move -> handleMove(event)
                is PointerInputEvent.Up -> handleUp(event)
                is PointerInputEvent.Cancel -> handleCancel(event)
            }
        } catch (_: Exception) {
            state = PointerState.Idle
            activePointers.clear()
            listOf(PointerAction.CancelStroke)
        }
    }

    private fun handleDown(event: PointerInputEvent.Down): List<PointerAction> {
        val actions = mutableListOf<PointerAction>()

        if (event.toolType == PointerToolType.STYLUS) {
            stylusDetected = true
        }

        // Palm rejection: If stylus is active or detected, reject large-contact finger touches
        if (stylusDetected && event.toolType == PointerToolType.FINGER) {
            if (event.touchMajor > palmRejectionRadiusThreshold) {
                // Reject palm touch
                return emptyList()
            }
        }

        activePointers[event.pointerId] = PointerPoint(event.x, event.y, event.pressure)

        when (val current = state) {
            is PointerState.Idle -> {
                if (activePointers.size == 1) {
                    if (activeMode == ActiveMode.SELECT) {
                        // Check if an existing selection handle or body is hit
                        val hitHandle = onHitTestSelection?.invoke(event.x, event.y)
                        if (hitHandle != null) {
                            state = PointerState.Transforming(
                                pointerId = event.pointerId,
                                handleId = hitHandle,
                                startScreenX = event.x,
                                startScreenY = event.y,
                                lastScreenX = event.x,
                                lastScreenY = event.y
                            )
                            actions.add(PointerAction.StartTransform(hitHandle, event.x, event.y))
                            return actions
                        } else {
                            state = PointerState.Selecting(
                                pointerId = event.pointerId,
                                startX = event.x,
                                startY = event.y,
                                currentPoints = listOf(Pair(event.x, event.y))
                            )
                            return actions
                        }
                    } else if (activeMode == ActiveMode.ERASE) {
                        state = PointerState.Drawing(
                            pointerId = event.pointerId,
                            toolType = event.toolType,
                            lastX = event.x,
                            lastY = event.y
                        )
                        actions.add(PointerAction.StartEraser(event.x, event.y))
                        return actions
                    } else if (activeMode == ActiveMode.DRAW) {
                        if (stylusOnlyDrawing && event.toolType == PointerToolType.FINGER) {
                            // Finger pans 1:1 when stylus-only drawing is active
                            state = PointerState.PanZoom(
                                pointer1Id = event.pointerId,
                                pointer2Id = -1,
                                lastCenterX = event.x,
                                lastCenterY = event.y,
                                lastDistance = 1f
                            )
                            return emptyList()
                        }

                        state = PointerState.Drawing(
                            pointerId = event.pointerId,
                            toolType = event.toolType,
                            lastX = event.x,
                            lastY = event.y
                        )
                        actions.add(PointerAction.StartStroke(event.x, event.y, event.pressure))
                    }
                } else if (activePointers.size == 2) {
                    val pointerIds = activePointers.keys.toList()
                    val p1 = activePointers[pointerIds[0]]!!
                    val p2 = activePointers[pointerIds[1]]!!
                    val dist = hypot(p1.x - p2.x, p1.y - p2.y)
                    state = PointerState.PanZoom(
                        pointer1Id = pointerIds[0],
                        pointer2Id = pointerIds[1],
                        lastCenterX = (p1.x + p2.x) / 2f,
                        lastCenterY = (p1.y + p2.y) / 2f,
                        lastDistance = if (dist <= 0f) 1f else dist
                    )
                }
            }
            is PointerState.Drawing -> {
                // Second finger lands mid-gesture: cancel in-progress drawing/erasing without committing
                if (activeMode == ActiveMode.ERASE) {
                    actions.add(PointerAction.CancelEraser)
                }
                actions.add(PointerAction.CancelStroke)

                val pointerIds = activePointers.keys.toList()
                val p1 = activePointers[pointerIds[0]]!!
                val p2 = activePointers[pointerIds[1]]!!

                val dist = hypot(p1.x - p2.x, p1.y - p2.y)
                state = PointerState.PanZoom(
                    pointer1Id = pointerIds[0],
                    pointer2Id = pointerIds[1],
                    lastCenterX = (p1.x + p2.x) / 2f,
                    lastCenterY = (p1.y + p2.y) / 2f,
                    lastDistance = if (dist <= 0f) 1f else dist
                )
            }
            is PointerState.Selecting -> {
                // Second finger cancels in-progress lasso without selecting
                val pointerIds = activePointers.keys.toList()
                val p1 = activePointers[pointerIds[0]]!!
                val p2 = activePointers[pointerIds[1]]!!
                val dist = hypot(p1.x - p2.x, p1.y - p2.y)
                state = PointerState.PanZoom(
                    pointer1Id = pointerIds[0],
                    pointer2Id = pointerIds[1],
                    lastCenterX = (p1.x + p2.x) / 2f,
                    lastCenterY = (p1.y + p2.y) / 2f,
                    lastDistance = if (dist <= 0f) 1f else dist
                )
            }
            is PointerState.Transforming -> {
                // Second finger cancels in-progress transform without committing
                actions.add(PointerAction.CancelTransform)
                val pointerIds = activePointers.keys.toList()
                val p1 = activePointers[pointerIds[0]]!!
                val p2 = activePointers[pointerIds[1]]!!
                val dist = hypot(p1.x - p2.x, p1.y - p2.y)
                state = PointerState.PanZoom(
                    pointer1Id = pointerIds[0],
                    pointer2Id = pointerIds[1],
                    lastCenterX = (p1.x + p2.x) / 2f,
                    lastCenterY = (p1.y + p2.y) / 2f,
                    lastDistance = if (dist <= 0f) 1f else dist
                )
            }
            is PointerState.PanZoom -> {
                // 3rd or 4th finger ignored safely
            }
        }

        return actions
    }

    private fun handleMove(event: PointerInputEvent.Move): List<PointerAction> {
        val actions = mutableListOf<PointerAction>()

        if (!activePointers.containsKey(event.pointerId)) {
            // Unregistered pointer move (ignore safely)
            return emptyList()
        }

        activePointers[event.pointerId] = PointerPoint(event.x, event.y, event.pressure)

        when (val current = state) {
            is PointerState.Drawing -> {
                if (event.pointerId == current.pointerId) {
                    if (activeMode == ActiveMode.ERASE) {
                        val sweep = mutableListOf<PointerPoint>()
                        for (hp in event.historicalPoints) {
                            sweep.add(hp)
                        }
                        sweep.add(PointerPoint(event.x, event.y, event.pressure))
                        actions.add(PointerAction.EraseSweep(sweep))
                        state = current.copy(lastX = event.x, lastY = event.y)
                    } else {
                        val points = mutableListOf<PointerPoint>()
                        // Append high-frequency historical points if available
                        for (hp in event.historicalPoints) {
                            points.add(hp)
                        }
                        points.add(PointerPoint(event.x, event.y, event.pressure))
                        actions.add(PointerAction.AppendPoints(points))

                        state = current.copy(lastX = event.x, lastY = event.y)
                    }
                }
            }
            is PointerState.PanZoom -> {
                if (current.pointer2Id == -1) {
                    // 1-finger pan
                    val p1 = activePointers[current.pointer1Id]
                    if (p1 != null) {
                        val deltaX = p1.x - current.lastCenterX
                        val deltaY = p1.y - current.lastCenterY
                        lastDeltaX = deltaX
                        lastDeltaY = deltaY
                        lastMoveTimeMs = System.currentTimeMillis()

                        actions.add(
                            PointerAction.PanZoomDelta(
                                deltaScreenX = deltaX,
                                deltaScreenY = deltaY,
                                zoomFactor = 1.0f,
                                focalScreenX = p1.x,
                                focalScreenY = p1.y
                            )
                        )

                        state = current.copy(
                            lastCenterX = p1.x,
                            lastCenterY = p1.y
                        )
                    }
                } else {
                    val p1 = activePointers[current.pointer1Id]
                    val p2 = activePointers[current.pointer2Id]

                    if (p1 != null && p2 != null) {
                        val currentCenterX = (p1.x + p2.x) / 2f
                        val currentCenterY = (p1.y + p2.y) / 2f
                        val currentDist = hypot(p1.x - p2.x, p1.y - p2.y).coerceAtLeast(1f)

                        val deltaX = currentCenterX - current.lastCenterX
                        val deltaY = currentCenterY - current.lastCenterY
                        val zoomFactor = currentDist / current.lastDistance

                        lastDeltaX = deltaX
                        lastDeltaY = deltaY
                        lastMoveTimeMs = System.currentTimeMillis()

                        actions.add(
                            PointerAction.PanZoomDelta(
                                deltaScreenX = deltaX,
                                deltaScreenY = deltaY,
                                zoomFactor = zoomFactor,
                                focalScreenX = currentCenterX,
                                focalScreenY = currentCenterY
                            )
                        )

                        state = current.copy(
                            lastCenterX = currentCenterX,
                            lastCenterY = currentCenterY,
                            lastDistance = currentDist
                        )
                    }
                }
            }
            is PointerState.Selecting -> {
                if (event.pointerId == current.pointerId) {
                    val updatedPoints = current.currentPoints + Pair(event.x, event.y)
                    val distFromStart = hypot(event.x - current.startX, event.y - current.startY)
                    val isLasso = current.isLasso || distFromStart > 16.0f

                    state = current.copy(currentPoints = updatedPoints, isLasso = isLasso)
                    if (isLasso) {
                        actions.add(PointerAction.LassoProgress(updatedPoints))
                    }
                }
            }
            is PointerState.Transforming -> {
                if (event.pointerId == current.pointerId) {
                    val dx = event.x - current.lastScreenX
                    val dy = event.y - current.lastScreenY
                    val totalDx = event.x - current.startScreenX
                    val totalDy = event.y - current.startScreenY
                    actions.add(
                        PointerAction.TransformDelta(
                            handleId = current.handleId,
                            deltaScreenX = dx,
                            deltaScreenY = dy,
                            totalDeltaX = totalDx,
                            totalDeltaY = totalDy,
                            currentScreenX = event.x,
                            currentScreenY = event.y
                        )
                    )
                    state = current.copy(lastScreenX = event.x, lastScreenY = event.y)
                }
            }
            is PointerState.Idle -> {
                // If two fingers are tracked in Idle (e.g. after stylus rejection), transition to PanZoom
                if (activePointers.size >= 2) {
                    val ids = activePointers.keys.toList()
                    val p1 = activePointers[ids[0]]!!
                    val p2 = activePointers[ids[1]]!!
                    val dist = hypot(p1.x - p2.x, p1.y - p2.y).coerceAtLeast(1f)
                    state = PointerState.PanZoom(
                        pointer1Id = ids[0],
                        pointer2Id = ids[1],
                        lastCenterX = (p1.x + p2.x) / 2f,
                        lastCenterY = (p1.y + p2.y) / 2f,
                        lastDistance = dist
                    )
                }
            }
        }

        return actions
    }

    private fun handleUp(event: PointerInputEvent.Up): List<PointerAction> {
        val actions = mutableListOf<PointerAction>()
        activePointers.remove(event.pointerId)

        when (val current = state) {
            is PointerState.Drawing -> {
                if (event.pointerId == current.pointerId) {
                    if (activeMode == ActiveMode.ERASE) {
                        actions.add(PointerAction.FinishEraser)
                    } else {
                        actions.add(PointerAction.FinishStroke)
                    }
                    state = PointerState.Idle
                }
            }
            is PointerState.PanZoom -> {
                if (event.pointerId == current.pointer1Id || event.pointerId == current.pointer2Id) {
                    val remainingId = if (event.pointerId == current.pointer1Id) current.pointer2Id else current.pointer1Id
                    val remainingPointer = activePointers[remainingId]
                    if (remainingId != -1 && remainingPointer != null && stylusOnlyDrawing) {
                        // Smoothly transition remaining finger to 1-finger pan without jumping focal point
                        state = PointerState.PanZoom(
                            pointer1Id = remainingId,
                            pointer2Id = -1,
                            lastCenterX = remainingPointer.x,
                            lastCenterY = remainingPointer.y,
                            lastDistance = 1f
                        )
                    } else {
                        // Check if fling velocity should be triggered
                        val now = System.currentTimeMillis()
                        if (now - lastMoveTimeMs < 100 && (hypot(lastDeltaX, lastDeltaY) > 8f)) {
                            actions.add(PointerAction.Fling(lastDeltaX * 12f, lastDeltaY * 12f))
                        }
                        state = PointerState.Idle
                    }
                }
            }
            is PointerState.Selecting -> {
                if (event.pointerId == current.pointerId) {
                    if (current.isLasso) {
                        actions.add(PointerAction.LassoComplete(current.currentPoints))
                    } else {
                        actions.add(PointerAction.TapSelect(current.startX, current.startY))
                    }
                    state = PointerState.Idle
                }
            }
            is PointerState.Transforming -> {
                if (event.pointerId == current.pointerId) {
                    val totalDx = event.x - current.startScreenX
                    val totalDy = event.y - current.startScreenY
                    actions.add(
                        PointerAction.FinishTransform(
                            handleId = current.handleId,
                            totalDeltaX = totalDx,
                            totalDeltaY = totalDy
                        )
                    )
                    state = PointerState.Idle
                }
            }
            is PointerState.Idle -> {
                // Up without down (e.g. off-screen release) handled safely
            }
        }

        if (activePointers.isEmpty()) {
            state = PointerState.Idle
        }

        return actions
    }

    private fun handleCancel(event: PointerInputEvent.Cancel): List<PointerAction> {
        val actions = mutableListOf<PointerAction>()
        activePointers.clear()

        when (state) {
            is PointerState.Drawing -> {
                if (activeMode == ActiveMode.ERASE) {
                    actions.add(PointerAction.CancelEraser)
                }
                actions.add(PointerAction.CancelStroke)
            }
            is PointerState.Transforming -> actions.add(PointerAction.CancelTransform)
            else -> {}
        }

        state = PointerState.Idle
        return actions
    }

    /**
     * Initiates transformation mode directly from on-canvas handle touch hit.
     */
    fun startTransform(pointerId: Int, handleId: Int, screenX: Float, screenY: Float) {
        state = PointerState.Transforming(
            pointerId = pointerId,
            handleId = handleId,
            startScreenX = screenX,
            startScreenY = screenY,
            lastScreenX = screenX,
            lastScreenY = screenY
        )
    }

    fun reset() {
        state = PointerState.Idle
        activePointers.clear()
        stylusDetected = false
    }
}
