// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.model

import com.arinara.fotara.canvas.model.StrokeElement

/**
 * Vector drawing overlaid on a specific PDF page.
 * Coordinates and stroke widths are stored in PDF page points (72 dpi).
 */
data class PdfPageDrawing(
    val documentId: Long,
    val pageIndex: Int,
    val strokes: List<StrokeElement>,
    val pageWidth: Float,
    val pageHeight: Float,
    val isVisible: Boolean = true,
    val updatedAt: Long = System.currentTimeMillis()
) {
    val hasStrokes: Boolean
        get() = strokes.isNotEmpty()
}
