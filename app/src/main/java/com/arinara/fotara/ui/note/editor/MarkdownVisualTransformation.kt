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
 * VisualTransformation that provides rich live rendering in the Markdown editor.
 * Styles content with bold, italic, headings, monospace code, links, checklists,
 * and strikethroughs.
 *
 * Untouched syntax markers are hidden with bidirectional [MarkdownOffsetMapping],
 * while markers touched by the cursor or selection are displayed dimmed and compact.
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

        val doc = MarkdownParser.parse(raw)
        val hiddenRanges = mutableListOf<IntRange>()

        // 1. Identify which markers to hide (untouched by cursor/selection)
        if (hideUntouchedMarkers) {
            for (span in doc.spans) {
                val isTouched = span.touchesSelection(cursorStart, cursorEnd)
                if (!isTouched) {
                    when (span.type) {
                        MarkdownSpanType.BOLD,
                        MarkdownSpanType.ITALIC,
                        MarkdownSpanType.BOLD_ITALIC,
                        MarkdownSpanType.STRIKETHROUGH,
                        MarkdownSpanType.INLINE_CODE -> {
                            if (span.markerStartLen > 0) {
                                hiddenRanges.add(span.start until (span.start + span.markerStartLen))
                            }
                            if (span.markerEndLen > 0) {
                                hiddenRanges.add((span.end - span.markerEndLen) until span.end)
                            }
                        }
                        MarkdownSpanType.HEADING_1,
                        MarkdownSpanType.HEADING_2,
                        MarkdownSpanType.HEADING_3,
                        MarkdownSpanType.BLOCKQUOTE -> {
                            if (span.markerStartLen > 0) {
                                hiddenRanges.add(span.start until (span.start + span.markerStartLen))
                            }
                        }
                        MarkdownSpanType.LINK -> {
                            // Hide the "[" and the "](url)" part when not touched
                            hiddenRanges.add(span.start until (span.start + 1))
                            hiddenRanges.add(span.contentEnd until span.end)
                        }
                        else -> {
                            // Keep list bullets and code blocks visible
                        }
                    }
                }
            }
        }

        // 2. Build transformed string and offset mapping
        val offsetMapping = if (hiddenRanges.isNotEmpty()) {
            MarkdownOffsetMapping(
                originalLength = raw.length,
                transformedLength = raw.length - hiddenRanges.sumOf { it.last - it.first + 1 },
                hiddenRanges = hiddenRanges
            )
        } else {
            OffsetMapping.Identity
        }

        // Build the styled output
        val annotated = buildAnnotatedString {
            // Append characters that are not in hidden ranges
            val isHidden = BooleanArray(raw.length)
            for (range in hiddenRanges) {
                for (i in range.first..range.last.coerceAtMost(raw.length - 1)) {
                    isHidden[i] = true
                }
            }

            for (i in raw.indices) {
                if (!isHidden[i]) {
                    append(raw[i])
                }
            }

            // Apply formatting styles to content spans
            for (span in doc.spans) {
                val tContentStart = offsetMapping.originalToTransformed(span.contentStart)
                val tContentEnd = offsetMapping.originalToTransformed(span.contentEnd)

                if (tContentStart < tContentEnd) {
                    when (span.type) {
                        MarkdownSpanType.BOLD -> {
                            addStyle(
                                SpanStyle(fontWeight = FontWeight.Bold, color = textColor),
                                tContentStart,
                                tContentEnd
                            )
                        }
                        MarkdownSpanType.ITALIC -> {
                            addStyle(
                                SpanStyle(fontStyle = FontStyle.Italic),
                                tContentStart,
                                tContentEnd
                            )
                        }
                        MarkdownSpanType.BOLD_ITALIC -> {
                            addStyle(
                                SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic, color = textColor),
                                tContentStart,
                                tContentEnd
                            )
                        }
                        MarkdownSpanType.STRIKETHROUGH -> {
                            addStyle(
                                SpanStyle(textDecoration = TextDecoration.LineThrough),
                                tContentStart,
                                tContentEnd
                            )
                        }
                        MarkdownSpanType.INLINE_CODE -> {
                            addStyle(
                                SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    background = codeBgColor,
                                    color = codeTextColor,
                                    fontSize = 14.sp
                                ),
                                tContentStart,
                                tContentEnd
                            )
                        }
                        MarkdownSpanType.CODE_BLOCK -> {
                            addStyle(
                                SpanStyle(
                                    fontFamily = FontFamily.Monospace,
                                    background = codeBgColor,
                                    color = codeTextColor,
                                    fontSize = 13.5.sp
                                ),
                                tContentStart,
                                tContentEnd
                            )
                        }
                        MarkdownSpanType.HEADING_1 -> {
                            addStyle(
                                SpanStyle(
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accentColor
                                ),
                                tContentStart,
                                tContentEnd
                            )
                        }
                        MarkdownSpanType.HEADING_2 -> {
                            addStyle(
                                SpanStyle(
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                ),
                                tContentStart,
                                tContentEnd
                            )
                        }
                        MarkdownSpanType.HEADING_3 -> {
                            addStyle(
                                SpanStyle(
                                    fontSize = 17.5.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = textColor
                                ),
                                tContentStart,
                                tContentEnd
                            )
                        }
                        MarkdownSpanType.BLOCKQUOTE -> {
                            addStyle(
                                SpanStyle(
                                    fontStyle = FontStyle.Italic,
                                    color = mutedColor
                                ),
                                tContentStart,
                                tContentEnd
                            )
                        }
                        MarkdownSpanType.LINK -> {
                            addStyle(
                                SpanStyle(
                                    color = linkColor,
                                    textDecoration = TextDecoration.Underline
                                ),
                                tContentStart,
                                tContentEnd
                            )
                        }
                        MarkdownSpanType.CHECKLIST_UNCHECKED -> {
                            addStyle(
                                SpanStyle(color = textColor),
                                tContentStart,
                                tContentEnd
                            )
                        }
                        MarkdownSpanType.CHECKLIST_CHECKED -> {
                            addStyle(
                                SpanStyle(
                                    color = mutedColor,
                                    textDecoration = TextDecoration.LineThrough
                                ),
                                tContentStart,
                                tContentEnd
                            )
                        }
                        MarkdownSpanType.BULLET_LIST,
                        MarkdownSpanType.NUMBERED_LIST -> {
                            addStyle(
                                SpanStyle(color = textColor),
                                tContentStart,
                                tContentEnd
                            )
                        }
                        MarkdownSpanType.HORIZONTAL_RULE -> {
                            addStyle(
                                SpanStyle(color = mutedColor.copy(alpha = 0.5f), fontWeight = FontWeight.Bold),
                                tContentStart,
                                tContentEnd
                            )
                        }
                    }
                }

                // If markers are visible (e.g. cursor is touching them), dim and shrink the marker characters
                val isTouched = span.touchesSelection(cursorStart, cursorEnd)
                if (isTouched || !hideUntouchedMarkers) {
                    val dimmedStyle = SpanStyle(
                        color = mutedColor.copy(alpha = 0.45f),
                        fontSize = 11.5.sp
                    )
                    // Opening marker
                    if (span.markerStartLen > 0) {
                        val tMStart = offsetMapping.originalToTransformed(span.start)
                        val tMEnd = offsetMapping.originalToTransformed(span.start + span.markerStartLen)
                        if (tMStart < tMEnd) {
                            addStyle(dimmedStyle, tMStart, tMEnd)
                        }
                    }
                    // Closing marker
                    if (span.markerEndLen > 0) {
                        val tMStart = offsetMapping.originalToTransformed(span.end - span.markerEndLen)
                        val tMEnd = offsetMapping.originalToTransformed(span.end)
                        if (tMStart < tMEnd) {
                            addStyle(dimmedStyle, tMStart, tMEnd)
                        }
                    }
                }
            }
        }

        return TransformedText(annotated, offsetMapping)
    }
}
