// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.tool

import com.arinara.fotara.canvas.engine.AddElementsCommand
import com.arinara.fotara.canvas.engine.AreaEraseCommand
import com.arinara.fotara.canvas.engine.CanvasHistoryManager
import com.arinara.fotara.canvas.engine.CanvasRect
import com.arinara.fotara.canvas.engine.CanvasSelectionEngine
import com.arinara.fotara.canvas.engine.RecognizedShape
import com.arinara.fotara.canvas.engine.ShapeAutoCorrectResult
import com.arinara.fotara.canvas.engine.RemoveElementsCommand
import com.arinara.fotara.canvas.engine.ReplaceElementsCommand
import com.arinara.fotara.canvas.engine.ShapeAutoCorrectEngine
import com.arinara.fotara.canvas.engine.StrokeProcessor
import com.arinara.fotara.canvas.engine.TransformElementsCommand
import com.arinara.fotara.canvas.engine.TransformHandlesMath
import com.arinara.fotara.canvas.engine.ViewportState
import com.arinara.fotara.canvas.engine.ViewportTransform
import com.arinara.fotara.canvas.engine.translated
import com.arinara.fotara.canvas.gesture.PointerPoint
import com.arinara.fotara.canvas.model.CanvasDocument
import com.arinara.fotara.canvas.model.CanvasElement
import com.arinara.fotara.canvas.model.CanvasSelection
import com.arinara.fotara.canvas.model.ImageElement
import com.arinara.fotara.canvas.model.SelectedElementReference
import com.arinara.fotara.canvas.model.StrokeBlendMode
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType
import com.arinara.fotara.canvas.model.TextLayerElement
import java.util.UUID
import kotlin.math.hypot

enum class CanvasToolType {
    PEN,
    HIGHLIGHTER,
    ERASER,
    SELECT,
    TEXT,
    SHAPE;

    companion object {
        @Deprecated("Consolidated into ERASER")
        val ERASER_STROKE = ERASER
        @Deprecated("Consolidated into ERASER")
        val ERASER_AREA = ERASER
    }
}

enum class ShapePrimitiveType {
    AUTO,
    RECTANGLE,
    CIRCLE,
    ARROW,
    LINE
}

data class CanvasToolState(
    val activeTool: CanvasToolType = CanvasToolType.PEN,
    val penColor: Long = 0xFFF8FAFC, // Electric Slate Off-White
    val penSize: Float = 4.0f,
    val penAlpha: Float = 1.0f,
    val highlighterColor: Long = 0xFFFBBF24, // Electric Slate Amber Gold
    val highlighterSize: Float = 20.0f,
    val highlighterBlendMode: StrokeBlendMode = StrokeBlendMode.MULTIPLY,
    val eraserRadius: Float = 24.0f,
    val stylusOnlyDrawing: Boolean = false,
    val selection: CanvasSelection = CanvasSelection.Empty,
    val textFontSizeSp: Float = 18f,
    val textColor: Long = 0xFFF8FAFC, // Electric Slate TextPrimary
    val textFontWeight: Int = 400,
    val textBackgroundStyle: com.arinara.fotara.canvas.model.TextBackgroundStyle = com.arinara.fotara.canvas.model.TextBackgroundStyle.TRANSPARENT,
    val autoSmoothen: Boolean = true,
    val shapeSnapping: Boolean = true,
    val smoothingStrength: Float = 0.5f,
    val activeShapePrimitive: ShapePrimitiveType = ShapePrimitiveType.RECTANGLE
) {
    val selectedElementIds: Set<String> get() = selection.elementIds
}

/**
 * Controller executing canvas tool operations through C8 commands (guaranteeing full undo/redo).
 * Manages pen, highlighter, free capsule-sweep area eraser, non-destructive lasso selection,
 * anchor-fixed transform handles, and split materialization on mutation.
 */
class CanvasToolController(
    var toolState: CanvasToolState = CanvasToolState()
) {

    var density: Float = 1.0f
    var activeLayerId: String? = null

    var selection: CanvasSelection
        get() = toolState.selection
        set(value) {
            toolState = toolState.copy(selection = value)
        }

    fun setHighlighterBlendMode(mode: StrokeBlendMode) {
        toolState = toolState.copy(highlighterBlendMode = mode)
    }

    // In-progress drawing stroke points in world coordinates
    private val activeStrokePoints = mutableListOf<StrokePoint>()
    val currentPoints: List<StrokePoint> get() = activeStrokePoints

    // Transform gesture tracking
    var transformStartState: TransformHandlesMath.TransformStartState? = null
        private set
    var previewElements: List<CanvasElement>? = null
        private set
    var previewSelection: CanvasSelection? = null
        private set
    val selectionToDraw: CanvasSelection get() = previewSelection ?: selection

    private val partialOutsideStrokes = mutableMapOf<String, List<StrokeElement>>()
    private val partialInsideStrokes = mutableMapOf<String, List<StrokeElement>>()

    // Free eraser sweep tracking (aggregates an entire drag into a single undo step)
    private val eraserTouchedOriginals = mutableMapOf<String, StrokeElement>()
    private val eraserActiveSurvivingMap = mutableMapOf<String, StrokeElement>()
    private val eraserTouchedNonStrokes = mutableMapOf<String, CanvasElement>()
    private var lastEraserPoint: Pair<Float, Float>? = null
    val activeEraserPoints = mutableListOf<StrokePoint>()
    private var hasErasedOverNonStrokes = false
    private var lastEraserWorldRadius: Float = 24.0f

    val activeEraserStrokePoints: List<StrokePoint>
        get() = activeEraserPoints

    var isShapeSnapped: Boolean = false

    fun isDrawing(): Boolean = activeStrokePoints.isNotEmpty()

    /**
     * Begins an in-progress stroke in world coordinates.
     */
    fun startStroke(worldX: Float, worldY: Float, rawPressure: Float) {
        isShapeSnapped = false
        activeStrokePoints.clear()
        val normalizedP = StrokeProcessor.normalizePressure(rawPressure)
        activeStrokePoints.add(StrokePoint(worldX, worldY, normalizedP))
    }

    /**
     * Appends streamed points (including high-frequency digitizer batch points).
     */
    fun appendPoints(points: List<PointerPoint>, viewport: ViewportState) {
        for (p in points) {
            val (wx, wy) = ViewportTransform.screenToWorld(p.x, p.y, viewport)
            val normalizedP = StrokeProcessor.normalizePressure(p.pressure)
            activeStrokePoints.add(StrokePoint(wx, wy, normalizedP))
        }
    }

    fun replaceCurrentStrokePoints(newPoints: List<StrokePoint>) {
        activeStrokePoints.clear()
        activeStrokePoints.addAll(newPoints)
    }

    /**
     * Attempts shape auto-correction and Bézier smoothing on the active in-progress stroke.
     * Returns true if a shape was snapped and stroke points replaced.
     */
    fun autoCorrectActiveStroke(): Boolean {
        if ((toolState.activeTool != CanvasToolType.PEN && toolState.activeTool != CanvasToolType.SHAPE) || activeStrokePoints.size < 5) return false
        val result = com.arinara.fotara.canvas.engine.ShapeAutoCorrectEngine.recognizeAndSnap(activeStrokePoints)
        if (result.shape !is com.arinara.fotara.canvas.engine.RecognizedShape.None) {
            activeStrokePoints.clear()
            activeStrokePoints.addAll(result.snappedPoints)
            isShapeSnapped = true
            return true
        }
        return false
    }

    fun nextZIndexForLayer(document: CanvasDocument, layerId: String): Int {
        return (document.elements.filter { it.layerId == layerId }.maxOfOrNull { it.zIndex } ?: 0) + 1
    }

    /**
     * Commits the in-progress stroke into the document via an AddElementsCommand.
     * Applies decimation, smoothing, and bounds computation.
     */
    fun finishStroke(
        document: CanvasDocument,
        historyManager: CanvasHistoryManager,
        viewportScale: Float = 1.0f
    ): Pair<CanvasDocument, CanvasRect> {
        if (activeStrokePoints.isEmpty()) {
            return Pair(document, CanvasRect.Empty)
        }

        // Zoom-aware decimation: screen tolerance converted to world units
        val screenTolerance = 0.8f
        val worldTolerance = screenTolerance / viewportScale.coerceAtLeast(0.05f)
        val decimated = StrokeProcessor.decimatePoints(activeStrokePoints, tolerance = worldTolerance)

        val toolType = when (toolState.activeTool) {
            CanvasToolType.HIGHLIGHTER -> StrokeToolType.HIGHLIGHTER
            else -> StrokeToolType.PEN
        }

        // Automatic drawing smoothing & shape recognition on finger lift for Pen and Shape strokes
        val finalPoints = if (toolType == StrokeToolType.PEN) {
            if (toolState.activeTool == CanvasToolType.SHAPE && decimated.size >= 2) {
                val p0 = decimated.first()
                val pn = decimated.last()
                when (toolState.activeShapePrimitive) {
                    ShapePrimitiveType.RECTANGLE -> {
                        ShapeAutoCorrectEngine.generateRectangleFromBounds(p0, pn, 1.0f)
                    }
                    ShapePrimitiveType.CIRCLE -> {
                        ShapeAutoCorrectEngine.generateCircleFromBounds(p0, pn, 1.0f)
                    }
                    ShapePrimitiveType.ARROW -> {
                        ShapeAutoCorrectEngine.generateArrowFromEndpoints(p0, pn, 1.0f)
                    }
                    ShapePrimitiveType.LINE -> {
                        ShapeAutoCorrectEngine.generateLinePoints(p0, pn, 1.0f)
                    }
                    ShapePrimitiveType.AUTO -> {
                        val snapResult = ShapeAutoCorrectEngine.recognizeAndSnap(decimated)
                        if (snapResult.shape !is RecognizedShape.None) {
                            snapResult.snappedPoints
                        } else {
                            ShapeAutoCorrectEngine.smoothPointsBezier(decimated, pressure = 1.0f, isClosed = false)
                        }
                    }
                }
            } else if (isShapeSnapped) {
                decimated
            } else if (toolState.autoSmoothen) {
                ShapeAutoCorrectEngine.smoothPointsBezier(decimated, pressure = 1.0f, isClosed = false)
            } else {
                decimated
            }
        } else {
            decimated
        }
        val strokeWidth = if (toolType == StrokeToolType.HIGHLIGHTER) toolState.highlighterSize else toolState.penSize
        val strokeColor = if (toolType == StrokeToolType.HIGHLIGHTER) toolState.highlighterColor else toolState.penColor

        val strokeBounds = StrokeProcessor.computeBounds(finalPoints, strokeWidth)
        val targetLayerId = activeLayerId ?: document.getPrimaryLayerId()
        val blendMode = if (toolType == StrokeToolType.HIGHLIGHTER) {
            toolState.highlighterBlendMode
        } else {
            StrokeBlendMode.NORMAL
        }

        val newStroke = StrokeElement(
            id = UUID.randomUUID().toString(),
            layerId = targetLayerId,
            points = finalPoints,
            color = strokeColor,
            width = strokeWidth,
            toolType = toolType,
            blendMode = blendMode,
            bounds = strokeBounds,
            zIndex = nextZIndexForLayer(document, targetLayerId)
        )

        activeStrokePoints.clear()
        isShapeSnapped = false

        val updatedDoc = historyManager.execute(
            AddElementsCommand(listOf(newStroke), description = "Draw Stroke"),
            document
        )

        return Pair(updatedDoc, strokeBounds)
    }

    fun cancelStroke() {
        activeStrokePoints.clear()
        isShapeSnapped = false
    }

    // ==========================================
    // Free Eraser (Capsule Sweep, 1 Undo Step per Drag)
    // ==========================================

    private fun capsuleIntersectsRect(
        x1: Float, y1: Float,
        x2: Float, y2: Float,
        radius: Float,
        rect: CanvasRect
    ): Boolean {
        val expandedLeft = rect.left - radius
        val expandedTop = rect.top - radius
        val expandedRight = rect.right + radius
        val expandedBottom = rect.bottom + radius

        val segMinX = minOf(x1, x2)
        val segMaxX = maxOf(x1, x2)
        val segMinY = minOf(y1, y2)
        val segMaxY = maxOf(y1, y2)

        return !(segMaxX < expandedLeft || segMinX > expandedRight || segMaxY < expandedTop || segMinY > expandedBottom)
    }

    fun startEraser(screenX: Float, screenY: Float) {
        eraserTouchedOriginals.clear()
        eraserActiveSurvivingMap.clear()
        eraserTouchedNonStrokes.clear()
        activeEraserPoints.clear()
        hasErasedOverNonStrokes = false
        lastEraserPoint = Pair(screenX, screenY)
    }

    fun sweepEraser(
        points: List<PointerPoint>,
        viewport: ViewportState,
        document: CanvasDocument
    ): Pair<CanvasDocument, CanvasRect?> {
        if (points.isEmpty()) return Pair(document, null)

        val targetLayerId = activeLayerId ?: document.getPrimaryLayerId()
        val worldRadius = (toolState.eraserRadius / viewport.scale).coerceAtLeast(4f)
        lastEraserWorldRadius = worldRadius
        val minRemainder = (4.0f * density) / viewport.scale.coerceAtLeast(0.01f)
        val layerMap = document.layers.associateBy { it.id }

        var dirtyBounds: CanvasRect? = null

        // Initialize active lookup on first move of drag strictly for active layer
        if (eraserTouchedOriginals.isEmpty() && eraserActiveSurvivingMap.isEmpty() && eraserTouchedNonStrokes.isEmpty()) {
            val eligibleStrokes = document.elements
                .filterIsInstance<StrokeElement>()
                .filter { stroke ->
                    val layer = layerMap[stroke.layerId]
                    val matchesLayer = stroke.layerId == targetLayerId
                    layer != null && layer.isVisible && !layer.isLocked && matchesLayer
                }
            for (s in eligibleStrokes) {
                eraserActiveSurvivingMap[s.id] = s
            }
        }

        var prev = lastEraserPoint ?: Pair(points.first().x, points.first().y)

        val nonStrokes = document.elements.filter {
            (it is ImageElement || it is TextLayerElement) && it.layerId == targetLayerId
        }

        for (pt in points) {
            val (wx1, wy1) = ViewportTransform.screenToWorld(prev.first, prev.second, viewport)
            val (wx2, wy2) = ViewportTransform.screenToWorld(pt.x, pt.y, viewport)

            activeEraserPoints.add(StrokePoint(wx2, wy2, pt.pressure))

            // Check if eraser brush traverses across image elements or text blocks
            for (ns in nonStrokes) {
                if (eraserTouchedNonStrokes.containsKey(ns.id)) continue
                val isHit = when (ns) {
                    is ImageElement -> hitTestImage(wx2, wy2, ns) || ns.bounds.expanded(worldRadius).contains(wx2, wy2)
                    is TextLayerElement -> ns.computeDynamicBounds().expanded(worldRadius).contains(wx2, wy2)
                    else -> false
                }
                if (isHit) {
                    hasErasedOverNonStrokes = true
                    eraserTouchedNonStrokes[ns.id] = ns
                    val nsBounds = if (ns is ImageElement) ns.bounds else (ns as TextLayerElement).computeDynamicBounds()
                    dirtyBounds = dirtyBounds?.union(nsBounds) ?: nsBounds
                }
            }

            if (hasErasedOverNonStrokes) {
                val ptBounds = CanvasRect(wx2 - worldRadius, wy2 - worldRadius, wx2 + worldRadius, wy2 + worldRadius)
                dirtyBounds = dirtyBounds?.union(ptBounds) ?: ptBounds
            }

            val strokesToCheck = eraserActiveSurvivingMap.values.toList()
            for (stroke in strokesToCheck) {
                val splitParts = StrokeProcessor.eraseStrokeWithCapsule(
                    stroke = stroke,
                    ax = wx1, ay = wy1,
                    bx = wx2, by = wy2,
                    eraserRadius = worldRadius,
                    minRemainder = minRemainder
                )

                // If stroke was modified
                if (splitParts.size != 1 || splitParts[0] !== stroke) {
                    dirtyBounds = dirtyBounds?.union(stroke.bounds) ?: stroke.bounds

                    // Record original stroke from document
                    val origInDoc = document.elements.find { it.id == stroke.id }
                    if (origInDoc is StrokeElement && !eraserTouchedOriginals.containsKey(stroke.id)) {
                        eraserTouchedOriginals[stroke.id] = origInDoc
                    }

                    eraserActiveSurvivingMap.remove(stroke.id)
                    for (part in splitParts) {
                        eraserActiveSurvivingMap[part.id] = part
                    }
                }
            }

            prev = Pair(pt.x, pt.y)
        }

        lastEraserPoint = prev

        if (dirtyBounds != null || hasErasedOverNonStrokes) {
            // Live preview: preserve non-strokes and render live carve DST_OUT stroke along eraser path
            val untouched = document.elements.filter {
                it.id !in eraserTouchedOriginals.keys
            }
            val liveCarveStroke = if (hasErasedOverNonStrokes && activeEraserPoints.isNotEmpty()) {
                val carveLayerId = eraserTouchedNonStrokes.values.firstOrNull()?.layerId
                    ?: targetLayerId
                val pts = if (activeEraserPoints.size == 1) {
                    val p = activeEraserPoints[0]
                    listOf(p, StrokePoint(p.x + 0.1f, p.y + 0.1f, p.pressure))
                } else activeEraserPoints.toList()
                val strokeWidth = (worldRadius * 2f).coerceAtLeast(8f)
                val strokeBounds = StrokeProcessor.computeBounds(pts, strokeWidth)
                listOf(
                    StrokeElement(
                        id = "__eraser_live_carve__",
                        layerId = carveLayerId,
                        points = pts,
                        color = 0xFF000000,
                        width = strokeWidth,
                        toolType = StrokeToolType.ERASER,
                        blendMode = StrokeBlendMode.DST_OUT,
                        bounds = strokeBounds
                    )
                )
            } else emptyList()

            val liveDoc = document.copy(elements = untouched + eraserActiveSurvivingMap.values + liveCarveStroke)
            return Pair(liveDoc, dirtyBounds)
        }

        return Pair(document, null)
    }

    fun finishEraser(
        document: CanvasDocument,
        historyManager: CanvasHistoryManager
    ): Pair<CanvasDocument, CanvasRect?> {
        lastEraserPoint = null

        val strokeOriginals = eraserTouchedOriginals.values.toList()
        val originalIds = strokeOriginals.map { it.id }.toSet()
        val survivingStrokes = eraserActiveSurvivingMap.values.filter { it.id !in originalIds }

        // If non-strokes were touched by the eraser path, add a DST_OUT stroke carving out along the path
        val eraserCarveStroke: StrokeElement? = if (hasErasedOverNonStrokes && activeEraserPoints.isNotEmpty()) {
            val carveLayerId = eraserTouchedNonStrokes.values.firstOrNull()?.layerId
                ?: activeLayerId
                ?: document.getPrimaryLayerId()
            val pts = if (activeEraserPoints.size == 1) {
                val p = activeEraserPoints[0]
                listOf(p, StrokePoint(p.x + 0.1f, p.y + 0.1f, p.pressure))
            } else activeEraserPoints.toList()
            val strokeWidth = (lastEraserWorldRadius * 2f).coerceAtLeast(8f)
            val strokeBounds = StrokeProcessor.computeBounds(pts, strokeWidth)
            StrokeElement(
                id = UUID.randomUUID().toString(),
                layerId = carveLayerId,
                points = pts,
                color = 0xFF000000,
                width = strokeWidth,
                toolType = StrokeToolType.ERASER,
                blendMode = StrokeBlendMode.DST_OUT,
                bounds = strokeBounds
            )
        } else null

        val replacements = if (eraserCarveStroke != null) {
            survivingStrokes + eraserCarveStroke
        } else {
            survivingStrokes
        }

        if (strokeOriginals.isEmpty() && eraserCarveStroke == null) {
            eraserActiveSurvivingMap.clear()
            eraserTouchedNonStrokes.clear()
            activeEraserPoints.clear()
            hasErasedOverNonStrokes = false
            return Pair(document.copy(elements = document.elements.filter { it.id != "__eraser_live_carve__" }), null)
        }

        val allAffectedBounds = mutableListOf<CanvasRect>()
        strokeOriginals.forEach { allAffectedBounds.add(it.bounds) }
        if (eraserCarveStroke != null) {
            allAffectedBounds.add(eraserCarveStroke.bounds)
            eraserTouchedNonStrokes.values.forEach { allAffectedBounds.add(it.bounds) }
        }

        val unionBounds = allAffectedBounds.fold(CanvasRect.Empty) { acc, cur -> acc.union(cur) }

        // Ensure we execute against the document containing original elements, excluding transient preview elements
        val cleanElements = document.elements.filter { it.id != "__eraser_live_carve__" }
        val baseDoc = if (cleanElements.any { it.id in originalIds }) {
            document.copy(elements = cleanElements)
        } else {
            val survivingIds = survivingStrokes.map { it.id }.toSet()
            val untouched = cleanElements.filter { it.id !in survivingIds }
            document.copy(elements = untouched + strokeOriginals)
        }

        val committedDoc = historyManager.execute(
            ReplaceElementsCommand(strokeOriginals, replacements, description = "Free Eraser"),
            baseDoc
        )

        eraserTouchedOriginals.clear()
        eraserActiveSurvivingMap.clear()
        eraserTouchedNonStrokes.clear()
        activeEraserPoints.clear()
        hasErasedOverNonStrokes = false

        return Pair(committedDoc, unionBounds)
    }

    fun cancelEraser(): CanvasDocument? {
        lastEraserPoint = null
        val hadTouches = eraserTouchedOriginals.isNotEmpty() || hasErasedOverNonStrokes
        eraserTouchedOriginals.clear()
        eraserActiveSurvivingMap.clear()
        eraserTouchedNonStrokes.clear()
        activeEraserPoints.clear()
        hasErasedOverNonStrokes = false
        return if (hadTouches) null else null
    }

    fun eraseAt(
        screenX: Float,
        screenY: Float,
        viewport: ViewportState,
        document: CanvasDocument,
        historyManager: CanvasHistoryManager
    ): Pair<CanvasDocument, CanvasRect?> {
        startEraser(screenX, screenY)
        val (previewDoc, _) = sweepEraser(listOf(PointerPoint(screenX, screenY)), viewport, document)
        return finishEraser(previewDoc, historyManager)
    }

    // ==========================================
    // Selection (Tap & Lasso with Non-Zero Winding)
    // ==========================================

    fun selectTap(
        screenX: Float,
        screenY: Float,
        viewport: ViewportState,
        document: CanvasDocument
    ): CanvasSelection {
        val (worldX, worldY) = ViewportTransform.screenToWorld(screenX, screenY, viewport)
        val layerMap = document.layers.associateBy { it.id }

        val hitElement = document.elements
            .sortedWith(
                compareByDescending<CanvasElement> { layerMap[it.layerId]?.order ?: 0 }
                    .thenByDescending { it.zIndex }
                    .thenByDescending { it.id }
            )
            .firstOrNull { el ->
                val layer = layerMap[el.layerId]
                if (layer == null || !layer.isVisible || layer.isLocked) return@firstOrNull false

                when (el) {
                    is StrokeElement -> StrokeProcessor.hitTestStroke(worldX, worldY, el, hitRadius = 8f / viewport.scale)
                    is ImageElement -> hitTestImage(worldX, worldY, el)
                    is TextLayerElement -> hitTestText(worldX, worldY, el)
                }
            }

        val newSel = if (hitElement != null) {
            val ref = SelectedElementReference.Whole(hitElement.id)
            val rot = when (hitElement) {
                is ImageElement -> hitElement.rotationDegrees
                is TextLayerElement -> hitElement.rotationDegrees
                else -> 0f
            }
            val selBounds = if (hitElement is TextLayerElement) {
                hitElement.computeDynamicBounds()
            } else {
                hitElement.bounds
            }
            CanvasSelection(
                references = mapOf(hitElement.id to ref),
                bounds = selBounds,
                rotationDegrees = rot
            )
        } else {
            CanvasSelection.Empty
        }

        selection = newSel
        return newSel
    }

    fun hitTestImage(worldX: Float, worldY: Float, el: ImageElement): Boolean {
        if (el.rotationDegrees == 0f) {
            return el.bounds.contains(worldX, worldY)
        }
        val rad = Math.toRadians(el.rotationDegrees.toDouble()).toFloat()
        val centerX = el.x + el.width / 2f
        val centerY = el.y + el.height / 2f
        val dx = worldX - centerX
        val dy = worldY - centerY
        val localX = dx * kotlin.math.cos(-rad) - dy * kotlin.math.sin(-rad)
        val localY = dx * kotlin.math.sin(-rad) + dy * kotlin.math.cos(-rad)
        return kotlin.math.abs(localX) <= el.width / 2f && kotlin.math.abs(localY) <= el.height / 2f
    }

    fun hitTestText(worldX: Float, worldY: Float, el: TextLayerElement): Boolean {
        val dynBounds = el.computeDynamicBounds()
        if (el.rotationDegrees == 0f) {
            val minX = minOf(el.x, dynBounds.left)
            val maxX = maxOf(el.x + dynBounds.width, dynBounds.right)
            val minY = minOf(el.y, dynBounds.top)
            val maxY = maxOf(el.y + dynBounds.height, dynBounds.bottom)
            return worldX in minX..maxX && worldY in minY..maxY
        }
        val rad = Math.toRadians(el.rotationDegrees.toDouble()).toFloat()
        val centerX = el.x + dynBounds.width / 2f
        val centerY = el.y + dynBounds.height / 2f
        val dx = worldX - centerX
        val dy = worldY - centerY
        val localX = dx * kotlin.math.cos(-rad) - dy * kotlin.math.sin(-rad)
        val localY = dx * kotlin.math.sin(-rad) + dy * kotlin.math.cos(-rad)
        return kotlin.math.abs(localX) <= dynBounds.width / 2f && kotlin.math.abs(localY) <= dynBounds.height / 2f
    }

    fun selectLasso(
        lassoScreenPoints: List<Pair<Float, Float>>,
        viewport: ViewportState,
        document: CanvasDocument
    ): CanvasSelection {
        if (lassoScreenPoints.size < 3) {
            selection = CanvasSelection.Empty
            return CanvasSelection.Empty
        }

        val worldPolygon = lassoScreenPoints.map {
            ViewportTransform.screenToWorld(it.first, it.second, viewport)
        }
        val layerMap = document.layers.associateBy { it.id }

        val selectedRefs = mutableMapOf<String, SelectedElementReference>()

        for (el in document.elements) {
            val layer = layerMap[el.layerId]
            if (layer == null || !layer.isVisible || layer.isLocked) continue

            when (el) {
                is ImageElement -> {
                    if (CanvasSelectionEngine.isPointInPolygonWinding(el.bounds.centerX, el.bounds.centerY, worldPolygon)) {
                        selectedRefs[el.id] = SelectedElementReference.Whole(el.id)
                    }
                }
                is StrokeElement -> {
                    val ref = CanvasSelectionEngine.sliceStrokeWithLasso(el, worldPolygon, viewport.scale, density)
                    if (ref != null) {
                        selectedRefs[el.id] = ref
                    }
                }
                is TextLayerElement -> {
                    if (CanvasSelectionEngine.isPointInPolygonWinding(el.bounds.centerX, el.bounds.centerY, worldPolygon)) {
                        selectedRefs[el.id] = SelectedElementReference.Whole(el.id)
                    }
                }
            }
        }

        val computedBounds = CanvasSelectionEngine.computeSelectionBounds(selectedRefs, document.elements)
        val newSel = CanvasSelection(
            references = selectedRefs,
            bounds = computedBounds,
            rotationDegrees = 0f
        )
        selection = newSel
        return newSel
    }

    fun clearSelection() {
        selection = CanvasSelection.Empty
        transformStartState = null
        previewElements = null
        previewSelection = null
        partialOutsideStrokes.clear()
        partialInsideStrokes.clear()
    }

    // ==========================================
    // Handle Hit-Testing & Live Transform Preview
    // ==========================================

    fun hitTestHandles(
        screenX: Float,
        screenY: Float,
        viewport: ViewportState,
        density: Float = 1.0f
    ): Int? {
        if (selection.isEmpty) return null
        return TransformHandlesMath.hitTest(
            screenX = screenX,
            screenY = screenY,
            bounds = selection.bounds,
            rotationDegrees = selection.rotationDegrees,
            viewport = viewport,
            density = density
        )
    }

    fun hitTestHandles(
        screenX: Float,
        screenY: Float,
        bounds: CanvasRect,
        viewport: ViewportState,
        density: Float = 1.0f
    ): Int? {
        if (bounds.isEmpty) return null
        return TransformHandlesMath.hitTest(
            screenX = screenX,
            screenY = screenY,
            bounds = bounds,
            rotationDegrees = 0f,
            viewport = viewport,
            density = density
        )
    }

    fun startTransformGesture(
        startScreenX: Float,
        startScreenY: Float,
        viewport: ViewportState,
        document: CanvasDocument
    ) {
        if (selection.isEmpty) return

        partialOutsideStrokes.clear()
        partialInsideStrokes.clear()
        previewSelection = null

        val originalStrokes = mutableMapOf<String, List<StrokePoint>>()
        val originalImageBounds = mutableMapOf<String, CanvasRect>()
        val originalImageRotations = mutableMapOf<String, Float>()
        val originalImages = mutableMapOf<String, ImageElement>()
        val originalTexts = mutableMapOf<String, TextLayerElement>()
        val elMap = document.elements.associateBy { it.id }

        for ((id, ref) in selection.references) {
            val el = elMap[id] ?: continue
            when (el) {
                is StrokeElement -> {
                    when (ref) {
                        is SelectedElementReference.Whole -> originalStrokes[id] = el.points
                        is SelectedElementReference.PartialStroke -> {
                            val (outside, inside) = CanvasSelectionEngine.materializePartialSplit(el, ref, viewport.scale, density)
                            partialOutsideStrokes[id] = outside
                            partialInsideStrokes[id] = inside
                            originalStrokes[id] = inside.flatMap { it.points }
                        }
                    }
                }
                is ImageElement -> {
                    originalImageBounds[id] = el.bounds
                    originalImageRotations[id] = el.rotationDegrees
                    originalImages[id] = el
                }
                is TextLayerElement -> {
                    originalTexts[id] = el
                }
            }
        }

        transformStartState = TransformHandlesMath.TransformStartState(
            originalStrokes = originalStrokes,
            originalImageBounds = originalImageBounds,
            originalImageRotations = originalImageRotations,
            originalImages = originalImages,
            originalTexts = originalTexts,
            worldCenter = Pair(selection.bounds.centerX, selection.bounds.centerY),
            worldWidth = selection.bounds.width,
            worldHeight = selection.bounds.height,
            rotationDegrees = selection.rotationDegrees,
            startScreenX = startScreenX,
            startScreenY = startScreenY
        )
    }

    fun updateTransformPreview(
        handleId: Int,
        currentScreenX: Float,
        currentScreenY: Float,
        viewport: ViewportState,
        document: CanvasDocument,
        density: Float = 1.0f
    ): List<CanvasElement>? {
        val startState = transformStartState ?: return null
        val elMap = document.elements.associateBy { it.id }
        val preview = mutableListOf<CanvasElement>()

        for ((id, ref) in selection.references) {
            val el = elMap[id] ?: continue
            when (el) {
                is StrokeElement -> {
                    when (ref) {
                        is SelectedElementReference.Whole -> {
                            val origPts = startState.originalStrokes[id] ?: el.points
                            val transformedPts = TransformHandlesMath.transformStrokePoints(
                                originalPoints = origPts,
                                handleId = handleId,
                                startState = startState,
                                currentScreenX = currentScreenX,
                                currentScreenY = currentScreenY,
                                viewport = viewport,
                                density = density
                            )
                            val newBounds = StrokeProcessor.computeBounds(transformedPts, el.width)
                            preview.add(el.copy(points = transformedPts, bounds = newBounds))
                        }
                        is SelectedElementReference.PartialStroke -> {
                            val outside = partialOutsideStrokes[id] ?: emptyList()
                            preview.addAll(outside)

                            val insideList = partialInsideStrokes[id] ?: emptyList()
                            for (inside in insideList) {
                                val transformedPts = TransformHandlesMath.transformStrokePoints(
                                    originalPoints = inside.points,
                                    handleId = handleId,
                                    startState = startState,
                                    currentScreenX = currentScreenX,
                                    currentScreenY = currentScreenY,
                                    viewport = viewport,
                                    density = density
                                )
                                val newBounds = StrokeProcessor.computeBounds(transformedPts, inside.width)
                                preview.add(inside.copy(points = transformedPts, bounds = newBounds))
                            }
                        }
                    }
                }
                is ImageElement -> {
                    val onlyImages = selection.references.isNotEmpty() && selection.references.keys.all { elMap[it] is ImageElement }
                    val origImg = startState.originalImages[id] ?: el
                    val transformed = TransformHandlesMath.transformImageElement(
                        originalImage = origImg,
                        handleId = handleId,
                        startState = startState,
                        currentScreenX = currentScreenX,
                        currentScreenY = currentScreenY,
                        viewport = viewport,
                        density = density,
                        isOnlyImages = onlyImages
                    )
                    preview.add(transformed)
                }
                is TextLayerElement -> {
                    val origTxt = startState.originalTexts[id] ?: el
                    val transformed = TransformHandlesMath.transformTextElement(
                        originalText = origTxt,
                        handleId = handleId,
                        startState = startState,
                        currentScreenX = currentScreenX,
                        currentScreenY = currentScreenY,
                        viewport = viewport,
                        density = density
                    )
                    preview.add(transformed)
                }
            }
        }

        // Live preview of selection box
        val onlySingleBox = preview.size == 1 && (preview[0] is ImageElement || preview[0] is TextLayerElement)
        previewSelection = if (onlySingleBox && preview[0] is ImageElement) {
            val img = preview[0] as ImageElement
            selection.copy(
                bounds = img.bounds,
                rotationDegrees = img.rotationDegrees
            )
        } else if (onlySingleBox && preview[0] is TextLayerElement) {
            val txt = preview[0] as TextLayerElement
            selection.copy(
                bounds = txt.computeDynamicBounds(),
                rotationDegrees = txt.rotationDegrees
            )
        } else if (handleId == -1) {
            val worldDx = (currentScreenX - startState.startScreenX) / viewport.scale
            val worldDy = (currentScreenY - startState.startScreenY) / viewport.scale
            selection.copy(bounds = selection.bounds.translated(worldDx, worldDy))
        } else if (handleId == 8) {
            val (scx, scy) = ViewportTransform.worldToScreen(startState.worldCenter.first, startState.worldCenter.second, viewport)
            val startAngle = kotlin.math.atan2(startState.startScreenY - scy, startState.startScreenX - scx)
            val curAngle = kotlin.math.atan2(currentScreenY - scy, currentScreenX - scx)
            val deltaDeg = Math.toDegrees((curAngle - startAngle).toDouble()).toFloat()
            selection.copy(rotationDegrees = (startState.rotationDegrees + deltaDeg) % 360f)
        } else {
            val pb = CanvasSelectionEngine.computeSelectionBounds(selection.references, preview)
            selection.copy(bounds = pb)
        }

        previewElements = preview
        return preview
    }

    fun commitTransform(
        handleId: Int,
        totalDeltaX: Float,
        totalDeltaY: Float,
        viewport: ViewportState,
        document: CanvasDocument,
        historyManager: CanvasHistoryManager,
        density: Float = 1.0f
    ): Pair<CanvasDocument, CanvasRect?> {
        val startState = transformStartState
        transformStartState = null
        previewElements = null
        previewSelection = null

        if (startState == null || (hypot(totalDeltaX, totalDeltaY) < 1.0f && handleId == -1)) {
            partialOutsideStrokes.clear()
            partialInsideStrokes.clear()
            return Pair(document, null)
        }

        val elMap = document.elements.associateBy { it.id }
        val beforeElements = mutableListOf<CanvasElement>()
        val afterElements = mutableListOf<CanvasElement>()

        var oldUnionBounds = selection.bounds
        val newReferences = mutableMapOf<String, SelectedElementReference>()

        for ((id, ref) in selection.references) {
            val el = elMap[id] ?: continue
            beforeElements.add(el)

            when (el) {
                is StrokeElement -> {
                    when (ref) {
                        is SelectedElementReference.Whole -> {
                            val origPts = startState.originalStrokes[id] ?: el.points
                            val transformedPts = TransformHandlesMath.transformStrokePoints(
                                originalPoints = origPts,
                                handleId = handleId,
                                startState = startState,
                                currentScreenX = startState.startScreenX + totalDeltaX,
                                currentScreenY = startState.startScreenY + totalDeltaY,
                                viewport = viewport,
                                density = density
                            )
                            val newBounds = StrokeProcessor.computeBounds(transformedPts, el.width)
                            val transformedStroke = el.copy(points = transformedPts, bounds = newBounds)
                            afterElements.add(transformedStroke)
                            newReferences[transformedStroke.id] = SelectedElementReference.Whole(transformedStroke.id)
                        }
                        is SelectedElementReference.PartialStroke -> {
                            val outside = partialOutsideStrokes[id] ?: emptyList()
                            afterElements.addAll(outside)

                            val insideList = partialInsideStrokes[id] ?: emptyList()
                            for (inside in insideList) {
                                val transformedPts = TransformHandlesMath.transformStrokePoints(
                                    originalPoints = inside.points,
                                    handleId = handleId,
                                    startState = startState,
                                    currentScreenX = startState.startScreenX + totalDeltaX,
                                    currentScreenY = startState.startScreenY + totalDeltaY,
                                    viewport = viewport,
                                    density = density
                                )
                                val newBounds = StrokeProcessor.computeBounds(transformedPts, inside.width)
                                val transformedInside = inside.copy(points = transformedPts, bounds = newBounds)
                                afterElements.add(transformedInside)
                                newReferences[transformedInside.id] = SelectedElementReference.Whole(transformedInside.id)
                            }
                        }
                    }
                }
                is ImageElement -> {
                    val onlyImages = selection.references.isNotEmpty() && selection.references.keys.all { elMap[it] is ImageElement }
                    val origImg = startState.originalImages[id] ?: el
                    val transformedImage = TransformHandlesMath.transformImageElement(
                        originalImage = origImg,
                        handleId = handleId,
                        startState = startState,
                        currentScreenX = startState.startScreenX + totalDeltaX,
                        currentScreenY = startState.startScreenY + totalDeltaY,
                        viewport = viewport,
                        density = density,
                        isOnlyImages = onlyImages
                    )
                    afterElements.add(transformedImage)
                    newReferences[transformedImage.id] = SelectedElementReference.Whole(transformedImage.id)
                }
                is TextLayerElement -> {
                    val origTxt = startState.originalTexts[id] ?: el
                    val transformedText = TransformHandlesMath.transformTextElement(
                        originalText = origTxt,
                        handleId = handleId,
                        startState = startState,
                        currentScreenX = startState.startScreenX + totalDeltaX,
                        currentScreenY = startState.startScreenY + totalDeltaY,
                        viewport = viewport,
                        density = density
                    )
                    afterElements.add(transformedText)
                    newReferences[transformedText.id] = SelectedElementReference.Whole(transformedText.id)
                }
            }
        }

        partialOutsideStrokes.clear()
        partialInsideStrokes.clear()

        if (beforeElements.isEmpty()) return Pair(document, null)

        val updatedDoc = historyManager.execute(
            ReplaceElementsCommand(
                before = beforeElements,
                after = afterElements,
                description = "Transform Selection"
            ),
            document
        )

        val newBounds = if (afterElements.size == 1 && afterElements[0] is ImageElement) {
            val img = afterElements[0] as ImageElement
            selection = CanvasSelection(
                references = newReferences,
                bounds = img.bounds,
                rotationDegrees = img.rotationDegrees
            )
            img.bounds
        } else if (afterElements.size == 1 && afterElements[0] is TextLayerElement) {
            val txt = afterElements[0] as TextLayerElement
            val dynBounds = txt.computeDynamicBounds()
            selection = CanvasSelection(
                references = newReferences,
                bounds = dynBounds,
                rotationDegrees = txt.rotationDegrees
            )
            dynBounds
        } else {
            val nb = CanvasSelectionEngine.computeSelectionBounds(newReferences, updatedDoc.elements)
            val finalRot = if (handleId == 8) {
                val (scx, scy) = ViewportTransform.worldToScreen(startState.worldCenter.first, startState.worldCenter.second, viewport)
                val startAngle = kotlin.math.atan2(startState.startScreenY - scy, startState.startScreenX - scx)
                val curAngle = kotlin.math.atan2(startState.startScreenY + totalDeltaY - scy, startState.startScreenX + totalDeltaX - scx)
                (startState.rotationDegrees + Math.toDegrees((curAngle - startAngle).toDouble()).toFloat()) % 360f
            } else {
                startState.rotationDegrees
            }

            selection = CanvasSelection(
                references = newReferences,
                bounds = nb,
                rotationDegrees = finalRot
            )
            nb
        }

        val dirtyBounds = oldUnionBounds.union(newBounds)
        return Pair(updatedDoc, dirtyBounds)
    }

    fun commitTransformGesture(
        handleId: Int,
        totalDeltaX: Float,
        totalDeltaY: Float,
        viewport: ViewportState,
        document: CanvasDocument,
        historyManager: CanvasHistoryManager,
        density: Float = 1.0f
    ): Pair<CanvasDocument, CanvasRect?> = commitTransform(handleId, totalDeltaX, totalDeltaY, viewport, document, historyManager, density)

    fun cancelTransformGesture() {
        transformStartState = null
        previewElements = null
        previewSelection = null
        partialOutsideStrokes.clear()
        partialInsideStrokes.clear()
    }

    // ==========================================
    // Contextual Selection Actions (Delete, Duplicate, Move to Layer)
    // ==========================================

    fun deleteSelection(
        document: CanvasDocument,
        historyManager: CanvasHistoryManager,
        viewportScale: Float = 1.0f
    ): Pair<CanvasDocument, CanvasRect?> {
        if (selection.isEmpty) return Pair(document, null)

        val targetLayerId = activeLayerId ?: document.getPrimaryLayerId()
        val elMap = document.elements.associateBy { it.id }
        val beforeElements = mutableListOf<CanvasElement>()
        val afterElements = mutableListOf<CanvasElement>()
        val dirtyBounds = selection.bounds

        for ((id, ref) in selection.references) {
            val el = elMap[id] ?: continue
            if (el.layerId != targetLayerId) {
                // Must not delete elements residing on non-active layers
                continue
            }
            beforeElements.add(el)

            when (el) {
                is StrokeElement -> {
                    when (ref) {
                        is SelectedElementReference.Whole -> {
                            // Fully deleted, adds nothing to afterElements
                        }
                        is SelectedElementReference.PartialStroke -> {
                            // Materialize split: keep only outside surviving strokes
                            val (outsideStrokes, _) = CanvasSelectionEngine.materializePartialSplit(
                                original = el,
                                ref = ref,
                                viewportScale = viewportScale,
                                density = density
                            )
                            afterElements.addAll(outsideStrokes)
                        }
                    }
                }
                is ImageElement -> {
                    // Fully deleted
                }
                else -> {
                    // Fully deleted
                }
            }
        }

        if (beforeElements.isEmpty()) return Pair(document, null)

        val updatedDoc = historyManager.execute(
            ReplaceElementsCommand(
                before = beforeElements,
                after = afterElements,
                description = "Delete Selection"
            ),
            document
        )

        clearSelection()
        return Pair(updatedDoc, dirtyBounds)
    }

    fun duplicateSelection(
        document: CanvasDocument,
        historyManager: CanvasHistoryManager,
        viewportScale: Float = 1.0f
    ): Pair<CanvasDocument, CanvasRect?> {
        if (selection.isEmpty) return Pair(document, null)

        val targetLayerId = activeLayerId ?: document.getPrimaryLayerId()
        val elMap = document.elements.associateBy { it.id }
        val offsetWorld = 24.0f / viewportScale.coerceAtLeast(0.05f)
        val newElements = mutableListOf<CanvasElement>()
        val newReferences = mutableMapOf<String, SelectedElementReference>()
        val layerNextZMap = mutableMapOf<String, Int>()

        fun nextZ(layerId: String): Int {
            val cur = layerNextZMap.getOrPut(layerId) {
                document.elements.filter { it.layerId == layerId }.maxOfOrNull { it.zIndex } ?: 0
            }
            val nxt = cur + 1
            layerNextZMap[layerId] = nxt
            return nxt
        }

        for ((id, ref) in selection.references) {
            val el = elMap[id] ?: continue
            val layerForElement = targetLayerId
            when (el) {
                is StrokeElement -> {
                    when (ref) {
                        is SelectedElementReference.Whole -> {
                            val dup = el.copy(
                                id = UUID.randomUUID().toString(),
                                layerId = layerForElement,
                                points = el.points.map { it.copy(x = it.x + offsetWorld, y = it.y + offsetWorld) },
                                bounds = el.bounds.translated(offsetWorld, offsetWorld),
                                zIndex = nextZ(layerForElement)
                            )
                            newElements.add(dup)
                            newReferences[dup.id] = SelectedElementReference.Whole(dup.id)
                        }
                        is SelectedElementReference.PartialStroke -> {
                            for (seg in ref.insideSegments) {
                                val offsetPts = seg.points.map { it.copy(x = it.x + offsetWorld, y = it.y + offsetWorld) }
                                val b = StrokeProcessor.computeBounds(offsetPts, el.width)
                                val dup = el.copy(
                                    id = UUID.randomUUID().toString(),
                                    layerId = layerForElement,
                                    points = offsetPts,
                                    bounds = b,
                                    zIndex = nextZ(layerForElement)
                                )
                                newElements.add(dup)
                                newReferences[dup.id] = SelectedElementReference.Whole(dup.id)
                            }
                        }
                    }
                }
                is ImageElement -> {
                    val dup = el.copy(
                        id = UUID.randomUUID().toString(),
                        layerId = layerForElement,
                        bounds = el.bounds.translated(offsetWorld, offsetWorld),
                        zIndex = nextZ(layerForElement)
                    )
                    newElements.add(dup)
                    newReferences[dup.id] = SelectedElementReference.Whole(dup.id)
                }
                is TextLayerElement -> {
                    val newId = UUID.randomUUID().toString()
                    val dup = el.copy(
                        id = newId,
                        layerId = layerForElement,
                        x = el.x + offsetWorld,
                        y = el.y + offsetWorld,
                        bounds = el.bounds.translated(offsetWorld, offsetWorld),
                        zIndex = nextZ(layerForElement)
                    )
                    newElements.add(dup)
                    newReferences[dup.id] = SelectedElementReference.Whole(dup.id)
                }
            }
        }

        if (newElements.isEmpty()) return Pair(document, null)

        val updatedDoc = historyManager.execute(
            AddElementsCommand(newElements, description = "Duplicate Selection"),
            document
        )

        val newBounds = CanvasSelectionEngine.computeSelectionBounds(newReferences, updatedDoc.elements)
        selection = CanvasSelection(
            references = newReferences,
            bounds = newBounds,
            rotationDegrees = selection.rotationDegrees
        )

        val dirtyBounds = selection.bounds.union(newBounds)
        return Pair(updatedDoc, dirtyBounds)
    }

    fun moveSelectionToLayer(
        targetLayerId: String,
        document: CanvasDocument,
        historyManager: CanvasHistoryManager,
        viewportScale: Float = 1.0f
    ): Pair<CanvasDocument, CanvasRect?> {
        if (selection.isEmpty) return Pair(document, null)

        val elMap = document.elements.associateBy { it.id }
        val beforeElements = mutableListOf<CanvasElement>()
        val elementsToMove = mutableListOf<CanvasElement>()
        val outsideElements = mutableListOf<CanvasElement>()
        val newReferences = mutableMapOf<String, SelectedElementReference>()

        for ((id, ref) in selection.references) {
            val el = elMap[id] ?: continue
            beforeElements.add(el)

            when (el) {
                is StrokeElement -> {
                    when (ref) {
                        is SelectedElementReference.Whole -> {
                            elementsToMove.add(el)
                        }
                        is SelectedElementReference.PartialStroke -> {
                            val (outsideStrokes, insideStrokes) = CanvasSelectionEngine.materializePartialSplit(
                                original = el,
                                ref = ref,
                                viewportScale = viewportScale,
                                density = density
                            )
                            outsideElements.addAll(outsideStrokes)
                            elementsToMove.addAll(insideStrokes)
                        }
                    }
                }
                is ImageElement -> {
                    elementsToMove.add(el)
                }
                else -> {
                    elementsToMove.add(el)
                }
            }
        }

        if (beforeElements.isEmpty()) return Pair(document, null)

        var topZ = (document.elements.filter { it.layerId == targetLayerId }.maxOfOrNull { it.zIndex } ?: 0)
        val sortedMoved = elementsToMove.sortedBy { it.zIndex }.map { el ->
            topZ++
            el.withLayerId(targetLayerId).withZIndex(topZ)
        }

        val afterElements = mutableListOf<CanvasElement>()
        afterElements.addAll(outsideElements)
        afterElements.addAll(sortedMoved)
        for (m in sortedMoved) {
            newReferences[m.id] = SelectedElementReference.Whole(m.id)
        }

        val updatedDoc = historyManager.execute(
            ReplaceElementsCommand(
                before = beforeElements,
                after = afterElements,
                description = "Move Selection to Layer"
            ),
            document
        )

        val newBounds = CanvasSelectionEngine.computeSelectionBounds(newReferences, updatedDoc.elements)
        selection = CanvasSelection(
            references = newReferences,
            bounds = newBounds,
            rotationDegrees = selection.rotationDegrees
        )

        return Pair(updatedDoc, newBounds)
    }

    companion object {
        fun nextZIndexForLayer(document: CanvasDocument, layerId: String): Int {
            return (document.elements.filter { it.layerId == layerId }.maxOfOrNull { it.zIndex } ?: 0) + 1
        }
    }
}
