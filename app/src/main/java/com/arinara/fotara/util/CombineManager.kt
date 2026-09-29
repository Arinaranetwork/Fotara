// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.pdf.PdfDocument
import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.DocumentPage
import com.arinara.fotara.data.model.DocumentType
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.model.PhotoGroup
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed class CombineItem {
    data class StandalonePhoto(val photo: Photo) : CombineItem()
    data class Group(val group: PhotoGroup, val members: List<Photo>) : CombineItem()
    data class Document(val documentNote: DocumentNote, val pages: List<DocumentPage>) : CombineItem()

    fun pageCount(): Int = when (this) {
        is StandalonePhoto -> 1
        is Group -> members.size.coerceAtLeast(1)
        is Document -> if (documentNote.docType == DocumentType.PDF) pages.size.coerceAtLeast(1) else 1
    }
}

class PageLimitExceededException(val totalPages: Int) : Exception(
    "Selection exceeds the 100-page limit (Selected: $totalPages pages). Please reduce your selection to ensure memory stability."
)

class CombineManager(private val context: Context) {

    private val docxExporter = DocxExporter(context)

    fun calculateTotalPages(items: List<CombineItem>): Int {
        return items.sumOf { it.pageCount() }
    }

    suspend fun combineToPdf(
        title: String,
        items: List<CombineItem>,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): File = withContext(Dispatchers.IO) {
        val totalPages = calculateTotalPages(items)
        if (totalPages > 100) {
            throw PageLimitExceededException(totalPages)
        }

        val exportsDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val cleanName = title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = File(exportsDir, "${cleanName}_${System.currentTimeMillis()}.pdf")

        val pdfDoc = PdfDocument()
        val pageWidth = 595
        val pageHeight = 842
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        var currentPageNum = 0

        try {
            for (item in items) {
                ensureActive()
                when (item) {
                    is CombineItem.StandalonePhoto -> {
                        currentPageNum++
                        renderImagePage(pdfDoc, item.photo.fileUri, item.photo.caption ?: "Note #$currentPageNum", currentPageNum, totalPages, pageWidth, pageHeight, paint)
                        onProgress?.invoke(currentPageNum, totalPages)
                    }
                    is CombineItem.Group -> {
                        for (photo in item.members) {
                            ensureActive()
                            currentPageNum++
                            val cap = "${item.group.name} — ${photo.caption ?: "Item"}"
                            renderImagePage(pdfDoc, photo.fileUri, cap, currentPageNum, totalPages, pageWidth, pageHeight, paint)
                            onProgress?.invoke(currentPageNum, totalPages)
                        }
                    }
                    is CombineItem.Document -> {
                        if (item.documentNote.docType == DocumentType.PDF) {
                            for (page in item.pages) {
                                ensureActive()
                                currentPageNum++
                                val cap = "${item.documentNote.name} (P${page.pageIndex + 1})"
                                renderImagePage(pdfDoc, page.imageUri, cap, currentPageNum, totalPages, pageWidth, pageHeight, paint)
                                onProgress?.invoke(currentPageNum, totalPages)
                            }
                        } else {
                            // DOCX: render text page
                            ensureActive()
                            currentPageNum++
                            renderTextPage(pdfDoc, item.documentNote.name, item.documentNote.extractedText ?: "", currentPageNum, totalPages, pageWidth, pageHeight, paint)
                            onProgress?.invoke(currentPageNum, totalPages)
                        }
                    }
                }
            }

            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }
            pdfDoc.close()
            outputFile
        } catch (e: CancellationException) {
            pdfDoc.close()
            if (outputFile.exists()) outputFile.delete()
            throw e
        } catch (e: Exception) {
            pdfDoc.close()
            if (outputFile.exists()) outputFile.delete()
            throw e
        }
    }

    suspend fun combineToDocx(
        title: String,
        items: List<CombineItem>,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): File = withContext(Dispatchers.IO) {
        val totalPages = calculateTotalPages(items)
        if (totalPages > 100) {
            throw PageLimitExceededException(totalPages)
        }

        val exportsDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val cleanName = title.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = File(exportsDir, "${cleanName}_${System.currentTimeMillis()}.docx")

        val docxItems = mutableListOf<DocxContentItem>()
        for (item in items) {
            ensureActive()
            when (item) {
                is CombineItem.StandalonePhoto -> {
                    docxItems.add(DocxContentItem.ImagePage(item.photo.fileUri, item.photo.caption))
                }
                is CombineItem.Group -> {
                    for (photo in item.members) {
                        docxItems.add(DocxContentItem.ImagePage(photo.fileUri, "${item.group.name}: ${photo.caption ?: ""}"))
                    }
                }
                is CombineItem.Document -> {
                    if (item.documentNote.docType == DocumentType.PDF) {
                        for (page in item.pages) {
                            docxItems.add(DocxContentItem.ImagePage(page.imageUri, "${item.documentNote.name} P${page.pageIndex + 1}"))
                        }
                    } else {
                        docxItems.add(DocxContentItem.TextSection(item.documentNote.name, item.documentNote.extractedText ?: ""))
                    }
                }
            }
        }

        try {
            docxExporter.createDocxDocument(outputFile, title, docxItems, onProgress)
            outputFile
        } catch (e: CancellationException) {
            if (outputFile.exists()) outputFile.delete()
            throw e
        } catch (e: Exception) {
            if (outputFile.exists()) outputFile.delete()
            throw e
        }
    }

    private fun renderImagePage(
        pdfDoc: PdfDocument,
        imagePath: String,
        caption: String,
        pageNum: Int,
        total: Int,
        pageWidth: Int,
        pageHeight: Int,
        paint: Paint
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        paint.color = Color.DKGRAY
        paint.textSize = 14f
        canvas.drawText(caption, 36f, 40f, paint)

        paint.textSize = 10f
        paint.color = Color.GRAY
        canvas.drawText("Page $pageNum of $total • Fotara Notes", 36f, 56f, paint)

        val file = File(imagePath)
        if (file.exists()) {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, bounds)

            val maxW = pageWidth - 72
            val maxH = pageHeight - 120
            var sample = 1
            while (bounds.outHeight / sample > maxH || bounds.outWidth / sample > maxW) {
                sample *= 2
            }
            val decodeOpts = BitmapFactory.Options().apply { inSampleSize = sample }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOpts)

            if (bitmap != null) {
                val scale = minOf(maxW.toFloat() / bitmap.width, maxH.toFloat() / bitmap.height)
                val dw = (bitmap.width * scale).toInt()
                val dh = (bitmap.height * scale).toInt()
                val left = (pageWidth - dw) / 2
                val top = 70 + (maxH - dh) / 2

                canvas.drawBitmap(bitmap, null, Rect(left, top, left + dw, top + dh), paint)
                bitmap.recycle()
            }
        }

        pdfDoc.finishPage(page)
    }

    private fun renderTextPage(
        pdfDoc: PdfDocument,
        title: String,
        text: String,
        pageNum: Int,
        total: Int,
        pageWidth: Int,
        pageHeight: Int,
        paint: Paint
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageNum).create()
        val page = pdfDoc.startPage(pageInfo)
        val canvas = page.canvas

        canvas.drawColor(Color.WHITE)

        paint.color = Color.BLACK
        paint.textSize = 16f
        canvas.drawText(title, 36f, 40f, paint)

        paint.textSize = 10f
        paint.color = Color.GRAY
        canvas.drawText("Page $pageNum of $total • Fotara Document", 36f, 56f, paint)

        paint.color = Color.DKGRAY
        paint.textSize = 11f
        var y = 80f
        val lines = text.split("\n")
        for (line in lines) {
            if (y > pageHeight - 40) break
            val trimmed = line.trim()
            if (trimmed.isNotBlank()) {
                canvas.drawText(trimmed.take(80), 36f, y, paint)
                y += 18f
            }
        }

        pdfDoc.finishPage(page)
    }
}
