// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.feature.academic.dewarp

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import java.io.File
import java.io.FileOutputStream
import kotlin.math.hypot
import kotlin.math.max

/**
 * Android Bitmap bridging utilities for the mathematical projective dewarper.
 * Handles downsampled preview decoding, optimal rectangular dimension calculations,
 * and high-resolution disk file persistence.
 */
object BitmapDewarpUtils {

    /**
     * Converts an Android ARGB_8888 [Bitmap] into an integer [PixelBuffer].
     */
    fun bitmapToPixelBuffer(bitmap: Bitmap): PixelBuffer {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)
        return PixelBuffer(width, height, pixels)
    }

    /**
     * Converts a [PixelBuffer] back into an Android [Bitmap].
     */
    fun pixelBufferToBitmap(buffer: PixelBuffer): Bitmap {
        val bitmap = Bitmap.createBitmap(buffer.width, buffer.height, Bitmap.Config.ARGB_8888)
        bitmap.setPixels(buffer.pixels, 0, buffer.width, 0, 0, buffer.width, buffer.height)
        return bitmap
    }

    /**
     * Computes the rectified target width and height by averaging opposite edge lengths
     * to preserve the native aspect ratio of the captured document.
     */
    fun computeOptimalDimensions(srcCorners: List<QuadPoint>): Pair<Int, Int> {
        require(srcCorners.size == 4) { "Requires exactly 4 corner points (TL, TR, BR, BL)." }
        val tl = srcCorners[0]
        val tr = srcCorners[1]
        val br = srcCorners[2]
        val bl = srcCorners[3]

        val topDist = hypot((tr.x - tl.x).toDouble(), (tr.y - tl.y).toDouble())
        val botDist = hypot((br.x - bl.x).toDouble(), (br.y - bl.y).toDouble())
        val leftDist = hypot((bl.x - tl.x).toDouble(), (bl.y - tl.y).toDouble())
        val rightDist = hypot((br.x - tr.x).toDouble(), (br.y - tr.y).toDouble())

        val targetWidth = max(topDist, botDist).toInt().coerceIn(100, 8192)
        val targetHeight = max(leftDist, rightDist).toInt().coerceIn(100, 8192)
        return Pair(targetWidth, targetHeight)
    }

    /**
     * Executes projective rectification and applies the specified academic optical filter.
     */
    fun dewarpBitmap(
        source: Bitmap,
        srcCorners: List<QuadPoint>,
        filterMode: DewarpFilterMode = DewarpFilterMode.ORIGINAL
    ): Bitmap {
        val (targetW, targetH) = computeOptimalDimensions(srcCorners)
        val srcBuffer = bitmapToPixelBuffer(source)
        val warped = PerspectiveDewarpEngine.warpPixelBuffer(
            src = srcBuffer,
            srcCorners = srcCorners,
            targetWidth = targetW,
            targetHeight = targetH
        )
        val filtered = PerspectiveDewarpEngine.applyFilter(warped, filterMode)
        return pixelBufferToBitmap(filtered)
    }

    /**
     * Rectifies an image file on disk in place or to a designated destination file,
     * maintaining high JPEG quality (92%).
     */
    fun dewarpImageFile(
        sourceFile: File,
        destinationFile: File,
        normalizedCorners: List<QuadPoint>,
        filterMode: DewarpFilterMode = DewarpFilterMode.ORIGINAL
    ): Boolean {
        if (!sourceFile.exists()) return false

        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(sourceFile.absolutePath, boundsOptions)
        val origW = boundsOptions.outWidth
        val origH = boundsOptions.outHeight
        if (origW <= 0 || origH <= 0) return false

        var inSampleSize = 1
        var w = origW
        var h = origH
        val maxDim = 4096
        while (w > maxDim || h > maxDim) {
            inSampleSize *= 2
            w /= 2
            h /= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            this.inSampleSize = inSampleSize
        }
        val fullBmp = BitmapFactory.decodeFile(sourceFile.absolutePath, decodeOptions) ?: return false
        var rectifiedBmp: Bitmap? = null
        return try {
            val pixelCorners = normalizedCorners.map { norm ->
                QuadPoint(norm.x * fullBmp.width, norm.y * fullBmp.height)
            }
            rectifiedBmp = dewarpBitmap(fullBmp, pixelCorners, filterMode)
            FileOutputStream(destinationFile).use { out ->
                rectifiedBmp.compress(Bitmap.CompressFormat.JPEG, 92, out)
            }
            true
        } catch (_: Exception) {
            false
        } finally {
            if (rectifiedBmp != null && rectifiedBmp != fullBmp) {
                rectifiedBmp.recycle()
            }
            fullBmp.recycle()
        }
    }

    /**
     * Loads a bounded preview bitmap (max dimension [maxDimensionPx]) to prevent OOM
     * during interactive canvas editing.
     */
    fun loadPreviewBitmap(file: File, maxDimensionPx: Int = 1200): Bitmap? {
        if (!file.exists()) return null

        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        val origW = options.outWidth
        val origH = options.outHeight
        if (origW <= 0 || origH <= 0) return null

        var sampleSize = 1
        var w = origW
        var h = origH
        while (w > maxDimensionPx * 1.5 || h > maxDimensionPx * 1.5) {
            sampleSize *= 2
            w /= 2
            h /= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        return BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
    }
}
