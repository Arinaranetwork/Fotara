// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.render

import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import com.arinara.fotara.canvas.engine.StrokePathBuilder
import com.arinara.fotara.canvas.engine.StrokeProcessor
import com.arinara.fotara.canvas.engine.ViewportState
import com.arinara.fotara.canvas.engine.ViewportTransform
import com.arinara.fotara.canvas.model.CanvasBackgroundStyle
import com.arinara.fotara.canvas.model.CanvasDocument
import com.arinara.fotara.canvas.model.CanvasElement
import com.arinara.fotara.canvas.model.CanvasSelection
import com.arinara.fotara.canvas.model.ImageElement
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType
import com.arinara.fotara.canvas.model.TextBackgroundStyle
import com.arinara.fotara.canvas.model.TextLayerElement
import com.arinara.fotara.canvas.persistence.CanvasAssetManager
import kotlin.math.max

/**
 * Pure rendering pipeline engineered for zero allocations per frame in the draw path.
 * Renders background patterns, cached tiles, in-progress live strokes, and selection handles.
 */
class CanvasRenderer(
    private val tileCacheManager: TileCacheManager,
    private val assetManager: CanvasAssetManager? = null
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
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
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
    private val selectionBoxPath = Path()
    private val tileSrcRect = Rect()
    private val tileDestRect = Rect()
    private val transformMatrix = Matrix()
    private val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val imageSrcRect = Rect()
    private val imageDstRectF = RectF()
    private val imageBitmapCache = LinkedHashMap<String, Bitmap>(16, 0.75f, true)
    private val layerCompositePaint = Paint(Paint.ANTI_ALIAS_FLAG)

    companion object {
        const val HIGHLIGHTER_BASE_ALPHA = 90

        /**
         * Shared alpha computation ensuring identical opacity across live preview,
         * committed rendering, tile caching, and PNG exports.
         */
        fun computeStrokeAlpha(toolType: StrokeToolType, layerOpacity: Float): Int {
            val base = if (toolType == StrokeToolType.HIGHLIGHTER) HIGHLIGHTER_BASE_ALPHA else 255
            return (base * layerOpacity).toInt().coerceIn(0, 255)
        }

        /**
         * Applies the given StrokeBlendMode to the paint.
         * Uses android.graphics.BlendMode on API 29+ and falls back to PorterDuff on API 24-28.
         * On API 24-28, MULTIPLY falls back to DARKEN to prevent strokes from vanishing over transparent pixels.
         */
        fun applyBlendMode(paint: Paint, mode: com.arinara.fotara.canvas.model.StrokeBlendMode) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                paint.blendMode = when (mode) {
                    com.arinara.fotara.canvas.model.StrokeBlendMode.NORMAL -> android.graphics.BlendMode.SRC_OVER
                    com.arinara.fotara.canvas.model.StrokeBlendMode.MULTIPLY -> android.graphics.BlendMode.MULTIPLY
                    com.arinara.fotara.canvas.model.StrokeBlendMode.DARKEN -> android.graphics.BlendMode.DARKEN
                    com.arinara.fotara.canvas.model.StrokeBlendMode.SCREEN -> android.graphics.BlendMode.SCREEN
                }
            } else {
                paint.xfermode = when (mode) {
                    com.arinara.fotara.canvas.model.StrokeBlendMode.NORMAL -> null
                    com.arinara.fotara.canvas.model.StrokeBlendMode.MULTIPLY -> PorterDuffXfermode(PorterDuff.Mode.DARKEN)
                    com.arinara.fotara.canvas.model.StrokeBlendMode.DARKEN -> PorterDuffXfermode(PorterDuff.Mode.DARKEN)
                    com.arinara.fotara.canvas.model.StrokeBlendMode.SCREEN -> PorterDuffXfermode(PorterDuff.Mode.SCREEN)
                }
            }
        }

        /**
         * Predicate deciding whether a layer requires an isolated offscreen surface (saveLayer).
         * Returns true only when non-NORMAL blended strokes exist on that layer.
         */
        fun shouldIsolateLayer(
            elements: List<CanvasElement>,
            hasBlendedInProgress: Boolean = false
        ): Boolean {
            if (hasBlendedInProgress) return true
            return elements.any { it is StrokeElement && it.blendMode != com.arinara.fotara.canvas.model.StrokeBlendMode.NORMAL }
        }
    }

    /**
     * Main render function called per frame.
     * Guaranteed zero object allocations.
     */
    private var cachedVisibleElements: List<CanvasElement>? = null

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
        inProgressBlendMode: com.arinara.fotara.canvas.model.StrokeBlendMode = com.arinara.fotara.canvas.model.StrokeBlendMode.NORMAL,
        inProgressLayerId: String? = null,
        selection: CanvasSelection = CanvasSelection.Empty,
        previewElements: List<CanvasElement>? = null,
        activeLassoPolygon: List<Pair<Float, Float>>?,
        density: Float = 1.0f,
        isGestureActive: Boolean = false,
        onTileInvalidated: () -> Unit
    ) {
        if (screenWidth <= 0f || screenHeight <= 0f) return

        // 1. Draw Adaptive Background Pattern
        drawBackground(canvas, viewport, screenWidth, screenHeight, documentSnapshot.backgroundStyle)

        // 2. Draw Committed Elements & In-Progress Stroke with Layer Isolation
        drawCommittedElements(
            canvas = canvas,
            viewport = viewport,
            screenWidth = screenWidth,
            screenHeight = screenHeight,
            documentSnapshot = documentSnapshot,
            spatialIndex = spatialIndex,
            selectedElementIds = selection.elementIds,
            previewElements = previewElements,
            inProgressPoints = inProgressPoints,
            inProgressTool = inProgressTool,
            inProgressColor = inProgressColor,
            inProgressWidth = inProgressWidth,
            inProgressBlendMode = inProgressBlendMode,
            inProgressLayerId = inProgressLayerId ?: documentSnapshot.getPrimaryLayerId(),
            isGestureActive = isGestureActive
        )

        // 3. Draw Active Lasso Polygon if selecting
        if (activeLassoPolygon != null && activeLassoPolygon.size >= 2) {
            drawLasso(canvas, activeLassoPolygon)
        }

        // 4. Draw Selection Bounding Box & Transform Handles
        if (selection.isNotEmpty && !selection.bounds.isEmpty) {
            drawSelectionHandles(canvas, viewport, selection, density)
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

    private fun drawCommittedElements(
        canvas: Canvas,
        viewport: ViewportState,
        screenWidth: Float,
        screenHeight: Float,
        documentSnapshot: CanvasDocument,
        spatialIndex: QuadTreeSpatialIndex,
        selectedElementIds: Set<String> = emptySet(),
        previewElements: List<CanvasElement>? = null,
        inProgressPoints: List<StrokePoint>? = null,
        inProgressTool: StrokeToolType? = null,
        inProgressColor: Long = 0xFFEBD8B8,
        inProgressWidth: Float = 4f,
        inProgressBlendMode: com.arinara.fotara.canvas.model.StrokeBlendMode = com.arinara.fotara.canvas.model.StrokeBlendMode.NORMAL,
        inProgressLayerId: String? = null,
        isGestureActive: Boolean = false
    ) {
        val visibleElements = if (isGestureActive && cachedVisibleElements != null) {
            cachedVisibleElements!!
        } else {
            val (worldMinX, worldMinY) = ViewportTransform.screenToWorld(0f, 0f, viewport)
            val (worldMaxX, worldMaxY) = ViewportTransform.screenToWorld(screenWidth, screenHeight, viewport)
            val viewportWorldBounds = CanvasRect(
                left = minOf(worldMinX, worldMaxX),
                top = minOf(worldMinY, worldMaxY),
                right = maxOf(worldMinX, worldMaxX),
                bottom = maxOf(worldMinY, worldMaxY)
            )
            spatialIndex.query(viewportWorldBounds).also {
                cachedVisibleElements = it
            }
        }
        val hasPreview = previewElements != null && selectedElementIds.isNotEmpty()

        canvas.save()
        canvas.translate(viewport.translateX, viewport.translateY)
        canvas.scale(viewport.scale, viewport.scale)

        var liveStrokeDrawn = false
        val sortedLayers = documentSnapshot.layers.sortedBy { it.order }

        for (layer in sortedLayers) {
            if (!layer.isVisible) continue

            val layerElements = visibleElements.filter { it.layerId == layer.id }.sortedWith(compareBy<CanvasElement> { it.zIndex }.thenBy { it.id })
            val layerPreviews = previewElements?.filter { it.layerId == layer.id }?.sortedWith(compareBy<CanvasElement> { it.zIndex }.thenBy { it.id })

            val hasLiveOnThisLayer = inProgressPoints != null && inProgressPoints.isNotEmpty() &&
                    inProgressTool != null && inProgressLayerId == layer.id
            val hasBlendedLive = hasLiveOnThisLayer && inProgressBlendMode != com.arinara.fotara.canvas.model.StrokeBlendMode.NORMAL

            val isIsolated = shouldIsolateLayer(layerElements, hasBlendedLive) ||
                    (layerPreviews?.let { shouldIsolateLayer(it) } == true)

            if (isIsolated) {
                // Layer-isolated offscreen surface: strokes blend only with same layer content
                layerCompositePaint.alpha = (255 * layer.opacity).toInt().coerceIn(0, 255)
                canvas.saveLayer(null, layerCompositePaint)

                for (element in layerElements) {
                    if (hasPreview && element.id in selectedElementIds) continue
                    drawSingleElement(canvas, element, layerOpacity = 1.0f, isIsolated = true)
                }

                if (layerPreviews != null) {
                    for (pElem in layerPreviews) {
                        drawSingleElement(canvas, pElem, layerOpacity = 1.0f, isIsolated = true)
                    }
                }

                if (hasLiveOnThisLayer) {
                    drawInProgressStrokeDirect(
                        canvas = canvas,
                        points = inProgressPoints,
                        toolType = inProgressTool,
                        color = inProgressColor,
                        width = inProgressWidth,
                        blendMode = inProgressBlendMode,
                        layerOpacity = 1.0f
                    )
                    liveStrokeDrawn = true
                }

                canvas.restore()
            } else {
                // Direct canvas drawing fast path for all-NORMAL layers (zero extra offscreen cost)
                for (element in layerElements) {
                    if (hasPreview && element.id in selectedElementIds) continue
                    drawSingleElement(canvas, element, layerOpacity = layer.opacity, isIsolated = false)
                }

                if (layerPreviews != null) {
                    for (pElem in layerPreviews) {
                        drawSingleElement(canvas, pElem, layerOpacity = layer.opacity, isIsolated = false)
                    }
                }

                if (hasLiveOnThisLayer) {
                    drawInProgressStrokeDirect(
                        canvas = canvas,
                        points = inProgressPoints,
                        toolType = inProgressTool,
                        color = inProgressColor,
                        width = inProgressWidth,
                        blendMode = inProgressBlendMode,
                        layerOpacity = layer.opacity
                    )
                    liveStrokeDrawn = true
                }
            }
        }

        // If live stroke was not rendered inside a layer (e.g. active layer was hidden/missing), render on top
        if (!liveStrokeDrawn && inProgressPoints != null && inProgressPoints.isNotEmpty() && inProgressTool != null) {
            drawInProgressStrokeDirect(
                canvas = canvas,
                points = inProgressPoints,
                toolType = inProgressTool,
                color = inProgressColor,
                width = inProgressWidth,
                blendMode = inProgressBlendMode,
                layerOpacity = 1.0f
            )
        }

        canvas.restore()
    }

    private fun drawSingleElement(
        canvas: Canvas,
        element: CanvasElement,
        layerOpacity: Float,
        isIsolated: Boolean = false
    ) {
        when (element) {
            is StrokeElement -> {
                val paint = if (element.toolType == StrokeToolType.HIGHLIGHTER) highlighterPaint else strokePaint
                paint.color = element.color.toInt()
                paint.strokeWidth = element.width
                paint.alpha = computeStrokeAlpha(element.toolType, layerOpacity)
                if (isIsolated) {
                    applyBlendMode(paint, element.blendMode)
                } else {
                    applyBlendMode(paint, com.arinara.fotara.canvas.model.StrokeBlendMode.NORMAL)
                }

                val pts = element.points
                if (pts.size >= 2) {
                    paint.style = Paint.Style.STROKE
                    StrokePathBuilder.buildStrokePath(drawPath, pts)
                    canvas.drawPath(drawPath, paint)
                } else if (pts.size == 1) {
                    // Render single tap (dot) with solid fill and exact stroke width radius
                    paint.style = Paint.Style.FILL
                    canvas.drawCircle(pts[0].x, pts[0].y, element.width / 2f, paint)
                    paint.style = Paint.Style.STROKE
                }
            }
            is ImageElement -> {
                val file = assetManager?.getAssetFile(element.assetId)
                if (file != null && file.exists()) {
                    val bmp = imageBitmapCache[element.assetId] ?: run {
                        try {
                            val decoded = BitmapFactory.decodeFile(file.absolutePath)
                            if (decoded != null) imageBitmapCache[element.assetId] = decoded
                            decoded
                        } catch (_: Exception) {
                            null
                        }
                    }
                    if (bmp != null && !bmp.isRecycled) {
                        canvas.save()
                        canvas.translate(element.x, element.y)
                        if (element.rotationDegrees != 0f) {
                            canvas.rotate(element.rotationDegrees, element.width / 2f, element.height / 2f)
                        }
                        imageSrcRect.set(0, 0, bmp.width, bmp.height)
                        imageDstRectF.set(0f, 0f, element.width, element.height)
                        imagePaint.alpha = (255 * layerOpacity).toInt().coerceIn(0, 255)
                        canvas.drawBitmap(bmp, imageSrcRect, imageDstRectF, imagePaint)
                        canvas.restore()
                    }
                }
            }
            is TextLayerElement -> {
                drawTextLayerElement(canvas, element, layerOpacity)
            }
        }
    }

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    private fun drawTextLayerElement(canvas: Canvas, element: TextLayerElement, layerOpacity: Float) {
        canvas.save()
        canvas.translate(element.x, element.y)
        if (element.rotationDegrees != 0f) {
            canvas.rotate(element.rotationDegrees, element.width / 2f, element.height / 2f)
        }

        // 1. Draw background style
        when (element.backgroundStyle) {
            TextBackgroundStyle.TRANSPARENT -> {}
            TextBackgroundStyle.FROSTED_DARK -> {
                textBgPaint.style = Paint.Style.FILL
                textBgPaint.color = 0xD91E293B.toInt() // Frosted dark #1E293B (~85% alpha)
                val bgRect = RectF(0f, 0f, element.width, element.height)
                canvas.drawRoundRect(bgRect, 12f, 12f, textBgPaint)
            }
            TextBackgroundStyle.SOLID_LIGHT -> {
                textBgPaint.style = Paint.Style.FILL
                textBgPaint.color = 0xFFFFFFFF.toInt()
                val bgRect = RectF(0f, 0f, element.width, element.height)
                canvas.drawRoundRect(bgRect, 12f, 12f, textBgPaint)
            }
        }

        // 2. Draw text using typeface (ElmsSans / Sans-Serif)
        textPaint.color = element.color.toInt()
        textPaint.alpha = (255 * layerOpacity).toInt().coerceIn(0, 255)
        textPaint.textSize = element.fontSizeSp * 2.2f
        val typefaceStyle = when {
            element.fontWeight >= 700 -> android.graphics.Typeface.BOLD
            else -> android.graphics.Typeface.NORMAL
        }
        textPaint.typeface = android.graphics.Typeface.create("sans-serif", typefaceStyle)

        val paddingX = 16f
        val lines = element.text.split("\n")
        val fontMetrics = textPaint.fontMetrics
        val lineHeight = fontMetrics.descent - fontMetrics.ascent + 4f
        val totalTextHeight = lines.size * lineHeight
        val startY = ((element.height - totalTextHeight) / 2f - fontMetrics.ascent).coerceAtLeast(-fontMetrics.ascent)

        when (element.alignment) {
            com.arinara.fotara.canvas.model.TextLayerAlignment.LEFT -> {
                textPaint.textAlign = Paint.Align.LEFT
                lines.forEachIndexed { i, line ->
                    canvas.drawText(line, paddingX, startY + i * lineHeight, textPaint)
                }
            }
            com.arinara.fotara.canvas.model.TextLayerAlignment.CENTER -> {
                textPaint.textAlign = Paint.Align.CENTER
                lines.forEachIndexed { i, line ->
                    canvas.drawText(line, element.width / 2f, startY + i * lineHeight, textPaint)
                }
            }
            com.arinara.fotara.canvas.model.TextLayerAlignment.RIGHT -> {
                textPaint.textAlign = Paint.Align.RIGHT
                lines.forEachIndexed { i, line ->
                    canvas.drawText(line, element.width - paddingX, startY + i * lineHeight, textPaint)
                }
            }
        }

        canvas.restore()
    }

    private fun drawInProgressStrokeDirect(
        canvas: Canvas,
        points: List<StrokePoint>,
        toolType: StrokeToolType,
        color: Long,
        width: Float,
        blendMode: com.arinara.fotara.canvas.model.StrokeBlendMode,
        layerOpacity: Float
    ) {
        if (points.isEmpty()) return
        val paint = if (toolType == StrokeToolType.HIGHLIGHTER) highlighterPaint else inProgressPaint
        paint.color = color.toInt()
        paint.strokeWidth = width
        paint.alpha = computeStrokeAlpha(toolType, layerOpacity)
        applyBlendMode(paint, blendMode)

        if (points.size >= 2) {
            paint.style = Paint.Style.STROKE
            StrokePathBuilder.buildStrokePath(drawPath, points)
            canvas.drawPath(drawPath, paint)
        } else if (points.size == 1) {
            paint.style = Paint.Style.FILL
            canvas.drawCircle(points[0].x, points[0].y, width / 2f, paint)
            paint.style = Paint.Style.STROKE
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
        selection: CanvasSelection,
        density: Float
    ) {
        val bounds = selection.bounds
        if (bounds.isEmpty) return

        val (scx, scy) = ViewportTransform.worldToScreen(bounds.centerX, bounds.centerY, viewport)
        val sW = bounds.width * viewport.scale
        val sH = bounds.height * viewport.scale

        val halfW = sW / 2.0f
        val halfH = sH / 2.0f

        val rad = Math.toRadians(selection.rotationDegrees.toDouble()).toFloat()
        val cosR = kotlin.math.cos(rad)
        val sinR = kotlin.math.sin(rad)

        // 4 corners of oriented bounding box in screen coords
        val nwX = scx + (-halfW) * cosR - (-halfH) * sinR
        val nwY = scy + (-halfW) * sinR + (-halfH) * cosR

        val neX = scx + (halfW) * cosR - (-halfH) * sinR
        val neY = scy + (halfW) * sinR + (-halfH) * cosR

        val seX = scx + (halfW) * cosR - (halfH) * sinR
        val seY = scy + (halfW) * sinR + (halfH) * cosR

        val swX = scx + (-halfW) * cosR - (halfH) * sinR
        val swY = scy + (-halfW) * sinR + (halfH) * cosR

        // Draw dashed oriented selection rectangle
        selectionBoxPath.rewind()
        selectionBoxPath.moveTo(nwX, nwY)
        selectionBoxPath.lineTo(neX, neY)
        selectionBoxPath.lineTo(seX, seY)
        selectionBoxPath.lineTo(swX, swY)
        selectionBoxPath.close()
        canvas.drawPath(selectionBoxPath, selectionBoxPaint)

        val handleRadius = 5.0f * density
        val suppressSide = sW < (88.0f * density) || sH < (88.0f * density)

        // 0: NW, 1: N, 2: NE, 3: E, 4: SE, 5: S, 6: SW, 7: W
        val handleLocals = arrayOf(
            Pair(-halfW, -halfH), // 0: NW
            Pair(0.0f, -halfH),   // 1: N
            Pair(halfW, -halfH),  // 2: NE
            Pair(halfW, 0.0f),    // 3: E
            Pair(halfW, halfH),   // 4: SE
            Pair(0.0f, halfH),    // 5: S
            Pair(-halfW, halfH),  // 6: SW
            Pair(-halfW, 0.0f)    // 7: W
        )

        for (i in handleLocals.indices) {
            if (suppressSide && (i == 1 || i == 3 || i == 5 || i == 7)) {
                continue
            }
            val (lx, ly) = handleLocals[i]
            val hx = scx + lx * cosR - ly * sinR
            val hy = scy + lx * sinR + ly * cosR
            canvas.drawCircle(hx, hy, handleRadius, handleFillPaint)
            canvas.drawCircle(hx, hy, handleRadius, handleStrokePaint)
        }

        // Rotation stem and handle (8) above top center
        val stemOffset = 28.0f * density
        val topCenterScreenX = scx + 0.0f * cosR - (-halfH) * sinR
        val topCenterScreenY = scy + 0.0f * sinR + (-halfH) * cosR

        val rotLocalY = -halfH - stemOffset
        val rotScreenX = scx + 0.0f * cosR - rotLocalY * sinR
        val rotScreenY = scy + 0.0f * sinR + rotLocalY * cosR

        canvas.drawLine(topCenterScreenX, topCenterScreenY, rotScreenX, rotScreenY, selectionBoxPaint)
        canvas.drawCircle(rotScreenX, rotScreenY, handleRadius, handleFillPaint)
        canvas.drawCircle(rotScreenX, rotScreenY, handleRadius, handleStrokePaint)
    }

    /**
     * Releases cached image bitmaps when view is detached.
     */
    fun clearImageCache() {
        imageBitmapCache.values.forEach { if (!it.isRecycled) it.recycle() }
        imageBitmapCache.clear()
    }
}
