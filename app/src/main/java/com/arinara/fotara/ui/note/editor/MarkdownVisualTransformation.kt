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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

/**
 * VisualTransformation providing rich interactive live-preview rendering in the Markdown editor.
 *
 * Rules:
 * 1. Active line (where the cursor or selection is) displays raw markers for natural editing,
 *    guaranteeing a 1:1 identity [OffsetMapping] on the active line so the caret never jumps.
 * 2. Inactive lines display real visual elements (sprites):
 *    - Checkbox: "☐ " / "☑ " with strikethrough and dimmed content when checked.
 *    - Horizontal rule: "───" divider styling.
 *    - Bullet list: "• " clean bullet glyph.
 *    - Blockquote: "▎ " vertical bar styling without italics.
 *    - Inline styles: syntax markers hidden, formatted with bold/italic/code/strike/link.
 */
class MarkdownVisualTransformation(
    val cursorStart: Int,
    val cursorEnd: Int,
    val hideUntouchedMarkers: Boolean = true,
    val textColor: Color = Color(0xFFEAE3D2),
    val accentColor: Color = Color(0xFFF77F00),
    val codeBgColor: Color = Color(0xFF1E254A),
    val codeTextColor: Color = Color(0xFFE2E8F0),
    val linkColor: Color = Color(0xFF64B5F6),
    val mutedColor: Color = Color(0xFF8E9AAF)
) : VisualTransformation {

    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val cMin = minOf(cursorStart, cursorEnd).coerceIn(0, raw.length)
        val cMax = maxOf(cursorStart, cursorEnd).coerceIn(0, raw.length)

        // Find active line boundaries touched by the caret or selection
        val activeLineStart = raw.lastIndexOf('\n', (cMin - 1).coerceAtLeast(0)).let { if (it == -1) 0 else it + 1 }
        val activeLineEnd = if (cMin == cMax) {
            raw.indexOf('\n', cMax).let { if (it == -1) raw.length else it }
        } else {
            val endSearch = (cMax - 1).coerceAtLeast(cMin)
            raw.indexOf('\n', endSearch).let { if (it == -1) raw.length else it }
        }

        val chunks = mutableListOf<TextMappingChunk>()
        val styles = mutableListOf<Pair<SpanStyle, IntRange>>()

        val sb = StringBuilder()
        var curRaw = 0

        // Parse lines sequentially
        val lineEnds = mutableListOf<Int>()
        var searchPos = 0
        while (searchPos <= raw.length) {
            val nextNl = raw.indexOf('\n', searchPos)
            if (nextNl == -1) {
                lineEnds.add(raw.length)
                break
            } else {
                lineEnds.add(nextNl)
                searchPos = nextNl + 1
            }
        }

        var lineStart = 0
        for (lineEnd in lineEnds) {
            val lineLen = lineEnd - lineStart
            val lineText = raw.substring(lineStart, lineEnd)
            val isActiveLine = (lineStart <= activeLineEnd && lineEnd >= activeLineStart)

            val tLineStart = sb.length

            if (isActiveLine || !hideUntouchedMarkers) {
                // Active line: keep characters 1:1, full identity mapping
                sb.append(lineText)
                chunks.add(TextMappingChunk(lineStart, lineEnd, tLineStart, sb.length))
            } else {
                // Inactive line: replace markdown prefixes with rich sprites
                val trimmed = lineText.trimStart()
                val leadingSpaces = lineText.length - trimmed.length
                val indentStr = lineText.substring(0, leadingSpaces)

                when {
                    trimmed.startsWith("- [ ] ") || trimmed.startsWith("* [ ] ") -> {
                        // Unchecked checkbox
                        sb.append(indentStr)
                        val tBoxStart = sb.length
                        sb.append("☐ ")
                        val tBoxEnd = sb.length
                        styles.add(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold) to (tBoxStart until tBoxEnd))

                        val content = trimmed.substring(6)
                        sb.append(content)

                        // Mapped chunk: original [lineStart..lineStart+leadingSpaces+6] -> transformed [tLineStart..tBoxEnd]
                        chunks.add(TextMappingChunk(lineStart, lineStart + leadingSpaces + 6, tLineStart, tBoxEnd))
                        if (content.isNotEmpty()) {
                            chunks.add(TextMappingChunk(lineStart + leadingSpaces + 6, lineEnd, tBoxEnd, sb.length))
                        }
                    }
                    trimmed.startsWith("- [x] ") || trimmed.startsWith("- [X] ") ||
                    trimmed.startsWith("* [x] ") || trimmed.startsWith("* [X] ") -> {
                        // Checked checkbox
                        sb.append(indentStr)
                        val tBoxStart = sb.length
                        sb.append("☑ ")
                        val tBoxEnd = sb.length
                        styles.add(SpanStyle(color = accentColor.copy(alpha = 0.65f), fontWeight = FontWeight.Bold) to (tBoxStart until tBoxEnd))

                        val content = trimmed.substring(6)
                        val tContentStart = sb.length
                        sb.append(content)
                        val tContentEnd = sb.length

                        // Style content as strikethrough & dimmed
                        styles.add(SpanStyle(color = mutedColor, textDecoration = TextDecoration.LineThrough) to (tContentStart until tContentEnd))

                        chunks.add(TextMappingChunk(lineStart, lineStart + leadingSpaces + 6, tLineStart, tBoxEnd))
                        if (content.isNotEmpty()) {
                            chunks.add(TextMappingChunk(lineStart + leadingSpaces + 6, lineEnd, tBoxEnd, sb.length))
                        }
                    }
                    trimmed == "---" || trimmed == "***" -> {
                        // Horizontal divider
                        sb.append("──────────────────────────────────")
                        styles.add(SpanStyle(color = mutedColor.copy(alpha = 0.5f), fontWeight = FontWeight.Bold) to (tLineStart until sb.length))
                        chunks.add(TextMappingChunk(lineStart, lineEnd, tLineStart, sb.length))
                    }
                    trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                        // Bullet list
                        sb.append(indentStr)
                        val tBulletStart = sb.length
                        sb.append("• ")
                        val tBulletEnd = sb.length
                        styles.add(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold) to (tBulletStart until tBulletEnd))

                        val content = trimmed.substring(2)
                        sb.append(content)
                        chunks.add(TextMappingChunk(lineStart, lineStart + leadingSpaces + 2, tLineStart, tBulletEnd))
                        if (content.isNotEmpty()) {
                            chunks.add(TextMappingChunk(lineStart + leadingSpaces + 2, lineEnd, tBulletEnd, sb.length))
                        }
                    }
                    trimmed.startsWith("> ") -> {
                        // Blockquote
                        sb.append(indentStr)
                        val tBarStart = sb.length
                        sb.append("▎ ")
                        val tBarEnd = sb.length
                        styles.add(SpanStyle(color = accentColor, fontWeight = FontWeight.Bold) to (tBarStart until tBarEnd))

                        val content = trimmed.substring(2)
                        val tContentStart = sb.length
                        sb.append(content)
                        styles.add(SpanStyle(color = mutedColor) to (tContentStart until sb.length))

                        chunks.add(TextMappingChunk(lineStart, lineStart + leadingSpaces + 2, tLineStart, tBarEnd))
                        if (content.isNotEmpty()) {
                            chunks.add(TextMappingChunk(lineStart + leadingSpaces + 2, lineEnd, tBarEnd, sb.length))
                        }
                    }
                    trimmed.startsWith("# ") -> {
                        sb.append(indentStr)
                        val content = trimmed.substring(2)
                        val tContentStart = sb.length
                        sb.append(content)
                        styles.add(SpanStyle(fontSize = 22.sp, fontWeight = FontWeight.Bold, color = accentColor) to (tContentStart until sb.length))
                        chunks.add(TextMappingChunk(lineStart, lineStart + leadingSpaces + 2, tLineStart, tContentStart))
                        if (content.isNotEmpty()) {
                            chunks.add(TextMappingChunk(lineStart + leadingSpaces + 2, lineEnd, tContentStart, sb.length))
                        }
                    }
                    trimmed.startsWith("## ") -> {
                        sb.append(indentStr)
                        val content = trimmed.substring(3)
                        val tContentStart = sb.length
                        sb.append(content)
                        styles.add(SpanStyle(fontSize = 19.sp, fontWeight = FontWeight.Bold, color = textColor) to (tContentStart until sb.length))
                        chunks.add(TextMappingChunk(lineStart, lineStart + leadingSpaces + 3, tLineStart, tContentStart))
                        if (content.isNotEmpty()) {
                            chunks.add(TextMappingChunk(lineStart + leadingSpaces + 3, lineEnd, tContentStart, sb.length))
                        }
                    }
                    trimmed.startsWith("### ") -> {
                        sb.append(indentStr)
                        val content = trimmed.substring(4)
                        val tContentStart = sb.length
                        sb.append(content)
                        styles.add(SpanStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = textColor) to (tContentStart until sb.length))
                        chunks.add(TextMappingChunk(lineStart, lineStart + leadingSpaces + 4, tLineStart, tContentStart))
                        if (content.isNotEmpty()) {
                            chunks.add(TextMappingChunk(lineStart + leadingSpaces + 4, lineEnd, tContentStart, sb.length))
                        }
                    }
                    else -> {
                        sb.append(lineText)
                        chunks.add(TextMappingChunk(lineStart, lineEnd, tLineStart, sb.length))
                    }
                }
            }

            if (lineEnd < raw.length && raw[lineEnd] == '\n') {
                val tNl = sb.length
                sb.append('\n')
                chunks.add(TextMappingChunk(lineEnd, lineEnd + 1, tNl, tNl + 1))
            }

            lineStart = lineEnd + 1
        }

        val transformedString = sb.toString()
        val offsetMapping = MarkdownOffsetMapping(raw.length, transformedString.length, chunks)

        // Apply markdown syntax formatting across all spans
        val doc = MarkdownParser.parse(raw)
        val annotated = buildAnnotatedString {
            append(transformedString)

            // 1. Add line-level sprite styles
            for ((spanStyle, range) in styles) {
                val cStart = range.first.coerceIn(0, length)
                val cEnd = (range.last + 1).coerceIn(cStart, length)
                if (cStart < cEnd) {
                    addStyle(spanStyle, cStart, cEnd)
                }
            }

            // 2. Add inline formatting styles
            for (span in doc.spans) {
                val tStart = offsetMapping.originalToTransformed(span.contentStart)
                val tEnd = offsetMapping.originalToTransformed(span.contentEnd)
                if (tStart < tEnd && tEnd <= length) {
                    when (span.type) {
                        MarkdownSpanType.BOLD -> {
                            addStyle(SpanStyle(fontWeight = FontWeight.Bold, color = textColor), tStart, tEnd)
                        }
                        MarkdownSpanType.ITALIC -> {
                            addStyle(SpanStyle(fontStyle = FontStyle.Italic), tStart, tEnd)
                        }
                        MarkdownSpanType.BOLD_ITALIC -> {
                            addStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = textColor), tStart, tEnd)
                        }
                        MarkdownSpanType.STRIKETHROUGH -> {
                            addStyle(SpanStyle(textDecoration = TextDecoration.LineThrough), tStart, tEnd)
                        }
                        MarkdownSpanType.INLINE_CODE -> {
                            addStyle(
                                SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    background = codeBgColor,
                                    color = codeTextColor,
                                    fontSize = 13.5.sp
                                ),
                                tStart,
                                tEnd
                            )
                        }
                        MarkdownSpanType.CODE_BLOCK -> {
                            addStyle(
                                SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    background = codeBgColor,
                                    color = codeTextColor,
                                    fontSize = 13.sp
                                ),
                                tStart,
                                tEnd
                            )
                        }
                        MarkdownSpanType.LINK -> {
                            addStyle(
                                SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline
                                ),
                                tStart,
                                tEnd
                            )
                        }
                        else -> {
                            // Already handled by line sprite parser
                        }
                    }
                }

                // If on active line, dim raw marker tokens
                val isTouched = span.touchesSelection(cursorStart, cursorEnd)
                if (isTouched) {
                    val dimmedStyle = SpanStyle(color = mutedColor.copy(alpha = 0.5f), fontSize = 11.5.sp)
                    if (span.markerStartLen > 0) {
                        val mStart = offsetMapping.originalToTransformed(span.start)
                        val mEnd = offsetMapping.originalToTransformed(span.start + span.markerStartLen)
                        if (mStart < mEnd && mEnd <= length) {
                            addStyle(dimmedStyle, mStart, mEnd)
                        }
                    }
                    if (span.markerEndLen > 0) {
                        val mStart = offsetMapping.originalToTransformed(span.end - span.markerEndLen)
                        val mEnd = offsetMapping.originalToTransformed(span.end)
                        if (mStart < mEnd && mEnd <= length) {
                            addStyle(dimmedStyle, mStart, mEnd)
                        }
                    }
                }
            }
        }

        return TransformedText(annotated, offsetMapping)
    }
}
