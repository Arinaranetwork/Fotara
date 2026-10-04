// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.model

import com.arinara.fotara.canvas.engine.CanvasRect

/**
 * A contiguous inside segment of a stroke resulting from lasso or partial selection.
 */
data class StrokeInsideSegment(
    val points: List<StrokePoint>
) {
    fun computeBounds(strokeWidth: Float): CanvasRect {
        if (points.isEmpty()) return CanvasRect.Empty
        val safeW = if (strokeWidth.isNaN() || strokeWidth <= 0f) 2.0f else strokeWidth
        val halfW = (safeW / 2.0f) + 1.0f

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (p in points) {
            if (p.x.isNaN() || p.y.isNaN()) continue
            if (p.x < minX) minX = p.x
            if (p.y < minY) minY = p.y
            if (p.x > maxX) maxX = p.x
            if (p.y > maxY) maxY = p.y
        }

        return if (minX > maxX || minY > maxY) {
            CanvasRect.Empty
        } else {
            CanvasRect(minX - halfW, minY - halfW, maxX + halfW, maxY + halfW)
        }
    }
}

/**
 * Non-destructive reference representing whether an element was selected whole or partially.
 */
sealed class SelectedElementReference {
    abstract val elementId: String

    data class Whole(
        override val elementId: String
    ) : SelectedElementReference()

    data class PartialStroke(
        override val elementId: String,
        val insideSegments: List<StrokeInsideSegment>
    ) : SelectedElementReference()
}

/**
 * Complete selection state containing references to selected elements/portions,
 * oriented bounds, and orientation angle in degrees.
 */
data class CanvasSelection(
    val references: Map<String, SelectedElementReference> = emptyMap(),
    val bounds: CanvasRect = CanvasRect.Empty,
    val rotationDegrees: Float = 0f
) {
    val elementIds: Set<String> get() = references.keys
    val isEmpty: Boolean get() = references.isEmpty()
    val isNotEmpty: Boolean get() = references.isNotEmpty()

    companion object {
        val Empty = CanvasSelection()
    }
}
