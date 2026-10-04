// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.util

import com.arinara.fotara.data.model.DocumentPage
import java.text.Normalizer

object PdfPageMatcher {

    /**
     * Normalizes text for case-insensitive and diacritic-insensitive search matching.
     */
    fun normalizeForSearch(input: String): String {
        val nfd = Normalizer.normalize(input, Normalizer.Form.NFD)
        return nfd.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "").lowercase()
    }

    /**
     * Finds the target zero-based page index for a search query.
     * Priority:
     * 1. First page whose OCR text contains ALL query terms.
     * 2. First page whose OCR text contains ANY query term.
     * 3. Defaults to 0 (page 1) if OCR text is missing or no terms match.
     */
    fun findMatchingPageIndex(pages: List<DocumentPage>, query: String?): Int {
        if (query.isNullOrBlank() || pages.isEmpty()) return 0

        val normalizedQuery = normalizeForSearch(query.trim())
        val terms = normalizedQuery.split("\\s+".toRegex()).filter { it.isNotBlank() }
        if (terms.isEmpty()) return 0

        val normalizedPages = pages.sortedBy { it.pageIndex }.map { page ->
            page to (page.ocrText?.let { normalizeForSearch(it) } ?: "")
        }

        // 1. First page containing ALL terms
        val allMatch = normalizedPages.firstOrNull { (_, text) ->
            text.isNotBlank() && terms.all { text.contains(it) }
        }
        if (allMatch != null) return allMatch.first.pageIndex

        // 2. First page containing ANY term
        val anyMatch = normalizedPages.firstOrNull { (_, text) ->
            text.isNotBlank() && terms.any { text.contains(it) }
        }
        if (anyMatch != null) return anyMatch.first.pageIndex

        return 0
    }
}
