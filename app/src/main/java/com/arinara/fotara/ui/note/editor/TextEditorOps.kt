// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.editor

/**
 * Result of a text editing operation containing the modified text
 * and the new cursor / selection range.
 */
data class TextEditResult(
    val text: String,
    val selectionStart: Int,
    val selectionEnd: Int
) {
    val selectionMin: Int get() = minOf(selectionStart, selectionEnd)
    val selectionMax: Int get() = maxOf(selectionStart, selectionEnd)
    val isCollapsed: Boolean get() = selectionStart == selectionEnd
}

enum class LineToolType {
    H1,
    H2,
    H3,
    QUOTE,
    BULLET_LIST,
    NUMBERED_LIST,
    CHECKBOX
}

data class ActiveEditorSyntax(
    val isBold: Boolean = false,
    val isItalic: Boolean = false,
    val isStrikethrough: Boolean = false,
    val isInlineCode: Boolean = false,
    val isH1: Boolean = false,
    val isH2: Boolean = false,
    val isH3: Boolean = false,
    val isQuote: Boolean = false,
    val isBulletList: Boolean = false,
    val isNumberedList: Boolean = false,
    val isChecklist: Boolean = false
)

/**
 * Pure, isolated functions implementing all text-editing logic for the markdown note editor.
 * Input: text + selection range.
 * Output: new text + new selection range.
 * Zero UI or Android framework dependencies for 100% deterministic testability.
 */
object TextEditorOps {

    // --- 1. Inline Formatting Tools (Bold, Italic, Strikethrough, Inline Code) ---

    fun toggleBold(text: String, selStart: Int, selEnd: Int): TextEditResult =
        toggleInlineWrap(text, selStart, selEnd, "**", "**")

    fun toggleItalic(text: String, selStart: Int, selEnd: Int): TextEditResult =
        toggleInlineWrap(text, selStart, selEnd, "*", "*")

    fun toggleStrikethrough(text: String, selStart: Int, selEnd: Int): TextEditResult =
        toggleInlineWrap(text, selStart, selEnd, "~~", "~~")

    fun toggleInlineCode(text: String, selStart: Int, selEnd: Int): TextEditResult =
        toggleInlineWrap(text, selStart, selEnd, "`", "`")

    fun toggleInlineWrap(
        text: String,
        selStart: Int,
        selEnd: Int,
        prefix: String,
        suffix: String
    ): TextEditResult {
        val min = minOf(selStart, selEnd).coerceIn(0, text.length)
        val max = maxOf(selStart, selEnd).coerceIn(0, text.length)

        if (min < max) {
            // Case A: Non-empty selection
            val selected = text.substring(min, max)

            // 1. Unwrap if selected text contains markers internally
            if (selected.startsWith(prefix) && selected.endsWith(suffix) && selected.length >= prefix.length + suffix.length) {
                val unwrapped = selected.substring(prefix.length, selected.length - suffix.length)
                val newText = text.substring(0, min) + unwrapped + text.substring(max)
                return TextEditResult(newText, min, min + unwrapped.length)
            }

            // 2. Unwrap if text immediately outside selection is wrapped
            if (min >= prefix.length && max + suffix.length <= text.length) {
                val before = text.substring(min - prefix.length, min)
                val after = text.substring(max, max + suffix.length)
                if (before == prefix && after == suffix) {
                    val newText = text.substring(0, min - prefix.length) + selected + text.substring(max + suffix.length)
                    return TextEditResult(newText, min - prefix.length, max - prefix.length)
                }
            }

            // 3. Otherwise wrap selection
            val newText = text.substring(0, min) + prefix + selected + suffix + text.substring(max)
            return TextEditResult(newText, min + prefix.length, max + prefix.length)
        } else {
            // Case B: Collapsed cursor (min == max)
            val cursor = min

            // 1. Check if cursor is between empty markers e.g. **|**
            if (cursor >= prefix.length && cursor + suffix.length <= text.length) {
                val before = text.substring(cursor - prefix.length, cursor)
                val after = text.substring(cursor, cursor + suffix.length)
                if (before == prefix && after == suffix) {
                    val newText = text.substring(0, cursor - prefix.length) + text.substring(cursor + suffix.length)
                    return TextEditResult(newText, cursor - prefix.length, cursor - prefix.length)
                }
            }

            // 2. Check if cursor is on a word
            val (wStart, wEnd) = findWordBounds(text, cursor)
            if (wStart < wEnd) {
                val word = text.substring(wStart, wEnd)
                // Check if word is already wrapped outside
                if (wStart >= prefix.length && wEnd + suffix.length <= text.length) {
                    val before = text.substring(wStart - prefix.length, wStart)
                    val after = text.substring(wEnd, wEnd + suffix.length)
                    if (before == prefix && after == suffix) {
                        // Unwrap word
                        val newText = text.substring(0, wStart - prefix.length) + word + text.substring(wEnd + suffix.length)
                        val newCursor = (cursor - prefix.length).coerceIn(0, newText.length)
                        return TextEditResult(newText, newCursor, newCursor)
                    }
                }
                // Wrap word
                val newText = text.substring(0, wStart) + prefix + word + suffix + text.substring(wEnd)
                val newCursor = (cursor + prefix.length).coerceIn(0, newText.length)
                return TextEditResult(newText, newCursor, newCursor)
            }

            // 3. Cursor on whitespace or empty line: insert empty pair, place cursor between
            val newText = text.substring(0, cursor) + prefix + suffix + text.substring(cursor)
            return TextEditResult(newText, cursor + prefix.length, cursor + prefix.length)
        }
    }

    private fun findWordBounds(text: String, offset: Int): Pair<Int, Int> {
        if (text.isEmpty()) return 0 to 0
        val clamped = offset.coerceIn(0, text.length)

        if (clamped < text.length && text[clamped].isWhitespace() && (clamped == 0 || text[clamped - 1].isWhitespace())) {
            return clamped to clamped
        }

        var start = clamped
        while (start > 0 && isWordChar(text[start - 1])) {
            start--
        }

        var end = clamped
        while (end < text.length && isWordChar(text[end])) {
            end++
        }

        return start to end
    }

    private fun isWordChar(c: Char): Boolean = c.isLetterOrDigit() || c == '_'

    // --- 2. Line-Based Tools (H1-H3, Quote, Bullet, Numbered, Checkbox) ---

    fun applyHeading(text: String, selStart: Int, selEnd: Int, level: Int): TextEditResult {
        val tool = when (level.coerceIn(1, 3)) {
            1 -> LineToolType.H1
            2 -> LineToolType.H2
            else -> LineToolType.H3
        }
        return applyLineTool(text, selStart, selEnd, tool)
    }

    fun applyQuote(text: String, selStart: Int, selEnd: Int): TextEditResult =
        applyLineTool(text, selStart, selEnd, LineToolType.QUOTE)

    fun applyBulletList(text: String, selStart: Int, selEnd: Int): TextEditResult =
        applyLineTool(text, selStart, selEnd, LineToolType.BULLET_LIST)

    fun applyNumberedList(text: String, selStart: Int, selEnd: Int): TextEditResult =
        applyLineTool(text, selStart, selEnd, LineToolType.NUMBERED_LIST)

    fun applyCheckbox(text: String, selStart: Int, selEnd: Int): TextEditResult =
        applyLineTool(text, selStart, selEnd, LineToolType.CHECKBOX)

    data class ParsedLine(
        val index: Int,
        val originalStart: Int,
        val originalEnd: Int, // end excluding '\n'
        val text: String,
        val indent: String,
        val prefixType: LineToolType?,
        val prefixString: String,
        val content: String
    )

    private val CHECKBOX_PATTERN = Regex("""^([-*+]\s*\[[ xX]\]\s*)""")
    private val BULLET_PATTERN = Regex("""^([-*+]\s+)""")
    private val NUMBERED_PATTERN = Regex("""^(\d+\.\s*)""")
    private val H3_PATTERN = Regex("""^(###\s*)""")
    private val H2_PATTERN = Regex("""^(##\s*)""")
    private val H1_PATTERN = Regex("""^(#\s*)""")
    private val QUOTE_PATTERN = Regex("""^(>\s*)""")

    fun parseLine(index: Int, start: Int, end: Int, lineText: String): ParsedLine {
        val indent = lineText.takeWhile { it == ' ' || it == '\t' }
        val withoutIndent = lineText.substring(indent.length)

        val chkMatch = CHECKBOX_PATTERN.find(withoutIndent)
        if (chkMatch != null) {
            val p = chkMatch.value
            return ParsedLine(index, start, end, lineText, indent, LineToolType.CHECKBOX, p, withoutIndent.substring(p.length))
        }

        val h3Match = H3_PATTERN.find(withoutIndent)
        if (h3Match != null) {
            val p = h3Match.value
            return ParsedLine(index, start, end, lineText, indent, LineToolType.H3, p, withoutIndent.substring(p.length))
        }

        val h2Match = H2_PATTERN.find(withoutIndent)
        if (h2Match != null) {
            val p = h2Match.value
            return ParsedLine(index, start, end, lineText, indent, LineToolType.H2, p, withoutIndent.substring(p.length))
        }

        val h1Match = H1_PATTERN.find(withoutIndent)
        if (h1Match != null) {
            val p = h1Match.value
            return ParsedLine(index, start, end, lineText, indent, LineToolType.H1, p, withoutIndent.substring(p.length))
        }

        val qMatch = QUOTE_PATTERN.find(withoutIndent)
        if (qMatch != null) {
            val p = qMatch.value
            return ParsedLine(index, start, end, lineText, indent, LineToolType.QUOTE, p, withoutIndent.substring(p.length))
        }

        val bulletMatch = BULLET_PATTERN.find(withoutIndent)
        if (bulletMatch != null) {
            val p = bulletMatch.value
            return ParsedLine(index, start, end, lineText, indent, LineToolType.BULLET_LIST, p, withoutIndent.substring(p.length))
        }

        val numMatch = NUMBERED_PATTERN.find(withoutIndent)
        if (numMatch != null) {
            val p = numMatch.value
            return ParsedLine(index, start, end, lineText, indent, LineToolType.NUMBERED_LIST, p, withoutIndent.substring(p.length))
        }

        return ParsedLine(index, start, end, lineText, indent, null, "", withoutIndent)
    }

    private fun getTouchedLines(text: String, selStart: Int, selEnd: Int): List<ParsedLine> {
        val min = minOf(selStart, selEnd).coerceIn(0, text.length)
        val max = maxOf(selStart, selEnd).coerceIn(0, text.length)

        val firstLineStart = text.lastIndexOf('\n', (min - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lastLineEnd = if (min == max) {
            text.indexOf('\n', max).let { if (it == -1) text.length else it }
        } else {
            val endSearch = (max - 1).coerceAtLeast(min)
            text.indexOf('\n', endSearch).let { if (it == -1) text.length else it }
        }

        val lines = mutableListOf<ParsedLine>()
        var cur = firstLineStart
        var idx = 0
        while (cur <= text.length) {
            val nxt = text.indexOf('\n', cur).let { if (it == -1 || it > lastLineEnd) lastLineEnd else it }
            val lineText = text.substring(cur, nxt)
            lines.add(parseLine(idx++, cur, nxt, lineText))
            cur = nxt + 1
            if (cur > lastLineEnd) break
        }

        return lines
    }

    fun applyLineTool(
        text: String,
        selStart: Int,
        selEnd: Int,
        targetType: LineToolType
    ): TextEditResult {
        val touched = getTouchedLines(text, selStart, selEnd)
        if (touched.isEmpty()) {
            return TextEditResult(text, selStart, selEnd)
        }

        val allHaveTarget = touched.all { it.prefixType == targetType }

        val newLines = mutableListOf<String>()
        var numberedCounter = 1

        for (line in touched) {
            val transformed = if (allHaveTarget) {
                line.indent + line.content
            } else {
                val newPrefix = when (targetType) {
                    LineToolType.H1 -> "# "
                    LineToolType.H2 -> "## "
                    LineToolType.H3 -> "### "
                    LineToolType.QUOTE -> "> "
                    LineToolType.BULLET_LIST -> "- "
                    LineToolType.NUMBERED_LIST -> "${numberedCounter++}. "
                    LineToolType.CHECKBOX -> "- [ ] "
                }
                line.indent + newPrefix + line.content
            }
            newLines.add(transformed)
        }

        val blockStart = touched.first().originalStart
        val blockEnd = touched.last().originalEnd

        val sb = StringBuilder()
        sb.append(text.substring(0, blockStart))
        for (i in newLines.indices) {
            if (i > 0) sb.append('\n')
            sb.append(newLines[i])
        }
        sb.append(text.substring(blockEnd))
        var newText = sb.toString()

        val isSingleLine = touched.size == 1
        val minSel = minOf(selStart, selEnd)
        val maxSel = maxOf(selStart, selEnd)

        if (isSingleLine) {
            val oldLine = touched.first()
            val newLine = newLines.first()
            val oldPrefixFullLen = oldLine.indent.length + oldLine.prefixString.length
            val newPrefixFullLen = newLine.length - oldLine.content.length

            val caretOffsetInLine = minSel - oldLine.originalStart

            // Caret invariant: Caret always lands strictly at the end of the new prefix or within content, never inside brackets/delimiters!
            val newCaretInLine = if (caretOffsetInLine <= oldPrefixFullLen) {
                newPrefixFullLen
            } else {
                (caretOffsetInLine - oldPrefixFullLen + newPrefixFullLen).coerceIn(newPrefixFullLen, newLine.length)
            }

            var newCursor = oldLine.originalStart + newCaretInLine

            if (targetType == LineToolType.NUMBERED_LIST || allHaveTarget) {
                val (renumbered, adjustedCursor) = renumberNumberedLists(newText, newCursor)
                newText = renumbered
                newCursor = adjustedCursor
            }

            return TextEditResult(newText, newCursor, newCursor)
        } else {
            val firstLine = touched.first()
            val firstOldPrefixLen = firstLine.indent.length + firstLine.prefixString.length
            val firstNewPrefixLen = newLines.first().length - firstLine.content.length

            val newMin = if (minSel <= firstLine.originalStart + firstOldPrefixLen) {
                firstLine.originalStart + firstNewPrefixLen
            } else {
                (minSel - firstOldPrefixLen + firstNewPrefixLen).coerceAtLeast(firstLine.originalStart + firstNewPrefixLen)
            }.coerceIn(0, newText.length)

            val totalDelta = (newText.length - text.length)
            val newMax = (maxSel + totalDelta).coerceIn(newMin, newText.length)

            if (targetType == LineToolType.NUMBERED_LIST || allHaveTarget) {
                val (renumbered, adjustedMin) = renumberNumberedLists(newText, newMin)
                val lenDiff = renumbered.length - newText.length
                newText = renumbered
                val finalMin = adjustedMin
                val finalMax = (newMax + lenDiff).coerceIn(finalMin, newText.length)
                return if (selStart <= selEnd) {
                    TextEditResult(newText, finalMin, finalMax)
                } else {
                    TextEditResult(newText, finalMax, finalMin)
                }
            }

            return if (selStart <= selEnd) {
                TextEditResult(newText, newMin, newMax)
            } else {
                TextEditResult(newText, newMax, newMin)
            }
        }
    }

    // --- 3. Indent and Outdent ---

    fun canIndent(text: String, selStart: Int, selEnd: Int): Boolean {
        val touched = getTouchedLines(text, selStart, selEnd)
        if (touched.isEmpty()) return false
        val listLines = touched.filter { it.prefixType in listOf(LineToolType.BULLET_LIST, LineToolType.NUMBERED_LIST, LineToolType.CHECKBOX) }
        if (listLines.isEmpty()) return false
        return listLines.any { it.indent.length < 6 }
    }

    fun canOutdent(text: String, selStart: Int, selEnd: Int): Boolean {
        val touched = getTouchedLines(text, selStart, selEnd)
        if (touched.isEmpty()) return false
        val listLines = touched.filter { it.prefixType in listOf(LineToolType.BULLET_LIST, LineToolType.NUMBERED_LIST, LineToolType.CHECKBOX) }
        if (listLines.isEmpty()) return false
        return listLines.any { it.indent.length >= 2 }
    }

    fun indent(text: String, selStart: Int, selEnd: Int): TextEditResult {
        val touched = getTouchedLines(text, selStart, selEnd)
        if (touched.isEmpty()) return TextEditResult(text, selStart, selEnd)

        var hasChange = false
        val newLines = touched.map {
            if (it.prefixType in listOf(LineToolType.BULLET_LIST, LineToolType.NUMBERED_LIST, LineToolType.CHECKBOX)) {
                if (it.indent.length < 6) {
                    hasChange = true
                    "  " + it.text
                } else {
                    it.text
                }
            } else {
                it.text
            }
        }
        if (!hasChange) return TextEditResult(text, selStart, selEnd)

        val blockStart = touched.first().originalStart
        val blockEnd = touched.last().originalEnd

        val joined = newLines.joinToString("\n")
        val newText = text.substring(0, blockStart) + joined + text.substring(blockEnd)

        val firstAdded = newLines.first().length - touched.first().text.length
        val newSelStart = (selStart + firstAdded).coerceIn(0, newText.length)
        val totalDelta = newText.length - text.length
        val newSelEnd = (selEnd + totalDelta).coerceIn(newSelStart, newText.length)

        return TextEditResult(newText, newSelStart, newSelEnd)
    }

    fun outdent(text: String, selStart: Int, selEnd: Int): TextEditResult {
        val touched = getTouchedLines(text, selStart, selEnd)
        if (touched.isEmpty()) return TextEditResult(text, selStart, selEnd)

        var hasChange = false
        val newLines = touched.map {
            if (it.prefixType in listOf(LineToolType.BULLET_LIST, LineToolType.NUMBERED_LIST, LineToolType.CHECKBOX)) {
                when {
                    it.text.startsWith("  ") -> {
                        hasChange = true
                        it.text.substring(2)
                    }
                    it.text.startsWith(" ") -> {
                        hasChange = true
                        it.text.substring(1)
                    }
                    it.text.startsWith("\t") -> {
                        hasChange = true
                        it.text.substring(1)
                    }
                    else -> it.text
                }
            } else {
                it.text
            }
        }
        if (!hasChange) return TextEditResult(text, selStart, selEnd)

        val blockStart = touched.first().originalStart
        val blockEnd = touched.last().originalEnd

        val joined = newLines.joinToString("\n")
        val newText = text.substring(0, blockStart) + joined + text.substring(blockEnd)

        val firstRemoved = touched.first().text.length - newLines.first().length
        val newSelStart = (selStart - firstRemoved).coerceIn(0, newText.length)
        val totalDelta = newText.length - text.length
        val newSelEnd = (selEnd + totalDelta).coerceIn(newSelStart, newText.length)

        return TextEditResult(newText, newSelStart, newSelEnd)
    }

    // --- 4. Enter Key Handler ---

    fun handleEnterKey(text: String, selStart: Int, selEnd: Int): TextEditResult? {
        if (selStart != selEnd) return null

        val cursor = selStart.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val lineText = text.substring(lineStart, lineEnd)
        val parsed = parseLine(0, lineStart, lineEnd, lineText)

        when (parsed.prefixType) {
            LineToolType.CHECKBOX -> {
                if (parsed.content.isBlank()) {
                    // Enter on EMPTY item: Exits the list, turns line into plain paragraph
                    val newText = text.substring(0, lineStart) + parsed.indent + text.substring(lineEnd)
                    val newCursor = lineStart + parsed.indent.length
                    return TextEditResult(newText, newCursor, newCursor)
                } else {
                    // Enter on item WITH text: splits or appends new unchecked checkbox item
                    val prefixFullLen = parsed.indent.length + parsed.prefixString.length
                    val cursorInLine = cursor - lineStart
                    val splitPos = if (cursorInLine < prefixFullLen) prefixFullLen else cursorInLine
                    val beforeCursor = lineText.substring(0, splitPos)
                    val afterCursor = lineText.substring(splitPos)

                    val nextMarker = "\n${parsed.indent}- [ ] "
                    val newText = text.substring(0, lineStart) + beforeCursor + nextMarker + afterCursor + text.substring(lineEnd)
                    val newCursor = lineStart + beforeCursor.length + nextMarker.length
                    return TextEditResult(newText, newCursor, newCursor)
                }
            }
            LineToolType.NUMBERED_LIST -> {
                if (parsed.content.isBlank()) {
                    // Enter on EMPTY item: Exit list
                    val newText = text.substring(0, lineStart) + parsed.indent + text.substring(lineEnd)
                    val newCursor = lineStart + parsed.indent.length
                    val (renumbered, adjustedCursor) = renumberNumberedLists(newText, newCursor)
                    return TextEditResult(renumbered, adjustedCursor, adjustedCursor)
                } else {
                    val prefixFullLen = parsed.indent.length + parsed.prefixString.length
                    val cursorInLine = cursor - lineStart
                    val splitPos = if (cursorInLine < prefixFullLen) prefixFullLen else cursorInLine
                    val beforeCursor = lineText.substring(0, splitPos)
                    val afterCursor = lineText.substring(splitPos)

                    val num = Regex("""^\d+""").find(parsed.prefixString.trim())?.value?.toIntOrNull() ?: 1
                    val nextMarker = "\n${parsed.indent}${num + 1}. "
                    val newText = text.substring(0, lineStart) + beforeCursor + nextMarker + afterCursor + text.substring(lineEnd)
                    val newCursor = lineStart + beforeCursor.length + nextMarker.length
                    val (renumbered, adjustedCursor) = renumberNumberedLists(newText, newCursor)
                    return TextEditResult(renumbered, adjustedCursor, adjustedCursor)
                }
            }
            LineToolType.BULLET_LIST -> {
                if (parsed.content.isBlank()) {
                    // Enter on EMPTY item: Exit list
                    val newText = text.substring(0, lineStart) + parsed.indent + text.substring(lineEnd)
                    val newCursor = lineStart + parsed.indent.length
                    return TextEditResult(newText, newCursor, newCursor)
                } else {
                    val prefixFullLen = parsed.indent.length + parsed.prefixString.length
                    val cursorInLine = cursor - lineStart
                    val splitPos = if (cursorInLine < prefixFullLen) prefixFullLen else cursorInLine
                    val beforeCursor = lineText.substring(0, splitPos)
                    val afterCursor = lineText.substring(splitPos)

                    val nextMarker = "\n${parsed.indent}- "
                    val newText = text.substring(0, lineStart) + beforeCursor + nextMarker + afterCursor + text.substring(lineEnd)
                    val newCursor = lineStart + beforeCursor.length + nextMarker.length
                    return TextEditResult(newText, newCursor, newCursor)
                }
            }
            LineToolType.QUOTE -> {
                if (parsed.content.isBlank()) {
                    // Enter on EMPTY item: Exit quote
                    val newText = text.substring(0, lineStart) + parsed.indent + text.substring(lineEnd)
                    val newCursor = lineStart + parsed.indent.length
                    return TextEditResult(newText, newCursor, newCursor)
                } else {
                    val prefixFullLen = parsed.indent.length + parsed.prefixString.length
                    val cursorInLine = cursor - lineStart
                    val splitPos = if (cursorInLine < prefixFullLen) prefixFullLen else cursorInLine
                    val beforeCursor = lineText.substring(0, splitPos)
                    val afterCursor = lineText.substring(splitPos)

                    val nextMarker = "\n${parsed.indent}> "
                    val newText = text.substring(0, lineStart) + beforeCursor + nextMarker + afterCursor + text.substring(lineEnd)
                    val newCursor = lineStart + beforeCursor.length + nextMarker.length
                    return TextEditResult(newText, newCursor, newCursor)
                }
            }
            else -> return null
        }
    }

    // --- 5. Backspace Key Handler ---

    fun handleBackspaceKey(text: String, selStart: Int, selEnd: Int): TextEditResult? {
        if (selStart != selEnd) return null

        val cursor = selStart.coerceIn(0, text.length)
        if (cursor == 0) return null

        val lineStart = if (cursor == 0) 0 else {
            val prevNl = text.lastIndexOf('\n', cursor - 1)
            if (prevNl == -1) 0 else prevNl + 1
        }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val lineText = text.substring(lineStart, lineEnd)

        // Check if cursor is at start of line below a divider
        if (cursor == lineStart && lineStart > 0) {
            val prevLineEnd = lineStart - 1
            val prevLineStart = if (prevLineEnd == 0) 0 else {
                val p = text.lastIndexOf('\n', prevLineEnd - 1)
                if (p == -1) 0 else p + 1
            }
            val prevLine = text.substring(prevLineStart, prevLineEnd).trim()
            if (prevLine == "---" || prevLine == "***") {
                val newText = text.substring(0, prevLineStart) + text.substring(lineStart)
                val newCursor = prevLineStart.coerceIn(0, newText.length)
                return TextEditResult(newText, newCursor, newCursor)
            }
        }

        val parsed = parseLine(0, lineStart, lineEnd, lineText)

        if (parsed.prefixType != null) {
            val prefixFullLen = parsed.indent.length + parsed.prefixString.length
            val cursorInLine = cursor - lineStart

            // If cursor is right after prefix
            if (cursorInLine == prefixFullLen) {
                // If indent > 0, outdent first!
                if (parsed.indent.length >= 2) {
                    val newIndent = parsed.indent.substring(2)
                    val newLine = newIndent + parsed.prefixString + parsed.content
                    val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
                    val newCursor = cursor - 2
                    return TextEditResult(newText, newCursor, newCursor)
                }

                // Removes only the prefix, text stays as a paragraph
                val newLine = parsed.indent + parsed.content
                val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
                val newCursor = lineStart + parsed.indent.length
                val (renumbered, adjustedCursor) = if (parsed.prefixType == LineToolType.NUMBERED_LIST) {
                    renumberNumberedLists(newText, newCursor)
                } else {
                    newText to newCursor
                }
                return TextEditResult(renumbered, adjustedCursor, adjustedCursor)
            }
        } else {
            // Check if divider line
            val trimmed = lineText.trim()
            if (trimmed == "---" || trimmed == "***") {
                val removeStart = if (lineStart > 0 && text[lineStart - 1] == '\n') lineStart - 1 else lineStart
                val newText = text.substring(0, removeStart) + text.substring(lineEnd)
                val newCursor = removeStart.coerceIn(0, newText.length)
                return TextEditResult(newText, newCursor, newCursor)
            }
        }

        return null
    }

    // --- 6. Typing Shortcuts at Line Start ---

    fun handleTypingShortcut(text: String, selStart: Int, selEnd: Int): TextEditResult? {
        val cursor = maxOf(selStart, selEnd).coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val minSel = minOf(selStart, selEnd)
        if (minSel < lineStart) return null
        val lineText = text.substring(lineStart, lineEnd)

        val indent = lineText.takeWhile { it == ' ' || it == '\t' }
        val withoutIndent = lineText.substring(indent.length)
        val cursorInLine = cursor - lineStart - indent.length

        if (cursorInLine <= 0) return null

        val prefixTyped = withoutIndent.substring(0, cursorInLine)
        val remainder = withoutIndent.substring(cursorInLine)

        // Matching triggers: "- ", "* ", "1. ", "[] ", "# ", "## ", "### ", "> "
        val (newPrefix, prefixLen) = when (prefixTyped) {
            "- ", "* " -> "- " to 2
            "1. " -> "1. " to 3
            "[] " -> "- [ ] " to 6
            "# " -> "# " to 2
            "## " -> "## " to 3
            "### " -> "### " to 4
            "> " -> "> " to 2
            else -> return null
        }

        val newLine = indent + newPrefix + remainder
        val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
        val newCursor = lineStart + indent.length + newPrefix.length
        return TextEditResult(newText, newCursor, newCursor)
    }

    // --- 7. Numbered List Auto-Renumbering ---

    fun renumberNumberedLists(text: String, caretOffset: Int = -1): Pair<String, Int> {
        val lines = text.lines()
        var adjustedCaret = caretOffset
        val newLines = mutableListOf<String>()

        var inNumberedRun = false
        var currentIndent = ""
        var expectedNumber = 1
        var charOffset = 0

        for (line in lines) {
            val lineLen = line.length
            val indent = line.takeWhile { it == ' ' || it == '\t' }
            val withoutIndent = line.substring(indent.length)
            val numMatch = NUMBERED_PATTERN.find(withoutIndent)

            if (numMatch != null) {
                if (!inNumberedRun || indent != currentIndent) {
                    inNumberedRun = true
                    currentIndent = indent
                    expectedNumber = 1
                }

                val currentPrefix = numMatch.value
                val newPrefix = "$expectedNumber. "
                expectedNumber++

                if (currentPrefix != newPrefix) {
                    val content = withoutIndent.substring(currentPrefix.length)
                    val newLine = indent + newPrefix + content
                    val delta = newLine.length - line.length
                    if (adjustedCaret >= charOffset + indent.length + currentPrefix.length) {
                        adjustedCaret += delta
                    }
                    newLines.add(newLine)
                } else {
                    newLines.add(line)
                }
            } else {
                inNumberedRun = false
                currentIndent = ""
                expectedNumber = 1
                newLines.add(line)
            }

            charOffset += lineLen + 1
        }

        val resultText = newLines.joinToString("\n")
        return resultText to adjustedCaret.coerceIn(0, resultText.length)
    }

    // --- 8. Code Block ---

    fun toggleCodeBlock(text: String, selStart: Int, selEnd: Int): TextEditResult {
        val min = minOf(selStart, selEnd).coerceIn(0, text.length)
        val max = maxOf(selStart, selEnd).coerceIn(0, text.length)

        if (min < max) {
            val selected = text.substring(min, max)
            if (selected.startsWith("```\n") && selected.endsWith("\n```")) {
                val unwrapped = selected.removePrefix("```\n").removeSuffix("\n```")
                val newText = text.substring(0, min) + unwrapped + text.substring(max)
                return TextEditResult(newText, min, min + unwrapped.length)
            } else {
                val newText = text.substring(0, min) + "```\n" + selected + "\n```" + text.substring(max)
                return TextEditResult(newText, min + 4, min + 4 + selected.length)
            }
        } else {
            val newText = text.substring(0, min) + "```\n\n```" + text.substring(min)
            return TextEditResult(newText, min + 4, min + 4)
        }
    }

    // --- 9. Horizontal Rule (Divider) ---

    fun insertHorizontalRule(text: String, selStart: Int, selEnd: Int): TextEditResult {
        val min = minOf(selStart, selEnd).coerceIn(0, text.length)
        val max = maxOf(selStart, selEnd).coerceIn(0, text.length)

        val before = if (min > 0 && text[min - 1] != '\n') "\n" else ""
        val after = if (max < text.length && text[max] != '\n') "\n" else ""
        val hr = "$before---\n$after"

        val newText = text.substring(0, min) + hr + text.substring(max)
        val newCursor = min + hr.length
        return TextEditResult(newText, newCursor, newCursor)
    }

    // --- 10. Link Insertion ---

    fun insertOrEditLink(
        text: String,
        selStart: Int,
        selEnd: Int,
        label: String,
        url: String
    ): TextEditResult {
        val min = minOf(selStart, selEnd).coerceIn(0, text.length)
        val max = maxOf(selStart, selEnd).coerceIn(0, text.length)

        var replaceStart = min
        var replaceEnd = max

        if (min == max) {
            val doc = MarkdownParser.parse(text)
            val existing = doc.findLinkAt(min)
            if (existing != null) {
                replaceStart = existing.start
                replaceEnd = existing.end
            }
        }

        val cleanLabel = label.ifBlank {
            if (min < max) text.substring(min, max) else "link"
        }
        val cleanUrl = url.trim().ifBlank { "https://" }
        val markdownLink = "[$cleanLabel]($cleanUrl)"

        val newText = text.substring(0, replaceStart) + markdownLink + text.substring(replaceEnd)
        val newCursor = replaceStart + markdownLink.length
        return TextEditResult(newText, newCursor, newCursor)
    }

    // --- 11. Checkbox Toggle at Offset / Line ---

    fun toggleChecklistAtOffset(text: String, selStart: Int, selEnd: Int, charOffset: Int): TextEditResult {
        if (text.isEmpty() || charOffset !in 0..text.length) return TextEditResult(text, selStart, selEnd)

        val clamped = charOffset.coerceIn(0, text.length)
        val lineStart = if (clamped == 0) 0 else {
            val prevNl = text.lastIndexOf('\n', clamped - 1)
            if (prevNl == -1) 0 else prevNl + 1
        }
        val lineEnd = text.indexOf('\n', clamped).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)

        val uncheckedRegex = Regex("""^(\s*[-*+]\s*)\[\s\](\s*)""")
        val checkedRegex = Regex("""^(\s*[-*+]\s*)\[[xX]\](\s*)""")

        val unMatch = uncheckedRegex.find(line)
        if (unMatch != null) {
            val newLine = line.replaceFirst(Regex("""\[\s\]"""), "[x]")
            val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
            return TextEditResult(newText, selStart.coerceIn(0, newText.length), selEnd.coerceIn(0, newText.length))
        }

        val chMatch = checkedRegex.find(line)
        if (chMatch != null) {
            val newLine = line.replaceFirst(Regex("""\[[xX]\]"""), "[ ]")
            val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
            return TextEditResult(newText, selStart.coerceIn(0, newText.length), selEnd.coerceIn(0, newText.length))
        }

        return TextEditResult(text, selStart, selEnd)
    }

    fun toggleChecklistAtLine(text: String, selStart: Int, selEnd: Int, targetLineIndex: Int): TextEditResult {
        val lines = text.lines()
        if (targetLineIndex !in lines.indices) return TextEditResult(text, selStart, selEnd)

        val uncheckedRegex = Regex("""^(\s*[-*+]\s*)\[\s\](\s*)""")
        val checkedRegex = Regex("""^(\s*[-*+]\s*)\[[xX]\](\s*)""")

        val oldLine = lines[targetLineIndex]
        val newLine = when {
            uncheckedRegex.containsMatchIn(oldLine) -> oldLine.replaceFirst(Regex("""\[\s\]"""), "[x]")
            checkedRegex.containsMatchIn(oldLine) -> oldLine.replaceFirst(Regex("""\[[xX]\]"""), "[ ]")
            else -> oldLine
        }

        if (newLine == oldLine) return TextEditResult(text, selStart, selEnd)

        val newLines = lines.toMutableList()
        newLines[targetLineIndex] = newLine
        val newText = newLines.joinToString("\n")
        return TextEditResult(newText, selStart.coerceIn(0, newText.length), selEnd.coerceIn(0, newText.length))
    }

    // --- 12. Active Toolbar States Detection ---

    fun getActiveToolbarStates(text: String, selStart: Int, selEnd: Int): ActiveEditorSyntax {
        if (text.isEmpty()) return ActiveEditorSyntax()

        val min = minOf(selStart, selEnd).coerceIn(0, text.length)
        val max = maxOf(selStart, selEnd).coerceIn(0, text.length)

        val touched = getTouchedLines(text, min, max)
        val firstLine = touched.firstOrNull()

        val isH1 = firstLine?.prefixType == LineToolType.H1
        val isH2 = firstLine?.prefixType == LineToolType.H2
        val isH3 = firstLine?.prefixType == LineToolType.H3
        val isQuote = firstLine?.prefixType == LineToolType.QUOTE
        val isBullet = firstLine?.prefixType == LineToolType.BULLET_LIST
        val isNumbered = firstLine?.prefixType == LineToolType.NUMBERED_LIST
        val isCheck = firstLine?.prefixType == LineToolType.CHECKBOX

        val doc = MarkdownParser.parse(text)
        val activeSpans = doc.activeTypesAt(min, max)

        return ActiveEditorSyntax(
            isBold = MarkdownSpanType.BOLD in activeSpans || MarkdownSpanType.BOLD_ITALIC in activeSpans,
            isItalic = MarkdownSpanType.ITALIC in activeSpans || MarkdownSpanType.BOLD_ITALIC in activeSpans,
            isStrikethrough = MarkdownSpanType.STRIKETHROUGH in activeSpans,
            isInlineCode = MarkdownSpanType.INLINE_CODE in activeSpans,
            isH1 = isH1,
            isH2 = isH2,
            isH3 = isH3,
            isQuote = isQuote,
            isBulletList = isBullet,
            isNumberedList = isNumbered,
            isChecklist = isCheck
        )
    }
}
