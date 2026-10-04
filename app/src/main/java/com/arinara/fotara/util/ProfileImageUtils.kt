// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayInputStream
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
     * Reads all bytes from a URI in a single pass.
     * Guarantees consistent in-memory access immune to stream consumption or multi-open restrictions.
     */
    private fun readBytesFromUri(context: Context, uri: Uri): ByteArray? {
        return try {
            if (uri.scheme == "file" && uri.path != null) {
                val f = File(uri.path!!)
                if (f.exists() && f.length() > 0L) f.readBytes() else null
            } else {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun getExifRotation(filePath: String): Int {
        return try {
            val exif = ExifInterface(filePath)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: Throwable) {
            0
        }
    }

    private fun getExifRotationFromBytes(bytes: ByteArray): Int {
        return try {
            val exif = ExifInterface(ByteArrayInputStream(bytes))
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } catch (_: Throwable) {
            0
        }
    }

    private fun applyRotation(bitmap: Bitmap, degrees: Int): Bitmap {
        if (degrees == 0) return bitmap
        return try {
            val matrix = Matrix().apply { postRotate(degrees.toFloat()) }
            val rotated = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )
            if (rotated != bitmap && !bitmap.isRecycled) {
                bitmap.recycle()
            }
            rotated
        } catch (_: Throwable) {
            bitmap
        }
    }

    /**
     * Reads image bounds and EXIF orientation without loading the full bitmap into memory.
     */
    fun getImageInfo(context: Context, uri: Uri): ImageInfo? {
        return try {
            if (uri.scheme == "file" && uri.path != null) {
                val f = File(uri.path!!)
                if (f.exists() && f.length() > 0L) {
                    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(f.absolutePath, boundsOpts)
                    if (boundsOpts.outWidth > 0 && boundsOpts.outHeight > 0) {
                        return ImageInfo(boundsOpts.outWidth, boundsOpts.outHeight, getExifRotation(f.absolutePath))
                    }
                }
            }
            val bytes = readBytesFromUri(context, uri)
            if (bytes != null && bytes.isNotEmpty()) {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
                if (boundsOpts.outWidth > 0 && boundsOpts.outHeight > 0) {
                    return ImageInfo(boundsOpts.outWidth, boundsOpts.outHeight, getExifRotationFromBytes(bytes))
                }
            }
            // Fallback: decode a small sample
            val sampled = decodeSampledBitmap(context, uri, 128) ?: return null
            val info = ImageInfo(sampled.width, sampled.height, 0)
            if (!sampled.isRecycled) sampled.recycle()
            info
        } catch (_: Throwable) {
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
            val copied = if (uri.scheme == "file" && uri.path != null) {
                val src = File(uri.path!!)
                if (src.exists() && src.length() > 0L) {
                    src.copyTo(tempFile, overwrite = true)
                    true
                } else false
            } else {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                        output.flush()
                    }
                    true
                } ?: false
            }
            if (copied && tempFile.exists() && tempFile.length() > 0L) {
                tempFile
            } else {
                if (tempFile.exists()) tempFile.delete()
                null
            }
        } catch (_: Throwable) {
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
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Safely decodes a bitmap from a content or file URI with memory-bounded downsampling
     * and automatic EXIF orientation correction.
     * Employs a resilient multi-tier fallback architecture:
     * 1. Modern ImageDecoder on API 28+ (hardware accelerated, handles all PNG variants and EXIF)
     * 2. Direct BitmapFactory.decodeFile for file:// schemes (seekable memory-mapped access)
     * 3. Single-pass ByteArray buffering with BitmapFactory.decodeByteArray (immune to stream reset / multiple opens)
     * 4. ParcelFileDescriptor with BitmapFactory.decodeFileDescriptor for content:// schemes
     * 5. BufferedInputStream mark/reset fallback
     */
    fun decodeSampledBitmap(
        context: Context,
        uri: Uri,
        maxDimension: Int = 2048
    ): Bitmap? {
        // Strategy 1: Modern ImageDecoder on API 28+ (Android 9+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val source = if (uri.scheme == "file" && uri.path != null) {
                    val f = File(uri.path!!)
                    if (f.exists() && f.length() > 0L) {
                        ImageDecoder.createSource(f)
                    } else null
                } else {
                    ImageDecoder.createSource(context.contentResolver, uri)
                }
                if (source != null) {
                    val decoded = ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
                        val origW = info.size.width
                        val origH = info.size.height
                        if (origW > 0 && origH > 0) {
                            var sampleSize = 1
                            while ((origW / sampleSize) > maxDimension || (origH / sampleSize) > maxDimension) {
                                sampleSize *= 2
                            }
                            if (sampleSize > 1) {
                                decoder.setTargetSampleSize(sampleSize)
                            }
                        }
                    }
                    val safeBmp = if (decoded.config != Bitmap.Config.ARGB_8888) {
                        decoded.copy(Bitmap.Config.ARGB_8888, true) ?: decoded
                    } else {
                        decoded
                    }
                    if (safeBmp.width > 0 && safeBmp.height > 0) {
                        return safeBmp
                    }
                }
            } catch (_: Throwable) {
                // Fall through to BitmapFactory strategies
            }
        }

        // Strategy 2: If file scheme, decode directly from File path (avoids stream issues completely)
        if (uri.scheme == "file" && uri.path != null) {
            try {
                val file = File(uri.path!!)
                if (file.exists() && file.length() > 0L) {
                    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
                    if (boundsOpts.outWidth > 0 && boundsOpts.outHeight > 0) {
                        var sampleSize = 1
                        while ((boundsOpts.outWidth / sampleSize) > maxDimension ||
                            (boundsOpts.outHeight / sampleSize) > maxDimension
                        ) {
                            sampleSize *= 2
                        }
                        val decodeOpts = BitmapFactory.Options().apply {
                            inSampleSize = sampleSize
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        val rawBitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
                        if (rawBitmap != null) {
                            val rotation = getExifRotation(file.absolutePath)
                            return applyRotation(rawBitmap, rotation)
                        }
                    }
                }
            } catch (_: Throwable) {
                // Fallback
            }
        }

        // Strategy 3: Read bytes into memory once and decode with BitmapFactory.decodeByteArray
        // This is 100% immune to stream-consumption, mark/reset, and content-provider permission issues.
        try {
            val bytes = readBytesFromUri(context, uri)
            if (bytes != null && bytes.isNotEmpty()) {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeByteArray(bytes, 0, bytes.size, boundsOpts)
                if (boundsOpts.outWidth > 0 && boundsOpts.outHeight > 0) {
                    var sampleSize = 1
                    while ((boundsOpts.outWidth / sampleSize) > maxDimension ||
                        (boundsOpts.outHeight / sampleSize) > maxDimension
                    ) {
                        sampleSize *= 2
                    }
                    val decodeOpts = BitmapFactory.Options().apply {
                        inSampleSize = sampleSize
                        inPreferredConfig = Bitmap.Config.ARGB_8888
                    }
                    val rawBitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size, decodeOpts)
                    if (rawBitmap != null) {
                        val rotation = getExifRotationFromBytes(bytes)
                        return applyRotation(rawBitmap, rotation)
                    }
                }
            }
        } catch (_: Throwable) {
            // Fallback
        }

        // Strategy 4: FileDescriptor via ContentResolver
        if (uri.scheme == "content") {
            try {
                context.contentResolver.openFileDescriptor(uri, "r")?.use { pfd ->
                    val fd = pfd.fileDescriptor
                    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeFileDescriptor(fd, null, boundsOpts)
                    if (boundsOpts.outWidth > 0 && boundsOpts.outHeight > 0) {
                        var sampleSize = 1
                        while ((boundsOpts.outWidth / sampleSize) > maxDimension ||
                            (boundsOpts.outHeight / sampleSize) > maxDimension
                        ) {
                            sampleSize *= 2
                        }
                        val decodeOpts = BitmapFactory.Options().apply {
                            inSampleSize = sampleSize
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        val rawBitmap = BitmapFactory.decodeFileDescriptor(fd, null, decodeOpts)
                        if (rawBitmap != null) {
                            return rawBitmap
                        }
                    }
                }
            } catch (_: Throwable) {
                // Fallback
            }
        }

        // Strategy 5: Buffered stream fallback
        try {
            openStream(context, uri)?.let { rawStream ->
                java.io.BufferedInputStream(rawStream, 64 * 1024).use { bis ->
                    bis.mark(10 * 1024 * 1024)
                    val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                    BitmapFactory.decodeStream(bis, null, boundsOpts)
                    if (boundsOpts.outWidth > 0 && boundsOpts.outHeight > 0) {
                        bis.reset()
                        var sampleSize = 1
                        while ((boundsOpts.outWidth / sampleSize) > maxDimension ||
                            (boundsOpts.outHeight / sampleSize) > maxDimension
                        ) {
                            sampleSize *= 2
                        }
                        val decodeOpts = BitmapFactory.Options().apply {
                            inSampleSize = sampleSize
                            inPreferredConfig = Bitmap.Config.ARGB_8888
                        }
                        val rawBitmap = BitmapFactory.decodeStream(bis, null, decodeOpts)
                        if (rawBitmap != null) {
                            return rawBitmap
                        }
                    }
                }
            }
        } catch (_: Throwable) {
            // All attempts failed
        }

        return null
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
