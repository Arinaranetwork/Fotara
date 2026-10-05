// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.engine

import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.data.model.PhotoDrawing

/**
 * Pure coordinate transforms for photo drawing strokes during photo edits (rotation and cropping).
 */
object PhotoDrawingTransform {

    /**
     * Rotates all stroke coordinates 90 degrees clockwise and swaps dimensions.
     * Transformation rule: (x, y) -> (oldH - y, x).
     * Applying this function 4 consecutive times returns all coordinates to their original values.
     */
    fun rotate90Clockwise(drawing: PhotoDrawing, oldW: Int, oldH: Int): PhotoDrawing {
        val hFloat = oldH.toFloat()
        val rotatedStrokes = drawing.strokes.map { stroke ->
            val rotatedPoints = stroke.points.map { p ->
                StrokePoint(
                    x = hFloat - p.y,
                    y = p.x,
                    pressure = p.pressure
                )
            }
            val newBounds = StrokeProcessor.computeBounds(rotatedPoints, stroke.width)
            stroke.copy(points = rotatedPoints, bounds = newBounds)
        }
        return drawing.copy(
            strokes = rotatedStrokes,
            widthPx = oldH,
            heightPx = oldW,
            updatedAt = System.currentTimeMillis()
        )
    }

    /**
     * Translates stroke coordinates relative to the crop window offset.
     * Transformation rule: (x, y) -> (x - cropLeft, y - cropTop).
     */
    fun crop(
        drawing: PhotoDrawing,
        cropLeft: Float,
        cropTop: Float,
        newW: Int,
        newH: Int
    ): PhotoDrawing {
        val croppedStrokes = drawing.strokes.map { stroke ->
            val shiftedPoints = stroke.points.map { p ->
                StrokePoint(
                    x = p.x - cropLeft,
                    y = p.y - cropTop,
                    pressure = p.pressure
                )
            }
            val newBounds = StrokeProcessor.computeBounds(shiftedPoints, stroke.width)
            stroke.copy(points = shiftedPoints, bounds = newBounds)
        }
        return drawing.copy(
            strokes = croppedStrokes,
            widthPx = newW,
            heightPx = newH,
            updatedAt = System.currentTimeMillis()
        )
    }
}
