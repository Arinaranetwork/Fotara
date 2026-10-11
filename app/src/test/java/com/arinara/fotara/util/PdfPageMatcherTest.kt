// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import com.arinara.fotara.data.model.DocumentPage
import org.junit.Assert.assertEquals
import org.junit.Test

class PdfPageMatcherTest {

    private fun createPage(index: Int, text: String?): DocumentPage {
        return DocumentPage(
            id = index.toLong(),
            documentNoteId = 1L,
            pageIndex = index,
            imageUri = "uri_$index",
            ocrText = text
        )
    }

    @Test
    fun testEmptyOrBlankQuery_ReturnsPageZero() {
        val pages = listOf(
            createPage(0, "Introduction to Machine Learning"),
            createPage(1, "Deep Neural Networks")
        )
        assertEquals(0, PdfPageMatcher.findMatchingPageIndex(pages, ""))
        assertEquals(0, PdfPageMatcher.findMatchingPageIndex(pages, "   "))
    }

    @Test
    fun testSingleWordMatch_ReturnsMatchingPage() {
        val pages = listOf(
            createPage(0, "Introduction and syllabus"),
            createPage(1, "Calculus and derivatives"),
            createPage(2, "Thermodynamics and entropy")
        )
        assertEquals(1, PdfPageMatcher.findMatchingPageIndex(pages, "derivatives"))
        assertEquals(2, PdfPageMatcher.findMatchingPageIndex(pages, "entropy"))
    }

    @Test
    fun testCaseInsensitiveAndPunctuationMatching() {
        val pages = listOf(
            createPage(0, "Chapter 1: THERMODYNAMICS; fundamental laws.")
        )
        assertEquals(0, PdfPageMatcher.findMatchingPageIndex(pages, "thermodynamics"))
        assertEquals(0, PdfPageMatcher.findMatchingPageIndex(pages, "Thermodynamics,"))
    }

    @Test
    fun testMultiWordMatch_PrefersPageWithAllTerms() {
        val pages = listOf(
            createPage(0, "Machine data processing and computation"),
            createPage(1, "Deep neural networks and cognitive learning"),
            createPage(2, "Modern Machine Learning architectures")
        )
        // Query has both "machine" and "learning"
        // Page 0 has only "machine", Page 1 has only "learning", Page 2 has BOTH "machine" AND "learning"
        assertEquals(2, PdfPageMatcher.findMatchingPageIndex(pages, "machine learning"))
    }

    @Test
    fun testMultiWordMatch_FallsBackToAnyTerm() {
        val pages = listOf(
            createPage(0, "Physics and mechanics"),
            createPage(1, "Advanced Quantum dynamics"),
            createPage(2, "Chemistry laboratory notes")
        )
        // Query has "quantum" and "biology". No page has both.
        // Page 1 has "quantum", so it matches any term!
        assertEquals(1, PdfPageMatcher.findMatchingPageIndex(pages, "quantum biology"))
    }

    @Test
    fun testNoMatch_ReturnsPageZero() {
        val pages = listOf(
            createPage(0, "World history notes"),
            createPage(1, "European geography")
        )
        assertEquals(0, PdfPageMatcher.findMatchingPageIndex(pages, "astronomy astrophysics"))
    }

    @Test
    fun testMissingOcrText_SafelyReturnsPageZero() {
        val pages = listOf(
            createPage(0, null),
            createPage(1, ""),
            createPage(2, "   ")
        )
        assertEquals(0, PdfPageMatcher.findMatchingPageIndex(pages, "calculus"))
    }

    @Test
    fun testEmptyPageList_ReturnsPageZero() {
        assertEquals(0, PdfPageMatcher.findMatchingPageIndex(emptyList(), "test"))
    }
}
