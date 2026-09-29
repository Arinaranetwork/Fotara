// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui

import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import com.arinara.fotara.ui.components.buildRichMarkdownAnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RichMarkdownTest {

    @Test
    fun testBoldParsing() {
        val annotated = buildRichMarkdownAnnotatedString("This is **bold** text")
        assertEquals("This is bold text", annotated.text)
        val boldSpans = annotated.spanStyles.filter { it.item.fontWeight == FontWeight.Bold }
        assertEquals(1, boldSpans.size)
        assertEquals(8, boldSpans[0].start)
        assertEquals(12, boldSpans[0].end)
    }

    @Test
    fun testItalicParsing() {
        val annotated = buildRichMarkdownAnnotatedString("This is *italic* text")
        assertEquals("This is italic text", annotated.text)
        val italicSpans = annotated.spanStyles.filter { it.item.fontStyle == FontStyle.Italic }
        assertEquals(1, italicSpans.size)
        assertEquals(8, italicSpans[0].start)
        assertEquals(14, italicSpans[0].end)
    }

    @Test
    fun testInlineCodeParsing() {
        val annotated = buildRichMarkdownAnnotatedString("Run `npm install` now")
        assertEquals("Run  npm install  now", annotated.text)
        val codeSpans = annotated.spanStyles.filter { it.item.fontFamily != null }
        assertEquals(1, codeSpans.size)
    }

    @Test
    fun testStrikethroughParsing() {
        val annotated = buildRichMarkdownAnnotatedString("Price ~~100~~ free")
        assertEquals("Price 100 free", annotated.text)
        val strikeSpans = annotated.spanStyles.filter { it.item.textDecoration == TextDecoration.LineThrough }
        assertEquals(1, strikeSpans.size)
    }

    @Test
    fun testCombinedSpansInOneLine() {
        val input = "- **OTA Verification Build**: Validates in-app package installer *initiation* and `permissions`."
        val annotated = buildRichMarkdownAnnotatedString(input)
        assertTrue(annotated.text.contains("OTA Verification Build"))
        assertTrue(annotated.spanStyles.any { it.item.fontWeight == FontWeight.Bold })
        assertTrue(annotated.spanStyles.any { it.item.fontStyle == FontStyle.Italic })
    }
}
