// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.document

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

/**
 * State holder managing viewport-level zoom, two-axis panning, focal point tracking,
 * and boundary clamping for the whole document viewer.
 *
 * Designed to feel identical to the photo viewer: free two-axis pan, double-tap to zoom/reset,
 * natural pinch around changing touch centroids, and native vertical scroll pass-through at 1.0x.
 */
class PdfViewportZoomState(
    val minScale: Float = 1.0f,
    val maxScale: Float = 4.0f,
    val doubleTapScale: Float = 2.5f
) {
    var scale by mutableFloatStateOf(minScale)
    var panX by mutableFloatStateOf(0.0f)
    var panY by mutableFloatStateOf(0.0f)

    var viewportWidth by mutableFloatStateOf(0.0f)
    var viewportHeight by mutableFloatStateOf(0.0f)

    val isZoomed: Boolean
        get() = scale > 1.02f

    val maxPanX: Float
        get() = calculateMaxPan(scale, viewportWidth)

    val maxPanY: Float
        get() = calculateMaxPan(scale, viewportHeight)

    /**
     * Updates viewport dimensions on layout or configuration changes.
     */
    fun updateViewport(width: Float, height: Float) {
        if (width <= 0f || height <= 0f) return
        viewportWidth = width
        viewportHeight = height
        if (isZoomed) {
            panX = panX.coerceIn(-maxPanX, maxPanX)
            panY = panY.coerceIn(-maxPanY, maxPanY)
        }
    }

    /**
     * Handles continuous pinch gestures with focal point compensation.
     */
    fun onPinch(
        zoomChange: Float,
        panChangeX: Float = 0f,
        panChangeY: Float = 0f,
        centroidX: Float = viewportWidth / 2f,
        centroidY: Float = viewportHeight / 2f
    ) {
        val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)

        if (newScale <= 1.02f) {
            reset()
            return
        }

        val newMaxX = calculateMaxPan(newScale, viewportWidth)
        val newMaxY = calculateMaxPan(newScale, viewportHeight)

        // Adjust pan to keep content under centroid stationary during scale changes
        val focalShiftX = if (viewportWidth > 0f) (centroidX - viewportWidth / 2f) * (1f - zoomChange) else 0f
        val focalShiftY = if (viewportHeight > 0f) (centroidY - viewportHeight / 2f) * (1f - zoomChange) else 0f

        panX = (panX + panChangeX + focalShiftX).coerceIn(-newMaxX, newMaxX)
        panY = (panY + panChangeY + focalShiftY).coerceIn(-newMaxY, newMaxY)
        scale = newScale
    }

    /**
     * Handles single-finger panning when zoomed.
     */
    fun onPan(dx: Float, dy: Float) {
        if (!isZoomed) return
        val currentMaxX = maxPanX
        val currentMaxY = maxPanY
        panX = (panX + dx).coerceIn(-currentMaxX, currentMaxX)
        panY = (panY + dy).coerceIn(-currentMaxY, currentMaxY)
    }

    /**
     * Toggles between 1.0x and doubleTapScale (2.5x) centered around the tap point.
     */
    fun onDoubleTap(tapX: Float, tapY: Float) {
        if (isZoomed) {
            reset()
        } else {
            scale = doubleTapScale
            val newMaxX = calculateMaxPan(doubleTapScale, viewportWidth)
            val newMaxY = calculateMaxPan(doubleTapScale, viewportHeight)

            val targetPanX = if (viewportWidth > 0f) (viewportWidth / 2f - tapX) * (doubleTapScale - 1f) else 0f
            val targetPanY = if (viewportHeight > 0f) (viewportHeight / 2f - tapY) * (doubleTapScale - 1f) else 0f

            panX = targetPanX.coerceIn(-newMaxX, newMaxX)
            panY = targetPanY.coerceIn(-newMaxY, newMaxY)
        }
    }

    /**
     * Resets scale and pan offsets to 1.0x.
     */
    fun reset() {
        scale = minScale
        panX = 0.0f
        panY = 0.0f
    }

    companion object {
        fun calculateMaxPan(scale: Float, dimension: Float): Float {
            if (scale <= 1.0f || dimension <= 0.0f) return 0.0f
            return (dimension * (scale - 1.0f)) / 2.0f
        }
    }
}
