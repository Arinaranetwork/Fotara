// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import com.arinara.fotara.canvas.render.PhotoDrawingRenderer
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoDrawing
import com.arinara.fotara.data.repository.PhotoDrawingRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PhotoFlattener {

    const val MAX_EXPORT_DIMENSION = 4096
    const val JPEG_EXPORT_QUALITY = 92

    /**
     * Determines whether a photo drawing needs to be flattened into an image export.
     * Only true if the drawing exists, is set to visible, and contains at least one stroke.
     */
    fun shouldFlatten(drawing: PhotoDrawing?): Boolean {
        return drawing != null && drawing.isVisible && drawing.hasStrokes
    }

    /**
     * Computes downsampling power-of-two factor so neither dimension exceeds [maxDim].
     */
    fun calculateInSampleSize(width: Int, height: Int, maxDim: Int = MAX_EXPORT_DIMENSION): Int {
        var inSampleSize = 1
        var w = width
        var h = height
        while (w > maxDim || h > maxDim) {
            inSampleSize *= 2
            w /= 2
            h /= 2
        }
        return inSampleSize.coerceAtLeast(1)
    }

    /**
     * Flattens the photo with its vector drawing if visible and containing strokes.
     * Returns the flattened JPEG file in cache/exports, or the original file from [photo.fileUri].
     */
    suspend fun flattenIfNeeded(
        context: Context,
        photo: Photo,
        drawing: PhotoDrawing?
    ): File = withContext(Dispatchers.IO) {
        val originalFile = File(photo.fileUri)
        if (!shouldFlatten(drawing) || !originalFile.exists()) {
            return@withContext originalFile
        }

        try {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(originalFile.absolutePath, boundsOptions)

            if (boundsOptions.outWidth <= 0 || boundsOptions.outHeight <= 0) {
                return@withContext originalFile
            }

            val sampleSize = calculateInSampleSize(boundsOptions.outWidth, boundsOptions.outHeight)
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inMutable = true
            }

            val decodedBitmap = BitmapFactory.decodeFile(originalFile.absolutePath, decodeOptions)
                ?: return@withContext originalFile

            val workingBitmap = if (decodedBitmap.isMutable) {
                decodedBitmap
            } else {
                val copy = decodedBitmap.copy(Bitmap.Config.ARGB_8888, true)
                decodedBitmap.recycle()
                copy
            }

            val canvas = Canvas(workingBitmap)
            val baseWidth = if (drawing!!.widthPx > 0) drawing.widthPx.toFloat() else workingBitmap.width.toFloat()
            val renderScale = workingBitmap.width.toFloat() / baseWidth

            PhotoDrawingRenderer.renderStrokes(
                canvas = canvas,
                strokes = drawing.strokes,
                scale = renderScale
            )

            val exportsDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val flattenedFile = File(exportsDir, "export_${photo.id}_${System.currentTimeMillis()}.jpg")
            FileOutputStream(flattenedFile).use { out ->
                workingBitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_EXPORT_QUALITY, out)
            }
            workingBitmap.recycle()

            flattenedFile
        } catch (_: Exception) {
            originalFile
        }
    }

    /**
     * Convenience overload resolving the drawing from [photoDrawingRepository].
     */
    suspend fun flattenIfNeeded(
        context: Context,
        photo: Photo,
        photoDrawingRepository: PhotoDrawingRepository
    ): File = withContext(Dispatchers.IO) {
        val drawing = photoDrawingRepository.getDrawing(photo.id)
        flattenIfNeeded(context, photo, drawing)
    }
}
