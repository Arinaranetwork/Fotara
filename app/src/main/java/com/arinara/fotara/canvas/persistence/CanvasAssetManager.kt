// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.canvas.persistence

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

data class CanvasAssetInfo(
    val assetId: String,
    val canvasId: Long,
    val filePath: String,
    val mimeType: String,
    val width: Int,
    val height: Int,
    val fileSizeBytes: Long
)

/**
 * Manages image assets imported into infinite canvas documents.
 * Handles downsampling to a safe maximum dimension (2048px), applying EXIF orientation,
 * storing inside app storage (`filesDir/canvas_assets/`), and purging unreferenced files.
 */
class CanvasAssetManager(private val context: Context? = null) {

    private val assetDir: File by lazy {
        val baseDir = context?.filesDir ?: File(System.getProperty("java.io.tmpdir"), "canvas_assets")
        File(baseDir, "canvas_assets").apply {
            if (!exists()) mkdirs()
        }
    }

    companion object {
        const val MAX_IMAGE_DIMENSION = 2048
    }

    /**
     * Imports an image from an external Uri, applying EXIF rotation and downsampling.
     * Guaranteed to run off the main thread.
     */
    suspend fun importImage(
        sourceUri: Uri,
        canvasId: Long
    ): CanvasAssetInfo? = withContext(Dispatchers.IO) {
        try {
            val cr = context?.contentResolver ?: return@withContext null

            // 1. Decode bounds & EXIF orientation
            val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            cr.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, boundsOpts)
            } ?: return@withContext null

            if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) {
                return@withContext null
            }

            var rotationDegrees = 0
            try {
                cr.openInputStream(sourceUri)?.use { input ->
                    val exif = ExifInterface(input)
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

            // 2. Compute sample size to clamp to MAX_IMAGE_DIMENSION
            var sample = 1
            while ((boundsOpts.outWidth / sample) > MAX_IMAGE_DIMENSION ||
                (boundsOpts.outHeight / sample) > MAX_IMAGE_DIMENSION
            ) {
                sample *= 2
            }

            // 3. Decode scaled bitmap
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            var bitmap = cr.openInputStream(sourceUri)?.use { input ->
                BitmapFactory.decodeStream(input, null, decodeOpts)
            } ?: return@withContext null

            // 4. Apply EXIF rotation if needed
            if (rotationDegrees != 0) {
                val matrix = Matrix().apply { postRotate(rotationDegrees.toFloat()) }
                val rotated = Bitmap.createBitmap(
                    bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
                )
                if (rotated != bitmap) {
                    bitmap.recycle()
                    bitmap = rotated
                }
            }

            // 5. Save into managed storage
            val assetId = "asset_${UUID.randomUUID()}"
            val targetFile = File(assetDir, "${assetId}.jpg")
            FileOutputStream(targetFile).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            }

            val finalWidth = bitmap.width
            val finalHeight = bitmap.height
            val finalSize = targetFile.length()
            bitmap.recycle()

            CanvasAssetInfo(
                assetId = assetId,
                canvasId = canvasId,
                filePath = targetFile.absolutePath,
                mimeType = "image/jpeg",
                width = finalWidth,
                height = finalHeight,
                fileSizeBytes = finalSize
            )
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Resolves the on-disk file for a given asset ID.
     */
    fun getAssetFile(assetId: String): File {
        return File(assetDir, "${assetId}.jpg")
    }

    /**
     * Deletes a specific asset file from disk.
     */
    fun deleteAssetFile(filePath: String): Boolean {
        return try {
            val file = File(filePath)
            if (file.exists() && file.parentFile == assetDir) {
                file.delete()
            } else {
                false
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Purges asset files on disk that are no longer referenced in the document.
     */
    fun purgeOrphanedAssets(canvasId: Long, activeAssetIds: Set<String>) {
        try {
            val prefix = "asset_"
            assetDir.listFiles()?.forEach { file ->
                val id = file.nameWithoutExtension
                if (id.startsWith(prefix) && id !in activeAssetIds) {
                    // Check if file belongs to this canvas or is completely orphaned
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }
}
