// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.editor

enum class MarkdownSpanType {
    HEADING_1,
    HEADING_2,
    HEADING_3,
    BOLD,
    ITALIC,
    BOLD_ITALIC,
    STRIKETHROUGH,
    INLINE_CODE,
    CODE_BLOCK,
    BLOCKQUOTE,
    BULLET_LIST,
    NUMBERED_LIST,
    CHECKLIST_UNCHECKED,
    CHECKLIST_CHECKED,
    LINK,
    HORIZONTAL_RULE
}

data class MarkdownSpan(
    val type: MarkdownSpanType,
    val start: Int,
    val end: Int,
    val contentStart: Int,
    val contentEnd: Int,
    val markerStartLen: Int,
    val markerEndLen: Int = 0,
    val extra: String? = null
) {
    val fullRange: IntRange get() = start until end
    val contentRange: IntRange get() = contentStart until contentEnd

    fun containsOffset(offset: Int): Boolean = offset in start..end
    fun touchesSelection(selStart: Int, selEnd: Int): Boolean {
        val min = minOf(selStart, selEnd)
        val max = maxOf(selStart, selEnd)
        if (min == max) {
            return min in start..end
        }
        return max > start && min < end
    }
}

data class MarkdownDocument(
    val rawText: String,
    val spans: List<MarkdownSpan>
) {
    fun activeTypesAt(cursorStart: Int, cursorEnd: Int): Set<MarkdownSpanType> {
        val set = mutableSetOf<MarkdownSpanType>()
        for (span in spans) {
            if (span.touchesSelection(cursorStart, cursorEnd)) {
                set.add(span.type)
            }
        }
        return set
    }

    fun findLinkAt(offset: Int): MarkdownSpan? {
        return spans.firstOrNull { it.type == MarkdownSpanType.LINK && offset in it.start..it.end }
    }
}

/**
 * Pure Kotlin Markdown parser for editor live rendering, syntax highlighting,
 * and toolbar active-state detection.
 */
object MarkdownParser {

    private val CODE_BLOCK_REGEX = Regex("""(?m)^```[^\n]*\n([\s\S]*?)\n```$""")
    private val HORIZONTAL_RULE_REGEX = Regex("""(?m)^---+$""")
    private val HEADING_1_REGEX = Regex("""(?m)^# (.*)$""")
    private val HEADING_2_REGEX = Regex("""(?m)^## (.*)$""")
    private val HEADING_3_REGEX = Regex("""(?m)^### (.*)$""")
    private val BLOCKQUOTE_REGEX = Regex("""(?m)^> (.*)$""")
    private val CHECKLIST_UNCHECKED_REGEX = Regex("""(?m)^(\s*[-*+]) \[\s\] (.*)$""")
    private val CHECKLIST_CHECKED_REGEX = Regex("""(?m)^(\s*[-*+]) \[[xX]\] (.*)$""")
    private val BULLET_LIST_REGEX = Regex("""(?m)^(\s*[-*+]) (?!\[[ xX]\])(.*)$""")
    private val NUMBERED_LIST_REGEX = Regex("""(?m)^(\s*\d+\.) (.*)$""")

    // Inline regexes
    private val BOLD_ITALIC_STAR_REGEX = Regex("""\*\*\*([^*\n]+?)\*\*\*""")
    private val BOLD_ITALIC_UNDERSCORE_REGEX = Regex("""___([^_\n]+?)___""")
    private val BOLD_STAR_REGEX = Regex("""\*\*([^*\n]+?)\*\*""")
    private val BOLD_UNDERSCORE_REGEX = Regex("""__([^_\n]+?)__""")
    private val ITALIC_STAR_REGEX = Regex("""(?<!\*)\*([^*\n]+?)\*(?!\*)""")
    private val ITALIC_UNDERSCORE_REGEX = Regex("""(?<!_)_([^_\n]+?)_(?!_)""")
    private val STRIKETHROUGH_REGEX = Regex("""~~([^~\n]+?)~~""")
    private val INLINE_CODE_REGEX = Regex("""`([^`\n]+?)`""")
    private val LINK_REGEX = Regex("""\[([^\]\n]+?)\]\(([^)\n]+?)\)""")

    fun parse(text: String): MarkdownDocument {
        if (text.isEmpty()) {
            return MarkdownDocument("", emptyList())
        }

        val spans = mutableListOf<MarkdownSpan>()

        // 1. Block: Fenced Code Blocks
        for (match in CODE_BLOCK_REGEX.findAll(text)) {
            val totalStart = match.range.first
            val totalEnd = match.range.last + 1
            val contentGroup = match.groups[1]
            val contentStart = contentGroup?.range?.first ?: (totalStart + 4)
            val contentEnd = contentGroup?.range?.last?.plus(1) ?: (totalEnd - 4)
            spans.add(
                MarkdownSpan(
                    type = MarkdownSpanType.CODE_BLOCK,
                    start = totalStart,
                    end = totalEnd,
                    contentStart = contentStart,
                    contentEnd = contentEnd,
                    markerStartLen = contentStart - totalStart,
                    markerEndLen = totalEnd - contentEnd
                )
            )
        }

        // Helper to check if a range overlaps an already-parsed code block
        fun isInsideCodeBlock(start: Int, end: Int): Boolean {
            return spans.any { it.type == MarkdownSpanType.CODE_BLOCK && (start in it.start until it.end || end in (it.start + 1)..it.end) }
        }

        // 2. Block: Horizontal Rules
        for (match in HORIZONTAL_RULE_REGEX.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!isInsideCodeBlock(start, end)) {
                spans.add(
                    MarkdownSpan(
                        type = MarkdownSpanType.HORIZONTAL_RULE,
                        start = start,
                        end = end,
                        contentStart = start,
                        contentEnd = end,
                        markerStartLen = end - start
                    )
                )
            }
        }

        // 3. Block: Headings (H3 before H2 before H1)
        for (match in HEADING_3_REGEX.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!isInsideCodeBlock(start, end)) {
                val contentGroup = match.groups[1]
                val cStart = contentGroup?.range?.first ?: (start + 4)
                val cEnd = contentGroup?.range?.last?.plus(1) ?: end
                spans.add(
                    MarkdownSpan(
                        type = MarkdownSpanType.HEADING_3,
                        start = start,
                        end = end,
                        contentStart = cStart,
                        contentEnd = cEnd,
                        markerStartLen = cStart - start
                    )
                )
            }
        }

        for (match in HEADING_2_REGEX.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!isInsideCodeBlock(start, end) && spans.none { it.start == start }) {
                val contentGroup = match.groups[1]
                val cStart = contentGroup?.range?.first ?: (start + 3)
                val cEnd = contentGroup?.range?.last?.plus(1) ?: end
                spans.add(
                    MarkdownSpan(
                        type = MarkdownSpanType.HEADING_2,
                        start = start,
                        end = end,
                        contentStart = cStart,
                        contentEnd = cEnd,
                        markerStartLen = cStart - start
                    )
                )
            }
        }

        for (match in HEADING_1_REGEX.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!isInsideCodeBlock(start, end) && spans.none { it.start == start }) {
                val contentGroup = match.groups[1]
                val cStart = contentGroup?.range?.first ?: (start + 2)
                val cEnd = contentGroup?.range?.last?.plus(1) ?: end
                spans.add(
                    MarkdownSpan(
                        type = MarkdownSpanType.HEADING_1,
                        start = start,
                        end = end,
                        contentStart = cStart,
                        contentEnd = cEnd,
                        markerStartLen = cStart - start
                    )
                )
            }
        }

        // 4. Block: Blockquotes
        for (match in BLOCKQUOTE_REGEX.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!isInsideCodeBlock(start, end)) {
                val contentGroup = match.groups[1]
                val cStart = contentGroup?.range?.first ?: (start + 2)
                val cEnd = contentGroup?.range?.last?.plus(1) ?: end
                spans.add(
                    MarkdownSpan(
                        type = MarkdownSpanType.BLOCKQUOTE,
                        start = start,
                        end = end,
                        contentStart = cStart,
                        contentEnd = cEnd,
                        markerStartLen = cStart - start
                    )
                )
            }
        }

        // 5. Block: Checklists (Unchecked & Checked)
        for (match in CHECKLIST_UNCHECKED_REGEX.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!isInsideCodeBlock(start, end)) {
                val contentGroup = match.groups[2]
                val cStart = contentGroup?.range?.first ?: (start + 6)
                val cEnd = contentGroup?.range?.last?.plus(1) ?: end
                spans.add(
                    MarkdownSpan(
                        type = MarkdownSpanType.CHECKLIST_UNCHECKED,
                        start = start,
                        end = end,
                        contentStart = cStart,
                        contentEnd = cEnd,
                        markerStartLen = cStart - start,
                        extra = "unchecked"
                    )
                )
            }
        }

        for (match in CHECKLIST_CHECKED_REGEX.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!isInsideCodeBlock(start, end)) {
                val contentGroup = match.groups[2]
                val cStart = contentGroup?.range?.first ?: (start + 6)
                val cEnd = contentGroup?.range?.last?.plus(1) ?: end
                spans.add(
                    MarkdownSpan(
                        type = MarkdownSpanType.CHECKLIST_CHECKED,
                        start = start,
                        end = end,
                        contentStart = cStart,
                        contentEnd = cEnd,
                        markerStartLen = cStart - start,
                        extra = "checked"
                    )
                )
            }
        }

        // 6. Block: Bullet Lists
        for (match in BULLET_LIST_REGEX.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!isInsideCodeBlock(start, end) && spans.none { it.start == start }) {
                val contentGroup = match.groups[2]
                val cStart = contentGroup?.range?.first ?: (start + 2)
                val cEnd = contentGroup?.range?.last?.plus(1) ?: end
                spans.add(
                    MarkdownSpan(
                        type = MarkdownSpanType.BULLET_LIST,
                        start = start,
                        end = end,
                        contentStart = cStart,
                        contentEnd = cEnd,
                        markerStartLen = cStart - start
                    )
                )
            }
        }

        // 7. Block: Numbered Lists
        for (match in NUMBERED_LIST_REGEX.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!isInsideCodeBlock(start, end)) {
                val contentGroup = match.groups[2]
                val cStart = contentGroup?.range?.first ?: (start + 3)
                val cEnd = contentGroup?.range?.last?.plus(1) ?: end
                spans.add(
                    MarkdownSpan(
                        type = MarkdownSpanType.NUMBERED_LIST,
                        start = start,
                        end = end,
                        contentStart = cStart,
                        contentEnd = cEnd,
                        markerStartLen = cStart - start
                    )
                )
            }
        }

        // 8. Inline Spans: Bold Italic, Bold, Italic, Strikethrough, Code, Links
        fun addInline(regex: Regex, type: MarkdownSpanType, markerLen: Int) {
            for (match in regex.findAll(text)) {
                val start = match.range.first
                val end = match.range.last + 1
                if (!isInsideCodeBlock(start, end)) {
                    spans.add(
                        MarkdownSpan(
                            type = type,
                            start = start,
                            end = end,
                            contentStart = start + markerLen,
                            contentEnd = end - markerLen,
                            markerStartLen = markerLen,
                            markerEndLen = markerLen
                        )
                    )
                }
            }
        }

        // Bold Italic (3 markers)
        addInline(BOLD_ITALIC_STAR_REGEX, MarkdownSpanType.BOLD_ITALIC, 3)
        addInline(BOLD_ITALIC_UNDERSCORE_REGEX, MarkdownSpanType.BOLD_ITALIC, 3)

        // Bold (2 markers)
        addInline(BOLD_STAR_REGEX, MarkdownSpanType.BOLD, 2)
        addInline(BOLD_UNDERSCORE_REGEX, MarkdownSpanType.BOLD, 2)

        // Italic (1 marker)
        addInline(ITALIC_STAR_REGEX, MarkdownSpanType.ITALIC, 1)
        addInline(ITALIC_UNDERSCORE_REGEX, MarkdownSpanType.ITALIC, 1)

        // Strikethrough (2 markers)
        addInline(STRIKETHROUGH_REGEX, MarkdownSpanType.STRIKETHROUGH, 2)

        // Inline Code (1 marker)
        addInline(INLINE_CODE_REGEX, MarkdownSpanType.INLINE_CODE, 1)

        // Links
        for (match in LINK_REGEX.findAll(text)) {
            val start = match.range.first
            val end = match.range.last + 1
            if (!isInsideCodeBlock(start, end)) {
                val labelGroup = match.groups[1]
                val urlGroup = match.groups[2]
                val label = labelGroup?.value ?: ""
                val url = urlGroup?.value ?: ""
                val cStart = labelGroup?.range?.first ?: (start + 1)
                val cEnd = labelGroup?.range?.last?.plus(1) ?: (start + 1 + label.length)
                spans.add(
                    MarkdownSpan(
                        type = MarkdownSpanType.LINK,
                        start = start,
                        end = end,
                        contentStart = cStart,
                        contentEnd = cEnd,
                        markerStartLen = 1,
                        markerEndLen = end - cEnd,
                        extra = url
                    )
                )
            }
        }

        // Sort spans by start index
        spans.sortBy { it.start }

        return MarkdownDocument(text, spans)
    }
}
