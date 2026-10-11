// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.editor

import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue

/**
 * Pure handler processing raw [TextFieldValue] changes from [BasicTextField].
 * Bridges user keyboard events (Enter, Backspace, shortcuts, pasting) to markdown ops
 * without ambiguous text-length comparisons.
 */
object EditorChangeHandler {

    fun processChange(oldBody: TextFieldValue, newBody: TextFieldValue): TextFieldValue {
        val oldText = oldBody.text
        val newText = newBody.text
        val oldSel = oldBody.selection
        val newSel = newBody.selection

        val newlinesAdded = newText.count { it == '\n' } - oldText.count { it == '\n' }

        // 1. Multi-line paste: keep lines intact and do not trigger Enter logic
        if (newlinesAdded > 1) {
            return newBody
        }

        // 2. Exactly one newline added -> User pressed Enter or committed a line ending
        if (newlinesAdded == 1) {
            // Find where the newline was inserted in newText
            val nlIdx = when {
                newSel.min > 0 && newText.getOrNull(newSel.min - 1) == '\n' -> newSel.min - 1
                oldSel.min in 0..newText.length && newText.getOrNull(oldSel.min) == '\n' -> oldSel.min
                else -> {
                    val p = newText.indexOf('\n', (oldSel.min - 1).coerceAtLeast(0))
                    if (p != -1) p else newText.lastIndexOf('\n')
                }
            }

            // Find the line preceding this newline in newText
            val lineStart = if (nlIdx == 0) 0 else {
                val p = newText.lastIndexOf('\n', nlIdx - 1)
                if (p == -1) 0 else p + 1
            }
            val lineText = newText.substring(lineStart, nlIdx)
            val parsed = TextEditorOps.parseLine(0, lineStart, nlIdx, lineText)

            if (parsed.prefixType != null) {
                if (parsed.content.isBlank()) {
                    // Enter on EMPTY item: Exits the list, turns line into plain paragraph
                    val newTextResult = newText.substring(0, lineStart) + parsed.indent + newText.substring(nlIdx + 1)
                    var newCaret = lineStart + parsed.indent.length
                    var finalText = newTextResult
                    if (parsed.prefixType == LineToolType.NUMBERED_LIST) {
                        val (renumbered, adjusted) = TextEditorOps.renumberNumberedLists(finalText, newCaret)
                        finalText = renumbered
                        newCaret = adjusted
                    }
                    return TextFieldValue(finalText, TextRange(newCaret))
                } else {
                    // Enter on item WITH text: creates new item of same type below, preserving indent
                    val nextMarker = when (parsed.prefixType) {
                        LineToolType.CHECKBOX -> "${parsed.indent}- [ ] "
                        LineToolType.BULLET_LIST -> {
                            val bullet = if (parsed.prefixString.trim().startsWith("*")) "* " else "- "
                            "${parsed.indent}$bullet"
                        }
                        LineToolType.NUMBERED_LIST -> {
                            val num = Regex("""^\d+""").find(parsed.prefixString.trim())?.value?.toIntOrNull() ?: 1
                            "${parsed.indent}${num + 1}. "
                        }
                        LineToolType.QUOTE -> "${parsed.indent}> "
                        else -> ""
                    }

                    if (nextMarker.isNotEmpty()) {
                        val newTextResult = newText.substring(0, nlIdx + 1) + nextMarker + newText.substring(nlIdx + 1)
                        var newCaret = nlIdx + 1 + nextMarker.length
                        var finalText = newTextResult
                        if (parsed.prefixType == LineToolType.NUMBERED_LIST) {
                            val (renumbered, adjusted) = TextEditorOps.renumberNumberedLists(finalText, newCaret)
                            finalText = renumbered
                            newCaret = adjusted
                        }
                        return TextFieldValue(finalText, TextRange(newCaret))
                    }
                }
            }

            return newBody
        }

        // 3. Backspace deletion handling
        val isDeletion = newText.length < oldText.length && oldSel.collapsed
        if (isDeletion) {
            val handled = TextEditorOps.handleBackspaceKey(oldText, oldSel.start, oldSel.end)
            if (handled != null) {
                return TextFieldValue(handled.text, TextRange(handled.selectionStart, handled.selectionEnd))
            }
        }

        // 4. Space & Typing shortcuts handling
        // Rule: Space never deletes or converts an existing marker
        if (newText.length == oldText.length + 1 && newSel.collapsed && newSel.min > 0 && newText[newSel.min - 1] == ' ') {
            val lineStart = if (oldSel.start == 0) 0 else {
                val p = oldText.lastIndexOf('\n', (oldSel.start - 1).coerceAtLeast(0))
                if (p == -1) 0 else p + 1
            }
            val lineEnd = oldText.indexOf('\n', oldSel.start).let { if (it == -1) oldText.length else it }
            val lineText = oldText.substring(lineStart, lineEnd)
            val parsed = TextEditorOps.parseLine(0, lineStart, lineEnd, lineText)

            if (parsed.prefixType != null) {
                // Line already has a list/block prefix: Space never deletes or converts it
                return newBody
            }

            val shortcutHandled = TextEditorOps.handleTypingShortcut(newText, newSel.start, newSel.end)
            if (shortcutHandled != null) {
                return TextFieldValue(shortcutHandled.text, TextRange(shortcutHandled.selectionStart, shortcutHandled.selectionEnd))
            }
        }

        return newBody
    }
}
