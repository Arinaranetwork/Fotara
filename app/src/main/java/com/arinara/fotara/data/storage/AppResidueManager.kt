// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.data.storage

import android.content.Context
import android.os.Environment
import com.arinara.fotara.data.model.AppResidueInfo
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

/**
 * Manages detection and safe purging of disposable application residue:
 * leftover update packages, partial download chunks, share staging files,
 * export scratch buffers, docx unpacking caches, and Coil image decode caches.
 *
 * Coursework photos, notes, folders, databases, and preferences are strictly excluded.
 */
class AppResidueManager(private val context: Context) {

    /**
     * Collects all disposable residue files from cache and download directories.
     */
    private fun collectResidueFiles(): List<File> {
        val files = mutableListOf<File>()

        fun addDirectoryContents(dir: File?) {
            if (dir == null || !dir.exists() || !dir.isDirectory) return
            dir.walkTopDown().forEach { file ->
                if (file.isFile && !isProtectedFile(file)) {
                    files.add(file)
                }
            }
        }

        // 1. Download directories for leftover APK packages & .part streams
        val downloadDirs = listOfNotNull(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            context.externalCacheDir,
            context.cacheDir
        )
        for (dir in downloadDirs) {
            dir.listFiles()?.forEach { file ->
                val name = file.name
                if (file.isFile && (name.startsWith("Fotara_Update_") || name.endsWith(".apk") || name.endsWith(".apk.part"))) {
                    if (!isProtectedFile(file)) {
                        files.add(file)
                    }
                }
            }
        }

        // 2. Share staging directories
        addDirectoryContents(File(context.cacheDir, "share_staging"))
        context.externalCacheDir?.let { addDirectoryContents(File(it, "share_staging")) }

        // 3. Export staging directories (PDF, DOCX, ZIP scratch)
        addDirectoryContents(File(context.cacheDir, "exports"))
        context.externalCacheDir?.let { addDirectoryContents(File(it, "exports")) }

        // 4. Temporary DOCX media directories in cache
        context.cacheDir.listFiles()?.forEach { file ->
            if (file.name.startsWith("docx_media_")) {
                if (file.isDirectory) {
                    addDirectoryContents(file)
                } else if (file.isFile && !isProtectedFile(file)) {
                    files.add(file)
                }
            }
        }

        // 5. Temporary crop, avatar, banner, canvas scratch files
        context.cacheDir.listFiles()?.forEach { file ->
            val name = file.name
            if (file.isFile && (
                    name.startsWith("temp_crop_") ||
                    name.startsWith("temp_avatar_") ||
                    name.startsWith("temp_banner_") ||
                    name.startsWith("canvas_stage_") ||
                    name.startsWith("temp_print_") ||
                    name.startsWith("temp_pdf_")
                )) {
                if (!isProtectedFile(file)) {
                    files.add(file)
                }
            }
        }

        // 6. Coil & image disk cache
        addDirectoryContents(File(context.cacheDir, "image_cache"))
        context.externalCacheDir?.let { addDirectoryContents(File(it, "image_cache")) }

        return files.distinctBy { it.absolutePath }
    }

    /**
     * Safety invariant: databases, user notes, and app preferences must NEVER be deleted.
     */
    private fun isProtectedFile(file: File): Boolean {
        val path = file.absolutePath.lowercase()
        return path.endsWith(".db") ||
            path.endsWith(".db-wal") ||
            path.endsWith(".db-shm") ||
            path.contains("/databases/") ||
            path.contains("/shared_prefs/") ||
            path.contains("/files/photos/") ||
            path.contains("/files/thumbnails/") ||
            path.contains("/files/workspaces/")
    }

    suspend fun calculateAppResidue(): AppResidueInfo = withContext(Dispatchers.IO) {
        val files = collectResidueFiles()
        val totalBytes = files.sumOf { it.length() }
        AppResidueInfo(
            totalSizeBytes = totalBytes,
            fileCount = files.size
        )
    }

    suspend fun cleanAppResidue(onProgress: (Float) -> Unit = {}): Long = withContext(Dispatchers.IO) {
        val files = collectResidueFiles()
        if (files.isEmpty()) {
            onProgress(1f)
            return@withContext 0L
        }

        var reclaimedBytes = 0L
        val total = files.size

        files.forEachIndexed { index, file ->
            val len = file.length()
            val deleted = try {
                file.delete()
            } catch (_: Exception) {
                false
            }
            if (deleted) {
                reclaimedBytes += len
            }
            val progress = ((index + 1).toFloat() / total.toFloat()).coerceIn(0f, 1f)
            onProgress(progress)
            if (total > 10 && index % 5 == 0) {
                delay(10) // Smooth progress reporting
            }
        }

        // Clean any empty directories leftover in staging
        try {
            File(context.cacheDir, "share_staging").delete()
            File(context.cacheDir, "exports").delete()
            context.cacheDir.listFiles()?.forEach { file ->
                if (file.isDirectory && file.name.startsWith("docx_media_")) {
                    file.deleteRecursively()
                }
            }
        } catch (_: Exception) {}

        onProgress(1f)
        reclaimedBytes
    }
}
