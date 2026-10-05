// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.render

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import com.arinara.fotara.canvas.engine.StrokePathBuilder
import com.arinara.fotara.canvas.model.StrokeBlendMode
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokePoint
import com.arinara.fotara.canvas.model.StrokeToolType

/**
 * Shared renderer for drawing strokes onto a photo in read-only overlay,
 * interactive drawing canvas, and export flattener.
 */
object PhotoDrawingRenderer {

    const val HIGHLIGHTER_BASE_ALPHA = 90 // 0x5A flat opacity

    private val strokePath = Path()
    private val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }

    /**
     * Renders a list of [StrokeElement] onto [canvas] with optional transformation.
     */
    fun renderStrokes(
        canvas: Canvas,
        strokes: List<StrokeElement>,
        scale: Float = 1f,
        offsetX: Float = 0f,
        offsetY: Float = 0f
    ) {
        synchronized(this) {
            if (strokes.isEmpty()) return@synchronized

            canvas.save()
            if (offsetX != 0f || offsetY != 0f) {
                canvas.translate(offsetX, offsetY)
            }
            if (scale != 1f) {
                canvas.scale(scale, scale)
            }

            for (stroke in strokes) {
                drawStroke(canvas, stroke)
            }

            canvas.restore()
        }
    }

    /**
     * Draws an individual stroke element onto [canvas].
     */
    fun drawStroke(canvas: Canvas, stroke: StrokeElement) {
        val pts = stroke.points
        if (pts.isEmpty()) return

        strokePaint.color = stroke.color.toInt()
        strokePaint.strokeWidth = stroke.width

        val alpha = if (stroke.toolType == StrokeToolType.HIGHLIGHTER) {
            HIGHLIGHTER_BASE_ALPHA
        } else {
            255
        }
        strokePaint.alpha = alpha
        applyBlendMode(strokePaint, stroke.blendMode)

        if (pts.size >= 2) {
            strokePaint.style = Paint.Style.STROKE
            StrokePathBuilder.buildStrokePath(strokePath, pts)
            canvas.drawPath(strokePath, strokePaint)
        } else {
            strokePaint.style = Paint.Style.FILL
            val p0 = pts[0]
            canvas.drawCircle(p0.x, p0.y, stroke.width / 2f, strokePaint)
        }
    }

    /**
     * Draws an in-progress stroke currently being dragged by the user.
     */
    fun drawLiveStroke(
        canvas: Canvas,
        points: List<StrokePoint>,
        toolType: StrokeToolType,
        color: Long,
        width: Float,
        blendMode: StrokeBlendMode
    ) {
        if (points.isEmpty()) return

        strokePaint.color = color.toInt()
        strokePaint.strokeWidth = width

        val alpha = if (toolType == StrokeToolType.HIGHLIGHTER) {
            HIGHLIGHTER_BASE_ALPHA
        } else {
            255
        }
        strokePaint.alpha = alpha
        applyBlendMode(strokePaint, blendMode)

        if (points.size >= 2) {
            strokePaint.style = Paint.Style.STROKE
            StrokePathBuilder.buildStrokePath(strokePath, points)
            canvas.drawPath(strokePath, strokePaint)
        } else {
            strokePaint.style = Paint.Style.FILL
            val p0 = points[0]
            canvas.drawCircle(p0.x, p0.y, width / 2f, strokePaint)
        }
    }

    private fun applyBlendMode(paint: Paint, mode: StrokeBlendMode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            paint.blendMode = when (mode) {
                StrokeBlendMode.NORMAL -> android.graphics.BlendMode.SRC_OVER
                StrokeBlendMode.MULTIPLY -> android.graphics.BlendMode.MULTIPLY
                StrokeBlendMode.DARKEN -> android.graphics.BlendMode.DARKEN
                StrokeBlendMode.SCREEN -> android.graphics.BlendMode.SCREEN
            }
        } else {
            paint.xfermode = when (mode) {
                StrokeBlendMode.NORMAL -> null
                StrokeBlendMode.MULTIPLY -> PorterDuffXfermode(PorterDuff.Mode.DARKEN)
                StrokeBlendMode.DARKEN -> PorterDuffXfermode(PorterDuff.Mode.DARKEN)
                StrokeBlendMode.SCREEN -> PorterDuffXfermode(PorterDuff.Mode.SCREEN)
            }
        }
    }
}
