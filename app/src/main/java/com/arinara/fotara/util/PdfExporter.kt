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
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import com.arinara.fotara.data.db.FotaraDbHelper
import com.arinara.fotara.data.model.Photo
import com.arinara.fotara.data.repository.PhotoDrawingRepository
import com.arinara.fotara.data.repository.SqlitePhotoDrawingRepository
import com.arinara.fotara.data.storage.PhotoFlattener
import com.arinara.fotara.canvas.render.PhotoDrawingRenderer
import com.arinara.fotara.data.model.PdfPageDrawing
import com.arinara.fotara.ui.folder.FolderGridItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object PdfExporter {

    suspend fun exportPhotosToPdf(
        context: Context,
        folderName: String,
        photos: List<Photo>,
        photoDrawingRepository: PhotoDrawingRepository? = null
    ): File = withContext(Dispatchers.IO) {
        val exportsDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val cleanName = folderName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = File(exportsDir, "${cleanName}_Notes_${System.currentTimeMillis()}.pdf")

        val pdfDoc = PdfDocument()
        val resolvedDrawingRepo = photoDrawingRepository ?: SqlitePhotoDrawingRepository(FotaraDbHelper(context))
        val tempFilesToClean = mutableListOf<File>()

        try {
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

                drawPhotoImage(context, canvas, photo, pageWidth, pageHeight, paint, resolvedDrawingRepo, tempFilesToClean)

                pdfDoc.finishPage(page)
            }

            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }

            outputFile
        } finally {
            try {
                pdfDoc.close()
            } catch (_: Throwable) {}
            for (f in tempFilesToClean) {
                if (f.exists()) f.delete()
            }
        }
    }

    suspend fun exportFolderGridToPdf(
        context: Context,
        folderName: String,
        gridItems: List<FolderGridItem>,
        photoDrawingRepository: PhotoDrawingRepository? = null
    ): File = withContext(Dispatchers.IO) {
        val exportsDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val cleanName = folderName.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = File(exportsDir, "${cleanName}_Notes_${System.currentTimeMillis()}.pdf")

        val pdfDoc = PdfDocument()
        val resolvedDrawingRepo = photoDrawingRepository ?: SqlitePhotoDrawingRepository(FotaraDbHelper(context))
        val tempFilesToClean = mutableListOf<File>()

        try {

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
                is FolderGridItem.Document -> {
                    totalPages += item.pages.size.coerceAtLeast(1)
                }
                is FolderGridItem.TextNoteItem -> {
                    // Under MD/TXT only sharing rules, text notes are skipped in PDF export
                }
                is FolderGridItem.CanvasNoteItem -> {
                    // Canvas notes are exported as PNG or skipped in document PDF export
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

                    drawPhotoImage(context, canvas, photo, pageWidth, pageHeight, paint, resolvedDrawingRepo, tempFilesToClean)
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

                            drawPhotoImage(context, canvas, photo, pageWidth, pageHeight, paint, resolvedDrawingRepo, tempFilesToClean)
                            pdfDoc.finishPage(page)
                        }
                    }
                }
                is FolderGridItem.Document -> {
                    for (docPage in item.pages) {
                        currentPageNumber++
                        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                        val page = pdfDoc.startPage(pageInfo)
                        val canvas: Canvas = page.canvas
                        canvas.drawColor(Color.WHITE)

                        paint.color = Color.DKGRAY
                        paint.textSize = 14f
                        canvas.drawText("${item.documentNote.name} — Page ${docPage.pageIndex + 1}", 36f, 40f, paint)

                        paint.textSize = 10f
                        paint.color = Color.GRAY
                        canvas.drawText("Page $currentPageNumber of $totalPages • Fotara Document Notes", 36f, 56f, paint)

                        var bmp: Bitmap? = null
                        try {
                            val availableWidth = (pageWidth - 72).toFloat()
                            val availableHeight = (pageHeight - 120).toFloat()
                            val boundsOptions = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                            BitmapFactory.decodeFile(docPage.imageUri, boundsOptions)
                            val sample = calculateInSampleSize(boundsOptions, availableWidth.toInt(), availableHeight.toInt())
                            val decodeOptions = BitmapFactory.Options().apply { inSampleSize = sample }

                            bmp = BitmapFactory.decodeFile(docPage.imageUri, decodeOptions)
                            if (bmp != null) {
                                val scale = (availableWidth / bmp.width).coerceAtMost(availableHeight / bmp.height)
                                val destWidth = bmp.width * scale
                                val destHeight = bmp.height * scale
                                val left = (pageWidth - destWidth) / 2f
                                val top = 80f + (availableHeight - destHeight) / 2f
                                val destRect = RectF(left, top, left + destWidth, top + destHeight)
                                canvas.drawBitmap(bmp, null, destRect, paint)
                            }
                        } catch (_: Throwable) {
                            paint.color = Color.LTGRAY
                            paint.textSize = 12f
                            canvas.drawText("Page image unavailable", 36f, 120f, paint)
                        } finally {
                            bmp?.recycle()
                        }

                        pdfDoc.finishPage(page)
                    }
                }
                is FolderGridItem.TextNoteItem -> {
                    // Under MD/TXT only sharing rules, text notes are skipped in PDF export
                }
                is FolderGridItem.CanvasNoteItem -> {
                    // Canvas notes are exported as PNG or skipped in document PDF export
                }
            }
        }

        FileOutputStream(outputFile).use { out ->
            pdfDoc.writeTo(out)
        }

        outputFile
        } finally {
            try {
                pdfDoc.close()
            } catch (_: Throwable) {}
            for (f in tempFilesToClean) {
                if (f.exists()) f.delete()
            }
        }
    }

    /**
     * Composites SQLite drawing ink strokes and typed text layers directly onto native PDF page canvases,
     * exporting an annotated PDF file that external readers (Acrobat, Chrome, Apple Preview) can view seamlessly.
     */
    suspend fun exportAnnotatedPdf(
        context: Context,
        documentFile: File,
        documentTitle: String,
        drawingsMap: Map<Int, PdfPageDrawing>
    ): File = withContext(Dispatchers.IO) {
        val exportsDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
        val cleanName = documentTitle.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val outputFile = File(exportsDir, "${cleanName}_Annotated_${System.currentTimeMillis()}.pdf")

        val pdfRenderer = PdfPageRenderer(documentFile)
        val pdfDoc = PdfDocument()

        try {
            val totalPages = pdfRenderer.pageCount
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            for (pageIndex in 0 until totalPages) {
                val (pageWFloat, pageHFloat) = pdfRenderer.getPageSizePoints(pageIndex)
                val w = if (pageWFloat > 0f) pageWFloat.toInt() else 595
                val h = if (pageHFloat > 0f) pageHFloat.toInt() else 842

                val pageInfo = PdfDocument.PageInfo.Builder(w, h, pageIndex + 1).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                // Render high-res base PDF page bitmap (2x points for razor sharpness)
                val renderW = (w * 2f).toInt()
                val renderH = (h * 2f).toInt()
                val baseBitmap = pdfRenderer.renderPageForExport(pageIndex, renderW, renderH)

                if (baseBitmap != null) {
                    val destRect = RectF(0f, 0f, w.toFloat(), h.toFloat())
                    canvas.drawBitmap(baseBitmap, null, destRect, paint)
                    baseBitmap.recycle()
                }

                // If drawings or text layers exist for this page, render them onto the PDF canvas!
                val drawing = drawingsMap[pageIndex]
                if (drawing != null && drawing.isVisible) {
                    val scale = if (drawing.pageWidth > 0f) w.toFloat() / drawing.pageWidth else 1f
                    if (drawing.hasStrokes) {
                        PhotoDrawingRenderer.renderStrokes(canvas, drawing.strokes, scale = scale)
                    }
                    if (drawing.textLayers.isNotEmpty()) {
                        PhotoDrawingRenderer.renderTextLayers(canvas, drawing.textLayers, scale = scale)
                    }
                }

                pdfDoc.finishPage(page)
            }

            FileOutputStream(outputFile).use { out ->
                pdfDoc.writeTo(out)
            }

            outputFile
        } finally {
            try {
                pdfDoc.close()
            } catch (_: Throwable) {}
            try {
                pdfRenderer.close()
            } catch (_: Throwable) {}
        }
    }

    /**
     * Reorganizes an existing PDF file by retaining only pages in [pageOrder],
     * applying rotations in [pageRotations] (0, 90, 180, 270 degrees), and writing
     * back to [targetFile] safely via atomic replacement.
     */
    suspend fun reorganizePdfPages(
        context: Context,
        sourceFile: File,
        pageOrder: List<Int>,
        pageRotations: Map<Int, Int> = emptyMap(),
        targetFile: File = sourceFile
    ): Boolean = withContext(Dispatchers.IO) {
        if (pageOrder.isEmpty()) return@withContext false
        val tempOut = File(context.cacheDir, "reorg_${System.currentTimeMillis()}.pdf")
        val pdfRenderer = PdfPageRenderer(sourceFile)
        val pdfDoc = PdfDocument()

        try {
            val paint = Paint(Paint.ANTI_ALIAS_FLAG)

            for ((newPageIndex, originalPageIndex) in pageOrder.withIndex()) {
                if (originalPageIndex < 0 || originalPageIndex >= pdfRenderer.pageCount) continue
                val (origWFloat, origHFloat) = pdfRenderer.getPageSizePoints(originalPageIndex)
                val origW = if (origWFloat > 0f) origWFloat.toInt() else 595
                val origH = if (origHFloat > 0f) origHFloat.toInt() else 842
                val rotation = (pageRotations[originalPageIndex] ?: 0) % 360
                val isRotated90or270 = rotation == 90 || rotation == 270

                val pageW = if (isRotated90or270) origH else origW
                val pageH = if (isRotated90or270) origW else origH

                val pageInfo = PdfDocument.PageInfo.Builder(pageW, pageH, newPageIndex + 1).create()
                val page = pdfDoc.startPage(pageInfo)
                val canvas: Canvas = page.canvas

                val renderW = (origW * 2f).toInt()
                val renderH = (origH * 2f).toInt()
                val baseBitmap = pdfRenderer.renderPageForExport(
                    originalPageIndex,
                    renderW,
                    renderH
                )

                if (baseBitmap != null) {
                    if (rotation != 0) {
                        canvas.save()
                        when (rotation) {
                            90 -> {
                                canvas.translate(pageW.toFloat(), 0f)
                                canvas.rotate(90f)
                            }
                            180 -> {
                                canvas.translate(pageW.toFloat(), pageH.toFloat())
                                canvas.rotate(180f)
                            }
                            270 -> {
                                canvas.translate(0f, pageH.toFloat())
                                canvas.rotate(270f)
                            }
                        }
                        val destRect = RectF(0f, 0f, origW.toFloat(), origH.toFloat())
                        canvas.drawBitmap(baseBitmap, null, destRect, paint)
                        canvas.restore()
                    } else {
                        val destRect = RectF(0f, 0f, pageW.toFloat(), pageH.toFloat())
                        canvas.drawBitmap(baseBitmap, null, destRect, paint)
                    }
                    baseBitmap.recycle()
                }

                pdfDoc.finishPage(page)
            }

            FileOutputStream(tempOut).use { out ->
                pdfDoc.writeTo(out)
            }

            // Close pdfRenderer before overwriting the file on disk
            pdfDoc.close()
            pdfRenderer.close()

            if (tempOut.exists() && tempOut.length() > 0) {
                tempOut.copyTo(targetFile, overwrite = true)
                tempOut.delete()
                true
            } else {
                false
            }
        } catch (_: Throwable) {
            false
        } finally {
            try { pdfDoc.close() } catch (_: Throwable) {}
            try { pdfRenderer.close() } catch (_: Throwable) {}
            if (tempOut.exists()) tempOut.delete()
        }
    }

    private suspend fun drawPhotoImage(
        context: Context,
        canvas: Canvas,
        photo: Photo,
        pageWidth: Int,
        pageHeight: Int,
        paint: Paint,
        drawingRepository: PhotoDrawingRepository,
        tempFilesToClean: MutableList<File>
    ) {
        val flattenedFile = PhotoFlattener.flattenIfNeeded(context, photo, drawingRepository)
        if (flattenedFile.absolutePath != photo.fileUri) {
            tempFilesToClean.add(flattenedFile)
        }
        val file = flattenedFile
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
