// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.document

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset

/**
 * State holder managing per-page zoom, two-axis panning, and boundary clamping.
 * Decouples gesture math from Compose view rendering for deterministic unit testing.
 */
class PdfPageZoomState(
    val minScale: Float = 1.0f,
    val maxScale: Float = 4.0f,
    val doubleTapScale: Float = 2.5f
) {
    var scale by mutableFloatStateOf(minScale)
    var offsetX by mutableFloatStateOf(0.0f)
    var offsetY by mutableFloatStateOf(0.0f)

    val isZoomed: Boolean
        get() = scale > 1.02f

    fun onPinch(zoomChange: Float, panChange: Offset, viewWidth: Float, viewHeight: Float) {
        val newScale = (scale * zoomChange).coerceIn(minScale, maxScale)
        scale = newScale

        if (newScale <= 1.02f) {
            reset()
        } else {
            val maxPanX = calculateMaxPan(newScale, viewWidth)
            val maxPanY = calculateMaxPan(newScale, viewHeight)
            offsetX = (offsetX + panChange.x).coerceIn(-maxPanX, maxPanX)
            offsetY = (offsetY + panChange.y).coerceIn(-maxPanY, maxPanY)
        }
    }

    fun onPan(panChange: Offset, viewWidth: Float, viewHeight: Float) {
        if (!isZoomed) return
        val maxPanX = calculateMaxPan(scale, viewWidth)
        val maxPanY = calculateMaxPan(scale, viewHeight)
        offsetX = (offsetX + panChange.x).coerceIn(-maxPanX, maxPanX)
        offsetY = (offsetY + panChange.y).coerceIn(-maxPanY, maxPanY)
    }

    fun onDoubleTap(tapPosition: Offset, viewWidth: Float, viewHeight: Float) {
        if (isZoomed) {
            reset()
        } else {
            scale = doubleTapScale
            // Target pan centers zoom towards tap point
            val targetPanX = (viewWidth / 2f - tapPosition.x) * (doubleTapScale - 1f)
            val targetPanY = (viewHeight / 2f - tapPosition.y) * (doubleTapScale - 1f)
            val maxPanX = calculateMaxPan(doubleTapScale, viewWidth)
            val maxPanY = calculateMaxPan(doubleTapScale, viewHeight)
            offsetX = targetPanX.coerceIn(-maxPanX, maxPanX)
            offsetY = targetPanY.coerceIn(-maxPanY, maxPanY)
        }
    }

    fun reset() {
        scale = minScale
        offsetX = 0.0f
        offsetY = 0.0f
    }

    companion object {
        fun calculateMaxPan(scale: Float, dimension: Float): Float {
            if (scale <= 1.0f || dimension <= 0.0f) return 0.0f
            return (dimension * (scale - 1.0f)) / 2.0f
        }
    }
}
