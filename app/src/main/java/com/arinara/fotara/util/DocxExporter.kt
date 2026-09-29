// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import android.content.Context
import android.graphics.BitmapFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

sealed class DocxContentItem {
    data class ImagePage(val imagePath: String, val caption: String? = null) : DocxContentItem()
    data class TextSection(val title: String, val text: String) : DocxContentItem()
}

class DocxExporter(private val context: Context) {

    suspend fun createDocxDocument(
        outputFile: File,
        documentTitle: String,
        items: List<DocxContentItem>,
        onProgress: ((current: Int, total: Int) -> Unit)? = null
    ): File = withContext(Dispatchers.IO) {
        val total = items.size

        FileOutputStream(outputFile).use { fos ->
            ZipOutputStream(fos).use { zos ->
                // 1. [Content_Types].xml
                writeZipEntry(zos, "[Content_Types].xml", buildContentTypesXml(items))

                // 2. _rels/.rels
                writeZipEntry(zos, "_rels/.rels", buildPackageRelsXml())

                // 3. word/_rels/document.xml.rels
                writeZipEntry(zos, "word/_rels/document.xml.rels", buildDocumentRelsXml(items))

                // 4. word/document.xml
                writeZipEntry(zos, "word/document.xml", buildDocumentXml(documentTitle, items))

                // 5. word/media/imageX.jpg
                var imageCounter = 1
                for (item in items) {
                    if (item is DocxContentItem.ImagePage) {
                        val file = File(item.imagePath)
                        if (file.exists()) {
                            zos.putNextEntry(ZipEntry("word/media/image$imageCounter.jpg"))
                            FileInputStream(file).use { fis ->
                                fis.copyTo(zos)
                            }
                            zos.closeEntry()
                        }
                        imageCounter++
                    }
                    onProgress?.invoke(imageCounter, total)
                }
            }
        }

        outputFile
    }

    private fun writeZipEntry(zos: ZipOutputStream, entryName: String, content: String) {
        zos.putNextEntry(ZipEntry(entryName))
        zos.write(content.toByteArray(Charsets.UTF_8))
        zos.closeEntry()
    }

    private fun buildContentTypesXml(items: List<DocxContentItem>): String {
        val hasImages = items.any { it is DocxContentItem.ImagePage }
        val imageExtension = if (hasImages) """<Default Extension="jpg" ContentType="image/jpeg"/>""" else ""
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
    <Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
    <Default Extension="xml" ContentType="application/xml"/>
    $imageExtension
    <Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>
</Types>""".trimIndent()
    }

    private fun buildPackageRelsXml(): String {
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
    <Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>
</Relationships>""".trimIndent()
    }

    private fun buildDocumentRelsXml(items: List<DocxContentItem>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
""")
        var imageCounter = 1
        for (item in items) {
            if (item is DocxContentItem.ImagePage) {
                sb.append("""    <Relationship Id="rIdImg$imageCounter" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/image" Target="media/image$imageCounter.jpg"/>
""")
                imageCounter++
            }
        }
        sb.append("</Relationships>")
        return sb.toString()
    }

    private fun buildDocumentXml(title: String, items: List<DocxContentItem>): String {
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main"
            xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships"
            xmlns:wp="http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing"
            xmlns:a="http://schemas.openxmlformats.org/drawingml/2006/main"
            xmlns:pic="http://schemas.openxmlformats.org/drawingml/2006/picture">
    <w:body>
""")

        // Title Paragraph
        val escapedTitle = escapeXml(title)
        sb.append("""
        <w:p>
            <w:pPr>
                <w:jc w:val="center"/>
            </w:pPr>
            <w:r>
                <w:rPr>
                    <w:b/>
                    <w:sz w:val="36"/>
                    <w:szCs w:val="36"/>
                </w:rPr>
                <w:t>$escapedTitle</w:t>
            </w:r>
        </w:p>
""")

        var imageCounter = 1
        for (item in items) {
            when (item) {
                is DocxContentItem.ImagePage -> {
                    // Embed image in drawing
                    val file = File(item.imagePath)
                    val (cx, cy) = getImageDimensionsEmu(file)
                    val rId = "rIdImg$imageCounter"

                    if (item.caption != null) {
                        sb.append("""
        <w:p>
            <w:r>
                <w:rPr><w:i/></w:rPr>
                <w:t>${escapeXml(item.caption)}</w:t>
            </w:r>
        </w:p>
""")
                    }

                    sb.append("""
        <w:p>
            <w:pPr>
                <w:jc w:val="center"/>
            </w:pPr>
            <w:r>
                <w:drawing>
                    <wp:inline distT="0" distB="0" distL="0" distR="0">
                        <wp:extent cx="$cx" cy="$cy"/>
                        <wp:docPr id="$imageCounter" name="Image $imageCounter"/>
                        <a:graphic>
                            <a:graphicData uri="http://schemas.openxmlformats.org/drawingml/2006/picture">
                                <pic:pic>
                                    <pic:nvPicPr>
                                        <pic:cNvPr id="$imageCounter" name="image$imageCounter.jpg"/>
                                        <pic:cNvPicPr/>
                                    </pic:nvPicPr>
                                    <pic:blipFill>
                                        <a:blip r:embed="$rId"/>
                                        <a:stretch><a:fillRect/></a:stretch>
                                    </pic:blipFill>
                                    <pic:spPr>
                                        <a:xfrm>
                                            <a:off x="0" y="0"/>
                                            <a:ext cx="$cx" cy="$cy"/>
                                        </a:xfrm>
                                        <a:prstGeom prst="rect"><a:avLst/></a:prstGeom>
                                    </pic:spPr>
                                </pic:pic>
                            </a:graphicData>
                        </a:graphic>
                    </wp:inline>
                </w:drawing>
            </w:r>
        </w:p>
""")
                    // Page break between items
                    sb.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>\n")
                    imageCounter++
                }
                is DocxContentItem.TextSection -> {
                    val secTitle = escapeXml(item.title)
                    sb.append("""
        <w:p>
            <w:r>
                <w:rPr><w:b/><w:sz w:val="28"/></w:rPr>
                <w:t>$secTitle</w:t>
            </w:r>
        </w:p>
""")
                    val paragraphs = item.text.split("\n")
                    for (para in paragraphs) {
                        val trimmed = escapeXml(para.trim())
                        if (trimmed.isNotBlank()) {
                            sb.append("""
        <w:p>
            <w:r>
                <w:t>$trimmed</w:t>
            </w:r>
        </w:p>
""")
                        }
                    }
                    sb.append("<w:p><w:r><w:br w:type=\"page\"/></w:r></w:p>\n")
                }
            }
        }

        sb.append("""
        <w:sectPr>
            <w:pgSz w:w="12240" w:h="15840"/>
            <w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440"/>
        </w:sectPr>
    </w:body>
</w:document>
""")
        return sb.toString()
    }

    private fun getImageDimensionsEmu(file: File): Pair<Long, Long> {
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, options)
        val imgWidth = options.outWidth.coerceAtLeast(1)
        val imgHeight = options.outHeight.coerceAtLeast(1)

        // Max page width: ~5.5 inches in EMU (1 inch = 914400 EMU) -> max ~5000000 EMU
        val maxEmuWidth = 5029200L
        val maxEmuHeight = 6500000L

        val aspectRatio = imgWidth.toDouble() / imgHeight.toDouble()
        var emuWidth = maxEmuWidth
        var emuHeight = (emuWidth / aspectRatio).toLong()

        if (emuHeight > maxEmuHeight) {
            emuHeight = maxEmuHeight
            emuWidth = (emuHeight * aspectRatio).toLong()
        }

        return emuWidth to emuHeight
    }

    private fun escapeXml(text: String): String {
        return text.replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
            .replace("'", "&apos;")
    }
}
