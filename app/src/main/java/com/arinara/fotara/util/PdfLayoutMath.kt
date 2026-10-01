// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import kotlin.math.roundToInt

/**
 * Encapsulates computed target pixel dimensions for PDF page rasterization.
 */
data class PageTargetSize(
    val widthPx: Int,
    val heightPx: Int
) {
    val aspectRatio: Float
        get() = if (heightPx > 0) widthPx.toFloat() / heightPx.toFloat() else 0.707f
}

/**
 * Pure Kotlin mathematical utilities for PDF page layout, aspect-ratio preservation,
 * and memory-safe density/zoom target bitmap size computation.
 */
object PdfLayoutMath {

    const val DEFAULT_MAX_DIMENSION_PX = 2560
    const val DEFAULT_MIN_DIMENSION_PX = 100
    const val FALLBACK_ASPECT_RATIO = 0.7071f // Standard ISO 216 / A4 ratio (1 / sqrt(2))

    /**
     * Sanitizes an aspect ratio, guarding against NaN, infinite, zero, or negative values.
     */
    fun sanitizeAspectRatio(ratio: Float): Float {
        return if (ratio.isNaN() || ratio.isInfinite() || ratio <= 0.001f) {
            FALLBACK_ASPECT_RATIO
        } else {
            ratio
        }
    }

    /**
     * Computes the display card height in DP given the allocated card width in DP and aspect ratio.
     * Guaranteed: cardHeightDp = cardWidthDp / aspectRatio.
     */
    fun computeCardHeightDp(cardWidthDp: Float, aspectRatio: Float): Float {
        val safeRatio = sanitizeAspectRatio(aspectRatio)
        val safeWidth = cardWidthDp.coerceAtLeast(10f)
        return safeWidth / safeRatio
    }

    /**
     * Single unit-safe function computing the target bitmap size in pixels for PDF rendering.
     * Takes card width in pixels (or DP * density), page aspect ratio, and zoom factor.
     *
     * Invariants:
     * 1. Aspect ratio of the returned dimensions matches the page aspect ratio.
     * 2. Dimensions are clamped to [minDimensionPx, maxDimensionPx] to prevent OutOfMemory.
     * 3. When maxDimensionPx is reached, width and height are scaled down proportionally.
     */
    fun computeTargetSize(
        cardWidthPx: Int,
        aspectRatio: Float,
        zoomFactor: Float = 1.0f,
        maxDimensionPx: Int = DEFAULT_MAX_DIMENSION_PX,
        minDimensionPx: Int = DEFAULT_MIN_DIMENSION_PX
    ): PageTargetSize {
        val safeRatio = sanitizeAspectRatio(aspectRatio)
        val safeZoom = zoomFactor.coerceIn(0.1f, 10.0f)
        val safeWidthPx = cardWidthPx.coerceAtLeast(minDimensionPx).toFloat()

        // Base unconstrained dimensions
        val rawWidth = safeWidthPx * safeZoom
        val rawHeight = rawWidth / safeRatio

        // Clamp to max dimension while strictly preserving aspect ratio
        val maxDim = maxOf(rawWidth, rawHeight)
        val scale = if (maxDim > maxDimensionPx) {
            maxDimensionPx.toFloat() / maxDim
        } else {
            1.0f
        }

        val finalWidth = (rawWidth * scale).roundToInt().coerceIn(minDimensionPx, maxDimensionPx)
        val finalHeight = (rawHeight * scale).roundToInt().coerceIn(minDimensionPx, maxDimensionPx)

        return PageTargetSize(widthPx = finalWidth, heightPx = finalHeight)
    }

    /**
     * Overload taking DP dimensions and screen display density factor.
     */
    fun computeTargetSizeFromDp(
        cardWidthDp: Float,
        aspectRatio: Float,
        density: Float,
        zoomFactor: Float = 1.0f,
        maxDimensionPx: Int = DEFAULT_MAX_DIMENSION_PX,
        minDimensionPx: Int = DEFAULT_MIN_DIMENSION_PX
    ): PageTargetSize {
        val safeDensity = density.coerceIn(0.5f, 6.0f)
        val safeWidthDp = cardWidthDp.coerceAtLeast(40f)
        val cardWidthPx = (safeWidthDp * safeDensity).roundToInt()
        return computeTargetSize(
            cardWidthPx = cardWidthPx,
            aspectRatio = aspectRatio,
            zoomFactor = zoomFactor,
            maxDimensionPx = maxDimensionPx,
            minDimensionPx = minDimensionPx
        )
    }
}
