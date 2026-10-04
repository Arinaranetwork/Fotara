// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import kotlin.math.max
import kotlin.math.min

import android.graphics.BitmapRegionDecoder
import com.arinara.fotara.ui.profile.SourceCropRect

object ProfileImageUtils {

    data class ImageInfo(
        val width: Int,
        val height: Int,
        val rotationDegrees: Int
    )

    /**
     * Safely opens an InputStream for either content:// or file:// URIs across all API levels.
     */
    fun openStream(context: Context, uri: Uri): InputStream? {
        return try {
            if (uri.scheme == "file" && uri.path != null) {
                java.io.FileInputStream(File(uri.path!!))
            } else {
                context.contentResolver.openInputStream(uri)
            }
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Reads image bounds and EXIF orientation without loading the full bitmap into memory.
     */
    fun getImageInfo(context: Context, uri: Uri): ImageInfo? {
        return try {
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStream(context, uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            } ?: return null
            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

            var rotationDegrees = 0
            try {
                openStream(context, uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    rotationDegrees = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }
            } catch (_: Exception) {}

            ImageInfo(boundsOpts.outWidth, boundsOpts.outHeight, rotationDegrees)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Copies a content URI stream into a temporary private cache file.
     * Guarantees seekable file descriptor access and immune to permission expiration.
     */
    fun stageUriToCache(context: Context, uri: Uri, prefix: String = "temp_crop"): File? {
        return try {
            val cacheDir = context.cacheDir
            val tempFile = File(cacheDir, "${prefix}_${System.currentTimeMillis()}.png")
            openStream(context, uri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                    output.flush()
                }
            } ?: return null
            if (tempFile.exists() && tempFile.length() > 0L) {
                tempFile
            } else {
                if (tempFile.exists()) tempFile.delete()
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Crops a sub-region directly from source with memory-bounded downsampling
     * and automatic EXIF orientation correction.
     */
    fun cropFromSourceUri(
        context: Context,
        uri: Uri,
        sourceRect: SourceCropRect,
        rotationDegrees: Int,
        targetWidth: Int? = null,
        targetHeight: Int? = null
    ): Bitmap? {
        return try {
            val fullBitmap = decodeSampledBitmap(context, uri, 4096) ?: return null
            val rawW = fullBitmap.width
            val rawH = fullBitmap.height
            val left = sourceRect.left.coerceIn(0, rawW - 1)
            val top = sourceRect.top.coerceIn(0, rawH - 1)
            val width = sourceRect.width.coerceIn(1, rawW - left)
            val height = sourceRect.height.coerceIn(1, rawH - top)
            val rawSubBitmap = Bitmap.createBitmap(fullBitmap, left, top, width, height)
            if (rawSubBitmap != fullBitmap && !fullBitmap.isRecycled) {
                fullBitmap.recycle()
            }

            val decoded = rawSubBitmap

            // Scale to target size if requested
            if (targetWidth != null && targetHeight != null) {
                val scaled = Bitmap.createScaledBitmap(decoded, targetWidth, targetHeight, true)
                if (scaled != decoded && !decoded.isRecycled) {
                    decoded.recycle()
                }
                scaled
            } else if (targetWidth != null && decoded.width > targetWidth) {
                val ratio = targetWidth.toFloat() / decoded.width.toFloat()
                val newHeight = (decoded.height * ratio).toInt().coerceAtLeast(1)
                val scaled = Bitmap.createScaledBitmap(decoded, targetWidth, newHeight, true)
                if (scaled != decoded && !decoded.isRecycled) {
                    decoded.recycle()
                }
                scaled
            } else {
                decoded
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Safely decodes a bitmap from a content or file URI with memory-bounded downsampling
     * and automatic EXIF orientation correction.
     */
    fun decodeSampledBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = 2048
    ): Bitmap? {
        return try {
            // 1. Decode bounds only
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            openStream(context, uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, boundsOpts)
            } ?: return null

            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) return null

            // 2. Read EXIF rotation
            var rotationDegrees = 0
            try {
                openStream(context, uri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_NORMAL
                    )
                    rotationDegrees = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> 90
                        ExifInterface.ORIENTATION_ROTATE_180 -> 180
                        ExifInterface.ORIENTATION_ROTATE_270 -> 270
                        else -> 0
                    }
                }
            } catch (_: Exception) {}

            // 3. Compute sample size
            var sampleSize = 1
            while ((boundsOpts.outWidth / sampleSize) > maxDimension ||
                (boundsOpts.outHeight / sampleSize) > maxDimension
            ) {
                sampleSize *= 2
            }

            // 4. Decode scaled bitmap
            val decodeOpts = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.ARGB_8888
            }
            val rawBitmap = openStream(context, uri)?.use { stream ->
                BitmapFactory.decodeStream(stream, null, decodeOpts)
            } ?: return null

            // 5. Apply EXIF rotation if needed
            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotated = Bitmap.createBitmap(
                    rawBitmap, 0, 0, rawBitmap.width, rawBitmap.height, matrix, true
                )
                if (rotated != rawBitmap) {
                    rawBitmap.recycle()
                }
                rotated
            } else {
                rawBitmap
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Crops a sub-region of a bitmap based on normalized (0f..1f) coordinates.
     */
    fun cropNormalized(
        source: Bitmap,
        normLeft: Float,
        normTop: Float,
        normRight: Float,
        normBottom: Float
    ): Bitmap? {
        return try {
            val clampedL = normLeft.coerceIn(0f, 1f)
            val clampedT = normTop.coerceIn(0f, 1f)
            val clampedR = normRight.coerceIn(clampedL + 0.01f, 1f)
            val clampedB = normBottom.coerceIn(clampedT + 0.01f, 1f)

            val x = (clampedL * source.width).toInt().coerceIn(0, source.width - 1)
            val y = (clampedT * source.height).toInt().coerceIn(0, source.height - 1)
            val w = ((clampedR - clampedL) * source.width).toInt().coerceIn(1, source.width - x)
            val h = ((clampedB - clampedT) * source.height).toInt().coerceIn(1, source.height - y)

            Bitmap.createBitmap(source, x, y, w, h)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Scales and compresses a bitmap into a PNG file atomically via a temporary file.
     * Preserves full 32-bit ARGB_8888 alpha transparency losslessly.
     */
    fun savePngAtomically(
        bitmap: Bitmap,
        targetFile: File,
        targetWidth: Int? = null,
        targetHeight: Int? = null
    ): Boolean {
        val parentDir = targetFile.parentFile ?: return false
        if (!parentDir.exists()) parentDir.mkdirs()

        // Scale if requested
        val processedBitmap = if (targetWidth != null && targetHeight != null) {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } else if (targetWidth != null && bitmap.width > targetWidth) {
            val ratio = targetWidth.toFloat() / bitmap.width.toFloat()
            val newHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, targetWidth, newHeight, true)
        } else {
            bitmap
        }

        val tempFile = File(parentDir, "${targetFile.name}.tmp_${System.currentTimeMillis()}")
        return try {
            val compressSuccess = FileOutputStream(tempFile).use { out ->
                val ok = processedBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                out.flush()
                ok
            }

            if (!compressSuccess || !tempFile.exists() || tempFile.length() == 0L) {
                if (tempFile.exists()) tempFile.delete()
                return false
            }

            // Atomic rename / replace
            val replaced = if (targetFile.exists()) {
                val backupFile = File(parentDir, "${targetFile.name}.bak")
                if (backupFile.exists()) backupFile.delete()
                targetFile.renameTo(backupFile)
                if (tempFile.renameTo(targetFile)) {
                    backupFile.delete()
                    true
                } else {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                    backupFile.delete()
                    true
                }
            } else {
                tempFile.renameTo(targetFile) || {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                    true
                }()
            }
            replaced && targetFile.exists() && targetFile.length() > 0L
        } catch (_: Exception) {
            if (tempFile.exists()) tempFile.delete()
            false
        } finally {
            if (processedBitmap != bitmap && !processedBitmap.isRecycled) {
                processedBitmap.recycle()
            }
        }
    }

    /**
     * Scales and compresses a bitmap into a WebP file atomically via a temporary file.
     * Prevents partial/corrupted files if interrupted.
     */
    fun saveWebpAtomically(
        bitmap: Bitmap,
        targetFile: File,
        targetWidth: Int? = null,
        targetHeight: Int? = null,
        quality: Int = 90
    ): Boolean {
        val parentDir = targetFile.parentFile ?: return false
        if (!parentDir.exists()) parentDir.mkdirs()

        // Scale if requested
        val processedBitmap = if (targetWidth != null && targetHeight != null) {
            Bitmap.createScaledBitmap(bitmap, targetWidth, targetHeight, true)
        } else if (targetWidth != null && bitmap.width > targetWidth) {
            val ratio = targetWidth.toFloat() / bitmap.width.toFloat()
            val newHeight = (bitmap.height * ratio).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(bitmap, targetWidth, newHeight, true)
        } else {
            bitmap
        }

        val tempFile = File(parentDir, "${targetFile.name}.tmp_${System.currentTimeMillis()}")
        return try {
            val compressSuccess = FileOutputStream(tempFile).use { out ->
                val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    if (processedBitmap.hasAlpha()) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        Bitmap.CompressFormat.WEBP_LOSSY
                    }
                } else {
                    @Suppress("DEPRECATION")
                    Bitmap.CompressFormat.WEBP
                }
                val ok = processedBitmap.compress(format, quality, out)
                out.flush()
                ok
            }

            if (!compressSuccess || !tempFile.exists() || tempFile.length() == 0L) {
                if (tempFile.exists()) tempFile.delete()
                return false
            }

            // Atomic rename / replace
            val replaced = if (targetFile.exists()) {
                val backupFile = File(parentDir, "${targetFile.name}.bak")
                if (backupFile.exists()) backupFile.delete()
                targetFile.renameTo(backupFile)
                if (tempFile.renameTo(targetFile)) {
                    backupFile.delete()
                    true
                } else {
                    // Fallback copy
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                    backupFile.delete()
                    true
                }
            } else {
                tempFile.renameTo(targetFile) || {
                    tempFile.copyTo(targetFile, overwrite = true)
                    tempFile.delete()
                    true
                }()
            }
            replaced && targetFile.exists() && targetFile.length() > 0L
        } catch (_: Exception) {
            if (tempFile.exists()) tempFile.delete()
            false
        } finally {
            if (processedBitmap != bitmap && !processedBitmap.isRecycled) {
                processedBitmap.recycle()
            }
        }
    }
}
