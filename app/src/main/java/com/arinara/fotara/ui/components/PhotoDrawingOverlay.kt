// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import com.arinara.fotara.canvas.render.PhotoDrawingRenderer
import com.arinara.fotara.data.model.PhotoDrawing

/**
 * Renders vector drawing strokes aligned 1:1 over a photo scaled with ContentScale.Fit.
 */
@Composable
fun PhotoDrawingOverlay(
    drawing: PhotoDrawing?,
    modifier: Modifier = Modifier
) {
    if (drawing == null || !drawing.isVisible || !drawing.hasStrokes) {
        return
    }

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val containerWidth = constraints.maxWidth.toFloat()
        val containerHeight = constraints.maxHeight.toFloat()
        val photoWidth = drawing.widthPx.toFloat()
        val photoHeight = drawing.heightPx.toFloat()

        if (containerWidth > 0f && containerHeight > 0f && photoWidth > 0f && photoHeight > 0f) {
            val fitScale = minOf(containerWidth / photoWidth, containerHeight / photoHeight)
            val renderedWidth = photoWidth * fitScale
            val renderedHeight = photoHeight * fitScale
            val offsetX = (containerWidth - renderedWidth) / 2f
            val offsetY = (containerHeight - renderedHeight) / 2f

            Canvas(modifier = Modifier.fillMaxSize()) {
                drawIntoCanvas { canvas ->
                    PhotoDrawingRenderer.renderStrokes(
                        canvas = canvas.nativeCanvas,
                        strokes = drawing.strokes,
                        scale = fitScale,
                        offsetX = offsetX,
                        offsetY = offsetY
                    )
                }
            }
        }
    }
}
