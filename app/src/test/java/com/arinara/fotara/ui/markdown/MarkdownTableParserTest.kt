// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.markdown

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTableParserTest {

    @Test
    fun parse_headerOnlyTable() {
        val markdown = """
            | Header 1 | Header 2 |
            | --- | --- |
        """.trimIndent()

        val blocks = MarkdownTableParser.parseReleaseNotes(markdown)
        assertEquals(1, blocks.size)
        assertTrue(blocks[0] is ReleaseNotesBlock.Table)
        val table = (blocks[0] as ReleaseNotesBlock.Table).table
        assertEquals(listOf("Header 1", "Header 2"), table.headers)
        assertEquals(0, table.rows.size)
        assertEquals(listOf(TableColumnAlignment.START, TableColumnAlignment.START), table.alignments)
    }

    @Test
    fun parse_withAlignments() {
        val markdown = """
            | Left | Center | Right |
            | :--- | :---: | ---: |
            | A | B | C |
        """.trimIndent()

        val blocks = MarkdownTableParser.parseReleaseNotes(markdown)
        assertEquals(1, blocks.size)
        val table = (blocks[0] as ReleaseNotesBlock.Table).table
        assertEquals(listOf(TableColumnAlignment.START, TableColumnAlignment.CENTER, TableColumnAlignment.END), table.alignments)
        assertEquals(1, table.rows.size)
        assertEquals(listOf("A", "B", "C"), table.rows[0])
    }

    @Test
    fun parse_escapedPipes() {
        val markdown = """
            | Item | Syntax |
            | --- | --- |
            | Pipe | a \| b |
        """.trimIndent()

        val blocks = MarkdownTableParser.parseReleaseNotes(markdown)
        assertEquals(1, blocks.size)
        val table = (blocks[0] as ReleaseNotesBlock.Table).table
        assertEquals(listOf("Pipe", "a | b"), table.rows[0])
    }

    @Test
    fun parse_noLeadingOrTrailingPipes() {
        val markdown = """
            Col 1 | Col 2
            --- | ---
            val 1 | val 2
        """.trimIndent()

        val blocks = MarkdownTableParser.parseReleaseNotes(markdown)
        assertEquals(1, blocks.size)
        val table = (blocks[0] as ReleaseNotesBlock.Table).table
        assertEquals(listOf("Col 1", "Col 2"), table.headers)
        assertEquals(listOf("val 1", "val 2"), table.rows[0])
    }

    @Test
    fun parse_raggedRows_padsEmptyAndTrimsExtra() {
        val markdown = """
            | A | B | C |
            | --- | --- | --- |
            | Only A |
            | A2 | B2 | C2 | D2 (extra) |
        """.trimIndent()

        val blocks = MarkdownTableParser.parseReleaseNotes(markdown)
        assertEquals(1, blocks.size)
        val table = (blocks[0] as ReleaseNotesBlock.Table).table
        assertEquals(3, table.headers.size)
        assertEquals(2, table.rows.size)
        assertEquals(listOf("Only A", "", ""), table.rows[0])
        assertEquals(listOf("A2", "B2", "C2"), table.rows[1])
    }

    @Test
    fun parse_tableDirectlyAfterParagraphWithoutBlankLine_treatedAsParagraphs() {
        val markdown = """
            This is a normal paragraph.
            | Col 1 | Col 2 |
            | --- | --- |
            | Data 1 | Data 2 |
        """.trimIndent()

        val blocks = MarkdownTableParser.parseReleaseNotes(markdown)
        // Must NOT parse as a table because it lacks a separating blank line
        assertTrue(blocks.none { it is ReleaseNotesBlock.Table })
        assertEquals(4, blocks.size)
    }

    @Test
    fun parse_twoTablesInOneText() {
        val markdown = """
            | T1 Col 1 | T1 Col 2 |
            | --- | --- |
            | Val 1 | Val 2 |

            Some middle paragraph text.

            | T2 A | T2 B |
            | :--- | ---: |
            | 10 | 20 |
        """.trimIndent()

        val blocks = MarkdownTableParser.parseReleaseNotes(markdown)
        assertEquals(3, blocks.size)
        assertTrue(blocks[0] is ReleaseNotesBlock.Table)
        assertTrue(blocks[1] is ReleaseNotesBlock.Paragraph)
        assertTrue(blocks[2] is ReleaseNotesBlock.Table)

        val t1 = (blocks[0] as ReleaseNotesBlock.Table).table
        val t2 = (blocks[2] as ReleaseNotesBlock.Table).table
        assertEquals(listOf("T1 Col 1", "T1 Col 2"), t1.headers)
        assertEquals(listOf("T2 A", "T2 B"), t2.headers)
        assertEquals(listOf(TableColumnAlignment.START, TableColumnAlignment.END), t2.alignments)
    }

    @Test
    fun parse_tableInsideListItem_treatedAsNormalText() {
        val markdown = """
            - | List Col 1 | List Col 2 |
            | --- | --- |
        """.trimIndent()

        val blocks = MarkdownTableParser.parseReleaseNotes(markdown)
        assertTrue(blocks[0] is ReleaseNotesBlock.BulletItem)
    }

    @Test
    fun parse_emptyCell() {
        val markdown = """
            | Head 1 | Head 2 |
            | --- | --- |
            | | Value 2 |
        """.trimIndent()

        val blocks = MarkdownTableParser.parseReleaseNotes(markdown)
        val table = (blocks[0] as ReleaseNotesBlock.Table).table
        assertEquals(listOf("", "Value 2"), table.rows[0])
    }

    @Test
    fun parse_unclosedFormattingInCell() {
        val markdown = """
            | Syntax | Notes |
            | --- | --- |
            | **unclosed bold | *unclosed italic |
        """.trimIndent()

        val blocks = MarkdownTableParser.parseReleaseNotes(markdown)
        val table = (blocks[0] as ReleaseNotesBlock.Table).table
        assertEquals(listOf("**unclosed bold", "*unclosed italic"), table.rows[0])
    }
}
