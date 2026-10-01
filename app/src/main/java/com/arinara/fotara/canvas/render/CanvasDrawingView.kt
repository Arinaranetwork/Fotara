// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.render

import android.content.Context
import android.graphics.Canvas
import android.view.MotionEvent
import android.view.View
import android.widget.OverScroller
import com.arinara.fotara.canvas.engine.CanvasHistoryManager
import com.arinara.fotara.canvas.engine.CanvasRect
import com.arinara.fotara.canvas.engine.QuadTreeSpatialIndex
import com.arinara.fotara.canvas.engine.ViewportState
import com.arinara.fotara.canvas.engine.ViewportTransform
import com.arinara.fotara.canvas.gesture.ActiveMode
import com.arinara.fotara.canvas.gesture.PointerAction
import com.arinara.fotara.canvas.gesture.PointerInputEvent
import com.arinara.fotara.canvas.gesture.PointerPoint
import com.arinara.fotara.canvas.gesture.PointerStateMachine
import com.arinara.fotara.canvas.gesture.PointerToolType
import com.arinara.fotara.canvas.model.CanvasDocument
import com.arinara.fotara.canvas.model.StrokeToolType
import com.arinara.fotara.canvas.tool.CanvasToolController
import com.arinara.fotara.canvas.tool.CanvasToolType

/**
 * Architectural Choice (C9-A):
 * We implement an AndroidView-hosted custom View (CanvasDrawingView) rather than a pure Compose
 * Canvas for the infinite drawing canvas.
 *
 * Rationale:
 * 1. High-frequency digitizer batching: MotionEvent provides historical coordinates
 *    (getHistoricalX/Y/Pressure) which are critical for smooth 120Hz/240Hz stylus handwriting.
 * 2. Zero Compose recomposition jank: Continuous touch movement runs entirely within the native
 *    View render loop without triggering Compose state recalculations or layout passes.
 * 3. Deterministic zero-allocation onDraw: Pre-allocated native Paint, Path, and Matrix instances
 *    operate with direct hardware acceleration and bounded memory tile blitting.
 */
class CanvasDrawingView(
    context: Context,
    private val tileCacheManager: TileCacheManager,
    private val canvasRenderer: CanvasRenderer,
    private val pointerStateMachine: PointerStateMachine,
    private val toolController: CanvasToolController,
    private val historyManager: CanvasHistoryManager,
    private val spatialIndex: QuadTreeSpatialIndex,
    var onDocumentChanged: ((CanvasDocument, CanvasRect?) -> Unit)? = null,
    var onViewportChanged: ((ViewportState) -> Unit)? = null
) : View(context) {

    var viewport: ViewportState = ViewportState()
        private set

    var documentSnapshot: CanvasDocument = CanvasDocument()
        set(value) {
            field = value
            spatialIndex.rebuild(value.elements)
            invalidate()
        }

    private val scroller = OverScroller(context)
    private var activeLassoPolygon: List<Pair<Float, Float>>? = null

    init {
        isFocusable = true
        isClickable = true
        // Enable hardware acceleration
        setLayerType(LAYER_TYPE_HARDWARE, null)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        val actionMasked = event.actionMasked
        val pointerIndex = event.actionIndex
        val pointerId = event.getPointerId(pointerIndex)

        val toolType = when (event.getToolType(pointerIndex)) {
            MotionEvent.TOOL_TYPE_STYLUS, MotionEvent.TOOL_TYPE_ERASER -> PointerToolType.STYLUS
            MotionEvent.TOOL_TYPE_MOUSE -> PointerToolType.MOUSE
            else -> PointerToolType.FINGER
        }

        val pointerInputEvent: PointerInputEvent = when (actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> {
                scroller.forceFinished(true)
                PointerInputEvent.Down(
                    pointerId = pointerId,
                    x = event.getX(pointerIndex),
                    y = event.getY(pointerIndex),
                    pressure = event.getPressure(pointerIndex),
                    toolType = toolType,
                    touchMajor = event.touchMajor
                )
            }
            MotionEvent.ACTION_MOVE -> {
                val historical = mutableListOf<PointerPoint>()
                val historySize = event.historySize
                for (h in 0 until historySize) {
                    historical.add(
                        PointerPoint(
                            x = event.getHistoricalX(pointerIndex, h),
                            y = event.getHistoricalY(pointerIndex, h),
                            pressure = event.getHistoricalPressure(pointerIndex, h)
                        )
                    )
                }

                PointerInputEvent.Move(
                    pointerId = pointerId,
                    x = event.getX(pointerIndex),
                    y = event.getY(pointerIndex),
                    pressure = event.getPressure(pointerIndex),
                    toolType = toolType,
                    touchMajor = event.touchMajor,
                    historicalPoints = historical
                )
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                PointerInputEvent.Up(
                    pointerId = pointerId,
                    x = event.getX(pointerIndex),
                    y = event.getY(pointerIndex)
                )
            }
            MotionEvent.ACTION_CANCEL -> {
                PointerInputEvent.Cancel(pointerId = pointerId)
            }
            else -> return super.onTouchEvent(event)
        }

        val actions = pointerStateMachine.processEvent(pointerInputEvent)
        handlePointerActions(actions)

        return true
    }

    private fun handlePointerActions(actions: List<PointerAction>) {
        var needsInvalidate = false

        for (action in actions) {
            when (action) {
                is PointerAction.StartStroke -> {
                    val (wx, wy) = ViewportTransform.screenToWorld(action.screenX, action.screenY, viewport)
                    toolController.startStroke(wx, wy, action.pressure)
                    needsInvalidate = true
                }
                is PointerAction.AppendPoints -> {
                    toolController.appendPoints(action.points, viewport)
                    needsInvalidate = true
                }
                is PointerAction.FinishStroke -> {
                    val (updatedDoc, strokeBounds) = toolController.finishStroke(documentSnapshot, historyManager)
                    documentSnapshot = updatedDoc
                    spatialIndex.rebuild(updatedDoc.elements)
                    tileCacheManager.invalidateRegion(strokeBounds)
                    onDocumentChanged?.invoke(updatedDoc, strokeBounds)
                    needsInvalidate = true
                }
                is PointerAction.CancelStroke -> {
                    toolController.cancelStroke()
                    needsInvalidate = true
                }
                is PointerAction.PanZoomDelta -> {
                    val panned = ViewportTransform.pan(viewport, action.deltaScreenX, action.deltaScreenY)
                    viewport = ViewportTransform.zoomAboutFocalPoint(
                        viewport = panned,
                        focalScreenX = action.focalScreenX,
                        focalScreenY = action.focalScreenY,
                        zoomDelta = action.zoomFactor
                    )
                    onViewportChanged?.invoke(viewport)
                    needsInvalidate = true
                }
                is PointerAction.Fling -> {
                    scroller.fling(
                        viewport.translateX.toInt(),
                        viewport.translateY.toInt(),
                        action.velocityX.toInt(),
                        action.velocityY.toInt(),
                        -100000, 100000,
                        -100000, 100000
                    )
                    postInvalidateOnAnimation()
                }
                is PointerAction.TapSelect -> {
                    val selectedIds = toolController.selectTap(action.screenX, action.screenY, viewport, documentSnapshot)
                    toolController.toolState = toolController.toolState.copy(selectedElementIds = selectedIds)
                    activeLassoPolygon = null
                    needsInvalidate = true
                }
                is PointerAction.LassoProgress -> {
                    activeLassoPolygon = action.polygon
                    needsInvalidate = true
                }
                is PointerAction.LassoComplete -> {
                    val selectedIds = toolController.selectLasso(action.polygon, viewport, documentSnapshot)
                    toolController.toolState = toolController.toolState.copy(selectedElementIds = selectedIds)
                    activeLassoPolygon = null
                    needsInvalidate = true
                }
                is PointerAction.TransformDelta -> {
                    val updatedDoc = toolController.applyTransformDelta(
                        handleId = action.handleId,
                        deltaScreenX = action.deltaScreenX,
                        deltaScreenY = action.deltaScreenY,
                        viewport = viewport,
                        selectedIds = toolController.toolState.selectedElementIds,
                        document = documentSnapshot,
                        historyManager = historyManager
                    )
                    documentSnapshot = updatedDoc
                    spatialIndex.rebuild(updatedDoc.elements)
                    tileCacheManager.invalidateAll()
                    onDocumentChanged?.invoke(updatedDoc, null)
                    needsInvalidate = true
                }
                is PointerAction.FinishTransform -> {
                    needsInvalidate = true
                }
            }
        }

        if (needsInvalidate) {
            invalidate()
        }
    }

    override fun computeScroll() {
        if (scroller.computeScrollOffset()) {
            viewport = viewport.copy(
                translateX = scroller.currX.toFloat(),
                translateY = scroller.currY.toFloat()
            )
            onViewportChanged?.invoke(viewport)
            postInvalidateOnAnimation()
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val inProgressPoints = if (toolController.isDrawing()) toolController.currentPoints else null
        val inProgressTool = when (toolController.toolState.activeTool) {
            CanvasToolType.HIGHLIGHTER -> StrokeToolType.HIGHLIGHTER
            else -> StrokeToolType.PEN
        }
        val inProgressColor = if (inProgressTool == StrokeToolType.HIGHLIGHTER) {
            toolController.toolState.highlighterColor
        } else {
            toolController.toolState.penColor
        }
        val inProgressWidth = if (inProgressTool == StrokeToolType.HIGHLIGHTER) {
            toolController.toolState.highlighterSize
        } else {
            toolController.toolState.penSize
        }

        canvasRenderer.drawCanvas(
            canvas = canvas,
            viewport = viewport,
            screenWidth = width.toFloat(),
            screenHeight = height.toFloat(),
            documentSnapshot = documentSnapshot,
            spatialIndex = spatialIndex,
            inProgressPoints = inProgressPoints,
            inProgressTool = inProgressTool,
            inProgressColor = inProgressColor,
            inProgressWidth = inProgressWidth,
            selectedElementIds = toolController.toolState.selectedElementIds,
            activeLassoPolygon = activeLassoPolygon,
            onTileInvalidated = { postInvalidate() }
        )
    }

    fun fitToContent() {
        val bounds = documentSnapshot.computeOverallBounds()
        viewport = ViewportTransform.fitToContent(
            contentBounds = bounds,
            screenWidth = width.toFloat(),
            screenHeight = height.toFloat()
        )
        tileCacheManager.invalidateAll()
        onViewportChanged?.invoke(viewport)
        invalidate()
    }

    fun resetZoom() {
        viewport = ViewportState(scale = 1.0f, translateX = 0f, translateY = 0f)
        tileCacheManager.invalidateAll()
        onViewportChanged?.invoke(viewport)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        scroller.abortAnimation()
        tileCacheManager.release()
    }
}
