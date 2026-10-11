// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class EditorActionsTest {

    @Test
    fun testToggleBold_WrapSelection() {
        val input = TextFieldValue("hello world", selection = TextRange(0, 5))
        val result = EditorActions.toggleBold(input)
        assertEquals("**hello** world", result.text)
        assertEquals(TextRange(2, 7), result.selection)
    }

    @Test
    fun testToggleBold_UnwrapSelectionInternal() {
        val input = TextFieldValue("**hello** world", selection = TextRange(0, 9))
        val result = EditorActions.toggleBold(input)
        assertEquals("hello world", result.text)
        assertEquals(TextRange(0, 5), result.selection)
    }

    @Test
    fun testToggleBold_UnwrapSelectionExternal() {
        val input = TextFieldValue("**hello** world", selection = TextRange(2, 7))
        val result = EditorActions.toggleBold(input)
        assertEquals("hello world", result.text)
        assertEquals(TextRange(0, 5), result.selection)
    }

    @Test
    fun testToggleBold_CollapsedCursorOnWord() {
        // Cursor on 'l' in "world" (index 8)
        val input = TextFieldValue("hello world", selection = TextRange(8, 8))
        val result = EditorActions.toggleBold(input)
        assertEquals("hello **world**", result.text)
    }

    @Test
    fun testToggleBold_CollapsedCursorEmpty() {
        val input = TextFieldValue("hello ", selection = TextRange(6, 6))
        val result = EditorActions.toggleBold(input)
        assertEquals("hello ****", result.text)
        assertEquals(TextRange(8, 8), result.selection)
    }

    @Test
    fun testHeadings_ReplaceWithoutStacking() {
        // Apply H1
        val input = TextFieldValue("Title line", selection = TextRange(0, 0))
        val h1 = EditorActions.toggleHeading(input, 1)
        assertEquals("# Title line", h1.text)

        // Apply H2 on H1 line: replaces H1 with H2 instead of stacking "# ##"
        val h2 = EditorActions.toggleHeading(h1, 2)
        assertEquals("## Title line", h2.text)

        // Apply H2 again: toggles off to plain line
        val off = EditorActions.toggleHeading(h2, 2)
        assertEquals("Title line", off.text)
    }

    @Test
    fun testBlockquote_Toggle() {
        val input = TextFieldValue("Quote line", selection = TextRange(0, 0))
        val quoted = EditorActions.toggleBlockquote(input)
        assertEquals("> Quote line", quoted.text)

        val unquoted = EditorActions.toggleBlockquote(quoted)
        assertEquals("Quote line", unquoted.text)
    }

    @Test
    fun testBulletList_Toggle() {
        val input = TextFieldValue("Item line", selection = TextRange(0, 0))
        val bulleted = EditorActions.toggleBulletList(input)
        assertEquals("- Item line", bulleted.text)

        val unbulleted = EditorActions.toggleBulletList(bulleted)
        assertEquals("Item line", unbulleted.text)
    }

    @Test
    fun testNumberedList_ConsecutiveRenumbering() {
        val input = TextFieldValue("First\nSecond\nThird", selection = TextRange(0, 17))
        val numbered = EditorActions.toggleNumberedList(input)
        assertEquals("1. First\n2. Second\n3. Third", numbered.text)

        val unnumbered = EditorActions.toggleNumberedList(numbered)
        assertEquals("First\nSecond\nThird", unnumbered.text)
    }

    @Test
    fun testChecklist_Toggle() {
        val input = TextFieldValue("Task one", selection = TextRange(0, 0))
        val checked = EditorActions.toggleChecklist(input)
        assertEquals("- [ ] Task one", checked.text)

        val unchecked = EditorActions.toggleChecklist(checked)
        assertEquals("Task one", unchecked.text)
    }

    @Test
    fun testHandleEnterKey_ContinuesList() {
        // Bullet
        val bulletInput = TextFieldValue("- item", selection = TextRange(6, 6))
        val nextBullet = EditorActions.handleEnterKey(bulletInput)
        assertNotNull(nextBullet)
        assertEquals("- item\n- ", nextBullet!!.text)

        // Numbered (+1)
        val numInput = TextFieldValue("1. item", selection = TextRange(7, 7))
        val nextNum = EditorActions.handleEnterKey(numInput)
        assertNotNull(nextNum)
        assertEquals("1. item\n2. ", nextNum!!.text)

        // Checklist (new item unchecked)
        val checkInput = TextFieldValue("- [x] item", selection = TextRange(10, 10))
        val nextCheck = EditorActions.handleEnterKey(checkInput)
        assertNotNull(nextCheck)
        assertEquals("- [x] item\n- [ ] ", nextCheck!!.text)
    }

    @Test
    fun testHandleEnterKey_ExitsOnEmptyItem() {
        val emptyBullet = TextFieldValue("- ", selection = TextRange(2, 2))
        val exitBullet = EditorActions.handleEnterKey(emptyBullet)
        assertNotNull(exitBullet)
        assertEquals("", exitBullet!!.text)

        val emptyNum = TextFieldValue("3. ", selection = TextRange(3, 3))
        val exitNum = EditorActions.handleEnterKey(emptyNum)
        assertNotNull(exitNum)
        assertEquals("", exitNum!!.text)

        val emptyCheck = TextFieldValue("- [ ] ", selection = TextRange(6, 6))
        val exitCheck = EditorActions.handleEnterKey(emptyCheck)
        assertNotNull(exitCheck)
        assertEquals("", exitCheck!!.text)
    }

    @Test
    fun testHandleBackspaceKey_RemovesMarkerAtStart() {
        val bulletAtStart = TextFieldValue("- ", selection = TextRange(2, 2))
        val removed = EditorActions.handleBackspaceKey(bulletAtStart)
        assertNotNull(removed)
        assertEquals("", removed!!.text)
    }

    @Test
    fun testIndentAndOutdent() {
        val input = TextFieldValue("- Item", selection = TextRange(0, 6))
        val indented = EditorActions.indent(input)
        assertEquals("  - Item", indented.text)

        val outdented = EditorActions.outdent(indented)
        assertEquals("- Item", outdented.text)
    }

    @Test
    fun testLinks_InsertAndEdit() {
        val input = TextFieldValue("Check Fotara site", selection = TextRange(6, 12))
        val inserted = EditorActions.insertOrEditLink(input, "Fotara", "https://fotara.arinara.com")
        assertEquals("Check [Fotara](https://fotara.arinara.com) site", inserted.text)

        // Edit link at cursor
        val editCursor = TextFieldValue(inserted.text, selection = TextRange(10, 10))
        val updated = EditorActions.insertOrEditLink(editCursor, "Fotara App", "https://arinara.network")
        assertEquals("Check [Fotara App](https://arinara.network) site", updated.text)
    }

    @Test
    fun testToggleChecklistAtOffset() {
        val text = "- [ ] Buy milk"
        val input = TextFieldValue(text, selection = TextRange(0, 0))
        val toggled = EditorActions.toggleChecklistAtOffset(input, 3)
        assertEquals("- [x] Buy milk", toggled.text)

        val toggledBack = EditorActions.toggleChecklistAtOffset(toggled, 3)
        assertEquals("- [ ] Buy milk", toggledBack.text)
    }

    @Test
    fun testEmptyDocument_AllButtonsSafe() {
        val empty = TextFieldValue("", selection = TextRange(0, 0))

        // Ensure none of the actions crash on empty input
        EditorActions.toggleBold(empty)
        EditorActions.toggleItalic(empty)
        EditorActions.toggleStrikethrough(empty)
        EditorActions.toggleInlineCode(empty)
        EditorActions.toggleHeading(empty, 1)
        EditorActions.toggleHeading(empty, 2)
        EditorActions.toggleHeading(empty, 3)
        EditorActions.toggleBlockquote(empty)
        EditorActions.toggleBulletList(empty)
        EditorActions.toggleNumberedList(empty)
        EditorActions.toggleChecklist(empty)
        EditorActions.toggleCodeBlock(empty)
        EditorActions.insertDivider(empty)
        EditorActions.clearFormatting(empty)
        EditorActions.indent(empty)
        EditorActions.outdent(empty)
    }
}
