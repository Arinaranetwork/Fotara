// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import kotlin.math.sqrt

/**
 * Pure functions for memory-safe PDF rendering scale and dimension calculations.
 */
object PdfRenderBudget {

    /** Maximum pixel budget per rendered bitmap (~16MB in ARGB_8888, preventing OOM). */
    const val MAX_PIXEL_BUDGET: Long = 4_000_000L

    /**
     * Calculates bounded width and height given destination card dimensions and requested render scale.
     * If the resulting pixel count exceeds [maxPixelBudget], scales down proportionally while
     * strictly preserving aspect ratio.
     */
    fun calculateBoundedDimensions(
        destWidth: Int,
        destHeight: Int,
        renderScale: Float,
        maxPixelBudget: Long = MAX_PIXEL_BUDGET
    ): Pair<Int, Int> {
        val safeScale = renderScale.coerceIn(0.5f, 4.0f)
        val rawWidth = (destWidth * safeScale).toInt().coerceAtLeast(100)
        val rawHeight = (destHeight * safeScale).toInt().coerceAtLeast(100)
        val rawPixels = rawWidth.toLong() * rawHeight.toLong()

        if (rawPixels <= maxPixelBudget) {
            return rawWidth to rawHeight
        }

        val scaleDown = sqrt(maxPixelBudget.toDouble() / rawPixels.toDouble())
        val boundedWidth = (rawWidth * scaleDown).toInt().coerceAtLeast(100)
        val boundedHeight = (rawHeight * scaleDown).toInt().coerceAtLeast(100)
        return boundedWidth to boundedHeight
    }
}
