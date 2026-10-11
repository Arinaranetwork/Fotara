// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.InputStream
import kotlin.math.roundToInt

/**
 * Bitmap downsampling utility for Glance AppWidgets.
 *
 * Guarantees that bitmaps passed across Binder IPC into RemoteViews never
 * exceed [MAX_WIDGET_BITMAP_SIZE] (512px) in width or height, strictly preventing
 * Android's TransactionTooLargeException.
 */
object WidgetBitmapUtils {

    const val MAX_WIDGET_BITMAP_SIZE = 512

    /**
     * Calculates the optimal inSampleSize power-of-two factor so that both dimensions
     * are at or below [maxSize].
     */
    fun calculateInSampleSize(rawWidth: Int, rawHeight: Int, maxSize: Int = MAX_WIDGET_BITMAP_SIZE): Int {
        var inSampleSize = 1
        if (rawHeight > maxSize || rawWidth > maxSize) {
            while ((rawHeight / inSampleSize) > maxSize || (rawWidth / inSampleSize) > maxSize) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    /**
     * Scales an existing in-memory [Bitmap] so that neither width nor height exceeds [maxSize].
     */
    fun scaleBitmapWithinBounds(source: Bitmap, maxSize: Int = MAX_WIDGET_BITMAP_SIZE): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxSize && height <= maxSize) {
            return source
        }

        val ratio = width.toFloat() / height.toFloat()
        val destWidth: Int
        val destHeight: Int
        if (width > height) {
            destWidth = maxSize
            destHeight = (maxSize / ratio).roundToInt().coerceAtLeast(1)
        } else {
            destHeight = maxSize
            destWidth = (maxSize * ratio).roundToInt().coerceAtLeast(1)
        }

        return Bitmap.createScaledBitmap(source, destWidth, destHeight, true)
    }

    /**
     * Loads a file from disk or content URI and decodes it strictly downsampled to <= [maxSize].
     */
    fun loadDownsampledBitmap(
        context: Context,
        fileUriOrPath: String,
        maxSize: Int = MAX_WIDGET_BITMAP_SIZE
    ): Bitmap? {
        if (fileUriOrPath.isBlank()) return null

        try {
            // Check if it's a direct file path
            val directFile = File(fileUriOrPath)
            if (directFile.exists() && directFile.canRead()) {
                return decodeFromFile(directFile, maxSize)
            }

            // Parse as URI
            val uri = Uri.parse(fileUriOrPath)
            if (uri.scheme == "file") {
                val path = uri.path
                if (path != null) {
                    val f = File(path)
                    if (f.exists()) return decodeFromFile(f, maxSize)
                }
            }

            // Content URI fallback
            return decodeFromContentUri(context, uri, maxSize)
        } catch (e: Exception) {
            Log.w("WidgetBitmapUtils", "Failed to load downsampled bitmap from: $fileUriOrPath", e)
            return null
        }
    }

    private fun decodeFromFile(file: File, maxSize: Int): Bitmap? {
        val boundsOptions = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

        val rawWidth = boundsOptions.outWidth
        val rawHeight = boundsOptions.outHeight
        if (rawWidth <= 0 || rawHeight <= 0) return null

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(rawWidth, rawHeight, maxSize)
            inPreferredConfig = Bitmap.Config.RGB_565 // Saves 50% memory over ARGB_8888
        }

        val decoded = BitmapFactory.decodeFile(file.absolutePath, decodeOptions) ?: return null
        return scaleBitmapWithinBounds(decoded, maxSize)
    }

    private fun decodeFromContentUri(context: Context, uri: Uri, maxSize: Int): Bitmap? {
        var inputStream: InputStream? = null
        try {
            inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val boundsOptions = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeStream(inputStream, null, boundsOptions)
            inputStream.close()

            val rawWidth = boundsOptions.outWidth
            val rawHeight = boundsOptions.outHeight
            if (rawWidth <= 0 || rawHeight <= 0) return null

            inputStream = context.contentResolver.openInputStream(uri) ?: return null
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = calculateInSampleSize(rawWidth, rawHeight, maxSize)
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            val decoded = BitmapFactory.decodeStream(inputStream, null, decodeOptions) ?: return null
            return scaleBitmapWithinBounds(decoded, maxSize)
        } finally {
            try {
                inputStream?.close()
            } catch (e: Exception) {
                Log.w("WidgetBitmapUtils", "Failed to close input stream", e)
            }
        }
    }
}
