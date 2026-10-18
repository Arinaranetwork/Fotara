// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import android.content.Context
import android.graphics.Color
import android.util.Xml
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream

sealed interface DocxElement {
    data class Heading(val level: Int, val text: String) : DocxElement
    data class Paragraph(val text: AnnotatedString) : DocxElement
    data class ListItem(val number: String?, val text: AnnotatedString) : DocxElement
    data class Table(val rows: List<List<String>>) : DocxElement
    data class Image(val localFilePath: String) : DocxElement
    data class Unsupported(val description: String) : DocxElement
}

object DocxParser {

    suspend fun parseDocx(context: Context, docxFile: File): List<DocxElement> = withContext(Dispatchers.IO) {
        if (!docxFile.exists() || docxFile.length() == 0L) {
            throw PdfCorruptException("DOCX file is empty or does not exist.")
        }

        val mediaMap = mutableMapOf<String, String>() // rId -> local media file path
        var documentXmlBytes: ByteArray? = null
        val tempMediaDir = File(context.cacheDir, "docx_media_${System.currentTimeMillis()}").apply { mkdirs() }

        try {
            docxFile.inputStream().use { fileInput ->
                val zip = ZipInputStream(fileInput)
                var entry = zip.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name == "word/document.xml" -> {
                            val buffer = ByteArrayOutputStream()
                            zip.copyTo(buffer)
                            documentXmlBytes = buffer.toByteArray()
                        }
                        name == "word/_rels/document.xml.rels" -> {
                            parseRels(zip, mediaMap, tempMediaDir)
                        }
                        name.startsWith("word/media/") -> {
                            val mediaName = File(name).name
                            val dest = File(tempMediaDir, mediaName)
                            FileOutputStream(dest).use { out ->
                                zip.copyTo(out)
                            }
                        }
                    }
                    entry = zip.nextEntry
                }
            }
        } catch (e: Exception) {
            throw PdfCorruptException("Failed to read Word document archive: ${e.message}")
        }

        val xmlBytes = documentXmlBytes
            ?: throw PdfCorruptException("Invalid Word document: missing word/document.xml")

        parseDocumentXml(xmlBytes, mediaMap)
    }

    private fun parseRels(
        zip: ZipInputStream,
        mediaMap: MutableMap<String, String>,
        tempMediaDir: File
    ) {
        try {
            val parser = Xml.newPullParser()
            parser.setInput(zip, "UTF-8")
            var event = parser.eventType
            while (event != XmlPullParser.END_DOCUMENT) {
                if (event == XmlPullParser.START_TAG && parser.name == "Relationship") {
                    val id = parser.getAttributeValue(null, "Id")
                    val target = parser.getAttributeValue(null, "Target")
                    if (id != null && target != null && target.contains("media/")) {
                        val fileName = File(target).name
                        val localFile = File(tempMediaDir, fileName)
                        mediaMap[id] = localFile.absolutePath
                    }
                }
                event = parser.next()
            }
        } catch (_: Exception) {}
    }

    private fun parseDocumentXml(
        xmlBytes: ByteArray,
        mediaMap: Map<String, String>
    ): List<DocxElement> {
        val elements = mutableListOf<DocxElement>()
        val parser = Xml.newPullParser()
        parser.setInput(ByteArrayInputStream(xmlBytes), "UTF-8")

        var eventType = parser.eventType

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name?.substringAfter(':')
            if (eventType == XmlPullParser.START_TAG) {
                when (tagName) {
                    "p" -> {
                        val pElement = parseParagraph(parser, mediaMap)
                        if (pElement != null) elements.add(pElement)
                    }
                    "tbl" -> {
                        val table = parseTable(parser)
                        if (table.rows.isNotEmpty()) elements.add(table)
                    }
                }
            }
            eventType = parser.next()
        }

        return elements
    }

    private fun parseParagraph(
        parser: XmlPullParser,
        mediaMap: Map<String, String>
    ): DocxElement? {
        var headingLevel: Int? = null
        var isListItem = false
        val builder = AnnotatedString.Builder()
        var embeddedImage: DocxElement.Image? = null
        var unsupportedElement: DocxElement.Unsupported? = null

        // Parse within this <w:p> tag until matching </w:p>
        var eventType = parser.next()
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name?.substringAfter(':')
            if (eventType == XmlPullParser.START_TAG) {
                when (tagName) {
                    "pStyle" -> {
                        val styleVal = parser.getAttributeValue(null, "val") ?: ""
                        if (styleVal.startsWith("Heading1", ignoreCase = true) || styleVal.startsWith("Title", ignoreCase = true)) {
                            headingLevel = 1
                        } else if (styleVal.startsWith("Heading2", ignoreCase = true)) {
                            headingLevel = 2
                        } else if (styleVal.startsWith("Heading3", ignoreCase = true)) {
                            headingLevel = 3
                        }
                    }
                    "numPr" -> {
                        isListItem = true
                    }
                    "r" -> {
                        val runResult = parseRun(parser, mediaMap)
                        if (runResult is RunContent.TextRun) {
                            builder.append(runResult.annotatedString)
                        } else if (runResult is RunContent.ImageRun) {
                            embeddedImage = DocxElement.Image(runResult.filePath)
                        } else if (runResult is RunContent.UnsupportedRun) {
                            unsupportedElement = DocxElement.Unsupported(runResult.desc)
                        }
                    }
                    "drawing" -> {
                        // Drawing outside or wrapping run
                        val rId = findBlipEmbed(parser)
                        if (rId != null && mediaMap.containsKey(rId)) {
                            embeddedImage = DocxElement.Image(mediaMap[rId]!!)
                        } else {
                            unsupportedElement = DocxElement.Unsupported("Drawing or diagram object")
                        }
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG && tagName == "p") {
                break
            }
            eventType = parser.next()
        }

        if (embeddedImage != null) return embeddedImage
        if (unsupportedElement != null && builder.length == 0) return unsupportedElement

        val resultText = builder.toAnnotatedString()
        if (resultText.isBlank() && unsupportedElement == null) return null

        return when {
            headingLevel != null -> DocxElement.Heading(headingLevel, resultText.text)
            isListItem -> DocxElement.ListItem(null, resultText)
            else -> DocxElement.Paragraph(resultText)
        }
    }

    private sealed interface RunContent {
        data class TextRun(val annotatedString: AnnotatedString) : RunContent
        data class ImageRun(val filePath: String) : RunContent
        data class UnsupportedRun(val desc: String) : RunContent
    }

    private fun parseRun(
        parser: XmlPullParser,
        mediaMap: Map<String, String>
    ): RunContent? {
        var isBold = false
        var isItalic = false
        var isUnderline = false
        var isStrike = false
        var colorHex: String? = null
        var fontSizeSp: Float? = null
        val runTextBuilder = StringBuilder()
        var embeddedImage: RunContent.ImageRun? = null
        var unsupported: RunContent.UnsupportedRun? = null

        var eventType = parser.next()
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name?.substringAfter(':')
            if (eventType == XmlPullParser.START_TAG) {
                when (tagName) {
                    "b" -> isBold = true
                    "i" -> isItalic = true
                    "u" -> isUnderline = true
                    "strike" -> isStrike = true
                    "color" -> colorHex = parser.getAttributeValue(null, "val")
                    "sz" -> {
                        val halfPoints = parser.getAttributeValue(null, "val")?.toFloatOrNull()
                        if (halfPoints != null) fontSizeSp = halfPoints / 2f
                    }
                    "t" -> {
                        try {
                            val text = parser.nextText()
                            runTextBuilder.append(text)
                        } catch (_: Exception) {}
                    }
                    "drawing" -> {
                        val rId = findBlipEmbed(parser)
                        if (rId != null && mediaMap.containsKey(rId)) {
                            embeddedImage = RunContent.ImageRun(mediaMap[rId]!!)
                        } else {
                            unsupported = RunContent.UnsupportedRun("Embedded Drawing / Equation")
                        }
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG && tagName == "r") {
                break
            }
            eventType = parser.next()
        }

        if (embeddedImage != null) return embeddedImage
        if (unsupported != null) return unsupported

        val rawText = runTextBuilder.toString()
        if (rawText.isEmpty()) return null

        val spanStyle = SpanStyle(
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontStyle = if (isItalic) FontStyle.Italic else FontStyle.Normal,
            textDecoration = when {
                isUnderline && isStrike -> TextDecoration.combine(listOf(TextDecoration.Underline, TextDecoration.LineThrough))
                isUnderline -> TextDecoration.Underline
                isStrike -> TextDecoration.LineThrough
                else -> TextDecoration.None
            },
            fontSize = fontSizeSp?.sp ?: 15.sp,
            color = if (colorHex != null) {
                try { androidx.compose.ui.graphics.Color(Color.parseColor("#$colorHex")) } catch (_: Exception) { androidx.compose.ui.graphics.Color.Unspecified }
            } else {
                androidx.compose.ui.graphics.Color.Unspecified
            }
        )

        return RunContent.TextRun(
            buildAnnotatedString {
                pushStyle(spanStyle)
                append(rawText)
                pop()
            }
        )
    }

    private fun findBlipEmbed(parser: XmlPullParser): String? {
        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name?.substringAfter(':')
            if (eventType == XmlPullParser.START_TAG && tagName == "blip") {
                return parser.getAttributeValue(null, "embed")
                    ?: parser.getAttributeValue("http://schemas.openxmlformats.org/officeDocument/2006/relationships", "embed")
            } else if (eventType == XmlPullParser.END_TAG && tagName == "drawing") {
                break
            }
            eventType = parser.next()
        }
        return null
    }

    private fun parseTable(parser: XmlPullParser): DocxElement.Table {
        val rows = mutableListOf<List<String>>()
        var currentRow = mutableListOf<String>()
        val currentCellBuilder = StringBuilder()

        var eventType = parser.next()
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name?.substringAfter(':')
            if (eventType == XmlPullParser.START_TAG) {
                when (tagName) {
                    "tr" -> currentRow = mutableListOf()
                    "tc" -> currentCellBuilder.clear()
                    "t" -> {
                        try {
                            currentCellBuilder.append(parser.nextText())
                        } catch (_: Exception) {}
                    }
                }
            } else if (eventType == XmlPullParser.END_TAG) {
                when (tagName) {
                    "tc" -> currentRow.add(currentCellBuilder.toString().trim())
                    "tr" -> if (currentRow.isNotEmpty()) rows.add(currentRow.toList())
                    "tbl" -> break
                }
            }
            eventType = parser.next()
        }
        return DocxElement.Table(rows)
    }
}
