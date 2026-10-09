// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.export

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.arinara.fotara.canvas.engine.StrokePathBuilder
import com.arinara.fotara.canvas.model.CanvasDocument
import com.arinara.fotara.canvas.model.CanvasElement
import com.arinara.fotara.canvas.model.ImageElement
import com.arinara.fotara.canvas.model.StrokeBlendMode
import com.arinara.fotara.canvas.model.StrokeElement
import com.arinara.fotara.canvas.model.StrokeToolType
import com.arinara.fotara.canvas.model.TextBackgroundStyle
import com.arinara.fotara.canvas.model.TextLayerElement
import com.arinara.fotara.canvas.persistence.CanvasAssetManager
import com.arinara.fotara.canvas.render.CanvasRenderer
import java.io.File
import java.io.FileOutputStream

/**
 * Renders full canvas note content (or selected elements) at 1x scale into a Bitmap for document export / combine.
 * Returns null if the canvas or target selection is empty.
 */
object CanvasImageExporter {

    private const val PADDING = 32f
    private const val MAX_DIMENSION = 2400

    fun renderCanvasToBitmap(
        document: CanvasDocument,
        assetManager: CanvasAssetManager? = null,
        selectedOnlyIds: Set<String>? = null
    ): Bitmap? {
        val targetElements = if (selectedOnlyIds != null) {
            document.elements.filter { it.id in selectedOnlyIds }
        } else {
            document.elements
        }
        if (targetElements.isEmpty()) return null

        var minX = Float.MAX_VALUE
        var minY = Float.MAX_VALUE
        var maxX = -Float.MAX_VALUE
        var maxY = -Float.MAX_VALUE

        for (element in targetElements) {
            val b = element.bounds
            if (b.left < minX) minX = b.left
            if (b.top < minY) minY = b.top
            if (b.right > maxX) maxX = b.right
            if (b.bottom > maxY) maxY = b.bottom
        }

        if (minX > maxX || minY > maxY) return null

        val unscaledWidth = ((maxX - minX) + PADDING * 2f).coerceAtLeast(100f)
        val unscaledHeight = ((maxY - minY) + PADDING * 2f).coerceAtLeast(100f)

        val scale = minOf(1.0f, MAX_DIMENSION.toFloat() / maxOf(unscaledWidth, unscaledHeight))
        val targetWidth = (unscaledWidth * scale).toInt().coerceIn(100, MAX_DIMENSION)
        val targetHeight = (unscaledHeight * scale).toInt().coerceIn(100, MAX_DIMENSION)

        val bitmap = try {
            Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        } catch (_: OutOfMemoryError) {
            return null
        }

        val canvas = Canvas(bitmap)
        canvas.drawColor(Color.WHITE)

        canvas.save()
        canvas.scale(scale, scale)
        canvas.translate(-minX + PADDING, -minY + PADDING)

        val strokePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            strokeCap = Paint.Cap.ROUND
            strokeJoin = Paint.Join.ROUND
        }
        val drawPath = Path()
        val layerCompositePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val imagePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        val imageSrcRect = Rect()
        val imageDstRectF = RectF()

        val sortedLayers = document.layers.sortedBy { it.order }
        for (layer in sortedLayers) {
            if (!layer.isVisible) continue
            val layerElements = targetElements.filter { it.layerId == layer.id }.sortedWith(compareBy<CanvasElement> { it.zIndex }.thenBy { it.id })
            if (layerElements.isEmpty()) continue

            val isIsolated = CanvasRenderer.shouldIsolateLayer(layerElements)
            if (isIsolated) {
                layerCompositePaint.alpha = (255 * layer.opacity).toInt().coerceIn(0, 255)
                canvas.saveLayer(null, layerCompositePaint)
                for (element in layerElements) {
                    renderElement(canvas, element, 1.0f, true, assetManager, strokePaint, drawPath, imagePaint, imageSrcRect, imageDstRectF)
                }
                canvas.restore()
            } else {
                for (element in layerElements) {
                    renderElement(canvas, element, layer.opacity, false, assetManager, strokePaint, drawPath, imagePaint, imageSrcRect, imageDstRectF)
                }
            }
        }

        canvas.restore()
        return bitmap
    }

    private fun renderElement(
        canvas: Canvas,
        element: CanvasElement,
        layerOpacity: Float,
        isIsolated: Boolean,
        assetManager: CanvasAssetManager?,
        strokePaint: Paint,
        drawPath: Path,
        imagePaint: Paint,
        imageSrcRect: Rect,
        imageDstRectF: RectF
    ) {
        when (element) {
            is StrokeElement -> {
                strokePaint.color = element.color.toInt()
                strokePaint.strokeWidth = element.width
                strokePaint.alpha = CanvasRenderer.computeStrokeAlpha(element.toolType, layerOpacity)
                if (isIsolated) {
                    CanvasRenderer.applyBlendMode(strokePaint, element.blendMode)
                } else {
                    CanvasRenderer.applyBlendMode(strokePaint, StrokeBlendMode.NORMAL)
                }

                val pts = element.points
                if (pts.size >= 2) {
                    strokePaint.style = Paint.Style.STROKE
                    drawPath.reset()
                    StrokePathBuilder.buildStrokePath(drawPath, pts)
                    canvas.drawPath(drawPath, strokePaint)
                } else if (pts.size == 1) {
                    strokePaint.style = Paint.Style.FILL
                    canvas.drawCircle(pts[0].x, pts[0].y, element.width / 2f, strokePaint)
                    strokePaint.style = Paint.Style.STROKE
                }
            }
            is ImageElement -> {
                val file = assetManager?.getAssetFile(element.assetId)
                if (file != null && file.exists()) {
                    val bmp = try {
                        BitmapFactory.decodeFile(file.absolutePath)
                    } catch (_: Exception) {
                        null
                    }
                    if (bmp != null) {
                        canvas.save()
                        canvas.translate(element.x, element.y)
                        if (element.rotationDegrees != 0f) {
                            canvas.rotate(element.rotationDegrees, element.width / 2f, element.height / 2f)
                        }
                        imageSrcRect.set(0, 0, bmp.width, bmp.height)
                        imageDstRectF.set(0f, 0f, element.width, element.height)
                        imagePaint.alpha = (255 * layerOpacity).toInt().coerceIn(0, 255)
                        canvas.drawBitmap(bmp, imageSrcRect, imageDstRectF, imagePaint)
                        canvas.restore()
                        bmp.recycle()
                    }
                }
            }
            is TextLayerElement -> {
                canvas.save()
                canvas.translate(element.x, element.y)
                if (element.rotationDegrees != 0f) {
                    canvas.rotate(element.rotationDegrees, element.width / 2f, element.height / 2f)
                }

                when (element.backgroundStyle) {
                    TextBackgroundStyle.TRANSPARENT -> {}
                    TextBackgroundStyle.FROSTED_DARK -> {
                        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.FILL
                            color = 0xD91E293B.toInt()
                        }
                        canvas.drawRoundRect(RectF(0f, 0f, element.width, element.height), 12f, 12f, bgPaint)
                    }
                    TextBackgroundStyle.SOLID_LIGHT -> {
                        val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            style = Paint.Style.FILL
                            color = 0xFFFFFFFF.toInt()
                        }
                        canvas.drawRoundRect(RectF(0f, 0f, element.width, element.height), 12f, 12f, bgPaint)
                    }
                }

                val tPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = element.color.toInt()
                    alpha = (255 * layerOpacity).toInt().coerceIn(0, 255)
                    textSize = element.fontSizeSp * 2.2f
                    val tfStyle = if (element.fontWeight >= 700) android.graphics.Typeface.BOLD else android.graphics.Typeface.NORMAL
                    typeface = android.graphics.Typeface.create("sans-serif", tfStyle)
                }
                val fm = tPaint.fontMetrics
                val textY = ((element.height - fm.bottom - fm.top) / 2f).coerceAtLeast(fm.descent)
                canvas.drawText(element.text, 16f, textY, tPaint)
                canvas.restore()
            }
        }
    }

    /**
     * Exports all sheets in a multi-sheet canvas document to a combined multi-page PDF.
     */
    fun exportMultiSheetPdf(
        document: CanvasDocument,
        outputFile: File,
        assetManager: CanvasAssetManager? = null
    ): Boolean {
        val sheets = document.getResolvedSheets()
        if (sheets.isEmpty()) return false

        val pdfDoc = PdfDocument()
        return try {
            for ((idx, sheet) in sheets.withIndex()) {
                val sheetDoc = document.copy(elements = sheet.elements, backgroundStyle = sheet.backgroundStyle)
                val bitmap = renderCanvasToBitmap(sheetDoc, assetManager)
                val pageW = bitmap?.width ?: 1200
                val pageH = bitmap?.height ?: 1600
                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, idx + 1).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas = page.canvas

                if (bitmap != null) {
                    canvas.drawBitmap(bitmap, 0f, 0f, null)
                    bitmap.recycle()
                } else {
                    canvas.drawColor(Color.WHITE)
                }
                pdfDoc.finishPage(page)
            }
            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }
            true
        } catch (_: Exception) {
            false
        } finally {
            pdfDoc.close()
        }
    }
}
