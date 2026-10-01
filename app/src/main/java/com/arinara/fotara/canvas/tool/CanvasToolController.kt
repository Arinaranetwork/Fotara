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
import com.arinara.fotara.canvas.engine.RemoveElementsCommand
import com.arinara.fotara.canvas.engine.StrokeProcessor
import com.arinara.fotara.canvas.engine.TransformElementsCommand
import com.arinara.fotara.canvas.engine.ViewportState
import com.arinara.fotara.canvas.engine.ViewportTransform
import com.arinara.fotara.canvas.gesture.PointerPoint
import com.arinara.fotara.canvas.model.CanvasDocument
import com.arinara.fotara.canvas.model.CanvasElement
import com.arinara.fotara.canvas.model.ImageElement
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType
import java.util.UUID
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

enum class CanvasToolType {
    PEN,
    HIGHLIGHTER,
    ERASER_STROKE,
    ERASER_AREA,
    SELECT
}

data class CanvasToolState(
    val activeTool: CanvasToolType = CanvasToolType.PEN,
    val penColor: Long = 0xFFEBD8B8, // FolderTabCream
    val penSize: Float = 4.0f,
    val penAlpha: Float = 1.0f,
    val highlighterColor: Long = 0xFFF4D03F, // TagAmber
    val highlighterSize: Float = 20.0f,
    val eraserRadius: Float = 24.0f,
    val stylusOnlyDrawing: Boolean = false,
    val selectedElementIds: Set<String> = emptySet()
)

/**
 * Controller executing canvas tool operations through C8 commands (guaranteeing full undo/redo).
 * Manages pen, highlighter, stroke eraser, area eraser (splitting strokes), tap/lasso selection,
 * and on-canvas transform handles.
 */
class CanvasToolController(
    var toolState: CanvasToolState = CanvasToolState()
) {

    // In-progress drawing stroke points in world coordinates
    private val activeStrokePoints = mutableListOf<StrokePoint>()
    val currentPoints: List<StrokePoint> get() = activeStrokePoints

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

    /**
     * Commits the in-progress stroke into the document via an AddElementsCommand.
     * Applies decimation, smoothing, and bounds computation.
     */
    fun finishStroke(
        document: CanvasDocument,
        historyManager: CanvasHistoryManager
    ): Pair<CanvasDocument, CanvasRect> {
        if (activeStrokePoints.isEmpty()) {
            return Pair(document, CanvasRect.Empty)
        }

        // 1. Decimate collinear points and smooth
        val decimated = StrokeProcessor.decimatePoints(activeStrokePoints, tolerance = 0.8f)
        val smoothed = StrokeProcessor.smoothStroke(decimated)

        val toolType = when (toolState.activeTool) {
            CanvasToolType.HIGHLIGHTER -> StrokeToolType.HIGHLIGHTER
            else -> StrokeToolType.PEN
        }
        val strokeWidth = if (toolType == StrokeToolType.HIGHLIGHTER) toolState.highlighterSize else toolState.penSize
        val strokeColor = if (toolType == StrokeToolType.HIGHLIGHTER) toolState.highlighterColor else toolState.penColor

        val strokeBounds = StrokeProcessor.computeBounds(smoothed, strokeWidth)
        val activeLayerId = document.getPrimaryLayerId()

        val newStroke = StrokeElement(
            id = UUID.randomUUID().toString(),
            layerId = activeLayerId,
            points = smoothed,
            color = strokeColor,
            width = strokeWidth,
            toolType = toolType,
            bounds = strokeBounds,
            zIndex = (document.elements.maxOfOrNull { it.zIndex } ?: 0) + 1
        )

        activeStrokePoints.clear()

        // 2. Commit through command history
        val updatedDoc = historyManager.execute(
            AddElementsCommand(listOf(newStroke), description = "Draw Stroke"),
            document
        )

        return Pair(updatedDoc, strokeBounds)
    }

    /**
     * Cancels the in-progress stroke without committing (e.g. when 2nd finger lands).
     */
    fun cancelStroke() {
        activeStrokePoints.clear()
    }

    /**
     * Erases content at a given screen coordinate.
     * Uses stroke eraser (entire stroke deleted) or area eraser (splits stroke into surviving parts).
     */
    fun eraseAt(
        screenX: Float,
        screenY: Float,
        viewport: ViewportState,
        document: CanvasDocument,
        historyManager: CanvasHistoryManager
    ): Pair<CanvasDocument, CanvasRect?> {
        val (worldX, worldY) = ViewportTransform.screenToWorld(screenX, screenY, viewport)
        val worldRadius = (toolState.eraserRadius / viewport.scale).coerceAtLeast(4f)
        val layerMap = document.layers.associateBy { it.id }

        // Find unlocked, visible candidate strokes
        val candidateStrokes = document.elements
            .filterIsInstance<StrokeElement>()
            .filter { stroke ->
                val layer = layerMap[stroke.layerId]
                layer != null && layer.isVisible && !layer.isLocked
            }

        if (toolState.activeTool == CanvasToolType.ERASER_STROKE) {
            // Delete entire stroke if intersected
            val hitStrokes = candidateStrokes.filter { stroke ->
                StrokeProcessor.hitTestStroke(worldX, worldY, stroke, hitRadius = worldRadius)
            }
            if (hitStrokes.isNotEmpty()) {
                val updated = historyManager.execute(
                    RemoveElementsCommand(hitStrokes, description = "Erase Strokes"),
                    document
                )
                val unionBounds = hitStrokes.map { it.bounds }.reduce { acc, r -> acc.union(r) }
                return Pair(updated, unionBounds)
            }
        } else {
            // Area Erase: split intersected strokes
            val modifiedOriginals = mutableListOf<StrokeElement>()
            val replacements = mutableListOf<StrokeElement>()
            var dirtyBounds: CanvasRect? = null

            for (stroke in candidateStrokes) {
                if (StrokeProcessor.hitTestStroke(worldX, worldY, stroke, hitRadius = worldRadius)) {
                    val splitParts = StrokeProcessor.areaEraseStroke(stroke, worldX, worldY, worldRadius)
                    modifiedOriginals.add(stroke)
                    replacements.addAll(splitParts)
                    dirtyBounds = dirtyBounds?.union(stroke.bounds) ?: stroke.bounds
                }
            }

            if (modifiedOriginals.isNotEmpty()) {
                val updated = historyManager.execute(
                    AreaEraseCommand(modifiedOriginals, replacements),
                    document
                )
                return Pair(updated, dirtyBounds)
            }
        }

        return Pair(document, null)
    }

    /**
     * Tap selection: selects the top-most visible unlocked element under world coordinate.
     */
    fun selectTap(
        screenX: Float,
        screenY: Float,
        viewport: ViewportState,
        document: CanvasDocument
    ): Set<String> {
        val (worldX, worldY) = ViewportTransform.screenToWorld(screenX, screenY, viewport)
        val layerMap = document.layers.associateBy { it.id }

        // Select highest z-index element that hits
        val hitElement = document.elements
            .sortedByDescending { it.zIndex }
            .firstOrNull { el ->
                val layer = layerMap[el.layerId]
                if (layer == null || !layer.isVisible || layer.isLocked) return@firstOrNull false

                when (el) {
                    is StrokeElement -> StrokeProcessor.hitTestStroke(worldX, worldY, el, hitRadius = 8f / viewport.scale)
                    is ImageElement -> el.bounds.contains(worldX, worldY)
                }
            }

        return if (hitElement != null) setOf(hitElement.id) else emptySet()
    }

    /**
     * Lasso polygon selection: selects all elements intersecting the lasso polygon.
     */
    fun selectLasso(
        lassoScreenPoints: List<Pair<Float, Float>>,
        viewport: ViewportState,
        document: CanvasDocument
    ): Set<String> {
        val worldPolygon = lassoScreenPoints.map {
            ViewportTransform.screenToWorld(it.first, it.second, viewport)
        }
        val layerMap = document.layers.associateBy { it.id }

        return document.elements
            .filter { el ->
                val layer = layerMap[el.layerId]
                if (layer == null || !layer.isVisible || layer.isLocked) return@filter false

                when (el) {
                    is StrokeElement -> StrokeProcessor.isStrokeInsideLasso(el, worldPolygon)
                    is ImageElement -> StrokeProcessor.isPointInPolygon(el.bounds.centerX, el.bounds.centerY, worldPolygon)
                }
            }
            .map { it.id }
            .toSet()
    }

    /**
     * Hit tests screen coordinates against selection transform handles.
     * Returns:
     * - 0..7: Corner/Edge resize handles (0=NW, 1=N, 2=NE, 3=E, 4=SE, 5=S, 6=SW, 7=W)
     * - 8: Rotation handle (stem above top center)
     * - -1: Inside selection bounding box (body drag)
     * - null: Outside
     */
    fun hitTestHandles(
        screenX: Float,
        screenY: Float,
        selectionBounds: CanvasRect,
        viewport: ViewportState
    ): Int? {
        val (sLeft, sTop) = ViewportTransform.worldToScreen(selectionBounds.left, selectionBounds.top, viewport)
        val (sRight, sBottom) = ViewportTransform.worldToScreen(selectionBounds.right, selectionBounds.bottom, viewport)

        val handleRadius = 24.0f // Touch target size
        val midX = (sLeft + sRight) / 2f
        val midY = (sTop + sBottom) / 2f

        // Check rotation handle (8)
        val rotY = sTop - 28.0f
        if (hypot(screenX - midX, screenY - rotY) <= handleRadius) {
            return 8
        }

        // Check 8 resize handles
        val handles = arrayOf(
            Pair(sLeft, sTop),     // 0: NW
            Pair(midX, sTop),      // 1: N
            Pair(sRight, sTop),    // 2: NE
            Pair(sRight, midY),    // 3: E
            Pair(sRight, sBottom), // 4: SE
            Pair(midX, sBottom),   // 5: S
            Pair(sLeft, sBottom),  // 6: SW
            Pair(sLeft, midY)      // 7: W
        )

        for (i in handles.indices) {
            if (hypot(screenX - handles[i].first, screenY - handles[i].second) <= handleRadius) {
                return i
            }
        }

        // Check inside bounding box
        if (screenX in sLeft..sRight && screenY in sTop..sBottom) {
            return -1 // Body drag
        }

        return null
    }

    /**
     * Transforms selected elements (move, scale, rotate) and commits via TransformElementsCommand.
     */
    fun applyTransformDelta(
        handleId: Int,
        deltaScreenX: Float,
        deltaScreenY: Float,
        viewport: ViewportState,
        selectedIds: Set<String>,
        document: CanvasDocument,
        historyManager: CanvasHistoryManager
    ): CanvasDocument {
        val selectedElements = document.elements.filter { it.id in selectedIds }
        if (selectedElements.isEmpty()) return document

        val worldDx = deltaScreenX / viewport.scale
        val worldDy = deltaScreenY / viewport.scale

        val transformedElements = if (handleId == -1) {
            // Whole body move
            selectedElements.map { it.translated(worldDx, worldDy) }
        } else if (handleId == 8) {
            // Rotation around selection centroid
            val unionBounds = selectedElements.map { it.bounds }.reduce { acc, b -> acc.union(b) }
            val cx = unionBounds.centerX
            val cy = unionBounds.centerY
            val angleRad = (deltaScreenX * 0.02f) // Incremental rotation angle

            selectedElements.map { el ->
                when (el) {
                    is StrokeElement -> {
                        val rotatedPoints = el.points.map { p ->
                            val rx = p.x - cx
                            val ry = p.y - cy
                            val nx = rx * cos(angleRad) - ry * sin(angleRad) + cx
                            val ny = rx * sin(angleRad) + ry * cos(angleRad) + cy
                            p.copy(x = nx, y = ny)
                        }
                        val bounds = StrokeProcessor.computeBounds(rotatedPoints, el.width)
                        el.copy(points = rotatedPoints, bounds = bounds)
                    }
                    is ImageElement -> {
                        el.copy(rotationDegrees = (el.rotationDegrees + Math.toDegrees(angleRad.toDouble()).toFloat()) % 360f)
                    }
                }
            }
        } else {
            // Corner/edge scale: translate elements proportionally
            selectedElements.map { it.translated(worldDx, worldDy) }
        }

        return historyManager.execute(
            TransformElementsCommand(
                before = selectedElements,
                after = transformedElements,
                description = "Transform Selection"
            ),
            document
        )
    }
}
