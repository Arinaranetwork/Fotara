// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.arinara.fotara.data.model.TextNote

/**
 * High-level bridge adapting [TextFieldValue] editing operations to the pure [TextEditorOps] engine.
 * Guarantees atomic state updates, selection invariants, and predictable caret placement.
 */
object EditorActions {

    // --- Inline Formatting Toggles (Bold, Italic, Strikethrough, Code) ---

    fun toggleBold(value: TextFieldValue): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.toggleBold(text, s, e) }

    fun toggleItalic(value: TextFieldValue): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.toggleItalic(text, s, e) }

    fun toggleStrikethrough(value: TextFieldValue): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.toggleStrikethrough(text, s, e) }

    fun toggleInlineCode(value: TextFieldValue): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.toggleInlineCode(text, s, e) }

    fun toggleInlineMath(value: TextFieldValue): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.toggleInlineMath(text, s, e) }

    fun insertBlockMath(value: TextFieldValue): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.insertBlockMath(text, s, e) }

    // --- Line-Based Formatting (Headings, Blockquotes, Lists, Checklists) ---

    fun toggleHeading(value: TextFieldValue, level: Int): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.applyHeading(text, s, e, level) }

    fun toggleBlockquote(value: TextFieldValue): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.applyQuote(text, s, e) }

    fun toggleBulletList(value: TextFieldValue): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.applyBulletList(text, s, e) }

    fun toggleNumberedList(value: TextFieldValue): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.applyNumberedList(text, s, e) }

    fun toggleChecklist(value: TextFieldValue): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.applyCheckbox(text, s, e) }

    // --- Enter Key & Backspace List Handlers ---

    fun handleEnterKey(value: TextFieldValue): TextFieldValue? {
        val res = TextEditorOps.handleEnterKey(value.text, value.selection.start, value.selection.end) ?: return null
        return TextFieldValue(res.text, TextRange(res.selectionStart, res.selectionEnd))
    }

    fun handleBackspaceKey(value: TextFieldValue): TextFieldValue? {
        val res = TextEditorOps.handleBackspaceKey(value.text, value.selection.start, value.selection.end) ?: return null
        return TextFieldValue(res.text, TextRange(res.selectionStart, res.selectionEnd))
    }

    fun handleTypingShortcut(value: TextFieldValue): TextFieldValue? {
        val res = TextEditorOps.handleTypingShortcut(value.text, value.selection.start, value.selection.end) ?: return null
        return TextFieldValue(res.text, TextRange(res.selectionStart, res.selectionEnd))
    }

    // --- Indent and Outdent ---

    fun indent(value: TextFieldValue): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.indent(text, s, e) }

    fun outdent(value: TextFieldValue): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.outdent(text, s, e) }

    // --- Code Block & Divider ---

    fun toggleCodeBlock(value: TextFieldValue): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.toggleCodeBlock(text, s, e) }

    fun insertDivider(value: TextFieldValue): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.insertHorizontalRule(text, s, e) }

    // --- Markdown Table Operations ---

    fun insertTable(value: TextFieldValue, rows: Int = 2, cols: Int = 3): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.insertTable(text, s, e, rows, cols) }

    fun addTableRowAbove(value: TextFieldValue): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.addTableRowAbove(text, s, e) }

    fun addTableRowBelow(value: TextFieldValue): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.addTableRowBelow(text, s, e) }

    fun deleteCurrentTableRow(value: TextFieldValue): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.deleteCurrentTableRow(text, s, e) }

    fun addTableColumnLeft(value: TextFieldValue): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.addTableColumnLeft(text, s, e) }

    fun addTableColumnRight(value: TextFieldValue): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.addTableColumnRight(text, s, e) }

    fun deleteCurrentTableColumn(value: TextFieldValue): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.deleteCurrentTableColumn(text, s, e) }

    // --- Links ---

    fun insertOrEditLink(value: TextFieldValue, label: String, url: String): TextFieldValue =
        applyOp(value) { text, s, e -> TextEditorOps.insertOrEditLink(text, s, e, label, url) }

    fun removeLink(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val sel = value.selection
        val cursor = sel.min.coerceIn(0, text.length)

        val doc = MarkdownParser.parse(text)
        val existingLink = doc.findLinkAt(cursor) ?: return value

        val linkText = text.substring(existingLink.contentStart, existingLink.contentEnd)
        val newText = text.substring(0, existingLink.start) + linkText + text.substring(existingLink.end)
        return TextFieldValue(newText, selection = TextRange(existingLink.start + linkText.length))
    }

    // --- Clear Formatting ---

    fun clearFormatting(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val sel = value.selection
        val start = sel.min.coerceIn(0, text.length)
        val end = sel.max.coerceIn(0, text.length)

        if (start < end) {
            val selected = text.substring(start, end)
            val cleaned = TextNote.stripMarkdownFormatting(selected)
            val newText = text.substring(0, start) + cleaned + text.substring(end)
            return TextFieldValue(newText, selection = TextRange(start, start + cleaned.length))
        } else {
            val lines = text.lines()
            val newLines = lines.map { TextNote.stripMarkdownFormatting(it) }
            val newText = newLines.joinToString("\n")
            return TextFieldValue(newText, selection = TextRange(start.coerceIn(0, newText.length)))
        }
    }

    // --- Tappable Checklist Checkbox Toggle ---

    fun toggleChecklistAtOffset(value: TextFieldValue, charOffset: Int): TextFieldValue {
        val res = TextEditorOps.toggleChecklistAtOffset(value.text, value.selection.start, value.selection.end, charOffset)
        return TextFieldValue(res.text, TextRange(res.selectionStart, res.selectionEnd))
    }

    fun toggleChecklistAtLine(value: TextFieldValue, lineIndex: Int): TextFieldValue {
        val res = TextEditorOps.toggleChecklistAtLine(value.text, value.selection.start, value.selection.end, lineIndex)
        return TextFieldValue(res.text, TextRange(res.selectionStart, res.selectionEnd))
    }

    // --- Helper Utility ---

    private inline fun applyOp(
        value: TextFieldValue,
        op: (String, Int, Int) -> TextEditResult
    ): TextFieldValue {
        val res = op(value.text, value.selection.start, value.selection.end)
        return TextFieldValue(res.text, TextRange(res.selectionStart, res.selectionEnd))
    }
}
