// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import com.arinara.fotara.canvas.render.PhotoDrawingRenderer
import com.arinara.fotara.data.model.PdfPageDrawing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.OutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

object PdfPageGalleryExporter {

    const val JPEG_QUALITY = 92
    const val MAX_LONG_SIDE_PX = 4096
    const val TARGET_DPI = 300
    const val PDF_POINTS_PER_INCH = 72f

    /**
     * Calculates render size at 300 dpi (page points * 300 / 72),
     * with the longer side capped at 4096 px while preserving aspect ratio.
     */
    fun computeRenderSize(
        pageWidthPoints: Float,
        pageHeightPoints: Float,
        dpi: Int = TARGET_DPI,
        maxLongSide: Int = MAX_LONG_SIDE_PX
    ): Pair<Int, Int> {
        val safeW = pageWidthPoints.coerceAtLeast(1f)
        val safeH = pageHeightPoints.coerceAtLeast(1f)
        val rawW = (safeW * dpi / PDF_POINTS_PER_INCH).roundToInt().coerceAtLeast(1)
        val rawH = (safeH * dpi / PDF_POINTS_PER_INCH).roundToInt().coerceAtLeast(1)
        val longSide = maxOf(rawW, rawH)

        return if (longSide > maxLongSide) {
            val scale = maxLongSide.toFloat() / longSide.toFloat()
            Pair(
                (rawW * scale).roundToInt().coerceAtLeast(1),
                (rawH * scale).roundToInt().coerceAtLeast(1)
            )
        } else {
            Pair(rawW, rawH)
        }
    }

    /**
     * Builds standard filename: <document name without extension, sanitized>_p<page number>_<yyyyMMdd_HHmmss>.jpg
     * Note: [pageNumber] is 1-indexed.
     */
    fun buildFileName(
        documentName: String,
        pageNumber: Int,
        timestamp: Long = System.currentTimeMillis()
    ): String {
        val nameWithoutExt = documentName.substringBeforeLast(".")
        val sanitized = nameWithoutExt.replace(Regex("[\\\\/:*?\"<>|\\s]+"), "_")
            .trim('_')
            .ifEmpty { "document" }
        val dateStr = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(timestamp))
        return "${sanitized}_p${pageNumber}_$dateStr.jpg"
    }

    /**
     * Renders the given PDF page to an off-screen bitmap at 300 dpi, flattened with any visible drawing strokes.
     */
    suspend fun renderPageBitmap(
        renderer: PdfPageRenderer,
        pageIndex: Int,
        drawing: PdfPageDrawing?
    ): Bitmap? = withContext(Dispatchers.IO) {
        val (pageW, pageH) = renderer.getPageSizePoints(pageIndex)
        val (targetW, targetH) = computeRenderSize(pageW, pageH)

        val baseBitmap = renderer.renderPageForExport(pageIndex, targetW, targetH) ?: return@withContext null

        if (drawing != null && drawing.isVisible && drawing.hasStrokes) {
            val canvas = Canvas(baseBitmap)
            val scale = if (drawing.pageWidth > 0f) {
                targetW.toFloat() / drawing.pageWidth
            } else {
                1f
            }
            PhotoDrawingRenderer.renderStrokes(canvas, drawing.strokes, scale = scale)
        }

        baseBitmap
    }

    /**
     * Writes [bitmap] to [outputStream] at 92 JPEG quality.
     */
    fun writeBitmapToStream(bitmap: Bitmap, outputStream: OutputStream) {
        val success = bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, outputStream)
        if (!success) {
            throw IllegalStateException("Failed to compress bitmap to JPEG")
        }
    }

    /**
     * Saves a rendered page to MediaStore on Android 10+ (API 29+).
     * Guarantees no partial file on failure.
     */
    suspend fun saveToMediaStore(
        context: Context,
        bitmap: Bitmap,
        fileName: String,
        relativePath: String
    ): Uri = withContext(Dispatchers.IO) {
        val resolver = context.contentResolver
        val contentValues = ContentValues().apply {
            put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
            put(MediaStore.Images.Media.RELATIVE_PATH, relativePath)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
        }

        val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            ?: throw IllegalStateException("Failed to create MediaStore entry")

        try {
            resolver.openOutputStream(uri)?.use { out ->
                writeBitmapToStream(bitmap, out)
            } ?: throw IllegalStateException("Failed to open output stream for MediaStore URI")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentValues.clear()
                contentValues.put(MediaStore.Images.Media.IS_PENDING, 0)
                resolver.update(uri, contentValues, null, null)
            }
            uri
        } catch (e: Exception) {
            try {
                resolver.delete(uri, null, null)
            } catch (_: Exception) {}
            throw e
        }
    }
}
