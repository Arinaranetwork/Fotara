// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.note

import com.arinara.fotara.ui.note.editor.LineToolType
import com.arinara.fotara.ui.note.editor.MarkdownOffsetMapping
import com.arinara.fotara.ui.note.editor.TextEditorOps
import com.arinara.fotara.ui.note.editor.TextMappingChunk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TextEditorOpsTest {

    // ==========================================
    // 1. INLINE TOOLS: Bold, Italic, Strike, Code
    // ==========================================

    @Test
    fun testBold_withSelection_wrapsSelection() {
        val result = TextEditorOps.toggleBold("Hello world", 6, 11)
        assertEquals("Hello **world**", result.text)
        assertEquals(8, result.selectionStart)
        assertEquals(13, result.selectionEnd)
    }

    @Test
    fun testBold_withSelection_unwrapsWhenAlreadyWrapped() {
        val result = TextEditorOps.toggleBold("Hello **world**", 6, 15)
        assertEquals("Hello world", result.text)
        assertEquals(6, result.selectionStart)
        assertEquals(11, result.selectionEnd)
    }

    @Test
    fun testBold_emptyNote_insertsMarkerAndPutsCaretInside() {
        val result = TextEditorOps.toggleBold("", 0, 0)
        assertEquals("****", result.text)
        assertEquals(2, result.selectionStart)
        assertEquals(2, result.selectionEnd)
    }

    @Test
    fun testBold_collapsedCaret_onWord_wrapsWord() {
        val result = TextEditorOps.toggleBold("Hello world", 2, 2)
        assertEquals("**Hello** world", result.text)
        assertEquals(4, result.selectionStart)
        assertEquals(4, result.selectionEnd)
    }

    @Test
    fun testBold_collapsedCaret_betweenMarkers_unwraps() {
        val result = TextEditorOps.toggleBold("Hello **** world", 8, 8)
        assertEquals("Hello  world", result.text)
        assertEquals(6, result.selectionStart)
        assertEquals(6, result.selectionEnd)
    }

    @Test
    fun testItalic_withSelection_wrapsAndUnwraps() {
        val wrapped = TextEditorOps.toggleItalic("Note text", 5, 9)
        assertEquals("Note *text*", wrapped.text)
        assertEquals(6, wrapped.selectionStart)
        assertEquals(10, wrapped.selectionEnd)

        val unwrapped = TextEditorOps.toggleItalic(wrapped.text, 5, 11)
        assertEquals("Note text", unwrapped.text)
        assertEquals(5, unwrapped.selectionStart)
        assertEquals(9, unwrapped.selectionEnd)
    }

    @Test
    fun testStrikethrough_emptyNote_insertsPair() {
        val result = TextEditorOps.toggleStrikethrough("", 0, 0)
        assertEquals("~~~~", result.text)
        assertEquals(2, result.selectionStart)
        assertEquals(2, result.selectionEnd)
    }

    @Test
    fun testInlineCode_withSelection_wraps() {
        val result = TextEditorOps.toggleInlineCode("val x = 42", 4, 10)
        assertEquals("val `x = 42`", result.text)
        assertEquals(5, result.selectionStart)
        assertEquals(11, result.selectionEnd)
    }

    // ==========================================
    // 2. LINE TOOLS: Headings, Quote, Lists
    // ==========================================

    @Test
    fun testQuote_emptyNote_placesCaretStrictlyAfterMarker() {
        // Bug a: caret must end up AFTER "> " (result: "> |"), NEVER before ">"
        val result = TextEditorOps.applyQuote("", 0, 0)
        assertEquals("> ", result.text)
        assertEquals(2, result.selectionStart)
        assertEquals(2, result.selectionEnd)
    }

    @Test
    fun testQuote_atLineStart_placesCaretAfterMarker() {
        val result = TextEditorOps.applyQuote("This is a quote", 0, 0)
        assertEquals("> This is a quote", result.text)
        assertEquals(2, result.selectionStart)
        assertEquals(2, result.selectionEnd)
    }

    @Test
    fun testQuote_insideLine_shiftsCaretByPrefix() {
        val result = TextEditorOps.applyQuote("This is a quote", 4, 4)
        assertEquals("> This is a quote", result.text)
        assertEquals(6, result.selectionStart)
        assertEquals(6, result.selectionEnd)
    }

    @Test
    fun testQuote_toggleOff_whenAlreadyQuote() {
        val result = TextEditorOps.applyQuote("> This is a quote", 2, 2)
        assertEquals("This is a quote", result.text)
        assertEquals(0, result.selectionStart)
        assertEquals(0, result.selectionEnd)
    }

    @Test
    fun testHeadings_H1_H2_H3_toggleAndReplace() {
        // H1 on plain text
        val h1 = TextEditorOps.applyHeading("Title", 0, 0, 1)
        assertEquals("# Title", h1.text)
        assertEquals(2, h1.selectionStart)

        // Switch H1 to H2 replaces prefix instead of stacking
        val h2 = TextEditorOps.applyHeading(h1.text, 2, 2, 2)
        assertEquals("## Title", h2.text)
        assertEquals(3, h2.selectionStart)

        // Switch H2 to H3 replaces prefix
        val h3 = TextEditorOps.applyHeading(h2.text, 3, 3, 3)
        assertEquals("### Title", h3.text)
        assertEquals(4, h3.selectionStart)

        // Toggling H3 again toggles off to plain text
        val off = TextEditorOps.applyHeading(h3.text, 4, 4, 3)
        assertEquals("Title", off.text)
        assertEquals(0, off.selectionStart)
    }

    @Test
    fun testBulletList_multiLine_andToggleOff() {
        val input = "Apple\nBanana\nCherry"
        val list = TextEditorOps.applyBulletList(input, 0, input.length)
        assertEquals("- Apple\n- Banana\n- Cherry", list.text)

        // Toggle off restores plain lines
        val off = TextEditorOps.applyBulletList(list.text, 0, list.text.length)
        assertEquals("Apple\nBanana\nCherry", off.text)
    }

    @Test
    fun testNumberedList_autoSequencesNumbers() {
        val input = "First\nSecond\nThird"
        val list = TextEditorOps.applyNumberedList(input, 0, input.length)
        assertEquals("1. First\n2. Second\n3. Third", list.text)
    }

    @Test
    fun testCheckbox_insertsRealCheckboxPrefix() {
        val result = TextEditorOps.applyCheckbox("Buy milk", 0, 0)
        assertEquals("- [ ] Buy milk", result.text)
        assertEquals(6, result.selectionStart)
        assertEquals(6, result.selectionEnd)

        // Toggle off
        val off = TextEditorOps.applyCheckbox(result.text, 6, 6)
        assertEquals("Buy milk", off.text)
        assertEquals(0, off.selectionStart)
    }

    @Test
    fun testLineTool_switchingPrefix_replacesOldPrefixWithoutStacking() {
        // Line has "- Bullet"
        val bullet = "- Bullet item"
        // Applying quote replaces "- " with "> "
        val quote = TextEditorOps.applyQuote(bullet, 2, 2)
        assertEquals("> Bullet item", quote.text)
        assertEquals(2, quote.selectionStart)

        // Applying checkbox replaces "> " with "- [ ] "
        val check = TextEditorOps.applyCheckbox(quote.text, 2, 2)
        assertEquals("- [ ] Bullet item", check.text)
        assertEquals(6, check.selectionStart)
    }

    // ==========================================
    // 3. INDENT AND OUTDENT
    // ==========================================

    @Test
    fun testIndentAndOutdent() {
        val input = "- [ ] Task 1\n- [ ] Task 2"
        val indented = TextEditorOps.indent(input, 0, input.length)
        assertEquals("  - [ ] Task 1\n  - [ ] Task 2", indented.text)

        val outdented = TextEditorOps.outdent(indented.text, 0, indented.text.length)
        assertEquals("- [ ] Task 1\n- [ ] Task 2", outdented.text)
    }

    // ==========================================
    // 4. ENTER KEY LIST CONTINUATION & EXIT
    // ==========================================

    @Test
    fun testEnterKey_continuesBulletList() {
        val input = "- First item"
        val result = TextEditorOps.handleEnterKey(input, input.length, input.length)
        assertNotNull(result)
        assertEquals("- First item\n- ", result!!.text)
        assertEquals("- First item\n- ".length, result.selectionStart)
    }

    @Test
    fun testEnterKey_exitsBulletListOnEmptyItem() {
        val input = "- "
        val result = TextEditorOps.handleEnterKey(input, 2, 2)
        assertNotNull(result)
        assertEquals("", result!!.text)
        assertEquals(0, result.selectionStart)
    }

    @Test
    fun testEnterKey_continuesNumberedList() {
        val input = "1. Item"
        val result = TextEditorOps.handleEnterKey(input, input.length, input.length)
        assertNotNull(result)
        assertEquals("1. Item\n2. ", result!!.text)
    }

    @Test
    fun testEnterKey_continuesChecklist() {
        val input = "- [ ] Task"
        val result = TextEditorOps.handleEnterKey(input, input.length, input.length)
        assertNotNull(result)
        assertEquals("- [ ] Task\n- [ ] ", result!!.text)
    }

    @Test
    fun testEnterKey_continuesQuote() {
        val input = "> Wisdom"
        val result = TextEditorOps.handleEnterKey(input, input.length, input.length)
        assertNotNull(result)
        assertEquals("> Wisdom\n> ", result!!.text)
    }

    // ==========================================
    // 5. BACKSPACE SINGLE-STROKE UNIT DELETION
    // ==========================================

    @Test
    fun testBackspace_removesEntirePrefixInOneStroke() {
        // Right after "> "
        val quote = "> "
        val resultQuote = TextEditorOps.handleBackspaceKey(quote, 2, 2)
        assertNotNull(resultQuote)
        assertEquals("", resultQuote!!.text)

        // Right after "- [ ] "
        val check = "- [ ] "
        val resultCheck = TextEditorOps.handleBackspaceKey(check, 6, 6)
        assertNotNull(resultCheck)
        assertEquals("", resultCheck!!.text)
    }

    // ==========================================
    // 6. CODE BLOCK & HORIZONTAL RULE & LINK
    // ==========================================

    @Test
    fun testCodeBlock_emptyNote_insertsFencedBlockWithCaretInside() {
        val result = TextEditorOps.toggleCodeBlock("", 0, 0)
        assertEquals("```\n\n```", result.text)
        assertEquals(4, result.selectionStart)
        assertEquals(4, result.selectionEnd)
    }

    @Test
    fun testHorizontalRule_insertsOnDedicatedLine() {
        val result = TextEditorOps.insertHorizontalRule("Paragraph 1", 11, 11)
        assertEquals("Paragraph 1\n---\n", result.text)
    }

    @Test
    fun testInsertLink_formatsCorrectMarkdown() {
        val result = TextEditorOps.insertOrEditLink("Click here", 0, 10, "Fotara", "https://fotara.app")
        assertEquals("[Fotara](https://fotara.app)", result.text)
    }

    @Test
    fun testToggleChecklistAtOffset_togglesUncheckedToCheckedAndBack() {
        val text = "- [ ] Exercise"
        val checked = TextEditorOps.toggleChecklistAtOffset(text, 0, 0, 2)
        assertEquals("- [x] Exercise", checked.text)

        val unchecked = TextEditorOps.toggleChecklistAtOffset(checked.text, 0, 0, 2)
        assertEquals("- [ ] Exercise", unchecked.text)
    }

    @Test
    fun testToggleChecklistAtLine_togglesUncheckedToCheckedAndBack() {
        val text = "# Header\n- [ ] Task 1\n- [x] Task 2"
        val checked = TextEditorOps.toggleChecklistAtLine(text, 0, 0, 1)
        assertEquals("# Header\n- [x] Task 1\n- [x] Task 2", checked.text)

        val unchecked = TextEditorOps.toggleChecklistAtLine(checked.text, 0, 0, 2)
        assertEquals("# Header\n- [x] Task 1\n- [ ] Task 2", unchecked.text)
    }

    // ==========================================
    // 7. ACTIVE TOOLBAR STATE DETECTION
    // ==========================================

    @Test
    fun testActiveToolbarState_detectsContextCorrectly() {
        val text = "# Heading 1\n\n> Quote line\n\n- [ ] Checklist item\n\nSome **bold** text"

        // Cursor on line 1 (# Heading 1)
        val state1 = TextEditorOps.getActiveToolbarStates(text, 2, 2)
        assertTrue(state1.isH1)
        assertFalse(state1.isQuote)
        assertFalse(state1.isBold)

        // Cursor on line 3 (> Quote line)
        val quoteOffset = text.indexOf("> Quote") + 2
        val state2 = TextEditorOps.getActiveToolbarStates(text, quoteOffset, quoteOffset)
        assertTrue(state2.isQuote)
        assertFalse(state2.isH1)

        // Cursor on bold word
        val boldOffset = text.indexOf("bold") + 1
        val state3 = TextEditorOps.getActiveToolbarStates(text, boldOffset, boldOffset)
        assertTrue(state3.isBold)
    }

    // ==========================================
    // 8. REGRESSION TESTS (Screenshot S1 - S4)
    // ==========================================

    @Test
    fun testRegression_checkboxInserted_caretEndsStrictlyOutsideBrackets() {
        // S1 Regression: Caret must end strictly after the brackets at offset 6, never inside "[ | ]"
        val result = TextEditorOps.applyCheckbox("", 0, 0)
        assertEquals("- [ ] ", result.text)
        assertEquals(6, result.selectionStart)
        assertEquals(6, result.selectionEnd)

        // Typing text writes OUTSIDE brackets, resulting in valid markdown checklist
        val typed = result.text + "teyss"
        assertEquals("- [ ] teyss", typed)
        val parsed = TextEditorOps.parseLine(0, 0, typed.length, typed)
        assertEquals(LineToolType.CHECKBOX, parsed.prefixType)
        assertEquals("teyss", parsed.content)
    }

    @Test
    fun testRegression_enterOnCheckboxWithText_createsNewCheckboxBelow() {
        // S2 Regression: Enter on an item WITH text creates a new unchecked item below
        val input = "- [ ] First item"
        val result = TextEditorOps.handleEnterKey(input, input.length, input.length)
        assertNotNull(result)
        assertEquals("- [ ] First item\n- [ ] ", result!!.text)
        assertEquals("- [ ] First item\n- [ ] ".length, result.selectionStart)
    }

    @Test
    fun testRegression_enterOnCheckedCheckboxWithText_createsUncheckedItem() {
        val input = "- [x] Done item"
        val result = TextEditorOps.handleEnterKey(input, input.length, input.length)
        assertNotNull(result)
        assertEquals("- [x] Done item\n- [ ] ", result!!.text)
    }

    @Test
    fun testRegression_enterOnEmptyCheckbox_exitsListToPlainParagraph() {
        // S2 Regression: Enter on an EMPTY item exits the list
        val input = "- [ ] "
        val result = TextEditorOps.handleEnterKey(input, input.length, input.length)
        assertNotNull(result)
        assertEquals("", result!!.text)
        assertEquals(0, result.selectionStart)
    }

    @Test
    fun testRegression_spaceNeverRemovesOrConvertsExistingPrefix() {
        // S2 Regression: Space on an existing checklist never removes or alters prefix
        val input = "- [ ] Item"
        val shortcut = TextEditorOps.handleTypingShortcut(input, 6, 6)
        assertEquals(null, shortcut)
    }

    // ==========================================
    // 9. KEYBOARD TABLE PURE-FUNCTION TESTS
    // ==========================================

    @Test
    fun testEnter_bulletWithText_createsNewBulletBelow() {
        val input = "- Milk"
        val result = TextEditorOps.handleEnterKey(input, input.length, input.length)
        assertNotNull(result)
        assertEquals("- Milk\n- ", result!!.text)
        assertEquals("- Milk\n- ".length, result.selectionStart)
    }

    @Test
    fun testEnter_emptyBullet_exitsList() {
        val input = "- "
        val result = TextEditorOps.handleEnterKey(input, input.length, input.length)
        assertNotNull(result)
        assertEquals("", result!!.text)
    }

    @Test
    fun testEnter_quoteWithText_createsNewQuoteBelow() {
        val input = "> Inspiring quote"
        val result = TextEditorOps.handleEnterKey(input, input.length, input.length)
        assertNotNull(result)
        assertEquals("> Inspiring quote\n> ", result!!.text)
    }

    @Test
    fun testEnter_emptyQuote_exitsQuote() {
        val input = "> "
        val result = TextEditorOps.handleEnterKey(input, input.length, input.length)
        assertNotNull(result)
        assertEquals("", result!!.text)
    }

    @Test
    fun testEnter_numberedList_autoIncrements() {
        val input = "1. Item one"
        val result = TextEditorOps.handleEnterKey(input, input.length, input.length)
        assertNotNull(result)
        assertEquals("1. Item one\n2. ", result!!.text)
    }

    @Test
    fun testEnter_emptyNumbered_exitsList() {
        val input = "1. "
        val result = TextEditorOps.handleEnterKey(input, input.length, input.length)
        assertNotNull(result)
        assertEquals("", result!!.text)
    }

    @Test
    fun testEnter_middleOfText_splitsItemCleanly() {
        val input = "- [ ] Buy milk today"
        val splitOffset = "- [ ] Buy ".length
        val result = TextEditorOps.handleEnterKey(input, splitOffset, splitOffset)
        assertNotNull(result)
        assertEquals("- [ ] Buy \n- [ ] milk today", result!!.text)
        assertEquals("- [ ] Buy \n- [ ] ".length, result.selectionStart)
    }

    @Test
    fun testBackspace_rightAfterPrefix_removesOnlyPrefix() {
        val input = "- [ ] Buy groceries"
        val result = TextEditorOps.handleBackspaceKey(input, 6, 6)
        assertNotNull(result)
        assertEquals("Buy groceries", result!!.text)
        assertEquals(0, result.selectionStart)
    }

    @Test
    fun testBackspace_indentedPrefix_outdentsFirst() {
        val input = "  - [ ] Nested task"
        val result = TextEditorOps.handleBackspaceKey(input, 8, 8)
        assertNotNull(result)
        assertEquals("- [ ] Nested task", result!!.text)
        assertEquals(6, result.selectionStart)
    }

    @Test
    fun testTypingShortcuts_atLineStart() {
        val bullet = TextEditorOps.handleTypingShortcut("- ", 0, 2)
        assertNotNull(bullet)
        assertEquals("- ", bullet!!.text)

        val starBullet = TextEditorOps.handleTypingShortcut("* ", 0, 2)
        assertNotNull(starBullet)
        assertEquals("- ", starBullet!!.text)

        val checkbox = TextEditorOps.handleTypingShortcut("[] ", 0, 3)
        assertNotNull(checkbox)
        assertEquals("- [ ] ", checkbox!!.text)

        val num = TextEditorOps.handleTypingShortcut("1. ", 0, 3)
        assertNotNull(num)
        assertEquals("1. ", num!!.text)

        val h1 = TextEditorOps.handleTypingShortcut("# ", 0, 2)
        assertNotNull(h1)
        assertEquals("# ", h1!!.text)

        val h2 = TextEditorOps.handleTypingShortcut("## ", 0, 3)
        assertNotNull(h2)
        assertEquals("## ", h2!!.text)

        val h3 = TextEditorOps.handleTypingShortcut("### ", 0, 4)
        assertNotNull(h3)
        assertEquals("### ", h3!!.text)

        val quote = TextEditorOps.handleTypingShortcut("> ", 0, 2)
        assertNotNull(quote)
        assertEquals("> ", quote!!.text)
    }

    @Test
    fun testNumberedList_autoRenumbering_contiguousRun() {
        val input = "1. First\n1. Second\n1. Third"
        val (renumbered, _) = TextEditorOps.renumberNumberedLists(input)
        assertEquals("1. First\n2. Second\n3. Third", renumbered)
    }

    @Test
    fun testNumberedList_autoRenumbering_restartsAfterBreak() {
        val input = "1. Item 1\n2. Item 2\n\n1. Item 1\n1. Item 2"
        val (renumbered, _) = TextEditorOps.renumberNumberedLists(input)
        assertEquals("1. Item 1\n2. Item 2\n\n1. Item 1\n2. Item 2", renumbered)
    }

    @Test
    fun testIndent_boundedToMaxThreeLevels() {
        val line = "- Item"
        val ind1 = TextEditorOps.indent(line, 0, line.length)
        assertEquals("  - Item", ind1.text)

        val ind2 = TextEditorOps.indent(ind1.text, 0, ind1.text.length)
        assertEquals("    - Item", ind2.text)

        val ind3 = TextEditorOps.indent(ind2.text, 0, ind2.text.length)
        assertEquals("      - Item", ind3.text)

        // 4th indent should remain capped at 3 levels (6 spaces)
        val ind4 = TextEditorOps.indent(ind3.text, 0, ind3.text.length)
        assertEquals("      - Item", ind4.text)
    }

    @Test
    fun testAtomicOffsetMapping_neverPutsCaretInsideBrackets() {
        // Transformed "[ ] " (6 chars) -> "☐ " (2 chars) with isAtomicPrefix = true
        val chunk = TextMappingChunk(0, 6, 0, 2, isAtomicPrefix = true)
        val mapping = MarkdownOffsetMapping(6, 2, listOf(chunk))

        // Any click on transformed index 1 (the checkbox box) maps to 6 (after the brackets)
        assertEquals(6, mapping.transformedToOriginal(1))
        // Click at index 0 maps to 0
        assertEquals(0, mapping.transformedToOriginal(0))
        // Click at index 2 maps to 6
        assertEquals(6, mapping.transformedToOriginal(2))

        // transformedToOriginal NEVER returns 1, 2, 3, 4, 5
        for (t in 1..2) {
            val o = mapping.transformedToOriginal(t)
            assertTrue("Original offset must not be inside brackets: $o", o == 6)
        }
    }

    @Test
    fun testToggleInlineMath_wrapsAndUnwraps() {
        val original = "E = mc^2"
        val wrapped = TextEditorOps.toggleInlineMath(original, 0, original.length)
        assertEquals("\$E = mc^2\$", wrapped.text)

        val unwrapped = TextEditorOps.toggleInlineMath(wrapped.text, 0, wrapped.text.length)
        assertEquals("E = mc^2", unwrapped.text)
    }

    @Test
    fun testInsertBlockMath_wrapsSelection() {
        val equation = "\\int_0^\\infty e^{-x} dx = 1"
        val res = TextEditorOps.insertBlockMath(equation, 0, equation.length)
        assertEquals("$$\n\\int_0^\\infty e^{-x} dx = 1\n$$", res.text)
    }

    @Test
    fun testInsertTable_generatesCorrectStructure() {
        val empty = ""
        val res = TextEditorOps.insertTable(empty, 0, 0, rows = 2, cols = 3)
        assertTrue(res.text.contains("| Header 1 | Header 2 | Header 3 |"))
        assertTrue(res.text.contains("| --- | --- | --- |"))
        assertTrue(res.text.contains("| Cell | Cell | Cell |"))
        assertTrue(TextEditorOps.isInsideTable(res.text, res.selectionStart, res.selectionEnd))
    }

    @Test
    fun testTableOperations_addAndRemoveRows() {
        val initial = TextEditorOps.insertTable("", 0, 0, rows = 1, cols = 2).text
        val offsetInside = initial.indexOf("Cell")
        assertTrue("Cursor must be in table", TextEditorOps.isInsideTable(initial, offsetInside, offsetInside))

        // Add row below
        val withExtraRow = TextEditorOps.addTableRowBelow(initial, offsetInside, offsetInside)
        val lineCountBefore = initial.lines().count { it.trim().startsWith("|") }
        val lineCountAfter = withExtraRow.text.lines().count { it.trim().startsWith("|") }
        assertEquals(lineCountBefore + 1, lineCountAfter)

        // Delete row
        val afterDelete = TextEditorOps.deleteCurrentTableRow(withExtraRow.text, withExtraRow.selectionStart, withExtraRow.selectionStart)
        val lineCountDeleted = afterDelete.text.lines().count { it.trim().startsWith("|") }
        assertEquals(lineCountBefore, lineCountDeleted)
    }

    @Test
    fun testTableOperations_addAndRemoveColumns() {
        val initial = TextEditorOps.insertTable("", 0, 0, rows = 1, cols = 2).text
        val offsetInside = initial.indexOf("Header 1")

        // Add column right
        val withExtraCol = TextEditorOps.addTableColumnRight(initial, offsetInside, offsetInside)
        val headerLine = withExtraCol.text.lines().first { it.trim().startsWith("|") }
        val pipes = headerLine.count { it == '|' }
        assertEquals(4, pipes) // 3 cols -> 4 pipes: | Col 1 | Col 2 | Col 3 |

        // Delete column
        val afterDel = TextEditorOps.deleteCurrentTableColumn(withExtraCol.text, offsetInside, offsetInside)
        val headerAfterDel = afterDel.text.lines().first { it.trim().startsWith("|") }
        assertEquals(3, headerAfterDel.count { it == '|' }) // Back to 2 cols: | Col 1 | Col 2 |
    }
}
