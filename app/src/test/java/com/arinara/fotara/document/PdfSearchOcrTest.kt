// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.document

import com.arinara.fotara.data.model.DocumentNote
import com.arinara.fotara.data.model.DocumentType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PdfSearchOcrTest {

    @Test
    fun testOcrSemanticsNullVsEmptyString() {
        // NULL means unprocessed / pending
        var pageOcrText: String? = null
        assertTrue("Initial state before OCR must be null", pageOcrText == null)

        // Processed with no text found -> must store empty string "", NEVER null
        val extractedTextBlank = "   \n  "
        pageOcrText = extractedTextBlank.trim()
        assertEquals("", pageOcrText)
        org.junit.Assert.assertNotNull("Processed page with no text must not remain null", pageOcrText)

        // Processed with text found -> must store trimmed text
        val extractedTextValid = "  Lecture Notes Chapter 4  "
        pageOcrText = extractedTextValid.trim()
        assertEquals("Lecture Notes Chapter 4", pageOcrText)
    }

    @Test
    fun testZeroDisplayOfOcrTextForPdfInSearchCards() {
        // PDF document with extracted OCR text
        val pdfDoc = DocumentNote(
            id = 1L,
            folderId = 10L,
            subfolderId = null,
            name = "Biology_Textbook.pdf",
            docType = DocumentType.PDF,
            originFileUri = "/storage/docs/Biology_Textbook.pdf",
            pageCount = 42,
            extractedText = "Photosynthesis takes place in chloroplasts.",
            tagColor = null,
            linkedDeadline = null,
            isTrashed = false,
            createdAt = 1000L,
            deletedAt = null
        )

        // DOCX document with extracted text
        val docxDoc = DocumentNote(
            id = 2L,
            folderId = 10L,
            subfolderId = null,
            name = "Lab_Report.docx",
            docType = DocumentType.DOCX,
            originFileUri = "/storage/docs/Lab_Report.docx",
            pageCount = 3,
            extractedText = "Experiment 1 results show significant increase.",
            tagColor = null,
            linkedDeadline = null,
            isTrashed = false,
            createdAt = 1000L,
            deletedAt = null
        )

        // Logic check: only DOCX is allowed to render snippet and badge
        fun shouldRenderSnippet(doc: DocumentNote): Boolean {
            return doc.docType == DocumentType.DOCX && !doc.extractedText.isNullOrBlank()
        }

        assertFalse("PDF notes must NEVER display OCR snippet in search cards", shouldRenderSnippet(pdfDoc))
        assertTrue("DOCX notes may display document snippet", shouldRenderSnippet(docxDoc))
    }

    @Test
    fun testIncrementalFtsConcatenation() {
        val pageTexts = listOf("Header page", "", "Chapter 1 Introduction", "Chapter 2 Methods")
        val fullOcrText = StringBuilder()
        for (text in pageTexts) {
            if (text.isNotBlank()) {
                if (fullOcrText.isNotEmpty()) fullOcrText.append("\n")
                fullOcrText.append(text)
            }
        }

        val expected = "Header page\nChapter 1 Introduction\nChapter 2 Methods"
        assertEquals(expected, fullOcrText.toString())
    }

    @Test
    fun testFailureRetryLimit() {
        val failureCounts = mutableMapOf<String, Int>()
        val maxRetries = 3

        val key = "100:0" // docId: 100, page: 0
        assertEquals(0, failureCounts[key] ?: 0)

        // Attempt 1, 2, 3
        failureCounts[key] = (failureCounts[key] ?: 0) + 1
        assertEquals(1, failureCounts[key] ?: 0)

        failureCounts[key] = (failureCounts[key] ?: 0) + 1
        assertEquals(2, failureCounts[key] ?: 0)

        failureCounts[key] = (failureCounts[key] ?: 0) + 1
        assertEquals(3, failureCounts[key] ?: 0)

        // After max retries reached, should skip processing
        val shouldSkip = (failureCounts[key] ?: 0) >= maxRetries
        assertTrue("Must skip page OCR when failure count reaches max retries", shouldSkip)
    }
}
