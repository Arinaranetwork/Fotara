// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.note.editor

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.FolderTabCream
import com.arinara.fotara.theme.HomeAddButtonBlue
import com.arinara.fotara.theme.HomeSearchBarSurface
import com.arinara.fotara.theme.TextMuted
import com.arinara.fotara.theme.TextPrimary

/**
 * VisualTransformation providing "Notion-lite" live interactive preview in the Markdown editor:
 * 1. Block prefixes are NEVER shown as raw text:
 *    - Bullet list: "• "
 *    - Numbered list: "1. ", "2. "...
 *    - Checkbox: "☐ " (unchecked) / "☑ " (checked, with dimmed strikethrough text)
 *    - Quote: "▎ " vertical bar
 *    - Divider: "────────────────────────"
 *    - Headings H1-H3 sized without raw '#' prefixes
 *    Caret is pinned to content boundary and can NEVER be placed inside brackets or prefixes.
 * 2. Inline marks (**bold**, *italic*, ~~strike~~, `code`, [text](url)):
 *    Hidden when caret is outside the span; shown/dimmed when caret is inside the span for editing.
 */
class MarkdownVisualTransformation(
    val cursorStart: Int,
    val cursorEnd: Int,
    val hideUntouchedMarkers: Boolean = true,
    val textColor: Color = FolderTabCream,
    val accentColor: Color = HomeAddButtonBlue,
    val codeBgColor: Color = HomeSearchBarSurface,
    val codeTextColor: Color = TextPrimary,
    val linkColor: Color = HomeAddButtonBlue,
    val mutedColor: Color = TextMuted
) : VisualTransformation {

    var lastOffsetMapping: OffsetMapping = OffsetMapping.Identity
        private set

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val cMin = minOf(cursorStart, cursorEnd).coerceIn(0, raw.length)
        val cMax = maxOf(cursorStart, cursorEnd).coerceIn(0, raw.length)

        val doc = MarkdownParser.parse(raw)

        val chunks = mutableListOf<TextMappingChunk>()
        val styles = mutableListOf<Pair<SpanStyle, IntRange>>()
        val sb = StringBuilder()

        // Find line boundaries
        val lineRanges = mutableListOf<Pair<Int, Int>>()
        var searchPos = 0
        while (searchPos <= raw.length) {
            val nextNl = raw.indexOf('\n', searchPos)
            if (nextNl == -1) {
                lineRanges.add(searchPos to raw.length)
                break
            } else {
                lineRanges.add(searchPos to nextNl)
                searchPos = nextNl + 1
            }
        }

        var lineIndex = 0
        for ((lineStart, lineEnd) in lineRanges) {
            val lineText = raw.substring(lineStart, lineEnd)
            val tLineStart = sb.length

            val isInsideCodeBlock = doc.spans.any {
                it.type == MarkdownSpanType.CODE_BLOCK && lineStart >= it.contentStart && lineEnd <= it.contentEnd
            }

            val isDivider = (lineText.trim() == "---" || lineText.trim() == "***") && !isInsideCodeBlock

            if (isDivider) {
                // Divider line: replace with clean horizontal rule sprite
                val dividerSprite = "────────────────────────"
                sb.append(dividerSprite)
                val tDividerEnd = sb.length
                chunks.add(TextMappingChunk(lineStart, lineEnd, tLineStart, tDividerEnd, isAtomicPrefix = true))
                styles.add(SpanStyle(color = mutedColor.copy(alpha = 0.5f), fontWeight = FontWeight.Bold) to (tLineStart until tDividerEnd))
            } else if (isInsideCodeBlock) {
                // Code block interior: 1:1 monospace styling
                sb.append(lineText)
                chunks.add(TextMappingChunk(lineStart, lineEnd, tLineStart, sb.length))
                styles.add(
                    SpanStyle(
                        fontFamily = ElmsSans,
                        background = codeBgColor,
                        color = codeTextColor,
                        fontSize = 14.sp
                    ) to (tLineStart until sb.length)
                )
            } else {
                val parsed = TextEditorOps.parseLine(lineIndex, lineStart, lineEnd, lineText)

                // 1. Indent with ~24dp visual indentation per level
                if (parsed.indent.isNotEmpty()) {
                    val tIndentStart = sb.length
                    val levels = (parsed.indent.length / 2).coerceIn(1, 3)
                    val visualIndent = "\u2003\u2002".repeat(levels)
                    sb.append(visualIndent)
                    chunks.add(TextMappingChunk(lineStart, lineStart + parsed.indent.length, tIndentStart, sb.length))
                }

                // 2. Prefix Sprite
                val prefixOrigStart = lineStart + parsed.indent.length
                val prefixOrigEnd = prefixOrigStart + parsed.prefixString.length
                val tPrefixStart = sb.length

                when (parsed.prefixType) {
                    LineToolType.CHECKBOX -> {
                        // 4 spaces (~24dp width) placeholder for the real drawn vector checkbox
                        val boxPlaceholder = "    "
                        sb.append(boxPlaceholder)
                        styles.add(SpanStyle(color = Color.Transparent) to (tPrefixStart until sb.length))
                        chunks.add(TextMappingChunk(prefixOrigStart, prefixOrigEnd, tPrefixStart, sb.length, isAtomicPrefix = true))
                    }
                    LineToolType.NUMBERED_LIST -> {
                        val numStr = Regex("""^\d+""").find(parsed.prefixString)?.value ?: "1"
                        sb.append("$numStr. ")
                        styles.add(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold) to (tPrefixStart until sb.length))
                        chunks.add(TextMappingChunk(prefixOrigStart, prefixOrigEnd, tPrefixStart, sb.length, isAtomicPrefix = true))
                    }
                    LineToolType.BULLET_LIST -> {
                        sb.append("• ")
                        styles.add(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold) to (tPrefixStart until sb.length))
                        chunks.add(TextMappingChunk(prefixOrigStart, prefixOrigEnd, tPrefixStart, sb.length, isAtomicPrefix = true))
                    }
                    LineToolType.QUOTE -> {
                        sb.append("▎ ")
                        styles.add(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold) to (tPrefixStart until sb.length))
                        chunks.add(TextMappingChunk(prefixOrigStart, prefixOrigEnd, tPrefixStart, sb.length, isAtomicPrefix = true))
                    }
                    LineToolType.H1, LineToolType.H2, LineToolType.H3 -> {
                        // Headings: prefix markers (#, ##, ###) hidden
                        chunks.add(TextMappingChunk(prefixOrigStart, prefixOrigEnd, tPrefixStart, tPrefixStart, isAtomicPrefix = true))
                    }
                    null -> {
                        // Plain text
                    }
                }

                // 3. Content with Inline Formatting (delimiters hidden when untouched)
                val contentStart = prefixOrigEnd
                val contentEnd = lineEnd
                val tContentStart = sb.length

                val lineInlineSpans = doc.spans.filter {
                    it.start >= contentStart && it.end <= contentEnd && it.type.isInline
                }

                // Filter to non-overlapping spans
                val nonOverlapping = mutableListOf<MarkdownSpan>()
                var lastSpanEnd = -1
                for (span in lineInlineSpans) {
                    if (span.start >= lastSpanEnd) {
                        nonOverlapping.add(span)
                        lastSpanEnd = span.end
                    }
                }

                var curPos = contentStart
                for (span in nonOverlapping) {
                    if (span.start > curPos) {
                        val tBeforeStart = sb.length
                        sb.append(raw.substring(curPos, span.start))
                        chunks.add(TextMappingChunk(curPos, span.start, tBeforeStart, sb.length))
                    }

                    val isTouched = span.touchesSelection(cMin, cMax)
                    if (isTouched || !hideUntouchedMarkers) {
                        // Caret is inside span: show markers so user can edit them
                        val tSpanStart = sb.length
                        sb.append(raw.substring(span.start, span.end))
                        chunks.add(TextMappingChunk(span.start, span.end, tSpanStart, sb.length))

                        // Dim the delimiter markers
                        val dimmedStyle = SpanStyle(color = mutedColor.copy(alpha = 0.5f), fontSize = 11.5.sp)
                        if (span.markerStartLen > 0) {
                            styles.add(dimmedStyle to (tSpanStart until (tSpanStart + span.markerStartLen)))
                        }
                        if (span.markerEndLen > 0) {
                            val tEndMarkerStart = tSpanStart + (span.end - span.start) - span.markerEndLen
                            styles.add(dimmedStyle to (tEndMarkerStart until (tSpanStart + (span.end - span.start))))
                        }

                        val spanStyle = getInlineSpanStyle(span.type, textColor, codeBgColor, codeTextColor, linkColor)
                        if (spanStyle != null) {
                            val tCStart = tSpanStart + span.markerStartLen
                            val tCEnd = tSpanStart + (span.end - span.start) - span.markerEndLen
                            if (tCStart < tCEnd) {
                                styles.add(spanStyle to (tCStart until tCEnd))
                            }
                        }
                    } else {
                        // Caret is outside span: HIDE the markers!
                        val tSpanStart = sb.length
                        // 1. Hide opening marker
                        chunks.add(TextMappingChunk(span.start, span.contentStart, tSpanStart, tSpanStart))

                        // 2. Visible content
                        val rawContent = raw.substring(span.contentStart, span.contentEnd)
                        val spanContent = if (span.type == MarkdownSpanType.MATH_INLINE) {
                            KatexMathRenderer.formatToReadableMath(rawContent)
                        } else {
                            rawContent
                        }
                        val tVisibleStart = sb.length
                        sb.append(spanContent)
                        val tVisibleEnd = sb.length
                        if (spanContent.isNotEmpty()) {
                            chunks.add(TextMappingChunk(span.contentStart, span.contentEnd, tVisibleStart, tVisibleEnd))
                            val spanStyle = getInlineSpanStyle(span.type, textColor, codeBgColor, codeTextColor, linkColor)
                            if (spanStyle != null) {
                                styles.add(spanStyle to (tVisibleStart until tVisibleEnd))
                            }
                        }

                        // 3. Hide closing marker
                        chunks.add(TextMappingChunk(span.contentEnd, span.end, tVisibleEnd, tVisibleEnd))
                    }

                    curPos = span.end
                }

                if (curPos < contentEnd) {
                    val tTrailingStart = sb.length
                    sb.append(raw.substring(curPos, contentEnd))
                    chunks.add(TextMappingChunk(curPos, contentEnd, tTrailingStart, sb.length))
                }

                val tContentEnd = sb.length

                // Apply line-level block styles
                if (parsed.prefixType == LineToolType.CHECKBOX && parsed.prefixString.contains(Regex("""\[[xX]\]"""))) {
                    if (tContentStart < tContentEnd) {
                        styles.add(SpanStyle(color = mutedColor, textDecoration = TextDecoration.LineThrough) to (tContentStart until tContentEnd))
                    }
                } else if (parsed.prefixType == LineToolType.H1) {
                    if (tContentStart < tContentEnd) {
                        styles.add(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accentColor) to (tContentStart until tContentEnd))
                    }
                } else if (parsed.prefixType == LineToolType.H2) {
                    if (tContentStart < tContentEnd) {
                        styles.add(SpanStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold, color = textColor) to (tContentStart until tContentEnd))
                    }
                } else if (parsed.prefixType == LineToolType.H3) {
                    if (tContentStart < tContentEnd) {
                        styles.add(SpanStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textColor) to (tContentStart until tContentEnd))
                    }
                } else if (parsed.prefixType == LineToolType.QUOTE) {
                    if (tContentStart < tContentEnd) {
                        styles.add(SpanStyle(color = mutedColor) to (tContentStart until tContentEnd))
                    }
                }
            }

            // Newline
            if (lineEnd < raw.length && raw[lineEnd] == '\n') {
                val tNl = sb.length
                sb.append('\n')
                chunks.add(TextMappingChunk(lineEnd, lineEnd + 1, tNl, tNl + 1))
            }

            lineIndex++
        }

        val transformedString = sb.toString()
        val offsetMapping = MarkdownOffsetMapping(raw.length, transformedString.length, chunks)
        lastOffsetMapping = offsetMapping

        val annotated = buildAnnotatedString {
            append(transformedString)
            for ((spanStyle, range) in styles) {
                val cStart = range.first.coerceIn(0, length)
                val cEnd = (range.last + 1).coerceIn(cStart, length)
                if (cStart < cEnd) {
                    addStyle(spanStyle, cStart, cEnd)
                }
            }
        }

        return TransformedText(annotated, offsetMapping)
    }

    private fun getInlineSpanStyle(
        type: MarkdownSpanType,
        textColor: Color,
        codeBgColor: Color,
        codeTextColor: Color,
        linkColor: Color
    ): SpanStyle? = when (type) {
        MarkdownSpanType.BOLD -> SpanStyle(fontWeight = FontWeight.Bold, color = textColor)
        MarkdownSpanType.ITALIC -> SpanStyle(fontWeight = FontWeight.Medium, color = FolderTabCream)
        MarkdownSpanType.BOLD_ITALIC -> SpanStyle(fontWeight = FontWeight.Bold, color = FolderTabCream)
        MarkdownSpanType.STRIKETHROUGH -> SpanStyle(textDecoration = TextDecoration.LineThrough)
        MarkdownSpanType.INLINE_CODE -> SpanStyle(
            fontFamily = ElmsSans,
            background = codeBgColor,
            color = codeTextColor,
            fontSize = 14.sp
        )
        MarkdownSpanType.LINK -> SpanStyle(
            fontFamily = ElmsSans,
            color = linkColor,
            textDecoration = TextDecoration.Underline
        )
        MarkdownSpanType.MATH_INLINE -> SpanStyle(
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Medium,
            background = codeBgColor,
            color = linkColor,
            fontSize = 14.sp
        )
        MarkdownSpanType.MATH_BLOCK -> SpanStyle(
            fontFamily = ElmsSans,
            fontWeight = FontWeight.Bold,
            background = codeBgColor,
            color = linkColor,
            fontSize = 16.sp
        )
        else -> null
    }

    private val MarkdownSpanType.isInline: Boolean
        get() = this == MarkdownSpanType.BOLD ||
                this == MarkdownSpanType.ITALIC ||
                this == MarkdownSpanType.BOLD_ITALIC ||
                this == MarkdownSpanType.STRIKETHROUGH ||
                this == MarkdownSpanType.INLINE_CODE ||
                this == MarkdownSpanType.LINK ||
                this == MarkdownSpanType.MATH_INLINE
}
