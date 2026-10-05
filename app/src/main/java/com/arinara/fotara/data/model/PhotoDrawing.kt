// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

import com.arinara.fotara.canvas.model.StrokeElement

/**
 * Persisted vector drawing overlaid on a photo note.
 * Stroke coordinates are defined in photo pixel space [0..widthPx, 0..heightPx].
 */
data class PhotoDrawing(
    val photoId: Long,
    val strokes: List<StrokeElement>,
    val widthPx: Int,
    val heightPx: Int,
    val isVisible: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val hasStrokes: Boolean
        get() = strokes.isNotEmpty()
}
