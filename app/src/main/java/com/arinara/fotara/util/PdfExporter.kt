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
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.ui.folder.FolderGridItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    suspend fun exportPhotosToPdf(
        context: Context,
        folderName: String,
        photos: List<Photo>
    ): File = withContext(Dispatchers.IO) {
        val exportsDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val cleanName = folderName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = File(exportsDir, "${cleanName}_Notes_${System.currentTimeMillis()}.pdf")

        val pdfDoc = PdfDocument()

        val pageWidth = 595 // Standard A4 points at 72dpi
        val pageHeight = 842
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        for ((index, photo) in photos.withIndex()) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, index + 1).create()
            val page = pdfDoc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Fill page background
            canvas.drawColor(Color.WHITE)

            // Header text
            paint.color = Color.DKGRAY
            paint.textSize = 14f
            val title = photo.caption ?: "$folderName — Note #${index + 1}"
            canvas.drawText(title, 36f, 40f, paint)

            paint.textSize = 10f
            paint.color = Color.GRAY
            canvas.drawText("Page ${index + 1} of ${photos.size} • Fotara Offline Notes", 36f, 56f, paint)

            drawPhotoImage(canvas, photo, pageWidth, pageHeight, paint)

            pdfDoc.finishPage(page)
        }

        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()

        outputFile
    }

    suspend fun exportFolderGridToPdf(
        context: Context,
        folderName: String,
        gridItems: List<FolderGridItem>
    ): File = withContext(Dispatchers.IO) {
        val exportsDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val cleanName = folderName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = File(exportsDir, "${cleanName}_Notes_${System.currentTimeMillis()}.pdf")

        val pdfDoc = PdfDocument()

        val pageWidth = 595 // Standard A4 points at 72dpi
        val pageHeight = 842
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        var totalPages = 0
        for (item in gridItems) {
            when (item) {
                is FolderGridItem.StandalonePhoto -> totalPages += 1
                is FolderGridItem.Group -> {
                    if (item.memberPhotos.isNotEmpty()) {
                        totalPages += 1 + item.memberPhotos.size
                    }
                }
            }
        }
        if (totalPages == 0) totalPages = 1

        var currentPageNumber = 0

        for (item in gridItems) {
            when (item) {
                is FolderGridItem.StandalonePhoto -> {
                    currentPageNumber++
                    val photo = item.photo
                    val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                    val page = pdfDoc.startPage(pageInfo)
                    val canvas: Canvas = page.canvas

                    canvas.drawColor(Color.WHITE)

                    paint.color = Color.DKGRAY
                    paint.textSize = 14f
                    val title = photo.caption ?: "$folderName — Note #$currentPageNumber"
                    canvas.drawText(title, 36f, 40f, paint)

                    paint.textSize = 10f
                    paint.color = Color.GRAY
                    canvas.drawText("Page $currentPageNumber of $totalPages • Fotara Offline Notes", 36f, 56f, paint)

                    drawPhotoImage(canvas, photo, pageWidth, pageHeight, paint)
                    pdfDoc.finishPage(page)
                }
                is FolderGridItem.Group -> {
                    val group = item.group
                    val members = item.memberPhotos
                    if (members.isNotEmpty()) {
                        // Group Section Separator Page
                        currentPageNumber++
                        val sepPageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                        val sepPage = pdfDoc.startPage(sepPageInfo)
                        val sepCanvas = sepPage.canvas

                        sepCanvas.drawColor(Color.parseColor("#F8F9FA"))

                        paint.color = Color.parseColor("#0B0C10")
                        val boxRect = Rect(48, 200, pageWidth - 48, 450)
                        paint.style = Paint.Style.FILL
                        sepCanvas.drawRect(boxRect, paint)

                        paint.color = Color.parseColor("#FFB703")
                        paint.textSize = 12f
                        paint.isFakeBoldText = true
                        sepCanvas.drawText("PHOTO GROUP", 72f, 250f, paint)

                        paint.color = Color.WHITE
                        paint.textSize = 24f
                        paint.isFakeBoldText = true
                        val groupDisplayName = if (group.name.length > 32) group.name.take(32) + "..." else group.name
                        sepCanvas.drawText(groupDisplayName, 72f, 290f, paint)

                        paint.color = Color.LTGRAY
                        paint.textSize = 13f
                        paint.isFakeBoldText = false
                        sepCanvas.drawText("${members.size} member notes bundled", 72f, 325f, paint)

                        paint.color = Color.GRAY
                        paint.textSize = 10f
                        sepCanvas.drawText("Page $currentPageNumber of $totalPages • Fotara Offline Notes", 36f, pageHeight - 36f, paint)

                        pdfDoc.finishPage(sepPage)

                        // Member photos in sequence with section header label
                        for ((mIndex, photo) in members.withIndex()) {
                            currentPageNumber++
                            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                            val page = pdfDoc.startPage(pageInfo)
                            val canvas: Canvas = page.canvas

                            canvas.drawColor(Color.WHITE)

                            paint.color = Color.parseColor("#0B0C10")
                            paint.textSize = 13f
                            paint.isFakeBoldText = true
                            val sectionTitle = "[${group.name}] ${photo.caption ?: "Note ${mIndex + 1}"}"
                            canvas.drawText(sectionTitle, 36f, 40f, paint)

                            paint.isFakeBoldText = false
                            paint.textSize = 10f
                            paint.color = Color.GRAY
                            canvas.drawText("Group Note ${mIndex + 1} of ${members.size} • Page $currentPageNumber of $totalPages • Fotara Offline Notes", 36f, 56f, paint)

                            drawPhotoImage(canvas, photo, pageWidth, pageHeight, paint)
                            pdfDoc.finishPage(page)
                        }
                    }
                }
            }
        }

        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()

        outputFile
    }

    private fun drawPhotoImage(
        canvas: Canvas,
        photo: Photo,
        pageWidth: Int,
        pageHeight: Int,
        paint: Paint
    ) {
        val file = File(photo.fileUri)
        if (file.exists()) {
            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeFile(file.absolutePath, boundsOptions)

            val maxContentW = pageWidth - 72
            val maxContentH = pageHeight - 120

            val sampleSize = calculateInSampleSize(boundsOptions, maxContentW, maxContentH)
            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sampleSize }
            val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)

            if (bitmap != null) {
                val scale = minOf(maxContentW.toFloat() / bitmap.width, maxContentH.toFloat() / bitmap.height)
                val drawW = (bitmap.width * scale).toInt()
                val drawH = (bitmap.height * scale).toInt()

                val left = (pageWidth - drawW) / 2
                val top = 70 + (maxContentH - drawH) / 2

                val destRect = Rect(left, top, left + drawW, top + drawH)
                canvas.drawBitmap(bitmap, null, destRect, paint)
                bitmap.recycle()
            }
        } else {
            paint.color = Color.LTGRAY
            paint.textSize = 12f
            canvas.drawText("Image file not available", 36f, 120f, paint)
        }
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
}
