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
 * Pure functions implementing all Markdown editing actions.
 * Every action takes a [TextFieldValue] and returns a new [TextFieldValue]
 * with guaranteed selection invariants, non-crashing boundary clamps,
 * and predictable cursor placement.
 */
object EditorActions {

    // --- Inline Formatting Toggles (Bold, Italic, Strikethrough, Code) ---

    fun toggleBold(value: TextFieldValue): TextFieldValue = toggleWrap(value, "**", "**")
    fun toggleItalic(value: TextFieldValue): TextFieldValue = toggleWrap(value, "*", "*")
    fun toggleStrikethrough(value: TextFieldValue): TextFieldValue = toggleWrap(value, "~~", "~~")
    fun toggleInlineCode(value: TextFieldValue): TextFieldValue = toggleWrap(value, "`", "`")

    fun toggleWrap(value: TextFieldValue, prefix: String, suffix: String): TextFieldValue {
        val text = value.text
        val sel = value.selection
        val start = sel.min.coerceIn(0, text.length)
        val end = sel.max.coerceIn(0, text.length)

        if (start < end) {
            // Case 1: Non-empty selection
            val selectedText = text.substring(start, end)

            // 1a. If selected text is already wrapped internally by prefix and suffix
            if (selectedText.startsWith(prefix) && selectedText.endsWith(suffix) && selectedText.length >= prefix.length + suffix.length) {
                val unwrapped = selectedText.substring(prefix.length, selectedText.length - suffix.length)
                val newText = text.substring(0, start) + unwrapped + text.substring(end)
                return TextFieldValue(newText, selection = TextRange(start, start + unwrapped.length))
            }

            // 1b. If text immediately outside the selection is wrapped by prefix and suffix
            if (start >= prefix.length && end + suffix.length <= text.length) {
                val before = text.substring(start - prefix.length, start)
                val after = text.substring(end, end + suffix.length)
                if (before == prefix && after == suffix) {
                    val newText = text.substring(0, start - prefix.length) + selectedText + text.substring(end + suffix.length)
                    return TextFieldValue(newText, selection = TextRange(start - prefix.length, end - prefix.length))
                }
            }

            // 1c. Otherwise, wrap the selection
            val newText = text.substring(0, start) + prefix + selectedText + suffix + text.substring(end)
            return TextFieldValue(newText, selection = TextRange(start + prefix.length, end + prefix.length))
        } else {
            // Case 2: Collapsed cursor
            // Check if cursor is on a word: find word bounds
            val (wordStart, wordEnd) = findWordBounds(text, start)
            if (wordStart < wordEnd) {
                val word = text.substring(wordStart, wordEnd)
                // Check if word is already wrapped outside
                if (wordStart >= prefix.length && wordEnd + suffix.length <= text.length) {
                    val before = text.substring(wordStart - prefix.length, wordStart)
                    val after = text.substring(wordEnd, wordEnd + suffix.length)
                    if (before == prefix && after == suffix) {
                        val newText = text.substring(0, wordStart - prefix.length) + word + text.substring(wordEnd + suffix.length)
                        val newCursor = (start - prefix.length).coerceIn(0, newText.length)
                        return TextFieldValue(newText, selection = TextRange(newCursor))
                    }
                }
                // Wrap the word
                val newText = text.substring(0, wordStart) + prefix + word + suffix + text.substring(wordEnd)
                val newCursor = (start + prefix.length).coerceIn(0, newText.length)
                return TextFieldValue(newText, selection = TextRange(newCursor))
            }

            // Cursor is not on a word: check if cursor is between empty markers
            if (start >= prefix.length && start + suffix.length <= text.length) {
                val before = text.substring(start - prefix.length, start)
                val after = text.substring(start, start + suffix.length)
                if (before == prefix && after == suffix) {
                    // Remove empty markers
                    val newText = text.substring(0, start - prefix.length) + text.substring(start + suffix.length)
                    return TextFieldValue(newText, selection = TextRange(start - prefix.length))
                }
            }

            // Insert empty markers with cursor inside
            val newText = text.substring(0, start) + prefix + suffix + text.substring(start)
            return TextFieldValue(newText, selection = TextRange(start + prefix.length))
        }
    }

    private fun findWordBounds(text: String, offset: Int): Pair<Int, Int> {
        if (text.isEmpty()) return 0 to 0
        val clamped = offset.coerceIn(0, text.length)

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

    // --- Line-Based Formatting (Headings, Blockquotes, Lists, Checklists) ---

    fun toggleHeading(value: TextFieldValue, level: Int): TextFieldValue {
        val prefix = "#".repeat(level.coerceIn(1, 3)) + " "
        return transformSelectedLines(value) { line ->
            val stripped = stripLinePrefix(line)
            if (line.startsWith(prefix)) {
                // Toggle off: revert to plain line
                stripped
            } else {
                // Apply new heading level, replacing any existing heading or list prefix
                prefix + stripped
            }
        }
    }

    fun toggleBlockquote(value: TextFieldValue): TextFieldValue {
        val prefix = "> "
        return transformSelectedLines(value) { line ->
            if (line.startsWith(prefix)) {
                line.removePrefix(prefix)
            } else {
                prefix + stripLinePrefix(line)
            }
        }
    }

    fun toggleBulletList(value: TextFieldValue): TextFieldValue {
        val prefix = "- "
        return transformSelectedLines(value) { line ->
            if (line.startsWith(prefix)) {
                line.removePrefix(prefix)
            } else {
                prefix + stripLinePrefix(line)
            }
        }
    }

    fun toggleNumberedList(value: TextFieldValue): TextFieldValue {
        val lines = getSelectedLinesInfo(value)
        val text = value.text

        // Check if all lines are already numbered
        val allNumbered = lines.all { Regex("""^\s*\d+\.\s+""").containsMatchIn(it.text) }

        val newLines = lines.mapIndexed { index, lineInfo ->
            if (allNumbered) {
                lineInfo.text.replace(Regex("""^\s*\d+\.\s+"""), "")
            } else {
                val num = index + 1
                "$num. " + stripLinePrefix(lineInfo.text)
            }
        }

        return replaceLines(value, lines, newLines)
    }

    fun toggleChecklist(value: TextFieldValue): TextFieldValue {
        val prefix = "- [ ] "
        return transformSelectedLines(value) { line ->
            if (line.startsWith("- [ ] ") || line.startsWith("- [x] ") || line.startsWith("- [X] ")) {
                line.replace(Regex("""^-\s*\[[ xX]\]\s*"""), "")
            } else {
                prefix + stripLinePrefix(line)
            }
        }
    }

    // --- Enter Key & Backspace List Handlers ---

    fun handleEnterKey(value: TextFieldValue): TextFieldValue? {
        val text = value.text
        val sel = value.selection
        if (sel.min != sel.max) return null // selection active, let default replace it

        val cursor = sel.min.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', cursor).let { if (it == -1) text.length else it }
        val currentLine = text.substring(lineStart, lineEnd)

        // 1. Checklist
        val checkMatch = Regex("""^(\s*[-*+]\s*\[[ xX]\]\s*)(.*)$""").find(currentLine)
        if (checkMatch != null) {
            val prefix = checkMatch.groups[1]!!.value
            val content = checkMatch.groups[2]!!.value
            val indent = prefix.takeWhile { it.isWhitespace() }
            if (content.isBlank()) {
                // Exit checklist on empty line: clear marker
                val newText = text.substring(0, lineStart) + text.substring(lineEnd)
                return TextFieldValue(newText, selection = TextRange(lineStart))
            } else {
                val nextMarker = "$indent- [ ] "
                val newText = text.substring(0, cursor) + "\n" + nextMarker + text.substring(cursor)
                return TextFieldValue(newText, selection = TextRange(cursor + 1 + nextMarker.length))
            }
        }

        // 2. Numbered list
        val numMatch = Regex("""^(\s*)(\d+)\.\s+(.*)$""").find(currentLine)
        if (numMatch != null) {
            val indent = numMatch.groups[1]!!.value
            val currentNum = numMatch.groups[2]!!.value.toIntOrNull() ?: 1
            val content = numMatch.groups[3]!!.value
            if (content.isBlank()) {
                // Exit list on empty item
                val newText = text.substring(0, lineStart) + text.substring(lineEnd)
                return TextFieldValue(newText, selection = TextRange(lineStart))
            } else {
                val nextMarker = "$indent${currentNum + 1}. "
                val newText = text.substring(0, cursor) + "\n" + nextMarker + text.substring(cursor)
                return TextFieldValue(newText, selection = TextRange(cursor + 1 + nextMarker.length))
            }
        }

        // 3. Bullet list
        val bulletMatch = Regex("""^(\s*[-*+]\s+)(.*)$""").find(currentLine)
        if (bulletMatch != null) {
            val prefix = bulletMatch.groups[1]!!.value
            val content = bulletMatch.groups[2]!!.value
            if (content.isBlank()) {
                // Exit list
                val newText = text.substring(0, lineStart) + text.substring(lineEnd)
                return TextFieldValue(newText, selection = TextRange(lineStart))
            } else {
                val newText = text.substring(0, cursor) + "\n" + prefix + text.substring(cursor)
                return TextFieldValue(newText, selection = TextRange(cursor + 1 + prefix.length))
            }
        }

        // 4. Blockquote
        val quoteMatch = Regex("""^(\s*>\s+)(.*)$""").find(currentLine)
        if (quoteMatch != null) {
            val prefix = quoteMatch.groups[1]!!.value
            val content = quoteMatch.groups[2]!!.value
            if (content.isBlank()) {
                val newText = text.substring(0, lineStart) + text.substring(lineEnd)
                return TextFieldValue(newText, selection = TextRange(lineStart))
            } else {
                val newText = text.substring(0, cursor) + "\n" + prefix + text.substring(cursor)
                return TextFieldValue(newText, selection = TextRange(cursor + 1 + prefix.length))
            }
        }

        return null
    }

    fun handleBackspaceKey(value: TextFieldValue): TextFieldValue? {
        val text = value.text
        val sel = value.selection
        if (sel.min != sel.max) return null

        val cursor = sel.min.coerceIn(0, text.length)
        val lineStart = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val prefixLen = cursor - lineStart

        if (prefixLen <= 0) return null

        val candidate = text.substring(lineStart, cursor)
        val isListPrefix = candidate.matches(Regex("""^\s*[-*+]\s*\[[ xX]\]\s*$""")) ||
                candidate.matches(Regex("""^\s*\d+\.\s*$""")) ||
                candidate.matches(Regex("""^\s*[-*+]\s*$""")) ||
                candidate.matches(Regex("""^\s*>\s*$"""))

        if (isListPrefix) {
            val newText = text.substring(0, lineStart) + text.substring(cursor)
            return TextFieldValue(newText, selection = TextRange(lineStart))
        }

        return null
    }

    // --- Indent and Outdent ---

    fun indent(value: TextFieldValue): TextFieldValue {
        return transformSelectedLines(value) { line ->
            "  $line"
        }
    }

    fun outdent(value: TextFieldValue): TextFieldValue {
        return transformSelectedLines(value) { line ->
            line.removePrefix("  ").removePrefix(" ")
        }
    }

    // --- Code Block & Divider ---

    fun toggleCodeBlock(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val sel = value.selection
        val start = sel.min.coerceIn(0, text.length)
        val end = sel.max.coerceIn(0, text.length)

        if (start < end) {
            val selected = text.substring(start, end)
            if (selected.startsWith("```\n") && selected.endsWith("\n```")) {
                val unwrapped = selected.removePrefix("```\n").removeSuffix("\n```")
                val newText = text.substring(0, start) + unwrapped + text.substring(end)
                return TextFieldValue(newText, selection = TextRange(start, start + unwrapped.length))
            } else {
                val newText = text.substring(0, start) + "```\n" + selected + "\n```" + text.substring(end)
                return TextFieldValue(newText, selection = TextRange(start + 4, start + 4 + selected.length))
            }
        } else {
            val newText = text.substring(0, start) + "```\n\n```" + text.substring(start)
            return TextFieldValue(newText, selection = TextRange(start + 4))
        }
    }

    fun insertDivider(value: TextFieldValue): TextFieldValue {
        val text = value.text
        val sel = value.selection
        val start = sel.min.coerceIn(0, text.length)
        val end = sel.max.coerceIn(0, text.length)

        val before = if (start > 0 && text[start - 1] != '\n') "\n" else ""
        val after = if (end < text.length && text[end] != '\n') "\n" else ""
        val divider = "$before---\n$after"

        val newText = text.substring(0, start) + divider + text.substring(end)
        val newCursor = start + divider.length
        return TextFieldValue(newText, selection = TextRange(newCursor))
    }

    // --- Links ---

    fun insertOrEditLink(value: TextFieldValue, label: String, url: String): TextFieldValue {
        val text = value.text
        val sel = value.selection
        val start = sel.min.coerceIn(0, text.length)
        val end = sel.max.coerceIn(0, text.length)

        // Check if cursor is on an existing link
        val doc = MarkdownParser.parse(text)
        val existingLink = doc.findLinkAt(start)

        val cleanLabel = label.ifBlank { "link" }
        val cleanUrl = url.trim().ifBlank { "https://" }
        val markdownLink = "[$cleanLabel]($cleanUrl)"

        if (existingLink != null) {
            val newText = text.substring(0, existingLink.start) + markdownLink + text.substring(existingLink.end)
            return TextFieldValue(newText, selection = TextRange(existingLink.start + markdownLink.length))
        } else {
            val newText = text.substring(0, start) + markdownLink + text.substring(end)
            return TextFieldValue(newText, selection = TextRange(start + markdownLink.length))
        }
    }

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
            val lines = getSelectedLinesInfo(value)
            val newLines = lines.map { TextNote.stripMarkdownFormatting(it.text) }
            return replaceLines(value, lines, newLines)
        }
    }

    // --- Tappable Checklist Checkbox Toggle ---

    fun toggleChecklistAtOffset(value: TextFieldValue, charOffset: Int): TextFieldValue {
        val text = value.text
        if (charOffset !in text.indices) return value

        val lineStart = text.lastIndexOf('\n', (charOffset - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lineEnd = text.indexOf('\n', charOffset).let { if (it == -1) text.length else it }
        val line = text.substring(lineStart, lineEnd)

        val uncheckedRegex = Regex("""^(\s*[-*+]\s*)\[\s\](\s*)""")
        val checkedRegex = Regex("""^(\s*[-*+]\s*)\[[xX]\](\s*)""")

        val unMatch = uncheckedRegex.find(line)
        if (unMatch != null) {
            val newLine = line.replaceFirst("[\u0020]", "[x]")
            val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
            return value.copy(text = newText)
        }

        val chMatch = checkedRegex.find(line)
        if (chMatch != null) {
            val newLine = line.replaceFirst(Regex("""\[[xX]\]"""), "[ ]")
            val newText = text.substring(0, lineStart) + newLine + text.substring(lineEnd)
            return value.copy(text = newText)
        }

        return value
    }

    // --- Private Helper Utilities ---

    private fun stripLinePrefix(line: String): String {
        return line
            .replace(Regex("""^#{1,6}\s+"""), "")
            .replace(Regex("""^>\s+"""), "")
            .replace(Regex("""^[-*+]\s*\[[ xX]\]\s*"""), "")
            .replace(Regex("""^[-*+]\s+"""), "")
            .replace(Regex("""^\d+\.\s+"""), "")
    }

    private data class LineInfo(
        val lineIndex: Int,
        val start: Int,
        val end: Int,
        val text: String
    )

    private fun getSelectedLinesInfo(value: TextFieldValue): List<LineInfo> {
        val text = value.text
        val sel = value.selection
        val selStart = sel.min.coerceIn(0, text.length)
        val selEnd = sel.max.coerceIn(0, text.length)

        val firstLineStart = text.lastIndexOf('\n', (selStart - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val lastLineEnd = if (selEnd == selStart) {
            text.indexOf('\n', selEnd).let { if (it == -1) text.length else it }
        } else {
            val endSearch = (selEnd - 1).coerceAtLeast(0)
            text.indexOf('\n', endSearch).let { if (it == -1) text.length else it }
        }

        val lines = mutableListOf<LineInfo>()
        var cur = firstLineStart
        var idx = 0
        while (cur <= lastLineEnd && cur <= text.length) {
            val nxt = text.indexOf('\n', cur).let { if (it == -1 || it > lastLineEnd) lastLineEnd else it }
            lines.add(LineInfo(idx++, cur, nxt, text.substring(cur, nxt)))
            cur = nxt + 1
            if (cur > lastLineEnd) break
        }

        return lines
    }

    private fun transformSelectedLines(value: TextFieldValue, transform: (String) -> String): TextFieldValue {
        val lines = getSelectedLinesInfo(value)
        val newLines = lines.map { transform(it.text) }
        return replaceLines(value, lines, newLines)
    }

    private fun replaceLines(value: TextFieldValue, oldLines: List<LineInfo>, newLines: List<String>): TextFieldValue {
        if (oldLines.isEmpty()) return value
        val text = value.text
        val blockStart = oldLines.first().start
        val blockEnd = oldLines.last().end

        val joinedNew = newLines.joinToString("\n")
        val newText = text.substring(0, blockStart) + joinedNew + text.substring(blockEnd)

        // Adjust selection
        val lengthDiff = joinedNew.length - (blockEnd - blockStart)
        val newSelStart = (value.selection.start + (if (value.selection.start > blockStart) lengthDiff else 0)).coerceIn(0, newText.length)
        val newSelEnd = (value.selection.end + (if (value.selection.end > blockStart) lengthDiff else 0)).coerceIn(0, newText.length)

        return TextFieldValue(newText, selection = TextRange(newSelStart, newSelEnd))
    }
}
