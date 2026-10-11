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
import com.arinara.fotara.canvas.model.TextBackgroundStyle
import com.arinara.fotara.canvas.model.TextLayerElement

/**
 * Shared renderer for drawing strokes and vector text annotations onto a photo in read-only overlay,
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

    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textBgPaint = Paint(Paint.ANTI_ALIAS_FLAG)

    fun renderTextLayers(
        canvas: Canvas,
        textLayers: List<TextLayerElement>,
        scale: Float = 1f,
        offsetX: Float = 0f,
        offsetY: Float = 0f
    ) {
        if (textLayers.isEmpty()) return
        canvas.save()
        if (offsetX != 0f || offsetY != 0f) {
            canvas.translate(offsetX, offsetY)
        }
        if (scale != 1f) {
            canvas.scale(scale, scale)
        }

        for (tl in textLayers) {
            drawTextLayer(canvas, tl)
        }
        canvas.restore()
    }

    fun drawTextLayer(canvas: Canvas, element: TextLayerElement) {
        canvas.save()
        canvas.translate(element.x, element.y)
        if (element.rotationDegrees != 0f) {
            canvas.rotate(element.rotationDegrees, element.width / 2f, element.height / 2f)
        }

        when (element.backgroundStyle) {
            TextBackgroundStyle.TRANSPARENT -> {}
            TextBackgroundStyle.FROSTED_DARK -> {
                textBgPaint.style = Paint.Style.FILL
                textBgPaint.color = 0xD91E293B.toInt()
                val bgRect = android.graphics.RectF(0f, 0f, element.width, element.height)
                canvas.drawRoundRect(bgRect, 8f, 8f, textBgPaint)
            }
            TextBackgroundStyle.SOLID_LIGHT -> {
                textBgPaint.style = Paint.Style.FILL
                textBgPaint.color = 0xFFFFFFFF.toInt()
                val bgRect = android.graphics.RectF(0f, 0f, element.width, element.height)
                canvas.drawRoundRect(bgRect, 8f, 8f, textBgPaint)
            }
        }

        textPaint.color = element.color.toInt()
        textPaint.textSize = element.fontSizeSp * 1.5f
        val typefaceStyle = if (element.fontWeight >= 700) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
        textPaint.typeface = android.graphics.Typeface.create("sans-serif", typefaceStyle)

        val paddingX = 12f
        val lines = element.text.split("\n")
        val fontMetrics = textPaint.fontMetrics
        val lineHeight = fontMetrics.descent - fontMetrics.ascent + 4f
        val totalTextHeight = lines.size * lineHeight
        val startY = ((element.height - totalTextHeight) / 2f - fontMetrics.ascent).coerceAtLeast(-fontMetrics.ascent)

        when (element.alignment) {
            com.arinara.fotara.canvas.model.TextLayerAlignment.LEFT -> {
                textPaint.textAlign = android.graphics.Paint.Align.LEFT
                lines.forEachIndexed { i, line ->
                    canvas.drawText(line, paddingX, startY + i * lineHeight, textPaint)
                }
            }
            com.arinara.fotara.canvas.model.TextLayerAlignment.CENTER -> {
                textPaint.textAlign = android.graphics.Paint.Align.CENTER
                lines.forEachIndexed { i, line ->
                    canvas.drawText(line, element.width / 2f, startY + i * lineHeight, textPaint)
                }
            }
            com.arinara.fotara.canvas.model.TextLayerAlignment.RIGHT -> {
                textPaint.textAlign = android.graphics.Paint.Align.RIGHT
                lines.forEachIndexed { i, line ->
                    canvas.drawText(line, element.width - paddingX, startY + i * lineHeight, textPaint)
                }
            }
        }

        canvas.restore()
    }
}
