// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import androidx.compose.ui.geometry.Offset
import com.arinara.fotara.canvas.model.StrokeBlendMode
import com.arinara.fotara.canvas.model.StrokeElement

/**
 * Coordinate-space-agnostic drawing tools and constants shared across photo drawing
 * and PDF page drawing modes.
 */
enum class DrawingTool {
    PEN,
    HIGHLIGHTER,
    ERASER
}

object DrawingConstants {
    const val MAX_UNDO_STEPS = 100
    const val AUTOSAVE_DEBOUNCE_MS = 600L

    const val DEFAULT_PEN_SIZE_DP = 4f
    const val DEFAULT_HIGHLIGHTER_SIZE_DP = 20f
    const val DEFAULT_ERASER_SIZE_DP = 24f

    const val MIN_PEN_SIZE_DP = 1f
    const val MAX_PEN_SIZE_DP = 30f

    const val MIN_HIGHLIGHTER_SIZE_DP = 8f
    const val MAX_HIGHLIGHTER_SIZE_DP = 50f

    const val MIN_ERASER_SIZE_DP = 10f
    const val MAX_ERASER_SIZE_DP = 60f

    val CURATED_PALETTE = listOf(
        0xFFEBD8B8, // FolderTabCream
        0xFFF4D03F, // TagAmber
        0xFFE74C3C, // TagCrimson
        0xFF2ECC71, // SageGreen
        0xFF3498DB, // RoyalBlue
        0xFFFFFFFF, // PureWhite
        0xFF95A5A6, // SlateGray
        0xFF0D1B2A  // MidnightNavy
    )

    /**
     * Maps a slider DP thickness to stored point/pixel coordinates based on screen density
     * and displayed fit scale.
     * Stored width = (sliderDp * density) / fitScale.
     */
    fun computeStoredWidth(sliderDp: Float, density: Float, fitScale: Float): Float {
        val safeFit = if (fitScale > 0f) fitScale else 1f
        return (sliderDp * density) / safeFit
    }

    /**
     * Erases strokes intersecting the capsule between [prevPt] and [currentPt] with [eraserRadius].
     */
    fun eraseStrokes(
        strokes: List<StrokeElement>,
        prevPt: Offset,
        currentPt: Offset,
        eraserRadius: Float,
        minRemainder: Float = 3f
    ): List<StrokeElement> {
        val result = mutableListOf<StrokeElement>()
        for (stroke in strokes) {
            val parts = StrokeProcessor.eraseStrokeWithCapsule(
                stroke = stroke,
                ax = prevPt.x,
                ay = prevPt.y,
                bx = currentPt.x,
                by = currentPt.y,
                eraserRadius = eraserRadius,
                minRemainder = minRemainder
            )
            result.addAll(parts)
        }
        return result
    }
}
