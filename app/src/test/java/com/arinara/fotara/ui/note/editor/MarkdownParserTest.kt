// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownParserTest {

    @Test
    fun testHeadingsParsing() {
        val markdown = """
            # Header One
            ## Header Two
            ### Header Three
        """.trimIndent()

        val doc = MarkdownParser.parse(markdown)
        val h1 = doc.spans.firstOrNull { it.type == MarkdownSpanType.HEADING_1 }
        val h2 = doc.spans.firstOrNull { it.type == MarkdownSpanType.HEADING_2 }
        val h3 = doc.spans.firstOrNull { it.type == MarkdownSpanType.HEADING_3 }

        assertNotNull(h1)
        assertNotNull(h2)
        assertNotNull(h3)

        assertEquals("Header One", markdown.substring(h1!!.contentStart, h1.contentEnd))
        assertEquals("Header Two", markdown.substring(h2!!.contentStart, h2.contentEnd))
        assertEquals("Header Three", markdown.substring(h3!!.contentStart, h3.contentEnd))
    }

    @Test
    fun testInlineSpansParsing() {
        val markdown = "This is **bold**, *italic*, ***both***, ~~struck~~, and `code`."
        val doc = MarkdownParser.parse(markdown)

        val bold = doc.spans.firstOrNull { it.type == MarkdownSpanType.BOLD }
        val italic = doc.spans.firstOrNull { it.type == MarkdownSpanType.ITALIC }
        val both = doc.spans.firstOrNull { it.type == MarkdownSpanType.BOLD_ITALIC }
        val struck = doc.spans.firstOrNull { it.type == MarkdownSpanType.STRIKETHROUGH }
        val code = doc.spans.firstOrNull { it.type == MarkdownSpanType.INLINE_CODE }

        assertNotNull(bold)
        assertNotNull(italic)
        assertNotNull(both)
        assertNotNull(struck)
        assertNotNull(code)

        assertEquals("bold", markdown.substring(bold!!.contentStart, bold.contentEnd))
        assertEquals("italic", markdown.substring(italic!!.contentStart, italic.contentEnd))
        assertEquals("both", markdown.substring(both!!.contentStart, both.contentEnd))
        assertEquals("struck", markdown.substring(struck!!.contentStart, struck.contentEnd))
        assertEquals("code", markdown.substring(code!!.contentStart, code.contentEnd))
    }

    @Test
    fun testChecklistsAndLists() {
        val markdown = """
            - [ ] Unchecked item
            - [x] Checked item
            - Simple bullet
            1. First number
            2. Second number
        """.trimIndent()

        val doc = MarkdownParser.parse(markdown)

        val uncheck = doc.spans.firstOrNull { it.type == MarkdownSpanType.CHECKLIST_UNCHECKED }
        val check = doc.spans.firstOrNull { it.type == MarkdownSpanType.CHECKLIST_CHECKED }
        val bullet = doc.spans.firstOrNull { it.type == MarkdownSpanType.BULLET_LIST }
        val num1 = doc.spans.firstOrNull { it.type == MarkdownSpanType.NUMBERED_LIST }

        assertNotNull(uncheck)
        assertNotNull(check)
        assertNotNull(bullet)
        assertNotNull(num1)

        assertEquals("Unchecked item", markdown.substring(uncheck!!.contentStart, uncheck.contentEnd))
        assertEquals("Checked item", markdown.substring(check!!.contentStart, check.contentEnd))
        assertEquals("Simple bullet", markdown.substring(bullet!!.contentStart, bullet.contentEnd))
    }

    @Test
    fun testLinkParsing() {
        val markdown = "Check out [Fotara](https://fotara.arinara.com) now."
        val doc = MarkdownParser.parse(markdown)

        val link = doc.spans.firstOrNull { it.type == MarkdownSpanType.LINK }
        assertNotNull(link)
        assertEquals("Fotara", markdown.substring(link!!.contentStart, link.contentEnd))
        assertEquals("https://fotara.arinara.com", link.extra)
    }

    @Test
    fun testActiveStateDetectionAtCursor() {
        val markdown = "Hello **world** test"
        val doc = MarkdownParser.parse(markdown)

        // Inside "world" (e.g. index 10)
        val activeInside = doc.activeTypesAt(10, 10)
        assertTrue(MarkdownSpanType.BOLD in activeInside)

        // Outside (e.g. index 2)
        val activeOutside = doc.activeTypesAt(2, 2)
        assertTrue(MarkdownSpanType.BOLD !in activeOutside)
    }

    @Test
    fun testInlineMathParsing() {
        val markdown = "The equation is ${'$'}E = mc^2${'$'} in the text."
        val doc = MarkdownParser.parse(markdown)

        val math = doc.spans.firstOrNull { it.type == MarkdownSpanType.MATH_INLINE }
        assertNotNull(math)
        assertEquals("E = mc^2", markdown.substring(math!!.contentStart, math.contentEnd))
    }

    @Test
    fun testMultiLineMathBlockParsing() {
        val markdown = """
            Here is a system of equations:
            ${'$'}${'$'}
            \begin{cases}
            2x + y = 5 \\
            x - 3y = -1
            \end{cases}
            ${'$'}${'$'}
            And following text.
        """.trimIndent()

        val doc = MarkdownParser.parse(markdown)
        val mathBlock = doc.spans.firstOrNull { it.type == MarkdownSpanType.MATH_BLOCK }

        assertNotNull(mathBlock)
        val content = markdown.substring(mathBlock!!.contentStart, mathBlock.contentEnd)
        assertTrue(content.contains("begin{cases}"))
        assertTrue(content.contains("2x + y = 5"))
    }
}
