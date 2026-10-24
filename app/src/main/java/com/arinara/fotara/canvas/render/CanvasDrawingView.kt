// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.render

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.view.MotionEvent
import android.view.View
import android.widget.OverScroller
import com.arinara.fotara.canvas.engine.CanvasHistoryManager
import com.arinara.fotara.canvas.engine.CanvasRect
import com.arinara.fotara.canvas.engine.QuadTreeSpatialIndex
import com.arinara.fotara.canvas.engine.ViewportState
import com.arinara.fotara.canvas.engine.ViewportTransform
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
import com.arinara.fotara.feature.collab.model.CollabPeer
import java.util.concurrent.ConcurrentHashMap

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
    var onSelectionChanged: ((CanvasSelection) -> Unit)? = null,
    var onEditBlocked: (() -> Unit)? = null,
    var onCursorMoved: ((worldX: Float, worldY: Float) -> Unit)? = null,
    var onLiveStrokeProgress: ((
        strokeId: String,
        points: List<Float>,
        colorHex: String,
        width: Float,
        toolType: String,
        blendMode: String,
        layerId: String
    ) -> Unit)? = null
) : View(context) {

    var viewport: ViewportState = ViewportState()
        private set

    private var _documentSnapshot: CanvasDocument = CanvasDocument()

    var documentSnapshot: CanvasDocument
        get() = _documentSnapshot
        set(value) {
            _documentSnapshot = value
            canvasRenderer.invalidateVisibilityCache()
            spatialIndex.rebuild(value.elements)
            invalidate()
        }

    fun setDocumentTransient(value: CanvasDocument) {
        _documentSnapshot = value
        canvasRenderer.invalidateVisibilityCache()
        invalidate()
    }

    var activeLayerId: String = "layer_default"

    private var _canUserEditCanvas: Boolean = true
    var canUserEditCanvas: Boolean
        get() = _canUserEditCanvas
        set(value) {
            if (_canUserEditCanvas != value) {
                _canUserEditCanvas = value
                if (!value) {
                    cancelShapeHold()
                    toolController.cancelStroke()
                    toolController.cancelEraser()
                    toolController.cancelTransformGesture()
                    activeLassoPolygon = null
                    invalidate()
                }
            }
        }

    private val scroller = OverScroller(context)
    private var activeLassoPolygon: List<Pair<Float, Float>>? = null

    // Real-time 60fps cursor & live stroke streaming state
    private var lastCursorPublishMs: Long = 0L
    private var lastPublishedCursorWorldX: Float = Float.NaN
    private var lastPublishedCursorWorldY: Float = Float.NaN
    private var lastLiveStrokePublishMs: Long = 0L
    private var lastCursorFrameTimeNs: Long = 0L

    private class PeerCursorRenderState(
        @Volatile var userTag: String,
        @Volatile var colorInt: Int,
        @Volatile var targetWorldX: Float,
        @Volatile var targetWorldY: Float,
        var currentWorldX: Float,
        var currentWorldY: Float,
        @Volatile var lastActiveMs: Long,
        @Volatile var hasValidPosition: Boolean
    )

    private val peerCursorStates = ConcurrentHashMap<String, PeerCursorRenderState>()
    private val cursorPointerPath = Path()
    private val cursorPillRect = RectF()
    private val cursorFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    private val cursorOutlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
    }
    private val cursorPillFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = 0xEB161C26.toInt() // SurfacePanel with high opacity
    }
    private val cursorTextPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFF8FAFC.toInt() // TextPrimary
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }

    fun updateRemotePeerCursors(peers: Collection<CollabPeer>, localPeerId: String) {
        val activeRemoteIds = HashSet<String>(peers.size)
        for (peer in peers) {
            if (peer.peerId == localPeerId || peer.peerId.isBlank()) continue
            activeRemoteIds.add(peer.peerId)
            val resolvedTag = peer.userTag.trim()
                .takeIf { it.isNotBlank() && !it.equals("Fotara User", ignoreCase = true) && !it.equals("My Profile", ignoreCase = true) }
                ?: peer.userHandle.trim().removePrefix("@").ifBlank {
                    peer.userTag.trim().ifBlank { "Member" }
                }
            val parsedColor = parsePeerColorInt(peer.colorHex)
            val hasPos = peer.lastCursorX != 0f || peer.lastCursorY != 0f
            val existing = peerCursorStates[peer.peerId]
            if (existing != null) {
                existing.userTag = resolvedTag
                existing.colorInt = parsedColor
                existing.lastActiveMs = peer.lastActiveMs
                if (hasPos) {
                    if (!existing.hasValidPosition) {
                        existing.currentWorldX = peer.lastCursorX
                        existing.currentWorldY = peer.lastCursorY
                        existing.hasValidPosition = true
                    }
                    existing.targetWorldX = peer.lastCursorX
                    existing.targetWorldY = peer.lastCursorY
                }
            } else {
                peerCursorStates[peer.peerId] = PeerCursorRenderState(
                    userTag = resolvedTag,
                    colorInt = parsedColor,
                    targetWorldX = peer.lastCursorX,
                    targetWorldY = peer.lastCursorY,
                    currentWorldX = peer.lastCursorX,
                    currentWorldY = peer.lastCursorY,
                    lastActiveMs = peer.lastActiveMs,
                    hasValidPosition = hasPos
                )
            }
        }
        val iter = peerCursorStates.keys.iterator()
        while (iter.hasNext()) {
            val id = iter.next()
            if (id !in activeRemoteIds) {
                iter.remove()
            }
        }
        postInvalidateOnAnimation()
    }

    private fun parsePeerColorInt(hex: String): Int {
        val cleaned = hex.trim().removePrefix("#")
        return try {
            when (cleaned.length) {
                6 -> (cleaned.toLong(16) or 0xFF000000L).toInt()
                8 -> cleaned.toLong(16).toInt()
                else -> 0xFF7A5AF8.toInt()
            }
        } catch (_: Exception) {
            0xFF7A5AF8.toInt()
        }
    }

    private fun publishCursorPosition(screenX: Float, screenY: Float, force: Boolean = false) {
        val callback = onCursorMoved ?: return
        val now = System.currentTimeMillis()
        if (!force && now - lastCursorPublishMs < 16L) return
        val wx = ViewportTransform.screenToWorldX(screenX, viewport)
        val wy = ViewportTransform.screenToWorldY(screenY, viewport)
        if (!force &&
            !lastPublishedCursorWorldX.isNaN() &&
            kotlin.math.abs(wx - lastPublishedCursorWorldX) < 0.25f &&
            kotlin.math.abs(wy - lastPublishedCursorWorldY) < 0.25f
        ) {
            return
        }
        lastCursorPublishMs = now
        lastPublishedCursorWorldX = wx
        lastPublishedCursorWorldY = wy
        callback.invoke(wx, wy)
    }

    private fun publishLiveStrokeIfDue(force: Boolean = false) {
        val callback = onLiveStrokeProgress ?: return
        if (!canUserEditCanvas || !toolController.isDrawing()) return
        val now = System.currentTimeMillis()
        if (!force && now - lastLiveStrokePublishMs < 32L) return
        val pts = toolController.currentPoints
        if (pts.isEmpty()) return
        lastLiveStrokePublishMs = now

        val flatPoints = ArrayList<Float>(maxOf(pts.size * 2, 4))
        for (p in pts) {
            flatPoints.add(p.x)
            flatPoints.add(p.y)
        }
        if (flatPoints.size == 2) {
            flatPoints.add(pts[0].x)
            flatPoints.add(pts[0].y)
        }
        val isHighlighter = toolController.toolState.activeTool == CanvasToolType.HIGHLIGHTER
        val rawColor = if (isHighlighter) toolController.toolState.highlighterColor else toolController.toolState.penColor
        val alphaMultiplier = if (isHighlighter) 1.0f else toolController.toolState.penAlpha.coerceIn(0.05f, 1.0f)
        val baseAlpha = ((rawColor ushr 24) and 0xFFL).let { if (it == 0L) 255L else it }
        val finalAlpha = (baseAlpha * alphaMultiplier).toLong().coerceIn(1L, 255L)
        val packedArgb = (finalAlpha shl 24) or (rawColor and 0x00FFFFFFL)
        val colorHex = String.format("#%08X", packedArgb)
        val strokeWidth = if (isHighlighter) toolController.toolState.highlighterSize else toolController.toolState.penSize
        val toolTypeName = if (isHighlighter) StrokeToolType.HIGHLIGHTER.name else StrokeToolType.PEN.name
        val blendModeName = if (isHighlighter) {
            toolController.toolState.highlighterBlendMode.name
        } else {
            com.arinara.fotara.canvas.model.StrokeBlendMode.NORMAL.name
        }
        val resolvedLayerId = activeLayerId.ifEmpty { documentSnapshot.getPrimaryLayerId() }
        callback.invoke(
            toolController.activeStrokeId,
            flatPoints,
            colorHex,
            strokeWidth,
            toolTypeName,
            blendModeName,
            resolvedLayerId
        )
    }

    init {
        isFocusable = true
        isClickable = true
        // Enable hardware acceleration
        setLayerType(LAYER_TYPE_HARDWARE, null)

        toolController.density = resources.displayMetrics.density

        // Route selection handle hit-testing to tool controller (strictly in SELECT tool)
        pointerStateMachine.onHitTestSelection = { sx, sy ->
            if (canUserEditCanvas && toolController.toolState.activeTool == com.arinara.fotara.canvas.tool.CanvasToolType.SELECT) {
                toolController.hitTestHandles(sx, sy, viewport, resources.displayMetrics.density)
            } else null
        }

        // Route direct text layer hit-testing to tool controller (strictly in SELECT tool)
        pointerStateMachine.onHitTestTextLayer = { sx, sy ->
            if (canUserEditCanvas && toolController.toolState.activeTool == com.arinara.fotara.canvas.tool.CanvasToolType.SELECT) {
                val (wx, wy) = ViewportTransform.screenToWorld(sx, sy, viewport)
                val hitText = documentSnapshot.elements
                    .filterIsInstance<com.arinara.fotara.canvas.model.TextLayerElement>()
                    .lastOrNull { toolController.hitTestText(wx, wy, it) }

                if (hitText != null) {
                    cancelShapeHold()
                    val dynBounds = hitText.computeDynamicBounds()
                    val selRef = com.arinara.fotara.canvas.model.SelectedElementReference.Whole(hitText.id)
                    toolController.selection = com.arinara.fotara.canvas.model.CanvasSelection(
                        references = mapOf(hitText.id to selRef),
                        bounds = dynBounds,
                        rotationDegrees = hitText.rotationDegrees
                    )
                    onSelectionChanged?.invoke(toolController.selection)
                    invalidate()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }

        // Route direct image element hit-testing to tool controller (strictly in SELECT tool)
        pointerStateMachine.onHitTestImageElement = { sx, sy ->
            if (canUserEditCanvas && toolController.toolState.activeTool == com.arinara.fotara.canvas.tool.CanvasToolType.SELECT) {
                val (wx, wy) = ViewportTransform.screenToWorld(sx, sy, viewport)
                val hitImg = documentSnapshot.elements
                    .filterIsInstance<com.arinara.fotara.canvas.model.ImageElement>()
                    .lastOrNull { toolController.hitTestImage(wx, wy, it) }

                if (hitImg != null) {
                    cancelShapeHold()
                    val selRef = com.arinara.fotara.canvas.model.SelectedElementReference.Whole(hitImg.id)
                    toolController.selection = com.arinara.fotara.canvas.model.CanvasSelection(
                        references = mapOf(hitImg.id to selRef),
                        bounds = hitImg.bounds,
                        rotationDegrees = hitImg.rotationDegrees
                    )
                    onSelectionChanged?.invoke(toolController.selection)
                    invalidate()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
    }

    private val shapeHoldHandler = android.os.Handler(android.os.Looper.getMainLooper())
    private var shapeHoldAnchorX = 0f
    private var shapeHoldAnchorY = 0f
    private var isShapeSnapped = false
    private val shapeHoldRunnable = Runnable {
        if (canUserEditCanvas && toolController.toolState.activeTool == CanvasToolType.PEN) {
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
        if (canUserEditCanvas && toolController.toolState.activeTool == CanvasToolType.PEN && !isShapeSnapped) {
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
                val sx = event.getX(pointerIndex)
                val sy = event.getY(pointerIndex)
                publishCursorPosition(sx, sy, force = true)
                scheduleShapeHold(sx, sy)
                PointerInputEvent.Down(
                    pointerId = pointerId,
                    x = sx,
                    y = sy,
                    pressure = event.getPressure(pointerIndex),
                    toolType = toolType,
                    touchMajor = event.touchMajor
                )
            }
            MotionEvent.ACTION_MOVE -> {
                val sx = event.getX(pointerIndex)
                val sy = event.getY(pointerIndex)
                publishCursorPosition(sx, sy, force = false)
                checkShapeHoldMovement(sx, sy)
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
                    x = sx,
                    y = sy,
                    pressure = event.getPressure(pointerIndex),
                    toolType = toolType,
                    touchMajor = event.touchMajor,
                    historicalPoints = historical
                )
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                cancelShapeHold()
                val sx = event.getX(pointerIndex)
                val sy = event.getY(pointerIndex)
                publishCursorPosition(sx, sy, force = true)
                PointerInputEvent.Up(
                    pointerId = pointerId,
                    x = sx,
                    y = sy
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

    override fun onGenericMotionEvent(event: MotionEvent): Boolean {
        when (event.actionMasked) {
            MotionEvent.ACTION_HOVER_ENTER,
            MotionEvent.ACTION_HOVER_MOVE -> {
                publishCursorPosition(
                    screenX = event.x,
                    screenY = event.y,
                    force = event.actionMasked == MotionEvent.ACTION_HOVER_ENTER
                )
            }
        }
        return super.onGenericMotionEvent(event)
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
                    if (!canUserEditCanvas) {
                        cancelShapeHold()
                        toolController.cancelStroke()
                        onEditBlocked?.invoke()
                        continue
                    }
                    val (wx, wy) = ViewportTransform.screenToWorld(action.screenX, action.screenY, viewport)
                    toolController.startStroke(wx, wy, action.pressure)
                    publishLiveStrokeIfDue(force = true)
                    needsInvalidate = true
                }
                is PointerAction.AppendPoints -> {
                    if (!canUserEditCanvas) continue
                    toolController.appendPoints(action.points, viewport)
                    publishLiveStrokeIfDue(force = false)
                    needsInvalidate = true
                }
                is PointerAction.FinishStroke -> {
                    if (!canUserEditCanvas) {
                        isShapeSnapped = false
                        toolController.cancelStroke()
                        needsInvalidate = true
                        continue
                    }
                    toolController.activeLayerId = activeLayerId.ifEmpty { documentSnapshot.getPrimaryLayerId() }
                    toolController.isShapeSnapped = isShapeSnapped
                    val (updatedDoc, strokeBounds) = toolController.finishStroke(documentSnapshot, historyManager, viewport.scale)
                    isShapeSnapped = false
                    documentSnapshot = updatedDoc
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
                    if (!canUserEditCanvas) {
                        toolController.cancelEraser()
                        onEditBlocked?.invoke()
                        continue
                    }
                    toolController.startEraser(action.screenX, action.screenY, viewport, documentSnapshot)
                }
                is PointerAction.EraseSweep -> {
                    if (!canUserEditCanvas) continue
                    val (liveDoc, dirtyBounds) = toolController.sweepEraser(
                        points = action.points,
                        viewport = viewport,
                        document = documentSnapshot
                    )
                    if (dirtyBounds != null) {
                        setDocumentTransient(liveDoc)
                        needsInvalidate = true
                    }
                }
                is PointerAction.FinishEraser -> {
                    if (!canUserEditCanvas) {
                        toolController.cancelEraser()
                        needsInvalidate = true
                        continue
                    }
                    val (committedDoc, dirtyBounds) = toolController.finishEraser(
                        document = documentSnapshot,
                        historyManager = historyManager
                    )
                    if (dirtyBounds != null) {
                        canvasRenderer.invalidateVisibilityCache()
                        documentSnapshot = committedDoc
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
                    if (!canUserEditCanvas) {
                        onEditBlocked?.invoke()
                        continue
                    }
                    val newSelection = toolController.selectTap(action.screenX, action.screenY, viewport, documentSnapshot)
                    activeLassoPolygon = null
                    onSelectionChanged?.invoke(newSelection)
                    needsInvalidate = true
                }
                is PointerAction.LassoProgress -> {
                    if (!canUserEditCanvas) continue
                    activeLassoPolygon = action.polygon
                    needsInvalidate = true
                }
                is PointerAction.LassoComplete -> {
                    if (!canUserEditCanvas) {
                        activeLassoPolygon = null
                        onEditBlocked?.invoke()
                        continue
                    }
                    val newSelection = toolController.selectLasso(action.polygon, viewport, documentSnapshot)
                    activeLassoPolygon = null
                    onSelectionChanged?.invoke(newSelection)
                    needsInvalidate = true
                }
                is PointerAction.StartTransform -> {
                    cancelShapeHold()
                    if (!canUserEditCanvas) {
                        toolController.cancelTransformGesture()
                        onEditBlocked?.invoke()
                        continue
                    }
                    toolController.startTransformGesture(action.screenX, action.screenY, viewport, documentSnapshot)
                }
                is PointerAction.TransformDelta -> {
                    if (!canUserEditCanvas) continue
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
                    if (!canUserEditCanvas) {
                        toolController.cancelTransformGesture()
                        needsInvalidate = true
                        continue
                    }
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
        val inProgressPoints = if (!canUserEditCanvas) {
            null
        } else if (isEraser) {
            if (toolController.activeEraserStrokePoints.isNotEmpty()) toolController.activeEraserStrokePoints else null
        } else if (toolController.isDrawing()) {
            toolController.currentPoints
        } else null

        val inProgressTool = when {
            isEraser -> StrokeToolType.ERASER
            toolController.toolState.activeTool == CanvasToolType.HIGHLIGHTER -> StrokeToolType.HIGHLIGHTER
            else -> StrokeToolType.PEN
        }
        val inProgressColor = when {
            isEraser -> 0xFF000000L
            inProgressTool == StrokeToolType.HIGHLIGHTER -> toolController.toolState.highlighterColor
            else -> toolController.toolState.penColor
        }
        val inProgressWidth = when {
            isEraser -> (toolController.toolState.eraserRadius * 2f).coerceAtLeast(8f)
            inProgressTool == StrokeToolType.HIGHLIGHTER -> toolController.toolState.highlighterSize
            else -> toolController.toolState.penSize
        }
        val inProgressBlendMode = when {
            isEraser -> com.arinara.fotara.canvas.model.StrokeBlendMode.DST_OUT
            inProgressTool == StrokeToolType.HIGHLIGHTER -> toolController.toolState.highlighterBlendMode
            else -> com.arinara.fotara.canvas.model.StrokeBlendMode.NORMAL
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
            selection = if (canUserEditCanvas && toolController.toolState.activeTool == com.arinara.fotara.canvas.tool.CanvasToolType.SELECT) toolController.selectionToDraw else com.arinara.fotara.canvas.model.CanvasSelection.Empty,
            previewElements = if (canUserEditCanvas && toolController.toolState.activeTool == com.arinara.fotara.canvas.tool.CanvasToolType.SELECT) toolController.previewElements else null,
            activeLassoPolygon = if (canUserEditCanvas && toolController.toolState.activeTool == com.arinara.fotara.canvas.tool.CanvasToolType.SELECT) activeLassoPolygon else emptyList(),
            density = resources.displayMetrics.density,
            isGestureActive = isGestureActive,
            onTileInvalidated = { postInvalidate() }
        )

        drawRemotePeerCursors(canvas)
    }

    private fun drawRemotePeerCursors(canvas: Canvas) {
        if (peerCursorStates.isEmpty()) {
            lastCursorFrameTimeNs = 0L
            return
        }
        val nowNs = System.nanoTime()
        val nowMs = System.currentTimeMillis()
        val dtSec = if (lastCursorFrameTimeNs == 0L) {
            0.016f
        } else {
            ((nowNs - lastCursorFrameTimeNs) / 1_000_000_000f).coerceIn(0.004f, 0.05f)
        }
        lastCursorFrameTimeNs = nowNs
        val lerpFactor = (1f - kotlin.math.exp(-24f * dtSec)).coerceIn(0.22f, 1f)

        val density = resources.displayMetrics.density
        val pointerSize = 14f * density
        val tipRadius = 2.8f * density
        val pillOffsetX = 12f * density
        val pillOffsetY = 12f * density
        val pillHeight = 22f * density
        val pillCorner = 11f * density
        val pillPadH = 8f * density
        cursorTextPaint.textSize = 11f * density

        var stillAnimating = false

        for (state in peerCursorStates.values) {
            if (!state.hasValidPosition) continue
            if (nowMs - state.lastActiveMs > 30_000L) continue

            val dx = state.targetWorldX - state.currentWorldX
            val dy = state.targetWorldY - state.currentWorldY
            if (kotlin.math.hypot(dx, dy) > 0.15f) {
                state.currentWorldX += dx * lerpFactor
                state.currentWorldY += dy * lerpFactor
                stillAnimating = true
            } else {
                state.currentWorldX = state.targetWorldX
                state.currentWorldY = state.targetWorldY
            }

            val sx = ViewportTransform.worldToScreenX(state.currentWorldX, viewport)
            val sy = ViewportTransform.worldToScreenY(state.currentWorldY, viewport)
            if (sx < -200f || sx > width + 200f || sy < -200f || sy > height + 200f) continue

            // 1. Directional pointer triangle
            cursorPointerPath.reset()
            cursorPointerPath.moveTo(sx, sy)
            cursorPointerPath.lineTo(sx, sy + pointerSize)
            cursorPointerPath.lineTo(sx + pointerSize * 0.42f, sy + pointerSize * 0.72f)
            cursorPointerPath.lineTo(sx + pointerSize * 0.85f, sy + pointerSize * 0.72f)
            cursorPointerPath.close()

            cursorFillPaint.color = state.colorInt
            canvas.drawPath(cursorPointerPath, cursorFillPaint)

            cursorOutlinePaint.color = 0xFF262F3E.toInt() // BorderSubtle
            cursorOutlinePaint.strokeWidth = 1f * density
            canvas.drawPath(cursorPointerPath, cursorOutlinePaint)

            // 2. Tip dot
            canvas.drawCircle(sx, sy, tipRadius, cursorFillPaint)

            // 3. Username pill badge
            val label = state.userTag.take(20)
            val textWidth = cursorTextPaint.measureText(label)
            val pillWidth = (textWidth + pillPadH * 2f).coerceAtLeast(36f * density)
            val left = sx + pillOffsetX
            val top = sy + pillOffsetY
            cursorPillRect.set(left, top, left + pillWidth, top + pillHeight)

            canvas.drawRoundRect(cursorPillRect, pillCorner, pillCorner, cursorPillFillPaint)

            cursorOutlinePaint.color = state.colorInt
            cursorOutlinePaint.strokeWidth = 1.5f * density
            canvas.drawRoundRect(cursorPillRect, pillCorner, pillCorner, cursorOutlinePaint)

            val fontMetrics = cursorTextPaint.fontMetrics
            val textBaseline = top + (pillHeight - (fontMetrics.bottom - fontMetrics.top)) / 2f - fontMetrics.top
            canvas.drawText(label, left + pillPadH, textBaseline, cursorTextPaint)
        }

        if (stillAnimating) {
            postInvalidateOnAnimation()
        }
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
