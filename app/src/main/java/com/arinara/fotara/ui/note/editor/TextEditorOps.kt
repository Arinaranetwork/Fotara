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

        // If offset is between words or on whitespace, don't expand
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

    fun parseLine(index: Int, start: Int, end: Int, lineText: String): ParsedLine {
        val indent = lineText.takeWhile { it == ' ' || it == '\t' }
        val withoutIndent = lineText.substring(indent.length)

        val (type, prefixStr, content) = when {
            withoutIndent.startsWith("- [ ] ") || withoutIndent.startsWith("- [x] ") || withoutIndent.startsWith("- [X] ") -> {
                val p = withoutIndent.substring(0, 6)
                Triple(LineToolType.CHECKBOX, p, withoutIndent.substring(6))
            }
            withoutIndent.startsWith("* [ ] ") || withoutIndent.startsWith("* [x] ") || withoutIndent.startsWith("* [X] ") -> {
                val p = withoutIndent.substring(0, 6)
                Triple(LineToolType.CHECKBOX, p, withoutIndent.substring(6))
            }
            withoutIndent.startsWith("### ") -> Triple(LineToolType.H3, "### ", withoutIndent.substring(4))
            withoutIndent.startsWith("## ") -> Triple(LineToolType.H2, "## ", withoutIndent.substring(3))
            withoutIndent.startsWith("# ") -> Triple(LineToolType.H1, "# ", withoutIndent.substring(2))
            withoutIndent.startsWith("> ") -> Triple(LineToolType.QUOTE, "> ", withoutIndent.substring(2))
            withoutIndent.startsWith(">") -> Triple(LineToolType.QUOTE, ">", withoutIndent.substring(1))
            withoutIndent.startsWith("- ") -> Triple(LineToolType.BULLET_LIST, "- ", withoutIndent.substring(2))
            withoutIndent.startsWith("* ") -> Triple(LineToolType.BULLET_LIST, "* ", withoutIndent.substring(2))
            Regex("""^\d+\.\s+""").containsMatchIn(withoutIndent) -> {
                val match = Regex("""^\d+\.\s+""").find(withoutIndent)!!
                Triple(LineToolType.NUMBERED_LIST, match.value, withoutIndent.substring(match.value.length))
            }
            else -> Triple(null, "", withoutIndent)
        }

        return ParsedLine(
            index = index,
            originalStart = start,
            originalEnd = end,
            text = lineText,
            indent = indent,
            prefixType = type,
            prefixString = prefixStr,
            content = content
        )
    }

    private fun getTouchedLines(text: String, selStart: Int, selEnd: Int): List<ParsedLine> {
        val min = minOf(selStart, selEnd).coerceIn(0, text.length)
        val max = maxOf(selStart, selEnd).coerceIn(0, text.length)

        // Find start line
        val firstLineStart = text.lastIndexOf('\n', (min - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }

        // Find end line
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

        // Rule: If ALL touched lines already have this prefix type -> toggle off
        val allHaveTarget = touched.all { it.prefixType == targetType }

        val newLines = mutableListOf<String>()
        var numberedCounter = 1

        for (line in touched) {
            val transformed = if (allHaveTarget) {
                // Toggle off: keep indent + content
                line.indent + line.content
            } else {
                // Apply target type: replace old prefix with new prefix
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

        // Splice new lines into text
        val blockStart = touched.first().originalStart
        val blockEnd = touched.last().originalEnd

        val sb = StringBuilder()
        sb.append(text.substring(0, blockStart))
        for (i in newLines.indices) {
            if (i > 0) sb.append('\n')
            sb.append(newLines[i])
        }
        sb.append(text.substring(blockEnd))
        val newText = sb.toString()

        // Compute new caret position with 100% precision
        val isSingleLine = touched.size == 1
        val minSel = minOf(selStart, selEnd)
        val maxSel = maxOf(selStart, selEnd)

        if (isSingleLine) {
            val oldLine = touched.first()
            val newLine = newLines.first()

            val oldPrefixFullLen = oldLine.indent.length + oldLine.prefixString.length
            val newPrefixFullLen = if (allHaveTarget) {
                oldLine.indent.length
            } else {
                when (targetType) {
                    LineToolType.H1 -> oldLine.indent.length + 2
                    LineToolType.H2 -> oldLine.indent.length + 3
                    LineToolType.H3 -> oldLine.indent.length + 4
                    LineToolType.QUOTE -> oldLine.indent.length + 2
                    LineToolType.BULLET_LIST -> oldLine.indent.length + 2
                    LineToolType.NUMBERED_LIST -> oldLine.indent.length + (newLines.first().length - oldLine.content.length)
                    LineToolType.CHECKBOX -> oldLine.indent.length + 6
                }
            }

            val caretOffsetInLine = minSel - oldLine.originalStart

            val newCaretInLine = if (caretOffsetInLine <= oldPrefixFullLen) {
                // Caret was at start or inside old prefix: place it strictly AFTER the new prefix!
                newPrefixFullLen
            } else {
                // Caret was inside content: shift by delta between new and old prefix
                caretOffsetInLine - oldPrefixFullLen + newPrefixFullLen
            }.coerceIn(0, newLine.length)

            val newCursor = oldLine.originalStart + newCaretInLine
            return TextEditResult(newText, newCursor, newCursor)
        } else {
            // Multi-line selection: adjust start and end
            val firstLine = touched.first()
            val lastLine = touched.last()

            val firstOldPrefixLen = firstLine.indent.length + firstLine.prefixString.length
            val firstNewPrefixLen = if (allHaveTarget) firstLine.indent.length else {
                newLines.first().length - firstLine.content.length
            }

            val newMin = if (minSel <= firstLine.originalStart + firstOldPrefixLen) {
                firstLine.originalStart + firstNewPrefixLen
            } else {
                minSel - firstOldPrefixLen + firstNewPrefixLen
            }.coerceIn(0, newText.length)

            // Total length change up to the last line
            val totalDelta = (newText.length - text.length)
            val newMax = (maxSel + totalDelta).coerceIn(newMin, newText.length)

            return if (selStart <= selEnd) {
                TextEditResult(newText, newMin, newMax)
            } else {
                TextEditResult(newText, newMax, newMin)
            }
        }
    }

    // --- 3. Indent and Outdent ---

    fun indent(text: String, selStart: Int, selEnd: Int): TextEditResult {
        val touched = getTouchedLines(text, selStart, selEnd)
        if (touched.isEmpty()) return TextEditResult(text, selStart, selEnd)

        val newLines = touched.map { "  " + it.text }
        val blockStart = touched.first().originalStart
        val blockEnd = touched.last().originalEnd

        val joined = newLines.joinToString("\n")
        val newText = text.substring(0, blockStart) + joined + text.substring(blockEnd)

        val shift = 2
        val newSelStart = (selStart + shift).coerceIn(0, newText.length)
        val newSelEnd = (selEnd + shift * touched.size).coerceIn(0, newText.length)

        return TextEditResult(newText, newSelStart, newSelEnd)
    }

    fun outdent(text: String, selStart: Int, selEnd: Int): TextEditResult {
        val touched = getTouchedLines(text, selStart, selEnd)
        if (touched.isEmpty()) return TextEditResult(text, selStart, selEnd)

        val newLines = touched.map {
            when {
                it.text.startsWith("  ") -> it.text.substring(2)
                it.text.startsWith(" ") -> it.text.substring(1)
                it.text.startsWith("\t") -> it.text.substring(1)
                else -> it.text
            }
        }

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
                    // Exit list on empty item: remove prefix from line
                    val newText = text.substring(0, lineStart) + parsed.indent + text.substring(lineEnd)
                    val newCursor = lineStart + parsed.indent.length
                    return TextEditResult(newText, newCursor, newCursor)
                } else {
                    // Continue checklist
                    val nextMarker = "\n${parsed.indent}- [ ] "
                    val newText = text.substring(0, cursor) + nextMarker + text.substring(cursor)
                    val newCursor = cursor + nextMarker.length
                    return TextEditResult(newText, newCursor, newCursor)
                }
            }
            LineToolType.NUMBERED_LIST -> {
                if (parsed.content.isBlank()) {
                    // Exit list
                    val newText = text.substring(0, lineStart) + parsed.indent + text.substring(lineEnd)
                    val newCursor = lineStart + parsed.indent.length
                    return TextEditResult(newText, newCursor, newCursor)
                } else {
                    val num = Regex("""^\d+""").find(parsed.prefixString)?.value?.toIntOrNull() ?: 1
                    val nextMarker = "\n${parsed.indent}${num + 1}. "
                    val newText = text.substring(0, cursor) + nextMarker + text.substring(cursor)
                    val newCursor = cursor + nextMarker.length
                    return TextEditResult(newText, newCursor, newCursor)
                }
            }
            LineToolType.BULLET_LIST -> {
                if (parsed.content.isBlank()) {
                    // Exit list
                    val newText = text.substring(0, lineStart) + parsed.indent + text.substring(lineEnd)
                    val newCursor = lineStart + parsed.indent.length
                    return TextEditResult(newText, newCursor, newCursor)
                } else {
                    val nextMarker = "\n${parsed.indent}- "
                    val newText = text.substring(0, cursor) + nextMarker + text.substring(cursor)
                    val newCursor = cursor + nextMarker.length
                    return TextEditResult(newText, newCursor, newCursor)
                }
            }
            LineToolType.QUOTE -> {
                if (parsed.content.isBlank()) {
                    // Exit quote
                    val newText = text.substring(0, lineStart) + parsed.indent + text.substring(lineEnd)
                    val newCursor = lineStart + parsed.indent.length
                    return TextEditResult(newText, newCursor, newCursor)
                } else {
                    val nextMarker = "\n${parsed.indent}> "
                    val newText = text.substring(0, cursor) + nextMarker + text.substring(cursor)
                    val newCursor = cursor + nextMarker.length
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

        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val prefixSlice = text.substring(lineStart, cursor)

        // Check if cursor is right after a line prefix with nothing else before it
        val matchesPrefix = prefixSlice.matches(Regex("""^\s*[-*+]\s*\[[ xX]\]\s*$""")) ||
                prefixSlice.matches(Regex("""^\s*\d+\.\s*$""")) ||
                prefixSlice.matches(Regex("""^\s*[-*+]\s*$""")) ||
                prefixSlice.matches(Regex("""^\s*>\s*$""")) ||
                prefixSlice.matches(Regex("""^\s*#{1,6}\s*$"""))

        if (matchesPrefix) {
            // Delete entire prefix in one stroke, turning line into plain text
            val newText = text.substring(0, lineStart) + text.substring(cursor)
            return TextEditResult(newText, lineStart, lineStart)
        }

        return null
    }

    // --- 6. Code Block ---

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

    // --- 7. Horizontal Rule ---

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

    // --- 8. Link Insertion ---

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

    // --- 9. Checkbox Toggle at Offset ---

    fun toggleChecklistAtOffset(text: String, selStart: Int, selEnd: Int, charOffset: Int): TextEditResult {
        if (charOffset !in text.indices) return TextEditResult(text, selStart, selEnd)

        val lineStart = text.lastIndexOf('\n', (charOffset - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', charOffset).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)

        val uncheckedRegex = Regex("""^(\s*[-*+]\s*)\[\s\](\s*)""")
        val checkedRegex = Regex("""^(\s*[-*+]\s*)\[[xX]\](\s*)""")

        val unMatch = uncheckedRegex.find(line)
        if (unMatch != null) {
            val newLine = line.replaceFirst(Regex("""\[\s\]"""), "[x]")
            val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
            return TextEditResult(newText, selStart, selEnd)
        }

        val chMatch = checkedRegex.find(line)
        if (chMatch != null) {
            val newLine = line.replaceFirst(Regex("""\[[xX]\]"""), "[ ]")
            val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
            return TextEditResult(newText, selStart, selEnd)
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

    // --- 10. Active Toolbar States Detection ---

    fun getActiveToolbarStates(text: String, selStart: Int, selEnd: Int): ActiveEditorSyntax {
        if (text.isEmpty()) return ActiveEditorSyntax()

        val min = minOf(selStart, selEnd).coerceIn(0, text.length)
        val max = maxOf(selStart, selEnd).coerceIn(0, text.length)

        // Parse line context
        val touched = getTouchedLines(text, min, max)
        val firstLine = touched.firstOrNull()

        val isH1 = firstLine?.prefixType == LineToolType.H1
        val isH2 = firstLine?.prefixType == LineToolType.H2
        val isH3 = firstLine?.prefixType == LineToolType.H3
        val isQuote = firstLine?.prefixType == LineToolType.QUOTE
        val isBullet = firstLine?.prefixType == LineToolType.BULLET_LIST
        val isNumbered = firstLine?.prefixType == LineToolType.NUMBERED_LIST
        val isCheck = firstLine?.prefixType == LineToolType.CHECKBOX

        // Parse inline context around cursor or selection
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
