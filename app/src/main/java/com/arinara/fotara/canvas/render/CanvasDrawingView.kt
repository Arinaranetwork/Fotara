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
import com.arinara.fotara.canvas.model.CanvasSelection
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
    var onViewportChanged: ((ViewportState) -> Unit)? = null,
    var onSelectionChanged: ((CanvasSelection) -> Unit)? = null
) : View(context) {

    var viewport: ViewportState = ViewportState()
        private set

    var documentSnapshot: CanvasDocument = CanvasDocument()
        set(value) {
            field = value
            spatialIndex.rebuild(value.elements)
            invalidate()
        }

    var activeLayerId: String = "layer_default"

    private val scroller = OverScroller(context)
    private var activeLassoPolygon: List<Pair<Float, Float>>? = null

    init {
        isFocusable = true
        isClickable = true
        // Enable hardware acceleration
        setLayerType(LAYER_TYPE_HARDWARE, null)

        toolController.density = resources.displayMetrics.density

        // Route selection handle hit-testing to tool controller
        pointerStateMachine.onHitTestSelection = { sx, sy ->
            toolController.hitTestHandles(sx, sy, viewport, resources.displayMetrics.density)
        }
    }

    private val shapeHoldHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var shapeHoldAnchorX = 0f
    private var shapeHoldAnchorY = 0f
    private var isShapeSnapped = false
    private val shapeHoldRunnable = Runnable {
        if (toolController.toolState.activeTool == CanvasToolType.PEN) {
            val snapped = toolController.autoCorrectActiveStroke()
            if (snapped) {
                isShapeSnapped = true
                performHapticFeedback(android.view.HapticFeedbackConstants.LONG_PRESS)
                invalidate()
            }
        }
    }

    private fun scheduleShapeHold(x: Float, y: Float) {
        shapeHoldHandler.removeCallbacks(shapeHoldRunnable)
        if (toolController.toolState.activeTool == CanvasToolType.PEN && !isShapeSnapped) {
            shapeHoldAnchorX = x
            shapeHoldAnchorY = y
            shapeHoldHandler.postDelayed(shapeHoldRunnable, com.arinara.fotara.canvas.engine.ShapeAutoCorrectEngine.HOLD_THRESHOLD_MS)
        }
    }

    private fun cancelShapeHold() {
        shapeHoldHandler.removeCallbacks(shapeHoldRunnable)
        isShapeSnapped = false
    }

    private fun checkShapeHoldMovement(x: Float, y: Float) {
        if (!isShapeSnapped) {
            val slopPx = 36f * resources.displayMetrics.density
            if (kotlin.math.hypot(x - shapeHoldAnchorX, y - shapeHoldAnchorY) > slopPx) {
                scheduleShapeHold(x, y)
            }
        }
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
                isShapeSnapped = false
                scheduleShapeHold(event.getX(pointerIndex), event.getY(pointerIndex))
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
                checkShapeHoldMovement(event.getX(pointerIndex), event.getY(pointerIndex))
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
                cancelShapeHold()
                PointerInputEvent.Up(
                    pointerId = pointerId,
                    x = event.getX(pointerIndex),
                    y = event.getY(pointerIndex)
                )
            }
            MotionEvent.ACTION_CANCEL -> {
                cancelShapeHold()
                PointerInputEvent.Cancel(pointerId = pointerId)
            }
            else -> return super.onTouchEvent(event)
        }

        val actions = pointerStateMachine.processEvent(pointerInputEvent)
        handlePointerActions(actions)

        return true
    }

    private var lastViewportPublishTimeMs: Long = 0L
    private val VIEWPORT_PUBLISH_INTERVAL_MS = 100L // Max 10 per second

    var isGestureActive: Boolean = false
        private set

    private val settleHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private val settleRunnable = Runnable {
        isGestureActive = false
        com.arinara.fotara.debug.FrameIntervalRecorder.stopGesture()
        publishViewportThrottled(force = true)
        invalidate()
    }

    fun publishViewportThrottled(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (force || now - lastViewportPublishTimeMs >= VIEWPORT_PUBLISH_INTERVAL_MS) {
            lastViewportPublishTimeMs = now
            onViewportChanged?.invoke(viewport)
        }
    }

    private fun markGestureActive() {
        if (!isGestureActive) {
            isGestureActive = true
            com.arinara.fotara.debug.FrameIntervalRecorder.startGesture()
        }
        settleHandler.removeCallbacks(settleRunnable)
        settleHandler.postDelayed(settleRunnable, 120L)
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
                    toolController.activeLayerId = activeLayerId.ifEmpty { documentSnapshot.getPrimaryLayerId() }
                    toolController.isShapeSnapped = isShapeSnapped
                    val (updatedDoc, strokeBounds) = toolController.finishStroke(documentSnapshot, historyManager, viewport.scale)
                    isShapeSnapped = false
                    documentSnapshot = updatedDoc
                    spatialIndex.rebuild(updatedDoc.elements)
                    tileCacheManager.invalidateRegion(strokeBounds)
                    onDocumentChanged?.invoke(updatedDoc, strokeBounds)
                    needsInvalidate = true
                }
                is PointerAction.CancelStroke -> {
                    isShapeSnapped = false
                    toolController.cancelStroke()
                    needsInvalidate = true
                }
                is PointerAction.PanZoomDelta -> {
                    markGestureActive()
                    val panned = ViewportTransform.pan(viewport, action.deltaScreenX, action.deltaScreenY)
                    val zoomed = if (kotlin.math.abs(action.zoomFactor - 1.0f) < 0.001f) {
                        panned
                    } else {
                        ViewportTransform.zoomAboutFocalPoint(
                            viewport = panned,
                            focalScreenX = action.focalScreenX,
                            focalScreenY = action.focalScreenY,
                            zoomDelta = action.zoomFactor
                        )
                    }
                    viewport = ViewportTransform.clampToExtent(zoomed, width.toFloat(), height.toFloat())
                    publishViewportThrottled(force = false)
                    needsInvalidate = true
                }
                is PointerAction.Fling -> {
                    markGestureActive()
                    val w = if (width > 0) width.toFloat() else 1080f
                    val h = if (height > 0) height.toFloat() else 1920f
                    val minX = (w / 2f - com.arinara.fotara.canvas.engine.CanvasConfig.WORLD_MAX_X * viewport.scale).toInt()
                    val maxX = (w / 2f - com.arinara.fotara.canvas.engine.CanvasConfig.WORLD_MIN_X * viewport.scale).toInt()
                    val minY = (h / 2f - com.arinara.fotara.canvas.engine.CanvasConfig.WORLD_MAX_Y * viewport.scale).toInt()
                    val maxY = (h / 2f - com.arinara.fotara.canvas.engine.CanvasConfig.WORLD_MIN_Y * viewport.scale).toInt()

                    scroller.fling(
                        viewport.translateX.toInt(),
                        viewport.translateY.toInt(),
                        action.velocityX.toInt(),
                        action.velocityY.toInt(),
                        minOf(minX, maxX), maxOf(minX, maxX),
                        minOf(minY, maxY), maxOf(minY, maxY)
                    )
                    postInvalidateOnAnimation()
                }
                is PointerAction.StartEraser -> {
                    toolController.startEraser(action.screenX, action.screenY)
                }
                is PointerAction.EraseSweep -> {
                    val (liveDoc, dirtyBounds) = toolController.sweepEraser(
                        points = action.points,
                        viewport = viewport,
                        document = documentSnapshot
                    )
                    if (dirtyBounds != null) {
                        documentSnapshot = liveDoc
                        spatialIndex.rebuild(liveDoc.elements)
                        needsInvalidate = true
                    }
                }
                is PointerAction.FinishEraser -> {
                    val (committedDoc, dirtyBounds) = toolController.finishEraser(
                        document = documentSnapshot,
                        historyManager = historyManager
                    )
                    if (dirtyBounds != null) {
                        documentSnapshot = committedDoc
                        spatialIndex.rebuild(committedDoc.elements)
                        tileCacheManager.invalidateRegion(dirtyBounds)
                        onDocumentChanged?.invoke(committedDoc, dirtyBounds)
                        needsInvalidate = true
                    }
                }
                is PointerAction.CancelEraser -> {
                    toolController.cancelEraser()
                    needsInvalidate = true
                }
                is PointerAction.EraseAt -> {
                    // Handled through StartEraser and EraseSweep
                }
                is PointerAction.TapSelect -> {
                    val newSelection = toolController.selectTap(action.screenX, action.screenY, viewport, documentSnapshot)
                    activeLassoPolygon = null
                    onSelectionChanged?.invoke(newSelection)
                    needsInvalidate = true
                }
                is PointerAction.LassoProgress -> {
                    activeLassoPolygon = action.polygon
                    needsInvalidate = true
                }
                is PointerAction.LassoComplete -> {
                    val newSelection = toolController.selectLasso(action.polygon, viewport, documentSnapshot)
                    activeLassoPolygon = null
                    onSelectionChanged?.invoke(newSelection)
                    needsInvalidate = true
                }
                is PointerAction.StartTransform -> {
                    toolController.startTransformGesture(action.screenX, action.screenY, viewport, documentSnapshot)
                }
                is PointerAction.TransformDelta -> {
                    toolController.updateTransformPreview(
                        handleId = action.handleId,
                        currentScreenX = action.currentScreenX,
                        currentScreenY = action.currentScreenY,
                        viewport = viewport,
                        document = documentSnapshot,
                        density = resources.displayMetrics.density
                    )
                    needsInvalidate = true
                }
                is PointerAction.FinishTransform -> {
                    val (updatedDoc, dirtyBounds) = toolController.commitTransform(
                        handleId = action.handleId,
                        totalDeltaX = action.totalDeltaX,
                        totalDeltaY = action.totalDeltaY,
                        viewport = viewport,
                        document = documentSnapshot,
                        historyManager = historyManager,
                        density = resources.displayMetrics.density
                    )
                    documentSnapshot = updatedDoc
                    spatialIndex.rebuild(updatedDoc.elements)
                    if (dirtyBounds != null) {
                        tileCacheManager.invalidateRegion(dirtyBounds)
                    }
                    onDocumentChanged?.invoke(updatedDoc, dirtyBounds)
                    onSelectionChanged?.invoke(toolController.selection)
                    needsInvalidate = true
                }
                is PointerAction.CancelTransform -> {
                    toolController.cancelTransformGesture()
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
            val updated = viewport.copy(
                translateX = scroller.currX.toFloat(),
                translateY = scroller.currY.toFloat()
            )
            viewport = ViewportTransform.clampToExtent(updated, width.toFloat(), height.toFloat())
            publishViewportThrottled(force = false)
            postInvalidateOnAnimation()
        } else if (isGestureActive) {
            publishViewportThrottled(force = true)
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val isEraser = toolController.toolState.activeTool == CanvasToolType.ERASER
        val inProgressPoints = if (!isEraser && toolController.isDrawing()) toolController.currentPoints else null
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
        val inProgressBlendMode = if (inProgressTool == StrokeToolType.HIGHLIGHTER) {
            toolController.toolState.highlighterBlendMode
        } else {
            com.arinara.fotara.canvas.model.StrokeBlendMode.NORMAL
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
            inProgressBlendMode = inProgressBlendMode,
            inProgressLayerId = activeLayerId.ifEmpty { documentSnapshot.getPrimaryLayerId() },
            selection = toolController.selectionToDraw,
            previewElements = toolController.previewElements,
            activeLassoPolygon = activeLassoPolygon,
            density = resources.displayMetrics.density,
            isGestureActive = isGestureActive,
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
        publishViewportThrottled(force = true)
        invalidate()
    }

    fun resetZoom() {
        viewport = ViewportState(scale = 1.0f, translateX = 0f, translateY = 0f)
        tileCacheManager.invalidateAll()
        publishViewportThrottled(force = true)
        invalidate()
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        settleHandler.removeCallbacks(settleRunnable)
        com.arinara.fotara.debug.FrameIntervalRecorder.stopGesture()
        scroller.abortAnimation()
        tileCacheManager.release()
        canvasRenderer.clearImageCache()
    }
}
