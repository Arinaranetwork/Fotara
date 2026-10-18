// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.note

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.arinara.fotara.ui.note.editor.EditorActions
import com.arinara.fotara.ui.note.editor.EditorChangeHandler
import com.arinara.fotara.ui.note.editor.TextEditorOps
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EditorChangeHandlerTest {

    // ==========================================
    // S1. Checkbox Tests (Offset, Indent, Toggle)
    // ==========================================

    @Test
    fun testCheckboxToggle_indentedChecklist() {
        // Toggling an indented checklist item "  - [ ] Task" at offset 2 (start of checkbox)
        val initial = TextFieldValue("  - [ ] Task", TextRange(10))
        val toggled = EditorActions.toggleChecklistAtOffset(initial, 2)
        assertEquals("  - [x] Task", toggled.text)
    }

    @Test
    fun testCheckboxToggle_atLineStart_charOffsetZero() {
        // Line start (charOffset = 0) of first line "- [ ] Task"
        val initial = TextFieldValue("- [ ] Task", TextRange(0))
        val toggled = EditorActions.toggleChecklistAtOffset(initial, 0)
        assertEquals("- [x] Task", toggled.text)
    }

    // ==========================================
    // S2. Indent and Outdent Bounding & States
    // ==========================================

    @Test
    fun testIndent_increasesTwoSpacesUpToLevelThree() {
        var value = TextFieldValue("- Item", TextRange(6))
        // Level 0 -> Level 1 (2 spaces)
        value = EditorActions.indent(value)
        assertEquals("  - Item", value.text)

        // Level 1 -> Level 2 (4 spaces)
        value = EditorActions.indent(value)
        assertEquals("    - Item", value.text)

        // Level 2 -> Level 3 (6 spaces)
        value = EditorActions.indent(value)
        assertEquals("      - Item", value.text)

        // Level 3 cap: Should NOT indent further
        val capped = EditorActions.indent(value)
        assertEquals("      - Item", capped.text)
    }

    @Test
    fun testOutdent_decreasesTwoSpacesDownToZero() {
        var value = TextFieldValue("      - Item", TextRange(12))
        // Level 3 -> 2
        value = EditorActions.outdent(value)
        assertEquals("    - Item", value.text)

        // Level 2 -> 1
        value = EditorActions.outdent(value)
        assertEquals("  - Item", value.text)

        // Level 1 -> 0
        value = EditorActions.outdent(value)
        assertEquals("- Item", value.text)

        // Level 0: Outdent does not strip the marker or negative indent
        val atZero = EditorActions.outdent(value)
        assertEquals("- Item", atZero.text)
    }

    // ==========================================
    // S3. Keyboard Enter Handling (IME & Hardware)
    // ==========================================

    @Test
    fun testEnter_onItemWithText_appendsNewItemWithoutDeletingText() {
        val oldBody = TextFieldValue("- Buy milk", TextRange(10))
        val newBody = TextFieldValue("- Buy milk\n", TextRange(11))

        val result = EditorChangeHandler.processChange(oldBody, newBody)
        assertEquals("- Buy milk\n- ", result.text)
        assertEquals(13, result.selection.start)
        assertEquals(13, result.selection.end)
    }

    @Test
    fun testEnter_inMiddleOfItemText_splitsItem() {
        val oldBody = TextFieldValue("- Hello World", TextRange(7)) // Caret between "Hello" and " World"
        val newBody = TextFieldValue("- Hello\n World", TextRange(8))

        val result = EditorChangeHandler.processChange(oldBody, newBody)
        assertEquals("- Hello\n-  World", result.text)
        assertEquals(10, result.selection.start)
    }

    @Test
    fun testEnter_onEmptyItem_exitsListToPlainParagraph() {
        val oldBody = TextFieldValue("- ", TextRange(2))
        val newBody = TextFieldValue("- \n", TextRange(3))

        val result = EditorChangeHandler.processChange(oldBody, newBody)
        assertEquals("", result.text)
        assertEquals(0, result.selection.start)
    }

    @Test
    fun testEnter_withIMEComposingRegion_preservesItemAndInsertsNewMarker() {
        // IME committing composing region while inserting newline
        val oldBody = TextFieldValue("- Item", TextRange(6), composition = TextRange(2, 6))
        val newBody = TextFieldValue("- Item\n", TextRange(7), composition = null)

        val result = EditorChangeHandler.processChange(oldBody, newBody)
        assertEquals("- Item\n- ", result.text)
        assertEquals(9, result.selection.start)
    }

    // ==========================================
    // S4. Deleting App-Drawn Blocks
    // ==========================================

    @Test
    fun testBackspace_rightAfterMarker_removesOnlyMarker() {
        val oldBody = TextFieldValue("- Content", TextRange(2))
        // IME deletes space before cursor
        val newBody = TextFieldValue("-Content", TextRange(1))

        val result = EditorChangeHandler.processChange(oldBody, newBody)
        assertEquals("Content", result.text)
        assertEquals(0, result.selection.start)
    }

    @Test
    fun testBackspace_rightAfterCheckboxMarker_removesOnlyMarker() {
        val oldBody = TextFieldValue("- [ ] Content", TextRange(6))
        val newBody = TextFieldValue("- [ ]Content", TextRange(5))

        val result = EditorChangeHandler.processChange(oldBody, newBody)
        assertEquals("Content", result.text)
        assertEquals(0, result.selection.start)
    }

    @Test
    fun testBackspace_indentedList_outdentsFirst() {
        val oldBody = TextFieldValue("  - Content", TextRange(4))
        val newBody = TextFieldValue("  -Content", TextRange(3))

        val result = EditorChangeHandler.processChange(oldBody, newBody)
        assertEquals("- Content", result.text)
        assertEquals(2, result.selection.start)
    }

    @Test
    fun testBackspace_fromLineBelowDivider_removesDividerLine() {
        // Text with divider:
        // Line 1\n---\nLine 2
        // Caret at start of "Line 2" (index 11)
        val oldBody = TextFieldValue("Line 1\n---\nLine 2", TextRange(11))
        // Backspace deletes the newline preceding "Line 2":
        val newBody = TextFieldValue("Line 1\n---Line 2", TextRange(10))

        val result = EditorChangeHandler.processChange(oldBody, newBody)
        assertEquals("Line 1\nLine 2", result.text)
        assertEquals(7, result.selection.start)
    }

    @Test
    fun testBackspace_directlyOnDivider_removesDividerLine() {
        val oldBody = TextFieldValue("Line 1\n---\nLine 2", TextRange(8))
        val newBody = TextFieldValue("Line 1\n--\nLine 2", TextRange(7))

        val result = EditorChangeHandler.processChange(oldBody, newBody)
        assertEquals("Line 1\nLine 2", result.text)
        assertEquals(6, result.selection.start)
    }

    // ==========================================
    // Additional Target Behavior Tests
    // ==========================================

    @Test
    fun testToolAppliedOnEmptyLine_thenEnter_exitsList() {
        // Tool applied to empty note produces "- [ ] "
        val oldBody = TextFieldValue("- [ ] ", TextRange(6))
        // User presses Enter
        val newBody = TextFieldValue("- [ ] \n", TextRange(7))

        val result = EditorChangeHandler.processChange(oldBody, newBody)
        assertEquals("", result.text)
        assertEquals(0, result.selection.start)
    }

    @Test
    fun testTypingAfterCheckboxTool_endsOutsideAnyBrackets() {
        // User applies checkbox tool
        val withTool = EditorActions.toggleChecklist(TextFieldValue("", TextRange(0)))
        assertEquals("- [ ] ", withTool.text)
        assertEquals(6, withTool.selection.start)

        // Typing "Do homework"
        val typed = TextFieldValue("- [ ] Do homework", TextRange(17))
        val result = EditorChangeHandler.processChange(withTool, typed)
        assertEquals("- [ ] Do homework", result.text)
        // Cursor is strictly outside the brackets
        assertTrue(result.selection.start >= 6)
    }

    @Test
    fun testSpaceOnEmptyItem_neverDeletesMarker() {
        val oldBody = TextFieldValue("- ", TextRange(2))
        val newBody = TextFieldValue("-  ", TextRange(3))

        val result = EditorChangeHandler.processChange(oldBody, newBody)
        assertEquals("-  ", result.text)
    }

    @Test
    fun testSpaceInsideItem_neverDeletesOrConvertsMarker() {
        val oldBody = TextFieldValue("- Some", TextRange(6))
        val newBody = TextFieldValue("- Some ", TextRange(7))

        val result = EditorChangeHandler.processChange(oldBody, newBody)
        assertEquals("- Some ", result.text)
    }

    @Test
    fun testMultiLinePaste_preservesLinesWithoutEnterLogic() {
        val oldBody = TextFieldValue("- First", TextRange(7))
        val pasteContent = "Line 1\nLine 2\nLine 3"
        val newBody = TextFieldValue("- First$pasteContent", TextRange(7 + pasteContent.length))

        val result = EditorChangeHandler.processChange(oldBody, newBody)
        assertEquals("- FirstLine 1\nLine 2\nLine 3", result.text)
    }

    @Test
    fun testIndentAndOutdent_multiLineSelection() {
        val text = "- Item 1\n- Item 2\n- Item 3"
        // Select all
        var value = TextFieldValue(text, TextRange(0, text.length))

        // Indent all lines
        value = EditorActions.indent(value)
        assertEquals("  - Item 1\n  - Item 2\n  - Item 3", value.text)

        // Indent again to level 2
        value = EditorActions.indent(value)
        assertEquals("    - Item 1\n    - Item 2\n    - Item 3", value.text)

        // Outdent back to level 1
        value = EditorActions.outdent(value)
        assertEquals("  - Item 1\n  - Item 2\n  - Item 3", value.text)

        // Outdent back to level 0
        value = EditorActions.outdent(value)
        assertEquals("- Item 1\n- Item 2\n- Item 3", value.text)
    }

    @Test
    fun testCanIndentAndCanOutdent_onlyEnabledForListItems() {
        // Plain text: both false
        assertFalse(TextEditorOps.canIndent("Plain paragraph", 0, 5))
        assertFalse(TextEditorOps.canOutdent("Plain paragraph", 0, 5))

        // List item at level 0: canIndent = true, canOutdent = false
        assertTrue(TextEditorOps.canIndent("- List item", 0, 5))
        assertFalse(TextEditorOps.canOutdent("- List item", 0, 5))

        // List item at level 1: canIndent = true, canOutdent = true
        assertTrue(TextEditorOps.canIndent("  - List item", 0, 5))
        assertTrue(TextEditorOps.canOutdent("  - List item", 0, 5))

        // List item at level 3 (6 spaces): canIndent = false, canOutdent = true
        assertFalse(TextEditorOps.canIndent("      - List item", 0, 5))
        assertTrue(TextEditorOps.canOutdent("      - List item", 0, 5))
    }

    @Test
    fun testNumberedList_autoRenumberingAcrossInsertsAndDeletes() {
        val initial = "1. First\n2. Second\n3. Third"
        val withInsert = EditorActions.toggleNumberedList(TextFieldValue(initial, TextRange(8))) // on line 1
        // Toggle removes number on second line -> subsequent line renumbers
        val (renumbered, _) = TextEditorOps.renumberNumberedLists("1. First\nSecond\n3. Third", 0)
        assertEquals("1. First\nSecond\n1. Third", renumbered)
    }
}
