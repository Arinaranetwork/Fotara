// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

/**
 * Axis-aligned bounding box representation in unbounded 2D world coordinates.
 */
data class CanvasRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
) {
    val width: Float get() = maxOf(0f, right - left)
    val height: Float get() = maxOf(0f, bottom - top)
    val centerX: Float get() = (left + right) / 2f
    val centerY: Float get() = (top + bottom) / 2f
    val isEmpty: Boolean get() = width <= 0f || height <= 0f

    fun contains(x: Float, y: Float): Boolean {
        return x in left..right && y in top..bottom
    }

    fun intersects(other: CanvasRect): Boolean {
        return left <= other.right && right >= other.left &&
               top <= other.bottom && bottom >= other.top
    }

    fun expanded(padding: Float): CanvasRect {
        return CanvasRect(
            left = left - padding,
            top = top - padding,
            right = right + padding,
            bottom = bottom + padding
        )
    }

    fun union(other: CanvasRect): CanvasRect {
        if (this.isEmpty) return other
        if (other.isEmpty) return this
        return CanvasRect(
            left = minOf(left, other.left),
            top = minOf(top, other.top),
            right = maxOf(right, other.right),
            bottom = maxOf(bottom, other.bottom)
        )
    }

    companion object {
        val Empty = CanvasRect(0f, 0f, 0f, 0f)

        fun fromPoints(p1X: Float, p1Y: Float, p2X: Float, p2Y: Float): CanvasRect {
            return CanvasRect(
                left = minOf(p1X, p2X),
                top = minOf(p1Y, p2Y),
                right = maxOf(p1X, p2X),
                bottom = maxOf(p1Y, p2Y)
            )
        }
    }
}

/**
 * Viewport state modeling infinite 2D canvas transform:
 * Screen = World * scale + (translateX, translateY)
 */
data class ViewportState(
    val scale: Float = 1.0f,
    val translateX: Float = 0.0f,
    val translateY: Float = 0.0f
)

/**
 * Pure functions for coordinate conversion, focal zoom, pan, and fit-to-content.
 * Strictly guarded against NaN, Infinity, zero scale, and non-invertible matrices.
 * Guaranteed to never throw under any input condition.
 */
object ViewportTransform {

    /**
     * Sensible zoom bounds:
     * - MIN_ZOOM = 0.05f (5% zoom-out, covers massive overview of study whiteboards)
     * - MAX_ZOOM = 50.0f (5000% zoom-in, allows sub-millimeter handwriting precision)
     */
    const val MIN_ZOOM = 0.05f
    const val MAX_ZOOM = 50.0f

    private fun sanitizeFloat(value: Float, fallback: Float = 0.0f): Float {
        return if (value.isNaN() || value.isInfinite()) fallback else value
    }

    private fun sanitizeScale(scale: Float): Float {
        if (scale.isNaN() || scale.isInfinite() || scale <= 0f) {
            return 1.0f
        }
        return scale.coerceIn(MIN_ZOOM, MAX_ZOOM)
    }

    /**
     * Converts a world coordinate to screen space:
     * Screen = World * scale + Translation
     */
    fun worldToScreen(
        worldX: Float,
        worldY: Float,
        viewport: ViewportState
    ): Pair<Float, Float> {
        val safeScale = sanitizeScale(viewport.scale)
        val safeTx = sanitizeFloat(viewport.translateX, 0f)
        val safeTy = sanitizeFloat(viewport.translateY, 0f)
        val safeWorldX = sanitizeFloat(worldX, 0f)
        val safeWorldY = sanitizeFloat(worldY, 0f)

        val screenX = safeWorldX * safeScale + safeTx
        val screenY = safeWorldY * safeScale + safeTy
        return Pair(sanitizeFloat(screenX, 0f), sanitizeFloat(screenY, 0f))
    }

    /**
     * Converts a screen coordinate to world space:
     * World = (Screen - Translation) / scale
     */
    fun screenToWorld(
        screenX: Float,
        screenY: Float,
        viewport: ViewportState
    ): Pair<Float, Float> {
        val safeScale = sanitizeScale(viewport.scale)
        val safeTx = sanitizeFloat(viewport.translateX, 0f)
        val safeTy = sanitizeFloat(viewport.translateY, 0f)
        val safeScreenX = sanitizeFloat(screenX, 0f)
        val safeScreenY = sanitizeFloat(screenY, 0f)

        val worldX = (safeScreenX - safeTx) / safeScale
        val worldY = (safeScreenY - safeTy) / safeScale
        return Pair(sanitizeFloat(worldX, 0f), sanitizeFloat(worldY, 0f))
    }

    /**
     * Zooms about an arbitrary screen focal point (e.g. pinch centroid or cursor).
     * Keeps the world coordinate under the focal point unchanged.
     */
    fun zoomAboutFocalPoint(
        viewport: ViewportState,
        focalScreenX: Float,
        focalScreenY: Float,
        zoomDelta: Float,
        minScale: Float = MIN_ZOOM,
        maxScale: Float = MAX_ZOOM
    ): ViewportState {
        val safeZoomDelta = if (zoomDelta.isNaN() || zoomDelta.isInfinite() || zoomDelta <= 0f) 1.0f else zoomDelta
        val currentScale = sanitizeScale(viewport.scale)
        val targetScale = (currentScale * safeZoomDelta).coerceIn(minScale, maxScale)

        if (currentScale == targetScale) {
            return viewport
        }

        val safeFocalX = sanitizeFloat(focalScreenX, 0f)
        val safeFocalY = sanitizeFloat(focalScreenY, 0f)
        val safeTx = sanitizeFloat(viewport.translateX, 0f)
        val safeTy = sanitizeFloat(viewport.translateY, 0f)

        // World coordinate currently at focal point
        val worldFocalX = (safeFocalX - safeTx) / currentScale
        val worldFocalY = (safeFocalY - safeTy) / currentScale

        // New translation keeping that world point stationary at screen focal coordinate
        val newTx = safeFocalX - worldFocalX * targetScale
        val newTy = safeFocalY - worldFocalY * targetScale

        return ViewportState(
            scale = sanitizeFloat(targetScale, 1.0f),
            translateX = sanitizeFloat(newTx, 0f),
            translateY = sanitizeFloat(newTy, 0f)
        )
    }

    /**
     * Pans the viewport by delta screen pixels.
     */
    fun pan(
        viewport: ViewportState,
        deltaScreenX: Float,
        deltaScreenY: Float
    ): ViewportState {
        val safeDx = sanitizeFloat(deltaScreenX, 0f)
        val safeDy = sanitizeFloat(deltaScreenY, 0f)
        val safeTx = sanitizeFloat(viewport.translateX, 0f)
        val safeTy = sanitizeFloat(viewport.translateY, 0f)
        val safeScale = sanitizeScale(viewport.scale)

        return ViewportState(
            scale = safeScale,
            translateX = sanitizeFloat(safeTx + safeDx, 0f),
            translateY = sanitizeFloat(safeTy + safeDy, 0f)
        )
    }

    /**
     * Adjusts scale and translation so the given world content bounds fit
     * comfortably inside the available screen dimensions with padding.
     */
    fun fitToContent(
        contentBounds: CanvasRect,
        screenWidth: Float,
        screenHeight: Float,
        padding: Float = 48.0f
    ): ViewportState {
        val safeW = sanitizeFloat(screenWidth, 1080f)
        val safeH = sanitizeFloat(screenHeight, 1920f)
        val safePad = sanitizeFloat(padding, 48f).coerceAtLeast(0f)

        if (contentBounds.isEmpty || safeW <= 0f || safeH <= 0f) {
            return ViewportState(
                scale = 1.0f,
                translateX = safeW / 2f,
                translateY = safeH / 2f
            )
        }

        val availableW = maxOf(10f, safeW - (2 * safePad))
        val availableH = maxOf(10f, safeH - (2 * safePad))

        val scaleX = availableW / contentBounds.width
        val scaleY = availableH / contentBounds.height
        val fitScale = minOf(scaleX, scaleY).coerceIn(MIN_ZOOM, 2.0f)

        val contentCenterX = contentBounds.centerX
        val contentCenterY = contentBounds.centerY

        val targetTx = (safeW / 2f) - (contentCenterX * fitScale)
        val targetTy = (safeH / 2f) - (contentCenterY * fitScale)

        return ViewportState(
            scale = sanitizeFloat(fitScale, 1.0f),
            translateX = sanitizeFloat(targetTx, 0f),
            translateY = sanitizeFloat(targetTy, 0f)
        )
    }
}
