// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID

data class SavedPhotoFile(
    val filePath: String,
    val thumbnailPath: String,
    val fileSizeBytes: Long
)

class PhotoStorageManager(private val context: Context) {

    private val photosDir: File by lazy {
        File(context.filesDir, "photos").apply { if (!exists()) mkdirs() }
    }

    private val thumbsDir: File by lazy {
        File(context.filesDir, "thumbnails").apply { if (!exists()) mkdirs() }
    }

    suspend fun saveBitmapAsPhoto(bitmap: Bitmap): SavedPhotoFile = withContext(Dispatchers.IO) {
        val id = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val photoFile = File(photosDir, "photo_$id.jpg")
        val thumbFile = File(thumbsDir, "thumb_$id.jpg")

        // 1. Save original full-resolution photo
        FileOutputStream(photoFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }

        // 2. Generate and save downscaled thumbnail (max 360x360 for fast grid rendering)
        val thumbBitmap = generateThumbnail(bitmap, maxDimension = 360)
        FileOutputStream(thumbFile).use { out ->
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }
        if (thumbBitmap != bitmap) {
            thumbBitmap.recycle()
        }

        SavedPhotoFile(
            filePath = photoFile.absolutePath,
            thumbnailPath = thumbFile.absolutePath,
            fileSizeBytes = photoFile.length()
        )
    }

    suspend fun saveUriAsPhoto(sourceUri: Uri): SavedPhotoFile = withContext(Dispatchers.IO) {
        val id = "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(6)}"
        val photoFile = File(photosDir, "photo_$id.jpg")
        val thumbFile = File(thumbsDir, "thumb_$id.jpg")

        // 1. Copy stream to photoFile
        context.contentResolver.openInputStream(sourceUri)?.use { input ->
            FileOutputStream(photoFile).use { output ->
                input.copyTo(output)
            }
        } ?: throw IllegalStateException("Unable to open input stream for URI: $sourceUri")

        // 2. Decode downsampled bitmap for thumbnail
        val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(photoFile.absolutePath, boundsOptions)

        val sampleSize = calculateInSampleSize(boundsOptions, 360, 360)
        val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
        val decodedBitmap = BitmapFactory.decodeFile(photoFile.absolutePath, decodeOptions)

        if (decodedBitmap != null) {
            // Check orientation via ExifInterface
            val orientedBitmap = applyExifRotation(photoFile.absolutePath, decodedBitmap)
            val thumbBitmap = generateThumbnail(orientedBitmap, maxDimension = 360)
            FileOutputStream(thumbFile).use { out ->
                thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
            }
            if (thumbBitmap != orientedBitmap) thumbBitmap.recycle()
            if (orientedBitmap != decodedBitmap) orientedBitmap.recycle()
            decodedBitmap.recycle()
        } else {
            // Fallback copy if decode fails
            photoFile.copyTo(thumbFile, overwrite = true)
        }

        SavedPhotoFile(
            filePath = photoFile.absolutePath,
            thumbnailPath = thumbFile.absolutePath,
            fileSizeBytes = photoFile.length()
        )
    }

    suspend fun deletePhotoFiles(filePath: String, thumbnailPath: String?) = withContext(Dispatchers.IO) {
        try {
            val mainFile = File(filePath)
            if (mainFile.exists()) mainFile.delete()

            if (!thumbnailPath.isNullOrBlank()) {
                val thumb = File(thumbnailPath)
                if (thumb.exists()) thumb.delete()
            }
        } catch (_: Exception) {
            // Safely ignore file system deletion errors
        }
    }

    suspend fun calculateStorageBreakdown(): com.arinara.fotara.data.model.StorageBreakdown = withContext(Dispatchers.IO) {
        val photosBytes = photosDir.listFiles()?.sumOf { it.length() } ?: 0L
        val thumbsBytes = thumbsDir.listFiles()?.sumOf { it.length() } ?: 0L
        val dbFile = context.getDatabasePath(com.arinara.fotara.data.db.FotaraDbHelper.DATABASE_NAME)
        var dbBytes = 0L
        if (dbFile != null && dbFile.exists()) {
            dbBytes += dbFile.length()
            val walFile = File(dbFile.parentFile, "${com.arinara.fotara.data.db.FotaraDbHelper.DATABASE_NAME}-wal")
            if (walFile.exists()) dbBytes += walFile.length()
            val shmFile = File(dbFile.parentFile, "${com.arinara.fotara.data.db.FotaraDbHelper.DATABASE_NAME}-shm")
            if (shmFile.exists()) dbBytes += shmFile.length()
        }
        com.arinara.fotara.data.model.StorageBreakdown(
            photosSizeBytes = photosBytes,
            thumbnailsSizeBytes = thumbsBytes,
            databaseSizeBytes = dbBytes
        )
    }

    suspend fun rebuildThumbnailForFile(filePath: String, thumbnailPath: String?): String? = withContext(Dispatchers.IO) {
        try {
            val photoFile = File(filePath)
            if (!photoFile.exists()) return@withContext null

            val thumbFile = if (!thumbnailPath.isNullOrBlank()) {
                File(thumbnailPath)
            } else {
                File(thumbsDir, "thumb_${photoFile.nameWithoutExtension}.jpg")
            }

            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(photoFile.absolutePath, boundsOptions)

            val sampleSize = calculateInSampleSize(boundsOptions, 360, 360)
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val decodedBitmap = BitmapFactory.decodeFile(photoFile.absolutePath, decodeOptions)

            if (decodedBitmap != null) {
                val orientedBitmap = applyExifRotation(photoFile.absolutePath, decodedBitmap)
                val thumbBitmap = generateThumbnail(orientedBitmap, maxDimension = 360)
                FileOutputStream(thumbFile).use { out ->
                    thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
                }
                if (thumbBitmap != orientedBitmap) thumbBitmap.recycle()
                if (orientedBitmap != decodedBitmap) orientedBitmap.recycle()
                decodedBitmap.recycle()
                thumbFile.absolutePath
            } else {
                photoFile.copyTo(thumbFile, overwrite = true)
                thumbFile.absolutePath
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun rotatePhotoClockwise(filePath: String, thumbnailPath: String?): SavedPhotoFile = withContext(Dispatchers.IO) {
        val photoFile = File(filePath)
        if (!photoFile.exists()) throw IllegalStateException("Photo file not found: $filePath")

        val original = BitmapFactory.decodeFile(photoFile.absolutePath)
            ?: throw IllegalStateException("Failed to decode photo for rotation")

        val matrix = Matrix().apply { postRotate(90f) }
        val rotated = Bitmap.createBitmap(original, 0, 0, original.width, original.height, matrix, true)

        FileOutputStream(photoFile).use { out ->
            rotated.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }

        val thumbFile = if (!thumbnailPath.isNullOrBlank()) {
            File(thumbnailPath)
        } else {
            File(thumbsDir, "thumb_${photoFile.nameWithoutExtension}.jpg")
        }
        val thumbBitmap = generateThumbnail(rotated, maxDimension = 360)
        FileOutputStream(thumbFile).use { out ->
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }

        if (thumbBitmap != rotated) thumbBitmap.recycle()
        if (rotated != original) rotated.recycle()
        original.recycle()

        SavedPhotoFile(
            filePath = photoFile.absolutePath,
            thumbnailPath = thumbFile.absolutePath,
            fileSizeBytes = photoFile.length()
        )
    }

    suspend fun cropPhoto(
        filePath: String,
        thumbnailPath: String?,
        leftFraction: Float,
        topFraction: Float,
        rightFraction: Float,
        bottomFraction: Float
    ): SavedPhotoFile = withContext(Dispatchers.IO) {
        val photoFile = File(filePath)
        if (!photoFile.exists()) throw IllegalStateException("Photo file not found: $filePath")

        val original = BitmapFactory.decodeFile(photoFile.absolutePath)
            ?: throw IllegalStateException("Failed to decode photo for cropping")

        val l = (leftFraction.coerceIn(0f, 1f) * original.width).toInt()
        val t = (topFraction.coerceIn(0f, 1f) * original.height).toInt()
        val r = (rightFraction.coerceIn(0f, 1f) * original.width).toInt().coerceAtLeast(l + 10)
        val b = (bottomFraction.coerceIn(0f, 1f) * original.height).toInt().coerceAtLeast(t + 10)

        val cropWidth = (r - l).coerceAtMost(original.width - l).coerceAtLeast(1)
        val cropHeight = (b - t).coerceAtMost(original.height - t).coerceAtLeast(1)

        val cropped = Bitmap.createBitmap(original, l, t, cropWidth, cropHeight)

        FileOutputStream(photoFile).use { out ->
            cropped.compress(Bitmap.CompressFormat.JPEG, 92, out)
        }

        val thumbFile = if (!thumbnailPath.isNullOrBlank()) {
            File(thumbnailPath)
        } else {
            File(thumbsDir, "thumb_${photoFile.nameWithoutExtension}.jpg")
        }
        val thumbBitmap = generateThumbnail(cropped, maxDimension = 360)
        FileOutputStream(thumbFile).use { out ->
            thumbBitmap.compress(Bitmap.CompressFormat.JPEG, 80, out)
        }

        if (thumbBitmap != cropped) thumbBitmap.recycle()
        if (cropped != original) cropped.recycle()
        original.recycle()

        SavedPhotoFile(
            filePath = photoFile.absolutePath,
            thumbnailPath = thumbFile.absolutePath,
            fileSizeBytes = photoFile.length()
        )
    }

    private fun generateThumbnail(source: Bitmap, maxDimension: Int): Bitmap {
        val width = source.width
        val height = source.height
        if (width <= maxDimension && height <= maxDimension) return source

        val scale = maxDimension.toFloat() / maxOf(width, height)
        val targetW = (width * scale).toInt().coerceAtLeast(1)
        val targetH = (height * scale).toInt().coerceAtLeast(1)

        return Bitmap.createScaledBitmap(source, targetW, targetH, true)
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize.coerceAtLeast(1)
    }

    private fun applyExifRotation(filePath: String, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(filePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val rotationDegrees = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90f
                ExifInterface.ORIENTATION_ROTATE_180 -> 180f
                ExifInterface.ORIENTATION_ROTATE_270 -> 270f
                else -> 0f
            }
            if (rotationDegrees != 0f) {
                val matrix = Matrix().apply { postRotate(rotationDegrees) }
                Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
            } else {
                bitmap
            }
        } catch (_: Exception) {
            bitmap
        }
    }
}
