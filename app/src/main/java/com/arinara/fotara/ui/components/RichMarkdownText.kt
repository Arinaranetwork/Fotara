// --- Arinara Network (c) 2026 ---
// Exclusive property of Arinara Network.
// Unauthorized use, reproduction, distribution, or modification of this code,
// in whole or in part, for any purpose, is strictly prohibited without prior
// written consent from Arinara Network as sole legal owner of this codebase.

package com.arinara.fotara.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import com.arinara.fotara.theme.ElmsSans
import com.arinara.fotara.theme.FolderTabCream
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Builds an [AnnotatedString] supporting inline markdown syntax:
 * - ***bold italic*** or ___bold italic___
 * - **bold** or __bold__
 * - *italic* or _italic_
 * - `inline code`
 * - ~~strikethrough~~
 * - [link text](url)
 */
fun buildRichMarkdownAnnotatedString(
    text: String,
    codeBackground: Color = Color(0xFF1E254A),
    codeColor: Color = Color(0xFFE2E8F0),
    accentColor: Color = Color(0xFFF77F00),
    linkColor: Color = Color(0xFF64B5F6)
): AnnotatedString {
    return buildAnnotatedString {
        appendRichMarkdownInternal(
            rawText = text,
            codeBackground = codeBackground,
            codeColor = codeColor,
            accentColor = accentColor,
            linkColor = linkColor,
            depth = 0
        )
    }
}

private val INLINE_TOKEN_REGEX = Regex(
    """(\*\*\*(?:[^*]|\*(?!\*\*))+\*\*\*)|""" + // 1: ***bold italic***
    """(___(?:[^_]|_(?!__))+___)|""" +         // 2: ___bold italic___
    """(\*\*(?:[^*]|\*(?!\*))+\*\*)|""" +       // 3: **bold**
    """(__(?:[^_]|_(?!_))+__)|""" +             // 4: __bold__
    """(\*(?:[^*])+\*)|""" +                     // 5: *italic*
    """(_(?:[^_])+_)|""" +                       // 6: _italic_
    """(`[^`\n]+`)|""" +                         // 7: `code`
    """(~~(?:[^~])+~~)|""" +                     // 8: ~~strikethrough~~
    """(\[([^\]]+)\]\(([^)]+)\))|""" +           // 9: [link](url)
    """(\$\$[\s\S]+?\$\$)|""" +                  // 10: $$ block math $$
    """((?<!\$)\$(?!\$)[^$\n]+?(?<!\$)\$)"""     // 11: $ inline math $
)

private fun AnnotatedString.Builder.appendRichMarkdownInternal(
    rawText: String,
    codeBackground: Color,
    codeColor: Color,
    accentColor: Color,
    linkColor: Color,
    depth: Int
) {
    if (depth > 3 || rawText.isEmpty()) {
        append(rawText)
        return
    }

    var lastIndex = 0
    val matches = INLINE_TOKEN_REGEX.findAll(rawText)

    for (match in matches) {
        val range = match.range
        if (range.first > lastIndex) {
            append(rawText.substring(lastIndex, range.first))
        }

        val fullMatch = match.value
        when {
            // ***bold italic*** or ___bold italic___
            fullMatch.startsWith("***") && fullMatch.endsWith("***") && fullMatch.length >= 6 -> {
                val inner = fullMatch.substring(3, fullMatch.length - 3)
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                appendRichMarkdownInternal(inner, codeBackground, codeColor, accentColor, linkColor, depth + 1)
                pop()
            }
            fullMatch.startsWith("___") && fullMatch.endsWith("___") && fullMatch.length >= 6 -> {
                val inner = fullMatch.substring(3, fullMatch.length - 3)
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic))
                appendRichMarkdownInternal(inner, codeBackground, codeColor, accentColor, linkColor, depth + 1)
                pop()
            }

            // **bold** or __bold__
            fullMatch.startsWith("**") && fullMatch.endsWith("**") && fullMatch.length >= 4 -> {
                val inner = fullMatch.substring(2, fullMatch.length - 2)
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                appendRichMarkdownInternal(inner, codeBackground, codeColor, accentColor, linkColor, depth + 1)
                pop()
            }
            fullMatch.startsWith("__") && fullMatch.endsWith("__") && fullMatch.length >= 4 -> {
                val inner = fullMatch.substring(2, fullMatch.length - 2)
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold))
                appendRichMarkdownInternal(inner, codeBackground, codeColor, accentColor, linkColor, depth + 1)
                pop()
            }

            // *italic* or _italic_
            fullMatch.startsWith("*") && fullMatch.endsWith("*") && fullMatch.length >= 2 -> {
                val inner = fullMatch.substring(1, fullMatch.length - 1)
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                appendRichMarkdownInternal(inner, codeBackground, codeColor, accentColor, linkColor, depth + 1)
                pop()
            }
            fullMatch.startsWith("_") && fullMatch.endsWith("_") && fullMatch.length >= 2 -> {
                val inner = fullMatch.substring(1, fullMatch.length - 1)
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic))
                appendRichMarkdownInternal(inner, codeBackground, codeColor, accentColor, linkColor, depth + 1)
                pop()
            }

            // `code`
            fullMatch.startsWith("`") && fullMatch.endsWith("`") && fullMatch.length >= 2 -> {
                val inner = fullMatch.substring(1, fullMatch.length - 1)
                pushStyle(
                    SpanStyle(
                        fontFamily = ElmsSans,
                        background = codeBackground,
                        color = codeColor,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp
                    )
                )
                append(" $inner ")
                pop()
            }

            // ~~strikethrough~~
            fullMatch.startsWith("~~") && fullMatch.endsWith("~~") && fullMatch.length >= 4 -> {
                val inner = fullMatch.substring(2, fullMatch.length - 2)
                pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough))
                appendRichMarkdownInternal(inner, codeBackground, codeColor, accentColor, linkColor, depth + 1)
                pop()
            }

            // [text](url)
            fullMatch.startsWith("[") && fullMatch.contains("](") && fullMatch.endsWith(")") -> {
                val closeBracket = fullMatch.indexOf("](")
                val linkText = fullMatch.substring(1, closeBracket)
                pushStyle(SpanStyle(fontFamily = ElmsSans, color = linkColor, textDecoration = TextDecoration.Underline))
                append(linkText)
                pop()
            }

            // $$ block math $$
            fullMatch.startsWith("$$") && fullMatch.endsWith("$$") && fullMatch.length >= 4 -> {
                val inner = fullMatch.substring(2, fullMatch.length - 2).trim()
                val readable = com.arinara.fotara.ui.note.editor.KatexMathRenderer.formatToReadableMath(inner)
                pushStyle(
                    SpanStyle(
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Bold,
                        background = codeBackground,
                        color = linkColor,
                        fontSize = 16.sp
                    )
                )
                append("  $readable  ")
                pop()
            }

            // $ inline math $
            fullMatch.startsWith("$") && fullMatch.endsWith("$") && fullMatch.length >= 2 -> {
                val inner = fullMatch.substring(1, fullMatch.length - 1).trim()
                val readable = com.arinara.fotara.ui.note.editor.KatexMathRenderer.formatToReadableMath(inner)
                pushStyle(
                    SpanStyle(
                        fontFamily = ElmsSans,
                        fontWeight = FontWeight.Medium,
                        background = codeBackground,
                        color = linkColor,
                        fontSize = 14.sp
                    )
                )
                append(" $readable ")
                pop()
            }

            else -> {
                append(fullMatch)
            }
        }
        lastIndex = range.last + 1
    }

    if (lastIndex < rawText.length) {
        append(rawText.substring(lastIndex))
    }
}

/**
 * Universal Composable for rendering rich text with inline markdown spans
 * (bold, italic, inline code, strikethrough, links).
 */
@Composable
fun RichMarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = Color(0xFFEAE3D2),
    fontSize: TextUnit = 14.sp,
    fontStyle: FontStyle? = null,
    fontWeight: FontWeight? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    maxLines: Int = Int.MAX_VALUE,
    overflow: TextOverflow = TextOverflow.Clip,
    codeBackground: Color = Color(0xFF1E254A),
    codeColor: Color = Color(0xFFE2E8F0),
    accentColor: Color = Color(0xFFF77F00),
    linkColor: Color = Color(0xFF64B5F6),
    textDecoration: TextDecoration? = null
) {
    val annotated = remember(text, codeBackground, codeColor, accentColor, linkColor) {
        buildRichMarkdownAnnotatedString(
            text = text,
            codeBackground = codeBackground,
            codeColor = codeColor,
            accentColor = accentColor,
            linkColor = linkColor
        )
    }

    Text(
        text = annotated,
        modifier = modifier,
        color = color,
        fontSize = fontSize,
        fontStyle = fontStyle,
        fontWeight = fontWeight,
        lineHeight = lineHeight,
        maxLines = maxLines,
        overflow = overflow,
        textDecoration = textDecoration
    )
}

/**
 * Blockquote composable with vertical gold bar and indented italic styling.
 */
@Composable
fun RichMarkdownBlockquote(
    quote: String,
    modifier: Modifier = Modifier,
    accentColor: Color = Color(0xFFF77F00),
    backgroundColor: Color = Color(0xFF101532),
    borderColor: Color = Color(0xFF283256),
    textColor: Color = Color(0xFFEAE3D2).copy(alpha = 0.88f)
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
            .height(IntrinsicSize.Min)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .fillMaxHeight()
                .clip(RoundedCornerShape(2.dp))
                .background(accentColor)
        )
        Spacer(modifier = Modifier.width(10.dp))
        RichMarkdownText(
            text = quote,
            color = textColor,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            accentColor = accentColor
        )
    }
}

/**
 * Multiline code block composable with dark editor background and monospace font.
 */
@Composable
fun RichMarkdownCodeBlock(
    code: String,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF0B1028),
    borderColor: Color = Color(0xFF242E52),
    codeColor: Color = Color(0xFFE2E8F0)
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(10.dp))
            .padding(12.dp)
            .horizontalScroll(rememberScrollState())
    ) {
        Text(
            text = code,
            color = codeColor,
            fontFamily = ElmsSans,
            fontSize = 12.sp,
            lineHeight = 16.sp
        )
    }
}

/**
 * Dedicated visual Markdown table composable:
 * - Elevated surface with RoundedCornerShape(8.dp)
 * - 1dp border with cardBorder
 * - Tinted header row (Color(0xFF1E254A)) with bold text
 * - Alternating subtle row backgrounds
 * - 1dp grid dividers between cells
 * - Horizontal scroll inside container for wide tables
 * - Rich Markdown formatting in each cell
 */
@Composable
fun RichMarkdownTable(
    headers: List<String>,
    rows: List<List<String>>,
    modifier: Modifier = Modifier,
    backgroundColor: Color = Color(0xFF141936),
    headerBackground: Color = Color(0xFF1E254A),
    borderColor: Color = Color(0xFF283256),
    headerTextColor: Color = FolderTabCream,
    textColor: Color = Color(0xFFEAE3D2),
    accentColor: Color = Color(0xFFF77F00)
) {
    if (headers.isEmpty()) return

    val colWidths = remember(headers, rows) {
        headers.indices.map { colIndex ->
            val headerLen = headers.getOrNull(colIndex)?.length ?: 0
            val maxRowLen = rows.maxOfOrNull { it.getOrNull(colIndex)?.length ?: 0 } ?: 0
            val maxLen = maxOf(headerLen, maxRowLen)
            (maxOf(80, maxLen * 9) + 24).dp.coerceIn(80.dp, 300.dp)
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(backgroundColor)
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(8.dp))
            .horizontalScroll(rememberScrollState())
    ) {
        Column {
            // Header Row
            Row(
                modifier = Modifier
                    .background(headerBackground)
                    .height(IntrinsicSize.Min),
                verticalAlignment = Alignment.CenterVertically
            ) {
                headers.forEachIndexed { colIndex, headerText ->
                    if (colIndex > 0) {
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(borderColor)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .width(colWidths[colIndex])
                            .padding(horizontal = 12.dp, vertical = 9.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        RichMarkdownText(
                            text = headerText,
                            color = headerTextColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            accentColor = accentColor
                        )
                    }
                }
            }

            // Divider between header and body
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(borderColor)
            )

            // Body Rows
            rows.forEachIndexed { rowIndex, rowCells ->
                if (rowIndex > 0) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(borderColor.copy(alpha = 0.6f))
                    )
                }
                Row(
                    modifier = Modifier
                        .background(if (rowIndex % 2 == 1) Color(0xFF101530) else backgroundColor)
                        .height(IntrinsicSize.Min),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    headers.indices.forEach { colIndex ->
                        if (colIndex > 0) {
                            Box(
                                modifier = Modifier
                                    .width(1.dp)
                                    .fillMaxHeight()
                                    .background(borderColor.copy(alpha = 0.6f))
                            )
                        }
                        val cellText = rowCells.getOrNull(colIndex) ?: ""
                        Box(
                            modifier = Modifier
                                .width(colWidths[colIndex])
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            RichMarkdownText(
                                text = cellText,
                                color = textColor.copy(alpha = 0.92f),
                                fontSize = 13.sp,
                                accentColor = accentColor
                            )
                        }
                    }
                }
            }
        }
    }
}

internal fun isTableSeparatorLine(line: String): Boolean {
    val trimmed = line.trim()
    if (!trimmed.contains('-') || !trimmed.contains('|')) return false
    val cells = parseTableRowCells(trimmed)
    if (cells.isEmpty()) return false
    return cells.all { cell -> cell.matches(Regex("""^:?-+:?$""")) }
}

internal fun parseTableRowCells(line: String): List<String> {
    var trimmed = line.trim()
    if (trimmed.startsWith("|") && trimmed.endsWith("|") && trimmed.length >= 2) {
        trimmed = trimmed.substring(1, trimmed.length - 1)
    } else if (trimmed.startsWith("|")) {
        trimmed = trimmed.substring(1)
    } else if (trimmed.endsWith("|")) {
        trimmed = trimmed.substring(0, trimmed.length - 1)
    }
    val cells = mutableListOf<String>()
    val current = StringBuilder()
    var i = 0
    while (i < trimmed.length) {
        val ch = trimmed[i]
        if (ch == '\\' && i + 1 < trimmed.length && trimmed[i + 1] == '|') {
            current.append('|')
            i += 2
        } else if (ch == '|') {
            cells.add(current.toString().trim())
            current.clear()
            i++
        } else {
            current.append(ch)
            i++
        }
    }
    cells.add(current.toString().trim())
    return cells
}

/**
 * High-level markdown document column that parses and renders:
 * - H1, H2, H3 headers
 * - Blockquotes (`> `)
 * - Bullet lists (`- `, `* `)
 * - Numbered lists (`1. `, `2. `)
 * - Code blocks (```)
 * - Markdown tables (`| ... |`)
 * - Paragraphs with inline bold/italic/code/strikethrough
 */
@Composable
fun RichMarkdownColumn(
    markdown: String,
    modifier: Modifier = Modifier,
    primaryTextColor: Color = Color(0xFFEAE3D2),
    accentColor: Color = Color(0xFFF77F00),
    cardBg: Color = Color(0xFF141936),
    cardBorder: Color = Color(0xFF283256),
    onToggleChecklistLine: ((Int) -> Unit)? = null
) {
    val lines = remember(markdown) { markdown.lines() }

    Column(modifier = modifier.fillMaxWidth()) {
        var inCodeBlock = false
        val codeBlockBuffer = StringBuilder()

        var lineIndex = 0
        while (lineIndex < lines.size) {
            val rawLine = lines[lineIndex]
            val trimmed = rawLine.trim()

            // Code block handling (```)
            if (trimmed.startsWith("```")) {
                if (inCodeBlock) {
                    // Close code block
                    inCodeBlock = false
                    RichMarkdownCodeBlock(
                        code = codeBlockBuffer.toString().trimEnd(),
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                    codeBlockBuffer.clear()
                } else {
                    // Start code block
                    inCodeBlock = true
                }
                lineIndex++
                continue
            }

            if (inCodeBlock) {
                codeBlockBuffer.append(rawLine).append("\n")
                lineIndex++
                continue
            }

            // Skip empty lines or image banners (already handled by hero banner)
            if (trimmed.isEmpty() || trimmed.startsWith("![")) {
                lineIndex++
                continue
            }

            // Table detection
            if (trimmed.contains('|') && lineIndex + 1 < lines.size && isTableSeparatorLine(lines[lineIndex + 1])) {
                val headerCells = parseTableRowCells(trimmed)
                lineIndex += 2 // Skip header line and separator line
                val dataRows = mutableListOf<List<String>>()
                while (lineIndex < lines.size) {
                    val nextRow = lines[lineIndex].trim()
                    if (nextRow.isEmpty() || !nextRow.contains('|') || nextRow.startsWith("```") || nextRow.startsWith("#")) {
                        break
                    }
                    dataRows.add(parseTableRowCells(nextRow))
                    lineIndex++
                }
                RichMarkdownTable(
                    headers = headerCells,
                    rows = dataRows,
                    backgroundColor = cardBg,
                    headerBackground = Color(0xFF1E254A),
                    borderColor = cardBorder,
                    headerTextColor = FolderTabCream,
                    textColor = primaryTextColor,
                    accentColor = accentColor,
                    modifier = Modifier.padding(vertical = 8.dp)
                )
                continue
            }

            when {
                // Header 1 (# Title)
                trimmed.startsWith("# ") -> {
                    Text(
                        text = trimmed.removePrefix("# ").trim(),
                        color = accentColor,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp)
                    )
                }

                // Header 2 (## Section)
                trimmed.startsWith("## ") -> {
                    Text(
                        text = trimmed.removePrefix("## ").trim(),
                        color = accentColor,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }

                // Header 3 (### Subtitle / Version)
                trimmed.startsWith("### ") -> {
                    Text(
                        text = trimmed.removePrefix("### ").trim(),
                        color = primaryTextColor,
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 2.dp)
                    )
                }

                // Blockquote (> Quote)
                trimmed.startsWith("> ") || trimmed.startsWith(">") -> {
                    val quoteContent = trimmed.removePrefix(">").trim()
                    RichMarkdownBlockquote(
                        quote = quoteContent,
                        accentColor = accentColor,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Checklist items (- [ ] or - [x] with optional indentation)
                Regex("""^(\s*[-*+]\s*)\[([ xX])\]\s*(.*)$""").containsMatchIn(rawLine) -> {
                    val chkMatch = Regex("""^(\s*[-*+]\s*)\[([ xX])\]\s*(.*)$""").find(rawLine)!!
                    val isChecked = chkMatch.groupValues[2].equals("x", ignoreCase = true)
                    val itemContent = chkMatch.groupValues[3]
                    val leadingSpaces = rawLine.takeWhile { it == ' ' }.length
                    val indentLevel = (leadingSpaces / 2).coerceIn(0, 3)

                    val currentLineIdx = lineIndex
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = (indentLevel * 24).dp)
                            .clip(RoundedCornerShape(6.dp))
                            .clickable(enabled = onToggleChecklistLine != null) {
                                onToggleChecklistLine?.invoke(currentLineIdx)
                            }
                            .padding(vertical = 4.dp, horizontal = 2.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(18.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isChecked) accentColor else Color.Transparent)
                                .border(
                                    BorderStroke(1.8.dp, accentColor),
                                    RoundedCornerShape(4.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isChecked) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Checked",
                                    tint = Color(0xFF03071E),
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        RichMarkdownText(
                            text = itemContent,
                            color = if (isChecked) primaryTextColor.copy(alpha = 0.5f) else primaryTextColor.copy(alpha = 0.92f),
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                            accentColor = accentColor,
                            textDecoration = if (isChecked) TextDecoration.LineThrough else null
                        )
                    }
                }

                // Bullet item (- Item or * Item)
                trimmed.startsWith("- ") || trimmed.startsWith("* ") -> {
                    val itemContent = trimmed.substring(2).trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 7.dp)
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(accentColor)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        RichMarkdownText(
                            text = itemContent,
                            color = primaryTextColor.copy(alpha = 0.92f),
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            accentColor = accentColor
                        )
                    }
                }

                // Numbered list (1. Item)
                trimmed.matches(Regex("""^\d+\.\s+.*""")) -> {
                    val dotIndex = trimmed.indexOf('.')
                    val number = trimmed.substring(0, dotIndex)
                    val content = trimmed.substring(dotIndex + 1).trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(accentColor.copy(alpha = 0.2f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text(
                                text = number,
                                color = accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        RichMarkdownText(
                            text = content,
                            color = primaryTextColor.copy(alpha = 0.92f),
                            fontSize = 13.sp,
                            lineHeight = 19.sp,
                            accentColor = accentColor
                        )
                    }
                }

                // Divider (--- or ***)
                trimmed == "---" || trimmed == "***" -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(cardBorder)
                            .padding(vertical = 6.dp)
                    )
                }

                // Regular paragraph
                else -> {
                    RichMarkdownText(
                        text = trimmed,
                        color = primaryTextColor.copy(alpha = 0.88f),
                        fontSize = 13.sp,
                        lineHeight = 19.sp,
                        accentColor = accentColor,
                        modifier = Modifier.padding(vertical = 2.dp)
                    )
                }
            }
            lineIndex++
        }

        // Flush any unclosed code block
        if (inCodeBlock && codeBlockBuffer.isNotEmpty()) {
            RichMarkdownCodeBlock(
                code = codeBlockBuffer.toString().trimEnd(),
                modifier = Modifier.padding(vertical = 6.dp)
            )
        }
    }
}
