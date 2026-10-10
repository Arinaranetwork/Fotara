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
    TEXT;

    companion object {
        @Deprecated("Consolidated into ERASER")
        val ERASER_STROKE = ERASER
        @Deprecated("Consolidated into ERASER")
        val ERASER_AREA = ERASER
    }
}

data class CanvasToolState(
    val activeTool: CanvasToolType = CanvasToolType.PEN,
    val penColor: Long = 0xFFEBD8B8, // FolderTabCream
    val penSize: Float = 4.0f,
    val penAlpha: Float = 1.0f,
    val highlighterColor: Long = 0xFFF4D03F, // TagAmber
    val highlighterSize: Float = 20.0f,
    val highlighterBlendMode: StrokeBlendMode = StrokeBlendMode.MULTIPLY,
    val eraserRadius: Float = 24.0f,
    val stylusOnlyDrawing: Boolean = false,
    val selection: CanvasSelection = CanvasSelection.Empty,
    val textFontSizeSp: Float = 18f,
    val textColor: Long = 0xFFF4F0E6,
    val textFontWeight: Int = 400,
    val textBackgroundStyle: com.arinara.fotara.canvas.model.TextBackgroundStyle = com.arinara.fotara.canvas.model.TextBackgroundStyle.TRANSPARENT
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
    private var lastEraserPoint: Pair<Float, Float>? = null

    fun isDrawing(): Boolean = activeStrokePoints.isNotEmpty()

    /**
     * Begins an in-progress stroke in world coordinates.
     */
    fun startStroke(worldX: Float, worldY: Float, rawPressure: Float) {
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
        if (toolState.activeTool != CanvasToolType.PEN || activeStrokePoints.size < 5) return false
        val result = com.arinara.fotara.canvas.engine.ShapeAutoCorrectEngine.recognizeAndSnap(activeStrokePoints)
        if (result.shape !is com.arinara.fotara.canvas.engine.RecognizedShape.None) {
            activeStrokePoints.clear()
            activeStrokePoints.addAll(result.snappedPoints)
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

        // Automatic drawing smoothing & shape recognition on finger lift for Pen strokes
        val finalPoints = if (toolType == StrokeToolType.PEN) {
            val snapResult = ShapeAutoCorrectEngine.recognizeAndSnap(decimated)
            if (snapResult.shape !is RecognizedShape.None) {
                snapResult.snappedPoints
            } else {
                ShapeAutoCorrectEngine.smoothPointsBezier(decimated, pressure = 1.0f, isClosed = false)
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

        val updatedDoc = historyManager.execute(
            AddElementsCommand(listOf(newStroke), description = "Draw Stroke"),
            document
        )

        return Pair(updatedDoc, strokeBounds)
    }

    fun cancelStroke() {
        activeStrokePoints.clear()
    }

    // ==========================================
    // Free Eraser (Capsule Sweep, 1 Undo Step per Drag)
    // ==========================================

    fun startEraser(screenX: Float, screenY: Float) {
        eraserTouchedOriginals.clear()
        eraserActiveSurvivingMap.clear()
        lastEraserPoint = Pair(screenX, screenY)
    }

    fun sweepEraser(
        points: List<PointerPoint>,
        viewport: ViewportState,
        document: CanvasDocument
    ): Pair<CanvasDocument, CanvasRect?> {
        if (points.isEmpty()) return Pair(document, null)

        val worldRadius = (toolState.eraserRadius / viewport.scale).coerceAtLeast(4f)
        val minRemainder = (4.0f * density) / viewport.scale.coerceAtLeast(0.01f)
        val layerMap = document.layers.associateBy { it.id }

        var dirtyBounds: CanvasRect? = null

        // Initialize active lookup on first move of drag
        if (eraserTouchedOriginals.isEmpty() && eraserActiveSurvivingMap.isEmpty()) {
            val eligibleStrokes = document.elements
                .filterIsInstance<StrokeElement>()
                .filter { stroke ->
                    val layer = layerMap[stroke.layerId]
                    layer != null && layer.isVisible && !layer.isLocked
                }
            for (s in eligibleStrokes) {
                eraserActiveSurvivingMap[s.id] = s
            }
        }

        var prev = lastEraserPoint ?: Pair(points.first().x, points.first().y)

        for (pt in points) {
            val (wx1, wy1) = ViewportTransform.screenToWorld(prev.first, prev.second, viewport)
            val (wx2, wy2) = ViewportTransform.screenToWorld(pt.x, pt.y, viewport)

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

        if (dirtyBounds != null) {
            // Live preview: untouched elements from document + currently surviving parts
            val untouched = document.elements.filter { it.id !in eraserTouchedOriginals.keys }
            val liveDoc = document.copy(elements = untouched + eraserActiveSurvivingMap.values)
            return Pair(liveDoc, dirtyBounds)
        }

        return Pair(document, null)
    }

    fun finishEraser(
        document: CanvasDocument,
        historyManager: CanvasHistoryManager
    ): Pair<CanvasDocument, CanvasRect?> {
        lastEraserPoint = null
        if (eraserTouchedOriginals.isEmpty()) {
            eraserActiveSurvivingMap.clear()
            return Pair(document, null)
        }

        val originals = eraserTouchedOriginals.values.toList()
        val originalIds = originals.map { it.id }.toSet()
        val replacements = eraserActiveSurvivingMap.values.filter { it.id !in originalIds }

        val unionBounds = originals.map { it.bounds }.fold(CanvasRect.Empty) { acc, cur -> acc.union(cur) }

        // Ensure we execute against the document containing original strokes
        val baseDoc = if (document.elements.any { it.id in originalIds }) {
            document
        } else {
            // Revert live preview strokes back to untouched + originals
            val survivingIds = replacements.map { it.id }.toSet()
            val untouched = document.elements.filter { it.id !in survivingIds }
            document.copy(elements = untouched + originals)
        }

        val committedDoc = historyManager.execute(
            AreaEraseCommand(originals, replacements, description = "Free Eraser"),
            baseDoc
        )

        eraserTouchedOriginals.clear()
        eraserActiveSurvivingMap.clear()

        return Pair(committedDoc, unionBounds)
    }

    fun cancelEraser(): CanvasDocument? {
        lastEraserPoint = null
        val hadTouches = eraserTouchedOriginals.isNotEmpty()
        eraserTouchedOriginals.clear()
        eraserActiveSurvivingMap.clear()
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
            CanvasSelection(
                references = mapOf(hitElement.id to ref),
                bounds = hitElement.bounds,
                rotationDegrees = rot
            )
        } else {
            CanvasSelection.Empty
        }

        selection = newSel
        return newSel
    }

    private fun hitTestImage(worldX: Float, worldY: Float, el: ImageElement): Boolean {
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
        if (el.rotationDegrees == 0f) {
            val minX = minOf(el.x, el.bounds.left)
            val maxX = maxOf(el.x + el.width, el.bounds.right)
            val minY = minOf(el.y, el.bounds.top)
            val maxY = maxOf(el.y + el.height, el.bounds.bottom)
            return worldX in minX..maxX && worldY in minY..maxY
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
                bounds = txt.bounds,
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
            selection = CanvasSelection(
                references = newReferences,
                bounds = txt.bounds,
                rotationDegrees = txt.rotationDegrees
            )
            txt.bounds
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

        val elMap = document.elements.associateBy { it.id }
        val beforeElements = mutableListOf<CanvasElement>()
        val afterElements = mutableListOf<CanvasElement>()
        val dirtyBounds = selection.bounds

        for ((id, ref) in selection.references) {
            val el = elMap[id] ?: continue
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

        val elMap = document.elements.associateBy { it.id }
        val offsetWorld = 20.0f / viewportScale.coerceAtLeast(0.05f)
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
            when (el) {
                is StrokeElement -> {
                    when (ref) {
                        is SelectedElementReference.Whole -> {
                            val dup = el.copy(
                                id = UUID.randomUUID().toString(),
                                points = el.points.map { it.copy(x = it.x + offsetWorld, y = it.y + offsetWorld) },
                                bounds = el.bounds.translated(offsetWorld, offsetWorld),
                                zIndex = nextZ(el.layerId)
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
                                    points = offsetPts,
                                    bounds = b,
                                    zIndex = nextZ(el.layerId)
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
                        bounds = el.bounds.translated(offsetWorld, offsetWorld),
                        zIndex = nextZ(el.layerId)
                    )
                    newElements.add(dup)
                    newReferences[dup.id] = SelectedElementReference.Whole(dup.id)
                }
                else -> {
                    val dup = el.translated(offsetWorld, offsetWorld).withZIndex(nextZ(el.layerId))
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
