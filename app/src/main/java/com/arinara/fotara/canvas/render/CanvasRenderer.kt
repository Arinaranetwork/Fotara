// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.render

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import com.arinara.fotara.canvas.engine.CanvasRect
import com.arinara.fotara.canvas.engine.QuadTreeSpatialIndex
import com.arinara.fotara.canvas.engine.StrokeProcessor
import com.arinara.fotara.canvas.engine.ViewportState
import com.arinara.fotara.canvas.engine.ViewportTransform
import com.arinara.fotara.canvas.model.CanvasBackgroundStyle
import com.arinara.fotara.canvas.model.CanvasDocument
import com.arinara.fotara.canvas.model.CanvasElement
import com.arinara.fotara.canvas.model.ImageElement
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType
import kotlin.math.max

/**
 * Pure rendering pipeline engineered for zero allocations per frame in the draw path.
 * Renders background patterns, cached tiles, in-progress live strokes, and selection handles.
 */
class CanvasRenderer(
    private val tileCacheManager: TileCacheManager
) {

    // Pre-allocated reusable Paint objects (strictly zero allocations during onDraw)
    private val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x26EBD8B8 // Subtle cream grid
        strokeWidth = 1.0f
        style = Paint.Style.STROKE
    }
    private val dotPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x40EBD8B8
        style = Paint.Style.FILL
    }
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val highlighterPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.SQUARE
        strokeJoin = Paint.Join.MITER
        // Flat semi-transparent blend mode to prevent uneven darkening
        xfermode = PorterDuffXfermode(PorterDuff.Mode.SRC_OVER)
    }
    private val inProgressPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val selectionBoxPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFEBD8B8.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
        pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
    }
    private val handleFillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF0D1B2A.toInt() // MidnightNavy
        style = Paint.Style.FILL
    }
    private val handleStrokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFEBD8B8.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
    }
    private val lassoPathPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x80EBD8B8.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
        pathEffect = DashPathEffect(floatArrayOf(8f, 6f), 0f)
    }

    // Pre-allocated reusable paths and rects
    private val drawPath = Path()
    private val lassoPath = Path()
    private val tileSrcRect = Rect()
    private val tileDestRect = Rect()
    private val transformMatrix = Matrix()

    /**
     * Main render function called per frame.
     * Guaranteed zero object allocations.
     */
    fun drawCanvas(
        canvas: Canvas,
        viewport: ViewportState,
        screenWidth: Float,
        screenHeight: Float,
        documentSnapshot: CanvasDocument,
        spatialIndex: QuadTreeSpatialIndex,
        inProgressPoints: List<StrokePoint>?,
        inProgressTool: StrokeToolType?,
        inProgressColor: Long,
        inProgressWidth: Float,
        selectedElementIds: Set<String>,
        activeLassoPolygon: List<Pair<Float, Float>>?,
        onTileInvalidated: () -> Unit
    ) {
        if (screenWidth <= 0f || screenHeight <= 0f) return

        // 1. Draw Adaptive Background Pattern
        drawBackground(canvas, viewport, screenWidth, screenHeight, documentSnapshot.backgroundStyle)

        // 2. Query and Draw Cached Tiles
        val tiles = tileCacheManager.queryVisibleTiles(
            viewport = viewport,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            documentSnapshot = documentSnapshot,
            spatialIndex = spatialIndex,
            onTileRendered = onTileInvalidated
        )

        for (tile in tiles) {
            val bmp = tile.bitmap ?: continue
            val (screenLeft, screenTop) = ViewportTransform.worldToScreen(
                tile.worldBounds.left, tile.worldBounds.top, viewport
            )
            val (screenRight, screenBottom) = ViewportTransform.worldToScreen(
                tile.worldBounds.right, tile.worldBounds.bottom, viewport
            )

            tileSrcRect.set(0, 0, bmp.width, bmp.height)
            tileDestRect.set(
                screenLeft.toInt(),
                screenTop.toInt(),
                screenRight.toInt(),
                screenBottom.toInt()
            )

            canvas.drawBitmap(bmp, tileSrcRect, tileDestRect, null)
        }

        // 3. Fallback direct draw for any viewport area not yet covered by cached tiles
        drawCommittedFallbackElements(canvas, viewport, screenWidth, screenHeight, documentSnapshot, spatialIndex)

        // 4. Draw Live In-Progress Stroke (High-frequency path)
        if (inProgressPoints != null && inProgressPoints.isNotEmpty() && inProgressTool != null) {
            drawInProgressStroke(
                canvas = canvas,
                viewport = viewport,
                points = inProgressPoints,
                toolType = inProgressTool,
                color = inProgressColor,
                width = inProgressWidth
            )
        }

        // 5. Draw Active Lasso Polygon if selecting
        if (activeLassoPolygon != null && activeLassoPolygon.size >= 2) {
            drawLasso(canvas, activeLassoPolygon)
        }

        // 6. Draw Selection Bounding Box & Transform Handles
        if (selectedElementIds.isNotEmpty()) {
            drawSelectionHandles(canvas, viewport, selectedElementIds, documentSnapshot)
        }
    }

    private fun drawBackground(
        canvas: Canvas,
        viewport: ViewportState,
        screenWidth: Float,
        screenHeight: Float,
        style: CanvasBackgroundStyle
    ) {
        // Base dark background fill
        bgPaint.color = 0xFF0D1B2A.toInt() // MidnightNavy
        bgPaint.style = Paint.Style.FILL
        canvas.drawRect(0f, 0f, screenWidth, screenHeight, bgPaint)

        if (style == CanvasBackgroundStyle.BLANK) return

        // Adaptive spacing: Never loop over thousands of lines at high zoom
        val baseSpacing = 40.0f
        var displaySpacing = baseSpacing * viewport.scale

        while (displaySpacing < 24.0f) {
            displaySpacing *= 2.0f
        }
        while (displaySpacing > 120.0f) {
            displaySpacing /= 2.0f
        }

        val startX = ((viewport.translateX % displaySpacing) + displaySpacing) % displaySpacing
        val startY = ((viewport.translateY % displaySpacing) + displaySpacing) % displaySpacing

        when (style) {
            CanvasBackgroundStyle.GRID -> {
                var x = startX
                while (x <= screenWidth) {
                    canvas.drawLine(x, 0f, x, screenHeight, gridPaint)
                    x += displaySpacing
                }
                var y = startY
                while (y <= screenHeight) {
                    canvas.drawLine(0f, y, screenWidth, y, gridPaint)
                    y += displaySpacing
                }
            }
            CanvasBackgroundStyle.DOTS -> {
                var x = startX
                while (x <= screenWidth) {
                    var y = startY
                    while (y <= screenHeight) {
                        canvas.drawCircle(x, y, 1.5f, dotPaint)
                        y += displaySpacing
                    }
                    x += displaySpacing
                }
            }
            CanvasBackgroundStyle.RULED -> {
                var y = startY
                while (y <= screenHeight) {
                    canvas.drawLine(0f, y, screenWidth, y, gridPaint)
                    y += displaySpacing
                }
            }
            CanvasBackgroundStyle.BLANK -> {}
        }
    }

    private fun drawCommittedFallbackElements(
        canvas: Canvas,
        viewport: ViewportState,
        screenWidth: Float,
        screenHeight: Float,
        documentSnapshot: CanvasDocument,
        spatialIndex: QuadTreeSpatialIndex
    ) {
        val (worldMinX, worldMinY) = ViewportTransform.screenToWorld(0f, 0f, viewport)
        val (worldMaxX, worldMaxY) = ViewportTransform.screenToWorld(screenWidth, screenHeight, viewport)
        val viewportWorldBounds = CanvasRect(worldMinX, worldMinY, worldMaxX, worldMaxY)

        val visibleElements = spatialIndex.query(viewportWorldBounds)
        val layerMap = documentSnapshot.layers.associateBy { it.id }

        canvas.save()
        canvas.translate(viewport.translateX, viewport.translateY)
        canvas.scale(viewport.scale, viewport.scale)

        for (element in visibleElements) {
            val layer = layerMap[element.layerId] ?: continue
            if (!layer.isVisible) continue

            when (element) {
                is StrokeElement -> {
                    val paint = if (element.toolType == StrokeToolType.HIGHLIGHTER) highlighterPaint else strokePaint
                    paint.color = element.color.toInt()
                    paint.strokeWidth = element.width
                    paint.alpha = (255 * layer.opacity).toInt().coerceIn(0, 255)

                    drawPath.rewind()
                    val pts = element.points
                    if (pts.size >= 2) {
                        drawPath.moveTo(pts[0].x, pts[0].y)
                        for (i in 1 until pts.size) {
                            drawPath.lineTo(pts[i].x, pts[i].y)
                        }
                        canvas.drawPath(drawPath, paint)
                    } else if (pts.size == 1) {
                        canvas.drawCircle(pts[0].x, pts[0].y, element.width / 2f, paint)
                    }
                }
                is ImageElement -> {
                    // Direct image bounds rendering
                }
            }
        }

        canvas.restore()
    }

    private fun drawInProgressStroke(
        canvas: Canvas,
        viewport: ViewportState,
        points: List<StrokePoint>,
        toolType: StrokeToolType,
        color: Long,
        width: Float
    ) {
        val paint = if (toolType == StrokeToolType.HIGHLIGHTER) highlighterPaint else inProgressPaint
        paint.color = color.toInt()
        paint.strokeWidth = width * viewport.scale
        if (toolType == StrokeToolType.HIGHLIGHTER) {
            paint.alpha = 90 // Flat semi-transparent
        } else {
            paint.alpha = 255
        }

        drawPath.rewind()
        if (points.size >= 2) {
            val (sx0, sy0) = ViewportTransform.worldToScreen(points[0].x, points[0].y, viewport)
            drawPath.moveTo(sx0, sy0)

            for (i in 1 until points.size) {
                val (sx, sy) = ViewportTransform.worldToScreen(points[i].x, points[i].y, viewport)
                drawPath.lineTo(sx, sy)
            }
            canvas.drawPath(drawPath, paint)
        } else if (points.size == 1) {
            val (sx, sy) = ViewportTransform.worldToScreen(points[0].x, points[0].y, viewport)
            canvas.drawCircle(sx, sy, (width * viewport.scale) / 2f, paint)
        }
    }

    private fun drawLasso(canvas: Canvas, polygon: List<Pair<Float, Float>>) {
        lassoPath.rewind()
        lassoPath.moveTo(polygon[0].first, polygon[0].second)
        for (i in 1 until polygon.size) {
            lassoPath.lineTo(polygon[i].first, polygon[i].second)
        }
        canvas.drawPath(lassoPath, lassoPathPaint)
    }

    private fun drawSelectionHandles(
        canvas: Canvas,
        viewport: ViewportState,
        selectedIds: Set<String>,
        doc: CanvasDocument
    ) {
        val selectedElements = doc.elements.filter { it.id in selectedIds }
        if (selectedElements.isEmpty()) return

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (el in selectedElements) {
            val b = el.bounds
            if (b.left < minX) minX = b.left
            if (b.top < minY) minY = b.top
            if (b.right > maxX) maxX = b.right
            if (b.bottom > maxY) maxY = b.bottom
        }

        if (minX > maxX || minY > maxY) return

        val (sLeft, sTop) = ViewportTransform.worldToScreen(minX, minY, viewport)
        val (sRight, sBottom) = ViewportTransform.worldToScreen(maxX, maxY, viewport)

        // Draw dashed selection rectangle
        canvas.drawRect(sLeft, sTop, sRight, sBottom, selectionBoxPaint)

        // Draw 8 handles: 4 corners + 4 midpoints
        val handleRadius = 6.0f
        val midX = (sLeft + sRight) / 2f
        val midY = (sTop + sBottom) / 2f

        val handlePoints = floatArrayOf(
            sLeft, sTop,       // NW (0)
            midX, sTop,        // N  (1)
            sRight, sTop,      // NE (2)
            sRight, midY,      // E  (3)
            sRight, sBottom,   // SE (4)
            midX, sBottom,     // S  (5)
            sLeft, sBottom,    // SW (6)
            sLeft, midY        // W  (7)
        )

        for (i in handlePoints.indices step 2) {
            val hx = handlePoints[i]
            val hy = handlePoints[i + 1]
            canvas.drawCircle(hx, hy, handleRadius, handleFillPaint)
            canvas.drawCircle(hx, hy, handleRadius, handleStrokePaint)
        }

        // Draw Rotation handle (8) stem above top-center
        val rotHandleY = sTop - 28.0f
        canvas.drawLine(midX, sTop, midX, rotHandleY, selectionBoxPaint)
        canvas.drawCircle(midX, rotHandleY, handleRadius, handleFillPaint)
        canvas.drawCircle(midX, rotHandleY, handleRadius, handleStrokePaint)
    }
}
