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
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import com.arinara.fotara.canvas.engine.CanvasRect
import com.arinara.fotara.canvas.engine.QuadTreeSpatialIndex
import com.arinara.fotara.canvas.engine.StrokePathBuilder
import com.arinara.fotara.canvas.engine.ViewportState
import com.arinara.fotara.canvas.engine.ViewportTransform
import com.arinara.fotara.canvas.model.CanvasDocument
import com.arinara.fotara.canvas.model.CanvasElement
import com.arinara.fotara.canvas.model.CanvasLayer
import com.arinara.fotara.canvas.model.ImageElement
import com.arinara.fotara.canvas.model.StrokeBlendMode
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokeToolType
import com.arinara.fotara.canvas.model.TextBackgroundStyle
import com.arinara.fotara.canvas.model.TextLayerElement
import com.arinara.fotara.canvas.persistence.CanvasAssetManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.ceil
import kotlin.math.floor

data class TileKey(
    val col: Int,
    val row: Int,
    val zoomTier: Int // Discretized zoom tier for stable tile caching
)

data class CachedTile(
    val key: TileKey,
    val bitmap: Bitmap? = null,
    val worldBounds: CanvasRect,
    var isDirty: Boolean = false,
    var lastAccessTime: Long = System.currentTimeMillis()
)

object TileGridHelper {
    fun getZoomTier(scale: Float): Int {
        return when {
            scale < 0.25f -> 0
            scale < 0.75f -> 1
            scale < 1.5f -> 2
            scale < 3.0f -> 3
            else -> 4
        }
    }

    fun computeTileBounds(col: Int, row: Int, worldTileSize: Float): CanvasRect {
        return CanvasRect(
            left = col * worldTileSize,
            top = row * worldTileSize,
            right = (col + 1) * worldTileSize,
            bottom = (row + 1) * worldTileSize
        )
    }

    fun computeIntersectingTileKeys(
        dirtyWorldRect: CanvasRect,
        worldTileSize: Float,
        zoomTier: Int
    ): List<TileKey> {
        val startCol = floor(dirtyWorldRect.left / worldTileSize).toInt()
        val endCol = ceil(dirtyWorldRect.right / worldTileSize).toInt()
        val startRow = floor(dirtyWorldRect.top / worldTileSize).toInt()
        val endRow = ceil(dirtyWorldRect.bottom / worldTileSize).toInt()

        val keys = mutableListOf<TileKey>()
        for (col in startCol..endCol) {
            for (row in startRow..endRow) {
                keys.add(TileKey(col, row, zoomTier))
            }
        }
        return keys
    }
}

/**
 * High-performance LRU tile cache manager with fixed memory budget and OOM degradation.
 * Renders committed elements into offscreen tile bitmaps off the main thread.
 */
class TileCacheManager(
    private val maxMemoryBytes: Long = 32 * 1024 * 1024L, // 32MB fixed budget
    val tileSizePixels: Int = 512,
    private val assetManager: CanvasAssetManager? = null,
    private val cacheScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
) {

    private val tileMap = LinkedHashMap<TileKey, CachedTile>(16, 0.75f, true)
    private val reusableBitmaps = ArrayDeque<Bitmap>()
    private var currentMemoryUsage = 0L

    // Invalidation tracker
    private val renderingKeys = ConcurrentHashMap.newKeySet<TileKey>()

    @Synchronized
    fun getTile(key: TileKey): CachedTile? {
        val tile = tileMap[key]
        if (tile != null) {
            tile.lastAccessTime = System.currentTimeMillis()
        }
        return tile
    }

    /**
     * Determines which tiles cover the visible viewport and returns all available cached tiles.
     * Triggers asynchronous background rendering for any missing or dirty tiles.
     */
    fun queryVisibleTiles(
        viewport: ViewportState,
        screenWidth: Float,
        screenHeight: Float,
        documentSnapshot: CanvasDocument,
        spatialIndex: QuadTreeSpatialIndex,
        onTileRendered: () -> Unit
    ): List<CachedTile> {
        val (worldMinX, worldMinY) = ViewportTransform.screenToWorld(0f, 0f, viewport)
        val (worldMaxX, worldMaxY) = ViewportTransform.screenToWorld(screenWidth, screenHeight, viewport)

        val zoomTier = getZoomTier(viewport.scale)
        val worldTileSize = (tileSizePixels / viewport.scale).coerceAtLeast(10f)

        val startCol = floor(worldMinX / worldTileSize).toInt()
        val endCol = ceil(worldMaxX / worldTileSize).toInt()
        val startRow = floor(worldMinY / worldTileSize).toInt()
        val endRow = ceil(worldMaxY / worldTileSize).toInt()

        val visibleTiles = mutableListOf<CachedTile>()

        for (col in startCol..endCol) {
            for (row in startRow..endRow) {
                val key = TileKey(col, row, zoomTier)
                val cached = synchronized(this) { getTile(key) }

                val tileBounds = CanvasRect(
                    left = col * worldTileSize,
                    top = row * worldTileSize,
                    right = (col + 1) * worldTileSize,
                    bottom = (row + 1) * worldTileSize
                )

                if (cached != null && !cached.isDirty) {
                    visibleTiles.add(cached)
                } else if (!renderingKeys.contains(key)) {
                    // Queue asynchronous tile rendering
                    renderingKeys.add(key)
                    cacheScope.launch {
                        renderTileAsync(key, tileBounds, viewport.scale, documentSnapshot, spatialIndex)
                        renderingKeys.remove(key)
                        withContext(Dispatchers.Main) {
                            onTileRendered()
                        }
                    }
                }
            }
        }

        return visibleTiles
    }

    private suspend fun renderTileAsync(
        key: TileKey,
        worldBounds: CanvasRect,
        scale: Float,
        documentSnapshot: CanvasDocument,
        spatialIndex: QuadTreeSpatialIndex
    ) = withContext(Dispatchers.Default) {
        val bitmap = obtainBitmap() ?: return@withContext
        bitmap.eraseColor(Color.TRANSPARENT)

        val canvas = Canvas(bitmap)
        val tileWidth = bitmap.width.toFloat()
        val tileHeight = bitmap.height.toFloat()

        // Scale and translate canvas so worldBounds maps exactly into (0, 0, tileWidth, tileHeight)
        val scaleX = tileWidth / worldBounds.width
        val scaleY = tileHeight / worldBounds.height

        canvas.save()
        canvas.scale(scaleX, scaleY)
        canvas.translate(-worldBounds.left, -worldBounds.top)

        // Query spatial index for elements intersecting this tile
        val intersectingElements = spatialIndex.query(worldBounds)

        // Render layers in order respecting visibility, opacity, and blend modes
        val sortedLayers = documentSnapshot.layers.sortedBy { it.order }
        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val tilePath = Path()
        val layerCompositePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val imageSrcRect = Rect()
        val imageDstRectF = RectF()

        for (layer in sortedLayers) {
            if (!layer.isVisible) continue
            val layerElements = intersectingElements.filter { it.layerId == layer.id }.sortedWith(compareBy<CanvasElement> { it.zIndex }.thenBy { it.id })
            if (layerElements.isEmpty()) continue

            val isIsolated = CanvasRenderer.shouldIsolateLayer(layerElements)
            if (isIsolated) {
                layerCompositePaint.alpha = (255 * layer.opacity).toInt().coerceIn(0, 255)
                canvas.saveLayer(null, layerCompositePaint)
                for (element in layerElements) {
                    renderElementToTile(canvas, element, 1.0f, true, strokePaint, tilePath, imagePaint, imageSrcRect, imageDstRectF)
                }
                canvas.restore()
            } else {
                for (element in layerElements) {
                    renderElementToTile(canvas, element, layer.opacity, false, strokePaint, tilePath, imagePaint, imageSrcRect, imageDstRectF)
                }
            }
        }

        canvas.restore()

        val cachedTile = CachedTile(
            key = key,
            bitmap = bitmap,
            worldBounds = worldBounds,
            isDirty = false
        )

        synchronized(this@TileCacheManager) {
            putTile(key, cachedTile)
        }
    }

    private fun renderElementToTile(
        canvas: Canvas,
        element: CanvasElement,
        layerOpacity: Float,
        isIsolated: Boolean,
        strokePaint: Paint,
        tilePath: Path,
        imagePaint: Paint,
        imageSrcRect: Rect,
        imageDstRectF: RectF
    ) {
        when (element) {
            is StrokeElement -> {
                strokePaint.color = element.color.toInt()
                strokePaint.strokeWidth = element.width
                strokePaint.alpha = CanvasRenderer.computeStrokeAlpha(element.toolType, layerOpacity)
                if (isIsolated) {
                    CanvasRenderer.applyBlendMode(strokePaint, element.blendMode)
                } else {
                    CanvasRenderer.applyBlendMode(strokePaint, StrokeBlendMode.NORMAL)
                }

                val pts = element.points
                if (pts.size >= 2) {
                    strokePaint.style = Paint.Style.STROKE
                    tilePath.reset()
                    StrokePathBuilder.buildStrokePath(tilePath, pts)
                    canvas.drawPath(tilePath, strokePaint)
                } else if (pts.size == 1) {
                    strokePaint.style = Paint.Style.FILL
                    canvas.drawCircle(pts[0].x, pts[0].y, element.width / 2f, strokePaint)
                    strokePaint.style = Paint.Style.STROKE
                }
            }
            is ImageElement -> {
                val file = assetManager?.getAssetFile(element.assetId)
                if (file != null && file.exists()) {
                    try {
                        val imgBmp = BitmapFactory.decodeFile(file.absolutePath)
                        if (imgBmp != null) {
                            canvas.save()
                            canvas.translate(element.x, element.y)
                            if (element.rotationDegrees != 0f) {
                                canvas.rotate(element.rotationDegrees, element.width / 2f, element.height / 2f)
                            }
                            imageSrcRect.set(0, 0, imgBmp.width, imgBmp.height)
                            imageDstRectF.set(0f, 0f, element.width, element.height)
                            imagePaint.alpha = (255 * layerOpacity).toInt().coerceIn(0, 255)
                            canvas.drawBitmap(imgBmp, imageSrcRect, imageDstRectF, imagePaint)
                            canvas.restore()
                            imgBmp.recycle()
                        }
                    } catch (_: Exception) {}
                }
            }
            is TextLayerElement -> {
                canvas.save()
                canvas.translate(element.x, element.y)
                if (element.rotationDegrees != 0f) {
                    canvas.rotate(element.rotationDegrees, element.width / 2f, element.height / 2f)
                }
                when (element.backgroundStyle) {
                    TextBackgroundStyle.TRANSPARENT -> {}
                    TextBackgroundStyle.FROSTED_DARK -> {
                        imagePaint.style = Paint.Style.FILL
                        imagePaint.color = 0xD91E293B.toInt()
                        canvas.drawRoundRect(RectF(0f, 0f, element.width, element.height), 12f, 12f, imagePaint)
                    }
                    TextBackgroundStyle.SOLID_LIGHT -> {
                        imagePaint.style = Paint.Style.FILL
                        imagePaint.color = 0xFFFFFFFF.toInt()
                        canvas.drawRoundRect(RectF(0f, 0f, element.width, element.height), 12f, 12f, imagePaint)
                    }
                }
                strokePaint.style = Paint.Style.FILL
                strokePaint.color = element.color.toInt()
                strokePaint.alpha = (255 * layerOpacity).toInt().coerceIn(0, 255)
                strokePaint.textSize = element.fontSizeSp * 2.0f
                strokePaint.isFakeBoldText = element.fontWeight >= 700
                canvas.drawText(element.text, 16f, element.fontSizeSp * 2.0f + 16f, strokePaint)
                canvas.restore()
            }
        }
    }

    /**
     * Regional invalidation: Marks any tiles intersecting [dirtyWorldRect] as dirty.
     */
    @Synchronized
    fun invalidateRegion(dirtyWorldRect: CanvasRect) {
        for ((_, tile) in tileMap) {
            if (tile.worldBounds.intersects(dirtyWorldRect)) {
                tile.isDirty = true
            }
        }
    }

    /**
     * Invalidates all tiles in the cache.
     */
    @Synchronized
    fun invalidateAll() {
        for ((_, tile) in tileMap) {
            tile.isDirty = true
        }
    }

    @Synchronized
    private fun obtainBitmap(): Bitmap? {
        if (reusableBitmaps.isNotEmpty()) {
            return reusableBitmaps.removeLast()
        }

        // Check memory budget before allocating
        val bytesNeeded = tileSizePixels * tileSizePixels * 4L
        while (currentMemoryUsage + bytesNeeded > maxMemoryBytes && tileMap.isNotEmpty()) {
            evictOldestTile()
        }

        return try {
            val bmp = Bitmap.createBitmap(tileSizePixels, tileSizePixels, Bitmap.Config.ARGB_8888)
            currentMemoryUsage += bmp.allocationByteCount
            bmp
        } catch (_: OutOfMemoryError) {
            // OOM degradation fallback: clear all tiles and try lower-resolution ARGB_8888 (never RGB_565 which lacks alpha and causes black tiles)
            clearAllBitmaps()
            try {
                val halfSize = tileSizePixels / 2
                val degraded = Bitmap.createBitmap(halfSize, halfSize, Bitmap.Config.ARGB_8888)
                currentMemoryUsage += degraded.allocationByteCount
                degraded
            } catch (_: OutOfMemoryError) {
                null
            }
        }
    }

    @Synchronized
    private fun putTile(key: TileKey, tile: CachedTile) {
        val old = tileMap.put(key, tile)
        if (old != null && old.bitmap != null && old.bitmap != tile.bitmap) {
            reusableBitmaps.addLast(old.bitmap)
        }
    }

    @Synchronized
    private fun evictOldestTile() {
        val iterator = tileMap.iterator()
        if (iterator.hasNext()) {
            val entry = iterator.next()
            iterator.remove()
            entry.value.bitmap?.let { reusableBitmaps.addLast(it) }
        }
    }

    @Synchronized
    private fun clearAllBitmaps() {
        tileMap.clear()
        reusableBitmaps.clear()
        currentMemoryUsage = 0L
    }

    fun getZoomTier(scale: Float): Int = TileGridHelper.getZoomTier(scale)

    @Synchronized
    fun putTileForTesting(key: TileKey, tile: CachedTile) {
        tileMap[key] = tile
    }

    @Synchronized
    fun getAllTilesForTesting(): List<CachedTile> {
        return tileMap.values.toList()
    }

    @Synchronized
    fun release() {
        clearAllBitmaps()
    }
}
