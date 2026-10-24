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
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.Base64
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
     * Copies the stream to a temporary file first to prevent issues with single-read content URIs.
     */
    suspend fun importImage(
        sourceUri: Uri,
        canvasId: Long
    ): CanvasAssetInfo? = withContext(Dispatchers.IO) {
        val cr = context?.contentResolver ?: return@withContext null
        val tempFile = try {
            File.createTempFile("canvas_stage_", ".tmp", context.cacheDir)
        } catch (_: Exception) {
            File(assetDir, "canvas_stage_${UUID.randomUUID()}.tmp")
        }

        try {
            // Copy stream exactly once to local staging file
            val copied = cr.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output) > 0
                }
            } ?: false

            if (!copied || !tempFile.exists() || tempFile.length() == 0L) {
                tempFile.delete()
                return@withContext null
            }

            importFromFileInternal(tempFile, canvasId)
        } catch (e: Exception) {
            android.util.Log.e("CanvasAssetManager", "Failed to import image from Uri: ${e.message}", e)
            null
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    /**
     * Imports an image from an existing local file (e.g. from photo notes or camera output),
     * applying EXIF rotation and downsampling to app-private storage.
     */
    suspend fun importImageFile(
        sourceFile: File,
        canvasId: Long
    ): CanvasAssetInfo? = withContext(Dispatchers.IO) {
        if (!sourceFile.exists() || sourceFile.length() == 0L) return@withContext null
        try {
            importFromFileInternal(sourceFile, canvasId)
        } catch (e: Exception) {
            android.util.Log.e("CanvasAssetManager", "Failed to import image from file: ${e.message}", e)
            null
        }
    }

    private fun importFromFileInternal(
        file: File,
        canvasId: Long
    ): CanvasAssetInfo? {
        // 1. Decode bounds
        val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, boundsOpts)

        if (boundsOpts.outWidth <= 0 || boundsOpts.outHeight <= 0) {
            return null
        }

        // 2. Read EXIF orientation
        var rotationDegrees = 0
        try {
            val exif = ExifInterface(file.absolutePath)
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
        } catch (_: Exception) {}

        // 3. Compute sample size to clamp to CanvasConfig.MAX_IMAGE_DIMENSION
        val maxDim = com.arinara.fotara.canvas.engine.CanvasConfig.MAX_IMAGE_DIMENSION
        var sample = 1
        while ((boundsOpts.outWidth / sample) > maxDim ||
            (boundsOpts.outHeight / sample) > maxDim
        ) {
            sample *= 2
        }

        // 4. Decode scaled bitmap
        val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
        var bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOpts) ?: return null

        // 5. Apply EXIF rotation if needed
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

        // 6. Save into managed storage
        val assetId = "asset_${UUID.randomUUID()}"
        val targetFile = File(assetDir, "${assetId}.jpg")
        FileOutputStream(targetFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }

        val finalWidth = bitmap.width
        val finalHeight = bitmap.height
        val finalSize = targetFile.length()
        bitmap.recycle()

        return CanvasAssetInfo(
            assetId = assetId,
            canvasId = canvasId,
            filePath = targetFile.absolutePath,
            mimeType = "image/jpeg",
            width = finalWidth,
            height = finalHeight,
            fileSizeBytes = finalSize
        )
    }

    /**
     * Resolves the on-disk file for a given asset ID.
     */
    fun getAssetFile(assetId: String): File {
        val safeId = assetId.replace(Regex("[^A-Za-z0-9_-]"), "_")
        return File(assetDir, "${safeId}.jpg")
    }

    /**
     * Encodes an existing canvas image asset into a compact Base64 JPEG payload for
     * real-time collaborative canvas synchronization across peers.
     */
    fun encodeAssetToBase64(assetId: String): String {
        return try {
            val file = getAssetFile(assetId)
            if (!file.exists() || file.length() == 0L) return ""
            val bytes = if (file.length() <= 350_000L) {
                file.readBytes()
            } else {
                val boundsOpts = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(file.absolutePath, boundsOpts)
                var sample = 1
                while ((boundsOpts.outWidth / sample) > 960 || (boundsOpts.outHeight / sample) > 960) {
                    sample *= 2
                }
                val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
                val bmp = BitmapFactory.decodeFile(file.absolutePath, decodeOpts)
                if (bmp != null) {
                    val out = ByteArrayOutputStream()
                    bmp.compress(Bitmap.CompressFormat.JPEG, 72, out)
                    bmp.recycle()
                    out.toByteArray()
                } else {
                    file.readBytes()
                }
            }
            Base64.getEncoder().encodeToString(bytes)
        } catch (_: Throwable) {
            ""
        }
    }

    /**
     * Persists a Base64-encoded image asset received from a remote collaborator so that
     * [com.arinara.fotara.canvas.render.CanvasRenderer] can render the [com.arinara.fotara.canvas.model.ImageElement].
     */
    fun saveRemoteAssetFromBase64(assetId: String, base64Data: String): Boolean {
        if (assetId.isBlank() || base64Data.isBlank()) return false
        return try {
            val targetFile = getAssetFile(assetId)
            if (targetFile.exists() && targetFile.length() > 0L) return true
            val decoded = Base64.getDecoder().decode(base64Data)
            if (decoded.isEmpty()) return false
            FileOutputStream(targetFile).use { out ->
                out.write(decoded)
            }
            true
        } catch (_: Throwable) {
            false
        }
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
                    file.delete()
                }
            }
        } catch (_: Exception) {}
    }

    /**
     * Saves an offscreen rendered thumbnail bitmap for a canvas note and returns its file path.
     */
    fun saveThumbnail(canvasId: Long, bitmap: Bitmap): String? {
        return try {
            val baseDir = context?.filesDir ?: File(System.getProperty("java.io.tmpdir") ?: ".")
            val thumbDir = File(baseDir, "thumbnails").apply { if (!exists()) mkdirs() }
            val file = File(thumbDir, "canvas_${canvasId}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 90, out)
            }
            file.absolutePath
        } catch (_: Exception) {
            null
        }
    }
}
