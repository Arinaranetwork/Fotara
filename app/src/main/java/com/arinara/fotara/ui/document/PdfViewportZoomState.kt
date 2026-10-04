// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.document

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue

/**
 * State holder managing viewport-level zoom, two-axis panning, focal point tracking,
 * and boundary clamping for the whole document viewer.
 *
 * Implements 1:1 instantaneous gesture tracking with fingers down, exact focal-point
 * anchoring, and smooth 250ms easing animations for double-tap zoom and reset.
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
        get() = scale > 1.01f

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
     * Handles continuous pinch gestures with exact focal point compensation.
     * Scale and pan follow the fingers 1:1 on every frame, anchored at the touch centroid.
     */
    fun onPinch(
        zoomChange: Float,
        panChangeX: Float = 0f,
        panChangeY: Float = 0f,
        centroidX: Float = viewportWidth / 2f,
        centroidY: Float = viewportHeight / 2f
    ) {
        val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)

        if (newScale <= 1.01f) {
            reset()
            return
        }

        val newMaxX = calculateMaxPan(newScale, viewportWidth)
        val newMaxY = calculateMaxPan(newScale, viewportHeight)

        // Exact focal-shift anchoring: keep content under centroid stationary
        val centerX = viewportWidth / 2f
        val centerY = viewportHeight / 2f
        val focalShiftX = if (viewportWidth > 0f) (1f - zoomChange) * (centroidX - centerX - panX) else 0f
        val focalShiftY = if (viewportHeight > 0f) (1f - zoomChange) * (centroidY - centerY - panY) else 0f

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
     * Smoothly animates zoom level between 1.0x and doubleTapScale (2.5x) centered around the tap point.
     */
    suspend fun animateDoubleTap(tapX: Float, tapY: Float) {
        val startScale = scale
        val startPanX = panX
        val startPanY = panY

        val targetScale: Float
        val targetPanX: Float
        val targetPanY: Float

        if (isZoomed) {
            targetScale = minScale
            targetPanX = 0f
            targetPanY = 0f
        } else {
            targetScale = doubleTapScale
            val newMaxX = calculateMaxPan(doubleTapScale, viewportWidth)
            val newMaxY = calculateMaxPan(doubleTapScale, viewportHeight)

            val centerX = viewportWidth / 2f
            val centerY = viewportHeight / 2f
            val tPanX = if (viewportWidth > 0f) (centerX - tapX) * (doubleTapScale - 1f) else 0f
            val tPanY = if (viewportHeight > 0f) (centerY - tapY) * (doubleTapScale - 1f) else 0f

            targetPanX = tPanX.coerceIn(-newMaxX, newMaxX)
            targetPanY = tPanY.coerceIn(-newMaxY, newMaxY)
        }

        val anim = Animatable(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 250, easing = FastOutSlowInEasing)
        ) {
            val fraction = this.value
            scale = startScale + (targetScale - startScale) * fraction
            panX = startPanX + (targetPanX - startPanX) * fraction
            panY = startPanY + (targetPanY - startPanY) * fraction
        }
        if (targetScale <= 1.01f) {
            reset()
        }
    }

    /**
     * Smoothly animates scale and pan back to 1.0x default fit.
     */
    suspend fun animateReset() {
        if (!isZoomed) {
            reset()
            return
        }
        val startScale = scale
        val startPanX = panX
        val startPanY = panY
        val anim = Animatable(0f)
        anim.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 200, easing = FastOutSlowInEasing)
        ) {
            val fraction = this.value
            scale = startScale + (minScale - startScale) * fraction
            panX = startPanX + (0f - startPanX) * fraction
            panY = startPanY + (0f - startPanY) * fraction
        }
        reset()
    }

    /**
     * Synchronous double-tap toggle (non-animated fallback).
     */
    fun onDoubleTap(tapX: Float, tapY: Float) {
        if (isZoomed) {
            reset()
        } else {
            scale = doubleTapScale
            val newMaxX = calculateMaxPan(doubleTapScale, viewportWidth)
            val newMaxY = calculateMaxPan(doubleTapScale, viewportHeight)

            val centerX = viewportWidth / 2f
            val centerY = viewportHeight / 2f
            val targetPanX = if (viewportWidth > 0f) (centerX - tapX) * (doubleTapScale - 1f) else 0f
            val targetPanY = if (viewportHeight > 0f) (centerY - tapY) * (doubleTapScale - 1f) else 0f

            panX = targetPanX.coerceIn(-newMaxX, newMaxX)
            panY = targetPanY.coerceIn(-newMaxY, newMaxY)
        }
    }

    /**
     * Resets scale and pan offsets to 1.0x immediately.
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
