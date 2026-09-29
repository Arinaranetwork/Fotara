// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import android.content.Context
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.ui.folder.FolderGridItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ZipExporter {

    suspend fun exportPhotosToZip(
        context: Context,
        title: String,
        photos: List<Photo>,
        onProgress: ((Float) -> Unit)? = null
    ): File = withContext(Dispatchers.IO) {
        val exportsDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cleanTitle = title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = File(exportsDir, "Fotara_${cleanTitle}_${dateFormat.format(Date())}.zip")

        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            val total = photos.size
            for ((index, photo) in photos.withIndex()) {
                val srcFile = File(photo.fileUri)
                if (srcFile.exists()) {
                    val entryName = String.format(Locale.US, "%02d_%s.jpg", index + 1, photo.caption?.take(30)?.replace(Regex("[^a-zA-Z0-9_-]"), "_") ?: "note")
                    zos.putNextEntry(ZipEntry(entryName))
                    FileInputStream(srcFile).use { fis ->
                        fis.copyTo(zos)
                    }
                    zos.closeEntry()

                    // Optional note file if note is present
                    val noteContent = photo.note ?: photo.caption
                    if (!noteContent.isNullOrBlank()) {
                        val txtEntryName = String.format(Locale.US, "%02d_%s.txt", index + 1, photo.caption?.take(30)?.replace(Regex("[^a-zA-Z0-9_-]"), "_") ?: "note")
                        zos.putNextEntry(ZipEntry(txtEntryName))
                        zos.write(noteContent.toByteArray(Charsets.UTF_8))
                        zos.closeEntry()
                    }
                }
                onProgress?.invoke((index + 1).toFloat() / total.coerceAtLeast(1))
            }
        }

        outputFile
    }

    suspend fun exportGridItemsToZip(
        context: Context,
        folderName: String,
        gridItems: List<FolderGridItem>,
        onProgress: ((Float) -> Unit)? = null
    ): File = withContext(Dispatchers.IO) {
        val exportsDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val dateFormat = SimpleDateFormat("yyyy-MM-dd_HHmmss", Locale.US)
        val cleanTitle = folderName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = File(exportsDir, "Fotara_Export_${cleanTitle}_${dateFormat.format(Date())}.zip")

        var totalPhotos = 0
        for (item in gridItems) {
            when (item) {
                is FolderGridItem.StandalonePhoto -> totalPhotos += 1
                is FolderGridItem.Group -> totalPhotos += item.memberPhotos.size
                is FolderGridItem.Document -> totalPhotos += 1
                is FolderGridItem.TextNoteItem -> totalPhotos += 1
            }
        }
        if (totalPhotos == 0) totalPhotos = 1

        var processedPhotos = 0

        ZipOutputStream(FileOutputStream(outputFile)).use { zos ->
            var standaloneIndex = 1
            for (item in gridItems) {
                when (item) {
                    is FolderGridItem.StandalonePhoto -> {
                        val photo = item.photo
                        val srcFile = File(photo.fileUri)
                        if (srcFile.exists()) {
                            val entryName = String.format(Locale.US, "%02d_%s.jpg", standaloneIndex, photo.caption?.take(30)?.replace(Regex("[^a-zA-Z0-9_-]"), "_") ?: "note")
                            zos.putNextEntry(ZipEntry(entryName))
                            FileInputStream(srcFile).use { fis -> fis.copyTo(zos) }
                            zos.closeEntry()

                            val noteContent = photo.note ?: photo.caption
                            if (!noteContent.isNullOrBlank()) {
                                val txtEntryName = String.format(Locale.US, "%02d_%s.txt", standaloneIndex, photo.caption?.take(30)?.replace(Regex("[^a-zA-Z0-9_-]"), "_") ?: "note")
                                zos.putNextEntry(ZipEntry(txtEntryName))
                                zos.write(noteContent.toByteArray(Charsets.UTF_8))
                                zos.closeEntry()
                            }
                        }
                        standaloneIndex++
                        processedPhotos++
                        onProgress?.invoke(processedPhotos.toFloat() / totalPhotos)
                    }
                    is FolderGridItem.Group -> {
                        val groupDirName = item.group.name.replace(Regex("[^a-zA-Z0-9_-]"), "_")
                        for ((mIndex, photo) in item.memberPhotos.withIndex()) {
                            val srcFile = File(photo.fileUri)
                            if (srcFile.exists()) {
                                val entryName = String.format(Locale.US, "%s/%02d_%s.jpg", groupDirName, mIndex + 1, photo.caption?.take(30)?.replace(Regex("[^a-zA-Z0-9_-]"), "_") ?: "photo")
                                zos.putNextEntry(ZipEntry(entryName))
                                FileInputStream(srcFile).use { fis -> fis.copyTo(zos) }
                                zos.closeEntry()

                                val noteContent = photo.note ?: photo.caption
                                if (!noteContent.isNullOrBlank()) {
                                    val txtEntryName = String.format(Locale.US, "%s/%02d_%s.txt", groupDirName, mIndex + 1, photo.caption?.take(30)?.replace(Regex("[^a-zA-Z0-9_-]"), "_") ?: "photo")
                                    zos.putNextEntry(ZipEntry(txtEntryName))
                                    zos.write(noteContent.toByteArray(Charsets.UTF_8))
                                    zos.closeEntry()
                                }
                            }
                            processedPhotos++
                            onProgress?.invoke(processedPhotos.toFloat() / totalPhotos)
                        }
                    }
                    is FolderGridItem.Document -> {
                        val doc = item.documentNote
                        val srcFile = File(doc.originFileUri)
                        if (srcFile.exists()) {
                            val ext = if (doc.docType == com.arinara.fotara.data.model.DocumentType.PDF) "pdf" else "docx"
                            val cleanName = doc.name.take(40).replace(Regex("[^a-zA-Z0-9_-]"), "_")
                            val entryName = String.format(Locale.US, "Documents/%02d_%s.%s", standaloneIndex, cleanName, ext)
                            zos.putNextEntry(ZipEntry(entryName))
                            FileInputStream(srcFile).use { fis -> fis.copyTo(zos) }
                            zos.closeEntry()
                        }
                        standaloneIndex++
                        processedPhotos++
                        onProgress?.invoke(processedPhotos.toFloat() / totalPhotos)
                    }
                    is FolderGridItem.TextNoteItem -> {
                        val note = item.textNote
                        val cleanName = note.title.take(40).replace(Regex("[^a-zA-Z0-9_-]"), "_")
                        val entryName = String.format(Locale.US, "Notes/%02d_%s.md", standaloneIndex, cleanName)
                        zos.putNextEntry(ZipEntry(entryName))
                        zos.write(note.bodyMarkdown.toByteArray(Charsets.UTF_8))
                        zos.closeEntry()
                        standaloneIndex++
                        processedPhotos++
                        onProgress?.invoke(processedPhotos.toFloat() / totalPhotos)
                    }
                }
            }
        }

        outputFile
    }
}
